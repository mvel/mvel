package org.mvel.tests;

import ognl.Ognl;
import org.mvel.ExpressionParser;

/**
 * Performance Tests Comparing MVEL to OGNL with Same Expressions.
 */
public class OgnlMvelComparison {
    private Base baseClass = new Base();

    private static final int TESTNUM = 10000;

    public static void main(String[] args) throws Exception {
        new OgnlMvelComparison().runTests();
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
        for (int reps = 0; reps < 4; reps++) {
            for (int i = 0; i < count; i++) {
                Ognl.getValue(expression, baseClass);
            }
        }
        System.out.println("(OGNL)               : " + ((System.currentTimeMillis() - time) / 4)
                + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb)");

        System.gc();

        time = System.currentTimeMillis();
        mem = Runtime.getRuntime().freeMemory();
        for (int reps = 0; reps < 4; reps++) {
            for (int i = 0; i < count; i++) {
                ExpressionParser.eval(expression, baseClass);
            }
        }
        System.out.println("(MVEL)               : " + ((System.currentTimeMillis() - time) / 4)
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
        for (int reps = 0; reps < 4; reps++) {
            for (int i = 0; i < count; i++) {
                Ognl.getValue(compiled, baseClass);
            }
        }
        System.out.println("(OGNL Compiled)      : " + ((System.currentTimeMillis() - time) / 4)
                + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb)");

        System.gc();

        time = System.currentTimeMillis();
        mem = Runtime.getRuntime().freeMemory();

        compiled = ExpressionParser.compileExpression(expression);
        for (int reps = 0; reps < 4; reps++) {
            for (int i = 0; i < count; i++) {
                ExpressionParser.executeExpression(compiled, baseClass);
            }
        }
        System.out.println("(MVEL Compiled)      : " + ((System.currentTimeMillis() - time) / 4)
                + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb)");
    }

}
