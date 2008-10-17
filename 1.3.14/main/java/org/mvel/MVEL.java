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

import static org.mvel.DataConversion.convert;
import static org.mvel.MVELRuntime.execute;
import org.mvel.integration.Interceptor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.MapVariableResolverFactory;
import org.mvel.optimizers.impl.refl.GetterAccessor;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.handleParserEgress;

import java.io.Serializable;
import static java.lang.Boolean.getBoolean;
import static java.lang.String.valueOf;
import java.util.Map;

public class MVEL {
    public static final String NAME = "MVEL (MVFLEX Expression Language)";
    public static final String VERSION = "1.3";
    public static final String VERSION_SUB = "14";
    public static final String CODENAME = "horizon";

    static boolean DEBUG_FILE = getBoolean("mvel.debug.fileoutput");
    static String ADVANCED_DEBUGGING_FILE = System.getProperty("mvel.debugging.file") == null ? "mvel_debug.txt"
            : System.getProperty("mvel.debugging.file");

    static boolean ADVANCED_DEBUG = getBoolean("mvel.advanced_debugging");
    static boolean WEAK_CACHE = getBoolean("mvel.weak_caching");
    static boolean NO_JIT = getBoolean("mvel.disable.jit");

    static boolean OPTIMIZER = true;

    static {
        if (System.getProperty("mvel.optimizer") != null) {
            OPTIMIZER = getBoolean("mvel.optimizer");
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
        return new MVELInterpretedRuntime(expression).parse();
    }

    public static Object eval(String expression, Object ctx) {
        return new MVELInterpretedRuntime(expression, ctx).parse();
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


    public static Serializable compileExpression(String expression, Map<String, Object> imports,
                                                 Map<String, Interceptor> interceptors, String sourceName) {

        return ParseTools.optimizeTree(new ExpressionCompiler(expression)
                .compile(new ParserContext(imports, interceptors, sourceName)));
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


    /**
     * Compiles an expression and returns a Serializable object containing the compiled
     * expression.
     *
     * @param expression   - the expression to be compiled
     * @param imports      -
     * @param interceptors -
     * @return -
     */
    public static Serializable compileExpression(char[] expression, Map<String, Object> imports,
                                                 Map<String, Interceptor> interceptors, String sourceName) {
        return ParseTools.optimizeTree(new ExpressionCompiler(expression).compile(new ParserContext(imports, interceptors, sourceName)));
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

    public static Serializable compileSetExpression(String expression) {
        return new CompiledSetExpression(expression.toCharArray());
    }

    public static Serializable compileSetExpression(char[] expression) {
        return new CompiledSetExpression(expression);
    }

    public static void executeSetExpression(Serializable compiledSet, Object ctx, Object value) {
        ((CompiledSetExpression) compiledSet).setValue(ctx, null, value);
    }

    public static void executeSetExpression(Serializable compiledSet, Object ctx, VariableResolverFactory vrf, Object value) {
        ((CompiledSetExpression) compiledSet).setValue(ctx, vrf, value);
    }


    public static Object executeExpression(Object compiledExpression) {
        try {
            return ((ExecutableStatement) compiledExpression).getValue(null, null);
        }
        catch (EndWithValue e) {
            return handleParserEgress(e.getValue(), false);
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
            return ((ExecutableStatement) compiledExpression).getValue(ctx, new MapVariableResolverFactory(vars));
        }

        catch (EndWithValue end) {
            return handleParserEgress(end.getValue(), false);
        }
    }

    public static Object executeExpression(final Object compiledExpression, final Object ctx, final VariableResolverFactory resolverFactory) {
        try {
            return ((ExecutableStatement) compiledExpression).getValue(ctx, resolverFactory);
        }
        catch (EndWithValue end) {
            return handleParserEgress(end.getValue(), false);
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
            return handleParserEgress(end.getValue(), false);
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
            return ((ExecutableStatement) compiledExpression).getValue(ctx, null);
        }
        catch (EndWithValue end) {
            return handleParserEgress(end.getValue(), false);
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
            return handleParserEgress(end.getValue(), false);
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
            return convert(handleParserEgress(end.getValue(), false), toType);
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
            return convert(handleParserEgress(end.getValue(), false), toType);
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
            return convert(handleParserEgress(end.getValue(), false), toType);
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
            return handleParserEgress(e.getValue(), false);
        }
    }


    @SuppressWarnings({"unchecked"})
    public static <T> T eval(char[] expression, Object ctx, Map vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(handleParserEgress(end.getValue(), false), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(char[] expression, Object ctx, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(handleParserEgress(end.getValue(), false), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(String expression, Object ctx, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(handleParserEgress(end.getValue(), false), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(String expression, Object ctx, Map vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(handleParserEgress(end.getValue(), false), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(char[] expression, Object ctx, VariableResolverFactory vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(handleParserEgress(end.getValue(), false), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(String expression, Object ctx, VariableResolverFactory vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, ctx, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(handleParserEgress(end.getValue(), false), toType);
        }
    }


    @SuppressWarnings({"unchecked"})
    public static <T> T eval(String expression, Map vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, null, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(handleParserEgress(end.getValue(), false), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T eval(String expression, VariableResolverFactory vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, null, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(handleParserEgress(end.getValue(), false), toType);
        }
    }


    @SuppressWarnings({"unchecked"})
    public static <T> T eval(char[] expression, Map vars, Class<T> toType) {
        try {
            return convert(new MVELInterpretedRuntime(expression, null, vars).parse(), toType);
        }
        catch (EndWithValue end) {
            return convert(handleParserEgress(end.getValue(), false), toType);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static Object eval(char[] expression, Object ctx, Map vars) {
        try {
            return new MVELInterpretedRuntime(expression, ctx, vars).parse();
        }
        catch (EndWithValue end) {
            return handleParserEgress(end.getValue(), false);
        }
    }

    public static String evalToString(String expression, Object ctx) {
        try {
            return valueOf(eval(expression, ctx));
        }
        catch (EndWithValue end) {
            return valueOf(handleParserEgress(end.getValue(), false));
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
            return valueOf(handleParserEgress(end.getValue(), false));
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
        MacroProcessor macroProcessor = new MacroProcessor();
        macroProcessor.setMacros(macros);
        return macroProcessor.parse(input);
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
