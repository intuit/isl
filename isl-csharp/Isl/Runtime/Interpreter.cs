using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Modifiers;

namespace Isl.Runtime;

public class Interpreter
{
    private readonly Ast.Module _module;
    private readonly Dictionary<string, FunctionDecl> _functions;
    private readonly ModifierRegistry _modifiers;

    public Interpreter(Ast.Module module)
    {
        _module = module;
        _functions = module.Functions.ToDictionary(f => f.Name, f => f);
        _modifiers = ModifierRegistry.Default();
    }

    public JsonNode? RunFunction(string funcName, ExecutionContext ctx)
    {
        if (!_functions.TryGetValue(funcName, out var fn))
            throw new IslRuntimeException($"Function '{funcName}' not found");

        var scope = ctx.CreateChildScope();
        // Map parameters - parameters were set on the context by the caller
        // Parameters declared in function signature may have variables passed in
        return ExecuteStatements(fn.Body, scope);
    }

    public JsonNode? RunFlat(ExecutionContext ctx)
    {
        return ExecuteStatements(_module.Statements, ctx);
    }

    private JsonNode? ExecuteStatements(List<Statement> stmts, ExecutionContext ctx)
    {
        // If statements form a flat "output object" (property assignments)
        // versus a function with return statements
        var outputObject = new JsonObject();
        bool hasOutput = false;

        foreach (var stmt in stmts)
        {
            switch (stmt)
            {
                case ReturnStatement ret:
                    return EvalExpr(ret.Value, ctx);

                case AssignVariable av:
                    var avVal = EvalExpr(av.Value, ctx);
                    ctx.SetVariable(av.Name, avVal);
                    break;

                case AssignProperty ap:
                    var apVal = EvalExpr(ap.Value, ctx);
                    SetNestedProperty(outputObject, ap.Path, apVal);
                    hasOutput = true;
                    break;

                case IfStatement ifs:
                    ExecuteIf(ifs, ctx);
                    break;

                case ForEachStatement fe:
                    var feResult = ExecuteForEach(fe, ctx);
                    // ForEach as statement doesn't contribute to output directly
                    break;

                case WhileStatement ws:
                    ExecuteWhile(ws, ctx);
                    break;

                case FunctionCallStatement fc:
                    EvalFunctionCall(fc.Call, ctx);
                    break;

                case SwitchStatement sw:
                    ExecuteSwitch(sw, ctx);
                    break;
            }
        }

        if (hasOutput) return outputObject;
        return null;
    }

    private JsonNode? ExecuteIf(IfStatement ifs, ExecutionContext ctx)
    {
        bool condResult = EvalCondition(ifs.Condition, ctx);
        var body = condResult ? ifs.TrueBody : ifs.FalseBody;
        return ExecuteStatements(body, ctx);
    }

    private JsonNode? ExecuteForEach(ForEachStatement fe, ExecutionContext ctx)
    {
        var source = EvalExpr(fe.Source, ctx);
        var arr = ToArray(source);
        var results = new JsonArray();

        foreach (var item in arr)
        {
            var scope = ctx.CreateChildScope();
            scope.SetVariable(fe.Iterator, item?.DeepClone());
            scope.SetVariable("$", item?.DeepClone()); // implicit $

            if (fe.BodyObject != null)
            {
                var obj = EvalObjectExpr(fe.BodyObject, scope);
                results.Add(obj?.DeepClone());
            }
            else if (fe.Body.Count > 0)
            {
                var res = ExecuteStatements(fe.Body, scope);
                results.Add(res?.DeepClone());
            }
        }
        return results;
    }

    private void ExecuteWhile(WhileStatement ws, ExecutionContext ctx)
    {
        int iter = 0;
        while (EvalCondition(ws.Condition, ctx) && iter < ws.MaxLoops)
        {
            ExecuteStatements(ws.Body, ctx);
            iter++;
        }
    }

    private JsonNode? ExecuteSwitch(SwitchStatement sw, ExecutionContext ctx)
    {
        var subject = EvalExpr(sw.Subject, ctx);
        foreach (var c in sw.Cases)
        {
            if (c.Pattern == null) continue;
            var pattern = EvalExpr(c.Pattern, ctx);
            var op = c.Operator ?? "==";
            if (CompareValues(subject, op, pattern))
            {
                if (c.ResultExpr != null) return EvalExpr(c.ResultExpr, ctx);
                return ExecuteStatements(c.Body, ctx);
            }
        }
        if (sw.ElseBody != null)
            return ExecuteStatements(sw.ElseBody, ctx);
        return null;
    }

    public JsonNode? EvalExpr(Expr expr, ExecutionContext ctx)
    {
        return expr switch
        {
            LiteralExpr lit => LiteralToJson(lit.Value),
            VariableExpr ve => EvalVariable(ve, ctx),
            ArrayExpr ae => EvalArrayExpr(ae, ctx),
            ObjectExpr oe => EvalObjectExpr(oe, ctx),
            InterpolateExpr ie => EvalInterpolate(ie, ctx),
            MathExprWrapper mw => JsonValue.Create(EvalMath(mw.Inner, ctx)),
            InlineIfExpr iif => EvalInlineIf(iif, ctx),
            FunctionCallExpr fc => EvalFunctionCall(fc, ctx),
            CoalesceExpr co => EvalCoalesce(co, ctx),
            ModifiedExpr me => EvalModified(me, ctx),
            ForEachExpr fe => EvalForEachExpr(fe, ctx),
            _ => null
        };
    }

    private JsonNode? LiteralToJson(object? value)
    {
        return value switch
        {
            null => null,
            bool b => JsonValue.Create(b),
            double d => JsonValue.Create(d),
            string s => JsonValue.Create(s),
            _ => JsonValue.Create(value?.ToString())
        };
    }

    private JsonNode? EvalVariable(VariableExpr ve, ExecutionContext ctx)
    {
        // Special case: bare $ is the implicit iterator
        if (ve.Name == "$" && ve.Parts.Count == 0)
            return ctx.GetVariable("$");

        JsonNode? current = ctx.GetVariable(ve.Name);
        if (current == null && ve.Parts.Count == 0) return null;

        foreach (var part in ve.Parts)
        {
            if (current == null) return null;
            switch (part)
            {
                case PropertyPart pp:
                    if (current is JsonObject jo)
                        current = jo.TryGetPropertyValue(pp.Name, out var v) ? v : null;
                    else
                        return null;
                    break;
                case IndexPart ip:
                    if (current is JsonArray ja && ip.Index < ja.Count)
                        current = ja[ip.Index];
                    else
                        return null;
                    break;
                case ConditionFilterPart cfp:
                    if (current is JsonArray jarr)
                    {
                        var filtered = new JsonArray();
                        foreach (var item in jarr)
                        {
                            var sc = ctx.CreateChildScope();
                            sc.SetVariable("$", item?.DeepClone());
                            sc.SetVariable("it", item?.DeepClone());
                            if (EvalCondition(cfp.Cond, sc))
                                filtered.Add(item?.DeepClone());
                        }
                        current = filtered;
                    }
                    break;
            }
        }
        return current;
    }

    private JsonNode? EvalArrayExpr(ArrayExpr ae, ExecutionContext ctx)
    {
        var arr = new JsonArray();
        foreach (var elem in ae.Elements)
            arr.Add(EvalExpr(elem, ctx)?.DeepClone());
        return arr;
    }

    private JsonNode? EvalObjectExpr(ObjectExpr oe, ExecutionContext ctx)
    {
        var obj = new JsonObject();
        foreach (var prop in oe.Properties)
        {
            switch (prop)
            {
                case PropAssign pa:
                    var paVal = EvalExpr(pa.Value, ctx);
                    SetNestedProperty(obj, pa.Path, paVal);
                    break;
                case TextPropAssign tpa:
                    var tpaVal = EvalExpr(tpa.Value, ctx);
                    obj[tpa.Key] = tpaVal?.DeepClone();
                    break;
                case SpreadProp sp:
                    var spVal = EvalExpr(sp.Source, ctx);
                    if (spVal is JsonObject spObj)
                        foreach (var kv in spObj)
                            obj[kv.Key] = kv.Value?.DeepClone();
                    break;
                case VarPropAssign vpa:
                    var vpaVal = EvalExpr(vpa.Value, ctx);
                    ctx.SetVariable(vpa.Name, vpaVal);
                    break;
            }
        }
        return obj;
    }

    private JsonNode? EvalInterpolate(InterpolateExpr ie, ExecutionContext ctx)
    {
        var sb = new System.Text.StringBuilder();
        foreach (var part in ie.Parts)
        {
            switch (part)
            {
                case TextPart tp:
                    sb.Append(tp.Text);
                    break;
                case ExprPart ep:
                    var epVal = EvalExpr(ep.Inner, ctx);
                    sb.Append(JsonToString(epVal));
                    break;
                case MathPart mp:
                    var mathVal = EvalMath(mp.Inner, ctx);
                    sb.Append(mathVal.ToString(System.Globalization.CultureInfo.InvariantCulture));
                    break;
                case FuncCallPart fp:
                    var fcVal = EvalFunctionCall(fp.Call, ctx);
                    sb.Append(JsonToString(fcVal));
                    break;
            }
        }
        return JsonValue.Create(sb.ToString());
    }

    private string JsonToString(JsonNode? node)
    {
        if (node == null) return "";
        if (node is JsonValue jv)
        {
            if (jv.TryGetValue<string>(out var s)) return s;
            if (jv.TryGetValue<double>(out var d))
            {
                // Return integer representation if no fractional part
                if (d == Math.Floor(d) && !double.IsInfinity(d))
                    return ((long)d).ToString();
                return d.ToString(System.Globalization.CultureInfo.InvariantCulture);
            }
            if (jv.TryGetValue<bool>(out var b)) return b.ToString().ToLower();
            return jv.ToString();
        }
        return node.ToJsonString();
    }

    private JsonNode? EvalInlineIf(InlineIfExpr iif, ExecutionContext ctx)
    {
        bool result = EvalCondition(iif.Condition, ctx);
        if (result) return EvalExpr(iif.ThenExpr, ctx);
        return iif.ElseExpr != null ? EvalExpr(iif.ElseExpr, ctx) : null;
    }

    private JsonNode? EvalFunctionCall(FunctionCallExpr fc, ExecutionContext ctx)
    {
        var service = fc.Service;
        var method = fc.Method;

        // @.This.FuncName(args) -> call user-defined function
        if (service == "This" && method != null)
        {
            // method may be "FuncName" or "FuncName.SubName"
            var funcName = method.Contains('.') ? method.Split('.')[0] : method;
            if (_functions.TryGetValue(funcName, out var fn))
            {
                var childCtx = ctx.CreateChildScope();
                // Bind arguments to parameters
                for (int i = 0; i < fn.Parameters.Count && i < fc.Arguments.Count; i++)
                {
                    var argVal = EvalExpr(fc.Arguments[i], ctx);
                    childCtx.SetVariable(fn.Parameters[i], argVal);
                }
                return ExecuteStatements(fn.Body, childCtx);
            }
        }

        // @.Service.Method(args) -> registered extension or built-in
        var extKey = method != null ? $"{service}.{method}" : service;
        var ext = ctx.GetExtension(extKey) ?? ctx.GetExtension(service);
        if (ext != null)
        {
            var args = fc.Arguments.Select(a => EvalExpr(a, ctx)).ToArray();
            return ext(args);
        }

        // Built-in extensions
        if (service == "Date" && method == "Now")
            return JsonValue.Create(DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"));

        if (service == "Math")
        {
            var args = fc.Arguments.Select(a => EvalExpr(a, ctx)).ToArray();
            return ApplyMathExtension(method ?? "", args);
        }

        // Unknown - return null
        return null;
    }

    private JsonNode? ApplyMathExtension(string method, JsonNode?[] args)
    {
        return method switch
        {
            "abs" => args.Length > 0 ? JsonValue.Create(Math.Abs(ToDouble(args[0]))) : null,
            "ceil" => args.Length > 0 ? JsonValue.Create(Math.Ceiling(ToDouble(args[0]))) : null,
            "floor" => args.Length > 0 ? JsonValue.Create(Math.Floor(ToDouble(args[0]))) : null,
            _ => null
        };
    }

    private JsonNode? EvalCoalesce(CoalesceExpr co, ExecutionContext ctx)
    {
        var left = EvalExpr(co.Left, ctx);
        if (left != null) return left;
        return EvalExpr(co.Right, ctx);
    }

    private JsonNode? EvalModified(ModifiedExpr me, ExecutionContext ctx)
    {
        var val = EvalExpr(me.Value, ctx);
        foreach (var mod in me.Modifiers)
            val = ApplyModifier(val, mod, ctx);
        return val;
    }

    private JsonNode? EvalForEachExpr(ForEachExpr fe, ExecutionContext ctx)
    {
        var source = EvalExpr(fe.Source, ctx);
        var arr = ToArray(source);
        var results = new JsonArray();

        foreach (var item in arr)
        {
            var scope = ctx.CreateChildScope();
            scope.SetVariable(fe.Iterator, item?.DeepClone());
            scope.SetVariable("$", item?.DeepClone());

            if (fe.BodyObject != null)
            {
                var obj = EvalObjectExpr(fe.BodyObject, scope);
                results.Add(obj?.DeepClone());
            }
            else if (fe.Body.Count > 0)
            {
                var res = ExecuteStatements(fe.Body, scope);
                results.Add(res?.DeepClone());
            }
        }
        return results;
    }

    private JsonNode? ApplyModifier(JsonNode? val, ModifierNode mod, ExecutionContext ctx)
    {
        var modName = mod.Name.ToLower();
        var modSub = mod.SubName?.ToLower();
        var args = mod.Arguments.Select(a => EvalExpr(a, ctx)).ToArray();

        // Built-in named modifier sets
        if (modName == "to" && modSub != null)
        {
            return TypeModifiers.Apply(val, modSub, args);
        }

        if (modName == "date" && modSub != null)
        {
            return DateModifiers.Apply(val, modSub, args);
        }

        if (modName == "math" && modSub != null)
        {
            return MathModifiers.ApplyNamed(val, modSub, args);
        }

        if (modName == "json") return JsonModifiers.Apply(val, args);

        // Filter modifier: filter condition
        if (modName == "filter" && mod.Condition != null)
        {
            return ApplyFilterModifier(val, mod.Condition, ctx);
        }

        // Map modifier: map( expr )
        if (modName == "map" && mod.Arguments.Count > 0)
        {
            return ApplyMapModifier(val, mod.Arguments[0], ctx);
        }

        // Try built-in modifiers
        var result = StringModifiers.Apply(val, modName, args, ctx);
        if (result != null) return result;

        result = ArrayModifiers.Apply(val, modName, args, ctx, (e, c) => EvalExpr(e, c));
        if (result != null) return result;

        result = MathModifiers.Apply(val, modName, args);
        if (result != null) return result;

        result = TypeModifiers.Apply(val, modName, args);
        if (result != null) return result;

        // Check registered modifier extensions
        var ext = ctx.GetExtension($"modifier.{modName}");
        if (ext != null) return ext(new[] { val }.Concat(args).ToArray());

        return val; // pass-through for unknown modifiers
    }

    private JsonNode? ApplyFilterModifier(JsonNode? val, ConditionExpr cond, ExecutionContext ctx)
    {
        var arr = ToArray(val);
        var result = new JsonArray();
        foreach (var item in arr)
        {
            var scope = ctx.CreateChildScope();
            scope.SetVariable("$", item?.DeepClone());
            scope.SetVariable("it", item?.DeepClone());
            if (EvalCondition(cond, scope))
                result.Add(item?.DeepClone());
        }
        return result;
    }

    private JsonNode? ApplyMapModifier(JsonNode? val, Expr mapExpr, ExecutionContext ctx)
    {
        var arr = ToArray(val);
        var result = new JsonArray();
        foreach (var item in arr)
        {
            var scope = ctx.CreateChildScope();
            scope.SetVariable("$", item?.DeepClone());
            scope.SetVariable("it", item?.DeepClone());
            var mapped = EvalExpr(mapExpr, scope);
            result.Add(mapped?.DeepClone());
        }
        return result;
    }

    // ---- Condition evaluation ----
    public bool EvalCondition(ConditionExpr cond, ExecutionContext ctx)
    {
        return cond switch
        {
            SimpleCondition sc => EvalSimpleCondition(sc, ctx),
            BoolCondition bc => bc.LogOp == "and"
                ? EvalCondition(bc.Left, ctx) && EvalCondition(bc.Right, ctx)
                : EvalCondition(bc.Left, ctx) || EvalCondition(bc.Right, ctx),
            ParenCondition pc => EvalCondition(pc.Inner, ctx),
            NegatedCondition nc => !IsTruthy(EvalExpr(nc.Operand, ctx)),
            _ => false
        };
    }

    private bool EvalSimpleCondition(SimpleCondition sc, ExecutionContext ctx)
    {
        if (sc.Op == "truthy")
            return IsTruthy(EvalExpr(sc.Left, ctx));

        var left = EvalExpr(sc.Left, ctx);
        if (sc.Right == null) return IsTruthy(left);
        var right = EvalExpr(sc.Right, ctx);
        return CompareValues(left, sc.Op, right);
    }

    private bool CompareValues(JsonNode? left, string op, JsonNode? right)
    {
        return op switch
        {
            "==" => JsonEquals(left, right),
            "!=" => !JsonEquals(left, right),
            ">" => ToDouble(left) > ToDouble(right),
            "<" => ToDouble(left) < ToDouble(right),
            ">=" => ToDouble(left) >= ToDouble(right),
            "<=" => ToDouble(left) <= ToDouble(right),
            "contains" => JsonToString2(left).Contains(JsonToString2(right)),
            "!contains" => !JsonToString2(left).Contains(JsonToString2(right)),
            "startsWith" => JsonToString2(left).StartsWith(JsonToString2(right)),
            "!startsWith" => !JsonToString2(left).StartsWith(JsonToString2(right)),
            "endsWith" => JsonToString2(left).EndsWith(JsonToString2(right)),
            "!endsWith" => !JsonToString2(left).EndsWith(JsonToString2(right)),
            "in" => IsIn(left, right),
            "!in" => !IsIn(left, right),
            "is" => JsonEquals(left, right),
            "!is" => !JsonEquals(left, right),
            "matches" => System.Text.RegularExpressions.Regex.IsMatch(JsonToString2(left), JsonToString2(right)),
            "!matches" => !System.Text.RegularExpressions.Regex.IsMatch(JsonToString2(left), JsonToString2(right)),
            _ => false
        };
    }

    private bool IsIn(JsonNode? val, JsonNode? arr)
    {
        if (arr is not JsonArray jarr) return false;
        return jarr.Any(item => JsonEquals(item, val));
    }

    private bool JsonEquals(JsonNode? a, JsonNode? b)
    {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.ToJsonString() == b.ToJsonString()
            || JsonToString2(a) == JsonToString2(b);
    }

    private string JsonToString2(JsonNode? node)
    {
        if (node == null) return "";
        if (node is JsonValue jv)
        {
            if (jv.TryGetValue<string>(out var s)) return s;
            if (jv.TryGetValue<double>(out var d)) return d.ToString(System.Globalization.CultureInfo.InvariantCulture);
            if (jv.TryGetValue<bool>(out var b)) return b.ToString().ToLower();
            return jv.ToString();
        }
        return node.ToJsonString();
    }

    public bool IsTruthy(JsonNode? node)
    {
        if (node == null) return false;
        if (node is JsonValue jv)
        {
            if (jv.TryGetValue<bool>(out var b)) return b;
            if (jv.TryGetValue<string>(out var s)) return !string.IsNullOrEmpty(s);
            if (jv.TryGetValue<double>(out var d)) return d != 0;
            // Try null string
            var str = jv.ToString();
            return str != "null" && !string.IsNullOrEmpty(str);
        }
        if (node is JsonArray ja) return ja.Count > 0;
        if (node is JsonObject jo) return jo.Count > 0;
        return true;
    }

    // ---- Math evaluation ----
    public double EvalMath(MathExpr expr, ExecutionContext ctx)
    {
        return expr switch
        {
            MathNumber mn => mn.Value,
            MathBinOp mb => mb.Op switch
            {
                "+" => EvalMath(mb.Left, ctx) + EvalMath(mb.Right, ctx),
                "-" => EvalMath(mb.Left, ctx) - EvalMath(mb.Right, ctx),
                "*" => EvalMath(mb.Left, ctx) * EvalMath(mb.Right, ctx),
                "/" => EvalMath(mb.Left, ctx) / EvalMath(mb.Right, ctx),
                _ => 0
            },
            MathVariable mv => ToDouble(EvalVariable(mv.Variable, ctx)),
            MathFuncCall mfc => ToDouble(EvalFunctionCall(mfc.Call, ctx)),
            MathParen mp => EvalMath(mp.Inner, ctx),
            _ => 0
        };
    }

    // ---- Helpers ----
    public static double ToDouble(JsonNode? node)
    {
        if (node == null) return 0;
        if (node is JsonValue jv)
        {
            if (jv.TryGetValue<double>(out var d)) return d;
            if (jv.TryGetValue<string>(out var s) && double.TryParse(s, System.Globalization.NumberStyles.Any, System.Globalization.CultureInfo.InvariantCulture, out var pd)) return pd;
            if (jv.TryGetValue<bool>(out var b)) return b ? 1 : 0;
        }
        return 0;
    }

    private static List<JsonNode?> ToArray(JsonNode? node)
    {
        if (node is JsonArray ja) return ja.Select(x => x).ToList();
        if (node == null) return new List<JsonNode?>();
        return new List<JsonNode?> { node };
    }

    private static void SetNestedProperty(JsonObject obj, List<string> path, JsonNode? value)
    {
        if (path.Count == 0) return;
        if (path.Count == 1)
        {
            obj[path[0]] = value?.DeepClone();
            return;
        }
        if (!obj.TryGetPropertyValue(path[0], out var existing) || existing is not JsonObject nested)
        {
            nested = new JsonObject();
            obj[path[0]] = nested;
        }
        SetNestedProperty(nested, path.Skip(1).ToList(), value);
    }

    private static void MergeObjects(JsonObject target, JsonObject? source)
    {
        if (source == null) return;
        foreach (var kv in source)
            target[kv.Key] = kv.Value?.DeepClone();
    }
}

public class IslRuntimeException : Exception
{
    public IslRuntimeException(string msg) : base(msg) { }
}
