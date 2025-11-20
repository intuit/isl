//package com.intuit.isl.debug
//
//import com.intuit.isl.commands.builder.ICommandVisitor
//import com.intuit.isl.commands.*
//import com.intuit.isl.commands.modifiers.*
//
//class TransformDebugBuilder : ICommandVisitor<IIslCommand> {
//    override fun visit(command: AssignPropertyCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("token", it.token);
//        };
//    }
//
//    override fun visit(command: AssignDynamicPropertyCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("token", it.token);
//        };
//    }
//
//    override fun visit(command: AssignVariableCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("\$${it.token.name}", it.token.value);
//        };
//    }
//
//    override fun visit(command: ForEachCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("\$${it.token.iterator}", it.token.source);
//        };
//    }
//
//    override fun visit(command: FunctionCallCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("\$${it.token.name}", it.token.arguments);
//        };
//    }
//
//    override fun visit(command: StatementFunctionCallCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("\$${it.token.name}", it.token.arguments);
//        };
//    }
//
//    override fun visit(command: FunctionDeclarationCommand): IIslCommand {
//        return FunctionDeclarationDebugDecorator(command);
//    }
//
//    override fun visit(command: AnnotationCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("\$${it.token.annotationName}", it.token.arguments);
//        };
//    }
//
//    override fun visit(command: FunctionReturnCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token);
//        };
//    }
//
//    override fun visit(command: LiteralValueCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("token", it.token)
//                .addLocal("value", it.token.value)
//        };
//    }
//
//    override fun visit(command: ModifierValueCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//    override fun visit(command: GenericConditionalModifierCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//    override fun visit(command: FilterModifierValueCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//    override fun visit(command: MapModifierValueCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//
//    override fun visit(command: ReduceModifierValueCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//
//    override fun visit(command: VariableSelectorValueCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//
//    override fun visit(command: VariableSimpleSelectorCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//
//    override fun visit(command: VariablePropertySelectorCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//
//    override fun visit(command: ObjectBuildCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//
//    override fun visit(command: SpreadCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//
//    override fun visit(command: StatementsBuildCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//
//    override fun visit(command: ConditionCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//
//    override fun visit(command: ConditionExpressionCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("left", it.left)
//                .addLocal("c", it.condition)
//                .addLocal("right", it.right)
//        };
//    }
//
//    override fun visit(command: SimpleConditionCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//    override fun visit(command: CoalesceCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//
//    override fun visit(command: ConditionModifierValueCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("prev", it.value)
//                .addLocal("c", it.expression)
//                .addLocal("result", it.trueModifier)
//        };
//    }
//
//    override fun visit(command: SwitchCaseCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("value", it.value)
//        };
//    }
//
//    override fun visit(command: SwitchCaseCommand.SwitchCaseBranchCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("op", it.condition)
//                .addLocal("case", it.right)
//        };
//    }
//
//    override fun visit(command: ArrayCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//
//    override fun visit(command: InterpolateCommand): IIslCommand {
//        return unknownVisit(command);
//    }
//
//    private fun <T : IIslCommand> unknownVisit(command: T): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("command", it)
//                .addLocal("token", it.token)
//        };
//    }
//
//    override fun visit(command: MathExpressionCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("left", command.left)
//                .addLocal("op", it.operator)
//                .addLocal("right", command.right)
//        }
//    }
//
//    override fun visit(command: NoopCommand): IIslCommand {
//        return command;
//    }
//
//    override fun visit(command: WhileCommand): IIslCommand {
//        return SimpleDebuggableOperation(command) {
//            OperationState(command, command.token)
//                .addLocal("token", it.token)
//        };
//    }
//}