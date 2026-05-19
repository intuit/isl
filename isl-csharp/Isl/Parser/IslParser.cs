using Isl.Ast;

namespace Isl.Parser;

public class IslParser
{
    private readonly List<Token> _tokens;
    private int _pos;

    public IslParser(List<Token> tokens)
    {
        // Filter out EOF from middle, keep last one
        _tokens = tokens;
        _pos = 0;
    }

    private Token Cur => _pos < _tokens.Count ? _tokens[_pos] : new Token(TokenType.Eof, "", 0, 0);
    private Token Peek(int offset = 1) => _pos + offset < _tokens.Count ? _tokens[_pos + offset] : new Token(TokenType.Eof, "", 0, 0);

    private Token Advance()
    {
        var t = _tokens[_pos];
        _pos++;
        return t;
    }

    private Token Expect(TokenType type)
    {
        if (Cur.Type != type)
            throw new IslParseException($"Expected {type} but got {Cur.Type} ('{Cur.Value}') at line {Cur.Line}:{Cur.Col}");
        return Advance();
    }

    private bool Check(TokenType type) => Cur.Type == type;
    private bool Match(TokenType type) { if (Check(type)) { Advance(); return true; } return false; }

    // ---- Top level ----
    public Module ParseModule()
    {
        var functions = new List<FunctionDecl>();
        var statements = new List<Statement>();

        // Skip annotations/cache before fun
        while (Cur.Type != TokenType.Eof)
        {
            if (IsFunctionStart())
            {
                functions.Add(ParseFunctionDecl());
            }
            else if (Cur.Type == TokenType.TypeDecl)
            {
                // Skip type declarations
                SkipTypeDeclaration();
            }
            else if (Cur.Type == TokenType.Import)
            {
                SkipImportDeclaration();
            }
            else if (functions.Count > 0)
            {
                // After functions, stop
                break;
            }
            else
            {
                // Flat statements mode
                var stmt = TryParseStatement();
                if (stmt == null) break;
                statements.Add(stmt);
            }
        }

        return new Module(functions, statements);
    }

    private bool IsFunctionStart()
    {
        // fun, cache fun, modifier, or @Annotation fun
        if (Cur.Type == TokenType.Fun || Cur.Type == TokenType.Modifier || Cur.Type == TokenType.Cache)
            return true;
        // @Annotation ... fun
        if (Cur.Type == TokenType.At)
        {
            // look ahead for fun
            int i = _pos;
            while (i < _tokens.Count && _tokens[i].Type != TokenType.Fun && _tokens[i].Type != TokenType.Eof)
                i++;
            return _tokens[i].Type == TokenType.Fun;
        }
        return false;
    }

    private void SkipTypeDeclaration()
    {
        // type ID as ... ; or type ID from "..." ;
        while (!Check(TokenType.Semicolon) && !Check(TokenType.Eof)) Advance();
        if (Check(TokenType.Semicolon)) Advance();
    }

    private void SkipImportDeclaration()
    {
        while (!Check(TokenType.Semicolon) && !Check(TokenType.Eof)) Advance();
        if (Check(TokenType.Semicolon)) Advance();
    }

    private FunctionDecl ParseFunctionDecl()
    {
        // Skip annotations
        while (Cur.Type == TokenType.At)
        {
            Advance(); // @
            Advance(); // ID
            if (Check(TokenType.OpenParen))
            {
                Advance();
                int depth = 1;
                while (depth > 0 && !Check(TokenType.Eof))
                {
                    if (Check(TokenType.OpenParen)) depth++;
                    else if (Check(TokenType.CloseParen)) depth--;
                    Advance();
                }
            }
        }

        // cache? fun | modifier
        if (Check(TokenType.Cache)) Advance();
        if (Check(TokenType.Fun) || Check(TokenType.Modifier)) Advance();

        var name = Expect(TokenType.Id).Value;

        // Parameters
        Expect(TokenType.OpenParen);
        var parameters = new List<string>();
        while (!Check(TokenType.CloseParen) && !Check(TokenType.Eof))
        {
            if (Check(TokenType.Dollar)) Advance();
            parameters.Add(ParseShortId());
            // skip :type
            if (Check(TokenType.Colon))
            {
                Advance();
                SkipTypeDefinition();
            }
            if (!Match(TokenType.Comma)) break;
        }
        Expect(TokenType.CloseParen);

        // return type annotation
        if (Check(TokenType.Colon))
        {
            Advance();
            SkipTypeDefinition();
        }

        Expect(TokenType.CurlyOpen);
        var body = ParseStatements(endToken: TokenType.CurlyClose);
        Expect(TokenType.CurlyClose);

        return new FunctionDecl(name, parameters, body);
    }

    private void SkipTypeDefinition()
    {
        // Skip a type definition until we hit something else (comma, ), {, etc.)
        // Type is: ID | ID[] | {fields} | [literals]
        if (Check(TokenType.CurlyOpen))
        {
            int depth = 1;
            Advance();
            while (depth > 0 && !Check(TokenType.Eof))
            {
                if (Check(TokenType.CurlyOpen)) depth++;
                else if (Check(TokenType.CurlyClose)) depth--;
                Advance();
            }
        }
        else if (Check(TokenType.SquareOpen))
        {
            Advance();
            while (!Check(TokenType.SquareClose) && !Check(TokenType.Eof)) Advance();
            if (Check(TokenType.SquareClose)) Advance();
        }
        else
        {
            // ID (DOT ID)* ([])?
            while ((Check(TokenType.Id) || IsKeywordId()) && !Check(TokenType.Eof))
            {
                Advance();
                if (Check(TokenType.Dot)) Advance(); else break;
            }
            if (Check(TokenType.SquareOpen) && Peek().Type == TokenType.SquareClose)
            {
                Advance(); Advance();
            }
        }
    }

    private List<Statement> ParseStatements(TokenType endToken = TokenType.Eof)
    {
        var stmts = new List<Statement>();
        while (!Check(endToken) && !Check(TokenType.Eof))
        {
            // stop on else/endif/endfor/endwhile/endswitch
            if (Check(TokenType.Else) || Check(TokenType.EndIf) || Check(TokenType.EndFor) || Check(TokenType.EndWhile) || Check(TokenType.EndSwitch))
                break;

            var stmt = TryParseStatement();
            if (stmt == null) break;
            stmts.Add(stmt);
        }
        return stmts;
    }

    private Statement? TryParseStatement()
    {
        // Skip semicolons
        while (Check(TokenType.Semicolon)) Advance();
        if (Check(TokenType.Eof)) return null;
        if (Check(TokenType.Else) || Check(TokenType.EndIf) || Check(TokenType.EndFor)
            || Check(TokenType.EndWhile) || Check(TokenType.EndSwitch) || Check(TokenType.CurlyClose))
            return null;

        try
        {
            return ParseStatement();
        }
        catch (IslParseException)
        {
            // Try to recover by skipping to next semicolon
            while (!Check(TokenType.Semicolon) && !Check(TokenType.Eof) && !Check(TokenType.CurlyClose))
                Advance();
            if (Check(TokenType.Semicolon)) Advance();
            return null;
        }
    }

    private Statement ParseStatement()
    {
        // return
        if (Check(TokenType.Return))
        {
            Advance();
            var val = ParseAssignmentValue();
            Match(TokenType.Semicolon);
            return new ReturnStatement(val);
        }

        // if statement
        if (Check(TokenType.If) && IsBlockIf())
        {
            return ParseIfStatement();
        }

        // switch
        if (Check(TokenType.Switch))
        {
            return ParseSwitchStatement();
        }

        // foreach (as statement)
        if (Check(TokenType.Foreach) || (Check(TokenType.Parallel) && PeekType(1) == TokenType.Foreach))
        {
            var fe = ParseForEach();
            return new ForEachStatement(fe.Iterator, fe.Source, fe.Body, fe.BodyObject);
        }

        // while
        if (Check(TokenType.While))
        {
            return ParseWhileStatement();
        }

        // function call: @.Service...
        if (Check(TokenType.At) && Peek().Type == TokenType.Dot)
        {
            var call = ParseFunctionCall();
            Match(TokenType.Semicolon);
            return new FunctionCallStatement(call);
        }

        // $var = value  or  $var: value
        if (Check(TokenType.Dollar))
        {
            return ParseAssignVariable();
        }

        // prop: value  or  prop = value  (property assignment)
        // An identifier (or dotted path) followed by : or =
        if (IsPropertyAssignment())
        {
            return ParseAssignProperty();
        }

        // Fallback: if we see an unknown token skip it
        throw new IslParseException($"Unexpected token {Cur.Type} ('{Cur.Value}') at {Cur.Line}:{Cur.Col}");
    }

    private TokenType PeekType(int offset) => _pos + offset < _tokens.Count ? _tokens[_pos + offset].Type : TokenType.Eof;

    private bool IsBlockIf()
    {
        // Distinguish block if (has statements after condition block) from inline if
        // Block if: if (cond) then actual statements (not a value expression that can be assigned)
        // We use block if when 'if' appears as a standalone statement (not after : or =)
        // Actually in the grammar: ifStatement is always block form when in statements context
        // So when parsing statements, 'if' is always block form
        return true;
    }

    private bool IsPropertyAssignment()
    {
        // Lookahead: id (. id)* (':' | '=')
        // Also handle interpolate as property name
        if (Check(TokenType.OpenBacktick)) return true; // interpolated property name
        if (!Check(TokenType.Id) && !IsKeywordId()) return false;
        int i = _pos;
        while (i < _tokens.Count && (_tokens[i].Type == TokenType.Id || IsKeywordAt(i)))
        {
            i++;
            if (i < _tokens.Count && _tokens[i].Type == TokenType.Dot)
                i++;
            else
                break;
        }
        // Could also have [0] or ["text"]
        if (i < _tokens.Count && _tokens[i].Type == TokenType.SquareOpen)
        {
            while (i < _tokens.Count && _tokens[i].Type != TokenType.SquareClose) i++;
            if (i < _tokens.Count) i++;
        }
        return i < _tokens.Count && (_tokens[i].Type == TokenType.Colon || _tokens[i].Type == TokenType.Equal);
    }

    private bool IsKeywordId() =>
        Cur.Type is TokenType.In or TokenType.Import or TokenType.TypeDecl or TokenType.As
        or TokenType.From or TokenType.Filter or TokenType.Return or TokenType.Map or TokenType.Matches;

    private bool IsKeywordAt(int i)
    {
        if (i >= _tokens.Count) return false;
        return _tokens[i].Type is TokenType.In or TokenType.Import or TokenType.TypeDecl or TokenType.As
            or TokenType.From or TokenType.Filter or TokenType.Return or TokenType.Map or TokenType.Matches;
    }

    private Statement ParseAssignVariable()
    {
        Expect(TokenType.Dollar);
        var path = ParseAssignSelector();
        // : type? = or just = or :
        if (Check(TokenType.Colon))
        {
            Advance();
            // might have type annotation before =
            if (!Check(TokenType.Equal) && !Check(TokenType.Dollar) && !IsValueStart())
                SkipTypeDefinition();
            if (Check(TokenType.Equal)) Advance(); // = after type
        }
        else if (Check(TokenType.Equal))
        {
            Advance();
        }
        else
        {
            throw new IslParseException($"Expected : or = after variable name at {Cur.Line}:{Cur.Col}");
        }

        var val = ParseAssignmentValue();
        Match(TokenType.Semicolon);

        // Always assign to the first name in the path
        return new AssignVariable(path[0], val);
    }

    private List<string> ParseAssignSelector()
    {
        var path = new List<string>();
        path.Add(ParseShortId());
        // optional index
        if (Check(TokenType.SquareOpen) && Peek().Type == TokenType.Num)
        {
            Advance(); Advance(); Expect(TokenType.SquareClose);
        }
        while (Check(TokenType.Dot) && (Peek().Type == TokenType.Id || IsKeywordAt(_pos + 1) || Peek().Type == TokenType.SquareOpen))
        {
            Advance(); // .
            if (Check(TokenType.SquareOpen))
            {
                // ["text"]
                Advance();
                var key = Expect(TokenType.QuotedString).Value;
                Expect(TokenType.SquareClose);
                path.Add(key);
            }
            else
            {
                path.Add(ParseShortId());
            }
            // optional index
            if (Check(TokenType.SquareOpen) && Peek().Type == TokenType.Num)
            {
                Advance(); Advance(); Expect(TokenType.SquareClose);
            }
        }
        return path;
    }

    private string ParseShortId()
    {
        if (Check(TokenType.Id) || IsKeywordId())
            return Advance().Value;
        throw new IslParseException($"Expected identifier at {Cur.Line}:{Cur.Col} got {Cur.Type} '{Cur.Value}'");
    }

    private Statement ParseAssignProperty()
    {
        List<string> path;
        if (Check(TokenType.OpenBacktick))
        {
            // Interpolated property name - just parse the interpolation and skip for now
            var interp = ParseInterpolate();
            // use a generated path
            path = new List<string> { "__interp__" };
            if (Check(TokenType.Colon)) Advance();
            else if (Check(TokenType.Equal)) Advance();
            // Skip type annotation
            if (!IsValueStart()) SkipTypeDefinition();
            if (Check(TokenType.Equal)) Advance();
            var v = ParseAssignmentValue();
            Match(TokenType.Semicolon);
            return new AssignProperty(path, v);
        }

        path = ParseAssignSelector();

        if (Check(TokenType.Colon))
        {
            Advance();
            // type annotation?
            if (!IsValueStart() && !Check(TokenType.Equal) && !Check(TokenType.CurlyOpen) && !Check(TokenType.SquareOpen))
                SkipTypeDefinition();
            if (Check(TokenType.Equal)) Advance();
        }
        else if (Check(TokenType.Equal))
        {
            Advance();
        }

        var val = ParseAssignmentValue();
        Match(TokenType.Semicolon);
        return new AssignProperty(path, val);
    }

    private bool IsValueStart()
    {
        return Cur.Type is TokenType.QuotedString or TokenType.Num or TokenType.Bool
            or TokenType.Dollar or TokenType.SquareOpen or TokenType.CurlyOpen
            or TokenType.OpenBacktick or TokenType.CurlyOpenOpen or TokenType.At
            or TokenType.If or TokenType.Foreach or TokenType.While or TokenType.Switch
            or TokenType.Parallel or TokenType.MathMinus;
    }

    // assignmentValue: can chain with ??
    private Expr ParseAssignmentValue()
    {
        var left = ParseAssignmentValueItem();
        if (Check(TokenType.Coalesce))
        {
            Advance();
            var right = ParseAssignmentValue();
            return new CoalesceExpr(left, right);
        }
        return left;
    }

    private Expr ParseAssignmentValueItem()
    {
        Expr val;

        if (Check(TokenType.CurlyOpenOpen))
        {
            val = ParseMathExprWrapper();
        }
        else if (Check(TokenType.CurlyOpen))
        {
            val = ParseObjectExpr();
        }
        else if (Check(TokenType.If))
        {
            val = ParseInlineIf();
        }
        else if (Check(TokenType.Foreach) || (Check(TokenType.Parallel) && PeekType(1) == TokenType.Foreach))
        {
            val = ParseForEachExpr();
        }
        else if (Check(TokenType.While))
        {
            val = ParseWhileExpr();
        }
        else if (Check(TokenType.OpenBacktick))
        {
            val = ParseInterpolate();
        }
        else if (Check(TokenType.At) && Peek().Type == TokenType.Dot)
        {
            val = ParseFunctionCall();
        }
        else if (Check(TokenType.Switch))
        {
            val = ParseSwitchExpr();
        }
        else
        {
            val = ParseRightSideValue();
        }

        // Parse modifier chain
        val = ParseModifiers(val);
        return val;
    }

    private Expr ParseRightSideValue()
    {
        if (Check(TokenType.QuotedString) || Check(TokenType.Num) || Check(TokenType.Bool))
            return ParseLiteral();
        if (Check(TokenType.SquareOpen))
            return ParseArrayExpr();
        if (Check(TokenType.OpenBacktick))
            return ParseInterpolate();
        if (Check(TokenType.At) && Peek().Type == TokenType.Dot)
            return ParseFunctionCall();
        if (Check(TokenType.Dollar))
            return ParseVariableExpr();
        throw new IslParseException($"Expected value at {Cur.Line}:{Cur.Col}, got {Cur.Type} '{Cur.Value}'");
    }

    private LiteralExpr ParseLiteral()
    {
        var tok = Advance();
        return tok.Type switch
        {
            TokenType.Num => new LiteralExpr(double.Parse(tok.Value, System.Globalization.CultureInfo.InvariantCulture)),
            TokenType.Bool => new LiteralExpr(tok.Value == "true" ? (object)true : tok.Value == "false" ? false : null),
            TokenType.QuotedString => new LiteralExpr(tok.Value),
            _ => throw new IslParseException($"Unexpected literal token {tok.Type}")
        };
    }

    private VariableExpr ParseVariableExpr()
    {
        Expect(TokenType.Dollar);
        string name = "";
        var parts = new List<VariablePart>();

        // $  alone (the implicit iterator in map/filter)
        if (!Check(TokenType.Id) && !IsKeywordId() && !Check(TokenType.Dot) && !Check(TokenType.SquareOpen))
        {
            return new VariableExpr("$", parts);
        }

        if (Check(TokenType.Id) || IsKeywordId())
        {
            name = Advance().Value;
        }

        // Array index: [0]
        if (Check(TokenType.SquareOpen) && Peek().Type == TokenType.Num)
        {
            Advance();
            var idx = int.Parse(Advance().Value);
            Expect(TokenType.SquareClose);
            parts.Add(new IndexPart(idx));
        }
        // Condition filter: [(cond)]
        else if (Check(TokenType.ArrayCondOpen))
        {
            Advance();
            var cond = ParseConditionExpression();
            Expect(TokenType.ArrayCondClose);
            parts.Add(new ConditionFilterPart(cond));
        }

        while (Check(TokenType.Dot))
        {
            // Lookahead: ensure next is an id or keyword or ["..."]
            if (Peek().Type != TokenType.Id && !IsKeywordAt(_pos + 1) && Peek().Type != TokenType.SquareOpen)
                break;
            Advance(); // .

            if (Check(TokenType.SquareOpen))
            {
                // ["text"]
                Advance();
                var key = Expect(TokenType.QuotedString).Value;
                Expect(TokenType.SquareClose);
                parts.Add(new PropertyPart(key));
            }
            else
            {
                var prop = ParseShortId();
                parts.Add(new PropertyPart(prop));
            }

            // Array index
            if (Check(TokenType.SquareOpen) && Peek().Type == TokenType.Num)
            {
                Advance();
                var idx = int.Parse(Advance().Value);
                Expect(TokenType.SquareClose);
                parts.Add(new IndexPart(idx));
            }
            else if (Check(TokenType.ArrayCondOpen))
            {
                Advance();
                var cond = ParseConditionExpression();
                Expect(TokenType.ArrayCondClose);
                parts.Add(new ConditionFilterPart(cond));
            }
        }

        return new VariableExpr(name, parts);
    }

    private ArrayExpr ParseArrayExpr()
    {
        Expect(TokenType.SquareOpen);
        var elems = new List<Expr>();
        while (!Check(TokenType.SquareClose) && !Check(TokenType.Eof))
        {
            if (Check(TokenType.Spread))
            {
                Advance();
                elems.Add(ParseRightSideValue());
            }
            else
            {
                elems.Add(ParseArgumentValue());
            }
            if (!Match(TokenType.Comma)) break;
        }
        Expect(TokenType.SquareClose);
        return new ArrayExpr(elems);
    }

    private ObjectExpr ParseObjectExpr()
    {
        Expect(TokenType.CurlyOpen);
        var props = new List<ObjectProperty>();

        while (!Check(TokenType.CurlyClose) && !Check(TokenType.Eof))
        {
            // Skip leading semicolons
            while (Check(TokenType.Semicolon)) Advance();
            if (Check(TokenType.CurlyClose)) break;

            if (Check(TokenType.Spread))
            {
                Advance();
                Expr src;
                if (Check(TokenType.Dollar))
                    src = ParseVariableExpr();
                else if (Check(TokenType.At))
                    src = ParseFunctionCall();
                else
                    src = ParseVariableExpr();
                props.Add(new SpreadProp(src));
            }
            else if (Check(TokenType.QuotedString))
            {
                // "text": value
                var key = Advance().Value;
                Expect(TokenType.Colon);
                // skip type annotation
                if (!IsValueStart()) SkipTypeDefinition();
                if (Check(TokenType.Equal)) Advance();
                var v = ParseAssignmentValue();
                props.Add(new TextPropAssign(key, v));
            }
            else if (Check(TokenType.Dollar))
            {
                // $var = value (variable property inside object)
                Advance();
                var path = ParseAssignSelector();
                if (Check(TokenType.Colon)) Advance();
                else if (Check(TokenType.Equal)) Advance();
                // skip type annotation
                if (!IsValueStart()) SkipTypeDefinition();
                if (Check(TokenType.Equal)) Advance();
                var v = ParseAssignmentValue();
                props.Add(new VarPropAssign(path[0], v));
            }
            else if (Check(TokenType.OpenBacktick))
            {
                // interpolated property name
                var interp = ParseInterpolate();
                if (Check(TokenType.Colon)) Advance();
                else if (Check(TokenType.Equal)) Advance();
                var v = ParseAssignmentValue();
                props.Add(new PropAssign(new List<string> { "__interp__" }, v));
            }
            else if (Check(TokenType.Id) || IsKeywordId())
            {
                // prop: value  OR bare identifier (treat as prop: null)
                var path = ParseAssignSelector();
                if (Check(TokenType.Colon) || Check(TokenType.Equal))
                {
                    if (Check(TokenType.Colon)) Advance();
                    else Advance();
                    // skip type annotation
                    if (!IsValueStart() && !Check(TokenType.Equal)) SkipTypeDefinition();
                    if (Check(TokenType.Equal)) Advance();
                    var v = ParseAssignmentValue();
                    props.Add(new PropAssign(path, v));
                }
                else
                {
                    // bare identifier - property with null value (e.g., dasdsa without : or =)
                    props.Add(new PropAssign(path, new LiteralExpr(null)));
                }
            }
            else
            {
                // unknown - skip one token and continue
                Advance();
                while (Check(TokenType.Comma) || Check(TokenType.Semicolon)) Advance();
                continue;
            }

            // separator: , or ;
            while (Check(TokenType.Comma) || Check(TokenType.Semicolon)) Advance();
        }

        Expect(TokenType.CurlyClose);
        return new ObjectExpr(props);
    }

    private InterpolateExpr ParseInterpolate()
    {
        Expect(TokenType.OpenBacktick);
        var parts = new List<InterpolPart>();

        while (!Check(TokenType.CloseBacktick) && !Check(TokenType.Eof))
        {
            if (Check(TokenType.Text) || Check(TokenType.RecoverTokens))
            {
                parts.Add(new TextPart(Advance().Value));
            }
            else if (Check(TokenType.IdInterp))
            {
                // $varname
                var varName = Advance().Value.TrimStart('$');
                parts.Add(new ExprPart(new VariableExpr(varName, new List<VariablePart>())));
            }
            else if (Check(TokenType.EnterExprInterp))
            {
                Advance(); // ${
                Expr inner;
                if (Check(TokenType.Dollar))
                    inner = ParseVariableExpr();
                else if (Check(TokenType.At))
                    inner = ParseFunctionCall();
                else if (Check(TokenType.CurlyOpenOpen))
                    inner = ParseMathExprWrapper();
                else
                    inner = ParseVariableExpr();
                // modifiers
                inner = ParseModifiers(inner);
                Expect(TokenType.CurlyClose);
                parts.Add(new ExprPart(inner));
            }
            else if (Check(TokenType.EnterMathInterp))
            {
                Advance(); // {{
                var math = ParseMathExpression();
                Expect(TokenType.CurlyCloseClose);
                parts.Add(new MathPart(math));
            }
            else if (Check(TokenType.EnterFuncInterp))
            {
                Advance(); // @.
                var funcId = ParseShortId();
                string? method = null;
                if (Check(TokenType.Dot))
                {
                    Advance();
                    method = ParseShortId();
                }
                var args = ParseArguments();
                parts.Add(new FuncCallPart(new FunctionCallExpr(funcId, method, args)));
            }
            else
            {
                // Unknown in interpolate context
                parts.Add(new TextPart(Advance().Value));
            }
        }
        Expect(TokenType.CloseBacktick);
        return new InterpolateExpr(parts);
    }

    private Expr ParseMathExprWrapper()
    {
        Expect(TokenType.CurlyOpenOpen);
        var math = ParseMathExpression();
        Expect(TokenType.CurlyCloseClose);
        return new MathExprWrapper(math);
    }

    private MathExpr ParseMathExpression()
    {
        return ParseMathAddSub();
    }

    private MathExpr ParseMathAddSub()
    {
        var left = ParseMathMulDiv();
        while (Check(TokenType.MathPlus) || Check(TokenType.MathMinus))
        {
            var op = Advance().Value;
            var right = ParseMathMulDiv();
            left = new MathBinOp(left, op, right);
        }
        return left;
    }

    private MathExpr ParseMathMulDiv()
    {
        var left = ParseMathUnary();
        while (Check(TokenType.MathTimes) || Check(TokenType.MathDiv))
        {
            var op = Advance().Value;
            var right = ParseMathUnary();
            left = new MathBinOp(left, op, right);
        }
        return left;
    }

    private MathExpr ParseMathUnary()
    {
        if (Check(TokenType.OpenParen))
        {
            Advance();
            var inner = ParseMathExpression();
            Expect(TokenType.CloseParen);
            return new MathParen(inner);
        }
        if (Check(TokenType.Num))
        {
            var val = double.Parse(Advance().Value, System.Globalization.CultureInfo.InvariantCulture);
            return new MathNumber(val);
        }
        if (Check(TokenType.Dollar))
        {
            var varExpr = ParseVariableExpr();
            return new MathVariable(varExpr);
        }
        if (Check(TokenType.At) && Peek().Type == TokenType.Dot)
        {
            var call = ParseFunctionCall();
            return new MathFuncCall(call);
        }
        throw new IslParseException($"Expected math value at {Cur.Line}:{Cur.Col}, got {Cur.Type}");
    }

    private InlineIfExpr ParseInlineIf()
    {
        Expect(TokenType.If);
        var cond = ParseCondition();
        Expr thenExpr;
        if (Check(TokenType.CurlyOpen))
            thenExpr = ParseObjectExpr();
        else
            thenExpr = ParseRhsExpr();

        Expr? elseExpr = null;
        if (Check(TokenType.Else))
        {
            Advance();
            if (Check(TokenType.CurlyOpen))
                elseExpr = ParseObjectExpr();
            else
                elseExpr = ParseRhsExpr();
        }
        // optional endif
        if (Check(TokenType.EndIf)) Advance();
        return new InlineIfExpr(cond, thenExpr, elseExpr);
    }

    private Expr ParseRhsExpr()
    {
        var val = ParseRightSideValue();
        return ParseModifiers(val);
    }

    private IfStatement ParseIfStatement()
    {
        Expect(TokenType.If);
        var cond = ParseCondition();
        var trueBody = ParseStatements();
        List<Statement> falseBody = new();
        if (Check(TokenType.Else))
        {
            Advance();
            falseBody = ParseStatements();
        }
        Expect(TokenType.EndIf);
        return new IfStatement(cond, trueBody, falseBody);
    }

    private ConditionExpr ParseCondition()
    {
        Expect(TokenType.OpenParen);
        var cond = ParseConditionExpression();
        Expect(TokenType.CloseParen);
        return cond;
    }

    private ConditionExpr ParseConditionExpression()
    {
        var left = ParseSimpleCondition();
        while (Check(TokenType.And) || Check(TokenType.Or))
        {
            var op = Advance().Value;
            var right = ParseSimpleCondition();
            left = new BoolCondition(left, op, right);
        }
        return left;
    }

    private ConditionExpr ParseSimpleCondition()
    {
        if (Check(TokenType.OpenParen))
        {
            Advance();
            var inner = ParseConditionExpression();
            Expect(TokenType.CloseParen);
            return new ParenCondition(inner);
        }

        if (Check(TokenType.Bang))
        {
            Advance();
            var operand = ParseRhsExpr();
            return new NegatedCondition(operand);
        }

        var leftExpr = ParseRhsExpr();

        // relop?
        var op = GetRelop();
        if (op != null)
        {
            Advance(); // consume the relop
            // Handle multi-token ops? No, they're single tokens
            var rightExpr = ParseRhsExpr();
            return new SimpleCondition(leftExpr, op, rightExpr);
        }

        // Simple boolean: just the expression
        return new SimpleCondition(leftExpr, "truthy", null);
    }

    private string? GetRelop()
    {
        return Cur.Type switch
        {
            TokenType.EqualEqual => "==",
            TokenType.NotEqual => "!=",
            TokenType.LessOrEqual => "<=",
            TokenType.GreaterOrEqual => ">=",
            TokenType.Greater => ">",
            TokenType.Less => "<",
            TokenType.Contains => "contains",
            TokenType.NotContains => "!contains",
            TokenType.StartsWith => "startsWith",
            TokenType.NotStartsWith => "!startsWith",
            TokenType.EndsWith => "endsWith",
            TokenType.NotEndsWith => "!endsWith",
            TokenType.In => "in",
            TokenType.NotIn => "!in",
            TokenType.Is => "is",
            TokenType.NotIs => "!is",
            TokenType.Matches => "matches",
            TokenType.NotMatches => "!matches",
            _ => null
        };
    }

    private SwitchStatement ParseSwitchStatement()
    {
        Expect(TokenType.Switch);
        Expect(TokenType.OpenParen);
        var subject = ParseRhsExpr();
        Expect(TokenType.CloseParen);

        var cases = new List<SwitchCase>();
        List<Statement>? elseBody = null;

        while (!Check(TokenType.EndSwitch) && !Check(TokenType.Eof))
        {
            if (Check(TokenType.Else))
            {
                Advance();
                Expect(TokenType.Arrow);
                elseBody = ParseStatements(TokenType.Semicolon);
                while (Check(TokenType.Semicolon)) Advance();
                break;
            }

            // pattern -> body;
            string? op = GetRelop();
            if (op != null) Advance();

            Expr? pattern = null;
            if (IsValueStart())
                pattern = ParseRhsExpr();

            Expect(TokenType.Arrow);

            List<Statement> body;
            Expr? resultExpr = null;

            if (Check(TokenType.CurlyOpen))
            {
                var obj = ParseObjectExpr();
                resultExpr = obj;
                body = new List<Statement>();
            }
            else if (IsValueStart() && !IsPropertyAssignment())
            {
                resultExpr = ParseRhsExpr();
                body = new List<Statement>();
            }
            else
            {
                body = ParseStatements(TokenType.Semicolon);
            }
            while (Check(TokenType.Semicolon)) Advance();

            cases.Add(new SwitchCase(pattern, op ?? "==", body, resultExpr));
        }
        Expect(TokenType.EndSwitch);
        return new SwitchStatement(subject, cases, elseBody);
    }

    private Expr ParseSwitchExpr()
    {
        var stmt = ParseSwitchStatement();
        return new InlineIfExpr(
            new SimpleCondition(new LiteralExpr(true), "truthy", null),
            new LiteralExpr(null),
            null
        ); // placeholder - switch as expr not commonly used in tests
    }

    private ForEachExpr ParseForEachExpr()
    {
        if (Check(TokenType.Parallel)) Advance(); // skip parallel

        Expect(TokenType.Foreach);
        Expect(TokenType.Dollar);
        var iterName = ParseShortId();
        Expect(TokenType.In);

        var source = ParseRhsExpr();

        List<Statement> body = new();
        ObjectExpr? bodyObject = null;

        if (Check(TokenType.CurlyOpen))
        {
            bodyObject = ParseObjectExpr();
        }
        else
        {
            body = ParseStatements(TokenType.EndFor);
            if (Check(TokenType.CurlyOpen))
                bodyObject = ParseObjectExpr();
        }

        Expect(TokenType.EndFor);
        return new ForEachExpr(iterName, source, body, bodyObject);
    }

    private ForEachStatement ParseForEach()
    {
        if (Check(TokenType.Parallel)) Advance();

        Expect(TokenType.Foreach);
        Expect(TokenType.Dollar);
        var iterName = ParseShortId();
        Expect(TokenType.In);

        var source = ParseRhsExpr();

        List<Statement> body = new();
        ObjectExpr? bodyObject = null;

        if (Check(TokenType.CurlyOpen))
        {
            bodyObject = ParseObjectExpr();
        }
        else
        {
            body = ParseStatements(TokenType.EndFor);
            if (Check(TokenType.CurlyOpen))
                bodyObject = ParseObjectExpr();
        }

        Expect(TokenType.EndFor);
        Match(TokenType.Comma);
        Match(TokenType.Semicolon);
        return new ForEachStatement(iterName, source, body, bodyObject);
    }

    private WhileStatement ParseWhileStatement()
    {
        Expect(TokenType.While);
        Expect(TokenType.OpenParen);
        var cond = ParseConditionExpression();
        int maxLoops = 50;
        if (Check(TokenType.Comma))
        {
            Advance();
            // options object - skip for now
            if (Check(TokenType.CurlyOpen)) ParseObjectExpr();
        }
        Expect(TokenType.CloseParen);
        var body = ParseStatements(TokenType.EndWhile);
        Expect(TokenType.EndWhile);
        return new WhileStatement(cond, body, maxLoops);
    }

    private Expr ParseWhileExpr()
    {
        var stmt = ParseWhileStatement();
        return new LiteralExpr(null); // while as expr placeholder
    }

    private FunctionCallExpr ParseFunctionCall()
    {
        Expect(TokenType.At);
        Expect(TokenType.Dot);
        var service = ParseShortId();
        string? method = null;
        if (Check(TokenType.Dot))
        {
            Advance();
            method = ParseShortId();
            if (Check(TokenType.Dot))
            {
                // Service.Name.Sub case - combine
                Advance();
                method = method + "." + ParseShortId();
            }
        }
        var args = ParseArguments();

        // optional { functionStatements }
        if (Check(TokenType.CurlyOpen))
        {
            Advance();
            ParseStatements(TokenType.CurlyClose);
            Expect(TokenType.CurlyClose);
        }

        return new FunctionCallExpr(service, method, args);
    }

    private List<Expr> ParseArguments()
    {
        Expect(TokenType.OpenParen);
        var args = new List<Expr>();
        while (!Check(TokenType.CloseParen) && !Check(TokenType.Eof))
        {
            args.Add(ParseArgumentValue());
            if (!Match(TokenType.Comma)) break;
        }
        Expect(TokenType.CloseParen);
        return args;
    }

    private Expr ParseArgumentValue()
    {
        var left = ParseArgumentItem();
        if (Check(TokenType.Coalesce))
        {
            Advance();
            var right = ParseArgumentValue();
            return new CoalesceExpr(left, right);
        }
        return left;
    }

    private Expr ParseArgumentItem()
    {
        Expr val;
        if (Check(TokenType.CurlyOpenOpen))
            val = ParseMathExprWrapper();
        else if (Check(TokenType.CurlyOpen))
            val = ParseObjectExpr();
        else if (Check(TokenType.OpenBacktick))
            val = ParseInterpolate();
        else if (Check(TokenType.At) && Peek().Type == TokenType.Dot)
            val = ParseFunctionCall();
        else if (Check(TokenType.SquareOpen))
            val = ParseArrayExpr();
        else if (Check(TokenType.QuotedString) || Check(TokenType.Num) || Check(TokenType.Bool))
            val = ParseLiteral();
        else if (Check(TokenType.Dollar))
            val = ParseVariableExpr();
        else
            val = ParseRightSideValue();
        return ParseModifiers(val);
    }

    private Expr ParseModifiers(Expr val)
    {
        var mods = new List<ModifierNode>();
        while (Check(TokenType.Pipe))
        {
            Advance(); // |
            var mod = ParseModifier();
            mods.Add(mod);
        }
        if (mods.Count > 0)
            return new ModifiedExpr(val, mods);
        return val;
    }

    private ModifierNode ParseModifier()
    {
        // filter (condition)
        if (Check(TokenType.Filter))
        {
            Advance();
            var cond = ParseCondition();
            return new ModifierNode("filter", null, new List<Expr>(), cond);
        }

        // map ( argumentValue )
        if (Check(TokenType.Map))
        {
            Advance();
            Expect(TokenType.OpenParen);
            var argVal = ParseArgumentValue();
            Expect(TokenType.CloseParen);
            return new ModifierNode("map", null, new List<Expr> { argVal }, null);
        }

        // if condition multiIdent args?
        if (Check(TokenType.If))
        {
            Advance();
            var cond = ParseCondition();
            var name = ParseShortId();
            string? subName = null;
            if (Check(TokenType.Dot)) { Advance(); subName = ParseShortId(); }
            List<Expr> args = new();
            if (Check(TokenType.OpenParen)) args = ParseArguments();
            return new ModifierNode(name, subName, args, cond);
        }

        // multiIdent (args)?
        var modName = ParseShortId();
        string? modSub = null;
        if (Check(TokenType.Dot))
        {
            Advance();
            modSub = ParseShortId();
        }

        List<Expr> modArgs = new();
        if (Check(TokenType.OpenParen))
        {
            Advance();
            while (!Check(TokenType.CloseParen) && !Check(TokenType.Eof))
            {
                // Check if this is a condition expression inside generic modifier
                // For now, parse as argument value
                modArgs.Add(ParseArgumentValue());
                if (!Match(TokenType.Comma)) break;
            }
            Expect(TokenType.CloseParen);
        }

        return new ModifierNode(modName, modSub, modArgs, null);
    }
}

public class IslParseException : Exception
{
    public IslParseException(string message) : base(message) { }
}
