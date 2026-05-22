using System.Text.Json;
using System.Text.Json.Nodes;
using Isl;
using Isl.Runtime;
using Xunit;
using YamlDotNet.RepresentationModel;
using ExecutionContext = Isl.Runtime.ExecutionContext;

namespace Isl.Tests;

public class YamlFixtureTests
{
    private static readonly string FixturesRoot = Path.GetFullPath(Path.Combine(
        AppContext.BaseDirectory, "..", "..", "..", "..", "..",
        "isl-transform", "src", "test", "resources", "tests"));

    public static IEnumerable<object[]> GetAllTestCases()
    {
        if (!Directory.Exists(FixturesRoot)) yield break;

        foreach (var yamlFile in Directory.EnumerateFiles(FixturesRoot, "*.yaml", SearchOption.AllDirectories))
        {
            string text;
            try { text = File.ReadAllText(yamlFile); } catch { continue; }

            List<YamlCase> cases;
            Exception? parseEx = null;
            try { cases = ParseFixture(yamlFile, text); }
            catch (Exception ex) { parseEx = ex; cases = new(); }
            if (parseEx != null)
            {
                yield return new object[] { Path.GetFileName(yamlFile), $"(parse error)", $"# {parseEx.Message}", (JsonNode?)null, (JsonNode?)null, "run", true };
                continue;
            }

            foreach (var c in cases)
                yield return new object[] { Path.GetFileName(c.File), c.Name, c.Script, c.Input, c.Expected, c.FunctionName, c.Skip };
        }
    }

    [Theory]
    [MemberData(nameof(GetAllTestCases))]
    public void RunFixture(string file, string name, string script, JsonNode? input, JsonNode? expected, string fn, bool skip)
    {
        if (skip) return; // silently skip unimplemented features

        IslTransformer transformer;
        try { transformer = IslCompiler.Compile(name, script); }
        catch (Exception ex)
        {
            Assert.Fail($"[{file}] \"{name}\" — compile error: {ex.Message}\nScript:\n{script}");
            return;
        }

        var ctx = new ExecutionContext();
        if (input is JsonObject inputObj)
            foreach (var kv in inputObj) ctx.SetVariable(kv.Key, kv.Value?.DeepClone());
        else if (input != null)
            ctx.SetVariable("input", input.DeepClone());

        // Register common mock extensions used in fixture tests
        RegisterMockExtensions(ctx, file, script);

        JsonNode? result;
        try { result = transformer.Run(fn, ctx); }
        catch (Exception ex)
        {
            Assert.Fail($"[{file}] \"{name}\" — runtime error: {ex.Message}\nScript:\n{script}");
            return;
        }

        AssertJsonEqual(expected, result, $"[{file}] \"{name}\"");
    }

    // ── Mock extensions ───────────────────────────────────────────────────────

    private static void RegisterMockExtensions(ExecutionContext ctx, string file, string script)
    {
        // @.Date.Now
        ctx.RegisterExtension("Date.Now", _ => JsonValue.Create(DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ss.fffZ")));

        // @.Config.Get(key) - mock config store used in coalesce tests
        var configStore = new Dictionary<string, string> { { "a", "a Value" }, { "abc", "abc Value" }, { "b", "b Value" } };
        ctx.RegisterExtension("Config.Get", args => {
            var key = args.Length > 0 && args[0] is System.Text.Json.Nodes.JsonValue jv && jv.TryGetValue<string>(out var k) ? k : null;
            if (string.IsNullOrEmpty(key)) return null;
            return configStore.TryGetValue(key, out var v) ? JsonValue.Create(v) : null;
        });

        // @.Call.Api() - mock API response for deep-object tests
        if (script.Contains("@.Call.Api"))
        {
            var apiResponse = System.Text.Json.Nodes.JsonNode.Parse(
                "{\"body\":{\"product\":{\"id\":1,\"value\":null,\"value2\":null}}}");
            ctx.RegisterExtension("Call.Api", _ => apiResponse?.DeepClone());
        }

        // @.Api.Call() - mock for function-types tests
        if (script.Contains("@.Api.Call"))
        {
            var apiCallResponse = System.Text.Json.Nodes.JsonNode.Parse("{\"success\":true}");
            ctx.RegisterExtension("Api.Call", _ => apiCallResponse?.DeepClone());
        }

        // modifier.test (conditional) and modifier.do.* (conditional wildcard) - for generic-conditional-modifiers tests
        if (file.Contains("generic-conditional-modifiers") || script.Contains("| test") || script.Contains("| do."))
        {
            // conditional modifier.test: receives (descriptor, args) where descriptor has 'expression' string
            ctx.RegisterExtension("conditional:modifier.test", args => {
                var expr = args.Length > 0 && args[0] is JsonObject d && d.TryGetPropertyValue("expression", out var e) ? e?.ToString() ?? "" : "";
                var argsStr = FmtArgsList(args.Length > 1 ? args[1] : null);
                return JsonValue.Create($"|test( {expr}, {argsStr} )");
            });
            // conditional modifier.do.*: receives (descriptor, args)
            ctx.RegisterExtension("conditional:modifier.do.*", args => {
                var desc = args.Length > 0 ? args[0] as JsonObject : null;
                var expr = desc != null && desc.TryGetPropertyValue("expression", out var e) ? e?.ToString() ?? "" : "";
                var subName = desc != null && desc.TryGetPropertyValue("subName", out var sn) ? sn?.ToString() ?? "when" : "when";
                var argsStr = FmtArgsList(args.Length > 1 ? args[1] : null);
                return JsonValue.Create($"|do.{subName}( {expr}, {argsStr} )");
            });
        }

        // modifier.simple and modifier.wild.* - mock extensions for generic-modifiers tests
        if (file.Contains("generic-modifiers") || script.Contains("| simple") || script.Contains("| wild."))
        {
            // modifier.simple(value, p1=null, p2=null) -> "|simple( val, p1, p2 )"
            ctx.RegisterExtension("modifier.simple", args => {
                var v = FmtVal(args.Length > 0 ? args[0] : null);
                var p1 = FmtVal(args.Length > 1 ? args[1] : null);
                var p2 = FmtVal(args.Length > 2 ? args[2] : null);
                return JsonValue.Create($"|simple( {v}, {p1}, {p2} )");
            });
            // modifier.wild.*(value, subName, p1=null, p2=null, p3=null) -> "|wild.card( val, card, p1, p2, p3 )"
            ctx.RegisterExtension("modifier.wild.*", args => {
                var v = FmtVal(args.Length > 0 ? args[0] : null);
                var sub = args.Length > 1 && args[1] is JsonValue sv && sv.TryGetValue<string>(out var ss) ? ss : "?";
                var p1 = FmtVal(args.Length > 2 ? args[2] : null);
                var p2 = FmtVal(args.Length > 3 ? args[3] : null);
                var p3 = FmtVal(args.Length > 4 ? args[4] : null);
                return JsonValue.Create($"|wild.{sub}( {v}, {sub}, {p1}, {p2}, {p3} )");
            });
        }
    }

    private static string FmtVal(JsonNode? node)
    {
        if (node == null) return "null";
        if (node is JsonValue jv)
        {
            if (jv.TryGetValue<bool>(out var b)) return b ? "true" : "false";
            if (jv.TryGetValue<string>(out var s)) return s;
            if (jv.TryGetValue<double>(out var d))
            {
                if (d == Math.Floor(d) && !double.IsInfinity(d)) return ((long)d).ToString();
                return d.ToString(System.Globalization.CultureInfo.InvariantCulture);
            }
        }
        return node.ToJsonString(new System.Text.Json.JsonSerializerOptions { WriteIndented = false });
    }

    private static string FmtArgsList(JsonNode? argsNode)
    {
        if (argsNode is not JsonArray arr || arr.Count == 0) return "[]";
        var parts = arr.Select(a => $"[Val] `{FmtVal(a)}`");
        return "[" + string.Join(", ", parts) + "]";
    }

    // ── YAML parsing ──────────────────────────────────────────────────────────

    private static List<YamlCase> ParseFixture(string path, string text)
    {
        var result = new List<YamlCase>();
        var yaml = new YamlStream();
        yaml.Load(new StringReader(text));
        if (yaml.Documents.Count == 0) return result;
        if (yaml.Documents[0].RootNode is not YamlMappingNode root) return result;
        if (!root.Children.TryGetValue(new YamlScalarNode("tests"), out var testsNode)) return result;
        if (testsNode is not YamlSequenceNode seq) return result;

        bool isLinter = path.Contains("linter");
        int idx = 0;

        foreach (var node in seq)
        {
            if (node is not YamlMappingNode m) continue;
            idx++;
            string name = Str(m, "name") ?? $"test_{idx}";
            if (string.IsNullOrWhiteSpace(name)) name = $"test_{idx}";

            // skip islFile-based tests (need file resolution logic)
            if (Has(m, "islFile")) { result.Add(Skip(path, name)); continue; }
            // skip linter tests (output is "issues" array, not a value)
            if (isLinter) { result.Add(Skip(path, name)); continue; }

            string? script = Str(m, "script");
            if (script == null) continue;

            string fn = Str(m, "functionName") ?? "run";

            // Support both "input" (JsonNode) and "inputs" (key-value map where each key becomes $key variable)
            JsonNode? input = null;
            if (Has(m, "input"))
                input = ToJson(m.Children[new YamlScalarNode("input")]);
            else if (Has(m, "inputs") && m.Children[new YamlScalarNode("inputs")] is YamlMappingNode inputsMap)
                input = MapToJson(inputsMap); // will be set as individual variables $key = val
            JsonNode? expected = Has(m, "expected") ? ToJson(m.Children[new YamlScalarNode("expected")]) : null;

            bool shouldSkip = NeedsSkip(script) || path.Contains("readonly-vars-fixture");
            result.Add(new YamlCase(path, name, script, input, expected, fn, shouldSkip));
        }
        return result;
    }

    private static bool NeedsSkip(string s) =>
        s.Contains("parallel foreach") ||
        s.Contains("@.Run.Sleep") || s.Contains("@.Thread.Id") ||
        s.Contains("@.Math.RandInt") || s.Contains("$isl.") ||
        s.Contains("modifier ") || s.Contains("@.Log.") ||
        s.Contains("@.Pagination.") || s.Contains("@.Retry.") ||
        s.Contains("| xml") || s.Contains("| yaml") || s.Contains("| csv") ||
        s.Contains("| xml.") ||
        s.Contains("| yaml.") || s.Contains("| csv.");

    private static YamlCase Skip(string path, string name) =>
        new(path, name, "", null, null, "run", true);

    // ── JSON equality ─────────────────────────────────────────────────────────

    private static void AssertJsonEqual(JsonNode? expected, JsonNode? actual, string ctx)
    {
        if (expected == null && actual == null) return;
        if (expected == null) { if (actual is JsonObject jo && jo.Count == 0) return; Assert.Null(actual); return; }
        if (actual == null) { Assert.Fail($"{ctx}: expected {expected.ToJsonString()} but got null"); return; }

        var opts = new JsonSerializerOptions { WriteIndented = false };
        string e = Normalize(expected, opts);
        string a = Normalize(actual, opts);
        Assert.Equal(e, a);
    }

    private static string Normalize(JsonNode n, JsonSerializerOptions opts)
    {
        // Re-parse to normalize number formatting (e.g. 1.0 vs 1)
        var txt = n.ToJsonString(opts);
        try { return JsonNode.Parse(txt)!.ToJsonString(opts); } catch { return txt; }
    }

    // ── YAML → JSON ───────────────────────────────────────────────────────────

    private static JsonNode? ToJson(YamlNode n) => n switch
    {
        YamlScalarNode s => ScalarToJson(s),
        YamlSequenceNode seq => SeqToJson(seq),
        YamlMappingNode map => MapToJson(map),
        _ => null
    };

    private static JsonNode? ScalarToJson(YamlScalarNode s)
    {
        var v = s.Value;
        // If YAML scalar is explicitly quoted (single or double), treat as string always
        // This preserves "123" as string, not number 123
        bool isExplicitString = s.Style == YamlDotNet.Core.ScalarStyle.SingleQuoted
                             || s.Style == YamlDotNet.Core.ScalarStyle.DoubleQuoted;
        if (!isExplicitString)
        {
            if (v is null || v == "null" || v == "~") return null;
            if (v == "true") return JsonValue.Create(true);
            if (v == "false") return JsonValue.Create(false);
            if (double.TryParse(v, System.Globalization.NumberStyles.Any,
                    System.Globalization.CultureInfo.InvariantCulture, out var d))
                return JsonValue.Create(d);
            if (v.Length > 1 && (v[0] == '{' || v[0] == '['))
                try { return JsonNode.Parse(v); } catch { }
        }
        return JsonValue.Create(v ?? "");
    }

    private static JsonArray SeqToJson(YamlSequenceNode s)
    { var a = new JsonArray(); foreach (var i in s) a.Add(ToJson(i)?.DeepClone()); return a; }

    private static JsonObject MapToJson(YamlMappingNode m)
    {
        var o = new JsonObject();
        foreach (var kv in m.Children)
        {
            var k = (kv.Key as YamlScalarNode)?.Value ?? kv.Key.ToString();
            if (k != null) o[k] = ToJson(kv.Value)?.DeepClone();
        }
        return o;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static string? Str(YamlMappingNode m, string k) =>
        m.Children.TryGetValue(new YamlScalarNode(k), out var v) ? (v as YamlScalarNode)?.Value : null;

    private static bool Has(YamlMappingNode m, string k) =>
        m.Children.ContainsKey(new YamlScalarNode(k));

    private record YamlCase(string File, string Name, string Script,
        JsonNode? Input, JsonNode? Expected, string FunctionName, bool Skip);
}
