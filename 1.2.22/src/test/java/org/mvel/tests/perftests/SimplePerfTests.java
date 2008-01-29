package org.mvel.tests.perftests;

import ognl.Ognl;
import org.mvel.MVEL;
import org.mvel.optimizers.OptimizerFactory;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: christopherbrock
 * Date: Jul 18, 2007
 * Time: 3:55:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimplePerfTests {

    public static void main(String[] args) throws Exception {

        // Expression we'll test.
        String expression = "foo.bar.name";

        // Number of iterations
        int iterations = 100000;

        Base base = new Base();

        // Compile expression in MVEL
        Serializable mvelCompiled = MVEL.compileExpression(expression);

        // Disable MVEL's JIT by making the default optimizer the Reflective optimizer.
   //     OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);

        // Compile OGNL AST
        Object ognlCompiled = Ognl.parseExpression(expression);


        // We loop twice, once to warm up HotSpot.
        for (int repeat = 0; repeat < 2; repeat++) {

            long tm = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                MVEL.executeExpression(mvelCompiled, base);
            }

            // Let's not report the results the first time around, HotSpot needs to warm up
            if (repeat != 0) System.out.println("MVEL  : " + ((System.currentTimeMillis() - tm)) + "ms");


            tm = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                Ognl.getValue(ognlCompiled, base);
            }

            // See above.
            if (repeat != 0) System.out.println("OGNL  : " + ((System.currentTimeMillis() - tm)) + "ms");
        }
    }


    public static class Base {
        private Foo foo = new Foo();


        public Foo getFoo() {
            return foo;
        }

        public void setFoo(Foo foo) {
            this.foo = foo;
        }
    }

    public static class Foo {
        private Bar bar = new Bar();


        public Bar getBar() {
            return bar;
        }

        public void setBar(Bar bar) {
            this.bar = bar;
        }
    }

    public static class Bar {
        private String name = "test";


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
