import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';

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
        const beforeCursor = line.substring(0, position.character);

        // Check if this is an imported function call: @.ModuleName.functionName()
        // Look for the pattern before the cursor, allowing for the function name to extend to cursor
        const importedFunctionMatch = beforeCursor.match(/@\.([a-zA-Z_][a-zA-Z0-9_]*)\.([a-zA-Z_][a-zA-Z0-9_]*)$/);
        if (importedFunctionMatch) {
            const moduleName = importedFunctionMatch[1];
            const functionName = importedFunctionMatch[2];
            
            // If the word matches the function name, go to definition
            if (functionName === word) {
                return this.findImportedFunctionDefinition(moduleName, functionName, document);
            }
        }

        // Also check the full line for @.ModuleName.word pattern (in case cursor is in middle of word)
        const fullLineFunctionMatch = line.match(new RegExp(`@\\.([a-zA-Z_][a-zA-Z0-9_]*)\\.${word}\\s*\\(`));
        if (fullLineFunctionMatch) {
            const moduleName = fullLineFunctionMatch[1];
            return this.findImportedFunctionDefinition(moduleName, word, document);
        }

        // Check if this is an imported modifier: | ModuleName.modifierName
        const importedModifierMatch = beforeCursor.match(/\|\s*([a-zA-Z_][a-zA-Z0-9_]*)\.([a-zA-Z_][a-zA-Z0-9_]*)$/);
        if (importedModifierMatch) {
            const moduleName = importedModifierMatch[1];
            const modifierName = importedModifierMatch[2];
            
            // If the word matches the modifier name, go to definition
            if (modifierName === word) {
                return this.findImportedModifierDefinition(moduleName, modifierName, document);
            }
        }

        // Also check the full line for | ModuleName.word pattern (in case cursor is in middle of word)
        const fullLineModifierMatch = line.match(new RegExp(`\\|\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\.${word}(?:\\s*\\(|\\s|$)`));
        if (fullLineModifierMatch) {
            const moduleName = fullLineModifierMatch[1];
            return this.findImportedModifierDefinition(moduleName, word, document);
        }

        // Check if this is a local function call
        if (line.includes('@.This.' + word)) {
            return this.findFunctionDefinition(word, document);
        }

        // Check if this is a type reference
        if (this.isTypeReference(word, line)) {
            return this.findTypeDefinition(word, document);
        }

        // Check if this is an import reference (module name itself)
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
        const importedUri = this.resolveImportPath(document, moduleName);
        if (importedUri) {
            return new vscode.Location(importedUri, new vscode.Position(0, 0));
        }
        return undefined;
    }

    private findImportedFunctionDefinition(moduleName: string, functionName: string, document: vscode.TextDocument): vscode.Location | undefined {
        const importedUri = this.resolveImportPath(document, moduleName);
        if (!importedUri) {
            return undefined;
        }

        try {
            const importedText = fs.readFileSync(importedUri.fsPath, 'utf-8');
            const lines = importedText.split('\n');

            // Look for function declaration
            const functionPattern = new RegExp(`^\\s*fun\\s+${functionName}\\s*\\(`);

            for (let i = 0; i < lines.length; i++) {
                if (functionPattern.test(lines[i])) {
                    const position = new vscode.Position(i, 0);
                    return new vscode.Location(importedUri, position);
                }
            }
        } catch (error) {
            console.warn(`Could not read imported file ${importedUri.fsPath}: ${error}`);
        }

        return undefined;
    }

    private findImportedModifierDefinition(moduleName: string, modifierName: string, document: vscode.TextDocument): vscode.Location | undefined {
        const importedUri = this.resolveImportPath(document, moduleName);
        if (!importedUri) {
            return undefined;
        }

        try {
            const importedText = fs.readFileSync(importedUri.fsPath, 'utf-8');
            const lines = importedText.split('\n');

            // Look for modifier declaration
            const modifierPattern = new RegExp(`^\\s*modifier\\s+${modifierName}\\s*\\(`);

            for (let i = 0; i < lines.length; i++) {
                if (modifierPattern.test(lines[i])) {
                    const position = new vscode.Position(i, 0);
                    return new vscode.Location(importedUri, position);
                }
            }
        } catch (error) {
            console.warn(`Could not read imported file ${importedUri.fsPath}: ${error}`);
        }

        return undefined;
    }

    private resolveImportPath(document: vscode.TextDocument, moduleName: string): vscode.Uri | null {
        const text = document.getText();
        const lines = text.split('\n');

        // Look for import statement
        const importPattern = new RegExp(`import\\s+${moduleName}\\s+from\\s+['"]([^'"]+)['"]`);

        for (let i = 0; i < lines.length; i++) {
            const match = lines[i].match(importPattern);
            if (match) {
                const importPath = match[1];
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
            }
        }

        return null;
    }
}

