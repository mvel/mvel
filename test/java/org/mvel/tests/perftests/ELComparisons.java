package org.mvel.tests.perftests;

import ognl.Ognl;
import org.mvel.MVEL;
import org.mvel.tests.main.res.Base;

import static java.lang.System.currentTimeMillis;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Performance Tests Comparing MVEL to OGNL with Same Expressions.
 */
public class ELComparisons {
    private Base baseClass = new Base();

    private static int mvel = 1;
    private static int ognl = 1 << 1;

    private static int ALL = mvel + ognl;

    private static final int TESTNUM = 50000;
    private static final int TESTITER = 5;

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
        runTest("Simple String Pass-Through", "'Hello World'", TESTNUM, ALL);
        runTest("Shallow Property", "data", TESTNUM, ALL);
        runTest("Deep Property", "foo.bar.name", TESTNUM, ALL);
        runTest("Static Field Access (MVEL)", "Integer.MAX_VALUE", TESTNUM, mvel);
        runTest("Static Field Access (OGNL)", "@java.lang.Integer@MAX_VALUE", TESTNUM, ognl);
        runTest("Inline Array Creation (MVEL)", "{'foo', 'bar'}", TESTNUM, mvel);
        runTest("Inline Array Creation (OGNL)", "new String[] {'foo', 'bar'}", TESTNUM, ognl);
        runTest("Collection Access + Method Call", "funMap['foo'].happy()", TESTNUM, ALL);
        runTest("Boolean compare", "data == 'cat'", TESTNUM, ALL);
        runTest("Object instantiation", "new String('Hello')", TESTNUM, ALL);
        runTest("Method access", "readBack('this is a string')", TESTNUM, ALL);
        runTest("Arithmetic", "10 + 1 - 1", TESTNUM, ALL);
    }

    public void runTest(String name, String expression, int count, int totest) throws Exception {
        System.out.println("Test Name            : " + name);
        System.out.println("Expression           : " + expression);
        System.out.println("Iterations           : " + count);

        System.out.println("Interpreted Results  :");

        long time;
        long mem;
        long total = 0;
        long[] res = new long[TESTITER];


        if ((totest & ognl) != 0) {
            try {
                // unbenched warm-up
                for (int i = 0; i < count; i++) {
                    Ognl.getValue(expression, baseClass);
                }

                System.gc();

                time = currentTimeMillis();
                mem = Runtime.getRuntime().freeMemory();

                for (int reps = 0; reps < TESTITER; reps++) {
                    for (int i = 0; i < count; i++) {
                        Ognl.getValue(expression, baseClass);
                    }

                    if (reps == 0) res[0] = total += currentTimeMillis() - time;
                    else res[reps] = (total * -1) + (total += currentTimeMillis() - time - total);
                }
                System.out.println("(OGNL)               : " + new BigDecimal(((currentTimeMillis() - time))).divide(new BigDecimal(TESTITER), 2, RoundingMode.HALF_UP)
                        + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb) " + resultsToString(res));
            }
            catch (Exception e) {
                System.out.println("(OGNL)               : <<COULD NOT EXECUTE>>");
            }

        }

        total = 0;

        if ((totest & mvel) != 0) {
            try {
                for (int i = 0; i < count; i++) {
                    MVEL.eval(expression, baseClass);
                }

                System.gc();

                time = currentTimeMillis();
                mem = Runtime.getRuntime().freeMemory();
                for (int reps = 0; reps < TESTITER; reps++) {
                    for (int i = 0; i < count; i++) {
                        MVEL.eval(expression, baseClass);
                    }

                    if (reps == 0) res[0] = total += currentTimeMillis() - time;
                    else res[reps] = (total * -1) + (total += currentTimeMillis() - time - total);
                }
                System.out.println("(MVEL)               : " + new BigDecimal(((currentTimeMillis() - time))).divide(new BigDecimal(TESTITER), 2, RoundingMode.HALF_UP)
                        + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb) " + resultsToString(res));

            }
            catch (Exception e) {
                System.out.println("(MVEL)               : <<COULD NOT EXECUTE>>");
            }
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


        runTestCompiled(name, expression, count, totest);

        System.out.println("------------------------------------------------");
    }

    public void runTestCompiled(String name, String expression, int count, int totest) throws Exception {
        Object compiled;

        long time;
        long mem;
        long total = 0;
        long[] res = new long[TESTITER];


        System.out.println("Compiled Results     :");


        if ((totest & ognl) != 0) {
            try {
                compiled = Ognl.parseExpression(expression);
                for (int i = 0; i < count; i++) {
                    Ognl.getValue(compiled, baseClass);
                }

                System.gc();

                time = currentTimeMillis();
                mem = Runtime.getRuntime().freeMemory();

                for (int reps = 0; reps < TESTITER; reps++) {
                    for (int i = 0; i < count; i++) {
                        Ognl.getValue(compiled, baseClass);
                    }

                    if (reps == 0) res[0] = total += currentTimeMillis() - time;
                    else res[reps] = (total * -1) + (total += currentTimeMillis() - time - total);
                }
                System.out.println("(OGNL Compiled)      : " + new BigDecimal(currentTimeMillis() - time).divide(new BigDecimal(TESTITER), 2, RoundingMode.HALF_UP)
                        + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb) " + resultsToString(res));
            }
            catch (Exception e) {
                System.out.println("(OGNL)               : <<COULD NOT EXECUTE>>");
            }
        }

        total = 0;

        if ((totest & mvel) != 0) {

            try {
                compiled = MVEL.compileExpression(expression);
                for (int i = 0; i < count; i++) {
                    MVEL.executeExpression(compiled, baseClass);
                }

                System.gc();

                time = currentTimeMillis();
                mem = Runtime.getRuntime().freeMemory();

                for (int reps = 0; reps < TESTITER; reps++) {
                    for (int i = 0; i < count; i++) {
                        MVEL.executeExpression(compiled, baseClass);
                    }

                    if (reps == 0) res[0] = total += currentTimeMillis() - time;
                    else res[reps] = (total * -1) + (total += currentTimeMillis() - time - total);
                }
                System.out.println("(MVEL Compiled)      : " + new BigDecimal(currentTimeMillis() - time).divide(new BigDecimal(TESTITER), 2, RoundingMode.HALF_UP)
                        + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb) " + resultsToString(res));
            }
            catch (Exception e) {
                System.out.println("(MVEL)               : <<COULD NOT EXECUTE>>");
            }
        }
    }

    private static String resultsToString(long[] res) {
        StringBuffer sbuf = new StringBuffer("[");
        for (int i = 0; i < res.length; i++) {
            sbuf.append(res[i]);

            if ((i + 1) < res.length) sbuf.append(",");
        }
        sbuf.append("]");

        return sbuf.toString();
    }

}
