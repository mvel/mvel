package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.tests.core.res.Base;
import org.mvel2.tests.core.res.Foo;

import java.util.*;
import java.io.Serializable;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.eval;
import static org.mvel2.MVEL.executeExpression;


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
    String ex = "if (5 > 4) { return 10; } else { return 5; }";

    assertEquals(10, MVEL.eval(ex));

    Serializable s = MVEL.compileExpression(ex);

    assertEquals(10, MVEL.executeExpression(s));
  }

  public void testIf2() {
    assertEquals(10, test("if (5 < 4) { return 5; } else { return 10; }"));
  }

  public void testIf3() {
    String ex = "if(5<4){return 5;}else{return 10;}";

    assertEquals(10, MVEL.eval(ex));

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

  public void testIfAndElseif3() {
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("if (false) { return false; } else if(100 < 50) { return false; } else if (10 > 5) return true;")));
  }


  public void testIfAndElseIfCondensedGrammar() {
    assertEquals("Foo244", test("if (false) return 'Bar'; else return 'Foo244';"));
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
    String ex = "String str = ''; for(i=0;i<6;i++) { str += i }; str";

    assertEquals("012345", MVEL.eval(ex, new HashMap()));

    assertEquals("012345", test(ex));
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
    pCtx.addInput("l", ArrayList.class, new Class[]{Foo.class});

    List l = new ArrayList();
    l.add(new Foo());
    l.add(new Foo());
    l.add(new Foo());

    Map vars = new HashMap();
    vars.put("l", l);

    Serializable s = MVEL.compileExpression("String s = ''; for (Foo f : l) { s += f.name }; s", pCtx);

    String r = (String) MVEL.executeExpression(s, vars);

    assertEquals("dogdogdog", r);
  }

  public void testForLoopWithVar() {
    String str = "int height = 100; int j = 0; for (i = 0; i < height; i++) {j++ }; return j;";

    ParserConfiguration pconf = new ParserConfiguration();
    ParserContext pctx = new ParserContext(pconf);
    pctx.setStrongTyping(true);

    ExecutableStatement stmt = (ExecutableStatement) MVEL.compileExpression(str, pctx);

    Map vars = new HashMap();
    assertEquals(new Integer(100), MVEL.executeExpression(stmt, vars));
  }

  public void testEmptyLoopSemantics() {
    Serializable s = MVEL.compileExpression("for (i = 0; i < 100000000000; i++) { }");
    MVEL.executeExpression(s, new HashMap());
  }

  public void testLoopWithEscape() {
    Serializable s = MVEL.compileExpression("x = 0; for (; x < 10000; x++) {}");
    Map<String, Object> vars = new HashMap<String, Object>();
    MVEL.executeExpression(s, vars);

    assertEquals(10000, vars.get("x"));

    vars.remove("x");

    MVEL.eval("x = 0; for (; x < 10000; x++) {}", vars);

    assertEquals(10000, vars.get("x"));
  }


  public static class TargetClass {
    private short _targetValue = 5;

    public short getTargetValue() {
      return _targetValue;
    }
  }

  public void testNestedMethodCall() {
    List elements = new ArrayList();
    elements.add(new TargetClass());
    Map variableMap = new HashMap();
    variableMap.put("elements",
        elements);
    eval("results = new java.util.ArrayList(); foreach (element : elements) { " +
        "if( {5} contains element.targetValue.intValue()) { results.add(element); } }; results",
        variableMap);
  }

  public void testStaticallyTypedItemInForEach() {
    assertEquals("1234",
        test("StringBuffer sbuf = new StringBuffer(); foreach (int i : new int[] { 1,2,3,4 })" +
            " { sbuf.append(i); }; sbuf.toString()"));
  }

  public void testJIRA115() {
    String exp = "results = new java.util.ArrayList(); foreach (element : elements) { " +
        "if( {1,32769,32767} contains element ) { results.add(element);  } }; results";
    Map map = new HashMap();
    map.put("elements",
        new int[]{1, 32769, 32767});
    ArrayList result = (ArrayList) MVEL.eval(exp,
        map);

    assertEquals(3,
        result.size());
  }

  public void testStringWithTernaryIf() {
    test("System.out.print(\"Hello : \" + (foo != null ? \"FOO!\" : \"NO FOO\") + \". Bye.\");");
  }

  private static Object testTernary(int i,
                                    String expression) throws Exception {
    Object val;
    Object val2;
    try {
      val = executeExpression(compileExpression(expression),
          JIRA124_CTX);
    }
    catch (Exception e) {
      System.out.println("FailedCompiled[" + i + "]:" + expression);
      throw e;
    }

    try {
      val2 = MVEL.eval(expression,
          JIRA124_CTX);
    }
    catch (Exception e) {
      System.out.println("FailedEval[" + i + "]:" + expression);
      throw e;
    }

    if (((val == null || val2 == null) && val != val2) || (val != null && !val.equals(val2))) {
      throw new AssertionError("results do not match (" + String.valueOf(val)
          + " != " + String.valueOf(val2) + ")");
    }

    return val;
  }

  private static Map<String, Boolean> JIRA124_CTX = Collections.singletonMap("testValue",
      true);

  public void testJIRA124() throws Exception {
    assertEquals("A",
        testTernary(1,
            "testValue == true ? 'A' :  'B' + 'C'"));
    assertEquals("AB",
        testTernary(2,
            "testValue ? 'A' +  'B' : 'C'"));
    assertEquals("A",
        testTernary(3,
            "(testValue ? 'A' :  'B' + 'C')"));
    assertEquals("AB",
        testTernary(4,
            "(testValue ? 'A' +  'B' : 'C')"));
    assertEquals("A",
        testTernary(5,
            "(testValue ? 'A' :  ('B' + 'C'))"));
    assertEquals("AB",
        testTernary(6,
            "(testValue ? ('A' + 'B') : 'C')"));

    JIRA124_CTX = Collections.singletonMap("testValue",
        false);

    assertEquals("BC",
        testTernary(1,
            "testValue ? 'A' :  'B' + 'C'"));
    assertEquals("C",
        testTernary(2,
            "testValue ? 'A' +  'B' : 'C'"));
    assertEquals("BC",
        testTernary(3,
            "(testValue ? 'A' :  'B' + 'C')"));
    assertEquals("C",
        testTernary(4,
            "(testValue ? 'A' +  'B' : 'C')"));
    assertEquals("BC",
        testTernary(5,
            "(testValue ? 'A' :  ('B' + 'C'))"));
    assertEquals("C",
        testTernary(6,
            "(testValue ? ('A' + 'B') : 'C')"));
  }

  /**
   * Community provided test cases
   */
  @SuppressWarnings({"unchecked"})
  public void testCalculateAge() {
    Calendar c1 = Calendar.getInstance();
    c1.set(1999,
        0,
        10); // 1999 jan 20
    Map objectMap = new HashMap(1);
    Map propertyMap = new HashMap(1);
    propertyMap.put("GEBDAT",
        c1.getTime());
    objectMap.put("EV_VI_ANT1",
        propertyMap);
    assertEquals("N",
        testCompiledSimple(
            "new org.mvel2.tests.core.res.PDFFieldUtil().calculateAge(EV_VI_ANT1.GEBDAT) >= 25 ? 'Y' : 'N'",
            null,
            objectMap));
  }

  public void testSubEvaluation() {
    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put("EV_BER_BER_NR",
        "12345");
    map.put("EV_BER_BER_PRIV",
        Boolean.FALSE);

    assertEquals("12345",
        testCompiledSimple("EV_BER_BER_NR + ((EV_BER_BER_PRIV != empty && EV_BER_BER_PRIV == true) ? \"/PRIVAT\" : '')",
            null,
            map));

    map.put("EV_BER_BER_PRIV",
        Boolean.TRUE);
    assertEquals("12345/PRIVAT",
        testCompiledSimple("EV_BER_BER_NR + ((EV_BER_BER_PRIV != empty && EV_BER_BER_PRIV == true) ? \"/PRIVAT\" : '')",
            null,
            map));
  }


  public void testCompactIfElse() {
    assertEquals("foo",
        test("if (false) 'bar'; else 'foo';"));
  }

  public void testForInitializerScope() {
    String ex = "for (int i = 0; i < 10; i++) { 'fop'; }\n" +
        "for (int i = 0; i < 10; i++) { 'foo'; }";

    Serializable s = MVEL.compileExpression(ex);

    MVEL.executeExpression(s, new HashMap());
  }


  public void testForEachTerminateFlow() {
    String ex = "for(int i=0;i<5;i++) {\n" +
        "System.out.println(\"LOOP\" + i);\n" +
        "return true;\n" +
        "}\n" +
        "System.out.println(\"END\");";

    Serializable s = MVEL.compileExpression(ex);

    assertEquals(true, MVEL.executeExpression(s, new HashMap()));
  }

  public final void testFunctionCall() {

    MVEL.eval(
        "def test() { for(i = 0; i < 3; i++) { System.out.println('...') } } \n" +
            "test()",
        new HashMap<String, Object>());
  }

  public void testMultipleArgumentsInFunction() {
    String expression = "def cond(x, y) {\n" +
        "\tif (x ~= \"fet.*\") {\n" +
        "\t\tif ((x.endsWith(('sock')))) {\n" +
        " \t\t\treturn 1;\n" +
        "\t\t}  else if ((x.endsWith(('lock')))) {\n" +
        " \t\t\treturn [1: ((y > 12) ? 1 : 2), 2: (12 + 1)];\n" +
        "\t\t} ;\n" +
        "\t}\n" +
        "(null).print();\n" +
        "\n" +
        "}\n" +
        "\n" +
        "cond('fetlock', 12)";

    System.out.println(expression);

    Exception thrown = null;
    try {
      MVEL.executeExpression(MVEL.compileExpression(expression), new HashMap());
    }
    catch (Exception e) {
      thrown = e;
    }

    assertNull("Return statement not being honored!", thrown);
  }

  public void testDhanji1() {
    String expression = "def insert(i, ls) {\n" +
        "  if (ls == empty) {\n" +
        "    return [];\n" +
        "  }\n" +
        "  if (ls is java.util.List) {\n" +
        "    x = ls[0];\n" +
        "    xs = ls.size() == 1 ? [] : ls.subList(1, ls.size());\n" +
        "    return (((i <= x) ? ([i, x] + xs) : insert(i, xs)));\n" +
        "  }\n" +
        "}\n" +
        "\n" +
        "insert(2, [1, 3, 4])";

    Object o = MVEL.eval(expression, new HashMap<String, Object>());

    System.out.println(o);
  }

  public void testDhanji2() {
    assertEquals(Arrays.asList(1, 2, 3, 4), MVEL.eval("x = 1; y = 2; [x,y] + [3,4]", new HashMap<String, Object>()));
  }


  private static int fibonacci(int n) {
    if (n < 2)
      return 1;
    else
      return fibonacci(n - 2) + fibonacci(n - 1);
  }
}
