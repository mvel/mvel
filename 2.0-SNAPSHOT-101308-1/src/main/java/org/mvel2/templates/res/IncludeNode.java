package org.mvel2.templates.res;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.TemplateError;
import org.mvel2.templates.TemplateRuntime;
import static org.mvel2.templates.util.TemplateTools.captureToEOS;
import org.mvel2.util.ExecutionStack;
import static org.mvel2.util.ParseTools.subset;
import org.mvel2.util.StringAppender;

import java.io.*;

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
        this.includeExpression = subset(contents, 0, mark = captureToEOS(contents, 0));
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
            FileInputStream instream = new FileInputStream(file);
            BufferedInputStream bufstream = new BufferedInputStream(instream);

            push(file.getParent());

            byte[] buf = new byte[10];
            int read;
            int i;

            StringAppender appender = new StringAppender();

            while ((read = bufstream.read(buf)) != -1) {
                for (i = 0; i < read; i++) {
                    appender.append((char) buf[i]);
                }
            }

            bufstream.close();
            instream.close();

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
