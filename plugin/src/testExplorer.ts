import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import * as os from 'os';
import * as cp from 'child_process';

/** Test file pattern: tests folder anywhere in workspace; applies to files from that folder down (e.g. tests/, src/tests/, a/b/tests/) */
const TEST_FILE_GLOB = '**/tests/**/*.isl';

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
        errorPosition: { file: string; line: number; column: number } | null;
    }>;
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

export class IslTestController {
    private readonly controller: vscode.TestController;
    private readonly watchers: vscode.FileSystemWatcher[] = [];
    private readonly outputChannel: vscode.OutputChannel;
    private readonly extensionPath: string;
    private documentChangeTimeouts = new Map<string, NodeJS.Timeout>();

    constructor(outputChannel: vscode.OutputChannel, extensionPath: string) {
        this.outputChannel = outputChannel;
        this.extensionPath = extensionPath;
        this.controller = vscode.tests.createTestController('isl-tests', 'ISL Tests');

        this.controller.resolveHandler = async (item) => {
            if (!item) {
                await this.discoverAllTestFiles();
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

        // Run profile - placeholder; user will define execution later
        this.controller.createRunProfile(
            'Run',
            vscode.TestRunProfileKind.Run,
            (request, token) => this.runTests(request, token)
        );

        // Watch for test file changes
        this.setupWatchers();
        // Parse open documents
        vscode.workspace.textDocuments.forEach(doc => this.parseTestsInDocument(doc));
        vscode.workspace.onDidOpenTextDocument(doc => this.parseTestsInDocument(doc));
        vscode.workspace.onDidChangeTextDocument(e => this.debouncedParseTestsInDocument(e.document));
    }

    private debouncedParseTestsInDocument(doc: vscode.TextDocument): void {
        if (doc.uri.scheme !== 'file' || !doc.uri.fsPath.endsWith('.isl')) return;
        const folder = vscode.workspace.getWorkspaceFolder(doc.uri);
        if (!folder || !isTestFile(doc.uri, folder)) return;

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
            const pattern = new vscode.RelativePattern(folder, TEST_FILE_GLOB);
            const watcher = vscode.workspace.createFileSystemWatcher(pattern);

            watcher.onDidCreate(uri => this.getOrCreateFile(uri));
            watcher.onDidChange(uri => this.parseTestsInFile(this.getOrCreateFile(uri)));
            watcher.onDidDelete(uri => this.controller.items.delete(uri.toString()));

            this.watchers.push(watcher);
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
            const pattern = new vscode.RelativePattern(folder, TEST_FILE_GLOB);
            const files = await vscode.workspace.findFiles(pattern);
            for (const uri of files) {
                this.getOrCreateFile(uri);
            }
        }
    }

    private parseTestsInDocument(doc: vscode.TextDocument): void {
        if (doc.uri.scheme !== 'file' || !doc.uri.fsPath.endsWith('.isl')) return;

        const folder = vscode.workspace.getWorkspaceFolder(doc.uri);
        if (!folder || !isTestFile(doc.uri, folder)) return;

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

        run.appendOutput(`=== ISL Test Run ===\r\n`);
        run.appendOutput(`Path: ${runPath}\r\n`);
        run.appendOutput(`Mode: ${isFile ? 'single file' : 'directory'}\r\n`);
        run.appendOutput(`Tests to run: ${testsToRun.map(t => t.id.split('#')[1] ?? t.label).join(', ')}\r\n`);
        const cmdLine = functionFilters.length > 0
            ? `java -jar isl-cmd-all.jar test "${runPath}" -o "${outputFile}" ${functionFilters.map(f => `-f "${f}"`).join(' ')}`
            : `java -jar isl-cmd-all.jar test "${runPath}" -o "${outputFile}"`;
        run.appendOutput(`Command: ${cmdLine}\r\n`);
        run.appendOutput(`\r\n`);

        let execStdout = '';
        let execStderr = '';
        try {
            const execResult = await this.execJavaTest(javaPath, jarPath, runPath, outputFile, functionFilters, env);
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

        if (execStdout) {
            run.appendOutput(`--- CLI output ---\r\n`);
            run.appendOutput(execStdout.replace(/\n/g, '\r\n') + '\r\n');
        }
        if (execStderr) {
            run.appendOutput(`--- CLI stderr ---\r\n`);
            run.appendOutput(execStderr.replace(/\n/g, '\r\n') + '\r\n');
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

        for (const tr of results.results) {
            const test = this.findTestById(testById, tr);
            if (!test) continue;

            matched.add(test);
            run.started(test);
            const duration = 0;

            if (tr.success) {
                run.passed(test, duration);
            } else {
                const msg = new vscode.TestMessage(tr.message ?? 'Test failed');
                if (tr.errorPosition && test.uri) {
                    const pos = new vscode.Position(
                        Math.max(0, (tr.errorPosition.line ?? 1) - 1),
                        Math.max(0, (tr.errorPosition.column ?? 1) - 1)
                    );
                    msg.location = new vscode.Location(test.uri, pos);
                }
                run.failed(test, msg, duration);
            }
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

    private findTestById(testById: Map<string, vscode.TestItem>, tr: IslTestResultJson['results'][0]): vscode.TestItem | undefined {
        const fnLower = tr.functionName.toLowerCase();
        const resultFile = tr.testFile.toLowerCase().replace(/\\/g, '/');
        const resultFileBase = path.basename(resultFile);

        for (const [id, test] of testById) {
            // Id format: uri#functionName or uri#functionName#line
            const idFn = id.split('#')[1] ?? '';
            if (idFn.toLowerCase() !== fnLower) continue;

            if (!test.uri) return test;

            const testBase = path.basename(test.uri.fsPath).toLowerCase();
            const testPath = test.uri.fsPath.toLowerCase().replace(/\\/g, '/');

            // Match: exact filename, or result path ends with our filename, or our path ends with result path
            if (
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

    private execJavaTest(javaPath: string, jarPath: string, runPath: string, outputFile: string, functionFilters: string[], env: NodeJS.ProcessEnv): Promise<{ stdout: string; stderr: string }> {
        return new Promise((resolve, reject) => {
            const args = ['-jar', jarPath, 'test', runPath, '-o', outputFile];
            for (const f of functionFilters) {
                args.push('-f', f);
            }
            const cwd = fs.existsSync(runPath) && fs.statSync(runPath).isFile()
                ? path.dirname(runPath)
                : runPath;
            cp.execFile(javaPath, args, { env, cwd }, (err, stdout, stderr) => {
                const stdoutStr = stdout?.toString() ?? '';
                const stderrStr = stderr?.toString() ?? '';
                if (stdoutStr) this.outputChannel.appendLine(stdoutStr);
                if (stderrStr) this.outputChannel.appendLine(stderrStr);
                if (err && err.code !== 1) {
                    reject(err);
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

    dispose(): void {
        this.documentChangeTimeouts.forEach(t => clearTimeout(t));
        this.documentChangeTimeouts.clear();
        this.watchers.forEach(w => w.dispose());
        this.controller.dispose();
    }
}
