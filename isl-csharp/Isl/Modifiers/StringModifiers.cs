using System.Text.Json.Nodes;
using System.Text.RegularExpressions;
using Isl.Runtime;
using ExecutionContext = Isl.Runtime.ExecutionContext;

namespace Isl.Modifiers;

public static class StringModifiers
{
    public static JsonNode? Apply(JsonNode? val, string name, JsonNode?[] args, ExecutionContext ctx)
    {
        var str = NodeToString(val);
        switch (name.ToLower())
        {
            case "trim":
                return JsonValue.Create(str.Trim());
            case "uppercase":
            case "toupper":
                return JsonValue.Create(str.ToUpper());
            case "lowercase":
            case "tolower":
                return JsonValue.Create(str.ToLower());
            case "capitalize":
                if (string.IsNullOrEmpty(str)) return JsonValue.Create(str);
                return JsonValue.Create(char.ToUpper(str[0]) + str.Substring(1).ToLower());
            case "titlecase":
                return JsonValue.Create(System.Globalization.CultureInfo.CurrentCulture.TextInfo.ToTitleCase(str.ToLower()));
            case "length":
                if (val is JsonArray ja) return JsonValue.Create((double)ja.Count);
                return JsonValue.Create((double)str.Length);
            case "padstart":
            {
                int n = args.Length > 0 ? (int)Interpreter.ToDouble(args[0]) : 0;
                char ch = args.Length > 1 ? NodeToString(args[1]).FirstOrDefault() : ' ';
                return JsonValue.Create(str.PadLeft(n, ch));
            }
            case "padend":
            {
                int n = args.Length > 0 ? (int)Interpreter.ToDouble(args[0]) : 0;
                char ch = args.Length > 1 ? NodeToString(args[1]).FirstOrDefault() : ' ';
                return JsonValue.Create(str.PadRight(n, ch));
            }
            case "split":
            {
                var sep = args.Length > 0 ? NodeToString(args[0]) : ",";
                var parts = str.Split(sep);
                var arr = new JsonArray();
                foreach (var p in parts) arr.Add(JsonValue.Create(p));
                return arr;
            }
            case "replace":
            {
                var a = args.Length > 0 ? NodeToString(args[0]) : "";
                var b = args.Length > 1 ? NodeToString(args[1]) : "";
                return JsonValue.Create(str.Replace(a, b));
            }
            case "startswith":
            {
                var s = args.Length > 0 ? NodeToString(args[0]) : "";
                return JsonValue.Create(str.StartsWith(s));
            }
            case "endswith":
            {
                var s = args.Length > 0 ? NodeToString(args[0]) : "";
                return JsonValue.Create(str.EndsWith(s));
            }
            case "contains":
            {
                var s = args.Length > 0 ? NodeToString(args[0]) : "";
                return JsonValue.Create(str.Contains(s));
            }
            case "matches":
            {
                var pattern = args.Length > 0 ? NodeToString(args[0]) : "";
                return JsonValue.Create(Regex.IsMatch(str, pattern));
            }
            case "truncate":
            {
                int maxLen = args.Length > 0 ? (int)Interpreter.ToDouble(args[0]) : 100;
                var ellipsis = args.Length > 1 ? NodeToString(args[1]) : "...";
                if (str.Length <= maxLen) return JsonValue.Create(str);
                return JsonValue.Create(str.Substring(0, maxLen - ellipsis.Length) + ellipsis);
            }
            case "substring":
            {
                int start = args.Length > 0 ? (int)Interpreter.ToDouble(args[0]) : 0;
                int len = args.Length > 1 ? (int)Interpreter.ToDouble(args[1]) : str.Length - start;
                return JsonValue.Create(str.Substring(Math.Max(0, start), Math.Min(len, str.Length - start)));
            }
            default:
                return null; // not handled
        }
    }

    private static string NodeToString(JsonNode? node)
    {
        if (node == null) return "";
        if (node is JsonValue jv)
        {
            if (jv.TryGetValue<string>(out var s)) return s;
            if (jv.TryGetValue<double>(out var d))
            {
                if (d == Math.Floor(d) && !double.IsInfinity(d)) return ((long)d).ToString();
                return d.ToString(System.Globalization.CultureInfo.InvariantCulture);
            }
            if (jv.TryGetValue<bool>(out var b)) return b.ToString().ToLower();
            return jv.ToString();
        }
        return node.ToJsonString();
    }
}
