using Isl.Compiler;
using Isl.Parser;
using Isl.Runtime;

namespace Isl;

/// <summary>
/// Public compile entry point. Mirrors Kotlin's <c>TransformCompiler.compileIsl</c>:
/// lex + parse + lower to commands + return a runnable <see cref="ITransformer"/>.
/// </summary>
public static class TransformCompiler
{
    public static ITransformer CompileIsl(string name, string script)
    {
        var lexer = new IslLexer(script);
        var tokens = lexer.Tokenize();
        var parser = new IslParser(tokens);
        var module = parser.ParseModule();
        var execModule = new ExecutionBuilder(name, module).Build();
        return new Transformer(execModule);
    }
}
