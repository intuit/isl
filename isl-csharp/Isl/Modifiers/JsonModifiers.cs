using System.Text.Json.Nodes;

namespace Isl.Modifiers;

public static class JsonModifiers
{
    public static JsonNode? Apply(JsonNode? val, JsonNode?[] args)
    {
        if (val == null) return null;
        return JsonValue.Create(val.ToJsonString());
    }
}
