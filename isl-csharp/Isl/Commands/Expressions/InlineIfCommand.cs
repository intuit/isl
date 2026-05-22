using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

/// <summary>
/// if (cond) value else other  — as an expression.
/// </summary>
public sealed class InlineIfCommand : BaseCommand
{
    private readonly ConditionCommand _condition;
    private readonly IIslCommand _then;
    private readonly IIslCommand? _else;
    public bool HasElse => _else != null;

    public InlineIfCommand(InlineIfExpr source, ConditionCommand condition, IIslCommand then, IIslCommand? @else)
        : base(source)
    {
        _condition = condition;
        _then = then;
        _else = @else;
    }

    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(EvaluateValue(ctx));

    public override JsonNode? EvaluateValue(IOperationContext ctx)
    {
        if (_condition.Evaluate(ctx))
            return _then.EvaluateValue(ctx);
        return _else?.EvaluateValue(ctx);
    }
}
