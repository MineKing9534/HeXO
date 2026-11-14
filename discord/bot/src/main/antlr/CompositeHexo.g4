grammar CompositeHexo;

@header {
@file:Suppress("RETURN_VALUE_NOT_USED")
package de.mineking.hexo.discord.bot.grammar
}

message: (paragraph | paragraphBreak)* EOF;
paragraph: (content | spoiler)+;
spoiler: SPOILER (content | paragraphBreak)* SPOILER?;
content: codeBlock | inlineCode | url | text;
paragraphBreak: PARAGRAPH_BREAK;
codeBlock: FENCED_CODE;
inlineCode: INLINE_CODE;
url: URL;
text: (TEXT | ESCAPED)+;

// Code is opaque to Markdown parsing, including when a delimiter is unclosed.
FENCED_CODE: '```' .*? ('```' | EOF);
INLINE_CODE: '`' ~[`\r\n]* '`'?;
SPOILER: '||';
PARAGRAPH_BREAK: NEWLINE [ \t]* NEWLINE ([ \t]* NEWLINE)*;
ESCAPED: '\\' .;
URL: 'http' 's'? '://' ~[ \t\r\n<>`|\\*~\]]+;
TEXT: .;

fragment NEWLINE: '\r'? '\n';
