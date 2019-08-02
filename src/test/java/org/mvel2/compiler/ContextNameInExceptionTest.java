package org.mvel2.compiler;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mvel2.MVEL;
import org.mvel2.PropertyAccessException;

import java.io.Serializable;
import java.util.Collections;

public class ContextNameInExceptionTest {
    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void testWrongMethodName() {
        expected.expect(PropertyAccessException.class);
        expected.expectMessage("unable to resolve method: org.mvel2.compiler.ContextNameInExceptionTest$Context.doSmth(java.lang.String)");
        final Serializable serializable = MVEL.compileExpression("ctx.doSmth('test')");
        MVEL.executeExpression(serializable, Collections.singletonMap("ctx", new Context()));
    }

    @Test
    public void testWrongParametersCount() {
        expected.expect(PropertyAccessException.class);
        expected.expectMessage("unable to resolve method: org.mvel2.compiler.ContextNameInExceptionTest$Context.doSomething(java.lang.String, java.lang.Integer)");
        final Serializable serializable = MVEL.compileExpression("ctx.doSomething('test', 0)");
        MVEL.executeExpression(serializable, Collections.singletonMap("ctx", new Context()));
    }

    @Test
    public void testPropertyAccessExceptionInMainContext() {
        expected.expect(PropertyAccessException.class);
        expected.expectMessage("null pointer or function not found: doSomething");
        final Serializable serializable = MVEL.compileExpression("doSomething('test', 0)");
        MVEL.executeExpression(serializable, Collections.singletonMap("ctx", new Context()));
    }

    public static final class Context {
        public String doSomething(String arg) {
            return arg;
        }
    }
}
