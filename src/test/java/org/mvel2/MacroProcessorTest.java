package org.mvel2;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class MacroProcessorTest extends TestCase {

    private MacroProcessor macroProcessor;

    protected void setUp() throws Exception {
        super.setUp();
        Map<String, Macro> macros = new HashMap<String, Macro>();
        macros.put( "insert",
                    new Macro() {
                        public String doMacro() {
                            return "drools.insert";
                        }
                    } ); 
        macroProcessor = new MacroProcessor();
        macroProcessor.setMacros( macros );
    }

    public void testParseString() {
        String raw = "    l.add( \"rule 2 executed \" + str);";
        try { 
            String result = macroProcessor.parse( raw );
            assertEquals( raw, result );
        } catch( Exception ex ) {
            ex.printStackTrace();
            fail( "there shouldn't be any exception: "+ex.getMessage());
        }
    }

    public void testParseConsequenceWithComments() {
        String raw = "    // str is null, we are just testing we don't get a null pointer \n "+
                     "    list.add( p ); ";
        try { 
            String result = macroProcessor.parse( raw );
            assertEquals( raw, result );
        } catch( Exception ex ) {
            ex.printStackTrace();
            fail( "there shouldn't be any exception: "+ex.getMessage());
        }
    }

    

}
