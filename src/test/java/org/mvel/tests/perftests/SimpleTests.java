package org.mvel.tests.perftests;

import org.mvel.MVEL;
import org.mvel.integration.impl.DefaultLocalVariableResolverFactory;
import org.mvel.util.ParseTools;
import org.mvel.util.QuickSort;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Field;

public class SimpleTests {
    private static final double COUNT = 10000;

    public static void main(String[] args) throws IOException {
        PrintStream ps = System.out;

        System.setOut(new PrintStream(new NullOutputStream()));
        try {
            for (int i = 0; i < 10; i++) {
                testQuickSortMVEL(ps);
                testQuickSortNative(ps);
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }

        System.setOut(ps);
    }


    private static void testQuickSortMVEL(PrintStream ps) throws IOException {
        double time;

        time = System.currentTimeMillis();

        char[] sourceFile = ParseTools.loadFromFile(new File("samples/scripts/quicksort.mvel"));
        Serializable c = MVEL.compileExpression(sourceFile);

        DefaultLocalVariableResolverFactory vrf = new DefaultLocalVariableResolverFactory();

        for (int i = 0; i < COUNT; i++) {
            MVEL.executeExpression(c, vrf);
        }

        ps.println("Result: " + (time = System.currentTimeMillis() - time));
        ps.println("Rate  : " + (COUNT / (time / 1000)) + " per second.");
    }

    private static void testQuickSortNative(PrintStream ps) {
        double time;

        time = System.currentTimeMillis();

        for (int i = 0; i < COUNT; i++) {
            QuickSort.quickSort(new int[]{50, 20, 21, 209, 10, 77, 8, 9, 55, 73, 41, 99});
        }

        ps.println("Result: " + (time = System.currentTimeMillis() - time));
        ps.println("Rate  : " + (COUNT / (time / 1000)) + " per second.");
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
