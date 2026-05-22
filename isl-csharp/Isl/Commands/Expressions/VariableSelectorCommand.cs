using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

/// <summary>
/// Variable + path selector ($var, $var.prop, $var[0], $var.path[(cond)]).
/// Mirrors the original <c>EvalVariable</c> behavior; Milestone 3 will split this into
/// fast-path tiers (no path / simple dot path / complex path with filters).
/// </summary>
public sealed class VariableSelectorCommand : BaseCommand
{
    private readonly string _name;
    private readonly IReadOnlyList<VariablePart> _parts;
    private readonly IReadOnlyList<ConditionCommand?> _filterCommands;

    public string VariableName => _name;
    public bool HasNoPath => _parts.Count == 0;

    public VariableSelectorCommand(
        VariableExpr source,
        IReadOnlyList<ConditionCommand?> filterCommands)
        : base(source)
    {
        _name = source.Name == "" ? "$" : source.Name;
        _parts = source.Parts;
        _filterCommands = filterCommands;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        var value = ResolveValue(ctx);
        return CommandResult.FromValue(value);
    }

    public JsonNode? ResolveValue(IOperationContext ctx)
    {
        if (_name == "$" && _parts.Count == 0)
            return ctx.GetVariable("$");

        JsonNode? current = ctx.GetVariable(_name);
        if (current == null && _parts.Count == 0) return null;

        for (int i = 0; i < _parts.Count; i++)
        {
            if (current == null) return null;
            switch (_parts[i])
            {
                case PropertyPart pp:
                    if (current is JsonObject jo)
                        current = jo.TryGetPropertyValue(pp.Name, out var v) ? v : null;
                    else
                        return null;
                    break;
                case IndexPart ip:
                    if (current is JsonArray ja && ip.Index < ja.Count)
                        current = ja[ip.Index];
                    else
                        return null;
                    break;
                case ConditionFilterPart:
                    var filterCmd = _filterCommands[i];
                    if (filterCmd != null && current is JsonArray jarr)
                    {
                        var filtered = new JsonArray();
                        foreach (var item in jarr)
                        {
                            var sc = ctx.CreateChildScope();
                            sc.SetVariable("$", item?.DeepClone());
                            sc.SetVariable("it", item?.DeepClone());
                            if (filterCmd.Evaluate(sc))
                                filtered.Add(item?.DeepClone());
                        }
                        current = filtered;
                    }
                    break;
            }
        }
        return current;
    }
}
