package org.mvel2.jsr223;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MvelScriptEngineTest {

    @Test
    public void testScriptEngine() throws ScriptException {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        scriptEngineManager.registerEngineName( "mvel", new MvelScriptEngineFactory());
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
        scriptEngineManager.registerEngineName( "mvel", new MvelScriptEngineFactory());
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("mvel");

        SimpleBindings simpleBindings = new SimpleBindings();
        simpleBindings.put("a", 1);
        simpleBindings.put("b", 2);

        Compilable compilableScriptEngine = (Compilable) scriptEngine;
        CompiledScript compiledScript = compilableScriptEngine.compile("a+ b");
        int c = (Integer) compiledScript.eval(simpleBindings);
        assertEquals(c, 3);
    }
}