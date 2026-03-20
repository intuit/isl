import * as path from 'path';
import * as fs from 'fs';

export interface ModifierSignatureParam {
    label: string;
    documentation?: string;
}

export interface ModifierSignature {
    label: string;
    parameters?: ModifierSignatureParam[];
    documentation?: string;
}

export interface ModifierHover {
    description: string;
    signature?: string;
    example?: string;
}

export interface IslReturns {
    type?: string;
    description?: string;
}

export interface BuiltInModifier {
    name: string;
    detail?: string;
    insertText: string;
    documentation?: string;
    returns?: IslReturns;
    signature?: ModifierSignature;
    hover?: ModifierHover;
}

export interface FunctionSignatureParam {
    label: string;
    documentation?: string;
}

export interface FunctionSignature {
    label: string;
    parameters?: FunctionSignatureParam[];
    documentation?: string;
}

export interface BuiltInFunction {
    namespace: string;
    name: string;
    detail?: string;
    params?: string;
    documentation?: string;
    returns?: IslReturns;
    signature?: FunctionSignature;
}

export interface ServiceInfo {
    name: string;
    detail?: string;
    documentation?: string;
}

export interface AnnotationInfo {
    name: string;
    detail?: string;
    insertText?: string;
    documentation?: string;
}

export interface IslLanguageData {
    modifierValidationPatterns: string[];
    modifiers: BuiltInModifier[];
    functions: BuiltInFunction[];
    services: ServiceInfo[];
    annotations?: AnnotationInfo[];
}

let cachedData: IslLanguageData | null = null;
let extensionPath: string | null = null;

/**
 * Initialize the language data from the extension path. Call this from extension.ts on activate.
 */
export function initIslLanguage(extPath: string): void {
    extensionPath = extPath;
    cachedData = null;
}

function getDataPath(): string {
    if (extensionPath) {
        return path.join(extensionPath, 'isl-language.json');
    }
    return path.join(__dirname, '..', 'isl-language.json');
}

function loadData(): IslLanguageData {
    if (cachedData) {
        return cachedData;
    }
    const dataPath = getDataPath();
    try {
        const raw = fs.readFileSync(dataPath, 'utf-8');
        cachedData = JSON.parse(raw) as IslLanguageData;
        if (!cachedData.modifiers) cachedData.modifiers = [];
        if (!cachedData.functions) cachedData.functions = [];
        if (!cachedData.services) cachedData.services = [];
        if (!cachedData.annotations) cachedData.annotations = [];
        if (!cachedData.modifierValidationPatterns) cachedData.modifierValidationPatterns = [];
        return cachedData;
    } catch (e) {
        console.error('[ISL] Failed to load isl-language.json:', e);
        cachedData = {
            modifierValidationPatterns: [],
            modifiers: [],
            functions: [],
            services: []
        };
        return cachedData;
    }
}

/**
 * Get the full language data (modifiers, functions, services, validation patterns).
 */
export function getLanguageData(): IslLanguageData {
    return loadData();
}

/**
 * Set of built-in modifier names + wildcard patterns for validation (e.g. "trim", "to.*").
 */
export function getBuiltInModifiersSet(): Set<string> {
    const data = loadData();
    const set = new Set<string>(data.modifiers.map(m => m.name));
    data.modifierValidationPatterns.forEach(p => set.add(p));
    return set;
}

/**
 * Set of built-in function keys "Namespace.name" for validation (e.g. "Date.now", "Math.sum").
 */
export function getBuiltInFunctionsSet(): Set<string> {
    const data = loadData();
    return new Set(data.functions.map(f => `${f.namespace}.${f.name}`));
}

/**
 * Set of built-in namespace names (Date, Math, String, etc.) for validation.
 */
export function getBuiltInNamespacesSet(): Set<string> {
    const data = loadData();
    const names = new Set(data.functions.map(f => f.namespace));
    names.add('This');
    names.add('Pagination');
    return names;
}

/**
 * Map of modifier name -> definition for completion, hover, signature.
 */
export function getModifiersMap(): Map<string, BuiltInModifier> {
    const data = loadData();
    const map = new Map<string, BuiltInModifier>();
    data.modifiers.forEach(m => map.set(m.name, m));
    return map;
}

/**
 * Map of "Namespace.name" -> function definition for completion, hover, signature.
 */
export function getFunctionsMap(): Map<string, BuiltInFunction> {
    const data = loadData();
    const map = new Map<string, BuiltInFunction>();
    data.functions.forEach(f => map.set(`${f.namespace}.${f.name}`, f));
    return map;
}

/**
 * Map of namespace -> function definitions for service method completion/signature.
 */
export function getFunctionsByNamespace(): Map<string, BuiltInFunction[]> {
    const data = loadData();
    const map = new Map<string, BuiltInFunction[]>();
    data.functions.forEach(f => {
        const list = map.get(f.namespace) || [];
        list.push(f);
        map.set(f.namespace, list);
    });
    return map;
}

/**
 * Map of service name -> ServiceInfo for service completion and hover.
 */
export function getServicesMap(): Map<string, ServiceInfo> {
    const data = loadData();
    const map = new Map<string, ServiceInfo>();
    data.services.forEach(s => map.set(s.name, s));
    return map;
}

/**
 * Get annotations (e.g. @test, @setup) for completion and hover.
 */
export function getAnnotations(): AnnotationInfo[] {
    const data = loadData();
    return data.annotations ?? [];
}
