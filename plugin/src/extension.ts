import * as vscode from 'vscode';
import { IslDocumentFormatter } from './formatter';
import { IslValidator } from './validator';
import { IslExecutor } from './executor';
import { IslCompletionProvider } from './completion';
import { IslHoverProvider } from './hover';
import { IslDefinitionProvider } from './definition';
import { IslCodeLensProvider, runIslFunction } from './codelens';

export function activate(context: vscode.ExtensionContext) {
    console.log('ISL Language Support is now active');

    const documentSelector: vscode.DocumentSelector = { scheme: 'file', language: 'isl' };

    // Register formatter
    const formatter = new IslDocumentFormatter();
    context.subscriptions.push(
        vscode.languages.registerDocumentFormattingEditProvider(documentSelector, formatter),
        vscode.languages.registerDocumentRangeFormattingEditProvider(documentSelector, formatter)
    );

    // Register validator
    const validator = new IslValidator();
    context.subscriptions.push(validator);

    // Validate on open, save, and change
    context.subscriptions.push(
        vscode.workspace.onDidOpenTextDocument(doc => {
            if (doc.languageId === 'isl') {
                validator.validate(doc);
            }
        }),
        vscode.workspace.onDidSaveTextDocument(doc => {
            if (doc.languageId === 'isl') {
                validator.validate(doc);
            }
        }),
        vscode.workspace.onDidChangeTextDocument(event => {
            if (event.document.languageId === 'isl') {
                validator.validateDebounced(event.document);
            }
        })
    );

    // Validate all open ISL documents on activation
    vscode.workspace.textDocuments.forEach(doc => {
        if (doc.languageId === 'isl') {
            validator.validate(doc);
        }
    });

    // Register completion provider
    const completionProvider = new IslCompletionProvider();
    context.subscriptions.push(
        vscode.languages.registerCompletionItemProvider(documentSelector, completionProvider, '$', '@', '.', '|')
    );

    // Register hover provider
    const hoverProvider = new IslHoverProvider();
    context.subscriptions.push(
        vscode.languages.registerHoverProvider(documentSelector, hoverProvider)
    );

    // Register definition provider
    const definitionProvider = new IslDefinitionProvider();
    context.subscriptions.push(
        vscode.languages.registerDefinitionProvider(documentSelector, definitionProvider)
    );

    // Register CodeLens provider for "Run" buttons
    const codeLensProvider = new IslCodeLensProvider();
    context.subscriptions.push(
        vscode.languages.registerCodeLensProvider(documentSelector, codeLensProvider)
    );

    // Register executor
    const executor = new IslExecutor();

    // Register commands
    context.subscriptions.push(
        vscode.commands.registerCommand('isl.validate', async () => {
            const editor = vscode.window.activeTextEditor;
            if (editor && editor.document.languageId === 'isl') {
                await validator.validate(editor.document);
                vscode.window.showInformationMessage('ISL validation complete');
            }
        }),

        vscode.commands.registerCommand('isl.run', async () => {
            const editor = vscode.window.activeTextEditor;
            if (editor && editor.document.languageId === 'isl') {
                await executor.run(editor.document);
            }
        }),

        vscode.commands.registerCommand('isl.runWithInput', async () => {
            const editor = vscode.window.activeTextEditor;
            if (editor && editor.document.languageId === 'isl') {
                await executor.runWithInput(editor.document);
            }
        }),

        vscode.commands.registerCommand('isl.format', async () => {
            const editor = vscode.window.activeTextEditor;
            if (editor && editor.document.languageId === 'isl') {
                await vscode.commands.executeCommand('editor.action.formatDocument');
            }
        }),

        vscode.commands.registerCommand('isl.showDocumentation', () => {
            vscode.env.openExternal(vscode.Uri.parse('https://intuit.github.io/isl/'));
        }),

        vscode.commands.registerCommand('isl.runFunction', async (uri: vscode.Uri, functionName: string, params: string) => {
            await runIslFunction(uri, functionName, params, context);
        })
    );

    // Status bar item
    const statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
    statusBarItem.text = '$(check) ISL';
    statusBarItem.tooltip = 'ISL Language Support Active';
    statusBarItem.command = 'isl.showDocumentation';
    context.subscriptions.push(statusBarItem);

    // Show status bar item when ISL file is active
    context.subscriptions.push(
        vscode.window.onDidChangeActiveTextEditor(editor => {
            if (editor && editor.document.languageId === 'isl') {
                statusBarItem.show();
            } else {
                statusBarItem.hide();
            }
        })
    );

    if (vscode.window.activeTextEditor?.document.languageId === 'isl') {
        statusBarItem.show();
    }
}

export function deactivate() {
    console.log('ISL Language Support is now deactivated');
}

