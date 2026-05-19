using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;
using ExecutionContext = Isl.Runtime.ExecutionContext;

namespace Isl.Modifiers;

public static class ArrayModifiers
{
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
