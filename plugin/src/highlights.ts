import * as vscode from 'vscode';
import { 
    findMatchingControlFlowKeyword, 
    CONTROL_FLOW_PAIRS,
    stripComments,
    findAllControlFlowKeywordsInLine
} from './controlFlowMatcher';

export class IslDocumentHighlightProvider implements vscode.DocumentHighlightProvider {
    
    provideDocumentHighlights(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken
    ): vscode.DocumentHighlight[] | undefined {
        const wordRange = document.getWordRangeAtPosition(position);
        if (!wordRange) {
            return undefined;
        }

        const word = document.getText(wordRange);

        // Check if the word is a control flow keyword
        const controlFlowPairs: { [key: string]: { match: string, isOpening: boolean } } = {
            'if': { match: 'endif', isOpening: true },
            'endif': { match: 'if', isOpening: false },
            'else': { match: 'if', isOpening: false }, // else matches with if
            'foreach': { match: 'endfor', isOpening: true },
            'endfor': { match: 'foreach', isOpening: false },
            'while': { match: 'endwhile', isOpening: true },
            'endwhile': { match: 'while', isOpening: false },
            'switch': { match: 'endswitch', isOpening: true },
            'endswitch': { match: 'switch', isOpening: false }
        };

        if (!controlFlowPairs[word]) {
            return undefined;
        }

        const { match: matchingKeyword, isOpening } = controlFlowPairs[word];
        
        // Special handling for if/else/endif - highlight all three
        if (word === 'if' || word === 'else' || word === 'endif') {
            return this.findIfElseEndifHighlights(document, position.line, word);
        }
        
        // Find the matching keyword for other control flow
        const matchPosition = this.findMatchingKeyword(
            document,
            position.line,
            word,
            matchingKeyword,
            isOpening
        );

        if (!matchPosition) {
            return undefined;
        }

        // Return highlights for both keywords
        return [
            new vscode.DocumentHighlight(wordRange, vscode.DocumentHighlightKind.Text),
            new vscode.DocumentHighlight(
                new vscode.Range(
                    matchPosition,
                    new vscode.Position(matchPosition.line, matchPosition.character + matchingKeyword.length)
                ),
                vscode.DocumentHighlightKind.Text
            )
        ];
    }

    private findMatchingKeyword(
        document: vscode.TextDocument,
        startLine: number,
        keyword: string,
        matchKeyword: string,
        searchForward: boolean
    ): vscode.Position | undefined {
        // Get the current position of the keyword on the start line
        const startLineText = document.lineAt(startLine).text;
        const codeLine = stripComments(startLineText);
        const keywordRegex = new RegExp(`\\b${keyword}\\b`, 'g');
        let match;
        let startColumn = -1;
        
        // Find the keyword position on the start line
        while ((match = keywordRegex.exec(codeLine)) !== null) {
            startColumn = match.index;
            break; // Use the first match
        }
        
        if (startColumn === -1) {
            return undefined;
        }
        
        // Use the shared utility to find the matching keyword
        // This properly handles multi-line statements and nested structures
        return findMatchingControlFlowKeyword(document, startLine, startColumn, keyword);
    }

    private findIfElseEndifHighlights(
        document: vscode.TextDocument,
        startLine: number,
        word: string
    ): vscode.DocumentHighlight[] | undefined {
        const text = document.getText();
        const lines = text.split('\n');
        const highlights: vscode.DocumentHighlight[] = [];
        let ifPosition: vscode.Position | undefined;

        // Find the 'if' position
        if (word === 'else' || word === 'endif') {
            // Search backwards to find the matching 'if'
            const startLineText = document.lineAt(startLine).text;
            const codeLine = stripComments(startLineText);
            
            // Get the position of 'else' or 'endif' on the current line
            let currentColumn = -1;
            if (word === 'else') {
                const elseMatch = codeLine.match(/\belse\b/);
                if (elseMatch) {
                    currentColumn = elseMatch.index!;
                }
            } else {
                const endifMatch = codeLine.match(/\bendif\b/);
                if (endifMatch) {
                    currentColumn = endifMatch.index!;
                }
            }
            
            if (currentColumn === -1) {
                return undefined;
            }
            
            // Use the shared utility to find the matching 'if'
            ifPosition = findMatchingControlFlowKeyword(document, startLine, currentColumn, word);
        } else {
            // We're on 'if', use current position
            const currentLine = lines[startLine];
            const ifMatch = currentLine.match(/\bif[\s(]/);
            if (ifMatch) {
                ifPosition = new vscode.Position(startLine, ifMatch.index!);
            } else {
                return undefined;
            }
        }

        if (!ifPosition) {
            return undefined;
        }

        // Now search forward from the 'if' position to find 'else' and 'endif'
        // Use depth tracking to handle nested if statements
        let depth = 0;
        let elsePosition: vscode.Position | undefined;
        let endifPosition: vscode.Position | undefined;
        const pair = CONTROL_FLOW_PAIRS.if;

        for (let i = ifPosition.line; i < lines.length; i++) {
            const line = lines[i];
            const codeLine = stripComments(line);

            // Find all if keywords (increase depth)
            const ifMatches = Array.from(codeLine.matchAll(pair.startPattern));
            for (const match of ifMatches) {
                // Skip the initial 'if' position
                if (i === ifPosition.line && match.index === ifPosition.character) {
                    continue;
                }
                depth++;
            }

            // Look for 'else' at depth 0 (same level as the initial if)
            if (depth === 0 && !elsePosition) {
                const elseMatch = codeLine.match(/\belse\b/);
                if (elseMatch && i !== ifPosition.line) {
                    elsePosition = new vscode.Position(i, elseMatch.index!);
                }
            }

            // Count endif keywords (decrease depth)
            const endifMatch = codeLine.match(pair.endPattern);
            if (endifMatch) {
                if (depth === 0) {
                    endifPosition = new vscode.Position(i, endifMatch.index!);
                    break;
                }
                depth--;
            }
        }

        // Add highlights
        if (ifPosition) {
            highlights.push(
                new vscode.DocumentHighlight(
                    new vscode.Range(ifPosition, new vscode.Position(ifPosition.line, ifPosition.character + 2)),
                    vscode.DocumentHighlightKind.Text
                )
            );
        }

        if (elsePosition) {
            highlights.push(
                new vscode.DocumentHighlight(
                    new vscode.Range(elsePosition, new vscode.Position(elsePosition.line, elsePosition.character + 4)),
                    vscode.DocumentHighlightKind.Text
                )
            );
        }

        if (endifPosition) {
            highlights.push(
                new vscode.DocumentHighlight(
                    new vscode.Range(endifPosition, new vscode.Position(endifPosition.line, endifPosition.character + 5)),
                    vscode.DocumentHighlightKind.Text
                )
            );
        }

        return highlights.length > 0 ? highlights : undefined;
    }
}

