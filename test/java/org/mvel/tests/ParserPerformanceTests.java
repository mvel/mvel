package org.mvel.tests;

import org.mvel.ExpressionParser;
import org.mvel.Interpreter;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

import ognl.Ognl;
import ognl.OgnlException;

public class ParserPerformanceTests extends TestCase {
    private static final int COUNT = 10000;

    Foo foo = new Foo();
    Map<String, Object> map = new HashMap<String, Object>(100);
    Base base = new Base();

    public ParserPerformanceTests() {
        foo.setBar(new Bar());
        map.put("foo", foo);
        map.put("a", null);
        map.put("b", null);
        map.put("c", "cat");
        map.put("BWAH", "");
        map.put("bar2", new Base());

        map.put("pi", "3.14");
        map.put("hour", "60");

        Interpreter.setCacheAggressively(true);
    }

    

    public void testSimplePropertyAccessPerf() {
        for (int i = 0; i < COUNT; i++) {
            parseDirect("((hour + 10 - 1) == 69) && c == 'cat'");
        }
    }

    public void testSimplePropertyAccessPerf2() {
        testSimplePropertyAccessPerf();
    }

    public void testDeepPropertyAccessPerf() {
        for (int i = 0; i < COUNT; i++) {
            parseDirect("foo.bar.name");
        }
    }

    public void testDeepPropertyAccessPerf2() {
        testDeepPropertyAccessPerf();
    }

    public void testSimplePropertyAccessPerfDirect() {
        for (int i = 0; i < COUNT; i++) {
            parseDirect("hour != null");
        }
    }

    public void testPreCompiled() {
        Object ex = ExpressionParser.compileExpression("hour != null");
        ExpressionParser ep = new ExpressionParser(null, map);

        for (int i = 0; i < COUNT; i++) {
            ep.setPrecompiledExpression(ex).parse();
        }
    }


    public void testPreCompiled2() {
        testPreCompiled();
    }

    public void testPreCompiled3() {
        testPreCompiled();
    }

    public void testPreCompiledB() {
        Object ex = ExpressionParser.compileExpression("((hour + 10 - 1) == 69) && c == 'cat'");
        for (int i = 0; i < COUNT; i++) {
            ExpressionParser.executeExpression(ex, null, map);
        }

        ExpressionParser.executeExpression(ex, null, map);
    }

    public void testPreCompiledB2() {
        testPreCompiledB();
    }


    public void testPreCompiledB3() {
        testPreCompiledB();
    }


    public void testPreCompiledC() {
        Object ex = ExpressionParser.compileExpression("hour");
        for (int i = 0; i < COUNT; i++) {
            assert "60".equals(ExpressionParser.executeExpression(ex, null, map));
        }

        ExpressionParser.executeExpression(ex, null, map);
    }

    public void testSimplePropertyAccessPerfDirect2() {
        testSimplePropertyAccessPerfDirect();
    }

    public void testLogicPerformance() {
        for (int i = 0; i < COUNT; i++) {
            parseDirect("a != null && pi == 3.14");
        }
    }

    public void testRegularExpression() {
        for (int i = 0; i < COUNT; i++) {
            parse("@{c ~= '[a-z].+'}");
        }
    }

    public void testOgnl() throws OgnlException {
        Object o = Ognl.parseExpression("hour");
        for (int i = 0; i < COUNT; i++) {
            assert "60".equals(Ognl.getValue(o, map));
        }
    }

    public void testOgnl2() throws OgnlException {
        testOgnl();
    }

    public void testOgnl3() throws OgnlException {
        testOgnl();
    }


    public void testObjectCreation() {
        for (int i = 0; i < COUNT; i++) {
            parse("@{new String('hello')}");
        }
    }


    public Object parse(String ex) {
        return new Interpreter(ex).execute(base, map);
    }

    public Object parseDirect(String ex) {
        return ExpressionParser.eval(ex, base, map);
    }

}
