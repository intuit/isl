using System.Text.Json.Nodes;
using Isl.Ast;
using Isl.Runtime;

namespace Isl.Commands.Expressions;

/// <summary>
/// Object literal: { prop: val, ... }. Each property is precompiled into a typed entry so the
/// runtime path is a tight switch with no AST inspection.
/// </summary>
public sealed class ObjectBuildCommand : BaseCommand
{
    public abstract class Entry
    {
        public abstract void Apply(JsonObject obj, IOperationContext ctx);
    }

    public sealed class PropEntry : Entry
    {
        public IReadOnlyList<string> Path { get; init; } = Array.Empty<string>();
        public IIslCommand Value { get; init; } = default!;
        public string? TypeName { get; init; }
        public bool HasOptionalElseInlineIf { get; init; }

        public override void Apply(JsonObject obj, IOperationContext ctx)
        {
            var v = Value.EvaluateValue(ctx);
            if (v == null && HasOptionalElseInlineIf) return;

            if (Path.Count == 1)
            {
                var key = Path[0];
                obj[key] = v?.DeepClone();
                if (TypeName != null && obj.TryGetPropertyValue(key, out var stored) && stored != null)
                    ctx.SetNodeType(stored, TypeName);
                return;
            }

            RuntimeHelpers.SetNestedProperty(obj, Path, v);
        }
    }

    public sealed class TextPropEntry : Entry
    {
        public string Key { get; init; } = "";
        public IIslCommand Value { get; init; } = default!;
        public string? TypeName { get; init; }
        public bool HasOptionalElseInlineIf { get; init; }

        public override void Apply(JsonObject obj, IOperationContext ctx)
        {
            var v = Value.EvaluateValue(ctx);
            if (v == null && HasOptionalElseInlineIf) return;
            obj[Key] = v?.DeepClone();
            if (TypeName != null && obj.TryGetPropertyValue(Key, out var stored) && stored != null)
                ctx.SetNodeType(stored, TypeName);
        }
    }

    public sealed class SpreadEntry : Entry
    {
        public IIslCommand Source { get; init; } = default!;
        public override void Apply(JsonObject obj, IOperationContext ctx)
        {
            var v = Source.EvaluateValue(ctx);
            if (v is JsonObject src)
                foreach (var kv in src) obj[kv.Key] = kv.Value?.DeepClone();
        }
    }

    public sealed class VarPropEntry : Entry
    {
        public string Name { get; init; } = "";
        public IIslCommand Value { get; init; } = default!;
        public override void Apply(JsonObject obj, IOperationContext ctx)
        {
            var v = Value.EvaluateValue(ctx);
            ctx.SetVariable(Name, v);
        }
    }

    private readonly Entry[] _entries;

    public ObjectBuildCommand(ObjectExpr source, IReadOnlyList<Entry> entries) : base(source)
    {
        _entries = entries is Entry[] arr ? arr : entries.ToArray();
    }

    public override CommandResult Execute(IOperationContext ctx) =>
        CommandResult.FromValue(EvaluateValue(ctx));

    public override JsonNode? EvaluateValue(IOperationContext ctx)
    {
        var obj = new JsonObject();
        var entries = _entries;
        for (int i = 0; i < entries.Length; i++)
            entries[i].Apply(obj, ctx);
        return obj;
    }
}
