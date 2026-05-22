using System.Runtime.CompilerServices;
using System.Text.Json.Nodes;

namespace Isl.Runtime;

/// <summary>
/// Root execution context: variables, extensions, type annotations, optional execution hook.
/// Public API consumed by user code; also implements <see cref="IOperationContext"/> so the
/// runtime can flow it through commands. Runtime-internal child scopes go through
/// <see cref="IOperationContext.CreateChildScope"/> and produce a parent-chain
/// <see cref="ScopedOperationContext"/>; the public <see cref="CreateChildScope"/> still
/// performs a dict-copy for backwards compatibility with external callers.
/// </summary>
public class ExecutionContext : IOperationContext
{
    private readonly Dictionary<string, JsonNode?> _variables = new();
    private readonly Dictionary<string, Func<JsonNode?[], JsonNode?>> _extensions = new();
    private readonly Dictionary<string, string> _typeAnnotations = new();
    private readonly ConditionalWeakTable<JsonNode, string> _nodeTypes;

    public IExecutionHook? ExecutionHook { get; set; }

    public ExecutionContext() : this(new ConditionalWeakTable<JsonNode, string>()) { }

    private ExecutionContext(ConditionalWeakTable<JsonNode, string> nodeTypes)
    {
        _nodeTypes = nodeTypes;
    }

    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public void SetVariable(string name, JsonNode? value) => _variables[name] = value;

    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public JsonNode? GetVariable(string name) => _variables.TryGetValue(name, out var v) ? v : null;

    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public bool HasVariable(string name) => _variables.ContainsKey(name);

    public void SetTypeAnnotation(string varName, string typeName) => _typeAnnotations[varName] = typeName;
    public string? GetTypeAnnotation(string varName) => _typeAnnotations.TryGetValue(varName, out var t) ? t : null;

    public void SetNodeType(JsonNode node, string typeName) => _nodeTypes.AddOrUpdate(node, typeName);
    public string? GetNodeType(JsonNode node) => _nodeTypes.TryGetValue(node, out var t) ? t : null;

    public void RegisterExtension(string name, Func<JsonNode?[], JsonNode?> fn) => _extensions[name] = fn;
    public Func<JsonNode?[], JsonNode?>? GetExtension(string name) => _extensions.TryGetValue(name, out var f) ? f : null;

    /// <summary>
    /// Public child-scope factory. Kept on the dict-copy semantics for any external code that
    /// holds a concrete ExecutionContext reference.
    /// </summary>
    public ExecutionContext CreateChildScope()
    {
        var child = new ExecutionContext(_nodeTypes);
        foreach (var kv in _variables) child._variables[kv.Key] = kv.Value;
        foreach (var kv in _extensions) child._extensions[kv.Key] = kv.Value;
        foreach (var kv in _typeAnnotations) child._typeAnnotations[kv.Key] = kv.Value;
        return child;
    }

    /// <summary>
    /// Runtime-internal child scope: cheap parent-chain context, no dict copy.
    /// </summary>
    IOperationContext IOperationContext.CreateChildScope() => new ScopedOperationContext(this);
}
