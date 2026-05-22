using System.Text.Json.Nodes;

namespace Isl.Runtime;

public interface ITransformer
{
    string Name { get; }
    JsonNode? RunTransformSync(string functionName, IOperationContext ctx);
    Task<JsonNode?> RunTransformAsync(string functionName, IOperationContext ctx);
}

/// <summary>
/// Runs precompiled commands. Mirrors the Kotlin <c>Transformer</c>: reusable instance,
/// stateless across calls except for the underlying <see cref="TransformModule"/>.
/// </summary>
public sealed class Transformer : ITransformer
{
    private readonly TransformModule _module;

    public Transformer(TransformModule module)
    {
        _module = module;
    }

    public string Name => _module.Name;

    public TransformModule Module => _module;

    public JsonNode? RunTransformSync(string functionName, IOperationContext ctx)
    {
        if (_module.Functions.Count > 0)
        {
            if (!_module.Functions.TryGetValue(functionName, out var fn))
                throw new IslRuntimeException($"Function '{functionName}' not found");

            return fn.Invoke(ctx, BindEntryArgs(fn, ctx));
        }

        if (_module.FlatStatements != null)
        {
            var result = _module.FlatStatements.Execute(ctx);
            return result.Value;
        }
        return null;
    }

    public Task<JsonNode?> RunTransformAsync(string functionName, IOperationContext ctx)
    {
        return Task.FromResult(RunTransformSync(functionName, ctx));
    }

    private static JsonNode?[] BindEntryArgs(Commands.Functions.FunctionDeclarationCommand fn, IOperationContext ctx)
    {
        if (fn.Parameters.Count == 0) return Array.Empty<JsonNode?>();
        var args = new JsonNode?[fn.Parameters.Count];
        for (int i = 0; i < fn.Parameters.Count; i++)
            args[i] = ctx.GetVariable(fn.Parameters[i]);
        return args;
    }
}
