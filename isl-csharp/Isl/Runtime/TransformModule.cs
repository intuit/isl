using Isl.Commands.Functions;
using Isl.Commands.Statements;

namespace Isl.Runtime;

/// <summary>
/// The compiled artifact of <c>TransformCompiler.CompileIsl</c>.
/// Holds the precompiled function table and (for flat scripts) the top-level statements
/// command. Reused across every <c>Transformer.RunTransformSync</c> invocation.
/// </summary>
public sealed class TransformModule
{
    public string Name { get; }

    /// <summary>
    /// All declared functions, keyed by their original-cased name. Lookup is case-sensitive
    /// today; Milestone 3 will switch all keys to lowercase at compile time.
    /// </summary>
    public IReadOnlyDictionary<string, FunctionDeclarationCommand> Functions { get; }

    /// <summary>
    /// Top-level statements when the module has no functions. Null otherwise.
    /// </summary>
    public StatementsBuildCommand? FlatStatements { get; }

    public TransformModule(
        string name,
        IReadOnlyDictionary<string, FunctionDeclarationCommand> functions,
        StatementsBuildCommand? flatStatements)
    {
        Name = name;
        Functions = functions;
        FlatStatements = flatStatements;
    }
}
