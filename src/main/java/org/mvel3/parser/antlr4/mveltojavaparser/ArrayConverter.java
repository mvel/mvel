package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.Type;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;

public final class ArrayConverter {

    private ArrayConverter() {
    }

    public static Node convertArrayInitializer(final Mvel3Parser.ArrayInitializerContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        NodeList<Expression> values = new NodeList<>();

        if (ctx.variableInitializer() != null && !ctx.variableInitializer().isEmpty()) {
            for (Mvel3Parser.VariableInitializerContext initCtx : ctx.variableInitializer()) {
                Expression expr = (Expression) VariableConverter.convertVariableInitializer(initCtx, mvel3toJavaParserVisitor);
                if (expr != null) {
                    values.add(expr);
                }
            }
        }

        ArrayInitializerExpr arrayInit = new ArrayInitializerExpr(values);
        arrayInit.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return arrayInit;
    }

    public static Type applyArrayDimensions(Type baseType, Mvel3Parser.VariableDeclaratorIdContext idContext) {
        if (idContext == null) {
            return baseType;
        }

        int dimensions = idContext.LBRACK() != null ? idContext.LBRACK().size() : 0;

        Type result = baseType;
        for (int i = 0; i < dimensions; i++) {
            ArrayType arrayType = new ArrayType(result);
            arrayType.setTokenRange(TokenRangeConverter.createTokenRange(idContext));
            result = arrayType;
        }

        return result;
    }
}
