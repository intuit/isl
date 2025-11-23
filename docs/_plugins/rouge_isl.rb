# frozen_string_literal: true

# Rouge lexer for ISL (Intuitive Scripting Language)
# ISL is a JSON transformation language
# Docs: https://intuit.github.io/isl/

module Rouge
  module Lexers
    class Isl < RegexLexer
      title "ISL"
      desc "ISL - Intuitive Scripting Language for JSON transformations"
      tag 'isl'
      filenames '*.isl'
      mimetypes 'text/x-isl', 'application/x-isl'

      state :root do
        # Comments
        rule %r(//.*$), Comment::Single
        rule %r(#.*$), Comment::Single
        rule %r(/\*), Comment::Multiline, :comment

        # Keywords
        rule %r/\b(if|else|endif|switch|endswitch|foreach|endfor|while|endwhile|parallel)\b/, Keyword::Control
        rule %r/\b(fun|modifier|return|cache)\b/, Keyword::Declaration
        rule %r/\b(import|type|as|from)\b/, Keyword::Namespace
        rule %r/\b(and|or|not|in)\b/, Operator::Word
        rule %r/\b(contains|startsWith|endsWith|matches|is)\b/, Operator::Word
        rule %r/\b(filter|map|reduce)\b/, Name::Function

        # Built-in types
        rule %r/\b(string|number|integer|boolean|object|array|any|null|text|date|datetime|binary)\b/, Keyword::Type

        # Constants
        rule %r/\b(true|false)\b/, Keyword::Constant
        rule %r/\bnull\b/, Keyword::Constant

        # Backtick strings with interpolation
        rule %r/`/, Str::Backtick, :backtick_string

        # Regular strings
        rule %r/"/, Str::Double, :dq_string
        rule %r/'/, Str::Single, :sq_string

        # Regular expressions
        rule %r((/)((?:\\.|[^/\\\n])+)(/)), Str::Regex

        # Numbers
        rule %r/-?\d+\.?\d*/, Num

        # Variables
        rule %r/(\$)([A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*)/ do
          groups Punctuation, Name::Variable
        end

        # Function calls (@.Service.method)
        rule %r/(@\.)([A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*)/ do
          groups Punctuation, Name::Function
        end

        # Modifiers (pipe operator)
        rule %r/\|/, Operator, :modifier

        # Math expressions
        rule %r/\{\{/, Punctuation, :math_expr

        # Operators
        rule %r/==|!=|<=|>=|<|>/, Operator
        rule %r/!contains|!startsWith|!endsWith|!in|!is|!matches/, Operator::Word
        rule %r/\?\?/, Operator
        rule %r/->/, Operator
        rule %r/\.\.\./, Operator
        rule %r/[+\-*\/]/, Operator

        # Punctuation
        rule %r/[{}()\[\]:;,.]/, Punctuation
        rule %r/=/, Operator

        # Whitespace
        rule %r/\s+/, Text
      end

      state :comment do
        rule %r/\*\//, Comment::Multiline, :pop!
        rule %r/[^*\/]+/, Comment::Multiline
        rule %r/[*\/]/, Comment::Multiline
      end

      state :backtick_string do
        rule %r/`/, Str::Backtick, :pop!
        # String interpolation ${...}
        rule %r/\$\{/, Punctuation, :interpolation
        # Math expression {{...}}
        rule %r/\{\{/, Punctuation, :math_expr_string
        # Simple variable interpolation
        rule %r/\$[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*/, Name::Variable
        # Function calls in strings
        rule %r/@\.[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*/, Name::Function
        rule %r/\\./, Str::Escape
        rule %r/[^`$@{\\]+/, Str::Backtick
        rule %r/[$@{]/, Str::Backtick
      end

      state :interpolation do
        rule %r/\}/, Punctuation, :pop!
        mixin :root
      end

      state :math_expr_string do
        rule %r/\}\}/, Punctuation, :pop!
        mixin :math_expr_common
      end

      state :math_expr do
        rule %r/\}\}/, Punctuation, :pop!
        mixin :math_expr_common
      end

      state :math_expr_common do
        rule %r/\$[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*/, Name::Variable
        rule %r/-?\d+\.?\d*/, Num
        rule %r/[+\-*\/()]/, Operator
        rule %r/@\.[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*/, Name::Function
        rule %r/\s+/, Text
      end

      state :modifier do
        rule %r/\s+/, Text
        # Namespaced modifiers (to.string, date.parse, etc.)
        rule %r/\b(to|date|Math|xml|csv|regex|encode|decode)\.(\w+)/ do
          groups Name::Namespace, Name::Function
          pop!
        end
        # Simple modifiers
        rule %r/[A-Za-z_]\w*/, Name::Function, :pop!
      end

      state :dq_string do
        rule %r/"/, Str::Double, :pop!
        rule %r/\\./, Str::Escape
        rule %r/[^"\\]+/, Str::Double
      end

      state :sq_string do
        rule %r/'/, Str::Single, :pop!
        rule %r/\\./, Str::Escape
        rule %r/[^'\\]+/, Str::Single
      end
    end
  end
end

