using System.Runtime.CompilerServices;
using System.Text.Json.Nodes;

namespace Isl.Runtime;

public class ExecutionContext
{
    private readonly Dictionary<string, JsonNode?> _variables = new();
    private readonly Dictionary<string, Func<JsonNode?[], JsonNode?>> _extensions = new();
    // Type annotations: maps variable name to ISL type name (e.g. "MyType", "idx.banking.account.v3")
    private readonly Dictionary<string, string> _typeAnnotations = new();

    // Global node type store: maps JsonNode identity → type name for typed nested properties
    // Shared across all scopes via reference so child scopes see parent's type registrations
    private readonly ConditionalWeakTable<JsonNode, string> _nodeTypes;

    public ExecutionContext() : this(new ConditionalWeakTable<JsonNode, string>()) { }

    private ExecutionContext(ConditionalWeakTable<JsonNode, string> nodeTypes)
    {
        _nodeTypes = nodeTypes;
    }

    public void SetVariable(string name, JsonNode? value) => _variables[name] = value;
    public JsonNode? GetVariable(string name) => _variables.TryGetValue(name, out var v) ? v : null;
    public bool HasVariable(string name) => _variables.ContainsKey(name);

    public void SetTypeAnnotation(string varName, string typeName) => _typeAnnotations[varName] = typeName;
    public string? GetTypeAnnotation(string varName) => _typeAnnotations.TryGetValue(varName, out var t) ? t : null;

    // Store/retrieve type name for a specific JsonNode instance (for typed object properties)
    public void SetNodeType(JsonNode node, string typeName) => _nodeTypes.AddOrUpdate(node, typeName);
    public string? GetNodeType(JsonNode node) => _nodeTypes.TryGetValue(node, out var t) ? t : null;

    public void RegisterExtension(string name, Func<JsonNode?[], JsonNode?> fn) => _extensions[name] = fn;
    public Func<JsonNode?[], JsonNode?>? GetExtension(string name) => _extensions.TryGetValue(name, out var f) ? f : null;

    public ExecutionContext CreateChildScope()
    {
        var child = new ExecutionContext(_nodeTypes); // share node type table
        foreach (var kv in _variables) child._variables[kv.Key] = kv.Value;
        foreach (var kv in _extensions) child._extensions[kv.Key] = kv.Value;
        foreach (var kv in _typeAnnotations) child._typeAnnotations[kv.Key] = kv.Value;
        return child;
    }
}
