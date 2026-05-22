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
                    // Handle type annotations
                    if (av.TypeName != null)
                    {
                        ctx.SetTypeAnnotation(av.Name, av.TypeName);
                        // Also register type on the node itself for path-based lookups
                        if (avVal != null) ctx.SetNodeType(avVal, av.TypeName);
                        // Byref: if value is another variable, propagate type to source too
                        if (av.Value is VariableExpr avSrcExpr && avSrcExpr.Parts.Count == 0)
                        {
                            ctx.SetTypeAnnotation(avSrcExpr.Name, av.TypeName);
                            if (avVal != null) ctx.SetNodeType(avVal, av.TypeName);
                        }
                    }
                    else if (av.Value is VariableExpr avVarExpr && avVarExpr.Parts.Count == 0)
                    {
                        // Byref: $b = $a => transfer type annotation
                        var srcType = ctx.GetTypeAnnotation(avVarExpr.Name);
                        if (srcType != null) ctx.SetTypeAnnotation(av.Name, srcType);
                    }
                    else if (av.Value is FunctionCallExpr avFcExpr)
                    {
                        // Function call: check if the called function has a return type annotation
                        var retType = GetFunctionReturnType(avFcExpr);
                        if (retType != null) ctx.SetTypeAnnotation(av.Name, retType);
                    }
                    break;

                case AssignVarProperty avp:
                    var avpVal = EvalExpr(avp.Value, ctx);
                    var avpTarget = ctx.GetVariable(avp.VarName);
                    if (avpTarget is not JsonObject avpObj)
                    {
                        avpObj = new JsonObject();
                        ctx.SetVariable(avp.VarName, avpObj);
                    }
                    SetNestedProperty(avpObj, avp.PropPath, avpVal);
                    break;

                case AssignProperty ap:
                    var apVal = EvalExpr(ap.Value, ctx);
                    // Skip null property assignment when the value is from an else-less inline-if
                    // (condition was false and there's no else branch)
                    if (apVal == null && ap.Value is InlineIfExpr apIif && apIif.ElseExpr == null)
                        break;
                    SetNestedProperty(outputObject, ap.Path, apVal);
                    hasOutput = true;
                    break;

                case IfStatement ifs:
                    var ifResult = ExecuteIf(ifs, ctx);
                    if (ifResult != null)
                    {
                        if (ifResult is JsonObject ifObj)
                        {
                            // Only merge if the if-body had property assignments (not a return statement)
                            var executedBody = EvalCondition(ifs.Condition, ctx) ? ifs.TrueBody : ifs.FalseBody;
                            bool bodyHasAssignProp = executedBody.Any(s => s is AssignProperty);
                            if (bodyHasAssignProp)
                            {
                                MergeObjects(outputObject, ifObj);
                                hasOutput = true;
                            }
                            else
                            {
                                // Result came from return statement - bubble up
                                return ifResult;
                            }
                        }
                        else
                        {
                            // Non-object return value (e.g., number, string) - bubble up
                            return ifResult;
                        }
                    }
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
                    var swResult = ExecuteSwitch(sw, ctx);
                    if (swResult is JsonObject swObj)
                    {
                        MergeObjects(outputObject, swObj);
                        hasOutput = true;
                    }
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

            bool matches;
            // Check for regex literal pattern (e.g. /^v/ or /deposit/)
            if (op == "==" && pattern is JsonValue pjv && pjv.TryGetValue<string>(out var ps)
                && ps.Length > 2 && ps.StartsWith("/") && ps.EndsWith("/"))
            {
                var regexPat = ps.Substring(1, ps.Length - 2);
                var subjectStr = JsonToString(subject);
                try { matches = System.Text.RegularExpressions.Regex.IsMatch(subjectStr, regexPat, System.Text.RegularExpressions.RegexOptions.IgnoreCase); }
                catch { matches = false; }
            }
            else
            {
                matches = CompareValues(subject, op, pattern);
            }

            if (matches)
            {
                if (c.ResultExpr != null) return EvalExpr(c.ResultExpr, ctx);
                return ExecuteStatements(c.Body, ctx);
            }
        }
        if (sw.ElseResultExpr != null)
            return EvalExpr(sw.ElseResultExpr, ctx);
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
            SwitchExpr se => ExecuteSwitch(se.Switch, ctx),
            NegatedExpr ne => JsonValue.Create(!IsTruthy(EvalExpr(ne.Operand, ctx))),
            RelationalExpr re => JsonValue.Create(CompareValues(EvalExpr(re.Left, ctx), re.Op, EvalExpr(re.Right, ctx))),
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

        // Empty name with parts means $. path (e.g., $.address.city)
        var varName = ve.Name == "" ? "$" : ve.Name;
        JsonNode? current = ctx.GetVariable(varName);
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
                    // Skip null properties from else-less inline-if (condition was false, no else)
                    if (paVal == null && pa.Value is InlineIfExpr paIif && paIif.ElseExpr == null)
                        break;
                    SetNestedProperty(obj, pa.Path, paVal);
                    // Store type annotation on the stored node if typed property
                    if (pa.TypeName != null && pa.Path.Count == 1 && obj.TryGetPropertyValue(pa.Path[0], out var paStored) && paStored != null)
                        ctx.SetNodeType(paStored, pa.TypeName);
                    break;
                case TextPropAssign tpa:
                    var tpaVal = EvalExpr(tpa.Value, ctx);
                    // Skip null properties from else-less inline-if
                    if (tpaVal == null && tpa.Value is InlineIfExpr tpaIif && tpaIif.ElseExpr == null)
                        break;
                    obj[tpa.Key] = tpaVal?.DeepClone();
                    // Store type annotation on the stored node if typed property
                    if (tpa.TypeName != null && obj.TryGetPropertyValue(tpa.Key, out var tpaStored) && tpaStored != null)
                        ctx.SetNodeType(tpaStored, tpa.TypeName);
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

        // @.This.FuncName(args) or @.this.FuncName(args) -> call user-defined function
        if ((service == "This" || service == "this") && method != null)
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

    private string? GetFunctionReturnType(FunctionCallExpr fc)
    {
        // Only @.this.FuncName() or @.This.FuncName() calls can have a known return type
        if (fc.Service != "This" && fc.Service != "this") return null;
        if (fc.Method == null) return null;
        var funcName = fc.Method.Contains('.') ? fc.Method.Split('.')[0] : fc.Method;
        return _functions.TryGetValue(funcName, out var fn) ? fn.ReturnTypeName : null;
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
        // Coalesce treats null AND empty string as "no value"
        if (left != null && !(left is JsonValue lv && lv.TryGetValue<string>(out var s) && s == ""))
            return left;
        return EvalExpr(co.Right, ctx);
    }

    private JsonNode? EvalModified(ModifiedExpr me, ExecutionContext ctx)
    {
        var val = EvalExpr(me.Value, ctx);
        // Track source variable name for typeof support with named types
        string? sourceVarName = me.Value is VariableExpr typeofVe && typeofVe.Parts.Count == 0 ? typeofVe.Name : null;
        foreach (var mod in me.Modifiers)
        {
            // Special handling for typeof with named type annotations
            if (mod.Name.ToLower() == "typeof" && mod.SubName == null && mod.Condition == null)
            {
                // null value always returns "null" regardless of type annotation
                if (val == null)
                {
                    val = JsonValue.Create("null");
                }
                else
                {
                    // Check variable-level type annotation
                    string? namedType = sourceVarName != null ? ctx.GetTypeAnnotation(sourceVarName) : null;
                    // Check node-level type annotation (for typed object properties like child: TypeName = {...})
                    if (namedType == null)
                        namedType = ctx.GetNodeType(val);
                    if (namedType != null)
                        val = JsonValue.Create(namedType);
                    else
                        val = ApplyModifier(val, mod, ctx);
                }
            }
            else
            {
                val = ApplyModifier(val, mod, ctx);
            }
            sourceVarName = null; // only use for first modifier in chain
        }
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
        // Conditional modifier: | if(cond) modifierName
        // If condition is false, pass value through unchanged
        if (mod.Condition != null)
        {
            var scope = ctx.CreateChildScope();
            // $mval and $ in modifier conditions refer to the current piped value
            // Variables are stored WITHOUT the $ prefix (the $ is just syntax)
            scope.SetVariable("mval", val?.DeepClone());
            scope.SetVariable("$", val?.DeepClone()); // $ (bare) also refers to the current value
            if (!EvalCondition(mod.Condition, scope))
                return val;
        }

        var modName = mod.Name.ToLower();
        var modSub = mod.SubName?.ToLower();

        // Special case: group.by needs access to raw expressions for per-item path evaluation
        if (modName == "group" && modSub == "by")
        {
            return ApplyGroupBy(val, mod.Arguments, ctx);
        }

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

        if (modName == "json")
        {
            if (modSub == "parse")
            {
                // json.parse: parse a JSON string into an object/array/value
                if (val == null) return null;
                var jsonStr = JsonToString(val);
                if (string.IsNullOrWhiteSpace(jsonStr)) return null;
                try { return System.Text.Json.Nodes.JsonNode.Parse(jsonStr); }
                catch { return null; }
            }
            if (modSub != null)
            {
                // Unknown json sub-modifier
                return JsonValue.Create($"Unknown modifier: json.{modSub}");
            }
            // json (serialize to JSON string)
            return JsonModifiers.Apply(val, args);
        }

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

        // kv: convert object to [{key, value}] array
        if (modName == "kv" && modSub == null)
        {
            if (val is JsonObject kvObj)
            {
                var arr = new JsonArray();
                foreach (var pair in kvObj)
                {
                    var item = new JsonObject();
                    item["key"] = JsonValue.Create(pair.Key);
                    item["value"] = pair.Value?.DeepClone();
                    arr.Add(item);
                }
                return arr;
            }
            return val;
        }

        // round.up(decimals) / round.down(decimals) - format number to fixed decimal string
        if (modName == "round" && modSub != null)
        {
            int decimals = args.Length > 0 ? (int)ToDouble(args[0]) : 0;
            double d = ToDouble(val);
            double rounded = modSub switch
            {
                "up" => Math.Ceiling(d * Math.Pow(10, decimals)) / Math.Pow(10, decimals),
                "down" => Math.Floor(d * Math.Pow(10, decimals)) / Math.Pow(10, decimals),
                _ => Math.Round(d, decimals, MidpointRounding.AwayFromZero)
            };
            return JsonValue.Create(rounded.ToString("F" + decimals, System.Globalization.CultureInfo.InvariantCulture));
        }

        // Build full modifier name (e.g. "regex.find" from name="regex", sub="find")
        var fullModName = modSub != null ? $"{modName}.{modSub}" : modName;

        // Try built-in modifiers (use TryApply pattern to distinguish null-result from not-handled)
        if (StringModifiers.TryApply(val, fullModName, args, ctx, out var strResult)) return strResult;
        if (ArrayModifiers.TryApply(val, fullModName, args, ctx, (e, c) => EvalExpr(e, c), out var arrResult)) return arrResult;
        if (MathModifiers.TryApply(val, fullModName, args, out var mathResult)) return mathResult;
        if (TypeModifiers.TryApply(val, fullModName, args, out var typeResult)) return typeResult;

        // Detect condition-selector args:
        // - NegatedExpr (!$var) - explicit condition
        // - RelationalExpr ($var > 10) - explicit condition
        // - VariableExpr as first arg to a RESERVED conditional modifier (test, do.*)
        bool firstArgIsNegated = mod.Arguments.Count > 0 && mod.Arguments[0] is NegatedExpr;
        bool firstArgIsRelational = mod.Arguments.Count > 0 && mod.Arguments[0] is RelationalExpr;
        bool firstArgIsVar = mod.Arguments.Count > 0 && mod.Arguments[0] is VariableExpr;

        // Reserved conditional modifier names: "test" and "do.*"
        bool isReservedConditional = modName == "test" || modName == "do";

        // Look up exact conditional extension: conditional:modifier.test
        var condExt = ctx.GetExtension($"conditional:modifier.{fullModName}");
        // Try wildcard conditional extension: conditional:modifier.do.*
        string? condWildcardSubName = null;
        if (condExt == null && modSub != null)
        {
            condExt = ctx.GetExtension($"conditional:modifier.{modName}.*");
            if (condExt != null) condWildcardSubName = modSub;
        }

        // Look up regular extension: modifier.simple or modifier.wild.card
        var ext = ctx.GetExtension($"modifier.{fullModName}");
        string? wildcardSubName = null;
        if (ext == null && modSub != null)
        {
            ext = ctx.GetExtension($"modifier.{modName}.*");
            if (ext != null) wildcardSubName = modSub;
        }

        // --- Conditional modifier dispatch ---
        // If there's a conditional extension registered and first arg is a condition-selector
        bool hasConditionSelectorArg = firstArgIsNegated || firstArgIsRelational ||
            (isReservedConditional && firstArgIsVar);

        if (condExt != null && hasConditionSelectorArg)
        {
            // Build descriptor from the first arg expression
            var descriptor = BuildConditionDescriptor(mod.Arguments[0], condWildcardSubName ?? modSub, ctx);
            // Remaining args = everything AFTER the first (condition-selector) arg
            var remainingArgs = new JsonArray();
            foreach (var a in args.Skip(1)) remainingArgs.Add(a?.DeepClone());
            return condExt(new JsonNode?[] { descriptor, remainingArgs });
        }

        // Reserved conditional modifier with no conditional extension → "Unknown Modifier"
        if (isReservedConditional)
            return JsonValue.Create($"Unknown Modifier: {fullModName}");

        // --- Regular extension dispatch ---
        if (ext != null)
        {
            // Condition-selector args → "Unknown Extension"
            if (firstArgIsNegated || firstArgIsRelational)
                return JsonValue.Create($"Unknown Extension: {fullModName}");
            // Wildcard: inject sub-name as second arg
            if (wildcardSubName != null)
                return ext(new[] { val, JsonValue.Create(wildcardSubName) }.Concat(args).ToArray());
            return ext(new[] { val }.Concat(args).ToArray());
        }

        return val; // pass-through for unknown modifiers
    }

    /// <summary>Build a descriptor object describing a condition-selector modifier argument.</summary>
    private JsonObject BuildConditionDescriptor(Expr firstArg, string? subName, ExecutionContext ctx)
    {
        var desc = new JsonObject();
        if (subName != null) desc["subName"] = JsonValue.Create(subName);

        if (firstArg is NegatedExpr ne)
        {
            // !$result or !$result.value
            desc["expression"] = JsonValue.Create(BuildSelectorExpr("notexists", ne.Operand));
        }
        else if (firstArg is VariableExpr ve)
        {
            // $result or $result.value
            desc["expression"] = JsonValue.Create(BuildSelectorExpr("exists", firstArg));
        }
        else if (firstArg is RelationalExpr re)
        {
            // $result > 10 — for now just use "exists" form
            desc["expression"] = JsonValue.Create(BuildSelectorExpr("exists", re.Left));
        }
        else
        {
            desc["expression"] = JsonValue.Create("exists [Select] ?");
        }
        return desc;
    }

    private static string BuildSelectorExpr(string prefix, Expr expr)
    {
        if (expr is VariableExpr ve)
        {
            // Build path: $result -> value -> subprop
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

    private JsonNode? ApplyGroupBy(JsonNode? val, List<Expr> rawArgs, ExecutionContext ctx)
    {
        if (val is not JsonArray jaGrp) return val;

        // First arg: field path expression (can be $.path.to.field or "literal" field name)
        Expr? keyExpr = rawArgs.Count > 0 ? rawArgs[0] : null;
        // Second arg: options object
        JsonNode? optsNode = rawArgs.Count > 1 ? EvalExpr(rawArgs[1], ctx) : null;
        JsonObject? opts = optsNode as JsonObject;

        string outputMode = GetStrOpt(opts, "as") ?? "object";
        string keyAs = GetStrOpt(opts, "keyAs") ?? "key";
        string valuesAs = GetStrOpt(opts, "valuesAs") ?? "items";
        string nullKeyAs = GetStrOpt(opts, "nullKeyAs") ?? "null";
        string? emptyKeyAs = GetStrOpt(opts, "emptyKeyAs");

        // Determine the key extraction mode
        // If keyExpr is a literal string => use as field name
        // If keyExpr is a VariableExpr (like $.address.city) => evaluate per-item with $ = item
        bool isFieldName = keyExpr is LiteralExpr;
        string? fieldName = isFieldName && keyExpr is LiteralExpr le ? le.Value?.ToString() : null;
        bool isPerItemExpr = keyExpr is VariableExpr; // e.g. $.address.city

        // Group items preserving insertion order
        var groups = new List<(string key, List<JsonNode?> items)>();
        var keyIndex = new Dictionary<string, int>();

        foreach (var item in jaGrp)
        {
            string rawKey = "null";
            if (keyExpr == null)
            {
                rawKey = JsonToString(item);
            }
            else if (isFieldName && fieldName != null)
            {
                // Simple field name: get item.fieldName
                if (item is JsonObject itemObj && itemObj.TryGetPropertyValue(fieldName, out var kv))
                    rawKey = kv == null || kv.ToJsonString() == "null" ? "null" : JsonToString(kv);
                else
                    rawKey = "null";
            }
            else
            {
                // Variable expr or other: evaluate with item as $
                var itemScope = ctx.CreateChildScope();
                itemScope.SetVariable("$", item?.DeepClone());
                var keyVal = EvalExpr(keyExpr, itemScope);
                if (keyVal == null || keyVal.ToJsonString() == "null")
                    rawKey = "null";
                else
                    rawKey = JsonToString(keyVal);
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
                var obj = new JsonObject();
                obj[keyAs] = JsonValue.Create(k);
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
        // For non-equality operators, null operands mean false (SQL-style NULL semantics)
        if (op is not "==" and not "!=" and not "is" and not "!is")
        {
            if (IsNullNode(left) || IsNullNode(right)) return false;
        }

        return op switch
        {
            "==" => JsonEquals(left, right),
            "!=" => !JsonEquals(left, right),
            ">" => CompareNodes(left, right) > 0,
            "<" => CompareNodes(left, right) < 0,
            ">=" => CompareNodes(left, right) >= 0,
            "<=" => CompareNodes(left, right) <= 0,
            "contains" => ArrayOrStringContains(left, right),
            "!contains" => !ArrayOrStringContains(left, right),
            "startsWith" => JsonToString2(left).StartsWith(JsonToString2(right)),
            "!startsWith" => !JsonToString2(left).StartsWith(JsonToString2(right)),
            "endsWith" => JsonToString2(left).EndsWith(JsonToString2(right)),
            "!endsWith" => !JsonToString2(left).EndsWith(JsonToString2(right)),
            "in" => IsIn(left, right),
            "!in" => !IsIn(left, right),
            "is" => JsonEquals(left, right),
            "!is" => !JsonEquals(left, right),
            "matches" => RegexMatch(JsonToString2(left), JsonToString2(right)),
            "!matches" => !RegexMatch(JsonToString2(left), JsonToString2(right)),
            _ => false
        };
    }

    private bool ArrayOrStringContains(JsonNode? left, JsonNode? right)
    {
        // If left is an array, check if any element equals right (array membership)
        if (left is JsonArray ja)
        {
            var rightStr = JsonToString2(right);
            foreach (var item in ja)
            {
                if (JsonToString2(item) == rightStr)
                    return true;
                // Also check numeric equality
                if (item is JsonValue itemVal && right is JsonValue rightVal)
                {
                    if (itemVal.TryGetValue<double>(out var d1) && rightVal.TryGetValue<double>(out var d2) && d1 == d2)
                        return true;
                }
            }
            return false;
        }
        // For strings: substring check
        return JsonToString2(left).Contains(JsonToString2(right));
    }

    private static bool RegexMatch(string input, string pattern)
    {
        // Strip regex literal slashes: /pattern/ -> pattern
        if (pattern.Length > 1 && pattern.StartsWith("/") && pattern.EndsWith("/"))
            pattern = pattern.Substring(1, pattern.Length - 2);
        try { return System.Text.RegularExpressions.Regex.IsMatch(input, pattern); }
        catch { return false; }
    }

    private static bool IsNullNode(JsonNode? node)
    {
        if (node == null) return true;
        if (node is JsonValue jv)
        {
            var s = jv.ToJsonString();
            return s == "null";
        }
        return false;
    }

    private static int CompareNodes(JsonNode? left, JsonNode? right)
    {
        // Try numeric comparison first
        if (left is JsonValue lv && right is JsonValue rv)
        {
            if (lv.TryGetValue<double>(out var ld) && rv.TryGetValue<double>(out var rd))
                return ld.CompareTo(rd);
            // Fall back to string comparison
            var ls = lv.TryGetValue<string>(out var lsv) ? lsv : lv.ToString();
            var rs = rv.TryGetValue<string>(out var rsv) ? rsv : rv.ToString();
            return string.Compare(ls, rs, StringComparison.Ordinal);
        }
        // Fall back to string comparison
        return string.Compare(JsonToString2Static(left), JsonToString2Static(right), StringComparison.Ordinal);
    }

    private static string JsonToString2Static(JsonNode? node)
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
