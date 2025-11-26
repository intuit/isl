# ISL Extension - Setup Guide

Guide for installing and setting up the ISL Language Support extension.

## For Users

### Installation

#### From Marketplace
1. Open VS Code or Cursor
2. Go to Extensions (Ctrl+Shift+X / Cmd+Shift+X)
3. Search for "ISL Language Support"
4. Click Install

#### Manual Installation
1. Download the `.vsix` file from releases
2. Open VS Code/Cursor
3. Extensions view → "..." menu → "Install from VSIX..."
4. Select the downloaded file

### Configuration

After installation, open Settings (Ctrl+,) and search for "ISL":

**Essential Settings:**
- `isl.execution.islCommand`: Path to `isl.sh` or `isl.bat`
- `isl.execution.javaHome`: Java installation path (if not in PATH)

**Optional Settings:**
- `isl.formatting.indentSize`: Spaces per indent level (default: 4)
- `isl.formatting.useTabs`: Use tabs instead of spaces
- `isl.validation.enabled`: Enable/disable validation

### ISL Runtime Setup

To execute ISL transformations, you need the ISL runtime:

1. **Install Java 11+**
   ```bash
   java -version  # Verify installation
   ```

2. **Get ISL Runtime**
   - Clone: https://github.com/intuit/isl
   - Build: `./gradlew build`
   - Or use pre-built `isl.sh` / `isl.bat`

3. **Configure Extension**
   - Set `isl.execution.islCommand` to path of ISL script
   - Example: `/path/to/isl/isl.sh`

### Verification

1. Open any `.isl` file
2. You should see:
   - Syntax highlighting
   - IntelliSense suggestions
   - Validation indicators
3. Test execution: Right-click → "ISL: Run Transformation"

## For Developers

### Prerequisites
- Node.js 18+
- TypeScript 5+
- VS Code or Cursor

### Development Setup

```bash
# Navigate to plugin directory
cd plugin

# Install dependencies
npm install

# Compile TypeScript
npm run compile

# Start watch mode (auto-recompile)
npm run watch
```

### Running Extension

1. Open `plugin` folder in VS Code
2. Press `F5` to launch Extension Development Host
3. Open a `.isl` file to test features

### Project Structure

```
plugin/
├── src/                      # TypeScript source
│   ├── extension.ts          # Entry point
│   ├── completion.ts         # Code completion
│   ├── validator.ts          # Validation
│   ├── formatter.ts          # Formatting
│   └── ...                   # Other providers
├── syntaxes/                 # Syntax highlighting
├── snippets/                 # Code snippets
└── package.json              # Extension manifest
```

### Building & Packaging

```bash
# Compile
npm run compile

# Package for distribution
npm install -g @vscode/vsce
vsce package
```

Creates `isl-language-support-X.X.X.vsix`

### Testing

**Feature Testing Checklist:**
- [ ] Syntax highlighting works
- [ ] Code completion (keywords, services, modifiers)
- [ ] Validation shows errors correctly
- [ ] Hover documentation appears
- [ ] Go to definition navigates correctly
- [ ] Formatting works
- [ ] ISL execution runs
- [ ] Snippets expand correctly
- [ ] Code actions appear
- [ ] Inlay hints show types

### Debugging

1. Set breakpoints in TypeScript files
2. Press `F5` to start debugger
3. Extension host opens with debugger attached
4. Check Debug Console for logs

## Troubleshooting

### Extension doesn't activate
- Check file has `.isl` extension
- View → Command Palette → "Change Language Mode" → ISL

### Syntax highlighting not working
- Restart VS Code
- Check language mode is set to ISL

### ISL execution fails
- Verify Java: `java -version`
- Check `isl.execution.islCommand` setting points to valid path
- View Output panel: View → Output → ISL

### Validation errors
- Check for actual syntax errors in code
- Disable temporarily: `isl.validation.enabled: false`
- Report false positives on GitHub

### Formatting issues
- Check `isl.formatting.indentSize` setting
- Try manual format: Shift+Alt+F
- Report issues with code sample

## Resources

- [Plugin Overview](PLUGIN-OVERVIEW.md) - Technical architecture
- [Publishing Guide](PUBLISHING.md) - How to publish
- [ISL Documentation](https://intuit.github.io/isl/)
- [VSCode Extension API](https://code.visualstudio.com/api)

## Contributing

1. Fork repository
2. Create feature branch
3. Make changes
4. Test thoroughly
5. Submit pull request

## License

Apache License 2.0 - See [LICENSE](LICENSE) file
