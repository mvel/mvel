package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.Mvel3ParserBaseVisitor;
import org.mvel3.parser.antlr4.mveltojavaparser.ArrayConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.CreatedNameConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.antlr4.mveltojavaparser.TypeConverter;

public final class ObjectCreationExpressionConverter {

    private ObjectCreationExpressionConverter() {
    }

    public static Node convertObjectCreationExpression(
            final Mvel3Parser.ObjectCreationExpressionContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        return convertCreator(ctx.creator(), mvel3toJavaParserVisitor);
    }

    public static Node convertCreator(
            final Mvel3Parser.CreatorContext ctx,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        Node createdName = CreatedNameConverter.convertCreatedName(ctx.createdName(), mvel3toJavaParserVisitor);

        if (ctx.arrayCreatorRest() != null) {
            // Handle array creation: new Type[] {...} or new Type[size]
            return visitArrayCreatorRest(ctx.arrayCreatorRest(), createdName, mvel3toJavaParserVisitor);
        } else if (ctx.classCreatorRest() != null) {
            // Handle class creation: new Type(args)
            Type type = (Type) createdName;

            // Handle nonWildcardTypeArguments: new <String>Foo()
            NodeList<Type> typeArguments = null;
            if (ctx.nonWildcardTypeArguments() != null) {
                typeArguments = new NodeList<>();
                typeArguments.addAll(TypeConverter.convertTypeList(ctx.nonWildcardTypeArguments().typeList(), mvel3toJavaParserVisitor));
            }

            // Get constructor arguments
            NodeList<Expression> arguments = new NodeList<>();
            if (ctx.classCreatorRest().arguments() != null &&
                    ctx.classCreatorRest().arguments().expressionList() != null) {
                for (Mvel3Parser.ExpressionContext exprCtx : ctx.classCreatorRest().arguments().expressionList().expression()) {
                    Expression arg = (Expression) mvel3toJavaParserVisitor.visit(exprCtx);
                    arguments.add(arg);
                }
            }

            // Handle anonymous class body if present
            NodeList<BodyDeclaration<?>> anonymousClassBody = null;
            if (ctx.classCreatorRest().classBody() != null) {
                anonymousClassBody = TypeConverter.convertAnonymousClassBody(ctx.classCreatorRest().classBody(), mvel3toJavaParserVisitor);
            }

            // Create ObjectCreationExpr
            ObjectCreationExpr objectCreation = new ObjectCreationExpr(null, (ClassOrInterfaceType) type, typeArguments, arguments, anonymousClassBody);
            objectCreation.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return objectCreation;
        }

        return createdName;
    }

    private static Node visitArrayCreatorRest(
            final Mvel3Parser.ArrayCreatorRestContext ctx,
            final Node createdName,
            final Mvel3ParserBaseVisitor<Node> mvel3toJavaParserVisitor) {
        Type elementType = (Type) createdName;

        if (ctx.arrayInitializer() != null) {
            // Handle: new Type[] { ... }
            ArrayInitializerExpr initializer = (ArrayInitializerExpr) ArrayConverter.convertArrayInitializer(ctx.arrayInitializer(), mvel3toJavaParserVisitor);

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
                    Expression dimExpr = (Expression) mvel3toJavaParserVisitor.visit(exprCtx);
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
}
