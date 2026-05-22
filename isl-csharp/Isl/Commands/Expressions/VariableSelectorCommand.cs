using System.Runtime.CompilerServices;
using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

/// <summary>
/// Variable + path selector ($var, $var.prop, $var[0], $var.path[(cond)]).
///
/// Milestone 3 splits this into three specialized subclasses chosen at compile time:
/// <list type="bullet">
///   <item><see cref="VarOnlySelectorCommand"/> — bare $var, no path.</item>
///   <item><see cref="SimplePathSelectorCommand"/> — only PropertyPart / IndexPart, no filters.</item>
///   <item><see cref="FilterPathSelectorCommand"/> — has at least one ConditionFilterPart.</item>
/// </list>
/// All three expose <see cref="ResolveValue"/> for callers that want a JsonNode? without
/// the <see cref="CommandResult"/> wrapping.
/// </summary>
public abstract class VariableSelectorCommand : BaseCommand
{
    protected readonly string _name;

    public string VariableName => _name;
    public abstract bool HasNoPath { get; }

    protected VariableSelectorCommand(VariableExpr source) : base(source)
    {
        _name = source.Name == "" ? "$" : source.Name;
    }

    public abstract JsonNode? ResolveValue(IOperationContext ctx);

    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(ResolveValue(ctx));

    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public override JsonNode? EvaluateValue(IOperationContext ctx) => ResolveValue(ctx);

    /// <summary>
    /// Factory: pick the cheapest selector tier the part list allows.
    /// </summary>
    public static VariableSelectorCommand Create(
        VariableExpr source,
        IReadOnlyList<ConditionCommand?> filterCommands)
    {
        if (source.Parts.Count == 0)
            return new VarOnlySelectorCommand(source);

        bool hasFilter = false;
        bool allProperty = true;
        for (int i = 0; i < source.Parts.Count; i++)
        {
            var p = source.Parts[i];
            if (p is ConditionFilterPart) { hasFilter = true; break; }
            if (p is not PropertyPart) allProperty = false;
        }
        if (!hasFilter)
        {
            // Fast path: $var.prop — single property hop, no array indexing
            if (allProperty && source.Parts.Count == 1)
                return new SinglePropertySelectorCommand(source);
            // Fast path: $var.prop1.prop2 — two property hops, very common
            if (allProperty && source.Parts.Count == 2)
                return new TwoPropertySelectorCommand(source);
            return new SimplePathSelectorCommand(source);
        }

        return new FilterPathSelectorCommand(source, filterCommands);
    }
}

/// <summary>
/// Tier 1: <c>$var</c> with no path. Just a single dictionary lookup.
/// </summary>
public sealed class VarOnlySelectorCommand : VariableSelectorCommand
{
    public override bool HasNoPath => true;

    public VarOnlySelectorCommand(VariableExpr source) : base(source) { }

    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public override JsonNode? ResolveValue(IOperationContext ctx) => ctx.GetVariable(_name);
}

/// <summary>
/// Tier 2a: <c>$var.prop</c> — exactly one property hop. Avoids the parts-array loop.
/// </summary>
public sealed class SinglePropertySelectorCommand : VariableSelectorCommand
{
    private readonly string _key;

    public override bool HasNoPath => false;

    public SinglePropertySelectorCommand(VariableExpr source) : base(source)
    {
        _key = ((PropertyPart)source.Parts[0]).Name;
    }

    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public override JsonNode? ResolveValue(IOperationContext ctx)
    {
        var current = ctx.GetVariable(_name);
        if (current is JsonObject jo && jo.TryGetPropertyValue(_key, out var v))
            return v;
        return null;
    }
}

/// <summary>
/// Tier 2b: <c>$var.prop1.prop2</c> — exactly two property hops.
/// </summary>
public sealed class TwoPropertySelectorCommand : VariableSelectorCommand
{
    private readonly string _key1;
    private readonly string _key2;

    public override bool HasNoPath => false;

    public TwoPropertySelectorCommand(VariableExpr source) : base(source)
    {
        _key1 = ((PropertyPart)source.Parts[0]).Name;
        _key2 = ((PropertyPart)source.Parts[1]).Name;
    }

    public override JsonNode? ResolveValue(IOperationContext ctx)
    {
        var current = ctx.GetVariable(_name);
        if (current is JsonObject jo && jo.TryGetPropertyValue(_key1, out var v1)
            && v1 is JsonObject jo2 && jo2.TryGetPropertyValue(_key2, out var v2))
            return v2;
        return null;
    }
}

/// <summary>
/// Tier 2c: <c>$var.foo[3].bar</c> — only Property and Index parts. Pre-flattened into a
/// (kind, name, index) tuple array so the runtime loop has no part-type dispatch.
/// </summary>
public sealed class SimplePathSelectorCommand : VariableSelectorCommand
{
    private const byte KIND_PROP = 0;
    private const byte KIND_INDEX = 1;

    private readonly byte[] _kinds;
    private readonly string?[] _names;
    private readonly int[] _indexes;

    public override bool HasNoPath => false;

    public SimplePathSelectorCommand(VariableExpr source) : base(source)
    {
        var parts = source.Parts;
        _kinds = new byte[parts.Count];
        _names = new string?[parts.Count];
        _indexes = new int[parts.Count];
        for (int i = 0; i < parts.Count; i++)
        {
            switch (parts[i])
            {
                case PropertyPart pp:
                    _kinds[i] = KIND_PROP;
                    _names[i] = pp.Name;
                    break;
                case IndexPart ip:
                    _kinds[i] = KIND_INDEX;
                    _indexes[i] = ip.Index;
                    break;
                default:
                    throw new InvalidOperationException("SimplePathSelectorCommand only supports PropertyPart/IndexPart");
            }
        }
    }

    public override JsonNode? ResolveValue(IOperationContext ctx)
    {
        var current = ctx.GetVariable(_name);
        if (current == null) return null;

        var kinds = _kinds;
        var names = _names;
        var indexes = _indexes;
        for (int i = 0; i < kinds.Length; i++)
        {
            if (current == null) return null;
            if (kinds[i] == KIND_PROP)
            {
                if (current is JsonObject jo)
                {
                    if (!jo.TryGetPropertyValue(names[i]!, out current)) return null;
                }
                else return null;
            }
            else
            {
                if (current is JsonArray ja)
                {
                    int idx = indexes[i];
                    if ((uint)idx < (uint)ja.Count) current = ja[idx];
                    else return null;
                }
                else return null;
            }
        }
        return current;
    }
}

/// <summary>
/// Tier 3: full path selector with at least one <c>[(cond)]</c> filter. Runs the original
/// general algorithm with per-item child-scope condition evaluation.
/// </summary>
public sealed class FilterPathSelectorCommand : VariableSelectorCommand
{
    private readonly IReadOnlyList<VariablePart> _parts;
    private readonly IReadOnlyList<ConditionCommand?> _filterCommands;

    public override bool HasNoPath => false;

    public FilterPathSelectorCommand(
        VariableExpr source,
        IReadOnlyList<ConditionCommand?> filterCommands) : base(source)
    {
        _parts = source.Parts;
        _filterCommands = filterCommands;
    }

    public override JsonNode? ResolveValue(IOperationContext ctx)
    {
        JsonNode? current = ctx.GetVariable(_name);
        if (current == null && _parts.Count == 0) return null;

        for (int i = 0; i < _parts.Count; i++)
        {
            if (current == null) return null;
            switch (_parts[i])
            {
                case PropertyPart pp:
                    if (current is JsonObject jo)
                        current = jo.TryGetPropertyValue(pp.Name, out var v) ? v : null;
                    else
                        return null;
                    break;
                case IndexPart ip:
                    if (current is JsonArray ja && ip.Index < ja.Count)
                        current = ja[ip.Index];
                    else
                        return null;
                    break;
                case ConditionFilterPart:
                    var filterCmd = _filterCommands[i];
                    if (filterCmd != null && current is JsonArray jarr)
                    {
                        var filtered = new JsonArray();
                        foreach (var item in jarr)
                        {
                            var sc = ctx.CreateChildScope();
                            var cloned = item?.DeepClone();
                            sc.SetVariable("$", cloned);
                            sc.SetVariable("it", cloned);
                            if (filterCmd.Evaluate(sc))
                                filtered.Add(item?.DeepClone());
                        }
                        current = filtered;
                    }
                    break;
            }
        }
        return current;
    }
}
