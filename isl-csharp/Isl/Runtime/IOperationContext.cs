using System.Text.Json.Nodes;

namespace Isl.Runtime;

/// <summary>
/// Per-execution state carried through every <see cref="Commands.IIslCommand.Execute"/> call.
/// Mirrors Kotlin's <c>IOperationContext</c>: variables, extension delegates, type annotations,
/// optional parent for nested scopes, optional execution hook.
/// </summary>
public interface IOperationContext
{
    JsonNode? GetVariable(string name);
    void SetVariable(string name, JsonNode? value);
    bool HasVariable(string name);

    Func<JsonNode?[], JsonNode?>? GetExtension(string name);
    void RegisterExtension(string name, Func<JsonNode?[], JsonNode?> fn);

    string? GetTypeAnnotation(string varName);
    void SetTypeAnnotation(string varName, string typeName);

    string? GetNodeType(JsonNode node);
    void SetNodeType(JsonNode node, string typeName);

    IExecutionHook? ExecutionHook { get; set; }

    IOperationContext CreateChildScope();
}
