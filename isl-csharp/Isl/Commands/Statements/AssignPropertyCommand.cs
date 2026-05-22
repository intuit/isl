using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Statements;

/// <summary>
/// prop.path: value  /  prop.path = value (writes into the surrounding object's output).
/// Returns a <see cref="CommandResult.PropertyPath"/> so the parent
/// <see cref="StatementsBuildCommand"/> writes it into the result object.
/// </summary>
public sealed class AssignPropertyCommand : BaseCommand
{
    private readonly IReadOnlyList<string> _path;
    private readonly IIslCommand _value;
    private readonly bool _hasOptionalElseInlineIf;

    public AssignPropertyCommand(AssignProperty source, IIslCommand value, bool hasOptionalElseInlineIf)
        : base(source)
    {
        _path = source.Path;
        _value = value;
        _hasOptionalElseInlineIf = hasOptionalElseInlineIf;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        var val = _value.Execute(ctx).Value;
        if (val == null && _hasOptionalElseInlineIf)
            return CommandResult.NullAppendFalse;
        return CommandResult.Property(_path, val);
    }
}
