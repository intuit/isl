using Isl.Ast;
using Isl.Commands;
using Isl.Commands.Expressions;
using Isl.Commands.Functions;
using Isl.Commands.Modifiers;
using Isl.Commands.Statements;
using Isl.Runtime;

namespace Isl.Compiler;

/// <summary>
/// AST -> Command lowering pass. Mirrors the Kotlin <c>ExecutionBuilder</c>.
/// Produces a <see cref="TransformModule"/> with a precompiled function table and parent links.
/// </summary>
public sealed class ExecutionBuilder
{
    private readonly string _moduleName;
    private readonly Ast.Module _module;
    private readonly Dictionary<string, FunctionDeclarationCommand> _compiledFunctions = new(StringComparer.Ordinal);
    private readonly Dictionary<string, FunctionDecl> _functionDecls = new(StringComparer.Ordinal);

    public ExecutionBuilder(string moduleName, Ast.Module module)
    {
        _moduleName = moduleName;
        _module = module;
        // Build the AST-level lookup eagerly so compile-time queries (return type, callee
        // existence) can answer regardless of compile order — including recursive references.
        foreach (var fn in module.Functions)
            _functionDecls[fn.Name] = fn;
    }

    public TransformModule Build()
    {
        // Compile each function body. Calls inside reference the compiled-functions dict via
        // FunctionCallCommand and resolve at runtime, so forward / recursive references work.
        foreach (var fn in _module.Functions)
            _compiledFunctions[fn.Name] = CompileFunction(fn);

        var flatStatements = _module.Statements.Count > 0
            ? CompileStatements(_module.Statements)
            : null;

        var moduleResult = new TransformModule(
            _moduleName,
            _compiledFunctions,
            flatStatements);

        return moduleResult;
    }

    private FunctionDeclarationCommand CompileFunction(FunctionDecl fn)
    {
        var body = CompileStatements(fn.Body);
        var cmd = new FunctionDeclarationCommand(fn, body);
        return cmd;
    }

    private StatementsBuildCommand CompileStatements(List<Statement> stmts)
    {
        var compiled = new IIslCommand[stmts.Count];
        bool hasAssignProperty = false;
        for (int i = 0; i < stmts.Count; i++)
        {
            compiled[i] = CompileStatement(stmts[i]);
            if (stmts[i] is AssignProperty) hasAssignProperty = true;
        }
        var build = new StatementsBuildCommand(null, compiled, hasAssignProperty);
        for (int i = 0; i < compiled.Length; i++)
            compiled[i].Parent = build;
        return build;
    }

    private IIslCommand CompileStatement(Statement stmt)
    {
        switch (stmt)
        {
            case ReturnStatement r:
            {
                var v = CompileExpr(r.Value);
                return new ReturnCommand(r, v);
            }
            case AssignVariable av:
            {
                var v = CompileExpr(av.Value);
                string? byrefSrc = null;
                string? fnReturnType = null;
                if (av.TypeName != null && av.Value is VariableExpr typedSrc && typedSrc.Parts.Count == 0)
                    byrefSrc = typedSrc.Name;
                else if (av.TypeName == null && av.Value is VariableExpr ve && ve.Parts.Count == 0)
                    byrefSrc = ve.Name;
                else if (av.TypeName == null && av.Value is FunctionCallExpr fce)
                    fnReturnType = LookupFunctionReturnType(fce);
                return new AssignVariableCommand(av, v, av.TypeName, byrefSrc, fnReturnType);
            }
            case AssignVarProperty avp:
            {
                var v = CompileExpr(avp.Value);
                return new AssignVarPropertyCommand(avp, v);
            }
            case AssignProperty ap:
            {
                var v = CompileExpr(ap.Value);
                bool optElse = ap.Value is InlineIfExpr iie && iie.ElseExpr == null;
                return new AssignPropertyCommand(ap, v, optElse);
            }
            case IfStatement ifs:
            {
                var cond = CompileCondition(ifs.Condition);
                var trueBody = CompileStatements(ifs.TrueBody);
                StatementsBuildCommand? falseBody = ifs.FalseBody.Count > 0 ? CompileStatements(ifs.FalseBody) : null;
                return new IfCommand(ifs, cond, trueBody, falseBody);
            }
            case ForEachStatement fe:
            {
                return CompileForEach(fe);
            }
            case WhileStatement ws:
            {
                var cond = CompileCondition(ws.Condition);
                var body = CompileStatements(ws.Body);
                return new WhileCommand(ws, cond, body);
            }
            case SwitchStatement sw:
            {
                return CompileSwitch(sw);
            }
            case FunctionCallStatement fcs:
            {
                var call = CompileExpr(fcs.Call);
                return new FunctionCallStatementCommand(fcs, call);
            }
            default:
                throw new InvalidOperationException($"Unhandled statement type: {stmt.GetType().Name}");
        }
    }

    private ForEachCommand CompileForEach(ForEachStatement fe)
    {
        var src = CompileExpr(fe.Source);
        StatementsBuildCommand? body = fe.Body.Count > 0 ? CompileStatements(fe.Body) : null;
        ObjectBuildCommand? bodyObject = fe.BodyObject != null ? (ObjectBuildCommand)CompileExpr(fe.BodyObject) : null;
        return new ForEachCommand(fe, fe.Iterator, src, body, bodyObject);
    }

    private ForEachCommand CompileForEachExpr(ForEachExpr fe)
    {
        var src = CompileExpr(fe.Source);
        StatementsBuildCommand? body = fe.Body.Count > 0 ? CompileStatements(fe.Body) : null;
        ObjectBuildCommand? bodyObject = fe.BodyObject != null ? (ObjectBuildCommand)CompileExpr(fe.BodyObject) : null;
        return new ForEachCommand(fe, fe.Iterator, src, body, bodyObject);
    }

    private SwitchCommand CompileSwitch(SwitchStatement sw)
    {
        var subject = CompileExpr(sw.Subject);
        var cases = new List<SwitchCommand.CompiledCase>(sw.Cases.Count);
        foreach (var c in sw.Cases)
        {
            cases.Add(new SwitchCommand.CompiledCase
            {
                Pattern = c.Pattern != null ? CompileExpr(c.Pattern) : null,
                Operator = c.Operator ?? "==",
                Body = c.Body.Count > 0 ? CompileStatements(c.Body) : null,
                ResultExpr = c.ResultExpr != null ? CompileExpr(c.ResultExpr) : null
            });
        }
        StatementsBuildCommand? elseBody = sw.ElseBody != null && sw.ElseBody.Count > 0 ? CompileStatements(sw.ElseBody) : null;
        IIslCommand? elseResult = sw.ElseResultExpr != null ? CompileExpr(sw.ElseResultExpr) : null;
        return new SwitchCommand(sw, subject, cases, elseBody, elseResult);
    }

    private IIslCommand CompileExpr(Expr expr)
    {
        switch (expr)
        {
            case LiteralExpr lit:
                return new LiteralValueCommand(lit);
            case VariableExpr ve:
            {
                var filterCmds = new ConditionCommand?[ve.Parts.Count];
                for (int i = 0; i < ve.Parts.Count; i++)
                {
                    if (ve.Parts[i] is ConditionFilterPart cfp)
                        filterCmds[i] = CompileCondition(cfp.Cond);
                }
                return new VariableSelectorCommand(ve, filterCmds);
            }
            case ArrayExpr ae:
            {
                var elems = new IIslCommand[ae.Elements.Count];
                for (int i = 0; i < ae.Elements.Count; i++) elems[i] = CompileExpr(ae.Elements[i]);
                return new ArrayCommand(ae, elems);
            }
            case ObjectExpr oe:
                return CompileObject(oe);
            case InterpolateExpr ie:
                return CompileInterpolate(ie);
            case MathExprWrapper mw:
                return new MathExprWrapperCommand(mw, CompileMath(mw.Inner));
            case InlineIfExpr iif:
            {
                var cond = CompileCondition(iif.Condition);
                var t = CompileExpr(iif.ThenExpr);
                var e = iif.ElseExpr != null ? CompileExpr(iif.ElseExpr) : null;
                return new InlineIfCommand(iif, cond, t, e);
            }
            case FunctionCallExpr fc:
                return CompileFunctionCall(fc);
            case CoalesceExpr co:
                return new CoalesceCommand(co, CompileExpr(co.Left), CompileExpr(co.Right));
            case ModifiedExpr me:
                return CompileModified(me);
            case ForEachExpr feExpr:
                return new ForEachExpressionCommand(feExpr, CompileForEachExpr(feExpr));
            case SwitchExpr seExpr:
                return new SwitchExpressionCommand(seExpr, CompileSwitch(seExpr.Switch));
            case NegatedExpr ne:
                return new NegatedExpressionCommand(ne, CompileExpr(ne.Operand));
            case RelationalExpr re:
                return new RelationalExpressionCommand(re, CompileExpr(re.Left), CompileExpr(re.Right));
            default:
                throw new InvalidOperationException($"Unhandled expression type: {expr.GetType().Name}");
        }
    }

    private ObjectBuildCommand CompileObject(ObjectExpr oe)
    {
        var entries = new List<ObjectBuildCommand.Entry>(oe.Properties.Count);
        foreach (var prop in oe.Properties)
        {
            switch (prop)
            {
                case PropAssign pa:
                    entries.Add(new ObjectBuildCommand.PropEntry
                    {
                        Path = pa.Path,
                        Value = CompileExpr(pa.Value),
                        TypeName = pa.TypeName,
                        HasOptionalElseInlineIf = pa.Value is InlineIfExpr iie && iie.ElseExpr == null
                    });
                    break;
                case TextPropAssign tpa:
                    entries.Add(new ObjectBuildCommand.TextPropEntry
                    {
                        Key = tpa.Key,
                        Value = CompileExpr(tpa.Value),
                        TypeName = tpa.TypeName,
                        HasOptionalElseInlineIf = tpa.Value is InlineIfExpr iie2 && iie2.ElseExpr == null
                    });
                    break;
                case SpreadProp sp:
                    entries.Add(new ObjectBuildCommand.SpreadEntry { Source = CompileExpr(sp.Source) });
                    break;
                case VarPropAssign vpa:
                    entries.Add(new ObjectBuildCommand.VarPropEntry { Name = vpa.Name, Value = CompileExpr(vpa.Value) });
                    break;
            }
        }
        return new ObjectBuildCommand(oe, entries);
    }

    private InterpolateCommand CompileInterpolate(InterpolateExpr ie)
    {
        var parts = new List<InterpolateCommand.PartCommand>(ie.Parts.Count);
        foreach (var p in ie.Parts)
        {
            switch (p)
            {
                case TextPart tp:
                    parts.Add(new InterpolateCommand.TextPartCommand { Text = tp.Text });
                    break;
                case ExprPart ep:
                    parts.Add(new InterpolateCommand.ExprPartCommand { Inner = CompileExpr(ep.Inner) });
                    break;
                case MathPart mp:
                    parts.Add(new InterpolateCommand.MathPartCommand { Math = CompileMath(mp.Inner) });
                    break;
                case FuncCallPart fp:
                    parts.Add(new InterpolateCommand.FuncCallPartCommand { Call = CompileExpr(fp.Call) });
                    break;
            }
        }
        return new InterpolateCommand(ie, parts);
    }

    private MathExpressionCommand CompileMath(MathExpr expr) => expr switch
    {
        MathNumber mn => new MathNumberCommand(mn),
        MathBinOp mb => new MathBinOpCommand(mb, CompileMath(mb.Left), CompileMath(mb.Right)),
        MathVariable mv => new MathVariableCommand(mv, (VariableSelectorCommand)CompileExpr(mv.Variable)),
        MathFuncCall mfc => new MathFuncCallCommand(mfc, CompileExpr(mfc.Call)),
        MathParen mp => new MathParenCommand(mp, CompileMath(mp.Inner)),
        _ => throw new InvalidOperationException($"Unhandled math expr: {expr.GetType().Name}")
    };

    private ConditionCommand CompileCondition(ConditionExpr cond) => cond switch
    {
        SimpleCondition sc => new SimpleConditionCommand(sc, CompileExpr(sc.Left), sc.Right != null ? CompileExpr(sc.Right) : null),
        BoolCondition bc => new BoolConditionCommand(bc, CompileCondition(bc.Left), CompileCondition(bc.Right)),
        ParenCondition pc => new ParenConditionCommand(pc, CompileCondition(pc.Inner)),
        NegatedCondition nc => new NegatedConditionCommand(nc, CompileExpr(nc.Operand)),
        _ => throw new InvalidOperationException($"Unhandled condition expr: {cond.GetType().Name}")
    };

    private FunctionCallCommand CompileFunctionCall(FunctionCallExpr fc)
    {
        var args = new IIslCommand[fc.Arguments.Count];
        for (int i = 0; i < fc.Arguments.Count; i++) args[i] = CompileExpr(fc.Arguments[i]);

        string? resolvedFnName = null;
        if ((fc.Service == "This" || fc.Service == "this") && fc.Method != null)
        {
            var funcName = fc.Method.Contains('.') ? fc.Method.Split('.')[0] : fc.Method;
            if (_functionDecls.ContainsKey(funcName)) resolvedFnName = funcName;
        }
        return new FunctionCallCommand(fc, args, resolvedFnName, _compiledFunctions);
    }

    private string? LookupFunctionReturnType(FunctionCallExpr fc)
    {
        if (fc.Service != "This" && fc.Service != "this") return null;
        if (fc.Method == null) return null;
        var funcName = fc.Method.Contains('.') ? fc.Method.Split('.')[0] : fc.Method;
        return _functionDecls.TryGetValue(funcName, out var fn) ? fn.ReturnTypeName : null;
    }

    private IIslCommand CompileModified(ModifiedExpr me)
    {
        var inner = CompileExpr(me.Value);
        var modCommands = new ModifierCommand[me.Modifiers.Count];
        for (int i = 0; i < me.Modifiers.Count; i++)
            modCommands[i] = CompileModifier(me.Modifiers[i], inner);

        // sourceVarName for typeof type-annotation lookup (only used for the head modifier)
        string? sourceVarName = null;
        if (me.Value is VariableExpr ve && ve.Parts.Count == 0)
            sourceVarName = ve.Name;

        return new ModifiedExpressionCommand(me, inner, modCommands, sourceVarName);
    }

    private ModifierCommand CompileModifier(ModifierNode mod, IIslCommand inner)
    {
        var args = new IIslCommand[mod.Arguments.Count];
        for (int i = 0; i < mod.Arguments.Count; i++) args[i] = CompileExpr(mod.Arguments[i]);

        ConditionCommand? ifCondition = mod.Condition != null ? CompileCondition(mod.Condition) : null;

        ConditionCommand? filterCondition = null;
        if (mod.Name.Equals("filter", StringComparison.OrdinalIgnoreCase) && mod.Condition != null)
            filterCondition = ifCondition;

        IIslCommand? mapProjection = null;
        if (mod.Name.Equals("map", StringComparison.OrdinalIgnoreCase) && mod.Arguments.Count > 0)
            mapProjection = args[0];

        IIslCommand? groupByKeyExpr = null;
        if (mod.Name.Equals("group", StringComparison.OrdinalIgnoreCase)
            && (mod.SubName?.Equals("by", StringComparison.OrdinalIgnoreCase) ?? false)
            && mod.Arguments.Count > 0
            && mod.Arguments[0] is VariableExpr)
        {
            groupByKeyExpr = args[0];
        }

        return new ModifierCommand(mod, inner, args, ifCondition, filterCondition, mapProjection, groupByKeyExpr);
    }
}
