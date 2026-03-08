import * as vscode from 'vscode';
import * as path from 'path';
import { IslDocumentFormatter } from './formatter';
import { IslValidator } from './validator';
import { IslExecutor } from './executor';
import { IslCompletionProvider } from './completion';
import { IslHoverProvider } from './hover';
import { IslDefinitionProvider } from './definition';
import { IslCodeLensProvider, runIslFunction, showUsages, testFunction } from './codelens';
import { IslSignatureHelpProvider } from './signature';
import { IslInlayHintsProvider } from './inlayhints';
import { IslCodeActionProvider, extractVariable, extractFunction, convertToTemplateString, useCoalesceOperator, useMathSum, formatChain, formatObject, renameDuplicateFunction } from './codeactions';
import { IslDocumentHighlightProvider } from './highlights';
import { IslExtensionsManager } from './extensions';
import { initIslLanguage } from './language';
import { IslTypeManager } from './types';
import { IslTestController, isTestFile, yamlHasIslTests, addMockToFile, addMockToTestFile } from './testExplorer';
import { IslYamlTestsCompletionProvider } from './islYamlTestsCompletion';
import { IslPasteEditProvider } from './islPasteProvider';

const outputChannelName = 'ISL Language Support';

export function activate(context: vscode.ExtensionContext) {
    const outputChannel = vscode.window.createOutputChannel(outputChannelName);
    try {
        initIslLanguage(context.extensionPath);
        outputChannel.appendLine('[ISL Language Support] Extension is now active');

    const documentSelector: vscode.DocumentSelector = [
        { scheme: 'file', language: 'isl' },
        { scheme: 'untitled', language: 'isl' }
    ];

    // Initialize extensions manager with output channel for extension logs
    const extensionsManager = new IslExtensionsManager(outputChannel);
    context.subscriptions.push(extensionsManager);

    const typeManager = new IslTypeManager(extensionsManager, outputChannel);
    context.subscriptions.push(typeManager);

    // ISL Test Explorer - discovers @test/@setup in tests/**/*.isl and *.tests.yaml (islTests)
    // Use a dedicated "ISL Tests" output channel for test run results
    const islTestsOutputChannel = vscode.window.createOutputChannel('ISL Tests');
    const testController = new IslTestController(islTestsOutputChannel, context.extensionPath);
    context.subscriptions.push({ dispose: () => testController.dispose() });

    // Update "Run all Tests in file" button visibility when active editor changes
    const updateIsTestFileContext = (doc: vscode.TextDocument | undefined) => {
        if (!doc || doc.uri.scheme !== 'file') {
            vscode.commands.executeCommand('setContext', 'isl.isTestFile', false);
            return;
        }
        const isIsl = doc.uri.fsPath.endsWith('.isl');
        const isYamlTests = doc.uri.fsPath.endsWith('.tests.yaml');
        if (isIsl) {
            const folder = vscode.workspace.getWorkspaceFolder(doc.uri);
            vscode.commands.executeCommand('setContext', 'isl.isTestFile', !!folder && isTestFile(doc.uri, folder));
        } else if (isYamlTests) {
            vscode.commands.executeCommand('setContext', 'isl.isTestFile', yamlHasIslTests(doc.getText()));
        } else {
            vscode.commands.executeCommand('setContext', 'isl.isTestFile', false);
        }
    };
    context.subscriptions.push(
        vscode.window.onDidChangeActiveTextEditor(e => updateIsTestFileContext(e?.document)),
        vscode.workspace.onDidChangeTextDocument(e => {
            if (vscode.window.activeTextEditor?.document === e.document) {
                updateIsTestFileContext(e.document);
            }
        })
    );
    updateIsTestFileContext(vscode.window.activeTextEditor?.document);

    // Preload extensions so first completion/validation has a warm cache
    extensionsManager.preloadExtensions().catch(() => {});

    // Register formatter
    const formatter = new IslDocumentFormatter();
    context.subscriptions.push(
        vscode.languages.registerDocumentFormattingEditProvider(documentSelector, formatter),
        vscode.languages.registerDocumentRangeFormattingEditProvider(documentSelector, formatter)
    );

    // Register validator (with output channel for validation logs and type manager for schema checks)
    const validator = new IslValidator(extensionsManager, { outputChannel, typeManager });
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

    // Register completion provider (triggers: $ @ . | - not { or , so user can type { and Enter without popup)
    const completionProvider = new IslCompletionProvider(extensionsManager, typeManager, outputChannel);
    context.subscriptions.push(
        vscode.languages.registerCompletionItemProvider(documentSelector, completionProvider, '$', '@', '.', '|')
    );

    // Autocomplete for ISL tests YAML (*.tests.yaml): root keys, setup, test entries, assertOptions, function names
    const yamlTestsSelector: vscode.DocumentSelector = { language: 'yaml', pattern: '**/*.tests.yaml' };
    const yamlTestsCompletionProvider = new IslYamlTestsCompletionProvider();
    context.subscriptions.push(
        vscode.languages.registerCompletionItemProvider(yamlTestsSelector, yamlTestsCompletionProvider, ':', '-', '\n')
    );

    // Register hover provider
    const hoverProvider = new IslHoverProvider(extensionsManager, typeManager);
    context.subscriptions.push(
        vscode.languages.registerHoverProvider(documentSelector, hoverProvider)
    );

    // Watch for extensions file changes and revalidate
    extensionsManager.onDidChange((uri) => {
        // Revalidate all open ISL documents
        vscode.workspace.textDocuments.forEach(doc => {
            if (doc.languageId === 'isl') {
                validator.validate(doc);
            }
        });
        vscode.window.showInformationMessage('ISL extensions reloaded');
    });

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

    // Paste provider: insert clipboard text as-is to avoid VS Code adding indentation when pasting
    // in the middle of a line (e.g. between quotes in $var.message = "").
    context.subscriptions.push(
        vscode.languages.registerDocumentPasteEditProvider(
            documentSelector,
            new IslPasteEditProvider(),
            {
                providedPasteEditKinds: [vscode.DocumentDropOrPasteEditKind.Text],
                pasteMimeTypes: ['text/plain']
            }
        )
    );
    // Refresh CodeLens when ISL documents change (e.g. after rename, copy-paste) - debounced
    let codeLensRefreshTimeout: NodeJS.Timeout | undefined;
    context.subscriptions.push(
        vscode.workspace.onDidChangeTextDocument(event => {
            if (event.document.languageId === 'isl') {
                if (codeLensRefreshTimeout) clearTimeout(codeLensRefreshTimeout);
                codeLensRefreshTimeout = setTimeout(() => {
                    codeLensRefreshTimeout = undefined;
                    codeLensProvider.refresh();
                }, 1500);
            }
        })
    );

    // Register executor
    const executor = new IslExecutor(context.extensionPath);

    // ISL terminal profile: shell with lib/ on PATH so user can run "isl" or "isl.bat" / "isl.sh"
    const islLibPath = path.join(context.extensionPath, 'lib');
    context.subscriptions.push(
        vscode.window.registerTerminalProfileProvider('isl.terminal-profile', {
            provideTerminalProfile(_token: vscode.CancellationToken): vscode.ProviderResult<vscode.TerminalProfile> {
                const pathEnv = process.env.PATH || process.env.Path || '';
                const newPath = `${islLibPath}${path.delimiter}${pathEnv}`;
                const shellPath = process.platform === 'win32'
                    ? (process.env.COMSPEC || 'cmd.exe')
                    : (process.env.SHELL || '/bin/bash');
                const workspaceRoot = vscode.workspace.workspaceFolders?.[0]?.uri;
                return new vscode.TerminalProfile({
                    name: 'ISL',
                    shellPath,
                    cwd: workspaceRoot,
                    env: { ...process.env, PATH: newPath }
                });
            }
        })
    );

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

        vscode.commands.registerCommand('isl.compile', async () => {
            const editor = vscode.window.activeTextEditor;
            if (editor && editor.document.languageId === 'isl') {
                await executor.compile(editor.document);
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

        vscode.commands.registerCommand('isl.runAllTestsInFile', async () => {
            const editor = vscode.window.activeTextEditor;
            if (editor) {
                await testController.runTestsInFile(editor.document.uri);
            }
        }),

        // Refactoring commands
        vscode.commands.registerCommand('isl.refactor.extractVariable', extractVariable),
        vscode.commands.registerCommand('isl.refactor.extractFunction', extractFunction),
        vscode.commands.registerCommand('isl.refactor.toTemplateString', convertToTemplateString),

        // Improvement commands
        vscode.commands.registerCommand('isl.improvement.useCoalesceOperator', useCoalesceOperator),
        vscode.commands.registerCommand('isl.improvement.useMathSum', useMathSum),
        vscode.commands.registerCommand('isl.improvement.formatChain', formatChain),
        vscode.commands.registerCommand('isl.improvement.formatObject', formatObject),

        // Quick fix commands
        vscode.commands.registerCommand('isl.quickFix.renameDuplicateFunction', (uri: vscode.Uri, lineNumber: number, functionName: string, kind: 'fun' | 'modifier') =>
            renameDuplicateFunction(uri, lineNumber, functionName, kind)),

        // Add mock from test failure (triggered by link in test message).
        // VS Code may pass the JSON array as separate arguments (arg0, arg1, ...) or as a single JSON string.
        vscode.commands.registerCommand('isl.addMockFromTestError', async (...allArgs: unknown[]) => {
            let arr: unknown[];
            if (allArgs.length >= 5) {
                arr = allArgs;
            } else if (allArgs.length === 1 && typeof allArgs[0] === 'string') {
                try {
                    let parsed = JSON.parse(allArgs[0]) as unknown;
                    arr = (typeof parsed === 'string' ? JSON.parse(parsed) : parsed) as unknown[];
                } catch {
                    arr = allArgs;
                }
            } else {
                arr = allArgs;
            }
            const [testFileUriStr, mockFileName, functionName, paramsJson, yamlSnippet, addToTestFile] = arr as [string?, string?, string?, string?, string?, boolean?];

            if (!testFileUriStr || functionName == null || paramsJson == null || !yamlSnippet) {
                vscode.window.showErrorMessage('ISL: Invalid add-mock arguments.');
                return;
            }
            if (!addToTestFile && !mockFileName) {
                vscode.window.showErrorMessage('ISL: Invalid add-mock arguments (missing mock file).');
                return;
            }
            const testFileUri = vscode.Uri.parse(testFileUriStr);
            try {
                if (addToTestFile) {
                    await addMockToTestFile(testFileUri, functionName, paramsJson, yamlSnippet, outputChannel);
                    vscode.window.showInformationMessage(`Added mock for @.${functionName} to test file (setup)`);
                } else {
                    await addMockToFile(testFileUri, mockFileName!, functionName, paramsJson, yamlSnippet, outputChannel);
                    vscode.window.showInformationMessage(`Added mock for @.${functionName} to ${mockFileName}`);
                }
            } catch (e) {
                const msg = e instanceof Error ? e.message : String(e);
                vscode.window.showErrorMessage(`ISL: Failed to add mock: ${msg}`);
                outputChannel.appendLine(`[ISL] addMockFromTestError failed: ${msg}`);
            }
        })
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
    } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        const stack = err instanceof Error ? err.stack : '';
        outputChannel.appendLine(`[ISL Language Support] Activation failed: ${msg}`);
        if (stack) outputChannel.appendLine(stack);
        outputChannel.show(true);
        vscode.window.showErrorMessage(`ISL Language Support failed to activate: ${msg}. See Output > ISL Language Support for details.`);
    }
}

export function deactivate() {
    console.log('ISL Language Support is now deactivated');
}

