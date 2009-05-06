package org.mvel.tests.perftests;

import junit.framework.TestCase;
import org.mvel.MVEL;
import org.mvel.TemplateInterpreter;
import org.mvel.tests.main.res.Bar;
import org.mvel.tests.main.res.Base;
import org.mvel.tests.main.res.Foo;

import java.util.HashMap;
import java.util.Map;

public class ParserPerformanceTests {
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


    public void testPreCompiledB() {
        Object ex = MVEL.compileExpression("((hour + 10 - 1) == 69) && c == 'cat'");
        for (int i = 0; i < COUNT; i++) {
            MVEL.executeExpression(ex, null, map);
        }

        MVEL.executeExpression(ex, null, map);
    }

    public void testPreCompiledB2() {
        testPreCompiledB();
    }


    public void testPreCompiledB3() {
        testPreCompiledB();
    }


    public void testPreCompiledC() {
        Object ex = MVEL.compileExpression("hour");
        for (int i = 0; i < COUNT; i++) {
            assert "60".equals(MVEL.executeExpression(ex, null, map));
        }

        MVEL.executeExpression(ex, null, map);
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

//    public void testOgnl() throws OgnlException {
////        Object o = Ognl.parseExpression("hour");
////        for (int i = 0; i < COUNT; i++) {
////            assert "60".equals(Ognl.getValue(o, map));
////        }
//    }

//    public void testOgnl2() throws OgnlException {
//        testOgnl();
//    }
//
//    public void testOgnl3() throws OgnlException {
//        testOgnl();
//    }


    public void testObjectCreation() {
        for (int i = 0; i < COUNT; i++) {
            parse("@{new String('hello')}");
        }
    }


    public Object parse(String ex) {
        return new TemplateInterpreter(ex).execute(base, map);
    }

    public Object parseDirect(String ex) {
        return MVEL.eval(ex, base, map);
    }

}
