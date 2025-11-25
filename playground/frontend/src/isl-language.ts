// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function registerIslLanguage(monaco: any) {
  // Check if already registered
  if (monaco.languages.getLanguages().some((lang: any) => lang.id === 'isl')) {
    return;
  }

  // Register the ISL language
  monaco.languages.register({ id: 'isl' });

  // Define ISL language configuration
  monaco.languages.setLanguageConfiguration('isl', {
    comments: {
      lineComment: '//',
      blockComment: ['/*', '*/'],
    },
    brackets: [
      ['{', '}'],
      ['[', ']'],
      ['(', ')'],
    ],
    autoClosingPairs: [
      { open: '{', close: '}' },
      { open: '[', close: ']' },
      { open: '(', close: ')' },
      { open: '"', close: '"' },
      { open: "'", close: "'" },
      { open: '`', close: '`' },
    ],
    surroundingPairs: [
      { open: '{', close: '}' },
      { open: '[', close: ']' },
      { open: '(', close: ')' },
      { open: '"', close: '"' },
      { open: "'", close: "'" },
      { open: '`', close: '`' },
    ],
  });

  // Define ISL syntax highlighting
  monaco.languages.setMonarchTokensProvider('isl', {
    keywords: [
      'if', 'else', 'endif', 'switch', 'endswitch', 'foreach', 'endfor',
      'while', 'endwhile', 'parallel', 'fun', 'modifier', 'return',
      'cache', 'import', 'type', 'as', 'from', 'and', 'or', 'not', 'in',
    ],
    
    operators: [
      '==', '!=', '<=', '>=', '<', '>', '??', '->', '...', '+', '-', '*', '/', '=', '|',
    ],
    
    builtins: [
      'filter', 'map', 'reduce', 'string', 'number', 'integer', 'boolean',
      'object', 'array', 'any', 'null', 'text', 'date', 'datetime', 'binary',
      'contains', 'startsWith', 'endsWith', 'matches', 'is',
    ],
    
    constants: ['true', 'false', 'null'],

    tokenizer: {
      root: [
        // Comments
        [/\/\/.*$/, 'comment'],
        [/#.*$/, 'comment'],
        [/\/\*/, 'comment', '@comment'],

        // Variables
        [/\$[a-zA-Z_]\w*/, 'variable'],

        // Function calls (@.Service.method)
        [/@\.[a-zA-Z_]\w*(?:\.[a-zA-Z_]\w*)*/, 'type.identifier'],

        // Keywords
        [/\b(?:if|else|endif|switch|endswitch|foreach|endfor|while|endwhile|parallel|fun|modifier|return|cache|import|type|as|from|and|or|not|in)\b/, 'keyword'],

        // Builtins
        [/\b(?:filter|map|reduce|string|number|integer|boolean|object|array|any|null|text|date|datetime|binary|contains|startsWith|endsWith|matches|is)\b/, 'keyword.control'],

        // Constants
        [/\b(?:true|false|null)\b/, 'constant.language'],

        // Numbers
        [/-?\d+\.?\d*/, 'number'],

        // Strings (double quotes)
        [/"([^"\\]|\\.)*$/, 'string.invalid'],
        [/"/, 'string', '@string_double'],

        // Strings (single quotes)
        [/'([^'\\]|\\.)*$/, 'string.invalid'],
        [/'/, 'string', '@string_single'],

        // Strings (backticks with interpolation)
        [/`/, 'string', '@string_backtick'],

        // Regex
        [/\/(?![/*])(?:[^\\/]|\\.)+\//, 'regexp'],

        // Math expressions
        [/\{\{/, 'delimiter', '@math_expression'],

        // Operators
        [/==|!=|<=|>=|<|>|\?\?|->|\.\.\./, 'operator'],
        [/[+\-*\/=|]/, 'operator'],

        // Delimiters
        [/[{}()\[\]:;,.]/, 'delimiter'],

        // Identifiers
        [/[a-zA-Z_]\w*/, 'identifier'],
      ],

      comment: [
        [/[^/*]+/, 'comment'],
        [/\*\//, 'comment', '@pop'],
        [/[/*]/, 'comment'],
      ],

      string_double: [
        [/[^\\"]+/, 'string'],
        [/\\./, 'string.escape'],
        [/"/, 'string', '@pop'],
      ],

      string_single: [
        [/[^\\']+/, 'string'],
        [/\\./, 'string.escape'],
        [/'/, 'string', '@pop'],
      ],

      string_backtick: [
        [/\$[a-zA-Z_]\w*/, 'variable'],
        [/\$\{/, 'delimiter', '@interpolation'],
        [/\{\{/, 'delimiter', '@math_expression'],
        [/@\.[a-zA-Z_]\w*(?:\.[a-zA-Z_]\w*)*/, 'type.identifier'],
        [/[^`$@{\\]+/, 'string'],
        [/\\./, 'string.escape'],
        [/`/, 'string', '@pop'],
        [/[$@{]/, 'string'],
      ],

      interpolation: [
        [/\}/, 'delimiter', '@pop'],
        { include: 'root' },
      ],

      math_expression: [
        [/\}\}/, 'delimiter', '@pop'],
        [/\$[a-zA-Z_]\w*/, 'variable'],
        [/@\.[a-zA-Z_]\w*(?:\.[a-zA-Z_]\w*)*/, 'type.identifier'],
        [/-?\d+\.?\d*/, 'number'],
        [/[+\-*\/()]/, 'operator'],
        [/\s+/, ''],
      ],
    },
  });

  // Define ISL theme
  monaco.editor.defineTheme('isl-dark', {
    base: 'vs-dark',
    inherit: true,
    rules: [
      { token: 'comment', foreground: '6A9955' },
      { token: 'keyword', foreground: 'C586C0' },
      { token: 'keyword.control', foreground: 'DCDCAA' },
      { token: 'string', foreground: 'CE9178' },
      { token: 'string.escape', foreground: 'D7BA7D' },
      { token: 'number', foreground: 'B5CEA8' },
      { token: 'regexp', foreground: 'D16969' },
      { token: 'variable', foreground: '9CDCFE' },
      { token: 'type.identifier', foreground: '4EC9B0' },
      { token: 'constant.language', foreground: '569CD6' },
      { token: 'operator', foreground: 'D4D4D4' },
      { token: 'delimiter', foreground: 'D4D4D4' },
    ],
    colors: {
      'editor.background': '#1E1E1E',
      'editor.foreground': '#D4D4D4',
      'editorLineNumber.foreground': '#858585',
      'editorCursor.foreground': '#AEAFAD',
      'editor.selectionBackground': '#264F78',
      'editor.inactiveSelectionBackground': '#3A3D41',
    },
  });
}

