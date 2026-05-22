using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Functions;

/// <summary>
/// Function/extension call: @.Service.Method(args).
/// In Milestone 1 this command keeps a string-keyed reference to the module's function
/// dictionary and resolves at runtime — that way recursive and forward references work
/// regardless of compile order. Milestone 3 will introduce <c>HardwiredFunctionCallCommand</c>
/// that bakes the resolved delegate after the dict is fully populated.
/// </summary>
public sealed class FunctionCallCommand : BaseCommand
{
    private readonly string _service;
    private readonly string? _method;
    private readonly IReadOnlyList<IIslCommand> _argCommands;
    private readonly string? _resolvedFnName;
    private readonly IReadOnlyDictionary<string, FunctionDeclarationCommand> _functions;
    private readonly string _extensionKey;
    private readonly string? _extensionFallbackKey;
    private readonly bool _isMathBuiltin;
    private readonly bool _isDateNow;

    public FunctionCallCommand(
        FunctionCallExpr source,
        IReadOnlyList<IIslCommand> argCommands,
        string? resolvedFnName,
        IReadOnlyDictionary<string, FunctionDeclarationCommand> functions)
        : base(source)
    {
        _service = source.Service;
        _method = source.Method;
        _argCommands = argCommands;
        _resolvedFnName = resolvedFnName;
        _functions = functions;
        _extensionKey = _method != null ? $"{_service}.{_method}" : _service;
        _extensionFallbackKey = _method != null ? _service : null;
        _isMathBuiltin = _service == "Math";
        _isDateNow = _service == "Date" && _method == "Now";
    }

    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(EvaluateValue(ctx));

    public override JsonNode? EvaluateValue(IOperationContext ctx)
    {
        if (_resolvedFnName != null && _functions.TryGetValue(_resolvedFnName, out var fn))
            return fn.InvokeWithCommands(ctx, _argCommands);

        var ext = ctx.GetExtension(_extensionKey)
                  ?? (_extensionFallbackKey != null ? ctx.GetExtension(_extensionFallbackKey) : null);
        if (ext != null)
            return ext(EvalArgs(ctx));

        if (_isDateNow)
            return JsonValue.Create(DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"));

        if (_isMathBuiltin)
            return ApplyMathExtension(_method ?? "", EvalArgs(ctx));

        return null;
    }

    private JsonNode?[] EvalArgs(IOperationContext ctx)
    {
        if (_argCommands.Count == 0) return Array.Empty<JsonNode?>();
        var args = new JsonNode?[_argCommands.Count];
        for (int i = 0; i < _argCommands.Count; i++)
            args[i] = _argCommands[i].EvaluateValue(ctx);
        return args;
    }

    private static JsonNode? ApplyMathExtension(string method, JsonNode?[] args) => method switch
    {
        "abs" => args.Length > 0 ? JsonValue.Create(Math.Abs(RuntimeHelpers.ToDouble(args[0]))) : null,
        "ceil" => args.Length > 0 ? JsonValue.Create(Math.Ceiling(RuntimeHelpers.ToDouble(args[0]))) : null,
        "floor" => args.Length > 0 ? JsonValue.Create(Math.Floor(RuntimeHelpers.ToDouble(args[0]))) : null,
        _ => null
    };
}
