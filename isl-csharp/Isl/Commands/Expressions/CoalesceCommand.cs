using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

/// <summary>
/// left ?? right — null AND empty string trigger fallback (parity with original).
/// </summary>
public sealed class CoalesceCommand : BaseCommand
{
    private readonly IIslCommand _left;
    private readonly IIslCommand _right;

    public CoalesceCommand(CoalesceExpr source, IIslCommand left, IIslCommand right) : base(source)
    {
        _left = left;
        _right = right;
    }

    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(EvaluateValue(ctx));

    public override JsonNode? EvaluateValue(IOperationContext ctx)
    {
        var left = _left.EvaluateValue(ctx);
        if (left != null && !(left is JsonValue lv && lv.TryGetValue<string>(out var s) && s == ""))
            return left;
        return _right.EvaluateValue(ctx);
    }
}
