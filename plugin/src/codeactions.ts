import * as vscode from 'vscode';

export class IslCodeActionProvider implements vscode.CodeActionProvider {
    
    public static readonly providedCodeActionKinds = [
        vscode.CodeActionKind.QuickFix,
        vscode.CodeActionKind.Refactor,
        vscode.CodeActionKind.RefactorExtract,
        vscode.CodeActionKind.RefactorRewrite
    ];
    
    provideCodeActions(
        document: vscode.TextDocument,
        range: vscode.Range | vscode.Selection,
        context: vscode.CodeActionContext,
        token: vscode.CancellationToken
    ): vscode.CodeAction[] {
        const actions: vscode.CodeAction[] = [];
        
        // Add quick fixes for diagnostics
        for (const diagnostic of context.diagnostics) {
            actions.push(...this.createQuickFixes(document, diagnostic));
        }
        
        // Add refactoring actions when text is selected
        if (!range.isEmpty) {
            actions.push(...this.createRefactoringActions(document, range));
        }
        
        // Add code improvements for current line
        const line = document.lineAt(range.start.line);
        actions.push(...this.createImprovementActions(document, line, range));
        
        return actions;
    }
    
    private createQuickFixes(document: vscode.TextDocument, diagnostic: vscode.Diagnostic): vscode.CodeAction[] {
        const actions: vscode.CodeAction[] = [];
        const line = document.lineAt(diagnostic.range.start.line).text;
        
        // Fix typos in modifier names
        if (diagnostic.message.includes('uppercase')) {
            actions.push(this.createFix(
                'Change to upperCase',
                document,
                diagnostic.range,
                'upperCase',
                diagnostic
            ));
        }
        
        if (diagnostic.message.includes('lowercase')) {
            actions.push(this.createFix(
                'Change to lowerCase',
                document,
                diagnostic.range,
                'lowerCase',
                diagnostic
            ));
        }
        
        // Suggest common typos
        const typoSuggestions: { [key: string]: string } = {
            'titlecase': 'titleCase',
            'capitalize': 'capitalize',
            'tostring': 'to.string',
            'tonumber': 'to.number',
            'todecimal': 'to.decimal',
            'toboolean': 'to.boolean',
            'dateparse': 'date.parse',
            'dateadd': 'date.add',
            'mathsum': 'Math.sum',
            'mathavg': 'Math.average',
            'mathmax': 'Math.max',
            'mathmin': 'Math.min',
        };
        
        const word = document.getText(diagnostic.range).toLowerCase();
        if (typoSuggestions[word]) {
            actions.push(this.createFix(
                `Did you mean '${typoSuggestions[word]}'?`,
                document,
                diagnostic.range,
                typoSuggestions[word],
                diagnostic
            ));
        }
        
        // Fix missing return statement
        if (diagnostic.message.includes('missing return')) {
            const action = new vscode.CodeAction(
                'Add return statement',
                vscode.CodeActionKind.QuickFix
            );
            action.edit = new vscode.WorkspaceEdit();
            const insertPosition = diagnostic.range.end;
            action.edit.insert(document.uri, insertPosition, '\n    return {};\n');
            action.diagnostics = [diagnostic];
            action.isPreferred = true;
            actions.push(action);
        }
        
        // Fix unclosed braces
        if (diagnostic.message.includes('unclosed') || diagnostic.message.includes('unbalanced')) {
            const closingChar = diagnostic.message.includes('{') ? '}' : 
                               diagnostic.message.includes('[') ? ']' : ')';
            const action = new vscode.CodeAction(
                `Add closing '${closingChar}'`,
                vscode.CodeActionKind.QuickFix
            );
            action.edit = new vscode.WorkspaceEdit();
            action.edit.insert(document.uri, diagnostic.range.end, closingChar);
            action.diagnostics = [diagnostic];
            action.isPreferred = true;
            actions.push(action);
        }

        // Format long object declaration
        if (diagnostic.code === 'format-object') {
            const line = document.lineAt(diagnostic.range.start.line);
            const action = new vscode.CodeAction(
                'Format object on multiple lines',
                vscode.CodeActionKind.QuickFix
            );
            action.command = {
                command: 'isl.improvement.formatObject',
                title: 'Format object on multiple lines',
                arguments: [document, line.range]
            };
            action.diagnostics = [diagnostic];
            action.isPreferred = true;
            actions.push(action);
        }

        // Simplify string interpolation
        if (diagnostic.code === 'simplify-interpolation') {
            const text = document.getText(diagnostic.range);
            // Extract the variable from ${$variable}
            const match = text.match(/\$\{(\$[a-zA-Z_][a-zA-Z0-9_]*)\}/);
            if (match) {
                const variable = match[1];
                const action = new vscode.CodeAction(
                    `Simplify to ${variable}`,
                    vscode.CodeActionKind.QuickFix
                );
                action.edit = new vscode.WorkspaceEdit();
                action.edit.replace(document.uri, diagnostic.range, variable);
                action.diagnostics = [diagnostic];
                action.isPreferred = true;
                actions.push(action);
            }
        }

        // Use coalesce operator
        if (diagnostic.code === 'use-coalesce-operator') {
            const line = document.lineAt(diagnostic.range.start.line);
            const action = new vscode.CodeAction(
                'Use ?? operator instead',
                vscode.CodeActionKind.QuickFix
            );
            action.command = {
                command: 'isl.improvement.useCoalesceOperator',
                title: 'Use ?? operator',
                arguments: [document, line.range]
            };
            action.diagnostics = [diagnostic];
            action.isPreferred = true;
            actions.push(action);
        }

        // Use = instead of : for variable assignment
        if (diagnostic.code === 'use-equals-assignment') {
            // Single fix
            const action = new vscode.CodeAction(
                'Change : to =',
                vscode.CodeActionKind.QuickFix
            );
            action.edit = new vscode.WorkspaceEdit();
            action.edit.replace(document.uri, diagnostic.range, '=');
            action.diagnostics = [diagnostic];
            action.isPreferred = true;
            actions.push(action);

            // Fix all in file
            const fixAllAction = new vscode.CodeAction(
                'Change all : to = in file',
                vscode.CodeActionKind.QuickFix
            );
            fixAllAction.edit = this.createFixAllColonAssignments(document);
            fixAllAction.diagnostics = [diagnostic];
            actions.push(fixAllAction);
        }

        // Wrap math expression in {{ }}
        if (diagnostic.code === 'math-outside-braces') {
            const mathExpression = document.getText(diagnostic.range);
            const wrappedExpression = `{{ ${mathExpression} }}`;
            
            const action = new vscode.CodeAction(
                `Wrap in {{ }}`,
                vscode.CodeActionKind.QuickFix
            );
            action.edit = new vscode.WorkspaceEdit();
            action.edit.replace(document.uri, diagnostic.range, wrappedExpression);
            action.diagnostics = [diagnostic];
            action.isPreferred = true;
            actions.push(action);
            
            // Also provide "Fix all in file" option
            const fixAllAction = new vscode.CodeAction(
                'Fix all math operations in file',
                vscode.CodeActionKind.QuickFix
            );
            fixAllAction.edit = createFixAllMathOperations(document);
            fixAllAction.diagnostics = [diagnostic];
            actions.push(fixAllAction);
        }

        // Combine consecutive filters
        if (diagnostic.code === 'inefficient-filter') {
            const diagnosticText = document.getText(diagnostic.range);
            
            // Extract filter conditions from the diagnostic range
            // Pattern: | filter(condition1) | filter(condition2) | filter(condition3) ...
            const filterPattern = /\|\s*filter\s*\(([^)]+)\)/g;
            const filterMatches: string[] = [];
            let match;
            
            while ((match = filterPattern.exec(diagnosticText)) !== null) {
                filterMatches.push(match[1].trim());
            }
            
            if (filterMatches.length >= 2) {
                // Combine all filter conditions with "and"
                const combinedCondition = filterMatches.join(' and ');
                
                // Replace all consecutive filters with a single combined filter
                // Match: | filter(...) | filter(...) | filter(...) ...
                // We need to match the entire sequence of consecutive filters
                const filterSequencePattern = /(\|\s*filter\s*\([^)]+\)\s*)+/;
                const replacement = diagnosticText.replace(
                    filterSequencePattern,
                    `| filter(${combinedCondition})`
                );
                
                const action = new vscode.CodeAction(
                    'Combine filters with "and" operator',
                    vscode.CodeActionKind.QuickFix
                );
                action.edit = new vscode.WorkspaceEdit();
                action.edit.replace(document.uri, diagnostic.range, replacement);
                action.diagnostics = [diagnostic];
                action.isPreferred = true;
                actions.push(action);
            }
        }

        // Convert foreach to map
        if (diagnostic.code === 'foreach-to-map') {
            const arrayVarName = (diagnostic as any).arrayVarName as string | undefined;
            const arrayInitLine = (diagnostic as any).arrayInitLine as number | undefined;
            const arrayVar = (diagnostic as any).arrayVar as string | undefined;
            const mapExpression = (diagnostic as any).mapExpression as string | undefined;
            const foreachStartLine = (diagnostic as any).foreachStartLine as number | undefined;
            const endforLine = (diagnostic as any).endforLine as number | undefined;
            
            if (arrayVarName && arrayInitLine !== undefined && arrayVar && mapExpression && 
                foreachStartLine !== undefined && endforLine !== undefined) {
                
                const action = new vscode.CodeAction(
                    'Convert foreach to map()',
                    vscode.CodeActionKind.QuickFix
                );
                action.edit = new vscode.WorkspaceEdit();
                
                // Update the array initialization line to use map
                // Preserve the original assignment operator (= or :)
                const initLine = document.lineAt(arrayInitLine);
                const initLineText = initLine.text;
                const assignmentOp = initLineText.includes(':') ? ':' : '=';
                
                // Get indentation from the original line
                const indentMatch = initLineText.match(/^(\s*)/);
                const indent = indentMatch ? indentMatch[1] : '';
                
                const newInitLine = `${indent}$${arrayVarName} ${assignmentOp} ${arrayVar} | map(${mapExpression});`;
                action.edit.replace(document.uri, initLine.range, newInitLine);
                
                // Remove the foreach loop (from foreach line to endfor line, including the endfor line)
                const endforLineObj = document.lineAt(endforLine);
                // Delete from start of foreach line to end of endfor line
                // If not the last line, also delete the newline after endfor
                const foreachRange = new vscode.Range(
                    foreachStartLine,
                    0,
                    endforLine,
                    endforLineObj.text.length
                );
                action.edit.delete(document.uri, foreachRange);
                
                action.diagnostics = [diagnostic];
                action.isPreferred = true;
                actions.push(action);
            }
        }

        // Fix naming convention
        if (diagnostic.code === 'naming-convention') {
            const originalName = (diagnostic as any).originalName as string | undefined;
            const correctName = (diagnostic as any).correctName as string | undefined;
            const type = (diagnostic as any).type as 'function' | 'modifier' | undefined;
            
            if (originalName && correctName && type) {
                // Rename in declaration
                const action = new vscode.CodeAction(
                    `Rename ${type} to '${correctName}'`,
                    vscode.CodeActionKind.QuickFix
                );
                action.edit = new vscode.WorkspaceEdit();
                
                // Replace the name in the declaration
                action.edit.replace(document.uri, diagnostic.range, correctName);
                
                // Also rename all usages in the document
                const text = document.getText();
                const lines = text.split('\n');
                
                for (let i = 0; i < lines.length; i++) {
                    const line = lines[i];
                    
                    // Skip the declaration line (already handled)
                    if (i === diagnostic.range.start.line) {
                        continue;
                    }
                    
                    // Find usages of the function/modifier
                    // For functions: @.This.functionName( or @.ModuleName.functionName(
                    // For modifiers: | modifierName(
                    let pattern: RegExp;
                    const escapedName = originalName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                    if (type === 'function') {
                        // Match: @.This.functionName( or @.ModuleName.functionName(
                        pattern = new RegExp(`@\\.(?:This|[A-Z][a-zA-Z0-9_]*)\\.${escapedName}\\s*\\(`, 'g');
                    } else {
                        // Match: | modifierName(
                        pattern = new RegExp(`\\|\\s*${escapedName}\\s*\\(`, 'g');
                    }
                    
                    let match;
                    while ((match = pattern.exec(line)) !== null) {
                        const matchStart = match.index;
                        const matchEnd = matchStart + match[0].length;
                        
                        // Find the position of the name within the match
                        const nameStart = line.indexOf(originalName, matchStart);
                        if (nameStart !== -1 && nameStart < matchEnd) {
                            const nameRange = new vscode.Range(
                                i,
                                nameStart,
                                i,
                                nameStart + originalName.length
                            );
                            action.edit.replace(document.uri, nameRange, correctName);
                        }
                    }
                }
                
                action.diagnostics = [diagnostic];
                action.isPreferred = true;
                actions.push(action);
            }
        }

        // Convert function to modifier
        if (diagnostic.code === 'function-to-modifier') {
            const functionName = (diagnostic as any).functionName as string | undefined;
            const definitionLine = (diagnostic as any).definitionLine as number | undefined;
            
            if (functionName && definitionLine !== undefined) {
                const action = new vscode.CodeAction(
                    `Convert function '${functionName}' to modifier`,
                    vscode.CodeActionKind.QuickFix
                );
                action.edit = new vscode.WorkspaceEdit();
                
                // Change 'fun' to 'modifier' in the definition
                const defLine = document.lineAt(definitionLine);
                const newDefLine = defLine.text.replace(/^\s*fun\s+/, (match) => match.replace('fun', 'modifier'));
                action.edit.replace(document.uri, defLine.range, newDefLine);
                
                // Update all usages: @.This.functionName(...) to | functionName(...)
                const text = document.getText();
                const lines = text.split('\n');
                const escapedName = functionName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                
                for (let i = 0; i < lines.length; i++) {
                    const line = lines[i];
                    
                    // Skip the definition line (already handled)
                    if (i === definitionLine) {
                        continue;
                    }
                    
                    // Skip comments
                    const commentIndex = Math.min(
                        line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
                        line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
                    );
                    const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;
                    
                    // Find @.This.functionName(...) calls
                    // Since we only flag functions always used with pipe, we expect | before the call
                    const functionCallPattern = new RegExp(`@\\.This\\.${escapedName}\\s*\\(`, 'g');
                    let match;
                    
                    while ((match = functionCallPattern.exec(codeOnlyLine)) !== null) {
                        const callStart = match.index;
                        
                        // Extract the function call with parameters
                        // We need to find the matching closing parenthesis
                        let parenCount = 1;
                        let paramStart = callStart + match[0].length;
                        let paramEnd = paramStart;
                        let foundParams = false;
                        
                        for (let j = paramStart; j < codeOnlyLine.length; j++) {
                            const char = codeOnlyLine[j];
                            if (char === '(') {
                                parenCount++;
                            } else if (char === ')') {
                                parenCount--;
                                if (parenCount === 0) {
                                    paramEnd = j + 1;
                                    foundParams = true;
                                    break;
                                }
                            }
                        }
                        
                        if (foundParams) {
                            // Replace @.This.functionName with just functionName
                            // The pipe should already be there since we only flag functions used with pipe
                            const fullCallRange = new vscode.Range(
                                i,
                                callStart,
                                i,
                                paramEnd
                            );
                            
                            const fullCall = codeOnlyLine.substring(callStart, paramEnd);
                            const newCall = fullCall.replace(new RegExp(`@\\.This\\.${escapedName}`), functionName);
                            action.edit.replace(document.uri, fullCallRange, newCall);
                        }
                    }
                }
                
                action.diagnostics = [diagnostic];
                action.isPreferred = true;
                actions.push(action);
            }
        }

        // Fix inconsistent spacing
        if (diagnostic.code === 'inconsistent-spacing') {
            const line = document.lineAt(diagnostic.range.start.line);
            const lineText = line.text;
            const diagnosticPos = diagnostic.range.start.character;
            const charAtPos = lineText[diagnosticPos];
            
            let fixedText = lineText;
            
            // Fix based on what character is at the diagnostic position
            if (charAtPos === ':') {
                // Missing space after :
                if (lineText[diagnosticPos + 1] !== ' ') {
                    fixedText = lineText.substring(0, diagnosticPos + 1) + ' ' + lineText.substring(diagnosticPos + 1);
                }
            } else if (charAtPos === '|') {
                // Missing spaces around |
                const beforePipe = lineText[diagnosticPos - 1];
                const afterPipe = lineText[diagnosticPos + 1];
                
                if (beforePipe !== ' ') {
                    fixedText = fixedText.substring(0, diagnosticPos) + ' ' + fixedText.substring(diagnosticPos);
                }
                if (afterPipe !== ' ') {
                    const insertPos = beforePipe !== ' ' ? diagnosticPos + 2 : diagnosticPos + 1;
                    fixedText = fixedText.substring(0, insertPos) + ' ' + fixedText.substring(insertPos);
                }
            }
            
            if (fixedText !== lineText) {
                const action = new vscode.CodeAction(
                    'Fix spacing',
                    vscode.CodeActionKind.QuickFix
                );
                action.edit = new vscode.WorkspaceEdit();
                action.edit.replace(document.uri, line.range, fixedText);
                action.diagnostics = [diagnostic];
                action.isPreferred = true;
                actions.push(action);
            }
        }

        // Fix foreach variable scoping
        if (diagnostic.code === 'foreach-variable-scoping') {
            const loopVar = (diagnostic as any).loopVar as string | undefined;
            const foreachStartLine = (diagnostic as any).foreachStartLine as number | undefined;
            
            if (loopVar && foreachStartLine !== undefined) {
                const action = new vscode.CodeAction(
                    `Convert foreach to map() to fix variable scoping`,
                    vscode.CodeActionKind.QuickFix
                );
                action.command = {
                    command: 'editor.action.showReferences',
                    title: 'Show foreach loop',
                    arguments: [document.uri, diagnostic.range.start, [{ uri: document.uri, range: new vscode.Range(foreachStartLine, 0, foreachStartLine, 0) }]]
                };
                action.diagnostics = [diagnostic];
                actions.push(action);
            }
        }

        // Fix implicit type conversion
        if (diagnostic.code === 'implicit-type-conversion') {
            const conversionModifier = (diagnostic as any).conversionModifier as string | undefined;
            const rightVar = (diagnostic as any).rightVar as string | undefined;
            
            if (conversionModifier && rightVar) {
                const line = document.lineAt(diagnostic.range.start.line);
                const lineText = line.text;
                
                // Find the right variable in the line and add the conversion modifier after it
                const rightVarIndex = diagnostic.range.start.character;
                const rightVarEnd = diagnostic.range.end.character;
                
                // Check what comes after the variable
                const afterVar = lineText.substring(rightVarEnd);
                const trimmedAfter = afterVar.trim();
                
                // Add the conversion modifier after the variable
                const replacement = ` | ${conversionModifier}`;
                const insertPos = new vscode.Position(
                    diagnostic.range.start.line,
                    rightVarEnd
                );
                
                const action = new vscode.CodeAction(
                    `Add explicit conversion: | ${conversionModifier}`,
                    vscode.CodeActionKind.QuickFix
                );
                action.edit = new vscode.WorkspaceEdit();
                action.edit.insert(document.uri, insertPos, replacement);
                action.diagnostics = [diagnostic];
                action.isPreferred = true;
                actions.push(action);
            }
        }
        
        return actions;
    }
    
    private createRefactoringActions(document: vscode.TextDocument, range: vscode.Range): vscode.CodeAction[] {
        const actions: vscode.CodeAction[] = [];
        const selectedText = document.getText(range);
        
        // Extract to variable
        if (this.canExtractToVariable(selectedText)) {
            const action = new vscode.CodeAction(
                'Extract to variable',
                vscode.CodeActionKind.RefactorExtract
            );
            action.command = {
                command: 'isl.refactor.extractVariable',
                title: 'Extract to variable',
                arguments: [document, range]
            };
            actions.push(action);
        }
        
        // Extract to function
        if (this.canExtractToFunction(selectedText, document, range)) {
            const action = new vscode.CodeAction(
                'Extract to function',
                vscode.CodeActionKind.RefactorExtract
            );
            action.command = {
                command: 'isl.refactor.extractFunction',
                title: 'Extract to function',
                arguments: [document, range]
            };
            actions.push(action);
        }
        
        // Convert to template string
        if (selectedText.match(/\$\w+(\.\w+)*\s*\+\s*["']/)) {
            const action = new vscode.CodeAction(
                'Convert to template string',
                vscode.CodeActionKind.RefactorRewrite
            );
            action.command = {
                command: 'isl.refactor.toTemplateString',
                title: 'Convert to template string',
                arguments: [document, range]
            };
            actions.push(action);
        }
        
        return actions;
    }
    
    private createImprovementActions(document: vscode.TextDocument, line: vscode.TextLine, range: vscode.Range): vscode.CodeAction[] {
        const actions: vscode.CodeAction[] = [];
        const lineText = line.text;
        
        // Suggest simplifying string interpolation for simple variables
        const unnecessaryInterpolation = /\$\{(\$[a-zA-Z_][a-zA-Z0-9_]*)\}/g;
        let match;
        while ((match = unnecessaryInterpolation.exec(lineText)) !== null) {
            const fullMatch = match[0]; // ${$variable}
            const variable = match[1];  // $variable
            
            // Only suggest if it's a simple variable (no dots)
            if (!variable.includes('.')) {
                const startPos = line.range.start.character + match.index;
                const endPos = startPos + fullMatch.length;
                const replaceRange = new vscode.Range(
                    line.lineNumber,
                    startPos,
                    line.lineNumber,
                    endPos
                );
                
                const action = new vscode.CodeAction(
                    `Simplify to ${variable} (remove unnecessary braces)`,
                    vscode.CodeActionKind.RefactorRewrite
                );
                action.edit = new vscode.WorkspaceEdit();
                action.edit.replace(document.uri, replaceRange, variable);
                action.isPreferred = true;
                actions.push(action);
            }
        }
        
        // Suggest using ?? instead of | default()
        if (lineText.match(/\|\s*default\s*\(/)) {
            const action = new vscode.CodeAction(
                'Use ?? operator instead of default()',
                vscode.CodeActionKind.QuickFix
            );
            action.command = {
                command: 'isl.improvement.useCoalesceOperator',
                title: 'Use ?? operator',
                arguments: [document, line.range]
            };
            actions.push(action);
        }
        
        // Suggest simplifying nested ifs
        if (lineText.match(/^\s*if\s*\(/) && this.hasNestedIf(document, line.lineNumber)) {
            const action = new vscode.CodeAction(
                'Simplify nested conditions',
                vscode.CodeActionKind.RefactorRewrite
            );
            action.command = {
                command: 'isl.improvement.simplifyNestedIfs',
                title: 'Simplify conditions',
                arguments: [document, line.lineNumber]
            };
            actions.push(action);
        }
        
        // Suggest using Math.sum instead of reduce
        if (lineText.match(/\|\s*reduce\s*\(\s*\{\{\s*\$acc\s*\+\s*\$it\s*\}\}/)) {
            const action = new vscode.CodeAction(
                'Use Math.sum() instead of reduce',
                vscode.CodeActionKind.QuickFix
            );
            action.command = {
                command: 'isl.improvement.useMathSum',
                title: 'Use Math.sum()',
                arguments: [document, line.range]
            };
            action.isPreferred = true;
            actions.push(action);
        }
        
        // Suggest formatting long modifier chains
        if (this.hasLongModifierChain(lineText)) {
            const action = new vscode.CodeAction(
                'Format modifier chain on multiple lines',
                vscode.CodeActionKind.RefactorRewrite
            );
            action.command = {
                command: 'isl.improvement.formatChain',
                title: 'Format chain',
                arguments: [document, line.range]
            };
            actions.push(action);
        }
        
        // Suggest formatting long object declarations
        if (this.hasLongObjectDeclaration(lineText)) {
            const action = new vscode.CodeAction(
                'Format object on multiple lines',
                vscode.CodeActionKind.RefactorRewrite
            );
            action.command = {
                command: 'isl.improvement.formatObject',
                title: 'Format object',
                arguments: [document, line.range]
            };
            actions.push(action);
        }
        
        return actions;
    }
    
    private createFix(
        title: string,
        document: vscode.TextDocument,
        range: vscode.Range,
        replacement: string,
        diagnostic: vscode.Diagnostic
    ): vscode.CodeAction {
        const action = new vscode.CodeAction(title, vscode.CodeActionKind.QuickFix);
        action.edit = new vscode.WorkspaceEdit();
        action.edit.replace(document.uri, range, replacement);
        action.diagnostics = [diagnostic];
        action.isPreferred = true;
        return action;
    }
    
    private canExtractToVariable(text: string): boolean {
        // Can extract expressions, not simple variables
        return text.trim().length > 2 && 
               !text.match(/^\$\w+$/) && 
               (text.includes('|') || text.includes('{{') || text.includes('@.'));
    }
    
    private canExtractToFunction(text: string, document: vscode.TextDocument, range: vscode.Range): boolean {
        // Can extract multi-line blocks or complex expressions
        const lines = text.split('\n');
        return lines.length > 1 || this.canExtractToVariable(text);
    }
    
    private hasNestedIf(document: vscode.TextDocument, lineNumber: number): boolean {
        // Check if there's a nested if within the next few lines
        for (let i = lineNumber + 1; i < Math.min(lineNumber + 10, document.lineCount); i++) {
            const line = document.lineAt(i).text.trim();
            if (line.startsWith('if (')) {
                return true;
            }
            if (line.startsWith('endif')) {
                return false;
            }
        }
        return false;
    }
    
    private hasLongModifierChain(line: string): boolean {
        // Check if line has 3+ modifiers or is longer than 100 chars with modifiers
        const pipeCount = (line.match(/\|/g) || []).length;
        return pipeCount >= 3 || (pipeCount >= 2 && line.length > 100);
    }
    
    private hasLongObjectDeclaration(line: string): boolean {
        // Check if line has an object declaration that's too long
        // Look for { ... } pattern with multiple properties
        if (line.length < 100) {
            return false;
        }
        
        // Check if line contains object with multiple properties
        const objectMatch = line.match(/\{[^}]+:[^}]+:[^}]+\}/);
        return objectMatch !== null;
    }

    private createFixAllColonAssignments(document: vscode.TextDocument): vscode.WorkspaceEdit {
        const edit = new vscode.WorkspaceEdit();
        const text = document.getText();
        const lines = text.split('\n');

        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            
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
                const colonPos = match[1].length + match[2].length + match[3].length;
                const range = new vscode.Range(i, colonPos, i, colonPos + 1);
                edit.replace(document.uri, range, '=');
            }
        }

        return edit;
    }
}

// Refactoring command implementations
export async function extractVariable(document: vscode.TextDocument, range: vscode.Range) {
    const selectedText = document.getText(range);
    
    const varName = await vscode.window.showInputBox({
        prompt: 'Enter variable name',
        value: 'extracted',
        validateInput: (text) => {
            if (!text.match(/^[a-zA-Z_][a-zA-Z0-9_]*$/)) {
                return 'Invalid variable name';
            }
            return null;
        }
    });
    
    if (!varName) {
        return;
    }
    
    const edit = new vscode.WorkspaceEdit();
    
    // Find the start of the statement/line to insert before it
    const line = document.lineAt(range.start.line);
    const indent = line.text.match(/^\s*/)?.[0] || '';
    const insertPosition = new vscode.Position(range.start.line, 0);
    
    // Insert variable declaration
    edit.insert(document.uri, insertPosition, `${indent}$${varName} = ${selectedText.trim()};\n`);
    
    // Replace selected text with variable reference
    edit.replace(document.uri, range, `$${varName}`);
    
    await vscode.workspace.applyEdit(edit);
}

export async function extractFunction(document: vscode.TextDocument, range: vscode.Range) {
    const selectedText = document.getText(range);
    
    const funcName = await vscode.window.showInputBox({
        prompt: 'Enter function name',
        value: 'extracted',
        validateInput: (text) => {
            if (!text.match(/^[a-zA-Z_][a-zA-Z0-9_]*$/)) {
                return 'Invalid function name';
            }
            return null;
        }
    });
    
    if (!funcName) {
        return;
    }
    
    // Find variables used in selection
    const variables = findVariablesInText(selectedText);
    const params = variables.join(', ');
    
    const edit = new vscode.WorkspaceEdit();
    
    // Insert function at the top of the file
    const funcDeclaration = `fun ${funcName}(${params}) {\n    return ${selectedText.trim()};\n}\n\n`;
    edit.insert(document.uri, new vscode.Position(0, 0), funcDeclaration);
    
    // Replace selected text with function call
    const args = variables.join(', ');
    edit.replace(document.uri, range, `@.This.${funcName}(${args})`);
    
    await vscode.workspace.applyEdit(edit);
}

export async function convertToTemplateString(document: vscode.TextDocument, range: vscode.Range) {
    const selectedText = document.getText(range);
    
    // Convert string concatenation to template string
    const converted = selectedText
        .replace(/\$(\w+(?:\.\w+)*)\s*\+\s*["']([^"']*)["']/g, '`${$$$1}$2`')
        .replace(/["']([^"']*)["']\s*\+\s*\$(\w+(?:\.\w+)*)/g, '`$1${$$$2}`')
        .replace(/\$(\w+(?:\.\w+)*)/g, '${$$$1}');
    
    const edit = new vscode.WorkspaceEdit();
    edit.replace(document.uri, range, converted);
    
    await vscode.workspace.applyEdit(edit);
}

export async function useCoalesceOperator(document: vscode.TextDocument, range: vscode.Range) {
    const lineText = document.getText(range);
    
    // Convert | default(value) to ?? value
    const converted = lineText.replace(/\|\s*default\s*\(\s*([^)]+)\s*\)/g, '?? $1');
    
    const edit = new vscode.WorkspaceEdit();
    edit.replace(document.uri, range, converted);
    
    await vscode.workspace.applyEdit(edit);
}

export async function useMathSum(document: vscode.TextDocument, range: vscode.Range) {
    const lineText = document.getText(range);
    
    // Convert reduce({{ $acc + $it }}, 0) to Math.sum(0)
    const converted = lineText.replace(/\|\s*reduce\s*\(\s*\{\{\s*\$acc\s*\+\s*\$it\s*\}\}\s*,\s*(\d+)\s*\)/g, '| Math.sum($1)');
    
    const edit = new vscode.WorkspaceEdit();
    edit.replace(document.uri, range, converted);
    
    await vscode.workspace.applyEdit(edit);
}

export async function formatChain(document: vscode.TextDocument, range: vscode.Range) {
    const lineText = document.getText(range);
    
    // Split long chains into multiple lines
    const indent = lineText.match(/^\s*/)?.[0] || '';
    const parts = lineText.split('|').map(p => p.trim()).filter(p => p);
    
    if (parts.length === 0) {
        return;
    }
    
    const firstPart = parts[0];
    const modifiers = parts.slice(1);
    
    const formatted = `${indent}${firstPart}\n${modifiers.map(m => `${indent}    | ${m}`).join('\n')}`;
    
    const edit = new vscode.WorkspaceEdit();
    edit.replace(document.uri, range, formatted);
    
    await vscode.workspace.applyEdit(edit);
}

export async function formatObject(document: vscode.TextDocument, range: vscode.Range) {
    const lineText = document.getText(range);
    
    // Find the opening brace
    const openBraceMatch = lineText.match(/^(\s*)(.*?)(\{)/);
    if (!openBraceMatch) {
        return;
    }
    
    const indent = openBraceMatch[1];
    const beforeBrace = openBraceMatch[2];
    
    // Find matching closing brace (accounting for strings and nesting)
    let depth = 0;
    let inString = false;
    let stringChar = '';
    let objectStart = -1;
    let objectEnd = -1;
    
    for (let i = 0; i < lineText.length; i++) {
        const char = lineText[i];
        const prevChar = i > 0 ? lineText[i - 1] : '';
        
        // Track string boundaries
        if ((char === '"' || char === "'" || char === '`') && prevChar !== '\\') {
            if (!inString) {
                inString = true;
                stringChar = char;
            } else if (char === stringChar) {
                inString = false;
            }
        }
        
        // Track braces outside of strings
        if (!inString) {
            if (char === '{') {
                if (depth === 0) {
                    objectStart = i;
                }
                depth++;
            } else if (char === '}') {
                depth--;
                if (depth === 0) {
                    objectEnd = i;
                    break;
                }
            }
        }
    }
    
    if (objectStart === -1 || objectEnd === -1) {
        return;
    }
    
    const objectContent = lineText.substring(objectStart + 1, objectEnd);
    const afterBrace = lineText.substring(objectEnd + 1);
    
    // Parse properties - reset tracking variables
    const properties: string[] = [];
    let currentProp = '';
    depth = 0;
    inString = false;
    stringChar = '';
    
    for (let i = 0; i < objectContent.length; i++) {
        const char = objectContent[i];
        const prevChar = i > 0 ? objectContent[i - 1] : '';
        
        // Track string boundaries
        if ((char === '"' || char === "'" || char === '`') && prevChar !== '\\') {
            if (!inString) {
                inString = true;
                stringChar = char;
            } else if (char === stringChar) {
                inString = false;
            }
        }
        
        // Track nested braces/brackets
        if (!inString) {
            if (char === '{' || char === '[' || char === '(') {
                depth++;
            } else if (char === '}' || char === ']' || char === ')') {
                depth--;
            }
        }
        
        // Split on comma at depth 0
        if (char === ',' && depth === 0 && !inString) {
            properties.push(currentProp.trim());
            currentProp = '';
        } else {
            currentProp += char;
        }
    }
    
    // Add the last property
    if (currentProp.trim()) {
        properties.push(currentProp.trim());
    }
    
    // Format as multi-line object
    const formattedProperties = properties
        .map(prop => `${indent}    ${prop}`)
        .join(',\n');
    
    const formatted = `${indent}${beforeBrace}{\n${formattedProperties}\n${indent}}${afterBrace}`;
    
    const edit = new vscode.WorkspaceEdit();
    edit.replace(document.uri, range, formatted);
    
    await vscode.workspace.applyEdit(edit);
}

function findVariablesInText(text: string): string[] {
    const variables = new Set<string>();
    const varPattern = /\$([a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*)/g;
    let match;
    
    while ((match = varPattern.exec(text)) !== null) {
        variables.add('$' + match[1]);
    }
    
    return Array.from(variables);
}

// Helper function to fix all math operations in a file
function createFixAllMathOperations(document: vscode.TextDocument): vscode.WorkspaceEdit {
    const edit = new vscode.WorkspaceEdit();
    const text = document.getText();
    const lines = text.split('\n');

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        
        // Skip comments
        const commentIndex = Math.min(
            line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
            line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
        );
        const codeOnlyLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

        if (codeOnlyLine.trim() === '') {
            continue;
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

        // Find all template strings to exclude them
        const backtickPattern = /`(?:[^`\\]|\\.)*`/g;
        const templateStrings: Array<{ start: number; end: number }> = [];
        let backtickMatch;
        while ((backtickMatch = backtickPattern.exec(codeOnlyLine)) !== null) {
            templateStrings.push({
                start: backtickMatch.index,
                end: backtickMatch.index + backtickMatch[0].length
            });
        }

        const allExclusionBlocks = [...mathBlocks, ...templateStrings];
        const isInsideExclusionBlocks = (start: number, end: number): boolean => {
            return allExclusionBlocks.some(block => start >= block.start && end <= block.end);
        };

        // Find math operations
        const mathExpressionPattern = /((?:\$[a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*|\d+(?:\.\d+)?))\s*([+\-*/%])\s*((?:\$[a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*|\d+(?:\.\d+)?))/g;
        let mathMatch;
        
        while ((mathMatch = mathExpressionPattern.exec(codeOnlyLine)) !== null) {
            const matchStart = mathMatch.index;
            const matchEnd = matchStart + mathMatch[0].length;
            
            if (isInsideExclusionBlocks(matchStart, matchEnd)) {
                continue;
            }

            // Check if it's a comparison operator
            const beforeOp = codeOnlyLine.substring(Math.max(0, matchStart - 1), matchStart);
            const afterOp = codeOnlyLine.substring(matchEnd, Math.min(codeOnlyLine.length, matchEnd + 1));
            const operator = mathMatch[2];
            
            if (operator === '=' && (beforeOp === '=' || beforeOp === '!' || afterOp === '=')) {
                continue;
            }
            if (operator === '<' && afterOp === '=') {
                continue;
            }
            if (operator === '>' && afterOp === '=') {
                continue;
            }
            if (operator === '<' || operator === '>') {
                const beforeMatch = codeOnlyLine.substring(0, matchStart);
                if (beforeMatch.match(/\b(if|while|switch|filter|map)\s*\(/)) {
                    continue;
                }
            }

            // Wrap in {{ }}
            const mathExpression = mathMatch[0];
            const wrappedExpression = `{{ ${mathExpression} }}`;
            const range = new vscode.Range(i, matchStart, i, matchEnd);
            edit.replace(document.uri, range, wrappedExpression);
        }
    }

    return edit;
}


