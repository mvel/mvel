package org.mvel.tests;

import java.util.HashMap;

import org.mvel.MVEL;

public class DroolsTest {
    public void test1() throws Exception {    
        MVEL.executeExpression( MVEL.compileExpression( "new Integer(5)" ),
                                null, new HashMap() );  
    }
}
