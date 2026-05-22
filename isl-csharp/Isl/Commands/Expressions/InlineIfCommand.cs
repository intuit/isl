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

    public override CommandResult Execute(IOperationContext ctx)
    {
        if (_condition.Evaluate(ctx))
            return CommandResult.FromValue(_then.Execute(ctx).Value);
        if (_else != null)
            return CommandResult.FromValue(_else.Execute(ctx).Value);
        return CommandResult.Null;
    }
}
