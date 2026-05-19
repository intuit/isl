using System.Text.Json.Nodes;
using Isl.Runtime;

namespace Isl.Modifiers;

public static class DateModifiers
{
    public static JsonNode? Apply(JsonNode? val, string subName, JsonNode?[] args)
    {
        switch (subName.ToLower())
        {
            case "parse":
            {
                var str = NodeToString(val);
                // Try parsing the date
                if (DateTime.TryParse(str, null, System.Globalization.DateTimeStyles.None, out var dt))
                    return JsonValue.Create(dt.ToString("o")); // ISO 8601 for internal storage
                // Try with DateTimeOffset
                if (DateTimeOffset.TryParse(str, null, System.Globalization.DateTimeStyles.None, out var dto))
                    return JsonValue.Create(dto.DateTime.ToString("o"));
                return JsonValue.Create(str);
            }

            case "format":
            {
                var str = NodeToString(val);
                var fmt = args.Length > 0 ? NodeToString(args[0]) : "yyyy-MM-dd";
                var csFmt = TypeModifiers.ConvertJavaDateFormat(fmt);
                if (DateTime.TryParse(str, out var dt))
                    return JsonValue.Create(dt.ToString(csFmt));
                return JsonValue.Create(str);
            }

            case "now":
                return JsonValue.Create(DateTime.Now.ToString("o"));

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
        }
        return node.ToJsonString();
    }
}
