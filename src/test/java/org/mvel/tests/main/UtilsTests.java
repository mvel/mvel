package org.mvel.tests.main;

import junit.framework.TestCase;
import org.mvel.util.FastMap;
import org.mvel.util.StringAppender;

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
}
