using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands;

/// <summary>
/// Compiled, self-executing IR node. Built once by <c>ExecutionBuilder</c> from the AST,
/// reused for every <c>Transformer.RunTransformSync</c> call.
/// Mirrors the Kotlin <c>IIslCommand</c>.
/// </summary>
public interface IIslCommand
{
    /// <summary>The originating AST node (for diagnostics/coverage).</summary>
    IslNode? Source { get; }

    IIslCommand? Parent { get; set; }

    CommandResult Execute(IOperationContext ctx);

    T Visit<T>(ICommandVisitor<T> visitor);
}
