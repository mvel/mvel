package org.mvel.tests.perftests;

import org.mvel.MVEL;
import org.mvel.util.ParseTools;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;

public class SimpleTests {

    public static void main(String[] args) throws IOException {
        //   Unsafe unsafe = getUnsafe();

        //   TestSuite testSuite = new TestSuite(CoreConfidenceTests.class);
        //    TestResult result = new TestResult();

        PrintStream ps = System.out;

        System.setOut(new PrintStream(new NullOutputStream()));
        System.setErr(new PrintStream(new NullOutputStream()));

        //  testSuite.run(result);
        long time;

        time = System.currentTimeMillis();

        char[] sourceFile = ParseTools.loadFromFile(new File("samples/scripts/quicksort.mvel"));
        for (int i = 0; i < 10000; i++) {
//            time = System.currentTimeMillis();
            //  testSuite.run(result);
            MVEL.compileExpression(sourceFile);
//            ps.println("Result: " + (System.currentTimeMillis() - time));
        }
        ps.println("Result: " + (System.currentTimeMillis() - time));


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


}
