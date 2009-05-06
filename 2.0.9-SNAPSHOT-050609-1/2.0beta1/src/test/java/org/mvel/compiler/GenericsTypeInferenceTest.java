package org.mvel.compiler;

import junit.framework.TestCase;
import org.mvel.ParserContext;
import org.mvel.MVEL;

import java.util.List;
import java.util.Arrays;

/**
 * Asserts that the element at the end of the parse chain has its type parameter correctly inferred
 * IF the egress type is a parametric type (i.e. generic).
 *
 *
 * @author Dhanji R. Prasanna (dhanji@gmail com)
 */
public class GenericsTypeInferenceTest extends TestCase {
    private static final List<String> STRINGS = Arrays.asList("hi", "there", "dude");

    public final void testInferLastTypeParametersFromProperty() {
        ParserContext context = new ParserContext();
        context.setStrongTyping(true);

        context.addInput("a", A.class);

        final CompiledExpression compiledExpression = new ExpressionCompiler("a.strings")
                .compile(context);

        final Object val = MVEL.executeExpression(compiledExpression, new AWrapper());

        assertTrue("Expression did not evaluate correctly: " + val, STRINGS.equals(val));
        assertTrue("No type parameters detected", null != context.getLastTypeParameters());
        assertTrue("Wrong parametric type inferred", String.class.equals(context.getLastTypeParameters()[0]));
    }

    public final void testInferLastTypeParametersFromMethod() {
        ParserContext context = new ParserContext();
        context.setStrongTyping(true);

        context.addInput("a", A.class);

        final CompiledExpression compiledExpression = new ExpressionCompiler("a.values()")
                .compile(context);

        final Object val = MVEL.executeExpression(compiledExpression, new AWrapper());

        assertTrue("Expression did not evaluate correctly: " + val, STRINGS.equals(val));
        assertTrue("No type parameters detected", null != context.getLastTypeParameters());
        assertTrue("Wrong parametric type inferred", String.class.equals(context.getLastTypeParameters()[0]));
    }

    public static class AWrapper {
        public A getA() {
            return new A();
        }
    }

    public static class A {

        public List<String> getStrings() {
            return STRINGS;
        }

        public List<String> values() {
            return STRINGS;
        }
    }
}
