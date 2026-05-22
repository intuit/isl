using System.Text.Json.Nodes;
using Isl.Runtime;

namespace Isl.Modifiers;

public static class TypeModifiers
{
    private static readonly HashSet<string> _knownNames = new(StringComparer.OrdinalIgnoreCase)
    {
        "string","tostr","str","number","tonumber","num","integer","tointeger","int",
        "decimal","todecimal","dec","boolean","toboolean","bool","typeof","xml"
    };

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

            case "xml":
            {
                // to.xml('rootName') - converts JSON object to XML string
                string rootName = args.Length > 0 && args[0] is JsonValue rv && rv.TryGetValue<string>(out var rn) ? rn : "root";
                if (val == null) return JsonValue.Create($"<{rootName}/>");
                var sb = new System.Text.StringBuilder();
                sb.Append('<').Append(rootName);
                SerializeXmlAttributes(sb, val as JsonObject);
                sb.Append('>');
                SerializeXmlContent(sb, val);
                sb.Append("</").Append(rootName).Append('>');
                return JsonValue.Create(sb.ToString());
            }

            default:
                return null;
        }
    }

    private static void SerializeXmlAttributes(System.Text.StringBuilder sb, JsonObject? obj)
    {
        if (obj == null) return;
        foreach (var kv in obj)
        {
            if (kv.Key.StartsWith("@"))
            {
                var attrName = kv.Key.Substring(1);
                var attrVal = NodeToString(kv.Value);
                sb.Append(' ').Append(attrName).Append("=\"").Append(XmlEscape(attrVal)).Append('"');
            }
        }
    }

    private static void SerializeXmlContent(System.Text.StringBuilder sb, JsonNode? val)
    {
        if (val is JsonObject obj)
        {
            // Check for #text
            string? textContent = null;
            if (obj.TryGetPropertyValue("#text", out var textNode) && textNode != null)
                textContent = NodeToString(textNode);

            if (textContent != null)
            {
                sb.Append(XmlEscape(textContent));
                return;
            }
            // Render child elements (skip @ attributes)
            foreach (var kv in obj)
            {
                if (kv.Key.StartsWith("@") || kv.Key == "#text") continue;
                RenderXmlElement(sb, kv.Key, kv.Value);
            }
        }
        else if (val != null)
        {
            sb.Append(XmlEscape(NodeToString(val)));
        }
    }

    private static void RenderXmlElement(System.Text.StringBuilder sb, string tagName, JsonNode? val)
    {
        if (val is JsonArray arr)
        {
            // Array: render each element with the same tag
            foreach (var item in arr)
                RenderXmlElement(sb, tagName, item);
            return;
        }
        sb.Append('<').Append(tagName);
        SerializeXmlAttributes(sb, val as JsonObject);
        sb.Append('>');
        SerializeXmlContent(sb, val);
        sb.Append("</").Append(tagName).Append('>');
    }

    private static string XmlEscape(string s) =>
        s.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;").Replace("\"", "&quot;");

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
