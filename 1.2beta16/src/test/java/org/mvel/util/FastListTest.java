package org.mvel.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mvel.MVEL;


import junit.framework.TestCase;

public class FastListTest extends TestCase {
    protected Map<String, Object> map = new HashMap<String, Object>();


    public FastListTest() {
        map.put("var", "var");
    }
    
    public void testHashCode() {
        List list = (List) parseDirect( "[ 'key1', var ]" );
        System.out.println( list.hashCode() );
    }
    
    
    public void testEquals() {
        List list1 = (List) parseDirect( "[ 'key1', var ]" );
        List list2 = new ArrayList();
        list2.add( "key1" );
        list2.add( "var" );
        assertEquals( list2, list1);
        assertEquals( list1, list2);        
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
