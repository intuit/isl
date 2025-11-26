import * as vscode from 'vscode';
import { IslExtensionsManager, IslFunctionDefinition, IslModifierDefinition } from './extensions';

export class IslCompletionProvider implements vscode.CompletionItemProvider {
    
    constructor(private extensionsManager: IslExtensionsManager) {}
    
    async provideCompletionItems(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken,
        context: vscode.CompletionContext
    ): Promise<vscode.CompletionItem[] | vscode.CompletionList> {
        const linePrefix = document.lineAt(position).text.substr(0, position.character);
        
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
        
        // Check for @.This. - show functions from current file + custom extensions
        if (linePrefix.match(/@\.This\.[\w]*$/)) {
            return this.getFunctionsFromDocument(document, extensions);
        }
        // Provide different completions based on context
        else if (linePrefix.endsWith('@.')) {
            return this.getServiceCompletions();
        } else if (linePrefix.match(/\|\s*[\w.]*$/)) {
            return this.getModifierCompletions(extensions);
        } else if (linePrefix.match(/\$\w*$/)) {
            return this.getVariableCompletions(document);
        } else {
            return this.getKeywordCompletions();
        }
    }

    private getFunctionsFromDocument(document: vscode.TextDocument, extensions: import('./extensions').IslExtensions): vscode.CompletionItem[] {
        const functions: vscode.CompletionItem[] = [];
        const text = document.getText();
        const lines = text.split('\n');
        
        // Find all function and modifier declarations
        const functionPattern = /^\s*(fun|modifier)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(([^)]*)\)/;
        
        for (let i = 0; i < lines.length; i++) {
            const match = lines[i].match(functionPattern);
            if (match) {
                const funcType = match[1]; // 'fun' or 'modifier'
                const funcName = match[2];
                const params = match[3].trim();
                
                const item = new vscode.CompletionItem(funcName, vscode.CompletionItemKind.Function);
                item.detail = `${funcType} ${funcName}(${params})`;
                
                // Extract parameter names for snippet
                const paramNames = params
                    .split(',')
                    .map(p => p.trim())
                    .filter(p => p.length > 0)
                    .map((p, idx) => {
                        // Extract just the parameter name (remove type annotations if present)
                        const paramName = p.split(':')[0].trim();
                        return `\${${idx + 1}:${paramName}}`;
                    });
                
                // Create snippet with parameters
                if (paramNames.length > 0) {
                    item.insertText = new vscode.SnippetString(`${funcName}(${paramNames.join(', ')})`);
                } else {
                    item.insertText = new vscode.SnippetString(`${funcName}()`);
                }
                
                // Add documentation from comments above function
                const docs = this.getDocumentationForFunction(lines, i);
                if (docs) {
                    item.documentation = new vscode.MarkdownString(docs);
                }
                
                functions.push(item);
            }
        }
        
        // Add custom functions from extensions
        for (const [name, funcDef] of extensions.functions) {
            const item = new vscode.CompletionItem(name, vscode.CompletionItemKind.Function);
            item.detail = this.formatFunctionSignature(funcDef);
            
            // Create snippet with parameters
            const paramSnippets = funcDef.parameters.map((param, idx) => {
                return `\${${idx + 1}:${param.name}}`;
            });
            
            if (paramSnippets.length > 0) {
                item.insertText = new vscode.SnippetString(`${name}(${paramSnippets.join(', ')})`);
            } else {
                item.insertText = new vscode.SnippetString(`${name}()`);
            }
            
            // Add documentation
            item.documentation = this.formatFunctionDocumentation(funcDef);
            
            // Mark as custom extension
            item.tags = [vscode.CompletionItemTag.Deprecated]; // Using this as a visual indicator
            
            functions.push(item);
        }
        
        return functions;
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
            { label: 'type', kind: vscode.CompletionItemKind.Keyword, detail: 'Type declaration', insertText: 'type ${1:TypeName} as {\n\t${2:prop}: ${3:String}\n};' },
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

    private getServiceCompletions(): vscode.CompletionItem[] {
        const services = [
            { 
                label: 'This', 
                methods: ['<functions in current file>'], 
                detail: 'Call functions in current script',
                documentation: 'Provides access to functions defined in the current ISL file.\n\nType `@.This.` to see available functions.'
            },
            { 
                label: 'Date', 
                methods: ['Now()', 'parse(format)', 'fromEpochSeconds(seconds)', 'fromEpochMillis(millis)'], 
                detail: 'Date/time operations',
                documentation: 'Date and time operations (UTC).\n\n**Methods:**\n- `Now()` - Current date/time\n- `parse(string, format)` - Parse date\n- `fromEpochSeconds(seconds)`\n- `fromEpochMillis(millis)`'
            },
            { 
                label: 'Math', 
                methods: ['sum(initial)', 'average()', 'mean()', 'min()', 'max()', 'mod(divisor)', 'sqrt()', 'clamp(min, max)', 'round()', 'floor()', 'ceil()', 'abs()', 'RandInt(min, max)', 'RandFloat()', 'RandDouble()'], 
                detail: 'Math operations',
                documentation: 'Mathematical operations on arrays and numbers.\n\nUse with arrays: `$total: $prices | Math.sum(0)`\n\nRandom numbers: `Math.RandInt(1, 100)`'
            },
            { 
                label: 'String', 
                methods: ['concat(...)', 'join(array, sep)', 'split(str, sep)', 'replace(str, find, replace)'], 
                detail: 'String operations',
                documentation: 'String manipulation functions.'
            },
            { 
                label: 'Array', 
                methods: ['concat(...)', 'slice(start, end)', 'reverse()', 'flatten()', 'range(start, count, increment)'], 
                detail: 'Array operations',
                documentation: 'Array manipulation functions.\n\n`Array.range(0, 10, 1)` creates array [0..9]'
            },
            { 
                label: 'Json', 
                methods: ['parse(string)', 'stringify(object)'], 
                detail: 'JSON operations',
                documentation: 'JSON parsing and serialization.\n\n```isl\n$obj: @.Json.parse($jsonString);\n```'
            },
            { 
                label: 'Xml', 
                methods: ['parse(string)', 'toXml(object, rootName)'], 
                detail: 'XML operations',
                documentation: 'XML parsing and generation.\n\nAttributes use @ prefix, text uses #text'
            },
            { 
                label: 'Csv', 
                methods: ['parse(string)', 'parsemultiline(string, options)'], 
                detail: 'CSV operations',
                documentation: 'CSV parsing.\n\n```isl\n$data: @.Csv.parsemultiline($csvText);\n```'
            },
            { 
                label: 'Crypto', 
                methods: ['md5(string)', 'sha1(string)', 'sha256(string)', 'base64encode(string)', 'base64decode(string)'], 
                detail: 'Cryptography functions',
                documentation: 'Cryptographic and encoding functions.'
            },
        ];

        return services.map(s => {
            const item = new vscode.CompletionItem(s.label, vscode.CompletionItemKind.Class);
            item.detail = s.detail;
            item.documentation = new vscode.MarkdownString(s.documentation);
            item.insertText = s.label;
            return item;
        });
    }

    private getModifierCompletions(extensions: import('./extensions').IslExtensions): vscode.CompletionItem[] {
        const modifiers = [
            // Type conversions
            { label: 'to.string', detail: 'Convert to string', insertText: 'to.string', kind: vscode.CompletionItemKind.Method, docs: 'Converts value to string.\n\nFor dates: `to.string(format)`' },
            { label: 'to.number', detail: 'Convert to number', insertText: 'to.number', kind: vscode.CompletionItemKind.Method },
            { label: 'to.decimal', detail: 'Convert to decimal', insertText: 'to.decimal', kind: vscode.CompletionItemKind.Method },
            { label: 'to.boolean', detail: 'Convert to boolean', insertText: 'to.boolean', kind: vscode.CompletionItemKind.Method },
            { label: 'to.array', detail: 'Convert to array', insertText: 'to.array', kind: vscode.CompletionItemKind.Method },
            { label: 'to.object', detail: 'Convert to object', insertText: 'to.object', kind: vscode.CompletionItemKind.Method },
            { label: 'to.json', detail: 'Convert to JSON string', insertText: 'to.json', kind: vscode.CompletionItemKind.Method },
            { label: 'to.yaml', detail: 'Convert to YAML string', insertText: 'to.yaml', kind: vscode.CompletionItemKind.Method },
            { label: 'to.csv', detail: 'Convert to CSV string', insertText: 'to.csv', kind: vscode.CompletionItemKind.Method },
            { label: 'to.xml', detail: 'Convert to XML', insertText: 'to.xml("${1:root}")', kind: vscode.CompletionItemKind.Method },
            { label: 'to.hex', detail: 'Convert to hex string', insertText: 'to.hex', kind: vscode.CompletionItemKind.Method },
            { label: 'to.bytes', detail: 'Convert to byte array', insertText: 'to.bytes', kind: vscode.CompletionItemKind.Method },
            { label: 'to.epochmillis', detail: 'Convert date to epoch milliseconds', insertText: 'to.epochmillis', kind: vscode.CompletionItemKind.Method },
            
            // String modifiers
            { label: 'trim', detail: 'Trim whitespace', insertText: 'trim', kind: vscode.CompletionItemKind.Method },
            { label: 'trimStart', detail: 'Trim start whitespace', insertText: 'trimStart', kind: vscode.CompletionItemKind.Method },
            { label: 'trimEnd', detail: 'Trim end whitespace', insertText: 'trimEnd', kind: vscode.CompletionItemKind.Method },
            { label: 'upperCase', detail: 'Convert to uppercase', insertText: 'upperCase', kind: vscode.CompletionItemKind.Method },
            { label: 'lowerCase', detail: 'Convert to lowercase', insertText: 'lowerCase', kind: vscode.CompletionItemKind.Method },
            { label: 'capitalize', detail: 'Capitalize first letter', insertText: 'capitalize', kind: vscode.CompletionItemKind.Method },
            { label: 'titleCase', detail: 'Convert to title case', insertText: 'titleCase', kind: vscode.CompletionItemKind.Method },
            { label: 'camelCase', detail: 'Convert to camelCase', insertText: 'camelCase', kind: vscode.CompletionItemKind.Method },
            { label: 'snakeCase', detail: 'Convert to snake_case', insertText: 'snakeCase', kind: vscode.CompletionItemKind.Method },
            { label: 'left', detail: 'Get left N characters', insertText: 'left(${1:length})', kind: vscode.CompletionItemKind.Method },
            { label: 'right', detail: 'Get right N characters', insertText: 'right(${1:length})', kind: vscode.CompletionItemKind.Method },
            { label: 'cap', detail: 'Cap string at length (alias for left)', insertText: 'cap(${1:length})', kind: vscode.CompletionItemKind.Method },
            { label: 'split', detail: 'Split string', insertText: 'split("${1:,}")', kind: vscode.CompletionItemKind.Method },
            { label: 'replace', detail: 'Replace string', insertText: 'replace("${1:find}", "${2:replace}")', kind: vscode.CompletionItemKind.Method },
            { label: 'remove', detail: 'Remove substring', insertText: 'remove("${1:text}")', kind: vscode.CompletionItemKind.Method },
            { label: 'substring', detail: 'Get substring', insertText: 'substring(${1:start}, ${2:end})', kind: vscode.CompletionItemKind.Method },
            { label: 'substringUpto', detail: 'Substring up to delimiter', insertText: 'substringUpto("${1:delimiter}")', kind: vscode.CompletionItemKind.Method },
            { label: 'substringAfter', detail: 'Substring after delimiter', insertText: 'substringAfter("${1:delimiter}")', kind: vscode.CompletionItemKind.Method },
            { label: 'truncate', detail: 'Truncate string', insertText: 'truncate(${1:length}, "${2:...}")', kind: vscode.CompletionItemKind.Method },
            { label: 'padStart', detail: 'Pad start', insertText: 'padStart(${1:length}, "${2: }")', kind: vscode.CompletionItemKind.Method },
            { label: 'padEnd', detail: 'Pad end', insertText: 'padEnd(${1:length}, "${2: }")', kind: vscode.CompletionItemKind.Method },
            { label: 'concat', detail: 'Concatenate strings', insertText: 'concat(${1:\\$other}, "${2:delimiter}")', kind: vscode.CompletionItemKind.Method },
            { label: 'append', detail: 'Append strings', insertText: 'append(${1:\\$value})', kind: vscode.CompletionItemKind.Method },
            { label: 'reverse', detail: 'Reverse string/array', insertText: 'reverse', kind: vscode.CompletionItemKind.Method },
            { label: 'sanitizeTid', detail: 'Sanitize UUID/TID', insertText: 'sanitizeTid', kind: vscode.CompletionItemKind.Method },
            
            // Array modifiers
            { label: 'filter', detail: 'Filter array', insertText: 'filter(${1:\\$fit.${2:condition}})', kind: vscode.CompletionItemKind.Method, docs: 'Filters array based on condition.\n\nUse $fit or $ for current item being filtered.\n\nExample: `$active: $items | filter($fit.active)`' },
            { label: 'map', detail: 'Map array', insertText: 'map(${1:\\$.${2:property}})', kind: vscode.CompletionItemKind.Method, docs: 'Transforms each element.\n\nUse $ for current item.\n\nExample: `$names: $users | map($.name)`' },
            { label: 'reduce', detail: 'Reduce array', insertText: 'reduce({{ \\$acc + \\$it }}, ${1:0})', kind: vscode.CompletionItemKind.Method, docs: 'Reduces array to single value.\n\nUse $acc (accumulator) and $it (current item).\n\nExample: `$sum: $numbers | reduce({{ $acc + $it }}, 0)`' },
            { label: 'sort', detail: 'Sort array/object', insertText: 'sort', kind: vscode.CompletionItemKind.Method },
            { label: 'unique', detail: 'Get unique values', insertText: 'unique', kind: vscode.CompletionItemKind.Method },
            { label: 'slice', detail: 'Slice array', insertText: 'slice(${1:start}, ${2:end})', kind: vscode.CompletionItemKind.Method },
            { label: 'length', detail: 'Get length', insertText: 'length', kind: vscode.CompletionItemKind.Method },
            { label: 'first', detail: 'Get first element', insertText: 'first', kind: vscode.CompletionItemKind.Method },
            { label: 'last', detail: 'Get last element', insertText: 'last', kind: vscode.CompletionItemKind.Method },
            { label: 'take', detail: 'Take first N elements', insertText: 'take(${1:n})', kind: vscode.CompletionItemKind.Method },
            { label: 'drop', detail: 'Drop first N elements', insertText: 'drop(${1:n})', kind: vscode.CompletionItemKind.Method },
            { label: 'at', detail: 'Get element at index', insertText: 'at(${1:index})', kind: vscode.CompletionItemKind.Method, docs: 'Supports negative indices: `at(-1)` gets last element' },
            { label: 'indexOf', detail: 'Find index of element', insertText: 'indexOf(${1:element})', kind: vscode.CompletionItemKind.Method },
            { label: 'lastIndexOf', detail: 'Find last index of element', insertText: 'lastIndexOf(${1:element})', kind: vscode.CompletionItemKind.Method },
            { label: 'isEmpty', detail: 'Check if empty', insertText: 'isEmpty', kind: vscode.CompletionItemKind.Method },
            { label: 'isNotEmpty', detail: 'Check if not empty', insertText: 'isNotEmpty', kind: vscode.CompletionItemKind.Method },
            { label: 'push', detail: 'Add item to array', insertText: 'push(${1:item})', kind: vscode.CompletionItemKind.Method },
            { label: 'pushItems', detail: 'Push full array to end', insertText: 'pushItems(${1:\\$otherArray})', kind: vscode.CompletionItemKind.Method, docs: 'Appends all items from the provided array to the end of the input array.\n\nExample: `$items | pushItems($moreItems)`' },
            { label: 'pop', detail: 'Remove last item', insertText: 'pop', kind: vscode.CompletionItemKind.Method },
            { label: 'chunk', detail: 'Split into chunks', insertText: 'chunk(${1:size})', kind: vscode.CompletionItemKind.Method },
            
            // Date modifiers
            { label: 'date.parse', detail: 'Parse date', insertText: 'date.parse("${1:yyyy-MM-dd}")', kind: vscode.CompletionItemKind.Method, docs: 'Parses date string.\n\nOptional locale: `date.parse(format, {locale: "en_US"})`' },
            { label: 'date.add', detail: 'Add to date', insertText: 'date.add(${1:value}, "${2:DAYS}")', kind: vscode.CompletionItemKind.Method, docs: 'Units: YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS' },
            { label: 'date.part', detail: 'Get date part', insertText: 'date.part("${1:YEAR}")', kind: vscode.CompletionItemKind.Method, docs: 'Parts: YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, DAYOFYEAR, DAYOFWEEK' },
            { label: 'date.fromEpochSeconds', detail: 'From epoch seconds', insertText: 'date.fromEpochSeconds', kind: vscode.CompletionItemKind.Method },
            { label: 'date.fromEpochMillis', detail: 'From epoch milliseconds', insertText: 'date.fromEpochMillis', kind: vscode.CompletionItemKind.Method },
            
            // JSON/XML/CSV modifiers
            { label: 'json.parse', detail: 'Parse JSON', insertText: 'json.parse', kind: vscode.CompletionItemKind.Method },
            { label: 'yaml.parse', detail: 'Parse YAML', insertText: 'yaml.parse', kind: vscode.CompletionItemKind.Method },
            { label: 'xml.parse', detail: 'Parse XML', insertText: 'xml.parse', kind: vscode.CompletionItemKind.Method },
            { label: 'csv.parsemultiline', detail: 'Parse CSV', insertText: 'csv.parsemultiline', kind: vscode.CompletionItemKind.Method, docs: 'Options: {headers: true, separator: ",", skipLines: 0}' },
            { label: 'csv.findrow', detail: 'Find CSV row', insertText: 'csv.findrow', kind: vscode.CompletionItemKind.Method },
            { label: 'html.escape', detail: 'Escape HTML entities', insertText: 'html.escape', kind: vscode.CompletionItemKind.Method },
            { label: 'html.unescape', detail: 'Unescape HTML entities', insertText: 'html.unescape', kind: vscode.CompletionItemKind.Method },
            
            // Math modifiers
            { label: 'Math.sum', detail: 'Sum values', insertText: 'Math.sum(${1:0})', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.average', detail: 'Average values', insertText: 'Math.average', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.mean', detail: 'Mean of values', insertText: 'Math.mean', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.min', detail: 'Minimum value', insertText: 'Math.min', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.max', detail: 'Maximum value', insertText: 'Math.max', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.mod', detail: 'Modulo operation', insertText: 'Math.mod(${1:divisor})', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.sqrt', detail: 'Square root', insertText: 'Math.sqrt', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.round', detail: 'Round number', insertText: 'Math.round', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.floor', detail: 'Round down', insertText: 'Math.floor', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.ceil', detail: 'Round up', insertText: 'Math.ceil', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.abs', detail: 'Absolute value', insertText: 'Math.abs', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.clamp', detail: 'Clamp value', insertText: 'Math.clamp(${1:min}, ${2:max})', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.RandInt', detail: 'Random integer', insertText: 'Math.RandInt(${1:min}, ${2:max})', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.RandFloat', detail: 'Random float', insertText: 'Math.RandFloat', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.RandDouble', detail: 'Random double', insertText: 'Math.RandDouble', kind: vscode.CompletionItemKind.Method },
            { label: 'negate', detail: 'Negate number', insertText: 'negate', kind: vscode.CompletionItemKind.Method },
            { label: 'absolute', detail: 'Absolute value', insertText: 'absolute', kind: vscode.CompletionItemKind.Method },
            { label: 'precision', detail: 'Set decimal precision', insertText: 'precision(${1:2})', kind: vscode.CompletionItemKind.Method },
            { label: 'round.up', detail: 'Round up', insertText: 'round.up', kind: vscode.CompletionItemKind.Method },
            { label: 'round.down', detail: 'Round down', insertText: 'round.down', kind: vscode.CompletionItemKind.Method },
            { label: 'round.half', detail: 'Round half', insertText: 'round.half', kind: vscode.CompletionItemKind.Method },
            
            // Object modifiers
            { label: 'keys', detail: 'Get object keys', insertText: 'keys', kind: vscode.CompletionItemKind.Method },
            { label: 'kv', detail: 'Key-value pairs', insertText: 'kv', kind: vscode.CompletionItemKind.Method, docs: 'Converts object to [{key, value}] array' },
            { label: 'delete', detail: 'Delete property', insertText: 'delete("${1:property}")', kind: vscode.CompletionItemKind.Method },
            { label: 'select', detail: 'Select by JSON path', insertText: 'select("${1:path}")', kind: vscode.CompletionItemKind.Method, docs: 'Example: select("user.address.city")' },
            { label: 'getProperty', detail: 'Get property (case-insensitive)', insertText: 'getProperty("${1:name}")', kind: vscode.CompletionItemKind.Method },
            { label: 'setProperty', detail: 'Set property', insertText: 'setProperty("${1:name}", ${2:value})', kind: vscode.CompletionItemKind.Method },
            { label: 'merge', detail: 'Merge objects', insertText: 'merge(${1:\\$other})', kind: vscode.CompletionItemKind.Method },
            { label: 'pick', detail: 'Pick properties', insertText: 'pick("${1:prop1}", "${2:prop2}")', kind: vscode.CompletionItemKind.Method },
            { label: 'omit', detail: 'Omit properties', insertText: 'omit("${1:prop1}", "${2:prop2}")', kind: vscode.CompletionItemKind.Method },
            { label: 'rename', detail: 'Rename property', insertText: 'rename("${1:oldName}", "${2:newName}")', kind: vscode.CompletionItemKind.Method },
            { label: 'has', detail: 'Check if has property', insertText: 'has("${1:property}")', kind: vscode.CompletionItemKind.Method },
            { label: 'default', detail: 'Default value if null/empty', insertText: 'default(${1:value})', kind: vscode.CompletionItemKind.Method },
            
            // Regex modifiers
            { label: 'regex.find', detail: 'Find regex matches', insertText: 'regex.find("${1:pattern}")', kind: vscode.CompletionItemKind.Method },
            { label: 'regex.matches', detail: 'Test regex match', insertText: 'regex.matches("${1:pattern}")', kind: vscode.CompletionItemKind.Method },
            { label: 'regex.replace', detail: 'Replace with regex', insertText: 'regex.replace("${1:pattern}", "${2:replacement}")', kind: vscode.CompletionItemKind.Method },
            { label: 'regex.replacefirst', detail: 'Replace first match', insertText: 'regex.replacefirst("${1:pattern}", "${2:replacement}")', kind: vscode.CompletionItemKind.Method },
            
            // Encoding modifiers
            { label: 'encode.base64', detail: 'Base64 encode', insertText: 'encode.base64', kind: vscode.CompletionItemKind.Method },
            { label: 'encode.base64url', detail: 'Base64 URL encode', insertText: 'encode.base64url', kind: vscode.CompletionItemKind.Method },
            { label: 'encode.path', detail: 'URL path encode', insertText: 'encode.path', kind: vscode.CompletionItemKind.Method },
            { label: 'encode.query', detail: 'URL query encode', insertText: 'encode.query', kind: vscode.CompletionItemKind.Method },
            { label: 'decode.base64', detail: 'Base64 decode', insertText: 'decode.base64', kind: vscode.CompletionItemKind.Method },
            { label: 'decode.base64url', detail: 'Base64 URL decode', insertText: 'decode.base64url', kind: vscode.CompletionItemKind.Method },
            { label: 'decode.query', detail: 'URL query decode', insertText: 'decode.query', kind: vscode.CompletionItemKind.Method },
            { label: 'hex.tobinary', detail: 'Convert hex to binary', insertText: 'hex.tobinary', kind: vscode.CompletionItemKind.Method },
            { label: 'join.string', detail: 'Join array to string', insertText: 'join.string("${1:,}")', kind: vscode.CompletionItemKind.Method },
            { label: 'join.path', detail: 'Join for URL path', insertText: 'join.path("${1:&}")', kind: vscode.CompletionItemKind.Method },
            { label: 'join.query', detail: 'Join for URL query', insertText: 'join.query("${1:&}")', kind: vscode.CompletionItemKind.Method },
            { label: 'email.parse', detail: 'Parse email addresses', insertText: 'email.parse', kind: vscode.CompletionItemKind.Method },
            
            // Compression modifiers
            { label: 'gzip', detail: 'GZip compress', insertText: 'gzip', kind: vscode.CompletionItemKind.Method },
            { label: 'gunzip', detail: 'GZip decompress', insertText: 'gunzip', kind: vscode.CompletionItemKind.Method },
            { label: 'gunzipToByte', detail: 'GZip decompress to bytes', insertText: 'gunzipToByte', kind: vscode.CompletionItemKind.Method },
            
            // Type checking
            { label: 'typeof', detail: 'Get type of value', insertText: 'typeof', kind: vscode.CompletionItemKind.Method },
            
            // Legacy/Common
            { label: 'contains', detail: 'Check if contains', insertText: 'contains("${1:value}")', kind: vscode.CompletionItemKind.Method },
            { label: 'startsWith', detail: 'Check if starts with', insertText: 'startsWith("${1:prefix}")', kind: vscode.CompletionItemKind.Method },
            { label: 'endsWith', detail: 'Check if ends with', insertText: 'endsWith("${1:suffix}")', kind: vscode.CompletionItemKind.Method },
        ];

        const completionItems = modifiers.map(m => {
            const item = new vscode.CompletionItem(m.label, m.kind);
            item.detail = m.detail;
            item.insertText = new vscode.SnippetString(m.insertText);
            if (m.docs) {
                item.documentation = new vscode.MarkdownString(m.docs);
            }
            return item;
        });

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
