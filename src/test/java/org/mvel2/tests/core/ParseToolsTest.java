package org.mvel2.tests.core;

import org.mvel2.util.ParseTools;

public class ParseToolsTest extends AbstractTest{
    public void testShouldIdentifyStringEscape() {
        assertEquals("Should return the correct terminate index of the String literal!",
            5, ParseTools.balancedCapture(new char[]{'"', 'a', '\\', '"', 'b', '"'}, 0, 6, '"'));
    }
}
