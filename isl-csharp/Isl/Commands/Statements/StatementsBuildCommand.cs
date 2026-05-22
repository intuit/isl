using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Statements;

/// <summary>
/// Executes a sequence of statement commands. Assembles property-style assignments
/// (and merge-style child results) into an output JsonObject; bubbles a return-style result
/// up immediately. Mirrors the original <c>ExecuteStatements</c> dispatch.
/// </summary>
public sealed class StatementsBuildCommand : BaseCommand
{
    private readonly IReadOnlyList<IIslCommand> _statements;

    /// <summary>
    /// True when the original statement list contains an <c>AssignProperty</c>.
    /// Used by the parent <see cref="IfCommand"/>/<see cref="SwitchCommand"/> to decide
    /// whether a returned object should merge or bubble up.
    /// </summary>
    public bool HasAssignProperty { get; }

    public StatementsBuildCommand(
        IslNode? source,
        IReadOnlyList<IIslCommand> statements,
        bool hasAssignProperty)
        : base(source)
    {
        _statements = statements;
        HasAssignProperty = hasAssignProperty;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        var outputObject = new JsonObject();
        bool hasOutput = false;

        for (int i = 0; i < _statements.Count; i++)
        {
            var result = _statements[i].Execute(ctx);

            if (result.IsReturn)
                return result;

            if (result.PropertyPath != null && result.Append)
            {
                RuntimeHelpers.SetNestedProperty(outputObject, result.PropertyPath, result.Value);
                hasOutput = true;
                continue;
            }

            if (result.Append && result.Value is JsonObject mergeObj)
            {
                RuntimeHelpers.MergeObjects(outputObject, mergeObj);
                hasOutput = true;
            }
        }

        if (hasOutput) return CommandResult.FromValue(outputObject);
        return CommandResult.Null;
    }
}
