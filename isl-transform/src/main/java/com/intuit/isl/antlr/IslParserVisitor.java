// Generated from IslParser.g4 by ANTLR 4.9.1
package com.intuit.isl.antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link IslParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface IslParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link IslParser#spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpec(IslParser.SpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#importDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportDeclaration(IslParser.ImportDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#annotationParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationParameter(IslParser.AnnotationParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#annotationArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationArguments(IslParser.AnnotationArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotation(IslParser.AnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#functionDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDeclaration(IslParser.FunctionDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#functionArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionArguments(IslParser.FunctionArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#variableWithType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableWithType(IslParser.VariableWithTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#returnCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnCall(IslParser.ReturnCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#functionStatements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionStatements(IslParser.FunctionStatementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#functionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionStatement(IslParser.FunctionStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatements(IslParser.StatementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(IslParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#relop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelop(IslParser.RelopContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#ifStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(IslParser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#elseClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElseClause(IslParser.ElseClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#inlineIf}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineIf(IslParser.InlineIfContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#inlineElse}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineElse(IslParser.InlineElseContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#switchCaseStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchCaseStatement(IslParser.SwitchCaseStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#switchCaseCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchCaseCondition(IslParser.SwitchCaseConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#switchCaseElseCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchCaseElseCondition(IslParser.SwitchCaseElseConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#regexrelop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegexrelop(IslParser.RegexrelopContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCondition(IslParser.ConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#simpleCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleCondition(IslParser.SimpleConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#conditionExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionExpression(IslParser.ConditionExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#assignmentValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentValue(IslParser.AssignmentValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#assignmentValueItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentValueItem(IslParser.AssignmentValueItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#variableConditionPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableConditionPart(IslParser.VariableConditionPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#variableIndexPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableIndexPart(IslParser.VariableIndexPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#variableTextPropertyPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableTextPropertyPart(IslParser.VariableTextPropertyPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#assignSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignSelector(IslParser.AssignSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#assignProperty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignProperty(IslParser.AssignPropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#assignVariableProperty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignVariableProperty(IslParser.AssignVariablePropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#variableSelectorStart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableSelectorStart(IslParser.VariableSelectorStartContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#variableSelectorPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableSelectorPart(IslParser.VariableSelectorPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#variableSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableSelector(IslParser.VariableSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#assignTextProperty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignTextProperty(IslParser.AssignTextPropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#spreadSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpreadSelector(IslParser.SpreadSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#declareObjectStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclareObjectStatement(IslParser.DeclareObjectStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#declareObject}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclareObject(IslParser.DeclareObjectContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#variableOrObject}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableOrObject(IslParser.VariableOrObjectContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#forEach}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForEach(IslParser.ForEachContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#whileLoop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileLoop(IslParser.WhileLoopContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#functionCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(IslParser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#rhsval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRhsval(IslParser.RhsvalContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#rhsid}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRhsid(IslParser.RhsidContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyword(IslParser.KeywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#shortIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShortIdentifier(IslParser.ShortIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#multiIdent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiIdent(IslParser.MultiIdentContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#variableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaration(IslParser.VariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#typeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeDefinition(IslParser.TypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#objectTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectTypeDefinition(IslParser.ObjectTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#declareObjectTypeProperty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclareObjectTypeProperty(IslParser.DeclareObjectTypePropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#arrayTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayTypeDefinition(IslParser.ArrayTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#enumTypeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumTypeDefinition(IslParser.EnumTypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#typeDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeDeclaration(IslParser.TypeDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#typeNameDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeNameDeclaration(IslParser.TypeNameDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#filterModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterModifier(IslParser.FilterModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#conditionModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionModifier(IslParser.ConditionModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#mapModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapModifier(IslParser.MapModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#genericConditionModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericConditionModifier(IslParser.GenericConditionModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#modifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModifier(IslParser.ModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#variableWithModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableWithModifier(IslParser.VariableWithModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#argumentValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentValue(IslParser.ArgumentValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#argumentItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentItem(IslParser.ArgumentItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(IslParser.ArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#rightSideValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRightSideValue(IslParser.RightSideValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#arrayArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayArgument(IslParser.ArrayArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#array}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray(IslParser.ArrayContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#expressionInterpolate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionInterpolate(IslParser.ExpressionInterpolateContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#mathInterpolate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMathInterpolate(IslParser.MathInterpolateContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#funcCallInterpolate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncCallInterpolate(IslParser.FuncCallInterpolateContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#simpleInterpolateVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleInterpolateVariable(IslParser.SimpleInterpolateVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#interpolateText}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolateText(IslParser.InterpolateTextContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#interpolate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolate(IslParser.InterpolateContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(IslParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#regexString}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegexString(IslParser.RegexStringContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#math}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMath(IslParser.MathContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#mathExpresion}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMathExpresion(IslParser.MathExpresionContext ctx);
	/**
	 * Visit a parse tree produced by {@link IslParser#mathValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMathValue(IslParser.MathValueContext ctx);
}