import * as vscode from 'vscode';

export class IslDocumentFormatter implements vscode.DocumentFormattingEditProvider, vscode.DocumentRangeFormattingEditProvider {
    
    provideDocumentFormattingEdits(
        document: vscode.TextDocument,
        options: vscode.FormattingOptions,
        token: vscode.CancellationToken
    ): vscode.TextEdit[] {
        const config = vscode.workspace.getConfiguration('isl.formatting');
        if (!config.get<boolean>('enabled', true)) {
            return [];
        }

        const fullRange = new vscode.Range(
            document.positionAt(0),
            document.positionAt(document.getText().length)
        );
        
        const formatted = this.formatIslCode(document.getText(), options);
        return [vscode.TextEdit.replace(fullRange, formatted)];
    }

    provideDocumentRangeFormattingEdits(
        document: vscode.TextDocument,
        range: vscode.Range,
        options: vscode.FormattingOptions,
        token: vscode.CancellationToken
    ): vscode.TextEdit[] {
        const config = vscode.workspace.getConfiguration('isl.formatting');
        if (!config.get<boolean>('enabled', true)) {
            return [];
        }

        const selectionText = document.getText(range);
        const contextRange = new vscode.Range(
            document.positionAt(0),
            range.start
        );
        const contextText = document.getText(contextRange);
        const indentState = this.computeIndentState(contextText);
        const formatted = this.formatIslCode(selectionText, options, indentState);
        return [vscode.TextEdit.replace(range, formatted)];
    }

    /**
     * Computes the indent level and open control flow state at the end of the given code.
     * Used for range formatting to preserve context-aware indentation.
     */
    private computeIndentState(code: string): { indentLevel: number; openInlineControlFlow: string[] } {
        let indentLevel = 0;
        let openInlineControlFlow: string[] = [];
        const lines = code.split('\n');

        const isInsideMultiLineString: boolean[] = new Array(lines.length).fill(false);
        let inMultiLineString = false;
        let backtickCount = 0;
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            let tempBacktickCount = 0;
            for (let j = 0; j < line.length; j++) {
                if (line[j] === '`' && (j === 0 || line[j - 1] !== '\\')) tempBacktickCount++;
            }
            if (!inMultiLineString && tempBacktickCount % 2 === 1) {
                inMultiLineString = true;
                backtickCount = tempBacktickCount;
            } else if (inMultiLineString) {
                isInsideMultiLineString[i] = true;
                backtickCount += tempBacktickCount;
                if (backtickCount % 2 === 0) {
                    inMultiLineString = false;
                    backtickCount = 0;
                }
            }
        }

        const processedLines: string[] = [];
        for (let i = 0; i < lines.length; i++) {
            if (isInsideMultiLineString[i]) {
                processedLines.push(lines[i].trimEnd());
                continue;
            }
            let line = lines[i].trim();
            line = this.normalizePipeSpacing(line);
            line = this.normalizeFunctionParameters(line);
            line = this.normalizeControlFlowParameters(line);
            line = this.collapseEmptyBrackets(line);
            line = this.normalizePropertyAssignments(line);
            line = this.normalizeVariableAssignments(line);
            line = this.normalizeFunctionObjectParameters(line);
            processedLines.push(line);
        }

        for (let i = 0; i < processedLines.length; i++) {
            const trimmedLine = processedLines[i];
            if (isInsideMultiLineString[i]) continue;
            if (trimmedLine.match(/^(fun|modifier)\s/)) indentLevel = 0;
            if (trimmedLine.startsWith('//') || trimmedLine.startsWith('#')) continue;
            if (trimmedLine === '') continue;
            if (trimmedLine.match(/^[\}\]\)]/) && indentLevel > 0) indentLevel--;
            if ((trimmedLine.match(/^[\}\]].*;$/) || trimmedLine.match(/^[\}\]],$/)) && openInlineControlFlow.length > 0) {
                const top = openInlineControlFlow[openInlineControlFlow.length - 1];
                if (top !== 'switch' && indentLevel > 0) {
                    indentLevel--;
                    openInlineControlFlow.pop();
                }
            }
            if (openInlineControlFlow.length > 0 && trimmedLine.match(/\s+else\s+[^,;]+[,;]$/)) {
                if (indentLevel > 0) {
                    indentLevel--;
                    openInlineControlFlow.pop();
                }
            }
            if (trimmedLine.match(/^(endif|endfor|endwhile|endswitch)/)) {
                if (indentLevel > 0) indentLevel--;
                if (openInlineControlFlow.length > 0) openInlineControlFlow.pop();
            }
            if (openInlineControlFlow.length > 0 && /^\$[a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*\s*[=:]/.test(trimmedLine)) {
                if (indentLevel > 0) indentLevel--;
                openInlineControlFlow.pop();
            }
            if (trimmedLine === 'else' && indentLevel > 0) {
                indentLevel--;
                indentLevel++;
                continue;
            }
            if (trimmedLine.match(/[\{\[\(]$/)) indentLevel++;
            const blockControlFlow = trimmedLine.match(/^(if|foreach|while|switch|parallel)[\s(]/);
            const inlineControlFlow = trimmedLine.match(/[=:>]\s*(if|foreach|while|switch)[\s(]/);
            if (blockControlFlow || inlineControlFlow) {
                if (!trimmedLine.includes('endif') && !trimmedLine.includes('endfor') &&
                    !trimmedLine.includes('endwhile') && !trimmedLine.includes('endswitch')) {
                    const endsWithBrace = trimmedLine.endsWith('{');
                    const endsWithTerminator = trimmedLine.endsWith(';') || trimmedLine.endsWith(',');
                    const isCompleteLine = inlineControlFlow && endsWithTerminator;
                    if (!endsWithBrace && !isCompleteLine) {
                        indentLevel++;
                        if (inlineControlFlow) {
                            const controlFlowType = inlineControlFlow[0].match(/(if|foreach|while|switch)/)?.[1] || 'unknown';
                            openInlineControlFlow.push(controlFlowType);
                        }
                    }
                }
            }
        }
        return { indentLevel, openInlineControlFlow };
    }

    private formatIslCode(code: string, options: vscode.FormattingOptions, initialIndentState?: { indentLevel: number; openInlineControlFlow: string[] }): string {
        const config = vscode.workspace.getConfiguration('isl.formatting');
        const indentSize = config.get<number>('indentSize', 4);
        const useTabs = config.get<boolean>('useTabs', false);
        const indentChar = useTabs ? '\t' : ' '.repeat(indentSize);
        const alignProperties = config.get<boolean>('alignProperties', false);

        let formatted = '';
        let indentLevel = initialIndentState?.indentLevel ?? 0;
        let openInlineControlFlow: string[] = initialIndentState ? [...initialIndentState.openInlineControlFlow] : [];

        const lines = code.split('\n');
        
        // First pass: identify lines that are inside multi-line backtick strings
        const isInsideMultiLineString: boolean[] = new Array(lines.length).fill(false);
        let inMultiLineString = false;
        let backtickCount = 0;
        
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            
            // Count backticks in this line (excluding escaped ones)
            let tempBacktickCount = 0;
            for (let j = 0; j < line.length; j++) {
                if (line[j] === '`' && (j === 0 || line[j - 1] !== '\\')) {
                    tempBacktickCount++;
                }
            }
            
            // Track if we're entering or exiting a multi-line string
            if (!inMultiLineString && tempBacktickCount % 2 === 1) {
                // Started a string on this line, check if it closes on same line
                inMultiLineString = true;
                backtickCount = tempBacktickCount;
            } else if (inMultiLineString) {
                // We're inside a multi-line string
                isInsideMultiLineString[i] = true;
                backtickCount += tempBacktickCount;
                
                // If we now have an even number of backticks, the string is closed
                if (backtickCount % 2 === 0) {
                    inMultiLineString = false;
                    backtickCount = 0;
                }
            }
        }
        
        // Process lines: normalize pipe spacing but don't split chains
        const processedLines: string[] = [];
        for (let i = 0; i < lines.length; i++) {
            // If this line is inside a multi-line string, preserve it as-is (only trim trailing spaces)
            if (isInsideMultiLineString[i]) {
                processedLines.push(lines[i].trimEnd());
                continue;
            }
            
            let line = lines[i].trim();
            
            // Normalize pipe spacing (ensure space after pipe)
            line = this.normalizePipeSpacing(line);
            
            // Normalize function/modifier declaration parameter spacing
            line = this.normalizeFunctionParameters(line);
            
            // Normalize control flow statement parameter spacing
            line = this.normalizeControlFlowParameters(line);
            
            // Collapse empty objects and arrays
            line = this.collapseEmptyBrackets(line);
            
            // Normalize property assignments (key: value)
            line = this.normalizePropertyAssignments(line);
            
            // Normalize variable assignments (= with spaces)
            line = this.normalizeVariableAssignments(line);
            
            // Normalize function calls with object parameters
            line = this.normalizeFunctionObjectParameters(line);
            
            processedLines.push(line);
        }

        // Indent and format
        let prevLineIndent = '';
        let prevLineEndsWithColon = false;
        for (let i = 0; i < processedLines.length; i++) {
            const trimmedLine = processedLines[i];

            // If this line is inside a multi-line string, preserve it as-is
            if (isInsideMultiLineString[i]) {
                formatted += trimmedLine + '\n';
                continue;
            }

            // Reset indent to 0 for function/modifier declarations (always start at column zero)
            if (trimmedLine.match(/^(fun|modifier)\s/)) {
                indentLevel = 0;
            }

            // Handle line comments
            if (trimmedLine.startsWith('//') || trimmedLine.startsWith('#')) {
                prevLineIndent = indentChar.repeat(indentLevel);
                prevLineEndsWithColon = false;
                formatted += prevLineIndent + trimmedLine + '\n';
                continue;
            }

            // Skip empty lines but preserve them
            if (trimmedLine === '') {
                prevLineIndent = '';
                prevLineEndsWithColon = false;
                formatted += '\n';
                continue;
            }

            // Adjust indent for closing braces/brackets
            if (trimmedLine.match(/^[\}\]\)]/) && indentLevel > 0) {
                indentLevel--;
            }
            
            // If line ends with }; or }], and we have open inline control flow,
            // this closes the inline expression - decrease indent
            // BUT: for switch statements, }; only closes a case, not the switch itself
            if ((trimmedLine.match(/^[\}\]].*;$/) || trimmedLine.match(/^[\}\]],$/)) && openInlineControlFlow.length > 0) {
                const topControlFlow = openInlineControlFlow[openInlineControlFlow.length - 1];
                // Only close if it's NOT a switch statement
                if (topControlFlow !== 'switch') {
                    if (indentLevel > 0) {
                        indentLevel--;
                        openInlineControlFlow.pop();
                    }
                }
            }
            
            // Check if this line completes an inline if/switch with 'else' but no explicit end keyword
            // Pattern: ] else value, or ) else value, etc. (ends with terminator)
            // This closes the inline control flow
            if (openInlineControlFlow.length > 0 && trimmedLine.match(/\s+else\s+[^,;]+[,;]$/)) {
                if (indentLevel > 0) {
                    indentLevel--;
                    openInlineControlFlow.pop();
                }
            }

            // Special handling for control flow endings
            if (trimmedLine.match(/^(endif|endfor|endwhile|endswitch)/)) {
                if (indentLevel > 0) indentLevel--;
                if (openInlineControlFlow.length > 0) openInlineControlFlow.pop();
            }

            // Inline if/switch used as conditional modifier don't require endif/endswitch.
            // A new statement (e.g. $var = ... or $var: ... or endfor) closes the previous inline expression.
            if (openInlineControlFlow.length > 0 && /^\$[a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*\s*[=:]/.test(trimmedLine)) {
                if (indentLevel > 0) indentLevel--;
                openInlineControlFlow.pop();
            }

            // Handle 'else' - decrease then increase indent
            if (trimmedLine === 'else' && indentLevel > 0) {
                indentLevel--;
                prevLineIndent = indentChar.repeat(indentLevel);
                prevLineEndsWithColon = false;
                formatted += prevLineIndent + trimmedLine + '\n';
                indentLevel++;
                continue;
            }

            // Check if this line is a continuation of a modifier chain (starts with |)
            const isModifierContinuation = trimmedLine.startsWith('|');
            const extraIndent = isModifierContinuation ? 1 : 0;

            // For standalone '{' after a property key (line ending with :), use same indent as key
            // to prevent drift when repeatedly formatting (e.g. format-on-Enter)
            let lineIndent = indentChar.repeat(indentLevel + extraIndent);
            if (trimmedLine === '{' && prevLineEndsWithColon && prevLineIndent !== '') {
                lineIndent = prevLineIndent;
            }

            // Add indentation (with extra indent for modifier continuations)
            formatted += lineIndent + trimmedLine;

            // Track for next iteration (property key with value on next line)
            prevLineIndent = lineIndent;
            prevLineEndsWithColon = trimmedLine.endsWith(':') && !trimmedLine.includes('?');

            // Adjust indent for opening braces/brackets
            if (trimmedLine.match(/[\{\[\(]$/)) {
                indentLevel++;
            }

            // Special handling for control flow beginnings (but not fun/modifier)
            // Check for both block statements (start of line) and inline statements (after : or = or ->)
            // Match either space or opening paren after keyword: if( or if (
            const blockControlFlow = trimmedLine.match(/^(if|foreach|while|switch|parallel)[\s(]/);
            const inlineControlFlow = trimmedLine.match(/[=:>]\s*(if|foreach|while|switch)[\s(]/);
            
            if (blockControlFlow || inlineControlFlow) {
                if (!trimmedLine.includes('endif') && !trimmedLine.includes('endfor') && 
                    !trimmedLine.includes('endwhile') && !trimmedLine.includes('endswitch')) {
                    // Don't increase indent if line ends with { (brace handles it)
                    // Don't increase indent if inline expression is complete on one line (; or , at end)
                    const endsWithBrace = trimmedLine.endsWith('{');
                    const endsWithTerminator = trimmedLine.endsWith(';') || trimmedLine.endsWith(',');
                    
                    // For inline expressions: if they end with a terminator, they're complete
                    // For block expressions: they never end with terminators, so this won't affect them
                    const isCompleteLine = inlineControlFlow && endsWithTerminator;
                    
                    if (!endsWithBrace && !isCompleteLine) {
                        indentLevel++;
                        // Track open inline control flow type (will be closed by }; or endif)
                        if (inlineControlFlow) {
                            // Extract the control flow type from the match
                            const controlFlowType = inlineControlFlow[0].match(/(if|foreach|while|switch)/)?.[1] || 'unknown';
                            openInlineControlFlow.push(controlFlowType);
                        }
                    }
                }
            }
            
            // Note: fun/modifier declarations don't increase indent themselves
            // The opening { (whether on same line or next line) handles the indentation

            // Add newline
            if (i < processedLines.length - 1) {
                formatted += '\n';
            }
        }

        // Align properties if enabled
        if (alignProperties) {
            formatted = this.alignObjectProperties(formatted);
        }

        return formatted;
    }
    
    private normalizePipeSpacing(line: string): string {
        // Skip comments and strings
        if (line.trim().startsWith('//') || line.trim().startsWith('#')) {
            return line;
        }
        
        // Replace | with | (with space) but be careful with strings
        // This regex finds pipes that are not inside strings
        let result = '';
        let inString = false;
        let stringChar = '';
        let i = 0;
        
        while (i < line.length) {
            const char = line[i];
            const nextChar = line[i + 1] || '';
            
            // Track string boundaries
            if ((char === '"' || char === "'" || char === '`') && (i === 0 || line[i - 1] !== '\\')) {
                if (!inString) {
                    inString = true;
                    stringChar = char;
                } else if (char === stringChar) {
                    inString = false;
                    stringChar = '';
                }
                result += char;
                i++;
                continue;
            }
            
            // If we find a pipe outside of strings
            if (char === '|' && !inString) {
                // Add pipe with space after
                result += '| ';
                // Skip any existing spaces after the pipe
                while (i + 1 < line.length && line[i + 1] === ' ') {
                    i++;
                }
                i++;
                continue;
            }
            
            result += char;
            i++;
        }
        
        return result;
    }

    private normalizeFunctionParameters(line: string): string {
        // Skip comments
        if (line.trim().startsWith('//') || line.trim().startsWith('#')) {
            return line;
        }

        // Match function/modifier declarations: fun name(...) or modifier name(...)
        const functionMatch = line.match(/^(fun|modifier)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(([^)]*)\)/);
        if (!functionMatch) {
            return line;
        }

        const keyword = functionMatch[1];     // 'fun' or 'modifier'
        const name = functionMatch[2];        // function name
        const params = functionMatch[3];      // parameters inside ()
        const afterParams = line.substring(functionMatch[0].length); // everything after )

        // Trim the parameters
        const trimmedParams = params.trim();

        // If no parameters, no spaces
        if (trimmedParams === '') {
            return `${keyword} ${name}()${afterParams}`;
        }

        // If there are parameters, add spaces after ( and before )
        // Also normalize spaces around commas
        const normalizedParams = trimmedParams
            .split(',')
            .map(p => p.trim())
            .join(', ');

        return `${keyword} ${name}( ${normalizedParams} )${afterParams}`;
    }

    private normalizeControlFlowParameters(line: string): string {
        // Skip comments
        if (line.trim().startsWith('//') || line.trim().startsWith('#')) {
            return line;
        }

        // Normalize if, switch, while, foreach statements
        // Pattern: keyword( condition ) or keyword ( condition )
        const keywords = ['if', 'switch', 'while'];
        
        for (const keyword of keywords) {
            // Match keyword followed by parentheses
            const regex = new RegExp(`\\b(${keyword})\\s*\\(([^)]*)\\)`, 'g');
            line = line.replace(regex, (match, kw, content) => {
                const trimmedContent = content.trim();
                // If no content, no spaces: if()
                if (trimmedContent === '') {
                    return `${kw}()`;
                }
                // Otherwise add spaces: if ( condition )
                return `${kw} ( ${trimmedContent} )`;
            });
        }

        return line;
    }
    
    private collapseEmptyBrackets(line: string): string {
        // Skip comments
        if (line.trim().startsWith('//') || line.trim().startsWith('#')) {
            return line;
        }
        
        let result = '';
        let inString = false;
        let stringChar = '';
        let i = 0;
        
        while (i < line.length) {
            const char = line[i];
            
            // Track string boundaries
            if ((char === '"' || char === "'" || char === '`') && (i === 0 || line[i - 1] !== '\\')) {
                if (!inString) {
                    inString = true;
                    stringChar = char;
                } else if (char === stringChar) {
                    inString = false;
                    stringChar = '';
                }
                result += char;
                i++;
                continue;
            }
            
            // If we're in a string, just copy the character
            if (inString) {
                result += char;
                i++;
                continue;
            }
            
            // Check for empty objects or arrays
            if (char === '{' || char === '[') {
                const closingChar = char === '{' ? '}' : ']';
                let j = i + 1;
                
                // Skip whitespace
                while (j < line.length && (line[j] === ' ' || line[j] === '\t')) {
                    j++;
                }
                
                // Check if next non-whitespace char is the closing bracket
                if (j < line.length && line[j] === closingChar) {
                    // Collapse: output {}, []
                    result += char + closingChar;
                    i = j + 1;
                    continue;
                }
            }
            
            result += char;
            i++;
        }
        
        return result;
    }
    
    private alignObjectProperties(code: string): string {
        const lines = code.split('\n');
        const result: string[] = [];
        let inObject = false;
        let objectLines: { indent: string, key: string, value: string, fullLine: string }[] = [];
        
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            const trimmed = line.trim();
            
            // Detect object start
            if (trimmed === '{' || trimmed.endsWith('{')) {
                if (objectLines.length > 0) {
                    // Flush previous object
                    result.push(...this.alignLines(objectLines));
                    objectLines = [];
                }
                result.push(line);
                inObject = true;
                continue;
            }
            
            // Detect object end
            if (trimmed === '}' || trimmed.startsWith('}')) {
                if (objectLines.length > 0) {
                    result.push(...this.alignLines(objectLines));
                    objectLines = [];
                }
                result.push(line);
                inObject = false;
                continue;
            }
            
            // Collect object properties
            if (inObject && trimmed.match(/^[a-zA-Z_`\$"][^:]*:\s*.+/)) {
                const match = line.match(/^(\s*)([a-zA-Z_`\$"][^:]*):\s*(.+)/);
                if (match) {
                    objectLines.push({
                        indent: match[1],
                        key: match[2],
                        value: match[3],
                        fullLine: line
                    });
                    continue;
                }
            }
            
            // Not an object property - flush and add
            if (objectLines.length > 0) {
                result.push(...this.alignLines(objectLines));
                objectLines = [];
            }
            result.push(line);
        }
        
        // Flush remaining
        if (objectLines.length > 0) {
            result.push(...this.alignLines(objectLines));
        }
        
        return result.join('\n');
    }
    
    private alignLines(lines: { indent: string, key: string, value: string }[]): string[] {
        if (lines.length === 0) {
            return [];
        }
        
        // Find max key length
        const maxKeyLength = Math.max(...lines.map(l => l.key.length));
        
        // Align all lines
        return lines.map(l => {
            const padding = ' '.repeat(maxKeyLength - l.key.length);
            return `${l.indent}${l.key}:${padding} ${l.value}`;
        });
    }
    
    /**
     * Rule 1: For property assignments using :, colon should be next to property with one space before value
     * Example: name: "value" (not name : "value" or name:"value")
     */
    private normalizePropertyAssignments(line: string): string {
        // Skip comments
        if (line.trim().startsWith('//') || line.trim().startsWith('#')) {
            return line;
        }
        
        let result = '';
        let inString = false;
        let stringChar = '';
        let i = 0;
        
        while (i < line.length) {
            const char = line[i];
            
            // Track string boundaries
            if ((char === '"' || char === "'" || char === '`') && (i === 0 || line[i - 1] !== '\\')) {
                if (!inString) {
                    inString = true;
                    stringChar = char;
                } else if (char === stringChar) {
                    inString = false;
                    stringChar = '';
                }
                result += char;
                i++;
                continue;
            }
            
            // If we're in a string, just copy the character
            if (inString) {
                result += char;
                i++;
                continue;
            }
            
            // Look for : used in property assignments
            if (char === ':') {
                // Check if this looks like a property assignment (not a ternary operator)
                // Property assignments have an identifier before the colon
                let j = result.length - 1;
                
                // Skip trailing spaces before colon
                while (j >= 0 && result[j] === ' ') {
                    result = result.substring(0, j);
                    j--;
                }
                
                // Check if there's an identifier before (property name)
                const beforeColon = result.match(/[a-zA-Z_$][a-zA-Z0-9_.]*$/);
                if (beforeColon) {
                    const trimmedResult = result.trimEnd();
                    // Type name context: "type idx" / "type ns.idx" or "$var : idx" / "$var: idx" - colon is inside type name (e.g. idx:name)
                    // Do not add space so we keep type names as one word: idx:name not idx: name
                    const isTypeNameContext = /^type\s+[a-zA-Z_][a-zA-Z0-9_.]*$/.test(trimmedResult) ||
                        /^\$[a-zA-Z_][a-zA-Z0-9_]*\s*:\s*[a-zA-Z_][a-zA-Z0-9_.:]*$/.test(trimmedResult);
                    if (isTypeNameContext) {
                        result += ':';
                        i++;
                        // Skip any spaces after colon, then copy the rest of the type name (e.g. name or ns:name)
                        while (i < line.length && line[i] === ' ') {
                            i++;
                        }
                        while (i < line.length) {
                            const c = line[i];
                            if (/[a-zA-Z0-9_]/.test(c) || c === '.' || (c === ':' && result.length > 0 && result[result.length - 1] !== ':')) {
                                result += c;
                                i++;
                            } else {
                                break;
                            }
                        }
                        continue;
                    }
                    // This looks like a property assignment
                    // Add colon without space, then ensure one space after
                    result += ': ';
                    i++;
                    
                    // Skip any existing spaces after the colon
                    while (i < line.length && line[i] === ' ') {
                        i++;
                    }
                    continue;
                }
            }
            
            result += char;
            i++;
        }
        
        return result;
    }
    
    /**
     * Rule 2: For variable assignments using =, ensure spaces on both sides
     * Example: $var = value (not $var=value or $var =value)
     */
    private normalizeVariableAssignments(line: string): string {
        // Skip comments
        if (line.trim().startsWith('//') || line.trim().startsWith('#')) {
            return line;
        }
        
        let result = '';
        let inString = false;
        let stringChar = '';
        let i = 0;
        
        while (i < line.length) {
            const char = line[i];
            const nextChar = line[i + 1] || '';
            
            // Track string boundaries
            if ((char === '"' || char === "'" || char === '`') && (i === 0 || line[i - 1] !== '\\')) {
                if (!inString) {
                    inString = true;
                    stringChar = char;
                } else if (char === stringChar) {
                    inString = false;
                    stringChar = '';
                }
                result += char;
                i++;
                continue;
            }
            
            // If we're in a string, just copy the character
            if (inString) {
                result += char;
                i++;
                continue;
            }
            
            // Look for = (but not ==, !=, >=, <=, =>)
            if (char === '=' && nextChar !== '=' && (i === 0 || line[i - 1] !== '!' && line[i - 1] !== '>' && line[i - 1] !== '<' && line[i - 1] !== '=')) {
                // Not an arrow function (=>)
                if (nextChar !== '>') {
                    // Remove trailing spaces before =
                    while (result.length > 0 && result[result.length - 1] === ' ') {
                        result = result.substring(0, result.length - 1);
                    }
                    
                    // Add = with spaces on both sides
                    result += ' = ';
                    i++;
                    
                    // Skip any existing spaces after =
                    while (i < line.length && line[i] === ' ') {
                        i++;
                    }
                    continue;
                }
            }
            
            result += char;
            i++;
        }
        
        return result;
    }
    
    /**
     * Rule 3: For function calls with object {} as first parameter, keep braces next to parentheses
     * Example: @.source.name({ key: value }) (not @.source.name( { key: value } ))
     */
    private normalizeFunctionObjectParameters(line: string): string {
        // Skip comments
        if (line.trim().startsWith('//') || line.trim().startsWith('#')) {
            return line;
        }
        
        let result = '';
        let inString = false;
        let stringChar = '';
        let i = 0;
        
        while (i < line.length) {
            const char = line[i];
            
            // Track string boundaries
            if ((char === '"' || char === "'" || char === '`') && (i === 0 || line[i - 1] !== '\\')) {
                if (!inString) {
                    inString = true;
                    stringChar = char;
                } else if (char === stringChar) {
                    inString = false;
                    stringChar = '';
                }
                result += char;
                i++;
                continue;
            }
            
            // If we're in a string, just copy the character
            if (inString) {
                result += char;
                i++;
                continue;
            }
            
            // Look for opening parenthesis that's part of a function call
            if (char === '(' && i > 0) {
                // Check if this is preceded by an identifier (function name)
                let j = i - 1;
                while (j >= 0 && line[j] === ' ') {
                    j--;
                }
                
                const beforeParen = line.substring(0, j + 1);
                const isAfterIdentifier = /[a-zA-Z_$\.]$/.test(beforeParen);
                
                if (isAfterIdentifier) {
                    result += '(';
                    i++;
                    
                    // Temporarily skip spaces to check what's next
                    let tempI = i;
                    while (tempI < line.length && line[tempI] === ' ') {
                        tempI++;
                    }
                    
                    // Check if the first parameter is an object {
                    if (tempI < line.length && line[tempI] === '{') {
                        // Skip the spaces (move i forward)
                        i = tempI;
                        // Find matching closing }
                        let depth = 1;
                        let k = i + 1;
                        let tempInString = false;
                        let tempStringChar = '';
                        
                        while (k < line.length && depth > 0) {
                            const c = line[k];
                            
                            // Track strings
                            if ((c === '"' || c === "'" || c === '`') && (k === 0 || line[k - 1] !== '\\')) {
                                if (!tempInString) {
                                    tempInString = true;
                                    tempStringChar = c;
                                } else if (c === tempStringChar) {
                                    tempInString = false;
                                    tempStringChar = '';
                                }
                            }
                            
                            if (!tempInString) {
                                if (c === '{') depth++;
                                if (c === '}') depth--;
                            }
                            k++;
                        }
                        
                        if (depth === 0) {
                            // Extract the object content
                            const objectContent = line.substring(i, k);
                            result += objectContent;
                            i = k;
                            
                            // Skip spaces after }
                            while (i < line.length && line[i] === ' ') {
                                i++;
                            }
                            
                            // If next char is ), add it without space
                            if (i < line.length && line[i] === ')') {
                                result += ')';
                                i++;
                            }
                            continue;
                        }
                    }
                    continue;
                }
            }
            
            result += char;
            i++;
        }
        
        return result;
    }
}

