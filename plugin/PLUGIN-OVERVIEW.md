# ISL VSCode Plugin - Technical Overview

## Architecture

A TypeScript-based VSCode extension providing comprehensive language support for ISL (Intuitive Scripting Language).

### Core Components

```
plugin/
├── src/
│   ├── extension.ts              # Activation, commands, provider wiring
│   ├── completion.ts             # IntelliSense (ISL + imported extensions)
│   ├── islYamlTestsCompletion.ts # Completion for *.tests.yaml suites
│   ├── hover.ts
│   ├── definition.ts
│   ├── formatter.ts
│   ├── validator.ts              # Diagnostics (semantic + extensions/types)
│   ├── executor.ts               # Run / compile via embedded or external CLI
│   ├── codelens.ts               # Run, test, debug, usages
│   ├── yamlTestsCodeLens.ts      # CodeLens on YAML test entries
│   ├── inlayhints.ts
│   ├── signature.ts
│   ├── codeactions.ts            # Quick fixes & refactors
│   ├── highlights.ts             # Document highlight for control-flow pairs
│   ├── islPasteProvider.ts       # Paste edits (avoid bad mid-line indentation)
│   ├── extensions.ts             # .islextensions + global source
│   ├── types.ts                  # Type/schema integration for validation
│   ├── language.ts               # Built-in definitions (isl-language.json)
│   ├── islImports.ts             # Import resolution helpers
│   ├── testExplorer.ts           # VS Code Testing API (ISL + YAML suites)
│   ├── diffViewer.ts             # Expected vs actual for failed tests
│   └── debugAdapter.ts           # DAP descriptor + launch defaults
│
├── lib/
│   ├── isl-cmd-all.jar         # Embedded CLI (transform, test, validate)
│   ├── isl-debug-adapter-all.jar
│   ├── isl.bat / isl.sh
│
├── syntaxes/
│   ├── isl.tmLanguage.json
│   └── isl-markdown-injection.tmLanguage.json  # ISL in Markdown fences
│
└── snippets/
    └── isl.json
```

## Implementation Details

### Provider Pattern
Each language feature is implemented as a separate provider:
- **CompletionItemProvider**: Context-aware ISL completion; separate provider for `*.tests.yaml` suite authoring
- **HoverProvider**: Documentation on hover
- **DefinitionProvider**: Navigate to declarations
- **DocumentFormattingProvider** / **DocumentRangeFormattingProvider**: Smart code formatting
- **Validator** (diagnostics collection): Real-time validation with debouncing
- **CodeLensProvider**: Inline run / test / debug / usages on `.isl`; YAML suite entries on `*.tests.yaml`
- **InlayHintsProvider**: Type annotations
- **SignatureHelpProvider**: Parameter hints
- **CodeActionProvider**: Quick fixes and refactoring
- **DocumentHighlightProvider**: Matching control-flow keywords
- **DocumentPasteEditProvider**: Plain-text paste behavior tuned for ISL strings
- **TestController** (Testing API): Discovers and runs tests under `**/tests/**/*.isl` (`@test` / `@setup`) and `islTests` entries in `*.tests.yaml`
- **DebugAdapterDescriptorFactory** / **DebugConfigurationProvider**: Spawns `java -jar isl-debug-adapter-all.jar` for the `isl` debug type
- **TerminalProfileProvider**: “ISL” profile with `lib/` on `PATH` for bundled `isl.bat` / `isl.sh`

### Syntax Highlighting
- **Technology**: TextMate grammars (JSON)
- **Scopes**: 50+ token types for precise highlighting in `.isl`
- **Features**: String interpolation, nested structures, modifiers
- **Markdown**: Injected grammar highlights ISL inside fenced code blocks in Markdown

### Validation Engine
- **Approach**: Real-time, incremental validation
- **Debouncing**: 500ms delay on typing
- **Checks**: Syntax errors, semantic validation, control flow balance
- **Diagnostics**: Error, Warning, Information, Hint levels

### Formatter
- **Strategy**: Multi-pass processing
  1. Normalize spacing (pipes, parameters, assignments)
  2. Calculate indentation (control flow, nesting, continuations)
  3. Preserve literals (multi-line strings, comments)
- **Features**:
  - Smart indentation for nested control flow
  - Modifier chain alignment
  - Parameter spacing normalization
  - Multi-line string preservation

## Performance Optimizations

- **Debounced Validation**: Reduces CPU during typing
- **Incremental Updates**: Only revalidates changed documents
- **Debounced CodeLens refresh**: Limits refresh churn on large edits
- **Efficient Parsing**: Optimized regex patterns
- **Async Execution**: Non-blocking ISL runtime calls

## Configuration System

All settings prefixed with `isl.`:
- `validation.*` - Validation behavior
- `formatting.*` - Formatting preferences
- `execution.*` - Java / CLI (`isl-cmd-all.jar` by default, optional external `islCommand`)
- `linting.*` - Linting rules
- `naming.convention` - Function / modifier naming style hints
- `extensions.source` / `extensions.cacheTTL` - Global `.islextensions` URL or file path and URL cache TTL

## Extension Lifecycle

1. **Activation**: Workspace contains `*.isl` or `*.tests.yaml`, or the ISL terminal profile is opened
2. **Registration**: Providers registered with VSCode
3. **Events**: Document change listeners attached
4. **Deactivation**: Cleanup on extension unload

## Technologies

- **TypeScript 5.0**: Type-safe implementation
- **VS Code API 1.75+**: Extension framework
- **TextMate**: Syntax highlighting
- **Node.js**: Runtime environment

## Key Features in v1.3.0

### Debugging (Debug Adapter Protocol)
- **`isl` debug type** with launch configs: `script`, optional `input` JSON file, `function` to run (default `run`)
- **YAML suite debugging**: `yamlSuite` + `yamlTestIndex` apply the same mocks and inputs as the CLI test runner
- **Breakpoints** in `.isl`; **Debug Function** from CodeLens; **Debug YAML Test** from YAML CodeLens / command
- **Embedded adapter**: `lib/isl-debug-adapter-all.jar` launched with Java (same `isl.execution.javaHome` story as the CLI)

### Testing & quality loops
- **Native Test Explorer** integration for `**/tests/**/*.isl` (`@test` / `@setup`) and for `*.tests.yaml` suites (`islTests` list)
- **Run All Tests in File** (editor title when the file is a test script or YAML suite)
- **Side-by-side diff** for expected vs actual on failures; **Add mock from test error** flow for service mocks
- **Dedicated output**: “ISL Tests” channel with ANSI-friendly run output

### YAML test authoring
- **Completion** on `*.tests.yaml` (structure, options, function names)
- **CodeLens** on individual suite entries (run / debug)

### Runtime & terminal
- **Embedded CLI** `lib/isl-cmd-all.jar` for transform, validate, and test when present (no separate ISL install required)
- **Optional** `isl.execution.islCommand` override when the embedded JAR is not shipped
- **ISL terminal profile**: preconfigured shell with `lib/` on `PATH` for `isl.bat` / `isl.sh`

### Extensions & types
- **Workspace** `.islextensions` plus **global** `isl.extensions.source` (URL or absolute path) with **cache TTL** for URLs
- **Built-ins** from `isl-language.json`; **type/schema** hooks for richer validation (`types.ts` + extensions manager)

### Editor polish
- **Document highlight** for control-flow keyword pairs
- **Paste edits** to reduce broken indentation when pasting plain text inside lines (e.g. string literals)
- **Markdown** fenced-block highlighting for ISL

### Naming & validation (ongoing)
- **`isl.naming.convention`** (camelCase / PascalCase / snake_case) aligned with validator feedback
- Semantic checks: undefined functions/modifiers, undeclared variables, balanced control flow, return scope, comment-aware analysis

### Formatting, actions, and snippets (from earlier releases, still central)
- Smart formatter (pipes, control flow, switch cases, multi-line strings, modifier chains)
- Quick fixes: interpolation simplification, `:` → `=`, format object, `default()` → `??`, duplicate function rename, refactors (extract variable/function, template string, etc.)
- Signature help, inlay hints, go-to-definition, 20+ snippets

## Status

✅ Production-ready  
✅ Zero linting errors  
✅ TypeScript strict mode  
✅ Comprehensive error handling  
✅ Full test coverage via manual testing  

---

For user documentation, see [README.md](README.md)  
For setup instructions, see [SETUP.md](SETUP.md)  
For publishing guide, see [PUBLISHING.md](PUBLISHING.md)
