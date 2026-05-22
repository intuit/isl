namespace Isl.Parser;

public enum TokenType
{
    // Keywords
    If, Else, EndIf, Switch, Arrow, EndSwitch,
    Filter, Map,
    EqualEqual, NotEqual, LessOrEqual, GreaterOrEqual, Greater, Less,
    Contains, NotContains, StartsWith, NotStartsWith, EndsWith, NotEndsWith,
    In, NotIn, Is, NotIs, Matches, NotMatches,
    Parallel, Foreach, EndFor,
    While, EndWhile,
    Fun, Modifier, Return, Cache,
    Import, TypeDecl, As, From,
    And, Or,

    // Symbols
    Bang, Colon, Equal, Semicolon, Pipe, Comma, Dollar, Backslash, At,
    Spread, Dot, Coalesce,
    OpenParen, CloseParen,
    ArrayCondOpen, ArrayCondClose,
    SquareOpen, SquareClose,
    CurlyOpen, CurlyClose,
    CurlyOpenOpen, CurlyCloseClose,
    MathTimes, MathDiv, MathPlus, MathMinus,

    // Literals
    Bool, Num, Id, QuotedString,

    // Backtick / interpolation
    OpenBacktick, CloseBacktick,
    EnterExprInterp, EnterMathInterp, EnterFuncInterp,
    IdInterp, Text, RecoverTokens,

    Eof, Unknown
}

public record Token(TokenType Type, string Value, int Line, int Col);

/// <summary>
/// Hand-rolled lexer for ISL, matching the ANTLR grammar behaviour.
/// Modes: 0 = DEFAULT, 1 = INTERPOLATE (inside backtick string)
/// Nesting counters handle ${...} and {{...}} inside backtick mode.
/// </summary>
public class IslLexer
{
    private readonly string _src;
    private int _pos;
    private int _line = 1;
    private int _col = 1;

    // Mode stack – values: 0 = DEFAULT, 1 = INTERPOLATE
    private readonly Stack<int> _modeStack = new();
    // Nesting counters tracking how many DEFAULT-mode opens we pushed
    // for each interpolation kind, so we can pop on the matching close.
    private int _exprInterp = 0;   // tracks ${ ... } nesting inside interpolate
    private int _mathInterp = 0;   // tracks {{ ... }} nesting inside interpolate
    private int _funcInterp = 0;   // tracks @. ... () nesting inside interpolate
    // Brace depth counter when inside ${ } to distinguish inner { } from the closing }
    private readonly Stack<int> _braceDepthStack = new(); // brace depth at each expr interp level

    private int Mode => _modeStack.Count > 0 ? _modeStack.Peek() : 0;

    public IslLexer(string src)
    {
        _src = src;
        _pos = 0;
    }

    private char Cur => _pos < _src.Length ? _src[_pos] : '\0';
    private char Peek(int offset = 1) => _pos + offset < _src.Length ? _src[_pos + offset] : '\0';

    private char Advance()
    {
        char c = _src[_pos++];
        if (c == '\n') { _line++; _col = 1; } else _col++;
        return c;
    }

    private string Consume(int n)
    {
        var sb = new System.Text.StringBuilder();
        for (int i = 0; i < n; i++) sb.Append(Advance());
        return sb.ToString();
    }

    private bool Match(string s)
    {
        if (_pos + s.Length > _src.Length) return false;
        return _src.Substring(_pos, s.Length) == s;
    }

    public List<Token> Tokenize()
    {
        var tokens = new List<Token>();
        while (true)
        {
            var tok = NextToken();
            tokens.Add(tok);
            if (tok.Type == TokenType.Eof) break;
        }
        return tokens;
    }

    private Token MakeToken(TokenType t, string v, int line, int col) => new(t, v, line, col);

    private Token NextToken()
    {
        if (_pos >= _src.Length) return MakeToken(TokenType.Eof, "", _line, _col);

        if (Mode == 1) // INTERPOLATE mode
            return NextInterpolateToken();
        return NextDefaultToken();
    }

    // ── INTERPOLATE MODE ──────────────────────────────────────────────────────

    private Token NextInterpolateToken()
    {
        if (_pos >= _src.Length) return MakeToken(TokenType.Eof, "", _line, _col);

        int sl = _line, sc = _col;

        // Close backtick
        if (Cur == '`')
        {
            Advance();
            if (_modeStack.Count > 0) _modeStack.Pop();
            return MakeToken(TokenType.CloseBacktick, "`", sl, sc);
        }

        // ${ → enter expr interp
        if (Cur == '$' && Peek() == '{')
        {
            Consume(2);
            _exprInterp++;
            _modeStack.Push(0);
            _braceDepthStack.Push(0); // start brace tracking at depth 0
            return MakeToken(TokenType.EnterExprInterp, "${", sl, sc);
        }

        // {{ → enter math interp
        if (Cur == '{' && Peek() == '{')
        {
            Consume(2);
            _mathInterp++;
            _modeStack.Push(0);
            return MakeToken(TokenType.EnterMathInterp, "{{", sl, sc);
        }

        // @. → enter func call interp
        if (Cur == '@' && Peek() == '.')
        {
            Consume(2);
            _funcInterp++;
            _modeStack.Push(0);
            return MakeToken(TokenType.EnterFuncInterp, "@.", sl, sc);
        }

        // $identifier
        if (Cur == '$' && (char.IsLetter(Peek()) || Peek() == '_'))
        {
            Advance(); // consume $
            var id = ReadIdRaw();
            return MakeToken(TokenType.IdInterp, "$" + id, sl, sc);
        }

        // TEXT – everything until special chars
        var sb = new System.Text.StringBuilder();
        while (_pos < _src.Length
               && Cur != '`'
               && !(Cur == '$' && (Peek() == '{' || char.IsLetter(Peek()) || Peek() == '_'))
               && !(Cur == '{' && Peek() == '{')
               && !(Cur == '@' && Peek() == '.'))
        {
            if (Cur == '\\' && Peek() == '$') { Advance(); Advance(); sb.Append('$'); continue; }
            sb.Append(Advance());
        }
        if (sb.Length > 0)
            return MakeToken(TokenType.Text, sb.ToString(), sl, sc);

        // Single recover token
        return MakeToken(TokenType.RecoverTokens, Advance().ToString(), sl, sc);
    }

    // ── DEFAULT MODE ──────────────────────────────────────────────────────────

    private Token NextDefaultToken()
    {
        // Skip whitespace and line comments
        while (_pos < _src.Length)
        {
            if (char.IsWhiteSpace(Cur)) { Advance(); continue; }
            if (Cur == '#' || (Cur == '/' && Peek() == '/')) { while (_pos < _src.Length && Cur != '\n') Advance(); continue; }
            break;
        }

        if (_pos >= _src.Length) return MakeToken(TokenType.Eof, "", _line, _col);

        int sl = _line, sc = _col;

        // Backtick → open interpolate mode
        if (Cur == '`')
        {
            Advance();
            _modeStack.Push(1);
            return MakeToken(TokenType.OpenBacktick, "`", sl, sc);
        }

        // Multi-char tokens (longest match first)
        if (Match("..."))  { Consume(3); return MakeToken(TokenType.Spread, "...", sl, sc); }
        if (Match("??"))   { Consume(2); return MakeToken(TokenType.Coalesce, "??", sl, sc); }
        if (Match("=="))   { Consume(2); return MakeToken(TokenType.EqualEqual, "==", sl, sc); }
        if (Match("!="))   { Consume(2); return MakeToken(TokenType.NotEqual, "!=", sl, sc); }
        if (Match("<="))   { Consume(2); return MakeToken(TokenType.LessOrEqual, "<=", sl, sc); }
        if (Match(">="))   { Consume(2); return MakeToken(TokenType.GreaterOrEqual, ">=", sl, sc); }
        if (Cur == '<')    { Advance(); return MakeToken(TokenType.Less, "<", sl, sc); }
        if (Cur == '>')    { Advance(); return MakeToken(TokenType.Greater, ">", sl, sc); }
        if (Match("->"))   { Consume(2); return MakeToken(TokenType.Arrow, "->", sl, sc); }
        if (Match("[("))   { Consume(2); return MakeToken(TokenType.ArrayCondOpen, "[(", sl, sc); }
        if (Match(")]"))   { Consume(2); return MakeToken(TokenType.ArrayCondClose, ")]", sl, sc); }
        if (Match("{{"))   { Consume(2); return MakeToken(TokenType.CurlyOpenOpen, "{{", sl, sc); }
        if (Match("}}"))
        {
            Consume(2);
            // If we were inside a math interp, close it
            if (_mathInterp > 0)
            {
                _mathInterp--;
                if (_modeStack.Count > 0) _modeStack.Pop();
            }
            return MakeToken(TokenType.CurlyCloseClose, "}}", sl, sc);
        }

        // Single character tokens
        switch (Cur)
        {
            case '!':
            {
                Advance();
                if (Match("contains"))  { Consume(8);  return MakeToken(TokenType.NotContains, "!contains", sl, sc); }
                if (Match("in"))        { Consume(2);  return MakeToken(TokenType.NotIn, "!in", sl, sc); }
                if (Match("is"))        { Consume(2);  return MakeToken(TokenType.NotIs, "!is", sl, sc); }
                if (Match("startsWith")){ Consume(10); return MakeToken(TokenType.NotStartsWith, "!startsWith", sl, sc); }
                if (Match("endsWith"))  { Consume(8);  return MakeToken(TokenType.NotEndsWith, "!endsWith", sl, sc); }
                if (Match("matches"))   { Consume(7);  return MakeToken(TokenType.NotMatches, "!matches", sl, sc); }
                return MakeToken(TokenType.Bang, "!", sl, sc);
            }
            case ':': Advance(); return MakeToken(TokenType.Colon, ":", sl, sc);
            case '=': Advance(); return MakeToken(TokenType.Equal, "=", sl, sc);
            case ';': Advance(); return MakeToken(TokenType.Semicolon, ";", sl, sc);
            case '|': Advance(); return MakeToken(TokenType.Pipe, "|", sl, sc);
            case ',': Advance(); return MakeToken(TokenType.Comma, ",", sl, sc);
            case '$': Advance(); return MakeToken(TokenType.Dollar, "$", sl, sc);
            case '\\': Advance(); return MakeToken(TokenType.Backslash, "\\", sl, sc);
            case '@': Advance(); return MakeToken(TokenType.At, "@", sl, sc);
            case '.': Advance(); return MakeToken(TokenType.Dot, ".", sl, sc);
            case '(': Advance(); return MakeToken(TokenType.OpenParen, "(", sl, sc);
            case ')':
            {
                Advance();
                // If we were inside a func interp, pop back to INTERPOLATE
                if (_funcInterp > 0)
                {
                    _funcInterp--;
                    if (_modeStack.Count > 0) _modeStack.Pop();
                }
                return MakeToken(TokenType.CloseParen, ")", sl, sc);
            }
            case '[': Advance(); return MakeToken(TokenType.SquareOpen, "[", sl, sc);
            case ']': Advance(); return MakeToken(TokenType.SquareClose, "]", sl, sc);
            case '{':
            {
                Advance();
                // Track inner brace depth for expr interp
                if (_exprInterp > 0 && _braceDepthStack.Count > 0)
                {
                    var d = _braceDepthStack.Pop();
                    _braceDepthStack.Push(d + 1);
                }
                return MakeToken(TokenType.CurlyOpen, "{", sl, sc);
            }
            case '}':
            {
                Advance();
                if (_exprInterp > 0 && _braceDepthStack.Count > 0)
                {
                    var d = _braceDepthStack.Peek();
                    if (d == 0)
                    {
                        // This } closes the ${ interpolation
                        _braceDepthStack.Pop();
                        _exprInterp--;
                        if (_modeStack.Count > 0) _modeStack.Pop();
                    }
                    else
                    {
                        _braceDepthStack.Pop();
                        _braceDepthStack.Push(d - 1);
                    }
                }
                return MakeToken(TokenType.CurlyClose, "}", sl, sc);
            }
            case '*': Advance(); return MakeToken(TokenType.MathTimes, "*", sl, sc);
            case '/': Advance(); return MakeToken(TokenType.MathDiv, "/", sl, sc);
            case '+': Advance(); return MakeToken(TokenType.MathPlus, "+", sl, sc);
        }

        // Numbers
        if (char.IsDigit(Cur) || (Cur == '-' && char.IsDigit(Peek())))
            return ReadNumber(sl, sc);

        // Standalone minus
        if (Cur == '-') { Advance(); return MakeToken(TokenType.MathMinus, "-", sl, sc); }

        // Quoted strings
        if (Cur == '"' || Cur == '\'')
            return ReadQuotedString(sl, sc);

        // Identifier / keyword
        if (char.IsLetter(Cur) || Cur == '_')
        {
            var id = ReadIdRaw();
            var type = id switch
            {
                "if"       => TokenType.If,
                "else"     => TokenType.Else,
                "endif"    => TokenType.EndIf,
                "switch"   => TokenType.Switch,
                "endswitch"=> TokenType.EndSwitch,
                "filter"   => TokenType.Filter,
                "map"      => TokenType.Map,
                "contains" => TokenType.Contains,
                "startsWith"=> TokenType.StartsWith,
                "endsWith" => TokenType.EndsWith,
                "in"       => TokenType.In,
                "is"       => TokenType.Is,
                "matches"  => TokenType.Matches,
                "parallel" => TokenType.Parallel,
                "foreach"  => TokenType.Foreach,
                "endfor"   => TokenType.EndFor,
                "while"    => TokenType.While,
                "endwhile" => TokenType.EndWhile,
                "fun"      => TokenType.Fun,
                "modifier" => TokenType.Modifier,
                "return"   => TokenType.Return,
                "cache"    => TokenType.Cache,
                "import"   => TokenType.Import,
                "type"     => TokenType.TypeDecl,
                "as"       => TokenType.As,
                "from"     => TokenType.From,
                "and"      => TokenType.And,
                "or"       => TokenType.Or,
                "false" or "true" or "null" => TokenType.Bool,
                _ => TokenType.Id
            };
            return MakeToken(type, id, sl, sc);
        }

        return MakeToken(TokenType.Unknown, Advance().ToString(), sl, sc);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private string ReadIdRaw()
    {
        var sb = new System.Text.StringBuilder();
        while (_pos < _src.Length && (char.IsLetterOrDigit(Cur) || Cur == '_'))
            sb.Append(Advance());
        return sb.ToString();
    }

    private Token ReadNumber(int sl, int sc)
    {
        var sb = new System.Text.StringBuilder();
        if (Cur == '-') sb.Append(Advance());
        while (_pos < _src.Length && char.IsDigit(Cur)) sb.Append(Advance());
        if (Cur == '.')
        {
            sb.Append(Advance());
            while (_pos < _src.Length && char.IsDigit(Cur)) sb.Append(Advance());
        }
        return MakeToken(TokenType.Num, sb.ToString(), sl, sc);
    }

    private Token ReadQuotedString(int sl, int sc)
    {
        char delim = Advance();
        var sb = new System.Text.StringBuilder();
        while (_pos < _src.Length && Cur != delim)
        {
            if (Cur == '\\' && _pos + 1 < _src.Length)
            {
                char next = Peek();
                // Standard escape sequences - convert to actual character
                switch (next)
                {
                    case 'n': Advance(); Advance(); sb.Append('\n'); break;
                    case 'r': Advance(); Advance(); sb.Append('\r'); break;
                    case 't': Advance(); Advance(); sb.Append('\t'); break;
                    case '\\': Advance(); Advance(); sb.Append('\\'); break;
                    case '"': Advance(); Advance(); sb.Append('"'); break;
                    case '\'': Advance(); Advance(); sb.Append('\''); break;
                    case '0': Advance(); Advance(); sb.Append('\0'); break;
                    default:
                        // Unknown escape: preserve the backslash (e.g. \s, \d for regex patterns)
                        sb.Append(Advance()); // '\'
                        sb.Append(Advance()); // next char
                        break;
                }
            }
            else sb.Append(Advance());
        }
        if (_pos < _src.Length) Advance(); // closing delimiter
        return MakeToken(TokenType.QuotedString, sb.ToString(), sl, sc);
    }
}
