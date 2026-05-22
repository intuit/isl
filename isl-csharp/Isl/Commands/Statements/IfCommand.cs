using Isl.Ast;
using Isl.Commands.Expressions;
using Isl.Runtime;

namespace Isl.Commands.Statements;

/// <summary>
/// if (cond) { trueBody } [else { falseBody }] endif.
/// Both branches are <see cref="StatementsBuildCommand"/>s; the chosen branch's result
/// is forwarded — preserving Append (merge-into-parent) vs IsReturn (bubble-up) semantics.
/// </summary>
public sealed class IfCommand : BaseCommand
{
    private readonly ConditionCommand _condition;
    private readonly StatementsBuildCommand _trueBody;
    private readonly StatementsBuildCommand? _falseBody;

    public IfCommand(
        IfStatement source,
        ConditionCommand condition,
        StatementsBuildCommand trueBody,
        StatementsBuildCommand? falseBody)
        : base(source)
    {
        _condition = condition;
        _trueBody = trueBody;
        _falseBody = falseBody;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        bool cond = _condition.Evaluate(ctx);
        var branch = cond ? _trueBody : _falseBody;
        if (branch == null) return CommandResult.Null;

        var result = branch.Execute(ctx);

        if (result.IsReturn) return result;
        if (result.Value == null) return CommandResult.Null;

        if (branch.HasAssignProperty)
        {
            return new CommandResult { Value = result.Value, Append = true, ValidResult = true };
        }

        return CommandResult.Return(result.Value);
    }
}
