using System.Text.Json.Nodes;
using System.Text.RegularExpressions;
using Isl.Runtime;
using ExecutionContext = Isl.Runtime.ExecutionContext;
using static Isl.Runtime.RuntimeHelpers;

namespace Isl.Modifiers;

/// <summary>
/// Compile-time-resolved modifier dispatch table. Each entry is a typed delegate that
/// transforms <c>(val, args, ctx) -> JsonNode?</c>. Hot built-ins are registered as inline
/// delegates that avoid the legacy per-modifier-class string switch; rarer ones fall back
/// to the existing <c>*Modifiers.Apply</c> implementations.
/// </summary>
public sealed class ModifierRegistry
{
    public delegate JsonNode? ModifierDelegate(JsonNode? val, JsonNode?[] args, IOperationContext ctx);

    private readonly Dictionary<string, ModifierDelegate> _modifiers = new(StringComparer.Ordinal);

    /// <summary>Default registry pre-populated with all built-in modifiers (lowercased keys).</summary>
    public static ModifierRegistry Default()
    {
        var r = new ModifierRegistry();
        r.RegisterStringModifiers();
        r.RegisterArrayModifiers();
        r.RegisterMathModifiers();
        r.RegisterTypeModifiers();
        r.RegisterDateModifiers();
        r.RegisterMiscModifiers();
        return r;
    }

    public void Register(string lowercaseName, ModifierDelegate fn) => _modifiers[lowercaseName] = fn;

    public ModifierDelegate? Get(string lowercaseName) =>
        _modifiers.TryGetValue(lowercaseName, out var f) ? f : null;

    private static string AsString(JsonNode? node)
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

    private void RegisterStringModifiers()
    {
        // Hot string ops — registered inline to skip the per-call switch in StringModifiers.Apply.
        _modifiers["trim"] = (val, _, _) => JsonValue.Create(AsString(val).Trim());
        _modifiers["uppercase"] =
        _modifiers["toupper"] = (val, _, _) => JsonValue.Create(AsString(val).ToUpper());
        _modifiers["lowercase"] =
        _modifiers["tolower"] = (val, _, _) => JsonValue.Create(AsString(val).ToLower());
        _modifiers["capitalize"] = (val, _, _) =>
        {
            var s = AsString(val);
            if (string.IsNullOrEmpty(s)) return JsonValue.Create(s);
            return JsonValue.Create(char.ToUpper(s[0]) + s.Substring(1).ToLower());
        };
        _modifiers["titlecase"] = (val, _, _) =>
            JsonValue.Create(System.Globalization.CultureInfo.CurrentCulture.TextInfo.ToTitleCase(AsString(val).ToLower()));
        _modifiers["length"] = (val, _, _) =>
        {
            if (val is JsonArray ja) return JsonValue.Create((double)ja.Count);
            return JsonValue.Create((double)AsString(val).Length);
        };
        _modifiers["startswith"] = (val, args, _) =>
            JsonValue.Create(AsString(val).StartsWith(args.Length > 0 ? AsString(args[0]) : ""));
        _modifiers["endswith"] = (val, args, _) =>
            JsonValue.Create(AsString(val).EndsWith(args.Length > 0 ? AsString(args[0]) : ""));
        _modifiers["contains"] = (val, args, _) =>
            JsonValue.Create(AsString(val).Contains(args.Length > 0 ? AsString(args[0]) : ""));
        _modifiers["padstart"] = (val, args, _) =>
        {
            int n = args.Length > 0 ? (int)ToDouble(args[0]) : 0;
            char ch = args.Length > 1 ? AsString(args[1]).FirstOrDefault() : ' ';
            return JsonValue.Create(AsString(val).PadLeft(n, ch));
        };
        _modifiers["padend"] = (val, args, _) =>
        {
            int n = args.Length > 0 ? (int)ToDouble(args[0]) : 0;
            char ch = args.Length > 1 ? AsString(args[1]).FirstOrDefault() : ' ';
            return JsonValue.Create(AsString(val).PadRight(n, ch));
        };
        _modifiers["split"] = (val, args, _) =>
        {
            var sep = args.Length > 0 ? AsString(args[0]) : ",";
            var parts = AsString(val).Split(sep);
            var arr = new JsonArray();
            foreach (var p in parts) arr.Add(JsonValue.Create(p));
            return arr;
        };
        _modifiers["replace"] = (val, args, _) =>
        {
            var a = args.Length > 0 ? AsString(args[0]) : "";
            var b = args.Length > 1 ? AsString(args[1]) : "";
            return JsonValue.Create(AsString(val).Replace(a, b));
        };
        _modifiers["matches"] = (val, args, _) =>
        {
            var pat = args.Length > 0 ? AsString(args[0]) : "";
            if (pat.Length > 1 && pat.StartsWith("/") && pat.EndsWith("/")) pat = pat.Substring(1, pat.Length - 2);
            return JsonValue.Create(Regex.IsMatch(AsString(val), pat));
        };
        _modifiers["truncate"] = (val, args, _) =>
        {
            var s = AsString(val);
            int maxLen = args.Length > 0 ? (int)ToDouble(args[0]) : 100;
            var ellipsis = args.Length > 1 ? AsString(args[1]) : "...";
            if (s.Length <= maxLen) return JsonValue.Create(s);
            return JsonValue.Create(s.Substring(0, maxLen - ellipsis.Length) + ellipsis);
        };
        _modifiers["substring"] = (val, args, _) =>
        {
            var s = AsString(val);
            int start = args.Length > 0 ? (int)ToDouble(args[0]) : 0;
            int len = args.Length > 1 ? (int)ToDouble(args[1]) : s.Length - start;
            return JsonValue.Create(s.Substring(Math.Max(0, start), Math.Min(len, s.Length - start)));
        };
        _modifiers["concat"] =
        _modifiers["append"] = (val, args, _) =>
        {
            var sb = new System.Text.StringBuilder(AsString(val));
            foreach (var a in args) if (a != null) sb.Append(AsString(a));
            return JsonValue.Create(sb.ToString());
        };

        // Regex ops — keep going through StringModifiers (more involved code paths)
        var execCtxFactory = new Lazy<ExecutionContext>(() => new ExecutionContext());
        ExecutionContext Bridge(IOperationContext ctx) => ctx as ExecutionContext ?? execCtxFactory.Value;
        foreach (var name in new[] { "regex.find", "regex.replace", "regex.replacefirst", "regex.groups", "regex.matches" })
        {
            var key = name;
            _modifiers[key] = (val, args, ctx) => StringModifiers.Apply(val, key, args, Bridge(ctx));
        }
    }

    private void RegisterArrayModifiers()
    {
        _modifiers["first"] = (val, _, _) =>
            val is JsonArray ja && ja.Count > 0 ? ja[0]?.DeepClone() : val;
        _modifiers["last"] = (val, _, _) =>
            val is JsonArray ja && ja.Count > 0 ? ja[ja.Count - 1]?.DeepClone() : val;
        _modifiers["count"] = (val, _, _) =>
            val is JsonArray ja ? JsonValue.Create((double)ja.Count) : JsonValue.Create(0.0);
        _modifiers["reverse"] = (val, _, _) =>
        {
            if (val is not JsonArray ja) return val;
            var items = ja.ToList();
            items.Reverse();
            var r = new JsonArray();
            foreach (var i in items) r.Add(i?.DeepClone());
            return r;
        };
        _modifiers["unique"] = (val, _, _) =>
        {
            if (val is not JsonArray ja) return val;
            var seen = new HashSet<string>();
            var r = new JsonArray();
            foreach (var i in ja)
            {
                var key = i?.ToJsonString() ?? "null";
                if (seen.Add(key)) r.Add(i?.DeepClone());
            }
            return r;
        };
        _modifiers["sort"] = (val, _, _) =>
        {
            if (val is not JsonArray ja) return val;
            var items = ja.ToList();
            items.Sort((a, b) => string.Compare(AsString(a), AsString(b), StringComparison.Ordinal));
            var r = new JsonArray();
            foreach (var i in items) r.Add(i?.DeepClone());
            return r;
        };
        _modifiers["join"] = (val, args, _) =>
        {
            if (val is not JsonArray ja) return val;
            var sep = args.Length > 0 ? AsString(args[0]) : ",";
            return JsonValue.Create(string.Join(sep, ja.Select(AsString)));
        };
        _modifiers["join.string"] = (val, args, _) =>
        {
            if (val is not JsonArray ja) return val;
            var sep = args.Length > 0 ? AsString(args[0]) : ",";
            return JsonValue.Create(string.Join(sep, ja.Select(AsString)));
        };
        _modifiers["flatten"] = (val, _, _) =>
        {
            if (val is not JsonArray ja) return val;
            var r = new JsonArray();
            foreach (var i in ja)
            {
                if (i is JsonArray inner) foreach (var x in inner) r.Add(x?.DeepClone());
                else r.Add(i?.DeepClone());
            }
            return r;
        };
        _modifiers["push"] = (val, args, _) =>
        {
            var r = new JsonArray();
            if (val is JsonArray ja) foreach (var i in ja) r.Add(i?.DeepClone());
            foreach (var a in args) r.Add(a?.DeepClone());
            return r;
        };
        _modifiers["at"] = (val, args, _) =>
        {
            int idx = args.Length > 0 ? (int)ToDouble(args[0]) : 0;
            return val is JsonArray ja && idx >= 0 && idx < ja.Count ? ja[idx]?.DeepClone() : null;
        };
        _modifiers["skip"] = (val, args, _) =>
        {
            int n = args.Length > 0 ? (int)ToDouble(args[0]) : 0;
            if (val is not JsonArray ja) return val;
            var r = new JsonArray();
            foreach (var i in ja.Skip(n)) r.Add(i?.DeepClone());
            return r;
        };
        _modifiers["take"] = (val, args, _) =>
        {
            int n = args.Length > 0 ? (int)ToDouble(args[0]) : 0;
            if (val is not JsonArray ja) return val;
            var r = new JsonArray();
            foreach (var i in ja.Take(n)) r.Add(i?.DeepClone());
            return r;
        };
    }

    private void RegisterMathModifiers()
    {
        _modifiers["precision"] = (val, args, _) =>
        {
            int decimals = args.Length > 0 ? (int)ToDouble(args[0]) : 2;
            return JsonValue.Create(Math.Round(ToDouble(val), decimals, MidpointRounding.AwayFromZero));
        };
        _modifiers["math.sum"] =
        _modifiers["mathsum"] = (val, args, _) =>
        {
            if (val is not JsonArray ja) return val;
            double init = args.Length > 0 ? ToDouble(args[0]) : 0;
            return JsonValue.Create(ja.Aggregate(init, (acc, item) => acc + ToDouble(item)));
        };
        _modifiers["math.min"] =
        _modifiers["mathmin"] = (val, _, _) =>
            val is JsonArray ja && ja.Count > 0 ? JsonValue.Create(ja.Min(ToDouble))! : val;
        _modifiers["math.max"] =
        _modifiers["mathmax"] = (val, _, _) =>
            val is JsonArray ja && ja.Count > 0 ? JsonValue.Create(ja.Max(ToDouble))! : val;
        _modifiers["math.clamp"] =
        _modifiers["mathclamp"] = (val, args, _) =>
        {
            double lo = args.Length > 0 ? ToDouble(args[0]) : double.MinValue;
            double hi = args.Length > 1 ? ToDouble(args[1]) : double.MaxValue;
            return JsonValue.Create(Math.Clamp(ToDouble(val), lo, hi));
        };
        _modifiers["math.abs"] = (val, _, _) => JsonValue.Create(Math.Abs(ToDouble(val)));
        _modifiers["math.ceil"] = (val, _, _) => JsonValue.Create(Math.Ceiling(ToDouble(val)));
        _modifiers["math.floor"] = (val, _, _) => JsonValue.Create(Math.Floor(ToDouble(val)));
    }

    private void RegisterTypeModifiers()
    {
        ModifierDelegate toString = (val, args, _) =>
        {
            if (args.Length > 0 && val is JsonValue jvd && jvd.TryGetValue<string>(out var dateStr))
            {
                var fmt = AsString(args[0]);
                if (DateTime.TryParse(dateStr, null, System.Globalization.DateTimeStyles.None, out var dt))
                    return JsonValue.Create(dt.ToString(TypeModifiers.ConvertJavaDateFormat(fmt)));
            }
            return JsonValue.Create(AsString(val));
        };
        ModifierDelegate toNumber = (val, _, _) => JsonValue.Create(ToDouble(val));
        ModifierDelegate toInteger = (val, _, _) => JsonValue.Create((double)Math.Truncate(ToDouble(val)));
        ModifierDelegate toBoolean = (val, _, _) =>
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
        };
        ModifierDelegate typeofModifier = (val, _, _) =>
        {
            if (val == null) return JsonValue.Create("null");
            if (val is JsonValue jv)
            {
                if (jv.TryGetValue<bool>(out _)) return JsonValue.Create("boolean");
                if (jv.TryGetValue<double>(out _)) return JsonValue.Create("number");
                if (jv.TryGetValue<string>(out _)) return JsonValue.Create("string");
            }
            if (val is JsonArray) return JsonValue.Create("array");
            if (val is JsonObject) return JsonValue.Create("object");
            return JsonValue.Create("any");
        };

        // Bare names (e.g. `| string`)
        _modifiers["string"] = _modifiers["tostr"] = _modifiers["str"] = toString;
        _modifiers["number"] = _modifiers["tonumber"] = _modifiers["num"] =
            _modifiers["decimal"] = _modifiers["todecimal"] = _modifiers["dec"] = toNumber;
        _modifiers["integer"] = _modifiers["tointeger"] = _modifiers["int"] = toInteger;
        _modifiers["boolean"] = _modifiers["toboolean"] = _modifiers["bool"] = toBoolean;
        _modifiers["typeof"] = typeofModifier;

        // to.X form
        _modifiers["to.string"] = _modifiers["to.tostr"] = _modifiers["to.str"] = toString;
        _modifiers["to.number"] = _modifiers["to.tonumber"] = _modifiers["to.num"] =
            _modifiers["to.decimal"] = _modifiers["to.todecimal"] = _modifiers["to.dec"] = toNumber;
        _modifiers["to.integer"] = _modifiers["to.tointeger"] = _modifiers["to.int"] = toInteger;
        _modifiers["to.boolean"] = _modifiers["to.toboolean"] = _modifiers["to.bool"] = toBoolean;
        _modifiers["to.xml"] = (val, args, _) => TypeModifiers.Apply(val, "xml", args);
    }

    private void RegisterDateModifiers()
    {
        // Date ops are non-trivial; route via the existing static class.
        foreach (var sub in new[] { "parse", "format", "fromepochmillis", "fromepochseconds",
                                    "toepochmillis", "toepochseconds", "now", "add", "sub" })
        {
            var key = "date." + sub;
            var subName = sub;
            _modifiers[key] = (val, args, _) => DateModifiers.Apply(val, subName, args);
        }
    }

    private void RegisterMiscModifiers()
    {
        _modifiers["json"] = (val, args, _) => JsonModifiers.Apply(val, args);

        _modifiers["json.parse"] = (val, _, _) =>
        {
            if (val == null) return null;
            var jsonStr = AsString(val);
            if (string.IsNullOrWhiteSpace(jsonStr)) return null;
            try { return JsonNode.Parse(jsonStr); }
            catch { return null; }
        };

        _modifiers["kv"] = (val, _, _) =>
        {
            if (val is not JsonObject obj) return val;
            var arr = new JsonArray();
            foreach (var pair in obj)
            {
                arr.Add(new JsonObject
                {
                    ["key"] = JsonValue.Create(pair.Key),
                    ["value"] = pair.Value?.DeepClone()
                });
            }
            return arr;
        };

        foreach (var sub in new[] { "up", "down" })
        {
            var key = "round." + sub;
            var subName = sub;
            _modifiers[key] = (val, args, _) =>
            {
                int decimals = args.Length > 0 ? (int)ToDouble(args[0]) : 0;
                double d = ToDouble(val);
                double rounded = subName switch
                {
                    "up" => Math.Ceiling(d * Math.Pow(10, decimals)) / Math.Pow(10, decimals),
                    "down" => Math.Floor(d * Math.Pow(10, decimals)) / Math.Pow(10, decimals),
                    _ => Math.Round(d, decimals, MidpointRounding.AwayFromZero)
                };
                return JsonValue.Create(rounded.ToString("F" + decimals, System.Globalization.CultureInfo.InvariantCulture));
            };
        }
    }
}
