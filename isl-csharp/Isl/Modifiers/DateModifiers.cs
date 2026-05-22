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
                var javaFmt = args.Length > 0 ? NodeToString(args[0]) : null;

                if (javaFmt != null)
                {
                    // Handle Java-style timezone offset specifiers (x, X, XX, XXX, XXXX, XXXXX)
                    var dto = TryParseWithJavaFormat(str, javaFmt);
                    if (dto.HasValue)
                        return JsonValue.Create(dto.Value.UtcDateTime.ToString("yyyy-MM-dd'T'HH:mm:ss.fff'Z'"));
                }

                // Try parsing with standard parsers
                if (DateTimeOffset.TryParse(str, null, System.Globalization.DateTimeStyles.RoundtripKind, out var dto2))
                    return JsonValue.Create(dto2.UtcDateTime.ToString("yyyy-MM-dd'T'HH:mm:ss.fff'Z'"));
                if (DateTime.TryParse(str, null, System.Globalization.DateTimeStyles.None, out var dt))
                    return JsonValue.Create(dt.ToString("yyyy-MM-dd'T'HH:mm:ss.fff'Z'"));
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

    // Parse a date string using a Java-style format with timezone offset specifiers
    private static DateTimeOffset? TryParseWithJavaFormat(string str, string javaFmt)
    {
        // Detect the timezone pattern in the format (x/X repeated)
        int xCount = 0;
        int xStart = -1;
        for (int i = javaFmt.Length - 1; i >= 0; i--)
        {
            char c = javaFmt[i];
            if (c == 'x' || c == 'X')
            {
                xCount++;
                xStart = i;
            }
            else if (xCount > 0) break;
        }

        if (xCount == 0) return null;

        // Strip the x/X specifier part from the format to get the date-only format
        string datePart = javaFmt.Substring(0, xStart);

        if (string.IsNullOrEmpty(str)) return null;

        // Find where the timezone offset starts in the string (last +, -, or Z)
        int tzPos = -1;
        for (int i = str.Length - 1; i >= 0; i--)
        {
            char c = str[i];
            if (c == '+' || c == '-') { tzPos = i; break; }
            if (c == 'Z' || c == 'z') { tzPos = i; break; }
            if (char.IsDigit(c) || c == ':') continue;
            break;
        }

        if (tzPos < 0) return null;

        string dateStr = str.Substring(0, tzPos);
        string tzStr = str.Substring(tzPos);

        // Parse the timezone offset
        TimeSpan offset = TimeSpan.Zero;
        if (tzStr != "Z" && tzStr != "z")
        {
            int sign = tzStr[0] == '-' ? -1 : 1;
            string tzDigits = tzStr.Substring(1).Replace(":", "");
            int hours = tzDigits.Length >= 2 ? int.Parse(tzDigits.Substring(0, 2)) : 0;
            int minutes = tzDigits.Length >= 4 ? int.Parse(tzDigits.Substring(2, 2)) : 0;
            int seconds = tzDigits.Length >= 6 ? int.Parse(tzDigits.Substring(4, 2)) : 0;
            offset = TimeSpan.FromSeconds(sign * (hours * 3600 + minutes * 60 + seconds));
        }

        // Convert Java format to C# format (without timezone part)
        string csFmt = datePart
            .Replace("'T'", "\\T")
            .Replace("'", "");

        if (DateTime.TryParseExact(dateStr, csFmt, System.Globalization.CultureInfo.InvariantCulture,
            System.Globalization.DateTimeStyles.None, out var dt))
        {
            // DateTimeOffset requires whole-minute offsets; strip seconds from offset
            // and apply them directly to the DateTime instead
            int totalOffsetSeconds = (int)offset.TotalSeconds;
            int wholeMinuteSeconds = (totalOffsetSeconds / 60) * 60;
            int remainderSeconds = totalOffsetSeconds - wholeMinuteSeconds;
            var wholeMinuteOffset = TimeSpan.FromSeconds(wholeMinuteSeconds);
            var adjustedDt = dt.AddSeconds(-remainderSeconds); // absorb sub-minute part
            return new DateTimeOffset(adjustedDt, wholeMinuteOffset);
        }

        return null;
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
