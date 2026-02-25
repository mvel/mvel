package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.Type;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;

public final class ParametersConverter {

    private ParametersConverter() {
    }

    public static NodeList<Parameter> convertFormalParameters(Mvel3Parser.FormalParametersContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        if (ctx == null || ctx.formalParameterList() == null) {
            return new NodeList<>();
        }
        return convertFormalParameters(ctx.formalParameterList(), mvel3toJavaParserVisitor);
    }

    public static Node convertFormalParameter(final Mvel3Parser.FormalParameterContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        ModifiersAnnotations modifiersAnnotations = VariableParser.parseVariableModifiers(ctx.variableModifier());
        Type type = (Type) mvel3toJavaParserVisitor.visit(ctx.typeType());
        Type adjustedType = ArrayConverter.applyArrayDimensions(type, ctx.variableDeclaratorId());
        SimpleName name = createSimpleName(ctx.variableDeclaratorId().identifier());

        Parameter parameter = new Parameter(modifiersAnnotations.modifiers(),
                modifiersAnnotations.annotations(),
                adjustedType,
                false,
                new NodeList<>(),
                name);
        parameter.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return parameter;
    }

    public static Node convertLastFormalParameter(final Mvel3Parser.LastFormalParameterContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        ModifiersAnnotations modifiersAnnotations = VariableParser.parseVariableModifiers(ctx.variableModifier());
        Type type = (Type) mvel3toJavaParserVisitor.visit(ctx.typeType());
        boolean isVarArgs = ctx.ELLIPSIS() != null;

        NodeList<AnnotationExpr> varArgsAnnotations = new NodeList<>();
        if (ctx.annotation() != null) {
            for (Mvel3Parser.AnnotationContext annotationContext : ctx.annotation()) {
                varArgsAnnotations.add(ModifiersConverter.convertAnnotationExpr(annotationContext));
            }
        }

        Type adjustedType = ArrayConverter.applyArrayDimensions(type, ctx.variableDeclaratorId());
        SimpleName name = createSimpleName(ctx.variableDeclaratorId().identifier());

        Parameter parameter = new Parameter(modifiersAnnotations.modifiers(),
                modifiersAnnotations.annotations(),
                adjustedType,
                isVarArgs,
                varArgsAnnotations,
                name);
        parameter.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return parameter;
    }

    public static NodeList<Parameter> convertFormalParameters(final Mvel3Parser.FormalParameterListContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        NodeList<Parameter> parameters = new NodeList<>();
        if (ctx == null) {
            return parameters;
        }

        if (ctx.formalParameter() != null) {
            for (Mvel3Parser.FormalParameterContext formalParameterContext : ctx.formalParameter()) {
                parameters.add((Parameter) convertFormalParameter(formalParameterContext, mvel3toJavaParserVisitor));
            }
        }

        if (ctx.lastFormalParameter() != null) {
            parameters.add((Parameter) convertLastFormalParameter(ctx.lastFormalParameter(), mvel3toJavaParserVisitor));
        }

        return parameters;
    }

    public static SimpleName createSimpleName(Mvel3Parser.IdentifierContext identifierContext) {
        SimpleName name = new SimpleName(identifierContext.getText());
        name.setTokenRange(TokenRangeConverter.createTokenRange(identifierContext));
        return name;
    }

    /**
     * Process receiver parameter (e.g., {@code Foo this} or {@code Outer.Inner this}) from
     * formalParameters and set it on the containing method or constructor declaration.
     * The receiver parameter is not a regular parameter — it's set via
     * {@link CallableDeclaration#setReceiverParameter(ReceiverParameter)}.
     */
    public static void convertReceiverParameter(
            final Mvel3Parser.FormalParametersContext formalParamsCtx,
            final CallableDeclaration<?> callableDecl,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        if (formalParamsCtx == null || formalParamsCtx.receiverParameter() == null) {
            return;
        }
        Mvel3Parser.ReceiverParameterContext rpCtx = formalParamsCtx.receiverParameter();

        // Parse the type
        Type type = (Type) mvel3toJavaParserVisitor.visit(rpCtx.typeType());

        // Build name from optional qualifiers + "this"
        // Grammar: typeType (identifier '.')* THIS
        StringBuilder nameBuilder = new StringBuilder();
        if (rpCtx.identifier() != null) {
            for (Mvel3Parser.IdentifierContext idCtx : rpCtx.identifier()) {
                nameBuilder.append(idCtx.getText()).append(".");
            }
        }
        nameBuilder.append("this");
        Name name = new Name(nameBuilder.toString());

        ReceiverParameter receiverParam = new ReceiverParameter(type, name);
        receiverParam.setTokenRange(TokenRangeConverter.createTokenRange(rpCtx));
        callableDecl.setReceiverParameter(receiverParam);
    }
}
