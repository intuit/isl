import * as vscode from 'vscode';

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
        const text = document.getText();
        const lines = text.split('\n');
        let depth = 0;

        const increment = searchForward ? 1 : -1;
        const start = searchForward ? startLine : startLine;
        const end = searchForward ? lines.length : -1;

        for (let i = start; searchForward ? i < end : i > end; i += increment) {
            const line = lines[i];
            
            // Skip comments
            const commentIndex = Math.min(
                line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
                line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
            );
            const codeLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

            // Find keywords in this line
            const keywordRegex = new RegExp(`\\b${keyword}\\b`, 'g');
            const matchKeywordRegex = new RegExp(`\\b${matchKeyword}\\b`, 'g');

            let match;

            // For the first line (where cursor is), skip the current keyword position
            const currentLineMatches: Array<{ keyword: string, index: number }> = [];

            // Find all opening keywords
            while ((match = keywordRegex.exec(codeLine)) !== null) {
                if (i !== startLine) { // Not on the same line as cursor
                    currentLineMatches.push({ keyword: keyword, index: match.index });
                }
            }

            // Find all closing keywords
            while ((match = matchKeywordRegex.exec(codeLine)) !== null) {
                currentLineMatches.push({ keyword: matchKeyword, index: match.index });
            }

            // Sort by index
            currentLineMatches.sort((a, b) => a.index - b.index);

            // Process matches
            for (const m of currentLineMatches) {
                if (m.keyword === keyword) {
                    depth++;
                } else if (m.keyword === matchKeyword) {
                    if (depth === 0) {
                        // Found the match!
                        return new vscode.Position(i, m.index);
                    }
                    depth--;
                }
            }
        }

        return undefined;
    }

    private findIfElseEndifHighlights(
        document: vscode.TextDocument,
        startLine: number,
        word: string
    ): vscode.DocumentHighlight[] | undefined {
        const text = document.getText();
        const lines = text.split('\n');
        const highlights: vscode.DocumentHighlight[] = [];
        let depth = 0;
        let foundIf = false;
        let ifPosition: vscode.Position | undefined;

        // Search backwards to find the matching 'if' if we're on 'else' or 'endif'
        if (word === 'else' || word === 'endif') {
            for (let i = startLine; i >= 0; i--) {
                const line = lines[i];
                const commentIndex = Math.min(
                    line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
                    line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
                );
                const codeLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

                // Count endif keywords (increase depth when going backwards)
                const endifMatches = codeLine.match(/\bendif\b/g);
                if (endifMatches && i !== startLine) {
                    depth += endifMatches.length;
                }

                // Count if keywords (decrease depth when going backwards)
                const ifMatches = Array.from(codeLine.matchAll(/\bif[\s(]/g));
                for (const match of ifMatches) {
                    if (depth === 0) {
                        ifPosition = new vscode.Position(i, match.index);
                        foundIf = true;
                        break;
                    }
                    depth--;
                }

                if (foundIf) break;
            }

            if (!ifPosition) {
                return undefined;
            }
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

        // Now search forward from the 'if' position to find 'else' and 'endif'
        depth = 0;
        let elsePosition: vscode.Position | undefined;
        let endifPosition: vscode.Position | undefined;

        for (let i = ifPosition.line; i < lines.length; i++) {
            const line = lines[i];
            const commentIndex = Math.min(
                line.indexOf('//') !== -1 ? line.indexOf('//') : Infinity,
                line.indexOf('#') !== -1 ? line.indexOf('#') : Infinity
            );
            const codeLine = commentIndex !== Infinity ? line.substring(0, commentIndex) : line;

            // Count if keywords (increase depth)
            const ifMatches = Array.from(codeLine.matchAll(/\bif[\s(]/g));
            for (const match of ifMatches) {
                if (i !== ifPosition.line) {
                    depth++;
                }
            }

            // Look for 'else' at the same depth
            if (depth === 0 && !elsePosition) {
                const elseMatch = codeLine.match(/\belse\b/);
                if (elseMatch && i !== ifPosition.line) {
                    elsePosition = new vscode.Position(i, elseMatch.index!);
                }
            }

            // Count endif keywords (decrease depth)
            const endifMatch = codeLine.match(/\bendif\b/);
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

