import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import * as os from 'os';
import * as cp from 'child_process';
import * as yaml from 'js-yaml';

/** Test file pattern: tests folder anywhere in workspace; applies to files from that folder down (e.g. tests/, src/tests/, a/b/tests/) */
const TEST_FILE_GLOB = '**/tests/**/*.isl';

/** Max length of inline diff payload in command URI; above this we use temp files. */
const MAX_INLINE_DIFF_PAYLOAD = 2400;

/** Max length of raw failure message shown in Test UI; longer messages are truncated. */
const MAX_MESSAGE_DISPLAY = 8000;

// ANSI color codes for test output panel coloring
const ANSI_GRAY  = '\u001B[90m';
const ANSI_GREEN = '\u001B[32m';
const ANSI_RED   = '\u001B[31m';
const ANSI_RESET = '\u001B[0m';

/**
 * Colorizes a single line of ISL test runner output using ANSI codes.
 * - [ISL Mock] and [ISL resolve] lines → gray (less prominent background lines)
 * - [ISL Result] prefix → green on pass lines, red on fail lines / summary with failures
 */
function colorizeIslLine(line: string): string {
    if (line.includes('[ISL Mock]') || line.includes('[ISL resolve]') || line.includes('[ISL load]')) {
        return `${ANSI_GRAY}${line}${ANSI_RESET}`;
    }
    if (line.includes('[ISL Result]')) {
        if (line.includes('[PASS]')) {
            return line.replace('[ISL Result]', `${ANSI_GREEN}[ISL Result]${ANSI_RESET}`);
        }
        if (line.includes('[FAIL]')) {
            return line.replace('[ISL Result]', `${ANSI_RED}[ISL Result]${ANSI_RESET}`);
        }
        const summaryMatch = line.match(/Results:\s*(\d+)\s*passed,\s*(\d+)\s*failed/);
        if (summaryMatch) {
            const failed = parseInt(summaryMatch[2], 10);
            const color = failed > 0 ? ANSI_RED : ANSI_GREEN;
            return line.replace('[ISL Result]', `${color}[ISL Result]${ANSI_RESET}`);
        }
    }
    return line;
}

/**
 * Colorizes a chunk of ISL test runner output (may contain multiple \r\n-terminated lines).
 */
function colorizeIslOutput(chunk: string): string {
    // Split on \r\n, colorize each line, rejoin preserving line endings
    const parts = chunk.split('\r\n');
    return parts.map((line, i) => {
        const colored = colorizeIslLine(line);
        return i < parts.length - 1 ? colored + '\r\n' : colored;
    }).join('');
}

/** YAML test suite files: [name].tests.yaml that contain islTests property */
const YAML_TEST_FILE_GLOB = '**/*.tests.yaml';

/** Parsed test info from an ISL test file */
export interface IslTestInfo {
    id: string;
    label: string;
    group?: string;
    functionName: string;
    range: vscode.Range;
    uri: vscode.Uri;
}

/** Parsed setup info */
export interface IslSetupInfo {
    functionName: string;
    range: vscode.Range;
}

/** Parsed test entry from a *.tests.yaml file (islTests array) */
export interface YamlTestEntryInfo {
    id: string;
    label: string;
    functionName: string;
    range: vscode.Range;
    uri: vscode.Uri;
}

/**
 * Check if YAML content looks like an ISL test suite (has top-level islTests array).
 */
export function yamlHasIslTests(content: string): boolean {
    try {
        const doc = yaml.load(content) as Record<string, unknown> | null;
        return !!doc && Array.isArray(doc.islTests);
    } catch {
        return false;
    }
}

/**
 * Parse islTests from a *.tests.yaml file. Returns test entries with id, label, functionName, range.
 * Uses simple line scan to approximate range for each entry (line of "- name:" or "name:").
 */
export function parseYamlIslTests(uri: vscode.Uri, content: string): YamlTestEntryInfo[] {
    const entries: YamlTestEntryInfo[] = [];
    try {
        const doc = yaml.load(content) as { islTests?: Array<{ name?: string; functionName?: string }> } | null;
        const list = doc?.islTests;
        if (!Array.isArray(list)) return entries;

        const lines = content.split(/\r?\n/);
        let listStartLine = -1;
        for (let i = 0; i < lines.length; i++) {
            const trimmed = lines[i].trim();
            if (/^islTests\s*:/.test(trimmed)) {
                listStartLine = i;
                break;
            }
        }
        if (listStartLine < 0) listStartLine = 0;

        for (let idx = 0; idx < list.length; idx++) {
            const entry = list[idx];
            if (!entry || typeof entry !== 'object') continue;
            const name = (entry.name != null && entry.name !== '') ? String(entry.name) : `Test ${idx + 1}`;
            const functionName = (entry.functionName != null && entry.functionName !== '') ? String(entry.functionName) : name;
            const id = `${uri.toString()}#${functionName}#${idx}`;
            const line = listStartLine + 1 + idx;
            const range = new vscode.Range(line, 0, line, 999);
            entries.push({ id, label: name, functionName, range, uri });
        }
    } catch {
        // ignore parse errors
    }
    return entries;
}

/**
 * Parse @test and @setup annotations from ISL test file content.
 * Supports:
 *   @setup
 *   fun setup() { ... }
 *
 *   @test or @test(optional name) or @test({ name: "test name", group: "test group" })
 *   fun testABCD(...) { ... }
 */
export function parseIslTests(uri: vscode.Uri, content: string): { tests: IslTestInfo[]; setup?: IslSetupInfo } {
    const tests: IslTestInfo[] = [];
    let setup: IslSetupInfo | undefined;
    const lines = content.split(/\r?\n/);

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const trimmed = line.trim();

        // @setup - must be followed by fun setup()
        const setupMatch = trimmed.match(/^@setup\s*$/);
        if (setupMatch) {
            const nextLine = lines[i + 1]?.trim() ?? '';
            const funMatch = nextLine.match(/^\s*fun\s+(setup)\s*\(\s*\)/);
            if (funMatch) {
                const start = new vscode.Position(i, line.indexOf('@setup'));
                const end = new vscode.Position(i, line.length);
                setup = {
                    functionName: 'setup',
                    range: new vscode.Range(start, end)
                };
            }
            continue;
        }

        // @test - various forms: @test, @test(), @test(name), @test({ name, group })
        const testAnnMatch = trimmed.match(/^@test\s*(?:\(\s*(.*?)\s*\))?\s*$/);
        if (testAnnMatch) {
            const nextLine = lines[i + 1]?.trim() ?? '';
            const funMatch = nextLine.match(/^\s*fun\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/);
            if (funMatch) {
                const functionName = funMatch[1];
                const argPart = testAnnMatch[1]?.trim();

                // When no name in annotation: use function name, strip test_ prefix if present
                let label = functionName.startsWith('test_')
                    ? functionName.slice(6)
                    : functionName;
                let group: string | undefined;

                if (argPart) {
                    // @test({ name: "x", group: "y" }) or @test("name") or @test(name)
                    const objMatch = argPart.match(/\{\s*name\s*:\s*["']?([^"',}]+)["']?\s*(?:,\s*group\s*:\s*["']?([^"',}]+)["']?)?\s*\}/);
                    if (objMatch) {
                        label = objMatch[1].trim();
                        group = objMatch[2]?.trim();
                    } else {
                        // Simple string: @test("My Test") or @test(My Test)
                        const strMatch = argPart.match(/^["'](.+)["']$/);
                        label = strMatch ? strMatch[1] : argPart;
                    }
                }

                // Include line number in id so copy-pasted functions (same name) get unique ids
                const id = `${uri.toString()}#${functionName}#${i}`;
                const start = new vscode.Position(i, line.indexOf('@test'));
                const end = new vscode.Position(i, line.length);

                tests.push({
                    id,
                    label,
                    group,
                    functionName,
                    range: new vscode.Range(start, end),
                    uri
                });
            }
        }
    }

    return { tests, setup };
}

/** JSON output from isl-cmd test -o output.json */
interface IslTestResultJson {
    passed: number;
    failed: number;
    total: number;
    success: boolean;
    results: Array<{
        testFile: string;
        functionName: string;
        testName: string;
        testGroup: string | null;
        success: boolean;
        message: string | null;
        errorPosition: {
            file: string;
            line: number;
            column: number;
            endLine?: number | null;
            endColumn?: number | null;
        } | null;
    }>;
}

/** Full expected/actual payload parsed from "[ISL Assert] Result Differences:" message. */
export interface FullResultDiff {
    expected: string;
    actual: string;
}

/**
 * Parse the full Expected and Actual JSON payloads from the "[ISL Assert] Result Differences:" section.
 * Format produced by YamlUnitTestRunner.buildComparisonFailureMessage:
 *   [ISL Assert] Result Differences:
 *   Expected: <full JSON on one line>
 *   Actual: <full JSON on one line>
 */
export function parseFullResultDiffFromMessage(message: string | null): FullResultDiff | null {
    if (!message || !message.includes('[ISL Assert] Result Differences:')) return null;
    const lines = message.split(/\r?\n/);
    let headerIdx = -1;
    for (let i = 0; i < lines.length; i++) {
        if (lines[i].includes('[ISL Assert] Result Differences:')) { headerIdx = i; break; }
    }
    if (headerIdx < 0) return null;

    let expected: string | null = null;
    let actual: string | null = null;
    // The header line itself may contain "Expected: ..." if on the same line
    const headerLine = lines[headerIdx];
    const inlineExp = headerLine.match(/\[ISL Assert\] Result Differences:\s*Expected:\s*(.+)/);
    if (inlineExp) expected = inlineExp[1].trim();

    for (let i = headerIdx + 1; i < lines.length; i++) {
        const line = lines[i];
        if (expected === null) {
            const m = line.match(/^Expected:\s*(.+)$/);
            if (m) { expected = m[1].trim(); continue; }
        }
        if (actual === null) {
            const m = line.match(/^Actual:\s*(.+)$/);
            if (m) { actual = m[1].trim(); break; }
        }
        // Stop searching if we hit the next section header
        if (line.startsWith('[ISL Assert]') && !line.includes('Result Differences:')) break;
    }

    if (expected === null || actual === null) return null;
    return { expected, actual };
}

/** Parsed assertion difference from "[ISL Assert] Difference(s):" message (path, expected, actual). */
export interface AssertDiff {
    path: string;
    expected: string;
    actual: string;
    /** Leaf key to find in YAML expected section (e.g. initialLoanDate). */
    key: string;
}

/**
 * Parse "[ISL Assert] Difference(s):" message into path/expected/actual pairs.
 * Format:
 *   Expected: $path = value
 *   Actual:   $path = value
 */
export function parseAssertDiffsFromMessage(message: string | null): AssertDiff[] {
    const diffs: AssertDiff[] = [];
    if (!message || !message.includes('Difference(s):') || !message.includes('Expected:')) return diffs;
    const lines = message.split(/\r?\n/);
    for (let i = 0; i < lines.length - 1; i++) {
        const expMatch = lines[i].match(/^\s*Expected:\s*(\$[^\s=]+)\s*=\s*(.*)$/);
        if (!expMatch) continue;
        const actMatch = lines[i + 1].match(/^\s*Actual:\s*(\$[^\s=]+)\s*=\s*(.*)$/);
        if (!actMatch || expMatch[1] !== actMatch[1]) continue;
        const pathStr = expMatch[1];
        const expectedVal = expMatch[2].trim();
        const actualVal = actMatch[2].trim();
        const key = getLeafKeyFromPath(pathStr);
        diffs.push({ path: pathStr, expected: expectedVal, actual: actualVal, key });
        i++;
    }
    return diffs;
}

/** Extract leaf key from JSON path like $providerResponses.accounts.[0].initialLoanDate -> initialLoanDate */
function getLeafKeyFromPath(pathStr: string): string {
    const trimmed = pathStr.replace(/^\$\.?/, '').trim();
    if (!trimmed) return '';
    const segments = trimmed.split(/\./).filter(s => s.length > 0);
    for (let i = segments.length - 1; i >= 0; i--) {
        const s = segments[i];
        if (!/^\[\d+\]$/.test(s)) return s;
    }
    return segments[segments.length - 1] ?? '';
}

/**
 * Find ranges in YAML content where the given key appears inside the "expected" section of the test
 * that matches functionName. Returns one range per occurrence (for squiggles).
 */
export function findExpectedKeyRangesInYaml(
    content: string,
    functionName: string,
    key: string
): vscode.Range[] {
    if (!key) return [];
    const lines = content.split(/\r?\n/);
    const ranges: vscode.Range[] = [];

    let islTestsIndent = -1;
    let entryStart = -1;
    let matchingTest = false;
    let expectedStart = -1;
    let expectedEnd = -1;

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const trimmed = line.trim();
        const indent = line.search(/\S/);
        if (indent < 0) continue;

        if (/^islTests\s*:/.test(trimmed)) {
            islTestsIndent = indent;
            continue;
        }
        if (islTestsIndent < 0) continue;

        const isListItem = /^\s*-\s+/.test(line) && indent <= islTestsIndent + 2;
        if (isListItem) {
            if (expectedStart >= 0 && matchingTest) {
                expectedEnd = i;
                for (let j = expectedStart; j < expectedEnd; j++) {
                    pushKeyRanges(lines[j], j, key, ranges);
                }
            }
            entryStart = i;
            matchingTest = false;
            expectedStart = -1;
        }

        if (entryStart >= 0) {
            const fnMatch = trimmed.match(/^(?:functionName|name)\s*:\s*["']?([^"'\s]+)["']?/);
            if (fnMatch && fnMatch[1].trim() === functionName) matchingTest = true;
            if (/^expected\s*:/.test(trimmed) && matchingTest) {
                expectedStart = i;
            }
        }
    }

    if (expectedStart >= 0 && matchingTest && expectedEnd < 0) {
        expectedEnd = lines.length;
        for (let j = expectedStart; j < expectedEnd; j++) {
            const l = lines[j];
            const lineIndent = l.search(/\S/);
            const expIndent = lines[expectedStart].search(/\S/);
            if (j > expectedStart && l.trim() && lineIndent <= expIndent) {
                expectedEnd = j;
                break;
            }
            pushKeyRanges(l, j, key, ranges);
        }
    }

    return ranges;
}

function pushKeyRanges(line: string, lineIndex: number, key: string, ranges: vscode.Range[]): void {
    let col = line.indexOf(key + ':');
    if (col === -1) {
        const jsonMatch = line.match(new RegExp(`"${escapeRegex(key)}"\\s*:`));
        if (jsonMatch && jsonMatch.index !== undefined) col = jsonMatch.index;
    }
    if (col === -1 && line.includes(key)) {
        const quoted = line.indexOf('"' + key + '"');
        if (quoted >= 0) col = quoted + 1;
        else col = line.indexOf(key);
    }
    if (col >= 0) {
        const endCol = Math.min(line.length, col + key.length);
        ranges.push(new vscode.Range(lineIndex, col, lineIndex, endCol));
    }
}

function escapeRegex(s: string): string {
    return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/** Parsed "No mock matched" / "Unmocked function" error from the test runner (suggested mock to add). */
export interface MockSuggestion {
    functionName: string;
    params: string; // JSON array string, e.g. '["start_date"]'
    mockFileName: string;
    yamlSnippet: string; // lines to add under func: (e.g. "- name: ...\n  params: ...\n  result: ...")
    /** When true, add to the test file (setup.mocks) instead of an imported mock file. */
    addToTestFile?: boolean;
}

/**
 * Parse backend "No mock matched" / "Unmocked function" error message to extract mock suggestion.
 * Returns null if the message does not match that format.
 */
export function parseMockSuggestionFromError(message: string | null): MockSuggestion | null {
    if (!message || !message.includes('To mock this function add this to your')) {
        return null;
    }
    const lines = message.split(/\r?\n/);
    let functionName = '';
    let params = '[]';
    let mockFileName = 'commonMocks.yaml';
    let addToTestFile = false;
    let inSnippet = false;
    const snippetLines: string[] = [];

    for (const line of lines) {
        const fnMatch = line.match(/^Function:\s*@\.(.+)$/);
        if (fnMatch) {
            functionName = fnMatch[1].trim();
            continue;
        }
        const paramsMatch = line.match(/^Parameters:\s*(.+)$/);
        if (paramsMatch) {
            params = paramsMatch[1].trim();
            continue;
        }
        const testFileMatch = line.match(/To mock this function add this to your test file \[([^\]]+)\]/);
        if (testFileMatch) {
            mockFileName = testFileMatch[1].trim();
            addToTestFile = true;
            inSnippet = true;
            continue;
        }
        const fileMatch = line.match(/To mock this function add this to your \[([^\]]+)\]/);
        if (fileMatch) {
            mockFileName = fileMatch[1].trim();
            inSnippet = true;
            continue;
        }
        if (inSnippet) {
            if (line.trim() === '' || line.trim() === 'func:') continue;
            if (line.startsWith('- ') || (line.match(/^\s{2,}/) && snippetLines.length > 0)) {
                snippetLines.push(line);
            } else if (snippetLines.length > 0) {
                break;
            }
        }
    }
    if (!functionName && snippetLines.length === 0) return null;
    const yamlSnippet = snippetLines.join('\n').trim();
    if (!yamlSnippet) return null;
    return { functionName, params, mockFileName, yamlSnippet, addToTestFile: addToTestFile || undefined };
}

/**
 * Build a TestMessage for a failed test.
 * - If the message contains an "Unmocked function" error, appends an "Add mock" link.
 * - If the message contains assertion diffs (Expected/Actual pairs), appends a
 *   "See Differences Side by Side" link that opens the Result Comparison Viewer.
 */
export function buildTestFailureMessage(
    rawMessage: string,
    testUri: vscode.Uri | undefined,
    suggestion: MockSuggestion | null,
    testName?: string
): vscode.TestMessage {
    const fullDiff = parseFullResultDiffFromMessage(rawMessage);
    const hasMockLink = !!(suggestion && testUri);
    const hasDiffLink = !!fullDiff && !!testName;

    if (!hasMockLink && !hasDiffLink) {
        return new vscode.TestMessage(rawMessage);
    }

    const md = new vscode.MarkdownString(undefined);

    // "See Differences Side by Side" at the top. Use temp files when payload is too large for command URI.
    if (hasDiffLink) {
        const expectedStr = fullDiff!.expected;
        const actualStr = fullDiff!.actual;
        const inlinePayload = JSON.stringify([testName, expectedStr, actualStr]);
        let payloadForCommand: [string, string, string];

        if (inlinePayload.length > MAX_INLINE_DIFF_PAYLOAD) {
            const id = `isl-diff-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
            const tmpDir = os.tmpdir();
            const expectedPath = path.join(tmpDir, `${id}-expected.json`);
            const actualPath = path.join(tmpDir, `${id}-actual.json`);
            try {
                fs.writeFileSync(expectedPath, expectedStr, 'utf8');
                fs.writeFileSync(actualPath, actualStr, 'utf8');
                payloadForCommand = [
                    testName,
                    vscode.Uri.file(expectedPath).toString(),
                    vscode.Uri.file(actualPath).toString()
                ];
            } catch {
                // Fallback: truncate and pass inline (viewer will show truncated)
                const maxEach = Math.max(200, Math.floor((MAX_INLINE_DIFF_PAYLOAD - testName.length - 50) / 2));
                payloadForCommand = [
                    testName,
                    expectedStr.length <= maxEach ? expectedStr : expectedStr.slice(0, maxEach) + '…',
                    actualStr.length <= maxEach ? actualStr : actualStr.slice(0, maxEach) + '…'
                ];
            }
        } else {
            payloadForCommand = [testName, expectedStr, actualStr];
        }
        const diffPayload = JSON.stringify(JSON.stringify(payloadForCommand));
        const diffCmdUri = `command:isl.showResultDiffViewer?${encodeURIComponent(diffPayload)}`;
        md.appendMarkdown(`[**>> See Differences Side by Side**](${diffCmdUri})\n\n---\n\n`);
    }

    // Trim very long failure message so the Test UI renders reliably
    const displayMessage = rawMessage.length > MAX_MESSAGE_DISPLAY
        ? rawMessage.slice(0, MAX_MESSAGE_DISPLAY) + '\n\n… *(message truncated; use "See Differences Side by Side" for full comparison)*'
        : rawMessage;
    md.appendMarkdown(displayMessage);

    if (hasMockLink) {
        const args = [
            testUri!.toString(),
            suggestion!.mockFileName,
            suggestion!.functionName,
            suggestion!.params,
            suggestion!.yamlSnippet,
            suggestion!.addToTestFile === true
        ];
        // Double-stringify so the command receives one string argument with the full array
        // (Test UI may pass only the first element when passing a raw array).
        const payload = JSON.stringify(JSON.stringify(args));
        const cmdUri = `command:isl.addMockFromTestError?${encodeURIComponent(payload)}`;
        const linkLabel = suggestion!.addToTestFile
            ? 'Add mock to test file (setup)'
            : `Add mock to ${suggestion!.mockFileName}`;
        md.appendMarkdown(`\n\n---\n\n[**${linkLabel}**](${cmdUri})`);
    }

    md.isTrusted = true;
    return new vscode.TestMessage(md);
}

/** Return the last line index (0-based) in content that contains the given substring, or -1. */
function findLastLineContaining(content: string, substring: string): number {
    const lines = content.split(/\r?\n/);
    for (let i = lines.length - 1; i >= 0; i--) {
        if (lines[i].includes(substring)) return i;
    }
    return -1;
}

/**
 * Add the suggested mock entry to the test file under setup.mocks.func (inline mocks in the test file).
 */
export async function addMockToTestFile(
    testFileUri: vscode.Uri,
    functionName: string,
    paramsJson: string,
    yamlSnippet: string,
    outputChannel: vscode.OutputChannel
): Promise<void> {
    const raw = await vscode.workspace.fs.readFile(testFileUri);
    const content = new TextDecoder().decode(raw);
    const doc = (yaml.load(content) as Record<string, unknown>) || {};
    let setup = doc.setup;
    if (!setup || typeof setup !== 'object') {
        setup = { islSource: (setup as Record<string, unknown>)?.islSource ?? '' };
        doc.setup = setup;
    }
    const setupObj = setup as Record<string, unknown>;
    let mocks = setupObj.mocks;
    if (!mocks || typeof mocks !== 'object') {
        mocks = {};
        setupObj.mocks = mocks;
    }
    const mocksObj = mocks as Record<string, unknown>;
    let funcList = mocksObj.func;
    if (!Array.isArray(funcList)) {
        funcList = [];
        mocksObj.func = funcList;
    }
    try {
        const newEntry = yaml.load(yamlSnippet) as unknown;
        const entry = Array.isArray(newEntry) ? newEntry[0] : newEntry;
        if (entry && typeof entry === 'object') {
            (funcList as unknown[]).push(entry);
        } else {
            const entryFromParts = { name: functionName, result: '<replace with expected return value>' };
            try {
                const p = JSON.parse(paramsJson) as unknown[];
                if (p.length > 0) (entryFromParts as Record<string, unknown>).params = p;
            } catch {
                // ignore
            }
            (funcList as unknown[]).push(entryFromParts);
        }
    } catch {
        const entryFromParts = { name: functionName, result: '<replace with expected return value>' };
        try {
            const p = JSON.parse(paramsJson) as unknown[];
            if (p.length > 0) (entryFromParts as Record<string, unknown>).params = p;
        } catch {
            // ignore
        }
        (funcList as unknown[]).push(entryFromParts);
    }
    const newContent = yaml.dump(doc, { lineWidth: -1, noRefs: true });
    await vscode.workspace.fs.writeFile(testFileUri, new TextEncoder().encode(newContent));
    outputChannel.appendLine(`Added mock for @.${functionName} to test file (setup.mocks) at ${testFileUri.fsPath}`);
    const editor = await vscode.window.showTextDocument(testFileUri, { preview: false });
    const targetLine = findLastLineContaining(newContent, functionName);
    if (targetLine >= 0) {
        const position = new vscode.Position(targetLine, 0);
        editor.selection = new vscode.Selection(position, position);
        editor.revealRange(new vscode.Range(position, position), vscode.TextEditorRevealType.InCenter);
    }
}

/**
 * Add the suggested mock entry to the mock file (relative to the test file's directory).
 * Resolves mockFileName relative to the directory of testFileUri. Creates the file if missing.
 */
export async function addMockToFile(
    testFileUri: vscode.Uri,
    mockFileName: string,
    functionName: string,
    paramsJson: string,
    yamlSnippet: string,
    outputChannel: vscode.OutputChannel
): Promise<void> {
    const testDir = path.dirname(testFileUri.fsPath);
    const mockPath = path.resolve(testDir, mockFileName);
    const mockUri = vscode.Uri.file(mockPath);

    let doc: Record<string, unknown> = {};
    try {
        const raw = await vscode.workspace.fs.readFile(mockUri);
        const content = new TextDecoder().decode(raw);
        if (content.trim()) {
            doc = (yaml.load(content) as Record<string, unknown>) || {};
        }
    } catch {
        // File does not exist or not readable; start with empty doc
    }

    let funcList = doc.func;
    if (!Array.isArray(funcList)) {
        funcList = [];
    }
    try {
        const newEntry = yaml.load(yamlSnippet) as unknown;
        const entry = Array.isArray(newEntry) ? newEntry[0] : newEntry;
        if (entry && typeof entry === 'object') {
            (funcList as unknown[]).push(entry);
        } else {
            const entryFromParts = { name: functionName, result: '<replace with expected return value>' };
            try {
                const p = JSON.parse(paramsJson) as unknown[];
                if (p.length > 0) (entryFromParts as Record<string, unknown>).params = p;
            } catch {
                // ignore
            }
            (funcList as unknown[]).push(entryFromParts);
        }
    } catch {
        const entryFromParts = { name: functionName, result: '<replace with expected return value>' };
        try {
            const p = JSON.parse(paramsJson) as unknown[];
            if (p.length > 0) (entryFromParts as Record<string, unknown>).params = p;
        } catch {
            // ignore
        }
        (funcList as unknown[]).push(entryFromParts);
    }

    doc.func = funcList;
    const newContent = yaml.dump(doc, { lineWidth: -1, noRefs: true });
    await vscode.workspace.fs.writeFile(mockUri, new TextEncoder().encode(newContent));
    outputChannel.appendLine(`Added mock for @.${functionName} to ${mockPath}`);
    const docOpened = await vscode.workspace.openTextDocument(mockUri);
    const editor = await vscode.window.showTextDocument(docOpened, { preview: false });
    const targetLine = findLastLineContaining(newContent, functionName);
    if (targetLine >= 0) {
        const position = new vscode.Position(targetLine, 0);
        editor.selection = new vscode.Selection(position, position);
        editor.revealRange(new vscode.Range(position, position), vscode.TextEditorRevealType.InCenter);
    }
}

/**
 * Check if a file path is under tests/ (e.g. tests/foo.isl or src/tests/bar.isl)
 */
export function isTestFile(uri: vscode.Uri, workspaceFolder: vscode.WorkspaceFolder): boolean {
    const rel = path.relative(workspaceFolder.uri.fsPath, uri.fsPath);
    const parts = rel.split(path.sep);
    return parts.some(p => p === 'tests');
}

/**
 * ISL Test Explorer - discovers @test and @setup in tests folder and registers with VS Code Test API.
 */
/** Debounce delay for document change handlers (ms) */
const DOCUMENT_CHANGE_DEBOUNCE_MS = 1500;

/** Diagnostic collection for YAML test assertion diffs (squiggles in expected section). */
const YAML_TEST_DIAGNOSTIC_SOURCE = 'isl-yaml-test';

export class IslTestController {
    private readonly controller: vscode.TestController;
    private readonly watchers: vscode.FileSystemWatcher[] = [];
    private readonly outputChannel: vscode.OutputChannel;
    private readonly extensionPath: string;
    private readonly assertionDiagnostics: vscode.DiagnosticCollection;
    private documentChangeTimeouts = new Map<string, NodeJS.Timeout>();
    private runProfileHandler!: (request: vscode.TestRunRequest, token: vscode.CancellationToken) => Promise<void>;

    constructor(outputChannel: vscode.OutputChannel, extensionPath: string) {
        this.outputChannel = outputChannel;
        this.extensionPath = extensionPath;
        this.assertionDiagnostics = vscode.languages.createDiagnosticCollection(YAML_TEST_DIAGNOSTIC_SOURCE);
        this.controller = vscode.tests.createTestController('isl-tests', 'ISL Tests');

        this.controller.resolveHandler = async (item) => {
            if (!item) {
                await this.discoverAllTestFiles();
                // Parse all files so the Test Explorer is fully populated (no need to expand each node)
                for (const [, fileItem] of this.controller.items) {
                    await this.parseTestsInFile(fileItem);
                }
            } else {
                await this.parseTestsInFile(item);
            }
        };

        // Refresh button - re-discover and re-parse all test files
        this.controller.refreshHandler = async () => {
            this.documentChangeTimeouts.forEach(t => clearTimeout(t));
            this.documentChangeTimeouts.clear();
            this.controller.items.replace([]);
            await this.discoverAllTestFiles();
            for (const [, item] of this.controller.items) {
                await this.parseTestsInFile(item);
            }
        };

        this.runProfileHandler = (request, token) => this.runTests(request, token);
        this.controller.createRunProfile(
            'Run',
            vscode.TestRunProfileKind.Run,
            this.runProfileHandler
        );

        // Watch for test file changes
        this.setupWatchers();
        // Parse open documents
        vscode.workspace.textDocuments.forEach(doc => this.parseTestsInDocument(doc));
        vscode.workspace.onDidOpenTextDocument(doc => this.parseTestsInDocument(doc));
        vscode.workspace.onDidChangeTextDocument(e => this.debouncedParseTestsInDocument(e.document));

        // Populate Test Explorer on load: discover and parse all test files when workspace is ready
        this.scheduleInitialDiscovery();
    }

    /** Run discovery + parse once when workspace has folders, so Test Explorer is populated on project load. */
    private scheduleInitialDiscovery(): void {
        const run = () => {
            if (!vscode.workspace.workspaceFolders?.length) return;
            this.discoverAllTestFiles().then(() => {
                for (const [, fileItem] of this.controller.items) {
                    this.parseTestsInFile(fileItem);
                }
            });
        };
        // Run after a short delay so workspace is ready; also run when folders change (e.g. folder added)
        setTimeout(run, 800);
        vscode.workspace.onDidChangeWorkspaceFolders(() => run());
    }

    private debouncedParseTestsInDocument(doc: vscode.TextDocument): void {
        if (doc.uri.scheme !== 'file') return;
        const isIsl = doc.uri.fsPath.endsWith('.isl');
        const isYamlTests = doc.uri.fsPath.endsWith('.tests.yaml');
        if (!isIsl && !isYamlTests) return;
        if (isIsl) {
            const folder = vscode.workspace.getWorkspaceFolder(doc.uri);
            if (!folder || !isTestFile(doc.uri, folder)) return;
        }
        if (isYamlTests && !yamlHasIslTests(doc.getText())) return;

        const key = doc.uri.toString();
        const existing = this.documentChangeTimeouts.get(key);
        if (existing) clearTimeout(existing);

        const timeout = setTimeout(() => {
            this.documentChangeTimeouts.delete(key);
            this.parseTestsInDocument(doc);
        }, DOCUMENT_CHANGE_DEBOUNCE_MS);
        this.documentChangeTimeouts.set(key, timeout);
    }

    private setupWatchers(): void {
        const folders = vscode.workspace.workspaceFolders;
        if (!folders) return;

        for (const folder of folders) {
            const islPattern = new vscode.RelativePattern(folder, TEST_FILE_GLOB);
            const islWatcher = vscode.workspace.createFileSystemWatcher(islPattern);
            islWatcher.onDidCreate(uri => this.getOrCreateFile(uri));
            islWatcher.onDidChange(uri => this.parseTestsInFile(this.getOrCreateFile(uri)));
            islWatcher.onDidDelete(uri => this.controller.items.delete(uri.toString()));
            this.watchers.push(islWatcher);

            const yamlPattern = new vscode.RelativePattern(folder, YAML_TEST_FILE_GLOB);
            const yamlWatcher = vscode.workspace.createFileSystemWatcher(yamlPattern);
            yamlWatcher.onDidCreate(uri => this.getOrCreateFile(uri));
            yamlWatcher.onDidChange(uri => this.parseTestsInFile(this.getOrCreateFile(uri)));
            yamlWatcher.onDidDelete(uri => this.controller.items.delete(uri.toString()));
            this.watchers.push(yamlWatcher);
        }
    }

    private getOrCreateFile(uri: vscode.Uri): vscode.TestItem {
        const id = uri.toString();
        const existing = this.controller.items.get(id);
        if (existing) return existing;

        const file = this.controller.createTestItem(id, path.basename(uri.fsPath), uri);
        file.canResolveChildren = true;
        this.controller.items.add(file);
        return file;
    }

    private async discoverAllTestFiles(): Promise<void> {
        const folders = vscode.workspace.workspaceFolders;
        if (!folders) return;

        for (const folder of folders) {
            const islPattern = new vscode.RelativePattern(folder, TEST_FILE_GLOB);
            const islFiles = await vscode.workspace.findFiles(islPattern);
            for (const uri of islFiles) {
                this.getOrCreateFile(uri);
            }
            const yamlPattern = new vscode.RelativePattern(folder, YAML_TEST_FILE_GLOB);
            const yamlFiles = await vscode.workspace.findFiles(yamlPattern);
            for (const uri of yamlFiles) {
                let raw: Uint8Array;
                try {
                    raw = await vscode.workspace.fs.readFile(uri);
                } catch {
                    raw = new Uint8Array(0);
                }
                const content = new TextDecoder().decode(raw);
                if (yamlHasIslTests(content)) {
                    this.getOrCreateFile(uri);
                }
            }
        }
    }

    private parseTestsInDocument(doc: vscode.TextDocument): void {
        if (doc.uri.scheme !== 'file') return;
        const isIsl = doc.uri.fsPath.endsWith('.isl');
        const isYamlTests = doc.uri.fsPath.endsWith('.tests.yaml');
        if (!isIsl && !isYamlTests) return;
        if (isIsl) {
            const folder = vscode.workspace.getWorkspaceFolder(doc.uri);
            if (!folder || !isTestFile(doc.uri, folder)) return;
        } else if (isYamlTests && !yamlHasIslTests(doc.getText())) return;

        const file = this.getOrCreateFile(doc.uri);
        this.parseTestsInFile(file, doc.getText());
    }

    private async parseTestsInFile(file: vscode.TestItem, contents?: string): Promise<void> {
        if (!file.uri) return;

        if (contents === undefined) {
            try {
                const raw = await vscode.workspace.fs.readFile(file.uri);
                contents = new TextDecoder().decode(raw);
            } catch {
                return;
            }
        }

        file.children.replace([]);
        const isYaml = file.uri.fsPath.endsWith('.tests.yaml');
        if (isYaml) {
            if (!yamlHasIslTests(contents)) return;
            const yamlTests = parseYamlIslTests(file.uri, contents);
            for (const t of yamlTests) {
                const item = this.controller.createTestItem(t.id, t.label, file.uri);
                item.range = t.range;
                file.children.add(item);
            }
        } else {
            const { tests } = parseIslTests(file.uri, contents);
            for (const t of tests) {
                const item = this.controller.createTestItem(t.id, t.label, file.uri);
                item.range = t.range;
                if (t.group) {
                    item.description = t.group;
                }
                file.children.add(item);
            }
        }
    }

    private async runTests(request: vscode.TestRunRequest, token: vscode.CancellationToken): Promise<void> {
        const run = this.controller.createTestRun(request);
        const testsToRun = this.collectLeafTests(request);

        if (testsToRun.length === 0) {
            run.end();
            return;
        }

        // Resolve file nodes to get children
        for (const test of testsToRun) {
            const parent = test.parent;
            if (parent?.canResolveChildren && parent.children.size === 0) {
                await this.parseTestsInFile(parent);
            }
        }

        const javaPath = await this.findJava();
        if (!javaPath) {
            run.appendOutput('Java not found. Install Java 11+ or configure isl.execution.javaHome.\r\n');
            for (const test of testsToRun) {
                run.started(test);
                run.errored(test, new vscode.TestMessage('Java not found'));
            }
            run.end();
            return;
        }

        const jarPath = path.join(this.extensionPath, 'lib', 'isl-cmd-all.jar');
        if (!fs.existsSync(jarPath)) {
            run.appendOutput('ISL runtime not found (isl-cmd-all.jar). Reinstall the extension.\r\n');
            for (const test of testsToRun) {
                run.started(test);
                run.errored(test, new vscode.TestMessage('ISL runtime not found'));
            }
            run.end();
            return;
        }

        const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
        if (!workspaceFolder) {
            run.appendOutput('No workspace folder open.\r\n');
            for (const test of testsToRun) {
                run.started(test);
                run.errored(test, new vscode.TestMessage('No workspace folder'));
            }
            run.end();
            return;
        }

        // Determine path to run: single file or directory
        const fileUris = new Set<string>();
        for (const t of testsToRun) {
            if (t.uri) fileUris.add(t.uri.toString());
        }
        const filePaths = Array.from(fileUris).map(u => vscode.Uri.parse(u).fsPath);
        const runPath = filePaths.length === 1
            ? filePaths[0]
            : this.getCommonParent(filePaths) ?? workspaceFolder.uri.fsPath;

        const outputFile = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'isl-test-')), 'results.json');
        const env = { ...process.env };
        const config = vscode.workspace.getConfiguration('isl.execution');
        const javaHome = config.get<string>('javaHome', '');
        if (javaHome) {
            env.JAVA_HOME = javaHome;
            env.PATH = `${path.join(javaHome, 'bin')}${path.delimiter}${env.PATH}`;
        }

        const isFile = fs.existsSync(runPath) && fs.statSync(runPath).isFile();
        const searchBase = isFile ? path.dirname(runPath) : runPath;

        // Build function filter for ISL CLI -f/--function (run only selected tests)
        const functionFilters: string[] = [];
        for (const t of testsToRun) {
            const fn = t.id.split('#')[1] ?? t.label;
            if (!t.uri) {
                functionFilters.push(fn);
            } else if (filePaths.length === 1) {
                functionFilters.push(fn);
            } else {
                const relPath = path.relative(searchBase, t.uri.fsPath).replace(/\\/g, '/');
                functionFilters.push(`${relPath}:${fn}`);
            }
        }

        this.outputChannel.clear();
        run.appendOutput(`=== ISL Test Run ===\r\n`);
        run.appendOutput(`Path: ${runPath}\r\n`);
        run.appendOutput(`Mode: ${isFile ? 'single file' : 'directory'}\r\n`);
        run.appendOutput(`Tests to run: ${testsToRun.map(t => t.id.split('#')[1] ?? t.label).join(', ')}\r\n`);
        const cmdLine = functionFilters.length > 0
            ? `java -jar isl-cmd-all.jar test "${runPath}" -o "${outputFile}" --verbose ${functionFilters.map(f => `-f "${f}"`).join(' ')}`
            : `java -jar isl-cmd-all.jar test "${runPath}" -o "${outputFile}" --verbose`;
        run.appendOutput(`Command: ${cmdLine}\r\n`);
        run.appendOutput(`\r\n`);

        let execStdout = '';
        let execStderr = '';
        try {
            const execResult = await this.execJavaTest(
                javaPath,
                jarPath,
                runPath,
                outputFile,
                functionFilters,
                env,
                (chunk) => run.appendOutput(colorizeIslOutput(chunk)),
                workspaceFolder.uri.fsPath
            );
            execStdout = execResult.stdout;
            execStderr = execResult.stderr;
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : String(err);
            run.appendOutput(`Execution failed: ${msg}\r\n`);
            for (const test of testsToRun) {
                run.started(test);
                run.errored(test, new vscode.TestMessage(msg));
            }
            run.end();
            return;
        }

        let results: IslTestResultJson;
        try {
            const raw = fs.readFileSync(outputFile, 'utf-8');
            results = JSON.parse(raw) as IslTestResultJson;
        } catch {
            run.appendOutput('Failed to parse test results JSON.\r\n');
            // Always show runner output so user can see what isl-cmd actually printed
            if (execStdout) {
                run.appendOutput(`--- Runner stdout ---\r\n`);
                run.appendOutput(execStdout.replace(/\n/g, '\r\n') + '\r\n');
            }
            if (execStderr) {
                run.appendOutput(`--- Runner stderr ---\r\n`);
                run.appendOutput(execStderr.replace(/\n/g, '\r\n') + '\r\n');
            }
            try {
                const rawContent = fs.readFileSync(outputFile, 'utf-8');
                run.appendOutput(`--- Output file (results.json) ---\r\n`);
                run.appendOutput(rawContent.length ? rawContent.replace(/\n/g, '\r\n') + '\r\n' : '(empty)\r\n');
            } catch {
                run.appendOutput(`--- Output file could not be read ---\r\n`);
            }
            for (const test of testsToRun) {
                run.started(test);
                run.errored(test, new vscode.TestMessage('Failed to parse results'));
            }
            run.end();
            return;
        } finally {
            try { fs.unlinkSync(outputFile); } catch { /* ignore */ }
            try { fs.rmdirSync(path.dirname(outputFile)); } catch { /* ignore */ }
        }

        run.appendOutput(`--- Parsed results: ${results.results.length} test(s) ---\r\n`);
        for (const tr of results.results) {
            run.appendOutput(`  ${tr.testFile} :: ${tr.functionName} => ${tr.success ? 'PASS' : 'FAIL'}\r\n`);
        }
        run.appendOutput(`\r\n`);

        const testById = new Map<string, vscode.TestItem>();
        for (const t of testsToRun) {
            testById.set(t.id, t);
        }

        const matched = new Set<vscode.TestItem>();
        const uriToAssertDiags = new Map<string, vscode.Diagnostic[]>();

        for (const t of testsToRun) {
            if (t.uri?.fsPath.endsWith('.tests.yaml')) {
                this.assertionDiagnostics.delete(t.uri);
            }
        }

        for (const tr of results.results) {
            const test = this.findTestById(testById, tr);
            if (!test) continue;

            matched.add(test);
            run.started(test);
            const duration = 0;

            if (tr.success) {
                run.passed(test, duration);
            } else {
                const rawMessage = tr.message ?? 'Test failed';
                const mockSuggestion = parseMockSuggestionFromError(tr.message ?? null);
                const displayName = (tr.testName && tr.testName !== tr.functionName)
                    ? `${tr.testName} (${tr.functionName})`
                    : (tr.testName || tr.functionName);
                const msg = buildTestFailureMessage(rawMessage, test.uri, mockSuggestion, displayName);
                if (tr.errorPosition) {
                    const fallbackUri = test.uri ?? workspaceFolder.uri;
                    const loc = this.resolveErrorLocation(tr.errorPosition, searchBase, workspaceFolder.uri.fsPath, fallbackUri);
                    if (loc) msg.location = loc;
                }
                run.failed(test, msg, duration);

                if (test.uri?.fsPath.endsWith('.tests.yaml')) {
                    const diffs = parseAssertDiffsFromMessage(tr.message ?? null);
                    if (diffs.length > 0) {
                        try {
                            const doc = await vscode.workspace.openTextDocument(test.uri);
                            const content = doc.getText();
                            const fn = tr.functionName;
                            const existing = uriToAssertDiags.get(test.uri.toString()) ?? [];
                            for (const d of diffs) {
                                const ranges = findExpectedKeyRangesInYaml(content, fn, d.key);
                                const message = `Expected: ${d.expected}, Actual: ${d.actual}`;
                                for (const r of ranges) {
                                    existing.push(new vscode.Diagnostic(r, message, vscode.DiagnosticSeverity.Error));
                                }
                            }
                            uriToAssertDiags.set(test.uri.toString(), existing);
                        } catch {
                            // ignore (e.g. file no longer open)
                        }
                    }
                }
            }
        }

        for (const [uriStr, diags] of uriToAssertDiags) {
            this.assertionDiagnostics.set(vscode.Uri.parse(uriStr), diags);
        }

        for (const test of testsToRun) {
            if (!matched.has(test)) {
                const fn = test.id.split('#')[1] ?? test.label;
                const file = test.uri ? path.basename(test.uri.fsPath) : '?';
                run.started(test);
                run.errored(
                    test,
                    new vscode.TestMessage(`Test not found in results. Looking for: file="${file}", function="${fn}". Check that the CLI discovered this test (see parsed results above).`)
                );
                run.appendOutput(`  [UNMATCHED] ${file} :: ${fn} (id: ${test.id})\r\n`);
            }
        }

        run.appendOutput(`\r\n=== Summary ===\r\n`);
        run.appendOutput(`Results: ${results.passed} passed, ${results.failed} failed, ${results.total} total\r\n`);
        run.end();
    }

    private collectLeafTests(request: vscode.TestRunRequest): vscode.TestItem[] {
        const leaves: vscode.TestItem[] = [];
        const queue: vscode.TestItem[] = request.include
            ? Array.from(request.include)
            : Array.from(this.controller.items).map(([, item]) => item);

        while (queue.length > 0) {
            const item = queue.shift()!;
            if (request.exclude?.includes(item)) continue;
            if (item.children.size > 0) {
                item.children.forEach((child) => queue.push(child));
            } else {
                leaves.push(item);
            }
        }
        return leaves;
    }

    private getCommonParent(paths: string[]): string | null {
        if (paths.length === 0) return null;
        let common = path.dirname(paths[0]);
        for (let i = 1; i < paths.length; i++) {
            const dir = path.dirname(paths[i]);
            while (!dir.startsWith(common) && common !== path.dirname(common)) {
                common = path.dirname(common);
            }
        }
        return common;
    }

    /**
     * Resolve errorPosition from the CLI to a VS Code Location so the test failure "arrow" opens the correct file/line.
     * errorPosition.file is relative to the test run cwd (searchBase). Falls back to testUri if resolution fails.
     */
    private resolveErrorLocation(
        errorPosition: NonNullable<IslTestResultJson['results'][0]['errorPosition']>,
        searchBase: string,
        workspaceRoot: string,
        testUri: vscode.Uri
    ): vscode.Location | null {
        const line = Math.max(0, (errorPosition.line ?? 1) - 1);
        const column = Math.max(0, (errorPosition.column ?? 1) - 1);
        const file = errorPosition.file?.trim();
        if (!file) {
            return new vscode.Location(testUri, new vscode.Position(line, column));
        }
        const normalized = file.replace(/\//g, path.sep);
        let resolved = path.resolve(searchBase, normalized);
        if (!fs.existsSync(resolved)) {
            resolved = path.resolve(workspaceRoot, normalized);
        }
        if (!fs.existsSync(resolved)) {
            return new vscode.Location(testUri, new vscode.Position(line, column));
        }
        const uri = vscode.Uri.file(resolved);
        const endLine = errorPosition.endLine != null && errorPosition.endLine > 0
            ? Math.max(0, errorPosition.endLine - 1)
            : line;
        const endColumn = errorPosition.endColumn != null && errorPosition.endColumn > 0
            ? Math.max(0, errorPosition.endColumn - 1)
            : column;
        const range = (endLine !== line || endColumn !== column)
            ? new vscode.Range(line, column, endLine, endColumn)
            : new vscode.Range(line, column, line, column);
        return new vscode.Location(uri, range);
    }

    private findTestById(testById: Map<string, vscode.TestItem>, tr: IslTestResultJson['results'][0]): vscode.TestItem | undefined {
        const fnLower = tr.functionName.toLowerCase();
        const resultFile = tr.testFile.toLowerCase().replace(/\\/g, '/');
        const resultFileBase = path.basename(resultFile);

        for (const [id, test] of testById) {
            // Id format: uri#functionName or uri#functionName#index
            const idFn = id.split('#')[1] ?? '';
            if (idFn.toLowerCase() !== fnLower) continue;

            if (!test.uri) return test;

            const testBase = path.basename(test.uri.fsPath).toLowerCase();
            const testPath = test.uri.fsPath.toLowerCase().replace(/\\/g, '/');

            // Match: exact path, exact filename, or result path ends with our filename (or vice versa)
            if (
                resultFile === testPath ||
                resultFile === testBase ||
                resultFileBase === testBase ||
                resultFile.endsWith('/' + testBase) ||
                resultFile.endsWith('\\' + testBase) ||
                testPath.endsWith(resultFile) ||
                testPath.endsWith(resultFile.replace(/\//g, path.sep))
            ) {
                return test;
            }
        }
        return undefined;
    }

    private execJavaTest(
        javaPath: string,
        jarPath: string,
        runPath: string,
        outputFile: string,
        functionFilters: string[],
        env: NodeJS.ProcessEnv,
        onOutput: (chunk: string, isStderr: boolean) => void,
        pathPrefixToStrip?: string
    ): Promise<{ stdout: string; stderr: string }> {
        return new Promise((resolve, reject) => {
            const args = ['-jar', jarPath, 'test', runPath, '-o', outputFile, '--verbose'];
            for (const f of functionFilters) {
                args.push('-f', f);
            }
            const cwd = fs.existsSync(runPath) && fs.statSync(runPath).isFile()
                ? path.dirname(runPath)
                : runPath;
            const stdoutChunks: string[] = [];
            const stderrChunks: string[] = [];

            // Build a regex that matches the workspace prefix (with either slash type) so we can
            // shorten absolute paths in the runner output to relative ones (e.g. .\tests\foo.isl).
            let prefixPattern: RegExp | null = null;
            const relativePrefix = '.' + path.sep;
            if (pathPrefixToStrip) {
                const escapedParts = pathPrefixToStrip.split(/[/\\]/).map(p => escapeRegex(p));
                const prefixRe = escapedParts.join('[/\\\\]') + '[/\\\\]';
                prefixPattern = new RegExp(prefixRe, process.platform === 'win32' ? 'gi' : 'g');
            }

            // Strip paths from a complete (line-aligned) chunk and forward to consumers.
            const stripAndSend = (text: string, isStderr: boolean) => {
                const stripped = prefixPattern ? text.replace(prefixPattern, relativePrefix) : text;
                const normalized = stripped.replace(/\r?\n/g, '\r\n');
                onOutput(normalized, isStderr);
                this.outputChannel.append(normalized);
            };

            // Buffer raw input so we only strip on complete lines (chunks may split mid-path).
            let lineBuffer = '';
            const append = (data: Buffer | string, isStderr: boolean) => {
                const s = data?.toString() ?? '';
                if (!s) return;
                // Keep raw chunks for error-fallback reporting (unmodified).
                if (isStderr) stderrChunks.push(s);
                else stdoutChunks.push(s);
                lineBuffer += s;
                const lastNl = lineBuffer.lastIndexOf('\n');
                if (lastNl >= 0) {
                    const complete = lineBuffer.slice(0, lastNl + 1);
                    lineBuffer = lineBuffer.slice(lastNl + 1);
                    stripAndSend(complete, isStderr);
                }
            };

            const child = cp.spawn(javaPath, args, { env, cwd });
            child.stdout?.on('data', (d) => append(d, false));
            child.stderr?.on('data', (d) => append(d, true));
            child.on('error', (e) => reject(e));
            child.on('close', (code, signal) => {
                // Flush any partial line that didn't end with a newline.
                if (lineBuffer) {
                    stripAndSend(lineBuffer, false);
                    lineBuffer = '';
                }
                const stdoutStr = stdoutChunks.join('');
                const stderrStr = stderrChunks.join('');
                if (code !== 0 && code !== 1) {
                    reject(new Error(`Process exited with code ${code}${signal ? `, signal ${signal}` : ''}`));
                } else {
                    resolve({ stdout: stdoutStr, stderr: stderrStr });
                }
            });
        });
    }

    private async findJava(): Promise<string | null> {
        const config = vscode.workspace.getConfiguration('isl.execution');
        const javaHome = config.get<string>('javaHome', '');
        if (javaHome) {
            const p = path.join(javaHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
            if (fs.existsSync(p)) return p;
        }
        const envHome = process.env.JAVA_HOME;
        if (envHome) {
            const p = path.join(envHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
            if (fs.existsSync(p)) return p;
        }
        return process.platform === 'win32' ? 'java.exe' : 'java';
    }

    /**
     * Run all tests in the given file (used by "Run all Tests in file" command).
     * Supports both tests-folder .isl files and *.tests.yaml files that have islTests.
     */
    async runTestsInFile(uri: vscode.Uri): Promise<void> {
        const folder = vscode.workspace.getWorkspaceFolder(uri);
        const isIsl = uri.fsPath.endsWith('.isl');
        const isYamlTests = uri.fsPath.endsWith('.tests.yaml');
        if (!isIsl && !isYamlTests) return;
        if (isIsl && (!folder || !isTestFile(uri, folder))) return;
        if (isYamlTests) {
            try {
                const raw = await vscode.workspace.fs.readFile(uri);
                const content = new TextDecoder().decode(raw);
                if (!yamlHasIslTests(content)) return;
            } catch {
                return;
            }
        }

        const fileItem = this.getOrCreateFile(uri);
        await this.parseTestsInFile(fileItem);
        if (fileItem.children.size === 0) return;

        const tokenSource = new vscode.CancellationTokenSource();
        const request: vscode.TestRunRequest = {
            include: [fileItem],
            exclude: undefined,
            profile: undefined as unknown as vscode.TestRunProfile,
            preserveFocus: false
        };
        await this.runProfileHandler(request, tokenSource.token);
    }

    dispose(): void {
        this.documentChangeTimeouts.forEach(t => clearTimeout(t));
        this.documentChangeTimeouts.clear();
        this.watchers.forEach(w => w.dispose());
        this.assertionDiagnostics.dispose();
        this.controller.dispose();
    }
}
