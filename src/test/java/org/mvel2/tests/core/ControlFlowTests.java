package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.tests.core.res.Base;
import org.mvel2.tests.core.res.Foo;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.Serializable;


public class ControlFlowTests extends AbstractTest {

    public void testSimpleIfStatement() {
        test("if (true) { System.out.println(\"test!\") }  \n");
    }

    public void testAnd() {
        assertEquals(true, test("c != null && foo.bar.name == 'dog' && foo.bar.woof"));
    }

    public void testAnd2() {
        assertEquals(true, test("c!=null&&foo.bar.name=='dog'&&foo.bar.woof"));
    }

    public void testComplexAnd() {
        assertEquals(true, test("(pi * hour) > 0 && foo.happy() == 'happyBar'"));
    }

    public void testShortPathExpression() {
        assertEquals(null, MVEL.eval("3 > 4 && foo.toUC('test'); foo.register", new Base(), createTestMap()));
    }

    public void testShortPathExpression2() {
        assertEquals(true, test("4 > 3 || foo.toUC('test')"));
    }

    public void testShortPathExpression4() {
        assertEquals(true, test("4>3||foo.toUC('test')"));
    }

    public void testOr() {
        assertEquals(true, test("fun || true"));
    }

    public void testOrOperator() {
        assertEquals(true, test("true||true"));
    }

    public void testOrOperator2() {
        assertEquals(true, test("2 > 3 || 3 > 2"));
    }

    public void testOrOperator3() {
        assertEquals(true, test("pi > 5 || pi > 6 || pi > 3"));
    }

    public void testShortPathExpression3() {
        assertEquals(false, test("defnull != null  && defnull.length() > 0"));
    }

    public void testMultiStatement() {
        assertEquals(true, test("populate(); barfoo == 'sarah'"));
    }

    public void testTernary() {
        assertEquals("foobie", test("zero==0?'foobie':zero"));
    }

    public void testTernary2() {
        assertEquals("blimpie", test("zero==1?'foobie':'blimpie'"));
    }

    public void testTernary3() {
        assertEquals("foobiebarbie", test("zero==1?'foobie':'foobie'+'barbie'"));
    }

    public void testTernary5() {
        assertEquals("skat!", test("isdef someWierdVar ? 'squid' : 'skat!';"));
    }

    public void testEmptyIf() {
        assertEquals(5, test("a = 5; if (a == 5) { }; return a;"));
    }

    public void testEmptyIf2() {
        assertEquals(5, test("a=5;if(a==5){};return a;"));
    }

    public void testIf() {
        assertEquals(10, test("if (5 > 4) { return 10; } else { return 5; }"));
    }

    public void testIf2() {
        assertEquals(10, test("if (5 < 4) { return 5; } else { return 10; }"));
    }

    public void testIf3() {
        assertEquals(10, test("if(5<4){return 5;}else{return 10;}"));
    }

    public void testIfAndElse() {
        assertEquals(true, test("if (false) { return false; } else { return true; }"));
    }

    public void testIfAndElseif() {
        assertEquals(true, test("if (false) { return false; } else if(100 < 50) { return false; } else if (10 > 5) return true;"));
    }

    public void testIfAndElseif2() {
        assertEquals(true, MVEL.eval("if (false) { return false; } else if(100 < 50) { return false; } else if (10 > 5) return true;"));
    }

    public void testIfAndElseIfCondensedGrammar() {
        assertEquals("Foo", test("if (false) return 'Bar'; else return 'Foo';"));
    }

    public void testTernary4() {
        assertEquals("<test>", test("true ? '<test>' : '<poo>'"));
    }

    public void testPrecedenceOrder1() {
        String ex = "50 > 60 && 20 < 10 || 100 > 90";
        System.out.println("Expression: " + ex);

        assertTrue((Boolean) MVEL.eval(ex));
    }

    public void testDoLoop() {
        assertEquals(10, test("i = 0; do { i++ } while (i != 10); i"));
    }

    public void testDoLoop2() {
        assertEquals(50, test("i=100;do{i--}until(i==50); i"));
    }

    public void testForLoop() {
        assertEquals("012345", test("String str = ''; for(i=0;i<6;i++) { str += i }; str"));
    }

    public void testForLoop2() {
        assertEquals("012345", MVEL.eval("String str='';for(i=0;i<6;i++){str+=i};str", new HashMap()));
    }

    public void testUntilLoop() {
        assertEquals("012345", test("String str = ''; int i = 0; until (i == 6) { str += i++; }; str"));
    }

    public void testQualifiedForLoop() {
        ParserContext pCtx = new ParserContext();
        pCtx.setStrongTyping(true);
        pCtx.addImport(Foo.class);
        pCtx.addInput("l", ArrayList.class, new Class[] { Foo.class });

        List l = new ArrayList();
        l.add(new Foo());
        l.add(new Foo());
        l.add(new Foo());

        Map vars = new HashMap();
        vars.put("l", l);

        Serializable s = MVEL.compileExpression("String s = ''; for (Foo f : l) { s += f.name }; s", pCtx);

        String r  = (String) MVEL.executeExpression(s, vars);

        assertEquals("dogdogdog", r);
    }


}
