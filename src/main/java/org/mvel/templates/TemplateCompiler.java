package org.mvel.templates;

import org.mvel.CompileException;
import org.mvel.templates.res.*;
import org.mvel.util.ExecutionStack;
import static org.mvel.util.ParseTools.balancedCaptureWithLineAccounting;
import static org.mvel.util.ParseTools.subset;
import static org.mvel.util.PropertyTools.isIdentifierPart;

import java.util.HashMap;
import java.util.Map;

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
                                    stack.push(n = markTextNode(n).setNext(
                                            new IfNode(start, name, template, captureOrbInternal(), start)));

                                    n.setTerminus(new TerminalNode());

                                    break;

                                case Opcodes.ELSE:
                                    if (!stack.isEmpty() && stack.peek() instanceof IfNode) {
                                        markTextNode(n).setNext((last = (IfNode) stack.pop()).getTerminus());

                                        last.demarcate(last.getTerminus(), template);
                                        last.setNext(n = new IfNode(start, name, template, captureOrbInternal(), start));
                                        n.setTerminus(last.getTerminus());

                                        stack.push(n);
                                    }
                                    break;

                                case Opcodes.FOREACH:
                                    stack.push(
                                            n = markTextNode(n).setNext(new ForEachNode(start, name, template, captureOrbInternal(), start))
                                    );

                                    n.setTerminus(new TerminalNode());

                                    break;

                                case Opcodes.INCLUDE_FILE:
                                    n = markTextNode(n).setNext(
                                            new IncludeNode(start, name, template, captureOrbInternal(), start = cursor + 1)
                                    );
                                    break;

                                case Opcodes.INCLUDE_NAMED:
                                    n = markTextNode(n).setNext(
                                            new NamedIncludeNode(start, name, template, captureOrbInternal(), start = cursor + 1)
                                    );
                                    break;

                                case Opcodes.CODE:
                                    n = markTextNode(n)
                                            .setNext(new CodeNode(start, name, template, captureOrbInternal(), start = cursor + 1));
                                    break;

                                case Opcodes.DECLARE:
                                    stack.push(n = markTextNode(n).setNext(
                                            new DeclareNode(start, name, template, captureOrbInternal(), start = cursor + 1)
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

                                    if (end.demarcate(terminal, template)) n = n.setNext(terminal);
                                    else n = terminal;

                                    break;

                                default:
                                    if (name.length() == 0) {
                                        n = markTextNode(n).setNext(
                                                new ExpressionNode(start, name, template, captureOrbInternal(), start = cursor + 1));
                                    }
                                    else if (customNodes != null && customNodes.containsKey(name)) {
                                        Class<? extends Node> customNode = customNodes.get(name);

                                        try {
                                            (n = markTextNode(n).setNext(customNode.newInstance())).setBegin(start);
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

                                            //throw new CompileException("unable to instantiate custome node class: " + customNode.getName(), template, start, e);
                                        }
                                        catch (IllegalAccessException e) {
                                            throw new RuntimeException("unable to instantiate custom node class: " + customNode.getName());

                                            //  throw new CompileException("unable to instantiate custome node class: " + customNode.getName(), template, start, e);
                                        }
                                    }
                                    else {
                                        throw new RuntimeException("unknown token type: " + name);
                                        //    throw new TemplateSyntaxError("uknown token type: " + name);
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
            CompileException ce = new CompileException("unclosed @" + ((Node) stack.peek()).getName() + " block. expected @end", template, cursor);
            ce.setColumn(cursor - colStart);
            ce.setLineNumber(line);
            throw ce;
        }

        if (start < template.length) {
            n = n.setNext(new TextNode(start, template.length));
        }
        n.setNext(new EndNode());

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
            }
            else {
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
        while ((cursor != length) && isIdentifierPart(template[cursor])) cursor++;
        if (cursor != length && template[cursor] == '{') return newStart;
        return -1;
    }

    public int captureOrbInternal() {
        try {
            int[] r = balancedCaptureWithLineAccounting(template, start = cursor, '{');
            cursor = r[0];
            line += r[1];
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
            return n.setNext(new TextNode(s, lastTextRangeEnding = start - 1));
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
