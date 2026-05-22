using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;
using ExecutionContext = Isl.Runtime.ExecutionContext;

namespace Isl.Modifiers;

public static class ArrayModifiers
{
    private static readonly HashSet<string> _knownNames = new(StringComparer.OrdinalIgnoreCase)
    {
        "length","first","last","unique","sort","join","reverse","flatten","push","at","skip","take","count",
        "math.sum","mathsum","math.min","mathmin","math.max","mathmax","math.clamp","mathclamp","group.by"
    };

    public static bool TryApply(JsonNode? val, string name, JsonNode?[] args, ExecutionContext ctx,
        Func<Expr, ExecutionContext, JsonNode?> evalExpr, out JsonNode? result)
    {
        if (!_knownNames.Contains(name)) { result = null; return false; }
        result = Apply(val, name, args, ctx, evalExpr);
        return true;
    }

    public static JsonNode? Apply(JsonNode? val, string name, JsonNode?[] args, ExecutionContext ctx,
        Func<Expr, ExecutionContext, JsonNode?> evalExpr)
    {
        switch (name.ToLower())
        {
            case "length":
                if (val is JsonArray jal) return JsonValue.Create((double)jal.Count);
                if (val is JsonValue jvs && jvs.TryGetValue<string>(out var str))
                    return JsonValue.Create((double)str.Length);
                return JsonValue.Create(0.0);

            case "first":
                if (val is JsonArray jaFirst && jaFirst.Count > 0)
                    return jaFirst[0]?.DeepClone();
                return val;

            case "last":
                if (val is JsonArray jaLast && jaLast.Count > 0)
                    return jaLast[jaLast.Count - 1]?.DeepClone();
                return val;

            case "unique":
                if (val is JsonArray jaUniq)
                {
                    var seen = new HashSet<string>();
                    var result = new JsonArray();
                    foreach (var item in jaUniq)
                    {
                        var key = item?.ToJsonString() ?? "null";
                        if (seen.Add(key)) result.Add(item?.DeepClone());
                    }
                    return result;
                }
                return val;

            case "sort":
                if (val is JsonArray jaSort)
                {
                    var items = jaSort.ToList();
                    items.Sort((a, b) => string.Compare(NodeToString(a), NodeToString(b), StringComparison.Ordinal));
                    var sorted = new JsonArray();
                    foreach (var item in items) sorted.Add(item?.DeepClone());
                    return sorted;
                }
                return val;

            case "join":
            {
                if (val is JsonArray jaJoin)
                {
                    var sep = args.Length > 0 ? NodeToString(args[0]) : ",";
                    var parts = jaJoin.Select(item => NodeToString(item));
                    return JsonValue.Create(string.Join(sep, parts));
                }
                return val;
            }

            case "math.sum":
            case "mathsum":
            {
                if (val is JsonArray jaSum)
                {
                    double init = args.Length > 0 ? Interpreter.ToDouble(args[0]) : 0;
                    return JsonValue.Create(jaSum.Aggregate(init, (acc, item) => acc + Interpreter.ToDouble(item)));
                }
                return val;
            }

            case "math.min":
            case "mathmin":
            {
                if (val is JsonArray jaMin && jaMin.Count > 0)
                    return JsonValue.Create(jaMin.Min(item => Interpreter.ToDouble(item)));
                return val;
            }

            case "math.max":
            case "mathmax":
            {
                if (val is JsonArray jaMax && jaMax.Count > 0)
                    return JsonValue.Create(jaMax.Max(item => Interpreter.ToDouble(item)));
                return val;
            }

            case "math.clamp":
            case "mathclamp":
            {
                double lo = args.Length > 0 ? Interpreter.ToDouble(args[0]) : double.MinValue;
                double hi = args.Length > 1 ? Interpreter.ToDouble(args[1]) : double.MaxValue;
                double v = Interpreter.ToDouble(val);
                return JsonValue.Create(Math.Clamp(v, lo, hi));
            }

            case "reverse":
                if (val is JsonArray jaRev)
                {
                    var items = jaRev.ToList();
                    items.Reverse();
                    var result = new JsonArray();
                    foreach (var item in items) result.Add(item?.DeepClone());
                    return result;
                }
                return val;

            case "flatten":
                if (val is JsonArray jaFlat)
                {
                    var result = new JsonArray();
                    foreach (var item in jaFlat)
                    {
                        if (item is JsonArray inner)
                            foreach (var x in inner) result.Add(x?.DeepClone());
                        else
                            result.Add(item?.DeepClone());
                    }
                    return result;
                }
                return val;

            case "push":
            {
                var lst = val is JsonArray jaP ? jaP.ToList() : new List<JsonNode?>();
                var result = new JsonArray();
                foreach (var i in lst) result.Add(i?.DeepClone());
                foreach (var a in args) result.Add(a?.DeepClone());
                return result;
            }

            case "at":
            {
                int idx = args.Length > 0 ? (int)Interpreter.ToDouble(args[0]) : 0;
                if (val is JsonArray jaAt && idx >= 0 && idx < jaAt.Count)
                    return jaAt[idx]?.DeepClone();
                return null;
            }

            case "skip":
            {
                int n = args.Length > 0 ? (int)Interpreter.ToDouble(args[0]) : 0;
                if (val is JsonArray jaSkip)
                {
                    var result = new JsonArray();
                    foreach (var item in jaSkip.Skip(n)) result.Add(item?.DeepClone());
                    return result;
                }
                return val;
            }

            case "take":
            {
                int n = args.Length > 0 ? (int)Interpreter.ToDouble(args[0]) : 0;
                if (val is JsonArray jaTake)
                {
                    var result = new JsonArray();
                    foreach (var item in jaTake.Take(n)) result.Add(item?.DeepClone());
                    return result;
                }
                return val;
            }

            case "count":
                if (val is JsonArray jaCnt) return JsonValue.Create((double)jaCnt.Count);
                return JsonValue.Create(0.0);

            case "group.by":
            {
                if (val is not JsonArray jaGrp) return val;

                // Parse field name from first arg
                JsonNode? fieldArg = args.Length > 0 ? args[0] : null;
                string? keyProp = fieldArg != null && fieldArg is not JsonObject ? NodeToString(fieldArg) : null;
                bool isJsonPath = keyProp != null && keyProp.StartsWith("$.");
                string[]? pathParts = isJsonPath ? keyProp!.Substring(2).Split('.') : null;

                // Parse options from second arg (or first if it's an object)
                JsonObject? opts = args.Length > 1 ? args[1] as JsonObject : null;
                string outputMode = opts != null && opts.TryGetPropertyValue("as", out var asV) ? NodeToString(asV) : "object";
                string keyAs = opts != null && opts.TryGetPropertyValue("keyAs", out var kaV) ? NodeToString(kaV) : "key";
                string valuesAs = opts != null && opts.TryGetPropertyValue("valuesAs", out var vaV) ? NodeToString(vaV) : "items";
                string nullKeyAs = opts != null && opts.TryGetPropertyValue("nullKeyAs", out var nkaV) ? NodeToString(nkaV) : "null";
                string? emptyKeyAs = opts != null && opts.TryGetPropertyValue("emptyKeyAs", out var ekaV) ? NodeToString(ekaV) : null;

                // Group items preserving insertion order
                var groups = new List<(string key, List<JsonNode?> items)>();
                var keyIndex = new Dictionary<string, int>();

                foreach (var item in jaGrp)
                {
                    string rawKey;
                    if (keyProp == null)
                    {
                        rawKey = NodeToString(item);
                    }
                    else if (pathParts != null)
                    {
                        // JSON path: $.address.city
                        JsonNode? cur = item;
                        foreach (var part in pathParts)
                            cur = cur is JsonObject jo && jo.TryGetPropertyValue(part, out var pv) ? pv : null;
                        rawKey = cur == null ? "null" : NodeToString(cur);
                    }
                    else
                    {
                        rawKey = (item is JsonObject itemObj && itemObj.TryGetPropertyValue(keyProp, out var kv))
                            ? (kv == null ? "null" : NodeToString(kv))
                            : "null";
                    }

                    // Apply key overrides
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

            case "typeof":
            {
                if (val == null) return JsonValue.Create("null");
                if (val is JsonValue jvt)
                {
                    if (jvt.TryGetValue<bool>(out _)) return JsonValue.Create("boolean");
                    if (jvt.TryGetValue<double>(out _)) return JsonValue.Create("number");
                    if (jvt.TryGetValue<string>(out _)) return JsonValue.Create("string");
                }
                if (val is JsonArray) return JsonValue.Create("array");
                if (val is JsonObject) return JsonValue.Create("object");
                return JsonValue.Create("any");
            }

            default:
                return null;
        }
    }

    private static string NodeToString(JsonNode? node)
    {
        if (node == null) return "";
        if (node is JsonValue jv)
        {
            if (jv.TryGetValue<string>(out var s)) return s;
            if (jv.TryGetValue<double>(out var d)) return d.ToString(System.Globalization.CultureInfo.InvariantCulture);
            if (jv.TryGetValue<bool>(out var b)) return b.ToString().ToLower();
        }
        return node.ToJsonString();
    }
}
