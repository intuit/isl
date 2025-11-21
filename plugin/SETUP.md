# ISL Extension - Setup Guide

This guide helps you set up the ISL Language Support extension for development and use.

## For Users

### Installation from Marketplace (Once Published)

1. Open VS Code or Cursor
2. Go to Extensions (Ctrl+Shift+X / Cmd+Shift+X)
3. Search for "ISL Language Support"
4. Click Install

### Manual Installation

1. Download the `.vsix` file
2. Open VS Code/Cursor
3. Go to Extensions view
4. Click "..." menu → "Install from VSIX..."
5. Select the downloaded file

### Configuration

After installation, configure the extension:

1. Open Settings (Ctrl+, / Cmd+,)
2. Search for "ISL"
3. Configure:
   - **ISL Command Path**: Set path to `isl.sh` or `isl.bat`
   - **Java Home**: Set JAVA_HOME if Java is not in PATH
   - **Formatting**: Adjust indentation preferences
   - **Validation**: Enable/disable validation features

## For Developers

### Prerequisites

- Node.js 18+ and npm
- TypeScript 5+
- VS Code or Cursor

### Development Setup

1. **Clone and Navigate**
   ```bash
   cd plugin
   ```

2. **Install Dependencies**
   ```bash
   npm install
   ```

3. **Compile TypeScript**
   ```bash
   npm run compile
   ```

4. **Watch Mode (for development)**
   ```bash
   npm run watch
   ```

5. **Run Extension**
   - Open the `plugin` folder in VS Code
   - Press `F5` to launch Extension Development Host
   - Open a `.isl` file to test

### Project Structure

```
plugin/
├── src/                      # TypeScript source files
│   ├── extension.ts          # Main extension entry point
│   ├── formatter.ts          # Code formatting
│   ├── validator.ts          # Syntax validation
│   ├── executor.ts           # ISL execution
│   ├── completion.ts         # Code completion provider
│   ├── hover.ts              # Hover documentation
│   └── definition.ts         # Go to definition
├── syntaxes/                 # Syntax highlighting
│   └── isl.tmLanguage.json   # TextMate grammar
├── snippets/                 # Code snippets
│   └── isl.json              # ISL snippets
├── images/                   # Assets
│   ├── icon.png              # Extension icon
│   └── file-icon.svg         # File icon
├── package.json              # Extension manifest
├── language-configuration.json # Language config
└── tsconfig.json             # TypeScript config
```

### Building

```bash
npm run compile
```

### Packaging

```bash
npm install -g @vscode/vsce
vsce package
```

This creates `isl-language-support-<version>.vsix`

### Testing

1. **Manual Testing**
   - Press F5 in VS Code
   - Test all features with sample `.isl` files

2. **Test Files**
   - Use files from `../isl-cmd/examples/`
   - Create edge case test files

3. **Feature Testing Checklist**
   - [ ] Syntax highlighting
   - [ ] Code completion (keywords, services, modifiers)
   - [ ] Validation (errors shown correctly)
   - [ ] Hover documentation
   - [ ] Go to definition
   - [ ] Formatting
   - [ ] ISL execution
   - [ ] Snippets

### Debugging

1. Set breakpoints in TypeScript files
2. Press F5 to start debugging
3. Extension host opens with debugger attached
4. View Debug Console for logs

### Common Development Tasks

**Add new keyword:**
1. Add to `syntaxes/isl.tmLanguage.json`
2. Add to `src/completion.ts`
3. Add to `src/hover.ts` if it needs documentation

**Add new modifier:**
1. Add to completion list in `src/completion.ts`
2. Add hover documentation in `src/hover.ts`

**Add new snippet:**
1. Edit `snippets/isl.json`
2. Follow VSCode snippet format

## ISL Runtime Setup

To execute ISL transformations:

### Option 1: Use Project ISL Runtime

If you're in the ISL repository:
- Windows: Use `isl.bat` in project root
- Unix/Mac: Use `isl.sh` in project root

### Option 2: Install ISL Separately

1. Clone ISL repository:
   ```bash
   git clone https://github.com/intuit/isl.git
   cd isl
   ```

2. Build ISL:
   ```bash
   ./gradlew build
   ```

3. Configure extension to use ISL:
   - Set `isl.execution.islCommand` to full path of `isl.sh` or `isl.bat`

### Option 3: Use Published ISL CLI

Once ISL CLI is published to package managers, you can install it globally:
```bash
npm install -g isl-cli  # (example, if published)
```

## Troubleshooting

### Extension doesn't activate
- Check file extension is `.isl`
- Verify `activationEvents` in package.json

### Syntax highlighting not working
- Check file is recognized as ISL language
- View → Command Palette → "Change Language Mode" → ISL

### ISL execution fails
- Verify Java is installed: `java -version`
- Check `isl.execution.islCommand` setting
- Check `isl.execution.javaHome` setting
- View Output panel (View → Output → ISL)

### Validation errors
- Disable validation temporarily: `isl.validation.enabled: false`
- Check for legitimate syntax errors
- Report false positives as issues

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## Resources

- [VSCode Extension API](https://code.visualstudio.com/api)
- [TextMate Grammar Guide](https://macromates.com/manual/en/language_grammars)
- [ISL Documentation](https://intuit.github.io/isl/)

## License

Apache License 2.0 - See LICENSE file

