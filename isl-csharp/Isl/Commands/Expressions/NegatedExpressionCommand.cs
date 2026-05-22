using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

public sealed class NegatedExpressionCommand : BaseCommand
{
    private readonly IIslCommand _operand;
    public NegatedExpressionCommand(NegatedExpr source, IIslCommand operand) : base(source) => _operand = operand;
    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(JsonValue.Create(!RuntimeHelpers.IsTruthy(_operand.Execute(ctx).Value)));
}

public sealed class RelationalExpressionCommand : BaseCommand
{
    private readonly IIslCommand _left;
    private readonly string _op;
    private readonly IIslCommand _right;

    public RelationalExpressionCommand(RelationalExpr source, IIslCommand left, IIslCommand right) : base(source)
    {
        _left = left;
        _op = source.Op;
        _right = right;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        var l = _left.Execute(ctx).Value;
        var r = _right.Execute(ctx).Value;
        return CommandResult.FromValue(JsonValue.Create(RuntimeHelpers.CompareValues(l, _op, r)));
    }
}

public sealed class MathExprWrapperCommand : BaseCommand
{
    private readonly MathExpressionCommand _inner;
    public MathExprWrapperCommand(MathExprWrapper source, MathExpressionCommand inner) : base(source) => _inner = inner;
    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(JsonValue.Create(_inner.EvalDouble(ctx)));
}
