# ISL Extensions

The ISL Language Support extension allows you to define custom functions and modifiers specific to your project using a `.islextensions` file. This enables the plugin to provide IntelliSense, hover documentation, and validation for your project-specific ISL extensions.

## How It Works

Similar to how `.cspell.json` defines custom dictionary words or `.cursorrules` defines AI assistant rules, you can create a `.islextensions` file in your project root to define custom ISL functions and modifiers.

When you open an ISL file, the extension automatically:
1. Looks for a `.islextensions` file in the workspace root (highest priority)
2. If not found, loads from global source (URL or file path) if configured
3. Loads and parses the custom definitions
4. Integrates them into autocomplete, hover documentation, and validation
5. Watches for changes and automatically reloads when the file is updated

### Loading Priority

The extension loads extensions in the following order (first match wins):

1. **Workspace-local `.islextensions`** - Project-specific extensions that override global ones
2. **Global source** - Shared extensions from URL or file path (configured in settings)
3. **Empty** - No custom extensions

This allows you to:
- Share common extensions across all projects via a global source
- Override or extend with project-specific extensions when needed
- Keep projects in sync without duplicating the file in each project

## File Format

The `.islextensions` file uses JSON format with the following structure:

```json
{
  "functions": [
    {
      "name": "functionName",
      "description": "What the function does",
      "parameters": [
        {
          "name": "param1",
          "type": "String",
          "description": "Parameter description",
          "optional": false,
          "defaultValue": "default"
        }
      ],
      "returns": {
        "type": "Object",
        "description": "What the function returns"
      },
      "examples": [
        "$result: @.This.functionName($param1);"
      ]
    }
  ],
  "modifiers": [
    {
      "name": "modifierName",
      "description": "What the modifier does",
      "parameters": [
        {
          "name": "param1",
          "type": "String",
          "optional": true,
          "defaultValue": "default",
          "description": "Parameter description"
        }
      ],
      "returns": {
        "type": "String",
        "description": "What the modifier returns"
      },
      "examples": [
        "$result: $input | modifierName;",
        "$result: $input | modifierName(\"value\");"
      ]
    }
  ]
}
```

## Field Reference

### Function Definition

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | The function name (used in `@.This.functionName()`) |
| `description` | string | No | Description shown in hover and autocomplete |
| `parameters` | array | No | Array of parameter definitions |
| `returns` | object | No | Return type and description |
| `examples` | array | No | Array of usage examples |

### Modifier Definition

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | The modifier name (used with `\| modifierName`) |
| `description` | string | No | Description shown in hover and autocomplete |
| `parameters` | array | No | Array of parameter definitions |
| `returns` | object | No | Return type and description |
| `examples` | array | No | Array of usage examples |

### Parameter Definition

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Parameter name |
| `type` | string | No | Parameter type (String, Number, Boolean, Array, Object, etc.) |
| `description` | string | No | Parameter description |
| `optional` | boolean | No | Whether the parameter is optional (default: false) |
| `defaultValue` | string | No | Default value if not provided |

### Returns Definition

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | string | No | Return type |
| `description` | string | No | Description of what is returned |

## Example

Here's a complete example for a project with custom email and database functions:

```json
{
  "functions": [
    {
      "name": "sendEmail",
      "description": "Sends an email using the custom email service",
      "parameters": [
        {
          "name": "to",
          "type": "String",
          "description": "Email recipient address"
        },
        {
          "name": "subject",
          "type": "String",
          "description": "Email subject line"
        },
        {
          "name": "body",
          "type": "String",
          "description": "Email body content"
        }
      ],
      "returns": {
        "type": "Object",
        "description": "Result with success status"
      },
      "examples": [
        "$result: @.This.sendEmail($user.email, \"Welcome\", `Hello ${$user.name}!`);"
      ]
    }
  ],
  "modifiers": [
    {
      "name": "formatPhone",
      "description": "Formats a phone number to standard format",
      "parameters": [
        {
          "name": "format",
          "type": "String",
          "optional": true,
          "defaultValue": "US",
          "description": "Format style (US, INTERNATIONAL, E164)"
        }
      ],
      "returns": {
        "type": "String",
        "description": "Formatted phone number"
      },
      "examples": [
        "$phone: $user.phone | formatPhone;",
        "$phone: $user.phone | formatPhone(\"INTERNATIONAL\");"
      ]
    }
  ]
}
```

## Features

Once you define your extensions, you get:

### 1. IntelliSense / Autocomplete
- Type `@.This.` to see your custom functions
- Type `|` to see your custom modifiers
- Full parameter hints and descriptions

### 2. Hover Documentation
- Hover over custom function or modifier names to see:
  - Full signature with parameter types
  - Description
  - Parameter details
  - Return type
  - Usage examples
  - Note indicating it's from `.islextensions`

### 3. Validation
- No warnings for using your custom functions/modifiers
- The validator recognizes them as valid
- Works alongside built-in ISL functions/modifiers

### 4. Auto-reload
- Changes to `.islextensions` are automatically detected
- Extension reloads definitions without restart
- All open ISL files are revalidated

## Global Extensions Source

To avoid maintaining `.islextensions` files in every project, you can configure a global source that all projects will use automatically.

### Configuration

Open VS Code Settings (Ctrl+, / Cmd+,) and search for "ISL extensions source", or add to your `settings.json`:

```json
{
  "isl.extensions.source": "https://example.com/shared-extensions.json",
  "isl.extensions.cacheTTL": 3600
}
```

### Options

- **URL**: Download from a web URL (e.g., `https://your-company.com/isl-extensions.json`)
  - Content is cached for the duration specified by `isl.extensions.cacheTTL` (default: 1 hour)
  - Automatically re-downloads when cache expires
  - Falls back to cached content if download fails

- **File Path**: Load from a local file
  - Absolute path: `/path/to/extensions.json` or `C:\path\to\extensions.json`
  - Relative to home: `~/isl-extensions.json` or `~/.config/isl/extensions.json`
  - Relative paths are resolved from your home directory

### Use Cases

**Team/Company Shared Extensions:**
```json
{
  "isl.extensions.source": "https://your-company.github.io/isl-extensions/shared.json"
}
```

**Local Shared File:**
```json
{
  "isl.extensions.source": "~/isl-extensions.json"
}
```

**Project Override:**
- Create a `.islextensions` file in your project root
- It will automatically override the global source for that project
- Perfect for project-specific extensions while still using shared ones

### Cache Management

- URL downloads are cached in memory for performance
- Cache duration is controlled by `isl.extensions.cacheTTL` (in seconds)
- Cache is cleared when configuration changes
- If download fails, expired cache is used as fallback

## Best Practices

1. **Keep it organized**: Group related functions and modifiers together
2. **Document thoroughly**: Add descriptions and examples - they appear in IDE tooltips
3. **Use types**: Specify parameter and return types for better IntelliSense
4. **Share globally**: Use `isl.extensions.source` to share extensions across all projects
5. **Project overrides**: Use workspace-local `.islextensions` for project-specific extensions
6. **Version control**: Commit `.islextensions` to your repository for project-specific extensions
7. **Validate JSON**: Use a JSON validator to ensure your file is valid before saving

## Troubleshooting

### Extensions not showing up
- Check that `.islextensions` is in the workspace root (if using workspace-local)
- Verify global source is configured correctly in settings (if using global source)
- Check that URL is accessible and returns valid JSON (if using URL)
- Verify file path exists and is readable (if using file path)
- Verify JSON syntax is valid (use a JSON validator)
- Check Output panel (View > Output, select "ISL Language Support") for error messages
- Reload VS Code window if needed

### Global source not loading
- Verify the URL is accessible (try opening in browser)
- Check network connectivity if using URL
- Verify file path is correct and file exists if using file path
- Check cache TTL setting - may need to wait for cache to expire
- Check Output panel for detailed error messages

### Validation errors
- Ensure `name` field is present for all functions/modifiers
- Check that parameter names are valid identifiers
- Verify no duplicate names

### Changes not applying
- The extension watches for file changes automatically
- If changes don't apply, try reloading the window
- Check the Output panel (View > Output, select "ISL Language Support") for error messages

## Schema Support

For better editing experience, you can add JSON schema validation by adding this at the top of your `.islextensions` file:

```json
{
  "$schema": "https://json-schema.org/draft-07/schema#",
  "functions": [
    ...
  ]
}
```

(Note: A full schema definition will be provided in future versions)

## Examples by Use Case

### Custom Database Functions
```json
{
  "functions": [
    {
      "name": "queryDB",
      "description": "Executes a database query",
      "parameters": [
        {"name": "sql", "type": "String"},
        {"name": "params", "type": "Array", "optional": true}
      ],
      "returns": {"type": "Array"},
      "examples": ["$users: @.This.queryDB(\"SELECT * FROM users\");"]
    }
  ]
}
```

### Custom Validation Modifiers
```json
{
  "modifiers": [
    {
      "name": "validateEmail",
      "description": "Validates email format",
      "parameters": [],
      "returns": {"type": "Boolean"},
      "examples": ["if ($email | validateEmail) ... endif"]
    }
  ]
}
```

### Custom Formatting Modifiers
```json
{
  "modifiers": [
    {
      "name": "toCurrency",
      "description": "Formats number as currency",
      "parameters": [
        {
          "name": "currency",
          "type": "String",
          "optional": true,
          "defaultValue": "USD"
        }
      ],
      "returns": {"type": "String"},
      "examples": [
        "$price: $amount | toCurrency;",
        "$price: $amount | toCurrency(\"EUR\");"
      ]
    }
  ]
}
```

## Future Enhancements

Planned improvements:
- JSON Schema for `.islextensions` validation
- Support for importing external extension definitions
- Multiple extension files support
- Extension marketplace/sharing
- Auto-generation from Java/Kotlin extension implementations

