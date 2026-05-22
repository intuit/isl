using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Statements;

/// <summary>
/// foreach $item in $arr { body } endfor — collects per-iteration results into a JsonArray.
/// Used both as a statement (result discarded) and via <see cref="Expressions.ForEachExpressionCommand"/>
/// as an expression (result kept).
/// </summary>
public sealed class ForEachCommand : BaseCommand
{
    private readonly string _iterator;
    private readonly IIslCommand _source;
    private readonly StatementsBuildCommand? _body;
    private readonly Expressions.ObjectBuildCommand? _bodyObject;

    public ForEachCommand(
        IslNode source,
        string iterator,
        IIslCommand sourceCommand,
        StatementsBuildCommand? body,
        Expressions.ObjectBuildCommand? bodyObject)
        : base(source)
    {
        _iterator = iterator;
        _source = sourceCommand;
        _body = body;
        _bodyObject = bodyObject;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        var sourceVal = _source.EvaluateValue(ctx);
        var arr = RuntimeHelpers.ToArrayList(sourceVal);
        var results = new JsonArray();

        foreach (var item in arr)
        {
            var scope = ctx.CreateChildScope();
            // One clone shared between the named iterator and the implicit $.
            // Both bindings see the same node, so a write to either is visible to the other —
            // this matches the prior behaviour for read-only iteration patterns and saves
            // a per-iteration DeepClone() on the hot path.
            var cloned = item?.DeepClone();
            scope.SetVariable(_iterator, cloned);
            scope.SetVariable("$", cloned);

            JsonNode? produced;
            if (_bodyObject != null)
                produced = _bodyObject.EvaluateValue(scope);
            else if (_body != null)
                produced = _body.EvaluateValue(scope);
            else
                produced = null;

            results.Add(produced?.DeepClone());
        }

        return CommandResult.FromValue(results);
    }
}
