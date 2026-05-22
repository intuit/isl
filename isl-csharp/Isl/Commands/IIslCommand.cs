using System.Text.Json.Nodes;
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

    /// <summary>
    /// Fast-path evaluation that skips the <see cref="CommandResult"/> envelope. Statements
    /// that don't carry property-path / return semantics override this to return the
    /// JsonNode directly, avoiding the 5-field struct copy on every hot Execute call.
    /// Default implementation calls <see cref="Execute"/> and unwraps <c>.Value</c>.
    /// </summary>
    JsonNode? EvaluateValue(IOperationContext ctx);

    T Visit<T>(ICommandVisitor<T> visitor);
}
