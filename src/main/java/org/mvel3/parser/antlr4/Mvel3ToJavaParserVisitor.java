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

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.mvel3.parser.antlr4.mveltojavaparser.AnnotationTypeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ArgumentsConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ArrayConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.BlockConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.CompilationUnitConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ConstructorConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.CreatedNameConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.EnumConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.FieldConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ImportConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.InterfaceConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.MethodConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ParametersConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.RecordConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.StatementConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.SwitchConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TypeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.VariableConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.BinaryOperatorExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.CastExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.InlineCastExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.InstanceOfOperatorExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.LambdaExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.ListCreationLiteralConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.LiteralConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.MapCreationLiteralConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.MethodReferenceExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.NullSafeExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.ObjectCreationExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.PostIncrementDecrementOperatorExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.SquareBracketExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.TernaryExpressionConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.expressions.UnaryOperatorExpressionConverter;
import org.mvel3.parser.ast.expr.BigDecimalLiteralExpr;
import org.mvel3.parser.ast.expr.BigIntegerLiteralExpr;
import org.mvel3.parser.ast.expr.DrlNameExpr;
import org.mvel3.parser.ast.expr.TemporalChunkExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralChunkExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralExpr;
import org.mvel3.parser.ast.expr.WithStatement;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Visitor that converts ANTLR4 parse tree to JavaParser AST nodes.
 * This implementation can return various types of nodes (Expression, Type, etc.).
 */
public class Mvel3ToJavaParserVisitor extends Mvel3ParserBaseVisitor<Node> {

    // Associate antlr tokenId with a JavaParser node for identifier, so it can be used for code completion.
    protected final Map<Integer, Node> tokenIdJPNodeMap = new HashMap<>();

    protected void associateAntlrTokenWithJPNode(ParserRuleContext ctx, Node jpNode) {
        // Antlr token <-> JavaParser node mapping for code completion
        ParseTree lastNode = ctx.children.get(ctx.children.size() - 1); // take the last terminal node
        if (lastNode instanceof TerminalNode terminalNode) {
            tokenIdJPNodeMap.put(terminalNode.getSymbol().getTokenIndex(), jpNode);
        }
    }

    @Override
    public Node visitCompilationUnit(Mvel3Parser.CompilationUnitContext ctx) {
        return CompilationUnitConverter.convertCompilationUnit(ctx, this);
    }
    
    @Override
    public Node visitImportDeclaration(Mvel3Parser.ImportDeclarationContext ctx) {
        return ImportConverter.convertImportDeclaration(ctx);
    }

    @Override
    public Node visitTypeDeclaration(Mvel3Parser.TypeDeclarationContext ctx) {
        return TypeConverter.convertTypeDeclaration(ctx, this);
    }
    
    @Override
    public Node visitClassDeclaration(Mvel3Parser.ClassDeclarationContext ctx) {
        return TypeConverter.convertClassDeclaration(ctx, this);
    }

    @Override
    public Node visitEnumDeclaration(Mvel3Parser.EnumDeclarationContext ctx) {
        return EnumConverter.convertEnumDeclaration(ctx, this);
    }

    @Override
    public Node visitInterfaceDeclaration(Mvel3Parser.InterfaceDeclarationContext ctx) {
        return InterfaceConverter.convertInterfaceDeclaration(ctx, this);
    }

    @Override
    public Node visitAnnotationTypeDeclaration(Mvel3Parser.AnnotationTypeDeclarationContext ctx) {
        return AnnotationTypeConverter.convertAnnotationTypeDeclaration(ctx, this);
    }

    @Override
    public Node visitRecordDeclaration(Mvel3Parser.RecordDeclarationContext ctx) {
        return RecordConverter.convertRecordDeclaration(ctx, this);
    }

    @Override
    public Node visitFieldDeclaration(Mvel3Parser.FieldDeclarationContext ctx) {
        return FieldConverter.convertFieldDeclaration(ctx, this);
    }

    @Override
    public Node visitConstructorDeclaration(Mvel3Parser.ConstructorDeclarationContext ctx) {
        return ConstructorConverter.convertConstructorDeclaration(ctx, this);
    }

    @Override
    public Node visitGenericMethodDeclaration(Mvel3Parser.GenericMethodDeclarationContext ctx) {
        return MethodConverter.convertGenericMethodDeclaration(ctx, this);
    }

    @Override
    public Node visitGenericConstructorDeclaration(Mvel3Parser.GenericConstructorDeclarationContext ctx) {
        return ConstructorConverter.convertGenericConstructorDeclaration(ctx, this);
    }

    @Override
    public Node visitMethodDeclaration(Mvel3Parser.MethodDeclarationContext ctx) {
        return MethodConverter.convertMethodDeclaration(ctx, this);
    }

    @Override
    public Node visitMethodCallExpression(Mvel3Parser.MethodCallExpressionContext ctx) {
        return MethodConverter.convertMethodCallExpression(ctx, this);
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
        return BinaryOperatorExpressionConverter.convertBinaryOperatorExpression(ctx, this);
    }

    @Override
    public Node visitTernaryExpression(Mvel3Parser.TernaryExpressionContext ctx) {
        return TernaryExpressionConverter.convertTernaryExpression(ctx, this);
    }

    @Override
    public Node visitInstanceOfOperatorExpression(Mvel3Parser.InstanceOfOperatorExpressionContext ctx) {
        return InstanceOfOperatorExpressionConverter.convertInstanceOfOperatorExpression(ctx, this);
    }

    @Override
    public Node visitExpressionSwitch(Mvel3Parser.ExpressionSwitchContext ctx) {
        return SwitchConverter.convertSwitchExpression(ctx.switchExpression(), this);
    }

    @Override
    public Node visitSwitchExpression(Mvel3Parser.SwitchExpressionContext ctx) {
        return SwitchConverter.convertSwitchExpression(ctx, this);
    }

    @Override
    public Node visitMethodReferenceExpression(Mvel3Parser.MethodReferenceExpressionContext ctx) {
        return MethodReferenceExpressionConverter.convertMethodReferenceExpression(ctx, this);
    }

    @Override
    public Node visitMemberReferenceExpression(Mvel3Parser.MemberReferenceExpressionContext ctx) {
        // Handle member reference like "java.math.MathContext.DECIMAL128"
        Expression scope = (Expression) visit(ctx.expression());
        
        if (ctx.identifier() != null) {
            // Simple field access: expression.identifier
            String fieldName = ctx.identifier().getText();
            FieldAccessExpr fieldAccess = new FieldAccessExpr(scope, fieldName);
            fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            associateAntlrTokenWithJPNode(ctx.identifier(), fieldAccess);
            return fieldAccess;
        } else if (ctx.methodCall() != null) {
            // Method call: expression.methodCall()
            String methodName = ctx.methodCall().identifier().getText();
            NodeList<Expression> args = ArgumentsConverter.convertArguments(ctx.methodCall().arguments(), this);
            
            MethodCallExpr methodCall = new MethodCallExpr(scope, methodName);
            methodCall.setArguments(args);
            methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return methodCall;
        } else if (ctx.THIS() != null) {
            // expression.this
            FieldAccessExpr fieldAccess = new FieldAccessExpr(scope, "this");
            fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return fieldAccess;
        } else if (ctx.SUPER() != null && ctx.superSuffix() != null) {
            // expression.super(args) or expression.super.method(args) or expression.super.field
            Mvel3Parser.SuperSuffixContext suffixCtx = ctx.superSuffix();

            // Build SuperExpr with the scope as type name (e.g., Outer.super)
            // The scope expression should be a name (e.g., NameExpr("Outer"))
            Name typeName = new Name(scope.toString());
            com.github.javaparser.ast.expr.SuperExpr superExpr = new com.github.javaparser.ast.expr.SuperExpr(typeName);

            if (suffixCtx.arguments() != null && suffixCtx.identifier() == null) {
                // expression.super(args) — super constructor invocation
                // Represented as MethodCallExpr with super as scope
                NodeList<Expression> args = ArgumentsConverter.convertArguments(suffixCtx.arguments(), this);
                MethodCallExpr methodCall = new MethodCallExpr(superExpr, "super");
                methodCall.setArguments(args);
                methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                return methodCall;
            } else if (suffixCtx.identifier() != null) {
                String memberName = suffixCtx.identifier().getText();
                if (suffixCtx.arguments() != null) {
                    // expression.super.method(args)
                    NodeList<Expression> args = ArgumentsConverter.convertArguments(suffixCtx.arguments(), this);
                    MethodCallExpr methodCall = new MethodCallExpr(superExpr, memberName);
                    methodCall.setArguments(args);
                    // Handle type arguments if present
                    if (suffixCtx.typeArguments() != null) {
                        methodCall.setTypeArguments(ArgumentsConverter.convertTypeArguments(suffixCtx.typeArguments(), this));
                    }
                    methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                    return methodCall;
                } else {
                    // expression.super.field
                    FieldAccessExpr fieldAccess = new FieldAccessExpr(superExpr, memberName);
                    fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                    return fieldAccess;
                }
            }
        } else if (ctx.NEW() != null && ctx.innerCreator() != null) {
            // expression.new InnerClass(args) [classBody]
            Mvel3Parser.InnerCreatorContext innerCtx = ctx.innerCreator();
            String className = innerCtx.identifier().getText();
            ClassOrInterfaceType type = new ClassOrInterfaceType(null, className);

            // Handle type arguments if present (e.g., expr.new Inner<String>())
            if (innerCtx.nonWildcardTypeArgumentsOrDiamond() != null) {
                Mvel3Parser.NonWildcardTypeArgumentsOrDiamondContext diamondCtx = innerCtx.nonWildcardTypeArgumentsOrDiamond();
                if (diamondCtx.nonWildcardTypeArguments() != null) {
                    type.setTypeArguments(parseNonWildcardTypeArguments(diamondCtx.nonWildcardTypeArguments()));
                } else {
                    // Diamond operator <>
                    type.setTypeArguments(new NodeList<>());
                }
            }

            // Parse constructor arguments
            NodeList<Expression> arguments = new NodeList<>();
            if (innerCtx.classCreatorRest().arguments() != null &&
                innerCtx.classCreatorRest().arguments().expressionList() != null) {
                for (Mvel3Parser.ExpressionContext exprCtx : innerCtx.classCreatorRest().arguments().expressionList().expression()) {
                    arguments.add((Expression) visit(exprCtx));
                }
            }

            // Handle anonymous class body if present
            NodeList<BodyDeclaration<?>> anonymousClassBody = null;
            if (innerCtx.classCreatorRest().classBody() != null) {
                anonymousClassBody = TypeConverter.convertAnonymousClassBody(innerCtx.classCreatorRest().classBody(), this);
            }

            ObjectCreationExpr objectCreation = new ObjectCreationExpr(scope, type, null, arguments, anonymousClassBody);
            objectCreation.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return objectCreation;
        } else if (ctx.explicitGenericInvocation() != null) {
            // expression.<Type>method(args) — explicit generic invocation
            Mvel3Parser.ExplicitGenericInvocationContext egiCtx = ctx.explicitGenericInvocation();

            // Parse type arguments from nonWildcardTypeArguments: '<' typeList '>'
            NodeList<Type> typeArgs = parseNonWildcardTypeArguments(egiCtx.nonWildcardTypeArguments());

            Mvel3Parser.ExplicitGenericInvocationSuffixContext suffixCtx = egiCtx.explicitGenericInvocationSuffix();
            if (suffixCtx.identifier() != null) {
                // <Type>method(args)
                String methodName = suffixCtx.identifier().getText();
                NodeList<Expression> args = ArgumentsConverter.convertArguments(suffixCtx.arguments(), this);
                MethodCallExpr methodCall = new MethodCallExpr(scope, typeArgs, methodName, args);
                methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                return methodCall;
            } else if (suffixCtx.SUPER() != null) {
                // expression.<Type>super(...) or expression.<Type>super.method(...)
                Mvel3Parser.SuperSuffixContext superSuffix = suffixCtx.superSuffix();
                Name typeName = new Name(scope.toString());
                com.github.javaparser.ast.expr.SuperExpr superExpr = new com.github.javaparser.ast.expr.SuperExpr(typeName);

                if (superSuffix.arguments() != null && superSuffix.identifier() == null) {
                    // expression.<Type>super(args) — generic super constructor call
                    NodeList<Expression> args = ArgumentsConverter.convertArguments(superSuffix.arguments(), this);
                    MethodCallExpr methodCall = new MethodCallExpr(superExpr, typeArgs, "super", args);
                    methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                    return methodCall;
                } else if (superSuffix.identifier() != null) {
                    // expression.<Type>super.method(args)
                    String memberName = superSuffix.identifier().getText();
                    NodeList<Expression> args = superSuffix.arguments() != null
                            ? ArgumentsConverter.convertArguments(superSuffix.arguments(), this) : new NodeList<>();
                    MethodCallExpr methodCall = new MethodCallExpr(superExpr, typeArgs, memberName, args);
                    methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                    return methodCall;
                }
            }
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
            // Always use DrlNameExpr for identifiers to match JavaCC behavior
            // backReferencesCount defaults to 0 for normal identifiers
            DrlNameExpr nameExpr = new DrlNameExpr(ctx.identifier().getText());
            nameExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            associateAntlrTokenWithJPNode(ctx.identifier(), nameExpr);
            return nameExpr;
        } else if (ctx.LPAREN() != null && ctx.expression() != null && ctx.RPAREN() != null) {
            // Parenthesized expression
            EnclosedExpr enclosedExpr = new EnclosedExpr((Expression) visit(ctx.expression()));
            enclosedExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return enclosedExpr;
        } else if (ctx.THIS() != null) {
            ThisExpr thisExpr = new ThisExpr();
            thisExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return thisExpr;
        } else if (ctx.SUPER() != null) {
            // super as primary expression
            return new com.github.javaparser.ast.expr.SuperExpr();
        } else if (ctx.typeTypeOrVoid() != null && ctx.CLASS() != null) {
            // Class literal: typeTypeOrVoid '.' CLASS  (e.g. String.class, int.class, void.class)
            Type type;
            if (ctx.typeTypeOrVoid().VOID() != null) {
                type = new VoidType();
            } else {
                type = (Type) visit(ctx.typeTypeOrVoid().typeType());
            }
            ClassExpr classExpr = new ClassExpr(type);
            classExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return classExpr;
        }

        if (ctx.nonWildcardTypeArguments() != null) {
            // nonWildcardTypeArguments (explicitGenericInvocationSuffix | THIS arguments)
            NodeList<Type> typeArgs = parseNonWildcardTypeArguments(ctx.nonWildcardTypeArguments());

            if (ctx.explicitGenericInvocationSuffix() != null) {
                Mvel3Parser.ExplicitGenericInvocationSuffixContext suffixCtx = ctx.explicitGenericInvocationSuffix();
                if (suffixCtx.identifier() != null) {
                    // <Type>method(args) — generic method call without scope
                    String methodName = suffixCtx.identifier().getText();
                    NodeList<Expression> args = ArgumentsConverter.convertArguments(suffixCtx.arguments(), this);
                    MethodCallExpr methodCall = new MethodCallExpr(null, typeArgs, methodName, args);
                    methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                    return methodCall;
                }
                // <Type>super(...) — handled as explicit constructor invocation
                // Falls through to visitChildren for now
            } else if (ctx.THIS() != null && ctx.arguments() != null) {
                // <Type>this(args) — explicit constructor invocation with type arguments
                NodeList<Expression> args = ArgumentsConverter.convertArguments(ctx.arguments(), this);
                MethodCallExpr thisCall = new MethodCallExpr(null, typeArgs, "this", args);
                thisCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                return thisCall;
            }
        }

        // Handle other primary cases that might be needed
        return visitChildren(ctx);
    }

    @Override
    public Node visitInlineCastExpression(Mvel3Parser.InlineCastExpressionContext ctx) {
        return InlineCastExpressionConverter.convertInlineCastExpression(ctx, this);
    }

    @Override
    public Node visitListCreationLiteralExpression(Mvel3Parser.ListCreationLiteralExpressionContext ctx) {
        // Visit the listCreationLiteral rule
        return visit(ctx.listCreationLiteral());
    }

    @Override
    public Node visitListCreationLiteral(Mvel3Parser.ListCreationLiteralContext ctx) {
        return ListCreationLiteralConverter.convertListCreationLiteral(ctx, this);
    }

    @Override
    public Node visitMapCreationLiteralExpression(Mvel3Parser.MapCreationLiteralExpressionContext ctx) {
        // Visit the mapCreationLiteral rule
        return visit(ctx.mapCreationLiteral());
    }

    @Override
    public Node visitMapCreationLiteral(Mvel3Parser.MapCreationLiteralContext ctx) {
        return MapCreationLiteralConverter.convertMapCreationLiteral(ctx, this);
    }

    @Override
    public Node visitNullSafeExpression(Mvel3Parser.NullSafeExpressionContext ctx) {
        return NullSafeExpressionConverter.convertNullSafeExpression(ctx, this);
    }

    @Override
    public Node visitCastExpression(Mvel3Parser.CastExpressionContext ctx) {
        return CastExpressionConverter.convertCastExpression(ctx, this);
    }

    @Override
    public Node visitLambdaExpression(Mvel3Parser.LambdaExpressionContext ctx) {
        return LambdaExpressionConverter.convertLambdaExpression(ctx, this);
    }

    @Override
    public Node visitLiteral(Mvel3Parser.LiteralContext ctx) {
        return LiteralConverter.convertLiteral(ctx);
    }

    @Override
    public Node visitClassOrInterfaceType(Mvel3Parser.ClassOrInterfaceTypeContext ctx) {
        return TypeConverter.convertClassOrInterfaceType(ctx, this);
    }

    @Override
    public Node visitTypeType(Mvel3Parser.TypeTypeContext ctx) {
        return TypeConverter.convertTypeType(ctx, this);
    }

    @Override
    public Node visitPrimitiveType(Mvel3Parser.PrimitiveTypeContext ctx) {
        return TypeConverter.convertPrimitiveType(ctx);
    }

    @Override
    public Node visitSquareBracketExpression(Mvel3Parser.SquareBracketExpressionContext ctx) {
        return SquareBracketExpressionConverter.convertSquareBracketExpression(ctx, this);
    }

    @Override
    public Node visitBlock(Mvel3Parser.BlockContext ctx) {
        return BlockConverter.convertBlock(ctx, this);
    }

    @Override
    public Node visitBlockStatement(Mvel3Parser.BlockStatementContext ctx) {
        return BlockConverter.convertBlockStatement(ctx, this);
    }

    @Override
    public Node visitStatement(Mvel3Parser.StatementContext ctx) {
        return StatementConverter.convertStatement(ctx, this);
    }
    
    @Override
    public Node visitFormalParameter(Mvel3Parser.FormalParameterContext ctx) {
        return ParametersConverter.convertFormalParameter(ctx, this);
    }

    @Override
    public Node visitLastFormalParameter(Mvel3Parser.LastFormalParameterContext ctx) {
        return  ParametersConverter.convertLastFormalParameter(ctx, this);
    }

    @Override
    public Node visitModifyStatement(Mvel3Parser.ModifyStatementContext ctx) {
        return StatementConverter.convertModifyStatement(ctx, this);
    }

    @Override
    public Node visitWithStatement(Mvel3Parser.WithStatementContext ctx) {
        return StatementConverter.convertWithStatement(ctx, this);
    }

    @Override
    public Node visitLocalVariableDeclaration(Mvel3Parser.LocalVariableDeclarationContext ctx) {
        return VariableConverter.convertLocalVariableDeclaration(ctx, this);
    }

    @Override
    public Node visitVariableInitializer(Mvel3Parser.VariableInitializerContext ctx) {
        return VariableConverter.convertVariableInitializer(ctx, this);
    }

    @Override
    public Node visitArrayInitializer(Mvel3Parser.ArrayInitializerContext ctx) {
        return ArrayConverter.convertArrayInitializer(ctx, this);
    }

    @Override
    public Node visitObjectCreationExpression(Mvel3Parser.ObjectCreationExpressionContext ctx) {
        return ObjectCreationExpressionConverter.convertObjectCreationExpression(ctx, this);
    }

    @Override
    public Node visitCreator(Mvel3Parser.CreatorContext ctx) {
        return ObjectCreationExpressionConverter.convertCreator(ctx, this);
    }

    @Override
    public Node visitCreatedName(Mvel3Parser.CreatedNameContext ctx) {
        return CreatedNameConverter.convertCreatedName(ctx, this);
    }

    @Override
    public Node visitUnaryOperatorExpression(Mvel3Parser.UnaryOperatorExpressionContext ctx) {
        return UnaryOperatorExpressionConverter.convertUnaryOperatorExpression(ctx, this);
    }

    @Override
    public Node visitPostIncrementDecrementOperatorExpression(Mvel3Parser.PostIncrementDecrementOperatorExpressionContext ctx) {
        return PostIncrementDecrementOperatorExpressionConverter.convertPostIncrementDecrementOperatorExpression(ctx, this);
    }

    @Override
    public Node visitClassType(Mvel3Parser.ClassTypeContext ctx) {
        return TypeConverter.convertClassType(ctx, this);
    }

    @Override
    public Node visitTypeArguments(Mvel3Parser.TypeArgumentsContext ctx) {
        // This is handled by visitClassOrInterfaceType
        // Just return the result of visiting the type arguments
        return visitChildren(ctx);
    }

    @Override
    public Node visitTypeArgument(Mvel3Parser.TypeArgumentContext ctx) {
        return ArgumentsConverter.convertTypeArgument(ctx, this);
    }

    private NodeList<Type> parseNonWildcardTypeArguments(Mvel3Parser.NonWildcardTypeArgumentsContext ctx) {
        NodeList<Type> typeArgs = new NodeList<>();
        if (ctx != null && ctx.typeList() != null) {
            for (Mvel3Parser.TypeTypeContext typeCtx : ctx.typeList().typeType()) {
                typeArgs.add((Type) visit(typeCtx));
            }
        }
        return typeArgs;
    }
}
