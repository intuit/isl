import * as vscode from 'vscode';
import { IslExtensionsManager, IslFunctionDefinition, IslModifierDefinition } from './extensions';
import { getModifiersMap, getFunctionsByNamespace, getServicesMap, type BuiltInModifier, type BuiltInFunction } from './language';
import { IslTypeManager } from './types';
import type { SchemaInfo } from './types';

export class IslCompletionProvider implements vscode.CompletionItemProvider {
    
    constructor(
        private extensionsManager: IslExtensionsManager,
        private typeManager?: IslTypeManager,
        private outputChannel?: vscode.OutputChannel
    ) {}
    
    async provideCompletionItems(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken,
        context: vscode.CompletionContext
    ): Promise<vscode.CompletionItem[] | vscode.CompletionList> {
        const linePrefix = document.lineAt(position).text.substr(0, position.character);
        
        // Type-based object literal completions (root and nested, e.g. billingAddress inside order)
        if (this.typeManager) {
            const schemaAt = await this.typeManager.getSchemaForObjectAt(document, position);
            if (schemaAt) {
                const { typeName, propertyPath, schema } = schemaAt;
                const pathStr = propertyPath.length > 0 ? `.${propertyPath.join('.')}` : '';
                const msg = `[ISL Completion] Schema at ${typeName}${pathStr}: ${Object.keys(schema.properties).length} properties`;
                this.outputChannel?.appendLine(msg);
                return this.getTypeBasedCompletions(document, position, schema, linePrefix);
            }
        }
        
        // Load custom extensions
        const extensions = await this.extensionsManager.getExtensionsForDocument(document);
        
        // Check for pagination cursor property access: $cursor.
        const paginationPropertyMatch = linePrefix.match(/\$([a-zA-Z_][a-zA-Z0-9_]*)\.(\w*)$/);
        if (paginationPropertyMatch) {
            const varName = paginationPropertyMatch[1];
            const paginationType = this.getPaginationType(document, varName);
            if (paginationType) {
                return this.getPaginationPropertyCompletions(paginationType);
            }
        }
        
        // Check for @.This. - only same-file functions and modifiers (not global extensions)
        if (linePrefix.match(/@\.This\.[\w]*$/)) {
            return this.getFunctionsFromDocument(document);
        }
        // Check for @.ServiceName. - show methods for that service (built-in) or single call for global extension
        const serviceMethodMatch = linePrefix.match(/@\.([A-Za-z_][a-zA-Z0-9_]*)\.(\w*)$/);
        if (serviceMethodMatch) {
            const serviceName = serviceMethodMatch[1];
            const methodPrefix = serviceMethodMatch[2] || '';
            return this.getServiceMethodCompletions(serviceName, methodPrefix, extensions);
        }
        // @. -> built-in services (Date, Math, This, ...) + global extension function names (called as @.name())
        else if (linePrefix.endsWith('@.')) {
            return this.getServiceCompletions(extensions);
        } else if (linePrefix.match(/\|\s*[\w.]*$/)) {
            return this.getModifierCompletions(document, extensions);
        } else if (linePrefix.match(/\$\w*$/)) {
            return this.getVariableCompletions(document);
        } else {
            return this.getKeywordCompletions();
        }
    }

    /** Same-file only: fun/modifier in this document. Used for @.This. (not global extensions). */
    private getFunctionsFromDocument(document: vscode.TextDocument): vscode.CompletionItem[] {
        const functions: vscode.CompletionItem[] = [];
        const text = document.getText();
        const lines = text.split('\n');
        
        const functionPattern = /^\s*(fun|modifier)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(([^)]*)\)/;
        
        for (let i = 0; i < lines.length; i++) {
            const match = lines[i].match(functionPattern);
            if (match) {
                const funcType = match[1];
                const funcName = match[2];
                const params = match[3].trim();
                
                const item = new vscode.CompletionItem(funcName, vscode.CompletionItemKind.Function);
                item.detail = `${funcType} ${funcName}(${params})`;
                
                const paramNames = params
                    .split(',')
                    .map(p => p.trim())
                    .filter(p => p.length > 0)
                    .map((p, idx) => {
                        const paramName = p.split(':')[0].trim();
                        return `\${${idx + 1}:${paramName}}`;
                    });
                
                if (paramNames.length > 0) {
                    item.insertText = new vscode.SnippetString(`${funcName}(${paramNames.join(', ')})`);
                } else {
                    item.insertText = new vscode.SnippetString(`${funcName}()`);
                }
                
                const docs = this.getDocumentationForFunction(lines, i);
                if (docs) {
                    item.documentation = new vscode.MarkdownString(docs);
                }
                
                functions.push(item);
            }
        }
        
        return functions;
    }

    /** Modifiers defined in this document (for | name completion). */
    private getSameFileModifiers(document: vscode.TextDocument): vscode.CompletionItem[] {
        const items: vscode.CompletionItem[] = [];
        const lines = document.getText().split('\n');
        const modifierPattern = /^\s*modifier\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(([^)]*)\)/;
        for (let i = 0; i < lines.length; i++) {
            const match = lines[i].match(modifierPattern);
            if (match) {
                const name = match[1];
                const params = match[2].trim();
                const item = new vscode.CompletionItem(name, vscode.CompletionItemKind.Method);
                item.detail = `modifier ${name}(${params}) (same file)`;
                const paramNames = params.split(',').map(p => p.trim().split(':')[0].trim()).filter(Boolean);
                const paramSnippets = paramNames.map((p, idx) => `\${${idx + 1}:${p}}`);
                item.insertText = paramSnippets.length > 0
                    ? new vscode.SnippetString(`${name}(${paramSnippets.join(', ')})`)
                    : new vscode.SnippetString(name);
                items.push(item);
            }
        }
        return items;
    }

    private formatFunctionSignature(func: IslFunctionDefinition): string {
        const params = func.parameters.map(p => {
            let result = `${p.name}`;
            if (p.type) {
                result += `: ${p.type}`;
            }
            if (p.optional) {
                result += '?';
            }
            return result;
        }).join(', ');
        
        return `function ${func.name}(${params})${func.returns?.type ? ': ' + func.returns.type : ''} (custom)`;
    }

    private formatFunctionDocumentation(func: IslFunctionDefinition): vscode.MarkdownString {
        const md = new vscode.MarkdownString();
        md.isTrusted = true;
        
        if (func.description) {
            md.appendMarkdown(`${func.description}\n\n`);
        }
        
        if (func.parameters.length > 0) {
            md.appendMarkdown('**Parameters:**\n');
            for (const param of func.parameters) {
                const optional = param.optional ? ' (optional)' : '';
                const type = param.type ? `: ${param.type}` : '';
                const desc = param.description ? ` - ${param.description}` : '';
                md.appendMarkdown(`- \`${param.name}${type}\`${optional}${desc}\n`);
            }
            md.appendMarkdown('\n');
        }
        
        if (func.returns) {
            md.appendMarkdown('**Returns:**');
            if (func.returns.type) {
                md.appendMarkdown(` \`${func.returns.type}\``);
            }
            if (func.returns.description) {
                md.appendMarkdown(` - ${func.returns.description}`);
            }
            md.appendMarkdown('\n\n');
        }
        
        if (func.examples && func.examples.length > 0) {
            md.appendMarkdown('**Examples:**\n');
            for (const example of func.examples) {
                md.appendMarkdown('```isl\n' + example + '\n```\n');
            }
        }
        
        md.appendMarkdown('\n*Custom function from .islextensions*');
        
        return md;
    }

    private getDocumentationForFunction(lines: string[], functionLineIndex: number): string | undefined {
        // Look for comments immediately above the function
        const docs: string[] = [];
        for (let i = functionLineIndex - 1; i >= 0; i--) {
            const line = lines[i].trim();
            if (line.startsWith('//')) {
                docs.unshift(line.substring(2).trim());
            } else if (line.startsWith('#')) {
                docs.unshift(line.substring(1).trim());
            } else if (line === '') {
                // Allow blank lines
                continue;
            } else {
                // Stop at first non-comment, non-blank line
                break;
            }
        }
        return docs.length > 0 ? docs.join('\n') : undefined;
    }

    private getKeywordCompletions(): vscode.CompletionItem[] {
        const keywords = [
            { label: 'fun', kind: vscode.CompletionItemKind.Keyword, detail: 'Function declaration', insertText: 'fun ${1:name}(${2:\\$param}) {\n\t${3:// body}\n\treturn ${4:value}\n}' },
            { label: 'modifier', kind: vscode.CompletionItemKind.Keyword, detail: 'Modifier function', insertText: 'modifier ${1:name}(${2:\\$value}) {\n\treturn ${3:\\$value}\n}' },
            { label: 'if', kind: vscode.CompletionItemKind.Keyword, detail: 'If statement', insertText: 'if (${1:condition})\n\t${2:// true block}\nendif' },
            { label: 'ifelse', kind: vscode.CompletionItemKind.Keyword, detail: 'If-else statement', insertText: 'if (${1:condition})\n\t${2:// true block}\nelse\n\t${3:// false block}\nendif' },
            { label: 'foreach', kind: vscode.CompletionItemKind.Keyword, detail: 'ForEach loop', insertText: 'foreach ${1:\\$item} in ${2:\\$array}\n\t${3:// loop body}\nendfor' },
            { label: 'while', kind: vscode.CompletionItemKind.Keyword, detail: 'While loop', insertText: 'while (${1:condition})\n\t${2:// loop body}\nendwhile' },
            { label: 'switch', kind: vscode.CompletionItemKind.Keyword, detail: 'Switch statement', insertText: 'switch (${1:\\$var})\n\t${2:value} -> ${3:result};\n\telse -> ${4:default};\nendswitch' },
            { label: 'return', kind: vscode.CompletionItemKind.Keyword, detail: 'Return statement', insertText: 'return ${1:value}' },
            { label: 'import', kind: vscode.CompletionItemKind.Keyword, detail: 'Import module', insertText: 'import ${1:Module} from \'${2:file.isl}\';' },
            { label: 'type', kind: vscode.CompletionItemKind.Keyword, detail: 'Type declaration (inline)', insertText: 'type ${1:TypeName} as {\n\t${2:prop}: ${3:String}\n};' },
            { label: 'type from', kind: vscode.CompletionItemKind.Keyword, detail: 'Type from schema URL', insertText: 'type ${1:ns}:${2:TypeName} from \'${3:https://...}\';' },
            { label: 'parallel', kind: vscode.CompletionItemKind.Keyword, detail: 'Parallel execution', insertText: 'parallel ' },
            { label: 'cache', kind: vscode.CompletionItemKind.Keyword, detail: 'Cache function result', insertText: 'cache ' },
        ];

        return keywords.map(k => {
            const item = new vscode.CompletionItem(k.label, k.kind);
            item.detail = k.detail;
            if (k.insertText) {
                item.insertText = new vscode.SnippetString(k.insertText);
            }
            return item;
        });
    }

    /** @. -> built-in services (Date, Math, This, ...) + global extension function names (called as @.name()) */
    private getServiceCompletions(extensions: import('./extensions').IslExtensions): vscode.CompletionItem[] {
        const servicesMap = getServicesMap();
        const items: vscode.CompletionItem[] = [];
        items.push(new vscode.CompletionItem('This', vscode.CompletionItemKind.Class));
        const thisInfo = servicesMap.get('This');
        if (thisInfo) {
            items[0].detail = thisInfo.detail;
            items[0].documentation = thisInfo.documentation ? new vscode.MarkdownString(thisInfo.documentation) : undefined;
        }
        items[0].insertText = 'This';

        for (const [name, info] of servicesMap) {
            if (name === 'This') continue;
            const item = new vscode.CompletionItem(name, vscode.CompletionItemKind.Class);
            item.detail = info.detail;
            item.documentation = info.documentation ? new vscode.MarkdownString(info.documentation) : undefined;
            item.insertText = name;
            items.push(item);
        }

        // Global extension functions: called directly as @.functionName(), same as built-ins
        for (const [name, funcDef] of extensions.functions) {
            const item = new vscode.CompletionItem(name, vscode.CompletionItemKind.Function);
            item.detail = this.formatFunctionSignature(funcDef);
            item.documentation = this.formatFunctionDocumentation(funcDef);
            const paramSnippets = funcDef.parameters.map((param, idx) => `\${${idx + 1}:${param.name}}`);
            item.insertText = paramSnippets.length > 0
                ? new vscode.SnippetString(`${name}(${paramSnippets.join(', ')})`)
                : new vscode.SnippetString(`${name}()`);
            items.push(item);
        }
        return items;
    }

    private getServiceMethodCompletions(serviceName: string, methodPrefix: string = '', extensions?: import('./extensions').IslExtensions): vscode.CompletionItem[] {
        const byNamespace = getFunctionsByNamespace();
        let methods = byNamespace.get(serviceName);
        // If no built-in namespace and this is a global extension function name, no "methods" (call is @.name())
        if (!methods && extensions?.functions.has(serviceName)) {
            return [];
        }
        if (!methods) {
            return [];
        }

        const filteredMethods = methodPrefix
            ? methods.filter(m => m.name.toLowerCase().startsWith(methodPrefix.toLowerCase()))
            : methods;

        return filteredMethods.map((m: BuiltInFunction) => {
            const item = new vscode.CompletionItem(m.name, vscode.CompletionItemKind.Method);
            item.detail = this.formatBuiltInFunctionDetail(m);
            let docText = m.documentation ?? '';
            if (m.returns) {
                if (docText) docText += '\n\n';
                docText += '**Returns:**';
                if (m.returns.type) docText += ` \`${m.returns.type}\``;
                if (m.returns.description) docText += ` - ${m.returns.description}`;
            }
            if (docText) {
                item.documentation = new vscode.MarkdownString(docText);
            }

            const params = m.params || '()';
            const paramMatch = params.match(/\(([^)]*)\)/);
            if (paramMatch && paramMatch[1]) {
                const paramList = paramMatch[1];
                if (paramList === '...') {
                    item.insertText = new vscode.SnippetString(`${m.name}(\${1:arg1}, \${2:arg2})`);
                } else if (paramList.includes(',')) {
                    const snippetParams = paramList.split(',').map((p, idx) => {
                        const paramName = p.trim();
                        return `\${${idx + 1}:${paramName}}`;
                    });
                    item.insertText = new vscode.SnippetString(`${m.name}(${snippetParams.join(', ')})`);
                } else if (paramList) {
                    item.insertText = new vscode.SnippetString(`${m.name}(\${1:${paramList.trim()}})`);
                } else {
                    item.insertText = new vscode.SnippetString(`${m.name}()`);
                }
            } else {
                item.insertText = new vscode.SnippetString(`${m.name}()`);
            }

            return item;
        });
    }

    /** | -> built-in modifiers + same-file modifiers + global extension modifiers */
    private getModifierCompletions(document: vscode.TextDocument, extensions: import('./extensions').IslExtensions): vscode.CompletionItem[] {
        const modifiersMap = getModifiersMap();
        const completionItems: vscode.CompletionItem[] = [];

        // Same-file modifiers first (shortcut: | name)
        const sameFileModifiers = this.getSameFileModifiers(document);
        for (const mod of sameFileModifiers) {
            completionItems.push(mod);
        }

        for (const [_name, m] of modifiersMap) {
            const item = new vscode.CompletionItem(m.name, vscode.CompletionItemKind.Method);
            item.detail = this.formatBuiltInModifierDetail(m);
            item.insertText = new vscode.SnippetString(m.insertText);
            let docText = m.documentation ?? '';
            if (m.returns) {
                if (docText) docText += '\n\n';
                docText += '**Returns:**';
                if (m.returns.type) docText += ` \`${m.returns.type}\``;
                if (m.returns.description) docText += ` - ${m.returns.description}`;
            }
            if (docText) {
                item.documentation = new vscode.MarkdownString(docText);
            }
            completionItems.push(item);
        }

        // Add custom modifiers from extensions
        for (const [name, modDef] of extensions.modifiers) {
            const item = new vscode.CompletionItem(name, vscode.CompletionItemKind.Method);
            item.detail = this.formatModifierSignature(modDef);
            
            // Create snippet with parameters
            const paramSnippets = modDef.parameters.map((param, idx) => {
                if (param.defaultValue) {
                    return `\${${idx + 1}:${param.defaultValue}}`;
                }
                return `\${${idx + 1}:${param.name}}`;
            });
            
            if (paramSnippets.length > 0) {
                item.insertText = new vscode.SnippetString(`${name}(${paramSnippets.join(', ')})`);
            } else {
                item.insertText = new vscode.SnippetString(name);
            }
            
            // Add documentation
            item.documentation = this.formatModifierDocumentation(modDef);
            
            completionItems.push(item);
        }

        return completionItems;
    }

    private formatBuiltInModifierDetail(m: BuiltInModifier): string {
        const base = m.detail ?? m.name;
        return m.returns?.type ? `${base} → ${m.returns.type}` : base;
    }

    private formatBuiltInFunctionDetail(m: BuiltInFunction): string {
        const base = m.detail ?? m.name;
        return m.returns?.type ? `${base} → ${m.returns.type}` : base;
    }

    private formatModifierSignature(mod: IslModifierDefinition): string {
        if (mod.parameters.length === 0) {
            return `modifier ${mod.name} (custom)`;
        }
        
        const params = mod.parameters.map(p => {
            let result = `${p.name}`;
            if (p.type) {
                result += `: ${p.type}`;
            }
            if (p.optional) {
                result += '?';
            }
            return result;
        }).join(', ');
        
        return `modifier ${mod.name}(${params})${mod.returns?.type ? ': ' + mod.returns.type : ''} (custom)`;
    }

    private formatModifierDocumentation(mod: IslModifierDefinition): vscode.MarkdownString {
        const md = new vscode.MarkdownString();
        md.isTrusted = true;
        
        if (mod.description) {
            md.appendMarkdown(`${mod.description}\n\n`);
        }
        
        if (mod.parameters.length > 0) {
            md.appendMarkdown('**Parameters:**\n');
            for (const param of mod.parameters) {
                const optional = param.optional ? ' (optional)' : '';
                const type = param.type ? `: ${param.type}` : '';
                const desc = param.description ? ` - ${param.description}` : '';
                const defaultVal = param.defaultValue ? ` (default: ${param.defaultValue})` : '';
                md.appendMarkdown(`- \`${param.name}${type}\`${optional}${defaultVal}${desc}\n`);
            }
            md.appendMarkdown('\n');
        }
        
        if (mod.returns) {
            md.appendMarkdown('**Returns:**');
            if (mod.returns.type) {
                md.appendMarkdown(` \`${mod.returns.type}\``);
            }
            if (mod.returns.description) {
                md.appendMarkdown(` - ${mod.returns.description}`);
            }
            md.appendMarkdown('\n\n');
        }
        
        if (mod.examples && mod.examples.length > 0) {
            md.appendMarkdown('**Examples:**\n');
            for (const example of mod.examples) {
                md.appendMarkdown('```isl\n' + example + '\n```\n');
            }
        }
        
        md.appendMarkdown('\n*Custom modifier from .islextensions*');
        
        return md;
    }

    private getVariableCompletions(document: vscode.TextDocument): vscode.CompletionItem[] {
        const variables: Map<string, vscode.CompletionItem> = new Map();
        const text = document.getText();
        
        // Find all variable declarations and usages
        const varPattern = /\$([a-zA-Z_][a-zA-Z0-9_]*)/g;
        let match;
        
        while ((match = varPattern.exec(text)) !== null) {
            const varName = match[1];
            if (!variables.has(varName)) {
                const item = new vscode.CompletionItem('$' + varName, vscode.CompletionItemKind.Variable);
                item.detail = 'Variable';
                item.insertText = '$' + varName; // Include $ in the insert text
                item.filterText = varName; // Filter without $ for better matching
                variables.set(varName, item);
            }
        }

        // Also add common input variable
        if (!variables.has('input')) {
            const inputItem = new vscode.CompletionItem('$input', vscode.CompletionItemKind.Variable);
            inputItem.detail = 'Input parameter';
            inputItem.insertText = '$input';
            inputItem.filterText = 'input';
            variables.set('input', inputItem);
        }

        return Array.from(variables.values());
    }

    /**
     * Completions when cursor is inside a typed object literal: $var : TypeName = { ... }
     * Offers "Fill all mandatory fields" snippet and property name completions from the schema.
     */
    private getTypeBasedCompletions(
        document: vscode.TextDocument,
        position: vscode.Position,
        schema: SchemaInfo,
        linePrefix: string
    ): vscode.CompletionItem[] {
        const items: vscode.CompletionItem[] = [];
        const existingProps = this.getExistingPropertiesInObjectLiteral(document, position);
        const indent = this.getIndentAtPosition(document, position);
        const indentUnit = this.getIndentUnit(document, indent);
        const requiredSet = new Set(schema.required);
        const missingRequired = schema.required.filter(p => !existingProps.has(p));

        // "Fill all mandatory fields" – insert only required properties not yet present
        if (missingRequired.length > 0) {
            const fillItem = new vscode.CompletionItem('Fill all mandatory fields', vscode.CompletionItemKind.Snippet);
            fillItem.detail = `Insert required: ${missingRequired.join(', ')}`;
            fillItem.documentation = new vscode.MarkdownString('Inserts all required properties from the schema with placeholders.');
            const snippetParts: string[] = [];
            let tabIndex = 1;
            for (const prop of missingRequired) {
                const propInfo = schema.properties[prop];
                if (propInfo?.schema) {
                    snippetParts.push(`${indentUnit}${prop}: {\n${indent}${indentUnit}\${${tabIndex++}:}\n${indent}}`);
                } else if (propInfo?.enum?.length) {
                    snippetParts.push(`${indentUnit}${prop}: "\${${tabIndex++}:${propInfo.enum[0]}}"`);
                } else {
                    const placeholder = this.schemaTypeToPlaceholder(propInfo?.type ?? 'any');
                    snippetParts.push(`${indentUnit}${prop}: \${${tabIndex++}:${placeholder}}`);
                }
            }
            fillItem.insertText = new vscode.SnippetString(snippetParts.join(`\n${indent}`));
            items.push(fillItem);
        }

        // Value completion: after "propName: " show enum values for that property
        const valueCompletion = this.getEnumValueCompletions(linePrefix, schema, indent);
        if (valueCompletion.length > 0) {
            return valueCompletion;
        }

        // Property name completions (exclude already declared in this block)
        const propPrefix = this.getPropertyNamePrefix(linePrefix);
        for (const [propName, propInfo] of Object.entries(schema.properties)) {
            if (existingProps.has(propName)) continue;
            if (propPrefix && !propName.toLowerCase().startsWith(propPrefix.toLowerCase())) continue;
            const isRequired = requiredSet.has(propName);
            if (propInfo.enum && propInfo.enum.length > 0) {
                for (const enumVal of propInfo.enum) {
                    const item = new vscode.CompletionItem(`${propName}: ${enumVal}`, vscode.CompletionItemKind.EnumMember);
                    item.detail = `${propInfo.type ?? 'string'}${isRequired ? ' (required)' : ' (optional)'}`;
                    if (propInfo.description) item.documentation = new vscode.MarkdownString(propInfo.description);
                    item.insertText = new vscode.SnippetString(`${propName}: "${enumVal}"`);
                    item.filterText = `${propName} ${enumVal}`;
                    items.push(item);
                }
            } else {
                const item = new vscode.CompletionItem(propName, vscode.CompletionItemKind.Property);
                item.detail = `${propInfo.type ?? 'any'}${isRequired ? ' (required)' : ' (optional)'}`;
                if (propInfo.description) item.documentation = new vscode.MarkdownString(propInfo.description);
                if (propInfo.schema) {
                    item.insertText = new vscode.SnippetString(`${propName}: {\n${indent}${indentUnit}$0\n${indent}}`);
                } else {
                    item.insertText = new vscode.SnippetString(`${propName}: \${1:${this.schemaTypeToPlaceholder(propInfo.type ?? 'any')}}`);
                }
                items.push(item);
            }
        }

        return items;
    }

    private schemaTypeToPlaceholder(type: string): string {
        const t = type.toLowerCase();
        if (t === 'string' || t === 'text') return '""';
        if (t === 'number' || t === 'integer') return '0';
        if (t === 'boolean') return 'true';
        if (t === 'date' || t === 'datetime') return '""';
        return '""';
    }

    private getIndentAtPosition(document: vscode.TextDocument, position: vscode.Position): string {
        const line = document.lineAt(position).text;
        const match = line.match(/^\s*/);
        return match ? match[0] : '';
    }

    /** One indent level: tab if file uses tabs, else spaces per editor.tabSize. */
    private getIndentUnit(document: vscode.TextDocument, baseIndent: string): string {
        const config = vscode.workspace.getConfiguration('editor', document.uri);
        const insertSpaces = config.get<boolean>('insertSpaces', true);
        const tabSize = config.get<number>('tabSize', 4);
        if (!insertSpaces || /\t/.test(baseIndent)) return '\t';
        return ' '.repeat(tabSize);
    }

    /** Returns the partial property name being typed (e.g. "acc" from "  acc|") */
    private getPropertyNamePrefix(linePrefix: string): string {
        const m = linePrefix.match(/(?:^|[\s{,]\s*)([a-zA-Z_][a-zA-Z0-9_]*)$/);
        return m ? m[1] : '';
    }

    /** When cursor is after "propName: " or "propName: partial", return enum value completions for that property. */
    private getEnumValueCompletions(linePrefix: string, schema: SchemaInfo, indent: string): vscode.CompletionItem[] {
        const match = linePrefix.match(/([a-zA-Z_][a-zA-Z0-9_]*)\s*:\s*([^\s]*)$/);
        if (!match) return [];
        const propName = match[1];
        const valuePrefix = match[2].replace(/^["']|["']$/g, '');
        const propInfo = schema.properties[propName];
        if (!propInfo?.enum?.length) return [];
        const items: vscode.CompletionItem[] = [];
        for (const enumVal of propInfo.enum) {
            if (valuePrefix && !enumVal.toLowerCase().startsWith(valuePrefix.toLowerCase())) continue;
            const item = new vscode.CompletionItem(enumVal, vscode.CompletionItemKind.EnumMember);
            item.detail = propInfo.type ?? 'string';
            if (propInfo.description) item.documentation = new vscode.MarkdownString(propInfo.description);
            item.insertText = `"${enumVal}"`;
            items.push(item);
        }
        return items;
    }

    /** Collects property names already present in the object literal containing position */
    private getExistingPropertiesInObjectLiteral(document: vscode.TextDocument, position: vscode.Position): Set<string> {
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
                    if (offset >= start && offset <= i) {
                        containingPairs.push({ start, end: i });
                    }
                }
            }
        }
        if (containingPairs.length === 0) return new Set();
        let best = containingPairs[0];
        for (const p of containingPairs) {
            if (p.end - p.start < best.end - best.start) best = p;
        }
        const objectContent = text.substring(best.start + 1, best.end);
        const names = new Set<string>();
        const propRe = /([a-zA-Z_][a-zA-Z0-9_]*)\s*:/g;
        let match;
        while ((match = propRe.exec(objectContent)) !== null) {
            names.add(match[1]);
        }
        return names;
    }
    
    private getPaginationType(document: vscode.TextDocument, varName: string): string | null {
        const text = document.getText();
        const lines = text.split('\n');
        
        // Look for @.Pagination.[Type]( $varName, ... )
        for (const line of lines) {
            const cursorMatch = line.match(/@\.Pagination\.Cursor\s*\(\s*\$([a-zA-Z_][a-zA-Z0-9_]*)/);
            if (cursorMatch && cursorMatch[1] === varName) {
                return 'Cursor';
            }
            
            const offsetMatch = line.match(/@\.Pagination\.Offset\s*\(\s*\$([a-zA-Z_][a-zA-Z0-9_]*)/);
            if (offsetMatch && offsetMatch[1] === varName) {
                return 'Offset';
            }
            
            const pageMatch = line.match(/@\.Pagination\.Page\s*\(\s*\$([a-zA-Z_][a-zA-Z0-9_]*)/);
            if (pageMatch && pageMatch[1] === varName) {
                return 'Page';
            }
            
            const keysetMatch = line.match(/@\.Pagination\.Keyset\s*\(\s*\$([a-zA-Z_][a-zA-Z0-9_]*)/);
            if (keysetMatch && keysetMatch[1] === varName) {
                return 'Keyset';
            }
            
            const dateMatch = line.match(/@\.Pagination\.Date\s*\(\s*\$([a-zA-Z_][a-zA-Z0-9_]*)/);
            if (dateMatch && dateMatch[1] === varName) {
                return 'Date';
            }
        }
        
        return null;
    }
    
    private getPaginationPropertyCompletions(paginationType: string): vscode.CompletionItem[] {
        if (paginationType === 'Cursor') {
            return [
                this.createPaginationProperty(
                    'current',
                    'The value of the current cursor for the current page',
                    'On the first loop this is null.'
                ),
                this.createPaginationProperty(
                    'next',
                    'The next value for the cursor on the next loop',
                    'This needs to be set in the loop. If this value is the same as the previous value (e.g. value was not set) or the value is null, the pagination loop exits.'
                )
            ];
        } else if (paginationType === 'Page') {
            return [
                this.createPaginationProperty(
                    'startIndex',
                    'The starting index as declared in the pagination',
                    'Default is 0. This is the value passed in the pagination configuration.'
                ),
                this.createPaginationProperty(
                    'pageSize',
                    'Size of the page as declared in the pagination',
                    'Default is 100. This is the value passed in the pagination configuration.'
                ),
                this.createPaginationProperty(
                    'page',
                    'Index of the current page',
                    'Starting at startIndex. Increments with each iteration of the pagination loop.'
                ),
                this.createPaginationProperty(
                    'fromOffset',
                    'Start offset for the current page',
                    'Calculated as: page * pageSize'
                ),
                this.createPaginationProperty(
                    'toOffset',
                    'End offset for the current page',
                    'Calculated as: (page + 1) * pageSize'
                ),
                this.createPaginationProperty(
                    'hasMorePages',
                    'Whether there are more pages to fetch',
                    'Set to false by default. In order to continue the pagination loop this needs to be set to true.'
                )
            ];
        } else if (paginationType === 'Date') {
            return [
                this.createPaginationProperty(
                    'startDate',
                    'Start date of the current period',
                    'The beginning of the current pagination period based on the duration.'
                ),
                this.createPaginationProperty(
                    'endDate',
                    'End date of the current period',
                    'The end of the current pagination period based on the duration.'
                ),
                this.createPaginationProperty(
                    'page',
                    'Zero-based page index for the current page',
                    'Increments with each iteration: 0, 1, 2, ...'
                )
            ];
        }
        
        // Placeholder for other pagination types (Offset, Keyset)
        return [];
    }
    
    private createPaginationProperty(name: string, detail: string, docs: string): vscode.CompletionItem {
        const item = new vscode.CompletionItem(name, vscode.CompletionItemKind.Property);
        item.detail = detail;
        item.documentation = new vscode.MarkdownString(docs);
        item.insertText = name;
        return item;
    }
}
