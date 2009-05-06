package org.mvel2;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class MacroProcessorTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testParseString() {
        Map<String, Macro> macros = new HashMap<String, Macro>();
        macros.put( "insert",
                    new Macro() {
                        public String doMacro() {
                            return "drools.insert";
                        }
                    } ); 
        
        String raw = "    l.add( \"rule 2 executed \" + str);\n";
        
        try { 
            MacroProcessor macroProcessor = new MacroProcessor();
            macroProcessor.setMacros( macros );
            String result = macroProcessor.parse( raw );
            assertEquals( raw, result );
        } catch( Exception ex ) {
            ex.printStackTrace();
            fail( "there shouldn't be any exception: "+ex.getMessage());
        }
    }

}
