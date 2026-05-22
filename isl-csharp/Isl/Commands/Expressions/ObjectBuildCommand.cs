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
            var v = Value.Execute(ctx).Value;
            if (v == null && HasOptionalElseInlineIf) return;
            RuntimeHelpers.SetNestedProperty(obj, Path, v);
            if (TypeName != null && Path.Count == 1 &&
                obj.TryGetPropertyValue(Path[0], out var stored) && stored != null)
            {
                ctx.SetNodeType(stored, TypeName);
            }
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
            var v = Value.Execute(ctx).Value;
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
            var v = Source.Execute(ctx).Value;
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
            var v = Value.Execute(ctx).Value;
            ctx.SetVariable(Name, v);
        }
    }

    private readonly IReadOnlyList<Entry> _entries;

    public ObjectBuildCommand(ObjectExpr source, IReadOnlyList<Entry> entries) : base(source)
    {
        _entries = entries;
    }

    public override CommandResult Execute(IOperationContext ctx)
    {
        var obj = new JsonObject();
        for (int i = 0; i < _entries.Count; i++)
            _entries[i].Apply(obj, ctx);
        return CommandResult.FromValue(obj);
    }
}
