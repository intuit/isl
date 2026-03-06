import * as vscode from 'vscode';
import * as path from 'path';

type YamlContext = 'root' | 'setup' | 'tests';

interface YamlStructure {
    rootIndent: number;
    hasCategory: boolean;
    hasSetup: boolean;
    hasAssertOptions: boolean;
    hasTests: boolean;
    setupIndent: number;
    testsIndent: number;
    context: YamlContext;
    /** True when cursor is inside assertOptions block (suite or per-test). */
    inAssertOptions: boolean;
}

const ASSERT_OPTION_NAMES = [
    'nullSameAsMissing',
    'nullSameAsEmptyArray',
    'missingSameAsEmptyArray',
    'ignoreExtraFieldsInActual',
    'numbersEqualIgnoreFormat'
];

/** Completions for *.tests.yaml ISL test suite files: schema keys and function names. */
export class IslYamlTestsCompletionProvider implements vscode.CompletionItemProvider {
    private readonly functionNameCache = new Map<string, { mtime: number; names: string[] }>();
    private static readonly CACHE_TTL_MS = 10000;

    async provideCompletionItems(
        document: vscode.TextDocument,
        position: vscode.Position,
        _token: vscode.CancellationToken,
        _context: vscode.CompletionContext
    ): Promise<vscode.CompletionItem[] | vscode.CompletionList | null> {
        if (!document.uri.fsPath.endsWith('.tests.yaml')) {
            return null;
        }

        const lineText = document.lineAt(position).text;
        const linePrefix = lineText.substring(0, position.character);
        const indent = lineText.match(/^\s*/)?.[0] ?? '';
        const indentLen = indent.length;
        const content = document.getText();
        const lines = content.split(/\r?\n/);
        const currentLineIndex = position.line;
        const structure = this.getYamlStructure(lines, currentLineIndex, indentLen);

        // ---- Root level: category, setup, assertOptions, tests, islTests (not when inside assertOptions block) ----
        if (structure.context === 'root' && !structure.inAssertOptions && indentLen <= structure.rootIndent + 2) {
            const rootKeyMatch = linePrefix.match(/^(\s*)(\w*)$/);
            const keyPrefix = rootKeyMatch ? rootKeyMatch[2] : '';
            const items = this.getRootKeyCompletions(structure, keyPrefix);
            if (items.length > 0) return items;
        }

        // ---- Inside setup: islSource, mockSource, mocks ----
        if (structure.context === 'setup') {
            const setupKeyMatch = linePrefix.match(/^(\s*)(\w*)$/);
            if (setupKeyMatch) {
                const keyPrefix = setupKeyMatch[2];
                const items = this.getSetupKeyCompletions(keyPrefix);
                if (items.length > 0) return items;
            }
        }

        // ---- Inside tests/islTests array: list item keys ----
        const insideTestsArray = structure.context === 'tests' && indentLen > structure.testsIndent;
        const listItemKeyMatch = linePrefix.match(/^(\s*)-\s*(\w*)$/);
        if (insideTestsArray && listItemKeyMatch) {
            const keyPrefix = listItemKeyMatch[2];
            const items = this.getTestEntryKeyCompletions(keyPrefix, listItemKeyMatch[1].length + 2);
            if (items.length > 0) return items;
        }

        // ---- functionName value: suggest ISL function names ----
        const afterFunctionNameKey = linePrefix.match(/^\s*functionName\s*:\s*["']?(\w*)$/);
        if (insideTestsArray && afterFunctionNameKey) {
            const valuePrefix = afterFunctionNameKey[1];
            const names = await this.getIslFunctionNames(document);
            const items = names
                .filter(n => !valuePrefix || n.toLowerCase().startsWith(valuePrefix.toLowerCase()))
                .map(n => {
                    const item = new vscode.CompletionItem(n, vscode.CompletionItemKind.Function);
                    item.detail = 'ISL function or modifier';
                    item.insertText = n;
                    return item;
                });
            if (items.length > 0) return items;
        }

        // ---- assertOptions keys (suite or per-test): option names ----
        const assertOptionKeyMatch = linePrefix.match(/^(\s*)(\w*)$/);
        if (structure.inAssertOptions && assertOptionKeyMatch) {
            const keyPrefix = assertOptionKeyMatch[2];
            const items = ASSERT_OPTION_NAMES
                .filter(n => !keyPrefix || n.toLowerCase().startsWith(keyPrefix.toLowerCase()))
                .map(n => {
                    const item = new vscode.CompletionItem(n, vscode.CompletionItemKind.Property);
                    item.detail = 'Assert option';
                    item.insertText = new vscode.SnippetString(`${n}: \${1:true}`);
                    return item;
                });
            if (items.length > 0) return items;
        }

        return null;
    }

    private getYamlStructure(lines: string[], currentLineIndex: number, currentIndent: number): YamlStructure {
        let rootIndent = 0;
        let hasCategory = false;
        let hasSetup = false;
        let hasAssertOptions = false;
        let hasTests = false;
        let setupIndent = -1;
        let testsIndent = -1;
        let context: YamlContext = 'root';

        const keyRe = /^(\s*)(\w+)\s*:/;
        for (let i = 0; i <= currentLineIndex && i < lines.length; i++) {
            const line = lines[i];
            const m = line.match(keyRe);
            if (m) {
                const ind = m[1].length;
                const key = m[2];
                if (i === 0 || ind <= rootIndent) {
                    rootIndent = ind;
                }
                if (ind === rootIndent) {
                    hasCategory = hasCategory || key === 'category';
                    hasSetup = hasSetup || key === 'setup';
                    hasAssertOptions = hasAssertOptions || key === 'assertOptions';
                    hasTests = hasTests || key === 'tests' || key === 'islTests';
                    if (key === 'setup') setupIndent = ind;
                    if (key === 'tests' || key === 'islTests') testsIndent = ind;
                    context = 'root';
                } else if (setupIndent >= 0 && ind > setupIndent && (testsIndent < 0 || ind <= testsIndent)) {
                    context = 'setup';
                } else if (testsIndent >= 0 && ind > testsIndent) {
                    context = 'tests';
                }
            }
        }

        // InAssertOptions: scan backward; only stop when we find a key with strictly less indent (parent). If that parent is assertOptions, we're inside it.
        let inAssertOptions = false;
        for (let i = currentLineIndex; i >= 0; i--) {
            const line = lines[i];
            const m = line.match(keyRe);
            if (m) {
                const ind = m[1].length;
                if (ind < currentIndent) {
                    if (m[2] === 'assertOptions') inAssertOptions = true;
                    break;
                }
            }
        }

        return {
            rootIndent,
            hasCategory,
            hasSetup,
            hasAssertOptions,
            hasTests,
            setupIndent: setupIndent >= 0 ? setupIndent : 0,
            testsIndent: testsIndent >= 0 ? testsIndent : 0,
            context,
            inAssertOptions
        };
    }

    private getRootKeyCompletions(structure: YamlStructure, keyPrefix: string): vscode.CompletionItem[] {
        const items: vscode.CompletionItem[] = [];

        const add = (key: string, detail: string, doc: string, insert: vscode.SnippetString | string) => {
            if (keyPrefix && !key.toLowerCase().startsWith(keyPrefix.toLowerCase())) return;
            const item = new vscode.CompletionItem(key, vscode.CompletionItemKind.Property);
            item.detail = detail;
            item.documentation = new vscode.MarkdownString(doc);
            item.insertText = typeof insert === 'string' ? insert : insert;
            items.push(item);
        };

        if (!structure.hasCategory) {
            add('category', 'Test group label in output', 'Optional label for the suite in results.', 'category: ${1:my-group-name}');
        }
        if (!structure.hasSetup) {
            add('setup', 'ISL source and mocks', 'Required. Contains **islSource** and optionally **mockSource**, **mocks**.', new vscode.SnippetString('setup:\n  islSource: ${1:module.isl}'));
        }
        if (!structure.hasAssertOptions) {
            add('assertOptions', 'Comparison options for expected vs actual', 'e.g. nullSameAsMissing, ignoreExtraFieldsInActual.', new vscode.SnippetString('assertOptions:\n  nullSameAsMissing: ${1:true}'));
        }
        if (!structure.hasTests) {
            add('tests', 'List of test entries', 'Same as islTests. Each entry: name, functionName, input, expected.', new vscode.SnippetString('tests:\n  - name: "${1:Test name}"\n    functionName: ${2:functionName}'));
            add('islTests', 'List of test entries', 'Same as tests. Each entry: name, functionName, input, expected.', new vscode.SnippetString('islTests:\n  - name: "${1:Test name}"\n    functionName: ${2:functionName}'));
        }

        return items;
    }

    private getSetupKeyCompletions(keyPrefix: string): vscode.CompletionItem[] {
        const items: vscode.CompletionItem[] = [];

        const add = (key: string, detail: string, insert: string | vscode.SnippetString) => {
            if (keyPrefix && !key.toLowerCase().startsWith(keyPrefix.toLowerCase())) return;
            const item = new vscode.CompletionItem(key, vscode.CompletionItemKind.Property);
            item.detail = detail;
            item.insertText = typeof insert === 'string' ? insert : insert;
            items.push(item);
        };

        add('islSource', 'ISL file to test (required)', 'islSource: ${1:module.isl}');
        add('mockSource', 'Mock definitions file(s)', 'mockSource: ${1:optional.yaml}');
        add('mocks', 'Inline mocks (func / annotation arrays)', new vscode.SnippetString('mocks:\n  func:\n    - name: "${1:FuncName}"\n      return: ${2:null}'));

        return items;
    }

    private getTestEntryKeyCompletions(keyPrefix: string, itemIndent: number): vscode.CompletionItem[] {
        const items: vscode.CompletionItem[] = [];
        const pad = ' '.repeat(itemIndent);

        const add = (key: string, detail: string, insert: string | vscode.SnippetString) => {
            if (keyPrefix && !key.toLowerCase().startsWith(keyPrefix.toLowerCase())) return;
            const item = new vscode.CompletionItem(key, vscode.CompletionItemKind.Property);
            item.detail = detail;
            item.insertText = typeof insert === 'string' ? insert : insert;
            items.push(item);
        };

        add('name', 'Display name for the test', new vscode.SnippetString('name: "${1:Test name}"'));
        add('functionName', 'ISL function to call', new vscode.SnippetString('functionName: ${1:functionName}'));
        add('input', 'Input (scalar or object with param names)', new vscode.SnippetString('input: ${1:null}'));
        add('expected', 'Expected return value', new vscode.SnippetString('expected: ${1:null}'));
        add('byPassAnnotations', 'Bypass annotation processing', 'byPassAnnotations: ${1:false}');
        add('assertOptions', 'Per-test assert options', new vscode.SnippetString('assertOptions:\n  ${1:nullSameAsMissing}: true'));

        if (!keyPrefix) {
            const fullItem = new vscode.CompletionItem('Test entry (name + functionName + input/expected)', vscode.CompletionItemKind.Snippet);
            fullItem.detail = 'Insert full test entry';
            fullItem.insertText = new vscode.SnippetString(`name: "\${1:Test name}"\n${pad}functionName: \${2:functionName}\n${pad}input: \${3:null}\n${pad}expected: \${4:null}`);
            items.push(fullItem);
        }

        return items;
    }

    /** Collect fun/modifier names from workspace .isl files (same dir, tests/, and root). */
    private async getIslFunctionNames(document: vscode.TextDocument): Promise<string[]> {
        const folder = vscode.workspace.getWorkspaceFolder(document.uri);
        if (!folder) return [];

        const dir = path.dirname(document.uri.fsPath);
        const cacheKey = `${folder.uri.fsPath}:${dir}`;
        const now = Date.now();
        const cached = this.functionNameCache.get(cacheKey);
        if (cached && (now - cached.mtime) < IslYamlTestsCompletionProvider.CACHE_TTL_MS) {
            return cached.names;
        }

        const names = new Set<string>();
        const pattern = new vscode.RelativePattern(folder, '**/*.isl');
        const uris = await vscode.workspace.findFiles(pattern);
        const funcPattern = /^\s*(fun|modifier)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/;

        for (const uri of uris) {
            try {
                const doc = await vscode.workspace.openTextDocument(uri);
                const lines = doc.getText().split(/\r?\n/);
                for (const line of lines) {
                    const m = line.match(funcPattern);
                    if (m) names.add(m[2]);
                }
            } catch {
                // skip unreadable files
            }
        }

        const list = Array.from(names).sort();
        this.functionNameCache.set(cacheKey, { mtime: now, names: list });
        return list;
    }
}
