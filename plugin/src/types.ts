import * as vscode from 'vscode';
import * as yaml from 'js-yaml';
import { IslExtensionsManager } from './extensions';

export interface SchemaProperty {
    type?: string;
    description?: string;
    /** Resolved nested object schema when this property is an object (from $ref or inline). */
    schema?: SchemaInfo;
    /** Enum values when this property is an enum (from $ref to enum schema or inline enum). */
    enum?: string[];
    /** Single example value (from example field). */
    example?: string;
    /** Multiple examples (from examples array). */
    examples?: string[];
    /** OpenAPI/JSON Schema x- extensions (x-example, x-summary, etc.). */
    extensions?: Record<string, unknown>;
}

export interface SchemaInfo {
    required: string[];
    properties: Record<string, SchemaProperty>;
}

/** Result when cursor is inside a typed object (root or nested): schema to use for completions. */
export interface SchemaAtPosition {
    typeName: string;
    propertyPath: string[];
    schema: SchemaInfo;
}

export interface TypeDeclaration {
    /** Type name (e.g. "account" or "idx:account") */
    name: string;
    source: 'url' | 'inline';
    url?: string;
    /** Inline schema string for source === 'inline', e.g. "{ FirstName: string, LastName: string }" */
    inlineBody?: string;
}

const CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour for schemas (configurable via isl.extensions.cacheTTL)

interface CachedSchema {
    schema: SchemaInfo;
    timestamp: number;
}

interface CachedDocument {
    doc: Record<string, unknown>;
    timestamp: number;
}

/**
 * Parses type declarations from an ISL document and resolves schema from URLs or inline definitions.
 * Loads $ref dependencies (internal and external) and builds the full object graph including child objects.
 */
export class IslTypeManager {
    private schemaCache: Map<string, CachedSchema> = new Map();
    private documentCache: Map<string, CachedDocument> = new Map();

    constructor(
        private extensionsManager: IslExtensionsManager,
        private outputChannel?: vscode.OutputChannel
    ) {}

    private log(message: string, level: 'log' | 'warn' | 'error' = 'log'): void {
        if (this.outputChannel) {
            this.outputChannel.appendLine(message);
        }
        if (level === 'error') {
            console.error(message);
        } else if (level === 'warn') {
            console.warn(message);
        }
    }

    /**
     * Extracts all type declarations from the document.
     * - type Name from 'url'
     * - type Name as { Prop: Type, ... }
     * Name may be qualified (e.g. idx:account).
     */
    getTypeDeclarations(document: vscode.TextDocument): TypeDeclaration[] {
        const declarations: TypeDeclaration[] = [];
        const text = document.getText();
        const lines = text.split('\n');

        for (const line of lines) {
            // type name from 'url' or type name from "url"
            const fromMatch = line.match(/^\s*type\s+([a-zA-Z_][a-zA-Z0-9_.:]*)\s+from\s+['"]([^'"]+)['"]\s*;?\s*$/);
            if (fromMatch) {
                declarations.push({
                    name: fromMatch[1].trim(),
                    source: 'url',
                    url: fromMatch[2].trim()
                });
                continue;
            }

            // type name as { ... };
            const asMatch = line.match(/^\s*type\s+([a-zA-Z_][a-zA-Z0-9_.:]*)\s+as\s+\{([^}]*)\}\s*;?\s*$/);
            if (asMatch) {
                declarations.push({
                    name: asMatch[1].trim(),
                    source: 'inline',
                    inlineBody: asMatch[2].trim()
                });
            }
        }

        return declarations;
    }

    /**
     * Returns the type declaration for a type name if declared in the document (for hover: URL, source).
     */
    getDeclarationForType(document: vscode.TextDocument, typeName: string): TypeDeclaration | null {
        const declarations = this.getTypeDeclarations(document);
        return declarations.find(d => this.typeNameMatches(d.name, typeName)) ?? null;
    }

    /**
     * Returns the schema for a type name (e.g. "account" or "idx:account") if declared in the document.
     * For URL-based types, fetches and parses the schema (using GitHub/git for GitHub URLs via extensions manager).
     */
    async getSchemaForType(document: vscode.TextDocument, typeName: string): Promise<SchemaInfo | null> {
        const declarations = this.getTypeDeclarations(document);
        const decl = declarations.find(d => this.typeNameMatches(d.name, typeName));
        if (!decl) {
            return null;
        }

        if (decl.source === 'inline' && decl.inlineBody !== undefined) {
            return this.parseInlineSchema(decl.inlineBody);
        }

        if (decl.source === 'url' && decl.url) {
            return await this.fetchAndParseSchemaFromUrl(decl.url);
        }

        return null;
    }

    private typeNameMatches(declared: string, requested: string): boolean {
        return declared === requested || declared.replace(/:/g, '.') === requested.replace(/:/g, '.');
    }

    private parseInlineSchema(inlineBody: string): SchemaInfo {
        const properties: Record<string, SchemaProperty> = {};
        const required: string[] = [];
        // Split by comma, but respect nested braces
        const parts = this.splitTopLevel(inlineBody, ',');
        for (const part of parts) {
            const colon = part.indexOf(':');
            if (colon === -1) continue;
            const propName = part.substring(0, colon).trim();
            const typePart = part.substring(colon + 1).trim();
            if (!propName) continue;
            properties[propName] = { type: typePart || 'any' };
            required.push(propName);
        }
        return { required, properties };
    }

    private splitTopLevel(str: string, sep: string): string[] {
        const result: string[] = [];
        let current = '';
        let depth = 0;
        for (let i = 0; i < str.length; i++) {
            const ch = str[i];
            if (ch === '{' || ch === '[' || ch === '(') depth++;
            else if (ch === '}' || ch === ']' || ch === ')') depth--;
            else if (depth === 0 && ch === sep) {
                result.push(current.trim());
                current = '';
                continue;
            }
            current += ch;
        }
        if (current.trim()) result.push(current.trim());
        return result;
    }

    private getCacheTTL(): number {
        try {
            const ttl = vscode.workspace.getConfiguration('isl').get<number>('extensions.cacheTTL', 3600);
            return ttl * 1000;
        } catch {
            return CACHE_TTL_MS;
        }
    }

    private async fetchAndParseSchemaFromUrl(urlString: string): Promise<SchemaInfo | null> {
        const cacheKey = urlString;
        const cached = this.schemaCache.get(cacheKey);
        const ttl = this.getCacheTTL();
        if (cached && (Date.now() - cached.timestamp) < ttl) {
            this.log(`[ISL Types] Schema cache hit: ${urlString} (${Object.keys(cached.schema.properties).length} props)`);
            return cached.schema;
        }

        try {
            this.log(`[ISL Types] Loading schema from ${urlString}`);
            const doc = await this.getDocument(urlString);
            if (!doc) {
                this.log(`[ISL Types] No document loaded from ${urlString}`, 'warn');
                return null;
            }
            const preferredName = this.getSchemaNameFromUrl(urlString);
            const rootSchemaObj = this.getRootSchemaObject(doc, preferredName);
            if (!rootSchemaObj) {
                this.log(`[ISL Types] No schema in components.schemas for ${urlString}${preferredName ? ` (tried ${preferredName})` : ''}`, 'warn');
                return null;
            }
            const schema = await this.resolveSchemaGraph(rootSchemaObj, doc, urlString, new Set());
            if (schema) {
                this.schemaCache.set(cacheKey, { schema, timestamp: Date.now() });
                this.log(`[ISL Types] Schema loaded from ${urlString} (${Object.keys(schema.properties).length} top-level properties)`);
            }
            return schema;
        } catch (error) {
            this.log(`[ISL Types] Failed to fetch schema from ${urlString}: ${error instanceof Error ? error.message : String(error)}`, 'error');
            if (cached) return cached.schema;
            return null;
        }
    }

    /** Load and cache a document from URL (JSON or YAML). */
    private async getDocument(urlString: string): Promise<Record<string, unknown> | null> {
        const cached = this.documentCache.get(urlString);
        const ttl = this.getCacheTTL();
        if (cached && (Date.now() - cached.timestamp) < ttl) {
            this.log(`[ISL Types] Document cache hit: ${urlString}`);
            return cached.doc;
        }
        try {
            this.log(`[ISL Types] Fetching document (cache miss): ${urlString}`);
            const content = await this.extensionsManager.fetchContentFromUrl(urlString);
            const trimmed = content.trim();
            const doc = trimmed.startsWith('{')
                ? (JSON.parse(content) as Record<string, unknown>)
                : (yaml.load(content) as Record<string, unknown>);
            if (doc && typeof doc === 'object') {
                this.documentCache.set(urlString, { doc, timestamp: Date.now() });
                return doc;
            }
        } catch (e) {
            this.log(`[ISL Types] Failed to load document from ${urlString}: ${e instanceof Error ? e.message : String(e)}`, 'error');
        }
        return null;
    }

    /**
     * Get the base URL for resolving relative $refs. For GitHub blob URLs, refs like
     * "common/v1/Address.yaml" are relative to the repo root, not the file's directory.
     */
    private getRefBaseUrl(documentUrl: string): string {
        try {
            const parsed = new URL(documentUrl);
            const pathname = parsed.pathname;
            // GitHub blob: /owner/repo/blob/ref/path/to/file.yaml -> base is /owner/repo/blob/ref/
            const blobMatch = pathname.match(/^(\/[^/]+\/[^/]+\/blob\/[^/]+\/)(?:.+)?$/);
            if (blobMatch) {
                return `${parsed.origin}${blobMatch[1]}`;
            }
            // raw.githubusercontent.com: /owner/repo/ref/path/to/file -> base is /owner/repo/ref/
            const rawMatch = pathname.match(/^(\/[^/]+\/[^/]+\/[^/]+\/)(?:.+)?$/);
            if (rawMatch && parsed.hostname.includes('raw')) {
                return `${parsed.origin}${rawMatch[1]}`;
            }
            // Fallback: use directory of current file (strip last path segment)
            const lastSlash = pathname.lastIndexOf('/');
            if (lastSlash > 0) {
                return `${parsed.origin}${pathname.substring(0, lastSlash + 1)}`;
            }
        } catch {
            /* ignore */
        }
        return documentUrl;
    }

    /** Get value at JSON Pointer path (e.g. ["components","schemas","Order"]). */
    private getByPath(obj: Record<string, unknown>, path: string[]): unknown {
        let current: unknown = obj;
        for (const segment of path) {
            if (current == null || typeof current !== 'object') return undefined;
            current = (current as Record<string, unknown>)[segment];
        }
        return current;
    }

    /** Resolve $ref to document and path. Handles #/path, relative URLs, and absolute URLs with optional fragment. */
    private async resolveRef(
        baseUrl: string,
        ref: string
    ): Promise<{ doc: Record<string, unknown>; path: string[]; resolvedUrl: string } | null> {
        const refTrim = ref.trim();
        if (!refTrim) {
            this.log(`[ISL Types] Empty $ref from ${baseUrl}`, 'warn');
            return null;
        }

        this.log(`[ISL Types] Resolving $ref: ${refTrim} (base: ${baseUrl})`);

        if (refTrim.startsWith('#')) {
            const path = refTrim.slice(1).split('/').filter(Boolean);
            const doc = await this.getDocument(baseUrl);
            if (!doc) {
                this.log(`[ISL Types] $ref ${refTrim}: base document not loaded`, 'warn');
                return null;
            }
            this.log(`[ISL Types] $ref ${refTrim} resolved (internal, path: ${path.join('/') || '(root)'})`);
            return { doc, path, resolvedUrl: baseUrl };
        }

        const hashIdx = refTrim.indexOf('#');
        const urlPart = hashIdx >= 0 ? refTrim.substring(0, hashIdx).trim() : refTrim;
        const fragment = hashIdx >= 0 ? refTrim.substring(hashIdx + 1).trim() : '';
        const path = fragment ? fragment.split('/').filter(Boolean) : [];

        // Base for relative refs:
        // - ./ or ../ or bare filename (OrderExtension.yaml) -> file's directory (same folder)
        // - path like common/v1/Address.yaml -> repo root
        const fileDir = baseUrl.replace(/\/[^/]*$/, '/');
        const refBase =
            urlPart.startsWith('./') || urlPart.startsWith('../') || !urlPart.includes('/')
                ? fileDir
                : this.getRefBaseUrl(baseUrl);
        let resolvedUrl: string;
        try {
            resolvedUrl = urlPart ? new URL(urlPart, refBase).href : baseUrl;
        } catch {
            this.log(`[ISL Types] Invalid $ref URL: ${refTrim}`, 'warn');
            return null;
        }

        const doc = await this.getDocument(resolvedUrl);
        if (!doc) {
            this.log(`[ISL Types] $ref ${refTrim}: failed to load ${resolvedUrl}`, 'warn');
            return null;
        }
        this.log(`[ISL Types] $ref ${refTrim} resolved to ${resolvedUrl} (path: ${path.join('/') || '(root)'})`);
        return { doc, path, resolvedUrl };
    }

    /** Derive schema name from URL path (e.g. .../Order.yaml -> Order, .../BaseEntity.yaml -> BaseEntity). */
    private getSchemaNameFromUrl(urlString: string): string | undefined {
        try {
            const parsed = new URL(urlString);
            const pathname = parsed.pathname;
            const lastSegment = pathname.split('/').filter(Boolean).pop() || '';
            const base = lastSegment.replace(/\.(yaml|yml|json)$/i, '');
            return base || undefined;
        } catch {
            return undefined;
        }
    }

    /** Get schema object from resolved ref (handles empty path = whole doc → use OpenAPI components.schemas). */
    private getSchemaObjectFromResolved(
        resolved: { doc: Record<string, unknown>; path: string[]; resolvedUrl: string }
    ): Record<string, unknown> | null {
        if (resolved.path.length > 0) {
            const target = this.getByPath(resolved.doc, resolved.path);
            if (target && typeof target === 'object') return target as Record<string, unknown>;
            return null;
        }
        const preferredName = this.getSchemaNameFromUrl(resolved.resolvedUrl);
        return this.getRootSchemaObject(resolved.doc, preferredName);
    }

    /**
     * Get the schema object from an OpenAPI document. Schemas live in components.schemas.[name].
     * When preferredSchemaName is given (e.g. "Order" from Order.yaml), use that schema if it exists.
     */
    private getRootSchemaObject(doc: Record<string, unknown>, preferredSchemaName?: string): Record<string, unknown> | null {
        const components = doc.components as Record<string, unknown> | undefined;
        const schemas = components?.schemas as Record<string, unknown> | undefined;
        if (schemas && typeof schemas === 'object') {
            if (preferredSchemaName && schemas[preferredSchemaName] && typeof schemas[preferredSchemaName] === 'object') {
                this.log(`[ISL Types] Using OpenAPI schema components.schemas.${preferredSchemaName}`);
                return schemas[preferredSchemaName] as Record<string, unknown>;
            }
            const firstKey = Object.keys(schemas)[0];
            const first = firstKey ? (schemas[firstKey] as Record<string, unknown>) : null;
            if (first && typeof first === 'object') {
                this.log(`[ISL Types] Using OpenAPI schema components.schemas.${firstKey}`);
                return first;
            }
        }
        if (doc.properties !== undefined && typeof doc === 'object') return doc;
        return doc as Record<string, unknown>;
    }

    /**
     * Resolve a schema object (possibly with $ref, allOf) and build SchemaInfo with nested $refs resolved.
     * Visited set uses "resolvedUrl#/path" to avoid circular refs.
     * If a $ref is missing or fails to load, we log and continue (keep other properties / refs).
     */
    private async resolveSchemaGraph(
        schemaObj: Record<string, unknown>,
        doc: Record<string, unknown>,
        baseUrl: string,
        visited: Set<string>
    ): Promise<SchemaInfo> {
        let obj = schemaObj;

        if (typeof obj.$ref === 'string') {
            const refKey = `${baseUrl}#${obj.$ref}`;
            if (visited.has(refKey)) {
                this.log(`[ISL Types] Circular $ref skipped: ${obj.$ref}`);
                return { required: [], properties: {} };
            }
            visited.add(refKey);
            try {
                const resolved = await this.resolveRef(baseUrl, obj.$ref);
                if (resolved) {
                    const target = this.getSchemaObjectFromResolved(resolved);
                    if (target) {
                        obj = target;
                        doc = resolved.doc;
                        baseUrl = resolved.resolvedUrl;
                    } else {
                        this.log(`[ISL Types] $ref ${obj.$ref}: path not found in document`, 'warn');
                    }
                }
            } catch (err) {
                this.log(`[ISL Types] $ref ${obj.$ref} failed: ${err instanceof Error ? err.message : String(err)}`, 'warn');
            }
        }

        if (Array.isArray(obj.allOf) && obj.allOf.length > 0) {
            const merged: SchemaInfo = { required: [], properties: {} };
            for (const item of obj.allOf) {
                if (!item || typeof item !== 'object') continue;
                const itemObj = item as Record<string, unknown>;
                const resolved = await this.resolveSchemaGraph(itemObj, doc, baseUrl, visited);
                this.mergeSchemaInfo(merged, resolved);
            }
            this.log(`[ISL Types] allOf merged: ${Object.keys(merged.properties).length} properties from ${obj.allOf.length} item(s)`);
            const inlineProps = obj.properties as Record<string, unknown> | undefined;
            if (inlineProps && typeof inlineProps === 'object') {
                const inlineSchema = await this.extractPropertiesFromObject(obj, doc, baseUrl, visited);
                this.mergeSchemaInfo(merged, inlineSchema);
                this.log(`[ISL Types] allOf + inline properties: ${Object.keys(merged.properties).length} total`);
            }
            return merged;
        }

        return this.extractPropertiesFromObject(obj, doc, baseUrl, visited);
    }

    private formatExampleValue(val: unknown): string | undefined {
        if (val === null) return 'null';
        if (typeof val === 'string') return val;
        if (typeof val === 'number' || typeof val === 'boolean') return String(val);
        if (typeof val === 'object') return JSON.stringify(val, null, 2);
        return undefined;
    }

    private mergeSchemaInfo(target: SchemaInfo, source: SchemaInfo): void {
        for (const [key, val] of Object.entries(source.properties)) {
            target.properties[key] = val;
        }
        const requiredSet = new Set(target.required);
        for (const r of source.required) {
            requiredSet.add(r);
        }
        target.required = Array.from(requiredSet);
    }

    private async extractPropertiesFromObject(
        obj: Record<string, unknown>,
        doc: Record<string, unknown>,
        baseUrl: string,
        visited: Set<string>
    ): Promise<SchemaInfo> {
        const properties = obj.properties as Record<string, unknown> | undefined;
        if (!properties || typeof properties !== 'object') {
            return { required: [], properties: {} };
        }

        const required = Array.isArray(obj.required) ? (obj.required as string[]) : [];
        const result: Record<string, SchemaProperty> = {};

        for (const [key, val] of Object.entries(properties)) {
            if (!val || typeof val !== 'object') {
                result[key] = { type: 'any' };
                continue;
            }
            const v = val as Record<string, unknown>;
            const prop: SchemaProperty = {
                type: typeof v.type === 'string' ? v.type : undefined,
                description: typeof v.description === 'string' ? v.description : undefined
            };

            // Extract example / examples
            if (v.example !== undefined) {
                prop.example = this.formatExampleValue(v.example);
            }
            if (Array.isArray(v.examples)) {
                prop.examples = v.examples
                    .map((e: unknown) => (typeof e === 'object' && e !== null && 'value' in e) ? (e as { value: unknown }).value : e)
                    .map((e: unknown) => this.formatExampleValue(e))
                    .filter((s): s is string => s !== undefined);
            }
            // Extract x-* extensions
            const extensions: Record<string, unknown> = {};
            for (const [k, val2] of Object.entries(v)) {
                if (k.startsWith('x-')) extensions[k] = val2;
            }
            if (Object.keys(extensions).length > 0) prop.extensions = extensions;

            if (Array.isArray(v.enum)) {
                prop.enum = v.enum.filter((e): e is string => typeof e === 'string');
                this.log(`[ISL Types] Property "${key}" enum: ${prop.enum.join(', ')}`);
            } else if (typeof v.$ref === 'string') {
                const refKey = `${baseUrl}#${v.$ref}`;
                if (!visited.has(refKey)) {
                    visited.add(refKey);
                    try {
                        const resolved = await this.resolveRef(baseUrl, v.$ref);
                        if (resolved) {
                            const target = this.getSchemaObjectFromResolved(resolved);
                            if (target) {
                                const t = target as Record<string, unknown>;
                                if (!prop.description && typeof t.description === 'string') prop.description = t.description;
                                if (!prop.example && t.example !== undefined) prop.example = this.formatExampleValue(t.example);
                                if (!prop.examples && Array.isArray(t.examples)) {
                                    prop.examples = t.examples
                                        .map((e: unknown) => (typeof e === 'object' && e !== null && 'value' in e) ? (e as { value: unknown }).value : e)
                                        .map((e: unknown) => this.formatExampleValue(e))
                                        .filter((s): s is string => s !== undefined);
                                }
                                if (!prop.extensions) {
                                    const ext: Record<string, unknown> = {};
                                    for (const [k, val2] of Object.entries(t)) {
                                        if (k.startsWith('x-')) ext[k] = val2;
                                    }
                                    if (Object.keys(ext).length > 0) prop.extensions = ext;
                                }
                                if (Array.isArray(target.enum)) {
                                    prop.enum = target.enum.filter((e): e is string => typeof e === 'string');
                                    this.log(`[ISL Types] Resolved property "${key}" $ref -> enum: ${prop.enum.join(', ')}`);
                                } else if (target.properties && typeof target.properties === 'object') {
                                    prop.schema = await this.resolveSchemaGraph(
                                        target,
                                        resolved.doc,
                                        resolved.resolvedUrl,
                                        visited
                                    );
                                    prop.type = prop.type ?? 'object';
                                    this.log(`[ISL Types] Resolved property "${key}" $ref -> ${Object.keys(prop.schema.properties).length} nested properties`);
                                }
                            } else {
                                this.log(`[ISL Types] Property "${key}" $ref ${v.$ref}: path not found, keeping as plain property`, 'warn');
                            }
                        } else {
                            this.log(`[ISL Types] Property "${key}" $ref ${v.$ref}: could not resolve, keeping as plain property`, 'warn');
                        }
                    } catch (err) {
                        this.log(`[ISL Types] Property "${key}" $ref ${v.$ref} failed: ${err instanceof Error ? err.message : String(err)}`, 'warn');
                    }
                }
            } else if (v.properties && typeof v.properties === 'object') {
                try {
                    prop.schema = await this.resolveSchemaGraph(v, doc, baseUrl, visited);
                    prop.type = prop.type ?? 'object';
                    this.log(`[ISL Types] Resolved property "${key}" inline -> ${Object.keys(prop.schema.properties).length} nested properties`);
                } catch (err) {
                    this.log(`[ISL Types] Property "${key}" inline schema failed: ${err instanceof Error ? err.message : String(err)}`, 'warn');
                }
            }

            result[key] = prop;
        }

        return { required, properties: result };
    }

    /**
     * Finds the type name of the object literal at the given position, if any.
     * Looks for patterns: $var name : TypeName = { or $name : TypeName = { and checks if position is inside the following {}.
     */
    getTypeNameForObjectLiteralAt(document: vscode.TextDocument, position: vscode.Position): string | null {
        const text = document.getText();
        const offset = document.offsetAt(position);

        // Find the innermost object literal that contains this position (matching { ... })
        const openBraces: number[] = [];
        const containingPairs: { start: number; end: number }[] = [];

        for (let i = 0; i < text.length; i++) {
            const ch = text[i];
            if (ch === '{') {
                openBraces.push(i);
            } else if (ch === '}') {
                if (openBraces.length > 0) {
                    const start = openBraces.pop()!;
                    if (offset >= start && offset <= i) {
                        containingPairs.push({ start, end: i });
                    }
                }
            }
        }

        // Use the innermost pair (smallest span)
        if (containingPairs.length === 0) return null;
        let best = containingPairs[0];
        for (const p of containingPairs) {
            if (p.end - p.start < best.end - best.start) best = p;
        }
        const objectStart = best.start;
        const objectEnd = best.end;

        // Look backward from objectStart for " = " then " : TypeName " then "$var name" or "$name"
        const before = text.substring(0, objectStart);
        const eqIdx = before.lastIndexOf('=');
        if (eqIdx === -1) return null;
        const afterEq = before.substring(eqIdx + 1).trim();
        if (afterEq !== '' && afterEq !== '{') return null; // only "= {" or "= {"

        const beforeEq = before.substring(0, eqIdx);
        // Match : TypeName (optional spaces); TypeName can contain :
        const colonTypeMatch = beforeEq.match(/\:\s*([a-zA-Z_][a-zA-Z0-9_.:]*)\s*$/);
        if (!colonTypeMatch) return null;

        return colonTypeMatch[1].trim();
    }

    /**
     * Gets the schema for the object at position, including nested objects (e.g. billingAddress inside order).
     * Returns root type + property path + schema for that nested object.
     */
    async getSchemaForObjectAt(document: vscode.TextDocument, position: vscode.Position): Promise<SchemaAtPosition | null> {
        const text = document.getText();
        const offset = document.offsetAt(position);

        const containingPairs: { start: number; end: number }[] = [];
        const openBraces: number[] = [];
        for (let i = 0; i < text.length; i++) {
            const ch = text[i];
            if (ch === '{') openBraces.push(i);
            else if (ch === '}') {
                if (openBraces.length > 0) {
                    const start = openBraces.pop()!;
                    if (offset >= start && offset <= i) containingPairs.push({ start, end: i });
                }
            }
        }
        if (containingPairs.length === 0) return null;

        const sortedBySpan = [...containingPairs].sort((a, b) => (a.end - a.start) - (b.end - b.start));
        const innermost = sortedBySpan[0];

        const isRootTypedObject = (objStart: number): { typeName: string } | null => {
            const before = text.substring(0, objStart);
            const eqIdx = before.lastIndexOf('=');
            if (eqIdx === -1) return null;
            const afterEq = before.substring(eqIdx + 1).trim();
            if (afterEq !== '' && afterEq !== '{') return null;
            const beforeEq = before.substring(0, eqIdx);
            const m = beforeEq.match(/\:\s*([a-zA-Z_][a-zA-Z0-9_.:]*)\s*$/);
            return m ? { typeName: m[1].trim() } : null;
        };

        const parentOf = (child: { start: number; end: number }): { start: number; end: number } | null => {
            for (const p of sortedBySpan) {
                if (p.start === child.start && p.end === child.end) continue;
                if (p.start <= child.start && p.end >= child.end && (p.end - p.start) > (child.end - child.start))
                    return p;
            }
            return null;
        };

        const propNameBefore = (parentStart: number, objStart: number): string | null => {
            const segment = text.substring(parentStart + 1, objStart + 1);
            const m = segment.match(/([a-zA-Z_][a-zA-Z0-9_]*)\s*:\s*\{\s*$/);
            return m ? m[1] : null;
        };

        let current = innermost;
        const path: string[] = [];
        let typeName: string | null = null;

        for (;;) {
            const root = isRootTypedObject(current.start);
            if (root) {
                typeName = root.typeName;
                break;
            }
            const parent = parentOf(current);
            if (!parent) break;
            const prop = propNameBefore(parent.start, current.start);
            if (!prop) break;
            path.unshift(prop);
            current = parent;
        }

        if (!typeName) return null;

        const schema = await this.getSchemaForType(document, typeName);
        if (!schema) {
            this.log(`[ISL Types] No schema for type ${typeName} at position`, 'warn');
            return null;
        }

        let nestedSchema = schema;
        for (const prop of path) {
            const propInfo = nestedSchema.properties[prop];
            if (!propInfo?.schema) {
                this.log(`[ISL Types] No nested schema for path ${typeName}.${path.join('.')} (${prop} has no schema)`, 'warn');
                return null;
            }
            nestedSchema = propInfo.schema;
        }

        if (path.length > 0) {
            this.log(`[ISL Types] Nested object at ${typeName}.${path.join('.')}: ${Object.keys(nestedSchema.properties).length} properties`);
        }

        return { typeName, propertyPath: path, schema: nestedSchema };
    }

    dispose(): void {
        this.schemaCache.clear();
        this.documentCache.clear();
    }
}
