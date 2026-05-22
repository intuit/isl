using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Statements;

/// <summary>
/// $name = expr  /  $name: TypeName = expr.
/// Carries pre-resolved type-annotation metadata so the runtime path does no AST inspection.
/// </summary>
public sealed class AssignVariableCommand : BaseCommand
{
    private readonly string _name;
    private readonly IIslCommand _value;
    private readonly string? _explicitTypeName;
    private readonly string? _byrefSourceVarName;
    private readonly string? _functionReturnTypeName;

    public AssignVariableCommand(
        AssignVariable source,
        IIslCommand value,
        string? explicitTypeName,
        string? byrefSourceVarName,
        string? functionReturnTypeName)
        : base(source)
    {
        _name = source.Name;
        _value = value;
        _explicitTypeName = explicitTypeName;
        _byrefSourceVarName = byrefSourceVarName;
        _functionReturnTypeName = functionReturnTypeName;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        var val = _value.EvaluateValue(ctx);
        ctx.SetVariable(_name, val);

        if (_explicitTypeName != null)
        {
            ctx.SetTypeAnnotation(_name, _explicitTypeName);
            if (val != null) ctx.SetNodeType(val, _explicitTypeName);
            if (_byrefSourceVarName != null)
            {
                ctx.SetTypeAnnotation(_byrefSourceVarName, _explicitTypeName);
                if (val != null) ctx.SetNodeType(val, _explicitTypeName);
            }
        }
        else if (_byrefSourceVarName != null)
        {
            var srcType = ctx.GetTypeAnnotation(_byrefSourceVarName);
            if (srcType != null) ctx.SetTypeAnnotation(_name, srcType);
        }
        else if (_functionReturnTypeName != null)
        {
            ctx.SetTypeAnnotation(_name, _functionReturnTypeName);
        }

        return CommandResult.Null;
    }
}
