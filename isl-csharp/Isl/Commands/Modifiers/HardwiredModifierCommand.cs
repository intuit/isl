using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Commands.Expressions;
using Isl.Modifiers;
using Isl.Runtime;

namespace Isl.Commands.Modifiers;

/// <summary>
/// Modifier whose dispatch was resolved at compile time to a single typed delegate.
/// Skips the giant switch in <see cref="ModifierExecutor"/> and the per-call name lookup
/// — runtime path is just argument eval + delegate invocation (+ optional <c>| if(cond)</c>).
/// Mirrors the Kotlin <c>HardwiredModifierCommand</c>.
/// </summary>
public sealed class HardwiredModifierCommand : ModifierCommand
{
    private readonly ModifierRegistry.ModifierDelegate _runner;
    private readonly IIslCommand[] _argCommandsArr;
    private readonly ConditionCommand? _ifCondition;

    public HardwiredModifierCommand(
        ModifierNode modifier,
        IIslCommand inner,
        IIslCommand[] argCommands,
        ConditionCommand? ifCondition,
        ModifierRegistry.ModifierDelegate runner)
        : base(modifier, inner, argCommands, ifCondition,
               filterCondition: null, mapProjection: null, groupByKeyExpr: null)
    {
        _runner = runner;
        _argCommandsArr = argCommands;
        _ifCondition = ifCondition;
    }

    public override JsonNode? ApplyTo(JsonNode? val, IOperationContext ctx)
    {
        if (_ifCondition != null)
        {
            var condScope = ctx.CreateChildScope();
            condScope.SetVariable("mval", val?.DeepClone());
            condScope.SetVariable("$", val?.DeepClone());
            if (!_ifCondition.Evaluate(condScope))
                return val;
        }

        JsonNode?[] args;
        if (_argCommandsArr.Length == 0)
        {
            args = Array.Empty<JsonNode?>();
        }
        else
        {
            args = new JsonNode?[_argCommandsArr.Length];
            for (int i = 0; i < _argCommandsArr.Length; i++)
                args[i] = _argCommandsArr[i].Execute(ctx).Value;
        }

        return _runner(val, args, ctx);
    }

    public override CommandResult Execute(IOperationContext ctx) =>
        throw new InvalidOperationException("HardwiredModifierCommand should be invoked via ModifiedExpressionCommand.");
}
