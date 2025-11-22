import * as vscode from 'vscode';

export class IslDefinitionProvider implements vscode.DefinitionProvider {
    
    provideDefinition(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken
    ): vscode.Definition | undefined {
        const range = document.getWordRangeAtPosition(position);
        if (!range) {
            return undefined;
        }

        const word = document.getText(range);
        const line = document.lineAt(position.line).text;

        // Check if this is a function call
        if (line.includes('@.This.' + word)) {
            return this.findFunctionDefinition(word, document);
        }

        // Check if this is a type reference
        if (this.isTypeReference(word, line)) {
            return this.findTypeDefinition(word, document);
        }

        // Check if this is an import reference
        if (this.isImportReference(word, line)) {
            return this.findImportedFile(word, document);
        }

        return undefined;
    }

    private findFunctionDefinition(functionName: string, document: vscode.TextDocument): vscode.Location | undefined {
        const text = document.getText();
        const lines = text.split('\n');

        // Look for function declaration
        const functionPattern = new RegExp(`^\\s*(fun|modifier)\\s+${functionName}\\s*\\(`);

        for (let i = 0; i < lines.length; i++) {
            if (functionPattern.test(lines[i])) {
                const position = new vscode.Position(i, 0);
                return new vscode.Location(document.uri, position);
            }
        }

        return undefined;
    }

    private isTypeReference(word: string, line: string): boolean {
        // Check if word appears after a colon (type annotation)
        return line.includes(': ' + word) || line.includes(':' + word);
    }

    private findTypeDefinition(typeName: string, document: vscode.TextDocument): vscode.Location | undefined {
        const text = document.getText();
        const lines = text.split('\n');

        // Look for type declaration
        const typePattern = new RegExp(`^\\s*type\\s+${typeName}\\s+(as|from)`);

        for (let i = 0; i < lines.length; i++) {
            if (typePattern.test(lines[i])) {
                const position = new vscode.Position(i, 0);
                return new vscode.Location(document.uri, position);
            }
        }

        return undefined;
    }

    private isImportReference(word: string, line: string): boolean {
        // Check if word appears in import statement or is being used as imported module
        return line.includes('import ' + word) || line.includes('@.' + word);
    }

    private findImportedFile(moduleName: string, document: vscode.TextDocument): vscode.Location | undefined {
        const text = document.getText();
        const lines = text.split('\n');

        // Look for import statement
        const importPattern = new RegExp(`import\\s+${moduleName}\\s+from\\s+['"]([^'"]+)['"]`);

        for (let i = 0; i < lines.length; i++) {
            const match = lines[i].match(importPattern);
            if (match) {
                const importPath = match[1];
                // Resolve relative path
                const currentDir = vscode.Uri.joinPath(document.uri, '..');
                const importedFileUri = vscode.Uri.joinPath(currentDir, importPath);
                
                return new vscode.Location(importedFileUri, new vscode.Position(0, 0));
            }
        }

        return undefined;
    }
}

