using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Commands;
using Isl.Commands.Expressions;
using Isl.Modifiers;

namespace Isl.Runtime;

/// <summary>
/// Static dispatcher for modifier application. Mirrors the original
/// <c>Interpreter.ApplyModifier</c> but takes pre-evaluated args and precompiled per-item commands
/// directly (no delegate allocations on the hot path).
/// Milestone 2 will replace name-based string dispatch here with a registry of typed delegates.
/// </summary>
public static class ModifierExecutor
{
    /// <summary>
    /// Apply a modifier given its already-evaluated value and arguments. Specialised commands
    /// (filter / map / group.by) supply precompiled per-item commands so that per-item evaluation
    /// avoids any per-call closure allocation.
    /// </summary>
    /// <param name="val">Upstream pipeline value.</param>
    /// <param name="mod">Originating AST node.</param>
    /// <param name="args">Already-evaluated argument values (length matches <c>mod.Arguments.Count</c>).</param>
    /// <param name="ctx">Operation context.</param>
    /// <param name="ifCondition">Optional <c>| if(cond)</c> wrapper, evaluated against a child scope with $/mval bound.</param>
    /// <param name="filterCondition">Per-item filter predicate (used by <c>| filter cond</c>).</param>
    /// <param name="mapProjection">Per-item map projection (used by <c>| map(expr)</c>).</param>
    /// <param name="groupByKeyExpr">Per-item key evaluator for <c>| group.by</c>.</param>
    public static JsonNode? Apply(
        JsonNode? val,
        ModifierNode mod,
        JsonNode?[] args,
        IOperationContext ctx,
        ConditionCommand? ifCondition,
        ConditionCommand? filterCondition,
        IIslCommand? mapProjection,
        IIslCommand? groupByKeyExpr)
    {
        if (mod.Condition != null && ifCondition != null)
        {
            var condScope = ctx.CreateChildScope();
            condScope.SetVariable("mval", val?.DeepClone());
            condScope.SetVariable("$", val?.DeepClone());
            if (!ifCondition.Evaluate(condScope))
                return val;
        }

        var modName = mod.Name.ToLower();
        var modSub = mod.SubName?.ToLower();

        if (modName == "group" && modSub == "by")
        {
            return ApplyGroupBy(val, mod.Arguments, args, ctx, groupByKeyExpr);
        }

        if (modName == "to" && modSub != null)
            return TypeModifiers.Apply(val, modSub, args);

        if (modName == "date" && modSub != null)
            return DateModifiers.Apply(val, modSub, args);

        if (modName == "math" && modSub != null)
            return MathModifiers.ApplyNamed(val, modSub, args);

        if (modName == "json")
        {
            if (modSub == "parse")
            {
                if (val == null) return null;
                var jsonStr = RuntimeHelpers.JsonToString(val);
                if (string.IsNullOrWhiteSpace(jsonStr)) return null;
                try { return JsonNode.Parse(jsonStr); }
                catch { return null; }
            }
            if (modSub != null)
                return JsonValue.Create($"Unknown modifier: json.{modSub}");
            return JsonModifiers.Apply(val, args);
        }

        if (modName == "filter" && filterCondition != null)
        {
            return ApplyFilter(val, ctx, filterCondition);
        }

        if (modName == "map" && mapProjection != null && mod.Arguments.Count > 0)
        {
            return ApplyMap(val, ctx, mapProjection);
        }

        if (modName == "kv" && modSub == null)
        {
            if (val is JsonObject kvObj)
            {
                var arr = new JsonArray();
                foreach (var pair in kvObj)
                {
                    var item = new JsonObject
                    {
                        ["key"] = JsonValue.Create(pair.Key),
                        ["value"] = pair.Value?.DeepClone()
                    };
                    arr.Add(item);
                }
                return arr;
            }
            return val;
        }

        if (modName == "round" && modSub != null)
        {
            int decimals = args.Length > 0 ? (int)RuntimeHelpers.ToDouble(args[0]) : 0;
            double d = RuntimeHelpers.ToDouble(val);
            double rounded = modSub switch
            {
                "up" => Math.Ceiling(d * Math.Pow(10, decimals)) / Math.Pow(10, decimals),
                "down" => Math.Floor(d * Math.Pow(10, decimals)) / Math.Pow(10, decimals),
                _ => Math.Round(d, decimals, MidpointRounding.AwayFromZero)
            };
            return JsonValue.Create(rounded.ToString("F" + decimals, System.Globalization.CultureInfo.InvariantCulture));
        }

        var fullModName = modSub != null ? $"{modName}.{modSub}" : modName;

        var execCtx = ctx as ExecutionContext ?? new ExecutionContext();
        if (StringModifiers.TryApply(val, fullModName, args, execCtx, out var strResult)) return strResult;
        if (ArrayModifiers.TryApply(val, fullModName, args, execCtx, (e, c) => null, out var arrResult)) return arrResult;
        if (MathModifiers.TryApply(val, fullModName, args, out var mathResult)) return mathResult;
        if (TypeModifiers.TryApply(val, fullModName, args, out var typeResult)) return typeResult;

        bool firstArgIsNegated = mod.Arguments.Count > 0 && mod.Arguments[0] is NegatedExpr;
        bool firstArgIsRelational = mod.Arguments.Count > 0 && mod.Arguments[0] is RelationalExpr;
        bool firstArgIsVar = mod.Arguments.Count > 0 && mod.Arguments[0] is VariableExpr;
        bool isReservedConditional = modName == "test" || modName == "do";

        var condExt = ctx.GetExtension($"conditional:modifier.{fullModName}");
        string? condWildcardSubName = null;
        if (condExt == null && modSub != null)
        {
            condExt = ctx.GetExtension($"conditional:modifier.{modName}.*");
            if (condExt != null) condWildcardSubName = modSub;
        }

        var ext = ctx.GetExtension($"modifier.{fullModName}");
        string? wildcardSubName = null;
        if (ext == null && modSub != null)
        {
            ext = ctx.GetExtension($"modifier.{modName}.*");
            if (ext != null) wildcardSubName = modSub;
        }

        bool hasConditionSelectorArg = firstArgIsNegated || firstArgIsRelational ||
            (isReservedConditional && firstArgIsVar);

        if (condExt != null && hasConditionSelectorArg)
        {
            var descriptor = BuildConditionDescriptor(mod.Arguments[0], condWildcardSubName ?? modSub);
            var remainingArgs = new JsonArray();
            foreach (var a in args.Skip(1)) remainingArgs.Add(a?.DeepClone());
            return condExt(new JsonNode?[] { descriptor, remainingArgs });
        }

        if (isReservedConditional)
            return JsonValue.Create($"Unknown Modifier: {fullModName}");

        if (ext != null)
        {
            if (firstArgIsNegated || firstArgIsRelational)
                return JsonValue.Create($"Unknown Extension: {fullModName}");
            if (wildcardSubName != null)
                return ext(new[] { val, JsonValue.Create(wildcardSubName) }.Concat(args).ToArray());
            return ext(new[] { val }.Concat(args).ToArray());
        }

        return val;
    }

    private static JsonNode? ApplyFilter(JsonNode? val, IOperationContext ctx, ConditionCommand predicate)
    {
        var arr = RuntimeHelpers.ToArrayList(val);
        var result = new JsonArray();
        for (int i = 0; i < arr.Count; i++)
        {
            var item = arr[i];
            var scope = ctx.CreateChildScope();
            scope.SetVariable("$", item?.DeepClone());
            scope.SetVariable("it", item?.DeepClone());
            if (predicate.Evaluate(scope))
                result.Add(item?.DeepClone());
        }
        return result;
    }

    private static JsonNode? ApplyMap(JsonNode? val, IOperationContext ctx, IIslCommand projection)
    {
        var arr = RuntimeHelpers.ToArrayList(val);
        var result = new JsonArray();
        for (int i = 0; i < arr.Count; i++)
        {
            var item = arr[i];
            var scope = ctx.CreateChildScope();
            scope.SetVariable("$", item?.DeepClone());
            scope.SetVariable("it", item?.DeepClone());
            var mapped = projection.Execute(scope).Value;
            result.Add(mapped?.DeepClone());
        }
        return result;
    }

    private static JsonNode? ApplyGroupBy(
        JsonNode? val,
        List<Expr> rawArgs,
        JsonNode?[] evaluatedArgs,
        IOperationContext ctx,
        IIslCommand? perItemKeyExpr)
    {
        if (val is not JsonArray jaGrp) return val;

        Expr? keyExpr = rawArgs.Count > 0 ? rawArgs[0] : null;
        JsonNode? optsNode = evaluatedArgs.Length > 1 ? evaluatedArgs[1] : null;
        JsonObject? opts = optsNode as JsonObject;

        string outputMode = GetStrOpt(opts, "as") ?? "object";
        string keyAs = GetStrOpt(opts, "keyAs") ?? "key";
        string valuesAs = GetStrOpt(opts, "valuesAs") ?? "items";
        string nullKeyAs = GetStrOpt(opts, "nullKeyAs") ?? "null";
        string? emptyKeyAs = GetStrOpt(opts, "emptyKeyAs");

        bool isFieldName = keyExpr is LiteralExpr;
        string? fieldName = isFieldName && keyExpr is LiteralExpr le ? le.Value?.ToString() : null;

        var groups = new List<(string key, List<JsonNode?> items)>();
        var keyIndex = new Dictionary<string, int>();

        foreach (var item in jaGrp)
        {
            string rawKey = "null";
            if (keyExpr == null)
            {
                rawKey = RuntimeHelpers.JsonToString(item);
            }
            else if (isFieldName && fieldName != null)
            {
                if (item is JsonObject itemObj && itemObj.TryGetPropertyValue(fieldName, out var kv))
                    rawKey = kv == null || kv.ToJsonString() == "null" ? "null" : RuntimeHelpers.JsonToString(kv);
                else
                    rawKey = "null";
            }
            else if (perItemKeyExpr != null)
            {
                var itemScope = ctx.CreateChildScope();
                itemScope.SetVariable("$", item?.DeepClone());
                var keyVal = perItemKeyExpr.Execute(itemScope).Value;
                if (keyVal == null || keyVal.ToJsonString() == "null") rawKey = "null";
                else rawKey = RuntimeHelpers.JsonToString(keyVal);
            }

            string displayKey = rawKey;
            if (rawKey == "null") displayKey = nullKeyAs;
            else if (rawKey == "" && emptyKeyAs != null) displayKey = emptyKeyAs;

            if (!keyIndex.TryGetValue(displayKey, out int idx))
            {
                idx = groups.Count;
                keyIndex[displayKey] = idx;
                groups.Add((displayKey, new List<JsonNode?>()));
            }
            groups[idx].items.Add(item?.DeepClone());
        }

        if (outputMode == "array")
        {
            var result = new JsonArray();
            foreach (var (k, items) in groups)
            {
                var obj = new JsonObject { [keyAs] = JsonValue.Create(k) };
                var itemsArr = new JsonArray();
                foreach (var it in items) itemsArr.Add(it?.DeepClone());
                obj[valuesAs] = itemsArr;
                result.Add(obj);
            }
            return result;
        }
        else
        {
            var result = new JsonObject();
            foreach (var (k, items) in groups)
            {
                var arr = new JsonArray();
                foreach (var it in items) arr.Add(it?.DeepClone());
                result[k] = arr;
            }
            return result;
        }
    }

    private static string? GetStrOpt(JsonObject? obj, string key)
    {
        if (obj == null) return null;
        if (obj.TryGetPropertyValue(key, out var v) && v is JsonValue jv && jv.TryGetValue<string>(out var s)) return s;
        return null;
    }

    private static JsonObject BuildConditionDescriptor(Expr firstArg, string? subName)
    {
        var desc = new JsonObject();
        if (subName != null) desc["subName"] = JsonValue.Create(subName);

        if (firstArg is NegatedExpr ne)
            desc["expression"] = JsonValue.Create(BuildSelectorExpr("notexists", ne.Operand));
        else if (firstArg is VariableExpr ve)
            desc["expression"] = JsonValue.Create(BuildSelectorExpr("exists", firstArg));
        else if (firstArg is RelationalExpr re)
            desc["expression"] = JsonValue.Create(BuildSelectorExpr("exists", re.Left));
        else
            desc["expression"] = JsonValue.Create("exists [Select] ?");
        return desc;
    }

    private static string BuildSelectorExpr(string prefix, Expr expr)
    {
        if (expr is VariableExpr ve)
        {
            var sb = new System.Text.StringBuilder();
            sb.Append($"{prefix} [Select] ${ve.Name}");
            foreach (var part in ve.Parts)
            {
                if (part is PropertyPart pp) sb.Append($" -> {pp.Name}");
                else if (part is IndexPart ip) sb.Append($"[{ip.Index}]");
            }
            return sb.ToString();
        }
        return $"{prefix} [Select] ?";
    }
}
