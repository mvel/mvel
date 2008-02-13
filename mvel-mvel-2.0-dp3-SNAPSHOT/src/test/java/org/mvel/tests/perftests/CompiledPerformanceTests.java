package org.mvel.tests.perftests;

import junit.framework.TestCase;
import org.mvel.MVEL;
import org.mvel.tests.main.CoreConfidenceTests;
import org.mvel.tests.main.res.Bar;
import org.mvel.tests.main.res.Base;
import org.mvel.tests.main.res.Foo;
import org.mvel.util.ParseTools;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CompiledPerformanceTests extends TestCase {
    private static final int ITERATIONS = 10000;

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

//    public void testToListBenchmark() {
//        String text = "misc.toList(foo.bar.name, 'hello', 42, ['key1' : 'value1', c : [ foo.bar.age, 'car', 42 ]], [42, [c : 'value1']] )";
//
//        MapVariableResolverFactory variableTable = new MapVariableResolverFactory(map);
//
//        Serializable compiled = MVEL.compileExpression(text);
//
//        for (int i = 0; i < ITERATIONS; i++) {
//            executeExpression(compiled, variableTable);
//        }
//
//        assertEquals(ITERATIONS, ((CoreConfidenceTests.MiscTestClass) map.get("misc")).getExec());
//    }
//
//
//    public void testToListBenchmark2() {
//        testToListBenchmark();
//    }
//
//    public void testToListBenchmark3() {
//        testToListBenchmark();
//    }

    public void testQuickSort() throws IOException {
        Serializable s = MVEL.compileExpression(new String(ParseTools.loadFromFile(new File("samples/scripts/quicksort.mvel"))));

        for (int i = 0; i < ITERATIONS; i++) {
            MVEL.executeExpression(s, map);
        }
    }

    public void testQuickSort2() throws IOException {
        testQuickSort();
    }

    public void testQuickSort3() throws IOException {
        testQuickSort();
    }


}
