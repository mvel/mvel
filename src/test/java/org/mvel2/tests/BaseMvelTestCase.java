package org.mvel2.tests;

import junit.framework.TestCase;

import static org.mvel2.tests.BaseMvelTest.cleanUpConfigurations;

/**
 * Base class for Junit 3 TestCase.
 */
public class BaseMvelTestCase extends TestCase {

    public BaseMvelTestCase() {
        super();
    }

    // Clean up on both setUp and tearDown,
    // so tests extending this base class don't leak configuration changes to other tests
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cleanUpConfigurations();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            cleanUpConfigurations();
        } finally {
            super.tearDown();
        }
    }
}
