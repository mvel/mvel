package org.mvel2.tests.core;

import org.mvel2.MVEL;
import junit.framework.TestCase;

import java.util.List;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Dhanji R. Prasanna (dhanji@gmail com)
 */
public class FunctionsTest extends TestCase {

    public final void testThatFunctionsCloseOverArguments() {
        final Object o = MVEL.eval(
                "def fun(x) { ($ in [1, 2, 3] if $ > x) }" +
                        "" +
                        "fun(0)",
                new HashMap<String, Object>()
        );

        assertTrue(o instanceof List);
        assertEquals(Arrays.asList(1, 2, 3), o);
    }
}
