//
// Parser Grammar for ISL
//

parser grammar IslParser;
options { tokenVocab=IslLexer; }

// Let's have some fun :)
spec
  :
  // we can have either a list of statements (a:b....) or a function declaration
  // if we have no function we create a default function main(){ ... } - JS/TS/KS style
  // We can have one or multiple functions or just a list of good old statements
  ( (importDeclaration* typeDeclaration* functionDeclaration*) | statements)
  EOF;

importDeclaration: IMPORT ID FROM QUOTEDSTRING SEMICOLON;

// Function Annotations that are converted to decorators
// @Name or @Name(params)
annotationParameter: (declareObject | literal | array);
annotationArguments
  : OPAREN (annotationParameter (COMMA annotationParameter)*)? CPAREN;
annotation: AT ID annotationArguments?;

// @cache @retry(...) @time('name')
// fun name() { return { ... } }
functionDeclaration: (((annotation*|CACHE?) FUN)|MODIFIER_FUN) ID functionArguments (COLON typeDefinition)?
    CURLYOPEN
        functionStatements
    CURLYCLOSE;

functionArguments
  : OPAREN (variableWithType (COMMA variableWithType)*)? CPAREN
  ;
variableWithType: variableDeclaration (COLON typeDefinition)?;


returnCall: RETURN assignmentValue;

// just like normal statements but with an extra returnCall supported
functionStatements: (functionStatement)+;
functionStatement:
    (assignVariableProperty SEMICOLON?)
    | (assignProperty SEMICOLON?)
    | ifStatement // if you are inside a function we'll only allow V2 format
    | switchCaseStatement
    | forEach
    | whileLoop
    | (functionCall SEMICOLON?)
    | (returnCall SEMICOLON?);

// format for Normal statements when not part of a function
statements: statement+;
statement:
    (assignVariableProperty SEMICOLON?)
    | (assignProperty SEMICOLON?)
    | ifStatement | switchCaseStatement
    | forEach
    | whileLoop
    | (functionCall SEMICOLON?)
    | (returnCall SEMICOLON?)
    ;

relop: EQUAL_EQUAL
    | NOT_EQUAL
    | LESS_OR_EQUAL
    | GREATER_OR_EQUAL
    | GREATER
    | LESS
    | CONTAINS
    | NOT_CONTAINS
    | STARTS_WITH
    | NOT_STARTS_WITH
    | ENDS_WITH
    | NOT_ENDS_WITH
    | IN
    | NOT_IN
    | IS
    | NOT_IS;

// if statement
ifStatement: IFCODE condition trueStatements=statements (elseClause)? ENDIFCODE;
elseClause: ELSE falseStatements=statements;

// inline if statement - because it looks nice and useful
// value: if ( condition ) value1 else value2;
// maybe we could also introduce the Elvis expression ?: - looks cool :)
inlineIf: IFCODE condition (rhsval|declareObject) inlineElse? ENDIFCODE?;
inlineElse: ELSE (rhsval|declareObject);

// switch ( value )
//      "123" : statements;
//      /regex/: statements;
//      else: statements
// endswitch
switchCaseStatement:
    SWITCH OPAREN rhsid CPAREN
        switchCaseCondition*
        switchCaseElseCondition?
    ENDSWITCH;
switchCaseCondition: ((relop? (variableSelector|array|literal))|regexString) ARROW (functionCall|statements|resultVariable=rhsid|declareObject) SEMICOLON;
switchCaseElseCondition: ELSE ARROW (functionCall|statements|rhsid|declareObject) SEMICOLON;

regexrelop: MATCHES | NOT_MATCHES;
condition: WS* OPAREN WS* conditionExpression WS* CPAREN WS*;
simpleCondition:
    (BANG? singleLeft=rhsid)
    | (leftCondition=rhsid relop rightCondition=rhsid)
    | (leftCondition=rhsid regexrelop regexString);

conditionExpression: leftExpression=conditionExpression LOP rigthExpression=conditionExpression
    | OPAREN conditionExpression CPAREN
    | simpleCondition;


// Note we have both assignmentValue and argumentValue
// a value that can be assigned to something else (e.g. property, variable, parameter)
assignmentValue: assignmentValue COALESCE assignmentValue
    | assignmentValueItem;
assignmentValueItem: (math | declareObject | inlineIf | forEach | whileLoop | literal | array | interpolate | functionCall | switchCaseStatement | variableSelector) modifier*;


// $var[( condition )]
variableConditionPart: ARRAYCONDOPEN WS* conditionExpression WS* ARRAYCONDCLOSE;
variableIndexPart: SQUAREOPEN NUM SQUARECLOSE;
variableTextPropertyPart: SQUAREOPEN WS* QUOTEDSTRING WS* SQUARECLOSE;
assignSelector:
    shortIdentifier variableIndexPart?
    (DOT (variableTextPropertyPart | shortIdentifier) variableIndexPart?)*;

// abc: 1234 or {{  math }} or abc: { prop: value, prop2: value }
// this does NOT capture variables, only properties

// we can either have a selector Name: Value
// or an interpolate in which case we build the property name dynamically
assignProperty: (assignSelector|interpolate) ((COLON (objectType=typeNameDeclaration EQUAL)?) | EQUAL) assignmentValue;

// Assign Variable: $var : { object }, $var = { object }
// $var: Type = { object }
// TODO: This will allow declarations like $var.property = which is a bit odd - review if we really want this
assignVariableProperty: DOLLAR assignSelector ((COLON (objectType=typeNameDeclaration EQUAL)?) | EQUAL) assignmentValue;

// We want this on multiple parts as it makes it easier to process
variableSelectorStart: shortIdentifier? (variableIndexPart|variableConditionPart)?;
variableSelectorPart: (variableTextPropertyPart | shortIdentifier) (variableIndexPart|variableConditionPart)?;
variableSelector: DOLLAR variableSelectorStart (DOT variableSelectorPart)*;

assignTextProperty: QUOTEDSTRING COLON (objectType=typeNameDeclaration EQUAL)? assignmentValue;

spreadSelector: SPREAD (variableSelector | functionCall);

// { prop: value, prop2: value }
// separating this in case we want in the future to reuse objects (e.g. as method parameters?)
declareObjectStatement:
    spreadSelector
    | assignTextProperty
    | assignProperty
    | assignVariableProperty;

declareObject:
    CURLYOPEN
        (declareObjectStatement ((COMMA|SEMICOLON) declareObjectStatement)* COMMA?)?
    CURLYCLOSE;

// Alternative approach: r: x | map( { abc: $it.property } )
variableOrObject: variableSelector|declareObject;
forEach:
    (PARALLEL (options=variableOrObject)?)? FOR iterator=variableDeclaration IN rhsid
//        (statements | declareObject | variableSelector)
       (statements)? (declareObject | variableSelector)?
    ENDFOR;

// Parallel processing
//parallelForEach:
//    PFOR (options=variableOrObject)? FOR iterator=variableDeclaration IN rhsid
//        (statements)? (declareObject | variableSelector)?
//    ENDFOR;

// While loop. 'maxLoops' is optional and has a default value of 50 and can be up-scaled to 200
// TODO: Maybe expose the maxLoops as a default on the environment?
// while ( condition, {maxLoops: 123} )
//  	statements
// endwhile
whileLoop:
	WHILELOOP OPAREN conditionExpression (COMMA options=declareObject)? CPAREN
            (statements | bodyDeclareObject=declareObject | variableDeclaration)
    ENDWHILELOOP;

// @.Service( arguments ) or @.Service.Name( arguments ) or @.Service.Name( arguments ) { statements }
// We can play with the syntax as we see fit ;)
functionCall:
    AT DOT service=ID (DOT name=multiIdent)? arguments
    (CURLYOPEN functionStatements CURLYCLOSE)?;

// right hand side value
rhsval
  : rhsid | rhsid SEMICOLON | SEMICOLON
  ;

rhsid
  : (rightSideValue modifier*)
  ;

// List of all keywords that we want to allow to be used as identificators
keyword: IN | IMPORT | DECLARETYPE | AS | FROM | LOP | FILTER | RETURN | MAP | MATCHES;
shortIdentifier: ID | keyword;
multiIdent: shortIdentifier (DOT shortIdentifier)?;	// a.b


variableDeclaration: DOLLAR shortIdentifier;
//variable: DOLLAR identifierPart;

// type declaration generally part of a signature. This is respecting JSON Schema types
// https://json-schema.org/understanding-json-schema/reference/type.html
typeDefinition: typeNameDeclaration | objectTypeDefinition | arrayTypeDefinition | enumTypeDefinition;
objectTypeDefinition:
    CURLYOPEN
        (declareObjectTypeProperty (COMMA declareObjectTypeProperty)* COMMA?)?
    CURLYCLOSE;
declareObjectTypeProperty: shortIdentifier COLON typeDefinition;
arrayTypeDefinition: (ID | objectTypeDefinition)? SQUAREOPEN SQUARECLOSE;
enumTypeDefinition: SQUAREOPEN literal (COMMA literal)* SQUARECLOSE;
typeDeclaration: DECLARETYPE ID ((AS typeDefinition)|(FROM QUOTEDSTRING)) SEMICOLON;
typeNameDeclaration: shortIdentifier (DOT shortIdentifier)* (SQUAREOPEN SQUARECLOSE)?;	// a.b.c.d[]


// These are custom modifiers that understands more complex parameters
filterModifier: FILTER condition;
conditionModifier: IFCODE condition multiIdent arguments?;
mapModifier: MAP OPAREN argumentValue CPAREN;
genericConditionModifier: multiIdent OPAREN (conditionExpression (COMMA argumentValue)*) CPAREN;
modifier: MODIFIER (conditionModifier|filterModifier|mapModifier|genericConditionModifier|(multiIdent arguments?));

variableWithModifier: variableSelector modifier*;

argumentValue:
   argumentValue COALESCE argumentValue
   | argumentItem;
argumentItem: (math | declareObject | literal | array | interpolate | functionCall | variableSelector) modifier*;

arguments
  : OPAREN (argumentValue (COMMA argumentValue)*)? CPAREN
  ;

rightSideValue
  : literal | array | interpolate | functionCall | variableSelector
  ;

// simple array definition [ value1, value2 ]
arrayArgument: argumentValue | spreadSelector;
array: SQUAREOPEN (arrayArgument (COMMA arrayArgument)*)? SQUARECLOSE;

// can we interpolate?
// TODO: Improve this - this is crude, has no escaping and no mode processing but it's enough to illustrate the idea
// Also should add support for ${ variable.property | modifier }
expressionInterpolate: ENTER_EXPR_INTERP (variableWithModifier | functionCall | math) CURLYCLOSE;
mathInterpolate: ENTER_MATH_INTERP mathExpresion CURLYCLOSECLOSE;
funcCallInterpolate: ENTER_FUNC_INTERP multiIdent arguments;
simpleInterpolateVariable: ID_INTERP;
interpolateText: TEXT | RECOVERTOKENS_INTERP;
interpolate: OPEN_BACKTICK (interpolateText | simpleInterpolateVariable | expressionInterpolate | mathInterpolate | funcCallInterpolate | RECOVERTOKENS_INTERP)* CLOSE_BACKTICK;



literal     : (QUOTEDSTRING | NUM | BOOL);
regexString : MATH_DIV ~MATH_DIV*? MATH_DIV;

// {{ (123 + 456) / $var }}
//math: CURLYOPENOPEN plus CURLYCLOSECLOSE;
//// let's do some math - NOTE: this is failing is we have no spaces so `+3` will fail while `+ 3` works -  and I don't know how to fix it
//// Check Left Factoring http://meri-stuff.blogspot.com/2011/09/antlr-tutorial-expression-language.html
////plus: mathExpresion (| (MATH_TIMES | MATH_DIV | MATH_PLUS | MATH_MINUS) mathExpresion);
//mathExpresion: mathValue | OPAREN plus CPAREN;
////    (OPAREN mathExpresion CPAREN)
////    |
////    ( mathValue ( | (MATH_TIMES | MATH_DIV | MATH_PLUS | MATH_MINUS) mathValue ) )
//    //| mathExpresion (MATH_PLUS | MATH_MINUS) mathExpresion
//    //| OPAREN mathExpresion CPAREN;
//    //| mathValue;

math: CURLYOPENOPEN mathExpresion CURLYCLOSECLOSE;
// let's do some math - NOTE: this is failing is we have no spaces so `+3` will fail while `+ 3` works -  and I don't know how to fix it
mathExpresion:
    mathExpresion (MATH_TIMES | MATH_DIV) mathExpresion
    | mathExpresion (MATH_PLUS | MATH_MINUS) mathExpresion
    | OPAREN mathExpresion CPAREN
    | mathValue;
mathValue: NUM | variableSelector | functionCall;

