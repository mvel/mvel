package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ToJavaParserVisitor;

public final class CreatedNameConverter {

    public CreatedNameConverter() {
    }

    public static Node convertCreatedName(
            final Mvel3Parser.CreatedNameContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        if (ctx.primitiveType() != null) {
            return TypeConverter.convertPrimitiveType(ctx.primitiveType());
        } else if (ctx.identifier() != null && !ctx.identifier().isEmpty()) {
            // Handle class/interface type creation - build qualified name with type arguments
            ClassOrInterfaceType type = null;

            // Build the qualified name from all identifiers with their type arguments
            for (int i = 0; i < ctx.identifier().size(); i++) {
                String name = ctx.identifier(i).getText();

                // Check if this identifier has type arguments or diamond operator
                NodeList<Type> typeArguments = null;
                if (i < ctx.typeArgumentsOrDiamond().size() && ctx.typeArgumentsOrDiamond(i) != null) {
                    typeArguments = handleTypeArgumentsOrDiamond(ctx.typeArgumentsOrDiamond(i), mvel3toJavaParserVisitor);
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

    private static NodeList<Type> handleTypeArgumentsOrDiamond(
            final Mvel3Parser.TypeArgumentsOrDiamondContext ctx,
            final Mvel3ToJavaParserVisitor mvel3toJavaParserVisitor) {
        if (ctx.typeArguments() != null) {
            return ArgumentsConverter.convertTypeArguments(ctx.typeArguments(), mvel3toJavaParserVisitor);
        } else {
            // Handle diamond operator: <> - return empty NodeList to represent diamond
            return new NodeList<>();
        }
    }
}
