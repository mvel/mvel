package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import org.mvel3.parser.antlr4.LambdaParametersResult;
import org.mvel3.parser.antlr4.ModifiersAnnotations;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.BlockConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.ParametersConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.VariableParser;

public final class LambdaExpressionConverter {

    private LambdaExpressionConverter() {
    }

    public static Node convertLambdaExpression(
            final Mvel3Parser.LambdaExpressionContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        LambdaParametersResult parametersResult = resolveLambdaParameters(ctx.lambdaParameters(), mvel3toJavaParserVisitor);
        Statement body = resolveLambdaBody(ctx.lambdaBody(), mvel3toJavaParserVisitor);

        LambdaExpr lambdaExpr = new LambdaExpr(parametersResult.parameters(), body, parametersResult.enclosingParameters());
        lambdaExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return lambdaExpr;
    }

    private static LambdaParametersResult resolveLambdaParameters(
            final Mvel3Parser.LambdaParametersContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
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
            parameters.addAll(ParametersConverter.convertFormalParameters(ctx.formalParameterList(), mvel3toJavaParserVisitor));
        } else if (ctx.lambdaLVTIList() != null) {
            parameters.addAll(collectLambdaLVTIParameters(ctx.lambdaLVTIList()));
        } else if (ctx.identifier() != null && !ctx.identifier().isEmpty()) {
            for (Mvel3Parser.IdentifierContext identifierContext : ctx.identifier()) {
                parameters.add(createInferredParameter(identifierContext));
            }
        }

        return new LambdaParametersResult(parameters, enclosingParameters);
    }

    private static NodeList<Parameter> collectLambdaLVTIParameters(Mvel3Parser.LambdaLVTIListContext ctx) {
        NodeList<Parameter> parameters = new NodeList<>();
        if (ctx != null) {
            for (Mvel3Parser.LambdaLVTIParameterContext parameterContext : ctx.lambdaLVTIParameter()) {
                parameters.add(createLambdaVarParameter(parameterContext));
            }
        }
        return parameters;
    }

    private static Parameter createLambdaVarParameter(Mvel3Parser.LambdaLVTIParameterContext ctx) {
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

    private static Parameter createInferredParameter(Mvel3Parser.IdentifierContext identifierContext) {
        UnknownType unknownType = new UnknownType();
        unknownType.setTokenRange(TokenRangeConverter.createTokenRange(identifierContext));

        SimpleName name = ParametersConverter.createSimpleName(identifierContext);

        Parameter parameter = new Parameter(new NodeList<>(), new NodeList<>(), unknownType, false, new NodeList<>(), name);
        parameter.setTokenRange(TokenRangeConverter.createTokenRange(identifierContext));
        return parameter;
    }

    private static Statement resolveLambdaBody(
            final Mvel3Parser.LambdaBodyContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        if (ctx.block() != null) {
            return (Statement) BlockConverter.convertBlock(ctx.block(), mvel3toJavaParserVisitor);
        }

        Expression expression = (Expression) mvel3toJavaParserVisitor.visit(ctx.expression());
        ExpressionStmt expressionStmt = new ExpressionStmt(expression);
        expressionStmt.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return expressionStmt;
    }
}
