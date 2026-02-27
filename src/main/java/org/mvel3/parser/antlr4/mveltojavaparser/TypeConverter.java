package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;

import java.util.ArrayList;
import java.util.List;

public final class TypeConverter {

    private TypeConverter() {
    }

    public static Node convertTypeDeclaration(final Mvel3Parser.TypeDeclarationContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        if (ctx.classDeclaration() != null) {
            return convertClassDeclaration(ctx.classDeclaration(), mvel3toJavaParserVisitor);
        } else if (ctx.enumDeclaration() != null) {
            return EnumConverter.convertEnumDeclaration(ctx.enumDeclaration(), mvel3toJavaParserVisitor);
        } else if (ctx.interfaceDeclaration() != null) {
            return InterfaceConverter.convertInterfaceDeclaration(ctx.interfaceDeclaration(), mvel3toJavaParserVisitor);
        } else if (ctx.annotationTypeDeclaration() != null) {
            return AnnotationTypeConverter.convertAnnotationTypeDeclaration(ctx.annotationTypeDeclaration(), mvel3toJavaParserVisitor);
        } else if (ctx.recordDeclaration() != null) {
            return RecordConverter.convertRecordDeclaration(ctx.recordDeclaration(), mvel3toJavaParserVisitor);
        }
        throw new UnsupportedOperationException("Type declaration not yet implemented: " + ctx.getText());
    }

    public static Node convertClassDeclaration(final Mvel3Parser.ClassDeclarationContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        String className = ctx.identifier().getText();
        ClassOrInterfaceDeclaration classDecl = new ClassOrInterfaceDeclaration();
        classDecl.setName(className);
        classDecl.setInterface(false);

        // Handle modifiers
        ModifiersAnnotations classModifiers = ModifiersParser.resolveModifiersFromParent(ctx);
        classDecl.setModifiers(classModifiers.modifiers());
        classDecl.setAnnotations(classModifiers.annotations());

        // Handle type parameters
        if (ctx.typeParameters() != null) {
            classDecl.setTypeParameters(convertTypeParameters(ctx.typeParameters(), mvel3toJavaParserVisitor));
        }

        // Handle extends
        if (ctx.EXTENDS() != null && ctx.typeType() != null) {
            Type extendedType = (Type) TypeConverter.convertTypeType(ctx.typeType(), mvel3toJavaParserVisitor);
            if (extendedType instanceof ClassOrInterfaceType) {
                classDecl.setExtendedTypes(new NodeList<>((ClassOrInterfaceType) extendedType));
            }
        }

        // Handle implements
        if (ctx.IMPLEMENTS() != null && !ctx.typeList().isEmpty()) {
            classDecl.setImplementedTypes(convertTypeList(ctx.typeList(0),  mvel3toJavaParserVisitor));
        }

        // Handle permits (Java 17)
        if (ctx.PERMITS() != null) {
            int permitsIndex = ctx.IMPLEMENTS() != null ? 1 : 0;
            if (ctx.typeList().size() > permitsIndex) {
                classDecl.setPermittedTypes(convertTypeList(ctx.typeList(permitsIndex), mvel3toJavaParserVisitor));
            }
        }

        // Handle class body
        if (ctx.classBody() != null) {
            classDecl.getMembers().addAll(TypeConverter.convertClassBodyDeclarations(ctx.classBody().classBodyDeclaration(), mvel3toJavaParserVisitor));
        }

        return classDecl;
    }

    public static Node convertClassOrInterfaceType(final Mvel3Parser.ClassOrInterfaceTypeContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // Grammar: (identifier typeArguments? '.')* typeIdentifier typeArguments?
        // Walk children to correctly associate each typeArguments with its preceding identifier.
        ClassOrInterfaceType type = null;
        ClassOrInterfaceType currentType = null;

        for (ParseTree child : ctx.children) {
            if (child instanceof Mvel3Parser.IdentifierContext idCtx) {
                currentType = new ClassOrInterfaceType(type, idCtx.getText());
                currentType.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                type = currentType;
            } else if (child instanceof Mvel3Parser.TypeIdentifierContext tiCtx) {
                currentType = new ClassOrInterfaceType(type, tiCtx.getText());
                currentType.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
                type = currentType;
            } else if (child instanceof Mvel3Parser.TypeArgumentsContext taCtx) {
                // Apply type arguments to the most recently built type
                if (currentType != null) {
                    NodeList<Type> typeArgs = ArgumentsConverter.convertTypeArguments(taCtx, mvel3toJavaParserVisitor);
                    if (!typeArgs.isEmpty()) {
                        currentType.setTypeArguments(typeArgs);
                    }
                }
            }
            // DOT terminals are skipped — scope is handled by constructor chaining
        }

        if (type == null) {
            throw new IllegalArgumentException("Missing typeIdentifier in ClassOrInterfaceType: " + ctx.getText());
        }
        return type;
    }

    /**
     * Process a list of ClassBodyDeclarationContexts and return all member declarations.
     * Used by class bodies, enum bodies, record bodies, and anonymous class bodies.
     */
    public static NodeList<BodyDeclaration<?>> convertClassBodyDeclarations(final List<Mvel3Parser.ClassBodyDeclarationContext> bodyDeclarations,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        NodeList<BodyDeclaration<?>> members = new NodeList<>();
        if (bodyDeclarations != null) {
            for (Mvel3Parser.ClassBodyDeclarationContext bodyDecl : bodyDeclarations) {
                if (bodyDecl.memberDeclaration() != null) {
                    BodyDeclaration<?> member = visitMemberDeclarationNode(bodyDecl.memberDeclaration(), mvel3toJavaParserVisitor);
                    if (member != null) {
                        members.add(member);
                    }
                } else if (bodyDecl.block() != null) {
                    boolean isStatic = bodyDecl.STATIC() != null;
                    BlockStmt block = (BlockStmt) BlockConverter.convertBlock(bodyDecl.block(), mvel3toJavaParserVisitor);
                    InitializerDeclaration initDecl = new InitializerDeclaration(isStatic, block);
                    initDecl.setTokenRange(TokenRangeConverter.createTokenRange(bodyDecl));
                    members.add(initDecl);
                }
            }
        }
        return members;
    }

    public static Node convertClassType(
            final Mvel3Parser.ClassTypeContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // classType: (classOrInterfaceType '.')? annotation* identifier typeArguments?
        ClassOrInterfaceType scope = null;
        if (ctx.classOrInterfaceType() != null) {
            scope = (ClassOrInterfaceType) convertClassOrInterfaceType(ctx.classOrInterfaceType(), mvel3toJavaParserVisitor);
        }

        String name = ctx.identifier().getText();
        ClassOrInterfaceType type = new ClassOrInterfaceType(scope, name);

        if (ctx.typeArguments() != null) {
            type.setTypeArguments(ArgumentsConverter.convertTypeArguments(ctx.typeArguments(), mvel3toJavaParserVisitor));
        }

        type.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return type;
    }

    public static NodeList<BodyDeclaration<?>> convertAnonymousClassBody(final Mvel3Parser.ClassBodyContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        return convertClassBodyDeclarations(ctx.classBodyDeclaration(), mvel3toJavaParserVisitor);
    }

    public static NodeList<ClassOrInterfaceType> convertTypeList(final Mvel3Parser.TypeListContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        NodeList<ClassOrInterfaceType> types = new NodeList<>();
        if (ctx != null && ctx.typeType() != null) {
            for (Mvel3Parser.TypeTypeContext typeCtx : ctx.typeType()) {
                Type type = (Type) TypeConverter.convertTypeType(typeCtx, mvel3toJavaParserVisitor);
                if (type instanceof ClassOrInterfaceType) {
                    types.add((ClassOrInterfaceType) type);
                }
            }
        }
        return types;
    }

    public static NodeList<TypeParameter> convertTypeParameters(final Mvel3Parser.TypeParametersContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        NodeList<TypeParameter> typeParams = new NodeList<>();
        if (ctx == null || ctx.typeParameter() == null) {
            return typeParams;
        }
        for (Mvel3Parser.TypeParameterContext tpCtx : ctx.typeParameter()) {
            String name = tpCtx.identifier().getText();
            NodeList<ClassOrInterfaceType> bounds = new NodeList<>();

            // Separate annotations: before identifier → type param annotations,
            // after EXTENDS → bound annotations (applied to first bound type)
            NodeList<AnnotationExpr> typeParamAnnotations = new NodeList<>();
            NodeList<AnnotationExpr> boundAnnotations = new NodeList<>();
            boolean foundIdentifier = false;
            if (tpCtx.children != null) {
                for (ParseTree child : tpCtx.children) {
                    if (child instanceof Mvel3Parser.AnnotationContext) {
                        if (!foundIdentifier) {
                            typeParamAnnotations.add(ModifiersConverter.convertAnnotationExpr((Mvel3Parser.AnnotationContext) child));
                        } else {
                            boundAnnotations.add(ModifiersConverter.convertAnnotationExpr((Mvel3Parser.AnnotationContext) child));
                        }
                    } else if (child instanceof Mvel3Parser.IdentifierContext) {
                        foundIdentifier = true;
                    }
                }
            }

            if (tpCtx.typeBound() != null) {
                for (int i = 0; i < tpCtx.typeBound().typeType().size(); i++) {
                    Type boundType = (Type) TypeConverter.convertTypeType(tpCtx.typeBound().typeType(i), mvel3toJavaParserVisitor);
                    if (boundType instanceof ClassOrInterfaceType) {
                        // Apply bound annotations to the first bound type
                        if (i == 0 && !boundAnnotations.isEmpty()) {
                            boundType.setAnnotations(boundAnnotations);
                        }
                        bounds.add((ClassOrInterfaceType) boundType);
                    }
                }
            }
            TypeParameter typeParam = new TypeParameter(new SimpleName(name), bounds, typeParamAnnotations);
            typeParam.setTokenRange(TokenRangeConverter.createTokenRange(tpCtx));
            typeParams.add(typeParam);
        }
        return typeParams;
    }

    public static Modifier createModifier(Modifier.Keyword keyword, ParserRuleContext ctx) {
        Modifier modifier = new Modifier(keyword);
        modifier.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return modifier;
    }

    public static NodeList<ReferenceType> convertQualifiedNameListAsTypes(Mvel3Parser.QualifiedNameListContext ctx) {
        NodeList<ReferenceType> types = new NodeList<>();
        if (ctx != null && ctx.qualifiedName() != null) {
            for (Mvel3Parser.QualifiedNameContext qn : ctx.qualifiedName()) {
                ClassOrInterfaceType type = new ClassOrInterfaceType(null, qn.getText());
                type.setTokenRange(TokenRangeConverter.createTokenRange(qn));
                types.add(type);
            }
        }
        return types;
    }

    public static Node convertTypeType(
            final Mvel3Parser.TypeTypeContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // typeType: annotation* (classOrInterfaceType | primitiveType) (annotation* '[' ']')*
        Type baseType = null;

        // Handle different type possibilities
        if (ctx.classOrInterfaceType() != null) {
            baseType = (Type) convertClassOrInterfaceType(ctx.classOrInterfaceType(), mvel3toJavaParserVisitor);
        } else if (ctx.primitiveType() != null) {
            baseType = (Type) convertPrimitiveType(ctx.primitiveType());
        }

        if (baseType == null) {
            // Fall back to default behavior
            return mvel3toJavaParserVisitor.visitChildren(ctx);
        }

        // Walk children to separate type annotations from array dimension annotations
        // Phase 1: annotations before the type node
        // Phase 2: for each '[', collect preceding annotations for that array dimension
        NodeList<AnnotationExpr> typeAnnotations = new NodeList<>();
        List<NodeList<AnnotationExpr>> arrayDimAnnotations = new ArrayList<>();
        boolean foundType = false;
        NodeList<AnnotationExpr> pendingAnnotations = new NodeList<>();

        if (ctx.children != null) {
            for (ParseTree child : ctx.children) {
                if (child instanceof Mvel3Parser.AnnotationContext) {
                    pendingAnnotations.add(ModifiersConverter.convertAnnotationExpr((Mvel3Parser.AnnotationContext) child));
                } else if (child instanceof Mvel3Parser.ClassOrInterfaceTypeContext
                        || child instanceof Mvel3Parser.PrimitiveTypeContext) {
                    typeAnnotations = pendingAnnotations;
                    pendingAnnotations = new NodeList<>();
                    foundType = true;
                } else if (foundType && child instanceof TerminalNode && "[".equals(child.getText())) {
                    arrayDimAnnotations.add(pendingAnnotations);
                    pendingAnnotations = new NodeList<>();
                }
            }
        }

        // Set annotations on the base type
        if (!typeAnnotations.isEmpty()) {
            baseType.setAnnotations(typeAnnotations);
        }

        // Wrap base type in ArrayType for each dimension, with per-dimension annotations
        Type resultType = baseType;
        for (NodeList<AnnotationExpr> dimAnnotations : arrayDimAnnotations) {
            resultType = new ArrayType(resultType, ArrayType.Origin.TYPE, dimAnnotations);
        }

        if (resultType instanceof ArrayType) {
            resultType.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        }

        return resultType;
    }

    public static Node convertPrimitiveType(Mvel3Parser.PrimitiveTypeContext ctx) {
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
        primitiveType.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return primitiveType;
    }

    /**
     * Dispatch a single MemberDeclarationContext to the appropriate visitor method.
     * Returns the resulting BodyDeclaration, or null if unhandled.
     */
    private static BodyDeclaration<?> visitMemberDeclarationNode(final Mvel3Parser.MemberDeclarationContext memberDecl,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        if (memberDecl.methodDeclaration() != null) {
            return (MethodDeclaration) MethodConverter.convertMethodDeclaration(memberDecl.methodDeclaration(), mvel3toJavaParserVisitor);
        } else if (memberDecl.fieldDeclaration() != null) {
            return (FieldDeclaration) FieldConverter.convertFieldDeclaration(memberDecl.fieldDeclaration(), mvel3toJavaParserVisitor);
        } else if (memberDecl.constructorDeclaration() != null) {
            return (ConstructorDeclaration) ConstructorConverter.convertConstructorDeclaration(memberDecl.constructorDeclaration(), mvel3toJavaParserVisitor);
        } else if (memberDecl.genericMethodDeclaration() != null) {
            return (MethodDeclaration) MethodConverter.convertGenericMethodDeclaration(memberDecl.genericMethodDeclaration(), mvel3toJavaParserVisitor);
        } else if (memberDecl.genericConstructorDeclaration() != null) {
            return (ConstructorDeclaration) ConstructorConverter.convertGenericConstructorDeclaration(memberDecl.genericConstructorDeclaration(), mvel3toJavaParserVisitor);
        } else if (memberDecl.classDeclaration() != null) {
            return (BodyDeclaration<?>) convertClassDeclaration(memberDecl.classDeclaration(), mvel3toJavaParserVisitor);
        } else if (memberDecl.enumDeclaration() != null) {
            return (BodyDeclaration<?>) EnumConverter.convertEnumDeclaration(memberDecl.enumDeclaration(), mvel3toJavaParserVisitor);
        } else if (memberDecl.interfaceDeclaration() != null) {
            return (BodyDeclaration<?>) InterfaceConverter.convertInterfaceDeclaration(memberDecl.interfaceDeclaration(), mvel3toJavaParserVisitor);
        } else if (memberDecl.annotationTypeDeclaration() != null) {
            return (BodyDeclaration<?>) AnnotationTypeConverter.convertAnnotationTypeDeclaration(memberDecl.annotationTypeDeclaration(), mvel3toJavaParserVisitor);
        } else if (memberDecl.recordDeclaration() != null) {
            return (BodyDeclaration<?>) RecordConverter.convertRecordDeclaration(memberDecl.recordDeclaration(), mvel3toJavaParserVisitor);
        }
        return null;
    }
}
