import * as vscode from 'vscode';

/**
 * Paste edit provider for ISL that inserts clipboard text as-is.
 * VS Code's default paste applies indentation based on the current line, which often
 * adds unwanted leading spaces when pasting in the middle of a line (e.g. between quotes).
 * This provider bypasses that by providing a plain-text paste edit with no extra indentation.
 */
export class IslPasteEditProvider implements vscode.DocumentPasteEditProvider {
    async provideDocumentPasteEdits(
        _document: vscode.TextDocument,
        _ranges: readonly vscode.Range[],
        dataTransfer: vscode.DataTransfer,
        _context: vscode.DocumentPasteEditContext,
        token: vscode.CancellationToken
    ): Promise<vscode.DocumentPasteEdit[] | undefined> {
        const item = dataTransfer.get('text/plain');
        if (!item) return undefined;
        const text = await item.asString();
        if (token.isCancellationRequested) return undefined;
        return [new vscode.DocumentPasteEdit(text, 'Plain text', vscode.DocumentDropOrPasteEditKind.Text)];
    }
}
