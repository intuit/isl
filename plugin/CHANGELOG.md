# Change Log

All notable changes to the ISL Language Support extension will be documented in this file.

## [1.0.0] - 2025-11-26

### Added
- **Signature Help**: Parameter hints for functions and modifiers
- **Inlay Hints**: Type annotations displayed inline for variables
- **Code Actions & Quick Fixes**:
  - Simplify unnecessary string interpolation braces (`${$var}` → `$var`)
  - Convert `default()` modifier to null coalescing operator (`??`)
  - Format long single-line objects onto multiple lines
  - Change colon (`:`) to equals (`=`) for variable assignments
  - "Fix all in file" option for colon-to-equals conversions
- **Enhanced Snippets**: 20+ new snippets for common ISL patterns including:
  - Safe navigation and error handling
  - Array and object transformations
  - Conditional field patterns
  - Date operations
  - Batch processing patterns
- **CodeLens Enhancements**: 
  - "Test Function" action for quick testing
  - "Find Usages" for tracking function references
- **Status Bar Enhancements**: Active file indicator and quick actions

### Improved

#### Syntax Highlighting
- Fixed division operator (`/`) being incorrectly highlighted as regex
- Removed regex literal pattern (ISL uses regex as string arguments)
- Improved modifier highlighting with consistent coloring for namespaced modifiers
- Enhanced pipe operator (`|`) visibility with distinct color
- Better number literal highlighting

#### Code Completion
- Fixed variable autocompletion preserving `$` prefix when completing
- Improved context-aware suggestions

#### Formatter
- **Pipe Spacing**: Automatically adds space after pipe operator (`|trim` → `| trim`)
- **Function/Modifier Parameters**: Consistent spacing in declarations
  - With parameters: `fun name( $param1, $param2 )`
  - Without parameters: `fun name()`
- **Control Flow Parameters**: Consistent spacing for `if`, `switch`, `while`, `foreach`
  - Example: `if ( $condition )`, `switch ( $value )`
- **Nested Control Flow**: Proper indentation for nested structures, including:
  - Nested switch statements in switch cases (`"A" -> switch($x) ... endswitch;`)
  - Control flow after arrow operator (`->`)
- **Switch Statement Objects**: Fixed indentation for object values in switch cases
  - Properly handles `};` in case values without closing the switch
- **Modifier Chain Indentation**: Multi-line modifier chains are now indented
- **Multi-line String Preservation**: Backtick strings preserve original formatting and indentation
- Removed automatic splitting of long modifier chains (user controls line breaks)

#### Validator
- **Semantic Validation**:
  - Undefined function detection
  - Undefined modifier detection (including `push`, `getProperty`, `setProperty`)
  - Undeclared variable usage detection
  - Smart variable declaration tracking (supports both `=` and `:` operators)
- **Improved Control Flow Balance**:
  - Fixed `endfor`/`foreach` matching in expression contexts
  - Fixed `endswitch`/`switch` matching for nested switches
  - Fixed `endif`/`if` detection in complex conditions
  - Recognizes control flow after arrow operator (`->`) in switch cases
  - Better handling of inline vs. block control flow statements
- **Return Statement Validation**: Completely rewritten to accurately track function scope with proper brace depth tracking
- **Comment Handling**: All validations now correctly ignore code in comments
- **Assignment Operators**: Recognizes both `=` and `:` for variable declarations
- **Information-Level Diagnostics** (blue squiggly):
  - Long object declarations suggesting formatting
  - Colon usage suggesting equals for consistency
  - Unnecessary string interpolation braces

#### Spell Checking
- Added ISL-specific keywords to cSpell dictionary:
  - Control flow: `endfor`, `endswitch`, `endif`, `endwhile`
  - Modifiers: `upperCase`, `toLowerCase`, `pushItems`, `getProperty`, `setProperty`
  - VSCode extension terms: `inlayhints`, `codelens`, `quickfix`
  - Common concatenated forms

### Fixed
- Division operator no longer highlighted as regex in expressions
- Variable completion maintains `$` prefix
- Object formatting no longer cuts string interpolations
- Math expressions no longer misidentified as objects in type hints
- Variables in comments no longer trigger "undeclared variable" warnings
- Control flow balance correctly handles all nesting scenarios
- Return statement validation accurate in complex nested functions
- Function/modifier parameter spacing consistent after formatting
- Nested switch statement indentation correct
- Switch case object values maintain proper indentation
- Multi-line backtick strings preserve internal formatting

### Configuration
- Removed `isl.formatting.formatModifierChains` (automatic chain splitting removed)
- Kept `isl.formatting.alignProperties` for object property alignment

## [1.0.0-Beta]

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
- Debug adapter protocol support
- Test runner integration
- Performance optimizations
- Multi-file validation
- Import resolution and auto-import
- Type checking based on type declarations
- Bracket pair colorization

