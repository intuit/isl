// Prism.js ISL Language Definition
// Provides syntax highlighting for ISL (Intuitive Scripting Language) code blocks
// Usage: ```isl ... ```

(function (Prism) {
  Prism.languages.isl = {
    'comment': [
      {
        pattern: /\/\*[\s\S]*?\*\//,
        greedy: true
      },
      {
        pattern: /\/\/.*/,
        greedy: true
      },
      {
        pattern: /#.*/,
        greedy: true
      }
    ],
    'string': [
      {
        pattern: /`(?:[^`\\$]|\\[\s\S]|\$(?!\{)|\$\{(?:[^{}]|\{(?:[^{}]|\{[^}]*\})*\})*\})*`/,
        greedy: true,
        inside: {
          'interpolation': {
            pattern: /\$\{[^}]+\}|\{\{[^}]+\}\}/,
            inside: {
              'interpolation-punctuation': {
                pattern: /^\$\{|\{?\{|\}\}?$/,
                alias: 'punctuation'
              },
              rest: Prism.languages.isl
            }
          },
          'variable': /\$[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*/,
          'function': /@\.[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*/
        }
      },
      {
        pattern: /"(?:\\.|[^"\\])*"/,
        greedy: true
      },
      {
        pattern: /'(?:\\.|[^'\\])*'/,
        greedy: true
      }
    ],
    'regex': {
      pattern: /\/(?![/*])(?:[^\\\n\/]|\\.)+\//,
      greedy: true
    },
    'keyword': /\b(?:if|else|endif|switch|endswitch|foreach|endfor|while|endwhile|parallel|fun|modifier|return|cache|import|type|as|from|and|or|not|in)\b/,
    'builtin': /\b(?:filter|map|reduce|string|number|integer|boolean|object|array|any|null|text|date|datetime|binary)\b/,
    'boolean': /\b(?:true|false)\b/,
    'null': /\bnull\b/,
    'number': /-?\b\d+(?:\.\d+)?\b/,
    'variable': /\$[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*/,
    'function': [
      /@\.[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*/,
      /\b[A-Za-z_]\w*(?=\()/
    ],
    'modifier': {
      pattern: /\|\s*(?:(?:to|date|Math|xml|csv|regex|encode|decode)\.\w+|[A-Za-z_]\w*)/,
      inside: {
        'punctuation': /\|/,
        'namespace': /\b(?:to|date|Math|xml|csv|regex|encode|decode)\b/,
        'function': /\.\w+|\b[A-Za-z_]\w*/
      }
    },
    'operator': [
      /==|!=|<=|>=|<|>/,
      /!(?:contains|startsWith|endsWith|in|is|matches)/,
      /\b(?:contains|startsWith|endsWith|matches|is)\b/,
      /\?\?/,
      /->/,
      /\.\.\./,
      /[+\-*\/=]/
    ],
    'punctuation': /[{}()\[\]:;,.]/,
    'math-expression': {
      pattern: /\{\{[\s\S]*?\}\}/,
      inside: {
        'punctuation': /^\{\{|\}\}$/,
        'variable': /\$[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*/,
        'number': /-?\b\d+(?:\.\d+)?\b/,
        'operator': /[+\-*\/()]/,
        'function': /@\.[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*/
      }
    }
  };

  // Add isl as an alias
  Prism.languages.isl.string[0].inside.interpolation.inside.rest = Prism.languages.isl;
}(Prism));

