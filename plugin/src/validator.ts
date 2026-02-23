import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import { IslExtensionsManager } from './extensions';
import { validateControlFlowBalance as validateControlFlowBalanceUtil } from './controlFlowMatcher';

export class IslValidator {
    private diagnosticCollection: vscode.DiagnosticCollection;
    private validationTimeout: NodeJS.Timeout | undefined;

    // Built-in modifiers
    private readonly builtInModifiers = new Set([
        // String modifiers
        'trim', 'trimStart', 'trimEnd',
        'cap', 'left', 'right',
        'substring', 'substringUpto', 'substringAfter',
        'lowerCase', 'upperCase',
        'replace', 'remove',
        'concat', 'append',
        'split',
        'csv.*',
        'padStart', 'padEnd',
        'reverse',
        'capitalize', 'titleCase', 'camelCase', 'snakeCase',
        'truncate',
        'html.*',
        'sanitizeTid',
        
        // Array modifiers
        'isEmpty', 'isNotEmpty',
        'push', 'pop', 'pushItems',
        'at', 'first', 'last',
        'take', 'drop',
        'unique', 'slice',
        'indexOf', 'lastIndexOf',
        'chunk',
        
        // Object modifiers
        'length',
        'keys', 'kv',
        'sort',
        'delete',
        'select',
        'getProperty', 'setProperty',
        'merge',
        'pick', 'omit',
        'rename',
        'has',
        'default',
        
        // Math modifiers (pipe usage)
        'negate', 'absolute', 'precision',
        'round.*',
        
        // Conversion modifiers
        'to.*',
        'hex.*',
        'join.*',
        'email.*',
        
        // Encoding modifiers
        'encode.*',
        'decode.*',
        
        // Compression modifiers
        'gzip', 'gunzip', 'gunzipToByte',
        
        // JSON/XML/YAML modifiers
        'json.*',
        'yaml.*',
        'xml.*',
        
        // Regex modifiers
        'regex.*',
        
        // Date modifiers
        'date.*',
        
        // Type modifiers
        'typeof',
        
        // High-order modifiers
        'map', 'filter', 'reduce',
        
        // Legacy/Alias modifiers
        'contains', 'startsWith', 'endsWith'
    ]);

    // Built-in functions (static methods) - these can also be used with pipes
    private readonly builtInFunctions = new Set([
        // Date functions
        'Date.now', 'Date.parse', 'Date.format',
        'Date.fromEpochSeconds', 'Date.fromEpochMillis',
        
        // Math functions (can be used as @.Math.* or | Math.*)
        'Math.min', 'Math.max', 'Math.mean', 'Math.mod', 'Math.sqrt',
        'Math.sum', 'Math.average',
        'Math.round', 'Math.floor', 'Math.ceil', 'Math.abs',
        'Math.RandInt', 'Math.RandFloat', 'Math.RandDouble',
        
        // String functions
        'String.concat', 'String.join',
        
        // Array functions
        'Array.from', 'Array.of', 'Array.range',
        'Array.slice', 'Array.unique',
        
        // JSON/XML/YAML functions
        'Json.parse', 'Json.stringify',
        'Xml.parse', 'Xml.toXml',
        'Yaml.parse',
        
        // Crypto functions
        'Crypto.md5', 'Crypto.sha1', 'Crypto.sha256',
        'Crypto.base64encode', 'Crypto.base64decode',
        
        // Pagination functions
        'Pagination.Cursor', 'Pagination.Page', 'Pagination.Date', 'Pagination.Offset', 'Pagination.Keyset'
    ]);

    // Built-in namespaces (these should not be treated as imported modules)
    private readonly builtInNamespaces = new Set([
        'Date', 'Math', 'String', 'Array', 'Json', 'Xml', 'Yaml', 'Crypto', 'Pagination', 'This'
    ]);

    constructor(private extensionsManager: IslExtensionsManager) {
        this.diagnosticCollection = vscode.languages.createDiagnosticCollection('isl');
    }

    public dispose() {
        this.diagnosticCollection.dispose();
        if (this.validationTimeout) {
            clearTimeout(this.validationTimeout);
        }
    }

    public validateDebounced(document: vscode.TextDocument) {
        if (this.validationTimeout) {
            clearTimeout(this.validationTimeout);
        }
        this.validationTimeout = setTimeout(() => this.validate(document), 500);
    }

    public async validate(document: vscode.TextDocument): Promise<void> {
        const config = vscode.workspace.getConfiguration('isl.validation');
        if (!config.get<boolean>('enabled', true)) {
            return;
        }

        const diagnostics: vscode.Diagnostic[] = [];
        const text = document.getText();
        const lines = text.split('\n');

        // Load custom extensions
        const extensions = await this.extensionsManager.getExtensionsForDocument(document);

        // Basic syntax validation
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            const lineNumber = i;

            // Check for common syntax errors
            this.checkBraceMatching(line, lineNumber, diagnostics, document);
            this.checkVariableDeclaration(line, lineNumber, diagnostics, document);
            this.checkStringInterpolation(line, lineNumber, diagnostics, document);
            this.checkFunctionDeclaration(line, lineNumber, diagnostics, document);
            this.checkControlFlow(line, lineNumber, diagnostics, document);
        }

        // Check for overall structure issues
        this.checkBalancedBraces(text, diagnostics, document);
        this.checkControlFlowBalance(text, diagnostics, document);
        this.checkBalancedBackticks(text, diagnostics, document);

        // Semantic validation
        const userDefinedFunctions = this.extractUserDefinedFunctions(document);
        const userDefinedModifiers = this.extractUserDefinedModifiers(document);
        const declaredVariables = this.extractDeclaredVariables(document);

        // Extract imports and their exported functions/modifiers
        const importedFunctions = await this.extractImportedFunctions(document);
        const importedModifiers = await this.extractImportedModifiers(document);

        // Extract pagination variables for property validation
        const paginationVariables = this.extractPaginationVariables(document);
        
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            this.checkFunctionCalls(line, i, diagnostics, document, userDefinedFunctions, extensions, importedFunctions);
            this.checkModifierUsage(line, i, diagnostics, document, userDefinedModifiers, extensions, importedModifiers);
            this.checkVariableUsage(line, i, diagnostics, document, declaredVariables);
            this.checkPaginationPropertyAccess(line, i, diagnostics, document, paginationVariables);
            this.checkLongObjectDeclaration(line, i, diagnostics, document);
            this.checkUnnecessaryStringInterpolation(line, i, diagnostics, document);
            this.checkDefaultModifier(line, i, diagnostics, document);
            this.checkColonAssignment(line, i, diagnostics, document);
        }

        this.diagnosticCollection.set(document.uri, diagnostics);
    }

    private checkBraceMatching(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Skip this check - it causes false positives on multi-line constructs
        // The overall balance check in checkBalancedBraces handles this better
        return;
    }

    private checkVariableDeclaration(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Check for invalid variable names
        const varPattern = /\$([a-zA-Z_][a-zA-Z0-9_]*)/g;
        let match;
        
        while ((match = varPattern.exec(line)) !== null) {
            const varName = match[1];
            
            // Check if variable name is a reserved keyword
            const reservedKeywords = ['if', 'else', 'endif', 'switch', 'endswitch', 'foreach', 'endfor', 'while', 'endwhile', 'fun', 'modifier', 'return', 'import', 'type', 'as', 'from'];
            if (reservedKeywords.includes(varName)) {
                const startPos = match.index;
                const range = new vscode.Range(lineNumber, startPos, lineNumber, startPos + match[0].length);
                diagnostics.push(new vscode.Diagnostic(
                    range,
                    `'${varName}' is a reserved keyword and cannot be used as a variable name`,
                    vscode.DiagnosticSeverity.Error
                ));
            }
        }
    }

    private checkStringInterpolation(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Note: Multi-line backtick strings are valid in ISL
        // We check for balanced backticks at the document level, not line level
        // This method now only checks for empty interpolation expressions
        
        // Check for invalid interpolation syntax (empty ${})
        // But be careful - the backtick might not close on this line
        const interpolationPattern = /\$\{([^}]*)\}/g;
        let match;
        while ((match = interpolationPattern.exec(line)) !== null) {
            const expr = match[1];
            if (expr.trim() === '') {
                const startPos = match.index;
                const range = new vscode.Range(lineNumber, startPos, lineNumber, startPos + match[0].length);
                diagnostics.push(new vscode.Diagnostic(
                    range,
                    'Empty interpolation expression',
                    vscode.DiagnosticSeverity.Warning
                ));
            }
        }
    }

    private checkFunctionDeclaration(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Check function declarations
        const funPattern = /^\s*(fun|modifier)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/;
        const match = line.match(funPattern);
        
        if (match) {
            const funcName = match[2];
            // Could add more validation here (e.g., duplicate function names)
        }

        // Check for return statements outside functions
        if (line.trim().startsWith('return ') && !this.isInsideFunction(document, lineNumber)) {
            const range = new vscode.Range(lineNumber, line.indexOf('return'), lineNumber, line.indexOf('return') + 6);
            diagnostics.push(new vscode.Diagnostic(
                range,
                'Return statement outside of function',
                vscode.DiagnosticSeverity.Error
            ));
        }
    }

    private checkControlFlow(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Check for proper control flow syntax
        const trimmed = line.trim();
        
        // Check if/else/endif
        if (trimmed.startsWith('if ') && !trimmed.includes('(')) {
            const range = new vscode.Range(lineNumber, 0, lineNumber, line.length);
            diagnostics.push(new vscode.Diagnostic(
                range,
                'If statement requires parentheses around condition',
                vscode.DiagnosticSeverity.Error
            ));
        }

        // Check foreach
        if (trimmed.startsWith('foreach ') && !trimmed.includes(' in ')) {
            const range = new vscode.Range(lineNumber, 0, lineNumber, line.length);
            diagnostics.push(new vscode.Diagnostic(
                range,
                'Foreach loop requires "in" keyword',
                vscode.DiagnosticSeverity.Error
            ));
        }
    }

    private checkBalancedBraces(text: string, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        let braceCount = 0;
        let bracketCount = 0;
        let parenCount = 0;

        const lines = text.split('\n');
        
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            
            // Skip comments
            const commentIndex = Math.min(
                line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
                line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
            );
            const checkLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

            braceCount += (checkLine.match(/\{/g) || []).length - (checkLine.match(/\}/g) || []).length;
            bracketCount += (checkLine.match(/\[/g) || []).length - (checkLine.match(/\]/g) || []).length;
            parenCount += (checkLine.match(/\(/g) || []).length - (checkLine.match(/\)/g) || []).length;
        }

        if (braceCount !== 0) {
            const range = new vscode.Range(0, 0, 0, 0);
            diagnostics.push(new vscode.Diagnostic(
                range,
                `Unbalanced braces: ${braceCount > 0 ? 'missing closing' : 'extra closing'} brace(s)`,
                vscode.DiagnosticSeverity.Error
            ));
        }

        if (bracketCount !== 0) {
            const range = new vscode.Range(0, 0, 0, 0);
            diagnostics.push(new vscode.Diagnostic(
                range,
                `Unbalanced brackets: ${bracketCount > 0 ? 'missing closing' : 'extra closing'} bracket(s)`,
                vscode.DiagnosticSeverity.Error
            ));
        }

        if (parenCount !== 0) {
            const range = new vscode.Range(0, 0, 0, 0);
            diagnostics.push(new vscode.Diagnostic(
                range,
                `Unbalanced parentheses: ${parenCount > 0 ? 'missing closing' : 'extra closing'} parenthesis/parentheses`,
                vscode.DiagnosticSeverity.Error
            ));
        }
    }

    private checkBalancedBackticks(text: string, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Check for balanced backticks across the entire document
        // Backtick strings can span multiple lines in ISL
        
        const lines = text.split('\n');
        let backtickCount = 0;
        let lastBacktickLine = -1;
        let lastBacktickCol = -1;
        
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            
            // Process each character to properly handle escaped backticks
            let inString = false;
            let stringChar = '';
            
            for (let j = 0; j < line.length; j++) {
                const char = line[j];
                const prevChar = j > 0 ? line[j - 1] : '';
                
                // Skip escaped characters
                if (prevChar === '\\') {
                    continue;
                }
                
                // Track single and double quoted strings (backticks don't nest inside them)
                if ((char === '"' || char === "'") && !inString) {
                    inString = true;
                    stringChar = char;
                    continue;
                } else if (char === stringChar && inString) {
                    inString = false;
                    stringChar = '';
                    continue;
                }
                
                // Count backticks only outside of regular strings
                if (char === '`' && !inString) {
                    backtickCount++;
                    lastBacktickLine = i;
                    lastBacktickCol = j;
                }
            }
        }
        
        // If backtick count is odd, we have an unclosed template literal
        if (backtickCount % 2 !== 0 && lastBacktickLine >= 0) {
            const range = new vscode.Range(lastBacktickLine, lastBacktickCol, lastBacktickLine, lastBacktickCol + 1);
            diagnostics.push(new vscode.Diagnostic(
                range,
                'Unclosed template literal (backtick string)',
                vscode.DiagnosticSeverity.Error
            ));
        }
    }

    private checkControlFlowBalance(text: string, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Use the shared utility for control flow validation
        // This properly handles multi-line statements and nested structures
        const errors = validateControlFlowBalanceUtil(document);
        for (const { diagnostic } of errors) {
            diagnostics.push(diagnostic);
        }
    }

    private isInsideFunction(document: vscode.TextDocument, lineNumber: number): boolean {
        // Track function scopes using brace counting
        const functionStarts: number[] = [];
        
        for (let i = 0; i <= lineNumber; i++) {
            const fullLine = document.lineAt(i).text;
            
            // Skip comments
            const commentIndex = Math.min(
                fullLine.indexOf('//') !== -1 ? fullLine.indexOf('//') : Infinity,
                fullLine.indexOf('#') !== -1 ? fullLine.indexOf('#') : Infinity
            );
            const line = commentIndex !== Infinity ? fullLine.substring(0, commentIndex) : fullLine;
            
            // Check if this line has a function/modifier definition
            if (line.match(/\b(fun|modifier)\s+[a-zA-Z_][a-zA-Z0-9_]*\s*\(/)) {
                functionStarts.push(i);
            }
        }
        
        // For each function start, check if the current line is within its braces
        for (const funcStart of functionStarts) {
            let braceDepth = 0;
            let foundOpeningBrace = false;
            
            for (let i = funcStart; i <= lineNumber; i++) {
                const fullLine = document.lineAt(i).text;
                const commentIndex = Math.min(
                    fullLine.indexOf('//') !== -1 ? fullLine.indexOf('//') : Infinity,
                    fullLine.indexOf('#') !== -1 ? fullLine.indexOf('#') : Infinity
                );
                const line = commentIndex !== Infinity ? fullLine.substring(0, commentIndex) : fullLine;
                
                // Count braces
                const openBraces = (line.match(/\{/g) || []).length;
                const closeBraces = (line.match(/\}/g) || []).length;
                
                braceDepth += openBraces;
                if (openBraces > 0) {
                    foundOpeningBrace = true;
                }
                braceDepth -= closeBraces;
                
                // If we're at the target line and inside this function's braces
                if (i === lineNumber && foundOpeningBrace && braceDepth > 0) {
                    return true;
                }
                
                // If we've closed this function completely before reaching the target line
                if (foundOpeningBrace && braceDepth === 0 && i < lineNumber) {
                    break; // This function is closed, try the next one
                }
            }
        }
        
        return false;
    }

    // Semantic validation methods

    /**
     * Extracts import statements from the document
     * Returns a map of module name -> file path
     */
    private extractImports(document: vscode.TextDocument): Map<string, string> {
        const imports = new Map<string, string>();
        const text = document.getText();
        const lines = text.split('\n');

        for (const line of lines) {
            // Match: import ModuleName from 'file.isl' or import ModuleName from "file.isl"
            const importMatch = line.match(/import\s+([a-zA-Z_][a-zA-Z0-9_]*)\s+from\s+['"]([^'"]+)['"]/);
            if (importMatch) {
                const moduleName = importMatch[1];
                const filePath = importMatch[2];
                imports.set(moduleName, filePath);
            }
        }

        return imports;
    }

    /**
     * Resolves the absolute path of an imported file relative to the current document
     */
    private resolveImportPath(document: vscode.TextDocument, importPath: string): vscode.Uri | null {
        const currentDir = path.dirname(document.uri.fsPath);
        let resolvedPath: string;

        if (path.isAbsolute(importPath)) {
            resolvedPath = importPath;
        } else {
            resolvedPath = path.resolve(currentDir, importPath);
        }

        // Try with .isl extension if not present
        if (!resolvedPath.endsWith('.isl')) {
            const withExtension = resolvedPath + '.isl';
            if (fs.existsSync(withExtension)) {
                resolvedPath = withExtension;
            }
        }

        if (fs.existsSync(resolvedPath)) {
            return vscode.Uri.file(resolvedPath);
        }

        return null;
    }

    /**
     * Extracts functions from imported files
     * Returns a map of module name -> set of function names
     */
    private async extractImportedFunctions(document: vscode.TextDocument): Promise<Map<string, Set<string>>> {
        const importedFunctions = new Map<string, Set<string>>();
        const imports = this.extractImports(document);

        for (const [moduleName, filePath] of imports) {
            const importedUri = this.resolveImportPath(document, filePath);
            if (!importedUri) {
                continue;
            }

            try {
                const importedText = fs.readFileSync(importedUri.fsPath, 'utf-8');
                const importedLines = importedText.split('\n');
                const functions = new Set<string>();

                for (const line of importedLines) {
                    // Match function definitions: fun functionName(
                    const funMatch = line.match(/^\s*fun\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/);
                    if (funMatch) {
                        functions.add(funMatch[1]);
                    }
                }

                importedFunctions.set(moduleName, functions);
            } catch (error) {
                // Silently skip if file cannot be read
                console.warn(`Could not read imported file ${filePath}: ${error}`);
            }
        }

        return importedFunctions;
    }

    /**
     * Extracts modifiers from imported files
     * Returns a map of module name -> set of modifier names
     */
    private async extractImportedModifiers(document: vscode.TextDocument): Promise<Map<string, Set<string>>> {
        const importedModifiers = new Map<string, Set<string>>();
        const imports = this.extractImports(document);

        for (const [moduleName, filePath] of imports) {
            const importedUri = this.resolveImportPath(document, filePath);
            if (!importedUri) {
                continue;
            }

            try {
                const importedText = fs.readFileSync(importedUri.fsPath, 'utf-8');
                const importedLines = importedText.split('\n');
                const modifiers = new Set<string>();

                for (const line of importedLines) {
                    // Match modifier definitions: modifier modifierName(
                    const modMatch = line.match(/^\s*modifier\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/);
                    if (modMatch) {
                        modifiers.add(modMatch[1]);
                    }
                }

                importedModifiers.set(moduleName, modifiers);
            } catch (error) {
                // Silently skip if file cannot be read
                console.warn(`Could not read imported file ${filePath}: ${error}`);
            }
        }

        return importedModifiers;
    }

    private extractUserDefinedFunctions(document: vscode.TextDocument): Set<string> {
        const functions = new Set<string>();
        const text = document.getText();
        const lines = text.split('\n');

        for (const line of lines) {
            // Match function definitions: fun functionName(
            const funMatch = line.match(/^\s*fun\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/);
            if (funMatch) {
                functions.add(funMatch[1]);
            }
        }

        return functions;
    }

    private extractUserDefinedModifiers(document: vscode.TextDocument): Set<string> {
        const modifiers = new Set<string>();
        const text = document.getText();
        const lines = text.split('\n');

        for (const line of lines) {
            // Match modifier definitions: modifier modifierName(
            const modMatch = line.match(/^\s*modifier\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/);
            if (modMatch) {
                modifiers.add(modMatch[1]);
            }
        }

        return modifiers;
    }

    private extractPaginationVariables(document: vscode.TextDocument): Map<string, string> {
        const paginationVars = new Map<string, string>(); // variable name -> pagination type
        const text = document.getText();
        const lines = text.split('\n');

        for (const line of lines) {
            // Match @.Pagination.[Type]( $varName, ... )
            const cursorMatch = line.match(/@\.Pagination\.Cursor\s*\(\s*\$([a-zA-Z_][a-zA-Z0-9_]*)/);
            if (cursorMatch) {
                paginationVars.set(cursorMatch[1], 'Cursor');
            }
            
            const offsetMatch = line.match(/@\.Pagination\.Offset\s*\(\s*\$([a-zA-Z_][a-zA-Z0-9_]*)/);
            if (offsetMatch) {
                paginationVars.set(offsetMatch[1], 'Offset');
            }
            
            const pageMatch = line.match(/@\.Pagination\.Page\s*\(\s*\$([a-zA-Z_][a-zA-Z0-9_]*)/);
            if (pageMatch) {
                paginationVars.set(pageMatch[1], 'Page');
            }
            
            const keysetMatch = line.match(/@\.Pagination\.Keyset\s*\(\s*\$([a-zA-Z_][a-zA-Z0-9_]*)/);
            if (keysetMatch) {
                paginationVars.set(keysetMatch[1], 'Keyset');
            }
            
            const dateMatch = line.match(/@\.Pagination\.Date\s*\(\s*\$([a-zA-Z_][a-zA-Z0-9_]*)/);
            if (dateMatch) {
                paginationVars.set(dateMatch[1], 'Date');
            }
        }

        return paginationVars;
    }
    
    private extractDeclaredVariables(document: vscode.TextDocument): Map<string, number> {
        const variables = new Map<string, number>(); // variable name -> first declaration line
        const text = document.getText();
        const lines = text.split('\n');

        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            
            // Match variable declarations: $varName = ... or $varName: ...
            const varDeclPattern = /\$([a-zA-Z_][a-zA-Z0-9_]*)\s*[=:]/g;
            let match;
            
            while ((match = varDeclPattern.exec(line)) !== null) {
                const varName = match[1];
                if (!variables.has(varName)) {
                    variables.set(varName, i);
                }
            }

            // Also track function parameters as declared variables
            const funParamMatch = line.match(/^\s*(fun|modifier)\s+[a-zA-Z_][a-zA-Z0-9_]*\s*\(([^)]*)\)/);
            if (funParamMatch) {
                const params = funParamMatch[2];
                const paramNames = params.split(',').map(p => p.trim().replace(/^\$/, ''));
                for (const param of paramNames) {
                    if (param && !variables.has(param)) {
                        variables.set(param, i);
                    }
                }
            }

            // Track foreach loop variables: foreach $item in $items
            // Creates both $item and $itemIndex (zero-based index)
            const foreachMatch = line.match(/foreach\s+\$([a-zA-Z_][a-zA-Z0-9_]*)\s+in/);
            if (foreachMatch) {
                const varName = foreachMatch[1];
                if (!variables.has(varName)) {
                    variables.set(varName, i);
                }
                
                // Also add the index variable: $iteratorIndex
                const indexVarName = varName + 'Index';
                if (!variables.has(indexVarName)) {
                    variables.set(indexVarName, i);
                }
            }
            
            // Track pagination function parameters: @.Pagination.[Type]( $varName, ... )
            // The first parameter to pagination functions is a variable declaration
            const paginationMatch = line.match(/@\.Pagination\.[A-Za-z_][A-Za-z0-9_]*\s*\(\s*\$([a-zA-Z_][a-zA-Z0-9_]*)/);
            if (paginationMatch) {
                const varName = paginationMatch[1];
                if (!variables.has(varName)) {
                    variables.set(varName, i);
                }
            }
        }

        return variables;
    }

    private checkFunctionCalls(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument, userDefinedFunctions: Set<string>, extensions: import('./extensions').IslExtensions, importedFunctions: Map<string, Set<string>>) {
        // Skip comments - only check code part
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;
        
        // First check for @.ModuleName.functionName() pattern (imported functions and built-ins)
        const importedFunctionPattern = /@\.([a-zA-Z_][a-zA-Z0-9_]*)\.([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/g;
        let match;
        const processedRanges: Array<{ start: number; end: number }> = [];

        while ((match = importedFunctionPattern.exec(codeOnlyLine)) !== null) {
            const moduleName = match[1];
            const funcName = match[2];
            const matchStart = match.index;
            const matchEnd = matchStart + match[0].length;
            
            // Track this range so we don't process it again
            processedRanges.push({ start: matchStart, end: matchEnd });
            
            // Skip if it's a built-in namespace (Date, Math, This, etc.)
            if (this.builtInNamespaces.has(moduleName)) {
                // Check if it's a valid built-in function (case-insensitive check)
                const builtInKey = `${moduleName}.${funcName}`;
                const builtInKeyLower = builtInKey.toLowerCase();
                let isBuiltIn = false;
                
                for (const builtIn of this.builtInFunctions) {
                    if (builtIn.toLowerCase() === builtInKeyLower) {
                        isBuiltIn = true;
                        break;
                    }
                }
                
                // For @.This.functionName(), check if it's a local function
                if (moduleName === 'This') {
                    if (userDefinedFunctions.has(funcName) || extensions.functions.has(funcName)) {
                        continue;
                    } else {
                        const startPos = match.index + match[0].indexOf(funcName);
                        const range = new vscode.Range(lineNumber, startPos, lineNumber, startPos + funcName.length);
                        diagnostics.push(new vscode.Diagnostic(
                            range,
                            `Function '${funcName}' is not defined`,
                            vscode.DiagnosticSeverity.Warning
                        ));
                    }
                } else if (isBuiltIn) {
                    // Valid built-in function
                    continue;
                } else {
                    // Built-in namespace but function doesn't exist
                    const startPos = match.index + match[0].indexOf(funcName);
                    const range = new vscode.Range(lineNumber, startPos, lineNumber, startPos + funcName.length);
                    diagnostics.push(new vscode.Diagnostic(
                        range,
                        `Function '${funcName}' is not a valid ${moduleName} function`,
                        vscode.DiagnosticSeverity.Warning
                    ));
                }
                continue;
            }

            // Check if it's an imported module
            if (importedFunctions.has(moduleName)) {
                if (importedFunctions.get(moduleName)!.has(funcName)) {
                    continue;
                } else {
                    const startPos = match.index + match[0].indexOf(funcName);
                    const range = new vscode.Range(lineNumber, startPos, lineNumber, startPos + funcName.length);
                    diagnostics.push(new vscode.Diagnostic(
                        range,
                        `Function '${funcName}' is not exported from module '${moduleName}'`,
                        vscode.DiagnosticSeverity.Warning
                    ));
                }
            } else {
                // Module not found - check if it's imported
                const imports = this.extractImports(document);
                if (!imports.has(moduleName)) {
                    const startPos = match.index + match[0].indexOf(moduleName);
                    const range = new vscode.Range(lineNumber, startPos, lineNumber, startPos + moduleName.length);
                    diagnostics.push(new vscode.Diagnostic(
                        range,
                        `Module '${moduleName}' is not imported`,
                        vscode.DiagnosticSeverity.Warning
                    ));
                }
            }
        }

        // Check for @.This.functionName() calls (skip already processed ranges)
        // Note: @.functionName() without This. is not valid ISL syntax, so we only check @.This.functionName()
        // All @.ModuleName.functionName() patterns were already handled above
        const thisFunctionPattern = /@\.This\.([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/g;
        while ((match = thisFunctionPattern.exec(codeOnlyLine)) !== null) {
            const matchStart = match.index;
            const matchEnd = matchStart + match[0].length;
            
            // Skip if this range was already processed as an imported function or built-in
            if (processedRanges.some(r => matchStart >= r.start && matchEnd <= r.end)) {
                continue;
            }

            const funcName = match[1];
            
            // Check if it's a user-defined function or custom extension function
            if (userDefinedFunctions.has(funcName) || extensions.functions.has(funcName)) {
                continue;
            }

            // Function not found
            const startPos = match.index + match[0].indexOf(funcName);
            const range = new vscode.Range(lineNumber, startPos, lineNumber, startPos + funcName.length);
            diagnostics.push(new vscode.Diagnostic(
                range,
                `Function '${funcName}' is not defined`,
                vscode.DiagnosticSeverity.Warning
            ));
        }
    }

    private checkModifierUsage(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument, userDefinedModifiers: Set<string>, extensions: import('./extensions').IslExtensions, importedModifiers: Map<string, Set<string>>) {
        // Skip comments - only check code part
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;
        
        // Check for | modifierName usage (including multi-level like regex.find or Math.sum)
        // Also check for | ModuleName.modifierName (imported modifiers)
        const modifierPattern = /\|\s*([a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*)\s*(?:\(|$|\|)/g;
        let match;

        while ((match = modifierPattern.exec(codeOnlyLine)) !== null) {
            const modifierName = match[1];
            
            // Check if it's a user-defined modifier or custom extension modifier
            if (userDefinedModifiers.has(modifierName) || extensions.modifiers.has(modifierName)) {
                continue;
            }
            
            // Check if it's a built-in modifier (exact match)
            if (this.builtInModifiers.has(modifierName)) {
                continue;
            }
            
            // Check if it matches a wildcard pattern (e.g., regex.* matches regex.find)
            let isBuiltIn = false;
            for (const builtIn of this.builtInModifiers) {
                if (builtIn.endsWith('.*')) {
                    const prefix = builtIn.slice(0, -1); // Remove the *
                    if (modifierName.startsWith(prefix)) {
                        isBuiltIn = true;
                        break;
                    }
                }
            }
            
            // Check if it's also in the builtInFunctions set (Math.sum, etc. can be used as modifiers)
            if (this.builtInFunctions.has(modifierName)) {
                isBuiltIn = true;
            }
            
            if (isBuiltIn) {
                continue;
            }

            // Check if it's an imported modifier: | ModuleName.modifierName
            const parts = modifierName.split('.');
            if (parts.length === 2) {
                const [moduleName, modName] = parts;
                
                // Skip if it's a built-in namespace (Date, Math, etc.)
                if (this.builtInNamespaces.has(moduleName)) {
                    // Check if it's a valid built-in function that can be used as a modifier (case-insensitive)
                    const builtInKey = `${moduleName}.${modName}`;
                    const builtInKeyLower = builtInKey.toLowerCase();
                    let isBuiltIn = false;
                    
                    for (const builtIn of this.builtInFunctions) {
                        if (builtIn.toLowerCase() === builtInKeyLower) {
                            isBuiltIn = true;
                            break;
                        }
                    }
                    
                    if (isBuiltIn) {
                        continue;
                    } else {
                        // Built-in namespace but modifier doesn't exist
                        const modifierStart = match.index + match[0].indexOf(modifierName) + (moduleName.length + 1);
                        const range = new vscode.Range(lineNumber, modifierStart, lineNumber, modifierStart + modName.length);
                        diagnostics.push(new vscode.Diagnostic(
                            range,
                            `Modifier '${modName}' is not a valid ${moduleName} modifier`,
                            vscode.DiagnosticSeverity.Warning
                        ));
                        continue;
                    }
                }

                // Check if it's an imported module
                if (importedModifiers.has(moduleName)) {
                    if (importedModifiers.get(moduleName)!.has(modName)) {
                        continue;
                    } else {
                        // Calculate position: find modName within the full match string
                        const modifierStart = match.index + match[0].indexOf(modifierName) + (moduleName.length + 1);
                        const range = new vscode.Range(lineNumber, modifierStart, lineNumber, modifierStart + modName.length);
                        diagnostics.push(new vscode.Diagnostic(
                            range,
                            `Modifier '${modName}' is not exported from module '${moduleName}'`,
                            vscode.DiagnosticSeverity.Warning
                        ));
                        continue;
                    }
                } else {
                    // Check if module is imported
                    const imports = this.extractImports(document);
                    if (!imports.has(moduleName)) {
                        // Calculate position: find moduleName within the full match string
                        const moduleStart = match.index + match[0].indexOf(moduleName);
                        const range = new vscode.Range(lineNumber, moduleStart, lineNumber, moduleStart + moduleName.length);
                        diagnostics.push(new vscode.Diagnostic(
                            range,
                            `Module '${moduleName}' is not imported`,
                            vscode.DiagnosticSeverity.Warning
                        ));
                        continue;
                    }
                }
            }

            const startPos = match.index + match[0].indexOf(modifierName);
            const range = new vscode.Range(lineNumber, startPos, lineNumber, startPos + modifierName.length);
            diagnostics.push(new vscode.Diagnostic(
                range,
                `Modifier '${modifierName}' is not defined`,
                vscode.DiagnosticSeverity.Warning
            ));
        }
    }

    private checkVariableUsage(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument, declaredVariables: Map<string, number>) {
        // Skip comments - remove everything after // or #
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;
        
        // Skip if the entire line is a comment
        if (codeOnlyLine.trim() === '') {
            return;
        }

        // Check if this line has an assignment (variable on left side of = or :)
        // Match: $var = ... or $var.prop = ... or $var.prop.nested = ... or prop: $var or prop: value
        const assignmentMatch = codeOnlyLine.match(/^(\s*)(\$[a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*)\s*[=:]\s*(.+)/);
        if (assignmentMatch) {
            const leftSide = assignmentMatch[2]; // The variable being assigned to
            const rightSide = assignmentMatch[3]; // The value being assigned
            
            // Extract the base variable from the left side (e.g., $var from $var.prop.nested)
            const baseVarMatch = leftSide.match(/\$([a-zA-Z_][a-zA-Z0-9_]*)/);
            const baseVarBeingDeclared = baseVarMatch ? baseVarMatch[1] : null;
            
            // Only check variables on the RIGHT side of assignment
            // But exclude the base variable being declared from the check
            this.checkVariablesInExpression(rightSide, lineNumber, diagnostics, declaredVariables, codeOnlyLine.indexOf(rightSide), baseVarBeingDeclared);
            return;
        }
        
        // Also check for object property syntax: propertyName: value (without $ on left)
        const propertyMatch = codeOnlyLine.match(/^(\s*)([a-zA-Z_][a-zA-Z0-9_]*)\s*:\s*(.+)/);
        if (propertyMatch) {
            const rightSide = propertyMatch[3];
            // Check variables on the right side of the colon
            this.checkVariablesInExpression(rightSide, lineNumber, diagnostics, declaredVariables, codeOnlyLine.indexOf(rightSide), null);
            return;
        }

        // Check all variable usages in the code part of the line (not comments)
        this.checkVariablesInExpression(codeOnlyLine, lineNumber, diagnostics, declaredVariables, 0, null);
    }

    private checkVariablesInExpression(expression: string, lineNumber: number, diagnostics: vscode.Diagnostic[], declaredVariables: Map<string, number>, offset: number, excludeVar: string | null) {
        // Find all variable references: $varName or $varName.property
        const varPattern = /\$([a-zA-Z_][a-zA-Z0-9_]*)(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*/g;
        let match;

        while ((match = varPattern.exec(expression)) !== null) {
            const baseVarName = match[1]; // Just the base variable name without $
            
            // Skip special variables created automatically by modifiers and contexts:
            // - it: current item in reduce, general iteration contexts
            // - fit: current item being filtered in filter()
            // - acc: accumulator in reduce()
            // - index: index in iteration contexts
            // - key, value: key-value iteration
            // - this, This: self-reference to current context/function
            const specialVars = ['it', 'fit', 'acc', 'index', 'key', 'value', 'this', 'This'];
            if (specialVars.includes(baseVarName)) {
                continue;
            }

            // Skip the variable being declared on this line (if any)
            if (excludeVar && baseVarName === excludeVar) {
                continue;
            }

            // Check if variable has been declared
            if (!declaredVariables.has(baseVarName)) {
                const startPos = offset + match.index;
                const range = new vscode.Range(lineNumber, startPos, lineNumber, startPos + match[0].length);
                diagnostics.push(new vscode.Diagnostic(
                    range,
                    `Variable '$${baseVarName}' is used before being declared`,
                    vscode.DiagnosticSeverity.Warning
                ));
            }
        }
    }

    private checkLongObjectDeclaration(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Check for long single-line object declarations that should be formatted
        if (line.length < 100) {
            return;
        }

        // Skip comments
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

        // Check if line contains object with multiple properties (at least 2 colons indicating properties)
        const objectMatch = codeOnlyLine.match(/\{[^}]*:[^}]*:[^}]*\}/);
        if (!objectMatch) {
            return;
        }

        // Find the position of the opening brace
        const bracePos = codeOnlyLine.indexOf('{');
        if (bracePos === -1) {
            return;
        }

        const range = new vscode.Range(lineNumber, bracePos, lineNumber, bracePos + 1);
        const diagnostic = new vscode.Diagnostic(
            range,
            'Long object declaration can be formatted on multiple lines',
            vscode.DiagnosticSeverity.Hint
        );
        diagnostic.code = 'format-object';
        diagnostics.push(diagnostic);
    }

    private checkUnnecessaryStringInterpolation(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Skip comments
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

        // Check for ${$variable} pattern (without dots - simple variables only)
        const unnecessaryInterpolation = /\$\{(\$[a-zA-Z_][a-zA-Z0-9_]*)\}/g;
        let match;

        while ((match = unnecessaryInterpolation.exec(codeOnlyLine)) !== null) {
            const fullMatch = match[0]; // ${$variable}
            const variable = match[1];  // $variable

            // Only flag simple variables (no property access)
            if (!variable.includes('.')) {
                const startPos = match.index;
                const endPos = startPos + fullMatch.length;
                const range = new vscode.Range(lineNumber, startPos, lineNumber, endPos);
                
                const diagnostic = new vscode.Diagnostic(
                    range,
                    `Unnecessary braces around ${variable} in string interpolation`,
                    vscode.DiagnosticSeverity.Hint
                );
                diagnostic.code = 'simplify-interpolation';
                diagnostics.push(diagnostic);
            }
        }
    }

    private checkDefaultModifier(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Skip comments
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

        // Check for | default() pattern
        const defaultPattern = /\|\s*(default)\s*\(/g;
        let match;

        while ((match = defaultPattern.exec(codeOnlyLine)) !== null) {
            const startPos = match.index;
            const endPos = startPos + match[0].length - 1; // Exclude the opening paren
            const range = new vscode.Range(lineNumber, startPos, lineNumber, endPos);
            
            const diagnostic = new vscode.Diagnostic(
                range,
                'Consider using ?? operator instead of | default()',
                vscode.DiagnosticSeverity.Hint
            );
            diagnostic.code = 'use-coalesce-operator';
            diagnostics.push(diagnostic);
        }
    }

    private checkPaginationPropertyAccess(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument, paginationVariables: Map<string, string>) {
        // Skip comments
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;
        
        // Find pagination variable property access: $varName.propertyName
        const propertyAccessPattern = /\$([a-zA-Z_][a-zA-Z0-9_]*)\.([a-zA-Z_][a-zA-Z0-9_]*)/g;
        let match;
        
        while ((match = propertyAccessPattern.exec(codeOnlyLine)) !== null) {
            const varName = match[1];
            const propertyName = match[2];
            
            if (paginationVariables.has(varName)) {
                const paginationType = paginationVariables.get(varName)!;
                const validProperties = this.getValidPaginationProperties(paginationType);
                
                if (!validProperties.includes(propertyName)) {
                    const startPos = match.index + match[0].indexOf(propertyName);
                    const range = new vscode.Range(lineNumber, startPos, lineNumber, startPos + propertyName.length);
                    
                    const validPropsStr = validProperties.join(', ');
                    diagnostics.push(new vscode.Diagnostic(
                        range,
                        `Invalid property '${propertyName}' for pagination ${paginationType}. Valid properties are: ${validPropsStr}`,
                        vscode.DiagnosticSeverity.Error
                    ));
                }
            }
        }
    }
    
    private getValidPaginationProperties(paginationType: string): string[] {
        switch (paginationType) {
            case 'Cursor':
                return ['current', 'next'];
            case 'Page':
                return ['startIndex', 'pageSize', 'page', 'fromOffset', 'toOffset', 'hasMorePages'];
            case 'Date':
                return ['startDate', 'endDate', 'page'];
            case 'Offset':
                // Placeholder - will be filled in next request
                return [];
            case 'Keyset':
                // Placeholder - will be filled in next request
                return [];
            default:
                return [];
        }
    }
    
    private checkColonAssignment(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Skip comments
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

        // Check for variable assignment using : instead of =
        // Match: $varName: (at the start of line, optionally with whitespace)
        const colonAssignmentPattern = /^(\s*)(\$[a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*)(\s*)(:)/;
        const match = codeOnlyLine.match(colonAssignmentPattern);

        if (match) {
            const colonPos = match.index! + match[1].length + match[2].length + match[3].length;
            const range = new vscode.Range(lineNumber, colonPos, lineNumber, colonPos + 1);
            
            const diagnostic = new vscode.Diagnostic(
                range,
                'Use = instead of : for variable assignment',
                vscode.DiagnosticSeverity.Hint
            );
            diagnostic.code = 'use-equals-assignment';
            diagnostics.push(diagnostic);
        }
    }
}

