using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands;

/// <summary>
/// Base class for commands. Holds the originating AST node and parent pointer.
/// </summary>
public abstract class BaseCommand : IIslCommand
{
    protected BaseCommand(IslNode? source) => Source = source;

    public IslNode? Source { get; }

    public IIslCommand? Parent { get; set; }

    public abstract CommandResult Execute(IOperationContext ctx);

    /// <inheritdoc />
    public virtual JsonNode? EvaluateValue(IOperationContext ctx) => Execute(ctx).Value;

    public virtual T Visit<T>(ICommandVisitor<T> visitor) => visitor.VisitDefault(this);
}
