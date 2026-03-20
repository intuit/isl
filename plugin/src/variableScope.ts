import * as vscode from 'vscode';

/**
 * Find the range of the function or modifier that contains the given position.
 * Returns { startLine, endLine } (0-based) or null if not inside a fun/modifier body.
 */
export function getEnclosingFunctionOrModifierRange(
    document: vscode.TextDocument,
    position: vscode.Position
): { startLine: number; endLine: number } | null {
    const lines = document.getText().split('\n');
    const cursorLine = position.line;
    let declLine = -1;

    for (let i = cursorLine; i >= 0; i--) {
        const match = lines[i].match(/^\s*(fun|modifier)\s+[a-zA-Z_][a-zA-Z0-9_]*\s*\([^)]*\)\s*\{?\s*/);
        if (match) {
            declLine = i;
            break;
        }
    }
    if (declLine < 0) return null;

    let openLine = declLine;
    if (!lines[declLine].includes('{')) {
        for (let i = declLine + 1; i <= Math.min(declLine + 5, lines.length - 1); i++) {
            if (lines[i].includes('{')) {
                openLine = i;
                break;
            }
        }
    }
    let depth = 0;
    let braceLine = -1;
    for (let i = openLine; i < lines.length; i++) {
        const line = lines[i];
        for (const ch of line) {
            if (ch === '{') depth++;
            else if (ch === '}') depth--;
        }
        if (depth === 0) {
            braceLine = i;
            break;
        }
    }
    if (braceLine < 0 || cursorLine > braceLine) return null;

    return { startLine: declLine, endLine: braceLine };
}

/**
 * Collect variable names declared in the current function/modifier on or above the current line.
 */
export function getVariablesDeclaredAboveInCurrentScope(
    document: vscode.TextDocument,
    position: vscode.Position
): Set<string> {
    const variables = new Set<string>();
    const lines = document.getText().split('\n');
    const cursorLine = position.line;

    const range = getEnclosingFunctionOrModifierRange(document, position);
    const startLine = range ? range.startLine : 0;
    const endLine = range ? range.endLine : lines.length - 1;
    const lastLineToScan = Math.min(cursorLine, endLine);

    for (let i = startLine; i <= lastLineToScan; i++) {
        const raw = lines[i];
        const commentIdx = Math.min(
            raw.indexOf('//') !== -1 ? raw.indexOf('//') : Infinity,
            raw.indexOf('#') !== -1 ? raw.indexOf('#') : Infinity
        );
        const line = commentIdx !== Infinity ? raw.substring(0, commentIdx) : raw;

        const funParamMatch = line.match(/^\s*(fun|modifier)\s+[a-zA-Z_][a-zA-Z0-9_]*\s*\(([^)]*)\)/);
        if (funParamMatch) {
            const params = funParamMatch[2];
            const paramNames = params.split(',').map(p => p.trim().replace(/^\$/, '').split(':')[0].trim()).filter(Boolean);
            for (const param of paramNames) {
                if (param) variables.add(param);
            }
        }

        const varDeclPattern = /\$([a-zA-Z_][a-zA-Z0-9_]*)\s*[=:]/g;
        let match;
        while ((match = varDeclPattern.exec(line)) !== null) {
            variables.add(match[1]);
        }

        const foreachMatch = line.match(/foreach\s+\$([a-zA-Z_][a-zA-Z0-9_]*)\s+in/);
        if (foreachMatch) {
            variables.add(foreachMatch[1]);
            variables.add(foreachMatch[1] + 'Index');
        }

        const paginationMatch = line.match(/@\.Pagination\.[A-Za-z_][A-Za-z0-9_]*\s*\(\s*\$([a-zA-Z_][a-zA-Z0-9_]*)/);
        if (paginationMatch) {
            variables.add(paginationMatch[1]);
        }
    }

    return variables;
}

/**
 * Find the location (range) where the given variable is defined in the same function/modifier,
 * on or above the given position. Returns undefined if not in scope or not found.
 */
export function getVariableDefinitionLocation(
    document: vscode.TextDocument,
    position: vscode.Position,
    variableName: string
): vscode.Location | undefined {
    const lines = document.getText().split('\n');
    const range = getEnclosingFunctionOrModifierRange(document, position);
    const startLine = range ? range.startLine : 0;
    const lastLineToScan = position.line;

    for (let i = startLine; i <= lastLineToScan; i++) {
        const raw = lines[i];
        const commentIdx = Math.min(
            raw.indexOf('//') !== -1 ? raw.indexOf('//') : Infinity,
            raw.indexOf('#') !== -1 ? raw.indexOf('#') : Infinity
        );
        const line = commentIdx !== Infinity ? raw.substring(0, commentIdx) : raw;

        // Function/modifier parameters: fun name($a, $b) or modifier name($value)
        const funParamMatch = line.match(/^\s*(fun|modifier)\s+[a-zA-Z_][a-zA-Z0-9_]*\s*\(([^)]*)\)/);
        if (funParamMatch) {
            const parenStart = line.indexOf('(');
            const paramsStr = funParamMatch[2];
            const escaped = variableName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            const re = new RegExp(`\\$${escaped}\\b`, 'g');
            const m = re.exec(paramsStr);
            if (m) {
                const startCol = parenStart + 1 + m.index;
                return new vscode.Location(document.uri, new vscode.Range(i, startCol, i, startCol + 1 + variableName.length));
            }
        }

        // $var = ... or $var: ...
        const varDeclPattern = /\$([a-zA-Z_][a-zA-Z0-9_]*)\s*[=:]/g;
        let match;
        while ((match = varDeclPattern.exec(line)) !== null) {
            if (match[1] === variableName) {
                return new vscode.Location(
                    document.uri,
                    new vscode.Range(i, match.index, i, match.index + 1 + match[1].length)
                );
            }
        }

        // foreach $item in ...
        const foreachMatch = line.match(/foreach\s+\$([a-zA-Z_][a-zA-Z0-9_]*)\s+in/);
        if (foreachMatch && (foreachMatch[1] === variableName || foreachMatch[1] + 'Index' === variableName)) {
            const idx = line.indexOf('$' + foreachMatch[1]);
            return new vscode.Location(
                document.uri,
                new vscode.Range(i, idx, i, idx + 1 + foreachMatch[1].length)
            );
        }

        // @.Pagination.*( $varName, ... )
        const paginationMatch = line.match(/@\.Pagination\.[A-Za-z_][A-Za-z0-9_]*\s*\(\s*\$([a-zA-Z_][a-zA-Z0-9_]*)/);
        if (paginationMatch && paginationMatch[1] === variableName) {
            const idx = line.indexOf('$' + variableName);
            return new vscode.Location(
                document.uri,
                new vscode.Range(i, idx, i, idx + 1 + variableName.length)
            );
        }
    }

    return undefined;
}
