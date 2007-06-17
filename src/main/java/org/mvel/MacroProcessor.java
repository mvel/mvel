package org.mvel;

import static java.lang.Character.isWhitespace;

import org.mvel.util.StringAppender;
import org.mvel.util.ParseTools;

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

            while (cursor < length
                    && (!isWhitespace(expr[cursor]) 
                    && expr[cursor] != '('
                    && expr[cursor] != ')')) cursor++;

            if (macros.containsKey(token = new String(expr, start, cursor - start))) {
                appender.append(macros.get(token).doMacro());
            }
            else {
                appender.append(token);
            }

            if (cursor < length) appender.append(expr[cursor]);
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
        while (cursor < length && !isWhitespace(expr[cursor])) cursor++;
    }
}
