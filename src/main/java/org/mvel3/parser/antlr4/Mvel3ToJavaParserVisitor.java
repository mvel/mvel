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
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ArrayType;
import org.mvel3.parser.ast.expr.ModifyStatement;
import org.mvel3.parser.ast.expr.BigDecimalLiteralExpr;
import org.mvel3.parser.ast.expr.BigIntegerLiteralExpr;
import org.mvel3.parser.antlr4.Mvel3Parser.ExpressionContext;

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
        AssignExpr.Operator assignOp = getAssignOperator(operatorText);
        if (assignOp != null) {
            return new AssignExpr(createTokenRange(ctx), left, right, assignOp);
        }
        
        // Handle other binary operators
        BinaryExpr.Operator operator = getBinaryExprOperator(operatorText);
        return new BinaryExpr(createTokenRange(ctx), left, right, operator);
    }
    
    /**
     * Map operator text to AssignExpr.Operator, return null if not an assignment operator
     */
    private AssignExpr.Operator getAssignOperator(String operatorText) {
        switch (operatorText) {
            case "=": return AssignExpr.Operator.ASSIGN;
            case "+=": return AssignExpr.Operator.PLUS;
            case "-=": return AssignExpr.Operator.MINUS;
            case "*=": return AssignExpr.Operator.MULTIPLY;
            case "/=": return AssignExpr.Operator.DIVIDE;
            case "&=": return AssignExpr.Operator.BINARY_AND;
            case "|=": return AssignExpr.Operator.BINARY_OR;
            case "^=": return AssignExpr.Operator.XOR;
            case "%=": return AssignExpr.Operator.REMAINDER;
            case "<<=": return AssignExpr.Operator.LEFT_SHIFT;
            case ">>=": return AssignExpr.Operator.SIGNED_RIGHT_SHIFT;
            case ">>>=": return AssignExpr.Operator.UNSIGNED_RIGHT_SHIFT;
            default: return null;
        }
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
            String text = ctx.DECIMAL_LITERAL().getText();
            // Check if it's a long literal (ends with 'L' or 'l')
            if (text.endsWith("L") || text.endsWith("l")) {
                LongLiteralExpr longLiteral = new LongLiteralExpr(text);
                longLiteral.setTokenRange(createTokenRange(ctx));
                return longLiteral;
            } else {
                IntegerLiteralExpr integerLiteral = new IntegerLiteralExpr(text);
                integerLiteral.setTokenRange(createTokenRange(ctx));
                return integerLiteral;
            }
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
        } else if (ctx.TEXT_BLOCK() != null) {
            String rawText = ctx.TEXT_BLOCK().getText();
            // Extract content between triple quotes: """content"""
            String content = rawText.substring(3, rawText.length() - 3);
            TextBlockLiteralExpr textBlockLiteral = new TextBlockLiteralExpr(content);
            textBlockLiteral.setTokenRange(createTokenRange(ctx));
            return textBlockLiteral;
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
        // For now, we skip typeArguments in the prefix (rare case)
        if (ctx.identifier() != null && !ctx.identifier().isEmpty()) {
            for (int i = 0; i < ctx.identifier().size(); i++) {
                String name = ctx.identifier(i).getText();
                ClassOrInterfaceType newType = new ClassOrInterfaceType(type, name);
                newType.setTokenRange(createTokenRange(ctx));
                type = newType;
                // TODO: Handle typeArguments for intermediate identifiers if needed (rare case)
            }
        }
        
        // Handle the required typeIdentifier at the end
        if (ctx.typeIdentifier() != null) {
            String typeName = ctx.typeIdentifier().getText();
            ClassOrInterfaceType newType = new ClassOrInterfaceType(type, typeName);
            newType.setTokenRange(createTokenRange(ctx));
            type = newType;
            
            // Handle final typeArguments if present (the common case for generics like List<Foo>)
            if (ctx.typeArguments() != null && !ctx.typeArguments().isEmpty()) {
                // Get the LAST typeArguments (which should be for the typeIdentifier)
                Mvel3Parser.TypeArgumentsContext lastTypeArgs = ctx.typeArguments(ctx.typeArguments().size() - 1);
                NodeList<com.github.javaparser.ast.type.Type> typeArgs = new NodeList<>();
                
                for (Mvel3Parser.TypeArgumentContext typeArgCtx : lastTypeArgs.typeArgument()) {
                    com.github.javaparser.ast.type.Type typeArg = (com.github.javaparser.ast.type.Type) visit(typeArgCtx);
                    if (typeArg != null) {
                        typeArgs.add(typeArg);
                    }
                }
                
                if (!typeArgs.isEmpty()) {
                    newType.setTypeArguments(typeArgs);
                }
            }
        } else {
            throw new IllegalArgumentException("Missing typeIdentifier in ClassOrInterfaceType: " + ctx.getText());
        }
        
        return type;
    }

    @Override
    public Node visitTypeType(Mvel3Parser.TypeTypeContext ctx) {
        Type baseType = null;
        
        // Handle different type possibilities
        if (ctx.classOrInterfaceType() != null) {
            baseType = (Type) visit(ctx.classOrInterfaceType());
        } else if (ctx.primitiveType() != null) {
            baseType = (Type) visit(ctx.primitiveType());
        }
        
        if (baseType == null) {
            // Fall back to default behavior
            return visitChildren(ctx);
        }
        
        // Handle array dimensions: (annotation* '[' ']')*
        // Count the number of '[' ']' pairs to determine array dimensions
        int arrayDimensions = 0;
        if (ctx.children != null) {
            for (ParseTree child : ctx.children) {
                if (child instanceof TerminalNode && "[".equals(child.getText())) {
                    arrayDimensions++;
                }
            }
        }
        
        // Wrap base type in ArrayType for each dimension
        Type resultType = baseType;
        for (int i = 0; i < arrayDimensions; i++) {
            resultType = new ArrayType(resultType);
        }
        
        if (resultType instanceof ArrayType) {
            resultType.setTokenRange(createTokenRange(ctx));
        }
        
        return resultType;
    }

    @Override
    public Node visitPrimitiveType(Mvel3Parser.PrimitiveTypeContext ctx) {
        // Map ANTLR primitive types to JavaParser PrimitiveType
        String typeName = ctx.getText();
        PrimitiveType.Primitive primitive;
        
        switch (typeName) {
            case "boolean":
                primitive = PrimitiveType.Primitive.BOOLEAN;
                break;
            case "byte":
                primitive = PrimitiveType.Primitive.BYTE;
                break;
            case "short":
                primitive = PrimitiveType.Primitive.SHORT;
                break;
            case "int":
                primitive = PrimitiveType.Primitive.INT;
                break;
            case "long":
                primitive = PrimitiveType.Primitive.LONG;
                break;
            case "char":
                primitive = PrimitiveType.Primitive.CHAR;
                break;
            case "float":
                primitive = PrimitiveType.Primitive.FLOAT;
                break;
            case "double":
                primitive = PrimitiveType.Primitive.DOUBLE;
                break;
            default:
                throw new IllegalArgumentException("Unknown primitive type: " + typeName);
        }
        
        PrimitiveType primitiveType = new PrimitiveType(primitive);
        primitiveType.setTokenRange(createTokenRange(ctx));
        return primitiveType;
    }

    @Override
    public Node visitSquareBracketExpression(Mvel3Parser.SquareBracketExpressionContext ctx) {
        // Handle array/list access: expression[index]
        Expression array = (Expression) visit(ctx.expression(0)); // The array/list expression
        Expression index = (Expression) visit(ctx.expression(1)); // The index expression
        
        // Create ArrayAccessExpr like mvel.jj does
        // The transformation to .get() method calls is handled by MVELToJavaRewriter
        ArrayAccessExpr arrayAccess = new ArrayAccessExpr(array, index);
        arrayAccess.setTokenRange(createTokenRange(ctx));
        return arrayAccess;
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
        } else if (ctx.IF() != null) {
            // Handle if statement: IF parExpression statement (ELSE statement)?
            Expression condition = (Expression) visit(ctx.parExpression().expression());
            Statement thenStmt = (Statement) visit(ctx.statement(0));
            
            IfStmt ifStmt = new IfStmt(condition, thenStmt, null);
            ifStmt.setTokenRange(createTokenRange(ctx));
            
            // Handle else clause if present
            if (ctx.ELSE() != null && ctx.statement().size() > 1) {
                Statement elseStmt = (Statement) visit(ctx.statement(1));
                ifStmt.setElseStmt(elseStmt);
            }
            
            return ifStmt;
        } else if (ctx.WHILE() != null) {
            // Handle while statement: WHILE parExpression statement
            Expression condition = (Expression) visit(ctx.parExpression().expression());
            Statement body = (Statement) visit(ctx.statement(0));
            
            WhileStmt whileStmt = new WhileStmt(condition, body);
            whileStmt.setTokenRange(createTokenRange(ctx));
            return whileStmt;
        } else if (ctx.FOR() != null) {
            // Handle for statement: FOR '(' forControl ')' statement
            Statement body = (Statement) visit(ctx.statement(0));

            // Check if this is an enhanced for loop (foreach)
            if (ctx.forControl() != null && ctx.forControl().enhancedForControl() != null) {
                // Create ForEachStmt for enhanced for loops
                ForEachStmt forEachStmt = new ForEachStmt();
                forEachStmt.setBody(body);
                visitEnhancedForControlAndPopulate(ctx.forControl().enhancedForControl(), forEachStmt);
                forEachStmt.setTokenRange(createTokenRange(ctx));
                return forEachStmt;
            } else {
                // Create regular ForStmt for traditional for loops
                ForStmt forStmt = new ForStmt();
                forStmt.setBody(body);

                // Parse forControl if available
                if (ctx.forControl() != null) {
                    visitForControlAndPopulate(ctx.forControl(), forStmt);
                }

                forStmt.setTokenRange(createTokenRange(ctx));
                return forStmt;
            }
        } else if (ctx.SWITCH() != null) {
            // Handle switch statement: SWITCH parExpression '{' switchBlockStatementGroup* switchLabel* '}'
            Expression selector = (Expression) visit(ctx.parExpression().expression());
            
            SwitchStmt switchStmt = new SwitchStmt();
            switchStmt.setSelector(selector);
            switchStmt.setTokenRange(createTokenRange(ctx));
            
            NodeList<SwitchEntry> entries = new NodeList<>();
            
            // Process switchBlockStatementGroups
            if (ctx.switchBlockStatementGroup() != null) {
                for (Mvel3Parser.SwitchBlockStatementGroupContext groupCtx : ctx.switchBlockStatementGroup()) {
                    SwitchEntry entry = processSwitchBlockStatementGroup(groupCtx);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
            }
            
            // Process standalone switchLabels (if any)
            if (ctx.switchLabel() != null) {
                for (Mvel3Parser.SwitchLabelContext labelCtx : ctx.switchLabel()) {
                    SwitchEntry entry = processSwitchLabel(labelCtx);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
            }
            
            switchStmt.setEntries(entries);
            return switchStmt;
        } else if (ctx.RETURN() != null) {
            // Handle return statement: RETURN expression? ';'
            ReturnStmt returnStmt;
            // Get the first expression after RETURN (if any)
            if (ctx.expression() != null && !ctx.expression().isEmpty()) {
                Expression expr = (Expression) visit(ctx.expression(0));
                returnStmt = new ReturnStmt(expr);
            } else {
                returnStmt = new ReturnStmt();
            }
            returnStmt.setTokenRange(createTokenRange(ctx));
            return returnStmt;
        }
        // TODO: Handle other statement types as needed (TRY, THROW, BREAK, CONTINUE, etc.)
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
        
        if (ctx.VAR() != null) {
            // Handle: var x = expression;
            Type varType = new VarType();
            varType.setTokenRange(createTokenRange(ctx));
            String varName = ctx.identifier().getText();
            
            VariableDeclarator varDeclarator = new VariableDeclarator(varType, varName);
            varDeclarator.setTokenRange(createTokenRange(ctx));
            
            // Handle initializer for var declaration
            if (ctx.expression() != null) {
                Expression initializer = (Expression) visit(ctx.expression());
                varDeclarator.setInitializer(initializer);
            }
            
            VariableDeclarationExpr varDecl = new VariableDeclarationExpr(varDeclarator);
            varDecl.setTokenRange(createTokenRange(ctx));
            return varDecl;
        } else if (ctx.typeType() != null && ctx.variableDeclarators() != null) {
            // Handle: Type name = expression;
            Type varType = (Type) visit(ctx.typeType());
            
            // Create NodeList for multiple declarators (though we usually have just one)
            NodeList<VariableDeclarator> declarators = new NodeList<>();
            
            for (Mvel3Parser.VariableDeclaratorContext declaratorCtx : ctx.variableDeclarators().variableDeclarator()) {
                // Get variable name
                String varName = declaratorCtx.variableDeclaratorId().identifier().getText();
                
                // Create variable declarator
                VariableDeclarator varDeclarator = new VariableDeclarator(varType, varName);
                varDeclarator.setTokenRange(createTokenRange(declaratorCtx));
                
                // Handle initializer if present
                if (declaratorCtx.variableInitializer() != null) {
                    Expression initializer = (Expression) visit(declaratorCtx.variableInitializer());
                    varDeclarator.setInitializer(initializer);
                }
                
                declarators.add(varDeclarator);
            }
            
            // Create the variable declaration expression with all declarators
            VariableDeclarationExpr varDecl = new VariableDeclarationExpr(declarators);
            varDecl.setTokenRange(createTokenRange(ctx));
            return varDecl;
        } else {
            throw new IllegalArgumentException("Unsupported local variable declaration: " + ctx.getText());
        }
    }

    @Override
    public Node visitVariableInitializer(Mvel3Parser.VariableInitializerContext ctx) {
        if (ctx.arrayInitializer() != null) {
            return visit(ctx.arrayInitializer());
        } else if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        return null;
    }

    @Override
    public Node visitArrayInitializer(Mvel3Parser.ArrayInitializerContext ctx) {
        NodeList<Expression> values = new NodeList<>();
        
        if (ctx.variableInitializer() != null && !ctx.variableInitializer().isEmpty()) {
            for (Mvel3Parser.VariableInitializerContext initCtx : ctx.variableInitializer()) {
                Expression expr = (Expression) visit(initCtx);
                if (expr != null) {
                    values.add(expr);
                }
            }
        }
        
        ArrayInitializerExpr arrayInit = new ArrayInitializerExpr(values);
        arrayInit.setTokenRange(createTokenRange(ctx));
        return arrayInit;
    }

    @Override
    public Node visitObjectCreationExpression(Mvel3Parser.ObjectCreationExpressionContext ctx) {
        return visit(ctx.creator());
    }

    @Override
    public Node visitCreator(Mvel3Parser.CreatorContext ctx) {
        Node createdName = visit(ctx.createdName());
        
        if (ctx.arrayCreatorRest() != null) {
            // Handle array creation: new Type[] {...} or new Type[size]
            return visitArrayCreatorRest(ctx.arrayCreatorRest(), createdName);
        } else if (ctx.classCreatorRest() != null) {
            // Handle class creation: new Type(args)
            Type type = (Type) createdName;

            // Get constructor arguments
            NodeList<Expression> arguments = new NodeList<>();
            if (ctx.classCreatorRest().arguments() != null &&
                ctx.classCreatorRest().arguments().expressionList() != null) {
                for (Mvel3Parser.ExpressionContext exprCtx : ctx.classCreatorRest().arguments().expressionList().expression()) {
                    Expression arg = (Expression) visit(exprCtx);
                    arguments.add(arg);
                }
            }

            // Create ObjectCreationExpr
            ObjectCreationExpr objectCreation = new ObjectCreationExpr(null, (ClassOrInterfaceType) type, arguments);
            objectCreation.setTokenRange(createTokenRange(ctx));
            return objectCreation;
        }
        
        return createdName;
    }

    private Node visitArrayCreatorRest(Mvel3Parser.ArrayCreatorRestContext ctx, Node createdName) {
        Type elementType = (Type) createdName;
        
        if (ctx.arrayInitializer() != null) {
            // Handle: new Type[] { ... }
            ArrayInitializerExpr initializer = (ArrayInitializerExpr) visit(ctx.arrayInitializer());
            
            // Count the array dimensions from '[' ']' pairs
            int dimensions = 0;
            if (ctx.children != null) {
                for (ParseTree child : ctx.children) {
                    if (child instanceof TerminalNode && "[".equals(child.getText())) {
                        dimensions++;
                    }
                }
            }
            
            // Create ArrayCreationLevel objects for each dimension (empty for array initializer)
            NodeList<ArrayCreationLevel> levels = new NodeList<>();
            for (int i = 0; i < dimensions; i++) {
                ArrayCreationLevel level = new ArrayCreationLevel();
                level.setTokenRange(createTokenRange(ctx)); 
                levels.add(level);
            }
            
            // Create ArrayCreationExpr
            ArrayCreationExpr arrayCreation = new ArrayCreationExpr(elementType, levels, initializer);
            arrayCreation.setTokenRange(createTokenRange(ctx));
            return arrayCreation;
        } else {
            // Handle: new Type[size] or new Type[size1][size2]
            NodeList<ArrayCreationLevel> levels = new NodeList<>();
            
            if (ctx.expression() != null) {
                for (Mvel3Parser.ExpressionContext exprCtx : ctx.expression()) {
                    Expression dimExpr = (Expression) visit(exprCtx);
                    ArrayCreationLevel level = new ArrayCreationLevel(dimExpr);
                    level.setTokenRange(createTokenRange(exprCtx));
                    levels.add(level);
                }
            }
            
            // Create ArrayCreationExpr with dimensions
            ArrayCreationExpr arrayCreation = new ArrayCreationExpr(elementType, levels, null);
            arrayCreation.setTokenRange(createTokenRange(ctx));
            return arrayCreation;
        }
    }

    @Override
    public Node visitCreatedName(Mvel3Parser.CreatedNameContext ctx) {
        if (ctx.primitiveType() != null) {
            return visit(ctx.primitiveType());
        } else if (ctx.identifier() != null && !ctx.identifier().isEmpty()) {
            // Handle class/interface type creation - build qualified name with type arguments
            ClassOrInterfaceType type = null;

            // Build the qualified name from all identifiers with their type arguments
            for (int i = 0; i < ctx.identifier().size(); i++) {
                String name = ctx.identifier(i).getText();

                // Check if this identifier has type arguments or diamond operator
                NodeList<Type> typeArguments = null;
                if (i < ctx.typeArgumentsOrDiamond().size() && ctx.typeArgumentsOrDiamond(i) != null) {
                    typeArguments = handleTypeArgumentsOrDiamond(ctx.typeArgumentsOrDiamond(i));
                }

                type = new ClassOrInterfaceType(type, name);
                if (typeArguments != null) {
                    type.setTypeArguments(typeArguments);
                }
            }

            if (type != null) {
                type.setTokenRange(createTokenRange(ctx));
            }
            return type;
        }

        throw new IllegalArgumentException("Unsupported created name: " + ctx.getText());
    }

    private NodeList<Type> handleTypeArgumentsOrDiamond(Mvel3Parser.TypeArgumentsOrDiamondContext ctx) {
        if (ctx.typeArguments() != null) {
            // Handle full type arguments: <String, Integer>
            NodeList<Type> typeArgs = new NodeList<>();
            for (Mvel3Parser.TypeArgumentContext typeArgCtx : ctx.typeArguments().typeArgument()) {
                Type typeArg = (Type) visit(typeArgCtx);
                typeArgs.add(typeArg);
            }
            return typeArgs;
        } else {
            // Handle diamond operator: <> - return empty NodeList to represent diamond
            return new NodeList<>();
        }
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

    @Override
    public Node visitUnaryOperatorExpression(Mvel3Parser.UnaryOperatorExpressionContext ctx) {
        // Handle unary operators: +expr, -expr, ++expr, --expr, ~expr, !expr
        Expression operand = (Expression) visit(ctx.expression());
        String operator = ctx.prefix.getText();

        UnaryExpr.Operator unaryOp;
        switch (operator) {
            case "+": unaryOp = UnaryExpr.Operator.PLUS; break;
            case "-": unaryOp = UnaryExpr.Operator.MINUS; break;
            case "++": unaryOp = UnaryExpr.Operator.PREFIX_INCREMENT; break;
            case "--": unaryOp = UnaryExpr.Operator.PREFIX_DECREMENT; break;
            case "~": unaryOp = UnaryExpr.Operator.BITWISE_COMPLEMENT; break;
            case "!": unaryOp = UnaryExpr.Operator.LOGICAL_COMPLEMENT; break;
            default:
                throw new IllegalArgumentException("Unknown unary operator: " + operator);
        }

        UnaryExpr unaryExpr = new UnaryExpr(operand, unaryOp);
        unaryExpr.setTokenRange(createTokenRange(ctx));
        return unaryExpr;
    }

    @Override
    public Node visitPostIncrementDecrementOperatorExpression(Mvel3Parser.PostIncrementDecrementOperatorExpressionContext ctx) {
        // Handle post-increment and post-decrement: expression++ or expression--
        Expression operand = (Expression) visit(ctx.expression());
        String operator = ctx.postfix.getText();
        
        UnaryExpr.Operator unaryOp;
        if ("++".equals(operator)) {
            unaryOp = UnaryExpr.Operator.POSTFIX_INCREMENT;
        } else if ("--".equals(operator)) {
            unaryOp = UnaryExpr.Operator.POSTFIX_DECREMENT;
        } else {
            throw new IllegalArgumentException("Unknown post-increment/decrement operator: " + operator);
        }
        
        UnaryExpr unaryExpr = new UnaryExpr(operand, unaryOp);
        unaryExpr.setTokenRange(createTokenRange(ctx));
        return unaryExpr;
    }

    // Handle constant expressions by visiting the contained expression
    private Node visitConstantExpr(ExpressionContext ctx) {
        return visit(ctx);
    }

    private void visitForControlAndPopulate(Mvel3Parser.ForControlContext forControlCtx, ForStmt forStmt) {
        // FOR control can be: forInit? ';' expression? ';' forUpdate?
        // Or enhanced for: variableDeclarator ':' expression
        
        // Check if it's an enhanced for (for-each) loop
        if (forControlCtx.enhancedForControl() != null) {
            // Enhanced for: for (Type var : iterable)
            // TODO: Implement enhanced for loop parsing if needed
            return;
        }
        
        // Regular for loop: for (init; condition; update)
        NodeList<Expression> initialization = new NodeList<>();
        Expression compare = null;
        NodeList<Expression> update = new NodeList<>();
        
        // Parse forInit
        if (forControlCtx.forInit() != null) {
            // forInit can be localVariableDeclaration or expressionList
            if (forControlCtx.forInit().localVariableDeclaration() != null) {
                // Variable declaration like: int i = 0
                VariableDeclarationExpr varDecl = (VariableDeclarationExpr) visit(forControlCtx.forInit().localVariableDeclaration());
                if (varDecl != null) {
                    initialization.add(varDecl);
                }
            } else if (forControlCtx.forInit().expressionList() != null) {
                // Expression list like: i = 0, j = 1
                for (Mvel3Parser.ExpressionContext exprCtx : forControlCtx.forInit().expressionList().expression()) {
                    Expression expr = (Expression) visit(exprCtx);
                    if (expr != null) {
                        initialization.add(expr);
                    }
                }
            }
        }
        
        // Parse condition
        if (forControlCtx.expression() != null) {
            compare = (Expression) visit(forControlCtx.expression());
        }
        
        // Parse forUpdate
        if (forControlCtx.forUpdate != null) {
            for (Mvel3Parser.ExpressionContext exprCtx : forControlCtx.forUpdate.expression()) {
                Expression expr = (Expression) visit(exprCtx);
                if (expr != null) {
                    update.add(expr);
                }
            }
        }
        
        // Set the for loop components - only set non-empty lists
        if (!initialization.isEmpty()) {
            forStmt.setInitialization(initialization);
        }
        if (compare != null) {
            forStmt.setCompare(compare);
        }
        if (!update.isEmpty()) {
            forStmt.setUpdate(update);
        }
    }

    private void visitEnhancedForControlAndPopulate(Mvel3Parser.EnhancedForControlContext enhancedForCtx, ForEachStmt forEachStmt) {
        // enhancedForControl: variableModifier* (typeType | VAR) variableDeclaratorId ':' expression

        // Extract variable modifiers
        NodeList<Modifier> modifiers = new NodeList<>();
        if (enhancedForCtx.variableModifier() != null) {
            for (Mvel3Parser.VariableModifierContext modCtx : enhancedForCtx.variableModifier()) {
                // TODO: Handle variable modifiers if needed (final, etc.)
            }
        }

        // Extract type (typeType or VAR)
        Type variableType = null;
        if (enhancedForCtx.typeType() != null) {
            variableType = (Type) visit(enhancedForCtx.typeType());
        } else if (enhancedForCtx.VAR() != null) {
            // Handle var type - use VarType from JavaParser
            variableType = new VarType();
        }

        // Extract variable name from variableDeclaratorId
        String variableName = enhancedForCtx.variableDeclaratorId().identifier().getText();

        // Create VariableDeclarator
        VariableDeclarator variableDeclarator = new VariableDeclarator(variableType, variableName);

        // Create VariableDeclarationExpr like mvel.jj does
        NodeList<VariableDeclarator> variables = new NodeList<>();
        variables.add(variableDeclarator);
        VariableDeclarationExpr varDecl = new VariableDeclarationExpr(modifiers, variables);

        // Extract iterable expression
        Expression iterable = (Expression) visit(enhancedForCtx.expression());

        // Set the ForEachStmt components to match mvel.jj: ForEachStmt(range, varExpr, expr, body)
        forEachStmt.setVariable(varDecl);
        forEachStmt.setIterable(iterable);
    }

    private SwitchEntry processSwitchBlockStatementGroup(Mvel3Parser.SwitchBlockStatementGroupContext groupCtx) {
        // switchBlockStatementGroup: switchLabel+ blockStatement*
        if (groupCtx.switchLabel() == null || groupCtx.switchLabel().isEmpty()) {
            return null;
        }

        // Process the first switch label
        Mvel3Parser.SwitchLabelContext firstLabel = groupCtx.switchLabel(0);
        SwitchEntry entry = processSwitchLabel(firstLabel);
        
        if (entry != null) {
            // Process statements in this switch block
            NodeList<Statement> statements = new NodeList<>();
            if (groupCtx.blockStatement() != null) {
                for (Mvel3Parser.BlockStatementContext blockStmtCtx : groupCtx.blockStatement()) {
                    Node stmt = visit(blockStmtCtx);
                    if (stmt instanceof Statement) {
                        statements.add((Statement) stmt);
                    }
                }
            }
            entry.setStatements(statements);
        }
        
        return entry;
    }

    private SwitchEntry processSwitchLabel(Mvel3Parser.SwitchLabelContext labelCtx) {
        // switchLabel: CASE (constantExpression | enumConstantName | typeType varName=IDENTIFIER) ':'
        //            | DEFAULT ':'
        
        if (labelCtx.CASE() != null) {
            // Case label
            NodeList<Expression> labels = new NodeList<>();
            
            if (labelCtx.constantExpression != null) {
                // case constantExpression:
                Expression caseExpr = (Expression) visitConstantExpr(labelCtx.constantExpression);
                if (caseExpr != null) {
                    labels.add(caseExpr);
                }
            } else if (labelCtx.enumConstantName != null) {
                // case enumConstantName:
                String enumName = labelCtx.enumConstantName.getText();
                labels.add(new NameExpr(enumName));
            }
            // TODO: Handle typeType varName case if needed
            
            SwitchEntry entry = new SwitchEntry(labels, SwitchEntry.Type.STATEMENT_GROUP, new NodeList<>());
            entry.setTokenRange(createTokenRange(labelCtx));
            return entry;
            
        } else if (labelCtx.DEFAULT() != null) {
            // Default label
            SwitchEntry entry = new SwitchEntry(new NodeList<>(), SwitchEntry.Type.STATEMENT_GROUP, new NodeList<>());
            entry.setTokenRange(createTokenRange(labelCtx));
            return entry;
        }
        
        return null;
    }

    @Override
    public Node visitTypeArguments(Mvel3Parser.TypeArgumentsContext ctx) {
        // This is handled by visitClassOrInterfaceType
        // Just return the result of visiting the type arguments
        return visitChildren(ctx);
    }

    @Override
    public Node visitTypeArgument(Mvel3Parser.TypeArgumentContext ctx) {
        // Handle individual type argument like "Foo" in List<Foo>
        if (ctx.typeType() != null) {
            return visit(ctx.typeType());
        } else if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
            // Handle annotated wildcards - for now, just handle the basic wildcard case
            // TODO: Implement annotation handling if needed
            return new WildcardType();
        } else {
            // Handle wildcards: ? extends Type or ? super Type
            WildcardType wildcard = new WildcardType();
            if (ctx.EXTENDS() != null && ctx.typeType() != null) {
                Type extendedType = (Type) visit(ctx.typeType());
                // Cast to ReferenceType for JavaParser compatibility
                if (extendedType instanceof ClassOrInterfaceType) {
                    wildcard.setExtendedType((ClassOrInterfaceType) extendedType);
                }
            } else if (ctx.SUPER() != null && ctx.typeType() != null) {
                Type superType = (Type) visit(ctx.typeType());
                // Cast to ReferenceType for JavaParser compatibility
                if (superType instanceof ClassOrInterfaceType) {
                    wildcard.setSuperType((ClassOrInterfaceType) superType);
                }
            }
            wildcard.setTokenRange(createTokenRange(ctx));
            return wildcard;
        }
    }
}