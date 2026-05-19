namespace Isl.Modifiers;

public class ModifierRegistry
{
    private readonly Dictionary<string, Func<object?[], object?>> _modifiers = new();

    public static ModifierRegistry Default() => new ModifierRegistry();

    public void Register(string name, Func<object?[], object?> fn) => _modifiers[name] = fn;

    public Func<object?[], object?>? Get(string name) =>
        _modifiers.TryGetValue(name, out var f) ? f : null;
}
