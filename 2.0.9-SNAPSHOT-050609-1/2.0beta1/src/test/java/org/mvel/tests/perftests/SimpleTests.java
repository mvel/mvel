package org.mvel.tests.perftests;

import org.mvel.MVEL;
import org.mvel.optimizers.dynamic.DynamicOptimizer;
import org.mvel.integration.impl.DefaultLocalVariableResolverFactory;
import org.mvel.util.ParseTools;
import org.mvel.util.QuickSort;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import static java.lang.Runtime.getRuntime;
import java.text.DecimalFormat;

public class SimpleTests {
    private static final double COUNT = 30000;

    public static void main(String[] args) throws IOException {
        PrintStream ps = System.out;

        //  System.setOut(new PrintStream(new NullOutputStream()));
        try {
            for (int i = 0; i < 10000; i++) {
                testQuickSortMVEL(ps);
                //        testQuickSortNative(ps);
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }

        System.setOut(ps);
    }

    private static int tg = 0;

    private static void testQuickSortMVEL(PrintStream ps) throws IOException {
        double time;

        time = System.currentTimeMillis();

        char[] sourceFile = ParseTools.loadFromFile(new File("samples/scripts/quicksort.mvel"));
        Serializable c = MVEL.compileExpression(sourceFile);

        DefaultLocalVariableResolverFactory vrf = new DefaultLocalVariableResolverFactory();

        DecimalFormat dc = new DecimalFormat("#.##");

        for (int a = 0; a < 10000; a++) {
                MVEL.executeExpression(c, vrf);
        }

        ps.println("Result: " + (time = System.currentTimeMillis() - time));
        ps.println("Rate  : " + (COUNT / (time / 1000)) + " per second.");
        ps.println("FreeMem: " + dc.format((double) getRuntime().freeMemory() / (1024d * 1024d)) + "MB / TotalMem: " + dc.format((double) getRuntime().totalMemory() / (1024d * 1024d)) + "MB");
        ps.println("TotalGarbaged: " + DynamicOptimizer.totalRecycled);
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
