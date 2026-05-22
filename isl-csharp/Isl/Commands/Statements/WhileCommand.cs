using Isl.Ast;
using Isl.Commands.Expressions;
using Isl.Runtime;

namespace Isl.Commands.Statements;

/// <summary>
/// while (cond) { body } endwhile — runs side effects only; returns Null.
/// </summary>
public sealed class WhileCommand : BaseCommand
{
    private readonly ConditionCommand _condition;
    private readonly StatementsBuildCommand _body;
    private readonly int _maxLoops;

    public WhileCommand(WhileStatement source, ConditionCommand condition, StatementsBuildCommand body)
        : base(source)
    {
        _condition = condition;
        _body = body;
        _maxLoops = source.MaxLoops;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        int iter = 0;
        while (_condition.Evaluate(ctx) && iter < _maxLoops)
        {
            _body.Execute(ctx);
            iter++;
        }
        return CommandResult.Null;
    }
}
