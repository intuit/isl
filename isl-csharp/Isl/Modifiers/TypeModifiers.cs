using System.Text.Json.Nodes;
using Isl.Runtime;

namespace Isl.Modifiers;

public static class TypeModifiers
{
    public static JsonNode? Apply(JsonNode? val, string name, JsonNode?[] args)
    {
        switch (name.ToLower())
        {
            case "string":
            case "tostr":
            case "str":
            {
                if (args.Length > 0)
                {
                    // Format argument - used for DateTime.ToString(fmt)
                    // If val is a DateTime-like, format it
                    if (val is JsonValue jvd && jvd.TryGetValue<string>(out var dateStr))
                    {
                        var fmt = NodeToString(args[0]);
                        if (DateTime.TryParse(dateStr, null, System.Globalization.DateTimeStyles.None, out var dt))
                        {
                            var csFmt = ConvertJavaDateFormat(fmt);
                            return JsonValue.Create(dt.ToString(csFmt));
                        }
                    }
                }
                return JsonValue.Create(NodeToString(val));
            }

            case "number":
            case "tonumber":
            case "num":
                return JsonValue.Create(Interpreter.ToDouble(val));

            case "integer":
            case "tointeger":
            case "int":
                return JsonValue.Create((double)Math.Truncate(Interpreter.ToDouble(val)));

            case "decimal":
            case "todecimal":
            case "dec":
                return JsonValue.Create(Interpreter.ToDouble(val));

            case "boolean":
            case "toboolean":
            case "bool":
            {
                if (val is JsonValue jv)
                {
                    if (jv.TryGetValue<bool>(out var b)) return JsonValue.Create(b);
                    if (jv.TryGetValue<string>(out var s))
                    {
                        if (s.ToLower() == "true") return JsonValue.Create(true);
                        if (s.ToLower() == "false") return JsonValue.Create(false);
                        return JsonValue.Create(!string.IsNullOrEmpty(s));
                    }
                    if (jv.TryGetValue<double>(out var d)) return JsonValue.Create(d != 0);
                }
                return JsonValue.Create(val != null);
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
            if (jv.TryGetValue<double>(out var d))
            {
                if (d == Math.Floor(d) && !double.IsInfinity(d)) return ((long)d).ToString();
                return d.ToString(System.Globalization.CultureInfo.InvariantCulture);
            }
            if (jv.TryGetValue<bool>(out var b)) return b.ToString().ToLower();
        }
        return node.ToJsonString();
    }

    public static string ConvertJavaDateFormat(string javaFmt)
    {
        // Convert Java date format to C# format
        return javaFmt
            .Replace("yyyy", "yyyy")
            .Replace("MM", "MM")
            .Replace("dd", "dd")
            .Replace("HH", "HH")
            .Replace("mm", "mm")
            .Replace("ss", "ss")
            .Replace("XXX", "zzz")
            .Replace("'T'", "'T'") // literal T
            .Replace("'", ""); // Remove single-quote escaping
    }
}
