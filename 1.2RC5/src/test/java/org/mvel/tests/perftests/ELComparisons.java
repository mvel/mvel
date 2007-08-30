package org.mvel.tests.perftests;

import ognl.Ognl;
import org.apache.commons.el.ExpressionEvaluatorImpl;
import org.mvel.MVEL;
import org.mvel.tests.main.res.Base;
import org.mvel.tests.main.res.Foo;

import javax.servlet.jsp.el.Expression;
import javax.servlet.jsp.el.VariableResolver;
import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performance Tests Comparing MVEL to OGNL with Same Expressions.
 */
public class ELComparisons implements Runnable {
    private final Base baseClass = new Base();

    public static int RUN_MVEL = 1;
    public static int RUN_OGNL = 1 << 1;
    public static int RUN_COMMONS_EL = 1 << 2;
    public static int RUN_JAVA_NATIVE = 1 << 3;
    // public static int RUN_GROOVY = 1 << 4;

    private static int COMPILED = 1 << 30;
    private static int INTERPRETED = 1 << 31;

    private static int ALL = RUN_MVEL + RUN_OGNL + RUN_COMMONS_EL + RUN_JAVA_NATIVE;

    private static final int TESTNUM = 100000;
    private static final int TESTITER = 3;

    private long ognlTotal = 0;
    private long mvelTotal = 0;
    private long commonElTotal = 0;
    private long javaNativeTotal = 0;
    private long groovyTotal = 0;

    private int testFlags = 0;

    private boolean silent = false;

    private static List<PerfTest> tests = new ArrayList<PerfTest>();

    private final static Map<String, Object> variables = new HashMap<String, Object>();

    static {
        NativeTest nt;

//        nt = new NativeTest() {
//
//            public Object run(Object baseClass, Map vars) {
//                return "Hello World";
//            }
//        };
//
//        tests.add(new PerfTest("Simple String Pass-Through", "'Hello World'", ALL, nt));

        nt = new NativeTest() {

            public Object run(Object baseClass, Map vars) {
                return vars.get("data");
            }
        };

//        tests.add(new PerfTest("Shallow Property", "data", ALL, nt));

        nt = new NativeTest() {
            public Object run(Object baseClass, Map vars) {
                return ((Base) baseClass).getFoo().getBar().getName();
            }
        };

        tests.add(new PerfTest("Deep Property", "foo.bar.name", ALL, nt));
        tests.add(new PerfTest("Static Field Access (MVEL)", "Integer.MAX_VALUE", RUN_MVEL, nt));
        tests.add(new PerfTest("Static Field Access (OGNL)", "@java.lang.Integer@MAX_VALUE", RUN_OGNL, nt));
        tests.add(new PerfTest("Inline Array Creation (MVEL)", "{'foo', 'bar'}", RUN_MVEL, nt));
        tests.add(new PerfTest("Inline Array Creation (OGNL)", "new String[] {'foo', 'bar'}", RUN_OGNL, nt));


        nt = new NativeTest() {
            public Object run(Object baseClass, Map vars) {
                return ((Foo) ((Base) baseClass).funMap.get("foo")).happy();
            }
        };

        nt = new NativeTest() {

            public Object run(Object baseClass, Map vars) {
                return 10 + 1 - 1;
            }
        };

        //      tests.add(new PerfTest("Collection Access + Method Call", "funMap['foo'].happy()", RUN_MVEL + RUN_OGNL + RUN_JAVA_NATIVE, nt));
//        tests.add(new PerfTest("Boolean compare", "data == 'cat'", ALL));
//        tests.add(new PerfTest("Object instantiation", "new String('Hello')", RUN_OGNL + RUN_MVEL));
//        tests.add(new PerfTest("Method access", "readBack('this is a string')", RUN_OGNL + RUN_MVEL));
        //    tests.add(new PerfTest("Arithmetic", "10 + 1 - 1", ALL, nt));
    }


    public ELComparisons() {
        variables.put("data", baseClass.data);
        variables.put("foo", baseClass.foo);
        variables.put("funMap", baseClass.funMap);
    }


    public void setTestFlags(int testFlags) {
        this.testFlags = testFlags;
    }

    public static void main(String[] args) throws Exception {
        ELComparisons omc = new ELComparisons();
        boolean multithreaded = false;
        boolean compiled = true;
        boolean interpreted = true;
        boolean continuous = false;
        boolean silent = false;
        long totaltime = 0;

        int threadMax = 1;

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-continuous")) continuous = true;
                else if (args[i].equals("-threaded")) {
                    if ((i + 1) == args.length) {
                        throw new RuntimeException("expected parameter for -threaded (number of threads)");
                    }
                    multithreaded = true;
                    threadMax = parseInt(args[++i]);

                }
                else if (args[i].equals("-nocompiled")) compiled = false;
                else if (args[i].equals("-nointerpret")) interpreted = false;
                else if (args[i].equals("-silent")) silent = true;
            }

        }

        int flags = (compiled ? COMPILED : 0) + (interpreted ? INTERPRETED : 0);

        long ognlTotals;
        long mvelTotals;
        long commonsElTotals;
        long javaNativeTotals;
        long groovyTotals;

        ELComparisons ognlTests = new ELComparisons();
        ognlTests.setTestFlags(flags + RUN_OGNL);
        ognlTests.setSilent(true);

        ELComparisons mvelTests = new ELComparisons();
        mvelTests.setTestFlags(flags + RUN_MVEL);
        mvelTests.setSilent(silent);

//        ELComparisons groovyTests = new ELComparisons();
//        groovyTests.setTestFlags(flags + RUN_GROOVY);
//        groovyTests.setSilent(silent);


        ELComparisons commonsELTests = new ELComparisons();
        commonsELTests.setTestFlags(flags + RUN_COMMONS_EL);
        commonsELTests.setSilent(silent);

        ELComparisons nativeJavaTests = new ELComparisons();
        nativeJavaTests.setTestFlags(flags + RUN_JAVA_NATIVE);
        nativeJavaTests.setSilent(silent);


        if (multithreaded) {
            System.out.println("THREADS\tOGNL\tMVEL\tCommons-EL\tNative Java");

            for (int threadNumber = 1; threadNumber < 100; threadNumber += 5) {

                totaltime = System.currentTimeMillis();

                ognlTests.reset();

                Thread[] threads = new Thread[threadNumber];
                for (int i = 0; i < threads.length; i++) {
                    threads[i] = new Thread(ognlTests);
                }

                for (Thread thread : threads) {
                    thread.run();
                }

                for (Thread thread : threads) {
                    thread.join();
                }

                ognlTotals = ognlTests.getOgnlTotal();


                mvelTests.reset();

                for (int i = 0; i < threads.length; i++) {
                    threads[i] = new Thread(mvelTests);
                }

                for (Thread thread : threads) {
                    thread.run();
                }

                for (Thread thread : threads) {
                    thread.join();
                }

                mvelTotals = mvelTests.getMvelTotal();

//                groovyTests.reset();
//
//                for (int i = 0; i < threads.length; i++) {
//                    threads[i] = new Thread(groovyTests);
//                }
//
//                for (Thread thread : threads) {
//                    thread.run();
//                }
//
//                for (Thread thread : threads) {
//                    thread.join();
//                }
//
//                groovyTotals = groovyTests.getGroovyTotal();

                commonsELTests.reset();

                for (int i = 0; i < threads.length; i++) {
                    threads[i] = new Thread(commonsELTests);
                }

                for (Thread thread : threads) {
                    thread.run();
                }

                for (Thread thread : threads) {
                    thread.join();
                }

                commonsElTotals = commonsELTests.getCommonElTotal();


                nativeJavaTests.reset();

                for (int i = 0; i < threads.length; i++) {
                    threads[i] = new Thread(nativeJavaTests);
                }

                for (Thread thread : threads) {
                    thread.run();
                }

                for (Thread thread : threads) {
                    thread.join();
                }

                javaNativeTotals = nativeJavaTests.getJavaNativeTotal();

                totaltime = System.currentTimeMillis() - totaltime;

                System.out.println(threadNumber + "\t" + ognlTotals + "\t" + mvelTotals + "\t" + commonsElTotals + "\t" + javaNativeTotals);

                //         System.out.println("\nPerformance Comparison Done. OUTPUT TOTALS:");
//                System.out.println("Total Number of Threads: " + threadMax);
//                System.out.println("OGNL Total Runtime (ms): " + ognlTotals);
//                System.out.println("MVEL Total Runtime (ms): " + mvelTotals);
//                System.out.println("Commons EL Total Runtime (ms): " + commonsELTests);

            }
            System.out.println("Done.");

        }
        else {
            omc.setTestFlags(ALL + INTERPRETED + COMPILED);
            omc.run();
        }

    }

    public void run() {
        try {

            for (PerfTest test : tests) {
                runTest(test, TESTNUM);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runTest(PerfTest test, int count) throws Exception {
        int exFlags = test.getRunFlags();
        String expression = test.getExpression();
        String name = test.getName();

        if (!silent) {
            System.out.println("Test Name            : " + test.getName());
            System.out.println("Expression           : " + test.getExpression());
            System.out.println("Iterations           : " + count);
        }

        long time;
        long mem;
        long total = 0;
        long[] res = new long[TESTITER];

        if ((testFlags & INTERPRETED) != 0) {

            if (!silent) System.out.println("Interpreted Results  :");


            if ((testFlags & RUN_OGNL) != 0 && ((exFlags & RUN_OGNL)) != 0) {
                try {
                    // unbenched warm-up
                    for (int i = 0; i < count; i++) {
                        Ognl.getValue(expression, baseClass);
                    }

                    //           System.gc();

                    time = currentTimeMillis();
                    mem = Runtime.getRuntime().freeMemory();

                    for (int reps = 0; reps < TESTITER; reps++) {
                        for (int i = 0; i < count; i++) {
                            Ognl.getValue(expression, baseClass);
                        }

                        if (reps == 0) res[0] = total += currentTimeMillis() - time;
                        else res[reps] = (total * -1) + (total += currentTimeMillis() - time - total);


                    }

                    if (!silent)
                        System.out.println("(OGNL)               : " + new BigDecimal(((currentTimeMillis() - time))).divide(new BigDecimal(TESTITER), 2, RoundingMode.HALF_UP)
                                + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb) " + resultsToString(res));
                }
                catch (Exception e) {
                    if (!silent)
                        System.out.println("(OGNL)               : <<COULD NOT EXECUTE>>");
                }

            }

            synchronized (this) {
                ognlTotal += total;
            }

            total = 0;

            if ((testFlags & RUN_MVEL) != 0 && ((exFlags & RUN_MVEL) != 0)) {
                try {
                    for (int i = 0; i < count; i++) {
                        MVEL.eval(expression, baseClass);
                    }

                    //       System.gc();

                    time = currentTimeMillis();
                    mem = Runtime.getRuntime().freeMemory();
                    for (int reps = 0; reps < TESTITER; reps++) {
                        for (int i = 0; i < count; i++) {
                            MVEL.eval(expression, baseClass);
                        }

                        if (reps == 0) res[0] = total += currentTimeMillis() - time;
                        else res[reps] = (total * -1) + (total += currentTimeMillis() - time - total);
                    }

                    if (!silent)
                        System.out.println("(MVEL)               : " + new BigDecimal(((currentTimeMillis() - time))).divide(new BigDecimal(TESTITER), 2, RoundingMode.HALF_UP)
                                + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb) " + resultsToString(res));

                }
                catch (Exception e) {
                    e.printStackTrace();

                    if (!silent)
                        System.out.println("(MVEL)               : <<COULD NOT EXECUTE>>");
                }
            }

            synchronized (this) {
                mvelTotal += total;
            }

//
//            total = 0;
//
//            if ((testFlags & RUN_GROOVY) != 0 && ((exFlags & RUN_GROOVY) != 0)) {
//                try {
//                    for (int i = 0; i < count; i++) {
//                        Binding binding = new Binding();
//                        for (String var : variables.keySet()) {
//                            binding.setProperty(var, variables.get(var));
//                        }
//
//                        GroovyShell groovyShell = new GroovyShell(binding);
//                        groovyShell.evaluate(expression);
//                    }
//
//
//                    time = currentTimeMillis();
//                    mem = Runtime.getRuntime().freeMemory();
//                    for (int reps = 0; reps < TESTITER; reps++) {
//                        for (int i = 0; i < count; i++) {
//                            Binding binding = new Binding();
//                            for (String var : variables.keySet()) {
//                                binding.setProperty(var, variables.get(var));
//                            }
//
//                            GroovyShell groovyShell = new GroovyShell(binding);
//                            groovyShell.evaluate(expression);
//                        }
//
//                        if (reps == 0) res[0] = total += currentTimeMillis() - time;
//                        else res[reps] = (total * -1) + (total += currentTimeMillis() - time - total);
//                    }
//
//                    if (!silent)
//                        System.out.println("(Groovy)               : " + new BigDecimal(((currentTimeMillis() - time))).divide(new BigDecimal(TESTITER), 2, RoundingMode.HALF_UP)
//                                + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb) " + resultsToString(res));
//
//                }
//                catch (Exception e) {
//                    e.printStackTrace();
//
//                    if (!silent)
//                        System.out.println("(Groovy)               : <<COULD NOT EXECUTE>>");
//                }
//            }
//
//            synchronized (this) {
//                groovyTotal += total;
//            }
//
            total = 0;


            if ((testFlags & RUN_COMMONS_EL) != 0 && ((exFlags & RUN_COMMONS_EL) != 0)) {
                VariableResolver vars = new JSPMapVariableResolver(variables);

                String commonsEx = "${" + expression + "}";

                try {
                    for (int i = 0; i < count; i++) {
                        new ExpressionEvaluatorImpl(true).parseExpression(commonsEx, Object.class, null).evaluate(vars);
                    }

                    //            System.gc();

                    time = currentTimeMillis();
                    mem = Runtime.getRuntime().freeMemory();
                    for (int reps = 0; reps < TESTITER; reps++) {
                        for (int i = 0; i < count; i++) {
                            new ExpressionEvaluatorImpl(true).parseExpression(commonsEx, Object.class, null).evaluate(vars);
                        }

                        if (reps == 0) res[0] = total += currentTimeMillis() - time;
                        else res[reps] = (total * -1) + (total += currentTimeMillis() - time - total);
                    }

                    if (!silent)
                        System.out.println("(CommonsEL)          : " + new BigDecimal(((currentTimeMillis() - time))).divide(new BigDecimal(TESTITER), 2, RoundingMode.HALF_UP)
                                + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb) " + resultsToString(res));

                }
                catch (Exception e) {
                    if (!silent)
                        System.out.println("(CommonsEL)          : <<COULD NOT EXECUTE>>");
                }
            }

            synchronized (this) {
                commonElTotal += total;
            }

        }

        if ((testFlags & COMPILED) != 0) {
            runTestCompiled(name, test.getOgnlCompiled(), test.getMvelCompiled(), test.getGroovyCompiled(), test.getElCompiled(), count, exFlags);
        }

        total = 0;

        if ((testFlags & RUN_JAVA_NATIVE) != 0 && ((exFlags & RUN_JAVA_NATIVE) != 0)) {
            NativeTest nt = test.getJavaNative();

            try {
                for (int i = 0; i < count; i++) {
                    nt.run(baseClass, variables);
                }

                //        System.gc();

                time = currentTimeMillis();
                mem = Runtime.getRuntime().freeMemory();
                for (int reps = 0; reps < TESTITER; reps++) {
                    for (int i = 0; i < count; i++) {
                        nt.run(baseClass, variables);
                    }

                    if (reps == 0) res[0] = total += currentTimeMillis() - time;
                    else res[reps] = (total * -1) + (total += currentTimeMillis() - time - total);
                }

                if (!silent)
                    System.out.println("(JavaNative)         : " + new BigDecimal(((currentTimeMillis() - time))).divide(new BigDecimal(TESTITER), 2, RoundingMode.HALF_UP)
                            + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb) " + resultsToString(res));

            }
            catch (Exception e) {
                if (!silent)
                    System.out.println("(JavaNative)         : <<COULD NOT EXECUTE>>");
            }
        }

        synchronized (this) {
            javaNativeTotal += total;
        }


        if (!silent)
            System.out.println("------------------------------------------------");
    }

    public void runTestCompiled(String name, Object compiledOgnl, Object compiledMvel, Object compiledGroovy, Expression compiledEl, int count, int exFlags) throws Exception {

        long time;
        long mem;
        long total = 0;
        long[] res = new long[TESTITER];


        if (!silent)
            System.out.println("Compiled Results     :");


        if ((testFlags & RUN_OGNL) != 0 && ((exFlags & RUN_OGNL) != 0)) {
            try {
                for (int i = 0; i < count; i++) {
                    Ognl.getValue(compiledOgnl, baseClass);
                }

                time = currentTimeMillis();
                mem = Runtime.getRuntime().freeMemory();

                for (int reps = 0; reps < TESTITER; reps++) {
                    for (int i = 0; i < count; i++) {
                        Ognl.getValue(compiledOgnl, baseClass);
                    }

                    if (reps == 0) res[0] = total += currentTimeMillis() - time;
                    else res[reps] = (total * -1) + (total += currentTimeMillis() - time - total);
                }

                if (!silent)
                    System.out.println("(OGNL Compiled)      : " + new BigDecimal(currentTimeMillis() - time).divide(new BigDecimal(TESTITER), 2, RoundingMode.HALF_UP)
                            + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb) " + resultsToString(res));
            }
            catch (Exception e) {

                if (!silent)
                    System.out.println("(OGNL)               : <<COULD NOT EXECUTE>>");
            }
        }

        synchronized (this) {
            ognlTotal += total;
        }

        total = 0;

        if ((testFlags & RUN_MVEL) != 0 && ((exFlags & RUN_MVEL)) != 0) {

            try {
                for (int i = 0; i < count; i++) {
                    MVEL.executeExpression(compiledMvel, baseClass);
                }

                time = currentTimeMillis();
                mem = Runtime.getRuntime().freeMemory();

                for (int reps = 0; reps < TESTITER; reps++) {
                    for (int i = 0; i < count; i++) {
                        MVEL.executeExpression(compiledMvel, baseClass);
                    }

                    if (reps == 0) res[0] = total += currentTimeMillis() - time;
                    else res[reps] = (total * -1) + (total += currentTimeMillis() - time - total);
                }

                if (!silent)
                    System.out.println("(MVEL Compiled)      : " + new BigDecimal(currentTimeMillis() - time).divide(new BigDecimal(TESTITER), 2, RoundingMode.HALF_UP)
                            + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb) " + resultsToString(res));
            }
            catch (Exception e) {

                if (!silent)
                    System.out.println("(MVEL)               : <<COULD NOT EXECUTE>>");
            }


            synchronized (this) {
                mvelTotal += total;
            }

            total = 0;
        }

//        total = 0;
//
//        if ((testFlags & RUN_GROOVY) != 0 && ((exFlags & RUN_GROOVY)) != 0) {
//
//            try {
//                for (int i = 0; i < count; i++) {
//                    Binding binding = new Binding();
//                    for (String var : variables.keySet()) {
//                        binding.setProperty(var, variables.get(var));
//                    }
//
//                    Script script = (Script) compiledGroovy;
//                    script.setBinding(binding);
//                    script.run();
//                }
//
//                time = currentTimeMillis();
//                mem = Runtime.getRuntime().freeMemory();
//
//                for (int reps = 0; reps < TESTITER; reps++) {
//                    for (int i = 0; i < count; i++) {
//                        Binding binding = new Binding();
//                        for (String var : variables.keySet()) {
//                            binding.setProperty(var, variables.get(var));
//                        }
//
//                        Script script = (Script) compiledGroovy;
//                        script.setBinding(binding);
//                        script.run();
//                    }
//
//                    if (reps == 0) res[0] = total += currentTimeMillis() - time;
//                    else res[reps] = (total * -1) + (total += currentTimeMillis() - time - total);
//                }
//
//                if (!silent)
//                    System.out.println("(Groovy Compiled)      : " + new BigDecimal(currentTimeMillis() - time).divide(new BigDecimal(TESTITER), 2, RoundingMode.HALF_UP)
//                            + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb) " + resultsToString(res));
//            }
//            catch (Exception e) {
//
//                if (!silent)
//                    System.out.println("(Groovy)               : <<COULD NOT EXECUTE>>");
//            }
//
//
//            synchronized (this) {
//                groovyTotal += total;
//            }
//
//            total = 0;
//        }
//

        if ((testFlags & RUN_COMMONS_EL) != 0 && ((exFlags & RUN_COMMONS_EL) != 0)) {
            VariableResolver vars = new JSPMapVariableResolver(variables);
            try {
                for (int i = 0; i < count; i++) {
                    compiledEl.evaluate(vars);
                }

                time = currentTimeMillis();
                mem = Runtime.getRuntime().freeMemory();
                for (int reps = 0; reps < TESTITER; reps++) {
                    for (int i = 0; i < count; i++) {
                        compiledEl.evaluate(vars);
                    }

                    if (reps == 0) res[0] = total += currentTimeMillis() - time;
                    else res[reps] = (total * -1) + (total += currentTimeMillis() - time - total);
                }

                if (!silent)
                    System.out.println("(CommonsEL Compiled) : " + new BigDecimal(((currentTimeMillis() - time))).divide(new BigDecimal(TESTITER), 2, RoundingMode.HALF_UP)
                            + "ms avg.  (mem delta: " + ((Runtime.getRuntime().freeMemory() - mem) / 1024) + "kb) " + resultsToString(res));

            }
            catch (Exception e) {
                if (!silent)
                    System.out.println("(CommonsEL Compiled) : <<COULD NOT EXECUTE>>");
            }
        }

        synchronized (this) {
            commonElTotal += total;
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


    public long getOgnlTotal() {
        return ognlTotal;
    }

    public void setOgnlTotal(long ognlTotal) {
        this.ognlTotal = ognlTotal;
    }

    public long getMvelTotal() {
        return mvelTotal;
    }

    public void setMvelTotal(long mvelTotal) {
        this.mvelTotal = mvelTotal;
    }


    public long getCommonElTotal() {
        return commonElTotal;
    }

    public void setCommonElTotal(long commonElTotal) {
        this.commonElTotal = commonElTotal;
    }


    public long getJavaNativeTotal() {
        return javaNativeTotal;
    }

    public void setJavaNativeTotal(long javaNativeTotal) {
        this.javaNativeTotal = javaNativeTotal;
    }


    public long getGroovyTotal() {
        return groovyTotal;
    }

    public void setGroovyTotal(long groovyTotal) {
        this.groovyTotal = groovyTotal;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public void reset() {
        ognlTotal = 0;
        mvelTotal = 0;
        javaNativeTotal = 0;
        commonElTotal = 0;
    }
}
