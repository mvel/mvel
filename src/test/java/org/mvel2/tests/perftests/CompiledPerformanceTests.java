package org.mvel2.tests.perftests;

import org.mvel2.MVEL;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.tests.core.CoreConfidenceTests;
import org.mvel2.tests.core.res.Bar;
import org.mvel2.tests.core.res.Base;
import org.mvel2.tests.core.res.Foo;
import org.mvel2.util.ParseTools;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class CompiledPerformanceTests {
  protected Foo foo = new Foo();
  protected Map<String, Object> map = new HashMap<String, Object>();
  protected Base base = new Base();

  public CompiledPerformanceTests() {
    foo.setBar(new Bar());
    map.put("foo", foo);
    map.put("a", null);
    map.put("b", null);
    map.put("c", "cat");
    map.put("BWAH", "");

    map.put("misc", new CoreConfidenceTests.MiscTestClass());

    map.put("pi", "3.14");
    map.put("hour", "60");
    map.put("zero", 0);
  }


  public void testQuickSort() throws IOException {
    Serializable s = MVEL.compileExpression(new String(ParseTools.loadFromFile(new File("samples/scripts/fquicksort.mvel"))));

    HashMap map = new HashMap();
    MapVariableResolverFactory mvrf = new MapVariableResolverFactory(map);

    for (int i = 0; i < 1000000; i++) {
      MVEL.executeExpression(s, mvrf);
      mvrf.clear();
    }

//        for (int x = 0; x < 4; x++) {
//            Serializable s = MVEL.compileSetExpression("tak.bar.name");
//            long time = System.currentTimeMillis();
//
//            for (int i = 0; i < ITERATIONS; i++) {
//                MVEL.executeSetExpression(s, map, "foobie");
//            }
//
//            System.out.println("SET PERFORMANCE: " + (System.currentTimeMillis() - time));
//
//            time = System.currentTimeMillis();
//
//            s = MVEL.compileExpression("tak.bar.name");
//
//            for (int i = 0; i < ITERATIONS; i++) {
//                MVEL.executeExpression(s, map);
//            }
//
//            System.out.println("GET PERFORMANCE: " + (System.currentTimeMillis() - time));
//
//        }
  }

  public void testQuickSort2() throws IOException {
    testQuickSort();
  }

  public void testQuickSort3() throws IOException {
    testQuickSort();
  }


}
