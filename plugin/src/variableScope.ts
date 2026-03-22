import * as vscode from 'vscode';

/** Declaration line opens with fun/modifier and `(` (params may continue on following lines). */
function isFunModifierDeclLine(line: string): boolean {
    return /^\s*(fun|modifier)\s+[a-zA-Z_][a-zA-Z0-9_]*\s*\(/.test(line);
}

/**
 * Text inside the outermost `(` … `)` after `fun|modifier name` on declLine (params only).
 */
function extractFunModifierParamListText(lines: string[], declLine: number): string | null {
    const line = lines[declLine];
    if (!isFunModifierDeclLine(line)) {
        return null;
    }
    const parenIdx = line.indexOf('(');
    if (parenIdx === -1) {
        return null;
    }
    let depth = 1;
    let result = '';
    for (let li = declLine; li < lines.length; li++) {
        const L = lines[li];
        const start = li === declLine ? parenIdx + 1 : 0;
        for (let i = start; i < L.length; i++) {
            const c = L[i];
            if (c === '(') {
                depth++;
                result += c;
            } else if (c === ')') {
                depth--;
                if (depth === 0) {
                    return result;
                }
                result += c;
            } else {
                result += c;
            }
        }
    }
    return null;
}

function parseParamNamesFromList(paramsStr: string): string[] {
    return paramsStr
        .split(',')
        .map(p => p.trim().replace(/^\$/, '').split(':')[0].trim())
        .filter(Boolean);
}

/** Locate `$variableName` in the parameter list (supports multiline). */
function findParamVariableLocation(
    document: vscode.TextDocument,
    lines: string[],
    declLine: number,
    variableName: string
): vscode.Location | undefined {
    const line = lines[declLine];
    if (!isFunModifierDeclLine(line)) {
        return undefined;
    }
    const parenIdx = line.indexOf('(');
    if (parenIdx === -1) {
        return undefined;
    }
    let depth = 1;
    for (let li = declLine; li < lines.length; li++) {
        const L = lines[li];
        const start = li === declLine ? parenIdx + 1 : 0;
        for (let i = start; i < L.length; i++) {
            if (depth === 1 && L[i] === '$') {
                const after = L.slice(i + 1);
                const id = after.match(/^[a-zA-Z_][a-zA-Z0-9_]*/);
                if (id && id[0] === variableName) {
                    return new vscode.Location(
                        document.uri,
                        new vscode.Range(li, i, li, i + 1 + variableName.length)
                    );
                }
            }
            const c = L[i];
            if (c === '(') {
                depth++;
            } else if (c === ')') {
                depth--;
                if (depth === 0) {
                    return undefined;
                }
            }
        }
    }
    return undefined;
}

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
        if (isFunModifierDeclLine(lines[i])) {
            declLine = i;
            break;
        }
    }
    if (declLine < 0) {
        return null;
    }

    const firstLine = lines[declLine];
    const parenIdx = firstLine.indexOf('(');
    if (parenIdx === -1) {
        return null;
    }

    let depth = 1;
    let paramCloseLine = -1;
    let paramCloseCol = -1;
    outer: for (let li = declLine; li < lines.length; li++) {
        const L = lines[li];
        const start = li === declLine ? parenIdx + 1 : 0;
        for (let i = start; i < L.length; i++) {
            const c = L[i];
            if (c === '(') {
                depth++;
            } else if (c === ')') {
                depth--;
                if (depth === 0) {
                    paramCloseLine = li;
                    paramCloseCol = i;
                    break outer;
                }
            }
        }
    }
    if (paramCloseLine < 0) {
        return null;
    }

    let braceOpenLine = -1;
    let braceOpenCol = -1;
    const tail = lines[paramCloseLine].substring(paramCloseCol + 1);
    const sameLineBrace = tail.indexOf('{');
    if (sameLineBrace !== -1) {
        braceOpenLine = paramCloseLine;
        braceOpenCol = paramCloseCol + 1 + sameLineBrace;
    } else {
        for (let li = paramCloseLine + 1; li < Math.min(paramCloseLine + 40, lines.length); li++) {
            const idx = lines[li].indexOf('{');
            if (idx !== -1) {
                braceOpenLine = li;
                braceOpenCol = idx;
                break;
            }
        }
    }
    if (braceOpenLine < 0) {
        return null;
    }

    depth = 0;
    let bodyEndLine = -1;
    let started = false;
    for (let li = braceOpenLine; li < lines.length; li++) {
        const L = lines[li];
        const start = li === braceOpenLine ? braceOpenCol : 0;
        for (let i = start; i < L.length; i++) {
            const c = L[i];
            if (c === '{') {
                depth++;
                started = true;
            } else if (c === '}') {
                depth--;
                if (started && depth === 0) {
                    bodyEndLine = li;
                    break;
                }
            }
        }
        if (bodyEndLine >= 0) {
            break;
        }
    }
    if (bodyEndLine < 0) {
        return null;
    }

    if (cursorLine < braceOpenLine || cursorLine > bodyEndLine) {
        return null;
    }
    if (cursorLine === braceOpenLine && position.character < braceOpenCol) {
        return null;
    }

    return { startLine: declLine, endLine: bodyEndLine };
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

        if (isFunModifierDeclLine(line)) {
            const paramsStr = extractFunModifierParamListText(lines, i);
            if (paramsStr !== null) {
                for (const param of parseParamNamesFromList(paramsStr)) {
                    variables.add(param);
                }
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

        if (isFunModifierDeclLine(line)) {
            const loc = findParamVariableLocation(document, lines, i, variableName);
            if (loc) {
                return loc;
            }
        }

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

        const foreachMatch = line.match(/foreach\s+\$([a-zA-Z_][a-zA-Z0-9_]*)\s+in/);
        if (foreachMatch && (foreachMatch[1] === variableName || foreachMatch[1] + 'Index' === variableName)) {
            const idx = line.indexOf('$' + foreachMatch[1]);
            return new vscode.Location(
                document.uri,
                new vscode.Range(i, idx, i, idx + 1 + foreachMatch[1].length)
            );
        }

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
