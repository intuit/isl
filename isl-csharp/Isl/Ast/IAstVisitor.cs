namespace Isl.Ast;

/// <summary>
/// Visitor over the AST. <c>ExecutionBuilder</c> is the primary implementation.
/// We use a flat dispatch (single Visit overload + pattern match inside) rather than typed methods
/// per node, to keep AST records lightweight and avoid plumbing 40+ Accept methods.
/// </summary>
public interface IAstVisitor<out T>
{
    T Visit(IslNode node);
}
