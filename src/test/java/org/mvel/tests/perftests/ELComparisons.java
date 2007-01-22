package org.mvel.tests.perftests;

import ognl.Ognl;
import org.mvel.ExpressionParser;
import org.mvel.tests.main.res.Base;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Performance Tests Comparing MVEL to OGNL with Same Expressions.
 */
public class ELComparisons {
    private Base baseClass = new Base();

    private static final int TESTNUM = 10000;

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

        System.gc();

        long time = System.currentTimeMillis();
        long mem = Runtime.getRuntime().freeMemory();
        for (int reps = 0; reps < 6; reps++) {
            for (int i = 0; i < count; i++) {
                Ognl.getValue(expression, baseClass);
            }
        }
        System.out.println("(OGNL)               : " + new BigDecimal(((System.currentTimeMillis() - time))).divide(new BigDecimal(6), 2, RoundingMode.HALF_UP)
                + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb)");

        System.gc();

        time = System.currentTimeMillis();
        mem = Runtime.getRuntime().freeMemory();
        for (int reps = 0; reps < 6; reps++) {
            for (int i = 0; i < count; i++) {
                ExpressionParser.eval(expression, baseClass);
                //   ExpressionParser.executeExpression(ExpressionParser.compileExpression(expression), baseClass);
            }
        }
        System.out.println("(MVEL)               : " + new BigDecimal(((System.currentTimeMillis() - time))).divide(new BigDecimal(6), 2, RoundingMode.HALF_UP)
                + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb)");

        runTestCompiled(name, expression, count);

        System.out.println("------------------------------------------------");
    }

    public void runTestCompiled(String name, String expression, int count) throws Exception {


        System.out.println("Compiled Results     :");

        System.gc();

        long time = System.currentTimeMillis();
        long mem = Runtime.getRuntime().freeMemory();

        Object compiled = Ognl.parseExpression(expression);
        for (int reps = 0; reps < 6; reps++) {
            for (int i = 0; i < count; i++) {
                Ognl.getValue(compiled, baseClass);
            }
        }
        System.out.println("(OGNL Compiled)      : " + new BigDecimal(System.currentTimeMillis() - time).divide(new BigDecimal(6), 2, RoundingMode.HALF_UP)
                + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb)");

        System.gc();

        time = System.currentTimeMillis();
        mem = Runtime.getRuntime().freeMemory();

        compiled = ExpressionParser.compileExpression(expression);
        for (int reps = 0; reps < 6; reps++) {
            for (int i = 0; i < count; i++) {
                ExpressionParser.executeExpression(compiled, baseClass);
            }
        }
        System.out.println("(MVEL Compiled)      : " + new BigDecimal(System.currentTimeMillis() - time).divide(new BigDecimal(6), 2, RoundingMode.HALF_UP)
                + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb)");
    }

}
