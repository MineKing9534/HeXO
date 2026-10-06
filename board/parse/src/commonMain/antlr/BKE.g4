grammar BKE;

@header {
@file:Suppress("RETURN_VALUE_NOT_USED")
package de.mineking.hexo.board.parse.generated
}

root
    : '0' EOF
    | context? turn+ EOF
    ;

turn
    : player MOVE+
    ;

player
    : 'x'
    | 'o'
    ;

context
    : DIRECTION CHIRALITY
    ;

CHIRALITY
    : 'CCW'
    | 'CW'
    ;

MOVE
    : [A-Z]+ [0-9.]*
    ;

DIRECTION
    : [bdpq<>]
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
