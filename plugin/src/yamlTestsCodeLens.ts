import * as vscode from 'vscode';
import { parseYamlIslTestsCodeLensInfos, yamlHasIslTests, startYamlIslTestDebugFromUri } from './testExplorer';

/**
 * CodeLens on each `islTests` entry in *.tests.yaml — Debug with suite mocks (same as Test Explorer debug profile).
 */
export class IslYamlTestsCodeLensProvider implements vscode.CodeLensProvider {
    private readonly _onDidChangeCodeLenses = new vscode.EventEmitter<void>();
    readonly onDidChangeCodeLenses = this._onDidChangeCodeLenses.event;

    provideCodeLenses(
        document: vscode.TextDocument,
        _token: vscode.CancellationToken
    ): vscode.ProviderResult<vscode.CodeLens[]> {
        if (!document.uri.fsPath.endsWith('.tests.yaml')) {
            return [];
        }
        const text = document.getText();
        if (!yamlHasIslTests(text)) {
            return [];
        }
        const infos = parseYamlIslTestsCodeLensInfos(document.uri, text);
        return infos.map(
            (info) =>
                new vscode.CodeLens(info.range, {
                    title: '🐛 Debug',
                    command: 'isl.debugYamlTest',
                    arguments: [document.uri.toString(), info.index],
                    tooltip: `Debug "${info.label}" (${info.functionName}) with suite mocks and YAML input`
                })
        );
    }

    resolveCodeLens(codeLens: vscode.CodeLens, _token: vscode.CancellationToken): vscode.CodeLens {
        return codeLens;
    }

    refresh(): void {
        this._onDidChangeCodeLenses.fire();
    }
}

export async function debugYamlTestFromCommand(yamlUriStr: string, testIndex: number): Promise<void> {
    const yamlUri = vscode.Uri.parse(yamlUriStr);
    await startYamlIslTestDebugFromUri(yamlUri, testIndex);
}
