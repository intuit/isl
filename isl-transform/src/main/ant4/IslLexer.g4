/**
 * Lexer Grammar ISL
 */
lexer grammar IslLexer;

@members {
    int interpolationNestingExpressions = 0;
    int interpolationNestingMath = 0;
    int interpolationNestingFunc = 0;
}

// IF
IFCODE: 'if';
ELSE: 'else';
ENDIFCODE:'endif';
SWITCH: 'switch';
ARROW: '->';
ENDSWITCH: 'endswitch';
// special modifiers
FILTER: 'filter';
MAP: 'map';

EQUAL_EQUAL: '==';
NOT_EQUAL: '!=';
LESS_OR_EQUAL: '<=';
GREATER_OR_EQUAL: '>=';
GREATER: '>';
LESS: '<';
CONTAINS: 'contains';
NOT_CONTAINS: '!contains';
STARTS_WITH: 'startsWith';
NOT_STARTS_WITH: '!startsWith';
ENDS_WITH: 'endsWith';
NOT_ENDS_WITH: '!endsWith';
IN: 'in';
NOT_IN: '!in';
IS: 'is';
NOT_IS: '!is';
MATCHES: 'matches';
NOT_MATCHES: '!matches';

PARALLEL: 'parallel';
FOR: 'foreach';
ENDFOR: 'endfor';

// While loop
WHILELOOP: 'while';
ENDWHILELOOP: 'endwhile';

// Functions
FUN: 'fun';
MODIFIER_FUN: 'modifier';
RETURN: 'return';
CACHE: 'cache';

// Imports
IMPORT: 'import';
DECLARETYPE: 'type';
AS: 'as';
FROM: 'from';

// we need a separate grammar for string interpolation so we don't start chewing the spaces - they are important in interpolation
OPEN_BACKTICK: '`' -> pushMode(INTERPOLATE);

LOP: 'and' | 'or';

// We can't declare this here as it will cut keywords from the actual text
// TYPE: 'string' | 'number' | 'integer' | 'object' | 'any' | 'boolean' | 'text' | 'date' | 'datetime';

BANG: '!';
COLON: ':';
EQUAL: '=';
SEMICOLON: ';';
MODIFIER: '|';
COMMA: ',';
DOLLAR: '$';
BACKSLASH: '\\';
AT: '@';
SPREAD: '...';
DOT: '.';
COALESCE: '??';

OPAREN: '(';
CPAREN: ')' {
    if ( interpolationNestingFunc > 0 ){
        interpolationNestingFunc--;
        popMode();
    }
};

ARRAYCONDOPEN: '[(';
ARRAYCONDCLOSE: ')]';

SQUAREOPEN: '[';
SQUARECLOSE: ']';

CURLYOPEN: '{';
CURLYCLOSE: '}' {
    if ( interpolationNestingExpressions > 0 ){
        interpolationNestingExpressions--;
        popMode();
    }
};

CURLYOPENOPEN: '{{';
CURLYCLOSECLOSE: '}}' {
    if ( interpolationNestingMath > 0 ){
        interpolationNestingMath--;
        popMode();
    }
};

// Math
MATH_TIMES: '*';
MATH_DIV: '/';
MATH_PLUS: '+';
MATH_MINUS: '-';

// CT: This is the worst place to put the null
// but it does not seem to parse it if it's in the quoted string
// I'm sure we'll fix the ANTLR somehow - but this is for now fixed in the IslScriptVisitor
BOOL
  : ('false' | 'true' | 'null')
  ;

NUM
  : '-'? Digit ('.' Digit)?
  ;

ID
  : [a-zA-Z_] [a-zA-Z0-9_]*;


QUOTEDSTRING
  : ('"' ~'"'*? '"') | ('\'' ~'\''*? '\'')
  ;

COMMENT
  : '#' ~[\r\n]* -> skip
  ;
COMMENT2
  : '//' ~[\r\n]* -> skip
  ;

WS
  : [ \t\r\n]+ -> skip
  ;

UNKNOWN
  : .
  ;


fragment Idpart
  : ID ('[' Digit ']')?
  ;

fragment Digit
  : [0-9]+
  ;

/**
mode: Interpolation
**/
mode INTERPOLATE;
ENTER_EXPR_INTERP: '${' {
    interpolationNestingExpressions++;
    pushMode(DEFAULT_MODE);
};
ENTER_MATH_INTERP: '{{' {
    interpolationNestingMath++;
    pushMode(DEFAULT_MODE);
};
ENTER_FUNC_INTERP: '@.' {
    interpolationNestingFunc++;
    pushMode(DEFAULT_MODE);
};
ID_INTERP: '$' [A-Za-z_] [A-Za-z0-9_]*;

TEXT: ( '\\$' | '$.' | ~( '`' | '$' | '{' | '@' ) )+;

// The Text will loose some tokens we want recovered
RECOVERTOKENS_INTERP: ('$' | '{' | '@');

CLOSE_BACKTICK: '`' -> popMode;

