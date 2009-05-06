package org.mvel;

import org.mvel.compiler.AbstractParser;
import org.mvel.util.StringAppender;

import static java.lang.Character.isWhitespace;
import java.util.Map;

/**
 * @author Christopher Brock
 */
public class MacroProcessor extends AbstractParser implements PreProcessor {
    private Map<String, Macro> macros;

    public char[] parse(char[] input) {
        setExpression(input);

        StringAppender appender = new StringAppender();

        int start;
        String token;

        for (; cursor < length; cursor++) {
            while (cursor < length && (isWhitespace(expr[cursor]) || expr[cursor] == ';')) {
                appender.append(expr[cursor++]);
            }

            start = cursor;

            while (cursor < length
                    && (!isWhitespace(expr[cursor])
                    && expr[cursor] != '('
                    && expr[cursor] != ')')) {

                cursor++;
            }

            if (macros.containsKey(token = new String(expr, start, cursor - start))) {
                appender.append(macros.get(token).doMacro());
            }
            else {
                appender.append(token);
            }

            if (cursor < length) {
                appender.append(expr[cursor]);
            }
        }

        return appender.toChars();
    }

    public String parse(String input) {
        return new String(parse(input.toCharArray()));
    }

    public Map<String, Macro> getMacros() {
        return macros;
    }

    public void setMacros(Map<String, Macro> macros) {
        this.macros = macros;
    }

    public void captureToWhitespace() {
        while (cursor < length && !isWhitespace(expr[cursor])) cursor++;
    }
}
