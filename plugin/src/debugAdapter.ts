import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';

const DEBUG_ADAPTER_JAR = 'isl-debug-adapter-all.jar';

/**
 * Provides the executable descriptor for the ISL Debug Adapter.
 * VSCode spawns `java -jar isl-debug-adapter-all.jar` and communicates via stdin/stdout.
 */
export class IslDebugAdapterDescriptorFactory implements vscode.DebugAdapterDescriptorFactory {
    private readonly extensionPath: string;

    constructor(extensionPath: string) {
        this.extensionPath = extensionPath;
    }

    async createDebugAdapterDescriptor(
        _session: vscode.DebugSession,
        _executable: vscode.DebugAdapterExecutable | undefined
    ): Promise<vscode.DebugAdapterDescriptor | null> {
        const jarPath = this.findDebugAdapterJar();
        if (!jarPath) {
            vscode.window.showErrorMessage(
                'ISL Debug Adapter JAR not found. Build it with: gradlew :isl-debug-adapter:shadowJar'
            );
            return null;
        }

        const javaPath = await this.findJava();
        if (!javaPath) {
            vscode.window.showErrorMessage(
                'Java not found. Set isl.execution.javaHome or ensure Java is on your PATH.'
            );
            return null;
        }

        return new vscode.DebugAdapterExecutable(javaPath, ['-jar', jarPath]);
    }

    private findDebugAdapterJar(): string | null {
        // Check in plugin/lib
        const libPath = path.join(this.extensionPath, 'lib', DEBUG_ADAPTER_JAR);
        if (fs.existsSync(libPath)) {
            return libPath;
        }

        // Check in workspace build output (for development)
        const workspaceFolders = vscode.workspace.workspaceFolders;
        if (workspaceFolders) {
            for (const folder of workspaceFolders) {
                const devPath = path.join(
                    folder.uri.fsPath, 'isl-debug-adapter', 'build', 'libs'
                );
                if (fs.existsSync(devPath)) {
                    const files = fs.readdirSync(devPath);
                    const jar = files.find(f => f.startsWith('isl-debug-adapter') && f.endsWith('-all.jar'));
                    if (jar) {
                        return path.join(devPath, jar);
                    }
                }
            }
        }

        return null;
    }

    private async findJava(): Promise<string | null> {
        const config = vscode.workspace.getConfiguration('isl.execution');
        const javaHome = config.get<string>('javaHome', '');
        if (javaHome) {
            const p = path.join(javaHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
            if (fs.existsSync(p)) {
                return p;
            }
        }
        const envHome = process.env.JAVA_HOME;
        if (envHome) {
            const p = path.join(envHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
            if (fs.existsSync(p)) {
                return p;
            }
        }
        return process.platform === 'win32' ? 'java.exe' : 'java';
    }
}

/**
 * Provides default launch configurations when no launch.json exists.
 */
export class IslDebugConfigurationProvider implements vscode.DebugConfigurationProvider {
    resolveDebugConfiguration(
        _folder: vscode.WorkspaceFolder | undefined,
        config: vscode.DebugConfiguration,
        _token?: vscode.CancellationToken
    ): vscode.ProviderResult<vscode.DebugConfiguration> {
        // If the user hits F5 with no launch.json, provide defaults
        if (!config.type && !config.request && !config.name) {
            const editor = vscode.window.activeTextEditor;
            if (editor && editor.document.languageId === 'isl') {
                config.type = 'isl';
                config.name = 'Debug ISL';
                config.request = 'launch';
                config.script = editor.document.uri.fsPath;
                config.function = 'run';
            }
        }

        if (!config.script) {
            return vscode.window.showInformationMessage('Cannot debug: no ISL script specified.').then(_ => undefined);
        }

        return config;
    }
}
