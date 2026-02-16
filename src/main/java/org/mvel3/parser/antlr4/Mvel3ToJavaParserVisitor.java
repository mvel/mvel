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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.javaparser.JavaToken;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
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
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
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

    /**
     * Create a JavaParser TokenRange from ANTLR ParserRuleContext.
     * This provides proper source location information instead of using TokenRange.INVALID.
     */
    protected TokenRange createTokenRange(ParserRuleContext ctx) {
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

        return cu;
    }
    
    @Override
    public Node visitImportDeclaration(Mvel3Parser.ImportDeclarationContext ctx) {
        String importName = ctx.qualifiedName().getText();
        boolean isStatic = ctx.STATIC() != null;
        boolean isAsterisk = ctx.getChildCount() > 3 && "*".equals(ctx.getChild(ctx.getChildCount() - 2).getText());
        return new ImportDeclaration(importName, isStatic, isAsterisk);
    }

    @Override
    public Node visitTypeDeclaration(Mvel3Parser.TypeDeclarationContext ctx) {
        if (ctx.classDeclaration() != null) {
            return visit(ctx.classDeclaration());
        } else if (ctx.enumDeclaration() != null) {
            return visit(ctx.enumDeclaration());
        }
        // TODO: Handle other type declarations (interface, annotation, record)
        throw new UnsupportedOperationException("Type declaration not yet implemented: " + ctx.getText());
    }
    
    @Override
    public Node visitClassDeclaration(Mvel3Parser.ClassDeclarationContext ctx) {
        String className = ctx.identifier().getText();
        ClassOrInterfaceDeclaration classDecl = new ClassOrInterfaceDeclaration();
        classDecl.setName(className);
        classDecl.setInterface(false);

        // Handle modifiers
        if (ctx.getParent() instanceof Mvel3Parser.TypeDeclarationContext) {
            Mvel3Parser.TypeDeclarationContext parent = (Mvel3Parser.TypeDeclarationContext) ctx.getParent();
            if (parent.classOrInterfaceModifier() != null) {
                ModifiersAnnotations ma = parseClassOrInterfaceModifiers(parent.classOrInterfaceModifier());
                classDecl.setModifiers(ma.modifiers);
                classDecl.setAnnotations(ma.annotations);
            }
        } else if (ctx.getParent() instanceof Mvel3Parser.MemberDeclarationContext) {
            // Nested class inside a class body
            Mvel3Parser.MemberDeclarationContext memberCtx = (Mvel3Parser.MemberDeclarationContext) ctx.getParent();
            if (memberCtx.getParent() instanceof Mvel3Parser.ClassBodyDeclarationContext) {
                Mvel3Parser.ClassBodyDeclarationContext bodyDeclCtx = (Mvel3Parser.ClassBodyDeclarationContext) memberCtx.getParent();
                if (bodyDeclCtx.modifier() != null) {
                    ModifiersAnnotations ma = parseModifiers(bodyDeclCtx.modifier());
                    classDecl.setModifiers(ma.modifiers);
                    classDecl.setAnnotations(ma.annotations);
                }
            }
        } else if (ctx.getParent() instanceof Mvel3Parser.LocalTypeDeclarationContext) {
            Mvel3Parser.LocalTypeDeclarationContext localCtx = (Mvel3Parser.LocalTypeDeclarationContext) ctx.getParent();
            if (localCtx.classOrInterfaceModifier() != null) {
                ModifiersAnnotations ma = parseClassOrInterfaceModifiers(localCtx.classOrInterfaceModifier());
                classDecl.setModifiers(ma.modifiers);
                classDecl.setAnnotations(ma.annotations);
            }
        }

        // Handle type parameters
        if (ctx.typeParameters() != null) {
            classDecl.setTypeParameters(parseTypeParameters(ctx.typeParameters()));
        }

        // Handle extends
        if (ctx.EXTENDS() != null && ctx.typeType() != null) {
            Type extendedType = (Type) visit(ctx.typeType());
            if (extendedType instanceof ClassOrInterfaceType) {
                classDecl.setExtendedTypes(new NodeList<>((ClassOrInterfaceType) extendedType));
            }
        }

        // Handle implements
        if (ctx.IMPLEMENTS() != null && !ctx.typeList().isEmpty()) {
            classDecl.setImplementedTypes(parseTypeList(ctx.typeList(0)));
        }

        // Handle permits (Java 17)
        if (ctx.PERMITS() != null) {
            int permitsIndex = ctx.IMPLEMENTS() != null ? 1 : 0;
            if (ctx.typeList().size() > permitsIndex) {
                classDecl.setPermittedTypes(parseTypeList(ctx.typeList(permitsIndex)));
            }
        }

        // Handle class body
        if (ctx.classBody() != null) {
            visitClassBody(ctx.classBody(), classDecl);
        }

        return classDecl;
    }

    @Override
    public Node visitEnumDeclaration(Mvel3Parser.EnumDeclarationContext ctx) {
        String enumName = ctx.identifier().getText();
        EnumDeclaration enumDecl = new EnumDeclaration(new NodeList<>(), enumName);

        // Handle modifiers
        if (ctx.getParent() instanceof Mvel3Parser.TypeDeclarationContext) {
            Mvel3Parser.TypeDeclarationContext parent = (Mvel3Parser.TypeDeclarationContext) ctx.getParent();
            if (parent.classOrInterfaceModifier() != null) {
                ModifiersAnnotations ma = parseClassOrInterfaceModifiers(parent.classOrInterfaceModifier());
                enumDecl.setModifiers(ma.modifiers);
                enumDecl.setAnnotations(ma.annotations);
            }
        } else if (ctx.getParent() instanceof Mvel3Parser.MemberDeclarationContext) {
            Mvel3Parser.MemberDeclarationContext memberCtx = (Mvel3Parser.MemberDeclarationContext) ctx.getParent();
            if (memberCtx.getParent() instanceof Mvel3Parser.ClassBodyDeclarationContext) {
                Mvel3Parser.ClassBodyDeclarationContext bodyDeclCtx = (Mvel3Parser.ClassBodyDeclarationContext) memberCtx.getParent();
                if (bodyDeclCtx.modifier() != null) {
                    ModifiersAnnotations ma = parseModifiers(bodyDeclCtx.modifier());
                    enumDecl.setModifiers(ma.modifiers);
                    enumDecl.setAnnotations(ma.annotations);
                }
            }
        } else if (ctx.getParent() instanceof Mvel3Parser.LocalTypeDeclarationContext) {
            Mvel3Parser.LocalTypeDeclarationContext localCtx = (Mvel3Parser.LocalTypeDeclarationContext) ctx.getParent();
            if (localCtx.classOrInterfaceModifier() != null) {
                ModifiersAnnotations ma = parseClassOrInterfaceModifiers(localCtx.classOrInterfaceModifier());
                enumDecl.setModifiers(ma.modifiers);
                enumDecl.setAnnotations(ma.annotations);
            }
        }

        // Handle implements
        if (ctx.IMPLEMENTS() != null && ctx.typeList() != null) {
            enumDecl.setImplementedTypes(parseTypeList(ctx.typeList()));
        }

        // Handle enum constants
        if (ctx.enumConstants() != null) {
            NodeList<EnumConstantDeclaration> entries = new NodeList<>();
            for (Mvel3Parser.EnumConstantContext constCtx : ctx.enumConstants().enumConstant()) {
                String constName = constCtx.identifier().getText();
                EnumConstantDeclaration constDecl = new EnumConstantDeclaration(constName);
                constDecl.setTokenRange(createTokenRange(constCtx));

                // Handle arguments
                if (constCtx.arguments() != null) {
                    constDecl.setArguments(parseArguments(constCtx.arguments()));
                }

                // Handle class body on enum constant
                if (constCtx.classBody() != null) {
                    constDecl.setClassBody(parseAnonymousClassBody(constCtx.classBody()));
                }

                entries.add(constDecl);
            }
            enumDecl.setEntries(entries);
        }

        // Handle enum body declarations (methods, fields after the semicolon)
        if (ctx.enumBodyDeclarations() != null) {
            for (Mvel3Parser.ClassBodyDeclarationContext bodyDecl : ctx.enumBodyDeclarations().classBodyDeclaration()) {
                if (bodyDecl.memberDeclaration() != null) {
                    Mvel3Parser.MemberDeclarationContext memberDecl = bodyDecl.memberDeclaration();
                    if (memberDecl.methodDeclaration() != null) {
                        Node method = visitMethodDeclaration(memberDecl.methodDeclaration());
                        if (method instanceof MethodDeclaration) {
                            enumDecl.addMember((MethodDeclaration) method);
                        }
                    } else if (memberDecl.fieldDeclaration() != null) {
                        FieldDeclaration field = (FieldDeclaration) visitFieldDeclaration(memberDecl.fieldDeclaration());
                        enumDecl.addMember(field);
                    } else if (memberDecl.constructorDeclaration() != null) {
                        ConstructorDeclaration constructor = (ConstructorDeclaration) visitConstructorDeclaration(memberDecl.constructorDeclaration());
                        enumDecl.addMember(constructor);
                    }
                }
            }
        }

        enumDecl.setTokenRange(createTokenRange(ctx));
        return enumDecl;
    }

    private void visitClassBody(Mvel3Parser.ClassBodyContext ctx, ClassOrInterfaceDeclaration classDecl) {
        if (ctx.classBodyDeclaration() != null) {
            for (Mvel3Parser.ClassBodyDeclarationContext bodyDecl : ctx.classBodyDeclaration()) {
                if (bodyDecl.memberDeclaration() != null) {
                    Mvel3Parser.MemberDeclarationContext memberDecl = bodyDecl.memberDeclaration();
                    if (memberDecl.methodDeclaration() != null) {
                        Node method = visitMethodDeclaration(memberDecl.methodDeclaration());
                        if (method instanceof MethodDeclaration) {
                            classDecl.addMember((MethodDeclaration) method);
                        }
                    } else if (memberDecl.fieldDeclaration() != null) {
                        FieldDeclaration field = (FieldDeclaration) visitFieldDeclaration(memberDecl.fieldDeclaration());
                        classDecl.addMember(field);
                    } else if (memberDecl.constructorDeclaration() != null) {
                        ConstructorDeclaration constructor = (ConstructorDeclaration) visitConstructorDeclaration(memberDecl.constructorDeclaration());
                        classDecl.addMember(constructor);
                    } else if (memberDecl.genericMethodDeclaration() != null) {
                        MethodDeclaration method = (MethodDeclaration) visitGenericMethodDeclaration(memberDecl.genericMethodDeclaration());
                        classDecl.addMember(method);
                    } else if (memberDecl.genericConstructorDeclaration() != null) {
                        ConstructorDeclaration constructor = (ConstructorDeclaration) visitGenericConstructorDeclaration(memberDecl.genericConstructorDeclaration());
                        classDecl.addMember(constructor);
                    }
                    // TODO: Handle other member types (nested classes, etc.)
                }
            }
        }
    }

    private NodeList<BodyDeclaration<?>> parseAnonymousClassBody(Mvel3Parser.ClassBodyContext ctx) {
        NodeList<BodyDeclaration<?>> members = new NodeList<>();
        if (ctx.classBodyDeclaration() != null) {
            for (Mvel3Parser.ClassBodyDeclarationContext bodyDecl : ctx.classBodyDeclaration()) {
                if (bodyDecl.memberDeclaration() != null) {
                    Mvel3Parser.MemberDeclarationContext memberDecl = bodyDecl.memberDeclaration();
                    if (memberDecl.methodDeclaration() != null) {
                        Node method = visitMethodDeclaration(memberDecl.methodDeclaration());
                        if (method instanceof MethodDeclaration) {
                            members.add((MethodDeclaration) method);
                        }
                    } else if (memberDecl.fieldDeclaration() != null) {
                        FieldDeclaration field = (FieldDeclaration) visitFieldDeclaration(memberDecl.fieldDeclaration());
                        members.add(field);
                    } else if (memberDecl.constructorDeclaration() != null) {
                        ConstructorDeclaration constructor = (ConstructorDeclaration) visitConstructorDeclaration(memberDecl.constructorDeclaration());
                        members.add(constructor);
                    } else if (memberDecl.genericMethodDeclaration() != null) {
                        MethodDeclaration method = (MethodDeclaration) visitGenericMethodDeclaration(memberDecl.genericMethodDeclaration());
                        members.add(method);
                    } else if (memberDecl.genericConstructorDeclaration() != null) {
                        ConstructorDeclaration constructor = (ConstructorDeclaration) visitGenericConstructorDeclaration(memberDecl.genericConstructorDeclaration());
                        members.add(constructor);
                    }
                    // TODO: Handle other member types (nested classes, etc.)
                }
            }
        }
        return members;
    }

    @Override
    public Node visitFieldDeclaration(Mvel3Parser.FieldDeclarationContext ctx) {
        Type fieldType = (Type) visit(ctx.typeType());

        NodeList<VariableDeclarator> declarators = new NodeList<>();
        for (Mvel3Parser.VariableDeclaratorContext declaratorCtx : ctx.variableDeclarators().variableDeclarator()) {
            String varName = declaratorCtx.variableDeclaratorId().identifier().getText();

            VariableDeclarator varDeclarator = new VariableDeclarator(fieldType, varName);
            varDeclarator.setTokenRange(createTokenRange(declaratorCtx));

            if (declaratorCtx.variableInitializer() != null) {
                Expression initializer = (Expression) visit(declaratorCtx.variableInitializer());
                varDeclarator.setInitializer(initializer);
            }

            declarators.add(varDeclarator);
        }

        FieldDeclaration fieldDecl = new FieldDeclaration(new NodeList<>(), declarators);
        fieldDecl.setTokenRange(createTokenRange(ctx));

        // Handle modifiers from parent context
        if (ctx.getParent() instanceof Mvel3Parser.MemberDeclarationContext) {
            Mvel3Parser.MemberDeclarationContext memberCtx = (Mvel3Parser.MemberDeclarationContext) ctx.getParent();
            if (memberCtx.getParent() instanceof Mvel3Parser.ClassBodyDeclarationContext) {
                Mvel3Parser.ClassBodyDeclarationContext bodyDeclCtx = (Mvel3Parser.ClassBodyDeclarationContext) memberCtx.getParent();
                if (bodyDeclCtx.modifier() != null) {
                    ModifiersAnnotations ma = parseModifiers(bodyDeclCtx.modifier());
                    fieldDecl.setModifiers(ma.modifiers);
                    fieldDecl.setAnnotations(ma.annotations);
                }
            }
        }

        return fieldDecl;
    }

    @Override
    public Node visitConstructorDeclaration(Mvel3Parser.ConstructorDeclarationContext ctx) {
        String name = ctx.identifier().getText();

        ConstructorDeclaration constructorDecl = new ConstructorDeclaration(name);

        // Handle parameters
        if (ctx.formalParameters() != null) {
            constructorDecl.setParameters(parseFormalParameters(ctx.formalParameters()));
        }

        // Handle throws clause
        if (ctx.THROWS() != null && ctx.qualifiedNameList() != null) {
            constructorDecl.setThrownExceptions(parseQualifiedNameListAsTypes(ctx.qualifiedNameList()));
        }

        // Handle modifiers from parent context
        if (ctx.getParent() instanceof Mvel3Parser.MemberDeclarationContext) {
            Mvel3Parser.MemberDeclarationContext memberCtx = (Mvel3Parser.MemberDeclarationContext) ctx.getParent();
            if (memberCtx.getParent() instanceof Mvel3Parser.ClassBodyDeclarationContext) {
                Mvel3Parser.ClassBodyDeclarationContext bodyDeclCtx = (Mvel3Parser.ClassBodyDeclarationContext) memberCtx.getParent();
                if (bodyDeclCtx.modifier() != null) {
                    ModifiersAnnotations ma = parseModifiers(bodyDeclCtx.modifier());
                    constructorDecl.setModifiers(ma.modifiers);
                    constructorDecl.setAnnotations(ma.annotations);
                }
            }
        } else if (ctx.getParent() instanceof Mvel3Parser.GenericConstructorDeclarationContext) {
            Mvel3Parser.GenericConstructorDeclarationContext genericCtx = (Mvel3Parser.GenericConstructorDeclarationContext) ctx.getParent();
            if (genericCtx.getParent() instanceof Mvel3Parser.MemberDeclarationContext) {
                Mvel3Parser.MemberDeclarationContext memberCtx = (Mvel3Parser.MemberDeclarationContext) genericCtx.getParent();
                if (memberCtx.getParent() instanceof Mvel3Parser.ClassBodyDeclarationContext) {
                    Mvel3Parser.ClassBodyDeclarationContext bodyDeclCtx = (Mvel3Parser.ClassBodyDeclarationContext) memberCtx.getParent();
                    if (bodyDeclCtx.modifier() != null) {
                        ModifiersAnnotations ma = parseModifiers(bodyDeclCtx.modifier());
                        constructorDecl.setModifiers(ma.modifiers);
                        constructorDecl.setAnnotations(ma.annotations);
                    }
                }
            }
        }

        // Handle constructor body
        if (ctx.block() != null) {
            BlockStmt body = (BlockStmt) visit(ctx.block());
            constructorDecl.setBody(body);
        }

        constructorDecl.setTokenRange(createTokenRange(ctx));
        return constructorDecl;
    }

    @Override
    public Node visitGenericMethodDeclaration(Mvel3Parser.GenericMethodDeclarationContext ctx) {
        MethodDeclaration methodDecl = (MethodDeclaration) visitMethodDeclaration(ctx.methodDeclaration());
        if (ctx.typeParameters() != null) {
            methodDecl.setTypeParameters(parseTypeParameters(ctx.typeParameters()));
        }
        return methodDecl;
    }

    @Override
    public Node visitGenericConstructorDeclaration(Mvel3Parser.GenericConstructorDeclarationContext ctx) {
        ConstructorDeclaration constructorDecl = (ConstructorDeclaration) visitConstructorDeclaration(ctx.constructorDeclaration());
        if (ctx.typeParameters() != null) {
            constructorDecl.setTypeParameters(parseTypeParameters(ctx.typeParameters()));
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
            methodDecl.setParameters(parseFormalParameters(ctx.formalParameters()));
        }

        // Handle throws clause
        if (ctx.THROWS() != null && ctx.qualifiedNameList() != null) {
            methodDecl.setThrownExceptions(parseQualifiedNameListAsTypes(ctx.qualifiedNameList()));
        }

        // Handle modifiers (from parent context)
        if (ctx.getParent() instanceof Mvel3Parser.MemberDeclarationContext) {
            Mvel3Parser.MemberDeclarationContext memberCtx = (Mvel3Parser.MemberDeclarationContext) ctx.getParent();
            if (memberCtx.getParent() instanceof Mvel3Parser.ClassBodyDeclarationContext) {
                Mvel3Parser.ClassBodyDeclarationContext bodyDeclCtx = (Mvel3Parser.ClassBodyDeclarationContext) memberCtx.getParent();
                if (bodyDeclCtx.modifier() != null) {
                    ModifiersAnnotations ma = parseModifiers(bodyDeclCtx.modifier());
                    methodDecl.setModifiers(ma.modifiers);
                    methodDecl.setAnnotations(ma.annotations);
                }
            }
        } else if (ctx.getParent() instanceof Mvel3Parser.GenericMethodDeclarationContext) {
            Mvel3Parser.GenericMethodDeclarationContext genericCtx = (Mvel3Parser.GenericMethodDeclarationContext) ctx.getParent();
            if (genericCtx.getParent() instanceof Mvel3Parser.MemberDeclarationContext) {
                Mvel3Parser.MemberDeclarationContext memberCtx = (Mvel3Parser.MemberDeclarationContext) genericCtx.getParent();
                if (memberCtx.getParent() instanceof Mvel3Parser.ClassBodyDeclarationContext) {
                    Mvel3Parser.ClassBodyDeclarationContext bodyDeclCtx = (Mvel3Parser.ClassBodyDeclarationContext) memberCtx.getParent();
                    if (bodyDeclCtx.modifier() != null) {
                        ModifiersAnnotations ma = parseModifiers(bodyDeclCtx.modifier());
                        methodDecl.setModifiers(ma.modifiers);
                        methodDecl.setAnnotations(ma.annotations);
                    }
                }
            }
        }

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
            methodCall.setArguments(parseArguments(ctx.arguments()));
        }

        methodCall.setTokenRange(createTokenRange(ctx));

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
    public Node visitTernaryExpression(Mvel3Parser.TernaryExpressionContext ctx) {
        Expression condition = (Expression) visit(ctx.expression(0));
        Expression thenExpr = (Expression) visit(ctx.expression(1));
        Expression elseExpr = (Expression) visit(ctx.expression(2));

        ConditionalExpr conditionalExpr = new ConditionalExpr(condition, thenExpr, elseExpr);
        conditionalExpr.setTokenRange(createTokenRange(ctx));
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
            patternExpr.setTokenRange(createTokenRange(patternCtx));

            InstanceOfExpr instanceOfExpr = new InstanceOfExpr(expression, type, patternExpr);
            instanceOfExpr.setTokenRange(createTokenRange(ctx));
            return instanceOfExpr;
        } else {
            // Classic instanceof: expr instanceof Type
            ReferenceType type = (ReferenceType) visit(ctx.typeType());
            InstanceOfExpr instanceOfExpr = new InstanceOfExpr(expression, type);
            instanceOfExpr.setTokenRange(createTokenRange(ctx));
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
        switchExpr.setTokenRange(createTokenRange(ctx));
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
        NodeList<Type> typeArguments = null;
        if (ctx.typeArguments() != null) {
            typeArguments = new NodeList<>();
            for (Mvel3Parser.TypeArgumentContext typeArgCtx : ctx.typeArguments().typeArgument()) {
                typeArguments.add((Type) visit(typeArgCtx));
            }
        }

        MethodReferenceExpr methodRef = new MethodReferenceExpr(scope, typeArguments, identifier);
        methodRef.setTokenRange(createTokenRange(ctx));
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
            fieldAccess.setTokenRange(createTokenRange(ctx));
            associateAntlrTokenWithJPNode(ctx.identifier(), fieldAccess);
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
            // expression.super(args) or expression.super.method(args) or expression.super.field
            Mvel3Parser.SuperSuffixContext suffixCtx = ctx.superSuffix();

            // Build SuperExpr with the scope as type name (e.g., Outer.super)
            // The scope expression should be a name (e.g., NameExpr("Outer"))
            Name typeName = new Name(scope.toString());
            com.github.javaparser.ast.expr.SuperExpr superExpr = new com.github.javaparser.ast.expr.SuperExpr(typeName);

            if (suffixCtx.arguments() != null && suffixCtx.identifier() == null) {
                // expression.super(args) — super constructor invocation
                // Represented as MethodCallExpr with super as scope
                NodeList<Expression> args = parseArguments(suffixCtx.arguments());
                MethodCallExpr methodCall = new MethodCallExpr(superExpr, "super");
                methodCall.setArguments(args);
                methodCall.setTokenRange(createTokenRange(ctx));
                return methodCall;
            } else if (suffixCtx.identifier() != null) {
                String memberName = suffixCtx.identifier().getText();
                if (suffixCtx.arguments() != null) {
                    // expression.super.method(args)
                    NodeList<Expression> args = parseArguments(suffixCtx.arguments());
                    MethodCallExpr methodCall = new MethodCallExpr(superExpr, memberName);
                    methodCall.setArguments(args);
                    // Handle type arguments if present
                    if (suffixCtx.typeArguments() != null) {
                        NodeList<Type> typeArgs = new NodeList<>();
                        for (Mvel3Parser.TypeArgumentContext typeArgCtx : suffixCtx.typeArguments().typeArgument()) {
                            typeArgs.add((Type) visit(typeArgCtx));
                        }
                        methodCall.setTypeArguments(typeArgs);
                    }
                    methodCall.setTokenRange(createTokenRange(ctx));
                    return methodCall;
                } else {
                    // expression.super.field
                    FieldAccessExpr fieldAccess = new FieldAccessExpr(superExpr, memberName);
                    fieldAccess.setTokenRange(createTokenRange(ctx));
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
                    NodeList<Type> typeArgs = new NodeList<>();
                    for (Mvel3Parser.TypeTypeContext typeCtx : diamondCtx.nonWildcardTypeArguments().typeList().typeType()) {
                        typeArgs.add((Type) visit(typeCtx));
                    }
                    type.setTypeArguments(typeArgs);
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
                anonymousClassBody = parseAnonymousClassBody(innerCtx.classCreatorRest().classBody());
            }

            ObjectCreationExpr objectCreation = new ObjectCreationExpr(scope, type, null, arguments, anonymousClassBody);
            objectCreation.setTokenRange(createTokenRange(ctx));
            return objectCreation;
        } else if (ctx.explicitGenericInvocation() != null) {
            // expression.<Type>method(args) — explicit generic invocation
            Mvel3Parser.ExplicitGenericInvocationContext egiCtx = ctx.explicitGenericInvocation();

            // Parse type arguments from nonWildcardTypeArguments: '<' typeList '>'
            NodeList<Type> typeArgs = new NodeList<>();
            for (Mvel3Parser.TypeTypeContext typeCtx : egiCtx.nonWildcardTypeArguments().typeList().typeType()) {
                typeArgs.add((Type) visit(typeCtx));
            }

            Mvel3Parser.ExplicitGenericInvocationSuffixContext suffixCtx = egiCtx.explicitGenericInvocationSuffix();
            if (suffixCtx.identifier() != null) {
                // <Type>method(args)
                String methodName = suffixCtx.identifier().getText();
                NodeList<Expression> args = parseArguments(suffixCtx.arguments());
                MethodCallExpr methodCall = new MethodCallExpr(scope, typeArgs, methodName, args);
                methodCall.setTokenRange(createTokenRange(ctx));
                return methodCall;
            } else if (suffixCtx.SUPER() != null) {
                // <Type>super(args) or <Type>super.method(args)
                // TODO: Handle super suffix with type arguments
                throw new UnsupportedOperationException("Generic super invocation not yet implemented");
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
            nameExpr.setTokenRange(createTokenRange(ctx));
            associateAntlrTokenWithJPNode(ctx.identifier(), nameExpr);
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
            classExpr.setTokenRange(createTokenRange(ctx));
            return classExpr;
        }

        if (ctx.nonWildcardTypeArguments() != null) {
            // nonWildcardTypeArguments (explicitGenericInvocationSuffix | THIS arguments)
            NodeList<Type> typeArgs = new NodeList<>();
            for (Mvel3Parser.TypeTypeContext typeCtx : ctx.nonWildcardTypeArguments().typeList().typeType()) {
                typeArgs.add((Type) visit(typeCtx));
            }

            if (ctx.explicitGenericInvocationSuffix() != null) {
                Mvel3Parser.ExplicitGenericInvocationSuffixContext suffixCtx = ctx.explicitGenericInvocationSuffix();
                if (suffixCtx.identifier() != null) {
                    // <Type>method(args) — generic method call without scope
                    String methodName = suffixCtx.identifier().getText();
                    NodeList<Expression> args = parseArguments(suffixCtx.arguments());
                    MethodCallExpr methodCall = new MethodCallExpr(null, typeArgs, methodName, args);
                    methodCall.setTokenRange(createTokenRange(ctx));
                    return methodCall;
                }
                // <Type>super(...) — handled as explicit constructor invocation
                // Falls through to visitChildren for now
            } else if (ctx.THIS() != null && ctx.arguments() != null) {
                // <Type>this(args) — explicit constructor invocation with type arguments
                NodeList<Expression> args = parseArguments(ctx.arguments());
                MethodCallExpr thisCall = new MethodCallExpr(null, typeArgs, "this", args);
                thisCall.setTokenRange(createTokenRange(ctx));
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
        inlineCastExpr.setTokenRange(createTokenRange(ctx));

        associateAntlrTokenWithJPNode(ctx, inlineCastExpr);

        if (ctx.identifier() != null) {
            String name = ctx.identifier().getText();
            if (ctx.arguments() != null) {
                MethodCallExpr methodCall = new MethodCallExpr(inlineCastExpr, name);
                methodCall.setTokenRange(createTokenRange(ctx));
                methodCall.setArguments(parseArguments(ctx.arguments()));
                return methodCall;
            }

            FieldAccessExpr fieldAccess = new FieldAccessExpr(inlineCastExpr, name);
            fieldAccess.setTokenRange(createTokenRange(ctx));
            return fieldAccess;
        }

        if (ctx.LBRACK() != null) {
            Expression indexExpr = (Expression) visit(ctx.expression(1));
            MethodCallExpr methodCall = new MethodCallExpr(inlineCastExpr, "get");
            methodCall.setTokenRange(createTokenRange(ctx));
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
                element.setTokenRange(createTokenRange(elementCtx));
                elements.add(element);
            }
        }

        ListCreationLiteralExpression listExpr = new ListCreationLiteralExpression(elements);
        listExpr.setTokenRange(createTokenRange(ctx));
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
            mapExpr.setTokenRange(createTokenRange(ctx));
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
                pair.setTokenRange(createTokenRange(entryCtx));
                entries.add(pair);
            }
        }

        MapCreationLiteralExpression mapExpr = new MapCreationLiteralExpression(entries);
        mapExpr.setTokenRange(createTokenRange(ctx));
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
            NodeList<Type> typeArguments = new NodeList<>();
            if (ctx.typeArguments() != null) {
                for (Mvel3Parser.TypeArgumentContext typeArgCtx : ctx.typeArguments().typeArgument()) {
                    typeArguments.add((Type) visit(typeArgCtx));
                }
            }

            NullSafeMethodCallExpr methodCall = new NullSafeMethodCallExpr(
                scope,
                typeArguments.isEmpty() ? null : typeArguments,
                name,
                arguments
            );
            methodCall.setTokenRange(createTokenRange(ctx));
            return methodCall;
        } else {
            // Field access: $p!.name
            NullSafeFieldAccessExpr fieldAccess = new NullSafeFieldAccessExpr(scope, name);
            fieldAccess.setTokenRange(createTokenRange(ctx));
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
            intersectionType.setTokenRange(createTokenRange(ctx));
            targetType = intersectionType;
        }

        Expression expression = (Expression) visit(ctx.expression());

        CastExpr castExpr = new CastExpr(targetType, expression);
        castExpr.setTokenRange(createTokenRange(ctx));
        return castExpr;
    }

    @Override
    public Node visitLambdaExpression(Mvel3Parser.LambdaExpressionContext ctx) {
        LambdaParametersResult parametersResult = resolveLambdaParameters(ctx.lambdaParameters());
        Statement body = resolveLambdaBody(ctx.lambdaBody());

        LambdaExpr lambdaExpr = new LambdaExpr(parametersResult.parameters, body, parametersResult.enclosingParameters);
        lambdaExpr.setTokenRange(createTokenRange(ctx));
        return lambdaExpr;
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
        } else if (ctx.HEX_LITERAL() != null) {
            String text = ctx.HEX_LITERAL().getText();
            if (text.endsWith("L") || text.endsWith("l")) {
                LongLiteralExpr longLiteral = new LongLiteralExpr(text);
                longLiteral.setTokenRange(createTokenRange(ctx));
                return longLiteral;
            } else {
                IntegerLiteralExpr integerLiteral = new IntegerLiteralExpr(text);
                integerLiteral.setTokenRange(createTokenRange(ctx));
                return integerLiteral;
            }
        } else if (ctx.OCT_LITERAL() != null) {
            String text = ctx.OCT_LITERAL().getText();
            if (text.endsWith("L") || text.endsWith("l")) {
                LongLiteralExpr longLiteral = new LongLiteralExpr(text);
                longLiteral.setTokenRange(createTokenRange(ctx));
                return longLiteral;
            } else {
                IntegerLiteralExpr integerLiteral = new IntegerLiteralExpr(text);
                integerLiteral.setTokenRange(createTokenRange(ctx));
                return integerLiteral;
            }
        } else if (ctx.BINARY_LITERAL() != null) {
            String text = ctx.BINARY_LITERAL().getText();
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
        } else if (ctx.HEX_FLOAT_LITERAL() != null) {
            DoubleLiteralExpr doubleLiteral = new DoubleLiteralExpr(ctx.HEX_FLOAT_LITERAL().getText());
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
        } else if (ctx.temporalLiteral() != null) {
            return buildTemporalLiteral(ctx.temporalLiteral());
        }
        
        throw new IllegalArgumentException("Unknown literal type: " + ctx.getText());
    }

    private TemporalLiteralExpr buildTemporalLiteral(Mvel3Parser.TemporalLiteralContext ctx) {
        NodeList<TemporalChunkExpr> chunks = new NodeList<>();
        for (Mvel3Parser.TemporalLiteralChunkContext chunkCtx : ctx.temporalLiteralChunk()) {
            chunks.add(buildTemporalLiteralChunk(chunkCtx));
        }
        TemporalLiteralExpr temporalLiteralExpr = new TemporalLiteralExpr(createTokenRange(ctx), chunks);
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

        return new TemporalLiteralChunkExpr(createTokenRange(ctx), stripTimeUnit(token.getText()), timeUnit);
    }

    private String stripTimeUnit(String text) {
        // Remove the time unit suffix (e.g., "m", "s", "h", "d", "ms")
        return text.replaceAll("[a-zA-Z]+$", "");
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
        } else if (ctx.DO() != null) {
            // Handle do-while statement: DO statement WHILE parExpression ';'
            Statement body = (Statement) visit(ctx.statement(0));
            Expression condition = (Expression) visit(ctx.parExpression().expression());

            DoStmt doStmt = new DoStmt(body, condition);
            doStmt.setTokenRange(createTokenRange(ctx));
            return doStmt;
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
        } else if (ctx.TRY() != null) {
            // Handle try statement
            // Grammar: TRY block (catchClause+ finallyBlock? | finallyBlock)
            //        | TRY resourceSpecification block catchClause* finallyBlock?
            NodeList<Expression> resources = new NodeList<>();
            if (ctx.resourceSpecification() != null) {
                resources = parseResources(ctx.resourceSpecification());
            }

            BlockStmt tryBlock = (BlockStmt) visit(ctx.block());

            NodeList<CatchClause> catchClauses = new NodeList<>();
            if (ctx.catchClause() != null) {
                for (Mvel3Parser.CatchClauseContext catchCtx : ctx.catchClause()) {
                    catchClauses.add(parseCatchClause(catchCtx));
                }
            }

            BlockStmt finallyBlock = null;
            if (ctx.finallyBlock() != null) {
                finallyBlock = (BlockStmt) visit(ctx.finallyBlock().block());
            }

            TryStmt tryStmt = new TryStmt(resources, tryBlock, catchClauses, finallyBlock);
            tryStmt.setTokenRange(createTokenRange(ctx));
            return tryStmt;
        } else if (ctx.THROW() != null) {
            // Handle throw statement: THROW expression ';'
            Expression expr = (Expression) visit(ctx.expression(0));
            ThrowStmt throwStmt = new ThrowStmt(expr);
            throwStmt.setTokenRange(createTokenRange(ctx));
            return throwStmt;
        } else if (ctx.BREAK() != null) {
            // Handle break statement: BREAK identifier? ';'
            BreakStmt breakStmt = new BreakStmt();
            if (ctx.identifier() != null) {
                breakStmt.setLabel(new SimpleName(ctx.identifier().getText()));
            }
            breakStmt.setTokenRange(createTokenRange(ctx));
            return breakStmt;
        } else if (ctx.CONTINUE() != null) {
            // Handle continue statement: CONTINUE identifier? ';'
            ContinueStmt continueStmt = new ContinueStmt();
            if (ctx.identifier() != null) {
                continueStmt.setLabel(new SimpleName(ctx.identifier().getText()));
            }
            continueStmt.setTokenRange(createTokenRange(ctx));
            return continueStmt;
        } else if (ctx.SYNCHRONIZED() != null) {
            // Handle synchronized block: SYNCHRONIZED parExpression block
            Expression expression = (Expression) visit(ctx.parExpression().expression());
            BlockStmt body = (BlockStmt) visit(ctx.block());
            SynchronizedStmt synchronizedStmt = new SynchronizedStmt(expression, body);
            synchronizedStmt.setTokenRange(createTokenRange(ctx));
            return synchronizedStmt;
        } else if (ctx.ASSERT() != null) {
            // Handle assert statement: ASSERT expression (':' expression)? ';'
            Expression check = (Expression) visit(ctx.expression(0));
            Expression message = null;
            if (ctx.expression().size() > 1) {
                message = (Expression) visit(ctx.expression(1));
            }
            AssertStmt assertStmt = new AssertStmt(check, message);
            assertStmt.setTokenRange(createTokenRange(ctx));
            return assertStmt;
        } else if (ctx.identifierLabel != null) {
            // Handle labeled statement: identifierLabel = identifier ':' statement
            SimpleName label = new SimpleName(ctx.identifierLabel.getText());
            Statement innerStmt = (Statement) visit(ctx.statement(0));
            LabeledStmt labeledStmt = new LabeledStmt(label, innerStmt);
            labeledStmt.setTokenRange(createTokenRange(ctx));
            return labeledStmt;
        } else if (ctx.YIELD() != null) {
            // Handle yield statement: YIELD expression ';' (Java 17)
            Expression expr = (Expression) visit(ctx.expression(0));
            YieldStmt yieldStmt = new YieldStmt(expr);
            yieldStmt.setTokenRange(createTokenRange(ctx));
            return yieldStmt;
        } else if (ctx.switchExpression() != null) {
            // Handle switch expression used as statement: switchExpression ';'?
            SwitchExpr switchExpr = (SwitchExpr) visit(ctx.switchExpression());
            ExpressionStmt exprStmt = new ExpressionStmt(switchExpr);
            exprStmt.setTokenRange(createTokenRange(ctx));
            return exprStmt;
        } else if (ctx.SEMI() != null) {
            // Handle empty statement: SEMI (bare ';')
            EmptyStmt emptyStmt = new EmptyStmt();
            emptyStmt.setTokenRange(createTokenRange(ctx));
            return emptyStmt;
        }
        // For now, fall back to default behavior
        return visitChildren(ctx);
    }
    
    @Override
    public Node visitFormalParameter(Mvel3Parser.FormalParameterContext ctx) {
        ModifiersAnnotations modifiersAnnotations = parseVariableModifiers(ctx.variableModifier());
        Type type = (Type) visit(ctx.typeType());
        Type adjustedType = applyArrayDimensions(type, ctx.variableDeclaratorId());
        SimpleName name = createSimpleName(ctx.variableDeclaratorId().identifier());

        Parameter parameter = new Parameter(modifiersAnnotations.modifiers,
                modifiersAnnotations.annotations,
                adjustedType,
                false,
                new NodeList<>(),
                name);
        parameter.setTokenRange(createTokenRange(ctx));
        return parameter;
    }

    @Override
    public Node visitLastFormalParameter(Mvel3Parser.LastFormalParameterContext ctx) {
        ModifiersAnnotations modifiersAnnotations = parseVariableModifiers(ctx.variableModifier());
        Type type = (Type) visit(ctx.typeType());
        boolean isVarArgs = ctx.ELLIPSIS() != null;

        NodeList<AnnotationExpr> varArgsAnnotations = new NodeList<>();
        if (ctx.annotation() != null) {
            for (Mvel3Parser.AnnotationContext annotationContext : ctx.annotation()) {
                varArgsAnnotations.add(parseAnnotationExpr(annotationContext));
            }
        }

        Type adjustedType = applyArrayDimensions(type, ctx.variableDeclaratorId());
        SimpleName name = createSimpleName(ctx.variableDeclaratorId().identifier());

        Parameter parameter = new Parameter(modifiersAnnotations.modifiers,
                modifiersAnnotations.annotations,
                adjustedType,
                isVarArgs,
                varArgsAnnotations,
                name);
        parameter.setTokenRange(createTokenRange(ctx));
        return parameter;
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
    public Node visitWithStatement(Mvel3Parser.WithStatementContext ctx) {
        String targetName = ctx.identifier().getText();
        NameExpr target = new NameExpr(targetName);
        target.setTokenRange(createTokenRange(ctx));

        NodeList<Statement> statements = new NodeList<>();
        for (Mvel3Parser.StatementContext stmtCtx : ctx.statement()) {
            Statement stmt = (Statement) visit(stmtCtx);
            statements.add(stmt);
        }

        return new WithStatement(createTokenRange(ctx), target, statements);
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

            // Handle anonymous class body if present
            NodeList<BodyDeclaration<?>> anonymousClassBody = null;
            if (ctx.classCreatorRest().classBody() != null) {
                anonymousClassBody = parseAnonymousClassBody(ctx.classCreatorRest().classBody());
            }

            // Create ObjectCreationExpr
            ObjectCreationExpr objectCreation = new ObjectCreationExpr(null, (ClassOrInterfaceType) type, null, arguments, anonymousClassBody);
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

    protected NodeList<Expression> parseArguments(Mvel3Parser.ArgumentsContext ctx) {
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
            }
            // TODO: Handle guardedPattern if needed
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
        entry.setTokenRange(createTokenRange(ruleCtx));
        return entry;
    }

    private CatchClause parseCatchClause(Mvel3Parser.CatchClauseContext ctx) {
        // catchClause: CATCH '(' variableModifier* catchType identifier ')' block
        // catchType: qualifiedName ('|' qualifiedName)*
        ModifiersAnnotations modifiersAnnotations = parseVariableModifiers(ctx.variableModifier());

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
        Parameter parameter = new Parameter(modifiersAnnotations.modifiers,
                modifiersAnnotations.annotations,
                catchType,
                false,
                new NodeList<>(),
                name);
        parameter.setTokenRange(createTokenRange(ctx));

        BlockStmt body = (BlockStmt) visit(ctx.block());

        CatchClause catchClause = new CatchClause(parameter, body);
        catchClause.setTokenRange(createTokenRange(ctx));
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
                    VariableDeclarationExpr varDecl = new VariableDeclarationExpr(declarator);
                    varDecl.setTokenRange(createTokenRange(resourceCtx));
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
            NodeList<Type> typeArgs = new NodeList<>();
            for (Mvel3Parser.TypeArgumentContext typeArgCtx : ctx.typeArguments().typeArgument()) {
                typeArgs.add((Type) visit(typeArgCtx));
            }
            type.setTypeArguments(typeArgs);
        }

        type.setTokenRange(createTokenRange(ctx));
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
            parameters.addAll(collectFormalParameters(ctx.formalParameterList()));
        } else if (ctx.lambdaLVTIList() != null) {
            parameters.addAll(collectLambdaLVTIParameters(ctx.lambdaLVTIList()));
        } else if (ctx.identifier() != null && !ctx.identifier().isEmpty()) {
            for (Mvel3Parser.IdentifierContext identifierContext : ctx.identifier()) {
                parameters.add(createInferredParameter(identifierContext));
            }
        }

        return new LambdaParametersResult(parameters, enclosingParameters);
    }

    private NodeList<Parameter> parseFormalParameters(Mvel3Parser.FormalParametersContext ctx) {
        if (ctx == null || ctx.formalParameterList() == null) {
            return new NodeList<>();
        }
        return collectFormalParameters(ctx.formalParameterList());
    }

    private NodeList<ReferenceType> parseQualifiedNameListAsTypes(Mvel3Parser.QualifiedNameListContext ctx) {
        NodeList<ReferenceType> types = new NodeList<>();
        if (ctx != null && ctx.qualifiedName() != null) {
            for (Mvel3Parser.QualifiedNameContext qn : ctx.qualifiedName()) {
                ClassOrInterfaceType type = new ClassOrInterfaceType(null, qn.getText());
                type.setTokenRange(createTokenRange(qn));
                types.add(type);
            }
        }
        return types;
    }

    private NodeList<ClassOrInterfaceType> parseTypeList(Mvel3Parser.TypeListContext ctx) {
        NodeList<ClassOrInterfaceType> types = new NodeList<>();
        if (ctx != null && ctx.typeType() != null) {
            for (Mvel3Parser.TypeTypeContext typeCtx : ctx.typeType()) {
                Type type = (Type) visit(typeCtx);
                if (type instanceof ClassOrInterfaceType) {
                    types.add((ClassOrInterfaceType) type);
                }
            }
        }
        return types;
    }

    private NodeList<TypeParameter> parseTypeParameters(Mvel3Parser.TypeParametersContext ctx) {
        NodeList<TypeParameter> typeParams = new NodeList<>();
        if (ctx == null || ctx.typeParameter() == null) {
            return typeParams;
        }
        for (Mvel3Parser.TypeParameterContext tpCtx : ctx.typeParameter()) {
            String name = tpCtx.identifier().getText();
            NodeList<ClassOrInterfaceType> bounds = new NodeList<>();
            if (tpCtx.typeBound() != null) {
                for (Mvel3Parser.TypeTypeContext boundCtx : tpCtx.typeBound().typeType()) {
                    Type boundType = (Type) visit(boundCtx);
                    if (boundType instanceof ClassOrInterfaceType) {
                        bounds.add((ClassOrInterfaceType) boundType);
                    }
                }
            }
            TypeParameter typeParam = new TypeParameter(name, bounds);
            typeParam.setTokenRange(createTokenRange(tpCtx));
            typeParams.add(typeParam);
        }
        return typeParams;
    }

    private NodeList<Parameter> collectFormalParameters(Mvel3Parser.FormalParameterListContext ctx) {
        NodeList<Parameter> parameters = new NodeList<>();
        if (ctx == null) {
            return parameters;
        }

        if (ctx.formalParameter() != null) {
            for (Mvel3Parser.FormalParameterContext formalParameterContext : ctx.formalParameter()) {
                parameters.add((Parameter) visit(formalParameterContext));
            }
        }

        if (ctx.lastFormalParameter() != null) {
            parameters.add((Parameter) visit(ctx.lastFormalParameter()));
        }

        return parameters;
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
        unknownType.setTokenRange(createTokenRange(identifierContext));

        SimpleName name = createSimpleName(identifierContext);

        Parameter parameter = new Parameter(new NodeList<>(), new NodeList<>(), unknownType, false, new NodeList<>(), name);
        parameter.setTokenRange(createTokenRange(identifierContext));
        return parameter;
    }

    private Parameter createLambdaVarParameter(Mvel3Parser.LambdaLVTIParameterContext ctx) {
        ModifiersAnnotations modifiersAnnotations = parseVariableModifiers(ctx.variableModifier());
        VarType varType = new VarType();
        varType.setTokenRange(createTokenRange(ctx));

        SimpleName name = createSimpleName(ctx.identifier());

        Parameter parameter = new Parameter(modifiersAnnotations.modifiers,
                modifiersAnnotations.annotations,
                varType,
                false,
                new NodeList<>(),
                name);
        parameter.setTokenRange(createTokenRange(ctx));
        return parameter;
    }

    private Statement resolveLambdaBody(Mvel3Parser.LambdaBodyContext ctx) {
        if (ctx.block() != null) {
            return (Statement) visit(ctx.block());
        }

        Expression expression = (Expression) visit(ctx.expression());
        ExpressionStmt expressionStmt = new ExpressionStmt(expression);
        expressionStmt.setTokenRange(createTokenRange(ctx));
        return expressionStmt;
    }

    private ModifiersAnnotations parseClassOrInterfaceModifiers(List<Mvel3Parser.ClassOrInterfaceModifierContext> modifierContexts) {
        NodeList<Modifier> modifiers = new NodeList<>();
        NodeList<AnnotationExpr> annotations = new NodeList<>();

        if (modifierContexts != null) {
            for (Mvel3Parser.ClassOrInterfaceModifierContext modCtx : modifierContexts) {
                if (modCtx.annotation() != null) {
                    annotations.add(parseAnnotationExpr(modCtx.annotation()));
                } else if (modCtx.PUBLIC() != null) {
                    modifiers.add(createModifier(Modifier.Keyword.PUBLIC, modCtx));
                } else if (modCtx.PROTECTED() != null) {
                    modifiers.add(createModifier(Modifier.Keyword.PROTECTED, modCtx));
                } else if (modCtx.PRIVATE() != null) {
                    modifiers.add(createModifier(Modifier.Keyword.PRIVATE, modCtx));
                } else if (modCtx.STATIC() != null) {
                    modifiers.add(createModifier(Modifier.Keyword.STATIC, modCtx));
                } else if (modCtx.ABSTRACT() != null) {
                    modifiers.add(createModifier(Modifier.Keyword.ABSTRACT, modCtx));
                } else if (modCtx.FINAL() != null) {
                    modifiers.add(createModifier(Modifier.Keyword.FINAL, modCtx));
                } else if (modCtx.STRICTFP() != null) {
                    modifiers.add(createModifier(Modifier.Keyword.STRICTFP, modCtx));
                } else if (modCtx.SEALED() != null) {
                    modifiers.add(createModifier(Modifier.Keyword.SEALED, modCtx));
                } else if (modCtx.NON_SEALED() != null) {
                    modifiers.add(createModifier(Modifier.Keyword.NON_SEALED, modCtx));
                }
            }
        }

        return new ModifiersAnnotations(modifiers, annotations);
    }

    private ModifiersAnnotations parseModifiers(List<Mvel3Parser.ModifierContext> modifierContexts) {
        NodeList<Modifier> modifiers = new NodeList<>();
        NodeList<AnnotationExpr> annotations = new NodeList<>();

        if (modifierContexts != null) {
            for (Mvel3Parser.ModifierContext modCtx : modifierContexts) {
                if (modCtx.classOrInterfaceModifier() != null) {
                    ModifiersAnnotations inner = parseClassOrInterfaceModifiers(List.of(modCtx.classOrInterfaceModifier()));
                    modifiers.addAll(inner.modifiers);
                    annotations.addAll(inner.annotations);
                } else if (modCtx.NATIVE() != null) {
                    modifiers.add(createModifier(Modifier.Keyword.NATIVE, modCtx));
                } else if (modCtx.SYNCHRONIZED() != null) {
                    modifiers.add(createModifier(Modifier.Keyword.SYNCHRONIZED, modCtx));
                } else if (modCtx.TRANSIENT() != null) {
                    modifiers.add(createModifier(Modifier.Keyword.TRANSIENT, modCtx));
                } else if (modCtx.VOLATILE() != null) {
                    modifiers.add(createModifier(Modifier.Keyword.VOLATILE, modCtx));
                }
            }
        }

        return new ModifiersAnnotations(modifiers, annotations);
    }

    private Modifier createModifier(Modifier.Keyword keyword, ParserRuleContext ctx) {
        Modifier modifier = new Modifier(keyword);
        modifier.setTokenRange(createTokenRange(ctx));
        return modifier;
    }

    private ModifiersAnnotations parseVariableModifiers(List<Mvel3Parser.VariableModifierContext> modifierContexts) {
        NodeList<Modifier> modifiers = new NodeList<>();
        NodeList<AnnotationExpr> annotations = new NodeList<>();

        if (modifierContexts != null) {
            for (Mvel3Parser.VariableModifierContext modifierContext : modifierContexts) {
                if (modifierContext.FINAL() != null) {
                    Modifier finalModifier = Modifier.finalModifier();
                    finalModifier.setTokenRange(createTokenRange(modifierContext));
                    modifiers.add(finalModifier);
                } else if (modifierContext.annotation() != null) {
                    annotations.add(parseAnnotationExpr(modifierContext.annotation()));
                }
            }
        }

        return new ModifiersAnnotations(modifiers, annotations);
    }

    private Type applyArrayDimensions(Type baseType, Mvel3Parser.VariableDeclaratorIdContext idContext) {
        if (idContext == null) {
            return baseType;
        }

        int dimensions = idContext.LBRACK() != null ? idContext.LBRACK().size() : 0;

        Type result = baseType;
        for (int i = 0; i < dimensions; i++) {
            ArrayType arrayType = new ArrayType(result);
            arrayType.setTokenRange(createTokenRange(idContext));
            result = arrayType;
        }

        return result;
    }

    private SimpleName createSimpleName(Mvel3Parser.IdentifierContext identifierContext) {
        SimpleName name = new SimpleName(identifierContext.getText());
        name.setTokenRange(createTokenRange(identifierContext));
        return name;
    }

    private AnnotationExpr parseAnnotationExpr(Mvel3Parser.AnnotationContext ctx) {
        AnnotationExpr annotationExpr = StaticJavaParser.parseAnnotation(ctx.getText());
        annotationExpr.setTokenRange(createTokenRange(ctx));
        return annotationExpr;
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

    private static final class ModifiersAnnotations {
        private final NodeList<Modifier> modifiers;
        private final NodeList<AnnotationExpr> annotations;

        private ModifiersAnnotations(NodeList<Modifier> modifiers, NodeList<AnnotationExpr> annotations) {
            this.modifiers = modifiers;
            this.annotations = annotations;
        }
    }
}
