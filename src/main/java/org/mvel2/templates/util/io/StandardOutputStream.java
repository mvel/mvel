package org.mvel2.templates.util.io;

import org.mvel2.templates.util.TemplateOutputStream;

import java.io.OutputStream;
import java.io.PrintWriter;

public class StandardOutputStream implements TemplateOutputStream {
    private OutputStream outputStream;
    private PrintWriter printWriter;

    public StandardOutputStream(OutputStream outputStream) {
        this.printWriter = new PrintWriter(this.outputStream = outputStream);
    }

    public TemplateOutputStream append(CharSequence c) {
        printWriter.append(c);
        return this;
    }

    public TemplateOutputStream append(char[] c) {
        for (char i : c) {
            printWriter.append(i);
        }
        return this;
    }

    @Override
    public String toString() {
        return null;
    }
}
