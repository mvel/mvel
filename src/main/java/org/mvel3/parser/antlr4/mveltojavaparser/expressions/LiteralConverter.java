package org.mvel3.parser.antlr4.mveltojavaparser.expressions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.mvel3.parser.antlr4.Mvel3Parser;
import org.mvel3.parser.antlr4.mveltojavaparser.TokenRangeConverter;
import org.mvel3.parser.ast.expr.BigDecimalLiteralExpr;
import org.mvel3.parser.ast.expr.BigIntegerLiteralExpr;
import org.mvel3.parser.ast.expr.TemporalChunkExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralChunkExpr;
import org.mvel3.parser.ast.expr.TemporalLiteralExpr;

import java.util.concurrent.TimeUnit;

public final class LiteralConverter {

    private LiteralConverter() {
    }

    public static Node convertLiteral(final Mvel3Parser.LiteralContext ctx) {
        if (ctx.STRING_LITERAL() != null) {
            String text = ctx.STRING_LITERAL().getText();
            // Remove quotes
            String value = text.substring(1, text.length() - 1);
            StringLiteralExpr stringLiteral = new StringLiteralExpr(value);
            stringLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return stringLiteral;
        } else if (ctx.DECIMAL_LITERAL() != null) {
            return createIntegerOrLongLiteral(ctx.DECIMAL_LITERAL().getText(), ctx);
        } else if (ctx.HEX_LITERAL() != null) {
            return createIntegerOrLongLiteral(ctx.HEX_LITERAL().getText(), ctx);
        } else if (ctx.OCT_LITERAL() != null) {
            return createIntegerOrLongLiteral(ctx.OCT_LITERAL().getText(), ctx);
        } else if (ctx.BINARY_LITERAL() != null) {
            return createIntegerOrLongLiteral(ctx.BINARY_LITERAL().getText(), ctx);
        } else if (ctx.FLOAT_LITERAL() != null) {
            DoubleLiteralExpr doubleLiteral = new DoubleLiteralExpr(ctx.FLOAT_LITERAL().getText());
            doubleLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return doubleLiteral;
        } else if (ctx.HEX_FLOAT_LITERAL() != null) {
            DoubleLiteralExpr doubleLiteral = new DoubleLiteralExpr(ctx.HEX_FLOAT_LITERAL().getText());
            doubleLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return doubleLiteral;
        } else if (ctx.BOOL_LITERAL() != null) {
            BooleanLiteralExpr booleanLiteral = new BooleanLiteralExpr(Boolean.parseBoolean(ctx.BOOL_LITERAL().getText()));
            booleanLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return booleanLiteral;
        } else if (ctx.NULL_LITERAL() != null) {
            NullLiteralExpr nullLiteral = new NullLiteralExpr();
            nullLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return nullLiteral;
        } else if (ctx.CHAR_LITERAL() != null) {
            String text = ctx.CHAR_LITERAL().getText();
            char value = text.charAt(1); // Simple case, more complex handling needed for escape sequences
            CharLiteralExpr charLiteral = new CharLiteralExpr(value);
            charLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return charLiteral;
        } else if (ctx.TEXT_BLOCK() != null) {
            String rawText = ctx.TEXT_BLOCK().getText();
            // Extract content between triple quotes: """content"""
            String content = rawText.substring(3, rawText.length() - 3);
            TextBlockLiteralExpr textBlockLiteral = new TextBlockLiteralExpr(content);
            textBlockLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return textBlockLiteral;
        }

        // Handle MVEL-specific literals - create proper AST nodes like mvel.jj does
        if (ctx.BigDecimalLiteral() != null) {
            String text = ctx.BigDecimalLiteral().getText();
            // Create BigDecimalLiteralExpr node (transformation happens in MVELToJavaRewriter)
            return new BigDecimalLiteralExpr(TokenRangeConverter.createTokenRange(ctx), text);
        } else if (ctx.BigIntegerLiteral() != null) {
            String text = ctx.BigIntegerLiteral().getText();
            // Create BigIntegerLiteralExpr node (transformation happens in MVELToJavaRewriter)
            return new BigIntegerLiteralExpr(TokenRangeConverter.createTokenRange(ctx), text);
        } else if (ctx.temporalLiteral() != null) {
            return buildTemporalLiteral(ctx.temporalLiteral());
        }

        throw new IllegalArgumentException("Unknown literal type: " + ctx.getText());
    }

    private static Expression createIntegerOrLongLiteral(String text, ParserRuleContext ctx) {
        if (text.endsWith("L") || text.endsWith("l")) {
            LongLiteralExpr longLiteral = new LongLiteralExpr(text);
            longLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return longLiteral;
        } else {
            IntegerLiteralExpr integerLiteral = new IntegerLiteralExpr(text);
            integerLiteral.setTokenRange(TokenRangeConverter.createTokenRange(ctx));
            return integerLiteral;
        }
    }

    private static TemporalLiteralExpr buildTemporalLiteral(Mvel3Parser.TemporalLiteralContext ctx) {
        NodeList<TemporalChunkExpr> chunks = new NodeList<>();
        for (Mvel3Parser.TemporalLiteralChunkContext chunkCtx : ctx.temporalLiteralChunk()) {
            chunks.add(buildTemporalLiteralChunk(chunkCtx));
        }
        TemporalLiteralExpr temporalLiteralExpr = new TemporalLiteralExpr(TokenRangeConverter.createTokenRange(ctx), chunks);
        return temporalLiteralExpr;
    }

    private static TemporalLiteralChunkExpr buildTemporalLiteralChunk(Mvel3Parser.TemporalLiteralChunkContext ctx) {
        Token token;
        TimeUnit timeUnit;

        if (ctx.MILLISECOND_LITERAL() != null) {
            token = ctx.MILLISECOND_LITERAL().getSymbol();
            timeUnit = TimeUnit.MILLISECONDS;
        } else if (ctx.SECOND_LITERAL() != null) {
            token = ctx.SECOND_LITERAL().getSymbol();
            timeUnit = TimeUnit.SECONDS;
        } else if (ctx.MINUTE_LITERAL() != null) {
            token = ctx.MINUTE_LITERAL().getSymbol();
            timeUnit = TimeUnit.MINUTES;
        } else if (ctx.HOUR_LITERAL() != null) {
            token = ctx.HOUR_LITERAL().getSymbol();
            timeUnit = TimeUnit.HOURS;
        } else if (ctx.DAY_LITERAL() != null) {
            token = ctx.DAY_LITERAL().getSymbol();
            timeUnit = TimeUnit.DAYS;
        } else {
            throw new IllegalArgumentException("Unsupported temporal literal chunk: " + ctx.getText());
        }

        return new TemporalLiteralChunkExpr(TokenRangeConverter.createTokenRange(ctx), stripTimeUnit(token.getText()), timeUnit);
    }

    private static String stripTimeUnit(String text) {
        // Remove the time unit suffix (e.g., "m", "s", "h", "d", "ms")
        return text.replaceAll("[a-zA-Z]+$", "");
    }
}
