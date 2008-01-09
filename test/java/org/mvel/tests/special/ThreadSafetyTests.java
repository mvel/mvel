package org.mvel.tests.special;

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.mvel.TemplateInterpreter;


public class ThreadSafetyTests extends TestCase {

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

        System.out.println(TemplateInterpreter.eval("@foreach{x as item1, y as item2}X:@{item1};Y:@{item2}@end{}", m));
        
        


    }
}
