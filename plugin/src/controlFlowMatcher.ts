import * as vscode from 'vscode';

/**
 * Control flow statement pairs in ISL
 */
export interface ControlFlowPair {
    start: string;
    end: string;
    // Regex pattern to match the start keyword (must use word boundaries)
    startPattern: RegExp;
    // Regex pattern to match the end keyword (must use word boundaries)
    endPattern: RegExp;
}

/**
 * Configuration for control flow matching
 */
export interface ControlFlowConfig {
    // Pattern to match block statements (at start of line)
    blockStartPattern: RegExp;
    // Pattern to match inline statements (after =, :, or ->)
    inlineStartPattern: RegExp;
}

/**
 * All control flow pairs in ISL
 */
export const CONTROL_FLOW_PAIRS: { [key: string]: ControlFlowPair } = {
    if: {
        start: 'if',
        end: 'endif',
        startPattern: /\bif[\s(]/,
        endPattern: /\bendif\b/
    },
    foreach: {
        start: 'foreach',
        end: 'endfor',
        startPattern: /\bforeach\s/,
        endPattern: /\bendfor\b/
    },
    while: {
        start: 'while',
        end: 'endwhile',
        startPattern: /\bwhile[\s(]/,
        endPattern: /\bendwhile\b/
    },
    switch: {
        start: 'switch',
        end: 'endswitch',
        startPattern: /\bswitch[\s(]/,
        endPattern: /\bendswitch\b/
    }
};

/**
 * Control flow configurations for each type
 */
export const CONTROL_FLOW_CONFIGS: { [key: string]: ControlFlowConfig } = {
    if: {
        blockStartPattern: /^\s*if[\s(]/,
        inlineStartPattern: /[=:>]\s*if[\s(]/  // Matches after =, :, or ->
    },
    foreach: {
        blockStartPattern: /^\s*foreach\s/,
        inlineStartPattern: /[=:>]\s*foreach\s/  // Matches after =, :, or ->
    },
    while: {
        blockStartPattern: /^\s*while[\s(]/,
        inlineStartPattern: /[=:>]\s*while[\s(]/  // Matches after =, :, or ->
    },
    switch: {
        blockStartPattern: /^\s*switch[\s(]/,
        inlineStartPattern: /[=:>]\s*switch[\s(]/  // Matches after =, :, or ->
    }
};

/**
 * Checks if a control flow statement appears after 'return' keyword
 * This is a special case that should be treated like a block statement
 */
export function isAfterReturn(line: string, keywordIndex: number): boolean {
    const beforeKeyword = line.substring(0, keywordIndex);
    // Check if 'return' appears before the keyword (with whitespace between)
    // Match: "return " or "    return " (with word boundary to avoid matching "returnValue")
    return /\breturn\s+$/.test(beforeKeyword);
}

/**
 * Represents a control flow statement in the stack
 */
export interface ControlFlowStackItem {
    type: string;
    line: number;
    isBlock: boolean;
    startColumn?: number;
}

/**
 * Represents a matched keyword position
 */
export interface KeywordMatch {
    keyword: string;
    index: number;
    line: number;
    isStart: boolean;
}

/**
 * Removes comments from a line
 */
export function stripComments(line: string): string {
    const commentIndex = Math.min(
        line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
        line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
    );
    return commentIndex !== Infinity ? line.substring(0, commentIndex) : line;
}

/**
 * Finds all keyword matches in a line for a given control flow pair
 * This properly handles multi-line statements by checking the entire line
 */
export function findKeywordMatchesInLine(
    line: string,
    lineNumber: number,
    pair: ControlFlowPair,
    excludeCurrentPosition?: { line: number; column: number }
): KeywordMatch[] {
    const codeLine = stripComments(line);
    const matches: KeywordMatch[] = [];

    // Find all start keywords
    let match;
    const startRegex = new RegExp(pair.startPattern.source, 'g');
    while ((match = startRegex.exec(codeLine)) !== null) {
        // Skip if this is the current position we're excluding
        if (excludeCurrentPosition && 
            lineNumber === excludeCurrentPosition.line && 
            match.index === excludeCurrentPosition.column) {
            continue;
        }
        matches.push({
            keyword: pair.start,
            index: match.index,
            line: lineNumber,
            isStart: true
        });
    }

    // Find all end keywords
    const endRegex = new RegExp(pair.endPattern.source, 'g');
    while ((match = endRegex.exec(codeLine)) !== null) {
        matches.push({
            keyword: pair.end,
            index: match.index,
            line: lineNumber,
            isStart: false
        });
    }

    // Sort by position in line
    matches.sort((a, b) => a.index - b.index);
    return matches;
}

/**
 * Finds all control flow keywords in a line across all types
 */
export function findAllControlFlowKeywordsInLine(
    line: string,
    lineNumber: number,
    excludeCurrentPosition?: { line: number; column: number }
): KeywordMatch[] {
    const allMatches: KeywordMatch[] = [];
    
    for (const [type, pair] of Object.entries(CONTROL_FLOW_PAIRS)) {
        const matches = findKeywordMatchesInLine(line, lineNumber, pair, excludeCurrentPosition);
        allMatches.push(...matches);
    }
    
    // Sort by position in line
    allMatches.sort((a, b) => a.index - b.index);
    return allMatches;
}

/**
 * Determines if a control flow statement is a block statement or inline
 */
export function isBlockStatement(line: string, type: string): boolean {
    const config = CONTROL_FLOW_CONFIGS[type];
    if (!config) {
        return false;
    }
    return config.blockStartPattern.test(line);
}

/**
 * Determines if a control flow statement is an inline statement
 */
export function isInlineStatement(line: string, type: string): boolean {
    const config = CONTROL_FLOW_CONFIGS[type];
    if (!config) {
        return false;
    }
    return config.inlineStartPattern.test(line);
}

/**
 * Checks if an inline statement is complete on the current line
 * (has terminator ; or , and no end keyword)
 */
export function isInlineStatementComplete(line: string): boolean {
    const codeLine = stripComments(line);
    const hasTerminator = codeLine.includes(';') || codeLine.includes(',');
    
    if (!hasTerminator) {
        return false;
    }
    
    // Check if there's an end keyword on this line
    for (const pair of Object.values(CONTROL_FLOW_PAIRS)) {
        if (pair.endPattern.test(codeLine)) {
            return false;
        }
    }
    
    return true;
}

/**
 * Returns true if the (trimmed) line looks like the start of a new statement.
 * Used to implicitly close inline if/switch that don't use endif/endswitch:
 * e.g. next line "$var = ..." or "return" or block "foreach"/"if" at line start.
 */
export function isNewStatementStart(trimmedLine: string): boolean {
    if (!trimmedLine) return false;
    // Assignment: $var = ... or $var: ...
    if (/^\$[a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*\s*[=:]/.test(trimmedLine)) {
        return true;
    }
    // return, or block-level control flow at start of line
    if (/^return\s/.test(trimmedLine)) return true;
    if (/^(foreach|if|while|switch)\s/.test(trimmedLine)) return true;
    // End keywords start a new "statement" in the sense that we're done with previous
    if (/^(endif|endfor|endwhile|endswitch)\b/.test(trimmedLine)) return true;
    return false;
}

/**
 * Finds the matching control flow keyword by tracking depth
 * Works for multi-line statements and handles nested structures
 */
export function findMatchingControlFlowKeyword(
    document: vscode.TextDocument,
    startLine: number,
    startColumn: number,
    keyword: string
): vscode.Position | undefined {
    // Find which control flow pair this keyword belongs to
    let pair: ControlFlowPair | undefined;
    let isStartKeyword = false;
    
    for (const [type, p] of Object.entries(CONTROL_FLOW_PAIRS)) {
        if (p.start === keyword) {
            pair = p;
            isStartKeyword = true;
            break;
        } else if (p.end === keyword) {
            pair = p;
            isStartKeyword = false;
            break;
        }
    }
    
    if (!pair) {
        return undefined;
    }
    
    const text = document.getText();
    const lines = text.split('\n');
    
    // Determine search direction
    const searchForward = isStartKeyword;
    const increment = searchForward ? 1 : -1;
    const end = searchForward ? lines.length : -1;
    
    // When searching backward from an end keyword, we start with depth 1
    // because we're closing the current statement
    // When searching forward from a start keyword, we start with depth 0
    let depth = searchForward ? 0 : 1;
    
    // Start from the line after/before the current position
    const start = searchForward ? startLine + 1 : startLine - 1;
    
    for (let i = start; searchForward ? i < end : i > end; i += increment) {
        const line = lines[i];
        const codeLine = stripComments(line);
        
        // Find all matches for this control flow pair in this line
        const matches = findKeywordMatchesInLine(
            line,
            i,
            pair,
            undefined // Don't exclude anything since we're not on the start line anymore
        );
        
        // Process matches in order
        for (const match of matches) {
            if (match.isStart) {
                // Found a start keyword
                if (searchForward) {
                    // Searching forward: start keywords increase depth
                    depth++;
                } else {
                    // Searching backward: start keywords decrease depth
                    depth--;
                    if (depth === 0) {
                        // Found the matching start!
                        return new vscode.Position(i, match.index);
                    }
                }
            } else {
                // Found an end keyword
                if (searchForward) {
                    // Searching forward: end keywords decrease depth
                    if (depth === 0) {
                        // Found the matching end!
                        return new vscode.Position(i, match.index);
                    }
                    depth--;
                } else {
                    // Searching backward: end keywords increase depth
                    depth++;
                }
            }
        }
    }
    
    return undefined;
}

/**
 * Validates control flow balance across the entire document
 * Returns diagnostics for unbalanced statements
 */
export function validateControlFlowBalance(
    document: vscode.TextDocument
): Array<{ item: ControlFlowStackItem; diagnostic: vscode.Diagnostic }> {
    const text = document.getText();
    const lines = text.split('\n');
    const stack: ControlFlowStackItem[] = [];
    const errors: Array<{ item: ControlFlowStackItem; diagnostic: vscode.Diagnostic }> = [];
    
    for (let i = 0; i < lines.length; i++) {
        const fullLine = lines[i];
        const codeLineWithoutComments = stripComments(fullLine);
        const codeLineTrimmed = codeLineWithoutComments.trim();
        
        // Skip empty lines
        if (!codeLineTrimmed) {
            continue;
        }
        
        // Inline if/switch don't require endif/endswitch. When we see a new statement
        // (e.g. next assignment or endfor), implicitly close any open inline items.
        // BUT: don't pop an item if this line contains its matching end keyword
        // (e.g. endswitch) - we'll handle that in the match processing below.
        if (isNewStatementStart(codeLineTrimmed)) {
            while (stack.length > 0 && !stack[stack.length - 1].isBlock) {
                const top = stack[stack.length - 1];
                const pair = CONTROL_FLOW_PAIRS[top.type];
                if (pair && pair.endPattern.test(codeLineWithoutComments)) {
                    // This line has the matching end keyword - don't pop, handle it below
                    break;
                }
                stack.pop();
            }
        }
        
        // Find all control flow keywords in this line
        const allMatches = findAllControlFlowKeywordsInLine(fullLine, i);
        
        // Process each match in order
        for (const match of allMatches) {
            // Find the pair for this keyword
            let pair: ControlFlowPair | undefined;
            let type: string | undefined;
            
            for (const [t, p] of Object.entries(CONTROL_FLOW_PAIRS)) {
                if (p.start === match.keyword || p.end === match.keyword) {
                    pair = p;
                    type = t;
                    break;
                }
            }
            
            if (!pair || !type) {
                continue;
            }
            
            const isStart = match.keyword === pair.start;
            
            if (isStart) {
                // Determine if this is a block or inline statement
                // Use the line without comments but with whitespace for block detection
                const isBlock = isBlockStatement(codeLineWithoutComments, type);
                const isInline = isInlineStatement(codeLineWithoutComments, type);
                const isAfterReturnKeyword = isAfterReturn(codeLineWithoutComments, match.index);
                
                // Determine if we should push this to the stack
                let shouldPush = false;
                let treatAsBlock = false;
                
                if (isBlock) {
                    // Block statement - always push
                    shouldPush = true;
                    treatAsBlock = true;
                } else if (isInline) {
                    // Inline statement - only push if not complete on this line
                    const isComplete = isInlineStatementComplete(codeLineWithoutComments);
                    shouldPush = !isComplete;
                    treatAsBlock = false;
                } else if (isAfterReturnKeyword) {
                    // Control flow after 'return' keyword (e.g., "return switch", "return foreach")
                    // Treat as block statement - always push
                    shouldPush = true;
                    treatAsBlock = true;
                } else {
                    // Neither block nor inline, and not after return
                    // This shouldn't normally happen, but treat as block to be safe
                    shouldPush = true;
                    treatAsBlock = true;
                }
                
                if (shouldPush) {
                    stack.push({
                        type: type,
                        line: i,
                        isBlock: treatAsBlock,
                        startColumn: match.index
                    });
                }
            } else {
                // This is an end keyword
                // Inline if/switch may be open without their own endif/endswitch; pop them first
                while (stack.length > 0 && !stack[stack.length - 1].isBlock && stack[stack.length - 1].type !== type) {
                    stack.pop();
                }
                // Pop from stack if there's a matching start
                if (stack.length === 0 || stack[stack.length - 1].type !== type) {
                    // Unexpected end keyword
                    const range = new vscode.Range(i, 0, i, fullLine.length);
                    const diagnostic = new vscode.Diagnostic(
                        range,
                        `Unexpected ${match.keyword} without matching ${pair.start}`,
                        vscode.DiagnosticSeverity.Error
                    );
                    errors.push({
                        item: { type: type, line: i, isBlock: false },
                        diagnostic: diagnostic
                    });
                } else {
                    stack.pop();
                }
            }
        }
    }
    
    // Check for unclosed statements (only report block statements)
    for (const item of stack) {
        if (item.isBlock) {
            const range = new vscode.Range(item.line, 0, item.line, lines[item.line].length);
            const diagnostic = new vscode.Diagnostic(
                range,
                `Unclosed ${item.type} statement`,
                vscode.DiagnosticSeverity.Error
            );
            errors.push({ item: item, diagnostic: diagnostic });
        }
    }
    
    return errors;
}
