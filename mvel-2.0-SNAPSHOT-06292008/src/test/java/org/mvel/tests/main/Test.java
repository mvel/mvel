package org.mvel.tests.main;

import org.mvel.MVEL;
import org.mvel.tests.main.res.Foo;

import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;


public class Test {
    public static void main(String[] args) {
        Foo foo = new Foo();

        Serializable s = MVEL.compileExpression("x + y");

        Map map = new HashMap();
        map.put("x", 10);
        map.put("y", 5);

        Object o = MVEL.executeExpression(s, map);

        System.out.println(o);
    }
}
