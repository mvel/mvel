package org.mvel.tests;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.mvel.MVEL;

public class DroolsTest extends TestCase {
    public void test1() throws Exception {    
        MVEL.executeExpression( MVEL.compileExpression( "new Integer(5)" ),
                                null, new HashMap() );  
    }
    
    
    public void test2() throws Exception {    
        String str = (String) MVEL.executeExpression( MVEL.compileExpression( "\"hello2\"" ),
                                null,
                                new HashMap());
        System.out.println( str );
    }    
    
    public void test3() throws Exception {    
        String str = (String) MVEL.executeExpression( MVEL.compileExpression( "new String(\"hello2\")" ),
                                null,
                                new HashMap());
        System.out.println( str );
    }        
}
