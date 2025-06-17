// MVEL3 Parser - minimal MVEL expression parser

parser grammar Mvel3Parser;

import JavaParser; // Import Java parser for basic types and structures

options {
    tokenVocab = Mvel3Lexer;
}

// Start rule for MVEL expressions
mvelStart
    : mvelExpression EOF
    ;

// MVEL expression - start with simple expression support
mvelExpression
    : expression
    ;

// Override literal to include MVEL-specific BigDecimal and BigInteger literals
literal
    : DECIMAL_LITERAL
    | HEX_LITERAL
    | OCT_LITERAL
    | BINARY_LITERAL
    | FLOAT_LITERAL
    | HEX_FLOAT_LITERAL
    | BOOL_LITERAL
    | CHAR_LITERAL
    | STRING_LITERAL
    | TEXT_BLOCK
    | NULL_LITERAL
    | BigDecimalLiteral
    | BigIntegerLiteral
    ;
