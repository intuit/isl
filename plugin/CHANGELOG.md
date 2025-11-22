# Change Log

All notable changes to the ISL Language Support extension will be documented in this file.

## [1.0.0] - 2024-11-21

### Added
- Initial release
- Comprehensive syntax highlighting for ISL files
- IntelliSense with code completion for:
  - Keywords and control flow structures
  - Service calls (@.Date, @.Math, etc.)
  - Modifiers (|filter, |map, etc.)
  - Variable references
- Real-time validation and linting
  - Syntax error detection
  - Brace matching
  - Control flow balance checking
  - Variable declaration validation
- Hover documentation for:
  - Keywords
  - Built-in services
  - Modifiers
  - Variables
- Go to definition support for:
  - Function declarations
  - Type definitions
  - Imported modules
- Code formatting with customizable settings
- ISL execution from VS Code
  - Run with inline JSON input
  - Run with external JSON file
  - Formatted output display
  - Integrated output console
- Comprehensive code snippets for common ISL patterns
- Configuration options for validation, formatting, and execution
- Status bar indicator for ISL files
- Language configuration for:
  - Comment toggling
  - Bracket matching
  - Auto-closing pairs
  - Code folding

### Features Details

#### Syntax Highlighting
- Keywords: fun, modifier, if, foreach, while, switch, return, etc.
- Operators: ==, !=, <, >, <=, >=, contains, matches, ??, etc.
- String interpolation: `${expression}`, `{{math}}`, `@.Function()`
- Variables with $ prefix
- Function calls and modifiers
- Comments (// and #)
- Regular expressions

#### Code Completion
- Context-aware completions
- Service completions after @.
- Modifier completions after |
- Variable completions after $
- Snippet expansions

#### Validation
- Balanced braces, brackets, and parentheses
- Matching if/endif, foreach/endfor, while/endwhile, switch/endswitch
- Valid variable names (no reserved keywords)
- Proper string interpolation syntax
- Return statements only in functions

#### Execution
- Automatic detection of ISL runtime (isl.sh/isl.bat)
- Custom Java home configuration
- Detailed execution output
- Error reporting with stdout/stderr
- JSON output formatting

### Documentation
- Comprehensive README with examples
- Configuration guide
- Feature documentation
- Getting started guide

## [Unreleased]

### Planned Features
- Semantic token provider for better highlighting
- Symbol provider for outline view
- Reference finder
- Rename symbol support
- Code actions and quick fixes
- Refactoring support
- Debug adapter protocol support
- Test runner integration
- Performance optimizations
- Multi-file validation
- Import resolution and auto-import
- Type checking based on type declarations
- More sophisticated code formatting
- Bracket pair colorization
- Inlay hints for inferred types

