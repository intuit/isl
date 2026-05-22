using System.Text.Json.Nodes;
using Isl;
using Isl.Runtime;
using Xunit;
using ExecutionContext = Isl.Runtime.ExecutionContext;

namespace Isl.Tests;

public class TransformTests
{
    private static readonly string RepoRoot = Path.GetFullPath(Path.Combine(
        AppContext.BaseDirectory, "..", "..", "..", "..", ".."));

    private static string ReadFile(string relativePath) =>
        File.ReadAllText(Path.Combine(RepoRoot, relativePath));

    private static JsonNode? ParseJson(string relativePath) =>
        JsonNode.Parse(ReadFile(relativePath));

    // ---- Test 1: Hello world ----
    [Fact]
    public void Hello_Run_ContainsHelloWorld()
    {
        var islSource = ReadFile("isl-cmd/examples/hello.isl");
        var transformer = IslCompiler.Compile("hello", islSource);

        var ctx = new ExecutionContext();
        var input = JsonNode.Parse("{\"name\": \"World\"}")!;
        ctx.SetVariable("input", input);
        // Register @.Date.Now extension
        ctx.RegisterExtension("Date.Now", _ => JsonValue.Create(DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss")));

        var result = transformer.Run("run", ctx);
        Assert.NotNull(result);

        var resultStr = result!.ToJsonString();
        Assert.Contains("Hello", resultStr);
        Assert.Contains("World", resultStr);
    }

    // ---- Test 2: Simple transform ----
    [Fact]
    public void SimpleTransform_MapsKeyFields()
    {
        var islSource = ReadFile("isl-transform/src/jmh/resources/simple-transform.isl");
        var transformer = IslCompiler.Compile("simple", islSource);

        var input = ParseJson("isl-transform/src/jmh/resources/simple-order.json")!;
        var ctx = new ExecutionContext();
        ctx.SetVariable("input", input);

        var result = transformer.Run("main", ctx);
        Assert.NotNull(result);

        var obj = result as JsonObject;
        Assert.NotNull(obj);
        Assert.True(obj!.ContainsKey("orderId") || obj.ContainsKey("amount") || obj.ContainsKey("customerName"),
            $"Expected orderId/amount/customerName in: {result!.ToJsonString()}");
    }

    // ---- Test 3: Shopify complex transform ----
    [Fact]
    public void ShopifyComplexTransform_HasRequiredFields()
    {
        var islSource = ReadFile("isl-transform/src/jmh/resources/shopify-transform-complex.isl");
        var transformer = IslCompiler.Compile("shopify-complex", islSource);

        var input = ParseJson("isl-transform/src/jmh/resources/shopify-order.json")!;
        var ctx = new ExecutionContext();
        ctx.SetVariable("input", input);
        ctx.RegisterExtension("Date.Now", _ => JsonValue.Create(DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss")));

        var result = transformer.Run("run", ctx);
        Assert.NotNull(result);

        var obj = result as JsonObject;
        Assert.NotNull(obj);
        var json = result!.ToJsonString();

        Assert.True(obj!.ContainsKey("orderId"), $"Missing orderId in: {json}");
        Assert.True(obj.ContainsKey("customer"), $"Missing customer in: {json}");
        Assert.True(obj.ContainsKey("items"), $"Missing items in: {json}");
        Assert.True(obj.ContainsKey("finalTotal"), $"Missing finalTotal in: {json}");
    }
}
