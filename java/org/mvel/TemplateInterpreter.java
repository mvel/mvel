/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
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
 *
 */

package org.mvel;

import static org.mvel.MVEL.compileExpression;
import static org.mvel.MVEL.executeExpression;
import static org.mvel.NodeType.*;
import org.mvel.TemplateCompiler.IncludeRef;
import org.mvel.TemplateCompiler.IncludeRefParam;
import org.mvel.util.ExecutionStack;
import org.mvel.util.StringAppender;

import java.io.*;
import static java.lang.String.valueOf;
import static java.lang.System.arraycopy;
import java.nio.ByteBuffer;
import static java.nio.ByteBuffer.allocateDirect;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import static java.util.Collections.synchronizedMap;

/**
 * The MVEL Template Interpreter.  Naming this an "Interpreter" is not inaccurate.   All template expressions
 * are pre-compiled by the the {@link TemplateCompiler} prior to being processed by this interpreter.<br/>
 * <br/>
 * Under normal circumstances, it is completely acceptable to execute the parser/interpreter from the static
 * convenience methods in this class.
 *
 * @author Christopher Brock
 */
public class TemplateInterpreter {

    public static boolean cacheAggressively = false;

    /**
     * Evaluates the template expression and returns a String value.  This is only a convenience method that
     * has the same semantics as using <tt>String.valueOf(eval(expr, vars, ctx))</tt>.
     *
     * @param template - the template to be evaluated
     * @param ctx      - the virtual root / context of the expression.
     * @return the resultant value represented in it's equivelant string value.
     */
    public static String evalToString(String template, Object ctx) {
        return valueOf(eval(template, ctx));
    }


    /**
     * Evaluates the template expression and returns a String value.  This is only a convenience method that
     * has the same semantics as using <tt>String.valueOf(eval(expr, vars, ctx))</tt>.
     *
     * @param template  - the template to be evaluated
     * @param variables - a map of variables for use in the expression.
     * @return the resultant value represented in it's equivelant string value.
     */
    public static String evalToString(String template, Map variables) {
        return valueOf(eval(template, variables));
    }

    /**
     * Evaluates the template expression and returns a String value.  This is only a convenience method that
     * has the same semantics as using <tt>String.valueOf(eval(expr, vars, ctx))</tt>.
     *
     * @param template  - the template to be evaluated
     * @param ctx       - the virtual root / context of the expression.
     * @param variables - a map of variables for use in the expression.
     * @return the resultant value represented in it's equivelant string value.
     */
    public static String evalToString(String template, Object ctx, Map variables) {
        return valueOf(eval(template, ctx, variables));
    }

    /**
     * @param template - the template to be evaluated
     * @param ctx      - the virtual root / context of the expression.
     * @return see description.
     * @see #eval(String,Object,Map)
     */
    public static Object eval(String template, Object ctx) {
        if (template == null) return null;
        return new TemplateInterpreter(template).execute(ctx, null);
    }

    /**
     * @param template  - the template to be evaluated
     * @param variables - a map of variables for use in the expression.
     * @return see description.
     * @see #eval(String,Object,Map)
     */
    public static Object eval(String template, Map variables) {
        //noinspection unchecked
        return new TemplateInterpreter(template).execute(null, variables);
    }

    /**
     * Compiles, interprets and returns the result from a template.  The value that this returns is dependant
     * on whether or not the template actually contains any literal values.<br/>
     * <br/>
     * For example, an expression that is simply "<tt>@{foobar}</tt>" will return the value of <tt>foobar</tt>,
     * not a string value.  An expression that only contains a single tag is a defacto expression and is not
     * considered a template.<br/>
     * <br/>
     * An expression such as "<tt>Hello my name is: @{name}</tt>" will return the a String value as it clearly a
     * template.<br/>
     *
     * @param template  - the template to be evaluated
     * @param ctx       - the virtual root / context of the expression.
     * @param variables - a map of variables for use in the expression.
     * @return see description.
     */
    public static Object eval(String template, Object ctx, Map variables) {
        if (template == null) return null;
        //noinspection unchecked
        return new TemplateInterpreter(template).execute(ctx, variables);
    }

    private char[] expression;
    private boolean debug = false;
    private Node[] nodes;
    private int node = 0;

    private static final Map<CharSequence, char[]> EX_PRECACHE;
    private static final Map<Object, Node[]> EX_NODE_CACHE;
    private static final Map<Object, Serializable> EX_PRECOMP_CACHE;
    private static boolean CACHE_DISABLE = false;

    static {
        if (MVEL.THREAD_SAFE) {
            EX_PRECACHE = synchronizedMap(new WeakHashMap<CharSequence, char[]>());
            EX_NODE_CACHE = synchronizedMap(new WeakHashMap<Object, Node[]>());
            EX_PRECOMP_CACHE = synchronizedMap(new WeakHashMap<Object, Serializable>());
        }
        else if (MVEL.WEAK_CACHE || MVEL.NO_JIT) {
            EX_PRECACHE = (new WeakHashMap<CharSequence, char[]>());
            EX_NODE_CACHE = (new WeakHashMap<Object, Node[]>());
            EX_PRECOMP_CACHE = (new WeakHashMap<Object, Serializable>());
        }
        else {
            EX_PRECACHE = (new HashMap<CharSequence, char[]>());
            EX_NODE_CACHE = (new HashMap<Object, Node[]>());
            EX_PRECOMP_CACHE = (new HashMap<Object, Serializable>());
        }
    }

    static void configureFactory() {
    }

    private ExecutionStack stack;

    /**
     * Creates a new intepreter
     *
     * @param template -
     */
    public TemplateInterpreter(CharSequence template) {
        if (CACHE_DISABLE) {
            nodes = new TemplateCompiler(this).compileExpression();
        }
        else if (!EX_PRECACHE.containsKey(template)) {
            EX_PRECACHE.put(template, this.expression = template.toString().toCharArray());
            nodes = new TemplateCompiler(this).compileExpression();
            Node[] nodes = cloneAll(EX_NODE_CACHE.get(expression));

            EX_NODE_CACHE.put(template, nodes);
        }
        else {
            this.expression = EX_PRECACHE.get(template);
            try {
                this.nodes = cloneAll(EX_NODE_CACHE.get(expression));
            }
            catch (NullPointerException e) {
                EX_NODE_CACHE.remove(expression);
                nodes = new TemplateCompiler(this).compileExpression();
                EX_NODE_CACHE.put(expression, cloneAll(nodes));
            }

        }

        //    cloneAllNodes();

    }

    private Node[] cloneAll(Node[] nodes) {
        Node[] newNodes = new Node[nodes.length];

        try {
            int i = 0;
            for (Node n : nodes) {
                newNodes[i++] = n.clone();
            }
        }
        catch (CloneNotSupportedException e) {

        }

        return newNodes;
    }


    public TemplateInterpreter(String expression) {
        if (CACHE_DISABLE) {
            nodes = new TemplateCompiler(this).compileExpression();
        }
        else if (!EX_PRECACHE.containsKey(expression)) {
            EX_PRECACHE.put(expression, this.expression = expression.toCharArray());
            nodes = new TemplateCompiler(this).compileExpression();
            EX_NODE_CACHE.put(expression, nodes);
            this.nodes = cloneAll(nodes);
        }
        else {
            this.expression = EX_PRECACHE.get(expression);
            try {
                this.nodes = cloneAll(EX_NODE_CACHE.get(expression));
            }
            catch (NullPointerException e) {
                EX_NODE_CACHE.remove(expression);
                nodes = new TemplateCompiler(this).compileExpression();
                EX_NODE_CACHE.put(expression, nodes);
                this.nodes = cloneAll(nodes);
            }
        }

    }

    public TemplateInterpreter(char[] expression) {
        this.expression = expression;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public static void parseToStream(File template, Object ctx, Map tokens, OutputStream out)
            throws IOException {

        //noinspection unchecked
        Object result = parse(template, ctx, tokens);
        CharSequence cs;

        if (result == null) return;
        else if (result instanceof CharSequence) {
            cs = (CharSequence) result;
        }
        else {
            cs = valueOf(result);
        }

        OutputStreamWriter writer = new OutputStreamWriter(out);

        int len = cs.length();
        for (int i = 0; i < len; i++) {
            writer.write(cs.charAt(i));
        }
        writer.flush();
        writer.close();
    }

    public static Object parse(File file, Object ctx, Map tokens) throws IOException {
        return parse(file, ctx, tokens, null);
    }

    public static Object parse(File file, Object ctx, Map tokens, TemplateRegistry registry) throws IOException {

        if (!file.exists())
            throw new CompileException("cannot find file: " + file.getName());

        FileInputStream inStream = null;
        ReadableByteChannel fc = null;
        try {
            inStream = new FileInputStream(file);
            fc = inStream.getChannel();
            ByteBuffer buf = allocateDirect(10);

            StringAppender sb = new StringAppender((int) file.length());

            int read = 0;
            while (read >= 0) {
                buf.rewind();
                read = fc.read(buf);
                buf.rewind();

                for (; read > 0; read--) {
                    sb.append((char) buf.get());
                }
            }

            //noinspection unchecked
            return parse(sb, ctx, tokens, registry);

        }
        catch (FileNotFoundException e) {
            // this can't be thrown, we check for this explicitly.
        }
        finally {
            if (inStream != null) inStream.close();
            if (fc != null) fc.close();
        }

        return null;
    }

    public static Object parse(CharSequence expression, Object ctx, Map vars) {
        return parse(expression, ctx, vars, null);
    }

    public static Object parse(CharSequence expression, Object ctx, Map vars, TemplateRegistry registry) {
        if (expression == null) return null;
        //noinspection unchecked
        return new TemplateInterpreter(expression).execute(ctx, vars, registry);
    }

    public static Object parse(String expression, Object ctx, Map vars) {
        return parse(expression, ctx, vars, null);
    }

    public static Object parse(String expression, Object ctx, Map vars, TemplateRegistry registry) {
        if (expression == null) return null;

        //noinspection unchecked
        return new TemplateInterpreter(expression).execute(ctx, vars, registry);
    }

    public Object execute(Object ctx, Map tokens) {
        return execute(ctx, tokens, null);
    }

    public Object execute(Object ctx, Map tokens, TemplateRegistry registry) {

        if (nodes == null) {
            return new String(expression);
        }
        else if (nodes.length == 2) {
            /**
             * This is an optimization for property expressions.
             */
            switch (nodes[0].getToken()) {
                case PROPERTY_EX:
                    //noinspection unchecked
                    if (CACHE_DISABLE || !cacheAggressively) {
                        char[] seg = new char[expression.length - 3];
                        arraycopy(expression, 2, seg, 0, seg.length);

                        return MVEL.eval(seg, ctx, tokens);
                    }
                    else {
                        String s = new String(expression, 2, expression.length - 3);
                        if (!EX_PRECOMP_CACHE.containsKey(s)) {
                            synchronized (EX_PRECOMP_CACHE) {
                                EX_PRECOMP_CACHE.put(s, compileExpression(s));
                                return executeExpression(EX_PRECOMP_CACHE.get(s), ctx, tokens);
                            }
                        }
                        else {
                            return executeExpression(EX_PRECOMP_CACHE.get(s), ctx, tokens);
                        }

                    }
                case LITERAL:
                    return new String(expression);

            }

            return new String(expression);
        }

        Object register = null;

        StringAppender sbuf = new StringAppender(10);
        Node currNode = null;

        try {
            //noinspection unchecked
            MVELInterpretedRuntime oParser = new MVELInterpretedRuntime(ctx, tokens);

            initStack();
            pushAndForward();

            while ((currNode = pop()) != null) {
                node = currNode.getNode();

                switch (currNode.getToken()) {
                    case LITERAL: {
                        sbuf.append(register = new String(expression, currNode.getStartPos(),
                                currNode.getEndPos() - currNode.getStartPos()));
                        break;
                    }
                    case PROPERTY_EX: {
                        sbuf.append(
                                valueOf(register = oParser.setExpressionArray(getInternalSegment(currNode)).parse())
                        );
                        break;
                    }
                    case IF:
                    case ELSEIF: {
                        try {
                            if (!((Boolean) oParser.setExpressionArray(getInternalSegment(currNode)).parse())) {
                                exitContext();
                            }
                        }
                        catch (ClassCastException e) {
                            throw new CompileException("IF expression does not return a boolean: " + new String(getSegment(currNode)));
                        }
                        break;
                    }
                    case FOREACH: {
                        ForeachContext foreachContext = (ForeachContext) currNode.getRegister();

                        if (tokens == null) {
                            tokens = new HashMap();
                        }

                        if (foreachContext.getItererators() == null) {
                            try {
                                String[] lists = getForEachSegment(currNode).split(",");
                                Iterator[] iters = new Iterator[lists.length];
                                for (int i = 0; i < lists.length; i++) {
                                    //noinspection unchecked
                                    Object listObject = new MVELInterpretedRuntime(lists[i], ctx, tokens).parse();
                                    if (listObject instanceof Object[]) {
                                        listObject = Arrays.asList((Object[]) listObject);
                                    }
                                    iters[i] = ((Collection) listObject).iterator();
                                }
                                foreachContext.setIterators(iters);
                            }
                            catch (ClassCastException e) {
                                throw new CompileException("expression for collections does not return a collections object: " + new String(getSegment(currNode)));
                            }
                            catch (NullPointerException e) {
                                throw new CompileException("null returned for foreach in expression: " + (getForEachSegment(currNode)));
                            }
                        }

                        Iterator[] iters = foreachContext.getItererators();
                        String[] alias = currNode.getAlias().split(",");
                        // must trim vars
                        for (int i = 0; i < alias.length; i++) {
                            alias[i] = alias[i].trim();
                        }

                        if (iters[0].hasNext()) {
                            push();

                            //noinspection unchecked
                            for (int i = 0; i < iters.length; i++) {

                                //noinspection unchecked
                                tokens.put(alias[i], iters[i].next());
                            }
                            if (foreachContext.getCount() != 0) {
                                sbuf.append(foreachContext.getSeperator());
                            }
                            //noinspection unchecked
                            tokens.put("i0", foreachContext.getCount());
                            foreachContext.setCount(foreachContext.getCount() + 1);
                        }
                        else {
                            for (int i = 0; i < iters.length; i++) {
                                tokens.remove(alias[i]);
                            }
                            foreachContext.setIterators(null);
                            foreachContext.setCount(0);
                            exitContext();
                        }
                        break;
                    }
                    case ELSE:
                    case END:
                        if (stack.isEmpty()) forwardAndPush();
                        continue;
                    case GOTO:
                        pushNode(currNode.getEndNode());
                        continue;
                    case TERMINUS: {
                        if (nodes.length != 2) {
                            return sbuf.toString();
                        }
                        else {
                            return register;
                        }
                    }
                    case INCLUDE_BY_REF: {
                        IncludeRef includeRef = (IncludeRef) nodes[node].getRegister();

                        IncludeRefParam[] params = includeRef.getParams();
                        Map<String, Object> vars = new HashMap<String, Object>(params.length * 2);
                        for (IncludeRefParam param : params) {
                            vars.put(param.getIdentifier(), MVEL.eval(param.getValue(), ctx, tokens));
                        }

                        if (registry == null) {
                            throw new CompileException("No TemplateRegistry specified, cannot load template='" + includeRef.getName() + "'");
                        }
                        String template = registry.getTemplate(includeRef.getName());

                        if (template == null) {
                            throw new CompileException("Template does not exist in the TemplateRegistry, cannot load template='" + includeRef.getName() + "'");
                        }

                        sbuf.append(TemplateInterpreter.parse(template, ctx, vars, registry));
                    }
                }

                forwardAndPush();
            }
            throw new CompileException("expression did not end properly: expected TERMINUS node");
        }
        catch (CompileException e) {
            throw e;
        }
        catch (Exception e) {
            if (currNode != null) {
                throw new CompileException("problem encountered at node [" + currNode.getNode() + "] "
                        + currNode.getToken() + "{" + currNode.getStartPos() + "," + currNode.getEndPos() + "}: " + e.getMessage(), e);
            }
            throw new CompileException("unhandled fatal exception (node:" + node + ")", e);
        }

    }

    private void initStack() {
        stack = new ExecutionStack();
    }

    private void push() {
        push(nodes[node]);
    }

    private void push(Node node) {
        if (node == null) return;
        stack.push(node);
    }

    private void pushNode(int i) {
        stack.push(nodes[i]);
    }

    private void exitContext() {
        node = nodes[node].getEndNode() - 1;
    }

    public void forwardAndPush() {
        node++;
        push();
    }

    private void pushAndForward() {
        push();
        node++;
    }

    private Node pop() {
        return (Node) stack.pop();
    }

    /**
     * @param expression -
     * @param ctx        -
     * @param tokens     -
     * @return -
     * @deprecated
     */
    public static Object getValuePE(String expression, Object ctx, Map tokens) {
        return new TemplateInterpreter(expression).execute(ctx, tokens);
    }


    /**
     * @param expression -
     * @param ctx        -
     * @param preParseCx -
     * @param value      -
     * @deprecated
     */
    public static void setValuePE(String expression, Object ctx, Object preParseCx, Object value) {
        PropertyAccessor.set(ctx, valueOf(eval(expression, preParseCx)), value);
    }

    public char[] getExpression() {
        return expression;
    }

    public void setExpression(char[] expression) {
        this.expression = expression;
    }

    private char[] getSegment(Node n) {
        char[] ca = new char[n.getLength()];
        arraycopy(expression, n.getStartPos(), ca, 0, ca.length);
        return ca;
    }

    private char[] getInternalSegment(Node n) {
        int start = n.getStartPos();
        int depth = 1;

        //noinspection StatementWithEmptyBody
        while ((expression[start++] != '{')) ;

        int end = start;
        while (depth > 0) {
            switch (expression[++end]) {
                case '}':
                    depth--;
                    break;
                case '{':
                    depth++;
                    break;
            }
        }

        char[] ca = new char[end - start];
        arraycopy(expression, start, ca, 0, ca.length);
        return ca;
    }

    private String getForEachSegment(Node n) {
        if (n.getAlias() == null) return new String(getInternalSegment(n));
        else {
            return n.getName();
        }
    }

    public static boolean isCacheAggressively() {
        return cacheAggressively;
    }

    public static void setCacheAggressively(boolean cacheAggressively) {
        TemplateInterpreter.cacheAggressively = cacheAggressively;
    }

    public static void setDisableCache(boolean disableCache) {
        CACHE_DISABLE = disableCache;
    }
}
