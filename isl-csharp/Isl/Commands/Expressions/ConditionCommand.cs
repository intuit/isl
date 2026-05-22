using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

/// <summary>
/// A compiled condition expression. Exposes <see cref="Evaluate"/> returning bool directly
/// to avoid round-tripping through <see cref="CommandResult"/> for every check.
/// Implementations are produced by <c>ExecutionBuilder</c> from <see cref="ConditionExpr"/> nodes.
/// </summary>
public abstract class ConditionCommand : BaseCommand
{
    protected ConditionCommand(IslNode source) : base(source) { }

    public abstract bool Evaluate(IOperationContext ctx);

    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(System.Text.Json.Nodes.JsonValue.Create(Evaluate(ctx)));
}

public sealed class SimpleConditionCommand : ConditionCommand
{
    private readonly IIslCommand _left;
    private readonly string _op;
    private readonly IIslCommand? _right;

    public SimpleConditionCommand(SimpleCondition source, IIslCommand left, IIslCommand? right) : base(source)
    {
        _left = left;
        _op = source.Op;
        _right = right;
    }

    public override bool Evaluate(IOperationContext ctx)
    {
        if (_op == "truthy")
            return RuntimeHelpers.IsTruthy(_left.Execute(ctx).Value);

        var left = _left.Execute(ctx).Value;
        if (_right == null) return RuntimeHelpers.IsTruthy(left);
        var right = _right.Execute(ctx).Value;
        return RuntimeHelpers.CompareValues(left, _op, right);
    }
}

public sealed class BoolConditionCommand : ConditionCommand
{
    private readonly ConditionCommand _left;
    private readonly string _logOp;
    private readonly ConditionCommand _right;

    public BoolConditionCommand(BoolCondition source, ConditionCommand left, ConditionCommand right) : base(source)
    {
        _left = left;
        _logOp = source.LogOp;
        _right = right;
    }

    public override bool Evaluate(IOperationContext ctx) =>
        _logOp == "and"
            ? _left.Evaluate(ctx) && _right.Evaluate(ctx)
            : _left.Evaluate(ctx) || _right.Evaluate(ctx);
}

public sealed class ParenConditionCommand : ConditionCommand
{
    private readonly ConditionCommand _inner;

    public ParenConditionCommand(ParenCondition source, ConditionCommand inner) : base(source)
    {
        _inner = inner;
    }

    public override bool Evaluate(IOperationContext ctx) => _inner.Evaluate(ctx);
}

public sealed class NegatedConditionCommand : ConditionCommand
{
    private readonly IIslCommand _operand;

    public NegatedConditionCommand(NegatedCondition source, IIslCommand operand) : base(source)
    {
        _operand = operand;
    }

    public override bool Evaluate(IOperationContext ctx) =>
        !RuntimeHelpers.IsTruthy(_operand.Execute(ctx).Value);
}
