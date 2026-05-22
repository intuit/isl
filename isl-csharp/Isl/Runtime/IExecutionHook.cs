using Isl.Commands;

namespace Isl.Runtime;

/// <summary>
/// Optional hook for tracing / debugging / coverage. Off by default; the hot path checks for null.
/// </summary>
public interface IExecutionHook
{
    void OnBeforeExecute(IIslCommand command, IOperationContext ctx);
    void OnAfterExecute(IIslCommand command, IOperationContext ctx, CommandResult result);
}
