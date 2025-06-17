// MVEL3 Lexer - extends Java 20 lexer with MVEL-specific tokens

lexer grammar Mvel3Lexer;

import Java20Lexer;

// MVEL-specific keywords and operators
IN        : 'in';
CONTAINS  : 'contains';
MATCHES   : 'matches';
SOUNDSLIKE: 'soundslike';
STRSIM    : 'strsim';
FOREACH   : 'foreach';
MODIFY    : 'modify';
WITH      : 'with';

// MVEL-specific literals (defined to avoid conflicts with imported tokens)
// BigDecimal literals with 'B' suffix
BigDecimalLiteral
    : DecimalNumeral [bB]
    | DecimalFloatingPointLiteral [bB]
    ;

// BigInteger literals with 'I' suffix  
BigIntegerLiteral
    : DecimalNumeral [iI]
    ;

