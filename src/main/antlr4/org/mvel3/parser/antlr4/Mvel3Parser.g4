// MVEL3 Parser - minimal MVEL expression parser

parser grammar Mvel3Parser;

import JavaParser; // Import Java parser for basic types and structures

options {
    tokenVocab = Mvel3Lexer;
}

// Start rule for MVEL expressions
mvelStart
    : mvelExpression SEMI? EOF
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

// Override expression to add MVEL-specific inline cast syntax at expression level with suffix
expression
    // Expression order in accordance with https://introcs.cs.princeton.edu/java/11precedence/
    // Level 16, Primary, array and member access
    : primary                                                       #PrimaryExpression
    | expression '[' expression ']'                                 #SquareBracketExpression

    // Mvel 3
    | expression HASH typeType HASH (identifier arguments? | '[' expression ']')? #InlineCastExpression

    | expression bop = '.' (
        identifier
        | methodCall
        | THIS
        | NEW nonWildcardTypeArguments? innerCreator
        | SUPER superSuffix
        | explicitGenericInvocation
    )                                                               #MemberReferenceExpression
    // Method calls and method references are part of primary, and hence level 16 precedence
    | methodCall                                                    #MethodCallExpression
    | expression '::' typeArguments? identifier                     #MethodReferenceExpression
    | typeType '::' (typeArguments? identifier | NEW)               #MethodReferenceExpression
    | classType '::' typeArguments? NEW                             #MethodReferenceExpression

    // Java17
    | switchExpression                                              #ExpressionSwitch

    // Level 15 Post-increment/decrement operators
    | expression postfix = ('++' | '--')                            #PostIncrementDecrementOperatorExpression

    // Level 14, Unary operators
    | prefix = ('+' | '-' | '++' | '--' | '~' | '!') expression     #UnaryOperatorExpression

    // Level 13 Cast and object creation
    | '(' annotation* typeType ('&' typeType)* ')' expression       #CastExpression
    | NEW creator                                                   #ObjectCreationExpression

    // Level 12 to 1, Remaining operators
    // Level 12, Multiplicative operators
    | expression bop = ('*' | '/' | '%') expression           #BinaryOperatorExpression
    // Level 11, Additive operators
    | expression bop = ('+' | '-') expression                 #BinaryOperatorExpression
    // Level 10, Shift operators
    | expression ('<' '<' | '>' '>' '>' | '>' '>') expression #BinaryOperatorExpression
    // Level 9, Relational operators
    | expression bop = ('<=' | '>=' | '>' | '<') expression   #BinaryOperatorExpression
    | expression bop = INSTANCEOF (typeType | pattern)        #InstanceOfOperatorExpression
    // Level 8, Equality Operators
    | expression bop = ('==' | '!=') expression               #BinaryOperatorExpression
    // Level 7, Bitwise AND
    | expression bop = '&' expression                         #BinaryOperatorExpression
    // Level 6, Bitwise XOR
    | expression bop = '^' expression                         #BinaryOperatorExpression
    // Level 5, Bitwise OR
    | expression bop = '|' expression                         #BinaryOperatorExpression
    // Level 4, Logic AND
    | expression bop = '&&' expression                        #BinaryOperatorExpression
    // Level 3, Logic OR
    | expression bop = '||' expression                        #BinaryOperatorExpression
    // Level 2, Ternary
    | <assoc = right> expression bop = '?' expression ':' expression #TernaryExpression
    // Level 1, Assignment
    | <assoc = right> expression bop = (
        '='
        | '+='
        | '-='
        | '*='
        | '/='
        | '&='
        | '|='
        | '^='
        | '>>='
        | '>>>='
        | '<<='
        | '%='
    ) expression                                              #BinaryOperatorExpression

    // Level 0, Lambda Expression // Java8
    | lambdaExpression                                        #ExpressionLambda
    ;

// Override expression to add MVEL-specific modify statement
statement
    : blockLabel = block
    | ASSERT expression (':' expression)? ';'
    | IF parExpression statement (ELSE statement)?
    | FOR '(' forControl ')' statement
    | WHILE parExpression statement
    | DO statement WHILE parExpression ';'
    | TRY block (catchClause+ finallyBlock? | finallyBlock)
    | TRY resourceSpecification block catchClause* finallyBlock?
    | SWITCH parExpression '{' switchBlockStatementGroup* switchLabel* '}'
    | SYNCHRONIZED parExpression block
    | RETURN expression? ';'
    | THROW expression ';'
    | BREAK identifier? ';'
    | CONTINUE identifier? ';'
    | YIELD expression ';' // Java17
    | SEMI
    | statementExpression = expression ';'
    | switchExpression ';'? // Java17
    | identifierLabel = identifier ':' statement
    | modifyStatement               // MVEL-specific modify statement
    | withStatement                 // MVEL-specific with statement
    ;

// MVEL-specific modify statement
modifyStatement
    : MODIFY LPAREN identifier RPAREN LBRACE (statement)* RBRACE
    ;

// MVEL-specific with statement
withStatement
    : WITH LPAREN identifier RPAREN LBRACE (statement)* RBRACE
    ;


// Override block without any changes. Just for ANTLR plugin conveinience. We may remove this later.
block
    : '{' blockStatement* '}'
    ;
