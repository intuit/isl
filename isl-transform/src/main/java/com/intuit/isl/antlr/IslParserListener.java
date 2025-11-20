// Generated from IslParser.g4 by ANTLR 4.9.1
package com.intuit.isl.antlr;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link IslParser}.
 */
public interface IslParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link IslParser#spec}.
	 * @param ctx the parse tree
	 */
	void enterSpec(IslParser.SpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#spec}.
	 * @param ctx the parse tree
	 */
	void exitSpec(IslParser.SpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterImportDeclaration(IslParser.ImportDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitImportDeclaration(IslParser.ImportDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#annotationParameter}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationParameter(IslParser.AnnotationParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#annotationParameter}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationParameter(IslParser.AnnotationParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#annotationArguments}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationArguments(IslParser.AnnotationArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#annotationArguments}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationArguments(IslParser.AnnotationArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(IslParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(IslParser.AnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDeclaration(IslParser.FunctionDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDeclaration(IslParser.FunctionDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#functionArguments}.
	 * @param ctx the parse tree
	 */
	void enterFunctionArguments(IslParser.FunctionArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#functionArguments}.
	 * @param ctx the parse tree
	 */
	void exitFunctionArguments(IslParser.FunctionArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#variableWithType}.
	 * @param ctx the parse tree
	 */
	void enterVariableWithType(IslParser.VariableWithTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#variableWithType}.
	 * @param ctx the parse tree
	 */
	void exitVariableWithType(IslParser.VariableWithTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#returnCall}.
	 * @param ctx the parse tree
	 */
	void enterReturnCall(IslParser.ReturnCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#returnCall}.
	 * @param ctx the parse tree
	 */
	void exitReturnCall(IslParser.ReturnCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#functionStatements}.
	 * @param ctx the parse tree
	 */
	void enterFunctionStatements(IslParser.FunctionStatementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#functionStatements}.
	 * @param ctx the parse tree
	 */
	void exitFunctionStatements(IslParser.FunctionStatementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#functionStatement}.
	 * @param ctx the parse tree
	 */
	void enterFunctionStatement(IslParser.FunctionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#functionStatement}.
	 * @param ctx the parse tree
	 */
	void exitFunctionStatement(IslParser.FunctionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#statements}.
	 * @param ctx the parse tree
	 */
	void enterStatements(IslParser.StatementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#statements}.
	 * @param ctx the parse tree
	 */
	void exitStatements(IslParser.StatementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(IslParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(IslParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#relop}.
	 * @param ctx the parse tree
	 */
	void enterRelop(IslParser.RelopContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#relop}.
	 * @param ctx the parse tree
	 */
	void exitRelop(IslParser.RelopContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(IslParser.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(IslParser.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#elseClause}.
	 * @param ctx the parse tree
	 */
	void enterElseClause(IslParser.ElseClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#elseClause}.
	 * @param ctx the parse tree
	 */
	void exitElseClause(IslParser.ElseClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#inlineIf}.
	 * @param ctx the parse tree
	 */
	void enterInlineIf(IslParser.InlineIfContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#inlineIf}.
	 * @param ctx the parse tree
	 */
	void exitInlineIf(IslParser.InlineIfContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#inlineElse}.
	 * @param ctx the parse tree
	 */
	void enterInlineElse(IslParser.InlineElseContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#inlineElse}.
	 * @param ctx the parse tree
	 */
	void exitInlineElse(IslParser.InlineElseContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#switchCaseStatement}.
	 * @param ctx the parse tree
	 */
	void enterSwitchCaseStatement(IslParser.SwitchCaseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#switchCaseStatement}.
	 * @param ctx the parse tree
	 */
	void exitSwitchCaseStatement(IslParser.SwitchCaseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#switchCaseCondition}.
	 * @param ctx the parse tree
	 */
	void enterSwitchCaseCondition(IslParser.SwitchCaseConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#switchCaseCondition}.
	 * @param ctx the parse tree
	 */
	void exitSwitchCaseCondition(IslParser.SwitchCaseConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#switchCaseElseCondition}.
	 * @param ctx the parse tree
	 */
	void enterSwitchCaseElseCondition(IslParser.SwitchCaseElseConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#switchCaseElseCondition}.
	 * @param ctx the parse tree
	 */
	void exitSwitchCaseElseCondition(IslParser.SwitchCaseElseConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#regexrelop}.
	 * @param ctx the parse tree
	 */
	void enterRegexrelop(IslParser.RegexrelopContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#regexrelop}.
	 * @param ctx the parse tree
	 */
	void exitRegexrelop(IslParser.RegexrelopContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCondition(IslParser.ConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCondition(IslParser.ConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#simpleCondition}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCondition(IslParser.SimpleConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#simpleCondition}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCondition(IslParser.SimpleConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#conditionExpression}.
	 * @param ctx the parse tree
	 */
	void enterConditionExpression(IslParser.ConditionExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#conditionExpression}.
	 * @param ctx the parse tree
	 */
	void exitConditionExpression(IslParser.ConditionExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#assignmentValue}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentValue(IslParser.AssignmentValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#assignmentValue}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentValue(IslParser.AssignmentValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#assignmentValueItem}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentValueItem(IslParser.AssignmentValueItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#assignmentValueItem}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentValueItem(IslParser.AssignmentValueItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#variableConditionPart}.
	 * @param ctx the parse tree
	 */
	void enterVariableConditionPart(IslParser.VariableConditionPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#variableConditionPart}.
	 * @param ctx the parse tree
	 */
	void exitVariableConditionPart(IslParser.VariableConditionPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#variableIndexPart}.
	 * @param ctx the parse tree
	 */
	void enterVariableIndexPart(IslParser.VariableIndexPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#variableIndexPart}.
	 * @param ctx the parse tree
	 */
	void exitVariableIndexPart(IslParser.VariableIndexPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#variableTextPropertyPart}.
	 * @param ctx the parse tree
	 */
	void enterVariableTextPropertyPart(IslParser.VariableTextPropertyPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#variableTextPropertyPart}.
	 * @param ctx the parse tree
	 */
	void exitVariableTextPropertyPart(IslParser.VariableTextPropertyPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#assignSelector}.
	 * @param ctx the parse tree
	 */
	void enterAssignSelector(IslParser.AssignSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#assignSelector}.
	 * @param ctx the parse tree
	 */
	void exitAssignSelector(IslParser.AssignSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#assignProperty}.
	 * @param ctx the parse tree
	 */
	void enterAssignProperty(IslParser.AssignPropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#assignProperty}.
	 * @param ctx the parse tree
	 */
	void exitAssignProperty(IslParser.AssignPropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#assignVariableProperty}.
	 * @param ctx the parse tree
	 */
	void enterAssignVariableProperty(IslParser.AssignVariablePropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#assignVariableProperty}.
	 * @param ctx the parse tree
	 */
	void exitAssignVariableProperty(IslParser.AssignVariablePropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#variableSelectorStart}.
	 * @param ctx the parse tree
	 */
	void enterVariableSelectorStart(IslParser.VariableSelectorStartContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#variableSelectorStart}.
	 * @param ctx the parse tree
	 */
	void exitVariableSelectorStart(IslParser.VariableSelectorStartContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#variableSelectorPart}.
	 * @param ctx the parse tree
	 */
	void enterVariableSelectorPart(IslParser.VariableSelectorPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#variableSelectorPart}.
	 * @param ctx the parse tree
	 */
	void exitVariableSelectorPart(IslParser.VariableSelectorPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#variableSelector}.
	 * @param ctx the parse tree
	 */
	void enterVariableSelector(IslParser.VariableSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#variableSelector}.
	 * @param ctx the parse tree
	 */
	void exitVariableSelector(IslParser.VariableSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#assignTextProperty}.
	 * @param ctx the parse tree
	 */
	void enterAssignTextProperty(IslParser.AssignTextPropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#assignTextProperty}.
	 * @param ctx the parse tree
	 */
	void exitAssignTextProperty(IslParser.AssignTextPropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#spreadSelector}.
	 * @param ctx the parse tree
	 */
	void enterSpreadSelector(IslParser.SpreadSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#spreadSelector}.
	 * @param ctx the parse tree
	 */
	void exitSpreadSelector(IslParser.SpreadSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#declareObjectStatement}.
	 * @param ctx the parse tree
	 */
	void enterDeclareObjectStatement(IslParser.DeclareObjectStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#declareObjectStatement}.
	 * @param ctx the parse tree
	 */
	void exitDeclareObjectStatement(IslParser.DeclareObjectStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#declareObject}.
	 * @param ctx the parse tree
	 */
	void enterDeclareObject(IslParser.DeclareObjectContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#declareObject}.
	 * @param ctx the parse tree
	 */
	void exitDeclareObject(IslParser.DeclareObjectContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#variableOrObject}.
	 * @param ctx the parse tree
	 */
	void enterVariableOrObject(IslParser.VariableOrObjectContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#variableOrObject}.
	 * @param ctx the parse tree
	 */
	void exitVariableOrObject(IslParser.VariableOrObjectContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#forEach}.
	 * @param ctx the parse tree
	 */
	void enterForEach(IslParser.ForEachContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#forEach}.
	 * @param ctx the parse tree
	 */
	void exitForEach(IslParser.ForEachContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#whileLoop}.
	 * @param ctx the parse tree
	 */
	void enterWhileLoop(IslParser.WhileLoopContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#whileLoop}.
	 * @param ctx the parse tree
	 */
	void exitWhileLoop(IslParser.WhileLoopContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(IslParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(IslParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#rhsval}.
	 * @param ctx the parse tree
	 */
	void enterRhsval(IslParser.RhsvalContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#rhsval}.
	 * @param ctx the parse tree
	 */
	void exitRhsval(IslParser.RhsvalContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#rhsid}.
	 * @param ctx the parse tree
	 */
	void enterRhsid(IslParser.RhsidContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#rhsid}.
	 * @param ctx the parse tree
	 */
	void exitRhsid(IslParser.RhsidContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#keyword}.
	 * @param ctx the parse tree
	 */
	void enterKeyword(IslParser.KeywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#keyword}.
	 * @param ctx the parse tree
	 */
	void exitKeyword(IslParser.KeywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#shortIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterShortIdentifier(IslParser.ShortIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#shortIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitShortIdentifier(IslParser.ShortIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#multiIdent}.
	 * @param ctx the parse tree
	 */
	void enterMultiIdent(IslParser.MultiIdentContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#multiIdent}.
	 * @param ctx the parse tree
	 */
	void exitMultiIdent(IslParser.MultiIdentContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#variableDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaration(IslParser.VariableDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#variableDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaration(IslParser.VariableDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#typeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterTypeDefinition(IslParser.TypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#typeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitTypeDefinition(IslParser.TypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#objectTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterObjectTypeDefinition(IslParser.ObjectTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#objectTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitObjectTypeDefinition(IslParser.ObjectTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#declareObjectTypeProperty}.
	 * @param ctx the parse tree
	 */
	void enterDeclareObjectTypeProperty(IslParser.DeclareObjectTypePropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#declareObjectTypeProperty}.
	 * @param ctx the parse tree
	 */
	void exitDeclareObjectTypeProperty(IslParser.DeclareObjectTypePropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#arrayTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterArrayTypeDefinition(IslParser.ArrayTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#arrayTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitArrayTypeDefinition(IslParser.ArrayTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#enumTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterEnumTypeDefinition(IslParser.EnumTypeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#enumTypeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitEnumTypeDefinition(IslParser.EnumTypeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterTypeDeclaration(IslParser.TypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitTypeDeclaration(IslParser.TypeDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#typeNameDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterTypeNameDeclaration(IslParser.TypeNameDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#typeNameDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitTypeNameDeclaration(IslParser.TypeNameDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#filterModifier}.
	 * @param ctx the parse tree
	 */
	void enterFilterModifier(IslParser.FilterModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#filterModifier}.
	 * @param ctx the parse tree
	 */
	void exitFilterModifier(IslParser.FilterModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#conditionModifier}.
	 * @param ctx the parse tree
	 */
	void enterConditionModifier(IslParser.ConditionModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#conditionModifier}.
	 * @param ctx the parse tree
	 */
	void exitConditionModifier(IslParser.ConditionModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#mapModifier}.
	 * @param ctx the parse tree
	 */
	void enterMapModifier(IslParser.MapModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#mapModifier}.
	 * @param ctx the parse tree
	 */
	void exitMapModifier(IslParser.MapModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#genericConditionModifier}.
	 * @param ctx the parse tree
	 */
	void enterGenericConditionModifier(IslParser.GenericConditionModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#genericConditionModifier}.
	 * @param ctx the parse tree
	 */
	void exitGenericConditionModifier(IslParser.GenericConditionModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#modifier}.
	 * @param ctx the parse tree
	 */
	void enterModifier(IslParser.ModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#modifier}.
	 * @param ctx the parse tree
	 */
	void exitModifier(IslParser.ModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#variableWithModifier}.
	 * @param ctx the parse tree
	 */
	void enterVariableWithModifier(IslParser.VariableWithModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#variableWithModifier}.
	 * @param ctx the parse tree
	 */
	void exitVariableWithModifier(IslParser.VariableWithModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#argumentValue}.
	 * @param ctx the parse tree
	 */
	void enterArgumentValue(IslParser.ArgumentValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#argumentValue}.
	 * @param ctx the parse tree
	 */
	void exitArgumentValue(IslParser.ArgumentValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#argumentItem}.
	 * @param ctx the parse tree
	 */
	void enterArgumentItem(IslParser.ArgumentItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#argumentItem}.
	 * @param ctx the parse tree
	 */
	void exitArgumentItem(IslParser.ArgumentItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(IslParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(IslParser.ArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#rightSideValue}.
	 * @param ctx the parse tree
	 */
	void enterRightSideValue(IslParser.RightSideValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#rightSideValue}.
	 * @param ctx the parse tree
	 */
	void exitRightSideValue(IslParser.RightSideValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#arrayArgument}.
	 * @param ctx the parse tree
	 */
	void enterArrayArgument(IslParser.ArrayArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#arrayArgument}.
	 * @param ctx the parse tree
	 */
	void exitArrayArgument(IslParser.ArrayArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#array}.
	 * @param ctx the parse tree
	 */
	void enterArray(IslParser.ArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#array}.
	 * @param ctx the parse tree
	 */
	void exitArray(IslParser.ArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#expressionInterpolate}.
	 * @param ctx the parse tree
	 */
	void enterExpressionInterpolate(IslParser.ExpressionInterpolateContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#expressionInterpolate}.
	 * @param ctx the parse tree
	 */
	void exitExpressionInterpolate(IslParser.ExpressionInterpolateContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#mathInterpolate}.
	 * @param ctx the parse tree
	 */
	void enterMathInterpolate(IslParser.MathInterpolateContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#mathInterpolate}.
	 * @param ctx the parse tree
	 */
	void exitMathInterpolate(IslParser.MathInterpolateContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#funcCallInterpolate}.
	 * @param ctx the parse tree
	 */
	void enterFuncCallInterpolate(IslParser.FuncCallInterpolateContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#funcCallInterpolate}.
	 * @param ctx the parse tree
	 */
	void exitFuncCallInterpolate(IslParser.FuncCallInterpolateContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#simpleInterpolateVariable}.
	 * @param ctx the parse tree
	 */
	void enterSimpleInterpolateVariable(IslParser.SimpleInterpolateVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#simpleInterpolateVariable}.
	 * @param ctx the parse tree
	 */
	void exitSimpleInterpolateVariable(IslParser.SimpleInterpolateVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#interpolateText}.
	 * @param ctx the parse tree
	 */
	void enterInterpolateText(IslParser.InterpolateTextContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#interpolateText}.
	 * @param ctx the parse tree
	 */
	void exitInterpolateText(IslParser.InterpolateTextContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#interpolate}.
	 * @param ctx the parse tree
	 */
	void enterInterpolate(IslParser.InterpolateContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#interpolate}.
	 * @param ctx the parse tree
	 */
	void exitInterpolate(IslParser.InterpolateContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(IslParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(IslParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#regexString}.
	 * @param ctx the parse tree
	 */
	void enterRegexString(IslParser.RegexStringContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#regexString}.
	 * @param ctx the parse tree
	 */
	void exitRegexString(IslParser.RegexStringContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#math}.
	 * @param ctx the parse tree
	 */
	void enterMath(IslParser.MathContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#math}.
	 * @param ctx the parse tree
	 */
	void exitMath(IslParser.MathContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#mathExpresion}.
	 * @param ctx the parse tree
	 */
	void enterMathExpresion(IslParser.MathExpresionContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#mathExpresion}.
	 * @param ctx the parse tree
	 */
	void exitMathExpresion(IslParser.MathExpresionContext ctx);
	/**
	 * Enter a parse tree produced by {@link IslParser#mathValue}.
	 * @param ctx the parse tree
	 */
	void enterMathValue(IslParser.MathValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link IslParser#mathValue}.
	 * @param ctx the parse tree
	 */
	void exitMathValue(IslParser.MathValueContext ctx);
}