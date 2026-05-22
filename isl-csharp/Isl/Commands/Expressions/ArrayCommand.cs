using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

/// <summary>
/// Array literal: [val1, val2, ...].
/// </summary>
public sealed class ArrayCommand : BaseCommand
{
    private readonly IReadOnlyList<IIslCommand> _elements;

    public ArrayCommand(ArrayExpr source, IReadOnlyList<IIslCommand> elements) : base(source)
    {
        _elements = elements;
    }

    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(EvaluateValue(ctx));

    public override JsonNode? EvaluateValue(IOperationContext ctx)
    {
        var arr = new JsonArray();
        for (int i = 0; i < _elements.Count; i++)
            arr.Add(_elements[i].EvaluateValue(ctx)?.DeepClone());
        return arr;
    }
}
