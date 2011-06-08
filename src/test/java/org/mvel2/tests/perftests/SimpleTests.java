package org.mvel2.tests.perftests;

import org.mvel2.MVEL;
import org.mvel2.optimizers.dynamic.DynamicOptimizer;
import org.mvel2.util.ParseTools;
import org.mvel2.util.QuickSort;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;

import static java.lang.Runtime.getRuntime;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class SimpleTests {
  private static final double COUNT = 30000;

  public static void main(String[] args) throws IOException {
    PrintStream ps = System.out;

    try {
      for (int i = 0; i < 10000; i++) {
        testQuickSortMVEL(ps);
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

    Map vars = new HashMap();

    DecimalFormat dc = new DecimalFormat("#.##");

    for (int a = 0; a < 10000; a++) {
      vars.clear();
      MVEL.executeExpression(c, vars);
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


}
