// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

parser grammar Java20Parser;

options {
    tokenVocab = Java20Lexer;
}

//=============

start_
    : compilationUnit EOF
    ;

// Paragraph 3.8 Identifiers
// -------------

identifier
    : Identifier
    | contextualKeyword
    ;

typeIdentifier
    : Identifier
    | contextualKeywordMinusForTypeIdentifier
    ;

unqualifiedMethodIdentifier
    : Identifier
    | contextualKeywordMinusForUnqualifiedMethodIdentifier
    ;

// 3.9 Keywords

contextualKeyword
    : EXPORTS
    | MODULE
    | NONSEALED
    | OPEN
    | OPENS
    | PERMITS
    | PROVIDES
    | RECORD
    | REQUIRES
    | SEALED
    | TO
    | TRANSITIVE
    | USES
    | VAR
    | WITH
    | YIELD
    ;

contextualKeywordMinusForTypeIdentifier
    : EXPORTS
    | MODULE
    | NONSEALED
    | OPEN
    | OPENS
//  | PERMITS
    | PROVIDES
//  | RECORD
    | REQUIRES
//  | SEALED
    | TO
    | TRANSITIVE
    | USES
//  | VAR
    | WITH
//  | YIELD
    ;


contextualKeywordMinusForUnqualifiedMethodIdentifier
    : EXPORTS
    | MODULE
    | NONSEALED
    | OPEN
    | OPENS
    | PERMITS
    | PROVIDES
    | RECORD
    | REQUIRES
    | SEALED
    | TO
    | TRANSITIVE
    | USES
    | VAR
    | WITH
//  | YIELD
    ;

// Paragraph 3.10
// --------------

literal
    : IntegerLiteral
    | FloatingPointLiteral
    | BooleanLiteral
    | CharacterLiteral
    | StringLiteral
    | TextBlock
    | NullLiteral
    ;

// Paragraph 4.1 // Type is not used.
// Type    ::=      primitiveType
//          |       referenceType
//          ;

// Paragraph 4.2
// -------------

primitiveType
    : annotation* (numericType | BOOLEAN)
    ;

numericType
    : integralType
    | floatingPointType
    ;

integralType
    : BYTE
    | SHORT
    | INT
    | LONG
    | CHAR
    ;

floatingPointType
    : FLOAT
    | DOUBLE
    ;

// Paragraph 4.3
// -------------

referenceType
    : classOrInterfaceType
    | typeVariable
    | arrayType
    ;

// replace classType in classOrInterfaceType

// classOrInterfaceType
//         : classType
//         | interfaceType
//         ;
//

// classOrInterfaceType
//         :                                      annotation* typeIdentifier typeArguments? coit
//         |             packageName          DOT annotation* typeIdentifier typeArguments? coit
//         |             classOrInterfaceType DOT annotation* typeIdentifier typeArguments?
//         | interfaceType coit
//         ;
//

coit
    : DOT annotation* typeIdentifier typeArguments? coit?
    ;

classOrInterfaceType
    : (packageName DOT)? annotation* typeIdentifier typeArguments? coit?
    ;

classType
    : annotation* typeIdentifier typeArguments?
    | packageName DOT annotation* typeIdentifier typeArguments?
    | classOrInterfaceType DOT annotation* typeIdentifier typeArguments?
    ;

interfaceType
    : classType
    ;

typeVariable
    : annotation* typeIdentifier
    ;

arrayType
    : primitiveType dims
    | classType dims
    | typeVariable dims
    ;

dims
    : annotation* LBRACK RBRACK (annotation* LBRACK RBRACK)*
    ;

// Paragraph 4.4
// -------------

typeParameter
    : typeParameterModifier* typeIdentifier typeBound?
    ;

typeParameterModifier
    : annotation
    ;

typeBound
    : EXTENDS (typeVariable | classOrInterfaceType additionalBound*)
    ;

additionalBound
    : BITAND interfaceType
    ;

// Paragraph 4.5.1
// ---------------

typeArguments
    : LT typeArgumentList GT
    ;

typeArgumentList
    : typeArgument (COMMA typeArgument)*
    ;

typeArgument
    : referenceType
    | wildcard
    ;

wildcard
    : annotation* QUESTION wildcardBounds?
    ;

wildcardBounds
    : EXTENDS referenceType
    | SUPER referenceType
    ;

// Paragraph 6.5
// -------------

moduleName
    : identifier (DOT moduleName)?
    // left recursion --> right recursion
    ;

packageName
    : identifier (DOT packageName)?
    // left recursion --> right recursion
    ;

typeName
    : packageName (DOT typeIdentifier)?
    ;

packageOrTypeName
    : identifier (DOT packageOrTypeName)?
    // left recursion --> right recursion
    ;

expressionName
    : (ambiguousName DOT)? identifier
    ;

methodName
    : unqualifiedMethodIdentifier
    ;

ambiguousName
    : identifier (DOT ambiguousName)?
    // left recursion --> right recursion
    ;

// Paragraph 7.3
// -------------

compilationUnit
    : ordinaryCompilationUnit
    | modularCompilationUnit
    ;

ordinaryCompilationUnit
    : packageDeclaration? importDeclaration* topLevelClassOrInterfaceDeclaration*
    ;

modularCompilationUnit
    : importDeclaration* moduleDeclaration
    ;

// Paragraph 7.4
// -------------

packageDeclaration
    : packageModifier* PACKAGE identifier (DOT identifier)* SEMI
    ;

packageModifier
    : annotation
    ;

// Paragraph 7.5
// -------------

importDeclaration
    : singleTypeImportDeclaration
    | typeImportOnDemandDeclaration
    | singleStaticImportDeclaration
    | staticImportOnDemandDeclaration
    ;

singleTypeImportDeclaration
    : IMPORT typeName SEMI
    ;

typeImportOnDemandDeclaration
    : IMPORT packageOrTypeName DOT MUL SEMI
    ;

singleStaticImportDeclaration
    : IMPORT STATIC typeName DOT identifier SEMI
    ;

staticImportOnDemandDeclaration
    : IMPORT STATIC typeName DOT MUL SEMI
    ;

// Paragraph 7.6
// -------------

topLevelClassOrInterfaceDeclaration
    : classDeclaration
    | interfaceDeclaration
    | SEMI
    ;

// Paragraph 7.7
// -------------

moduleDeclaration
    : annotation* OPEN? MODULE identifier (DOT identifier)* LBRACE moduleDirective* RBRACE
    ;

moduleDirective
    : REQUIRES requiresModifier* moduleName SEMI
    | EXPORTS packageName (TO moduleName ( COMMA moduleName)*)? SEMI
    | OPENS packageName (TO moduleName ( COMMA moduleName)*)? SEMI
    | USES typeName SEMI
    | PROVIDES typeName WITH typeName ( COMMA typeName)* SEMI
    ;

requiresModifier
    : TRANSITIVE
    | STATIC
    ;

// Paragraph 8.1
// -------------

classDeclaration
    : normalClassDeclaration
    | enumDeclaration
    | recordDeclaration
    ;

normalClassDeclaration
    : classModifier* CLASS typeIdentifier typeParameters? classExtends? classImplements? classPermits? classBody
    ;

classModifier
    : annotation
    | PUBLIC
    | PROTECTED
    | PRIVATE
    | ABSTRACT
    | STATIC
    | FINAL
    | SEALED
    | NONSEALED
    | STRICTFP
    ;

typeParameters
    : LT typeParameterList GT
    ;

typeParameterList
    : typeParameter (COMMA typeParameter)*
    ;

classExtends
    : EXTENDS classType
    ;

classImplements
    : IMPLEMENTS interfaceTypeList
    ;

interfaceTypeList
    : interfaceType (COMMA interfaceType)*
    ;

classPermits
    : PERMITS typeName (COMMA typeName)*
    ;

classBody
    : LBRACE classBodyDeclaration* RBRACE
    ;

classBodyDeclaration
    : classMemberDeclaration
    | instanceInitializer
    | staticInitializer
    | constructorDeclaration
    ;

classMemberDeclaration
    : fieldDeclaration
    | methodDeclaration
    | classDeclaration
    | interfaceDeclaration
    | SEMI
    ;

// Paragraph 8.3
// -------------

fieldDeclaration
    : fieldModifier* unannType variableDeclaratorList SEMI
    ;

fieldModifier
    : annotation
    | PUBLIC
    | PROTECTED
    | PRIVATE
    | STATIC
    | FINAL
    | TRANSIENT
    | VOLATILE
    ;

variableDeclaratorList
    : variableDeclarator (COMMA variableDeclarator)*
    ;

variableDeclarator
    : variableDeclaratorId (ADD_ASSIGN variableInitializer)?
    ;

variableDeclaratorId
    : identifier dims?
    ;

variableInitializer
    : expression
    | arrayInitializer
    ;

unannType
    : unannPrimitiveType
    | unannReferenceType
    ;

unannPrimitiveType
    : numericType
    | BOOLEAN
    ;

unannReferenceType
    : unannClassOrInterfaceType
    | unannTypeVariable
    | unannArrayType
    ;

// Replace unannClassType in unannClassOrInterfaceType

// unannClassOrInterfaceType
//         : unannClassType
//         | unannInterfaceType
//         ;
//

unannClassOrInterfaceType
    : (packageName DOT annotation*)? typeIdentifier typeArguments? uCOIT?
    ;

uCOIT
    : DOT annotation* typeIdentifier typeArguments? uCOIT?
    ;

unannClassType
    : typeIdentifier typeArguments?
    | (packageName | unannClassOrInterfaceType) DOT annotation* typeIdentifier typeArguments?
    ;

unannInterfaceType
    : unannClassType
    ;

unannTypeVariable
    : typeIdentifier
    ;

unannArrayType
    : (unannPrimitiveType | unannClassOrInterfaceType | unannTypeVariable) dims
    ;

// Paragraph 8.4
// -------------

methodDeclaration
    : methodModifier* methodHeader methodBody
    ;

methodModifier
    : annotation
    | PUBLIC
    | PROTECTED
    | PRIVATE
    | ABSTRACT
    | STATIC
    | FINAL
    | SYNCHRONIZED
    | NATIVE
    | STRICTFP
    ;

methodHeader
    : (typeParameters annotation*)? result methodDeclarator throwsT?
    ;

result
    : unannType
    | VOID
    ;

methodDeclarator
    : identifier LPAREN (receiverParameter COMMA)? formalParameterList? RPAREN dims?
    ;

receiverParameter
    : annotation* unannType (identifier DOT)? THIS
    ;

formalParameterList
    : formalParameter (COMMA formalParameter)*
    ;

formalParameter
    : variableModifier* unannType variableDeclaratorId
    | variableArityParameter
    ;

variableArityParameter
    : variableModifier* unannType annotation* ELLIPSIS identifier
    ;

variableModifier
    : annotation
    | FINAL
    ;

throwsT
    : THROWS exceptionTypeList
    ;

exceptionTypeList
    : exceptionType (COMMA exceptionType)*
    ;

exceptionType
    : classType
    | typeVariable
    ;

methodBody
    : block
    | SEMI
    ;

// Paragraph 8.6
// -------------

instanceInitializer
    : block
    ;

// Paragraph 8.7
// -------------

staticInitializer
    : STATIC block
    ;

// Paragraph 8.8
// -------------

constructorDeclaration
    : constructorModifier* constructorDeclarator throwsT? constructorBody
    ;

constructorModifier
    : annotation
    | PUBLIC
    | PROTECTED
    | PRIVATE
    ;

constructorDeclarator
    : typeParameters? simpleTypeName LPAREN (receiverParameter COMMA)? formalParameterList? RPAREN
    ;

simpleTypeName
    : typeIdentifier
    ;

constructorBody
    : LBRACE explicitConstructorInvocation? blockStatements? RBRACE
    ;

explicitConstructorInvocation
    : typeArguments? (THIS | SUPER) LPAREN argumentList? RPAREN SEMI
    | (expressionName | primary) DOT typeArguments? SUPER LPAREN argumentList? RPAREN SEMI
    ;

// Paragraph 8.9
// -------------

enumDeclaration
    : classModifier* ENUM typeIdentifier classImplements? enumBody
    ;

enumBody
    : LBRACE enumConstantList? COMMA? enumBodyDeclarations? RBRACE
    // It is not my  grammarmistake! It is based on //docs.oracle.com/javase/specs/jls/se20/jls20.pdf.
    // Notice, javac accepts "enum One { ,  }" and also "enum Two { , ; {} }"
    ;

enumConstantList
    : enumConstant (COMMA enumConstant)*
    ;

enumConstant
    : enumConstantModifier* identifier (LPAREN argumentList? RPAREN)? classBody?
    ;

enumConstantModifier
    : annotation
    ;

enumBodyDeclarations
    : SEMI classBodyDeclaration*
    ;

// Paragraph 8.10
// --------------

recordDeclaration
    : classModifier* RECORD typeIdentifier typeParameters? recordHeader classImplements? recordBody
    ;

recordHeader
    : LPAREN recordComponentList? RPAREN
    ;

recordComponentList
    : recordComponent (COMMA recordComponent)*
    ;

recordComponent
    : recordComponentModifier* unannType identifier
    | variableArityRecordComponent
    ;

variableArityRecordComponent
    : recordComponentModifier* unannType annotation* ELLIPSIS identifier
    ;

recordComponentModifier
    : annotation
    ;

recordBody
    : LBRACE recordBodyDeclaration* RBRACE
    ;

recordBodyDeclaration
    : classBodyDeclaration
    | compactConstructorDeclaration
    ;

compactConstructorDeclaration
    : constructorModifier* simpleTypeName constructorBody
    ;

// Paragraph 9.1
// -------------

interfaceDeclaration
    : normalInterfaceDeclaration
    | annotationInterfaceDeclaration
    ;

normalInterfaceDeclaration
    : interfaceModifier* INTERFACE typeIdentifier typeParameters? interfaceExtends? interfacePermits? interfaceBody
    ;

interfaceModifier
    : annotation
    | PUBLIC
    | PROTECTED
    | PRIVATE
    | ABSTRACT
    | STATIC
    | SEALED
    | NONSEALED
    | STRICTFP
    ;

interfaceExtends
    : EXTENDS interfaceTypeList
    ;

interfacePermits
    : PERMITS typeName (COMMA typeName)*
    ;

interfaceBody
    : LBRACE interfaceMemberDeclaration* RBRACE
    ;

interfaceMemberDeclaration
    : constantDeclaration
    | interfaceMethodDeclaration
    | classDeclaration
    | interfaceDeclaration
    | SEMI
    ;

// Paragraph 9.3
// -------------

constantDeclaration
    : constantModifier* unannType variableDeclaratorList SEMI
    ;

constantModifier
    : annotation
    | PUBLIC
    | STATIC
    | FINAL
    ;

// Paragraph 9.4
// -------------

interfaceMethodDeclaration
    : interfaceMethodModifier* methodHeader methodBody
    ;

interfaceMethodModifier
    : annotation
    | PUBLIC
    | PRIVATE
    | ABSTRACT
    | DEFAULT
    | STATIC
    | STRICTFP
    ;

// Paragraph 9.6
// -------------

annotationInterfaceDeclaration
    : interfaceModifier* AT INTERFACE typeIdentifier annotationInterfaceBody
    ;

annotationInterfaceBody
    : LBRACE annotationInterfaceMemberDeclaration* RBRACE
    ;

annotationInterfaceMemberDeclaration
    : annotationInterfaceElementDeclaration
    | constantDeclaration
    | classDeclaration
    | interfaceDeclaration
    | SEMI
    ;

annotationInterfaceElementDeclaration
    : annotationInterfaceElementModifier* unannType identifier LPAREN RPAREN dims? defaultValue? SEMI
    ;

annotationInterfaceElementModifier
    : annotation
    | PUBLIC
    | ABSTRACT
    ;

defaultValue
    : DEFAULT elementValue
    ;

// Paragraph 9.7
// -------------

annotation
    : normalAnnotation
    | markerAnnotation
    | singleElementAnnotation
    ;

normalAnnotation
    : AT typeName LPAREN elementValuePairList? RPAREN
    ;

elementValuePairList
    : elementValuePair (COMMA elementValuePair)*
    ;

elementValuePair
    : identifier ADD_ASSIGN elementValue
    ;

elementValue
    : conditionalExpression
    | elementValueArrayInitializer
    | annotation
    ;

elementValueArrayInitializer
    : LBRACE elementValueList? COMMA? RBRACE
    ;

elementValueList
    : elementValue (COMMA elementValue)*
    ;

markerAnnotation
    : AT typeName
    ;

singleElementAnnotation
    : AT typeName LPAREN elementValue RPAREN
    ;

// Paragraph 10.6
// --------------

arrayInitializer
    : LBRACE variableInitializerList? COMMA? RBRACE
    // Strange  COMMA  ?! staat ook in antlr_java.g4
    ;

variableInitializerList
    : variableInitializer (COMMA variableInitializer)*
    ;

// Paragraph 14.2
// --------------

block
    : LBRACE blockStatements? RBRACE
    ;

blockStatements
    : blockStatement blockStatement*
    ;

blockStatement
    : localClassOrInterfaceDeclaration
    | localVariableDeclarationStatement
    | statement
    ;

// Paragraph 14.3
// --------------

localClassOrInterfaceDeclaration
    : classDeclaration
    | normalInterfaceDeclaration
    ;

// Paragraph 14.4
// --------------

localVariableDeclaration
    : variableModifier* localVariableType variableDeclaratorList?
    ;

localVariableType
    : unannType
    | VAR
    ;

localVariableDeclarationStatement
    : localVariableDeclaration SEMI
    ;

// Paragraph 14.5
// --------------

statement
    : statementWithoutTrailingSubstatement
    | labeledStatement
    | ifThenStatement
    | ifThenElseStatement
    | whileStatement
    | forStatement
    ;

statementNoShortIf
    : statementWithoutTrailingSubstatement
    | labeledStatementNoShortIf
    | ifThenElseStatementNoShortIf
    | whileStatementNoShortIf
    | forStatementNoShortIf
    ;

statementWithoutTrailingSubstatement
    : block
    | emptyStatement_
    | expressionStatement
    | assertStatement
    | switchStatement
    | doStatement
    | breakStatement
    | continueStatement
    | returnStatement
    | synchronizedStatement
    | throwStatement
    | tryStatement
    | yieldStatement
    ;

// Paragraph 14.6
// --------------

emptyStatement_
    : SEMI
    ;

// Paragraph 14.7
// --------------

labeledStatement
    : identifier COLON statement
    ;

labeledStatementNoShortIf
    : identifier COLON statementNoShortIf
    ;

// Paragraph 14.8
// --------------

expressionStatement
    : statementExpression SEMI
    ;

statementExpression
    : assignment
    | preIncrementExpression
    | preDecrementExpression
    | postIncrementExpression
    | postDecrementExpression
    | methodInvocation
    | classInstanceCreationExpression
    ;

// Paragraph 14.9
// --------------

ifThenStatement
    : IF LPAREN expression RPAREN statement
    ;

ifThenElseStatement
    : IF LPAREN expression RPAREN statementNoShortIf ELSE statement
    ;

ifThenElseStatementNoShortIf
    : IF LPAREN expression RPAREN statementNoShortIf ELSE statementNoShortIf
    ;

// Paragraph 14.10
// ---------------

assertStatement
    : ASSERT expression (COLON expression)? SEMI
    ;

// Paragraph 14.11
// --------------

switchStatement
    : SWITCH LPAREN expression RPAREN switchBlock
    ;

switchBlock
    : LBRACE switchRule switchRule* RBRACE
    | LBRACE switchBlockStatementGroup* ( switchLabel COLON)* RBRACE
    ;

switchRule
    : switchLabel ARROW (expression SEMI | block | throwStatement)
    ;

switchBlockStatementGroup
    : switchLabel COLON (switchLabel COLON)* blockStatements
    ;

switchLabel
    : CASE caseConstant (COMMA caseConstant)*
    | DEFAULT
    ;

caseConstant
    : conditionalExpression
    ;

// Paragraph 14.12
// ---------------

whileStatement
    : WHILE LPAREN expression RPAREN statement
    ;

whileStatementNoShortIf
    : WHILE LPAREN expression RPAREN statementNoShortIf
    ;

// Paragraph 14.13
// ---------------

doStatement
    : DO statement WHILE LPAREN expression RPAREN SEMI
    ;

// Paragraph 14.14
// ---------------

forStatement
    : basicForStatement
    | enhancedForStatement
    ;

forStatementNoShortIf
    : basicForStatementNoShortIf
    | enhancedForStatementNoShortIf
    ;

basicForStatement
    : FOR LPAREN forInit? SEMI expression? SEMI forUpdate? RPAREN statement
    ;

basicForStatementNoShortIf
    : FOR LPAREN forInit? SEMI expression? SEMI forUpdate? RPAREN statementNoShortIf
    ;

forInit
    : statementExpressionList
    | localVariableDeclaration
    ;

forUpdate
    : statementExpressionList
    ;

statementExpressionList
    : statementExpression (COMMA statementExpression)*
    ;

enhancedForStatement
    : FOR LPAREN localVariableDeclaration COLON expression RPAREN statement
    ;

enhancedForStatementNoShortIf
    : FOR LPAREN localVariableDeclaration COLON expression RPAREN statementNoShortIf
    ;

// Paragraph 14.15
// ---------------

breakStatement
    : BREAK identifier? SEMI
    ;

// Paragraph 14.16
// ---------------

continueStatement
    : CONTINUE identifier? SEMI
    ;

// Paragraph 14.17
// ---------------

returnStatement
    : RETURN expression? SEMI
    ;

// Paragraph 14.18
// ---------------

throwStatement
    : THROW expression SEMI
    ;

// Paragraph 14.19
// ---------------

synchronizedStatement
    : SYNCHRONIZED LPAREN expression RPAREN block
    ;

// Paragraph 14.20
// ---------------

tryStatement
    : TRY block catches
    | TRY block finallyBlock
    | TRY block catches? finallyBlock
    | tryWithResourcesStatement
    ;

catches
    : catchClause catchClause*
    ;

catchClause
    : CATCH LPAREN catchFormalParameter RPAREN block
    ;

catchFormalParameter
    : variableModifier* catchType variableDeclaratorId
    ;

catchType
    : unannClassType (BITOR classType)*
    ;

finallyBlock
    : FINALLY block
    ;

tryWithResourcesStatement
    : TRY resourceSpecification block catches? finallyBlock?
    ;

resourceSpecification
    : LPAREN resourceList SEMI? RPAREN
    ;

resourceList
    : resource (SEMI resource)*
    ;

resource
    : localVariableDeclaration
    | variableAccess
    ;

variableAccess
    : expressionName
    | fieldAccess
    ;

// Paragraph 14.21
//----------------

yieldStatement
    : YIELD expression SEMI
    ;

// Paragraph 14.30
// --------------

pattern
    : typePattern
    ;

typePattern
    : localVariableDeclaration
    ;

// Paragraph 15.2
// --------------

expression
    : lambdaExpression
    | assignmentExpression
    ;

// Paragraph 15.8
// --------------

primary
    : primaryNoNewArray
    | arrayCreationExpression
    ;

// Replace classInstanceCreationExpression, fieldAccess, arrayAccess, methodInvocation, and
// methodReference in primaryNoNewArray.
// Replace in these two rules primary by primaryNoNewArray.

// primaryNoNewArray
//         : literal
//         | classLiteral
//         | THIS
//         | typeName DOT THIS
//         | LPAREN expression RPAREN
//         | classInstanceCreationExpression
//         | fieldAccess
//         | arrayAccess
//         | methodInvocation
//         | methodReference
//         ;
//

// primaryNoNewArray
//         : literal
//         | classLiteral
//         | THIS
//         | typeName DOT THIS
//         | LPAREN expression RPAREN
//         |                                  unqualifiedClassInstanceCreationExpression
//         | expressionName              DOT  unqualifiedClassInstanceCreationExpression
//
//         | primaryNoNewArray           DOT  unqualifiedClassInstanceCreationExpression
//         | arrayCreationExpression     DOT  unqualifiedClassInstanceCreationExpression
//
//         | primaryNoNewArray           DOT  Identifier
//         | arrayCreationExpression     DOT  Identifier
//
//         | SUPER  DOT                     Identifier
//         | typeName DOT SUPER        DOT  Identifier
//
//         | expressionName                         LBRACK expression RBRACK
//         | primaryNoNewArray                      LBRACK expression RBRACK
//         | arrayCreationExpressionWithInitializer LBRACK expression RBRACK
//
//         | methodName                                                 LPAREN argumentList? RPAREN
//         | typeName                    DOT  typeArguments? Identifier LPAREN argumentList? RPAREN
//         | expressionName              DOT  typeArguments? Identifier LPAREN argumentList? RPAREN
//
//         | primaryNoNewArray           DOT  typeArguments? Identifier LPAREN argumentList? RPAREN
//         | arrayCreationExpression     DOT  typeArguments? Identifier LPAREN argumentList? RPAREN
//
//         | SUPER                     DOT  typeArguments? Identifier LPAREN argumentList? RPAREN
//         | typeName       DOT SUPER  DOT  typeArguments? Identifier LPAREN argumentList? RPAREN
//
//         | expressionName              COLONCOLON typeArguments? Identifier
//
//         | primaryNoNewArray           COLONCOLON typeArguments? Identifier
//         | arrayCreationExpression     COLONCOLON typeArguments? Identifier
//
//
//         | referenceType               COLONCOLON typeArguments? Identifier
//         | SUPER                     COLONCOLON typeArguments? Identifier
//         | typeName DOT SUPER        COLONCOLON typeArguments? Identifier
//         | classType                   COLONCOLON typeArguments? NEW
//         | arrayType                   COLONCOLON                NEW
//         ;
//

primaryNoNewArray
    : literal pNNA?
    | classLiteral pNNA?
    | THIS pNNA?
    | typeName DOT THIS pNNA?
    | LPAREN expression RPAREN pNNA?
    | unqualifiedClassInstanceCreationExpression pNNA?
    | expressionName DOT unqualifiedClassInstanceCreationExpression pNNA?
    | arrayCreationExpression DOT unqualifiedClassInstanceCreationExpression pNNA?
    | arrayCreationExpression DOT identifier pNNA?
    | SUPER DOT identifier pNNA?
    | typeName DOT SUPER DOT identifier pNNA?
    | expressionName LBRACK expression RBRACK pNNA?
    | arrayCreationExpressionWithInitializer LBRACK expression RBRACK pNNA?
    | methodName LPAREN argumentList? RPAREN pNNA?
    | typeName DOT typeArguments? identifier LPAREN argumentList? RPAREN pNNA?
    | expressionName DOT typeArguments? identifier LPAREN argumentList? RPAREN pNNA?
    | arrayCreationExpression DOT typeArguments? identifier LPAREN argumentList? RPAREN pNNA?
    | SUPER DOT typeArguments? identifier LPAREN argumentList? RPAREN pNNA?
    | typeName DOT SUPER DOT typeArguments? identifier LPAREN argumentList? RPAREN pNNA?
    | expressionName COLONCOLON typeArguments? identifier pNNA?
    | arrayCreationExpression COLONCOLON typeArguments? identifier pNNA?
    | referenceType COLONCOLON typeArguments? identifier pNNA?
    | SUPER COLONCOLON typeArguments? identifier pNNA?
    | typeName DOT SUPER COLONCOLON typeArguments? identifier pNNA?
    | classType COLONCOLON typeArguments? NEW pNNA?
    | arrayType COLONCOLON NEW pNNA?
    ;

pNNA
    : DOT unqualifiedClassInstanceCreationExpression pNNA?
    | DOT identifier pNNA?
    | LBRACK expression RBRACK pNNA?
    | DOT typeArguments? identifier LPAREN argumentList? RPAREN pNNA?
    | COLONCOLON typeArguments? identifier pNNA?
    ;

classLiteral
    : typeName (LBRACK RBRACK)* DOT CLASS
    | numericType ( LBRACK RBRACK)* DOT CLASS
    | BOOLEAN ( LBRACK RBRACK)* DOT CLASS
    | VOID DOT CLASS
    ;

// Paragraph 15.9
// --------------

classInstanceCreationExpression
    : unqualifiedClassInstanceCreationExpression
    | expressionName DOT unqualifiedClassInstanceCreationExpression
    | primary DOT unqualifiedClassInstanceCreationExpression
    ;

unqualifiedClassInstanceCreationExpression
    : NEW typeArguments? classOrInterfaceTypeToInstantiate LPAREN argumentList? RPAREN classBody?
    ;

classOrInterfaceTypeToInstantiate
    : annotation* identifier (DOT annotation* identifier)* typeArgumentsOrDiamond?
    ;

typeArgumentsOrDiamond
    : typeArguments
    | OACA
    ;

// Paragraph 15.10
// ---------------

arrayCreationExpression
    : arrayCreationExpressionWithoutInitializer
    | arrayCreationExpressionWithInitializer
    ;

arrayCreationExpressionWithoutInitializer
    : NEW primitiveType dimExprs dims?
    | NEW classType dimExprs dims?
    ;

arrayCreationExpressionWithInitializer
    : NEW primitiveType dims arrayInitializer
    | NEW classOrInterfaceType dims arrayInitializer
    ;

dimExprs
    : dimExpr dimExpr*
    ;

dimExpr
    : annotation* LBRACK expression RBRACK
    ;

arrayAccess
    : expressionName LBRACK expression RBRACK
    | primaryNoNewArray LBRACK expression RBRACK
    | arrayCreationExpressionWithInitializer LBRACK expression RBRACK
    ;

// Paragraph 15.11
// ---------------

fieldAccess
    : primary DOT identifier
    | SUPER DOT identifier
    | typeName DOT SUPER DOT identifier
    ;

// Paragraph 15.12
// ---------------

methodInvocation
    : methodName LPAREN argumentList? RPAREN
    | typeName DOT typeArguments? identifier LPAREN argumentList? RPAREN
    | expressionName DOT typeArguments? identifier LPAREN argumentList? RPAREN
    | primary DOT typeArguments? identifier LPAREN argumentList? RPAREN
    | SUPER DOT typeArguments? identifier LPAREN argumentList? RPAREN
    | typeName DOT SUPER DOT typeArguments? identifier LPAREN argumentList? RPAREN
    ;

argumentList
    : expression (COMMA expression)*
    ;

// Paragraph 15.13
// ---------------

methodReference
    : expressionName COLONCOLON typeArguments? identifier
    | primary COLONCOLON typeArguments? identifier
    | referenceType COLONCOLON typeArguments? identifier
    | SUPER COLONCOLON typeArguments? identifier
    | typeName DOT SUPER COLONCOLON typeArguments? identifier
    | classType COLONCOLON typeArguments? NEW
    | arrayType COLONCOLON NEW
    ;

// Paragraph 15.14
// ---------------

// Replace postIncrementExpression and postDecrementExpression by postfixExpression.

// postfixExpression
//         : primary
//         | expressionName
//         | postIncrementExpression
//         | postDecrementExpression
//         ;
//

// postfixExpression
//         : primary
//         | expressionName
//         | postfixExpression '++'
//         | postfixExpression DEC
//         ;
//

postfixExpression
    : primary pfE?
    | expressionName pfE?
    ;

pfE
    : INC pfE?
    | DEC pfE?
    ;

postIncrementExpression
    : postfixExpression INC
    ;

postDecrementExpression
    : postfixExpression DEC
    ;

// Paragraph 15.15
// ---------------

unaryExpression
    : preIncrementExpression
    | preDecrementExpression
    | ADD unaryExpression
    | SUB unaryExpression
    | unaryExpressionNotPlusMinus
    ;

preIncrementExpression
    : INC unaryExpression
    ;

preDecrementExpression
    : DEC unaryExpression
    ;

unaryExpressionNotPlusMinus
    : postfixExpression
    | TILDE unaryExpression
    | BANG unaryExpression
    | castExpression
    | switchExpression
    ;

// Paragraph 15.16
// ---------------

castExpression
    : LPAREN primitiveType RPAREN unaryExpression
    | LPAREN referenceType additionalBound* RPAREN unaryExpressionNotPlusMinus
    | LPAREN referenceType additionalBound* RPAREN lambdaExpression
    ;

// Paragraph 15.17
// ---------------

multiplicativeExpression
    : unaryExpression
    | multiplicativeExpression MUL unaryExpression
    | multiplicativeExpression DIV unaryExpression
    | multiplicativeExpression MOD unaryExpression
    ;

// Paragraph 15.18
// ---------------

additiveExpression
    : multiplicativeExpression
    | additiveExpression ADD multiplicativeExpression
    | additiveExpression SUB multiplicativeExpression
    ;

// Paragraph 15.19
// ---------------

shiftExpression
    : additiveExpression
    | shiftExpression LT LT additiveExpression
    | shiftExpression GT GT additiveExpression
    | shiftExpression GT GT GT additiveExpression
    ;

// Paragraph 15.20
// ---------------

relationalExpression
    : shiftExpression
    | relationalExpression LT shiftExpression
    | relationalExpression GT shiftExpression
    | relationalExpression LE shiftExpression
    | relationalExpression GE shiftExpression
    //      | instanceofExpression
    | relationalExpression INSTANCEOF (referenceType | pattern)
    // Solves left recursion with instanceofExpression.
    ;

// instanceofExpression
//        : relationalExpression INSTANCEOF (referenceType | pattern)
//        ;
// Resulted to left recursion with relationalExpression.

// Paragraph 15.21
// ---------------

equalityExpression
    : relationalExpression
    | equalityExpression EQUAL relationalExpression
    | equalityExpression NOTEQUAL relationalExpression
    ;

// Paragraph 15.22
// ---------------

andExpression
    : equalityExpression
    | andExpression BITAND equalityExpression
    ;

exclusiveOrExpression
    : andExpression
    | exclusiveOrExpression CARET andExpression
    ;

inclusiveOrExpression
    : exclusiveOrExpression
    | inclusiveOrExpression BITOR exclusiveOrExpression
    ;

// Paragraph 15.23
// ---------------

conditionalAndExpression
    : inclusiveOrExpression
    | conditionalAndExpression AND inclusiveOrExpression
    ;

// Paragraph 15.24
// ---------------

conditionalOrExpression
    : conditionalAndExpression
    | conditionalOrExpression OR conditionalAndExpression
    ;

// Paragraph 15.25
// ---------------

conditionalExpression
    : conditionalOrExpression
    | conditionalOrExpression QUESTION expression COLON conditionalExpression
    | conditionalOrExpression QUESTION expression COLON lambdaExpression
    ;

// Paragraph 15.26
// ---------------

assignmentExpression
    : conditionalExpression
    | assignment
    ;

assignment
    : leftHandSide assignmentOperator expression
    ;

leftHandSide
    : expressionName
    | fieldAccess
    | arrayAccess
    ;

assignmentOperator
    : ASSIGN
    | MUL_ASSIGN
    | DIV_ASSIGN
    | MOD_ASSIGN
    | ADD_ASSIGN
    | SUB_ASSIGN
    | LSHIFT_ASSIGN
    | RSHIFT_ASSIGN
    | URSHIFT_ASSIGN
    | AND_ASSIGN
    | XOR_ASSIGN
    | OR_ASSIGN
    ;

// Paragraph 15.27
// ---------------

lambdaExpression
    : lambdaParameters ARROW lambdaBody
    ;

lambdaParameters
    : LPAREN lambdaParameterList? RPAREN
    | identifier
    ;

lambdaParameterList
    : lambdaParameter (COMMA lambdaParameter)*
    | identifier ( COMMA identifier)*
    ;

lambdaParameter
    : variableModifier* lambdaParameterType variableDeclaratorId
    | variableArityParameter
    ;

lambdaParameterType
    : unannType
    | VAR
    ;

lambdaBody
    : expression
    | block
    ;

// Paragraph 15.28
// ---------------

switchExpression
    : SWITCH LPAREN expression RPAREN switchBlock
    ;

// Paragraph 15.29
// ---------------

constantExpression
    : expression
    ;
