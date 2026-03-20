import * as vscode from 'vscode';
import { IslExtensionsManager, getExtensionFunction, getExtensionModifier } from './extensions';
import { getModifiersMap, getServicesMap, type BuiltInModifier } from './language';
import { IslTypeManager } from './types';
import type { SchemaInfo, SchemaProperty, TypeDeclaration } from './types';

export class IslHoverProvider implements vscode.HoverProvider {

    constructor(
        private extensionsManager: IslExtensionsManager,
        private typeManager?: IslTypeManager
    ) {}
    
    async provideHover(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken
    ): Promise<vscode.Hover | undefined> {
        const range = document.getWordRangeAtPosition(position);
        if (!range) {
            return undefined;
        }

        let word = document.getText(range);
        const line = document.lineAt(position.line).text;
        const offset = document.offsetAt(position);

        // Load custom extensions
        const extensions = await this.extensionsManager.getExtensionsForDocument(document);

        // If we're in an @.identifier context, resolve the exact identifier at cursor (more reliable than word).
        // Use dotted pattern so @.Call.Api() resolves to "Call.Api" when hovering over Call or Api.
        const lineStartOffset = document.offsetAt(new vscode.Position(position.line, 0));
        const col = offset - lineStartOffset;
        const atDotIdentifierPattern = /@\.\s*([a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*)/g;
        for (const m of line.matchAll(atDotIdentifierPattern)) {
            const start = m.index! + m[0].indexOf(m[1]);
            const end = start + m[1].length;
            if (col >= start && col <= end) {
                word = m[1];
                break;
            }
        }

        // Check what kind of token we're hovering over
        if (this.isKeyword(word)) {
            return this.getKeywordHover(word);
        }
        // @.word or @.Module.func – built-in service (Date, Math) or global extension (sendEmail, Call.Api, …)
        if (line.includes('@.' + word) || line.includes('@. ' + word)) {
            const extFunc = getExtensionFunction(extensions, word);
            if (extFunc) {
                return this.getCustomFunctionHover(extFunc);
            }
            // For compound names (e.g. Date.Now), show service hover only for the first part
            const serviceWord = word.includes('.') ? word.split('.')[0] : word;
            return this.getServiceHover(serviceWord);
        }
        if (this.isModifier(word, line)) {
            const extMod = getExtensionModifier(extensions, word);
            if (extMod) {
                return this.getCustomModifierHover(extMod);
            }
            return this.getModifierHover(word, line);
        }
        if (line.includes('$' + word)) {
            return this.getVariableHover(word, document);
        }
        // @.This.word – same-file function
        if (line.includes('@.This.' + word)) {
            const extFunc = getExtensionFunction(extensions, word);
            if (extFunc) {
                return this.getCustomFunctionHover(extFunc);
            }
        }

        // Type name (e.g. idx:name in "type idx:name from '...'" or "$var: idx:name = { ... }")
        const typeAtPos = this.getTypeNameAtPosition(line, col);
        if (typeAtPos && this.typeManager) {
            const decl = this.typeManager.getDeclarationForType(document, typeAtPos.typeName);
            if (decl) {
                const schema = await this.typeManager.getSchemaForType(document, typeAtPos.typeName);
                const hoverRange = new vscode.Range(
                    position.line, typeAtPos.start,
                    position.line, typeAtPos.end
                );
                return this.getTypeHover(decl, schema, typeAtPos.typeName, hoverRange);
            }
        }

        // Property name inside typed object literal ($var : Type = { propName: ... })
        if (this.typeManager && range) {
            const schemaAt = await this.typeManager.getSchemaForObjectAt(document, position);
            if (schemaAt) {
                const prop = schemaAt.schema.properties[word];
                if (prop) {
                    return this.getPropertyHover(word, prop, schemaAt.schema, range);
                }
            }
        }

        return undefined;
    }

    /**
     * Finds the full type name at the given column (e.g. idx:name) if the cursor is inside one.
     * Type names appear after "type " or after ": " in variable declarations.
     */
    private getTypeNameAtPosition(line: string, col: number): { typeName: string; start: number; end: number } | null {
        // After "type " (declaration)
        const typeDeclRegex = /\btype\s+([a-zA-Z_][a-zA-Z0-9_.:]*)\s+(?:from|as)\b/g;
        let m: RegExpExecArray | null;
        while ((m = typeDeclRegex.exec(line)) !== null) {
            const start = m.index + m[0].indexOf(m[1]);
            const end = start + m[1].length;
            if (col >= start && col <= end) {
                return { typeName: m[1], start, end };
            }
        }
        // After ": " (type annotation, e.g. $var: idx:name = or $var: idx:name)
        const typeAnnotRegex = /:\s*([a-zA-Z_][a-zA-Z0-9_.:]*)(?:\s*[={]|\s*$)/g;
        while ((m = typeAnnotRegex.exec(line)) !== null) {
            const start = m.index + m[0].indexOf(m[1]);
            const end = start + m[1].length;
            if (col >= start && col <= end) {
                return { typeName: m[1], start, end };
            }
        }
        return null;
    }

    private getPropertyHover(propName: string, prop: SchemaProperty, schema: SchemaInfo, range: vscode.Range): vscode.Hover {
        const md = new vscode.MarkdownString();
        md.isTrusted = true;
        const typeStr = prop.type ?? 'any';
        const required = schema.required?.includes(propName) ? ' *(required)*' : '';
        md.appendMarkdown(`**\`${propName}\`** \`${typeStr}\`${required}\n\n`);

        if (prop.description) {
            md.appendMarkdown(`${prop.description}\n\n`);
        }

        if (prop.enum && prop.enum.length > 0) {
            md.appendMarkdown(`**Allowed values:** \`${prop.enum.join('`, `')}\`\n\n`);
        }

        const examples: string[] = [];
        if (prop.example) examples.push(prop.example);
        if (prop.examples) examples.push(...prop.examples);
        if (examples.length > 0) {
            md.appendMarkdown('**Examples:**\n');
            for (const ex of examples) {
                md.appendMarkdown('```\n' + ex + '\n```\n');
            }
            md.appendMarkdown('\n');
        }

        if (prop.extensions && Object.keys(prop.extensions).length > 0) {
            md.appendMarkdown('**Extensions:**\n');
            for (const [k, v] of Object.entries(prop.extensions)) {
                const valStr = typeof v === 'object' && v !== null
                    ? JSON.stringify(v, null, 2)
                    : String(v);
                if (valStr.includes('\n')) {
                    md.appendMarkdown(`- \`${k}\`:\n\`\`\`\n${valStr}\n\`\`\`\n`);
                } else {
                    md.appendMarkdown(`- \`${k}\`: ${valStr}\n`);
                }
            }
        }

        return new vscode.Hover(md, range);
    }

    private getTypeHover(decl: TypeDeclaration, schema: SchemaInfo | null, typeName: string, range: vscode.Range): vscode.Hover {
        const md = new vscode.MarkdownString();
        md.isTrusted = true;
        md.appendMarkdown(`**\`${typeName}\`** *(type)*\n\n`);
        if (decl.source === 'url' && decl.url) {
            md.appendMarkdown(`**From:** [${decl.url}](${decl.url})\n\n`);
        } else {
            md.appendMarkdown('**From:** inline definition\n\n');
        }
        if (schema && Object.keys(schema.properties).length > 0) {
            md.appendMarkdown('**Properties:**\n');
            for (const [propName, prop] of Object.entries(schema.properties)) {
                const typeStr = prop.type ?? 'any';
                const required = schema.required?.includes(propName) ? ' *(required)*' : '';
                md.appendMarkdown(`- \`${propName}\`: \`${typeStr}\`${required}\n`);
            }
        } else if (schema) {
            md.appendMarkdown('*No properties.*\n');
        }
        return new vscode.Hover(md, range);
    }

    private getCustomFunctionHover(func: import('./extensions').IslFunctionDefinition): vscode.Hover {
        const md = new vscode.MarkdownString();
        md.isTrusted = true;
        
        // Build signature
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
        
        const returnType = func.returns?.type ? `: ${func.returns.type}` : '';
        md.appendMarkdown(`**\`${func.name}(${params})${returnType}\`** *(custom function)*\n\n`);
        
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
        
        md.appendMarkdown('\n---\n*Defined in .islextensions*');
        
        return new vscode.Hover(md);
    }

    private getCustomModifierHover(mod: import('./extensions').IslModifierDefinition): vscode.Hover {
        const md = new vscode.MarkdownString();
        md.isTrusted = true;
        
        // Build signature
        if (mod.parameters.length > 0) {
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
            
            const returnType = mod.returns?.type ? `: ${mod.returns.type}` : '';
            md.appendMarkdown(`**\`${mod.name}(${params})${returnType}\`** *(custom modifier)*\n\n`);
        } else {
            md.appendMarkdown(`**\`${mod.name}\`** *(custom modifier)*\n\n`);
        }
        
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
        
        md.appendMarkdown('\n---\n*Defined in .islextensions*');
        
        return new vscode.Hover(md);
    }

    private isKeyword(word: string): boolean {
        const keywords = ['fun', 'modifier', 'if', 'else', 'endif', 'foreach', 'endfor', 'while', 'endwhile', 
                         'switch', 'endswitch', 'return', 'import', 'type', 'as', 'from', 'in', 'cache', 'parallel',
                         'filter', 'map', 'and', 'or', 'not', 'contains', 'startsWith', 'endsWith', 'matches', 'is'];
        return keywords.includes(word);
    }

    private isModifier(word: string, line: string): boolean {
        return line.includes('|') && (line.includes(word + '(') || line.includes(word + ' ') || 
               line.includes('| ' + word) || line.includes('|' + word) || 
               new RegExp(`\\|\\s*${word}\\b`).test(line));
    }

    private getKeywordHover(word: string): vscode.Hover {
        const docs: { [key: string]: string } = {
            'fun': '**Function Declaration**\n\nDefines a function that can be called within ISL.\n\n```isl\nfun myFunction($param) {\n    return $param | upperCase\n}\n\n// Call it:\n$result: @.This.myFunction($value);\n```',
            'modifier': '**Modifier Function**\n\nDefines a custom modifier that can be used with the pipe operator.\n\n```isl\nmodifier double($value) {\n    return {{ $value * 2 }}\n}\n\n// Use it:\n$result: $input | double;\n```',
            'if': '**If Statement**\n\nConditional execution based on a boolean expression.\n\n**Block form** (requires `endif`):\n```isl\nif ($value > 10)\n    result: "high"\nelse\n    result: "low"\nendif\n```\n\n**Inline / conditional modifier** (optional `endif` when used in assignment or property):\n```isl\n$status: if ($active) "active" else "inactive";\n$val: if ($ok) $a else $b;\n{ prop: if ($x) "yes" else "no" }\n```',
            'foreach': '**ForEach Loop**\n\nIterates over an array and transforms each element.\n\n```isl\nforeach $item in $array\n    { id: $item.id, name: $item.name }\nendfor\n```\n\n**With filtering:**\n```isl\nforeach $item in $array | filter($item.active)\n    $item.name | upperCase\nendfor\n```',
            'while': '**While Loop**\n\nRepeats a block while a condition is true. Max 50 iterations by default.\n\n```isl\n$counter = 0;\nwhile ($counter < 10)\n    $counter = {{ $counter + 1 }}\nendwhile\n```\n\n**With options:**\n```isl\nwhile ($condition, {maxLoops: 100})\n    // statements\nendwhile\n```',
            'switch': '**Switch Statement**\n\nMatches a value against multiple cases.\n\n```isl\nswitch ($status)\n    "active" -> "Active";\n    "pending" -> "Pending";\n    /^temp.*/ -> "Temporary";\n    < 100 -> "Low";\n    else -> "Unknown";\nendswitch\n```',
            'return': '**Return Statement**\n\nReturns a value from a function. Functions must always return a value.\n\n```isl\nfun calculate($x) {\n    return {{ $x * 2 }}\n}\n```\n\n**Note:** Use `return {};` to return empty object, not `return;`',
            'import': '**Import Statement**\n\nImports functions and types from another ISL file.\n\n```isl\nimport Common from \'common.isl\';\nimport Utils from \'../utils.isl\';\n\n$result: @.Common.someFunction();\n$value: $input | Common.someModifier;\n```',
            'type': '**Type Declaration**\n\nDefines a custom type for validation.\n\n```isl\ntype Address as { \n    street: String, \n    city: String,\n    zip: String \n};\n\ntype User as {\n    name: String,\n    address: Address\n};\n```',
            'parallel': '**Parallel Execution**\n\nExecutes a foreach loop in parallel for better performance.\n\n```isl\nparallel foreach $item in $array\n    @.Service.process($item)\nendfor\n\nparallel {threads: 10} foreach $item in $largeArray\n    // processing\nendfor\n```',
            'cache': '**Cache Decorator**\n\nCaches the result of a function call based on parameters.\n\n```isl\ncache fun expensiveOperation($param) {\n    // result is cached\n    return @.Service.slowCall($param)\n}\n```',
            'filter': '**Filter Modifier**\n\nFilters an array based on a condition.\n\n```isl\n$active: $items | filter($item.status == "active");\n$highValue: $orders | filter($order.total > 1000);\n```',
            'map': '**Map Modifier**\n\nTransforms each element of an array.\n\n```isl\n$names: $users | map($user.name);\n$totals: $orders | map({{ $order.qty * $order.price }});\n```',
        };

        const markdown = new vscode.MarkdownString(docs[word] || `**${word}**\n\nISL keyword`);
        markdown.isTrusted = true;
        return new vscode.Hover(markdown);
    }

    private getServiceHover(word: string): vscode.Hover {
        const servicesMap = getServicesMap();
        const info = servicesMap.get(word);
        const markdown = new vscode.MarkdownString(
            info?.documentation ? `**@.${word}**\n\n${info.documentation}` : `**@.${word}**\n\nISL service`
        );
        markdown.isTrusted = true;
        return new vscode.Hover(markdown);
    }

    /** Resolve full modifier name from line (e.g. "fromEpochSeconds" in "| date.fromEpochSeconds" -> "date.fromEpochSeconds"). */
    private resolveFullModifierName(line: string, word: string): string {
        const modifiersMap = getModifiersMap();
        if (modifiersMap.has(word)) return word;
        const modifierPattern = /\|\s*([a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*)\s*(?:\(|$|\|)/g;
        let match: RegExpExecArray | null;
        while ((match = modifierPattern.exec(line)) !== null) {
            const fullName = match[1];
            if (fullName === word || fullName.endsWith('.' + word)) {
                return fullName;
            }
        }
        return word;
    }

    private getModifierHover(word: string, line: string): vscode.Hover {
        const modifiersMap = getModifiersMap();
        const fullName = this.resolveFullModifierName(line, word);
        const mod = modifiersMap.get(fullName);
        if (mod && mod.hover) {
            const info = mod.hover;
            let markdown = `**\`${fullName}\`** modifier\n\n${info.description}`;
            if (info.signature) {
                markdown += `\n\n**Signature:** \`${info.signature}\``;
            }
            if (mod.returns) {
                markdown += `\n\n**Returns:**`;
                if (mod.returns.type) markdown += ` \`${mod.returns.type}\``;
                if (mod.returns.description) markdown += ` - ${mod.returns.description}`;
                markdown += '\n';
            }
            if (info.example) {
                markdown += `\n\n**Example:**\n\`\`\`isl\n${info.example}\n\`\`\``;
            }
            const md = new vscode.MarkdownString(markdown);
            md.isTrusted = true;
            return new vscode.Hover(md);
        }
        if (mod) {
            let markdown = `**\`${fullName}\`** modifier`;
            if (mod.detail) markdown += `\n\n${mod.detail}`;
            if (mod.returns) {
                markdown += `\n\n**Returns:**`;
                if (mod.returns.type) markdown += ` \`${mod.returns.type}\``;
                if (mod.returns.description) markdown += ` - ${mod.returns.description}`;
                markdown += '\n';
            }
            if (mod.documentation) markdown += `\n\n${mod.documentation}`;
            const md = new vscode.MarkdownString(markdown);
            md.isTrusted = true;
            return new vscode.Hover(md);
        }

        const markdown = new vscode.MarkdownString(`**\`${word}\`** modifier`);
        markdown.isTrusted = true;
        return new vscode.Hover(markdown);
    }

    private getVariableHover(word: string, document: vscode.TextDocument): vscode.Hover {
        const markdown = new vscode.MarkdownString(`**$${word}**\n\nVariable`);
        markdown.isTrusted = true;
        return new vscode.Hover(markdown);
    }
}
