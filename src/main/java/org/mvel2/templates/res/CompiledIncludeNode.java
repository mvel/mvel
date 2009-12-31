/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.templates.res;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateError;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.util.TemplateOutputStream;
import org.mvel2.util.ExecutionStack;
import org.mvel2.util.StringAppender;

import java.io.*;

import static org.mvel2.templates.util.TemplateTools.captureToEOS;
import static org.mvel2.util.ParseTools.subset;

public class CompiledIncludeNode extends Node {
    private static final StackThreadLocal<ExecutionStack> relativePathStack;

    private char[] includeExpression;
    private char[] preExpression;

    private Serializable cIncludeExpression;
    private Serializable cPreExpression;
    private long fileDateStamp;
    private CompiledTemplate cFileCache;

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

    public CompiledIncludeNode(int begin, String name, char[] template, int start, int end) {
        this.begin = begin;
        this.name = name;
        this.contents = subset(template, this.cStart = start, (this.end = this.cEnd = end) - start - 1);

        int mark;
        cIncludeExpression = MVEL.compileExpression(this.includeExpression = subset(contents, 0, mark = captureToEOS(contents, 0)));
        if (mark != contents.length)
            cPreExpression = MVEL.compileExpression(this.preExpression = subset(contents, ++mark, contents.length - mark));
    }

    public Object eval(TemplateRuntime runtime, TemplateOutputStream appender, Object ctx, VariableResolverFactory factory) {
        String file = MVEL.executeExpression(cIncludeExpression, ctx, factory, String.class);

        if (this.cPreExpression != null) {
            MVEL.executeExpression(cPreExpression, ctx, factory);
        }

        if (next != null) {
            return next.eval(runtime, appender.append(String.valueOf(TemplateRuntime.eval(readFile(file, ctx, factory), ctx, factory))), ctx, factory);
        } else {
            return appender.append(String.valueOf(MVEL.eval(readFile(file, ctx, factory), ctx, factory)));
        }
    }

    private String readFile(String fileName, Object ctx, VariableResolverFactory factory) {
        File file = getFile(fileName);
        if (fileDateStamp == 0 || fileDateStamp != file.lastModified()) {
            fileDateStamp = file.lastModified();
            cFileCache = TemplateCompiler.compileTemplate(readInFile(file));
        }
        return String.valueOf(TemplateRuntime.execute(cFileCache, ctx, factory));
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

    public static File getFile(String fileName) {
        return new File(String.valueOf(peek()) + "/" + fileName);
    }

    public static String readInFile(File file) {
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
            throw new TemplateError("cannot include template '" + file.getPath() + "': file not found.");
        }
        catch (IOException e) {
            throw new TemplateError("unknown I/O exception while including '" + file.getPath() + "' (stacktrace nested)", e);
        }
    }
}