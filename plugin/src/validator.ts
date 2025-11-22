import * as vscode from 'vscode';

export class IslValidator {
    private diagnosticCollection: vscode.DiagnosticCollection;
    private validationTimeout: NodeJS.Timeout | undefined;

    constructor() {
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
        // Check for unmatched interpolation syntax
        const backtickCount = (line.match(/`/g) || []).length;
        if (backtickCount % 2 !== 0) {
            const lastBacktick = line.lastIndexOf('`');
            const range = new vscode.Range(lineNumber, lastBacktick, lineNumber, lastBacktick + 1);
            diagnostics.push(new vscode.Diagnostic(
                range,
                'Unmatched backtick for string interpolation',
                vscode.DiagnosticSeverity.Warning
            ));
        }

        // Check for invalid interpolation syntax
        const interpolationPattern = /`[^`]*\$\{([^}]*)\}/g;
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

    private checkControlFlowBalance(text: string, diagnostics: vscode.Diagnostic[], document: vscode.TextDocument) {
        const lines = text.split('\n');
        
        const controlFlowStack: Array<{ type: string, line: number }> = [];

        for (let i = 0; i < lines.length; i++) {
            const line = lines[i].trim();

            if (line.startsWith('if ') || line.startsWith('if(')) {
                controlFlowStack.push({ type: 'if', line: i });
            } else if (line === 'endif') {
                if (controlFlowStack.length === 0 || controlFlowStack[controlFlowStack.length - 1].type !== 'if') {
                    const range = new vscode.Range(i, 0, i, line.length);
                    diagnostics.push(new vscode.Diagnostic(
                        range,
                        'Unexpected endif without matching if',
                        vscode.DiagnosticSeverity.Error
                    ));
                } else {
                    controlFlowStack.pop();
                }
            } else if (line.startsWith('foreach ')) {
                controlFlowStack.push({ type: 'foreach', line: i });
            } else if (line === 'endfor') {
                if (controlFlowStack.length === 0 || controlFlowStack[controlFlowStack.length - 1].type !== 'foreach') {
                    const range = new vscode.Range(i, 0, i, line.length);
                    diagnostics.push(new vscode.Diagnostic(
                        range,
                        'Unexpected endfor without matching foreach',
                        vscode.DiagnosticSeverity.Error
                    ));
                } else {
                    controlFlowStack.pop();
                }
            } else if (line.startsWith('while ')) {
                controlFlowStack.push({ type: 'while', line: i });
            } else if (line === 'endwhile') {
                if (controlFlowStack.length === 0 || controlFlowStack[controlFlowStack.length - 1].type !== 'while') {
                    const range = new vscode.Range(i, 0, i, line.length);
                    diagnostics.push(new vscode.Diagnostic(
                        range,
                        'Unexpected endwhile without matching while',
                        vscode.DiagnosticSeverity.Error
                    ));
                } else {
                    controlFlowStack.pop();
                }
            } else if (line.startsWith('switch ')) {
                controlFlowStack.push({ type: 'switch', line: i });
            } else if (line === 'endswitch') {
                if (controlFlowStack.length === 0 || controlFlowStack[controlFlowStack.length - 1].type !== 'switch') {
                    const range = new vscode.Range(i, 0, i, line.length);
                    diagnostics.push(new vscode.Diagnostic(
                        range,
                        'Unexpected endswitch without matching switch',
                        vscode.DiagnosticSeverity.Error
                    ));
                } else {
                    controlFlowStack.pop();
                }
            }
        }

        // Check for unclosed control flow statements
        for (const item of controlFlowStack) {
            const range = new vscode.Range(item.line, 0, item.line, lines[item.line].length);
            diagnostics.push(new vscode.Diagnostic(
                range,
                `Unclosed ${item.type} statement`,
                vscode.DiagnosticSeverity.Error
            ));
        }
    }

    private isInsideFunction(document: vscode.TextDocument, lineNumber: number): boolean {
        let functionDepth = 0;
        
        for (let i = 0; i <= lineNumber; i++) {
            const line = document.lineAt(i).text.trim();
            if (line.match(/^\s*(fun|modifier)\s+/)) {
                functionDepth++;
            }
            if (line === '}' && functionDepth > 0) {
                functionDepth--;
            }
        }
        
        return functionDepth > 0;
    }
}

