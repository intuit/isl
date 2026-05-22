using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Statements;

/// <summary>
/// $var.prop.path = value (assign nested property on a variable).
/// </summary>
public sealed class AssignVarPropertyCommand : BaseCommand
{
    private readonly string _varName;
    private readonly IReadOnlyList<string> _propPath;
    private readonly IIslCommand _value;

    public AssignVarPropertyCommand(AssignVarProperty source, IIslCommand value)
        : base(source)
    {
        _varName = source.VarName;
        _propPath = source.PropPath;
        _value = value;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        var val = _value.EvaluateValue(ctx);
        var target = ctx.GetVariable(_varName);
        if (target is not JsonObject obj)
        {
            obj = new JsonObject();
            ctx.SetVariable(_varName, obj);
        }
        RuntimeHelpers.SetNestedProperty(obj, _propPath, val);
        return CommandResult.Null;
    }
}
