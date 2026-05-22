using Isl.Ast;
using Isl.Commands.Statements;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

/// <summary>
/// switch as expression — wraps <see cref="SwitchCommand"/> and surfaces its result.
/// </summary>
public sealed class SwitchExpressionCommand : BaseCommand
{
    private readonly SwitchCommand _inner;

    public SwitchExpressionCommand(SwitchExpr source, SwitchCommand inner) : base(source)
    {
        _inner = inner;
    }

    public override CommandResult Execute(IOperationContext ctx) => _inner.Execute(ctx);
}
