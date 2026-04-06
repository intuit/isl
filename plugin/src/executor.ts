import * as vscode from 'vscode';
import * as cp from 'child_process';
import * as path from 'path';
import * as fs from 'fs';
import type { IslCoverageDecorationManager } from './coverageDecorations';
import { islCoverageUiEnabled } from './coverageDecorations';

const EMBEDDED_JAR_NAME = 'isl-cmd-all.jar';

export class IslExecutor {
    private outputChannel: vscode.OutputChannel;
    private readonly extensionPath: string;
    private readonly coverageDecorations: IslCoverageDecorationManager | undefined;

    constructor(extensionPath: string, coverageDecorations?: IslCoverageDecorationManager) {
        this.extensionPath = extensionPath;
        this.coverageDecorations = coverageDecorations;
        this.outputChannel = vscode.window.createOutputChannel('ISL');
    }

    private getEmbeddedJarPath(): string | null {
        const jarPath = path.join(this.extensionPath, 'lib', EMBEDDED_JAR_NAME);
        return fs.existsSync(jarPath) ? jarPath : null;
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

    public async run(document: vscode.TextDocument): Promise<void> {
        // Prompt for input data
        const inputJson = await vscode.window.showInputBox({
            prompt: 'Enter input JSON (or leave empty for empty object)',
            placeHolder: '{"key": "value"}',
            value: '{}'
        });

        if (inputJson === undefined) {
            return; // User cancelled
        }

        await this.executeIsl(document, inputJson);
    }

    /**
     * Compile (validate) the current ISL file using the embedded isl-cmd-all.jar or external CLI.
     * Calls the CLI with "validate" and only the ISL file path (no input file).
     */
    public async compile(document: vscode.TextDocument): Promise<void> {
        if (document.uri.scheme !== 'file') {
            vscode.window.showWarningMessage('Save the document to a file first to compile.');
            return;
        }
        if (document.isDirty) {
            await document.save();
        }

        this.outputChannel.clear();
        this.outputChannel.show();
        this.outputChannel.appendLine('=== ISL Compile (validate) ===');
        this.outputChannel.appendLine(`File: ${document.fileName}`);
        this.outputChannel.appendLine('');

        let useEmbeddedJar = false;
        try {
            const config = await this.buildValidateConfig(document.fileName);
            useEmbeddedJar = config.useEmbeddedJar;
            this.outputChannel.appendLine(`Command: ${config.command} ${config.args.join(' ')}`);
            this.outputChannel.appendLine('');

            const result = await this.execPromise(config.command, config.args, {
                env: config.env,
                cwd: path.dirname(document.fileName)
            });
            this.outputChannel.appendLine(result.stdout);
            if (result.stderr) {
                this.outputChannel.appendLine(result.stderr);
            }
            vscode.window.showInformationMessage('ISL compile succeeded.');
        } catch (error: any) {
            this.outputChannel.appendLine('=== Compile Failed ===');
            this.outputChannel.appendLine(error.message);
            if (error.stdout) this.outputChannel.appendLine(error.stdout);
            if (error.stderr) this.outputChannel.appendLine(error.stderr);
            this.handleExecutionError(error, useEmbeddedJar);
        }
    }

    public async runWithInput(document: vscode.TextDocument): Promise<void> {
        // Prompt for input file
        const files = await vscode.window.showOpenDialog({
            canSelectMany: false,
            filters: { 'JSON Files': ['json'] },
            openLabel: 'Select Input JSON File'
        });

        if (!files || files.length === 0) {
            return; // User cancelled
        }

        const inputFile = files[0].fsPath;
        
        try {
            const inputJson = fs.readFileSync(inputFile, 'utf8');
            await this.executeIsl(document, inputJson);
        } catch (error) {
            vscode.window.showErrorMessage(`Failed to read input file: ${error}`);
        }
    }

    /**
     * Build command, args, and env for running ISL validate (compile only, no input file).
     * Prefers embedded lib/isl-cmd-all.jar (java -jar ...); otherwise uses isl.execution.islCommand or workspace isl.bat/isl.sh.
     */
    private async buildValidateConfig(scriptPath: string): Promise<{ command: string; args: string[]; env: NodeJS.ProcessEnv; useEmbeddedJar: boolean }> {
        const env = { ...process.env };
        const config = vscode.workspace.getConfiguration('isl.execution');
        const javaHome = config.get<string>('javaHome', '');
        if (javaHome) {
            env.JAVA_HOME = javaHome;
            env.PATH = `${path.join(javaHome, 'bin')}${path.delimiter}${env.PATH}`;
        }

        const jarPath = this.getEmbeddedJarPath();
        if (jarPath) {
            const javaPath = await this.findJava();
            if (!javaPath) {
                throw new Error('Java not found. Set isl.execution.javaHome or JAVA_HOME.');
            }
            return {
                command: javaPath,
                args: ['-jar', jarPath, 'validate', scriptPath],
                env,
                useEmbeddedJar: true
            };
        }

        const islCommand = config.get<string>('islCommand', '') || (() => {
            const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
            if (workspaceFolder) {
                const islBat = path.join(workspaceFolder.uri.fsPath, 'isl.bat');
                const islSh = path.join(workspaceFolder.uri.fsPath, 'isl.sh');
                if (process.platform === 'win32' && fs.existsSync(islBat)) return islBat;
                if (fs.existsSync(islSh)) return islSh;
            }
            return 'isl';
        })();
        return {
            command: islCommand,
            args: ['validate', scriptPath],
            env,
            useEmbeddedJar: false
        };
    }

    /**
     * Build command, args, and env for running ISL transform (run with input).
     * Prefers embedded lib/isl-cmd-all.jar (java -jar ...); otherwise uses isl.execution.islCommand or workspace isl.bat/isl.sh.
     */
    private async buildRunConfig(
        scriptPath: string,
        inputFilePath: string,
        coverageReportPath?: string
    ): Promise<{ command: string; args: string[]; env: NodeJS.ProcessEnv; useEmbeddedJar: boolean }> {
        const env = { ...process.env };
        const config = vscode.workspace.getConfiguration('isl.execution');
        const javaHome = config.get<string>('javaHome', '');
        if (javaHome) {
            env.JAVA_HOME = javaHome;
            env.PATH = `${path.join(javaHome, 'bin')}${path.delimiter}${env.PATH}`;
        }

        const jarPath = this.getEmbeddedJarPath();
        if (jarPath) {
            const javaPath = await this.findJava();
            if (!javaPath) {
                throw new Error('Java not found. Set isl.execution.javaHome or JAVA_HOME.');
            }
            const args = ['-jar', jarPath, 'transform', scriptPath, '-i', inputFilePath];
            if (coverageReportPath) {
                args.push('--coverage-report', coverageReportPath);
            }
            return {
                command: javaPath,
                args,
                env,
                useEmbeddedJar: true
            };
        }

        const islCommand = config.get<string>('islCommand', '') || (() => {
            const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
            if (workspaceFolder) {
                const islBat = path.join(workspaceFolder.uri.fsPath, 'isl.bat');
                const islSh = path.join(workspaceFolder.uri.fsPath, 'isl.sh');
                if (process.platform === 'win32' && fs.existsSync(islBat)) return islBat;
                if (fs.existsSync(islSh)) return islSh;
            }
            return 'isl';
        })();
        const args = ['transform', scriptPath, '-i', inputFilePath];
        if (coverageReportPath) {
            args.push('--coverage-report', coverageReportPath);
        }
        return {
            command: islCommand,
            args,
            env,
            useEmbeddedJar: false
        };
    }

    private handleExecutionError(error: any, useEmbeddedJar: boolean): void {
        const isNotFound = error.code === 'ENOENT' || (error.message && String(error.message).includes('ENOENT'));
        if (isNotFound) {
            this.outputChannel.appendLine('');
            if (useEmbeddedJar) {
                this.outputChannel.appendLine('Java was not found. Set isl.execution.javaHome to your Java installation, or ensure Java is on your PATH.');
                vscode.window.showErrorMessage(
                    'Java not found. Set isl.execution.javaHome or add Java to PATH. See Output for details.',
                    'Open Settings'
                ).then(choice => {
                    if (choice === 'Open Settings') {
                        vscode.commands.executeCommand('workbench.action.openSettings', 'isl.execution.javaHome');
                    }
                });
            } else {
                this.outputChannel.appendLine('Embedded CLI (lib/isl-cmd-all.jar) not found. Set isl.execution.islCommand to an external ISL executable (isl.bat or isl.sh), or reinstall the extension.');
                vscode.window.showErrorMessage(
                    'ISL CLI not found. Set isl.execution.islCommand to isl.bat/isl.sh, or reinstall the extension. See Output for details.',
                    'Open Settings'
                ).then(choice => {
                    if (choice === 'Open Settings') {
                        vscode.commands.executeCommand('workbench.action.openSettings', 'isl.execution.islCommand');
                    }
                });
            }
        } else {
            vscode.window.showErrorMessage('ISL execution failed. Check output for details.');
        }
    }

    private async executeIsl(document: vscode.TextDocument, inputJson: string): Promise<void> {
        if (document.isDirty) {
            await document.save();
        }

        const tempDir = path.join(path.dirname(document.fileName), '.isl-temp');
        if (!fs.existsSync(tempDir)) {
            fs.mkdirSync(tempDir, { recursive: true });
        }
        const tempInputFile = path.join(tempDir, 'input.json');
        fs.writeFileSync(tempInputFile, inputJson);

        const coverageCfg = vscode.workspace.getConfiguration('isl.coverage');
        const showCoverage =
            !!this.coverageDecorations &&
            islCoverageUiEnabled() &&
            coverageCfg.get<boolean>('showAfterRun', true);
        const tempCoverageFile = showCoverage
            ? path.join(tempDir, `coverage-${Date.now()}.json`)
            : undefined;

        this.outputChannel.clear();
        this.outputChannel.show();
        this.outputChannel.appendLine('=== ISL Execution ===');
        this.outputChannel.appendLine(`Script: ${document.fileName}`);
        this.outputChannel.appendLine(`Input: ${inputJson}`);
        this.outputChannel.appendLine('');

        let useEmbeddedJar = false;
        try {
            const runConfig = await this.buildRunConfig(
                document.fileName,
                tempInputFile,
                tempCoverageFile
            );
            useEmbeddedJar = runConfig.useEmbeddedJar;
            this.outputChannel.appendLine(`Command: ${runConfig.command} ${runConfig.args.join(' ')}`);
            this.outputChannel.appendLine('');

            const result = await this.execPromise(runConfig.command, runConfig.args, { env: runConfig.env, cwd: path.dirname(document.fileName) });

            this.outputChannel.appendLine('=== Output ===');
            this.outputChannel.appendLine(result.stdout);

            if (result.stderr) {
                this.outputChannel.appendLine('');
                this.outputChannel.appendLine('=== Errors/Warnings ===');
                this.outputChannel.appendLine(result.stderr);
            }

            // Try to parse and format output JSON
            try {
                const output = JSON.parse(result.stdout);
                const formatted = JSON.stringify(output, null, 2);
                
                // Show formatted output in new editor
                const doc = await vscode.workspace.openTextDocument({
                    content: formatted,
                    language: 'json'
                });
                await vscode.window.showTextDocument(doc, { preview: false, viewColumn: vscode.ViewColumn.Beside });
            } catch (e) {
                // Output is not valid JSON, just show it as is
            }

            if (showCoverage && tempCoverageFile && fs.existsSync(tempCoverageFile)) {
                this.coverageDecorations!.applyReportToOpenIslDocuments(tempCoverageFile);
                const covLine = this.coverageDecorations.formatLastCoverageVerification();
                if (covLine) {
                    this.outputChannel.appendLine('');
                    this.outputChannel.appendLine(covLine);
                }
            }

        } catch (error: any) {
            this.outputChannel.appendLine('=== Execution Failed ===');
            this.outputChannel.appendLine(error.message);
            if (error.stdout) {
                this.outputChannel.appendLine('');
                this.outputChannel.appendLine('=== Stdout ===');
                this.outputChannel.appendLine(error.stdout);
            }
            if (error.stderr) {
                this.outputChannel.appendLine('');
                this.outputChannel.appendLine('=== Stderr ===');
                this.outputChannel.appendLine(error.stderr);
            }
            this.handleExecutionError(error, useEmbeddedJar);
        } finally {
            try {
                fs.unlinkSync(tempInputFile);
                if (tempCoverageFile && fs.existsSync(tempCoverageFile)) {
                    fs.unlinkSync(tempCoverageFile);
                }
                fs.rmdirSync(tempDir);
            } catch (e) {
                // Ignore cleanup errors
            }
        }
    }

    private execPromise(command: string, args: string[], options: cp.ExecFileOptions): Promise<{ stdout: string, stderr: string }> {
        return new Promise((resolve, reject) => {
            cp.execFile(command, args, options, (error, stdout, stderr) => {
                const stdoutStr = stdout ? stdout.toString() : '';
                const stderrStr = stderr ? stderr.toString() : '';
                if (error) {
                    reject({
                        message: error.message,
                        code: (error as NodeJS.ErrnoException).code,
                        stdout: stdoutStr,
                        stderr: stderrStr
                    });
                } else {
                    resolve({ stdout: stdoutStr, stderr: stderrStr });
                }
            });
        });
    }
}

