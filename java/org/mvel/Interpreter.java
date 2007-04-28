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

import static org.mvel.NodeType.*;

import org.mvel.TemplateCompiler.IncludeRef;
import org.mvel.TemplateCompiler.IncludeRefParam;
import org.mvel.util.ExecutionStack;
import org.mvel.util.StringAppender;

import java.io.*;

import static java.lang.Character.isWhitespace;
import static java.lang.String.valueOf;
import static java.lang.System.arraycopy;
import java.nio.ByteBuffer;
import static java.nio.ByteBuffer.allocateDirect;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import static java.util.Collections.synchronizedMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The MVEL Template Interpreter.  Naming this an "Interpreter" is not inaccurate.   All template expressions
 * are pre-compiled by the the {@link TemplateCompiler} prior to being processed by this interpreter.<br/>
 * <br/>
 * Under normal circumstances, it is completely acceptable to execute the parser/interpreter from the static
 * convenience methods in this class.
 *
 * @author Christopher Brock
 */
public class Interpreter {

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
        return new Interpreter(template).execute(ctx, null);
    }

    /**
     * @param template  - the template to be evaluated
     * @param variables - a map of variables for use in the expression.
     * @return see description.
     * @see #eval(String,Object,Map)
     */
    public static Object eval(String template, Map variables) {
        return new Interpreter(template).execute(null, variables);
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
        return new Interpreter(template).execute(ctx, variables);
    }

    private char[] expression;
    private boolean debug = false;
    private Node[] nodes;
    private int node = 0;

    private static final Map<CharSequence, char[]> EX_PRECACHE;
    private static final Map<Object, Node[]> EX_NODE_CACHE;
    private static final Map<Object, Serializable> EX_PRECOMP_CACHE;
    
    private static final Map<String, String> EX_TEMPLATE_REGISTRY;

    static {
        if (MVEL.THREAD_SAFE) {
            EX_PRECACHE = synchronizedMap(new WeakHashMap<CharSequence, char[]>());
            EX_NODE_CACHE = synchronizedMap(new WeakHashMap<Object, Node[]>());
            EX_PRECOMP_CACHE = synchronizedMap(new WeakHashMap<Object, Serializable>());
            EX_TEMPLATE_REGISTRY = synchronizedMap( new HashMap() );
        }
        else {
            EX_PRECACHE = (new WeakHashMap<CharSequence, char[]>());
            EX_NODE_CACHE = (new WeakHashMap<Object, Node[]>());
            EX_PRECOMP_CACHE = (new WeakHashMap<Object, Serializable>());
            EX_TEMPLATE_REGISTRY = new HashMap();
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
    public Interpreter(CharSequence template) {
        if (!EX_PRECACHE.containsKey(template)) {
            EX_PRECACHE.put(template, this.expression = template.toString().toCharArray());
            nodes = new TemplateCompiler(this).compileExpression();
            EX_NODE_CACHE.put(template, nodes.clone());
        }
        else {
            this.expression = EX_PRECACHE.get(template);
            try {
                this.nodes = EX_NODE_CACHE.get(expression).clone();
            }
            catch (NullPointerException e) {
                EX_NODE_CACHE.remove(expression);
                nodes = new TemplateCompiler(this).compileExpression();
                EX_NODE_CACHE.put(expression, nodes.clone());
            }

        }
        cloneAllNodes();

    }

    public Interpreter(String expression) {
        if (!EX_PRECACHE.containsKey(expression)) {
            EX_PRECACHE.put(expression, this.expression = expression.toCharArray());
            nodes = new TemplateCompiler(this).compileExpression();
            EX_NODE_CACHE.put(expression, nodes.clone());
        }
        else {
            this.expression = EX_PRECACHE.get(expression);
            try {
                this.nodes = EX_NODE_CACHE.get(expression).clone();
            }
            catch (NullPointerException e) {
                EX_NODE_CACHE.remove(expression);
                nodes = new TemplateCompiler(this).compileExpression();
                EX_NODE_CACHE.put(expression, nodes.clone());
            }

        }
        cloneAllNodes();
    }    

    private void cloneAllNodes() {
        try {
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = nodes[i].clone();
            }
        }
        catch (Exception e) {
            throw new CompileException("unknown exception", e);
        }
    }

    public Interpreter(char[] expression) {
        this.expression = expression;
    }
    
    public static void registryTemplate(String name, String template) {
        EX_TEMPLATE_REGISTRY.put( name, template );
    }    

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public static void parseToStream(File template, Object ctx, Map<String, Object> tokens, OutputStream out)
            throws IOException {
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

    public static Object parse(File file, Object ctx, Map<String, Object> tokens) throws IOException {
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

            return parse(sb, ctx, tokens);

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

    public static Object parse(CharSequence expression, Object ctx, Map<String, Object> vars) {
        if (expression == null) return null;
        return new Interpreter(expression).execute(ctx, vars);
    }

    public static Object parse(String expression, Object ctx, Map<String, Object> vars) {
        if (expression == null) return null;

        return new Interpreter(expression).execute(ctx, vars);
    }

    public Object execute(Object ctx, Map tokens) {
        if (nodes == null) {
            return new String(expression);
        }
        else if (nodes.length == 2) {
            switch (nodes[0].getToken()) {
                case PROPERTY_EX:
                    //noinspection unchecked
                    //  return ExpressionParser.eval(getInternalSegment(nodes[0]), ctx, tokens);

                    if (!cacheAggressively) {
                        char[] seg = new char[expression.length - 3];
                        arraycopy(expression, 2, seg, 0, seg.length);

                        return MVEL.eval(seg, ctx, tokens);
                    }
                    else {
                        String s = new String(expression, 2, expression.length - 3);
                        if (!EX_PRECOMP_CACHE.containsKey(s)) {
                            synchronized (EX_PRECOMP_CACHE) {
                                EX_PRECOMP_CACHE.put(s, MVEL.compileExpression(s));
                                return MVEL.executeExpression(EX_PRECOMP_CACHE.get(s), ctx, tokens);
                            }
                        }
                        else {
                            return MVEL.executeExpression(EX_PRECOMP_CACHE.get(s), ctx, tokens);
                        }

                    }
                case LITERAL:
                    return new String(expression);
                case INCLUDE_BY_REF: {
                    IncludeRef includeRef = (IncludeRef) nodes[0].getRegister();
                    String template = EX_TEMPLATE_REGISTRY.get( includeRef.getName() );
                    
                    IncludeRefParam[] params = includeRef.getParams();
                    Map vars = new HashMap( params.length );
                    for ( int i = 0; i < params.length; i++ ) {
                        vars.put( params[i].getIdentifier(), parse(params[i].getValue(), ctx, tokens));
                    }
                    
                    return Interpreter.parse( template, ctx, vars );                    
                }                    
            }

            return new String(expression);
        }

        Object register = null;

        StringAppender sbuf = new StringAppender(10);
        Node currNode = null;

        try {
            ExpressionParser oParser = new ExpressionParser(ctx, tokens);

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
                        ForeachContext foreachContext = ( ForeachContext ) currNode.getRegister();
                        if ( foreachContext.getItererators() == null ) {
                            try {
                                String[] lists = getForEachSegment(currNode).split( "," );
                                Iterator[] iters = new Iterator[lists.length];
                                for( int i = 0; i < lists.length; i++ ) {
                                    Object listObject = new ExpressionParser(lists[i], ctx, tokens).parse();
                                    if ( listObject instanceof Object[]) {
                                        listObject = Arrays.asList( (Object[]) listObject );
                                    }    
                                    iters[i] = ((Collection)listObject).iterator() ;
                                }
                                foreachContext.setIterators( iters );
                            } catch (ClassCastException e) {
                                throw new CompileException("expression for collections does not return a collections object: " + new String(getSegment(currNode)));
                            }
                            catch (NullPointerException e) {
                                throw new CompileException("null returned for foreach in expression: " + (getForEachSegment(currNode)));
                            }
                        }

                        Iterator[] iters = foreachContext.getItererators();
                        String[] alias = currNode.getAlias().split( "," );
                        // must trim vars
                        for ( int i = 0; i < alias.length; i++ ) {
                            alias[i] = alias[i].trim();
                        }                         
                        
                        if (iters[0].hasNext()) {
                            push();

                            //noinspection unchecked
                            for ( int i = 0; i < iters.length; i++ ) {
                                tokens.put(alias[i], iters[i].next());
                            }
                            if ( foreachContext.getCount() != 0 ) {
                                sbuf.append( foreachContext.getSeperator() );
                            }
                            foreachContext.setCount( foreachContext.getCount( ) + 1 );
                        }
                        else {
                            for ( int i = 0; i < iters.length; i++ ) {
                                tokens.remove(alias[i]);
                            }      
                            foreachContext.setIterators( null );
                            foreachContext.setCount( 0 );
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
                        if (nodes.length == 2) {
                            return register;
                        }
                        else {
                            return sbuf.toString();
                        }
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
                        + currNode.getToken() + "{" + currNode.getStartPos() + "," + currNode.getEndPos() + "}", e);
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
        node = nodes[node].getEndNode();
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
    public static Object getValuePE(String expression, Object ctx, Map<String, Object> tokens) {
        return new Interpreter(expression).execute(ctx, tokens);
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
                case'{':
                    depth++;
                    break;
                case'}':
                    depth--;
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
        Interpreter.cacheAggressively = cacheAggressively;
    }
}
