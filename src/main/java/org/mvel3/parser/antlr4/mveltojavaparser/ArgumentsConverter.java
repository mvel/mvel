package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.WildcardType;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;

public final class ArgumentsConverter {

    private ArgumentsConverter() {
    }

    public static NodeList<Expression> convertArguments(final Mvel3Parser.ArgumentsContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        NodeList<Expression> args = new NodeList<>();
        if (ctx.expressionList() != null) {
            // Parse each expression in the argument list
            for (Mvel3Parser.ExpressionContext exprCtx : ctx.expressionList().expression()) {
                Expression arg = (Expression) mvel3toJavaParserVisitor.visit(exprCtx);
                args.add(arg);
            }
        }
        return args;
    }

    public static Node convertTypeArgument(final Mvel3Parser.TypeArgumentContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        // typeArgument: typeType | annotation* '?' ((EXTENDS | SUPER) typeType)?
        if (ctx.QUESTION() != null) {
            // Wildcard: ?, ? extends T, ? super T (with optional annotations)
            WildcardType wildcard = new WildcardType();
            if (ctx.EXTENDS() != null && ctx.typeType() != null) {
                wildcard.setExtendedType((ReferenceType) TypeConverter.convertTypeType(ctx.typeType(), mvel3toJavaParserVisitor));
            } else if (ctx.SUPER() != null && ctx.typeType() != null) {
                wildcard.setSuperType((ReferenceType) TypeConverter.convertTypeType(ctx.typeType(), mvel3toJavaParserVisitor));
            }
            wildcard.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return wildcard;
        } else {
            // Plain type argument: String, List<Integer>, etc.
            return TypeConverter.convertTypeType(ctx.typeType(), mvel3toJavaParserVisitor);
        }
    }

    public static NodeList<Type> convertTypeArguments(final Mvel3Parser.TypeArgumentsContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        NodeList<Type> typeArgs = new NodeList<>();
        if (ctx != null) {
            for (Mvel3Parser.TypeArgumentContext typeArgCtx : ctx.typeArgument()) {
                Type typeArg = (Type) convertTypeArgument(typeArgCtx, mvel3toJavaParserVisitor);
                if (typeArg != null) {
                    typeArgs.add(typeArg);
                }
            }
        }
        return typeArgs;
    }

    public static NodeList<Type> convertNonWildcardTypeArguments(
            final Mvel3Parser.NonWildcardTypeArgumentsContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        NodeList<Type> typeArgs = new NodeList<>();
        if (ctx != null && ctx.typeList() != null) {
            for (Mvel3Parser.TypeTypeContext typeCtx : ctx.typeList().typeType()) {
                typeArgs.add((Type) TypeConverter.convertTypeType(typeCtx, mvel3toJavaParserVisitor));
            }
        }
        return typeArgs;
    }
}
