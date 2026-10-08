grammar HTTTX;

@header {
@file:Suppress("RETURN_VALUE_NOT_USED")
package de.mineking.hexo.board.parse.generated
}

root
    : metadata (turn+ | setup turn*) EOF
    ;

metadata
    : version tag* ';'
    ;

setup
    : player_setup ':' player_setup visual* ';'
    ;

player_setup
    : PLAYER ('[' coordinate ']')*
    ;

version
    : 'version[' INTEGER extension* ']'
    ;

extension
    : KEY | PLAYER
    ;

tag
    : (KEY | PLAYER) '[' value ']'
    ;

value
    : (ESCAPED | ~']')
    ;

turn
    : INTEGER '.' move+ ';'
    ;

move
    : '[' (coordinate | '/') ']' visual*
    ;

coordinate
    : INTEGER ',' INTEGER
    ;

visual
    : '<' coordinate (':' '#' highlight)? (':' '$' label)? '>'
    ;

label
    : (ESCAPED | ~'>')+
    ;

highlight
    : (PLAYER | 'N' | 'n')?
    ;

PLAYER
    : [xoXO]
    ;

KEY
    : [a-zA-Z]+
    ;

ESCAPED
    : '\\' .
    ;

INTEGER
    : '-'? [0-9]+
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
