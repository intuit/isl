using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Parser;
using Isl.Runtime;
using ExecutionContext = Isl.Runtime.ExecutionContext;

namespace Isl;

public static class IslCompiler
{
    public static IslTransformer Compile(string name, string islSource)
    {
        var lexer = new IslLexer(islSource);
        var tokens = lexer.Tokenize();
        var parser = new IslParser(tokens);
        var module = parser.ParseModule();
        return new IslTransformer(name, module);
    }
}

public class IslTransformer
{
    private readonly string _name;
    private readonly Ast.Module _module;
    private readonly Interpreter _interpreter;

    internal IslTransformer(string name, Ast.Module module)
    {
        _name = name;
        _module = module;
        _interpreter = new Interpreter(module);
    }

    /// <summary>
    /// Run a named function or the flat module statements.
    /// </summary>
    public JsonNode? Run(string functionName, ExecutionContext ctx)
    {
        // If functions are defined, call the named function
        if (_module.Functions.Count > 0)
            return _interpreter.RunFunction(functionName, ctx);

        // Otherwise execute flat statements
        return _interpreter.RunFlat(ctx);
    }
}
