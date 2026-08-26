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

    // Clean up on both setUp and teatDown,
    // so never affect/affected by other classes which don't extend the base test class
    @Override
    protected void setUp() throws Exception {
        cleanUpConfigurations();
    }

    @Override
    protected void tearDown() throws Exception {
        cleanUpConfigurations();
    }
}
