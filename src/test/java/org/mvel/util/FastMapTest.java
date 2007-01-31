package org.mvel.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.mvel.MVEL;


import junit.framework.TestCase;

public class FastMapTest extends TestCase {
    protected Map<String, Object> map = new HashMap<String, Object>();


    public FastMapTest() {
        map.put("var", "var");
    }
    
    public void testHashCode() {
        Map map = (Map) parseDirect( "[ 'key1' : var ]" );
        System.out.println( map.hashCode() );
    }
    
    public Object parseDirect(String ex) {
        return compiledExecute(ex);
    }

    public Object compiledExecute(String ex) {
        Serializable compiled = MVEL.compileExpression(ex);
        Object first = MVEL.executeExpression(compiled, null, map);
        Object second = MVEL.executeExpression(compiled, null, map);


        if (first != null && !first.getClass().isArray())
            assertEquals(first, second);

        return second;
    }    
}
