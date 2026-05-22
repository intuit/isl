using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

/// <summary>
/// Literal value: "string", 123, true/false/null. Pre-converts the AST literal into a JsonNode
/// at build time and returns it directly (Milestone 4 will cache the same instance to avoid
/// per-call allocation).
/// </summary>
public sealed class LiteralValueCommand : BaseCommand
{
    private readonly object? _rawValue;

    public LiteralValueCommand(LiteralExpr source) : base(source)
    {
        _rawValue = source.Value;
    }

    public object? RawValue => _rawValue;

    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(RuntimeHelpers.LiteralToJson(_rawValue));
}
