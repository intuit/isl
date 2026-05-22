using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Statements;

/// <summary>
/// switch(subject) cases [else] endswitch — used both as a statement and via
/// <see cref="Expressions.SwitchExpressionCommand"/> as an expression.
/// </summary>
public sealed class SwitchCommand : BaseCommand
{
    public sealed class CompiledCase
    {
        public IIslCommand? Pattern { get; init; }
        public string Operator { get; init; } = "==";
        public StatementsBuildCommand? Body { get; init; }
        public IIslCommand? ResultExpr { get; init; }
    }

    private readonly IIslCommand _subject;
    private readonly IReadOnlyList<CompiledCase> _cases;
    private readonly StatementsBuildCommand? _elseBody;
    private readonly IIslCommand? _elseResultExpr;

    public SwitchCommand(
        IslNode source,
        IIslCommand subject,
        IReadOnlyList<CompiledCase> cases,
        StatementsBuildCommand? elseBody,
        IIslCommand? elseResultExpr)
        : base(source)
    {
        _subject = subject;
        _cases = cases;
        _elseBody = elseBody;
        _elseResultExpr = elseResultExpr;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        var subject = _subject.EvaluateValue(ctx);

        foreach (var c in _cases)
        {
            if (c.Pattern == null) continue;
            var pattern = c.Pattern.EvaluateValue(ctx);
            var op = c.Operator;

            bool matches;
            if (op == "==" && pattern is JsonValue pjv && pjv.TryGetValue<string>(out var ps)
                && ps.Length > 2 && ps.StartsWith("/") && ps.EndsWith("/"))
            {
                var regexPat = ps.Substring(1, ps.Length - 2);
                var subjectStr = RuntimeHelpers.JsonToString(subject);
                try
                {
                    matches = System.Text.RegularExpressions.Regex.IsMatch(subjectStr, regexPat,
                        System.Text.RegularExpressions.RegexOptions.IgnoreCase);
                }
                catch { matches = false; }
            }
            else
            {
                matches = RuntimeHelpers.CompareValues(subject, op, pattern);
            }

            if (matches)
            {
                if (c.ResultExpr != null) return CommandResult.FromValue(c.ResultExpr.EvaluateValue(ctx));
                if (c.Body != null) return c.Body.Execute(ctx);
                return CommandResult.Null;
            }
        }

        if (_elseResultExpr != null) return CommandResult.FromValue(_elseResultExpr.EvaluateValue(ctx));
        if (_elseBody != null) return _elseBody.Execute(ctx);
        return CommandResult.Null;
    }
}
