package org.mvel2.tests.core;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mvel2.MVEL;
import org.mvel2.tests.BaseMvelTest;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class InterpretedStatementSeparatorTest extends BaseMvelTest {
    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"; value = 1; value + 1", 2},
                {";; value = 1; value + 1", 2},
                {"value = 1;; value + 1", 2},
                {"value = 1;;; value + 1", 2},
                {"value = 1;;;; value + 1", 2},
                {"42;;", null},
                {"42;;;", null},
                {"value = 'x'; if (false) { value = 'a'; } else if (true) { value = 'b'; } "
                        + "value = value + 'c'; return value;", "bc"},
                {"value = 'x'; if (false) { value = 'a'; } else if (false) { value = 'b'; } "
                        + "else { value = 'd'; } value = value + 'c'; return value;", "dc"},
                {"value = 'x'; if (true) { value = 'a'; } else if (false) { value = 'b'; } "
                        + "value = value + 'c'; return value;", "ac"},
                {"value = 'x'; if (false) { value = 'a'; } else if (false) { value = 'b'; } "
                        + "value = value + 'c'; return value;", "xc"},
                {"value = 'x'; if (false) { value = 'a'; } else if (false) { value = 'b'; } "
                        + "else if (true) { value = 'd'; } value = value + 'c'; return value;", "dc"},
                {"if (false) { 1; } else if (true) { 2; }", 2},
                {";", null},
                {";;", null}
        });
    }

    private final String expression;
    private final Object expected;

    public InterpretedStatementSeparatorTest(String expression, Object expected) {
        this.expression = expression;
        this.expected = expected;
    }

    @Test
    public void interpretsStatementSeparators() {
        assertEquals(expected, MVEL.eval(expression, new HashMap<>()));
    }

    @Test
    public void compilesStatementSeparators() {
        assertEquals(expected, MVEL.executeExpression(MVEL.compileExpression(expression), new HashMap<>()));
    }
}
