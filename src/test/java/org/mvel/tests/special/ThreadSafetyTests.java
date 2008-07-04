package org.mvel.tests.special;

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.mvel.TemplateInterpreter;


public class ThreadSafetyTests extends TestCase {
    private static final int TOTAL_THREADS = 5;
    private static final int RUNCOUNT = 1000;

    private Thread[] threads;

    public void testMultiIteratorMultiThread() {
        threads = new Thread[TOTAL_THREADS];
        for (int i = 0; i < TOTAL_THREADS; i++) {
            threads[i] = new Thread(new TestRunner(this, i));
        }
        for (int i = 0; i < TOTAL_THREADS; i++) {
            threads[i].setPriority(Thread.MIN_PRIORITY);
            threads[i].start();
        }

        while (areThreadsActive()) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                return;
            }
        }

        System.out.println("Done.");

    }

    private boolean areThreadsActive() {
        for (int i = 0; i < TOTAL_THREADS; i++) {
            if (threads[i].isAlive()) return true;
        }
        return false;
    }

    public static class TestRunner implements Runnable {
        private ThreadSafetyTests tst;
        private int threadNumber;


        public TestRunner(ThreadSafetyTests tst, int threadNumber) {
            this.tst = tst;
            this.threadNumber = threadNumber;
        }

        public void run() {
            System.out.println("Thread " + threadNumber + " started.");

            for (int i = 0; i < RUNCOUNT; i++) {
                tst.testMultiIterator();
            }


            System.out.println("Thread " + threadNumber + " ended.");
        }


    }

    public void testMultiIterator() {
        Map m = new HashMap();

        List x = new LinkedList();
        x.add("foo");
        x.add("bar");

        List y = new LinkedList();
        y.add("FOO");
        y.add("BAR");

        m.put("x", x);
        m.put("y", y);

        TemplateInterpreter.eval("@foreach{x as item1, y as item2}X:@{item1};Y:@{item2}@end{}", m);

    }
}
