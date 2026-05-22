using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Compiler;
using Isl.Parser;
using Isl.Runtime;
using ExecutionContext = Isl.Runtime.ExecutionContext;

namespace Isl;

/// <summary>
/// Public compile entry point. Lex + parse + lower to commands.
/// Equivalent to <see cref="TransformCompiler.CompileIsl"/> but returns the legacy
/// <see cref="IslTransformer"/> wrapper so existing user code keeps compiling.
/// </summary>
public static class IslCompiler
{
    public static IslTransformer Compile(string name, string islSource)
    {
        var lexer = new IslLexer(islSource);
        var tokens = lexer.Tokenize();
        var parser = new IslParser(tokens);
        var module = parser.ParseModule();
        var execModule = new ExecutionBuilder(name, module).Build();
        return new IslTransformer(name, module, new Transformer(execModule));
    }
}

/// <summary>
/// Backwards-compatible wrapper. Delegates execution to the underlying
/// <see cref="ITransformer"/> built from the command graph.
/// </summary>
public class IslTransformer
{
    private readonly string _name;
    private readonly Ast.Module _module;
    private readonly ITransformer _transformer;

    internal IslTransformer(string name, Ast.Module module, ITransformer transformer)
    {
        _name = name;
        _module = module;
        _transformer = transformer;
    }

    /// <summary>
    /// Run a named function or the flat module statements.
    /// </summary>
    public JsonNode? Run(string functionName, ExecutionContext ctx)
    {
        // For flat modules (no functions), the entry name is irrelevant — empty string is fine.
        var entry = _module.Functions.Count > 0 ? functionName : "";
        return _transformer.RunTransformSync(entry, ctx);
    }
}
