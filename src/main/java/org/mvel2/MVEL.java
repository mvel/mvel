/**
 * MVEL 2.0
 * Copyright (C) 2007  MVFLEX/Valhalla Project and the Codehaus
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
package org.mvel2;

import static org.mvel2.DataConversion.convert;
import static org.mvel2.MVELRuntime.execute;
import org.mvel2.compiler.*;
import org.mvel2.integration.Interceptor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.optimizers.impl.refl.nodes.GetterAccessor;
import static org.mvel2.util.ParseTools.loadFromFile;
import static org.mvel2.util.ParseTools.optimizeTree;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import static java.lang.Boolean.getBoolean;
import static java.lang.String.valueOf;
import java.util.HashMap;
import java.util.Map;

public class
        MVEL {
    public static final String NAME = "MVEL (MVFLEX Expression Language)";
    public static final String VERSION = "2.0";
    public static final String VERSION_SUB = "RC";
    public static final String CODENAME = "enceladus";

    static boolean DEBUG_FILE = getBoolean("mvel2.debug.fileoutput");

    static String ADVANCED_DEBUGGING_FILE = System.getProperty("mvel2.debugging.file") == null ? "mvel_debug.txt"
            : System.getProperty("mvel2.debugging.file");

    static boolean ADVANCED_DEBUG = getBoolean("mvel2.advanced_debugging");
    static boolean WEAK_CACHE = getBoolean("mvel2.weak_caching");
    static boolean NO_JIT = getBoolean("mvel2.disable.jit");

    public static boolean COMPILER_OPT_ALLOW_NAKED_METH_CALL =
            getBoolean("mvel2.compiler.allow_naked_meth_calls");

    static boolean OPTIMIZER = true;

    static {
        if (System.getProperty("mvel2.optimizer") != null) {
            OPTIMIZER = getBoolean("mvel2.optimizer");
        }
    }

    public static boolean isAdvancedDebugging() {
        return ADVANCED_DEBUG;
    }

    public static String getDebuggingOutputFileName() {
        return ADVANCED_DEBUGGING_FILE;
    }

    public static boolean isFileDebugging() {
        return DEBUG_FILE;
    }

    /**
     * Evaluate an expression and return the value.
     *
     * @param expression A String containing the expression to be evaluated.
     * @return the resultant value
     */
    public static Object eval(String expression) {
        try {
            return new MVELInterpretedRuntime(expression, MVELRuntime.IMMUTABLE_DEFAULT_FACTORY).parse();
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    /**
     * Evaluate an expression against a context object.  Expressions evaluated against a context object are designed
     * to treat members of that context object as variables in the expression.  For example:
     * <pre><code>
     * MVEL.eval("foo == 1", ctx);
     * </code></pre>
     * In this case, the identifier <tt>foo</tt> would be resolved against the <tt>ctx</tt> object.  So it would have
     * the equivalent of: <tt>ctc.getFoo() == 1</tt> in Java.
     *
     * @param expression A String containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against.
     * @return The resultant value
     */
    public static Object eval(String expression, Object ctx) {
        try {
            return new MVELInterpretedRuntime(expression, ctx, MVELRuntime.IMMUTABLE_DEFAULT_FACTORY).parse();
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    /**
     * Evaluate an expression with externally injected variables via a {@link VariableResolverFactory}.  A factory
     * provides the means by which MVEL can resolve external variables.  MVEL contains a straight-forward implementation
     * for wrapping Maps: {@link MapVariableResolverFactory}, which is used implicitly when calling overloaded methods
     * in this class that use Maps.
     * <p/>
     * An example:
     * <pre><code>
     * Map varsMap = new HashMap();
     * varsMap.put("x", 5);
     * varsMap.put("y", 2);
     * <p/>
     * VariableResolverFactory factory = new MapVariableResolverFactory(varsMap);
     * <p/>
     * Integer i = (Integer) MVEL.eval("x * y", factory);
     * <p/>
     * assert i == 10;
     * </code></pre>
     *
     * @param expression      A String containing the expression to be evaluated.
     * @param resolverFactory The instance of the VariableResolverFactory to be used.
     * @return The resultant value.
     */
    public static Object eval(String expression, VariableResolverFactory resolverFactory) {
        try {
            return new MVELInterpretedRuntime(expression, resolverFactory).parse();
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    /**
     * Evaluates an expression against a context object and injected variables from a {@link VariableResolverFactory}.
     * This method of execution will prefer to find variables from the factory and <em>then</em> from the context.
     *
     * @param expression      A string containing the expression to be evaluated
     * @param ctx             The context object to evaluate against.
     * @param resolverFactory The instance of the VariableResolverFactory to be used.
     * @return The resultant value
     * @see #eval(String, org.mvel2.integration.VariableResolverFactory)
     */
    public static Object eval(String expression, Object ctx, VariableResolverFactory resolverFactory) {
        try {
            return new MVELInterpretedRuntime(expression, ctx, resolverFactory).parse();
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    /**
     * Evaluates an expression against externally injected variables.  This is a wrapper convenience method which
     * wraps the provided Map of vars in a {@link MapVariableResolverFactory}
     *
     * @param expression A string containing the expression to be evaluated.
     * @param vars       A map of vars to be injected
     * @return The resultant value
     * @see #eval(String, org.mvel2.integration.VariableResolverFactory)
     */
    public static Object eval(String expression, Map<String, Object> vars) {
        try {
            return new MVELInterpretedRuntime(expression, null, new MapVariableResolverFactory(vars)).parse();
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    /**
     * Evaluates an expression against a context object and externally injected variables.  This is a wrapper
     * convenience method which wraps the provided Map of vars in a {@link MapVariableResolverFactory}
     *
     * @param expression A string containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against.
     * @param vars       A map of vars to be injected
     * @return The resultant value
     * @see #eval(String, VariableResolverFactory)
     */
    public static Object eval(String expression, Object ctx, Map<String, Object> vars) {
        try {
            return new MVELInterpretedRuntime(expression, ctx, new MapVariableResolverFactory(vars)).parse();
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }


    /**
     * Evaluates an expression and, if necessary, coerces the resultant value to the specified type. Example:
     * <pre><code>
     * Float output = MVEL.eval("5 + 5", Float.class);
     * </code></pre>
     * <p/>
     * This converts an expression that would otherwise return an <tt>Integer</tt> to a <tt>Float</tt>.
     *
     * @param expression A string containing the expression to be evaluated.
     * @param toType     The target type that the resultant value will be converted to, if necessary.
     * @return The resultant value.
     */
    public static <T> T eval(String expression, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }


    /**
     * Evaluates an expression against a context object and, if necessary, coerces the resultant value to the specified
     * type.
     *
     * @param expression A string containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against.
     * @param toType     The target type that the resultant value will be converted to, if necessary.
     * @return The resultant value
     * @see #eval(String,Class)
     */
    public static <T> T eval(String expression, Object ctx, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }


    /**
     * Evaluates an expression against externally injected variables and, if necessary, coerces the resultant value
     * to the specified type.
     *
     * @param expression A string containing the expression to be evaluated
     * @param vars       The variables to be injected
     * @param toType     The target type that the resultant value will be converted to, if necessary.
     * @return The resultant value
     * @see #eval(String,VariableResolverFactory)
     * @see #eval(String,Class)
     */
    public static <T> T eval(String expression, VariableResolverFactory vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, null, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }


    /**
     * Evaluates an expression against externally injected variables.  The resultant value is coerced to the specified
     * type if necessary. This is a wrapper convenience method which wraps the provided Map of vars in a
     * {@link MapVariableResolverFactory}
     *
     * @param expression A string containing the expression to be evaluated.
     * @param vars       A map of vars to be injected
     * @param toType     The target type the resultant value will be converted to, if necessary.
     * @return The resultant value
     * @see #eval(String, org.mvel2.integration.VariableResolverFactory)
     */
    public static <T> T eval(String expression, Map<String, Object> vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, null, new MapVariableResolverFactory(vars)).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    /**
     * Evaluates an expression against a context object and externally injected variables.  If necessary, the resultant
     * value is coerced to the specified type.
     *
     * @param expression A string containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against
     * @param vars       The vars to be injected
     * @param toType     The target type that the resultant value will be converted to, if necessary.
     * @return The resultant value.
     * @see #eval(String,Object,VariableResolverFactory)
     * @see #eval(String,Class)
     */
    public static <T> T eval(String expression, Object ctx, VariableResolverFactory vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    /**
     * Evaluates an expression against a context object and externally injected variables.  If necessary, the resultant
     * value is coerced to the specified type.
     *
     * @param expression A string containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against
     * @param vars       A Map of variables to be injected.
     * @param toType     The target type that the resultant value will be converted to, if necessary.
     * @return The resultant value.
     * @see #eval(String,Object,VariableResolverFactory)
     * @see #eval(String,Class)
     */
    public static <T> T eval(String expression, Object ctx, Map<String, Object> vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx, new MapVariableResolverFactory(vars)).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    /**
     * Evaluates an expression and returns the resultant value as a String.
     *
     * @param expression A string containing the expressino to be evaluated.
     * @return The resultant value
     */
    public static String evalToString(String expression) {
        try {
            return valueOf(eval(expression));
        }
        catch (EndWithValue end) {
            return valueOf(end.getValue());
        }
    }


    /**
     * Evaluates an expression and returns the resultant value as a String.
     *
     * @param expression A string containing the expressino to be evaluated.
     * @param ctx        The context object to evaluate against
     * @return The resultant value
     * @see #eval(String,Object)
     */
    public static String evalToString(String expression, Object ctx) {
        try {
            return valueOf(eval(expression, ctx));
        }
        catch (EndWithValue end) {
            return valueOf(end.getValue());
        }
    }

    /**
     * Evaluates an expression and returns the resultant value as a String.
     *
     * @param expression A string containing the expressino to be evaluated.
     * @param vars       The variables to be injected
     * @return The resultant value
     * @see #eval(String,VariableResolverFactory)
     */
    public static String evalToString(String expression, VariableResolverFactory vars) {
        try {
            return valueOf(eval(expression, vars));
        }
        catch (EndWithValue end) {
            return valueOf(end.getValue());
        }
    }


    /**
     * Evaluates an expression and returns the resultant value as a String.
     *
     * @param expression A string containing the expressino to be evaluated.
     * @param vars       A Map of variables to be injected
     * @return The resultant value
     * @see #eval(String,Map)
     */
    public static String evalToString(String expression, Map vars) {
        try {
            return valueOf(eval(expression, vars));
        }
        catch (EndWithValue end) {
            return valueOf(end.getValue());
        }
    }

    /**
     * Evaluates an expression and returns the resultant value as a String.
     *
     * @param expression A string containing the expressino to be evaluated.
     * @param ctx        The context object to evaluate against.
     * @param vars       The variables to be injected
     * @return The resultant value
     * @see #eval(String,Map)
     */
    public static String evalToString(String expression, Object ctx, VariableResolverFactory vars) {
        try {
            return valueOf(eval(expression, ctx, vars));
        }
        catch (EndWithValue end) {
            return valueOf(end.getValue());
        }
    }

    /**
     * Evaluates an expression and returns the resultant value as a String.
     *
     * @param expression A string containing the expressino to be evaluated.
     * @param ctx        The context object to evaluate against.
     * @param vars       A Map of variables to be injected
     * @return The resultant value
     * @see #eval(String,Map)
     */
    public static String evalToString(String expression, Object ctx, Map vars) {
        try {
            return valueOf(eval(expression, ctx, vars));
        }
        catch (EndWithValue end) {
            return valueOf(end.getValue());
        }
    }


    /**
     * Evaluate an expression and return the value.
     *
     * @param expression A char[] containing the expression to be evaluated.
     * @return The resultant value
     * @see #eval(String)
     */
    public static Object eval(char[] expression) {
        try {
            return new MVELInterpretedRuntime(expression, MVELRuntime.IMMUTABLE_DEFAULT_FACTORY).parse();
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    /**
     * Evaluate an expression against a context object and return the value
     *
     * @param expression A char[] containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against
     * @return The resultant value
     * @see #eval(String,Object)
     */
    public static Object eval(char[] expression, Object ctx) {
        try {
            return new MVELInterpretedRuntime(expression, ctx).parse();
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    /**
     * Evaluate an expression against a context object and return the value
     *
     * @param expression A char[] containing the expression to be evaluated.
     * @param ctx        The context object to evaluate against
     * @param vars       The variables to be injected
     * @return The resultant value
     * @see #eval(String,Object,VariableResolverFactory)
     */
    public static Object eval(char[] expression, Object ctx, VariableResolverFactory vars) {
        try {
            return new MVELInterpretedRuntime(expression, ctx, vars).parse();
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }


    public static Object eval(char[] expression, Object ctx, Map vars) {
        try {
            return new MVELInterpretedRuntime(expression, ctx, vars).parse();
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    public static <T> T eval(char[] expression, Object ctx, Map<String, Object> vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    public static <T> T eval(char[] expression, Object ctx, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }


    public static <T> T eval(char[] expression, Object ctx, VariableResolverFactory vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    public static <T> T eval(char[] expression, Map<String, Object> vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, null, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }


    public static Object evalFile(File file) throws IOException {
        try {
            return _evalFile(file, null, new MapVariableResolverFactory(new HashMap()));
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    public static Object evalFile(File file, Object ctx) throws IOException {
        try {
            return _evalFile(file, ctx, new MapVariableResolverFactory(new HashMap()));
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    public static Object evalFile(File file, Map<String, Object> vars) throws IOException {
        try {
            return evalFile(file, null, vars);
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    public static Object evalFile(File file, Object ctx, Map<String, Object> vars) throws IOException {
        try {
            return _evalFile(file, ctx, new MapVariableResolverFactory(vars));
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    public static Object evalFile(File file, Object ctx, VariableResolverFactory factory) throws IOException {
        try {
            return _evalFile(file, ctx, factory);
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    private static Object _evalFile(File file, Object ctx, VariableResolverFactory factory) throws IOException {
        try {
            return eval(loadFromFile(file), ctx, factory);
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }


    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param ctx        -
     * @param vars       -
     * @return -
     */
    @SuppressWarnings({"unchecked"})
    public static Boolean evalToBoolean(String expression, Object ctx, Map vars) {
        try {
            return eval(expression, ctx, vars, Boolean.class);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), Boolean.class);
        }
    }

    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param ctx        -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, Object ctx) {
        try {
            return eval(expression, ctx, Boolean.class);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), Boolean.class);
        }
    }

    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param ctx        -
     * @param factory    -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, Object ctx, VariableResolverFactory factory) {
        try {
            return eval(expression, ctx, factory, Boolean.class);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), Boolean.class);
        }
    }

    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param factory    -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, VariableResolverFactory factory) {
        try {
            return eval(expression, factory, Boolean.class);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), Boolean.class);
        }
    }

    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param vars       -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, Map vars) {
        try {
            return evalToBoolean(expression, null, vars);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), Boolean.class);
        }
    }

    public static Serializable compileExpression(String expression, ParserContext ctx) {
        return optimizeTree(new ExpressionCompiler(expression)
                .compile(ctx));
    }

    public static Serializable compileExpression(String expression, Map<String, Object> imports,
                                                 Map<String, Interceptor> interceptors, String sourceName) {
        return compileExpression(expression, new ParserContext(imports, interceptors, sourceName));

    }

    /**
     * Compiles an expression and returns a Serializable object containing the compiled
     * expression.
     *
     * @param expression - the expression to be compiled
     * @return -
     */
    public static Serializable compileExpression(String expression) {
        return compileExpression(expression, null, null, null);
    }

    public static Serializable compileExpression(String expression, Map<String, Object> imports) {
        return compileExpression(expression, imports, null, null);
    }

    public static Serializable compileExpression(String expression, Map<String, Object> imports, Map<String, Interceptor> interceptors) {
        return compileExpression(expression, imports, interceptors, null);
    }

    public static Serializable compileExpression(char[] expression, ParserContext ctx) {
        return optimizeTree(new ExpressionCompiler(expression).compile(ctx));
    }

    /**
     * Compiles an expression and returns a Serializable object containing the compiled
     * expression.
     *
     * @param expression   - the expression to be compiled
     * @param imports      -
     * @param interceptors -
     * @param sourceName   -
     * @return -
     */
    public static Serializable compileExpression(char[] expression, Map<String, Object> imports,
                                                 Map<String, Interceptor> interceptors, String sourceName) {
        return compileExpression(expression, new ParserContext(imports, interceptors, sourceName));
    }

    public static Serializable compileExpression(char[] expression) {
        return compileExpression(expression, null, null, null);
    }

    public static Serializable compileExpression(char[] expression, Map<String, Object> imports) {
        return compileExpression(expression, imports, null, null);
    }

    public static Serializable compileExpression(char[] expression, Map<String, Object> imports, Map<String, Interceptor> interceptors) {
        return compileExpression(expression, imports, interceptors, null);
    }

    public static Serializable compileGetExpression(String expression) {
        return new CompiledAccExpression(expression.toCharArray(), new ParserContext());
    }

    public static Serializable compileGetExpression(String expression, ParserContext ctx) {
        return new CompiledAccExpression(expression.toCharArray(), ctx);
    }

    public static Serializable compileGetExpression(char[] expression) {
        return new CompiledAccExpression(expression, new ParserContext());
    }

    public static Serializable compileGetExpression(char[] expression, ParserContext ctx) {
        return new CompiledAccExpression(expression, ctx);
    }

    public static Serializable compileSetExpression(String expression) {
        return new CompiledAccExpression(expression.toCharArray(), new ParserContext());
    }

    public static Serializable compileSetExpression(String expression, ParserContext ctx) {
        return new CompiledAccExpression(expression.toCharArray(), ctx);
    }

    public static Serializable compileSetExpression(char[] expression) {
        return new CompiledAccExpression(expression, new ParserContext());
    }

    public static Serializable compileSetExpression(char[] expression, ParserContext ctx) {
        return new CompiledAccExpression(expression, ctx);
    }

    public static void executeSetExpression(Serializable compiledSet, Object ctx, Object value) {
        ((CompiledAccExpression) compiledSet).setValue(ctx, ctx, MVELRuntime.IMMUTABLE_DEFAULT_FACTORY, value);
    }

    public static void executeSetExpression(Serializable compiledSet, Object ctx, VariableResolverFactory vrf, Object value) {
        ((CompiledAccExpression) compiledSet).setValue(ctx, ctx, vrf, value);
    }

    public static Object executeExpression(Object compiledExpression) {
        try {
            return ((ExecutableStatement) compiledExpression).getValue(null, MVELRuntime.IMMUTABLE_DEFAULT_FACTORY);
        }
        catch (EndWithValue e) {
            return e.getValue();
        }
    }

    /**
     * Executes a compiled expression.
     *
     * @param compiledExpression -
     * @param ctx                -
     * @param vars               -
     * @return -
     * @see #compileExpression(String)
     */
    @SuppressWarnings({"unchecked"})
    public static Object executeExpression(final Object compiledExpression, final Object ctx, final Map vars) {
        try {
            return ((ExecutableStatement) compiledExpression).getValue(ctx, vars != null ? new MapVariableResolverFactory(vars) : null);
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    public static Object executeExpression(final Object compiledExpression, final Object ctx, final VariableResolverFactory resolverFactory) {
        try {
            return ((ExecutableStatement) compiledExpression).getValue(ctx, resolverFactory);
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    /**
     * Executes a compiled expression.
     *
     * @param compiledExpression -
     * @param factory            -
     * @return -
     * @see #compileExpression(String)
     */
    public static Object executeExpression(final Object compiledExpression, final VariableResolverFactory factory) {
        try {
            return ((ExecutableStatement) compiledExpression).getValue(null, factory);
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    /**
     * Executes a compiled expression.
     *
     * @param compiledExpression -
     * @param ctx                -
     * @return -
     * @see #compileExpression(String)
     */
    public static Object executeExpression(final Object compiledExpression, final Object ctx) {
        try {
            return ((ExecutableStatement) compiledExpression).getValue(ctx, MVELRuntime.IMMUTABLE_DEFAULT_FACTORY);
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    /**
     * Executes a compiled expression.
     *
     * @param compiledExpression -
     * @param vars               -
     * @return -
     * @see #compileExpression(String)
     */
    @SuppressWarnings({"unchecked"})
    public static Object executeExpression(final Object compiledExpression, final Map vars) {
        try {
            return ((ExecutableStatement) compiledExpression).getValue(null, new MapVariableResolverFactory(vars));
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    /**
     * Execute a compiled expression and convert the result to a type
     *
     * @param compiledExpression -
     * @param ctx                -
     * @param vars               -
     * @param toType             -
     * @return -
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T executeExpression(final Object compiledExpression, final Object ctx, final Map vars, Class<T> toType) {
        try {
            return convert(executeExpression(compiledExpression, ctx, vars), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    /**
     * Execute a compiled expression and convert the result to a type
     *
     * @param compiledExpression -
     * @param vars               -
     * @param toType             -
     * @return -
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T executeExpression(final Object compiledExpression, Map vars, Class<T> toType) {
        try {
            return convert(executeExpression(compiledExpression, vars), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    /**
     * Execute a compiled expression and convert the result to a type.
     *
     * @param compiledExpression -
     * @param ctx                -
     * @param toType             -
     * @return -
     */
    public static <T> T executeExpression(final Object compiledExpression, final Object ctx, Class<T> toType) {
        try {
            return convert(executeExpression(compiledExpression, ctx), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    public static void executeExpression(Iterable<CompiledExpression> compiledExpression) {
        for (CompiledExpression ce : compiledExpression) {
            ce.getValue(null, null);
        }
    }

    public static void executeExpression(Iterable<CompiledExpression> compiledExpression, Object ctx) {
        for (CompiledExpression ce : compiledExpression) {
            ce.getValue(ctx, null);
        }
    }

    public static void executeExpression(Iterable<CompiledExpression> compiledExpression, Map vars) {
        executeExpression(compiledExpression, null, new MapVariableResolverFactory(vars));

    }

    public static void executeExpression(Iterable<CompiledExpression> compiledExpression, Object ctx, Map vars) {
        executeExpression(compiledExpression, ctx, new MapVariableResolverFactory(vars));
    }

    public static void executeExpression(Iterable<CompiledExpression> compiledExpression, Object ctx, VariableResolverFactory vars) {
        for (CompiledExpression ce : compiledExpression) {
            ce.getValue(ctx, vars);
        }
    }

    public static Object[] executeAllExpression(Serializable[] compiledExpressions, Object ctx, VariableResolverFactory vars) {
        if (compiledExpressions == null) return GetterAccessor.EMPTY;

        Object[] o = new Object[compiledExpressions.length];
        for (int i = 0; i < compiledExpressions.length; i++) {
            o[i] = executeExpression(compiledExpressions[i], ctx, vars);
        }
        return o;
    }

    public static Object executeDebugger(CompiledExpression expression, Object ctx, VariableResolverFactory vars) {
        try {
            return execute(true, expression, ctx, vars);
        }
        catch (EndWithValue e) {
            return e.getValue();
        }
    }


    public static String parseMacros(String input, Map<String, Macro> macros) {
        return new MacroProcessor(macros).parse(input);
    }

    public static String preprocess(char[] input, PreProcessor[] preprocessors) {
        char[] ex = input;
        for (PreProcessor proc : preprocessors) {
            ex = proc.parse(ex);
        }
        return new String(ex);
    }

    public static String preprocess(String input, PreProcessor[] preprocessors) {
        return preprocess(input.toCharArray(), preprocessors);
    }


    public static Object getProperty(String property, Object ctx) {
        return PropertyAccessor.get(property, ctx);
    }

    public static void setProperty(Object ctx, String property, Object value) {
        PropertyAccessor.set(ctx, property, value);
    }
}
