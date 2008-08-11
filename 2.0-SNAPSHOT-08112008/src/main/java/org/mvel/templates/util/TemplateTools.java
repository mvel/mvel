package org.mvel.templates.util;

import org.mvel.templates.res.Node;
import org.mvel.templates.res.TerminalNode;
import org.mvel.templates.TemplateError;
import static org.mvel.util.ParseTools.balancedCapture;
import org.mvel.util.StringAppender;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import static java.nio.ByteBuffer.allocateDirect;

public class TemplateTools {
    public static Node getLastNode(Node node) {
        Node n = node;
        while (true) {
            if (n.getNext() instanceof TerminalNode) return n;
            n = n.getNext();
        }
    }

    public static int captureToEOS(char[] expression, int cursor) {
        int length = expression.length;
        while (cursor != length) {
            switch (expression[cursor]) {
                case '(':
                case '[':
                case '{':
                    cursor = balancedCapture(expression, cursor, expression[cursor]);
                    break;

                case ';':
                case '}':
                    return cursor;

            }
            cursor++;
        }

        return cursor;
    }

    public static String readInFile(String file) {
        return readInFile(new File(file));
    }

    public static String readInFile(File file) {
        try {
            FileChannel fc = new FileInputStream(file).getChannel();
            ByteBuffer buf = allocateDirect(10);
            StringAppender appender = new StringAppender();
            int read;

            while (true) {
                buf.rewind();
                if ((read = fc.read(buf)) != -1) {
                    buf.rewind();
                    for (; read != 0; read--) {
                        appender.append((char) buf.get());
                    }
                }
                else {
                    break;
                }
            }

            return appender.toString();
        }
        catch (FileNotFoundException e) {
            throw new TemplateError("cannot include template '" + file.getName() + "': file not found.");
        }
        catch (IOException e) {
            throw new TemplateError("unknown I/O exception while including '" + file.getName() + "' (stacktrace nested)", e);
        }
    }

    public static String readStream(InputStream instream) {
        try {
            byte[] buf = new byte[10];
            StringAppender appender = new StringAppender();
            int read;
            while ((read = instream.read(buf)) != -1) {
                for (int i = 0; i < read; i++) {
                    appender.append((char) buf[i]);
                }
            }

            return appender.toString();
        }
        catch (IOException e) {
            throw new TemplateError("unknown I/O exception while including (stacktrace nested)", e);
        }

    }
}
