import * as vscode from 'vscode';

export class IslInlayHintsProvider implements vscode.InlayHintsProvider {
    
    provideInlayHints(
        document: vscode.TextDocument,
        range: vscode.Range,
        token: vscode.CancellationToken
    ): vscode.InlayHint[] {
        const hints: vscode.InlayHint[] = [];
        const text = document.getText(range);
        const lines = text.split('\n');
        const startLine = range.start.line;
        
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            const lineNumber = startLine + i;
            
            // Add type hints for variable declarations
            this.addVariableTypeHints(line, lineNumber, hints, document);
            
            // Add parameter name hints for function calls
            this.addParameterNameHints(line, lineNumber, hints, document);
            
            // Add array operation hints
            this.addArrayOperationHints(line, lineNumber, hints, document);
        }
        
        return hints;
    }
    
    private addVariableTypeHints(line: string, lineNumber: number, hints: vscode.InlayHint[], document: vscode.TextDocument) {
        // Detect variable declarations with modifiers that indicate type
        const patterns = [
            // to.string suggests string
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*[^;|]*\|\s*to\.string/g, type: 'string' },
            // to.number suggests number
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*[^;|]*\|\s*to\.number/g, type: 'number' },
            // to.decimal suggests decimal
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*[^;|]*\|\s*to\.decimal/g, type: 'decimal' },
            // to.boolean suggests boolean
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*[^;|]*\|\s*to\.boolean/g, type: 'boolean' },
            // to.array or map suggests array
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*[^;|]*\|\s*(to\.array|map|filter)/g, type: 'array' },
            // date.parse suggests date
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*[^;|]*\|\s*date\.parse/g, type: 'date' },
            // @.Date suggests date
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*@\.Date\./g, type: 'date' },
            // Math operations suggest number
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*\{\{[^}]+\}\}/g, type: 'number' },
            // keys suggests string array
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*[^;|]*\|\s*keys/g, type: 'string[]' },
            // Object literal suggests object (but not {{ for math expressions)
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*\{(?!\{)/g, type: 'object' },
            // Array literal suggests array
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*\[/g, type: 'array' },
            // String literal
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*["'`]/g, type: 'string' },
            // Number literal
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*-?\d+(\.\d+)?(?!\w)/g, type: 'number' },
            // Boolean literal
            { pattern: /(\$[a-zA-Z_][a-zA-Z0-9_]*)\s*=\s*(true|false)\b/g, type: 'boolean' },
        ];
        
        for (const { pattern, type } of patterns) {
            let match;
            pattern.lastIndex = 0; // Reset regex
            
            while ((match = pattern.exec(line)) !== null) {
                const varName = match[1];
                const position = new vscode.Position(lineNumber, match.index + varName.length);
                
                const hint = new vscode.InlayHint(
                    position,
                    `: ${type}`,
                    vscode.InlayHintKind.Type
                );
                hint.paddingLeft = false;
                hint.paddingRight = false;
                
                hints.push(hint);
            }
        }
    }
    
    private addParameterNameHints(line: string, lineNumber: number, hints: vscode.InlayHint[], document: vscode.TextDocument) {
        // Add parameter name hints for common functions with multiple parameters
        const functionCalls = [
            // truncate(length, suffix)
            { 
                pattern: /\|\s*truncate\s*\(\s*(\d+)\s*,\s*("[^"]*")/g, 
                params: ['maxLength:', 'suffix:']
            },
            // padStart(length, padString)
            { 
                pattern: /\|\s*padStart\s*\(\s*(\d+)\s*,\s*("[^"]*")/g, 
                params: ['length:', 'padString:']
            },
            // padEnd(length, padString)
            { 
                pattern: /\|\s*padEnd\s*\(\s*(\d+)\s*,\s*("[^"]*")/g, 
                params: ['length:', 'padString:']
            },
            // Math.clamp(min, max)
            { 
                pattern: /\|\s*Math\.clamp\s*\(\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)/g, 
                params: ['min:', 'max:']
            },
            // date.add(amount, unit)
            { 
                pattern: /\|\s*date\.add\s*\(\s*(-?\d+)\s*,\s*"([^"]*)"/g, 
                params: ['amount:', 'unit:']
            },
            // substring(start, end)
            { 
                pattern: /\|\s*substring\s*\(\s*(\d+)\s*,\s*(\d+)/g, 
                params: ['start:', 'end:']
            },
            // replace(find, replaceWith)
            { 
                pattern: /\|\s*replace\s*\(\s*("[^"]*")\s*,\s*("[^"]*")/g, 
                params: ['find:', 'replaceWith:']
            },
        ];
        
        for (const { pattern, params } of functionCalls) {
            let match;
            pattern.lastIndex = 0;
            
            while ((match = pattern.exec(line)) !== null) {
                // Add hint for each parameter
                for (let i = 1; i < match.length && i - 1 < params.length; i++) {
                    const paramValue = match[i];
                    const paramStart = match.index + match[0].indexOf(paramValue);
                    const position = new vscode.Position(lineNumber, paramStart);
                    
                    const hint = new vscode.InlayHint(
                        position,
                        params[i - 1],
                        vscode.InlayHintKind.Parameter
                    );
                    hint.paddingLeft = false;
                    hint.paddingRight = true;
                    
                    hints.push(hint);
                }
            }
        }
    }
    
    private addArrayOperationHints(line: string, lineNumber: number, hints: vscode.InlayHint[], document: vscode.TextDocument) {
        // Add hints for array operations that produce specific types
        const arrayOps = [
            // map produces array
            { pattern: /(\|)\s*(map\s*\([^)]+\))/g, hint: ' → array' },
            // filter produces array
            { pattern: /(\|)\s*(filter\s*\([^)]+\))/g, hint: ' → array' },
            // unique produces array
            { pattern: /(\|)\s*(unique)/g, hint: ' → array' },
            // sort produces array
            { pattern: /(\|)\s*(sort)/g, hint: ' → array' },
            // reverse produces array
            { pattern: /(\|)\s*(reverse)/g, hint: ' → array' },
            // flatten produces array
            { pattern: /(\|)\s*(flatten)/g, hint: ' → array' },
            // Math.sum produces number
            { pattern: /(\|)\s*(Math\.sum\([^)]*\))/g, hint: ' → number' },
            // Math.average produces number
            { pattern: /(\|)\s*(Math\.average)/g, hint: ' → number' },
            // length produces number
            { pattern: /(\|)\s*(length)/g, hint: ' → number' },
            // first produces single item
            { pattern: /(\|)\s*(first)/g, hint: ' → item' },
            // last produces single item
            { pattern: /(\|)\s*(last)/g, hint: ' → item' },
            // keys produces string array
            { pattern: /(\|)\s*(keys)/g, hint: ' → string[]' },
            // to.boolean produces boolean
            { pattern: /(\|)\s*(to\.boolean)/g, hint: ' → boolean' },
            // isEmpty produces boolean
            { pattern: /(\|)\s*(isEmpty)/g, hint: ' → boolean' },
            // isNotEmpty produces boolean
            { pattern: /(\|)\s*(isNotEmpty)/g, hint: ' → boolean' },
        ];
        
        for (const { pattern, hint: hintText } of arrayOps) {
            let match;
            pattern.lastIndex = 0;
            
            while ((match = pattern.exec(line)) !== null) {
                const opEnd = match.index + match[0].length;
                const position = new vscode.Position(lineNumber, opEnd);
                
                const hint = new vscode.InlayHint(
                    position,
                    hintText,
                    vscode.InlayHintKind.Type
                );
                hint.paddingLeft = true;
                hint.paddingRight = false;
                
                hints.push(hint);
            }
        }
    }
}

