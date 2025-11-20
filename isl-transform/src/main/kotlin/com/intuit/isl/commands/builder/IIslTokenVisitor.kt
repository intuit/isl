package com.intuit.isl.commands.builder

import com.intuit.isl.commands.NoopToken
import com.intuit.isl.runtime.TransformModule
import com.intuit.isl.parser.tokens.*

interface IIslTokenVisitor<T> {
    fun visit(token: AssignPropertyToken): T;
    fun visit(token: AssignDynamicPropertyToken): T;
    fun visit(token: AssignVariableToken): T;
    fun visit(token: ForEachToken): T;

    fun visit(token: ParallelForEachToken): T;
//    fun visit(token: WithLockToken): T;

    fun visit(token: WhileToken): T;
    fun visit(token: FunctionCallToken): T;
    fun visit(token: AnnotationDeclarationToken): T;
    fun visit(token: FunctionDeclarationToken): T;
    fun visit(token: FunctionReturnToken): T;
    fun visit(token: LiteralValueToken): T;
    fun visit(token: ModifierValueToken): T;

    fun visit(token: VariableSelectorValueToken): T;
    fun visit(token: SimpleVariableSelectorValueToken): T;
    fun visit(token: SimplePropertySelectorValueToken): T;

    fun visit(token: StatementsToken): T;
    fun visit(token: DeclareObjectToken): T;
    fun visit(token: SpreadToken): T;

    fun visit(token: ConditionToken): T;
    fun visit(token: ConditionExpressionToken): T;
    fun visit(token: SimpleConditionToken): T;
    fun visit(token: CoalesceToken): T;

    fun visit(token: SwitchCaseToken): T;
    fun visit(token: SwitchCaseBranchToken): T;

    fun visit(token: DeclareArrayToken): T;
    fun visit(token: StringInterpolateToken): T;
    fun visit(token: MathExpressionToken): T;
    fun visit(token: ModuleImplementationToken): TransformModule;
    fun visit(token: ImportDeclarationToken): T;

    fun visit(token: NoopToken): T;
}