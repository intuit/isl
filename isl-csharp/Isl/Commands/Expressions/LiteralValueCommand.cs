using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

/// <summary>
/// Literal value: "string", 123, true/false/null. Pre-converts the AST literal into a JsonNode
/// at build time and returns the same cached instance on every Execute. All attach sites
/// (object/array entries, SetNestedProperty, MergeObjects) DeepClone before parenting, so
/// sharing the literal node is safe.
/// </summary>
public sealed class LiteralValueCommand : BaseCommand
{
    private readonly object? _rawValue;
    private readonly JsonNode? _cached;
    private readonly CommandResult _cachedResult;

    public LiteralValueCommand(LiteralExpr source) : base(source)
    {
        _rawValue = source.Value;
        _cached = RuntimeHelpers.LiteralToJson(_rawValue);
        _cachedResult = CommandResult.FromValue(_cached);
    }

    public object? RawValue => _rawValue;

    public JsonNode? CachedValue => _cached;

    public override CommandResult Execute(IOperationContext ctx) => _cachedResult;

    public override JsonNode? EvaluateValue(IOperationContext ctx) => _cached;
}
