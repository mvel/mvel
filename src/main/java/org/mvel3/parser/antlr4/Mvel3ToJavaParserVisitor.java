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

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleDirective;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleOpensDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.ast.type.VoidType;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.mvel3.parser.antlr4.mveltojavaparser.AnnotationTypeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ArgumentsConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ArrayConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.BlockConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.EnumConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ImportConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.InterfaceConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ModifiersConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ModifiersParser;
import org.mvel3.parser.antlr4.mveltojavaparser.ParametersConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.RecordConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.StatementConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TypeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.VariableConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.VariableParser;
import org.mvel3.parser.ast.expr.BigDecimalLiteralExpr;
import org.mvel3.parser.ast.expr.BigIntegerLiteralExpr;
import org.mvel3.parser.ast.expr.DrlNameExpr;
import org.mvel3.parser.ast.expr.InlineCastExpr;
import org.mvel3.parser.ast.expr.ListCreationLiteralExpression;
import org.mvel3.parser.ast.expr.ListCreationLiteralExpressionElement;
import org.mvel3.parser.ast.expr.MapCreationLiteralExpression;
import org.mvel3.parser.ast.expr.MapCreationLiteralExpressionKeyValuePair;
import org.mvel3.parser.ast.expr.ModifyStatement;
import org.mvel3.parser.ast.expr.NullSafeFieldAccessExpr;
import org.mvel3.parser.ast.expr.NullSafeMethodCallExpr;
import org.mvel3.parser.ast.expr.TemporalChunkExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralChunkExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralExpr;
import org.mvel3.parser.ast.expr.WithStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mvel3.parser.util.AstUtils.getBinaryExprOperator;

/**
 * Visitor that converts ANTLR4 parse tree to JavaParser AST nodes.
 * This implementation can return various types of nodes (Expression, Type, etc.).
 */
public class Mvel3ToJavaParserVisitor extends Mvel3ParserBaseVisitor<Node> {

    // Associate antlr tokenId with a JavaParser node for identifier, so it can be used for code completion.
    protected final Map<Integer, Node> tokenIdJPNodeMap = new HashMap<>();

    public Map<Integer, Node> getTokenIdJPNodeMap() {
        return tokenIdJPNodeMap;
    }

    @Override
    public Node visitCompilationUnit(Mvel3Parser.CompilationUnitContext ctx) {
        CompilationUnit cu = new CompilationUnit();

        if (ctx.packageDeclaration() != null) {
            Name pkg = StaticJavaParser.parseName(ctx.packageDeclaration().qualifiedName().getText());
            cu.setPackageDeclaration(new PackageDeclaration(pkg));
        }

        if (ctx.importDeclaration() != null) {
            for (Mvel3Parser.ImportDeclarationContext importDecl : ctx.importDeclaration()) {
                ImportDeclaration importDeclaration = (ImportDeclaration) visit(importDecl);
                if (importDeclaration != null) {
                    cu.addImport(importDeclaration);
                }
            }
        }

        // Handle type declarations
        if (ctx.typeDeclaration() != null) {
            for (Mvel3Parser.TypeDeclarationContext typeDecl : ctx.typeDeclaration()) {
                Node node = visit(typeDecl);
                if (node instanceof TypeDeclaration) {
                    cu.addType((TypeDeclaration<?>) node);
                }
            }
        }

        // Handle module declaration (second alternative: moduleDeclaration EOF)
        if (ctx.moduleDeclaration() != null) {
            Mvel3Parser.ModuleDeclarationContext moduleCtx = ctx.moduleDeclaration();
            boolean isOpen = moduleCtx.OPEN() != null;
            Name moduleName = StaticJavaParser.parseName(moduleCtx.qualifiedName().getText());

            NodeList<ModuleDirective> directives = new NodeList<>();
            if (moduleCtx.moduleBody() != null && moduleCtx.moduleBody().moduleDirective() != null) {
                for (Mvel3Parser.ModuleDirectiveContext dirCtx : moduleCtx.moduleBody().moduleDirective()) {
                    directives.add(processModuleDirective(dirCtx));
                }
            }

            ModuleDeclaration moduleDecl = new ModuleDeclaration(moduleName, isOpen);
            moduleDecl.setDirectives(directives);
            cu.setModule(moduleDecl);
        }

        return cu;
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
        Type fieldType = (Type) visit(ctx.typeType());

        NodeList<VariableDeclarator> declarators = new NodeList<>();
        for (Mvel3Parser.VariableDeclaratorContext declaratorCtx : ctx.variableDeclarators().variableDeclarator()) {
            String varName = declaratorCtx.variableDeclaratorId().identifier().getText();

            // Apply C-style array dimensions from variableDeclaratorId
            Type declType = ArrayConverter.applyArrayDimensions(fieldType, declaratorCtx.variableDeclaratorId());

            VariableDeclarator varDeclarator = new VariableDeclarator(declType, varName);
            varDeclarator.setTokenRange(TokenRangeConverter.createTokenRange(declaratorCtx));

            if (declaratorCtx.variableInitializer() != null) {
                Expression initializer = (Expression) visit(declaratorCtx.variableInitializer());
                varDeclarator.setInitializer(initializer);
            }

            declarators.add(varDeclarator);
        }

        FieldDeclaration fieldDecl = new FieldDeclaration(new NodeList<>(), declarators);
        fieldDecl.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        // Handle modifiers from parent context
        ModifiersAnnotations fieldModifiers = ModifiersParser.resolveModifiersFromParent(ctx);
        fieldDecl.setModifiers(fieldModifiers.modifiers());
        fieldDecl.setAnnotations(fieldModifiers.annotations());

        return fieldDecl;
    }

    @Override
    public Node visitConstructorDeclaration(Mvel3Parser.ConstructorDeclarationContext ctx) {
        String name = ctx.identifier().getText();

        ConstructorDeclaration constructorDecl = new ConstructorDeclaration(name);

        // Handle parameters
        if (ctx.formalParameters() != null) {
            constructorDecl.setParameters(ParametersConverter.convertFormalParameters(ctx.formalParameters(), this));
            ParametersConverter.convertReceiverParameter(ctx.formalParameters(), constructorDecl, this);
        }

        // Handle throws clause
        if (ctx.THROWS() != null && ctx.qualifiedNameList() != null) {
            constructorDecl.setThrownExceptions(TypeConverter.convertQualifiedNameListAsTypes(ctx.qualifiedNameList()));
        }

        // Handle modifiers from parent context
        ModifiersAnnotations constructorModifiers = ModifiersParser.resolveModifiersFromParent(ctx);
        constructorDecl.setModifiers(constructorModifiers.modifiers());
        constructorDecl.setAnnotations(constructorModifiers.annotations());

        // Handle constructor body
        if (ctx.block() != null) {
            BlockStmt body = (BlockStmt) visit(ctx.block());
            constructorDecl.setBody(body);
        }

        constructorDecl.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return constructorDecl;
    }

    @Override
    public Node visitGenericMethodDeclaration(Mvel3Parser.GenericMethodDeclarationContext ctx) {
        MethodDeclaration methodDecl = (MethodDeclaration) visitMethodDeclaration(ctx.methodDeclaration());
        if (ctx.typeParameters() != null) {
            methodDecl.setTypeParameters(TypeConverter.convertTypeParameters(ctx.typeParameters(), this));
        }
        return methodDecl;
    }

    @Override
    public Node visitGenericConstructorDeclaration(Mvel3Parser.GenericConstructorDeclarationContext ctx) {
        ConstructorDeclaration constructorDecl = (ConstructorDeclaration) visitConstructorDeclaration(ctx.constructorDeclaration());
        if (ctx.typeParameters() != null) {
            constructorDecl.setTypeParameters(TypeConverter.convertTypeParameters(ctx.typeParameters(), this));
        }
        return constructorDecl;
    }

    @Override
    public Node visitMethodDeclaration(Mvel3Parser.MethodDeclarationContext ctx) {
        // Get method name
        String methodName = ctx.identifier().getText();

        // Create method declaration
        MethodDeclaration methodDecl = new MethodDeclaration();
        methodDecl.setName(methodName);

        // Handle return type
        if (ctx.typeTypeOrVoid() != null) {
            if (ctx.typeTypeOrVoid().VOID() != null) {
                methodDecl.setType(new VoidType());
            } else if (ctx.typeTypeOrVoid().typeType() != null) {
                Type returnType = (Type) visit(ctx.typeTypeOrVoid().typeType());
                // Handle array dimensions after formal parameters: ('[' ']')*
                int extraDims = ctx.LBRACK() != null ? ctx.LBRACK().size() : 0;
                for (int i = 0; i < extraDims; i++) {
                    returnType = new ArrayType(returnType);
                }
                methodDecl.setType(returnType);
            }
        }

        // Handle parameters
        if (ctx.formalParameters() != null) {
            methodDecl.setParameters(ParametersConverter.convertFormalParameters(ctx.formalParameters(), this));
            ParametersConverter.convertReceiverParameter(ctx.formalParameters(), methodDecl, this);
        }

        // Handle throws clause
        if (ctx.THROWS() != null && ctx.qualifiedNameList() != null) {
            methodDecl.setThrownExceptions(TypeConverter.convertQualifiedNameListAsTypes(ctx.qualifiedNameList()));
        }

        // Handle modifiers (from parent context)
        ModifiersAnnotations methodModifiers = ModifiersParser.resolveModifiersFromParent(ctx);
        methodDecl.setModifiers(methodModifiers.modifiers());
        methodDecl.setAnnotations(methodModifiers.annotations());

        // Handle method body
        if (ctx.methodBody() != null) {
            if (ctx.methodBody().block() != null) {
                BlockStmt body = (BlockStmt) visit(ctx.methodBody().block());
                methodDecl.setBody(body);
            }
        }

        return methodDecl;
    }

    @Override
    public Node visitMethodCallExpression(Mvel3Parser.MethodCallExpressionContext ctx) {
        // Handle method call without scope
        return visitMethodCallWithScope(ctx.methodCall(), null);
    }

    private MethodCallExpr visitMethodCallWithScope(Mvel3Parser.MethodCallContext ctx, Expression scope) {
        String methodName;
        if (ctx.identifier() != null) {
            methodName = ctx.identifier().getText();
        } else if (ctx.THIS() != null) {
            methodName = "this";
        } else if (ctx.SUPER() != null) {
            methodName = "super";
        } else {
            throw new IllegalArgumentException("Unknown method call type: " + ctx.getText());
        }

        MethodCallExpr methodCall = new MethodCallExpr(scope, methodName);

        // Handle arguments
        if (ctx.arguments() != null && ctx.arguments().expressionList() != null) {
            methodCall.setArguments(ArgumentsConverter.convertArguments(ctx.arguments(), this));
        }

        methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        return methodCall;
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

        String operatorText = resolveOperatorText(ctx);

        // Handle assignment operators separately
        AssignExpr.Operator assignOp = getAssignOperator(operatorText);
        if (assignOp != null) {
            return new AssignExpr(TokenRangeConverter.createTokenRange(ctx), left, right, assignOp);
        }
        
        // Handle other binary operators
        BinaryExpr.Operator operator = getBinaryExprOperator(operatorText);
        return new BinaryExpr(TokenRangeConverter.createTokenRange(ctx), left, right, operator);
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
    public Node visitTernaryExpression(Mvel3Parser.TernaryExpressionContext ctx) {
        Expression condition = (Expression) visit(ctx.expression(0));
        Expression thenExpr = (Expression) visit(ctx.expression(1));
        Expression elseExpr = (Expression) visit(ctx.expression(2));

        ConditionalExpr conditionalExpr = new ConditionalExpr(condition, thenExpr, elseExpr);
        conditionalExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return conditionalExpr;
    }

    @Override
    public Node visitInstanceOfOperatorExpression(Mvel3Parser.InstanceOfOperatorExpressionContext ctx) {
        Expression expression = (Expression) visit(ctx.expression());

        if (ctx.pattern() != null) {
            // Java 14+ pattern matching: expr instanceof Type varName
            Mvel3Parser.PatternContext patternCtx = ctx.pattern();
            ReferenceType type = (ReferenceType) visit(patternCtx.typeType());
            SimpleName name = new SimpleName(patternCtx.identifier().getText());

            NodeList<Modifier> modifiers = new NodeList<>();
            if (patternCtx.variableModifier() != null) {
                for (Mvel3Parser.VariableModifierContext modCtx : patternCtx.variableModifier()) {
                    if (modCtx.FINAL() != null) {
                        modifiers.add(Modifier.finalModifier());
                    }
                }
            }

            PatternExpr patternExpr = new PatternExpr(modifiers, type, name);
            patternExpr.setTokenRange(TokenRangeConverter.createTokenRange(patternCtx));

            InstanceOfExpr instanceOfExpr = new InstanceOfExpr(expression, type, patternExpr);
            instanceOfExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return instanceOfExpr;
        } else {
            // Classic instanceof: expr instanceof Type
            ReferenceType type = (ReferenceType) visit(ctx.typeType());
            InstanceOfExpr instanceOfExpr = new InstanceOfExpr(expression, type);
            instanceOfExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return instanceOfExpr;
        }
    }

    @Override
    public Node visitExpressionSwitch(Mvel3Parser.ExpressionSwitchContext ctx) {
        return visit(ctx.switchExpression());
    }

    @Override
    public Node visitSwitchExpression(Mvel3Parser.SwitchExpressionContext ctx) {
        // switchExpression: SWITCH parExpression '{' switchLabeledRule* '}'
        Expression selector = (Expression) visit(ctx.parExpression().expression());

        NodeList<SwitchEntry> entries = new NodeList<>();
        if (ctx.switchLabeledRule() != null) {
            for (Mvel3Parser.SwitchLabeledRuleContext ruleCtx : ctx.switchLabeledRule()) {
                SwitchEntry entry = processSwitchLabeledRule(ruleCtx);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }

        SwitchExpr switchExpr = new SwitchExpr(selector, entries);
        switchExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return switchExpr;
    }

    @Override
    public Node visitMethodReferenceExpression(Mvel3Parser.MethodReferenceExpressionContext ctx) {
        Expression scope;
        if (ctx.expression() != null) {
            // expression '::' typeArguments? identifier
            scope = (Expression) visit(ctx.expression());
        } else if (ctx.classType() != null) {
            // classType '::' typeArguments? NEW
            Type type = (Type) visit(ctx.classType());
            scope = new TypeExpr(type);
        } else if (ctx.typeType() != null) {
            // typeType '::' (typeArguments? identifier | NEW)
            Type type = (Type) visit(ctx.typeType());
            scope = new TypeExpr(type);
        } else {
            throw new IllegalArgumentException("Unsupported method reference: " + ctx.getText());
        }

        // Identifier is the method name, or "new" for constructor references
        String identifier = ctx.NEW() != null ? "new" : ctx.identifier().getText();

        // Handle type arguments if present
        NodeList<Type> typeArguments = ctx.typeArguments() != null ? ArgumentsConverter.convertTypeArguments(ctx.typeArguments(), this) : null;

        MethodReferenceExpr methodRef = new MethodReferenceExpr(scope, typeArguments, identifier);
        methodRef.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return methodRef;
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

    protected void associateAntlrTokenWithJPNode(ParserRuleContext ctx, Node jpNode) {
        // Antlr token <-> JavaParser node mapping for code completion
        ParseTree lastNode = ctx.children.get(ctx.children.size() - 1); // take the last terminal node
        if (lastNode instanceof TerminalNode terminalNode) {
            tokenIdJPNodeMap.put(terminalNode.getSymbol().getTokenIndex(), jpNode);
        }
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
        // Handle inline cast: expr#Type#[member]
        Expression scope = (Expression) visit(ctx.expression(0));
        Type type = (Type) visit(ctx.typeType());

        InlineCastExpr inlineCastExpr = new InlineCastExpr(type, scope);
        inlineCastExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        associateAntlrTokenWithJPNode(ctx, inlineCastExpr);

        if (ctx.identifier() != null) {
            String name = ctx.identifier().getText();
            if (ctx.arguments() != null) {
                MethodCallExpr methodCall = new MethodCallExpr(inlineCastExpr, name);
                methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                methodCall.setArguments(ArgumentsConverter.convertArguments(ctx.arguments(), this));
                return methodCall;
            }

            FieldAccessExpr fieldAccess = new FieldAccessExpr(inlineCastExpr, name);
            fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return fieldAccess;
        }

        if (ctx.LBRACK() != null) {
            Expression indexExpr = (Expression) visit(ctx.expression(1));
            MethodCallExpr methodCall = new MethodCallExpr(inlineCastExpr, "get");
            methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            methodCall.addArgument(indexExpr);
            return methodCall;
        }

        return inlineCastExpr;
    }

    @Override
    public Node visitListCreationLiteralExpression(Mvel3Parser.ListCreationLiteralExpressionContext ctx) {
        // Visit the listCreationLiteral rule
        return visit(ctx.listCreationLiteral());
    }

    @Override
    public Node visitListCreationLiteral(Mvel3Parser.ListCreationLiteralContext ctx) {
        NodeList<Expression> elements = new NodeList<>();

        // Process each list element
        if (ctx.listElement() != null) {
            for (Mvel3Parser.ListElementContext elementCtx : ctx.listElement()) {
                Expression expr = (Expression) visit(elementCtx.expression());
                // Wrap in ListCreationLiteralExpressionElement as per mvel.jj
                ListCreationLiteralExpressionElement element =
                    new ListCreationLiteralExpressionElement(expr);
                element.setTokenRange(TokenRangeConverter.createTokenRange(elementCtx));
                elements.add(element);
            }
        }

        ListCreationLiteralExpression listExpr = new ListCreationLiteralExpression(elements);
        listExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return listExpr;
    }

    @Override
    public Node visitMapCreationLiteralExpression(Mvel3Parser.MapCreationLiteralExpressionContext ctx) {
        // Visit the mapCreationLiteral rule
        return visit(ctx.mapCreationLiteral());
    }

    @Override
    public Node visitMapCreationLiteral(Mvel3Parser.MapCreationLiteralContext ctx) {
        NodeList<Expression> entries = new NodeList<>();

        // Check for empty map syntax [:]
        if (ctx.COLON() != null && ctx.mapEntry().isEmpty()) {
            // Empty map
            MapCreationLiteralExpression mapExpr = new MapCreationLiteralExpression(entries);
            mapExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return mapExpr;
        }

        // Process each map entry
        if (ctx.mapEntry() != null) {
            for (Mvel3Parser.MapEntryContext entryCtx : ctx.mapEntry()) {
                Expression key = (Expression) visit(entryCtx.expression(0));
                Expression value = (Expression) visit(entryCtx.expression(1));

                // Wrap in MapCreationLiteralExpressionKeyValuePair as per mvel.jj
                MapCreationLiteralExpressionKeyValuePair pair =
                    new MapCreationLiteralExpressionKeyValuePair(key, value);
                pair.setTokenRange(TokenRangeConverter.createTokenRange(entryCtx));
                entries.add(pair);
            }
        }

        MapCreationLiteralExpression mapExpr = new MapCreationLiteralExpression(entries);
        mapExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return mapExpr;
    }

    @Override
    public Node visitNullSafeExpression(Mvel3Parser.NullSafeExpressionContext ctx) {
        // Extract the scope (left side of !.)
        Expression scope = (Expression) visit(ctx.expression());

        // Extract the identifier name
        String name = ctx.identifier().getText();

        // Check if there are arguments (method call) or not (field access)
        if (ctx.arguments() != null) {
            // Method call: $p!.getName()
            NodeList<Expression> arguments = new NodeList<>();
            if (ctx.arguments().expressionList() != null) {
                for (Mvel3Parser.ExpressionContext argCtx : ctx.arguments().expressionList().expression()) {
                    arguments.add((Expression) visit(argCtx));
                }
            }

            // Extract type arguments if present
            NodeList<Type> typeArguments = ctx.typeArguments() != null ? ArgumentsConverter.convertTypeArguments(ctx.typeArguments(), this) : null;

            NullSafeMethodCallExpr methodCall = new NullSafeMethodCallExpr(
                scope,
                typeArguments,
                name,
                arguments
            );
            methodCall.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return methodCall;
        } else {
            // Field access: $p!.name
            NullSafeFieldAccessExpr fieldAccess = new NullSafeFieldAccessExpr(scope, name);
            fieldAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return fieldAccess;
        }
    }

    @Override
    public Node visitCastExpression(Mvel3Parser.CastExpressionContext ctx) {
        NodeList<Type> parsedTypes = new NodeList<>();
        for (Mvel3Parser.TypeTypeContext typeCtx : ctx.typeType()) {
            parsedTypes.add((Type) visit(typeCtx));
        }

        Type targetType;
        if (parsedTypes.size() == 1) {
            targetType = parsedTypes.get(0);
        } else {
            NodeList<ReferenceType> referenceTypes = new NodeList<>();
            for (Type type : parsedTypes) {
                if (!(type instanceof ReferenceType)) {
                    throw new IllegalArgumentException("Intersection casts require reference types: " + ctx.getText());
                }
                referenceTypes.add((ReferenceType) type);
            }
            IntersectionType intersectionType = new IntersectionType(referenceTypes);
            intersectionType.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            targetType = intersectionType;
        }

        Expression expression = (Expression) visit(ctx.expression());

        CastExpr castExpr = new CastExpr(targetType, expression);
        castExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return castExpr;
    }

    @Override
    public Node visitLambdaExpression(Mvel3Parser.LambdaExpressionContext ctx) {
        LambdaParametersResult parametersResult = resolveLambdaParameters(ctx.lambdaParameters());
        Statement body = resolveLambdaBody(ctx.lambdaBody());

        LambdaExpr lambdaExpr = new LambdaExpr(parametersResult.parameters, body, parametersResult.enclosingParameters);
        lambdaExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return lambdaExpr;
    }

    @Override
    public Node visitLiteral(Mvel3Parser.LiteralContext ctx) {
        if (ctx.STRING_LITERAL() != null) {
            String text = ctx.STRING_LITERAL().getText();
            // Remove quotes
            String value = text.substring(1, text.length() - 1);
            StringLiteralExpr stringLiteral = new StringLiteralExpr(value);
            stringLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return stringLiteral;
        } else if (ctx.DECIMAL_LITERAL() != null) {
            return createIntegerOrLongLiteral(ctx.DECIMAL_LITERAL().getText(), ctx);
        } else if (ctx.HEX_LITERAL() != null) {
            return createIntegerOrLongLiteral(ctx.HEX_LITERAL().getText(), ctx);
        } else if (ctx.OCT_LITERAL() != null) {
            return createIntegerOrLongLiteral(ctx.OCT_LITERAL().getText(), ctx);
        } else if (ctx.BINARY_LITERAL() != null) {
            return createIntegerOrLongLiteral(ctx.BINARY_LITERAL().getText(), ctx);
        } else if (ctx.FLOAT_LITERAL() != null) {
            DoubleLiteralExpr doubleLiteral = new DoubleLiteralExpr(ctx.FLOAT_LITERAL().getText());
            doubleLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return doubleLiteral;
        } else if (ctx.HEX_FLOAT_LITERAL() != null) {
            DoubleLiteralExpr doubleLiteral = new DoubleLiteralExpr(ctx.HEX_FLOAT_LITERAL().getText());
            doubleLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return doubleLiteral;
        } else if (ctx.BOOL_LITERAL() != null) {
            BooleanLiteralExpr booleanLiteral = new BooleanLiteralExpr(Boolean.parseBoolean(ctx.BOOL_LITERAL().getText()));
            booleanLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return booleanLiteral;
        } else if (ctx.NULL_LITERAL() != null) {
            NullLiteralExpr nullLiteral = new NullLiteralExpr();
            nullLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return nullLiteral;
        } else if (ctx.CHAR_LITERAL() != null) {
            String text = ctx.CHAR_LITERAL().getText();
            char value = text.charAt(1); // Simple case, more complex handling needed for escape sequences
            CharLiteralExpr charLiteral = new CharLiteralExpr(value);
            charLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return charLiteral;
        } else if (ctx.TEXT_BLOCK() != null) {
            String rawText = ctx.TEXT_BLOCK().getText();
            // Extract content between triple quotes: """content"""
            String content = rawText.substring(3, rawText.length() - 3);
            TextBlockLiteralExpr textBlockLiteral = new TextBlockLiteralExpr(content);
            textBlockLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return textBlockLiteral;
        }
        
        // Handle MVEL-specific literals - create proper AST nodes like mvel.jj does
        if (ctx.BigDecimalLiteral() != null) {
            String text = ctx.BigDecimalLiteral().getText();
            // Create BigDecimalLiteralExpr node (transformation happens in MVELToJavaRewriter)
            return new BigDecimalLiteralExpr(TokenRangeConverter.createTokenRange(ctx), text);
        } else if (ctx.BigIntegerLiteral() != null) {
            String text = ctx.BigIntegerLiteral().getText();
            // Create BigIntegerLiteralExpr node (transformation happens in MVELToJavaRewriter)
            return new BigIntegerLiteralExpr(TokenRangeConverter.createTokenRange(ctx), text);
        } else if (ctx.temporalLiteral() != null) {
            return buildTemporalLiteral(ctx.temporalLiteral());
        }
        
        throw new IllegalArgumentException("Unknown literal type: " + ctx.getText());
    }

    private Expression createIntegerOrLongLiteral(String text, ParserRuleContext ctx) {
        if (text.endsWith("L") || text.endsWith("l")) {
            LongLiteralExpr longLiteral = new LongLiteralExpr(text);
            longLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return longLiteral;
        } else {
            IntegerLiteralExpr integerLiteral = new IntegerLiteralExpr(text);
            integerLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return integerLiteral;
        }
    }

    private TemporalLiteralExpr buildTemporalLiteral(Mvel3Parser.TemporalLiteralContext ctx) {
        NodeList<TemporalChunkExpr> chunks = new NodeList<>();
        for (Mvel3Parser.TemporalLiteralChunkContext chunkCtx : ctx.temporalLiteralChunk()) {
            chunks.add(buildTemporalLiteralChunk(chunkCtx));
        }
        TemporalLiteralExpr temporalLiteralExpr = new TemporalLiteralExpr(TokenRangeConverter.createTokenRange(ctx), chunks);
        return temporalLiteralExpr;
    }

    private TemporalLiteralChunkExpr buildTemporalLiteralChunk(Mvel3Parser.TemporalLiteralChunkContext ctx) {
        Token token;
        TimeUnit timeUnit;

        if (ctx.MILLISECOND_LITERAL() != null) {
            token = ctx.MILLISECOND_LITERAL().getSymbol();
            timeUnit = TimeUnit.MILLISECONDS;
        } else if (ctx.SECOND_LITERAL() != null) {
            token = ctx.SECOND_LITERAL().getSymbol();
            timeUnit = TimeUnit.SECONDS;
        } else if (ctx.MINUTE_LITERAL() != null) {
            token = ctx.MINUTE_LITERAL().getSymbol();
            timeUnit = TimeUnit.MINUTES;
        } else if (ctx.HOUR_LITERAL() != null) {
            token = ctx.HOUR_LITERAL().getSymbol();
            timeUnit = TimeUnit.HOURS;
        } else if (ctx.DAY_LITERAL() != null) {
            token = ctx.DAY_LITERAL().getSymbol();
            timeUnit = TimeUnit.DAYS;
        } else {
            throw new IllegalArgumentException("Unsupported temporal literal chunk: " + ctx.getText());
        }

        return new TemporalLiteralChunkExpr(TokenRangeConverter.createTokenRange(ctx), stripTimeUnit(token.getText()), timeUnit);
    }

    private String stripTimeUnit(String text) {
        // Remove the time unit suffix (e.g., "m", "s", "h", "d", "ms")
        return text.replaceAll("[a-zA-Z]+$", "");
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
        // Handle array/list access: expression[index]
        Expression array = (Expression) visit(ctx.expression(0)); // The array/list expression
        Expression index = (Expression) visit(ctx.expression(1)); // The index expression
        
        // Create ArrayAccessExpr like mvel.jj does
        // The transformation to .get() method calls is handled by MVELToJavaRewriter
        ArrayAccessExpr arrayAccess = new ArrayAccessExpr(array, index);
        arrayAccess.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return arrayAccess;
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
        // modify ( identifier ) { statement* }
        String targetName = ctx.identifier().getText();
        NameExpr target = new NameExpr(targetName);
        target.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        // Create a NodeList for the statements  
        NodeList<Statement> statements = new NodeList<>();
        
        // Process each statement in the modify block
        // Keep assignments as simple names - MVELToJavaRewriter will add the target prefix
        for (Mvel3Parser.StatementContext stmtCtx : ctx.statement()) {
            Statement stmt = (Statement) visit(stmtCtx);
            statements.add(stmt);
        }
        
        // Create and return a ModifyStatement with proper TokenRange
        return new ModifyStatement(TokenRangeConverter.createTokenRange(ctx), target, statements);
    }

    @Override
    public Node visitWithStatement(Mvel3Parser.WithStatementContext ctx) {
        String targetName = ctx.identifier().getText();
        NameExpr target = new NameExpr(targetName);
        target.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        NodeList<Statement> statements = new NodeList<>();
        for (Mvel3Parser.StatementContext stmtCtx : ctx.statement()) {
            Statement stmt = (Statement) visit(stmtCtx);
            statements.add(stmt);
        }

        return new WithStatement(TokenRangeConverter.createTokenRange(ctx), target, statements);
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

            // Handle nonWildcardTypeArguments: new <String>Foo()
            NodeList<Type> typeArguments = null;
            if (ctx.nonWildcardTypeArguments() != null) {
                typeArguments = new NodeList<>();
                typeArguments.addAll(TypeConverter.convertTypeList(ctx.nonWildcardTypeArguments().typeList(), this));
            }

            // Get constructor arguments
            NodeList<Expression> arguments = new NodeList<>();
            if (ctx.classCreatorRest().arguments() != null &&
                ctx.classCreatorRest().arguments().expressionList() != null) {
                for (Mvel3Parser.ExpressionContext exprCtx : ctx.classCreatorRest().arguments().expressionList().expression()) {
                    Expression arg = (Expression) visit(exprCtx);
                    arguments.add(arg);
                }
            }

            // Handle anonymous class body if present
            NodeList<BodyDeclaration<?>> anonymousClassBody = null;
            if (ctx.classCreatorRest().classBody() != null) {
                anonymousClassBody = TypeConverter.convertAnonymousClassBody(ctx.classCreatorRest().classBody(), this);
            }

            // Create ObjectCreationExpr
            ObjectCreationExpr objectCreation = new ObjectCreationExpr(null, (ClassOrInterfaceType) type, typeArguments, arguments, anonymousClassBody);
            objectCreation.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
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
                level.setTokenRange(TokenRangeConverter.createTokenRange(ctx)); 
                levels.add(level);
            }
            
            // Create ArrayCreationExpr
            ArrayCreationExpr arrayCreation = new ArrayCreationExpr(elementType, levels, initializer);
            arrayCreation.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return arrayCreation;
        } else {
            // Handle: new Type[size] or new Type[size1][size2]
            NodeList<ArrayCreationLevel> levels = new NodeList<>();
            
            if (ctx.expression() != null) {
                for (Mvel3Parser.ExpressionContext exprCtx : ctx.expression()) {
                    Expression dimExpr = (Expression) visit(exprCtx);
                    ArrayCreationLevel level = new ArrayCreationLevel(dimExpr);
                    level.setTokenRange(TokenRangeConverter.createTokenRange(exprCtx));
                    levels.add(level);
                }
            }
            
            // Create ArrayCreationExpr with dimensions
            ArrayCreationExpr arrayCreation = new ArrayCreationExpr(elementType, levels, null);
            arrayCreation.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
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
                type.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            }
            return type;
        }

        throw new IllegalArgumentException("Unsupported created name: " + ctx.getText());
    }

    private NodeList<Type> handleTypeArgumentsOrDiamond(Mvel3Parser.TypeArgumentsOrDiamondContext ctx) {
        if (ctx.typeArguments() != null) {
            return ArgumentsConverter.convertTypeArguments(ctx.typeArguments(), this);
        } else {
            // Handle diamond operator: <> - return empty NodeList to represent diamond
            return new NodeList<>();
        }
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
        unaryExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
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
        unaryExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return unaryExpr;
    }

    // Handle constant expressions by visiting the contained expression
    private Node visitConstantExpr(Mvel3Parser.ExpressionContext ctx) {
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

        // Extract variable modifiers (final, annotations)
        ModifiersAnnotations varModifiers = VariableParser.parseVariableModifiers(enhancedForCtx.variableModifier());

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

        // Create VariableDeclarationExpr with modifiers
        NodeList<VariableDeclarator> variables = new NodeList<>();
        variables.add(variableDeclarator);
        VariableDeclarationExpr varDecl = new VariableDeclarationExpr(varModifiers.modifiers(), varModifiers.annotations(), variables);

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
            } else if (labelCtx.varName != null) {
                // case typeType varName:
                ReferenceType type = (ReferenceType) visit(labelCtx.typeType());
                SimpleName name = new SimpleName(labelCtx.varName.getText());
                PatternExpr patternExpr = new PatternExpr(new NodeList<>(), type, name);
                patternExpr.setTokenRange(TokenRangeConverter.createTokenRange(labelCtx));
                labels.add(patternExpr);
            }
            
            SwitchEntry entry = new SwitchEntry(labels, SwitchEntry.Type.STATEMENT_GROUP, new NodeList<>());
            entry.setTokenRange(TokenRangeConverter.createTokenRange(labelCtx));
            return entry;
            
        } else if (labelCtx.DEFAULT() != null) {
            // Default label
            SwitchEntry entry = new SwitchEntry(new NodeList<>(), SwitchEntry.Type.STATEMENT_GROUP, new NodeList<>());
            entry.setTokenRange(TokenRangeConverter.createTokenRange(labelCtx));
            return entry;
        }
        
        return null;
    }

    private Expression processGuardedPattern(Mvel3Parser.GuardedPatternContext ctx) {
        // guardedPattern
        //     : '(' guardedPattern ')'                                             // alt 1
        //     | variableModifier* typeType annotation* identifier ('&&' expression)*  // alt 2
        //     | guardedPattern '&&' expression                                     // alt 3

        if (ctx.LPAREN() != null) {
            // Alt 1: parenthesized guarded pattern
            Expression inner = processGuardedPattern(ctx.guardedPattern());
            EnclosedExpr enclosed = new EnclosedExpr(inner);
            enclosed.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return enclosed;
        } else if (ctx.identifier() != null) {
            // Alt 2: type pattern with optional guards
            ReferenceType type = (ReferenceType) visit(ctx.typeType());
            SimpleName name = new SimpleName(ctx.identifier().getText());

            NodeList<Modifier> modifiers = new NodeList<>();
            if (ctx.variableModifier() != null) {
                for (Mvel3Parser.VariableModifierContext modCtx : ctx.variableModifier()) {
                    if (modCtx.FINAL() != null) {
                        modifiers.add(Modifier.finalModifier());
                    }
                }
            }

            PatternExpr patternExpr = new PatternExpr(modifiers, type, name);
            patternExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

            Expression result = patternExpr;
            if (ctx.expression() != null) {
                for (Mvel3Parser.ExpressionContext exprCtx : ctx.expression()) {
                    Expression guard = (Expression) visit(exprCtx);
                    result = new BinaryExpr(result, guard, BinaryExpr.Operator.AND);
                }
            }
            return result;
        } else {
            // Alt 3: recursive guard — guardedPattern '&&' expression
            Expression left = processGuardedPattern(ctx.guardedPattern());
            Expression right = (Expression) visit(ctx.expression(0));
            return new BinaryExpr(left, right, BinaryExpr.Operator.AND);
        }
    }

    private SwitchEntry processSwitchLabeledRule(Mvel3Parser.SwitchLabeledRuleContext ruleCtx) {
        // switchLabeledRule: CASE (expressionList | NULL_LITERAL | guardedPattern) (ARROW | COLON) switchRuleOutcome
        //                 | DEFAULT (ARROW | COLON) switchRuleOutcome

        NodeList<Expression> labels = new NodeList<>();
        boolean isArrow = ruleCtx.ARROW() != null;

        if (ruleCtx.CASE() != null) {
            if (ruleCtx.expressionList() != null) {
                // case expr1, expr2, ... ->
                for (Mvel3Parser.ExpressionContext exprCtx : ruleCtx.expressionList().expression()) {
                    labels.add((Expression) visit(exprCtx));
                }
            } else if (ruleCtx.NULL_LITERAL() != null) {
                labels.add(new NullLiteralExpr());
            } else if (ruleCtx.guardedPattern() != null) {
                labels.add(processGuardedPattern(ruleCtx.guardedPattern()));
            }
        }
        // DEFAULT has empty labels

        // Determine entry type and process switchRuleOutcome
        Mvel3Parser.SwitchRuleOutcomeContext outcomeCtx = ruleCtx.switchRuleOutcome();
        NodeList<Statement> statements = new NodeList<>();
        SwitchEntry.Type entryType;

        if (outcomeCtx.block() != null) {
            entryType = isArrow ? SwitchEntry.Type.BLOCK : SwitchEntry.Type.STATEMENT_GROUP;
            BlockStmt block = (BlockStmt) visit(outcomeCtx.block());
            statements.addAll(block.getStatements());
        } else {
            // blockStatement*
            if (isArrow) {
                // For arrow cases with a single expression, determine the type
                if (outcomeCtx.blockStatement() != null && outcomeCtx.blockStatement().size() == 1) {
                    Node node = visit(outcomeCtx.blockStatement(0));
                    if (node instanceof Statement) {
                        Statement stmt = (Statement) node;
                        if (stmt instanceof ThrowStmt) {
                            entryType = SwitchEntry.Type.THROWS_STATEMENT;
                        } else if (stmt instanceof ExpressionStmt) {
                            entryType = SwitchEntry.Type.EXPRESSION;
                        } else {
                            entryType = SwitchEntry.Type.STATEMENT_GROUP;
                        }
                        statements.add(stmt);
                    } else {
                        entryType = SwitchEntry.Type.STATEMENT_GROUP;
                    }
                } else {
                    entryType = SwitchEntry.Type.STATEMENT_GROUP;
                    if (outcomeCtx.blockStatement() != null) {
                        for (Mvel3Parser.BlockStatementContext blockStmtCtx : outcomeCtx.blockStatement()) {
                            Node node = visit(blockStmtCtx);
                            if (node instanceof Statement) {
                                statements.add((Statement) node);
                            }
                        }
                    }
                }
            } else {
                entryType = SwitchEntry.Type.STATEMENT_GROUP;
                if (outcomeCtx.blockStatement() != null) {
                    for (Mvel3Parser.BlockStatementContext blockStmtCtx : outcomeCtx.blockStatement()) {
                        Node node = visit(blockStmtCtx);
                        if (node instanceof Statement) {
                            statements.add((Statement) node);
                        }
                    }
                }
            }
        }

        SwitchEntry entry = new SwitchEntry(labels, entryType, statements);
        entry.setTokenRange(TokenRangeConverter.createTokenRange(ruleCtx));
        return entry;
    }

    private CatchClause parseCatchClause(Mvel3Parser.CatchClauseContext ctx) {
        // catchClause: CATCH '(' variableModifier* catchType identifier ')' block
        // catchType: qualifiedName ('|' qualifiedName)*
        ModifiersAnnotations modifiersAnnotations = VariableParser.parseVariableModifiers(ctx.variableModifier());

        // Parse catch type (may be a union type like IOException | NullPointerException)
        Mvel3Parser.CatchTypeContext catchTypeCtx = ctx.catchType();
        Type catchType;
        if (catchTypeCtx.qualifiedName().size() == 1) {
            // Single type
            catchType = new ClassOrInterfaceType(null, catchTypeCtx.qualifiedName(0).getText());
        } else {
            // Union type: IOException | NullPointerException
            NodeList<ReferenceType> types = new NodeList<>();
            for (Mvel3Parser.QualifiedNameContext qn : catchTypeCtx.qualifiedName()) {
                types.add(new ClassOrInterfaceType(null, qn.getText()));
            }
            catchType = new UnionType(types);
        }

        SimpleName name = new SimpleName(ctx.identifier().getText());
        Parameter parameter = new Parameter(modifiersAnnotations.modifiers(),
                modifiersAnnotations.annotations(),
                catchType,
                false,
                new NodeList<>(),
                name);
        parameter.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        BlockStmt body = (BlockStmt) visit(ctx.block());

        CatchClause catchClause = new CatchClause(parameter, body);
        catchClause.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return catchClause;
    }

    private NodeList<Expression> parseResources(Mvel3Parser.ResourceSpecificationContext ctx) {
        NodeList<Expression> resources = new NodeList<>();
        if (ctx.resources() != null) {
            for (Mvel3Parser.ResourceContext resourceCtx : ctx.resources().resource()) {
                if (resourceCtx.qualifiedName() != null && resourceCtx.expression() == null) {
                    // Simple name reference: try (existingResource) { ... }
                    resources.add(new NameExpr(resourceCtx.qualifiedName().getText()));
                } else {
                    // Variable declaration: try (Type var = expr) or try (var x = expr)
                    ModifiersAnnotations resourceModifiers = VariableParser.parseVariableModifiers(resourceCtx.variableModifier());
                    Type type;
                    String varName;
                    if (resourceCtx.VAR() != null) {
                        type = new VarType();
                        varName = resourceCtx.identifier().getText();
                    } else {
                        type = (Type) visit(resourceCtx.classOrInterfaceType());
                        varName = resourceCtx.variableDeclaratorId().identifier().getText();
                    }
                    VariableDeclarator declarator = new VariableDeclarator(type, varName);
                    Expression initializer = (Expression) visit(resourceCtx.expression());
                    declarator.setInitializer(initializer);
                    NodeList<VariableDeclarator> declarators = new NodeList<>();
                    declarators.add(declarator);
                    VariableDeclarationExpr varDecl = new VariableDeclarationExpr(resourceModifiers.modifiers(), resourceModifiers.annotations(), declarators);
                    varDecl.setTokenRange(TokenRangeConverter.createTokenRange(resourceCtx));
                    resources.add(varDecl);
                }
            }
        }
        return resources;
    }

    @Override
    public Node visitClassType(Mvel3Parser.ClassTypeContext ctx) {
        // classType: (classOrInterfaceType '.')? annotation* identifier typeArguments?
        ClassOrInterfaceType scope = null;
        if (ctx.classOrInterfaceType() != null) {
            scope = (ClassOrInterfaceType) visit(ctx.classOrInterfaceType());
        }

        String name = ctx.identifier().getText();
        ClassOrInterfaceType type = new ClassOrInterfaceType(scope, name);

        if (ctx.typeArguments() != null) {
            type.setTypeArguments(ArgumentsConverter.convertTypeArguments(ctx.typeArguments(), this));
        }

        type.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return type;
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

    private LambdaParametersResult resolveLambdaParameters(Mvel3Parser.LambdaParametersContext ctx) {
        if (ctx == null) {
            return new LambdaParametersResult(new NodeList<>(), false);
        }

        NodeList<Parameter> parameters = new NodeList<>();
        boolean enclosingParameters = ctx.LPAREN() != null;

        if (!enclosingParameters && ctx.identifier() != null && !ctx.identifier().isEmpty()) {
            parameters.add(createInferredParameter(ctx.identifier(0)));
            return new LambdaParametersResult(parameters, false);
        }

        if (ctx.formalParameterList() != null) {
            parameters.addAll(ParametersConverter.convertFormalParameters(ctx.formalParameterList(), this));
        } else if (ctx.lambdaLVTIList() != null) {
            parameters.addAll(collectLambdaLVTIParameters(ctx.lambdaLVTIList()));
        } else if (ctx.identifier() != null && !ctx.identifier().isEmpty()) {
            for (Mvel3Parser.IdentifierContext identifierContext : ctx.identifier()) {
                parameters.add(createInferredParameter(identifierContext));
            }
        }

        return new LambdaParametersResult(parameters, enclosingParameters);
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

    private NodeList<Parameter> collectLambdaLVTIParameters(Mvel3Parser.LambdaLVTIListContext ctx) {
        NodeList<Parameter> parameters = new NodeList<>();
        if (ctx != null) {
            for (Mvel3Parser.LambdaLVTIParameterContext parameterContext : ctx.lambdaLVTIParameter()) {
                parameters.add(createLambdaVarParameter(parameterContext));
            }
        }
        return parameters;
    }

    private Parameter createInferredParameter(Mvel3Parser.IdentifierContext identifierContext) {
        UnknownType unknownType = new UnknownType();
        unknownType.setTokenRange(TokenRangeConverter.createTokenRange(identifierContext));

        SimpleName name = ParametersConverter.createSimpleName(identifierContext);

        Parameter parameter = new Parameter(new NodeList<>(), new NodeList<>(), unknownType, false, new NodeList<>(), name);
        parameter.setTokenRange(TokenRangeConverter.createTokenRange(identifierContext));
        return parameter;
    }

    private Parameter createLambdaVarParameter(Mvel3Parser.LambdaLVTIParameterContext ctx) {
        ModifiersAnnotations modifiersAnnotations = VariableParser.parseVariableModifiers(ctx.variableModifier());
        VarType varType = new VarType();
        varType.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        SimpleName name = ParametersConverter.createSimpleName(ctx.identifier());

        Parameter parameter = new Parameter(modifiersAnnotations.modifiers(),
                modifiersAnnotations.annotations(),
                varType,
                false,
                new NodeList<>(),
                name);
        parameter.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return parameter;
    }

    private Statement resolveLambdaBody(Mvel3Parser.LambdaBodyContext ctx) {
        if (ctx.block() != null) {
            return (Statement) visit(ctx.block());
        }

        Expression expression = (Expression) visit(ctx.expression());
        ExpressionStmt expressionStmt = new ExpressionStmt(expression);
        expressionStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return expressionStmt;
    }

    private ModuleDirective processModuleDirective(Mvel3Parser.ModuleDirectiveContext ctx) {
        if (ctx.REQUIRES() != null) {
            NodeList<Modifier> modifiers = new NodeList<>();
            if (ctx.requiresModifier() != null) {
                for (Mvel3Parser.RequiresModifierContext modCtx : ctx.requiresModifier()) {
                    if (modCtx.TRANSITIVE() != null) {
                        modifiers.add(new Modifier(Modifier.Keyword.TRANSITIVE));
                    } else if (modCtx.STATIC() != null) {
                        modifiers.add(new Modifier(Modifier.Keyword.STATIC));
                    }
                }
            }
            Name name = StaticJavaParser.parseName(ctx.qualifiedName(0).getText());
            return new ModuleRequiresDirective(modifiers, name);
        } else if (ctx.EXPORTS() != null) {
            Name name = StaticJavaParser.parseName(ctx.qualifiedName(0).getText());
            NodeList<Name> moduleNames = new NodeList<>();
            if (ctx.TO() != null && ctx.qualifiedName().size() > 1) {
                moduleNames.add(StaticJavaParser.parseName(ctx.qualifiedName(1).getText()));
            }
            return new ModuleExportsDirective(name, moduleNames);
        } else if (ctx.OPENS() != null) {
            Name name = StaticJavaParser.parseName(ctx.qualifiedName(0).getText());
            NodeList<Name> moduleNames = new NodeList<>();
            if (ctx.TO() != null && ctx.qualifiedName().size() > 1) {
                moduleNames.add(StaticJavaParser.parseName(ctx.qualifiedName(1).getText()));
            }
            return new ModuleOpensDirective(name, moduleNames);
        } else if (ctx.USES() != null) {
            Name name = StaticJavaParser.parseName(ctx.qualifiedName(0).getText());
            return new ModuleUsesDirective(name);
        } else if (ctx.PROVIDES() != null) {
            Name name = StaticJavaParser.parseName(ctx.qualifiedName(0).getText());
            NodeList<Name> withNames = new NodeList<>();
            if (ctx.qualifiedName().size() > 1) {
                withNames.add(StaticJavaParser.parseName(ctx.qualifiedName(1).getText()));
            }
            return new ModuleProvidesDirective(name, withNames);
        }
        throw new IllegalArgumentException("Unknown module directive: " + ctx.getText());
    }

    /**
     * The Java grammar we inherit emits shift operators as separate '<' and '>' tokens, so
     * {@code ctx.bop} remains {@code null}. JavaCC still produces "<<", ">>", ">>>", so we
     * synthesise that text here to keep the generated AST identical to the legacy pipeline.
     */
    private String resolveOperatorText(Mvel3Parser.BinaryOperatorExpressionContext ctx) {
        if (ctx.bop != null) {
            return ctx.bop.getText();
        }

        // This looks odd, but indeed it's expected by JavaParser.g4
        int ltCount = ctx.LT().size();
        int gtCount = ctx.GT().size();

        if (ltCount == 2) {
            return "<<";
        }
        if (gtCount == 3) {
            return ">>>";
        }
        if (gtCount == 2) {
            return ">>";
        }

        throw new IllegalArgumentException("Unknown binary operator: " + ctx.getText());
    }

    private static final class LambdaParametersResult {
        private final NodeList<Parameter> parameters;
        private final boolean enclosingParameters;

        private LambdaParametersResult(NodeList<Parameter> parameters, boolean enclosingParameters) {
            this.parameters = parameters;
            this.enclosingParameters = enclosingParameters;
        }
    }
}
