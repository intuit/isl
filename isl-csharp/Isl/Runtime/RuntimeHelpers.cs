using System.Text.Json.Nodes;
using System.Text.RegularExpressions;

namespace Isl.Runtime;

/// <summary>
/// Pure static helpers shared by command implementations.
/// Mostly extracted from the original <see cref="Interpreter"/>; nothing here owns dispatch.
/// </summary>
public static class RuntimeHelpers
{
    public static double ToDouble(JsonNode? node)
    {
        if (node == null) return 0;
        if (node is JsonValue jv)
        {
            if (jv.TryGetValue<double>(out var d)) return d;
            if (jv.TryGetValue<string>(out var s) &&
                double.TryParse(s, System.Globalization.NumberStyles.Any,
                    System.Globalization.CultureInfo.InvariantCulture, out var pd)) return pd;
            if (jv.TryGetValue<bool>(out var b)) return b ? 1 : 0;
        }
        return 0;
    }

    public static List<JsonNode?> ToArrayList(JsonNode? node)
    {
        if (node is JsonArray ja) return ja.Select(x => x).ToList();
        if (node == null) return new List<JsonNode?>();
        return new List<JsonNode?> { node };
    }

    public static JsonNode? LiteralToJson(object? value) => value switch
    {
        null => null,
        bool b => JsonValue.Create(b),
        double d => JsonValue.Create(d),
        string s => JsonValue.Create(s),
        _ => JsonValue.Create(value?.ToString())
    };

    public static string JsonToString(JsonNode? node)
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

    public static bool IsTruthy(JsonNode? node)
    {
        if (node == null) return false;
        if (node is JsonValue jv)
        {
            if (jv.TryGetValue<bool>(out var b)) return b;
            if (jv.TryGetValue<string>(out var s)) return !string.IsNullOrEmpty(s);
            if (jv.TryGetValue<double>(out var d)) return d != 0;
            var str = jv.ToString();
            return str != "null" && !string.IsNullOrEmpty(str);
        }
        if (node is JsonArray ja) return ja.Count > 0;
        if (node is JsonObject jo) return jo.Count > 0;
        return true;
    }

    public static bool IsNullNode(JsonNode? node)
    {
        if (node == null) return true;
        if (node is JsonValue jv) return jv.ToJsonString() == "null";
        return false;
    }

    public static int CompareNodes(JsonNode? left, JsonNode? right)
    {
        if (left is JsonValue lv && right is JsonValue rv)
        {
            if (lv.TryGetValue<double>(out var ld) && rv.TryGetValue<double>(out var rd))
                return ld.CompareTo(rd);
            var ls = lv.TryGetValue<string>(out var lsv) ? lsv : lv.ToString();
            var rs = rv.TryGetValue<string>(out var rsv) ? rsv : rv.ToString();
            return string.Compare(ls, rs, StringComparison.Ordinal);
        }
        return string.Compare(JsonToString(left), JsonToString(right), StringComparison.Ordinal);
    }

    public static bool JsonEquals(JsonNode? a, JsonNode? b)
    {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.ToJsonString() == b.ToJsonString() || JsonToString(a) == JsonToString(b);
    }

    public static bool ArrayOrStringContains(JsonNode? left, JsonNode? right)
    {
        if (left is JsonArray ja)
        {
            var rightStr = JsonToString(right);
            foreach (var item in ja)
            {
                if (JsonToString(item) == rightStr) return true;
                if (item is JsonValue itemVal && right is JsonValue rightVal &&
                    itemVal.TryGetValue<double>(out var d1) && rightVal.TryGetValue<double>(out var d2) && d1 == d2)
                    return true;
            }
            return false;
        }
        return JsonToString(left).Contains(JsonToString(right));
    }

    public static bool RegexMatch(string input, string pattern)
    {
        if (pattern.Length > 1 && pattern.StartsWith("/") && pattern.EndsWith("/"))
            pattern = pattern.Substring(1, pattern.Length - 2);
        try { return Regex.IsMatch(input, pattern); }
        catch { return false; }
    }

    public static bool IsIn(JsonNode? val, JsonNode? arr)
    {
        if (arr is not JsonArray jarr) return false;
        return jarr.Any(item => JsonEquals(item, val));
    }

    public static bool CompareValues(JsonNode? left, string op, JsonNode? right)
    {
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
            "startsWith" => JsonToString(left).StartsWith(JsonToString(right)),
            "!startsWith" => !JsonToString(left).StartsWith(JsonToString(right)),
            "endsWith" => JsonToString(left).EndsWith(JsonToString(right)),
            "!endsWith" => !JsonToString(left).EndsWith(JsonToString(right)),
            "in" => IsIn(left, right),
            "!in" => !IsIn(left, right),
            "is" => JsonEquals(left, right),
            "!is" => !JsonEquals(left, right),
            "matches" => RegexMatch(JsonToString(left), JsonToString(right)),
            "!matches" => !RegexMatch(JsonToString(left), JsonToString(right)),
            _ => false
        };
    }

    public static void SetNestedProperty(JsonObject obj, IReadOnlyList<string> path, JsonNode? value)
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

    public static void MergeObjects(JsonObject target, JsonObject? source)
    {
        if (source == null) return;
        foreach (var kv in source)
            target[kv.Key] = kv.Value?.DeepClone();
    }
}
