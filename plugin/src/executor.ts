import * as vscode from 'vscode';
import * as cp from 'child_process';
import * as path from 'path';
import * as fs from 'fs';

export class IslExecutor {
    private outputChannel: vscode.OutputChannel;

    constructor() {
        this.outputChannel = vscode.window.createOutputChannel('ISL');
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

    private async executeIsl(document: vscode.TextDocument, inputJson: string): Promise<void> {
        // Save document if it has unsaved changes
        if (document.isDirty) {
            await document.save();
        }

        const config = vscode.workspace.getConfiguration('isl.execution');
        let islCommand = config.get<string>('islCommand', 'isl');
        const javaHome = config.get<string>('javaHome', '');

        // Check if we should use the local isl.sh or isl.bat
        const workspaceFolder = vscode.workspace.getWorkspaceFolder(document.uri);
        if (workspaceFolder) {
            const islSh = path.join(workspaceFolder.uri.fsPath, 'isl.sh');
            const islBat = path.join(workspaceFolder.uri.fsPath, 'isl.bat');
            
            if (process.platform === 'win32' && fs.existsSync(islBat)) {
                islCommand = islBat;
            } else if (fs.existsSync(islSh)) {
                islCommand = islSh;
            }
        }

        // Prepare environment
        const env = { ...process.env };
        if (javaHome) {
            env.JAVA_HOME = javaHome;
            env.PATH = `${path.join(javaHome, 'bin')}${path.delimiter}${env.PATH}`;
        }

        this.outputChannel.clear();
        this.outputChannel.show();
        this.outputChannel.appendLine('=== ISL Execution ===');
        this.outputChannel.appendLine(`Script: ${document.fileName}`);
        this.outputChannel.appendLine(`Input: ${inputJson}`);
        this.outputChannel.appendLine('');

        // Create temporary files for input
        const tempDir = path.join(path.dirname(document.fileName), '.isl-temp');
        if (!fs.existsSync(tempDir)) {
            fs.mkdirSync(tempDir, { recursive: true });
        }

        const tempInputFile = path.join(tempDir, 'input.json');
        fs.writeFileSync(tempInputFile, inputJson);

        try {
            // Execute ISL command
            const args = [document.fileName, '-i', tempInputFile];
            
            this.outputChannel.appendLine(`Command: ${islCommand} ${args.join(' ')}`);
            this.outputChannel.appendLine('');

            const result = await this.execPromise(islCommand, args, { env, cwd: path.dirname(document.fileName) });

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
            vscode.window.showErrorMessage('ISL execution failed. Check output for details.');
        } finally {
            // Cleanup temp files
            try {
                fs.unlinkSync(tempInputFile);
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
                    reject({ message: error.message, stdout: stdoutStr, stderr: stderrStr });
                } else {
                    resolve({ stdout: stdoutStr, stderr: stderrStr });
                }
            });
        });
    }
}

