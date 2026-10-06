package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mvel3.ContextType;
import org.mvel3.Evaluator;
import org.mvel3.MVEL;
import org.mvel3.MVELBuilder;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.TestInterface;
import org.mvel3.transpiler.context.Declaration;

/** Adapts only the evaluation calls used by the imported arithmetic tests. */
public final class Mvel3TestSupport {

    private Mvel3TestSupport() {
    }

    public static CompiledExpression compileExpression(final String expression) {
        return compileExpression(expression, new TestCompilerContext());
    }

    public static CompiledExpression compileExpression(final String expression, final TestCompilerContext context) {
        return new CompiledExpression(expression, context, Object.class);
    }

    public static Object executeExpression(final Serializable expression) {
        return executeExpression(expression, new LinkedHashMap<>());
    }

    public static Object executeExpression(final Serializable expression, final Map variables) {
        return ((CompiledExpression) expression).evaluate(variables);
    }

    public static Object executeExpression(final Serializable expression, final Object root) {
        if (root instanceof Map) {
            return executeExpression(expression, (Map) root);
        }
        return ((CompiledExpression) expression).evaluate(root);
    }

    public static Object executeExpression(final Serializable expression, final Object root, final Map variables) {
        if (root != null) {
            return ((CompiledExpression) expression).evaluate(root, variables);
        }
        return executeExpression(expression, variables);
    }

    public static Object eval(final String expression) {
        return eval(expression, new LinkedHashMap<>());
    }

    public static Object eval(final String expression, final Map variables) {
        return compileExpression(expression).evaluate(variables);
    }

    public static Object eval(final String expression, final Object root) {
        if (root instanceof Map) {
            return eval(expression, (Map) root);
        }
        return compileExpression(expression).evaluate(root);
    }

    public static <T> T eval(final String expression, final Class<T> outputType) {
        return outputType.cast(new CompiledExpression(expression, new TestCompilerContext(), outputType)
                .evaluate(new LinkedHashMap<>()));
    }

    public static final class ExpressionCompiler {
        private final String expression;
        private final TestCompilerContext context;

        public ExpressionCompiler(final String expression) {
            this(expression, new TestCompilerContext());
        }

        public ExpressionCompiler(final String expression, final TestCompilerContext context) {
            this.expression = expression;
            this.context = context;
        }

        public CompiledExpression compile() {
            return compileExpression(expression, context);
        }
    }

    public static final class CompiledExpression implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String expression;
        private final Map<String, Type<?>> declaredTypes = new LinkedHashMap<>();
        private final Set<String> imports = new LinkedHashSet<>();
        private final Class<?> outputType;

        // NOT thread-safe: each imported test owns its compiled expression.
        private transient Evaluator evaluator;

        private CompiledExpression(final String expression, final TestCompilerContext context, final Class<?> outputType) {
            this.expression = expression;
            this.outputType = outputType;
            context.getInputs().forEach((name, type) -> declaredTypes.put(name, Type.type(type)));
            context.getImports().forEach(type -> imports.add(type.getCanonicalName()));
        }

        private Object evaluate(final Map<String, Object> variables) {
            if (evaluator == null) {
                final Map<String, Type<?>> types = new LinkedHashMap<>();
                variables.forEach((name, value) -> {
                    final Class<?> type;
                    if (value instanceof TestInterface) {
                        type = TestInterface.class;
                    } else {
                        type = value == null ? Object.class : value.getClass();
                    }
                    types.put(name, Type.type(type));
                });
                types.putAll(declaredTypes);

                // MVEL3 needs input types. Without declarations, compile on first evaluation.
                // Preserve the expression exactly, including implicit returns and dynamic assignments.
                final MVEL mvel = new MVEL();
                if (expression.indexOf(';') > 0) {
                    evaluator = mvel.compileMapBlock(expression, outputType, imports, types);
                } else {
                    evaluator = mvel.compileMapExpression(expression, outputType, imports, types);
                }
            }
            return ((Evaluator<Map<String, Object>, Void, ?>) evaluator).eval(variables);
        }

        private Object evaluate(final Object root) {
            return evaluate(root, null);
        }

        private Object evaluate(final Object root, final Map<String, Object> variables) {
            if (root == null) {
                return evaluate(variables != null ? variables : new LinkedHashMap<>());
            }
            if (root instanceof Map) {
                Map<String, Object> merged = new LinkedHashMap<>((Map<String, Object>) root);
                if (variables != null) {
                    merged.putAll(variables);
                }
                return evaluate(merged);
            }
            if (evaluator == null) {
                List<Declaration<?>> decls = new ArrayList<>();
                declaredTypes.forEach((name, type) -> decls.add(Declaration.of(name, type)));
                if (variables != null) {
                    variables.forEach((name, value) -> {
                        Class<?> type = value == null ? Object.class : value.getClass();
                        decls.add(Declaration.of(name, type));
                    });
                }
                Declaration<?>[] declArray = decls.toArray(new Declaration[0]);
                MVELBuilder.WithBuilder<?> withBuilder = new MVELBuilder.WithBuilder<>(
                        ContextType.POJO, Declaration.of(MVELBuilder.CONTEXT_NAME, Type.type(root.getClass())), declArray);
                MVELBuilder<?, ?, ?> builder;
                if (expression.indexOf(';') > 0) {
                    builder = withBuilder.out(outputType).block(expression);
                } else {
                    builder = withBuilder.out(outputType).expression(expression);
                }
                org.mvel3.CompilerParameters params = builder.imports(imports).build();
                evaluator = new MVEL().compilePojoEvaluator(params);
            }
            return ((Evaluator<Object, Void, ?>) evaluator).eval(root);
        }
    }
}
