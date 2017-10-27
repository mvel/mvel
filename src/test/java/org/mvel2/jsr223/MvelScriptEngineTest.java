package org.mvel2.jsr223;

import org.junit.Test;
import org.mvel2.MVEL;

import javax.script.*;

import static org.junit.Assert.*;

/**
 * Created by rahult on 4/1/17.
 */
public class MvelScriptEngineTest {

    @Test
    public void testScriptEngine() throws ScriptException {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("mvel");

        SimpleBindings simpleBindings = new SimpleBindings();
        simpleBindings.put("a", 1);
        simpleBindings.put("b", 2);

        int c = (Integer) scriptEngine.eval("a + b", simpleBindings);
        assertEquals(c, 3);

    }

    @Test
    public void testScriptEngineCompiledScript() throws ScriptException {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("mvel");

        SimpleBindings simpleBindings = new SimpleBindings();
        simpleBindings.put("a", 1);
        simpleBindings.put("b", 2);

        if (scriptEngine instanceof Compilable) {
            Compilable compilableScriptEngine = (Compilable) scriptEngine;
            CompiledScript compiledScript = compilableScriptEngine.compile("a+ b");
            int c = (Integer) compiledScript.eval(simpleBindings);
            assertEquals(c, 3);
        }

    }

}