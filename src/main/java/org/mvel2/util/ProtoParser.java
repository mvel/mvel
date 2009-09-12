package org.mvel2.util;

import org.mvel2.CompileException;
import org.mvel2.ParserContext;
import org.mvel2.ast.Proto;
import org.mvel2.compiler.ExecutableStatement;
import static org.mvel2.util.ParseTools.*;

public class ProtoParser {
    private char[] expr;
    private ParserContext pCtx;
    private int endOffset;

    private int cursor;
    private String protoName;

    String tk1 = null;
    String tk2 = null;

    private Class type;
    private String name;

    public ProtoParser(char[] expr, int offset, int offsetEnd, String protoName, ParserContext pCtx) {
        this.expr = expr;

        this.cursor = offset;
        this.endOffset = offsetEnd;

        this.protoName = protoName;
        this.pCtx = pCtx;
    }

    public Proto parse() {
        Proto proto = new Proto(protoName);

        Mainloop:
        while (cursor < endOffset) {
            skipWhitespace();
            int start = cursor;

            if (tk2 == null) {
                while (cursor < endOffset && isIdentifierPart(expr[cursor])) cursor++;

                if (cursor > start) {
                    tk1 = new String(expr, start, cursor - start);
                }

                skipWhitespace();
            }


            if (cursor == endOffset) {
                throw new CompileException("unexpected end of statement in proto declaration: " + protoName);
            }

            switch (expr[cursor]) {
                case ';':
                    cursor++;
                    calculateDecl();
                    proto.declareReceiver(name, type, null);
                    break;
                case '=':
                    cursor++;
                    skipWhitespace();
                    start = cursor;

                    Loop:
                    while (cursor < endOffset) {
                        switch (expr[cursor]) {
                            case '{':
                            case '[':
                            case '(':
                            case '\'':
                            case '"':
                                cursor = balancedCaptureWithLineAccounting(expr, cursor, expr[cursor], pCtx);
                                break;

                            case ';':
                                break Loop;

                        }
                    }

                    calculateDecl();

                    ExecutableStatement stmt = (ExecutableStatement)
                            subCompileExpression(new String(expr, start, cursor - start), pCtx);
                    proto.declareReceiver(name, type, stmt);


                    break;

                default:
                    start = cursor;
                    while (cursor < endOffset && isIdentifierPart(expr[cursor])) cursor++;
                    if (cursor > start) {
                        tk2 = new String(expr, start, cursor - start);
                        continue Mainloop;
                    }


            }


        }
        return proto;

    }

    private void calculateDecl() {
        if (tk2 != null) {
            try {
                type = ParseTools.findClass(null, tk1, pCtx);
                name = tk2;
            }
            catch (ClassNotFoundException e) {
                throw new CompileException("could not resolve class: " + tk1, e);
            }
        }
        else {
            type = Object.class;
            name = tk1;
        }

    //    System.out.println("declare (type=" + type.getName() + "):" + name);


        tk1 = null;
        tk2 = null;
    }


    private void skipWhitespace() {
        while (cursor < endOffset && isWhitespace(expr[cursor])) cursor++;
    }
}
