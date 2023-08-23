package org.mvel2.tests.core;

import org.junit.Test;
import org.mvel2.MVEL;
import org.mvel2.tests.BaseMvelTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class StringConcatWithHashTest extends BaseMvelTest {

    @Test
    public void testConcatWithHash() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("number1", Integer.valueOf(0));
        props.put("number2", Integer.valueOf(1));
        props.put("foo", "bar");
        Map<String, Object> vars = new HashMap<>();
        vars.put("props", props);

        MVEL.eval("props['res'] = props['number1'] # props['number2']", vars);
        assertEquals("01", props.get("res"));

        MVEL.eval("props['res'] = props['number1'] # props['foo']", vars);
        assertEquals("0bar", props.get("res"));

        MVEL.eval("props['res'] = 'bar' # props['foo']", vars);
        assertEquals("barbar", props.get("res"));

    }

}
