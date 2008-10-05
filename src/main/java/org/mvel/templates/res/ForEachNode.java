package org.mvel.templates.res;

import org.mvel.CompileException;
import org.mvel.MVEL;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.MapVariableResolverFactory;
import org.mvel.templates.TemplateRuntime;
import org.mvel.templates.TemplateRuntimeError;
import org.mvel.templates.util.ArrayIterator;
import org.mvel.util.ParseTools;
import org.mvel.util.StringAppender;

import java.util.*;

public class ForEachNode extends Node {
    public Node nestedNode;

    private String[] item;
    private String[] expression;

    private char[] sepExpr;

    public ForEachNode() {
    }

    public ForEachNode(int begin, String name, char[] template, int start, int end) {
        super(begin, name, template, start, end);
        configure();
    }

    public Node getNestedNode() {
        return nestedNode;
    }

    public void setNestedNode(Node nestedNode) {
        this.nestedNode = nestedNode;
    }

    public boolean demarcate(Node terminatingnode, char[] template) {
        nestedNode = next;
        next = terminus;

        sepExpr = terminatingnode.getContents();
        if (sepExpr.length == 0) sepExpr = null;

        return false;
    }

    public Object eval(TemplateRuntime runtime, StringAppender appender, Object ctx, VariableResolverFactory factory) {
        Iterator[] iters = new Iterator[item.length];

        Object o;
        for (int i = 0; i < iters.length; i++) {
            if ((o = MVEL.eval(expression[i], ctx, factory)) instanceof Collection) {
                iters[i] = ((Collection) o).iterator();
            }
            else if (o instanceof Object[]) {
                iters[i] = new ArrayIterator((Object[]) o);
            }
            else {
                throw new TemplateRuntimeError("cannot iterate object type: " + o.getClass().getName());
            }
        }

        Map<String, Object> locals = new HashMap<String, Object>();
        MapVariableResolverFactory localFactory = new MapVariableResolverFactory(locals, factory);

        int iterate = iters.length;

        while (true) {
            for (int i = 0; i < iters.length; i++) {
                if (!iters[i].hasNext()) {
                    iterate--;
                    locals.put(item[i], "");
                }
                else {
                    locals.put(item[i], iters[i].next());
                }
            }
            if (iterate != 0) {
                nestedNode.eval(runtime, appender, ctx, localFactory);

                if (sepExpr != null) {
                    for (Iterator it : iters) {
                        if (it.hasNext()) {
                            appender.append(MVEL.eval(sepExpr, ctx, factory));
                            break;
                        }
                    }
                }
            }
            else break;
        }

        return next != null ? next.eval(runtime, appender, ctx, factory) : null;
    }

    private void configure() {
        ArrayList<String> items = new ArrayList<String>();
        ArrayList<String> expr = new ArrayList<String>();

        int start = 0;
        for (int i = 0; i < contents.length; i++) {
            switch (contents[i]) {
                case ':':
                    items.add(ParseTools.createStringTrimmed(contents, start, i - start));
                    start = i + 1;
                    break;
                case ',':
                    if (expr.size() != (items.size() - 1)) {
                        throw new CompileException("unexpected character ',' in foreach tag", cStart + i);
                    }
                    expr.add(ParseTools.createStringTrimmed(contents, start, i - start));
                    start = i + 1;
                    break;
            }
        }

        if (start < contents.length) {
            if (expr.size() != (items.size() - 1)) {
                throw new CompileException("expected character ':' in foreach tag", cEnd);
            }
            expr.add(ParseTools.createStringTrimmed(contents, start, contents.length - start));
        }

        item = new String[items.size()];
        int i = 0;
        for (String s : items) item[i++] = s;

        expression = new String[expr.size()];
        i = 0;
        for (String s : expr) expression[i++] = s;
    }
}
