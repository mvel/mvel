package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.Type;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.type.TypeConverter;

public final class AnnotationTypeConverter {

    private AnnotationTypeConverter() {
    }

    public static Node convertAnnotationTypeDeclaration(final Mvel3Parser.AnnotationTypeDeclarationContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        String annotationName = ctx.identifier().getText();
        AnnotationDeclaration annotationDecl = new AnnotationDeclaration(new NodeList<>(), annotationName);

        // Handle modifiers from parent context
        ModifiersAnnotations annotationModifiers = ModifiersParser.resolveModifiersFromParent(ctx);
        annotationDecl.setModifiers(annotationModifiers.modifiers());
        annotationDecl.setAnnotations(annotationModifiers.annotations());

        // Handle annotation type body
        if (ctx.annotationTypeBody() != null) {
            convertAnnotationTypeBody(ctx.annotationTypeBody(), annotationDecl, mvel3toJavaParserVisitor);
        }

        annotationDecl.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return annotationDecl;
    }

    private static void convertAnnotationTypeBody(final Mvel3Parser.AnnotationTypeBodyContext ctx,
            final AnnotationDeclaration annotationDecl,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        if (ctx.annotationTypeElementDeclaration() != null) {
            for (Mvel3Parser.AnnotationTypeElementDeclarationContext elemDecl : ctx.annotationTypeElementDeclaration()) {
                if (elemDecl.annotationTypeElementRest() == null) {
                    continue; // bare semicolon
                }
                Mvel3Parser.AnnotationTypeElementRestContext rest = elemDecl.annotationTypeElementRest();

                if (rest.annotationMethodOrConstantRest() != null) {
                    // annotation method or constant: typeType annotationMethodOrConstantRest ';'
                    Type type = (Type) TypeConverter.convertTypeType(rest.typeType(), mvel3toJavaParserVisitor);
                    Mvel3Parser.AnnotationMethodOrConstantRestContext methodOrConst = rest.annotationMethodOrConstantRest();

                    if (methodOrConst.annotationMethodRest() != null) {
                        // Annotation method: identifier '(' ')' defaultValue?
                        Mvel3Parser.AnnotationMethodRestContext methodRest = methodOrConst.annotationMethodRest();
                        String methodName = methodRest.identifier().getText();

                        AnnotationMemberDeclaration memberDecl = new AnnotationMemberDeclaration();
                        memberDecl.setType(type);
                        memberDecl.setName(methodName);

                        // Handle default value
                        if (methodRest.defaultValue() != null) {
                            Expression defaultValue = convertElementValue(methodRest.defaultValue().elementValue(), mvel3toJavaParserVisitor);
                            memberDecl.setDefaultValue(defaultValue);
                        }

                        // Handle modifiers from parent annotationTypeElementDeclaration
                        if (elemDecl.modifier() != null) {
                            ModifiersAnnotations ma = ModifiersParser.parseModifiers(elemDecl.modifier());
                            memberDecl.setModifiers(ma.modifiers());
                            memberDecl.setAnnotations(ma.annotations());
                        }

                        memberDecl.setTokenRange(TokenRangeConverter.createTokenRange(rest));
                        annotationDecl.addMember(memberDecl);
                    } else if (methodOrConst.annotationConstantRest() != null) {
                        // Annotation constant: variableDeclarators
                        Mvel3Parser.AnnotationConstantRestContext constRest = methodOrConst.annotationConstantRest();
                        NodeList<VariableDeclarator> declarators = new NodeList<>();
                        for (Mvel3Parser.VariableDeclaratorContext declaratorCtx : constRest.variableDeclarators().variableDeclarator()) {
                            String varName = declaratorCtx.variableDeclaratorId().identifier().getText();
                            VariableDeclarator varDeclarator = new VariableDeclarator(type, varName);
                            varDeclarator.setTokenRange(TokenRangeConverter.createTokenRange(declaratorCtx));

                            if (declaratorCtx.variableInitializer() != null) {
                                Expression initializer = (Expression) VariableConverter.convertVariableInitializer(declaratorCtx.variableInitializer(), mvel3toJavaParserVisitor);
                                varDeclarator.setInitializer(initializer);
                            }

                            declarators.add(varDeclarator);
                        }

                        FieldDeclaration fieldDecl = new FieldDeclaration(new NodeList<>(), declarators);
                        fieldDecl.setTokenRange(TokenRangeConverter.createTokenRange(rest));

                        // Handle modifiers
                        if (elemDecl.modifier() != null) {
                            ModifiersAnnotations ma = ModifiersParser.parseModifiers(elemDecl.modifier());
                            fieldDecl.setModifiers(ma.modifiers());
                            fieldDecl.setAnnotations(ma.annotations());
                        }

                        annotationDecl.addMember(fieldDecl);
                    }
                } else if (rest.classDeclaration() != null) {
                    Node nested = TypeConverter.convertClassDeclaration(rest.classDeclaration(), mvel3toJavaParserVisitor);
                    annotationDecl.addMember((BodyDeclaration<?>) nested);
                } else if (rest.interfaceDeclaration() != null) {
                    Node nested = InterfaceConverter.convertInterfaceDeclaration(rest.interfaceDeclaration(), mvel3toJavaParserVisitor);
                    annotationDecl.addMember((BodyDeclaration<?>) nested);
                } else if (rest.enumDeclaration() != null) {
                    Node nested = EnumConverter.convertEnumDeclaration(rest.enumDeclaration(), mvel3toJavaParserVisitor);
                    annotationDecl.addMember((BodyDeclaration<?>) nested);
                } else if (rest.annotationTypeDeclaration() != null) {
                    Node nested = convertAnnotationTypeDeclaration(rest.annotationTypeDeclaration(), mvel3toJavaParserVisitor);
                    annotationDecl.addMember((BodyDeclaration<?>) nested);
                } else if (rest.recordDeclaration() != null) {
                    Node nested = RecordConverter.convertRecordDeclaration(rest.recordDeclaration(), mvel3toJavaParserVisitor);
                    annotationDecl.addMember((BodyDeclaration<?>) nested);
                }
            }
        }
    }

    private static Expression convertElementValue(final Mvel3Parser.ElementValueContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        if (ctx.expression() != null) {
            return (Expression) mvel3toJavaParserVisitor.visit(ctx.expression());
        } else if (ctx.annotation() != null) {
            return (Expression) mvel3toJavaParserVisitor.visit(ctx.annotation());
        } else if (ctx.elementValueArrayInitializer() != null) {
            // Convert { val1, val2, ... } to ArrayInitializerExpr
            NodeList<Expression> values = new NodeList<>();
            for (Mvel3Parser.ElementValueContext elemCtx : ctx.elementValueArrayInitializer().elementValue()) {
                values.add(convertElementValue(elemCtx, mvel3toJavaParserVisitor));
            }
            return new ArrayInitializerExpr(values);
        }
        throw new UnsupportedOperationException("Unsupported element value: " + ctx.getText());
    }
}
