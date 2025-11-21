import * as vscode from 'vscode';

export class IslCompletionProvider implements vscode.CompletionItemProvider {
    
    provideCompletionItems(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken,
        context: vscode.CompletionContext
    ): vscode.CompletionItem[] | vscode.CompletionList {
        const linePrefix = document.lineAt(position).text.substr(0, position.character);
        
        // Check for @.This. - show functions from current file
        if (linePrefix.match(/@\.This\.[\w]*$/)) {
            return this.getFunctionsFromDocument(document);
        }
        // Provide different completions based on context
        else if (linePrefix.endsWith('@.')) {
            return this.getServiceCompletions();
        } else if (linePrefix.match(/\|\s*[\w.]*$/)) {
            return this.getModifierCompletions();
        } else if (linePrefix.match(/\$\w*$/)) {
            return this.getVariableCompletions(document);
        } else {
            return this.getKeywordCompletions();
        }
    }

    private getFunctionsFromDocument(document: vscode.TextDocument): vscode.CompletionItem[] {
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
        
        return functions;
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
                methods: ['sum(initial)', 'average()', 'min()', 'max()', 'clamp(min, max)', 'round()', 'floor()', 'ceil()', 'abs()'], 
                detail: 'Math operations',
                documentation: 'Mathematical operations on arrays and numbers.\n\nUse with arrays: `$total: $prices | Math.sum(0)`'
            },
            { 
                label: 'String', 
                methods: ['concat(...)', 'join(array, sep)', 'split(str, sep)', 'replace(str, find, replace)'], 
                detail: 'String operations',
                documentation: 'String manipulation functions.'
            },
            { 
                label: 'Array', 
                methods: ['concat(...)', 'slice(start, end)', 'reverse()', 'flatten()'], 
                detail: 'Array operations',
                documentation: 'Array manipulation functions.'
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

    private getModifierCompletions(): vscode.CompletionItem[] {
        const modifiers = [
            // Type conversions
            { label: 'to.string', detail: 'Convert to string', insertText: 'to.string', kind: vscode.CompletionItemKind.Method, docs: 'Converts value to string.\n\nFor dates: `to.string(format)`' },
            { label: 'to.number', detail: 'Convert to number', insertText: 'to.number', kind: vscode.CompletionItemKind.Method },
            { label: 'to.decimal', detail: 'Convert to decimal', insertText: 'to.decimal', kind: vscode.CompletionItemKind.Method },
            { label: 'to.boolean', detail: 'Convert to boolean', insertText: 'to.boolean', kind: vscode.CompletionItemKind.Method },
            { label: 'to.array', detail: 'Convert to array', insertText: 'to.array', kind: vscode.CompletionItemKind.Method },
            { label: 'to.json', detail: 'Convert to JSON string', insertText: 'to.json', kind: vscode.CompletionItemKind.Method },
            { label: 'to.xml', detail: 'Convert to XML', insertText: 'to.xml("${1:root}")', kind: vscode.CompletionItemKind.Method },
            { label: 'to.epochmillis', detail: 'Convert date to epoch milliseconds', insertText: 'to.epochmillis', kind: vscode.CompletionItemKind.Method },
            
            // String modifiers
            { label: 'trim', detail: 'Trim whitespace', insertText: 'trim', kind: vscode.CompletionItemKind.Method },
            { label: 'upperCase', detail: 'Convert to uppercase', insertText: 'upperCase', kind: vscode.CompletionItemKind.Method },
            { label: 'lowerCase', detail: 'Convert to lowercase', insertText: 'lowerCase', kind: vscode.CompletionItemKind.Method },
            { label: 'capitalize', detail: 'Capitalize first letter', insertText: 'capitalize', kind: vscode.CompletionItemKind.Method },
            { label: 'titleCase', detail: 'Convert to title case', insertText: 'titleCase', kind: vscode.CompletionItemKind.Method },
            { label: 'split', detail: 'Split string', insertText: 'split("${1:,}")', kind: vscode.CompletionItemKind.Method },
            { label: 'replace', detail: 'Replace string', insertText: 'replace("${1:find}", "${2:replace}")', kind: vscode.CompletionItemKind.Method },
            { label: 'substring', detail: 'Get substring', insertText: 'substring(${1:start}, ${2:end})', kind: vscode.CompletionItemKind.Method },
            { label: 'truncate', detail: 'Truncate string', insertText: 'truncate(${1:length}, "${2:...}")', kind: vscode.CompletionItemKind.Method },
            { label: 'padStart', detail: 'Pad start', insertText: 'padStart(${1:length}, "${2: }")', kind: vscode.CompletionItemKind.Method },
            { label: 'padEnd', detail: 'Pad end', insertText: 'padEnd(${1:length}, "${2: }")', kind: vscode.CompletionItemKind.Method },
            
            // Array modifiers
            { label: 'filter', detail: 'Filter array', insertText: 'filter(${1:\\$item.${2:condition}})', kind: vscode.CompletionItemKind.Method, docs: 'Filters array based on condition.\n\nExample: `$active: $items | filter($item.active)`' },
            { label: 'map', detail: 'Map array', insertText: 'map(${1:\\$item.${2:property}})', kind: vscode.CompletionItemKind.Method, docs: 'Transforms each element.\n\nExample: `$names: $users | map($user.name)`' },
            { label: 'reduce', detail: 'Reduce array', insertText: 'reduce({{ \\$acc + \\$it }}, ${1:0})', kind: vscode.CompletionItemKind.Method, docs: 'Reduces array to single value.\n\nUse $acc (accumulator) and $it (current item)' },
            { label: 'sort', detail: 'Sort array', insertText: 'sort', kind: vscode.CompletionItemKind.Method },
            { label: 'reverse', detail: 'Reverse array', insertText: 'reverse', kind: vscode.CompletionItemKind.Method },
            { label: 'unique', detail: 'Get unique values', insertText: 'unique', kind: vscode.CompletionItemKind.Method },
            { label: 'flatten', detail: 'Flatten nested arrays', insertText: 'flatten', kind: vscode.CompletionItemKind.Method },
            { label: 'length', detail: 'Get length', insertText: 'length', kind: vscode.CompletionItemKind.Method },
            { label: 'first', detail: 'Get first element', insertText: 'first', kind: vscode.CompletionItemKind.Method },
            { label: 'last', detail: 'Get last element', insertText: 'last', kind: vscode.CompletionItemKind.Method },
            { label: 'at', detail: 'Get element at index', insertText: 'at(${1:index})', kind: vscode.CompletionItemKind.Method, docs: 'Supports negative indices: `at(-1)` gets last element' },
            { label: 'isEmpty', detail: 'Check if empty', insertText: 'isEmpty', kind: vscode.CompletionItemKind.Method },
            { label: 'isNotEmpty', detail: 'Check if not empty', insertText: 'isNotEmpty', kind: vscode.CompletionItemKind.Method },
            { label: 'push', detail: 'Add item to array', insertText: 'push(${1:item})', kind: vscode.CompletionItemKind.Method },
            { label: 'pop', detail: 'Remove last item', insertText: 'pop', kind: vscode.CompletionItemKind.Method },
            
            // Date modifiers
            { label: 'date.parse', detail: 'Parse date', insertText: 'date.parse("${1:yyyy-MM-dd}")', kind: vscode.CompletionItemKind.Method, docs: 'Parses date string.\n\nOptional locale: `date.parse(format, {locale: "en_US"})`' },
            { label: 'date.add', detail: 'Add to date', insertText: 'date.add(${1:value}, "${2:DAYS}")', kind: vscode.CompletionItemKind.Method, docs: 'Units: YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS' },
            { label: 'date.part', detail: 'Get date part', insertText: 'date.part("${1:YEAR}")', kind: vscode.CompletionItemKind.Method, docs: 'Parts: YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, DAYOFYEAR, DAYOFWEEK' },
            { label: 'date.fromEpochSeconds', detail: 'From epoch seconds', insertText: 'date.fromEpochSeconds', kind: vscode.CompletionItemKind.Method },
            { label: 'date.fromEpochMillis', detail: 'From epoch milliseconds', insertText: 'date.fromEpochMillis', kind: vscode.CompletionItemKind.Method },
            
            // XML/CSV modifiers
            { label: 'xml.parse', detail: 'Parse XML', insertText: 'xml.parse', kind: vscode.CompletionItemKind.Method },
            { label: 'csv.parsemultiline', detail: 'Parse CSV', insertText: 'csv.parsemultiline', kind: vscode.CompletionItemKind.Method, docs: 'Options: {headers: true, separator: ",", skipLines: 0}' },
            
            // Math modifiers
            { label: 'Math.sum', detail: 'Sum values', insertText: 'Math.sum(${1:0})', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.average', detail: 'Average values', insertText: 'Math.average', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.min', detail: 'Minimum value', insertText: 'Math.min', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.max', detail: 'Maximum value', insertText: 'Math.max', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.clamp', detail: 'Clamp value', insertText: 'Math.clamp(${1:min}, ${2:max})', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.round', detail: 'Round number', insertText: 'Math.round', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.floor', detail: 'Round down', insertText: 'Math.floor', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.ceil', detail: 'Round up', insertText: 'Math.ceil', kind: vscode.CompletionItemKind.Method },
            { label: 'Math.abs', detail: 'Absolute value', insertText: 'Math.abs', kind: vscode.CompletionItemKind.Method },
            { label: 'precision', detail: 'Set decimal precision', insertText: 'precision(${1:2})', kind: vscode.CompletionItemKind.Method },
            
            // Object modifiers
            { label: 'keys', detail: 'Get object keys', insertText: 'keys', kind: vscode.CompletionItemKind.Method },
            { label: 'kv', detail: 'Key-value pairs', insertText: 'kv', kind: vscode.CompletionItemKind.Method, docs: 'Converts object to [{key, value}] array' },
            { label: 'delete', detail: 'Delete property', insertText: 'delete("${1:property}")', kind: vscode.CompletionItemKind.Method },
            { label: 'select', detail: 'Select nested property', insertText: 'select("${1:path}")', kind: vscode.CompletionItemKind.Method, docs: 'Example: select("user.address.city")' },
            { label: 'getProperty', detail: 'Get property (case-insensitive)', insertText: 'getProperty("${1:name}")', kind: vscode.CompletionItemKind.Method },
            
            // Regex
            { label: 'regex.find', detail: 'Find regex match', insertText: 'regex.find("/${1:pattern}/")', kind: vscode.CompletionItemKind.Method },
            { label: 'regex.matches', detail: 'Test regex match', insertText: 'regex.matches("/${1:pattern}/")', kind: vscode.CompletionItemKind.Method },
            { label: 'regex.replace', detail: 'Replace with regex', insertText: 'regex.replace("/${1:pattern}/", "${2:replacement}")', kind: vscode.CompletionItemKind.Method },
            
            // Encoding
            { label: 'encode.base64', detail: 'Base64 encode', insertText: 'encode.base64', kind: vscode.CompletionItemKind.Method },
            { label: 'decode.base64', detail: 'Base64 decode', insertText: 'decode.base64', kind: vscode.CompletionItemKind.Method },
            
            // Other modifiers
            { label: 'default', detail: 'Default value if null', insertText: 'default(${1:value})', kind: vscode.CompletionItemKind.Method },
            { label: 'coalesce', detail: 'First non-null value', insertText: 'coalesce(${1:value})', kind: vscode.CompletionItemKind.Method, docs: 'Prefer ?? operator: $value ?? $default' },
        ];

        return modifiers.map(m => {
            const item = new vscode.CompletionItem(m.label, m.kind);
            item.detail = m.detail;
            item.insertText = new vscode.SnippetString(m.insertText);
            if (m.docs) {
                item.documentation = new vscode.MarkdownString(m.docs);
            }
            return item;
        });
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
                item.insertText = varName; // Don't include $ since user already typed it
                variables.set(varName, item);
            }
        }

        // Also add common input variable
        if (!variables.has('input')) {
            const inputItem = new vscode.CompletionItem('$input', vscode.CompletionItemKind.Variable);
            inputItem.detail = 'Input parameter';
            inputItem.insertText = 'input';
            variables.set('input', inputItem);
        }

        return Array.from(variables.values());
    }
}
