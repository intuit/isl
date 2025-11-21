import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';

export class IslCodeLensProvider implements vscode.CodeLensProvider {
    private _onDidChangeCodeLenses: vscode.EventEmitter<void> = new vscode.EventEmitter<void>();
    public readonly onDidChangeCodeLenses: vscode.Event<void> = this._onDidChangeCodeLenses.event;

    constructor() {}

    public provideCodeLenses(document: vscode.TextDocument, token: vscode.CancellationToken): vscode.CodeLens[] | Thenable<vscode.CodeLens[]> {
        const codeLenses: vscode.CodeLens[] = [];
        const text = document.getText();
        const lines = text.split('\n');

        // Find all function declarations
        const functionPattern = /^\s*(fun|modifier)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(([^)]*)\)/;

        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            const match = line.match(functionPattern);

            if (match) {
                const funcType = match[1]; // 'fun' or 'modifier'
                const funcName = match[2];
                const params = match[3].trim();

                const range = new vscode.Range(i, 0, i, line.length);

                // Add "Run" button
                const runCommand: vscode.Command = {
                    title: `▶ Run ${funcName}`,
                    command: 'isl.runFunction',
                    arguments: [document.uri, funcName, params]
                };

                codeLenses.push(new vscode.CodeLens(range, runCommand));
            }
        }

        return codeLenses;
    }

    public resolveCodeLens(codeLens: vscode.CodeLens, token: vscode.CancellationToken): vscode.CodeLens | Thenable<vscode.CodeLens> {
        return codeLens;
    }

    public refresh(): void {
        this._onDidChangeCodeLenses.fire();
    }
}

export async function runIslFunction(uri: vscode.Uri, functionName: string, params: string, context: vscode.ExtensionContext) {
    const document = await vscode.workspace.openTextDocument(uri);
    const filePath = uri.fsPath;

    // Parse parameters to create input JSON
    const paramList = params
        .split(',')
        .map(p => p.trim())
        .filter(p => p.length > 0)
        .map(p => {
            // Extract parameter name (remove type annotations if present)
            const paramName = p.split(':')[0].trim().replace('$', '');
            return paramName;
        });

    // Create input JSON based on parameters
    let inputJson = '{}';
    if (paramList.length > 0) {
        // Prompt user for input values
        const userInput = await vscode.window.showInputBox({
            prompt: `Enter input JSON for ${functionName}(${params})`,
            placeHolder: createSampleInput(paramList),
            value: createSampleInput(paramList),
            ignoreFocusOut: true
        });

        if (userInput === undefined) {
            return; // User cancelled
        }

        inputJson = userInput;
    }

    // Save document if dirty
    if (document.isDirty) {
        await document.save();
    }

    // Find Java
    const javaPath = await findJava();
    if (!javaPath) {
        vscode.window.showErrorMessage(
            'Java not found. Please install Java 11+ or configure isl.execution.javaHome',
            'Open Settings'
        ).then(selection => {
            if (selection === 'Open Settings') {
                vscode.commands.executeCommand('workbench.action.openSettings', 'isl.execution.javaHome');
            }
        });
        return;
    }

    // Get embedded JAR path
    const jarPath = path.join(context.extensionPath, 'lib', 'isl-cmd-all.jar');
    
    if (!fs.existsSync(jarPath)) {
        vscode.window.showErrorMessage(
            'ISL runtime not found in extension. The extension may be corrupted. Please reinstall.'
        );
        return;
    }

    // Create terminal
    const isWindows = process.platform === 'win32';
    const shellPath = isWindows ? 'powershell.exe' : undefined;
    
    const terminal = vscode.window.createTerminal({
        name: `ISL: ${functionName}`,
        cwd: path.dirname(filePath),
        shellPath: shellPath
    });

    terminal.show();

    // Write input to temporary file
    const tempDir = path.join(path.dirname(filePath), '.isl-temp');
    if (!fs.existsSync(tempDir)) {
        fs.mkdirSync(tempDir, { recursive: true });
    }

    const tempInputFile = path.join(tempDir, `input-${functionName}.json`);
    fs.writeFileSync(tempInputFile, inputJson);

    // Build command
    let command: string;

    if (isWindows) {
        // PowerShell command
        const javaCmd = escapeForPowerShell(javaPath);
        const jarFile = escapeForPowerShell(jarPath);
        const islFile = escapeForPowerShell(filePath);
        const inputFile = escapeForPowerShell(tempInputFile);
        
        command = `& "${javaCmd}" -jar "${jarFile}" transform "${islFile}" -i "${inputFile}" --function ${functionName} --pretty`;
    } else {
        // Unix/Mac command
        const javaCmd = escapeForBash(javaPath);
        const jarFile = escapeForBash(jarPath);
        const islFile = escapeForBash(filePath);
        const inputFile = escapeForBash(tempInputFile);
        
        command = `"${javaCmd}" -jar "${jarFile}" transform "${islFile}" -i "${inputFile}" --function ${functionName} --pretty`;
    }

    terminal.sendText(command);

    // Show notification
    vscode.window.showInformationMessage(`Running ${functionName}...`);

    // Clean up temp file after a delay
    setTimeout(() => {
        try {
            if (fs.existsSync(tempInputFile)) {
                fs.unlinkSync(tempInputFile);
            }
        } catch (e) {
            // Ignore cleanup errors
        }
    }, 10000);
}

async function findJava(): Promise<string | null> {
    // Check configured JAVA_HOME
    const config = vscode.workspace.getConfiguration('isl.execution');
    const configuredJavaHome = config.get<string>('javaHome');
    
    if (configuredJavaHome) {
        const javaPath = path.join(configuredJavaHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
        if (fs.existsSync(javaPath)) {
            return javaPath;
        }
    }

    // Check JAVA_HOME environment variable
    const javaHome = process.env.JAVA_HOME;
    if (javaHome) {
        const javaPath = path.join(javaHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
        if (fs.existsSync(javaPath)) {
            return javaPath;
        }
    }

    // Try java in PATH
    return process.platform === 'win32' ? 'java.exe' : 'java';
}

function escapeForPowerShell(filepath: string): string {
    // For PowerShell, escape single quotes
    return filepath.replace(/'/g, "''");
}

function escapeForBash(filepath: string): string {
    // For bash, escape special characters
    return filepath.replace(/(["\s'$`\\])/g, '\\$1');
}

function createSampleInput(paramNames: string[]): string {
    if (paramNames.length === 0) {
        return '{}';
    }

    const inputObj: any = {};
    for (const param of paramNames) {
        // Create sample values based on parameter names
        const paramLower = param.toLowerCase();
        if (paramLower.includes('id')) {
            inputObj[param] = 123;
        } else if (paramLower.includes('name')) {
            inputObj[param] = 'Sample Name';
        } else if (paramLower.includes('email')) {
            inputObj[param] = 'user@example.com';
        } else if (paramLower.includes('price') || paramLower.includes('amount')) {
            inputObj[param] = 99.99;
        } else if (paramLower.includes('count') || paramLower.includes('quantity')) {
            inputObj[param] = 1;
        } else if (paramLower.includes('active') || paramLower.includes('enabled')) {
            inputObj[param] = true;
        } else if (paramLower.includes('date')) {
            inputObj[param] = '2024-01-15';
        } else if (paramLower.includes('items') || paramLower.includes('list') || paramLower.includes('array')) {
            inputObj[param] = [];
        } else if (paramLower.includes('input') || paramLower.includes('data')) {
            inputObj[param] = { value: 'example' };
        } else {
            inputObj[param] = 'value';
        }
    }

    return JSON.stringify(inputObj, null, 2);
}
