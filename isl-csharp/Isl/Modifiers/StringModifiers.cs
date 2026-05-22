using System.Text.Json.Nodes;
using System.Text.RegularExpressions;
using Isl.Runtime;
using ExecutionContext = Isl.Runtime.ExecutionContext;
using JsonObject = System.Text.Json.Nodes.JsonObject;

namespace Isl.Modifiers;

public static class StringModifiers
{
    // Known modifier names — used by TryApply to distinguish "not handled" from null result
    private static readonly HashSet<string> _knownNames = new(StringComparer.OrdinalIgnoreCase)
    {
        "trim","uppercase","toupper","lowercase","tolower","capitalize","titlecase","length",
        "padstart","padend","split","replace","startswith","endswith","contains","matches",
        "truncate","substring","concat","append","regex.find","regex.replace","regex.replacefirst",
        "regex.groups","regex.matches"
    };

    public static bool TryApply(JsonNode? val, string name, JsonNode?[] args, ExecutionContext ctx, out JsonNode? result)
    {
        if (!_knownNames.Contains(name)) { result = null; return false; }
        result = Apply(val, name, args, ctx);
        return true;
    }

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
                var pattern = args.Length > 0 ? ExtractRegex(NodeToString(args[0])) : "";
                return JsonValue.Create(Regex.IsMatch(str, pattern));
            }
            case "concat":
            case "append":
            {
                var sb = new System.Text.StringBuilder(str);
                foreach (var a in args) if (a != null) sb.Append(NodeToString(a));
                return JsonValue.Create(sb.ToString());
            }
            case "regex.find":
            {
                string pattern = args.Length > 0 ? ExtractRegexPattern(args[0]) : "";
                var opts = args.Length > 1 ? args[1] as JsonObject : (args.Length > 0 && args[0] is JsonObject ? args[0] as JsonObject : null);
                var regexOpts = BuildRegexOptions(opts);
                var arr = new JsonArray();
                try
                {
                    var regex = new Regex(pattern, regexOpts);
                    var matches = regex.Matches(str);
                    bool hasNamedGroups = regex.GetGroupNames().Any(n => !int.TryParse(n, out _));
                    bool hasCaptures = regex.GetGroupNumbers().Length > 1;
                    foreach (Match m in matches)
                    {
                        if (!m.Success) continue;
                        if (hasNamedGroups)
                        {
                            var obj = new JsonObject();
                            foreach (var gname in regex.GetGroupNames())
                            {
                                if (int.TryParse(gname, out _)) continue;
                                var g = m.Groups[gname];
                                if (g.Success) obj[gname] = JsonValue.Create(g.Value);
                            }
                            arr.Add(obj);
                        }
                        else if (hasCaptures)
                        {
                            for (int i = 1; i < m.Groups.Count; i++)
                                if (m.Groups[i].Success) arr.Add(JsonValue.Create(m.Groups[i].Value));
                        }
                        else
                        {
                            arr.Add(JsonValue.Create(m.Value));
                        }
                    }
                }
                catch { }
                return arr;
            }
            case "regex.replace":
            {
                string pattern = args.Length > 0 ? ExtractRegexPattern(args[0]) : "";
                var repl = args.Length > 1 ? NodeToString(args[1]) : "";
                var opts = args.Length > 2 ? args[2] as JsonObject : null;
                var regexOpts = BuildRegexOptions(opts);
                try { return JsonValue.Create(Regex.Replace(str, pattern, repl, regexOpts)); }
                catch { return JsonValue.Create(str); }
            }
            case "regex.replacefirst":
            {
                string pattern = args.Length > 0 ? ExtractRegexPattern(args[0]) : "";
                var repl = args.Length > 1 ? NodeToString(args[1]) : "";
                var opts = args.Length > 2 ? args[2] as JsonObject : null;
                var regexOpts = BuildRegexOptions(opts);
                bool replaced = false;
                try
                {
                    return JsonValue.Create(Regex.Replace(str, pattern, m => {
                        if (replaced) return m.Value;
                        replaced = true;
                        return repl;
                    }, regexOpts));
                }
                catch { return JsonValue.Create(str); }
            }
            case "regex.groups":
            {
                string pattern = args.Length > 0 ? ExtractRegexPattern(args[0]) : "";
                var opts = args.Length > 1 ? args[1] as JsonObject : null;
                var regexOpts = BuildRegexOptions(opts);
                var m2 = Regex.Match(str, pattern, regexOpts);
                var arr = new JsonArray();
                if (m2.Success)
                    for (int i = 1; i < m2.Groups.Count; i++)
                        arr.Add(JsonValue.Create(m2.Groups[i].Value));
                return arr;
            }
            case "regex.matches":
            {
                string pattern = args.Length > 0 ? ExtractRegexPattern(args[0]) : "";
                var opts = args.Length > 1 ? args[1] as JsonObject : null;
                var regexOpts = BuildRegexOptions(opts);
                try { return JsonValue.Create(Regex.IsMatch(str, pattern, regexOpts)); }
                catch { return JsonValue.Create(false); }
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

    private static string ExtractRegex(string pattern)
    {
        if (pattern.Length > 1 && pattern.StartsWith("/") && pattern.EndsWith("/"))
            return pattern.Substring(1, pattern.Length - 2);
        return pattern;
    }

    private static string ExtractRegexPattern(JsonNode? arg)
    {
        if (arg is JsonObject) return ""; // options object, not pattern
        return ExtractRegex(NodeToString(arg));
    }

    private static RegexOptions BuildRegexOptions(JsonObject? opts)
    {
        var o = RegexOptions.None;
        if (opts == null) return o;
        if (GetBoolOpt(opts, "multiLine")) o |= RegexOptions.Multiline;
        if (GetBoolOpt(opts, "ignoreCase")) o |= RegexOptions.IgnoreCase;
        if (GetBoolOpt(opts, "comments")) o |= RegexOptions.IgnorePatternWhitespace;
        return o;
    }

    private static bool GetBoolOpt(JsonObject obj, string key)
    {
        if (obj.TryGetPropertyValue(key, out var v) && v is JsonValue jv && jv.TryGetValue<bool>(out var b))
            return b;
        return false;
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
