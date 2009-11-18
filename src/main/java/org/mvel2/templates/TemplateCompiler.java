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

package org.mvel2.templates;

import org.mvel2.CompileException;
import org.mvel2.ParserContext;
import org.mvel2.compiler.AbstractParser;
import org.mvel2.templates.res.*;
import org.mvel2.templates.util.TemplateTools;
import org.mvel2.util.ExecutionStack;
import org.mvel2.util.ParseTools;

import static org.mvel2.util.ParseTools.balancedCaptureWithLineAccounting;
import static org.mvel2.util.ParseTools.subset;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The TemplateCompiler class is used for pre-compiling MVEL Templates.  To execute a compiled template see
 * {@link TemplateRuntime}
 *
 * @author Mike Brock
 */
@SuppressWarnings({"ManualArrayCopy"})
public class TemplateCompiler {
    private char[] template;
    private int length;

    private int start;
    private int cursor;
    private int lastTextRangeEnding;

    private int line;
    private int colStart;

    private Map<String, Class<? extends Node>> customNodes;

    private static final Map<String, Integer> OPCODES = new HashMap<String, Integer>();

    static {
        OPCODES.put("if", Opcodes.IF);
        OPCODES.put("else", Opcodes.ELSE);
        OPCODES.put("elseif", Opcodes.ELSE);
        OPCODES.put("end", Opcodes.END);
        OPCODES.put("foreach", Opcodes.FOREACH);

        OPCODES.put("includeNamed", Opcodes.INCLUDE_NAMED);
        OPCODES.put("include", Opcodes.INCLUDE_FILE);
        OPCODES.put("comment", Opcodes.COMMENT);
        OPCODES.put("code", Opcodes.CODE);

        OPCODES.put("declare", Opcodes.DECLARE);

        OPCODES.put("stop", Opcodes.STOP);
    }


    public CompiledTemplate compile() {
        return new CompiledTemplate(template, compileFrom(null, new ExecutionStack()));
    }

    public Node compileFrom(Node root, ExecutionStack stack) {
        line = 1;

        Node n = root;
        if (root == null) {
            n = root = new TextNode(0, 0);
        }

        IfNode last;
        Integer opcode;
        String name;
        int x;

        try {
            while (cursor < length) {
                switch (template[cursor]) {
                    case '\n':
                        line++;
                        colStart = cursor + 1;
                        break;
                    case '@':
                    case '$':
                        if (isNext(template[cursor])) {
                            start = ++cursor;
                            (n = markTextNode(n)).setEnd(n.getEnd() + 1);
                            lastTextRangeEnding = ++cursor;

                            continue;
                        }
                        if ((x = captureOrbToken()) != -1) {
                            start = x;
                            switch ((opcode = OPCODES.get(name = new String(capture()))) == null ? 0 : opcode) {
                                case Opcodes.IF:
                                    /**
                                     * Capture any residual text node, and push the if statement on the nesting stack.
                                     */
                                    stack.push(n = markTextNode(n).next =
                                            new IfNode(start, name, template, captureOrbInternal(), start));

                                    n.setTerminus(new TerminalNode());

                                    break;

                                case Opcodes.ELSE:
                                    if (!stack.isEmpty() && stack.peek() instanceof IfNode) {
                                        markTextNode(n).next = (last = (IfNode) stack.pop()).getTerminus();

                                        last.demarcate(last.getTerminus(), template);
                                        last.next = n = new IfNode(start, name, template, captureOrbInternal(), start);
                                        n.setTerminus(last.getTerminus());

                                        stack.push(n);
                                    }
                                    break;

                                case Opcodes.FOREACH:
                                    stack.push(
                                            n = markTextNode(n).next = new ForEachNode(start, name, template, captureOrbInternal(), start)
                                    );

                                    n.setTerminus(new TerminalNode());

                                    break;

                                case Opcodes.INCLUDE_FILE:
                                    n = markTextNode(n).next =
                                            new IncludeNode(start, name, template, captureOrbInternal(), start = cursor + 1
                                            );
                                    break;

                                case Opcodes.INCLUDE_NAMED:
                                    n = markTextNode(n).next =
                                            new NamedIncludeNode(start, name, template, captureOrbInternal(), start = cursor + 1
                                            );
                                    break;

                                case Opcodes.CODE:
                                    n = markTextNode(n)
                                            .next = new CodeNode(start, name, template, captureOrbInternal(), start = cursor + 1);
                                    break;

                                case Opcodes.COMMENT:
                                    n = markTextNode(n)
                                            .next = new CommentNode(start, name, template, captureOrbInternal(), start = cursor + 1);

                                    break;

                                case Opcodes.DECLARE:
                                    stack.push(n = markTextNode(n).next =
                                            new DeclareNode(start, name, template, captureOrbInternal(), start = cursor + 1
                                            ));

                                    n.setTerminus(new TerminalNode());

                                    break;

                                case Opcodes.END:
                                    n = markTextNode(n);

                                    Node end = (Node) stack.pop();
                                    Node terminal = end.getTerminus();

                                    terminal.setCStart(captureOrbInternal());
                                    terminal.setEnd((lastTextRangeEnding = start) - 1);
                                    terminal.calculateContents(template);

                                    if (end.demarcate(terminal, template)) n = n.next = terminal;
                                    else n = terminal;

                                    break;

                                default:
                                    if (name.length() == 0) {
                                        n = markTextNode(n).next =
                                                new ExpressionNode(start, name, template, captureOrbInternal(), start = cursor + 1);
                                    } else if (customNodes != null && customNodes.containsKey(name)) {
                                        Class<? extends Node> customNode = customNodes.get(name);

                                        try {
                                            (n = markTextNode(n).next = (customNode.newInstance())).setBegin(start);
                                            n.setName(name);
                                            n.setCStart(captureOrbInternal());
                                            n.setCEnd(start = cursor + 1);
                                            n.setEnd(n.getCEnd());

                                            n.setContents(subset(template, n.getCStart(), n.getCEnd() - n.getCStart() - 1));

                                            if (n.isOpenNode()) {
                                                stack.push(n);
                                            }
                                        }
                                        catch (InstantiationException e) {
                                            throw new RuntimeException("unable to instantiate custom node class: " + customNode.getName());
                                        }
                                        catch (IllegalAccessException e) {
                                            throw new RuntimeException("unable to instantiate custom node class: " + customNode.getName());
                                        }
                                    } else {
                                        throw new RuntimeException("unknown token type: " + name);
                                    }

                            }
                        }
                }
                cursor++;
            }
        }
        catch (RuntimeException e) {
            CompileException ce = new CompileException(e.getMessage());
            ce.setExpr(template);

            if (e instanceof CompileException) {
                CompileException ce2 = (CompileException) e;
                if (ce2.getCursor() != -1) {
                    ce.setCursor(ce2.getCursor());
                    if (ce2.getColumn() == -1) ce.setColumn(ce.getCursor() - colStart);
                    else ce.setColumn(ce2.getColumn());
                }
            }
            ce.setLineNumber(line);

            throw ce;
        }

        if (!stack.isEmpty()) {
            CompileException ce = new CompileException("unclosed @" + ((Node) stack.peek()).getName() + "{} block. expected @end{}", template, cursor);
            ce.setColumn(cursor - colStart);
            ce.setLineNumber(line);
            throw ce;
        }

        if (start < template.length) {
            n = n.next = new TextNode(start, template.length);
        }
        n.next = new EndNode();

        n = root;
        do {
            if (n.getLength() != 0) {
                break;
            }
        }
        while ((n = n.getNext()) != null);

        if (n != null && n.getLength() == template.length - 1) {
            if (n instanceof ExpressionNode) {
                return new TerminalExpressionNode(n);
            } else {
                return n;
            }
        }

        return root;
    }

    // Parse Utilities Start Here

    public boolean isNext(char c) {
        return cursor != length && template[cursor + 1] == c;
    }

    public int captureOrbToken() {
        int newStart = ++cursor;
        while ((cursor != length) && ParseTools.isIdentifierPart(template[cursor])) cursor++;
        if (cursor != length && template[cursor] == '{') return newStart;
        return -1;
    }

    public int captureOrbInternal() {
        try {
            ParserContext pCtx = new ParserContext();
            cursor = balancedCaptureWithLineAccounting(template, start = cursor, '{', pCtx);
            line += pCtx.getLineCount();
            int ret = start + 1;
            start = cursor + 1;
            return ret;
        }
        catch (CompileException e) {
            e.setLineNumber(line);
            e.setColumn((cursor - colStart) + 1);
            throw e;
        }
    }

    public char[] capture() {
        char[] newChar = new char[cursor - start];
        for (int i = 0; i < newChar.length; i++) {
            newChar[i] = template[i + start];
        }
        return newChar;
    }

    public Node markTextNode(Node n) {
        int s = (n.getEnd() > lastTextRangeEnding ? n.getEnd() : lastTextRangeEnding);

        if (s < start) {
            return n.next = new TextNode(s, lastTextRangeEnding = start - 1);
        }
        return n;
    }

    public static CompiledTemplate compileTemplate(String template) {
        return new TemplateCompiler(template).compile();
    }

    public static CompiledTemplate compileTemplate(char[] template) {
        return new TemplateCompiler(template).compile();
    }

    public static CompiledTemplate compileTemplate(CharSequence template) {
        return new TemplateCompiler(template).compile();
    }

    public static CompiledTemplate compileTemplate(String template, Map<String, Class<? extends Node>> customNodes) {
        return new TemplateCompiler(template, customNodes).compile();
    }

    public static CompiledTemplate compileTemplate(char[] template, Map<String, Class<? extends Node>> customNodes) {
        return new TemplateCompiler(template, customNodes).compile();
    }

    public static CompiledTemplate compileTemplate(CharSequence template, Map<String, Class<? extends Node>> customNodes) {
        return new TemplateCompiler(template, customNodes).compile();
    }

    public static CompiledTemplate compileTemplate(InputStream stream) {
        return compileTemplate(stream, null);
    }

    public static CompiledTemplate compileTemplate(InputStream stream, Map<String, Class<? extends Node>> customNodes) {
        return new TemplateCompiler(TemplateTools.readStream(stream), customNodes).compile();
    }

    public static CompiledTemplate compileTemplate(File file) {
        return compileTemplate(file, null);
    }

    public static CompiledTemplate compileTemplate(File file, Map<String, Class<? extends Node>> customNodes) {
        return new TemplateCompiler(TemplateTools.readInFile(file), customNodes).compile();
    }

    public TemplateCompiler(String template) {
        this.length = (this.template = template.toCharArray()).length;
    }

    public TemplateCompiler(char[] template) {
        this.length = (this.template = template).length;
    }

    public TemplateCompiler(CharSequence sequence) {
        this.length = (this.template = sequence.toString().toCharArray()).length;
    }

    public TemplateCompiler(String template, Map<String, Class<? extends Node>> customNodes) {
        this.length = (this.template = template.toCharArray()).length;
        this.customNodes = customNodes;
    }

    public TemplateCompiler(char[] template, Map<String, Class<? extends Node>> customNodes) {
        this.length = (this.template = template).length;
        this.customNodes = customNodes;
    }

    public TemplateCompiler(CharSequence sequence, Map<String, Class<? extends Node>> customNodes) {
        this.length = (this.template = sequence.toString().toCharArray()).length;
        this.customNodes = customNodes;
    }

}
