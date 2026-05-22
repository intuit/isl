using System.Text;
using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

/// <summary>
/// Backtick interpolated string. Each part is compiled to a typed entry so the runtime
/// avoids AST type checks per interpolation.
/// </summary>
public sealed class InterpolateCommand : BaseCommand
{
    public abstract class PartCommand
    {
        public abstract void Append(StringBuilder sb, IOperationContext ctx);
    }

    public sealed class TextPartCommand : PartCommand
    {
        public string Text { get; init; } = "";
        public override void Append(StringBuilder sb, IOperationContext ctx) => sb.Append(Text);
    }

    public sealed class ExprPartCommand : PartCommand
    {
        public IIslCommand Inner { get; init; } = default!;
        public override void Append(StringBuilder sb, IOperationContext ctx)
        {
            var v = Inner.EvaluateValue(ctx);
            sb.Append(RuntimeHelpers.JsonToString(v));
        }
    }

    public sealed class MathPartCommand : PartCommand
    {
        public MathExpressionCommand Math { get; init; } = default!;
        public override void Append(StringBuilder sb, IOperationContext ctx)
        {
            var d = Math.EvalDouble(ctx);
            sb.Append(d.ToString(System.Globalization.CultureInfo.InvariantCulture));
        }
    }

    public sealed class FuncCallPartCommand : PartCommand
    {
        public IIslCommand Call { get; init; } = default!;
        public override void Append(StringBuilder sb, IOperationContext ctx)
        {
            var v = Call.EvaluateValue(ctx);
            sb.Append(RuntimeHelpers.JsonToString(v));
        }
    }

    private readonly PartCommand[] _parts;
    // Fast path: exactly one TextPart — return the cached JsonValue directly, no StringBuilder.
    private readonly JsonNode? _cachedTextOnly;

    public InterpolateCommand(InterpolateExpr source, IReadOnlyList<PartCommand> parts) : base(source)
    {
        _parts = parts is PartCommand[] arr ? arr : parts.ToArray();
        if (_parts.Length == 1 && _parts[0] is TextPartCommand tpc)
            _cachedTextOnly = JsonValue.Create(tpc.Text);
    }

    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(EvaluateValue(ctx));

    public override JsonNode? EvaluateValue(IOperationContext ctx)
    {
        if (_cachedTextOnly != null)
            return _cachedTextOnly;

        // Estimate capacity: 16 chars per part avoids most StringBuilder reallocations
        // for the common 1–4 part interpolations.
        var sb = new StringBuilder(_parts.Length * 16);
        for (int i = 0; i < _parts.Length; i++)
            _parts[i].Append(sb, ctx);
        return JsonValue.Create(sb.ToString());
    }
}
