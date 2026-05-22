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
            var v = Inner.Execute(ctx).Value;
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
            var v = Call.Execute(ctx).Value;
            sb.Append(RuntimeHelpers.JsonToString(v));
        }
    }

    private readonly IReadOnlyList<PartCommand> _parts;

    public InterpolateCommand(InterpolateExpr source, IReadOnlyList<PartCommand> parts) : base(source)
    {
        _parts = parts;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        var sb = new StringBuilder();
        for (int i = 0; i < _parts.Count; i++)
            _parts[i].Append(sb, ctx);
        return CommandResult.FromValue(JsonValue.Create(sb.ToString()));
    }
}
