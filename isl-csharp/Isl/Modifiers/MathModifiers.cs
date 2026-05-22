using System.Text.Json.Nodes;
using Isl.Runtime;

namespace Isl.Modifiers;

public static class MathModifiers
{
    private static readonly HashSet<string> _knownNames = new(StringComparer.OrdinalIgnoreCase) { "precision" };

    public static bool TryApply(JsonNode? val, string name, JsonNode?[] args, out JsonNode? result)
    {
        if (!_knownNames.Contains(name)) { result = null; return false; }
        result = Apply(val, name, args);
        return true;
    }

    public static JsonNode? Apply(JsonNode? val, string name, JsonNode?[] args)
    {
        switch (name.ToLower())
        {
            case "precision":
            {
                int decimals = args.Length > 0 ? (int)Interpreter.ToDouble(args[0]) : 2;
                double d = Interpreter.ToDouble(val);
                return JsonValue.Create(Math.Round(d, decimals, MidpointRounding.AwayFromZero));
            }
            default:
                return null;
        }
    }

    public static JsonNode? ApplyNamed(JsonNode? val, string subName, JsonNode?[] args)
    {
        switch (subName.ToLower())
        {
            case "sum":
            {
                if (val is JsonArray ja)
                {
                    double init = args.Length > 0 ? Interpreter.ToDouble(args[0]) : 0;
                    return JsonValue.Create(ja.Aggregate(init, (acc, item) => acc + Interpreter.ToDouble(item)));
                }
                return val;
            }
            case "min":
            {
                if (val is JsonArray ja && ja.Count > 0)
                    return JsonValue.Create(ja.Min(item => Interpreter.ToDouble(item)));
                return val;
            }
            case "max":
            {
                if (val is JsonArray ja && ja.Count > 0)
                    return JsonValue.Create(ja.Max(item => Interpreter.ToDouble(item)));
                return val;
            }
            case "clamp":
            {
                double lo = args.Length > 0 ? Interpreter.ToDouble(args[0]) : double.MinValue;
                double hi = args.Length > 1 ? Interpreter.ToDouble(args[1]) : double.MaxValue;
                double v = Interpreter.ToDouble(val);
                return JsonValue.Create(Math.Clamp(v, lo, hi));
            }
            case "abs":
                return JsonValue.Create(Math.Abs(Interpreter.ToDouble(val)));
            case "ceil":
                return JsonValue.Create(Math.Ceiling(Interpreter.ToDouble(val)));
            case "floor":
                return JsonValue.Create(Math.Floor(Interpreter.ToDouble(val)));
            default:
                return null;
        }
    }
}
