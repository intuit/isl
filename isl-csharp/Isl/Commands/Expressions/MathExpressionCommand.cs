using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

/// <summary>
/// Compiled math expression: numbers, variable refs, function calls, binary ops, parens.
/// Exposes <see cref="EvalDouble"/> directly so callers can avoid boxing into JsonValue.
/// </summary>
public abstract class MathExpressionCommand : BaseCommand
{
    protected MathExpressionCommand(IslNode source) : base(source) { }

    public abstract double EvalDouble(IOperationContext ctx);

    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(JsonValue.Create(EvalDouble(ctx)));
}

public sealed class MathNumberCommand : MathExpressionCommand
{
    private readonly double _value;
    public MathNumberCommand(MathNumber source) : base(source) => _value = source.Value;
    public override double EvalDouble(IOperationContext ctx) => _value;
}

public sealed class MathBinOpCommand : MathExpressionCommand
{
    private readonly MathExpressionCommand _left;
    private readonly string _op;
    private readonly MathExpressionCommand _right;

    public MathBinOpCommand(MathBinOp source, MathExpressionCommand left, MathExpressionCommand right) : base(source)
    {
        _left = left;
        _op = source.Op;
        _right = right;
    }

    public override double EvalDouble(IOperationContext ctx) => _op switch
    {
        "+" => _left.EvalDouble(ctx) + _right.EvalDouble(ctx),
        "-" => _left.EvalDouble(ctx) - _right.EvalDouble(ctx),
        "*" => _left.EvalDouble(ctx) * _right.EvalDouble(ctx),
        "/" => _left.EvalDouble(ctx) / _right.EvalDouble(ctx),
        _ => 0
    };
}

public sealed class MathVariableCommand : MathExpressionCommand
{
    private readonly VariableSelectorCommand _var;
    public MathVariableCommand(MathVariable source, VariableSelectorCommand var) : base(source) => _var = var;
    public override double EvalDouble(IOperationContext ctx) => RuntimeHelpers.ToDouble(_var.ResolveValue(ctx));
}

public sealed class MathFuncCallCommand : MathExpressionCommand
{
    private readonly IIslCommand _call;
    public MathFuncCallCommand(MathFuncCall source, IIslCommand call) : base(source) => _call = call;
    public override double EvalDouble(IOperationContext ctx) => RuntimeHelpers.ToDouble(_call.EvaluateValue(ctx));
}

public sealed class MathParenCommand : MathExpressionCommand
{
    private readonly MathExpressionCommand _inner;
    public MathParenCommand(MathParen source, MathExpressionCommand inner) : base(source) => _inner = inner;
    public override double EvalDouble(IOperationContext ctx) => _inner.EvalDouble(ctx);
}
