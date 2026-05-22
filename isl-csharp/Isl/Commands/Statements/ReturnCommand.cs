using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Statements;

/// <summary>
/// return expr — bubbles through <see cref="StatementsBuildCommand"/> via <see cref="CommandResult.IsReturn"/>.
/// </summary>
public sealed class ReturnCommand : BaseCommand
{
    private readonly IIslCommand _value;

    public ReturnCommand(ReturnStatement source, IIslCommand value) : base(source)
    {
        _value = value;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        var val = _value.Execute(ctx).Value;
        return CommandResult.Return(val);
    }
}
