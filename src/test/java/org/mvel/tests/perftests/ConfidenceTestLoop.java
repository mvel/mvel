package org.mvel.tests.perftests;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestResult;
import org.mvel.tests.main.CoreConfidenceTests;

/**
 * Created by IntelliJ IDEA.
 * User: brockm
 * Date: Jul 3, 2007
 * Time: 6:08:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfidenceTestLoop extends TestCase {
    private static final int loops = 1000000;

    public void testUnitTests() {
        CoreConfidenceTests tests = new CoreConfidenceTests();

        for (int i = 0; i < loops; i++) {
            tests.testAssertion();
            tests.testAnd();
            tests.testDeepAssignment();
        }
    }

}
