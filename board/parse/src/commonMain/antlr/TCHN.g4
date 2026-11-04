grammar TCHN;

@header {
@file:Suppress("RETURN_VALUE_NOT_USED")
package de.mineking.hexo.board.parse.generated
}

root
    : '$' boardExpression EOF
    ;

boardExpression
    : functionCall
    | NOTATION
    ;

functionCall
    : IDENTIFIER '(' (argument (',' argument)*)? ')'
    ;

argument
    : INTEGER
    | AXIS
    | boardExpression
    ;

AXIS
    : [-/\\]
    ;

INTEGER
    : '-'? [0-9]+
    ;

IDENTIFIER
    : [a-zA-Z_] [a-zA-Z_0-9]*
    ;

NOTATION
    : '{' (NOTATION | ~[{}])* '}'
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
