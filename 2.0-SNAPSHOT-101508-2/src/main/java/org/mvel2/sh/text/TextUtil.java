package org.mvel2.sh.text;

import org.mvel2.util.StringAppender;

import static java.lang.String.valueOf;


public class TextUtil {
    public static String pad(int colLength, int tabPos) {
        StringAppender sAppend = new StringAppender();
        for (int len = tabPos - colLength; len != -1; len--) {
            sAppend.append(' ');
        }

        return sAppend.toString();
    }

    public static String paint(char c, int amount) {
        StringAppender append = new StringAppender();
        for (; amount != -1; amount--) {
            append.append(c);
        }
        return append.toString();
    }

    public static String padTwo(Object first, Object second, int tab) {
        return new StringAppender(valueOf(first)).append(pad(valueOf(first).length(), tab)).append(second).toString();
    }
}
