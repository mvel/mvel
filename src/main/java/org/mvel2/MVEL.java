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

package org.mvel2;

import static org.mvel2.DataConversion.convert;
import static org.mvel2.MVELRuntime.execute;
import org.mvel2.compiler.*;
import org.mvel2.integration.Interceptor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.optimizers.impl.refl.GetterAccessor;
import static org.mvel2.util.ParseTools.loadFromFile;
import static org.mvel2.util.ParseTools.optimizeTree;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import static java.lang.Boolean.getBoolean;
import static java.lang.String.valueOf;
import java.util.HashMap;
import java.util.Map;

public class MVEL {
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

    public static boolean isOptimizationEnabled() {
        return OPTIMIZER;
    }

    public static Object eval(String expression) {
        return new MVELInterpretedRuntime(expression, MVELRuntime.IMMUTABLE_DEFAULT_FACTORY).parse();
    }

    public static Object eval(char[] expression) {
        return new MVELInterpretedRuntime(expression, MVELRuntime.IMMUTABLE_DEFAULT_FACTORY).parse();
    }

    public static Object eval(String expression, Object ctx) {
        return new MVELInterpretedRuntime(expression, ctx, MVELRuntime.IMMUTABLE_DEFAULT_FACTORY).parse();
    }

    public static Object eval(String expression, VariableResolverFactory resolverFactory) {
        return new MVELInterpretedRuntime(expression, resolverFactory).parse();
    }

    public static Object eval(char[] expression, Object ctx, VariableResolverFactory resolverFactory) {
        return new MVELInterpretedRuntime(expression, ctx, resolverFactory).parse();
    }

    public static Object eval(char[] expression, Object ctx, VariableResolverFactory resolverFactory, boolean returnBigDecimal) {
        return new MVELInterpretedRuntime(expression, ctx, resolverFactory, returnBigDecimal).parse();
    }

    public static Object eval(String expression, Object ctx, VariableResolverFactory resolverFactory) {
        return new MVELInterpretedRuntime(expression, ctx, resolverFactory).parse();
    }

    public static Object eval(String expression, Object ctx, VariableResolverFactory resolverFactory, boolean returnBigDecimal) {
        return new MVELInterpretedRuntime(expression, ctx, resolverFactory, returnBigDecimal).parse();
    }

    @SuppressWarnings({"unchecked"})
    public static Object eval(String expression, Map tokens) {
        return new MVELInterpretedRuntime(expression, null, tokens).parse();
    }

    @SuppressWarnings({"unchecked"})
    public static Object eval(String expression, Object ctx, Map tokens) {
        return new MVELInterpretedRuntime(expression, ctx, tokens).parse();
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


    @SuppressWarnings({"unchecked"})
    public static <T> T eval(char[] expression, Object ctx, Map vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(char[] expression, Object ctx, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(String expression, Object ctx, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(String expression, Object ctx, Map vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(char[] expression, Object ctx, VariableResolverFactory vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(String expression, Object ctx, VariableResolverFactory vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }


    @SuppressWarnings({"unchecked"})
    public static <T> T eval(String expression, Map vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, null, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(String expression, VariableResolverFactory vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, null, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }


    @SuppressWarnings({"unchecked"})
    public static <T> T eval(char[] expression, Map vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, null, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(end.getValue(), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static Object eval(char[] expression, Object ctx, Map vars) {
        try {
            return new MVELInterpretedRuntime(expression, ctx, vars).parse();
        }
        catch (EndWithValue end) {
            return end.getValue();
        }
    }

    public static String evalToString(String expression, Object ctx) {
        try {
            return valueOf(eval(expression, ctx));
        }
        catch (EndWithValue end) {
            return valueOf(end.getValue());
        }
    }

    @SuppressWarnings({"unchecked"})
    public static String evalToString(String expression, Map vars) {
        return valueOf(eval(expression, vars));
    }

    @SuppressWarnings({"unchecked"})
    public static String evalToString(String expression, Object ctx, Map vars) {
        try {
            return valueOf(eval(expression, ctx, vars));
        }
        catch (EndWithValue end) {
            return valueOf(end.getValue());
        }
    }

    public static Object evalFile(File file) throws IOException {
        return _evalFile(file, null, new MapVariableResolverFactory(new HashMap()));
    }

    public static Object evalFile(File file, Object ctx) throws IOException {
        return _evalFile(file, ctx, new MapVariableResolverFactory(new HashMap()));
    }

    public static Object evalFile(File file, Map vars) throws IOException {
        return evalFile(file, null, vars);
    }

    public static Object evalFile(File file, Object ctx, Map vars) throws IOException {
        return _evalFile(file, ctx, new MapVariableResolverFactory(vars));
    }

    public static Object evalFile(File file, Object ctx, VariableResolverFactory factory) throws IOException {
        return _evalFile(file, ctx, factory);
    }

    private static Object _evalFile(File file, Object ctx, VariableResolverFactory factory) throws IOException {
        return eval(loadFromFile(file), ctx, factory);
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
        return eval(expression, ctx, vars, Boolean.class);
    }

    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param ctx        -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, Object ctx) {
        return eval(expression, ctx, Boolean.class);
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
        return eval(expression, ctx, factory, Boolean.class);
    }

    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param factory    -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, VariableResolverFactory factory) {
        return eval(expression, factory, Boolean.class);
    }

    /**
     * Evaluate an expression in Boolean-only mode.
     *
     * @param expression -
     * @param vars       -
     * @return -
     */
    public static Boolean evalToBoolean(String expression, Map vars) {
        return evalToBoolean(expression, null, vars);
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
