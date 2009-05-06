package org.mvel2.templates.util.io;

import org.mvel2.templates.util.TemplateOutputStream;
import org.mvel2.util.StringAppender;

/**
 * Created by IntelliJ IDEA.
 * User: christopherbrock
 * Date: 31-Mar-2009
 * Time: 7:36:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class StringAppenderStream implements TemplateOutputStream {
    private StringAppender appender;

    public StringAppenderStream(StringAppender appender) {
        this.appender = appender;
    }

    public TemplateOutputStream append(CharSequence c) {
        appender.append(c);
        return this;
    }

    public TemplateOutputStream append(char[] c) {
        appender.append(c);
        return this;
    }

    @Override
    public String toString() {
        return appender.toString();
    }
}
