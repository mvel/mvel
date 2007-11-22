package org.mvel.tests.perftests;

import org.mvel.ExecutableAccessor;
import org.mvel.MVEL;
import org.mvel.integration.impl.MapVariableResolverFactory;
import org.mvel.util.FastList;

import java.util.HashMap;
import java.util.Map;


public class InlineCollectionsPerformance {
    private static final int COUNT = 200000;


    public static void main(String[] args) {
        long time;
        for (int i = 0; i < 5; i++) {
//            time = System.currentTimeMillis();
//            testJavaList();
//            System.out.println("Java    : " + (System.currentTimeMillis() - time));
//

            time = System.currentTimeMillis();
            testMVELList();
            System.out.println("MVEL    : " + (System.currentTimeMillis() - time));
            System.out.println();
        }

    }

    public static void testMVELList() {
        Map vals = new HashMap();
        vals.put("a", "BARFOO");

        MapVariableResolverFactory mvr = new MapVariableResolverFactory(vals);
        ExecutableAccessor s = (ExecutableAccessor) MVEL.compileExpression("['Foo':'Bar',a:'Bar','Foo1':'Bar','Foo2':'Bar','Foo3':'Bar']");
        Map map;
        s.getNode().getReducedValueAccelerated(null, null, mvr);
        for (int i = 0; i < COUNT; i++) {
            map = (Map) s.getNode().getAccessor().getValue(null, null, mvr);

            assert "Bar".equals(map.get("BARFOO")) && map.size() == 5;
        }
    }

    public static void testJavaList() {
        FastList list;
        for (int i = 0; i < COUNT; i++) {
            list = new FastList(10);

            list.add("Foo");
            list.add("Bar");

            list.add("Foo");
            list.add("Bar");

            list.add("Foo");
            list.add("Bar");

            list.add("Foo");
            list.add("Bar");

            list.add("Foo");
            list.add("Bar");

            assert "Foo".equals(list.get(0)) && "Bar".equals(list.get(1)) && list.size() == 10;
        }

    }
}
