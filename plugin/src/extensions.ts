import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import * as https from 'https';
import * as http from 'http';
import * as os from 'os';
import * as cp from 'child_process';
import * as url from 'url';

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

/** Case-insensitive lookup for extension function by name (e.g. sendEmail vs SendEmail). */
export function getExtensionFunction(ext: IslExtensions, name: string): IslFunctionDefinition | undefined {
    if (!name) return undefined;
    if (ext.functions.has(name)) return ext.functions.get(name);
    const lower = name.toLowerCase();
    for (const [key, def] of ext.functions) {
        if (key.toLowerCase() === lower) return def;
    }
    return undefined;
}

/** Case-insensitive lookup for extension modifier by name. */
export function getExtensionModifier(ext: IslExtensions, name: string): IslModifierDefinition | undefined {
    if (!name) return undefined;
    if (ext.modifiers.has(name)) return ext.modifiers.get(name);
    const lower = name.toLowerCase();
    for (const [key, def] of ext.modifiers) {
        if (key.toLowerCase() === lower) return def;
    }
    return undefined;
}

/**
 * Manages loading and caching of custom ISL extensions from .islextensions files
 */
export class IslExtensionsManager {
    private extensionsCache: Map<string, IslExtensions> = new Map();
    private fileWatcher: vscode.FileSystemWatcher | undefined;
    private onDidChangeEmitter = new vscode.EventEmitter<vscode.Uri>();
    private globalExtensionsCache: { content: string; timestamp: number } | null = null;
    private configWatcher: vscode.Disposable | undefined;
    private readonly outputChannel: vscode.OutputChannel | undefined;

    /**
     * Event fired when an .islextensions file changes
     */
    public readonly onDidChange = this.onDidChangeEmitter.event;

    constructor(outputChannel?: vscode.OutputChannel) {
        this.outputChannel = outputChannel;
        this.setupFileWatcher();
        this.setupConfigWatcher();
    }

    /**
     * Writes a message to the extension output channel (and console when debugging)
     */
    private log(message: string, level: 'log' | 'warn' | 'error' = 'log'): void {
        if (this.outputChannel) {
            this.outputChannel.appendLine(message);
        }
        if (level === 'log') {
            console.log(message);
        } else if (level === 'warn') {
            console.warn(message);
        } else {
            console.error(message);
        }
    }

    /**
     * Sets up file watcher for .islextensions files
     */
    private setupFileWatcher() {
        // Watch for .islextensions files in the workspace
        this.fileWatcher = vscode.workspace.createFileSystemWatcher('**/.islextensions');
        
        this.fileWatcher.onDidCreate(uri => {
            this.log(`[ISL Extensions] File created: ${uri.fsPath}`);
            this.invalidateCache(uri);
            this.onDidChangeEmitter.fire(uri);
        });
        
        this.fileWatcher.onDidChange(uri => {
            this.log(`[ISL Extensions] File changed: ${uri.fsPath}`);
            this.invalidateCache(uri);
            this.onDidChangeEmitter.fire(uri);
        });
        
        this.fileWatcher.onDidDelete(uri => {
            this.log(`[ISL Extensions] File deleted: ${uri.fsPath}`);
            this.invalidateCache(uri);
            this.onDidChangeEmitter.fire(uri);
        });
    }

    /**
     * Sets up configuration watcher for global extensions source
     */
    private setupConfigWatcher() {
        this.configWatcher = vscode.workspace.onDidChangeConfiguration(event => {
            if (event.affectsConfiguration('isl.extensions.source') || 
                event.affectsConfiguration('isl.extensions.cacheTTL')) {
                const source = vscode.workspace.getConfiguration('isl').get<string>('extensions.source', '');
                this.log(`[ISL Extensions] Configuration changed - source: ${source || '(none)'}`);
                // Clear global cache when config changes
                this.globalExtensionsCache = null;
                // Invalidate all workspace caches to force reload
                this.extensionsCache.clear();
                this.log('[ISL Extensions] Cleared all extension caches due to configuration change');
                // Notify all workspaces to reload
                vscode.workspace.workspaceFolders?.forEach(folder => {
                    this.onDidChangeEmitter.fire(vscode.Uri.file(folder.uri.fsPath));
                });
            }
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

    /** Cache key used when document has no workspace folder (e.g. untitled or single-file) */
    private static readonly NO_WORKSPACE_CACHE_KEY = '__global_extensions__';

    /**
     * Gets extensions for a given document's workspace (or global source if no workspace)
     */
    public async getExtensionsForDocument(document: vscode.TextDocument): Promise<IslExtensions> {
        const workspaceFolder = vscode.workspace.getWorkspaceFolder(document.uri);

        if (workspaceFolder) {
            // Check cache first
            const cached = this.extensionsCache.get(workspaceFolder.uri.fsPath);
            if (cached) {
                return cached;
            }
            const extensions = await this.loadExtensionsForWorkspace(workspaceFolder);
            this.extensionsCache.set(workspaceFolder.uri.fsPath, extensions);
            return extensions;
        }

        // No workspace folder (untitled, or file outside workspace) – use global source only
        const cached = this.extensionsCache.get(IslExtensionsManager.NO_WORKSPACE_CACHE_KEY);
        if (cached) {
            return cached;
        }
        const extensions = await this.loadExtensionsFromGlobalSourceOnly();
        this.extensionsCache.set(IslExtensionsManager.NO_WORKSPACE_CACHE_KEY, extensions);
        return extensions;
    }

    /**
     * Loads extensions from global source only (used when document has no workspace folder)
     */
    private async loadExtensionsFromGlobalSourceOnly(): Promise<IslExtensions> {
        const globalSource = vscode.workspace.getConfiguration('isl').get<string>('extensions.source', '');
        if (!globalSource) {
            return this.createEmptyExtensions();
        }
        try {
            this.log(`[ISL Extensions] Loading global extensions (no workspace): ${globalSource}`);
            const content = await this.loadGlobalExtensions(globalSource);
            if (content) {
                const extensions = this.parseExtensionsFile(content, globalSource);
                this.log(`[ISL Extensions] Loaded ${extensions.functions.size} functions and ${extensions.modifiers.size} modifiers from global source`);
                return extensions;
            }
        } catch (error) {
            this.log(`[ISL Extensions] Error loading global extensions: ${error instanceof Error ? error.message : String(error)}`, 'error');
        }
        return this.createEmptyExtensions();
    }

    /**
     * Loads extensions from .islextensions file in the workspace
     * Priority: workspace-local file > global source (URL/file) > empty
     */
    private async loadExtensionsForWorkspace(workspaceFolder: vscode.WorkspaceFolder): Promise<IslExtensions> {
        // First, try workspace-local file (highest priority)
        const extensionsPath = path.join(workspaceFolder.uri.fsPath, '.islextensions');
        
        try {
            if (fs.existsSync(extensionsPath)) {
                this.log(`[ISL Extensions] Loading workspace-local extensions from: ${extensionsPath}`);
                const content = fs.readFileSync(extensionsPath, 'utf-8');
                const extensions = this.parseExtensionsFile(content, extensionsPath);
                this.log(`[ISL Extensions] Loaded ${extensions.functions.size} functions and ${extensions.modifiers.size} modifiers from workspace-local file`);
                return extensions;
            } else {
                this.log(`[ISL Extensions] No workspace-local .islextensions file found at: ${extensionsPath}`);
            }
        } catch (error) {
            this.log(`[ISL Extensions] Error loading workspace-local .islextensions file: ${error instanceof Error ? error.message : String(error)}`, 'error');
            vscode.window.showWarningMessage(`Failed to load workspace .islextensions file: ${error instanceof Error ? error.message : String(error)}`);
        }

        // If no workspace-local file, try global source
        const globalSource = vscode.workspace.getConfiguration('isl').get<string>('extensions.source', '');
        if (!globalSource) {
            return this.createEmptyExtensions();
        }

        try {
            this.log(`[ISL Extensions] Loading global extensions from: ${globalSource}`);
            const content = await this.loadGlobalExtensions(globalSource);
            if (content) {
                const extensions = this.parseExtensionsFile(content, globalSource);
                this.log(`[ISL Extensions] Loaded ${extensions.functions.size} functions and ${extensions.modifiers.size} modifiers from global source`);
                return extensions;
            } else {
                this.log(`[ISL Extensions] No content loaded from global source: ${globalSource}`);
            }
        } catch (error) {
            this.log(`[ISL Extensions] Error loading global ISL extensions: ${error instanceof Error ? error.message : String(error)}`, 'error');
            // Don't show warning for global source failures - it's optional
        }

        return this.createEmptyExtensions();
    }

    /**
     * Loads extensions from global source (URL or file path)
     */
    private async loadGlobalExtensions(source: string): Promise<string | null> {
        // Check if it's a URL
        if (source.startsWith('http://') || source.startsWith('https://')) {
            return await this.loadFromUrl(source);
        }

        // Otherwise, treat as file path
        return await this.loadFromFile(source);
    }

    /**
     * Loads extensions from a URL with caching
     */
    private async loadFromUrl(urlString: string): Promise<string | null> {
        const cacheTTL = vscode.workspace.getConfiguration('isl').get<number>('extensions.cacheTTL', 3600);
        const now = Date.now();
        const cacheAge = this.globalExtensionsCache 
            ? Math.floor((now - this.globalExtensionsCache.timestamp) / 1000)
            : null;

        // Check cache
        if (this.globalExtensionsCache && 
            (now - this.globalExtensionsCache.timestamp) < (cacheTTL * 1000)) {
            this.log(`[ISL Extensions] Using cached extensions from URL (age: ${cacheAge}s, TTL: ${cacheTTL}s)`);
            return this.globalExtensionsCache.content;
        }

        // Check if it's a GitHub URL
        const githubInfo = this.parseGitHubUrl(urlString);
        if (githubInfo) {
            this.log(`[ISL Extensions] Detected GitHub URL: ${urlString}`);
            this.log(`[ISL Extensions] Parsed GitHub info: ${githubInfo.host}/${githubInfo.owner}/${githubInfo.repo}@${githubInfo.ref}:${githubInfo.path}`);
            try {
                const content = await this.downloadFromGitHub(githubInfo);
                this.log(`[ISL Extensions] Successfully downloaded from GitHub (${content.length} bytes)`);
                
                // Update cache
                this.globalExtensionsCache = {
                    content: content,
                    timestamp: now
                };
                this.log(`[ISL Extensions] Cached extensions (will expire in ${cacheTTL}s)`);
                return content;
            } catch (error) {
                this.log(`[ISL Extensions] Failed to download from GitHub: ${error instanceof Error ? error.message : String(error)}`, 'error');
                // Return cached content even if expired, as fallback
                if (this.globalExtensionsCache) {
                    this.log(`[ISL Extensions] Using expired cache as fallback (age: ${cacheAge}s)`);
                    return this.globalExtensionsCache.content;
                }
                return null;
            }
        }

        // Regular URL download
        try {
            this.log(`[ISL Extensions] Downloading extensions from URL: ${urlString}`);
            const content = await this.downloadUrl(urlString);
            this.log(`[ISL Extensions] Successfully downloaded ${content.length} bytes`);
            
            // Update cache
            this.globalExtensionsCache = {
                content: content,
                timestamp: now
            };
            this.log(`[ISL Extensions] Cached extensions (will expire in ${cacheTTL}s)`);
            return content;
        } catch (error) {
            this.log(`[ISL Extensions] Failed to download extensions from URL ${urlString}: ${error instanceof Error ? error.message : String(error)}`, 'error');
            // Return cached content even if expired, as fallback
            if (this.globalExtensionsCache) {
                this.log(`[ISL Extensions] Using expired cache as fallback (age: ${cacheAge}s)`);
                return this.globalExtensionsCache.content;
            }
            return null;
        }
    }

    /**
     * Parses a GitHub URL to extract repository information
     */
    private parseGitHubUrl(urlString: string): { host: string; owner: string; repo: string; ref: string; path: string } | null {
        try {
            const parsed = new url.URL(urlString);
            const host = parsed.hostname;
            
            // Check if it's a GitHub host (github.com or github.*.com)
            if (!host.includes('github')) {
                return null;
            }

            // Handle raw.githubusercontent.com URLs
            if (host === 'raw.githubusercontent.com') {
                const parts = parsed.pathname.split('/').filter(p => p);
                if (parts.length >= 4) {
                    return {
                        host: 'github.com',
                        owner: parts[0],
                        repo: parts[1],
                        ref: parts[2],
                        path: parts.slice(3).join('/')
                    };
                }
            }

            // Handle regular GitHub URLs (github.com or github.*.com)
            // Pattern: https://github.com/owner/repo/blob/branch/path/to/file
            const pathMatch = parsed.pathname.match(/^\/([^\/]+)\/([^\/]+)\/blob\/([^\/]+)\/(.+)$/);
            if (pathMatch) {
                return {
                    host: host,
                    owner: pathMatch[1],
                    repo: pathMatch[2],
                    ref: pathMatch[3],
                    path: pathMatch[4]
                };
            }

            return null;
        } catch (error) {
            return null;
        }
    }

    /**
     * Downloads content from GitHub using git/gh CLI with proper authentication
     */
    private async downloadFromGitHub(githubInfo: { host: string; owner: string; repo: string; ref: string; path: string }): Promise<string> {
        // Try GitHub CLI (gh) first
        try {
            this.log(`[ISL Extensions] Attempting to download via GitHub CLI (gh)`);
            const content = await this.downloadViaGhCli(githubInfo);
            if (content) {
                return content;
            }
        } catch (error) {
            this.log(`[ISL Extensions] GitHub CLI failed: ${error instanceof Error ? error.message : String(error)}`);
        }

        // Fall back to git checkout
        try {
            this.log(`[ISL Extensions] Attempting to download via git checkout`);
            const content = await this.downloadViaGit(githubInfo);
            if (content) {
                return content;
            }
        } catch (error) {
            this.log(`[ISL Extensions] Git checkout failed: ${error instanceof Error ? error.message : String(error)}`);
        }

        // Final fallback: direct download from raw URL
        this.log(`[ISL Extensions] Falling back to direct download from raw URL`);
        const rawUrl = `https://${githubInfo.host === 'github.com' ? 'raw.githubusercontent.com' : githubInfo.host.replace('github', 'raw.githubusercontent')}/${githubInfo.owner}/${githubInfo.repo}/${githubInfo.ref}/${githubInfo.path}`;
        return await this.downloadUrl(rawUrl);
    }

    /**
     * Downloads file using GitHub CLI (gh)
     */
    private downloadViaGhCli(githubInfo: { host: string; owner: string; repo: string; ref: string; path: string }): Promise<string> {
        return new Promise((resolve, reject) => {
            const repo = `${githubInfo.owner}/${githubInfo.repo}`;
            const command = 'gh';
            const args = ['api', `repos/${repo}/contents/${githubInfo.path}`, '--jq', '.content', '--field', `ref=${githubInfo.ref}`];

            // If it's not github.com, set the host
            if (githubInfo.host !== 'github.com') {
                args.push('--hostname', githubInfo.host);
            }

            this.log(`[ISL Extensions] Executing: gh ${args.join(' ')}`);

            cp.execFile(command, args, { timeout: 30000 }, (error, stdout, stderr) => {
                if (error) {
                    reject(new Error(`gh CLI failed: ${error.message}`));
                    return;
                }

                if (stderr && !stdout) {
                    reject(new Error(`gh CLI error: ${stderr}`));
                    return;
                }

                try {
                    // GitHub API returns base64-encoded content
                    const base64Content = stdout.trim().replace(/"/g, '');
                    const content = Buffer.from(base64Content, 'base64').toString('utf-8');
                    resolve(content);
                } catch (parseError) {
                    reject(new Error(`Failed to decode GitHub API response: ${parseError instanceof Error ? parseError.message : String(parseError)}`));
                }
            });
        });
    }

    /**
     * Downloads file using git checkout
     */
    private downloadViaGit(githubInfo: { host: string; owner: string; repo: string; ref: string; path: string }): Promise<string> {
        return new Promise((resolve, reject) => {
            // Create a temporary directory for git operations
            const tempDir = path.join(os.tmpdir(), `isl-extensions-${Date.now()}`);
            
            const repoUrl = `https://${githubInfo.host}/${githubInfo.owner}/${githubInfo.repo}.git`;
            const filePath = path.join(tempDir, githubInfo.path);

            this.log(`[ISL Extensions] Cloning ${repoUrl} to ${tempDir}`);

            // Clone the repository (shallow, single branch)
            cp.exec(`git clone --depth 1 --branch ${githubInfo.ref} ${repoUrl} ${tempDir}`, { timeout: 60000 }, (cloneError) => {
                if (cloneError) {
                    // Try without branch specification (some repos might not have the branch)
                    this.log(`[ISL Extensions] Clone with branch failed, trying without branch`);
                    cp.exec(`git clone --depth 1 ${repoUrl} ${tempDir}`, { timeout: 60000 }, (cloneError2) => {
                        if (cloneError2) {
                            // Cleanup
                            try {
                                fs.rmSync(tempDir, { recursive: true, force: true });
                            } catch {}
                            reject(new Error(`Git clone failed: ${cloneError2.message}`));
                            return;
                        }

                        // Checkout the specific ref
                        cp.exec(`git checkout ${githubInfo.ref}`, { cwd: tempDir, timeout: 30000 }, (checkoutError) => {
                            if (checkoutError) {
                                try {
                                    fs.rmSync(tempDir, { recursive: true, force: true });
                                } catch {}
                                reject(new Error(`Git checkout failed: ${checkoutError.message}`));
                                return;
                            }

                            this.readAndCleanup(tempDir, filePath, resolve, reject);
                        });
                    });
                    return;
                }

                this.readAndCleanup(tempDir, filePath, resolve, reject);
            });
        });
    }

    /**
     * Helper to read file and cleanup temp directory
     */
    private readAndCleanup(tempDir: string, filePath: string, resolve: (value: string) => void, reject: (error: Error) => void) {
        try {
            if (!fs.existsSync(filePath)) {
                try {
                    fs.rmSync(tempDir, { recursive: true, force: true });
                } catch {}
                reject(new Error(`File not found in repository: ${filePath}`));
                return;
            }

            const content = fs.readFileSync(filePath, 'utf-8');
            
            // Cleanup
            try {
                fs.rmSync(tempDir, { recursive: true, force: true });
            } catch (cleanupError) {
                this.log(`[ISL Extensions] Failed to cleanup temp directory: ${cleanupError}`, 'warn');
            }

            resolve(content);
        } catch (readError) {
            // Cleanup on error
            try {
                fs.rmSync(tempDir, { recursive: true, force: true });
            } catch {}
            reject(new Error(`Failed to read file: ${readError instanceof Error ? readError.message : String(readError)}`));
        }
    }

    /**
     * Downloads content from a URL
     */
    private downloadUrl(urlString: string): Promise<string> {
        return new Promise((resolve, reject) => {
            const client = urlString.startsWith('https://') ? https : http;
            
            client.get(urlString, (res) => {
                if (res.statusCode !== 200) {
                    reject(new Error(`HTTP ${res.statusCode}: ${res.statusMessage}`));
                    return;
                }

                let data = '';
                res.on('data', (chunk) => {
                    data += chunk;
                });
                res.on('end', () => {
                    resolve(data);
                });
            }).on('error', (err) => {
                reject(err);
            });
        });
    }

    /**
     * Loads extensions from a file path
     */
    private async loadFromFile(filePath: string): Promise<string | null> {
        try {
            // Resolve ~ to home directory
            const resolvedPath = filePath.startsWith('~') 
                ? path.join(os.homedir(), filePath.slice(1))
                : filePath;

            // Resolve relative paths from home directory if not absolute
            const absolutePath = path.isAbsolute(resolvedPath) 
                ? resolvedPath 
                : path.join(os.homedir(), resolvedPath);

            if (!fs.existsSync(absolutePath)) {
                this.log(`[ISL Extensions] Global extensions file not found: ${absolutePath}`, 'warn');
                return null;
            }

            this.log(`[ISL Extensions] Loading extensions from file: ${absolutePath}`);
            const content = fs.readFileSync(absolutePath, 'utf-8');
            this.log(`[ISL Extensions] Loaded ${content.length} bytes from file`);
            return content;
        } catch (error) {
            this.log(`[ISL Extensions] Failed to load extensions from file ${filePath}: ${error instanceof Error ? error.message : String(error)}`, 'error');
            return null;
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
                        this.log('[ISL Extensions] Skipping function without name in .islextensions', 'warn');
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
                        this.log('[ISL Extensions] Skipping modifier without name in .islextensions', 'warn');
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

            this.log(`[ISL Extensions] Parsed ${extensions.functions.size} custom functions and ${extensions.modifiers.size} custom modifiers from ${filePath}`);
        } catch (error) {
            this.log(`[ISL Extensions] Error parsing .islextensions file: ${error instanceof Error ? error.message : String(error)}`, 'error');
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
     * Preloads extensions for all workspace folders and global-only cache.
     * Call on activation so the first completion/validation has a warm cache.
     */
    public async preloadExtensions(): Promise<void> {
        const folders = vscode.workspace.workspaceFolders;
        if (folders?.length) {
            for (const folder of folders) {
                if (!this.extensionsCache.has(folder.uri.fsPath)) {
                    const ext = await this.loadExtensionsForWorkspace(folder);
                    this.extensionsCache.set(folder.uri.fsPath, ext);
                }
            }
        }
        const globalSource = vscode.workspace.getConfiguration('isl').get<string>('extensions.source', '');
        if (globalSource && !this.extensionsCache.has(IslExtensionsManager.NO_WORKSPACE_CACHE_KEY)) {
            const ext = await this.loadExtensionsFromGlobalSourceOnly();
            this.extensionsCache.set(IslExtensionsManager.NO_WORKSPACE_CACHE_KEY, ext);
        }
    }

    /**
     * Reloads all cached extensions
     */
    public async reloadAll(): Promise<void> {
        this.log('[ISL Extensions] Reloading all extensions (clearing cache)');
        this.extensionsCache.clear();
        this.globalExtensionsCache = null;
        
        // Reload for all open ISL documents
        const islDocuments = vscode.workspace.textDocuments.filter(doc => doc.languageId === 'isl');
        this.log(`[ISL Extensions] Reloading extensions for ${islDocuments.length} open ISL document(s)`);
        
        for (const document of islDocuments) {
            await this.getExtensionsForDocument(document);
        }
        
        this.log('[ISL Extensions] Finished reloading all extensions');
    }

    /**
     * Disposes the manager and cleans up resources
     */
    public dispose() {
        this.fileWatcher?.dispose();
        this.configWatcher?.dispose();
        this.onDidChangeEmitter.dispose();
        this.extensionsCache.clear();
        this.globalExtensionsCache = null;
    }
}

