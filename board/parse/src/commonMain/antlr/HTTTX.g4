grammar HTTTX;

@header {
@file:Suppress("RETURN_VALUE_NOT_USED")
package de.mineking.hexo.board.parse.generated
}

root
    : 'version[' INTEGER ']' ';' turn+ EOF
    ;

turn
    : INTEGER '.' move+ ';'
    ;

move
    : '[' coordinate ']' visual*
    ;

coordinate
    : INTEGER ',' INTEGER
    ;

visual
    : '<' coordinate (':' HIGHLIGHT)? (':' LABEL)? '>'
    ;

HIGHLIGHT
    : '#' [xoXO]?
    ;

LABEL
    : '$' [a-zA-Z0-9]+
    ;

INTEGER
    : '-'? [0-9]+
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
