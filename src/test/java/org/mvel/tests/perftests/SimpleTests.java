package org.mvel.tests.perftests;

import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.mvel.tests.main.CoreConfidenceTests;

import java.io.PrintStream;

public class SimpleTests {

    public static void main(String[] args) {
//        Foo foo = new Foo();
//        Map map = new HashMap();
//
//
//
//
//        map.put("foo", foo);
//
//        Serializable s = MVEL.compileExpression("foo.name == 'dog'");
//
//        long time = System.currentTimeMillis();
//
//        int i = 0;
//        for (; i < 100000000; i++) {
//            MVEL.executeExpression(s, map);
//        }
//        System.out.println(i);
//
//        System.out.println("time: " + (System.currentTimeMillis() - time));


        TestSuite testSuite = new TestSuite(CoreConfidenceTests.class);
        TestResult result = new TestResult();

        PrintStream ps = System.out;

        System.setOut(new PrintStream(new NullOutputStream()));
        System.setErr(new PrintStream(new NullOutputStream()));

        testSuite.run(result);
        long time;

        for (int i = 0; i < 100; i++) {
            time = System.currentTimeMillis();
            testSuite.run(result);
            ps.println("Result: " + (System.currentTimeMillis() - time));
        }

        System.setOut(ps);


    }

}
