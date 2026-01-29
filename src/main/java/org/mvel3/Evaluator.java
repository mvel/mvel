package org.mvel3;

/**
 * Interface for compiled MVEL expression evaluators.
 * <p>
 * Generated evaluator classes implement this interface with specific type parameters:
 * <ul>
 *   <li>{@code C} - Context type: the main input (Map, List, or POJO class)</li>
 *   <li>{@code W} - With type: optional root context object (typically {@code Void} when not used)</li>
 *   <li>{@code O} - Output type: the return type of the expression</li>
 * </ul>
 * <p>
 * <b>Important:</b> Each generated evaluator class implements only ONE of the eval methods below,
 * depending on how it was compiled. The other methods retain the default implementation that throws
 * {@code RuntimeException}. Use the fluent builder API ({@link MVEL#map}, {@link MVEL#list},
 * {@link MVEL#pojo}) to compile expressions - the returned evaluator will have the appropriate
 * method implemented.
 * <p>
 * <b>Method usage by context type:</b>
 * <ul>
 *   <li>MAP context ({@code MVEL.map(...)}): Use {@link #eval(Object) eval(C c)} where C is {@code Map}</li>
 *   <li>LIST context ({@code MVEL.list(...)}): Use {@link #eval(Object) eval(C c)} where C is {@code List}</li>
 *   <li>POJO context ({@code MVEL.pojo(...)}): Use {@link #eval(Object) eval(C c)} where C is the POJO class</li>
 *   <li>With root context: Use {@link #eval(Object, Object) eval(C c, W w)} when a "with" object is configured</li>
 * </ul>
 *
 * @param <C> the context type (Map, List, or POJO)
 * @param <W> the "with" root context type (Void if not used)
 * @param <O> the output/return type
 */
public interface Evaluator<C, W, O> {

    /**
     * Evaluates the compiled expression with the given context.
     * <p>
     * This is the primary evaluation method used for MAP, LIST, and POJO contexts.
     * Variables are extracted from the context object based on the context type:
     * <ul>
     *   <li>MAP: Variables extracted via {@code map.get("varName")}</li>
     *   <li>LIST: Variables extracted via {@code list.get(index)}</li>
     *   <li>POJO: Variables extracted via getter methods</li>
     * </ul>
     *
     * @param c the context object containing input variables
     * @return the result of evaluating the expression
     * @throws RuntimeException if this method is not implemented for this evaluator
     */
    default O eval(C c) {
        throw new RuntimeException("Not Implemented");
    }

    /**
     * Evaluates the compiled expression with both a context and a "with" root object.
     * <p>
     * This method is used when the expression was compiled with a "with" root context,
     * allowing access to both context variables and a root object.
     *
     * @param c the context object containing input variables
     * @param w the "with" root object
     * @return the result of evaluating the expression
     * @throws RuntimeException if this method is not implemented for this evaluator
     */
    default O eval(C c, W w) {
        throw new RuntimeException("Not Implemented");
    }

    /**
     * Evaluates the compiled expression with only a "with" root object.
     * <p>
     * This method is used when the expression operates solely on a root object
     * without additional context variables.
     *
     * @param w the "with" root object
     * @return the result of evaluating the expression
     * @throws RuntimeException if this method is not implemented for this evaluator
     */
    default O evalWith(W w) {
        throw new RuntimeException("Not Implemented");
    }
}
