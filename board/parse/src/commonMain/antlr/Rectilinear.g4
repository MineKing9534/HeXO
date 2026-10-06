grammar Rectilinear;

@header {
@file:Suppress("RETURN_VALUE_NOT_USED")
package de.mineking.hexo.board.parse.generated
}

root
    : column='c'? item* EOF
    ;

item
    : cell
    | label
    | highlight
    | empty
    | step
    | gap
    | row
    | WS
    ;

empty
    : '.'
    ;

step
    : '-'
    ;

gap
    : INTEGER
    ;

row
    : ROW
    ;

cell
    : OWNER
    | HIGHLIGHTED_OWNER
    | HIGHLIGHTED_EMPTY
    ;

label
    : '[' (ESCAPED | label | ~']')* ']'
    ;

highlight
    : '(' WS* (lineHighlight | cellHighlight) WS* ')'
    ;

lineHighlight
    : DIRECTION WS* INTEGER? WS* highlightColor?
    ;

cellHighlight
    : highlightColor?
    ;

highlightColor
    : OWNER
    | HIGHLIGHTED_EMPTY
    ;

ESCAPED
    : '\\' .
    ;

OWNER
    : [xo]
    ;

HIGHLIGHTED_OWNER
    : [XO]
    ;

HIGHLIGHTED_EMPTY
    : '!'
    ;

INTEGER
    : [0-9]+
    ;


ROW
    : [/\n]
    ;

DIRECTION
    : [bdpq<>]
    ;

WS
    : [ \t\r]+
    ;

OTHER
    : .
    ;
