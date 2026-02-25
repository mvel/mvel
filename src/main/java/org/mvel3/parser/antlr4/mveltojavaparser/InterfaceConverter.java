package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;

import java.util.List;

public final class InterfaceConverter {

    private InterfaceConverter() {
    }

    public static Node convertInterfaceDeclaration(
            final Mvel3Parser.InterfaceDeclarationContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        String interfaceName = ctx.identifier().getText();
        ClassOrInterfaceDeclaration interfaceDecl = new ClassOrInterfaceDeclaration();
        interfaceDecl.setName(interfaceName);
        interfaceDecl.setInterface(true);

        // Handle modifiers
        ModifiersAnnotations interfaceModifiers = ModifiersParser.resolveModifiersFromParent(ctx);
        interfaceDecl.setModifiers(interfaceModifiers.modifiers());
        interfaceDecl.setAnnotations(interfaceModifiers.annotations());

        // Handle type parameters
        if (ctx.typeParameters() != null) {
            interfaceDecl.setTypeParameters(TypeConverter.convertTypeParameters(ctx.typeParameters(), mvel3toJavaParserVisitor));
        }

        // Handle extends (interfaces can extend multiple interfaces)
        if (ctx.EXTENDS() != null && !ctx.typeList().isEmpty()) {
            interfaceDecl.setExtendedTypes(TypeConverter.convertTypeList(ctx.typeList(0), mvel3toJavaParserVisitor));
        }

        // Handle permits (Java 17)
        if (ctx.PERMITS() != null) {
            int permitsIndex = ctx.EXTENDS() != null ? 1 : 0;
            if (ctx.typeList().size() > permitsIndex) {
                interfaceDecl.setPermittedTypes(TypeConverter.convertTypeList(ctx.typeList(permitsIndex), mvel3toJavaParserVisitor));
            }
        }

        // Handle interface body
        if (ctx.interfaceBody() != null) {
            convertInterfaceBody(ctx.interfaceBody(), interfaceDecl, mvel3toJavaParserVisitor);
        }

        interfaceDecl.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return interfaceDecl;
    }

    private static void convertInterfaceBody(
            final Mvel3Parser.InterfaceBodyContext ctx,
            final ClassOrInterfaceDeclaration interfaceDecl,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        if (ctx.interfaceBodyDeclaration() != null) {
            for (Mvel3Parser.InterfaceBodyDeclarationContext bodyDecl : ctx.interfaceBodyDeclaration()) {
                if (bodyDecl.interfaceMemberDeclaration() != null) {
                    Mvel3Parser.InterfaceMemberDeclarationContext memberDecl = bodyDecl.interfaceMemberDeclaration();
                    if (memberDecl.interfaceMethodDeclaration() != null) {
                        MethodDeclaration method = convertInterfaceMethodDeclaration(memberDecl.interfaceMethodDeclaration(), bodyDecl, mvel3toJavaParserVisitor);
                        interfaceDecl.addMember(method);
                    } else if (memberDecl.constDeclaration() != null) {
                        FieldDeclaration field = convertConstDeclaration(memberDecl.constDeclaration(), bodyDecl, mvel3toJavaParserVisitor);
                        interfaceDecl.addMember(field);
                    } else if (memberDecl.genericInterfaceMethodDeclaration() != null) {
                        MethodDeclaration method = convertGenericInterfaceMethodDeclaration(memberDecl.genericInterfaceMethodDeclaration(), bodyDecl, mvel3toJavaParserVisitor);
                        interfaceDecl.addMember(method);
                    } else if (memberDecl.classDeclaration() != null) {
                        interfaceDecl.addMember((BodyDeclaration<?>) TypeConverter.convertClassDeclaration(memberDecl.classDeclaration(), mvel3toJavaParserVisitor));
                    } else if (memberDecl.enumDeclaration() != null) {
                        interfaceDecl.addMember((BodyDeclaration<?>) EnumConverter.convertEnumDeclaration(memberDecl.enumDeclaration(), mvel3toJavaParserVisitor));
                    } else if (memberDecl.interfaceDeclaration() != null) {
                        interfaceDecl.addMember((BodyDeclaration<?>) convertInterfaceDeclaration(memberDecl.interfaceDeclaration(), mvel3toJavaParserVisitor));
                    } else if (memberDecl.annotationTypeDeclaration() != null) {
                        interfaceDecl.addMember((BodyDeclaration<?>) AnnotationTypeConverter.convertAnnotationTypeDeclaration(memberDecl.annotationTypeDeclaration(), mvel3toJavaParserVisitor));
                    } else if (memberDecl.recordDeclaration() != null) {
                        interfaceDecl.addMember((BodyDeclaration<?>) RecordConverter.convertRecordDeclaration(memberDecl.recordDeclaration(), mvel3toJavaParserVisitor));
                    }
                }
            }
        }
    }

    private static MethodDeclaration convertInterfaceMethodDeclaration(
            final Mvel3Parser.InterfaceMethodDeclarationContext ctx,
            final Mvel3Parser.InterfaceBodyDeclarationContext bodyDecl,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        MethodDeclaration methodDecl = buildInterfaceMethodDeclaration(
                ctx.interfaceCommonBodyDeclaration(), bodyDecl, ctx.interfaceMethodModifier(), mvel3toJavaParserVisitor);
        methodDecl.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return methodDecl;
    }

    private static MethodDeclaration convertGenericInterfaceMethodDeclaration(
            final Mvel3Parser.GenericInterfaceMethodDeclarationContext ctx,
            final Mvel3Parser.InterfaceBodyDeclarationContext bodyDecl,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        MethodDeclaration methodDecl = buildInterfaceMethodDeclaration(
                ctx.interfaceCommonBodyDeclaration(), bodyDecl, ctx.interfaceMethodModifier(), mvel3toJavaParserVisitor);
        if (ctx.typeParameters() != null) {
            methodDecl.setTypeParameters(TypeConverter.convertTypeParameters(ctx.typeParameters(), mvel3toJavaParserVisitor));
        }
        methodDecl.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return methodDecl;
    }

    /**
     * Shared logic for building an interface method declaration from its common body,
     * parent body declaration (for outer modifiers), and interface method modifiers.
     */
    private static MethodDeclaration buildInterfaceMethodDeclaration(
            final Mvel3Parser.InterfaceCommonBodyDeclarationContext commonBody,
            final Mvel3Parser.InterfaceBodyDeclarationContext bodyDecl,
            final List<Mvel3Parser.InterfaceMethodModifierContext> interfaceMethodModifiers,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        String methodName = commonBody.identifier().getText();
        MethodDeclaration methodDecl = new MethodDeclaration();
        methodDecl.setName(methodName);

        // Handle return type
        if (commonBody.typeTypeOrVoid() != null) {
            if (commonBody.typeTypeOrVoid().VOID() != null) {
                methodDecl.setType(new VoidType());
            } else if (commonBody.typeTypeOrVoid().typeType() != null) {
                Type returnType = (Type) TypeConverter.convertTypeType(commonBody.typeTypeOrVoid().typeType(), mvel3toJavaParserVisitor);
                int extraDims = commonBody.LBRACK() != null ? commonBody.LBRACK().size() : 0;
                for (int i = 0; i < extraDims; i++) {
                    returnType = new ArrayType(returnType);
                }
                methodDecl.setType(returnType);
            }
        }

        // Handle parameters
        if (commonBody.formalParameters() != null) {
            methodDecl.setParameters(ParametersConverter.convertFormalParameters(commonBody.formalParameters(), mvel3toJavaParserVisitor));
            ParametersConverter.convertReceiverParameter(commonBody.formalParameters(), methodDecl, mvel3toJavaParserVisitor);
        }

        // Handle throws clause
        if (commonBody.THROWS() != null && commonBody.qualifiedNameList() != null) {
            methodDecl.setThrownExceptions(TypeConverter.convertQualifiedNameListAsTypes(commonBody.qualifiedNameList()));
        }

        // Handle method body (default methods have a body)
        if (commonBody.methodBody() != null && commonBody.methodBody().block() != null) {
            BlockStmt body = (BlockStmt) BlockConverter.convertBlock(commonBody.methodBody().block(), mvel3toJavaParserVisitor);
            methodDecl.setBody(body);
        }

        // Handle modifiers from interfaceMethodModifier* and parent interfaceBodyDeclaration
        NodeList<Modifier> modifiers = new NodeList<>();
        NodeList<AnnotationExpr> annotations = new NodeList<>();

        if (bodyDecl.modifier() != null) {
            ModifiersAnnotations ma = ModifiersParser.parseModifiers(bodyDecl.modifier());
            modifiers.addAll(ma.modifiers());
            annotations.addAll(ma.annotations());
        }

        if (interfaceMethodModifiers != null) {
            for (Mvel3Parser.InterfaceMethodModifierContext modCtx : interfaceMethodModifiers) {
                if (modCtx.PUBLIC() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.PUBLIC, modCtx));
                } else if (modCtx.ABSTRACT() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.ABSTRACT, modCtx));
                } else if (modCtx.DEFAULT() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.DEFAULT, modCtx));
                } else if (modCtx.STATIC() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.STATIC, modCtx));
                } else if (modCtx.STRICTFP() != null) {
                    modifiers.add(TypeConverter.createModifier(Modifier.Keyword.STRICTFP, modCtx));
                }
            }
        }

        if (!modifiers.isEmpty()) {
            methodDecl.setModifiers(modifiers);
        }
        if (!annotations.isEmpty()) {
            methodDecl.setAnnotations(annotations);
        }

        return methodDecl;
    }

    private static FieldDeclaration convertConstDeclaration(
            final Mvel3Parser.ConstDeclarationContext ctx,
            final Mvel3Parser.InterfaceBodyDeclarationContext bodyDecl,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        Type fieldType = (Type) TypeConverter.convertTypeType(ctx.typeType(), mvel3toJavaParserVisitor);

        NodeList<VariableDeclarator> declarators = new NodeList<>();
        for (Mvel3Parser.ConstantDeclaratorContext constCtx : ctx.constantDeclarator()) {
            String varName = constCtx.identifier().getText();

            // Apply C-style array dimensions from constantDeclarator (identifier ('[' ']')*)
            Type declType = fieldType;
            int dimensions = constCtx.LBRACK() != null ? constCtx.LBRACK().size() : 0;
            for (int i = 0; i < dimensions; i++) {
                ArrayType arrayType = new ArrayType(declType);
                arrayType.setTokenRange(TokenRangeConverter.createTokenRange(constCtx));
                declType = arrayType;
            }

            VariableDeclarator varDeclarator = new VariableDeclarator(declType, varName);
            varDeclarator.setTokenRange(TokenRangeConverter.createTokenRange(constCtx));

            if (constCtx.variableInitializer() != null) {
                Expression initializer = (Expression) VariableConverter.convertVariableInitializer(constCtx.variableInitializer(), mvel3toJavaParserVisitor);
                varDeclarator.setInitializer(initializer);
            }

            declarators.add(varDeclarator);
        }

        FieldDeclaration fieldDecl = new FieldDeclaration(new NodeList<>(), declarators);
        fieldDecl.setTokenRange(TokenRangeConverter.createTokenRange(ctx));

        // Handle modifiers from parent context
        if (bodyDecl.modifier() != null) {
            ModifiersAnnotations ma = ModifiersParser.parseModifiers(bodyDecl.modifier());
            fieldDecl.setModifiers(ma.modifiers());
            fieldDecl.setAnnotations(ma.annotations());
        }

        return fieldDecl;
    }
}
