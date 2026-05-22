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
        var sourceVal = _source.Execute(ctx).Value;
        var arr = RuntimeHelpers.ToArrayList(sourceVal);
        var results = new JsonArray();

        foreach (var item in arr)
        {
            var scope = ctx.CreateChildScope();
            scope.SetVariable(_iterator, item?.DeepClone());
            scope.SetVariable("$", item?.DeepClone());

            JsonNode? produced;
            if (_bodyObject != null)
                produced = _bodyObject.Execute(scope).Value;
            else if (_body != null)
                produced = _body.Execute(scope).Value;
            else
                produced = null;

            results.Add(produced?.DeepClone());
        }

        return CommandResult.FromValue(results);
    }
}
