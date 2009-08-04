package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.PropertyHandlerFactory;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;

public class IntegrationTests extends AbstractTest {
    class NullPropertyHandler implements PropertyHandler {

        public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory) {
            return null;
        }

        public Object setProperty(String name, Object contextObj, VariableResolverFactory variableFactory, Object value) {
            return null;
        }
    }


    class MyClass {
        public String getWhatever() {
            return "foo";
        }
    }

    class MySubClass extends MyClass {
    }

    interface MyInterface {
    }

    class MyInterfacedClass implements MyInterface {
    }

    class MyInterfacedSubClass extends MyInterfacedClass {
    }


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;

        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
        PropertyHandlerFactory.registerPropertyHandler(MyClass.class, new NullPropertyHandler());
        PropertyHandlerFactory.registerPropertyHandler(MyInterface.class, new NullPropertyHandler());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = false;
        
        PropertyHandlerFactory.unregisterPropertyHandler(MyClass.class);
        PropertyHandlerFactory.unregisterPropertyHandler(MyInterface.class);
    }

    public void test1() {
        // handler for subclass is not found, but its superclass handler is registered
        // why didn't use its superclass handler?
        assertEquals(null, MVEL.eval("whatever", new MySubClass()));
    }

    public void test2() {
        // "NullPointerException" fired
        assertEquals(null, MVEL.eval("whatever", new MyInterfacedSubClass()));
    }
}
