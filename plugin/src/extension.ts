import * as vscode from 'vscode';
import { IslDocumentFormatter } from './formatter';
import { IslValidator } from './validator';
import { IslExecutor } from './executor';
import { IslCompletionProvider } from './completion';
import { IslHoverProvider } from './hover';
import { IslDefinitionProvider } from './definition';
import { IslCodeLensProvider, runIslFunction, showUsages, testFunction } from './codelens';
import { IslSignatureHelpProvider } from './signature';
import { IslInlayHintsProvider } from './inlayhints';
import { IslCodeActionProvider, extractVariable, extractFunction, convertToTemplateString, useCoalesceOperator, useMathSum, formatChain, formatObject } from './codeactions';
import { IslDocumentHighlightProvider } from './highlights';

export function activate(context: vscode.ExtensionContext) {
    console.log('ISL Language Support is now active');

    const documentSelector: vscode.DocumentSelector = [
        { scheme: 'file', language: 'isl' },
        { scheme: 'untitled', language: 'isl' }
    ];

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

    // Register signature help provider
    const signatureHelpProvider = new IslSignatureHelpProvider();
    context.subscriptions.push(
        vscode.languages.registerSignatureHelpProvider(
            documentSelector,
            signatureHelpProvider,
            '(', ',', ' '
        )
    );

    // Register inlay hints provider
    const inlayHintsProvider = new IslInlayHintsProvider();
    context.subscriptions.push(
        vscode.languages.registerInlayHintsProvider(documentSelector, inlayHintsProvider)
    );

    // Register document highlight provider for control flow keyword matching
    const highlightProvider = new IslDocumentHighlightProvider();
    context.subscriptions.push(
        vscode.languages.registerDocumentHighlightProvider(documentSelector, highlightProvider)
    );

    // Register code action provider
    const codeActionProvider = new IslCodeActionProvider();
    context.subscriptions.push(
        vscode.languages.registerCodeActionsProvider(
            documentSelector,
            codeActionProvider,
            {
                providedCodeActionKinds: IslCodeActionProvider.providedCodeActionKinds
            }
        )
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
        }),

        vscode.commands.registerCommand('isl.showUsages', async (uri: vscode.Uri, functionName: string, functionType: string) => {
            await showUsages(uri, functionName, functionType);
        }),

        vscode.commands.registerCommand('isl.testFunction', async (uri: vscode.Uri, functionName: string, params: string) => {
            await testFunction(uri, functionName, params, context);
        }),

        // Refactoring commands
        vscode.commands.registerCommand('isl.refactor.extractVariable', extractVariable),
        vscode.commands.registerCommand('isl.refactor.extractFunction', extractFunction),
        vscode.commands.registerCommand('isl.refactor.toTemplateString', convertToTemplateString),

        // Improvement commands
        vscode.commands.registerCommand('isl.improvement.useCoalesceOperator', useCoalesceOperator),
        vscode.commands.registerCommand('isl.improvement.useMathSum', useMathSum),
        vscode.commands.registerCommand('isl.improvement.formatChain', formatChain),
        vscode.commands.registerCommand('isl.improvement.formatObject', formatObject)
    );

    // Enhanced status bar item
    const statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
    context.subscriptions.push(statusBarItem);

    // Update status bar
    function updateStatusBar() {
        const editor = vscode.window.activeTextEditor;
        if (editor && editor.document.languageId === 'isl') {
            const document = editor.document;
            const text = document.getText();
            
            // Count functions and modifiers
            const functionCount = (text.match(/^\s*(fun|modifier)\s+/gm) || []).length;
            
            // Get diagnostics count
            const diagnostics = vscode.languages.getDiagnostics(document.uri);
            const errorCount = diagnostics.filter(d => d.severity === vscode.DiagnosticSeverity.Error).length;
            const warningCount = diagnostics.filter(d => d.severity === vscode.DiagnosticSeverity.Warning).length;
            
            // Build status text
            let statusText = '$(check) ISL';
            if (functionCount > 0) {
                statusText += ` | ${functionCount} ${functionCount === 1 ? 'function' : 'functions'}`;
            }
            
            if (errorCount > 0) {
                statusText += ` | $(error) ${errorCount}`;
            } else if (warningCount > 0) {
                statusText += ` | $(warning) ${warningCount}`;
            } else {
                statusText += ` | $(pass) Valid`;
            }
            
            statusBarItem.text = statusText;
            statusBarItem.tooltip = `ISL Language Support\n${functionCount} functions\n${errorCount} errors, ${warningCount} warnings`;
            statusBarItem.command = 'isl.showDocumentation';
            statusBarItem.show();
        } else {
            statusBarItem.hide();
        }
    }

    // Update status bar on various events
    context.subscriptions.push(
        vscode.window.onDidChangeActiveTextEditor(updateStatusBar),
        vscode.workspace.onDidChangeTextDocument(e => {
            if (e.document.languageId === 'isl') {
                updateStatusBar();
            }
        }),
        vscode.languages.onDidChangeDiagnostics(e => {
            const editor = vscode.window.activeTextEditor;
            if (editor && e.uris.some(uri => uri.toString() === editor.document.uri.toString())) {
                updateStatusBar();
            }
        })
    );

    // Initial status bar update
    updateStatusBar();
}

export function deactivate() {
    console.log('ISL Language Support is now deactivated');
}

