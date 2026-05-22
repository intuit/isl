using System.Text.Json.Nodes;

namespace Isl.Runtime;

/// <summary>
/// Lightweight scoped operation context that defers to a parent for any name not set
/// locally. Replaces the previous full dict-copy <see cref="ExecutionContext.CreateChildScope"/>
/// for runtime-internal child scopes (function calls, foreach iterations, filter expressions).
/// <para>
/// Behaviour matches the legacy copy-then-mutate semantics:
///   - reads walk up the chain;
///   - writes never escape the local scope (parent values are shadowed, not overwritten);
///   - extensions and node-type registrations are routed to the root so they remain process-wide.
/// </para>
/// </summary>
public sealed class ScopedOperationContext : IOperationContext
{
    private readonly IOperationContext _parent;
    private Dictionary<string, JsonNode?>? _localVars;
    private Dictionary<string, string>? _localTypes;

    public IExecutionHook? ExecutionHook { get; set; }

    public ScopedOperationContext(IOperationContext parent)
    {
        _parent = parent;
        ExecutionHook = parent.ExecutionHook;
    }

    public JsonNode? GetVariable(string name)
    {
        if (_localVars != null && _localVars.TryGetValue(name, out var v)) return v;
        return _parent.GetVariable(name);
    }

    public void SetVariable(string name, JsonNode? value)
    {
        (_localVars ??= new())[name] = value;
    }

    public bool HasVariable(string name)
    {
        if (_localVars != null && _localVars.ContainsKey(name)) return true;
        return _parent.HasVariable(name);
    }

    public Func<JsonNode?[], JsonNode?>? GetExtension(string name) => _parent.GetExtension(name);

    public void RegisterExtension(string name, Func<JsonNode?[], JsonNode?> fn) =>
        _parent.RegisterExtension(name, fn);

    public string? GetTypeAnnotation(string varName)
    {
        if (_localTypes != null && _localTypes.TryGetValue(varName, out var t)) return t;
        return _parent.GetTypeAnnotation(varName);
    }

    public void SetTypeAnnotation(string varName, string typeName) =>
        (_localTypes ??= new())[varName] = typeName;

    public string? GetNodeType(JsonNode node) => _parent.GetNodeType(node);

    public void SetNodeType(JsonNode node, string typeName) => _parent.SetNodeType(node, typeName);

    public IOperationContext CreateChildScope() => new ScopedOperationContext(this);
}
