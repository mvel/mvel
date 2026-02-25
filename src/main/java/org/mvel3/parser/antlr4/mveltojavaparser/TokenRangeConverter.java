package org.mvel3.parser.antlr4.mveltojavaparser;

import com.github.javaparser.JavaToken;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.TokenRange;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public final class TokenRangeConverter {

    private TokenRangeConverter() {
    }

    /**
     * Create a JavaParser TokenRange from ANTLR ParserRuleContext.
     * This provides proper source location information instead of using TokenRange.INVALID.
     */
    public static TokenRange createTokenRange(ParserRuleContext ctx) {
        if (ctx == null) {
            return TokenRange.INVALID;
        }

        Token startToken = ctx.getStart();
        Token stopToken = ctx.getStop();

        if (startToken == null || stopToken == null) {
            return TokenRange.INVALID;
        }

        // Create JavaParser positions
        Position startPos = new Position(startToken.getLine(), startToken.getCharPositionInLine() + 1);
        Position stopPos = new Position(stopToken.getLine(), stopToken.getCharPositionInLine() + stopToken.getText().length());

        // Create JavaParser Range
        Range range = new Range(startPos, stopPos);

        // Create JavaParser JavaTokens (simplified - we use token type 0 and the actual text)
        JavaToken startJavaToken = new JavaToken(0, startToken.getText());
        startJavaToken.setRange(range);

        JavaToken stopJavaToken = new JavaToken(0, stopToken.getText());
        stopJavaToken.setRange(range);

        return new TokenRange(startJavaToken, stopJavaToken);
    }

    /**
     * Create a TokenRange from a single ANTLR token (for terminal nodes).
     */
    public static TokenRange createTokenRange(Token token) {
        if (token == null) {
            return TokenRange.INVALID;
        }

        Position startPos = new Position(token.getLine(), token.getCharPositionInLine() + 1);
        Position stopPos = new Position(token.getLine(), token.getCharPositionInLine() + token.getText().length());
        Range range = new Range(startPos, stopPos);

        JavaToken javaToken = new JavaToken(0, token.getText());
        javaToken.setRange(range);

        return new TokenRange(javaToken, javaToken);
    }
}
