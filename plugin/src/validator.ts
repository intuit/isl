import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import { IslExtensionsManager, getExtensionFunction, getExtensionModifier } from './extensions';
import { validateControlFlowBalance as validateControlFlowBalanceUtil } from './controlFlowMatcher';
import { getBuiltInModifiersSet, getBuiltInFunctionsSet, getBuiltInNamespacesSet } from './language';
import type { IslTypeManager } from './types';

export class IslValidator {
    private diagnosticCollection: vscode.DiagnosticCollection;
    private validationTimeout: NodeJS.Timeout | undefined;
    private readonly logValidation: (msg: string) => void;

    private readonly builtInModifiers: Set<string>;
    private readonly builtInFunctions: Set<string>;
    private readonly builtInNamespaces: Set<string>;

    constructor(
        private extensionsManager: IslExtensionsManager,
        options?: { outputChannel?: vscode.OutputChannel; typeManager?: IslTypeManager }
    ) {
        this.builtInModifiers = getBuiltInModifiersSet();
        this.builtInFunctions = getBuiltInFunctionsSet();
        this.builtInNamespaces = getBuiltInNamespacesSet();
        this.diagnosticCollection = vscode.languages.createDiagnosticCollection('isl');
        this.logValidation = options?.outputChannel
            ? (msg: string) => options.outputChannel!.appendLine(`[ISL Validation] ${msg}`)
            : () => {};
        this.typeManager = options?.typeManager;
    }

    private readonly typeManager?: IslTypeManager;

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
            this.checkMathInTemplateString(line, i, diagnostics, document);
            this.checkConsecutiveFilters(line, i, diagnostics, document);
            this.checkNamingConvention(line, i, diagnostics, document);
            this.checkMathOutsideBraces(line, i, diagnostics, document);
            this.checkInconsistentSpacing(line, i, diagnostics, document);
        }
        
        // Multi-line checks
        this.checkForeachVariableScoping(document, diagnostics);
        this.checkTypeConversion(document, diagnostics);
        
        // Check for foreach loops that can be converted to map (multi-line check)
        this.checkForeachToMap(document, diagnostics);
        
        // Check for functions that should be modifiers (multi-line check)
        this.checkFunctionToModifier(document, diagnostics);

        // Check for extra properties in typed object literals ($var : Type = { ... })
        if (this.typeManager) {
            await this.checkTypedObjectExtraProperties(document, diagnostics);
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
        let match;
        const processedRanges: Array<{ start: number; end: number }> = [];

        // First: mark all valid @.Name() global extension calls so we never treat them as "module not imported"
        const globalFunctionPattern = /@\.([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/g;
        while ((match = globalFunctionPattern.exec(codeOnlyLine)) !== null) {
            const name = match[1];
            if (name && getExtensionFunction(extensions, name)) {
                processedRanges.push({ start: match.index, end: match.index + match[0].length });
                this.logValidation(`@.${name}() resolved as global extension (single name)`);
            }
        }

        // Now check for @.ModuleName.functionName() pattern (imported functions and built-ins)
        const importedFunctionPattern = /@\.([a-zA-Z_][a-zA-Z0-9_]*)\.([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/g;
        while ((match = importedFunctionPattern.exec(codeOnlyLine)) !== null) {
            const moduleName = match[1];
            const funcName = match[2];
            const matchStart = match.index;
            const matchEnd = matchStart + match[0].length;
            const compoundName = `${moduleName}.${funcName}`;

            // Skip if this span is already a valid global extension call (@.name())
            if (processedRanges.some(r => matchStart >= r.start && matchEnd <= r.end)) {
                continue;
            }

            // Global extension with compound name (e.g. @.Call.Api() where "Call.Api" is in extensions)
            const compoundExtFunc = getExtensionFunction(extensions, compoundName);
            if (compoundExtFunc) {
                processedRanges.push({ start: matchStart, end: matchEnd });
                this.logValidation(`@.${compoundName}() resolved as global extension (compound name)`);
                continue;
            }

            // First: if first part is a global extension function, this is wrong form (@.Call.Api → use @.Call())
            const extFunc = getExtensionFunction(extensions, moduleName);
            if (extFunc && match[0].startsWith('@.' + moduleName + '.')) {
                const startPos = match.index + match[0].indexOf(moduleName);
                const range = new vscode.Range(lineNumber, startPos, lineNumber, startPos + moduleName.length);
                diagnostics.push(new vscode.Diagnostic(
                    range,
                    `Global extension functions are called as @.${moduleName}(), not @.${moduleName}.${funcName}()`,
                    vscode.DiagnosticSeverity.Warning
                ));
                continue;
            }

            // Track this range so we don't process it again
            processedRanges.push({ start: matchStart, end: matchEnd });

            // Skip if it's a built-in namespace (Date, Math, This, etc.)
            if (this.builtInNamespaces.has(moduleName)) {
                this.logValidation(`@.${compoundName}() checking built-in namespace '${moduleName}'`);
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
                
                // For @.This.functionName(), only same-file functions (not global extensions)
                if (moduleName === 'This') {
                    if (userDefinedFunctions.has(funcName)) {
                        this.logValidation(`@.${compoundName}() resolved as same-file function`);
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
                    this.logValidation(`@.${compoundName}() resolved as built-in function`);
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
                    this.logValidation(`@.${compoundName}() resolved as imported function`);
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
                // Module not found - check if it's imported (never report "not imported" for global extension names)
                const imports = this.extractImports(document);
                if (getExtensionFunction(extensions, moduleName)) {
                    // Should have been caught above; treat as wrong form
                    this.logValidation(`@.${compoundName}() wrong form: global extension '${moduleName}' should be called as @.${moduleName}(), not @.${compoundName}()`);
                    const startPos = match.index + match[0].indexOf(moduleName);
                    const range = new vscode.Range(lineNumber, startPos, lineNumber, startPos + moduleName.length);
                    diagnostics.push(new vscode.Diagnostic(
                        range,
                        `Global extension functions are called as @.${moduleName}(), not @.${moduleName}.${funcName}()`,
                        vscode.DiagnosticSeverity.Warning
                    ));
                } else if (getExtensionFunction(extensions, compoundName)) {
                    // Compound global extension (e.g. Call.Api) – valid, don't report "not imported"
                    this.logValidation(`@.${compoundName}() resolved as global extension (compound); skipping "module not imported"`);
                } else if (!imports.has(moduleName)) {
                    this.logValidation(`@.${compoundName}() module '${moduleName}' is not imported (not in extensions as '${moduleName}' or '${compoundName}')`);
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

        // Check for @.SingleName() – global extension functions (called directly by name, like built-ins)
        // Pattern matches only one identifier after the dot (e.g. @.sendEmail(), not @.Date.Now())
        globalFunctionPattern.lastIndex = 0;
        while ((match = globalFunctionPattern.exec(codeOnlyLine)) !== null) {
            const name = match[1];
            if (name && getExtensionFunction(extensions, name)) {
                continue; // valid global extension call
            }
            if (this.builtInNamespaces.has(name)) {
                const startPos = match.index + match[0].indexOf(name);
                const range = new vscode.Range(lineNumber, startPos, lineNumber, startPos + name.length);
                diagnostics.push(new vscode.Diagnostic(
                    range,
                    `Use @.${name}.method() form, not @.${name}()`,
                    vscode.DiagnosticSeverity.Warning
                ));
            }
        }

        // Check for @.This.functionName() calls – only same-file functions
        const thisFunctionPattern = /@\.This\.([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/g;
        while ((match = thisFunctionPattern.exec(codeOnlyLine)) !== null) {
            const matchStart = match.index;
            const matchEnd = matchStart + match[0].length;
            
            if (processedRanges.some(r => matchStart >= r.start && matchEnd <= r.end)) {
                continue;
            }

            const funcName = match[1];
            
            if (userDefinedFunctions.has(funcName)) {
                continue;
            }

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
            if (userDefinedModifiers.has(modifierName) || getExtensionModifier(extensions, modifierName)) {
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
                        const modifierStart = match.index + match[0].indexOf(modifierName) + (moduleName.length + 1);
                        const range = new vscode.Range(lineNumber, modifierStart, lineNumber, modifierStart + modName.length);
                        diagnostics.push(new vscode.Diagnostic(
                            range,
                            `Modifier '${modName}' is not exported from module '${moduleName}'`,
                            vscode.DiagnosticSeverity.Warning
                        ));
                        continue;
                    }
                }
                // Before "module not imported": check if this is a global extension (function or modifier)
                if (getExtensionFunction(extensions, moduleName)) {
                    const moduleStart = match.index + match[0].indexOf(moduleName);
                    const range = new vscode.Range(lineNumber, moduleStart, lineNumber, moduleStart + moduleName.length);
                    diagnostics.push(new vscode.Diagnostic(
                        range,
                        `Use @.${moduleName}() for extension functions, not | ${moduleName}.${modName}`,
                        vscode.DiagnosticSeverity.Warning
                    ));
                    continue;
                }
                if (getExtensionModifier(extensions, moduleName)) {
                    const moduleStart = match.index + match[0].indexOf(moduleName);
                    const range = new vscode.Range(lineNumber, moduleStart, lineNumber, moduleStart + moduleName.length);
                    diagnostics.push(new vscode.Diagnostic(
                        range,
                        `Global extension modifiers are used as | ${moduleName}, not | ${moduleName}.${modName}`,
                        vscode.DiagnosticSeverity.Warning
                    ));
                    continue;
                }
                const imports = this.extractImports(document);
                if (!imports.has(moduleName)) {
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
            // Skip when this is typed assignment: $var : type = ... (colon is type separator, = is assignment)
            const afterColon = codeOnlyLine.substring(match.index! + match[0].length);
            if (/^\s*[a-zA-Z_][a-zA-Z0-9_.:]*\s*=/.test(afterColon)) {
                return; // $val: type = { ... } is valid
            }

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

    private checkMathInTemplateString(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Skip comments
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

        // Find all template strings (backtick strings) in the line
        // Pattern to find backtick strings: `...`
        // We need to be careful about escaped backticks
        const backtickPattern = /`(?:[^`\\]|\\.)*`/g;
        let backtickMatch;
        
        while ((backtickMatch = backtickPattern.exec(codeOnlyLine)) !== null) {
            const templateString = backtickMatch[0];
            const templateStart = backtickMatch.index;
            
            // Extract the content inside the backticks (without the backticks themselves)
            const content = templateString.slice(1, -1);
            
            // Helper function to check if a position range is inside any of the exclusion blocks
            const isInsideExclusionBlocks = (start: number, end: number, exclusionBlocks: Array<{ start: number; end: number }>): boolean => {
                return exclusionBlocks.some(block => start >= block.start && end <= block.end);
            };
            
            // Find all {{ ... }} blocks to exclude them (these are already correct)
            const mathBlockPattern = /\{\{([^}]+)\}\}/g;
            const mathBlocks: Array<{ start: number; end: number }> = [];
            let mathBlockMatch;
            while ((mathBlockMatch = mathBlockPattern.exec(content)) !== null) {
                mathBlocks.push({
                    start: mathBlockMatch.index,
                    end: mathBlockMatch.index + mathBlockMatch[0].length
                });
            }
            
            // Find all ${ ... } interpolation blocks to exclude them (these are correct)
            const interpolationPattern = /\$\{([^}]+)\}/g;
            const interpolationBlocks: Array<{ start: number; end: number }> = [];
            let interpolationMatch;
            while ((interpolationMatch = interpolationPattern.exec(content)) !== null) {
                interpolationBlocks.push({
                    start: interpolationMatch.index,
                    end: interpolationMatch.index + interpolationMatch[0].length
                });
            }
            
            // Combine all exclusion blocks
            const allExclusionBlocks = [...mathBlocks, ...interpolationBlocks];
            
            // Find math expressions: $var operator (number|$var) or (number|$var) operator $var
            // Math operators: +, -, *, /, %
            // Pattern matches: $var * 1.1, $var + $var, 10 * $var, etc.
            const mathExpressionPattern = /((?:\$[a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*|\d+(?:\.\d+)?))\s*([+\-*/%])\s*((?:\$[a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*|\d+(?:\.\d+)?))/g;
            let mathMatch;
            while ((mathMatch = mathExpressionPattern.exec(content)) !== null) {
                const matchStart = mathMatch.index;
                const matchEnd = matchStart + mathMatch[0].length;
                
                // Skip if already inside an exclusion block
                if (isInsideExclusionBlocks(matchStart, matchEnd, allExclusionBlocks)) {
                    continue;
                }
                
                // This is a math expression that needs {{ }} wrapping
                // Calculate the absolute position in the document line
                const absoluteStart = templateStart + 1 + matchStart; // +1 for opening backtick
                const absoluteEnd = templateStart + 1 + matchEnd;
                
                const range = new vscode.Range(
                    lineNumber,
                    absoluteStart,
                    lineNumber,
                    absoluteEnd
                );
                
                const diagnostic = new vscode.Diagnostic(
                    range,
                    'Math expressions in template strings must be wrapped in {{ }}',
                    vscode.DiagnosticSeverity.Warning
                );
                diagnostic.code = 'math-outside-braces';
                diagnostics.push(diagnostic);
            }
        }
    }

    private checkConsecutiveFilters(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Skip comments
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

        // Find consecutive filter operations: | filter(...) | filter(...)
        // Pattern: | filter(condition) | filter(condition)
        const filterPattern = /\|\s*filter\s*\(([^)]+)\)/g;
        const filterMatches: Array<{ match: RegExpMatchArray; condition: string; start: number; end: number }> = [];
        
        let filterMatch;
        while ((filterMatch = filterPattern.exec(codeOnlyLine)) !== null) {
            filterMatches.push({
                match: filterMatch,
                condition: filterMatch[1].trim(),
                start: filterMatch.index,
                end: filterMatch.index + filterMatch[0].length
            });
        }

        // Check if we have at least 2 consecutive filters
        if (filterMatches.length < 2) {
            return;
        }

        // Find the longest sequence of consecutive filters
        let consecutiveStart = -1;
        let consecutiveEnd = -1;
        
        for (let i = 0; i < filterMatches.length - 1; i++) {
            const currentFilter = filterMatches[i];
            const nextFilter = filterMatches[i + 1];
            
            // Get the text between the two filters
            const textBetween = codeOnlyLine.substring(currentFilter.end, nextFilter.start);
            
            // If there's only whitespace or pipes between them, they're consecutive
            const betweenTrimmed = textBetween.trim();
            if (betweenTrimmed === '' || betweenTrimmed === '|') {
                // Mark the start of consecutive sequence
                if (consecutiveStart === -1) {
                    consecutiveStart = currentFilter.start;
                }
                consecutiveEnd = nextFilter.end;
            } else {
                // If we found a consecutive sequence, create diagnostic and reset
                if (consecutiveStart !== -1 && consecutiveEnd !== -1) {
                    const range = new vscode.Range(
                        lineNumber,
                        consecutiveStart,
                        lineNumber,
                        consecutiveEnd
                    );
                    
                    const diagnostic = new vscode.Diagnostic(
                        range,
                        'Consecutive filter operations can be combined with "and" operator',
                        vscode.DiagnosticSeverity.Hint
                    );
                    diagnostic.code = 'inefficient-filter';
                    diagnostics.push(diagnostic);
                    
                    // Reset for next potential sequence
                    consecutiveStart = -1;
                    consecutiveEnd = -1;
                }
            }
        }
        
        // Check if we ended with a consecutive sequence
        if (consecutiveStart !== -1 && consecutiveEnd !== -1) {
            const range = new vscode.Range(
                lineNumber,
                consecutiveStart,
                lineNumber,
                consecutiveEnd
            );
            
            const diagnostic = new vscode.Diagnostic(
                range,
                'Consecutive filter operations can be combined with "and" operator',
                vscode.DiagnosticSeverity.Hint
            );
            diagnostic.code = 'inefficient-filter';
            diagnostics.push(diagnostic);
        }
    }

    private checkForeachToMap(document: vscode.TextDocument, diagnostics: vscode.Diagnostic[]) {
        const text = document.getText();
        const lines = text.split('\n');
        
        // Find all foreach loops
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            const trimmed = line.trim();
            
            // Check if this line starts a foreach loop
            const foreachMatch = trimmed.match(/^foreach\s+\$([a-zA-Z_][a-zA-Z0-9_]*)\s+in\s+(.+)/);
            if (!foreachMatch) {
                continue;
            }
            
            const loopVar = foreachMatch[1];
            const arrayVar = foreachMatch[2].trim();
            const foreachStartLine = i;
            
            // Find the matching endfor
            let braceDepth = 0;
            let foundEndfor = false;
            let endforLine = -1;
            
            for (let j = i + 1; j < lines.length; j++) {
                const currentLine = lines[j];
                const currentTrimmed = currentLine.trim();
                
                // Skip comments
                const commentIndex = Math.min(
                    currentLine.indexOf('//') !== -1 ? currentLine.indexOf('//') : Infinity,
                    currentLine.indexOf('#') !== -1 ? currentLine.indexOf('#') : Infinity
                );
                const codeLine = commentIndex !== Infinity ? currentLine.substring(0, commentIndex) : currentLine;
                
                // Count braces to handle nested structures
                braceDepth += (codeLine.match(/\{/g) || []).length;
                braceDepth -= (codeLine.match(/\}/g) || []).length;
                
                if (currentTrimmed === 'endfor' && braceDepth === 0) {
                    foundEndfor = true;
                    endforLine = j;
                    break;
                }
            }
            
            if (!foundEndfor) {
                continue; // Skip if no matching endfor found
            }
            
            // Check lines before the foreach for array initialization
            // Look for pattern: $varName: [] or $varName = []
            let arrayVarName: string | null = null;
            let arrayInitLine = -1;
            
            for (let j = Math.max(0, foreachStartLine - 5); j < foreachStartLine; j++) {
                const prevLine = lines[j];
                const commentIndex = Math.min(
                    prevLine.indexOf('//') !== -1 ? prevLine.indexOf('//') : Infinity,
                    prevLine.indexOf('#') !== -1 ? prevLine.indexOf('#') : Infinity
                );
                const codeLine = commentIndex !== Infinity ? prevLine.substring(0, commentIndex) : prevLine;
                
                // Match: $varName: [] or $varName = []
                const arrayInitMatch = codeLine.match(/\$([a-zA-Z_][a-zA-Z0-9_]*)\s*[=:]\s*\[\s*\]/);
                if (arrayInitMatch) {
                    arrayVarName = arrayInitMatch[1];
                    arrayInitLine = j;
                    break;
                }
            }
            
            if (!arrayVarName) {
                continue; // No array initialization found
            }
            
            // Check the loop body for push operations
            // Pattern: $arrayVarName: $arrayVarName | push(expression)
            let foundPush = false;
            let pushExpression: string | null = null;
            let pushLine = -1;
            let hasOtherStatements = false;
            
            for (let j = foreachStartLine + 1; j < endforLine; j++) {
                const bodyLine = lines[j];
                const trimmedBody = bodyLine.trim();
                
                // Skip empty lines and comments
                if (trimmedBody === '' || trimmedBody.startsWith('//') || trimmedBody.startsWith('#')) {
                    continue;
                }
                
                const commentIndex = Math.min(
                    bodyLine.indexOf('//') !== -1 ? bodyLine.indexOf('//') : Infinity,
                    bodyLine.indexOf('#') !== -1 ? bodyLine.indexOf('#') : Infinity
                );
                const codeLine = commentIndex !== Infinity ? bodyLine.substring(0, commentIndex) : bodyLine;
                const trimmedCode = codeLine.trim();
                
                // Match: $arrayVarName: $arrayVarName | push(expression)
                // Also match: $arrayVarName = $arrayVarName | push(expression)
                const pushPattern = new RegExp(`\\$${arrayVarName}\\s*[=:]\\s*\\$${arrayVarName}\\s*\\|\\s*push\\s*\\(([^)]+)\\)`, 'g');
                const pushMatch = pushPattern.exec(trimmedCode);
                
                if (pushMatch) {
                    if (!foundPush) {
                        foundPush = true;
                        pushExpression = pushMatch[1].trim();
                        pushLine = j;
                    }
                } else {
                    // Check if this is another statement (not just whitespace)
                    if (trimmedCode.length > 0) {
                        hasOtherStatements = true;
                    }
                }
            }
            
            // Only suggest conversion if we found a push and no other statements
            if (!foundPush || !pushExpression || hasOtherStatements) {
                continue;
            }
            
            // Check if the push expression uses the loop variable
            // Replace $loopVar with $ in the expression to convert to map syntax
            const mapExpression = pushExpression.replace(new RegExp(`\\$${loopVar}`, 'g'), '$');
            
            // Create diagnostic covering the foreach loop
            const foreachLine = document.lineAt(foreachStartLine);
            const endforLineObj = document.lineAt(endforLine);
            const range = new vscode.Range(
                foreachStartLine,
                0,
                endforLine,
                endforLineObj.text.length
            );
            
            const diagnostic = new vscode.Diagnostic(
                range,
                'Foreach loop can be replaced with map() modifier',
                vscode.DiagnosticSeverity.Hint
            );
            diagnostic.code = 'foreach-to-map';
            // Store metadata for the quick fix
            (diagnostic as any).arrayVarName = arrayVarName;
            (diagnostic as any).arrayInitLine = arrayInitLine;
            (diagnostic as any).arrayVar = arrayVar;
            (diagnostic as any).mapExpression = mapExpression;
            (diagnostic as any).foreachStartLine = foreachStartLine;
            (diagnostic as any).endforLine = endforLine;
            diagnostics.push(diagnostic);
        }
    }

    private checkNamingConvention(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Get naming convention from configuration
        const config = vscode.workspace.getConfiguration('isl.naming');
        const convention = config.get<string>('convention', 'camelCase');
        
        // Skip if convention is not set or disabled
        if (!convention) {
            return;
        }

        // Skip comments
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

        // Check function declarations: fun functionName(
        const funMatch = codeOnlyLine.match(/^\s*fun\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/);
        if (funMatch) {
            const funcName = funMatch[1];
            const funcNameStart = codeOnlyLine.indexOf(funcName);
            
            if (!this.matchesNamingConvention(funcName, convention)) {
                const correctName = this.convertToNamingConvention(funcName, convention);
                const range = new vscode.Range(
                    lineNumber,
                    funcNameStart,
                    lineNumber,
                    funcNameStart + funcName.length
                );
                
                const diagnostic = new vscode.Diagnostic(
                    range,
                    `Function name '${funcName}' should be ${convention}. Suggested: '${correctName}'`,
                    vscode.DiagnosticSeverity.Hint
                );
                diagnostic.code = 'naming-convention';
                (diagnostic as any).originalName = funcName;
                (diagnostic as any).correctName = correctName;
                (diagnostic as any).type = 'function';
                diagnostics.push(diagnostic);
            }
        }

        // Check modifier declarations: modifier modifierName(
        const modMatch = codeOnlyLine.match(/^\s*modifier\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/);
        if (modMatch) {
            const modName = modMatch[1];
            const modNameStart = codeOnlyLine.indexOf(modName);
            
            if (!this.matchesNamingConvention(modName, convention)) {
                const correctName = this.convertToNamingConvention(modName, convention);
                const range = new vscode.Range(
                    lineNumber,
                    modNameStart,
                    lineNumber,
                    modNameStart + modName.length
                );
                
                const diagnostic = new vscode.Diagnostic(
                    range,
                    `Modifier name '${modName}' should be ${convention}. Suggested: '${correctName}'`,
                    vscode.DiagnosticSeverity.Hint
                );
                diagnostic.code = 'naming-convention';
                (diagnostic as any).originalName = modName;
                (diagnostic as any).correctName = correctName;
                (diagnostic as any).type = 'modifier';
                diagnostics.push(diagnostic);
            }
        }
    }

    private matchesNamingConvention(name: string, convention: string): boolean {
        switch (convention) {
            case 'PascalCase':
                // PascalCase: First letter uppercase, rest can be lowercase or uppercase
                // Examples: TransformVariant, GetUser, ProcessData
                return /^[A-Z][a-zA-Z0-9]*$/.test(name);
            case 'camelCase':
                // camelCase: First letter lowercase, rest can be mixed
                // Examples: transformVariant, getUser, processData
                return /^[a-z][a-zA-Z0-9]*$/.test(name);
            case 'snake_case':
                // snake_case: All lowercase with underscores
                // Examples: transform_variant, get_user, process_data
                return /^[a-z][a-z0-9_]*$/.test(name) && !/[A-Z]/.test(name);
            default:
                return true; // Unknown convention, don't enforce
        }
    }

    private convertToNamingConvention(name: string, convention: string): string {
        // First, normalize the name by splitting on capital letters, underscores, and numbers
        // This handles: PascalCase, camelCase, snake_case, and mixed cases
        
        // Split on capital letters, underscores, and numbers
        const parts: string[] = [];
        let currentPart = '';
        
        for (let i = 0; i < name.length; i++) {
            const char = name[i];
            const isUpper = /[A-Z]/.test(char);
            const isLower = /[a-z]/.test(char);
            const isUnderscore = char === '_';
            const isNumber = /[0-9]/.test(char);
            
            if (isUnderscore) {
                if (currentPart) {
                    parts.push(currentPart.toLowerCase());
                    currentPart = '';
                }
            } else if (isUpper && currentPart && /[a-z]/.test(currentPart[currentPart.length - 1])) {
                // Capital letter after lowercase - start new part
                parts.push(currentPart.toLowerCase());
                currentPart = char;
            } else {
                currentPart += char;
            }
        }
        
        if (currentPart) {
            parts.push(currentPart.toLowerCase());
        }
        
        // Filter out empty parts
        const cleanParts = parts.filter(p => p.length > 0);
        
        if (cleanParts.length === 0) {
            return name; // Can't convert, return original
        }
        
        switch (convention) {
            case 'PascalCase':
                // Capitalize first letter of each part
                return cleanParts.map(p => p.charAt(0).toUpperCase() + p.slice(1)).join('');
            case 'camelCase':
                // First part lowercase, rest capitalized
                return cleanParts[0] + cleanParts.slice(1).map(p => p.charAt(0).toUpperCase() + p.slice(1)).join('');
            case 'snake_case':
                // All lowercase with underscores
                return cleanParts.join('_');
            default:
                return name; // Unknown convention, return original
        }
    }

    private checkFunctionToModifier(document: vscode.TextDocument, diagnostics: vscode.Diagnostic[]) {
        const text = document.getText();
        const lines = text.split('\n');
        
        // First, extract all user-defined functions
        const userDefinedFunctions = this.extractUserDefinedFunctions(document);
        
        // Track how each function is used
        const functionUsage: Map<string, { withPipe: number; withoutPipe: number; definitionLine: number }> = new Map();
        
        // Initialize usage tracking
        for (const funcName of userDefinedFunctions) {
            functionUsage.set(funcName, { withPipe: 0, withoutPipe: 0, definitionLine: -1 });
        }
        
        // Find function definitions and record their line numbers
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            const funMatch = line.match(/^\s*fun\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/);
            if (funMatch) {
                const funcName = funMatch[1];
                if (functionUsage.has(funcName)) {
                    functionUsage.get(funcName)!.definitionLine = i;
                }
            }
        }
        
        // Check how functions are used
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            
            // Skip comments
            const commentIndex = Math.min(
                line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
                line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
            );
            const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;
            
            // Check for function calls: @.This.functionName( or @.ModuleName.functionName(
            const functionCallPattern = /@\.([a-zA-Z_][a-zA-Z0-9_]*)\.([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/g;
            let match;
            
            while ((match = functionCallPattern.exec(codeOnlyLine)) !== null) {
                const moduleName = match[1];
                const funcName = match[2];
                
                // Only check user-defined functions (skip built-ins)
                if (moduleName === 'This' && functionUsage.has(funcName)) {
                    // Check if this call is preceded by a pipe operator
                    // Look for | before @.This.functionName on the same line
                    const callStart = match.index;
                    const beforeCall = codeOnlyLine.substring(0, callStart);
                    // Check if there's a pipe operator before the call (with optional whitespace)
                    const hasPipe = /\|\s*$/.test(beforeCall.trim()) || /\|\s+@\.This\./.test(beforeCall);
                    
                    if (hasPipe) {
                        functionUsage.get(funcName)!.withPipe++;
                    } else {
                        functionUsage.get(funcName)!.withoutPipe++;
                    }
                }
            }
        }
        
        // Check if any functions are always used with pipe
        for (const [funcName, usage] of functionUsage.entries()) {
            // Only suggest conversion if:
            // 1. Function is used at least once with pipe
            // 2. Function is never used without pipe (or used with pipe more than without)
            // 3. We found the definition line
            if (usage.withPipe > 0 && usage.withoutPipe === 0 && usage.definitionLine >= 0) {
                const definitionLine = document.lineAt(usage.definitionLine);
                const funMatch = definitionLine.text.match(/^\s*(fun)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/);
                
                if (funMatch) {
                    const funKeywordStart = definitionLine.text.indexOf('fun');
                    const range = new vscode.Range(
                        usage.definitionLine,
                        funKeywordStart,
                        usage.definitionLine,
                        funKeywordStart + 3
                    );
                    
                    const diagnostic = new vscode.Diagnostic(
                        range,
                        `Function '${funcName}' is always used with pipe operator. Consider converting to modifier.`,
                        vscode.DiagnosticSeverity.Hint
                    );
                    diagnostic.code = 'function-to-modifier';
                    (diagnostic as any).functionName = funcName;
                    (diagnostic as any).definitionLine = usage.definitionLine;
                    diagnostics.push(diagnostic);
                }
            }
        }
    }

    /**
     * Checks typed object literals ($var : Type = { ... }) for properties not declared in the schema.
     */
    private async checkTypedObjectExtraProperties(document: vscode.TextDocument, diagnostics: vscode.Diagnostic[]): Promise<void> {
        const text = document.getText();

        // Find all { } pairs
        const pairs: { start: number; end: number }[] = [];
        const openBraces: number[] = [];
        for (let i = 0; i < text.length; i++) {
            const ch = text[i];
            if (ch === '{') openBraces.push(i);
            else if (ch === '}') {
                if (openBraces.length > 0) {
                    pairs.push({ start: openBraces.pop()!, end: i });
                }
            }
        }

        for (const { start, end } of pairs) {
            const position = document.positionAt(start + 1);
            const schemaAt = await this.typeManager!.getSchemaForObjectAt(document, position);
            if (!schemaAt) continue;

            const { schema } = schemaAt;
            const content = text.substring(start + 1, end);
            const propsWithRanges = this.getTopLevelPropertiesWithRanges(document, content, start + 1);

            for (const { name, range } of propsWithRanges) {
                if (!(name in schema.properties)) {
                    diagnostics.push(new vscode.Diagnostic(
                        range,
                        `Property '${name}' does not seem declared on the schema.`,
                        vscode.DiagnosticSeverity.Warning
                    ));
                }
            }
        }
    }

    /** Extracts top-level property names and their ranges from object content. */
    private getTopLevelPropertiesWithRanges(
        document: vscode.TextDocument,
        content: string,
        contentStartOffset: number
    ): Array<{ name: string; range: vscode.Range }> {
        const result: Array<{ name: string; range: vscode.Range }> = [];
        let depth = 0;
        let i = 0;
        let inString = false;
        let stringChar = '';

        while (i < content.length) {
            const c = content[i];
            if (inString) {
                if (c === '\\') {
                    i += 2;
                    continue;
                }
                if (c === stringChar) inString = false;
                i++;
                continue;
            }
            if (c === '"' || c === "'" || c === '`') {
                inString = true;
                stringChar = c;
                i++;
                continue;
            }
            if (c === '{' || c === '[' || c === '(') {
                depth++;
                i++;
                continue;
            }
            if (c === '}' || c === ']' || c === ')') {
                depth--;
                i++;
                continue;
            }
            if (depth === 0) {
                const propMatch = content.slice(i).match(/^([a-zA-Z_][a-zA-Z0-9_]*)\s*:/);
                if (propMatch) {
                    const name = propMatch[1];
                    const nameStart = contentStartOffset + i;
                    const nameEnd = nameStart + name.length;
                    result.push({
                        name,
                        range: new vscode.Range(document.positionAt(nameStart), document.positionAt(nameEnd))
                    });
                    i += propMatch[0].length;
                    continue;
                }
            }
            i++;
        }
        return result;
    }

    private checkMathOutsideBraces(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Skip comments
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

        // Skip if line is empty or only whitespace
        if (codeOnlyLine.trim() === '') {
            return;
        }

        // Find all {{ ... }} blocks to exclude them
        const mathBlockPattern = /\{\{([^}]+)\}\}/g;
        const mathBlocks: Array<{ start: number; end: number }> = [];
        let mathBlockMatch;
        while ((mathBlockMatch = mathBlockPattern.exec(codeOnlyLine)) !== null) {
            mathBlocks.push({
                start: mathBlockMatch.index,
                end: mathBlockMatch.index + mathBlockMatch[0].length
            });
        }

        // Find all template strings (backtick strings) to exclude them (already handled by checkMathInTemplateString)
        const backtickPattern = /`(?:[^`\\]|\\.)*`/g;
        const templateStrings: Array<{ start: number; end: number }> = [];
        let backtickMatch;
        while ((backtickMatch = backtickPattern.exec(codeOnlyLine)) !== null) {
            templateStrings.push({
                start: backtickMatch.index,
                end: backtickMatch.index + backtickMatch[0].length
            });
        }

        // Combine exclusion blocks
        const allExclusionBlocks = [...mathBlocks, ...templateStrings];

        // Helper function to check if a position is inside exclusion blocks
        const isInsideExclusionBlocks = (start: number, end: number): boolean => {
            return allExclusionBlocks.some(block => start >= block.start && end <= block.end);
        };

        // Find math operations: $var operator (number|$var) or (number|$var) operator $var
        // Math operators: +, -, *, /, %
        // But exclude comparison operators: ==, !=, <, >, <=, >=
        // Pattern: variable/number operator variable/number
        const mathExpressionPattern = /((?:\$[a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*|\d+(?:\.\d+)?))\s*([+\-*/%])\s*((?:\$[a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*|\d+(?:\.\d+)?))/g;
        let mathMatch;
        
        while ((mathMatch = mathExpressionPattern.exec(codeOnlyLine)) !== null) {
            const matchStart = mathMatch.index;
            const matchEnd = matchStart + mathMatch[0].length;
            
            // Skip if already inside an exclusion block
            if (isInsideExclusionBlocks(matchStart, matchEnd)) {
                continue;
            }

            // Check if this is part of a comparison operator (==, !=, <=, >=, <, >)
            // Look at characters before and after the operator
            const beforeOp = codeOnlyLine.substring(Math.max(0, matchStart - 1), matchStart);
            const afterOp = codeOnlyLine.substring(matchEnd, Math.min(codeOnlyLine.length, matchEnd + 1));
            const operator = mathMatch[2];
            
            // Skip if it's part of a comparison
            if (operator === '=' && (beforeOp === '=' || beforeOp === '!' || afterOp === '=')) {
                continue; // == or != or <= or >=
            }
            if (operator === '<' && afterOp === '=') {
                continue; // <=
            }
            if (operator === '>' && afterOp === '=') {
                continue; // >=
            }
            if (operator === '<' || operator === '>') {
                // Could be comparison, but also could be math in some contexts
                // Check if it's in a condition context (if, while, etc.)
                const beforeMatch = codeOnlyLine.substring(0, matchStart);
                if (beforeMatch.match(/\b(if|while|switch|filter|map)\s*\(/)) {
                    continue; // Likely a comparison in a condition
                }
            }

            // This is a math expression that needs {{ }} wrapping
            const range = new vscode.Range(
                lineNumber,
                matchStart,
                lineNumber,
                matchEnd
            );
            
            const diagnostic = new vscode.Diagnostic(
                range,
                'Math operations must be wrapped in {{ }}',
                vscode.DiagnosticSeverity.Warning
            );
            diagnostic.code = 'math-outside-braces';
            diagnostics.push(diagnostic);
        }
    }

    private checkInconsistentSpacing(line: string, lineNumber: number, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        // Skip comments
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

        // Check for missing spaces around operators and pipes
        // Pattern: $var:value (missing space after :)
        // Pattern: $var|modifier (missing space before |)
        // Pattern: $var| modifier (missing space after |)
        
        // Check for missing space after : in assignments
        const colonPattern = /(\$[a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*)\s*:\s*([^\s=:])/g;
        let colonMatch;
        while ((colonMatch = colonPattern.exec(codeOnlyLine)) !== null) {
            const colonPos = codeOnlyLine.indexOf(':', colonMatch.index);
            if (colonPos !== -1 && codeOnlyLine[colonPos + 1] !== ' ') {
                const range = new vscode.Range(
                    lineNumber,
                    colonPos + 1,
                    lineNumber,
                    colonPos + 1
                );
                
                const diagnostic = new vscode.Diagnostic(
                    range,
                    'Missing space after :',
                    vscode.DiagnosticSeverity.Hint
                );
                diagnostic.code = 'inconsistent-spacing';
                diagnostics.push(diagnostic);
            }
        }

        // Check for missing spaces around pipe operator |
        const pipePattern = /([^\s|])\s*\|([^\s|])/g;
        let pipeMatch;
        while ((pipeMatch = pipePattern.exec(codeOnlyLine)) !== null) {
            const pipePos = codeOnlyLine.indexOf('|', pipeMatch.index);
            if (pipePos !== -1) {
                const beforePipe = codeOnlyLine[pipePos - 1];
                const afterPipe = codeOnlyLine[pipePos + 1];
                
                if (beforePipe !== ' ' || afterPipe !== ' ') {
                    const range = new vscode.Range(
                        lineNumber,
                        pipePos,
                        lineNumber,
                        pipePos + 1
                    );
                    
                    const diagnostic = new vscode.Diagnostic(
                        range,
                        'Missing space around | operator',
                        vscode.DiagnosticSeverity.Hint
                    );
                    diagnostic.code = 'inconsistent-spacing';
                    diagnostics.push(diagnostic);
                }
            }
        }
    }

    private checkForeachVariableScoping(document: vscode.TextDocument, diagnostics: vscode.Diagnostic[]) {
        const text = document.getText();
        const lines = text.split('\n');
        
        // Track foreach loops and their variables
        const foreachLoops: Array<{ loopVar: string; startLine: number; endLine: number }> = [];
        
        // Find all foreach loops
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            const trimmed = line.trim();
            
            const foreachMatch = trimmed.match(/^foreach\s+\$([a-zA-Z_][a-zA-Z0-9_]*)\s+in\s+(.+)/);
            if (foreachMatch) {
                const loopVar = foreachMatch[1];
                const foreachStartLine = i;
                
                // Find the matching endfor
                let braceDepth = 0;
                let foundEndfor = false;
                let endforLine = -1;
                
                for (let j = i + 1; j < lines.length; j++) {
                    const currentLine = lines[j];
                    const currentTrimmed = currentLine.trim();
                    
                    const commentIndex = Math.min(
                        currentLine.indexOf('//') !== -1 ? currentLine.indexOf('//') : Infinity,
                        currentLine.indexOf('#') !== -1 ? currentLine.indexOf('#') : Infinity
                    );
                    const codeLine = commentIndex !== Infinity ? currentLine.substring(0, commentIndex) : currentLine;
                    
                    braceDepth += (codeLine.match(/\{/g) || []).length;
                    braceDepth -= (codeLine.match(/\}/g) || []).length;
                    
                    if (currentTrimmed === 'endfor' && braceDepth === 0) {
                        foundEndfor = true;
                        endforLine = j;
                        break;
                    }
                }
                
                if (foundEndfor) {
                    foreachLoops.push({
                        loopVar,
                        startLine: foreachStartLine,
                        endLine: endforLine
                    });
                }
            }
        }
        
        // Check if loop variables are used after their loops
        for (const loop of foreachLoops) {
            for (let i = loop.endLine + 1; i < lines.length; i++) {
                const line = lines[i];
                
                // Skip comments
                const commentIndex = Math.min(
                    line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
                    line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
                );
                const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;
                
                // Check if loop variable is used
                const varPattern = new RegExp(`\\$${loop.loopVar}(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*\\b`, 'g');
                const varMatch = varPattern.exec(codeOnlyLine);
                
                if (varMatch) {
                    const range = new vscode.Range(
                        i,
                        varMatch.index,
                        i,
                        varMatch.index + varMatch[0].length
                    );
                    
                    const diagnostic = new vscode.Diagnostic(
                        range,
                        `Loop variable '$${loop.loopVar}' is used outside its loop scope. Consider using map() instead.`,
                        vscode.DiagnosticSeverity.Warning
                    );
                    diagnostic.code = 'foreach-variable-scoping';
                    (diagnostic as any).loopVar = loop.loopVar;
                    (diagnostic as any).foreachStartLine = loop.startLine;
                    diagnostics.push(diagnostic);
                    break; // Only flag first occurrence
                }
            }
        }
    }

    private checkTypeConversion(document: vscode.TextDocument, diagnostics: vscode.Diagnostic[]) {
        const text = document.getText();
        const lines = text.split('\n');
        
        // Patterns that suggest implicit type conversion
        // $number: $stringValue (assigning string to number variable)
        // $boolean: $stringValue (assigning string to boolean variable)
        // etc.
        
        // This is a heuristic check - we look for variable names that suggest types
        // and assignments from variables that might be different types
        
        const typeSuggestingNames = {
            number: /\b(number|num|count|total|sum|price|amount|quantity|qty|index|id|size|length)\b/i,
            boolean: /\b(boolean|bool|is|has|can|should|enabled|active|valid|flag)\b/i,
            string: /\b(string|str|text|name|label|message|description|title|value)\b/i
        };
        
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            
            // Skip comments
            const commentIndex = Math.min(
                line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
                line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
            );
            const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;
            
            // Match variable assignments: $varName = $otherVar or $varName: $otherVar
            const assignmentMatch = codeOnlyLine.match(/^\s*\$([a-zA-Z_][a-zA-Z0-9_]*)\s*[=:]\s*\$([a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*)/);
            if (!assignmentMatch) {
                continue;
            }
            
            const leftVar = assignmentMatch[1];
            const rightVar = assignmentMatch[2];
            
            // Check if left variable name suggests a type
            let suggestedType: string | null = null;
            for (const [type, pattern] of Object.entries(typeSuggestingNames)) {
                if (pattern.test(leftVar)) {
                    suggestedType = type;
                    break;
                }
            }
            
            if (!suggestedType) {
                continue;
            }
            
            // Check if right variable name suggests a different type
            let rightVarType: string | null = null;
            for (const [type, pattern] of Object.entries(typeSuggestingNames)) {
                if (pattern.test(rightVar)) {
                    rightVarType = type;
                    break;
                }
            }
            
            // If types don't match, suggest explicit conversion
            if (rightVarType && rightVarType !== suggestedType) {
                const rightVarStart = codeOnlyLine.indexOf('$' + rightVar);
                const range = new vscode.Range(
                    i,
                    rightVarStart,
                    i,
                    rightVarStart + rightVar.length + 1 // Include the $
                );
                
                const conversionModifier = `to.${suggestedType === 'number' ? 'number' : suggestedType === 'boolean' ? 'boolean' : 'string'}`;
                const diagnostic = new vscode.Diagnostic(
                    range,
                    `Implicit type conversion detected. Consider using explicit conversion: | ${conversionModifier}`,
                    vscode.DiagnosticSeverity.Hint
                );
                diagnostic.code = 'implicit-type-conversion';
                (diagnostic as any).conversionModifier = conversionModifier;
                (diagnostic as any).rightVar = '$' + rightVar;
                diagnostics.push(diagnostic);
            }
        }
    }
}

