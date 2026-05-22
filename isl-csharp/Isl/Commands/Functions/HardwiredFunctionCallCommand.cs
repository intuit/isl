using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Functions;

/// <summary>
/// Function call where the callee is a user-defined function in the same module and the
/// reference can be baked at compile time. Skips the dictionary lookup + extension fallback
/// chain on every invocation that <see cref="FunctionCallCommand"/> walks through.
/// </summary>
public sealed class HardwiredFunctionCallCommand : BaseCommand
{
    private readonly IReadOnlyList<IIslCommand> _argCommands;
    // Holder is mutated after all functions compile so this command sees the resolved
    // target without a dict lookup. Stored as a single-element array so child commands
    // share the same slot when the function table is finalised.
    private readonly FunctionDeclarationCommand[] _targetSlot;

    public HardwiredFunctionCallCommand(
        FunctionCallExpr source,
        IReadOnlyList<IIslCommand> argCommands,
        FunctionDeclarationCommand[] targetSlot)
        : base(source)
    {
        _argCommands = argCommands;
        _targetSlot = targetSlot;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        var target = _targetSlot[0];
        return CommandResult.FromValue(target.InvokeWithCommands(ctx, _argCommands));
    }
}
