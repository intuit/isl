using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Commands.Statements;
using Isl.Runtime;

namespace Isl.Commands.Functions;

/// <summary>
/// A compiled user-defined function. Wraps the body and parameter list; <see cref="Invoke"/>
/// creates a child scope, binds parameters, and runs the body. <see cref="GetRunner"/> returns
/// a delegate suitable for hardwired call dispatch (Milestone 3).
/// </summary>
public sealed class FunctionDeclarationCommand : BaseCommand
{
    public string Name { get; }
    public IReadOnlyList<string> Parameters { get; }
    public string? ReturnTypeName { get; }
    private readonly StatementsBuildCommand _body;

    public FunctionDeclarationCommand(FunctionDecl source, StatementsBuildCommand body) : base(source)
    {
        Name = source.Name;
        Parameters = source.Parameters;
        ReturnTypeName = source.ReturnTypeName;
        _body = body;
    }

    public override CommandResult Execute(IOperationContext ctx) => _body.Execute(ctx);

    public JsonNode? Invoke(IOperationContext callerCtx, JsonNode?[] args)
    {
        var childCtx = callerCtx.CreateChildScope();
        for (int i = 0; i < Parameters.Count && i < args.Length; i++)
            childCtx.SetVariable(Parameters[i], args[i]);
        var result = _body.Execute(childCtx);
        return result.Value;
    }

    /// <summary>
    /// Invoke without a pre-built args array — caller binds arguments inline. Avoids the
    /// per-call allocation of a JsonNode?[] when the call site has compiled arg commands.
    /// </summary>
    public JsonNode? InvokeWithCommands(IOperationContext callerCtx, IReadOnlyList<IIslCommand> argCommands)
    {
        var childCtx = callerCtx.CreateChildScope();
        var paramCount = Parameters.Count;
        for (int i = 0; i < paramCount && i < argCommands.Count; i++)
            childCtx.SetVariable(Parameters[i], argCommands[i].Execute(callerCtx).Value);
        var result = _body.Execute(childCtx);
        return result.Value;
    }

    public Func<FunctionExecuteContext, JsonNode?> GetRunner() =>
        fec => Invoke(fec.OperationContext, fec.Parameters);
}
