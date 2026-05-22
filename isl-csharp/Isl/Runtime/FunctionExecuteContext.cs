using System.Text.Json.Nodes;
using Isl.Commands;

namespace Isl.Runtime;

/// <summary>
/// Context passed when invoking a function or hardwired modifier.
/// Mirrors Kotlin's <c>FunctionExecuteContext</c>.
/// </summary>
public sealed class FunctionExecuteContext
{
    public string Name { get; }
    public IIslCommand? CallSite { get; }
    public IOperationContext OperationContext { get; }
    public JsonNode?[] Parameters { get; }

    public FunctionExecuteContext(string name, IIslCommand? callSite, IOperationContext ctx, JsonNode?[] parameters)
    {
        Name = name;
        CallSite = callSite;
        OperationContext = ctx;
        Parameters = parameters;
    }
}
