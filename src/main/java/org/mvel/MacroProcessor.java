package org.mvel;

import org.mvel.util.StringAppender;

import java.util.Map;

/**
 * @author Christopher Brock
 */
public class MacroProcessor extends AbstractParser {

    public String parse(Map<String, Macro> macros) {
        StringAppender appender = new StringAppender();

        int start;
        String token;

        for (; cursor < length; cursor++) {
            skipWhitespace();
            start = cursor;
            captureToWhitespace();

            if (macros.containsKey(token = new String(expr, start, cursor - start))) {
                appender.append(macros.get(token).doMacro());
            }
            else {
                appender.append(token);
            }

            if (cursor < length) appender.append(" ");
        }

        return appender.toString();
    }

    public MacroProcessor(char[] expr) {
        setExpression(expr);
    }

    public MacroProcessor(String expr) {
        setExpression(expr);
    }

    public void captureToWhitespace() {
        while (cursor < length && !Character.isWhitespace(expr[cursor])) cursor++;
    }
}
