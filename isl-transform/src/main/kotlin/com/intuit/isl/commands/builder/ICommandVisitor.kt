package com.intuit.isl.commands.builder

import com.intuit.isl.commands.*
import com.intuit.isl.commands.modifiers.*
import com.intuit.isl.commands.modifiers.ConditionModifierValueCommand
import com.intuit.isl.commands.modifiers.MapModifierValueCommand
import com.intuit.isl.commands.modifiers.ReduceModifierValueCommand

interface ICommandVisitor<T> {
    fun visit(command: AssignPropertyCommand): T;
    fun visit(command: AssignDynamicPropertyCommand): T;
    fun visit(command: AssignVariableCommand): T;

    fun visit(command: ParallelForEachCommand): T;
//    fun visit(command: WithLockCommand): T;

    fun visit(command: ForEachCommand): T;
    fun visit(command: WhileCommand): T;
    fun visit(command: FunctionCallCommand): T;
    fun visit(command: StatementFunctionCallCommand): T;
    fun visit(command: AnnotationCommand): T;
    fun visit(command: FunctionDeclarationCommand): T;
    fun visit(command: FunctionReturnCommand): T;
    fun visit(command: LiteralValueCommand): T;
    fun visit(command: ModifierValueCommand): T;
    fun visit(command: FilterModifierValueCommand): T;
    fun visit(command: MapModifierValueCommand): T;
    fun visit(command: ReduceModifierValueCommand): T;
    fun visit(command: GenericConditionalModifierCommand): T;

    fun visit(command: VariableSelectorValueCommand): T;
    fun visit(command: VariableSimpleSelectorCommand): T;
    fun visit(command: VariablePropertySelectorCommand): T;

    fun visit(command: ObjectBuildCommand): T;
    fun visit(command: SpreadCommand): T;
    fun visit(command: StatementsBuildCommand): T;

    fun visit(command: ConditionCommand): T;
    fun visit(command: ConditionExpressionCommand): T;
    fun visit(command: SimpleConditionCommand): T;
    fun visit(command: CoalesceCommand): T;
    fun visit(command: ConditionModifierValueCommand): T;

    fun visit(command: SwitchCaseCommand): T;
    fun visit(command: SwitchCaseCommand.SwitchCaseBranchCommand): T;

    fun visit(command: ArrayCommand): T;
    fun visit(command: InterpolateCommand): T;
    fun visit(command: MathExpressionCommand): T;

    fun visit(command: NoopCommand): T;
}