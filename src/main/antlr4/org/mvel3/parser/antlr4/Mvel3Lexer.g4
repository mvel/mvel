// MVEL3 Lexer - extends Java lexer with MVEL-specific tokens

lexer grammar Mvel3Lexer;

import JavaLexer;

// MVEL-specific keywords and operators
IN        : 'in';
CONTAINS  : 'contains';
MATCHES   : 'matches';
SOUNDSLIKE: 'soundslike';
STRSIM    : 'strsim';
FOREACH   : 'foreach';
MODIFY    : 'modify';
WITH      : 'with';

// MVEL-specific operators
HASH      : '#';
EXCL_DOT  : '!.';

// MVEL-specific literals (defined to avoid conflicts with imported tokens)
// BigDecimal literals with 'B' suffix
BigDecimalLiteral
    : ('0' | [1-9] (Digits? | '_'+ Digits)) [bB]
    | ((Digits '.' Digits? | '.' Digits) ExponentPart? | Digits ExponentPart) [bB]
    ;

// BigInteger literals with 'I' suffix  
BigIntegerLiteral
    : ('0' | [1-9] (Digits? | '_'+ Digits)) [iI]
    ;

// Temporal literals
MILLISECOND_LITERAL
    : Digits 'ms'
    ;

SECOND_LITERAL
    : Digits 's'
    ;

MINUTE_LITERAL
    : Digits 'm'
    ;

HOUR_LITERAL
    : Digits 'h'
    ;
