package org.mvel2.util;

import java.io.OutputStream;
import java.io.IOException;


public class StreamStringAppender extends OutputStream {
    private StringAppender appender;

    public StreamStringAppender(StringAppender appender) {
        this.appender = appender;
    }

    public StreamStringAppender(String encoding) {
        appender = new StringAppender(15, encoding);
    }

    public void write(int b) throws IOException {
        appender.append((byte) b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        appender.append(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
