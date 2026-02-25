import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';

export class IslCodeLensProvider implements vscode.CodeLensProvider {
    private _onDidChangeCodeLenses: vscode.EventEmitter<void> = new vscode.EventEmitter<void>();
    public readonly onDidChangeCodeLenses: vscode.Event<void> = this._onDidChangeCodeLenses.event;

    constructor() {}

    public async provideCodeLenses(document: vscode.TextDocument, token: vscode.CancellationToken): Promise<vscode.CodeLens[]> {
        const codeLenses: vscode.CodeLens[] = [];
        const text = document.getText();
        const lines = text.split('\n');

        // Find all function declarations
        const functionPattern = /^\s*(fun|modifier)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(([^)]*)\)/;
        const functions: { name: string, type: string, line: number, params: string }[] = [];

        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            const match = line.match(functionPattern);

            if (match) {
                const funcType = match[1]; // 'fun' or 'modifier'
                const funcName = match[2];
                const params = match[3].trim();

                functions.push({ name: funcName, type: funcType, line: i, params });
            }
        }

        // Add CodeLens for each function
        for (const func of functions) {
            const range = new vscode.Range(func.line, 0, func.line, lines[func.line].length);
            const isTestOrSetup = this.isTestOrSetupFunction(lines, func.line);

            if (!isTestOrSetup) {
                // Count usages including imports (async)
                const usageCount = await this.countUsagesAsync(document, func.name, func.type);

                // Add "Run" button
                const runCommand: vscode.Command = {
                    title: `▶ Run`,
                    command: 'isl.runFunction',
                    arguments: [document.uri, func.name, func.params]
                };
                codeLenses.push(new vscode.CodeLens(range, runCommand));

                // Add usage count
                if (usageCount > 0) {
                    const usageCommand: vscode.Command = {
                        title: `📊 ${usageCount} ${usageCount === 1 ? 'usage' : 'usages'}`,
                        command: 'isl.showUsages',
                        arguments: [document.uri, func.name, func.type]
                    };
                    codeLenses.push(new vscode.CodeLens(range, usageCommand));
                } else if (func.name !== 'run') {
                    // Show "No usages" for functions other than 'run'
                    const noUsageCommand: vscode.Command = {
                        title: `⚠️ No usages found`,
                        command: '',
                        tooltip: 'This function is not being called anywhere'
                    };
                    codeLenses.push(new vscode.CodeLens(range, noUsageCommand));
                }
            }
        }

        return codeLenses;
    }
    
    /**
     * Returns true if the function at the given line is annotated with @setup or @test.
     */
    private isTestOrSetupFunction(lines: string[], functionLine: number): boolean {
        for (let i = Math.max(0, functionLine - 5); i < functionLine; i++) {
            const trimmed = lines[i].trim();
            if (/^@setup\s*$/.test(trimmed)) return true;
            if (/^@test\s*(?:\([^)]*\))?\s*$/.test(trimmed)) return true;
        }
        return false;
    }

    private countUsages(text: string, functionName: string, functionType: string): number {
        let count = 0;
        
        if (functionType === 'fun') {
            // Count @.This.functionName() calls
            const functionCallPattern = new RegExp(`@\\.This\\.${functionName}\\s*\\(`, 'g');
            const matches = text.match(functionCallPattern);
            count += matches ? matches.length : 0;
        } else if (functionType === 'modifier') {
            // Count | functionName usages
            const modifierPattern = new RegExp(`\\|\\s*${functionName}(?:\\s*\\(|\\s|$)`, 'g');
            const matches = text.match(modifierPattern);
            count += matches ? matches.length : 0;
        }
        
        return count;
    }

    private async countUsagesAsync(document: vscode.TextDocument, functionName: string, functionType: string): Promise<number> {
        // Start with local usages
        const text = document.getText();
        let count = this.countUsages(text, functionName, functionType);
        
        // Find all files that import this file and get the module names they use
        const importInfo = await this.findFilesAndModuleNamesThatImport(document.uri);
        
        // Count usages in imported files using the correct module name for each file
        for (const { fileUri, moduleName } of importInfo) {
            try {
                const importedText = fs.readFileSync(fileUri.fsPath, 'utf-8');
                
                if (functionType === 'fun') {
                    // Count @.ModuleName.functionName() calls (using the import name, not filename)
                    const functionCallPattern = new RegExp(`@\\.${moduleName}\\.${functionName}\\s*\\(`, 'g');
                    const matches = importedText.match(functionCallPattern);
                    count += matches ? matches.length : 0;
                } else if (functionType === 'modifier') {
                    // Count | ModuleName.modifierName usages (using the import name, not filename)
                    const modifierPattern = new RegExp(`\\|\\s*${moduleName}\\.${functionName}(?:\\s*\\(|\\s|$)`, 'g');
                    const matches = importedText.match(modifierPattern);
                    count += matches ? matches.length : 0;
                }
            } catch (error) {
                // Silently skip files that can't be read
                console.warn(`Could not read file ${fileUri.fsPath} for usage counting: ${error}`);
            }
        }
        
        return count;
    }

    /**
     * Finds all files that import the given file and returns the module name each file uses
     */
    private async findFilesAndModuleNamesThatImport(uri: vscode.Uri): Promise<Array<{ fileUri: vscode.Uri, moduleName: string }>> {
        const results: Array<{ fileUri: vscode.Uri, moduleName: string }> = [];
        const workspaceFolder = vscode.workspace.getWorkspaceFolder(uri);
        
        if (!workspaceFolder) {
            return results;
        }
        
        // Search for all .isl files in the workspace
        const pattern = new vscode.RelativePattern(workspaceFolder, '**/*.isl');
        const allIslFiles = await vscode.workspace.findFiles(pattern, null, 1000);
        
        // Check each file for imports that point to the current file
        for (const fileUri of allIslFiles) {
            // Skip the current file
            if (fileUri.fsPath === uri.fsPath) {
                continue;
            }
            
            try {
                const fileText = fs.readFileSync(fileUri.fsPath, 'utf-8');
                const lines = fileText.split('\n');
                
                // Check each line for import statements
                for (const line of lines) {
                    // Pattern: import ModuleName from 'file.isl' or import ModuleName from "file.isl"
                    const importMatch = line.match(/import\s+([a-zA-Z_][a-zA-Z0-9_]*)\s+from\s+['"]([^'"]+)['"]/);
                    if (importMatch) {
                        const moduleName = importMatch[1];
                        const importPath = importMatch[2];
                        
                        // Resolve the import path relative to the importing file
                        const importingDir = path.dirname(fileUri.fsPath);
                        let resolvedPath: string;
                        
                        if (path.isAbsolute(importPath)) {
                            resolvedPath = importPath;
                        } else {
                            resolvedPath = path.resolve(importingDir, importPath);
                        }
                        
                        // Try with .isl extension if not present
                        if (!resolvedPath.endsWith('.isl')) {
                            const withExtension = resolvedPath + '.isl';
                            if (fs.existsSync(withExtension)) {
                                resolvedPath = withExtension;
                            }
                        }
                        
                        // Check if the resolved path matches the current file
                        if (fs.existsSync(resolvedPath) && path.resolve(resolvedPath) === path.resolve(uri.fsPath)) {
                            results.push({ fileUri, moduleName });
                            break; // Found the import, no need to check more lines in this file
                        }
                    }
                }
            } catch (error) {
                // Silently skip files that can't be read
                console.warn(`Could not check file ${fileUri.fsPath} for imports: ${error}`);
            }
        }
        
        return results;
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

export async function showUsages(uri: vscode.Uri, functionName: string, functionType: string) {
    const document = await vscode.workspace.openTextDocument(uri);
    const text = document.getText();
    const locations: vscode.Location[] = [];
    
    // Find usages in current file
    const lines = text.split('\n');
    
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        let match;
        
        if (functionType === 'fun') {
            const pattern = new RegExp(`@\\.This\\.${functionName}\\s*\\(`, 'g');
            while ((match = pattern.exec(line)) !== null) {
                const startPos = new vscode.Position(i, match.index);
                const endPos = new vscode.Position(i, match.index + match[0].length);
                locations.push(new vscode.Location(uri, new vscode.Range(startPos, endPos)));
            }
        } else if (functionType === 'modifier') {
            const pattern = new RegExp(`\\|\\s*${functionName}(?:\\s*\\(|\\s|$)`, 'g');
            while ((match = pattern.exec(line)) !== null) {
                const startPos = new vscode.Position(i, match.index);
                const endPos = new vscode.Position(i, match.index + match[0].length);
                locations.push(new vscode.Location(uri, new vscode.Range(startPos, endPos)));
            }
        }
    }
    
    // Find all files that import this file and get the module names they use
    const importInfo = await findFilesAndModuleNamesThatImport(uri);
    
    for (const { fileUri, moduleName } of importInfo) {
        try {
            const importedText = fs.readFileSync(fileUri.fsPath, 'utf-8');
            const importedLines = importedText.split('\n');
            
            for (let i = 0; i < importedLines.length; i++) {
                const line = importedLines[i];
                let match;
                
                if (functionType === 'fun') {
                    // Look for @.ModuleName.functionName() calls (using the import name)
                    const pattern = new RegExp(`@\\.${moduleName}\\.${functionName}\\s*\\(`, 'g');
                    while ((match = pattern.exec(line)) !== null) {
                        const startPos = new vscode.Position(i, match.index);
                        const endPos = new vscode.Position(i, match.index + match[0].length);
                        locations.push(new vscode.Location(fileUri, new vscode.Range(startPos, endPos)));
                    }
                } else if (functionType === 'modifier') {
                    // Look for | ModuleName.modifierName usages (using the import name)
                    const pattern = new RegExp(`\\|\\s*${moduleName}\\.${functionName}(?:\\s*\\(|\\s|$)`, 'g');
                    while ((match = pattern.exec(line)) !== null) {
                        const startPos = new vscode.Position(i, match.index);
                        const endPos = new vscode.Position(i, match.index + match[0].length);
                        locations.push(new vscode.Location(fileUri, new vscode.Range(startPos, endPos)));
                    }
                }
            }
        } catch (error) {
            // Silently skip files that can't be read
            console.warn(`Could not read file ${fileUri.fsPath} for usage display: ${error}`);
        }
    }
    
    if (locations.length > 0) {
        vscode.commands.executeCommand('editor.action.showReferences', uri, locations[0].range.start, locations);
    } else {
        vscode.window.showInformationMessage(`No usages found for ${functionName}`);
    }
}

async function findFilesAndModuleNamesThatImport(uri: vscode.Uri): Promise<Array<{ fileUri: vscode.Uri, moduleName: string }>> {
    const results: Array<{ fileUri: vscode.Uri, moduleName: string }> = [];
    const workspaceFolder = vscode.workspace.getWorkspaceFolder(uri);
    
    if (!workspaceFolder) {
        return results;
    }
    
    // Search for all .isl files in the workspace
    const pattern = new vscode.RelativePattern(workspaceFolder, '**/*.isl');
    const allIslFiles = await vscode.workspace.findFiles(pattern, null, 1000);
    
    // Check each file for imports that point to the current file
    for (const fileUri of allIslFiles) {
        // Skip the current file
        if (fileUri.fsPath === uri.fsPath) {
            continue;
        }
        
        try {
            const fileText = fs.readFileSync(fileUri.fsPath, 'utf-8');
            const lines = fileText.split('\n');
            
            // Check each line for import statements
            for (const line of lines) {
                // Pattern: import ModuleName from 'file.isl' or import ModuleName from "file.isl"
                const importMatch = line.match(/import\s+([a-zA-Z_][a-zA-Z0-9_]*)\s+from\s+['"]([^'"]+)['"]/);
                if (importMatch) {
                    const moduleName = importMatch[1];
                    const importPath = importMatch[2];
                    
                    // Resolve the import path relative to the importing file
                    const importingDir = path.dirname(fileUri.fsPath);
                    let resolvedPath: string;
                    
                    if (path.isAbsolute(importPath)) {
                        resolvedPath = importPath;
                    } else {
                        resolvedPath = path.resolve(importingDir, importPath);
                    }
                    
                    // Try with .isl extension if not present
                    if (!resolvedPath.endsWith('.isl')) {
                        const withExtension = resolvedPath + '.isl';
                        if (fs.existsSync(withExtension)) {
                            resolvedPath = withExtension;
                        }
                    }
                    
                    // Check if the resolved path matches the current file
                    if (fs.existsSync(resolvedPath) && path.resolve(resolvedPath) === path.resolve(uri.fsPath)) {
                        results.push({ fileUri, moduleName });
                        break; // Found the import, no need to check more lines in this file
                    }
                }
            }
        } catch (error) {
            // Silently skip files that can't be read
            console.warn(`Could not check file ${fileUri.fsPath} for imports: ${error}`);
        }
    }
    
    return results;
}


export async function testFunction(uri: vscode.Uri, functionName: string, params: string, context: vscode.ExtensionContext) {
    // Same as runFunction but with test data
    await runIslFunction(uri, functionName, params, context);
}
