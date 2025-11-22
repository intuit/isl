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

        let formatted = '';
        let indentLevel = 0;
        let inString = false;
        let stringChar = '';
        let inComment = false;
        let inLineComment = false;
        let inInterpolation = false;
        let interpolationDepth = 0;

        const lines = code.split('\n');

        for (let i = 0; i < lines.length; i++) {
            let line = lines[i];
            const trimmedLine = line.trim();

            // Handle line comments
            if (inLineComment) {
                inLineComment = false;
            }

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

            // Add indentation
            formatted += indentChar.repeat(indentLevel) + trimmedLine;

            // Adjust indent for opening braces/brackets
            if (trimmedLine.match(/[\{\[\(]$/)) {
                indentLevel++;
            }

            // Special handling for control flow beginnings
            if (trimmedLine.match(/^(if|else|foreach|while|switch|parallel|fun|modifier)\s/)) {
                if (!trimmedLine.includes('endif') && !trimmedLine.includes('endfor') && 
                    !trimmedLine.includes('endwhile') && !trimmedLine.includes('endswitch')) {
                    if (!trimmedLine.endsWith('{')) {
                        indentLevel++;
                    }
                }
            }

            // Add newline
            if (i < lines.length - 1) {
                formatted += '\n';
            }
        }

        return formatted;
    }
}

