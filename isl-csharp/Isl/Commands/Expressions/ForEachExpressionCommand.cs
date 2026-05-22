using Isl.Ast;
using Isl.Commands.Statements;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

/// <summary>
/// foreach as expression — wraps <see cref="ForEachCommand"/> and surfaces its result.
/// </summary>
public sealed class ForEachExpressionCommand : BaseCommand
{
    private readonly ForEachCommand _inner;

    public ForEachExpressionCommand(ForEachExpr source, ForEachCommand inner) : base(source)
    {
        _inner = inner;
    }

    public override CommandResult Execute(IOperationContext ctx) => _inner.Execute(ctx);
}
