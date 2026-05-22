namespace Isl.Ast;

// Base type for all AST nodes
public abstract record IslNode;

// Top-level module
public record Module(List<FunctionDecl> Functions, List<Statement> Statements) : IslNode;

// Function declaration
public record FunctionDecl(string Name, List<string> Parameters, List<Statement> Body, string? ReturnTypeName = null) : IslNode;

// ---- Statements ----
public abstract record Statement : IslNode;

// $var = value  or  $var: value  or  $var: TypeName = value
public record AssignVariable(string Name, Expr Value, string? TypeName = null) : Statement;

// $var.prop.path = value (assign nested property on a variable)
public record AssignVarProperty(string VarName, List<string> PropPath, Expr Value) : Statement;

// prop.path: value  or  prop.path = value
public record AssignProperty(List<string> Path, Expr Value) : Statement;

// return value
public record ReturnStatement(Expr Value) : Statement;

// if (cond) { stmts } [else { stmts }] endif
public record IfStatement(ConditionExpr Condition, List<Statement> TrueBody, List<Statement> FalseBody) : Statement;

// switch(expr) cases endswitch
public record SwitchStatement(Expr Subject, List<SwitchCase> Cases, List<Statement>? ElseBody, Expr? ElseResultExpr = null) : Statement;

// foreach $item in $arr { stmts } endfor
public record ForEachStatement(string Iterator, Expr Source, List<Statement> Body, ObjectExpr? BodyObject) : Statement;

// while (cond) { stmts } endwhile
public record WhileStatement(ConditionExpr Condition, List<Statement> Body, int MaxLoops = 50) : Statement;

// standalone function call as statement
public record FunctionCallStatement(FunctionCallExpr Call) : Statement;

// ---- Switch Case ----
public record SwitchCase(Expr? Pattern, string? Operator, List<Statement> Body, Expr? ResultExpr) : IslNode;

// ---- Expressions ----
public abstract record Expr : IslNode;

// Literal: "string", 123, true/false/null
public record LiteralExpr(object? Value) : Expr;

// Variable selector: $var, $var.prop, $var[0], $var.prop[(condition)]
public record VariableExpr(string Name, List<VariablePart> Parts) : Expr;
public abstract record VariablePart : IslNode;
public record PropertyPart(string Name) : VariablePart;
public record IndexPart(int Index) : VariablePart;
public record ConditionFilterPart(ConditionExpr Cond) : VariablePart;

// Array literal: [val1, val2, ...]
public record ArrayExpr(List<Expr> Elements) : Expr;

// Object literal: { prop: val, ... }
public record ObjectExpr(List<ObjectProperty> Properties) : Expr;
public abstract record ObjectProperty : IslNode;
public record PropAssign(List<string> Path, Expr Value, string? TypeName = null) : ObjectProperty;
public record TextPropAssign(string Key, Expr Value, string? TypeName = null) : ObjectProperty;
public record SpreadProp(Expr Source) : ObjectProperty;
public record VarPropAssign(string Name, Expr Value) : ObjectProperty;

// Backtick interpolated string
public record InterpolateExpr(List<InterpolPart> Parts) : Expr;
public abstract record InterpolPart : IslNode;
public record TextPart(string Text) : InterpolPart;
public record ExprPart(Expr Inner) : InterpolPart;
public record MathPart(MathExpr Inner) : InterpolPart;
public record FuncCallPart(FunctionCallExpr Call) : InterpolPart;

// {{ math }}
public record MathExprWrapper(MathExpr Inner) : Expr;

// Inline if: if (cond) value else other [endif]
public record InlineIfExpr(ConditionExpr Condition, Expr ThenExpr, Expr? ElseExpr) : Expr;

// Function call: @.Service.Name(args)
public record FunctionCallExpr(string Service, string? Method, List<Expr> Arguments) : Expr;

// Coalesce: left ?? right
public record CoalesceExpr(Expr Left, Expr Right) : Expr;

// Value with modifier chain
public record ModifiedExpr(Expr Value, List<ModifierNode> Modifiers) : Expr;

// ForEach inline as expression (forEach ... endfor)
public record ForEachExpr(string Iterator, Expr Source, List<Statement> Body, ObjectExpr? BodyObject) : Expr;

// Switch inline as expression (switch ... endswitch)
public record SwitchExpr(SwitchStatement Switch) : Expr;

// Negated expression: !$expr (used in modifier args to signal condition-selector)
public record NegatedExpr(Expr Operand) : Expr;

// Relational expression: $left op $right (used in modifier args to signal condition-selector)
public record RelationalExpr(Expr Left, string Op, Expr Right) : Expr;

// ---- Modifiers ----
public record ModifierNode(string Name, string? SubName, List<Expr> Arguments, ConditionExpr? Condition) : IslNode;

// ---- Math ----
public abstract record MathExpr : IslNode;
public record MathBinOp(MathExpr Left, string Op, MathExpr Right) : MathExpr;
public record MathNumber(double Value) : MathExpr;
public record MathVariable(VariableExpr Variable) : MathExpr;
public record MathFuncCall(FunctionCallExpr Call) : MathExpr;
public record MathParen(MathExpr Inner) : MathExpr;

// ---- Conditions ----
public abstract record ConditionExpr : IslNode;
public record SimpleCondition(Expr Left, string Op, Expr? Right) : ConditionExpr;
public record BoolCondition(ConditionExpr Left, string LogOp, ConditionExpr Right) : ConditionExpr;
public record ParenCondition(ConditionExpr Inner) : ConditionExpr;
public record NegatedCondition(Expr Operand) : ConditionExpr;
