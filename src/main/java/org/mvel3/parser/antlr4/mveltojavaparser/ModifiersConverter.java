package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.AnnotationExpr;
import org.mvel3.parser.antlr4.Mvel3Parser;

public final class ModifiersConverter {

    private ModifiersConverter() {
    }

    public static AnnotationExpr convertAnnotationExpr(Mvel3Parser.AnnotationContext ctx) {
        AnnotationExpr annotationExpr = StaticJavaParser.parseAnnotation(ctx.getText());
        annotationExpr.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
        return annotationExpr;
    }
}
