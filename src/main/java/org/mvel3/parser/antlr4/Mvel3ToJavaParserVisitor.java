/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.parser.antlr4;

import com.github.javaparser.TokenRange;
import com.github.javaparser.JavaToken;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VarType;
import org.mvel3.parser.ast.expr.ModifyStatement;
import org.mvel3.parser.ast.expr.BigDecimalLiteralExpr;
import org.mvel3.parser.ast.expr.BigIntegerLiteralExpr;

import static org.mvel3.parser.util.AstUtils.getBinaryExprOperator;

/**
 * Visitor that converts ANTLR4 parse tree to JavaParser AST nodes.
 * This implementation can return various types of nodes (Expression, Type, etc.).
 */
public class Mvel3ToJavaParserVisitor extends Mvel3ParserBaseVisitor<Node> {
    
    /**
     * Create a JavaParser TokenRange from ANTLR ParserRuleContext.
     * This provides proper source location information instead of using TokenRange.INVALID.
     */
    private TokenRange createTokenRange(ParserRuleContext ctx) {
        if (ctx == null) {
            return TokenRange.INVALID;
        }
        
        Token startToken = ctx.getStart();
        Token stopToken = ctx.getStop();
        
        if (startToken == null || stopToken == null) {
            return TokenRange.INVALID;
        }
        
        // Create JavaParser positions
        Position startPos = new Position(startToken.getLine(), startToken.getCharPositionInLine() + 1);
        Position stopPos = new Position(stopToken.getLine(), stopToken.getCharPositionInLine() + stopToken.getText().length());
        
        // Create JavaParser Range
        Range range = new Range(startPos, stopPos);
        
        // Create JavaParser JavaTokens (simplified - we use token type 0 and the actual text)
        JavaToken startJavaToken = new JavaToken(0, startToken.getText());
        startJavaToken.setRange(range);
        
        JavaToken stopJavaToken = new JavaToken(0, stopToken.getText());
        stopJavaToken.setRange(range);
        
        return new TokenRange(startJavaToken, stopJavaToken);
    }
    
    /**
     * Create a TokenRange from a single ANTLR token (for terminal nodes).
     */
    private TokenRange createTokenRange(Token token) {
        if (token == null) {
            return TokenRange.INVALID;
        }
        
        Position startPos = new Position(token.getLine(), token.getCharPositionInLine() + 1);
        Position stopPos = new Position(token.getLine(), token.getCharPositionInLine() + token.getText().length());
        Range range = new Range(startPos, stopPos);
        
        JavaToken javaToken = new JavaToken(0, token.getText());
        javaToken.setRange(range);
        
        return new TokenRange(javaToken, javaToken);
    }

    @Override
    public Node visitMvelStart(Mvel3Parser.MvelStartContext ctx) {
        return visit(ctx.mvelExpression());
    }

    @Override
    public Node visitMvelExpression(Mvel3Parser.MvelExpressionContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Node visitBinaryOperatorExpression(Mvel3Parser.BinaryOperatorExpressionContext ctx) {
        Expression left = (Expression) visit(ctx.expression(0));
        Expression right = (Expression) visit(ctx.expression(1));
        
        String operatorText = ctx.bop.getText();
        
        // Handle assignment operators separately
        if ("=".equals(operatorText)) {
            return new AssignExpr(createTokenRange(ctx), left, right, AssignExpr.Operator.ASSIGN);
        }
        
        // Handle other binary operators
        BinaryExpr.Operator operator = getBinaryExprOperator(operatorText);
        return new BinaryExpr(createTokenRange(ctx), left, right, operator);
    }

    @Override
    public Node visitMemberReferenceExpression(Mvel3Parser.MemberReferenceExpressionContext ctx) {
        // Handle member reference like "java.math.MathContext.DECIMAL128"
        Expression scope = (Expression) visit(ctx.expression());
        
        if (ctx.identifier() != null) {
            // Simple field access: expression.identifier
            String fieldName = ctx.identifier().getText();
            FieldAccessExpr fieldAccess = new FieldAccessExpr(scope, fieldName);
            fieldAccess.setTokenRange(createTokenRange(ctx));
            return fieldAccess;
        } else if (ctx.methodCall() != null) {
            // Method call: expression.methodCall()
            String methodName = ctx.methodCall().identifier().getText();
            NodeList<Expression> args = parseArguments(ctx.methodCall().arguments());
            
            MethodCallExpr methodCall = new MethodCallExpr(scope, methodName);
            methodCall.setArguments(args);
            methodCall.setTokenRange(createTokenRange(ctx));
            return methodCall;
        } else if (ctx.THIS() != null) {
            // expression.this
            FieldAccessExpr fieldAccess = new FieldAccessExpr(scope, "this");
            fieldAccess.setTokenRange(createTokenRange(ctx));
            return fieldAccess;
        } else if (ctx.SUPER() != null && ctx.superSuffix() != null) {
            // expression.super.something
            // TODO: Implement super handling
            throw new UnsupportedOperationException("Super references not yet implemented");
        } else if (ctx.NEW() != null && ctx.innerCreator() != null) {
            // expression.new InnerClass()
            // TODO: Implement inner class creation
            throw new UnsupportedOperationException("Inner class creation not yet implemented");
        }
        
        throw new IllegalArgumentException("Unsupported member reference: " + ctx.getText());
    }

    @Override
    public Node visitPrimaryExpression(Mvel3Parser.PrimaryExpressionContext ctx) {
        return visit(ctx.primary());
    }

    @Override
    public Node visitPrimary(Mvel3Parser.PrimaryContext ctx) {
        if (ctx.literal() != null) {
            return visit(ctx.literal());
        } else if (ctx.identifier() != null) {
            NameExpr nameExpr = new NameExpr(ctx.identifier().getText());
            nameExpr.setTokenRange(createTokenRange(ctx));
            return nameExpr;
        } else if (ctx.LPAREN() != null && ctx.expression() != null && ctx.RPAREN() != null) {
            // Parenthesized expression
            EnclosedExpr enclosedExpr = new EnclosedExpr((Expression) visit(ctx.expression()));
            enclosedExpr.setTokenRange(createTokenRange(ctx));
            return enclosedExpr;
        } else if (ctx.THIS() != null) {
            ThisExpr thisExpr = new ThisExpr();
            thisExpr.setTokenRange(createTokenRange(ctx));
            return thisExpr;
        }
        
        // Handle other primary cases that might be needed
        return visitChildren(ctx);
    }

    @Override
    public Node visitInlineCastExpression(Mvel3Parser.InlineCastExpressionContext ctx) {
        return visit(ctx.inlineCast());
    }

    @Override
    public Node visitInlineCast(Mvel3Parser.InlineCastContext ctx) {
        // Handle inline cast: primary#Type#[methodCall] or primary#Type#[arrayAccess]
        Expression expr = (Expression) visit(ctx.primary());
        Type type = (Type) visit(ctx.typeType());
        CastExpr castExpr = new CastExpr(type, expr);
        castExpr.setTokenRange(createTokenRange(ctx));
        
        // Check what comes after the cast
        if (ctx.identifier() != null) {
            String methodName = ctx.identifier().getText();
            if (ctx.arguments() != null) {
                // Method call with arguments
                MethodCallExpr methodCall = new MethodCallExpr(castExpr, methodName);
                methodCall.setTokenRange(createTokenRange(ctx));
                // Parse arguments if they exist
                NodeList<Expression> args = parseArguments(ctx.arguments());
                methodCall.setArguments(args);
                return methodCall;
            } else {
                // Field access
                FieldAccessExpr fieldAccess = new FieldAccessExpr(castExpr, methodName);
                fieldAccess.setTokenRange(createTokenRange(ctx));
                return fieldAccess;
            }
        } else if (ctx.LBRACK() != null && ctx.expression() != null && ctx.RBRACK() != null) {
            // Array access: primary#Type#[expression]
            // Convert to method call: ((Type)primary).get(expression)
            Expression indexExpr = (Expression) visit(ctx.expression());
            MethodCallExpr methodCall = new MethodCallExpr(castExpr, "get");
            methodCall.setTokenRange(createTokenRange(ctx));
            methodCall.addArgument(indexExpr);
            return methodCall;
        }
        
        return castExpr;
    }

    @Override
    public Node visitLiteral(Mvel3Parser.LiteralContext ctx) {
        if (ctx.STRING_LITERAL() != null) {
            String text = ctx.STRING_LITERAL().getText();
            // Remove quotes
            String value = text.substring(1, text.length() - 1);
            StringLiteralExpr stringLiteral = new StringLiteralExpr(value);
            stringLiteral.setTokenRange(createTokenRange(ctx));
            return stringLiteral;
        } else if (ctx.DECIMAL_LITERAL() != null) {
            IntegerLiteralExpr integerLiteral = new IntegerLiteralExpr(ctx.DECIMAL_LITERAL().getText());
            integerLiteral.setTokenRange(createTokenRange(ctx));
            return integerLiteral;
        } else if (ctx.FLOAT_LITERAL() != null) {
            DoubleLiteralExpr doubleLiteral = new DoubleLiteralExpr(ctx.FLOAT_LITERAL().getText());
            doubleLiteral.setTokenRange(createTokenRange(ctx));
            return doubleLiteral;
        } else if (ctx.BOOL_LITERAL() != null) {
            BooleanLiteralExpr booleanLiteral = new BooleanLiteralExpr(Boolean.parseBoolean(ctx.BOOL_LITERAL().getText()));
            booleanLiteral.setTokenRange(createTokenRange(ctx));
            return booleanLiteral;
        } else if (ctx.NULL_LITERAL() != null) {
            NullLiteralExpr nullLiteral = new NullLiteralExpr();
            nullLiteral.setTokenRange(createTokenRange(ctx));
            return nullLiteral;
        } else if (ctx.CHAR_LITERAL() != null) {
            String text = ctx.CHAR_LITERAL().getText();
            char value = text.charAt(1); // Simple case, more complex handling needed for escape sequences
            CharLiteralExpr charLiteral = new CharLiteralExpr(value);
            charLiteral.setTokenRange(createTokenRange(ctx));
            return charLiteral;
        }
        
        // Handle MVEL-specific literals - create proper AST nodes like mvel.jj does
        if (ctx.BigDecimalLiteral() != null) {
            String text = ctx.BigDecimalLiteral().getText();
            // Create BigDecimalLiteralExpr node (transformation happens in MVELToJavaRewriter)
            return new BigDecimalLiteralExpr(createTokenRange(ctx), text);
        } else if (ctx.BigIntegerLiteral() != null) {
            String text = ctx.BigIntegerLiteral().getText();
            // Create BigIntegerLiteralExpr node (transformation happens in MVELToJavaRewriter)
            return new BigIntegerLiteralExpr(createTokenRange(ctx), text);
        }
        
        throw new IllegalArgumentException("Unknown literal type: " + ctx.getText());
    }

    @Override
    public Node visitClassOrInterfaceType(Mvel3Parser.ClassOrInterfaceTypeContext ctx) {
        // Grammar: (identifier typeArguments? '.')* typeIdentifier typeArguments?
        
        ClassOrInterfaceType type = null;
        
        // Handle the optional qualified prefix (identifier typeArguments? '.')*
        if (ctx.identifier() != null && !ctx.identifier().isEmpty()) {
            for (int i = 0; i < ctx.identifier().size(); i++) {
                String name = ctx.identifier(i).getText();
                ClassOrInterfaceType newType = new ClassOrInterfaceType(type, name);
                newType.setTokenRange(createTokenRange(ctx));
                type = newType;
                // TODO: Handle typeArguments if present
            }
        }
        
        // Handle the required typeIdentifier at the end
        if (ctx.typeIdentifier() != null) {
            String typeName = ctx.typeIdentifier().getText();
            ClassOrInterfaceType newType = new ClassOrInterfaceType(type, typeName);
            newType.setTokenRange(createTokenRange(ctx));
            type = newType;
            // TODO: Handle final typeArguments if present
        } else {
            throw new IllegalArgumentException("Missing typeIdentifier in ClassOrInterfaceType: " + ctx.getText());
        }
        
        return type;
    }

    @Override
    public Node visitTypeType(Mvel3Parser.TypeTypeContext ctx) {
        // Handle different type possibilities
        if (ctx.classOrInterfaceType() != null) {
            return visit(ctx.classOrInterfaceType());
        } else if (ctx.primitiveType() != null) {
            return visit(ctx.primitiveType());
        }
        
        // Fall back to default behavior
        return visitChildren(ctx);
    }

    @Override
    public Node visitSquareBracketExpression(Mvel3Parser.SquareBracketExpressionContext ctx) {
        // Handle array/list access: expression[index]
        Expression array = (Expression) visit(ctx.expression(0));
        Expression index = (Expression) visit(ctx.expression(1));
        
        // Convert to method call .get(index) for List access
        // In MVEL, array access is converted to appropriate method calls
        MethodCallExpr methodCall = new MethodCallExpr(array, "get");
        methodCall.setTokenRange(createTokenRange(ctx));
        methodCall.addArgument(index);
        return methodCall;
    }

    @Override
    public Node visitBlock(Mvel3Parser.BlockContext ctx) {
        BlockStmt blockStmt = new BlockStmt();
        blockStmt.setTokenRange(createTokenRange(ctx));
        NodeList<Statement> statements = new NodeList<>();
        
        if (ctx.blockStatement() != null) {
            for (Mvel3Parser.BlockStatementContext blockStatementCtx : ctx.blockStatement()) {
                Node node = visit(blockStatementCtx);
                if (node instanceof Statement) {
                    statements.add((Statement) node);
                }
            }
        }
        
        blockStmt.setStatements(statements);
        return blockStmt;
    }

    @Override
    public Node visitBlockStatement(Mvel3Parser.BlockStatementContext ctx) {
        if (ctx.localVariableDeclaration() != null) {
            // Handle local variable declaration
            VariableDeclarationExpr varDecl = (VariableDeclarationExpr) visit(ctx.localVariableDeclaration());
            ExpressionStmt exprStmt = new ExpressionStmt(varDecl);
            exprStmt.setTokenRange(createTokenRange(ctx));
            return exprStmt;
        } else if (ctx.statement() != null) {
            return visit(ctx.statement());
        } else if (ctx.localTypeDeclaration() != null) {
            // TODO: Handle local type declarations if needed
            throw new UnsupportedOperationException("Local type declarations not yet implemented");
        }
        return null;
    }

    @Override
    public Node visitStatement(Mvel3Parser.StatementContext ctx) {
        // Handle modify statement
        if (ctx.modifyStatement() != null) {
            return visit(ctx.modifyStatement());
        } else if (ctx.statementExpression != null) {
            // Handle expression statement: expression ';'
            Expression expr = (Expression) visit(ctx.statementExpression);
            ExpressionStmt exprStmt = new ExpressionStmt(expr);
            exprStmt.setTokenRange(createTokenRange(ctx));
            return exprStmt;
        } else if (ctx.blockLabel != null) {
            // Handle block statement
            return visit(ctx.blockLabel);
        }
        // TODO: Handle other statement types as needed
        // For now, fall back to default behavior
        return visitChildren(ctx);
    }
    
    @Override
    public Node visitModifyStatement(Mvel3Parser.ModifyStatementContext ctx) {
        // modify ( identifier ) { statement* }
        String targetName = ctx.identifier().getText();
        NameExpr target = new NameExpr(targetName);
        target.setTokenRange(createTokenRange(ctx));

        // Create a NodeList for the statements  
        NodeList<Statement> statements = new NodeList<>();
        
        // Process each statement in the modify block
        // Keep assignments as simple names - MVELToJavaRewriter will add the target prefix
        for (Mvel3Parser.StatementContext stmtCtx : ctx.statement()) {
            Statement stmt = (Statement) visit(stmtCtx);
            statements.add(stmt);
        }
        
        // Create and return a ModifyStatement with proper TokenRange
        return new ModifyStatement(createTokenRange(ctx), target, statements);
    }

    @Override
    public Node visitLocalVariableDeclaration(Mvel3Parser.LocalVariableDeclarationContext ctx) {
        // Handle both: var x = expression; and Type name = expression;
        
        Type varType;
        String varName;
        
        if (ctx.VAR() != null) {
            // Handle: var x = expression;
            varType = new VarType();
            varType.setTokenRange(createTokenRange(ctx));
            varName = ctx.identifier().getText();
        } else if (ctx.typeType() != null && ctx.variableDeclarators() != null) {
            // Handle: Type name = expression;
            varType = (Type) visit(ctx.typeType());
            // Get the first variable declarator name
            Mvel3Parser.VariableDeclaratorContext firstDeclarator = ctx.variableDeclarators().variableDeclarator(0);
            varName = firstDeclarator.variableDeclaratorId().identifier().getText();
        } else {
            throw new IllegalArgumentException("Unsupported local variable declaration: " + ctx.getText());
        }
        
        // Create variable declarator
        VariableDeclarator varDeclarator = new VariableDeclarator(varType, varName);
        varDeclarator.setTokenRange(createTokenRange(ctx));
        
        // Check if there's an initializer
        if (ctx.ASSIGN() != null && ctx.expression() != null) {
            Expression initializer = (Expression) visit(ctx.expression());
            varDeclarator.setInitializer(initializer);
        } else if (ctx.variableDeclarators() != null) {
            // Handle initializer from variableDeclarators
            Mvel3Parser.VariableDeclaratorContext firstDeclarator = ctx.variableDeclarators().variableDeclarator(0);
            if (firstDeclarator.variableInitializer() != null) {
                Expression initializer = (Expression) visit(firstDeclarator.variableInitializer().expression());
                varDeclarator.setInitializer(initializer);
            }
        }
        
        // Create the variable declaration expression
        VariableDeclarationExpr varDecl = new VariableDeclarationExpr(varDeclarator);
        varDecl.setTokenRange(createTokenRange(ctx));
        return varDecl;
    }

    @Override
    public Node visitMethodCall(Mvel3Parser.MethodCallContext ctx) {
        String methodName = ctx.identifier().getText();
        NodeList<Expression> args = parseArguments(ctx.arguments());
        
        // For method calls in member reference, we need the scope from the parent context
        // This will be handled by visitMemberReferenceExpression
        MethodCallExpr methodCall = new MethodCallExpr(null, methodName);
        methodCall.setTokenRange(createTokenRange(ctx));
        methodCall.setArguments(args);
        return methodCall;
    }

    private NodeList<Expression> parseArguments(Mvel3Parser.ArgumentsContext ctx) {
        NodeList<Expression> args = new NodeList<>();
        if (ctx.expressionList() != null) {
            // Parse each expression in the argument list
            for (Mvel3Parser.ExpressionContext exprCtx : ctx.expressionList().expression()) {
                Expression arg = (Expression) visit(exprCtx);
                args.add(arg);
            }
        }
        return args;
    }
}