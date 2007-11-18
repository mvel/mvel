package org.mvel.tests.perftests;

import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.mvel.tests.main.CoreConfidenceTests;
import sun.misc.Unsafe;

import java.io.PrintStream;
import java.lang.reflect.Field;

public class SimpleTests {

    public static void main(String[] args) {
        //   Unsafe unsafe = getUnsafe();


        TestSuite testSuite = new TestSuite(CoreConfidenceTests.class);
        TestResult result = new TestResult();

        PrintStream ps = System.out;

        System.setOut(new PrintStream(new NullOutputStream()));
        System.setErr(new PrintStream(new NullOutputStream()));

        testSuite.run(result);
        long time;

        for (int i = 0; i < 100; i++) {
            time = System.currentTimeMillis();
            testSuite.run(result);
            ps.println("Result: " + (System.currentTimeMillis() - time));
        }

        System.setOut(ps);
    }

    private static Unsafe getUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        }
        catch (Exception ex) {
            throw new RuntimeException("can't get Unsafe instance", ex);
        }
    }

    private static final Unsafe unsafe__ = getUnsafe();

    private static void setBoolean(Field f, Object o, boolean v) {
        unsafe__.putBoolean(o, unsafe__.objectFieldOffset(f), v);
    }

    private static Object newInstance(Class clazz) throws Exception {
        return (unsafe__.allocateInstance(clazz));
    }

}
