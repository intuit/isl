import * as vscode from 'vscode';

export class IslHoverProvider implements vscode.HoverProvider {
    
    provideHover(
        document: vscode.TextDocument,
        position: vscode.Position,
        token: vscode.CancellationToken
    ): vscode.Hover | undefined {
        const range = document.getWordRangeAtPosition(position);
        if (!range) {
            return undefined;
        }

        const word = document.getText(range);
        const line = document.lineAt(position.line).text;

        // Check what kind of token we're hovering over
        if (this.isKeyword(word)) {
            return this.getKeywordHover(word);
        } else if (line.includes('@.' + word)) {
            return this.getServiceHover(word);
        } else if (this.isModifier(word, line)) {
            return this.getModifierHover(word, line);
        } else if (line.includes('$' + word)) {
            return this.getVariableHover(word, document);
        }

        return undefined;
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
            'if': '**If Statement**\n\nConditional execution based on a boolean expression.\n\n```isl\nif ($value > 10)\n    result: "high"\nelse\n    result: "low"\nendif\n```\n\n**Inline if expression:**\n```isl\n$status: if ($active) "active" else "inactive" endif;\n```',
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
        const services: { [key: string]: string } = {
            'Date': '**Date Service**\n\nDate and time operations (all times in UTC).\n\n**Methods:**\n- `Now()` - Get current date/time\n- `parse(string, format, {locale})` - Parse date string\n- `fromEpochSeconds(seconds)` - Create from epoch seconds\n- `fromEpochMillis(millis)` - Create from epoch milliseconds\n\n**Example:**\n```isl\n$now: @.Date.Now();\n$parsed: @.Date.parse("2024-01-15", "yyyy-MM-dd");\n```',
            'Math': '**Math Service**\n\nMathematical operations on arrays and numbers.\n\n**Methods:**\n- `sum(initial)` - Sum array values\n- `average()` - Calculate average\n- `min()` - Find minimum\n- `max()` - Find maximum  \n- `clamp(min, max)` - Clamp value to range\n- `round()`, `floor()`, `ceil()` - Rounding\n- `abs()` - Absolute value\n\n**Example:**\n```isl\n$total: $prices | Math.sum(0);\n$clamped: $value | Math.clamp(0, 100);\n```',
            'This': '**This Service**\n\nCalls functions defined in the current ISL script.\n\n**Example:**\n```isl\nfun helper($x) {\n    return $x | upperCase\n}\n\nfun run($input) {\n    result: @.This.helper($input.name)\n}\n```',
            'String': '**String Service**\n\nString manipulation functions.\n\n**Methods:**\n- `concat(strings...)` - Concatenate strings\n- `join(array, separator)` - Join array to string\n- `split(string, separator)` - Split string to array\n- `replace(string, find, replace)` - Replace text\n- `substring(string, start, end)` - Get substring',
            'Array': '**Array Service**\n\nArray manipulation functions.\n\n**Methods:**\n- `concat(arrays...)` - Concatenate arrays\n- `slice(start, end)` - Get array slice\n- `reverse()` - Reverse array\n- `flatten()` - Flatten nested arrays',
            'Json': '**JSON Service**\n\nJSON parsing and serialization.\n\n**Methods:**\n- `parse(string)` - Parse JSON string to object\n- `stringify(object)` - Convert object to JSON string\n\n**Example:**\n```isl\n$obj: @.Json.parse($jsonString);\n$json: @.Json.stringify($object);\n```',
            'Xml': '**XML Service**\n\nXML parsing and generation.\n\n**Methods:**\n- `parse(string)` - Parse XML string to JSON object\n- `toXml(object, rootName)` - Convert JSON object to XML\n\n**Note:** Attributes use @ prefix, text content uses #text\n\n**Example:**\n```isl\n$obj: @.Xml.parse($xmlString);\n$xml: @.Xml.toXml($object, "root");\n```',
            'Csv': '**CSV Service**\n\nCSV parsing.\n\n**Methods:**\n- `parse(string)` - Parse single line CSV\n- `parsemultiline(string, options)` - Parse multi-line CSV\n\n**Options:** `{headers: true, separator: ",", skipLines: 0}`\n\n**Example:**\n```isl\n$data: @.Csv.parsemultiline($csvText);\n```',
            'Crypto': '**Crypto Service**\n\nCryptographic and encoding functions.\n\n**Methods:**\n- `md5(string)` - MD5 hash\n- `sha1(string)` - SHA-1 hash\n- `sha256(string)` - SHA-256 hash\n- `base64encode(string)` - Base64 encode\n- `base64decode(string)` - Base64 decode\n\n**Example:**\n```isl\n$hash: @.Crypto.sha256($password);\n$encoded: @.Crypto.base64encode($data);\n```',
        };

        const markdown = new vscode.MarkdownString(services[word] || `**@.${word}**\n\nISL service`);
        markdown.isTrusted = true;
        return new vscode.Hover(markdown);
    }

    private getModifierHover(word: string, line: string): vscode.Hover {
        const modifiers: { [key: string]: { desc: string, signature?: string, example?: string } } = {
            // String modifiers
            'trim': { 
                desc: 'Removes leading and trailing whitespace from a string.',
                signature: 'trim',
                example: '$name: $input.name | trim;'
            },
            'upperCase': { 
                desc: 'Converts a string to uppercase.',
                signature: 'upperCase',
                example: '$code: $input.status | upperCase; // "ACTIVE"'
            },
            'lowerCase': { 
                desc: 'Converts a string to lowercase.',
                signature: 'lowerCase',
                example: '$email: $input.email | lowerCase;'
            },
            'capitalize': { 
                desc: 'Capitalizes the first letter of a string.',
                signature: 'capitalize',
                example: '$name: "john" | capitalize; // "John"'
            },
            'titleCase': { 
                desc: 'Converts a string to title case (capitalizes each word).',
                signature: 'titleCase',
                example: '$title: "hello world" | titleCase; // "Hello World"'
            },
            'split': { 
                desc: 'Splits a string into an array using a delimiter.',
                signature: 'split(delimiter)',
                example: '$tags: "red,blue,green" | split(","); // ["red", "blue", "green"]'
            },
            'replace': { 
                desc: 'Replaces occurrences of a substring with another string.',
                signature: 'replace(find, replaceWith)',
                example: '$text: $input | replace("old", "new");'
            },
            'substring': { 
                desc: 'Extracts a portion of a string.',
                signature: 'substring(start, end)',
                example: '$code: $input | substring(0, 5);'
            },
            'truncate': { 
                desc: 'Truncates a string to a maximum length with optional suffix.',
                signature: 'truncate(maxLength, suffix)',
                example: '$short: $longText | truncate(100, "...");'
            },
            'padStart': { 
                desc: 'Pads the start of a string to a target length.',
                signature: 'padStart(length, padString)',
                example: '$id: $number | to.string | padStart(8, "0"); // "00000123"'
            },
            'padEnd': { 
                desc: 'Pads the end of a string to a target length.',
                signature: 'padEnd(length, padString)',
                example: '$code: $text | padEnd(10, " ");'
            },
            
            // Array modifiers
            'filter': { 
                desc: 'Filters an array based on a condition. Use $ or $it for current item.',
                signature: 'filter(condition)',
                example: '$active: $items | filter($item.status == "active");\n$highValue: $nums | filter($ > 100);'
            },
            'map': { 
                desc: 'Transforms each element of an array. Use $ or $it for current item.',
                signature: 'map(expression)',
                example: '$names: $users | map($user.name);\n$doubled: $numbers | map({{ $ * 2 }});'
            },
            'reduce': { 
                desc: 'Reduces an array to a single value. Use $acc for accumulator, $it for current item.',
                signature: 'reduce(expression, initialValue)',
                example: '$sum: [1, 2, 3] | reduce({{ $acc + $it }}, 0); // 6'
            },
            'length': { 
                desc: 'Returns the length of a string or array.',
                signature: 'length',
                example: '$count: $array | length;\n$size: $text | length;'
            },
            'sort': { 
                desc: 'Sorts an array in ascending order.',
                signature: 'sort',
                example: '$sorted: $numbers | sort;'
            },
            'reverse': { 
                desc: 'Reverses the order of elements in an array.',
                signature: 'reverse',
                example: '$reversed: $array | reverse;'
            },
            'unique': { 
                desc: 'Returns unique values from an array, removing duplicates.',
                signature: 'unique',
                example: '$uniqueTags: $tags | unique;'
            },
            'flatten': { 
                desc: 'Flattens nested arrays into a single array.',
                signature: 'flatten',
                example: '$flat: [[1,2], [3,4]] | flatten; // [1,2,3,4]'
            },
            'first': { 
                desc: 'Returns the first element of an array.',
                signature: 'first',
                example: '$firstItem: $array | first;'
            },
            'last': { 
                desc: 'Returns the last element of an array.',
                signature: 'last',
                example: '$lastItem: $array | last;'
            },
            'at': { 
                desc: 'Returns the element at a specific index (supports negative indices).',
                signature: 'at(index)',
                example: '$second: $array | at(1);\n$lastItem: $array | at(-1);'
            },
            'isEmpty': { 
                desc: 'Returns true if array or string is empty.',
                signature: 'isEmpty',
                example: 'if ($array | isEmpty) ... endif'
            },
            'isNotEmpty': { 
                desc: 'Returns true if array or string is not empty.',
                signature: 'isNotEmpty',
                example: 'if ($array | isNotEmpty) ... endif'
            },
            'push': { 
                desc: 'Adds an item to the end of an array.',
                signature: 'push(item)',
                example: '$newArray: $array | push($newItem);'
            },
            'pop': { 
                desc: 'Removes and returns the last element from an array.',
                signature: 'pop',
                example: '$item: $array | pop;'
            },
            
            // Type conversion modifiers
            'to': { 
                desc: 'Type conversion namespace.',
                signature: 'to.string | to.number | to.decimal | to.boolean | to.array | to.json | to.xml',
                example: '$id: $input.id | to.string;\n$price: $input.price | to.decimal;'
            },
            'string': { 
                desc: 'Converts value to string. Also used for date formatting.',
                signature: 'to.string or to.string(format) for dates',
                example: '$id: $num | to.string;\n$date: $timestamp | to.string("yyyy-MM-dd");'
            },
            'number': { 
                desc: 'Converts value to integer number.',
                signature: 'to.number',
                example: '$count: $input.count | to.number;'
            },
            'decimal': { 
                desc: 'Converts value to decimal number.',
                signature: 'to.decimal',
                example: '$price: $input.price | to.decimal;'
            },
            'boolean': { 
                desc: 'Converts value to boolean.',
                signature: 'to.boolean',
                example: '$active: $input.active | to.boolean;'
            },
            'array': { 
                desc: 'Converts value to array.',
                signature: 'to.array',
                example: '$items: $single | to.array;'
            },
            'json': { 
                desc: 'Converts object to JSON string.',
                signature: 'to.json',
                example: '$jsonStr: $object | to.json;'
            },
            'toxml': { 
                desc: 'Converts object to XML string.',
                signature: 'to.xml(rootName)',
                example: '$xml: $object | to.xml("root");'
            },
            'epochmillis': { 
                desc: 'Converts date to epoch milliseconds.',
                signature: 'to.epochmillis',
                example: '$timestamp: $date | to.epochmillis;'
            },
            
            // Date modifiers
            'date': { 
                desc: 'Date operations namespace.',
                signature: 'date.parse | date.add | date.part | date.fromEpochSeconds | date.fromEpochMillis',
                example: '$parsed: $str | date.parse("yyyy-MM-dd");\n$tomorrow: $date | date.add(1, "DAYS");'
            },
            'parse': { 
                desc: 'Parses a date string with the given format.',
                signature: 'date.parse(format, {locale})',
                example: '$date: "2024-01-15" | date.parse("yyyy-MM-dd");\n$parsed: $str | date.parse("MM/dd/yyyy", {locale: "en_US"});'
            },
            'add': { 
                desc: 'Adds time to a date. Units: YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS.',
                signature: 'date.add(amount, unit)',
                example: '$tomorrow: $date | date.add(1, "DAYS");\n$nextWeek: $date | date.add(7, "DAYS");'
            },
            'part': { 
                desc: 'Extracts a part of a date. Parts: YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, DAYOFYEAR, DAYOFWEEK.',
                signature: 'date.part(part)',
                example: '$year: $date | date.part("YEAR");\n$month: $date | date.part("MONTH");'
            },
            
            // Math modifiers
            'Math': { 
                desc: 'Math operations on arrays and numbers.',
                signature: 'Math.sum | Math.average | Math.min | Math.max | Math.clamp | Math.round',
                example: '$total: $prices | Math.sum(0);\n$avg: $values | Math.average;'
            },
            'sum': { 
                desc: 'Sums all numbers in an array.',
                signature: 'Math.sum(initialValue)',
                example: '$total: $prices | Math.sum(0);'
            },
            'average': { 
                desc: 'Calculates the average of numbers in an array.',
                signature: 'Math.average',
                example: '$avg: $scores | Math.average;'
            },
            'min': { 
                desc: 'Finds the minimum value in an array.',
                signature: 'Math.min',
                example: '$lowest: $prices | Math.min;'
            },
            'max': { 
                desc: 'Finds the maximum value in an array.',
                signature: 'Math.max',
                example: '$highest: $scores | Math.max;'
            },
            'clamp': { 
                desc: 'Clamps a number to a range [min, max].',
                signature: 'Math.clamp(min, max)',
                example: '$safe: $value | Math.clamp(0, 100);'
            },
            'precision': { 
                desc: 'Sets decimal precision for a number.',
                signature: 'precision(digits)',
                example: '$price: $value | precision(2); // 12.99'
            },
            'round': { 
                desc: 'Rounds a number.',
                signature: 'Math.round',
                example: '$rounded: $value | Math.round;'
            },
            'floor': { 
                desc: 'Rounds down to nearest integer.',
                signature: 'Math.floor',
                example: '$down: $value | Math.floor;'
            },
            'ceil': { 
                desc: 'Rounds up to nearest integer.',
                signature: 'Math.ceil',
                example: '$up: $value | Math.ceil;'
            },
            'abs': { 
                desc: 'Returns absolute value.',
                signature: 'Math.abs',
                example: '$positive: $negative | Math.abs;'
            },
            
            // XML/CSV modifiers
            'xml': { 
                desc: 'XML operations.',
                signature: 'xml.parse',
                example: '$obj: $xmlString | xml.parse;'
            },
            'csv': { 
                desc: 'CSV parsing operations.',
                signature: 'csv.parsemultiline(options)',
                example: '$data: $csvText | csv.parsemultiline;'
            },
            'parsemultiline': { 
                desc: 'Parses multi-line CSV into array of objects.',
                signature: 'csv.parsemultiline({headers, separator, skipLines})',
                example: '$data: $csv | csv.parsemultiline;\n$custom: $csv | csv.parsemultiline({separator: ";", skipLines: 1});'
            },
            
            // Regex modifiers
            'regex': { 
                desc: 'Regular expression operations.',
                signature: 'regex.find | regex.matches | regex.replace',
                example: '$found: $text | regex.find("/\\d+/");\n$clean: $text | regex.replace("/[^a-z]/", "");'
            },
            
            // Object modifiers
            'keys': { 
                desc: 'Returns an array of object keys.',
                signature: 'keys',
                example: '$propNames: $object | keys;'
            },
            'kv': { 
                desc: 'Converts object to array of key-value pairs.',
                signature: 'kv',
                example: '$pairs: $object | kv; // [{key: "name", value: "John"}]'
            },
            'delete': { 
                desc: 'Removes a property from an object.',
                signature: 'delete(propertyName)',
                example: '$clean: $object | delete("tempField");'
            },
            'select': { 
                desc: 'Selects a nested property by path.',
                signature: 'select(path)',
                example: '$value: $object | select("user.address.city");'
            },
            'getProperty': { 
                desc: 'Gets a property by name (case-insensitive).',
                signature: 'getProperty(name)',
                example: '$value: $object | getProperty("Name"); // finds "name", "Name", or "NAME"'
            },
            
            // Encoding
            'encode': { 
                desc: 'Encoding operations.',
                signature: 'encode.base64',
                example: '$encoded: $text | encode.base64;'
            },
            'decode': { 
                desc: 'Decoding operations.',
                signature: 'decode.base64',
                example: '$decoded: $encoded | decode.base64;'
            },
            'base64': { 
                desc: 'Base64 encode/decode.',
                signature: 'encode.base64 or decode.base64',
                example: '$enc: $text | encode.base64;\n$dec: $enc | decode.base64;'
            },
            
            // Other
            'default': { 
                desc: 'Returns a default value if the input is null or empty.',
                signature: 'default(defaultValue)',
                example: '$name: $input.name | default("Unknown");'
            },
            'coalesce': { 
                desc: 'Returns first non-null value. Use ?? operator instead.',
                signature: 'coalesce(alternativeValue)',
                example: '$value: $input.value | coalesce($fallback);\n// Better: $value: $input.value ?? $fallback;'
            },
        };

        const info = modifiers[word];
        if (info) {
            let markdown = `**\`${word}\`** modifier\n\n${info.desc}`;
            if (info.signature) {
                markdown += `\n\n**Signature:** \`${info.signature}\``;
            }
            if (info.example) {
                markdown += `\n\n**Example:**\n\`\`\`isl\n${info.example}\n\`\`\``;
            }
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
