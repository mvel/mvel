package org.mvel2.tests.core;

import org.mvel2.MVEL;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeExpression;
import static org.mvel2.util.ParseTools.containsCheck;
import static org.mvel2.util.ParseTools.loadFromFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SamplesTests extends AbstractTest {

  public void testFunctionsScript1() throws IOException {
    MVEL.evalFile(new File("samples/scripts/functions1.mvel"));
  }

  public void testQuickSortScript1() throws IOException {
    MVEL.evalFile(new File("samples/scripts/quicksort.mvel"));
  }

  public void testQuickSortScriptFunctional() throws IOException {
    MVEL.evalFile(new File("samples/scripts/fquicksort.mvel"));
  }


  public void testQuickSortScript2() throws IOException {
    Object[] sorted = (Object[]) test(new String(loadFromFile(new File("samples/scripts/quicksort.mvel"))));
    int last = -1;
    for (Object o : sorted) {
      if (last == -1) {
        last = (Integer) o;
      }
      else {
        assertTrue(((Integer) o) > last);
        last = (Integer) o;
      }
    }
  }

  public void testQuickSortScript3() throws IOException {
    Object[] sorted = (Object[]) MVEL.eval(new String(loadFromFile(new File("samples/scripts/quicksort2.mvel"))), new HashMap());
    int last = -1;
    for (Object o : sorted) {
      if (last == -1) {
        last = (Integer) o;
      }
      else {
        assertTrue(((Integer) o) > last);
        last = (Integer) o;
      }
    }
  }

  public void testQuickSortScript4() throws IOException {
    Object[] sorted = (Object[]) MVEL.eval(new String(loadFromFile(new File("samples/scripts/quicksort3.mvel"))), new HashMap());
    int last = -1;
    for (Object o : sorted) {
      if (last == -1) {
        last = (Integer) o;
      }
      else {
        assertTrue(((Integer) o) > last);
        last = (Integer) o;
      }
    }
  }

  public void testQuickSortScript5() throws IOException {
    List sorted = (List) executeExpression(compileExpression(loadFromFile(new File("samples/scripts/fquicksort.mvel"))), new HashMap());
    int last = -1;
    for (Object o : sorted) {
      if (last == -1) {
        last = (Integer) o;
      }
      else {
        assertTrue(((Integer) o) > last);
        last = (Integer) o;
      }
    }
  }

  public void testMultiLineString() throws IOException {
    MVEL.evalFile(new File("samples/scripts/multilinestring.mvel"));
  }
}
