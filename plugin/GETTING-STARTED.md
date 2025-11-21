# Getting Started with ISL Extension

Welcome! This guide will help you get the ISL Language Support extension up and running.

## Quick Start

### 1. Install Dependencies

```bash
cd plugin
npm install
```

### 2. Compile the Extension

```bash
npm run compile
```

### 3. Test the Extension

Open the `plugin` folder in VS Code and press **F5**. This will:
- Compile the TypeScript code
- Launch a new VS Code window with the extension loaded
- Allow you to test all features

### 4. Try It Out

In the Extension Development Host window:

1. Create a new file with `.isl` extension
2. Start typing ISL code - you'll see:
   - Syntax highlighting
   - Code completion
   - Real-time validation

Example ISL code to try:

```isl
// Simple transformation
fun run($input) {
    result: {
        id: $input.id | to.string,
        name: $input.name | upperCase,
        timestamp: @.Date.Now() | to.string("yyyy-MM-dd")
    }
}
```

## Features to Test

### 1. Syntax Highlighting
All ISL syntax should be properly colored:
- Keywords (fun, if, foreach)
- Variables ($variable)
- Strings and interpolation (`${...}`)
- Function calls (@.Service.Method())
- Comments (// and #)

### 2. Code Completion

Type and press `Ctrl+Space`:
- `fun` → function template
- `@.` → service completions
- `|` → modifier completions  
- `$` → variable completions

### 3. Validation

Try these to see validation:
```isl
// Unbalanced braces (error)
fun test() {
    result: "test"
// Missing }

// Unmatched control flow (error)
if ($x > 5)
    result: "high"
// Missing endif
```

### 4. Hover Documentation

Hover over:
- Keywords (fun, foreach, if)
- Services (@.Date, @.Math)
- Modifiers (|filter, |map)

### 5. Go to Definition

```isl
fun myFunction($input) {
    return $input
}

fun run($data) {
    // Ctrl+Click or F12 on myFunction to jump to definition
    result: @.This.myFunction($data)
}
```

### 6. Formatting

- Format document: `Shift+Alt+F`
- Format selection: Select code → `Shift+Alt+F`

### 7. Code Snippets

Type these prefixes and press Tab:
- `fun` → function template
- `foreach` → loop template
- `if` → conditional template
- `switch` → switch statement

### 8. Execute ISL (Requires ISL Runtime)

1. Save an ISL file
2. Right-click → "ISL: Run Transformation"
3. Enter input JSON when prompted
4. View output in new editor

## Next Steps

### For Extension Development

1. **Make Changes**: Edit files in `src/`
2. **Watch Mode**: Run `npm run watch` for auto-compilation
3. **Reload**: In Extension Development Host, press `Ctrl+R` to reload
4. **Debug**: Set breakpoints in TypeScript files

### For Publishing

1. **Update Icon**: Replace `images/icon.png` with a proper 128x128 PNG
2. **Update Publisher**: Set your publisher name in `package.json`
3. **Package**: Run `vsce package`
4. **Publish**: Run `vsce publish` (see PUBLISHING.md)

## Configuration

Configure the extension via Settings:

```json
{
    "isl.validation.enabled": true,
    "isl.formatting.enabled": true,
    "isl.formatting.indentSize": 4,
    "isl.execution.islCommand": "path/to/isl.sh"
}
```

## Troubleshooting

**Extension doesn't load:**
- Check for TypeScript compilation errors
- Run `npm run compile` and fix any errors

**Syntax highlighting broken:**
- Verify file extension is `.isl`
- Check `syntaxes/isl.tmLanguage.json` for errors

**Completion not working:**
- Check console for errors (Help → Toggle Developer Tools)
- Verify `activationEvents` in package.json

## Sample ISL Files

Test with files in the main ISL repository:
- `isl-cmd/examples/hello.isl`
- `isl-cmd/examples/transform.isl`
- `isl-transform/src/jmh/resources/*.isl`

## Resources

- **ISL Documentation**: https://intuit.github.io/isl/
- **VSCode Extension API**: https://code.visualstudio.com/api
- **Setup Guide**: See SETUP.md
- **Publishing Guide**: See PUBLISHING.md

## Support

For issues or questions:
- Check existing documentation
- Review ISL examples in the repository
- Test with sample ISL files
- Check the ISL documentation website

Happy coding with ISL! 🚀

