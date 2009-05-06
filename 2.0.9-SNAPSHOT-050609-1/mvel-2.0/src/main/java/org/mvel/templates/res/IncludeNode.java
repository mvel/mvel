package org.mvel.templates.res;

import org.mvel.util.StringAppender;
import org.mvel.util.ExecutionStack;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.subset;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.templates.TemplateError;
import org.mvel.templates.TemplateRuntime;
import org.mvel.templates.util.TemplateTools;
import static org.mvel.templates.util.TemplateTools.captureToEOS;
import org.mvel.MVEL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import static java.nio.ByteBuffer.allocateDirect;

public class IncludeNode extends Node {
    private static final StackThreadLocal<ExecutionStack> relativePathStack;

    private char[] includeExpression;
    private char[] preExpression;

    private static class StackThreadLocal<T> extends ThreadLocal<T> {
        protected T initialValue() {
            ExecutionStack stk = new ExecutionStack();
            stk.push(".");
            return (T) stk;
        }
    }

    static {
        relativePathStack = new StackThreadLocal<ExecutionStack>();
    }

    public IncludeNode(int begin, String name, char[] template, int start, int end) {
        this.begin = begin;
        this.name = name;
        this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
                                        
        int mark;
        this.includeExpression = subset(contents, 0 , mark = captureToEOS(contents, 0));
        if (mark != contents.length) this.preExpression = subset(contents, ++mark, contents.length - mark);
    }

    public IncludeNode(int begin, String name, char[] template, int start, int end, Node next) {
        this.name = name;
        this.begin = begin;
        this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);
        this.next = next;
    }


    public Object eval(TemplateRuntime runtime, StringAppender appender, Object ctx, VariableResolverFactory factory) {
        String file = MVEL.eval(includeExpression, ctx, factory, String.class);

        if (this.preExpression != null) {
            MVEL.eval(preExpression, ctx, factory);
        }

        if (next != null) {            
            return next.eval(runtime, appender.append(TemplateRuntime.eval(readInFile(file), ctx, factory)), ctx, factory);
        }
        else {
            return appender.append(MVEL.eval(readInFile(file), ctx, factory));
        }
    }

    public boolean demarcate(Node terminatingNode, char[] template) {
        return false;
    }

    private static Object peek() {
        return relativePathStack.get().peek();
    }

    public static Object pop() {
        return relativePathStack.get().pop();
    }

    private static void push(String path) {
        relativePathStack.get().push(path);
    }

    public static String readInFile(String fileName) {
        File file = new File(String.valueOf(peek()) + "/" + fileName);

        try {
            FileChannel fc = new FileInputStream(file).getChannel();

            push(file.getParent());

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

            pop();

            return appender.toString();

        }
        catch (FileNotFoundException e) {
            throw new TemplateError("cannot include template '" + fileName + "': file not found.");
        }
        catch (IOException e) {
            throw new TemplateError("unknown I/O exception while including '" + fileName + "' (stacktrace nested)", e);
        }
    }
}
