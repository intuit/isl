using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Commands.Expressions;
using Isl.Runtime;

namespace Isl.Commands.Modifiers;

/// <summary>
/// One stage of a modifier pipeline: <c>$x | trim</c> becomes <c>ModifierCommand("trim", $x)</c>.
/// Chains nest: <c>$x | trim | upper</c> = <c>ModifierCommand("upper", ModifierCommand("trim", $x))</c>.
/// Milestone 1 dispatch goes through <see cref="ModifierExecutor"/>; Milestone 2 will replace it
/// with hardwired delegate calls.
/// </summary>
public class ModifierCommand : BaseCommand
{
    private readonly IIslCommand _inner;
    private readonly ModifierNode _modifier;
    private readonly IReadOnlyList<IIslCommand> _argCommands;
    private readonly ConditionCommand? _ifCondition;

    /// <summary>Per-item filter predicate (only set when modifier name is "filter" with a condition).</summary>
    private readonly ConditionCommand? _filterCondition;

    /// <summary>Per-item map projection (only set when modifier name is "map" with at least one arg).</summary>
    private readonly IIslCommand? _mapProjection;

    /// <summary>Per-item key selector for group.by when the first arg is a VariableExpr (not a literal field name).</summary>
    private readonly IIslCommand? _groupByKeyExpr;

    /// <summary>True when this modifier is a "typeof" with no sub-name and no condition — special-cased for type annotations.</summary>
    public bool IsTypeofPlain { get; }

    public ModifierCommand(
        ModifierNode modifier,
        IIslCommand inner,
        IReadOnlyList<IIslCommand> argCommands,
        ConditionCommand? ifCondition,
        ConditionCommand? filterCondition,
        IIslCommand? mapProjection,
        IIslCommand? groupByKeyExpr)
        : base(modifier)
    {
        _modifier = modifier;
        _inner = inner;
        _argCommands = argCommands;
        _ifCondition = ifCondition;
        _filterCondition = filterCondition;
        _mapProjection = mapProjection;
        _groupByKeyExpr = groupByKeyExpr;
        IsTypeofPlain = modifier.Name.Equals("typeof", StringComparison.OrdinalIgnoreCase)
                         && modifier.SubName == null
                         && modifier.Condition == null;
    }

    public IIslCommand Inner => _inner;

    public override CommandResult Execute(IOperationContext ctx)
    {
        var val = _inner.Execute(ctx).Value;
        var result = ApplyTo(val, ctx);
        return CommandResult.FromValue(result);
    }

    /// <summary>
    /// Runs the modifier transform against an externally-supplied value (used by
    /// <see cref="ModifiedExpressionCommand"/> so the chain head can apply typeof
    /// type-annotation lookup before delegating).
    /// </summary>
    public JsonNode? ApplyTo(JsonNode? val, IOperationContext ctx)
    {
        var args = EvalArgs(ctx);
        return ModifierExecutor.Apply(val, _modifier, args, ctx,
            _ifCondition, _filterCondition, _mapProjection, _groupByKeyExpr);
    }

    private JsonNode?[] EvalArgs(IOperationContext ctx)
    {
        if (_argCommands.Count == 0) return Array.Empty<JsonNode?>();
        var args = new JsonNode?[_argCommands.Count];
        for (int i = 0; i < _argCommands.Count; i++)
            args[i] = _argCommands[i].Execute(ctx).Value;
        return args;
    }
}
