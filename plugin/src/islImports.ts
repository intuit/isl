import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';

/**
 * Parse a single-line ISL import: `import A from 'x.isl'` or `import A, B from 'x.isl'`.
 * Returns null if the line does not contain a valid import clause.
 */
export function parseIslImportLine(line: string): { names: string[]; importPath: string } | null {
    const m = line.match(/import\s+(.+?)\s+from\s+['"]([^'"]+)['"]/);
    if (!m) {
        return null;
    }
    const lhs = m[1];
    const importPath = m[2];
    const names = lhs
        .split(',')
        .map(s => {
            const t = s.trim();
            const id = t.match(/^([a-zA-Z_][a-zA-Z0-9_]*)/);
            return id ? id[1] : '';
        })
        .filter(Boolean);
    return names.length > 0 ? { names, importPath } : null;
}

/** Resolve an import path string relative to the document (adds .isl when needed). */
export function resolveImportPathToUri(document: vscode.TextDocument, importPath: string): vscode.Uri | null {
    const currentDir = path.dirname(document.uri.fsPath);
    let resolvedPath: string;

    if (path.isAbsolute(importPath)) {
        resolvedPath = importPath;
    } else {
        resolvedPath = path.resolve(currentDir, importPath);
    }

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

/** URI of the .isl file bound to `moduleName` in this document's import lines. */
export function findImportUriForModule(document: vscode.TextDocument, moduleName: string): vscode.Uri | null {
    const lines = document.getText().split('\n');
    for (const raw of lines) {
        const parsed = parseIslImportLine(raw);
        if (parsed && parsed.names.includes(moduleName)) {
            return resolveImportPathToUri(document, parsed.importPath);
        }
    }
    return null;
}

export type IslExportKind = 'fun' | 'modifier';

/** First top-level `fun` / `modifier` declaration of `symbolName` in an ISL file. */
export function findExportedSymbolInIslFile(
    uri: vscode.Uri,
    symbolName: string
): { line: number; lineText: string; kind: IslExportKind } | undefined {
    try {
        const text = fs.readFileSync(uri.fsPath, 'utf-8');
        const lines = text.split('\n');
        const esc = symbolName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const funRe = new RegExp(`^\\s*(?:cache\\s+)?fun\\s+${esc}\\s*\\(`);
        const modRe = new RegExp(`^\\s*(?:cache\\s+)?modifier\\s+${esc}\\s*\\(`);
        for (let i = 0; i < lines.length; i++) {
            const L = lines[i].replace(/\r$/, '');
            if (funRe.test(L)) {
                return { line: i, lineText: L.trim(), kind: 'fun' };
            }
            if (modRe.test(L)) {
                return { line: i, lineText: L.trim(), kind: 'modifier' };
            }
        }
    } catch {
        // ignore
    }
    return undefined;
}
