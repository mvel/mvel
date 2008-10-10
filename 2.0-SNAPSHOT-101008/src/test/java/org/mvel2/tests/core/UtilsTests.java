package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.util.FastList;
import org.mvel2.util.StringAppender;

public class UtilsTests extends TestCase {

    public void testMain() {
        assertEquals("foobarfoobar", new StringAppender().append("foo").append('b').append('a').append('r').append("foobar").toString());
    }

    public void testMain2() {
        assertEquals("foo bar test 1 2 3foo bar test 1 2 3",
                new StringAppender(0).append("foo bar ").append("test").append(" 1 2 3")
                        .append("foo bar").append(" ").append("test").append(" 1 2 3").toString());
    }

    public void testMain3() {
        assertEquals("C:/projects/webcat/exploded/resources/productimages/",
                new StringAppender(10).append("C:/projects/webcat/exploded/")
                        .append("resources/productimages/").toString());
    }

    public void testFastList1() {
        FastList list = new FastList(3);
        list.add("One");
        list.add("Two");
        list.add("Three");
        list.add("Five");

        list.add(1, "Four");

        String[] zz1 = {"One", "Four", "Two", "Three", "Five"};
        int i = 0;
        for (Object o : list) {
            if (!zz1[i++].equals(o)) throw new AssertionError("problem with list!");
        }

        list.remove(2);

        String[] zz2 = {"One", "Four", "Three", "Five"};
        i = 0;
        for (Object o : list) {
            if (!zz2[i++].equals(o)) throw new AssertionError("problem with list!");
        }
    }

//    public void testMainPerf() {
//        for (int i = 0; i < 1000000; i++) {
//            testMain();
//        }
//    }
//
//    public void testStringBuilderPerf() {
//        for (int i = 0; i < 1000000; i++) {
//            testStringBuilder();
//        }
//    }
//
//    public void testMainPerf2() {
//        for (int i = 0; i < 1000000; i++) {
//            testMain();
//        }
//    }
//
//    public void testStringBuilderPerf2() {
//        for (int i = 0; i < 1000000; i++) {
//            testStringBuilder();
//        }
//    }


    public static void main(String[] args) throws Exception {
        Class.forName("[Ljava.lang.String;");
    }
}
