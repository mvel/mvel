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

    @Override
    protected void setUp() throws Exception {
        cleanUpConfigurations();
    }
}
