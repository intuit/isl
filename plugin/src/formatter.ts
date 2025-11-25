import * as vscode from 'vscode';

export class IslDocumentFormatter implements vscode.DocumentFormattingEditProvider, vscode.DocumentRangeFormattingEditProvider {
    
    provideDocumentFormattingEdits(
        document: vscode.TextDocument,
        options: vscode.FormattingOptions,
        token: vscode.CancellationToken
    ): vscode.TextEdit[] {
        const config = vscode.workspace.getConfiguration('isl.formatting');
        if (!config.get<boolean>('enabled', true)) {
            return [];
        }

        const fullRange = new vscode.Range(
            document.positionAt(0),
            document.positionAt(document.getText().length)
        );
        
        const formatted = this.formatIslCode(document.getText(), options);
        return [vscode.TextEdit.replace(fullRange, formatted)];
    }

    provideDocumentRangeFormattingEdits(
        document: vscode.TextDocument,
        range: vscode.Range,
        options: vscode.FormattingOptions,
        token: vscode.CancellationToken
    ): vscode.TextEdit[] {
        const config = vscode.workspace.getConfiguration('isl.formatting');
        if (!config.get<boolean>('enabled', true)) {
            return [];
        }

        const text = document.getText(range);
        const formatted = this.formatIslCode(text, options);
        return [vscode.TextEdit.replace(range, formatted)];
    }

    private formatIslCode(code: string, options: vscode.FormattingOptions): string {
        const config = vscode.workspace.getConfiguration('isl.formatting');
        const indentSize = config.get<number>('indentSize', 4);
        const useTabs = config.get<boolean>('useTabs', false);
        const indentChar = useTabs ? '\t' : ' '.repeat(indentSize);
        const alignProperties = config.get<boolean>('alignProperties', false);

        let formatted = '';
        let indentLevel = 0;

        const lines = code.split('\n');
        
        // Process lines: normalize pipe spacing but don't split chains
        const processedLines: string[] = [];
        for (let i = 0; i < lines.length; i++) {
            let line = lines[i].trim();
            
            // Normalize pipe spacing (ensure space after pipe)
            line = this.normalizePipeSpacing(line);
            
            processedLines.push(line);
        }

        // Indent and format
        for (let i = 0; i < processedLines.length; i++) {
            const trimmedLine = processedLines[i];

            // Handle line comments
            if (trimmedLine.startsWith('//') || trimmedLine.startsWith('#')) {
                formatted += indentChar.repeat(indentLevel) + trimmedLine + '\n';
                continue;
            }

            // Skip empty lines but preserve them
            if (trimmedLine === '') {
                formatted += '\n';
                continue;
            }

            // Adjust indent for closing braces/brackets
            if (trimmedLine.match(/^[\}\]\)]/) && indentLevel > 0) {
                indentLevel--;
            }

            // Special handling for control flow endings
            if (trimmedLine.match(/^(endif|endfor|endwhile|endswitch)/)) {
                if (indentLevel > 0) indentLevel--;
            }

            // Handle 'else' - decrease then increase indent
            if (trimmedLine === 'else' && indentLevel > 0) {
                indentLevel--;
                formatted += indentChar.repeat(indentLevel) + trimmedLine + '\n';
                indentLevel++;
                continue;
            }

            // Add indentation
            formatted += indentChar.repeat(indentLevel) + trimmedLine;

            // Adjust indent for opening braces/brackets
            if (trimmedLine.match(/[\{\[\(]$/)) {
                indentLevel++;
            }

            // Special handling for control flow beginnings
            if (trimmedLine.match(/^(if|foreach|while|switch|parallel|fun|modifier)\s/)) {
                if (!trimmedLine.includes('endif') && !trimmedLine.includes('endfor') && 
                    !trimmedLine.includes('endwhile') && !trimmedLine.includes('endswitch')) {
                    if (!trimmedLine.endsWith('{')) {
                        indentLevel++;
                    }
                }
            }

            // Add newline
            if (i < processedLines.length - 1) {
                formatted += '\n';
            }
        }

        // Align properties if enabled
        if (alignProperties) {
            formatted = this.alignObjectProperties(formatted);
        }

        return formatted;
    }
    
    private normalizePipeSpacing(line: string): string {
        // Skip comments and strings
        if (line.trim().startsWith('//') || line.trim().startsWith('#')) {
            return line;
        }
        
        // Replace | with | (with space) but be careful with strings
        // This regex finds pipes that are not inside strings
        let result = '';
        let inString = false;
        let stringChar = '';
        let i = 0;
        
        while (i < line.length) {
            const char = line[i];
            const nextChar = line[i + 1] || '';
            
            // Track string boundaries
            if ((char === '"' || char === "'" || char === '`') && (i === 0 || line[i - 1] !== '\\')) {
                if (!inString) {
                    inString = true;
                    stringChar = char;
                } else if (char === stringChar) {
                    inString = false;
                    stringChar = '';
                }
                result += char;
                i++;
                continue;
            }
            
            // If we find a pipe outside of strings
            if (char === '|' && !inString) {
                // Add pipe with space after
                result += '| ';
                // Skip any existing spaces after the pipe
                while (i + 1 < line.length && line[i + 1] === ' ') {
                    i++;
                }
                i++;
                continue;
            }
            
            result += char;
            i++;
        }
        
        return result;
    }
    
    private alignObjectProperties(code: string): string {
        const lines = code.split('\n');
        const result: string[] = [];
        let inObject = false;
        let objectLines: { indent: string, key: string, value: string, fullLine: string }[] = [];
        
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            const trimmed = line.trim();
            
            // Detect object start
            if (trimmed === '{' || trimmed.endsWith('{')) {
                if (objectLines.length > 0) {
                    // Flush previous object
                    result.push(...this.alignLines(objectLines));
                    objectLines = [];
                }
                result.push(line);
                inObject = true;
                continue;
            }
            
            // Detect object end
            if (trimmed === '}' || trimmed.startsWith('}')) {
                if (objectLines.length > 0) {
                    result.push(...this.alignLines(objectLines));
                    objectLines = [];
                }
                result.push(line);
                inObject = false;
                continue;
            }
            
            // Collect object properties
            if (inObject && trimmed.match(/^[a-zA-Z_`\$"][^:]*:\s*.+/)) {
                const match = line.match(/^(\s*)([a-zA-Z_`\$"][^:]*):\s*(.+)/);
                if (match) {
                    objectLines.push({
                        indent: match[1],
                        key: match[2],
                        value: match[3],
                        fullLine: line
                    });
                    continue;
                }
            }
            
            // Not an object property - flush and add
            if (objectLines.length > 0) {
                result.push(...this.alignLines(objectLines));
                objectLines = [];
            }
            result.push(line);
        }
        
        // Flush remaining
        if (objectLines.length > 0) {
            result.push(...this.alignLines(objectLines));
        }
        
        return result.join('\n');
    }
    
    private alignLines(lines: { indent: string, key: string, value: string }[]): string[] {
        if (lines.length === 0) {
            return [];
        }
        
        // Find max key length
        const maxKeyLength = Math.max(...lines.map(l => l.key.length));
        
        // Align all lines
        return lines.map(l => {
            const padding = ' '.repeat(maxKeyLength - l.key.length);
            return `${l.indent}${l.key}:${padding} ${l.value}`;
        });
    }
}

