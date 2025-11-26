import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';

export interface IslParameter {
    name: string;
    type?: string;
    description?: string;
    optional?: boolean;
    defaultValue?: string;
}

export interface IslFunctionDefinition {
    name: string;
    description?: string;
    parameters: IslParameter[];
    returns?: {
        type?: string;
        description?: string;
    };
    examples?: string[];
}

export interface IslModifierDefinition {
    name: string;
    description?: string;
    parameters: IslParameter[];
    returns?: {
        type?: string;
        description?: string;
    };
    examples?: string[];
}

export interface IslExtensions {
    functions: Map<string, IslFunctionDefinition>;
    modifiers: Map<string, IslModifierDefinition>;
}

/**
 * Manages loading and caching of custom ISL extensions from .islextensions files
 */
export class IslExtensionsManager {
    private extensionsCache: Map<string, IslExtensions> = new Map();
    private fileWatcher: vscode.FileSystemWatcher | undefined;
    private onDidChangeEmitter = new vscode.EventEmitter<vscode.Uri>();
    
    /**
     * Event fired when an .islextensions file changes
     */
    public readonly onDidChange = this.onDidChangeEmitter.event;

    constructor() {
        this.setupFileWatcher();
    }

    /**
     * Sets up file watcher for .islextensions files
     */
    private setupFileWatcher() {
        // Watch for .islextensions files in the workspace
        this.fileWatcher = vscode.workspace.createFileSystemWatcher('**/.islextensions');
        
        this.fileWatcher.onDidCreate(uri => {
            console.log('ISL extensions file created:', uri.fsPath);
            this.invalidateCache(uri);
            this.onDidChangeEmitter.fire(uri);
        });
        
        this.fileWatcher.onDidChange(uri => {
            console.log('ISL extensions file changed:', uri.fsPath);
            this.invalidateCache(uri);
            this.onDidChangeEmitter.fire(uri);
        });
        
        this.fileWatcher.onDidDelete(uri => {
            console.log('ISL extensions file deleted:', uri.fsPath);
            this.invalidateCache(uri);
            this.onDidChangeEmitter.fire(uri);
        });
    }

    /**
     * Invalidates the cache for a specific extensions file
     */
    private invalidateCache(uri: vscode.Uri) {
        const workspaceFolder = vscode.workspace.getWorkspaceFolder(uri);
        if (workspaceFolder) {
            this.extensionsCache.delete(workspaceFolder.uri.fsPath);
        }
    }

    /**
     * Gets extensions for a given document's workspace
     */
    public async getExtensionsForDocument(document: vscode.TextDocument): Promise<IslExtensions> {
        const workspaceFolder = vscode.workspace.getWorkspaceFolder(document.uri);
        if (!workspaceFolder) {
            return this.createEmptyExtensions();
        }

        // Check cache first
        const cached = this.extensionsCache.get(workspaceFolder.uri.fsPath);
        if (cached) {
            return cached;
        }

        // Load extensions file
        const extensions = await this.loadExtensionsForWorkspace(workspaceFolder);
        this.extensionsCache.set(workspaceFolder.uri.fsPath, extensions);
        return extensions;
    }

    /**
     * Loads extensions from .islextensions file in the workspace
     */
    private async loadExtensionsForWorkspace(workspaceFolder: vscode.WorkspaceFolder): Promise<IslExtensions> {
        const extensionsPath = path.join(workspaceFolder.uri.fsPath, '.islextensions');
        
        try {
            if (!fs.existsSync(extensionsPath)) {
                return this.createEmptyExtensions();
            }

            const content = fs.readFileSync(extensionsPath, 'utf-8');
            return this.parseExtensionsFile(content, extensionsPath);
        } catch (error) {
            console.error('Error loading ISL extensions:', error);
            vscode.window.showWarningMessage(`Failed to load .islextensions file: ${error instanceof Error ? error.message : String(error)}`);
            return this.createEmptyExtensions();
        }
    }

    /**
     * Parses the .islextensions file content
     */
    private parseExtensionsFile(content: string, filePath: string): IslExtensions {
        const extensions = this.createEmptyExtensions();

        try {
            const data = JSON.parse(content);

            // Parse functions
            if (data.functions && Array.isArray(data.functions)) {
                for (const func of data.functions) {
                    if (!func.name || typeof func.name !== 'string') {
                        console.warn('Skipping function without name in .islextensions');
                        continue;
                    }

                    const functionDef: IslFunctionDefinition = {
                        name: func.name,
                        description: func.description || func.desc || '',
                        parameters: this.parseParameters(func.parameters || func.params || []),
                        returns: func.returns ? {
                            type: func.returns.type,
                            description: func.returns.description || func.returns.desc
                        } : undefined,
                        examples: Array.isArray(func.examples) ? func.examples : undefined
                    };

                    extensions.functions.set(func.name, functionDef);
                }
            }

            // Parse modifiers
            if (data.modifiers && Array.isArray(data.modifiers)) {
                for (const mod of data.modifiers) {
                    if (!mod.name || typeof mod.name !== 'string') {
                        console.warn('Skipping modifier without name in .islextensions');
                        continue;
                    }

                    const modifierDef: IslModifierDefinition = {
                        name: mod.name,
                        description: mod.description || mod.desc || '',
                        parameters: this.parseParameters(mod.parameters || mod.params || []),
                        returns: mod.returns ? {
                            type: mod.returns.type,
                            description: mod.returns.description || mod.returns.desc
                        } : undefined,
                        examples: Array.isArray(mod.examples) ? mod.examples : undefined
                    };

                    extensions.modifiers.set(mod.name, modifierDef);
                }
            }

            console.log(`Loaded ${extensions.functions.size} custom functions and ${extensions.modifiers.size} custom modifiers from ${filePath}`);
        } catch (error) {
            console.error('Error parsing .islextensions file:', error);
            throw new Error(`Failed to parse .islextensions: ${error instanceof Error ? error.message : String(error)}`);
        }

        return extensions;
    }

    /**
     * Parses parameter definitions
     */
    private parseParameters(params: any[]): IslParameter[] {
        if (!Array.isArray(params)) {
            return [];
        }

        return params.map(param => {
            // Support both object and string formats
            if (typeof param === 'string') {
                return { name: param };
            }

            return {
                name: param.name || '',
                type: param.type,
                description: param.description || param.desc,
                optional: param.optional === true,
                defaultValue: param.default || param.defaultValue
            };
        });
    }

    /**
     * Creates an empty extensions object
     */
    private createEmptyExtensions(): IslExtensions {
        return {
            functions: new Map(),
            modifiers: new Map()
        };
    }

    /**
     * Reloads all cached extensions
     */
    public async reloadAll(): Promise<void> {
        this.extensionsCache.clear();
        
        // Reload for all open ISL documents
        for (const document of vscode.workspace.textDocuments) {
            if (document.languageId === 'isl') {
                await this.getExtensionsForDocument(document);
            }
        }
    }

    /**
     * Disposes the manager and cleans up resources
     */
    public dispose() {
        this.fileWatcher?.dispose();
        this.onDidChangeEmitter.dispose();
        this.extensionsCache.clear();
    }
}

