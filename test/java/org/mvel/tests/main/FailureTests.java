package org.mvel.tests.main;

import sun.jvm.hotspot.utilities.AssertionFailure;
import org.mvel.PropertyAccessException;

/**
 * Tests to ensure MVEL fails when it should.
 */
public class FailureTests extends AbstractTest {
    public void testIncompleteStatement() {
        try {
            test("someUnknownToken");

        }
        catch (Throwable e) {
            return;
        }

        throw new AssertionFailure();
    }


}
