import * as vscode from 'vscode';

export class IslSignatureHelpProvider implements vscode.SignatureHelpProvider {
    
    provideSignatureHelp(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken,
        context: vscode.SignatureHelpContext
    ): vscode.SignatureHelp | undefined {
        const line = document.lineAt(position).text;
        const beforeCursor = line.substring(0, position.character);
        
        // Check if we're inside a modifier call
        const modifierMatch = beforeCursor.match(/\|\s*([a-zA-Z_][a-zA-Z0-9_.]*)(?:\(([^)]*))?$/);
        if (modifierMatch) {
            return this.getModifierSignature(modifierMatch[1], beforeCursor);
        }
        
        // Check if we're inside a service call
        const serviceMatch = beforeCursor.match(/@\.([A-Z][a-zA-Z0-9_]*)\.([a-zA-Z_][a-zA-Z0-9_]*)(?:\(([^)]*))?$/);
        if (serviceMatch) {
            return this.getServiceSignature(serviceMatch[1], serviceMatch[2], beforeCursor);
        }
        
        // Check if we're inside a custom function call (@.This.)
        const thisMatch = beforeCursor.match(/@\.This\.([a-zA-Z_][a-zA-Z0-9_]*)(?:\(([^)]*))?$/);
        if (thisMatch) {
            return this.getCustomFunctionSignature(document, thisMatch[1], beforeCursor);
        }
        
        return undefined;
    }
    
    private getModifierSignature(modifier: string, beforeCursor: string): vscode.SignatureHelp | undefined {
        const signatures: { [key: string]: { label: string, params: Array<{ label: string, doc: string }>, doc: string } } = {
            // String modifiers
            'split': {
                label: 'split(delimiter: string)',
                params: [
                    { label: 'delimiter', doc: 'The string to split on (e.g., ",", " ")' }
                ],
                doc: 'Splits a string into an array using the delimiter'
            },
            'replace': {
                label: 'replace(find: string, replaceWith: string)',
                params: [
                    { label: 'find', doc: 'The string to find' },
                    { label: 'replaceWith', doc: 'The replacement string' }
                ],
                doc: 'Replaces all occurrences of find with replaceWith'
            },
            'substring': {
                label: 'substring(start: number, end: number)',
                params: [
                    { label: 'start', doc: 'Starting index (0-based)' },
                    { label: 'end', doc: 'Ending index (exclusive)' }
                ],
                doc: 'Extracts a portion of the string'
            },
            'truncate': {
                label: 'truncate(maxLength: number, suffix: string)',
                params: [
                    { label: 'maxLength', doc: 'Maximum length of the string' },
                    { label: 'suffix', doc: 'Suffix to add if truncated (e.g., "...")' }
                ],
                doc: 'Truncates string to maxLength and adds suffix if needed'
            },
            'padStart': {
                label: 'padStart(length: number, padString: string)',
                params: [
                    { label: 'length', doc: 'Target length of the string' },
                    { label: 'padString', doc: 'String to pad with (e.g., "0", " ")' }
                ],
                doc: 'Pads the start of the string to reach target length'
            },
            'padEnd': {
                label: 'padEnd(length: number, padString: string)',
                params: [
                    { label: 'length', doc: 'Target length of the string' },
                    { label: 'padString', doc: 'String to pad with' }
                ],
                doc: 'Pads the end of the string to reach target length'
            },
            
            // Array modifiers
            'filter': {
                label: 'filter(condition: expression)',
                params: [
                    { label: 'condition', doc: 'Boolean expression using $ or $item for current element' }
                ],
                doc: 'Filters array elements based on condition. Use $ for current item.'
            },
            'map': {
                label: 'map(expression: any)',
                params: [
                    { label: 'expression', doc: 'Transform expression using $ or $item for current element' }
                ],
                doc: 'Transforms each array element. Use $ for current item.'
            },
            'reduce': {
                label: 'reduce(expression: any, initialValue: any)',
                params: [
                    { label: 'expression', doc: 'Accumulator expression using $acc and $it' },
                    { label: 'initialValue', doc: 'Initial value for the accumulator' }
                ],
                doc: 'Reduces array to single value. Use $acc (accumulator) and $it (current item).'
            },
            'at': {
                label: 'at(index: number)',
                params: [
                    { label: 'index', doc: 'Array index (supports negative for counting from end)' }
                ],
                doc: 'Returns element at index. Use -1 for last element.'
            },
            'push': {
                label: 'push(item: any)',
                params: [
                    { label: 'item', doc: 'Item to add to array' }
                ],
                doc: 'Adds item to end of array'
            },
            
            // Date modifiers
            'date.parse': {
                label: 'date.parse(format: string, options?: object)',
                params: [
                    { label: 'format', doc: 'Date format pattern (e.g., "yyyy-MM-dd")' },
                    { label: 'options', doc: 'Optional: {locale: "en_US"}' }
                ],
                doc: 'Parses date string using Java DateTimeFormatter patterns'
            },
            'date.add': {
                label: 'date.add(amount: number, unit: string)',
                params: [
                    { label: 'amount', doc: 'Amount to add (can be negative)' },
                    { label: 'unit', doc: 'Time unit: YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS' }
                ],
                doc: 'Adds time to a date'
            },
            'date.part': {
                label: 'date.part(part: string)',
                params: [
                    { label: 'part', doc: 'Date part: YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, DAYOFYEAR, DAYOFWEEK' }
                ],
                doc: 'Extracts a specific part from a date'
            },
            
            // Math modifiers
            'Math.sum': {
                label: 'Math.sum(initialValue: number)',
                params: [
                    { label: 'initialValue', doc: 'Starting value for sum (usually 0)' }
                ],
                doc: 'Sums all numbers in array'
            },
            'Math.clamp': {
                label: 'Math.clamp(min: number, max: number)',
                params: [
                    { label: 'min', doc: 'Minimum allowed value' },
                    { label: 'max', doc: 'Maximum allowed value' }
                ],
                doc: 'Clamps number to range [min, max]'
            },
            'precision': {
                label: 'precision(digits: number)',
                params: [
                    { label: 'digits', doc: 'Number of decimal places' }
                ],
                doc: 'Sets decimal precision (e.g., precision(2) → 12.99)'
            },
            
            // Type conversions
            'to.string': {
                label: 'to.string(format?: string)',
                params: [
                    { label: 'format', doc: 'For dates: format pattern (e.g., "yyyy-MM-dd HH:mm:ss")' }
                ],
                doc: 'Converts value to string. For dates, optional format parameter.'
            },
            'to.xml': {
                label: 'to.xml(rootName: string)',
                params: [
                    { label: 'rootName', doc: 'Root element name for XML' }
                ],
                doc: 'Converts object to XML string'
            },
            
            // Regex
            'regex.find': {
                label: 'regex.find(pattern: string, options?: object)',
                params: [
                    { label: 'pattern', doc: 'Regular expression pattern' },
                    { label: 'options', doc: 'Optional: {ignoreCase: true, multiLine: true}' }
                ],
                doc: 'Finds all matches of pattern in string'
            },
            'regex.matches': {
                label: 'regex.matches(pattern: string, options?: object)',
                params: [
                    { label: 'pattern', doc: 'Regular expression pattern' },
                    { label: 'options', doc: 'Optional: {ignoreCase: true}' }
                ],
                doc: 'Tests if pattern matches string (returns boolean)'
            },
            'regex.replace': {
                label: 'regex.replace(pattern: string, replacement: string, options?: object)',
                params: [
                    { label: 'pattern', doc: 'Regular expression pattern' },
                    { label: 'replacement', doc: 'Replacement string' },
                    { label: 'options', doc: 'Optional: {ignoreCase: true}' }
                ],
                doc: 'Replaces pattern matches with replacement'
            },
            
            // CSV
            'csv.parsemultiline': {
                label: 'csv.parsemultiline(options?: object)',
                params: [
                    { label: 'options', doc: 'Optional: {headers: true, separator: ",", skipLines: 0}' }
                ],
                doc: 'Parses multi-line CSV to array of objects'
            },
            
            // Object
            'delete': {
                label: 'delete(propertyName: string)',
                params: [
                    { label: 'propertyName', doc: 'Name of property to delete' }
                ],
                doc: 'Removes property from object'
            },
            'select': {
                label: 'select(path: string)',
                params: [
                    { label: 'path', doc: 'Property path (e.g., "user.address.city")' }
                ],
                doc: 'Selects nested property by path'
            },
            'getProperty': {
                label: 'getProperty(name: string)',
                params: [
                    { label: 'name', doc: 'Property name (case-insensitive)' }
                ],
                doc: 'Gets property by name (case-insensitive lookup)'
            },
            
            // Other
            'default': {
                label: 'default(defaultValue: any)',
                params: [
                    { label: 'defaultValue', doc: 'Value to use if input is null/empty' }
                ],
                doc: 'Returns default value if input is null or empty'
            },
            'coalesce': {
                label: 'coalesce(alternativeValue: any)',
                params: [
                    { label: 'alternativeValue', doc: 'Alternative value if input is null' }
                ],
                doc: 'Returns first non-null value (prefer ?? operator)'
            },
        };
        
        const sig = signatures[modifier];
        if (!sig) {
            return undefined;
        }
        
        const signatureHelp = new vscode.SignatureHelp();
        const signature = new vscode.SignatureInformation(sig.label, new vscode.MarkdownString(sig.doc));
        
        sig.params.forEach(param => {
            signature.parameters.push(new vscode.ParameterInformation(param.label, new vscode.MarkdownString(param.doc)));
        });
        
        signatureHelp.signatures = [signature];
        signatureHelp.activeSignature = 0;
        
        // Determine active parameter based on comma count
        const paramSection = beforeCursor.match(/\(([^)]*)?$/)?.[1] || '';
        const commaCount = (paramSection.match(/,/g) || []).length;
        signatureHelp.activeParameter = Math.min(commaCount, sig.params.length - 1);
        
        return signatureHelp;
    }
    
    private getServiceSignature(service: string, method: string, beforeCursor: string): vscode.SignatureHelp | undefined {
        const signatures: { [key: string]: { [key: string]: { label: string, params: Array<{ label: string, doc: string }>, doc: string } } } = {
            'Date': {
                'parse': {
                    label: '@.Date.parse(dateString: string, format: string, options?: object)',
                    params: [
                        { label: 'dateString', doc: 'String containing the date to parse' },
                        { label: 'format', doc: 'Date format pattern (e.g., "yyyy-MM-dd")' },
                        { label: 'options', doc: 'Optional: {locale: "en_US"}' }
                    ],
                    doc: 'Parses a date string into a date object'
                },
                'fromEpochSeconds': {
                    label: '@.Date.fromEpochSeconds(seconds: number)',
                    params: [
                        { label: 'seconds', doc: 'Unix timestamp in seconds' }
                    ],
                    doc: 'Creates date from Unix timestamp (seconds)'
                },
                'fromEpochMillis': {
                    label: '@.Date.fromEpochMillis(milliseconds: number)',
                    params: [
                        { label: 'milliseconds', doc: 'Unix timestamp in milliseconds' }
                    ],
                    doc: 'Creates date from Unix timestamp (milliseconds)'
                }
            },
            'Math': {
                'sum': {
                    label: '@.Math.sum(array: number[], initialValue: number)',
                    params: [
                        { label: 'array', doc: 'Array of numbers to sum' },
                        { label: 'initialValue', doc: 'Starting value (usually 0)' }
                    ],
                    doc: 'Sums all numbers in array'
                },
                'clamp': {
                    label: '@.Math.clamp(value: number, min: number, max: number)',
                    params: [
                        { label: 'value', doc: 'Number to clamp' },
                        { label: 'min', doc: 'Minimum allowed value' },
                        { label: 'max', doc: 'Maximum allowed value' }
                    ],
                    doc: 'Clamps value to range [min, max]'
                }
            },
            'String': {
                'concat': {
                    label: '@.String.concat(...strings: string[])',
                    params: [
                        { label: 'strings', doc: 'Strings to concatenate' }
                    ],
                    doc: 'Concatenates multiple strings'
                },
                'join': {
                    label: '@.String.join(array: string[], separator: string)',
                    params: [
                        { label: 'array', doc: 'Array of strings to join' },
                        { label: 'separator', doc: 'Separator string (e.g., ", ")' }
                    ],
                    doc: 'Joins array elements into string'
                },
                'split': {
                    label: '@.String.split(string: string, separator: string)',
                    params: [
                        { label: 'string', doc: 'String to split' },
                        { label: 'separator', doc: 'Separator pattern' }
                    ],
                    doc: 'Splits string into array'
                }
            },
            'Json': {
                'parse': {
                    label: '@.Json.parse(jsonString: string)',
                    params: [
                        { label: 'jsonString', doc: 'JSON string to parse' }
                    ],
                    doc: 'Parses JSON string into object'
                },
                'stringify': {
                    label: '@.Json.stringify(object: any)',
                    params: [
                        { label: 'object', doc: 'Object to convert to JSON' }
                    ],
                    doc: 'Converts object to JSON string'
                }
            },
            'Xml': {
                'parse': {
                    label: '@.Xml.parse(xmlString: string)',
                    params: [
                        { label: 'xmlString', doc: 'XML string to parse' }
                    ],
                    doc: 'Parses XML string into JSON object'
                },
                'toXml': {
                    label: '@.Xml.toXml(object: any, rootName: string)',
                    params: [
                        { label: 'object', doc: 'Object to convert' },
                        { label: 'rootName', doc: 'Root element name' }
                    ],
                    doc: 'Converts object to XML string'
                }
            },
            'Csv': {
                'parse': {
                    label: '@.Csv.parse(csvLine: string)',
                    params: [
                        { label: 'csvLine', doc: 'Single line of CSV to parse' }
                    ],
                    doc: 'Parses single CSV line to array'
                },
                'parsemultiline': {
                    label: '@.Csv.parsemultiline(csvString: string, options?: object)',
                    params: [
                        { label: 'csvString', doc: 'Multi-line CSV string' },
                        { label: 'options', doc: 'Optional: {headers: true, separator: ","}' }
                    ],
                    doc: 'Parses multi-line CSV to array of objects'
                }
            },
            'Crypto': {
                'md5': {
                    label: '@.Crypto.md5(string: string)',
                    params: [
                        { label: 'string', doc: 'String to hash' }
                    ],
                    doc: 'Computes MD5 hash (returns hex string)'
                },
                'sha1': {
                    label: '@.Crypto.sha1(string: string)',
                    params: [
                        { label: 'string', doc: 'String to hash' }
                    ],
                    doc: 'Computes SHA-1 hash (returns hex string)'
                },
                'sha256': {
                    label: '@.Crypto.sha256(string: string)',
                    params: [
                        { label: 'string', doc: 'String to hash' }
                    ],
                    doc: 'Computes SHA-256 hash (returns hex string)'
                },
                'base64encode': {
                    label: '@.Crypto.base64encode(string: string)',
                    params: [
                        { label: 'string', doc: 'String to encode' }
                    ],
                    doc: 'Encodes string to Base64'
                },
                'base64decode': {
                    label: '@.Crypto.base64decode(string: string)',
                    params: [
                        { label: 'string', doc: 'Base64 string to decode' }
                    ],
                    doc: 'Decodes Base64 string'
                }
            }
        };
        
        const serviceSigs = signatures[service];
        if (!serviceSigs) {
            return undefined;
        }
        
        const sig = serviceSigs[method];
        if (!sig) {
            return undefined;
        }
        
        const signatureHelp = new vscode.SignatureHelp();
        const signature = new vscode.SignatureInformation(sig.label, new vscode.MarkdownString(sig.doc));
        
        sig.params.forEach(param => {
            signature.parameters.push(new vscode.ParameterInformation(param.label, new vscode.MarkdownString(param.doc)));
        });
        
        signatureHelp.signatures = [signature];
        signatureHelp.activeSignature = 0;
        
        // Determine active parameter
        const paramSection = beforeCursor.match(/\(([^)]*)?$/)?.[1] || '';
        const commaCount = (paramSection.match(/,/g) || []).length;
        signatureHelp.activeParameter = Math.min(commaCount, sig.params.length - 1);
        
        return signatureHelp;
    }
    
    private getCustomFunctionSignature(document: vscode.TextDocument, functionName: string, beforeCursor: string): vscode.SignatureHelp | undefined {
        const text = document.getText();
        const lines = text.split('\n');
        
        // Find function declaration
        const functionPattern = new RegExp(`^\\s*(fun|modifier)\\s+${functionName}\\s*\\(([^)]*)\\)`);
        
        for (let i = 0; i < lines.length; i++) {
            const match = lines[i].match(functionPattern);
            if (match) {
                const funcType = match[1];
                const params = match[2].trim();
                
                if (!params) {
                    return undefined; // No parameters
                }
                
                const signatureHelp = new vscode.SignatureHelp();
                const signature = new vscode.SignatureInformation(
                    `${funcType} ${functionName}(${params})`,
                    new vscode.MarkdownString(`Custom ${funcType} defined in this file`)
                );
                
                // Parse parameters
                const paramList = params.split(',').map(p => p.trim()).filter(p => p.length > 0);
                paramList.forEach(param => {
                    const paramName = param.split(':')[0].trim();
                    signature.parameters.push(new vscode.ParameterInformation(paramName));
                });
                
                signatureHelp.signatures = [signature];
                signatureHelp.activeSignature = 0;
                
                // Determine active parameter
                const paramSection = beforeCursor.match(/\(([^)]*)?$/)?.[1] || '';
                const commaCount = (paramSection.match(/,/g) || []).length;
                signatureHelp.activeParameter = Math.min(commaCount, paramList.length - 1);
                
                return signatureHelp;
            }
        }
        
        return undefined;
    }
}

