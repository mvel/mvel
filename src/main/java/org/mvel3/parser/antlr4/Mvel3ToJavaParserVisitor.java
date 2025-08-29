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

import static org.mvel3.parser.util.AstUtils.getBinaryExprOperator;

/**
 * Visitor that converts ANTLR4 parse tree to JavaParser AST nodes.
 * This implementation can return various types of nodes (Expression, Type, etc.).
 */
public class Mvel3ToJavaParserVisitor extends Mvel3ParserBaseVisitor<Node> {

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
            return new AssignExpr(left, right, AssignExpr.Operator.ASSIGN);
        }
        
        // Handle other binary operators
        BinaryExpr.Operator operator = getBinaryExprOperator(operatorText);
        return new BinaryExpr(left, right, operator);
    }

    @Override
    public Node visitMemberReferenceExpression(Mvel3Parser.MemberReferenceExpressionContext ctx) {
        // Handle member reference like "java.math.MathContext.DECIMAL128"
        Expression scope = (Expression) visit(ctx.expression());
        
        if (ctx.identifier() != null) {
            // Simple field access: expression.identifier
            String fieldName = ctx.identifier().getText();
            return new FieldAccessExpr(scope, fieldName);
        } else if (ctx.methodCall() != null) {
            // Method call: expression.methodCall()
            String methodName = ctx.methodCall().identifier().getText();
            NodeList<Expression> args = parseArguments(ctx.methodCall().arguments());
            
            MethodCallExpr methodCall = new MethodCallExpr(scope, methodName);
            methodCall.setArguments(args);
            return methodCall;
        } else if (ctx.THIS() != null) {
            // expression.this
            return new FieldAccessExpr(scope, "this");
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
            return new NameExpr(ctx.identifier().getText());
        } else if (ctx.LPAREN() != null && ctx.expression() != null && ctx.RPAREN() != null) {
            // Parenthesized expression
            return new EnclosedExpr((Expression) visit(ctx.expression()));
        } else if (ctx.THIS() != null) {
            return new ThisExpr();
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
        
        // Check what comes after the cast
        if (ctx.identifier() != null) {
            String methodName = ctx.identifier().getText();
            if (ctx.arguments() != null) {
                // Method call with arguments
                MethodCallExpr methodCall = new MethodCallExpr(castExpr, methodName);
                // Parse arguments if they exist
                NodeList<Expression> args = parseArguments(ctx.arguments());
                methodCall.setArguments(args);
                return methodCall;
            } else {
                // Field access
                return new FieldAccessExpr(castExpr, methodName);
            }
        } else if (ctx.LBRACK() != null && ctx.expression() != null && ctx.RBRACK() != null) {
            // Array access: primary#Type#[expression]
            // Convert to method call: ((Type)primary).get(expression)
            Expression indexExpr = (Expression) visit(ctx.expression());
            MethodCallExpr methodCall = new MethodCallExpr(castExpr, "get");
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
            return new StringLiteralExpr(value);
        } else if (ctx.DECIMAL_LITERAL() != null) {
            return new IntegerLiteralExpr(ctx.DECIMAL_LITERAL().getText());
        } else if (ctx.FLOAT_LITERAL() != null) {
            return new DoubleLiteralExpr(ctx.FLOAT_LITERAL().getText());
        } else if (ctx.BOOL_LITERAL() != null) {
            return new BooleanLiteralExpr(Boolean.parseBoolean(ctx.BOOL_LITERAL().getText()));
        } else if (ctx.NULL_LITERAL() != null) {
            return new NullLiteralExpr();
        } else if (ctx.CHAR_LITERAL() != null) {
            String text = ctx.CHAR_LITERAL().getText();
            char value = text.charAt(1); // Simple case, more complex handling needed for escape sequences
            return new CharLiteralExpr(value);
        }
        
        // Handle MVEL-specific literals
        if (ctx.BigDecimalLiteral() != null) {
            String text = ctx.BigDecimalLiteral().getText();
            // Remove 'B' suffix
            text = text.substring(0, text.length() - 1);
            
            // Check if it's a decimal number
            if (text.contains(".")) {
                // Use BigDecimal constructor for decimal values: new BigDecimal("10.5")
                ObjectCreationExpr constructor = new ObjectCreationExpr();
                constructor.setType("BigDecimal");
                constructor.addArgument(new StringLiteralExpr(text));
                return constructor;
            } else {
                // Use BigDecimal.valueOf() for integer values: BigDecimal.valueOf(10)
                MethodCallExpr valueOf = new MethodCallExpr(new NameExpr("BigDecimal"), "valueOf");
                valueOf.addArgument(new IntegerLiteralExpr(text));
                return valueOf;
            }
        } else if (ctx.BigIntegerLiteral() != null) {
            String text = ctx.BigIntegerLiteral().getText();
            // Remove 'I' suffix
            text = text.substring(0, text.length() - 1);
            
            // Use BigInteger.valueOf(): BigInteger.valueOf(10)
            MethodCallExpr valueOf = new MethodCallExpr(new NameExpr("BigInteger"), "valueOf");
            valueOf.addArgument(new IntegerLiteralExpr(text));
            return valueOf;
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
                type = new ClassOrInterfaceType(type, name);
                // TODO: Handle typeArguments if present
            }
        }
        
        // Handle the required typeIdentifier at the end
        if (ctx.typeIdentifier() != null) {
            String typeName = ctx.typeIdentifier().getText();
            type = new ClassOrInterfaceType(type, typeName);
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
        methodCall.addArgument(index);
        return methodCall;
    }

    @Override
    public Node visitBlock(Mvel3Parser.BlockContext ctx) {
        BlockStmt blockStmt = new BlockStmt();
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
            return new ExpressionStmt(varDecl);
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
        if (ctx.statementExpression != null) {
            // Handle expression statement: expression ';'
            Expression expr = (Expression) visit(ctx.statementExpression);
            return new ExpressionStmt(expr);
        } else if (ctx.blockLabel != null) {
            // Handle block statement
            return visit(ctx.blockLabel);
        }
        // TODO: Handle other statement types as needed
        // For now, fall back to default behavior
        return visitChildren(ctx);
    }

    @Override
    public Node visitLocalVariableDeclaration(Mvel3Parser.LocalVariableDeclarationContext ctx) {
        // Handle: var x = expression;
        
        // Get the identifier (variable name)
        String varName = ctx.identifier().getText();
        
        // Create variable declarator with var type
        VariableDeclarator varDeclarator = new VariableDeclarator(new VarType(), varName);
        
        // Check if there's an initializer
        if (ctx.ASSIGN() != null && ctx.expression() != null) {
            Expression initializer = (Expression) visit(ctx.expression());
            varDeclarator.setInitializer(initializer);
        }
        
        // Create the variable declaration expression
        VariableDeclarationExpr varDecl = new VariableDeclarationExpr(varDeclarator);
        return varDecl;
    }

    @Override
    public Node visitMethodCall(Mvel3Parser.MethodCallContext ctx) {
        String methodName = ctx.identifier().getText();
        NodeList<Expression> args = parseArguments(ctx.arguments());
        
        // For method calls in member reference, we need the scope from the parent context
        // This will be handled by visitMemberReferenceExpression
        MethodCallExpr methodCall = new MethodCallExpr(null, methodName);
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