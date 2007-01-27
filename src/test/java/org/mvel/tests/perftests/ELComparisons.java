package org.mvel.tests.perftests;

import ognl.Ognl;
import org.mvel.MVEL;
import org.mvel.optimizers.impl.refl.ReflectiveOptimizer;
import org.mvel.tests.main.res.Base;

import java.math.BigDecimal;
import java.math.RoundingMode;

import wicket.util.lang.PropertyResolver;

/**
 * Performance Tests Comparing MVEL to OGNL with Same Expressions.
 */
public class ELComparisons {
    private Base baseClass = new Base();

    private static final int TESTNUM = 100000;

    public ELComparisons() {
    }

    public static void main(String[] args) throws Exception {
        ELComparisons omc = new ELComparisons();
        if (args.length > 0 && args[0].equals("-continuous")) {
            while (true) omc.runTests();
        }

        omc.runTests();
    }

    public void runTests() throws Exception {
        runTest("Simple String Pass-Through", "'Hello World'", TESTNUM);
        runTest("Shallow Property", "data", TESTNUM);
        runTest("Deep Property", "foo.bar.name", TESTNUM);
        runTest("Static Field Access (MVEL)", "Integer.MAX_VALUE", TESTNUM);
        runTest("Static Field Access (OGNL)", "@java.lang.Integer@MAX_VALUE", TESTNUM);
        runTest("Arithmetic", "10 + 1 - 1", TESTNUM);
        runTest("Collection Access + Method Call", "funMap['foo'].happy()", TESTNUM);
        runTest("Boolean compare", "data == 'cat'", TESTNUM);
        runTest("Object instantiation", "new String('Hello')", TESTNUM);
        runTest("Method access", "readBack('this is a string')", TESTNUM);

    }

    public void runTest(String name, String expression, int count) throws Exception {
        System.out.println("Test Name            : " + name);
        System.out.println("Expression           : " + expression);
        System.out.println("Iterations           : " + count);

        System.out.println("Results              :");

        long time;
        long mem;


        try {
            // unbenched warm-up
            for (int i = 0; i < count; i++) {
                Ognl.getValue(expression, baseClass);
            }

            System.gc();

            time = System.currentTimeMillis();
            mem = Runtime.getRuntime().freeMemory();

            for (int reps = 0; reps < 5; reps++) {
                for (int i = 0; i < count; i++) {
                    Ognl.getValue(expression, baseClass);
                }
            }
            System.out.println("(OGNL)               : " + new BigDecimal(((System.currentTimeMillis() - time))).divide(new BigDecimal(6), 2, RoundingMode.HALF_UP)
                    + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb)");
        }
        catch (Exception e) {
            System.out.println("(OGNL)               : <<COULD NOT EXECUTE>>");
        }


        try {
            for (int i = 0; i < count; i++) {
                MVEL.eval(expression, baseClass);
            }

            System.gc();

            time = System.currentTimeMillis();
            mem = Runtime.getRuntime().freeMemory();
            for (int reps = 0; reps < 5; reps++) {
                for (int i = 0; i < count; i++) {
                    MVEL.eval(expression, baseClass);
                }
            }
            System.out.println("(MVEL)               : " + new BigDecimal(((System.currentTimeMillis() - time))).divide(new BigDecimal(6), 2, RoundingMode.HALF_UP)
                    + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb)");

        }
        catch (Exception e) {
            System.out.println("(MVEL)               : <<COULD NOT EXECUTE>>");
        }

//
//        try {
//            for (int i = 0; i < count; i++) {
//                ReflectiveOptimizer.get(expression, baseClass);
//            }
//
//            System.gc();
//
//            time = System.currentTimeMillis();
//            mem = Runtime.getRuntime().freeMemory();
//            for (int reps = 0; reps < 5; reps++) {
//                for (int i = 0; i < count; i++) {
//                    ReflectiveOptimizer.get(expression, baseClass);
//                }
//            }
//            System.out.println("(MVELPropAcc)        : " + new BigDecimal(((System.currentTimeMillis() - time))).divide(new BigDecimal(6), 2, RoundingMode.HALF_UP)
//                    + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb)");
//
//        }
//        catch (Exception e) {
//            System.out.println("(MVELPropAcc)        : <<COULD NOT EXECUTE>>");
//        }
//
//
//        try {
//            for (int i = 0; i < count; i++) {
//                PropertyResolver.getValue(expression, baseClass);
//            }
//
//            System.gc();
//
//            time = System.currentTimeMillis();
//            mem = Runtime.getRuntime().freeMemory();
//            for (int reps = 0; reps < 5; reps++) {
//                for (int i = 0; i < count; i++) {
//                    PropertyResolver.getValue(expression, baseClass);
//                }
//            }
//            System.out.println("(WicketPropRes)      : " + new BigDecimal(((System.currentTimeMillis() - time)))
//                    .divide(new BigDecimal(6), 2, RoundingMode.HALF_UP)
//                    + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb)");
//        }
//        catch (Exception e) {
//            System.out.println("(WicketPropRes)      : <<COULD NOT EXECUTE>>");
//        }


        runTestCompiled(name, expression, count);

        System.out.println("------------------------------------------------");
    }

    public void runTestCompiled(String name, String expression, int count) throws Exception {
        Object compiled;
        long time;
        long mem;

        System.out.println("Compiled Results     :");

        try {
            compiled = Ognl.parseExpression(expression);
            for (int i = 0; i < count; i++) {
                Ognl.getValue(compiled, baseClass);
            }

            System.gc();

            time = System.currentTimeMillis();
            mem = Runtime.getRuntime().freeMemory();

            for (int reps = 0; reps < 5; reps++) {
                for (int i = 0; i < count; i++) {
                    Ognl.getValue(compiled, baseClass);
                }
            }
            System.out.println("(OGNL Compiled)      : " + new BigDecimal(System.currentTimeMillis() - time).divide(new BigDecimal(6), 2, RoundingMode.HALF_UP)
                    + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb)");
        }
        catch (Exception e) {
            System.out.println("(OGNL)               : <<COULD NOT EXECUTE>>");
        }

        try {
            compiled = MVEL.compileExpression(expression);
            for (int i = 0; i < count; i++) {
                MVEL.executeExpression(compiled, baseClass);
            }

            System.gc();

            time = System.currentTimeMillis();
            mem = Runtime.getRuntime().freeMemory();

            for (int reps = 0; reps < 5; reps++) {
                for (int i = 0; i < count; i++) {
                    MVEL.executeExpression(compiled, baseClass);
                }
            }
            System.out.println("(MVEL Compiled)      : " + new BigDecimal(System.currentTimeMillis() - time).divide(new BigDecimal(6), 2, RoundingMode.HALF_UP)
                    + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb)");
        }
        catch (Exception e) {
            System.out.println("(MVEL)               : <<COULD NOT EXECUTE>>");
        }
    }

}
