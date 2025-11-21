# ISL Language Support for VS Code & Cursor

Comprehensive language support for ISL (Intuitive Scripting Language) - a powerful JSON transformation scripting language.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Features

### 🎨 Syntax Highlighting
- Full syntax highlighting for ISL files (.isl)
- Support for all ISL constructs: functions, modifiers, variables, interpolations, etc.
- Highlighting for string interpolation `${...}`, math expressions `{{...}}`, and function calls `@.Service.Method()`

### 📝 IntelliSense & Code Completion
- Smart code completion for:
  - Keywords (fun, foreach, if, switch, etc.)
  - Service calls (@.Date, @.Math, @.This, etc.)
  - Modifiers (|filter, |map, |trim, |upperCase, etc.)
  - Variables (with automatic discovery from your code)
- Snippet support for common patterns

### 🔍 Validation & Linting
- Real-time syntax validation
- Error detection for:
  - Unbalanced braces, brackets, and parentheses
  - Unclosed control flow statements (if/endif, foreach/endfor, etc.)
  - Invalid variable declarations
  - Mismatched string interpolations
- Configurable validation rules

### 📖 Documentation on Hover
- Hover over keywords, functions, and modifiers to see documentation
- Quick reference for ISL syntax and built-in functions
- Examples and usage hints

### 🎯 Go to Definition
- Jump to function definitions with F12
- Navigate to type declarations
- Follow import statements to their source files

### ✨ Code Formatting
- Automatic code formatting with customizable indentation
- Format on save support
- Range formatting support

### ▶️ Run & Execute ISL
- Run ISL transformations directly from VS Code
- Test with inline JSON input or external JSON files
- View formatted output side-by-side
- Integrated output panel for debugging

### 📋 Code Snippets
Ready-to-use snippets for:
- Functions and modifiers
- Control flow (if, foreach, while, switch)
- Date operations
- Array transformations
- And more!

## Getting Started

1. Install the extension
2. Open or create a `.isl` file
3. Start coding with full language support!

### Example ISL Code

```isl
// Transform customer data
fun run($input) {
    // Extract and transform fields
    $customer = {
        id: $input.customer_id | to.string,
        name: `${$input.first_name} ${$input.last_name}` | trim,
        email: $input.email | lowerCase,
        joinDate: $input.created_at | date.parse("yyyy-MM-dd") | to.string("MM/dd/yyyy")
    };
    
    // Process orders
    orders: foreach $order in $input.orders | filter($order.status == "completed")
        {
            orderId: $order.id,
            total: {{ $order.subtotal + $order.tax }},
            items: $order.line_items | map($item.name)
        }
    endfor,
    
    customer: $customer,
    processed: @.Date.Now() | to.string("yyyy-MM-dd HH:mm:ss")
}
```

## Requirements

To execute ISL transformations, you need:
- Java Runtime Environment (JRE) 11 or later
- ISL runtime (from the [ISL repository](https://github.com/intuit/isl))

**Note for Publishers**: The extension icon has been set to the official ISL logo. If the dimensions need adjustment for marketplace requirements (128x128), see `ICON-SETUP.md` for resize instructions.

## Extension Settings

This extension contributes the following settings:

* `isl.validation.enabled`: Enable/disable ISL validation (default: true)
* `isl.formatting.enabled`: Enable/disable ISL formatting (default: true)
* `isl.formatting.indentSize`: Number of spaces for indentation (default: 4)
* `isl.formatting.useTabs`: Use tabs instead of spaces (default: false)
* `isl.linting.enabled`: Enable/disable ISL linting (default: true)
* `isl.execution.islCommand`: Path to ISL command (default: "isl")
* `isl.execution.javaHome`: Path to Java home directory (optional)

## Commands

* `ISL: Validate Current File` - Validate the current ISL file
* `ISL: Run Transformation` - Run ISL transformation with inline input
* `ISL: Run Transformation with Input File` - Run ISL transformation with a JSON file
* `ISL: Format Document` - Format the current document
* `ISL: Open Documentation` - Open ISL documentation in browser

## Keyboard Shortcuts

* `Shift+Alt+F` - Format document
* `F12` - Go to definition
* `Ctrl+Space` - Trigger code completion

## Known Issues

- Complex nested string interpolations may have limited highlighting
- Validation is syntax-based; runtime errors are only caught during execution

## Release Notes

### 1.0.0

Initial release of ISL Language Support:
- Syntax highlighting
- Code completion
- Validation and linting
- Hover documentation
- Go to definition
- Code formatting
- ISL execution support
- Comprehensive snippets

## Contributing

Found a bug or have a feature request? Please file an issue on our [GitHub repository](https://github.com/intuit/isl).

## Resources

- [ISL Documentation](https://intuit.github.io/isl/)
- [ISL GitHub Repository](https://github.com/intuit/isl)
- [Quick Start Guide](https://intuit.github.io/isl/quickstart/)
- [Language Reference](https://intuit.github.io/isl/dsl/)

## License

This extension is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

---

**Enjoy using ISL!** 🚀

