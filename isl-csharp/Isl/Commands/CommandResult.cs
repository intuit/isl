using System.Text.Json.Nodes;

namespace Isl.Commands;

/// <summary>
/// Result carried back from <see cref="IIslCommand.Execute"/>.
/// Mirrors the Kotlin <c>CommandResult</c>: most call sites only consume <see cref="Value"/>,
/// but statement-level commands use <see cref="PropertyPath"/>/<see cref="Append"/>/<see cref="ValidResult"/>
/// to drive object-building dispatch in <c>StatementsBuildCommand</c>.
/// </summary>
public readonly struct CommandResult
{
    public JsonNode? Value { get; init; }

    public IReadOnlyList<string>? PropertyPath { get; init; }

    public bool Append { get; init; }

    public bool ValidResult { get; init; }

    public bool IsReturn { get; init; }

    public static readonly CommandResult Null = new() { Value = null, ValidResult = false, Append = false };

    public static readonly CommandResult NullAppendFalse = new() { Value = null, ValidResult = false, Append = false };

    public static readonly CommandResult NullNotValid = new() { Value = null, ValidResult = false, Append = false };

    public static CommandResult FromValue(JsonNode? value) =>
        new() { Value = value, ValidResult = value != null, Append = true };

    public static CommandResult Property(IReadOnlyList<string> path, JsonNode? value, bool valid = true) =>
        new() { Value = value, PropertyPath = path, ValidResult = valid, Append = valid };

    public static CommandResult Return(JsonNode? value) =>
        new() { Value = value, IsReturn = true, ValidResult = value != null, Append = true };
}
