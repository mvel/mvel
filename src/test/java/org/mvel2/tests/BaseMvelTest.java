package org.mvel2.tests;

import org.junit.After;
import org.junit.Before;
import org.mvel2.MVEL;
import org.mvel2.optimizers.OptimizerFactory;

/**
 * Base class for Junit 4 TestCase.
 */
public class BaseMvelTest {

    // Clean up on both setUp and tearDown,
    // so tests extending this base class don't leak configuration changes to other tests
    @Before
    public void setUp() {
        cleanUpConfigurations();
    }

    @After
    public void tearDown() {
        cleanUpConfigurations();
    }

    public static void cleanUpConfigurations() {
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);
        MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = false;
        MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = false;
        MVEL.COMPILER_OPT_ALLOW_RESOLVE_INNERCLASSES_WITH_DOTNOTATION = false;
        MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS = false;
    }
}
