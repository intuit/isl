using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Commands.Expressions;
using Isl.Runtime;

namespace Isl.Commands.Modifiers;

/// <summary>
/// Top-level wrapper around a modifier chain. Owns the typeof type-annotation special case
/// so each <see cref="ModifierCommand"/> stays generic.
/// In a chain <c>$x | trim | upper</c>, the value is computed once here, then each modifier
/// applied left-to-right; the head modifier sees the source variable name for typeof lookup.
/// </summary>
public sealed class ModifiedExpressionCommand : BaseCommand
{
    private readonly IIslCommand _value;
    private readonly IReadOnlyList<ModifierCommand> _modifiers;
    private readonly string? _sourceVarName;

    public ModifiedExpressionCommand(
        ModifiedExpr source,
        IIslCommand value,
        IReadOnlyList<ModifierCommand> modifiers,
        string? sourceVarName)
        : base(source)
    {
        _value = value;
        _modifiers = modifiers;
        _sourceVarName = sourceVarName;
    }

    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(EvaluateValue(ctx));

    public override JsonNode? EvaluateValue(IOperationContext ctx)
    {
        var val = _value.EvaluateValue(ctx);
        string? sourceVarName = _sourceVarName;

        for (int i = 0; i < _modifiers.Count; i++)
        {
            var mod = _modifiers[i];
            if (mod.IsTypeofPlain)
            {
                if (val == null)
                {
                    val = JsonValue.Create("null");
                }
                else
                {
                    string? namedType = sourceVarName != null ? ctx.GetTypeAnnotation(sourceVarName) : null;
                    if (namedType == null)
                        namedType = ctx.GetNodeType(val);
                    if (namedType != null)
                        val = JsonValue.Create(namedType);
                    else
                        val = mod.ApplyTo(val, ctx);
                }
            }
            else
            {
                val = mod.ApplyTo(val, ctx);
            }
            sourceVarName = null;
        }
        return val;
    }
}
