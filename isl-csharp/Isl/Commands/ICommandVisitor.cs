namespace Isl.Commands;

/// <summary>
/// Optional analysis-only visitor for the command graph. Not used at runtime.
/// Mirrors the Kotlin <c>ICommandVisitor</c> (also unused at runtime).
/// Concrete commands typically implement specific Visit methods or fall through to <see cref="VisitDefault"/>.
/// </summary>
public interface ICommandVisitor<out T>
{
    T VisitDefault(IIslCommand command);
}
