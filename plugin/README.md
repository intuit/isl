# ISL Language Support for VS Code & Cursor

Comprehensive language support for ISL [(Intuitive Scripting Language)](https://intuit.github.io/isl/) - a powerful JSON transformation scripting language.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## What is ISL

ISL is a low-code [interpreted scripting language](<https://en.wikipedia.org/wiki/Interpreter_(computing)>) and runtime container designed to provide developers and non-developers an easy way to write, test, and deploy user developed code inside any service.

The ISL supports an intuitive simplified syntax with features that make data acquisition and data transformations easy with minimal lines of code. In addition, the language supports easy extensibility allowing it to be used as a multi-purpose service extensibility language.

The ISL can be embedded in any JVM based project to provide runtime based extensibility through a fast and lightweight runtime.

## Overview

In the most simple form the ISL is a JSON transformation language:

Given Input JSON:
```json
{
	"title": "IPod Nano - 8GB",
	"body_html": "It's the small iPod with a big idea: Video.",
	"id": 632910392,
	"images": [
		{
			"id": 850703190,
			"src": "http://example.com/burton.jpg",
		}
	],
	"options": {
		"name": "Color",
		"values": [
			"Pink",
			"Red",
			"Green",
			"Black"
		]
	},
	"status": "active",
	"tags": "Emotive, Flash Memory, MP3, Music",
	"updated_at": 1645004735,
	"vendor": "Apple"
}
```

And Transformation:
```isl
fun transform( $input ){
    return {
      // Simple JSON Path Selectors
      id: $input.id,
      // piped modifiers using `|`
      name: $input.title | trim,
      // easy string building using interpolation ` ... `
      short_description: `${ $input.title } by ${ $input.vendor }`,
      // child object building
      primary_image: {
          id: $input.images[0].id,
          url: $input.images[0].src
      },
      // conditional properties
      is_active: if( $input.status == "active" ) true else false,
      option_name: $input.options.name,
      // array to csv
      option_values: $input.options.values | join(','),
      // date processing
      updated: $input.updated_at | date.fromEpochSeconds | to.string("yyyy-MM-dd HH:mm")
    }
}
```

Will output:
```json
{
	"id": 632910392,
	"name": "IPod Nano - 8GB",
	"short_description": "IPod Nano - 8GB by Apple",
	"primary_image": {
		"id": 850703190,
		"url": "http://example.com/burton.jpg"
	},
	"is_active": true,
	"option_name": "Color",
	"option_values": "Pink,Red,Green,Black",
	"updated": "2022-02-47 09:45"
}
```


## Features

### 🎨 Syntax Highlighting
- Complete syntax highlighting for all ISL constructs
- String interpolation: `${variable}`, `{{math}}`, `@.Function()`
- Modifiers, operators, control flow, and more

### 🔧 Custom Extensions Support
**NEW!** Define your own project-specific functions and modifiers:
- Create a `.islextensions` file in your workspace root
- **Or configure a global source** (URL or file path) shared across all projects
- Define custom functions and modifiers with full type information
- Get IntelliSense, hover documentation, and validation for your extensions
- Auto-reload when definitions change
- Workspace-local files override global source for project-specific extensions
- [Learn more about ISL Extensions](docs/EXTENSIONS.md)

**Built-in definitions:** All built-in functions and modifiers (e.g. `date.fromEpochSeconds`, `Math.sum`, `trim`) are defined in **`isl-language.json`** at the plugin root. Edit that file to add or change built-ins; completion, hover, signatures, and validation all use it as the single source of truth.

### 💡 IntelliSense & Code Completion
Smart completion for:
- **Keywords**: `fun`, `foreach`, `if`, `switch`, etc.
- **Services**: `@.Date`, `@.Math`, `@.String`, etc.
- **Modifiers**: `|filter`, `|map`, `|trim`, `|upperCase`, 50+ more
- **Variables**: Automatic discovery from your code

### ✅ Validation & Linting
Real-time error detection:
- Balanced braces, brackets, parentheses
- Control flow matching (`if`/`endif`, `foreach`/`endfor`, etc.)
- Undefined functions and modifiers
- Undeclared variable usage
- Invalid syntax and semantic errors

### 📖 Hover Documentation
Hover over any element for:
- Keyword syntax and usage
- Service method descriptions
- Modifier documentation with examples
- Variable type information

### 🔧 Code Actions & Quick Fixes
- Simplify string interpolation (`${$var}` → `$var`)
- Convert `:` to `=` for variable assignments
- Format long objects onto multiple lines
- Replace `default()` with `??` operator

### ✨ Smart Formatting
- Automatic indentation and spacing
- Parameter spacing normalization
- Modifier chain alignment
- Multi-line string preservation
- Format on save support

### 🎯 Signature Help & Type Hints
- Parameter hints for functions and modifiers
- Inline type annotations for variables
- CodeLens actions (test functions, find usages)

### ▶️ Execute ISL
- Run transformations directly from editor
- Test with inline JSON or external files
- View formatted output side-by-side
- Integrated error reporting

### 📋 Code Snippets
20+ ready-to-use snippets for:
- Functions and modifiers
- Control flow patterns
- Array and object transformations
- Date operations
- Error handling

## Quick Start

1. Install the extension
2. Open or create a `.isl` file
3. Start coding with full language support!

### Defining Custom Extensions

Want IntelliSense for your project-specific functions and modifiers?

**Option 1: Project-specific (workspace-local)**
1. Create a `.islextensions` file in your workspace root
2. Define your custom functions and modifiers in JSON format
3. Enjoy full IDE support for your extensions!

**Option 2: Global source (shared across projects)**
1. Configure `isl.extensions.source` in VS Code settings
2. Set to a URL (e.g., `https://example.com/extensions.json`) or file path
3. All projects automatically use these extensions
4. Override with workspace-local `.islextensions` when needed

**Example `.islextensions`:**
```json
{
  "functions": [
    {
      "name": "sendEmail",
      "description": "Sends an email via custom service",
      "parameters": [
        {"name": "to", "type": "String"},
        {"name": "subject", "type": "String"},
        {"name": "body", "type": "String"}
      ]
    }
  ],
  "modifiers": [
    {
      "name": "formatPhone",
      "description": "Formats phone numbers",
      "parameters": [
        {"name": "format", "type": "String", "optional": true}
      ]
    }
  ]
}
```

See [ISL Extensions Documentation](docs/EXTENSIONS.md) for complete details.

### Example

```isl
fun run($input) {
    $customers = foreach $customer in $input.customers
        {
            id: $customer.id | to.string,
            name: `${$customer.first} ${$customer.last}` | trim,
            email: $customer.email | lowerCase,
            orders: $customer.orders | filter($order.status == "completed")
        }
    endfor
    
    return {
        customers: $customers,
        total: $customers | length,
        processed: @.Date.Now() | to.string("yyyy-MM-dd")
    }
}
```

## Configuration

Available settings (all prefixed with `isl.`):

```json
{
  "isl.validation.enabled": true,
  "isl.formatting.enabled": true,
  "isl.formatting.indentSize": 4,
  "isl.formatting.useTabs": false,
  "isl.formatting.alignProperties": false,
  "isl.execution.islCommand": "isl",
  "isl.execution.javaHome": "",
  "isl.extensions.source": "",
  "isl.extensions.cacheTTL": 3600
}
```

**Extension Settings:**
- `isl.extensions.source`: Global source for `.islextensions` (URL or file path). Workspace-local files take precedence.
- `isl.extensions.cacheTTL`: Cache TTL in seconds for URL-based extensions (default: 3600 = 1 hour).

## Commands

- **ISL: Validate Current File** - Run validation
- **ISL: Run Transformation** - Execute with inline input
- **ISL: Run Transformation with Input File** - Execute with JSON file
- **ISL: Format Document** - Format code
- **ISL: Open Documentation** - Open ISL docs

## Windsurf Troubleshooting

If the extension doesn't work in Windsurf (commands not found, no Output panel):

1. **Check Developer Tools** – Open **Help → Toggle Developer Tools** (or **Developer: Toggle Developer Tools** from Command Palette). Check the Console tab for errors when loading the extension or when activating it.

2. **Check Extension Host** – In the Output panel, select **Extension Host** from the dropdown. Look for activation errors or stack traces.

3. **Verify installation** – Ensure you're using Windsurf 1.89+ if required. Reinstall the extension: uninstall, then install from the `.vsix` file again.

4. **Activation errors** – If activation fails, the extension now shows an error message. Check **Output → ISL Language Support** for details.

## Requirements

To execute ISL transformations:
- Java Runtime Environment (JRE) 11+
- The extension bundles an embedded ISL CLI (`plugin/lib/isl-cmd-all.jar`) — no separate install needed

### Updating the Embedded ISL Runtime

When developing the extension with ISL changes, rebuild and copy the fat JAR:

```bash
# From the repository root
./gradlew copyIslToPlugin
```

See [lib/README.md](lib/README.md) for details.

## 🤖 AI Assistant Support

This extension includes AI configuration for Cursor, Windsurf, GitHub Copilot, and other AI editors. Your AI assistant automatically understands ISL syntax and can help write transformations.

Ask your AI to:
- Generate ISL transformations
- Explain ISL syntax and modifiers
- Convert data logic to ISL
- Debug and optimize code

## Resources

- [ISL Documentation](https://intuit.github.io/isl/)
- [ISL GitHub Repository](https://github.com/intuit/isl)
- [Language Reference](https://intuit.github.io/isl/dsl/)
- [Quick Start Guide](https://intuit.github.io/isl/quickstart/)
- [ISL Extensions Guide](docs/EXTENSIONS.md) - Define custom functions and modifiers
- [Example `.islextensions` file](.islextensions.example)

## Release Notes

### 1.1.0

**Major improvements:**
- Signature help and inlay hints
- Code actions and quick fixes
- Enhanced formatter (parameter spacing, nested control flow)
- Semantic validation (undefined functions/modifiers, variable tracking)
- 20+ new code snippets
- Better control flow balance detection
- Multi-line string preservation

### 1.0.0

Initial release with syntax highlighting, completion, validation, formatting, and execution support.

See [CHANGELOG.md](CHANGELOG.md) for full details.

## Contributing

Found a bug or have a feature request? File an issue on [GitHub](https://github.com/intuit/isl).

## License

Apache License 2.0 - See [LICENSE](LICENSE) file.

---

**Enjoy using ISL!** 🚀
