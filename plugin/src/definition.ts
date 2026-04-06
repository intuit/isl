import * as vscode from 'vscode';
import { getVariableDefinitionLocation } from './variableScope';
import { findExportedSymbolInIslFile, findImportUriForModule } from './islImports';

export class IslDefinitionProvider implements vscode.DefinitionProvider {
    
    provideDefinition(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken
    ): vscode.Definition | undefined {
        const line = document.lineAt(position.line).text.replace(/\r$/, '');

        // Column-based @./pipe first: works when the cursor is on `.`, `(`, or when word range is undefined
        const atDotRef = this.parseAtDotModuleMember(line, position.character);
        if (atDotRef) {
            if (atDotRef.segment === 'member') {
                if (atDotRef.module === 'This') {
                    const loc = this.findFunctionDefinition(atDotRef.member, document);
                    if (loc) {
                        return loc;
                    }
                } else if (findImportUriForModule(document, atDotRef.module)) {
                    const funLoc = this.findImportedFunctionDefinition(atDotRef.module, atDotRef.member, document);
                    if (funLoc) {
                        return funLoc;
                    }
                    const modLoc = this.findImportedModifierDefinition(atDotRef.module, atDotRef.member, document);
                    if (modLoc) {
                        return modLoc;
                    }
                }
            } else if (atDotRef.segment === 'module' && atDotRef.module !== 'This') {
                const fileLoc = this.findImportedFile(atDotRef.module, document);
                if (fileLoc) {
                    return fileLoc;
                }
            }
        }

        const pipeRef = this.parsePipeModuleMember(line, position.character);
        if (pipeRef) {
            if (pipeRef.segment === 'member' && findImportUriForModule(document, pipeRef.module)) {
                const modLoc = this.findImportedModifierDefinition(pipeRef.module, pipeRef.member, document);
                if (modLoc) {
                    return modLoc;
                }
            } else if (pipeRef.segment === 'module' && pipeRef.module !== 'This') {
                const fileLoc = this.findImportedFile(pipeRef.module, document);
                if (fileLoc) {
                    return fileLoc;
                }
            }
        }

        const range = document.getWordRangeAtPosition(position, /\$?[a-zA-Z_][a-zA-Z0-9_]*/);
        if (!range) {
            return undefined;
        }

        const word = document.getText(range);

        const variableName = word.startsWith('$') ? word.slice(1) : (range.start.character > 0 && line[range.start.character - 1] === '$' ? word : null);
        if (variableName !== null) {
            const loc = getVariableDefinitionLocation(document, position, variableName);
            if (loc) {
                return loc;
            }
        }

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

    /**
     * If cursor is on `Module` or `member` in `@.Module.member`, return which segment and names.
     */
    private parseAtDotModuleMember(
        line: string,
        column: number
    ): { segment: 'module'; module: string } | { segment: 'member'; module: string; member: string } | undefined {
        const re = /@\.([a-zA-Z_][a-zA-Z0-9_]*)\.([a-zA-Z_][a-zA-Z0-9_]*)/g;
        let m: RegExpExecArray | null;
        while ((m = re.exec(line)) !== null) {
            const modStart = m.index + 2;
            const modEnd = modStart + m[1].length;
            const memStart = modEnd + 1;
            const memEnd = memStart + m[2].length;
            const tail = line.slice(memEnd);
            const parenAfter = tail.match(/^\s*\(/);
            const spanEnd = parenAfter ? memEnd + parenAfter[0].length : memEnd;
            const inMember =
                (column >= memStart && column < spanEnd) ||
                (column >= modEnd && column < memStart);
            if (inMember) {
                return { segment: 'member', module: m[1], member: m[2] };
            }
            if (column >= modStart && column < modEnd) {
                return { segment: 'module', module: m[1] };
            }
        }
        return undefined;
    }

    /**
     * If cursor is on `Module` or `member` in `| Module.member`, return which segment and names.
     */
    private parsePipeModuleMember(
        line: string,
        column: number
    ): { segment: 'module'; module: string } | { segment: 'member'; module: string; member: string } | undefined {
        const re = /\|\s*([a-zA-Z_][a-zA-Z0-9_]*)\.([a-zA-Z_][a-zA-Z0-9_]*)/g;
        let m: RegExpExecArray | null;
        while ((m = re.exec(line)) !== null) {
            const modStart = m.index + m[0].indexOf(m[1]);
            const modEnd = modStart + m[1].length;
            const memStart = modEnd + 1;
            const memEnd = memStart + m[2].length;
            const tail = line.slice(memEnd);
            const parenAfter = tail.match(/^\s*\(/);
            const spanEnd = parenAfter ? memEnd + parenAfter[0].length : memEnd;
            const inMember =
                (column >= memStart && column < spanEnd) ||
                (column >= modEnd && column < memStart);
            if (inMember) {
                return { segment: 'member', module: m[1], member: m[2] };
            }
            if (column >= modStart && column < modEnd) {
                return { segment: 'module', module: m[1] };
            }
        }
        return undefined;
    }

    private findFunctionDefinition(functionName: string, document: vscode.TextDocument): vscode.Location | undefined {
        const escaped = functionName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const functionPattern = new RegExp(`^\\s*(?:cache\\s+)?(fun|modifier)\\s+${escaped}\\s*\\(`);
        const lines = document.getText().split('\n');

        for (let i = 0; i < lines.length; i++) {
            const L = lines[i].replace(/\r$/, '');
            if (functionPattern.test(L)) {
                return new vscode.Location(document.uri, new vscode.Position(i, 0));
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
        const importedUri = findImportUriForModule(document, moduleName);
        if (importedUri) {
            return new vscode.Location(importedUri, new vscode.Position(0, 0));
        }
        return undefined;
    }

    private findImportedFunctionDefinition(moduleName: string, functionName: string, document: vscode.TextDocument): vscode.Location | undefined {
        const importedUri = findImportUriForModule(document, moduleName);
        if (!importedUri) {
            return undefined;
        }
        const sym = findExportedSymbolInIslFile(importedUri, functionName);
        if (!sym || sym.kind !== 'fun') {
            return undefined;
        }
        return new vscode.Location(importedUri, new vscode.Position(sym.line, 0));
    }

    private findImportedModifierDefinition(moduleName: string, modifierName: string, document: vscode.TextDocument): vscode.Location | undefined {
        const importedUri = findImportUriForModule(document, moduleName);
        if (!importedUri) {
            return undefined;
        }
        const sym = findExportedSymbolInIslFile(importedUri, modifierName);
        if (!sym || sym.kind !== 'modifier') {
            return undefined;
        }
        return new vscode.Location(importedUri, new vscode.Position(sym.line, 0));
    }
}

