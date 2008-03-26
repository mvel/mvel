package org.mvel.templates;

import org.mvel.templates.res.*;
import org.mvel.util.ExecutionStack;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.balancedCapture;
import static org.mvel.util.ParseTools.balancedCaptureWithLineAccounting;
import static org.mvel.util.PropertyTools.isIdentifierPart;
import org.mvel.CompileException;

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

    public static CompiledTemplate compileTemplate(String template) {
        return new TemplateCompiler(template).compile();
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
                                //todo: delimiter

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
                                n = markTextNode(n).setNext(
                                        new ExpressionNode(start, name, template, captureOrbInternal(), start = cursor + 1));
                        }
                    }
            }
            cursor++;
        }

        if (!stack.isEmpty()) throw new TemplateSyntaxError("unbalanced tokens. expected @end");

        if (start < template.length) {
            n = n.setNext(new TextNode(start, template.length));
        }
        n.setNext(new EndNode());
//
//        if (root.getNext() != null && (n = root.getNext()) instanceof ExpressionNode && n.getNext() instanceof EndNode) {
//            return new TerminalExpressionNode(n);
//        }

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
            e.setColumn((cursor - colStart)+1);
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

    public TemplateCompiler(String template) {
        this.length = (this.template = template.toCharArray()).length;
    }

    public TemplateCompiler(char[] template) {
        this.length = (this.template = template).length;
    }

    public TemplateCompiler(CharSequence sequence) {
        this.length = (this.template = sequence.toString().toCharArray()).length;
    }
}
