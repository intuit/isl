/*
 * ISL C# Performance Benchmark
 * Mirrors the Java JMH and Python timeit benchmark suites for apples-to-apples comparison.
 * Uses System.Diagnostics.Stopwatch for timing (no external dependencies).
 */

using System.Diagnostics;
using System.Text.Json.Nodes;
using Isl;
using Isl.Runtime;
using ExecutionContext = Isl.Runtime.ExecutionContext;

// ── Paths (same JMH resource files used by Java and Python benchmarks) ────────

// JMH resource files — shared across Java, Python, and C# benchmarks
// These live in the main repo, not in any worktree
var jmhRes = @"C:\Projects\ISL\gh\isl\isl-transform\src\jmh\resources";

var shopifyInput  = JsonNode.Parse(File.ReadAllText(Path.Combine(jmhRes, "shopify-order.json")))!;
var simpleInput   = JsonNode.Parse(File.ReadAllText(Path.Combine(jmhRes, "simple-order.json")))!;

var simpleTransformIsl  = File.ReadAllText(Path.Combine(jmhRes, "simple-transform.isl"));
var shopifySimpleIsl    = File.ReadAllText(Path.Combine(jmhRes, "shopify-transform-simple.isl"));
var shopifyComplexIsl   = File.ReadAllText(Path.Combine(jmhRes, "shopify-transform-complex.isl"));

// Inline "product" ISL — matches the Java JMH islSimpleTransformation benchmark
const string ProductIsl = """
    fun run( $input ){
       return {
            id: $input.id,
            name: $input.title | trim,
            short_description: `${ $input.title } by ${ $input.vendor }`,
            primary_image: {
                id: $input.images[0].id,
                url: $input.images[0].src,
            },
            is_active: if ( $input.status == 'active' ) true else false,
            option_name: $input.options.name,
            option_values: $input.options.values | join.string(),
            updated: $input.updated_at | date.fromEpochSeconds | to.string('YYYY-MM-DD HH:mm'),
            total: {{ $input.amount * 3 / $input.amount }},
        };
    }
    """;

var productInput = JsonNode.Parse("""
    {
        "title": "IPod Nano - 8GB",
        "body_html": "It's the small iPod with a big idea: Video.",
        "id": 632910392,
        "amount": 1235.678,
        "images": [{"id": 850703190, "src": "http://example.com/burton.jpg"}],
        "options": {"name": "Color", "values": ["Pink", "Red", "Green", "Black"]},
        "status": "active",
        "tags": "Emotive, Flash Memory, MP3, Music",
        "updated_at": 1645004735,
        "vendor": "Apple"
    }
    """)!;

// ── Benchmark definitions ─────────────────────────────────────────────────────

var benchmarks = new[]
{
    new BenchDef("islSimpleTransformation",   "Product transform (trim, interpolation, date, math)", ProductIsl,      productInput, "run",  2000),
    new BenchDef("islSimpleTransform (flat)", "Flat field mapping, no modifiers (simple-transform)", simpleTransformIsl, simpleInput, null,  2000),
    new BenchDef("shopifySimple",             "Shopify → internal (field mapping + array map)",       shopifySimpleIsl,   shopifyInput, "run", 1000),
    new BenchDef("shopifyComplex",            "Shopify → internal (functions, modifiers, date, math)",shopifyComplexIsl,  shopifyInput, "run",  500),
};

// ── Runner ────────────────────────────────────────────────────────────────────

static ExecutionContext MakeCtx(JsonNode input)
{
    var ctx = new ExecutionContext();
    ctx.SetVariable("input", input.DeepClone());
    return ctx;
}

static (double msPerOp, double opsPerSec) RunTransform(BenchDef b, IslTransformer t)
{
    // Warmup — 100 iterations not timed
    for (int i = 0; i < 100; i++)
        t.Run(b.Fn, MakeCtx(b.Input));

    var sw = Stopwatch.StartNew();
    for (int i = 0; i < b.Iters; i++)
        t.Run(b.Fn, MakeCtx(b.Input));
    sw.Stop();

    double msPerOp  = sw.Elapsed.TotalMilliseconds / b.Iters;
    double opsPerSec = b.Iters / sw.Elapsed.TotalSeconds;
    return (msPerOp, opsPerSec);
}

static (double msPerOp, double opsPerSec) RunFullCycle(BenchDef b)
{
    int iters = Math.Max(b.Iters / 5, 50);

    var sw = Stopwatch.StartNew();
    for (int i = 0; i < iters; i++)
    {
        var t = IslCompiler.Compile(b.Name, b.Script);
        t.Run(b.Fn, MakeCtx(b.Input));
    }
    sw.Stop();

    double msPerOp  = sw.Elapsed.TotalMilliseconds / iters;
    double opsPerSec = iters / sw.Elapsed.TotalSeconds;
    return (msPerOp, opsPerSec);
}

// ── Main ──────────────────────────────────────────────────────────────────────

Console.WriteLine(new string('=', 72));
Console.WriteLine("ISL C# Performance Benchmark");
Console.WriteLine(new string('=', 72));
Console.WriteLine();

var results = new List<(string name, double msOp, double opsS, double fcMs, double fcOps)>();

foreach (var b in benchmarks)
{
    Console.Write($"Compiling: {b.Name} ...");
    IslTransformer transformer;
    try
    {
        transformer = IslCompiler.Compile(b.Name, b.Script);
        Console.WriteLine(" OK");
    }
    catch (Exception ex)
    {
        Console.WriteLine($" ERROR: {ex.Message}");
        continue;
    }

    // Smoke test
    try
    {
        var r = transformer.Run(b.Fn, MakeCtx(b.Input));
        if (r == null) Console.WriteLine($"  WARNING: {b.Name} returned null");
    }
    catch (Exception ex)
    {
        Console.WriteLine($"  RUNTIME ERROR: {ex.Message}");
        continue;
    }

    Console.Write($"  transform ({b.Iters} iters) ...");
    var (msOp, opsS) = RunTransform(b, transformer);
    Console.WriteLine($" {msOp:F4} ms/op  ({opsS:N0} ops/sec)");

    Console.Write($"  full-cycle ({Math.Max(b.Iters / 5, 50)} iters) ...");
    var (fcMs, fcOps) = RunFullCycle(b);
    Console.WriteLine($" {fcMs:F4} ms/op  ({fcOps:N0} ops/sec)");

    results.Add((b.Name, msOp, opsS, fcMs, fcOps));
}

Console.WriteLine();
Console.WriteLine(new string('=', 72));
Console.WriteLine($"{"Benchmark",-30} {"ms/op",10} {"ops/sec",12} {"fc ms/op",10} {"fc ops/s",12}");
Console.WriteLine(new string('-', 72));
foreach (var (name, ms, ops, fcMs, fcOps) in results)
    Console.WriteLine($"{name,-30} {ms,10:F4} {ops,12:N0} {fcMs,10:F3} {fcOps,12:N0}");
Console.WriteLine(new string('=', 72));
Console.WriteLine();
Console.WriteLine("Notes:");
Console.WriteLine("  ms/op    = transform-only (pre-compiled, context setup + run)");
Console.WriteLine("  fc ms/op = full-cycle (compile + context setup + run)");

// ── Record type ───────────────────────────────────────────────────────────────

record BenchDef(string Name, string Description, string Script, JsonNode Input, string? Fn, int Iters);
