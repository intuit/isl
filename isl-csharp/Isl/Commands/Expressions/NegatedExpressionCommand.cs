using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

public sealed class NegatedExpressionCommand : BaseCommand
{
    private readonly IIslCommand _operand;
    public NegatedExpressionCommand(NegatedExpr source, IIslCommand operand) : base(source) => _operand = operand;
    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(EvaluateValue(ctx));
    public override JsonNode? EvaluateValue(IOperationContext ctx) =>
        JsonValue.Create(!RuntimeHelpers.IsTruthy(_operand.EvaluateValue(ctx)));
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

    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(EvaluateValue(ctx));

    public override JsonNode? EvaluateValue(IOperationContext ctx) =>
        JsonValue.Create(RuntimeHelpers.CompareValues(_left.EvaluateValue(ctx), _op, _right.EvaluateValue(ctx)));
}

public sealed class MathExprWrapperCommand : BaseCommand
{
    private readonly MathExpressionCommand _inner;
    public MathExprWrapperCommand(MathExprWrapper source, MathExpressionCommand inner) : base(source) => _inner = inner;
    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(JsonValue.Create(_inner.EvalDouble(ctx)));
    public override JsonNode? EvaluateValue(IOperationContext ctx) =>
        JsonValue.Create(_inner.EvalDouble(ctx));
}
