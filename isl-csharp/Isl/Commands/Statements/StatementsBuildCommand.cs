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
    private readonly IIslCommand[] _statements;
    // Compile-time guarantee: this scope can never produce an output object — every statement
    // is a return / variable-assign / function-call statement. Skip the JsonObject scaffolding
    // entirely. This is the common shape of user-defined function bodies that just
    // <c>return { ... }</c>.
    private readonly bool _returnOrSideEffectOnly;

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
        _statements = statements is IIslCommand[] arr ? arr : statements.ToArray();
        HasAssignProperty = hasAssignProperty;
        _returnOrSideEffectOnly = !hasAssignProperty && AllSimpleSideEffects(_statements);
    }

    /// <summary>
    /// True when every statement is a plain <c>return</c>, variable assign, or
    /// statement-form function call — none of which can contribute to an output object.
    /// </summary>
    private static bool AllSimpleSideEffects(IIslCommand[] stmts)
    {
        for (int i = 0; i < stmts.Length; i++)
        {
            var s = stmts[i];
            if (s is not ReturnCommand
                && s is not AssignVariableCommand
                && s is not AssignVarPropertyCommand
                && s is not FunctionCallStatementCommand)
            {
                return false;
            }
        }
        return true;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        var stmts = _statements;

        if (_returnOrSideEffectOnly)
        {
            for (int i = 0; i < stmts.Length; i++)
            {
                var result = stmts[i].Execute(ctx);
                if (result.IsReturn) return result;
            }
            return CommandResult.Null;
        }

        JsonObject? outputObject = null;
        bool hasOutput = false;

        for (int i = 0; i < stmts.Length; i++)
        {
            var result = stmts[i].Execute(ctx);

            if (result.IsReturn)
                return result;

            if (result.PropertyPath != null && result.Append)
            {
                outputObject ??= new JsonObject();
                RuntimeHelpers.SetNestedProperty(outputObject, result.PropertyPath, result.Value);
                hasOutput = true;
                continue;
            }

            if (result.Append && result.Value is JsonObject mergeObj)
            {
                outputObject ??= new JsonObject();
                RuntimeHelpers.MergeObjects(outputObject, mergeObj);
                hasOutput = true;
            }
        }

        if (hasOutput) return CommandResult.FromValue(outputObject);
        return CommandResult.Null;
    }
}
