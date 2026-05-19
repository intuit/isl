using System.Text.Json.Nodes;

namespace Isl.Runtime;

public class ExecutionContext
{
    private readonly Dictionary<string, JsonNode?> _variables = new();
    private readonly Dictionary<string, Func<JsonNode?[], JsonNode?>> _extensions = new();

    public void SetVariable(string name, JsonNode? value) => _variables[name] = value;
    public JsonNode? GetVariable(string name) => _variables.TryGetValue(name, out var v) ? v : null;
    public bool HasVariable(string name) => _variables.ContainsKey(name);

    public void RegisterExtension(string name, Func<JsonNode?[], JsonNode?> fn) => _extensions[name] = fn;
    public Func<JsonNode?[], JsonNode?>? GetExtension(string name) => _extensions.TryGetValue(name, out var f) ? f : null;

    public ExecutionContext CreateChildScope()
    {
        var child = new ExecutionContext();
        foreach (var kv in _variables) child._variables[kv.Key] = kv.Value;
        foreach (var kv in _extensions) child._extensions[kv.Key] = kv.Value;
        return child;
    }
}
