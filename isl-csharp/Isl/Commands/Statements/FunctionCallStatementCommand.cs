using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Statements;

/// <summary>
/// Standalone function call as statement — runs the call, discards the value.
/// </summary>
public sealed class FunctionCallStatementCommand : BaseCommand
{
    private readonly IIslCommand _call;

    public FunctionCallStatementCommand(FunctionCallStatement source, IIslCommand call) : base(source)
    {
        _call = call;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        _call.Execute(ctx);
        return CommandResult.Null;
    }
}
