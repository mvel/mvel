package org.mvel.tests.perftests;

import org.mvel.MVEL;
import org.mvel.util.FastList;

import java.io.Serializable;


public class InlineCollectionsPerformance {
    private static final int COUNT = 1000000;


    public static void main(String[] args) {
        long time;
        for (int i = 0; i < 4; i++) {
            time = System.currentTimeMillis();
            testJavaList();
            System.out.println("Java:" + (System.currentTimeMillis() - time));

            time = System.currentTimeMillis();
            testJavaList();
            System.out.println("MVEL: " + (System.currentTimeMillis() - time));
        }

    }

    public static void testMVELList() {
        Serializable s = MVEL.compileExpression("['Foo', 'Bar','Foo', 'Bar','Foo', 'Bar','Foo', 'Bar','Foo', 'Bar']");
        for (int i = 0; i < COUNT; i++) {
            MVEL.executeExpression(s);
        }
    }


    public static void testJavaList() {
        for (int i = 0; i < COUNT; i++) {
            FastList list = new FastList(10);

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
        }

    }
}
