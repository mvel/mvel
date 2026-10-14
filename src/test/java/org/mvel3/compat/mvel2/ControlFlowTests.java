package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.Foo;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.ControlFlowTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class ControlFlowTests extends AbstractTest {

  // Category not isolated: the `if` statement has no `;`, so it is compiled in expression mode and fails to parse. With a `;` inside the braces
  // the next error is "missing return statement" (verified).
  @Test
  @KnownCompatibility("block-syntax")
  public void testSimpleIfStatement() {
    test("if (true) { System.out.println(\"test!\") }     \n");
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testAnd() {
    assertEquals(true, test("c != null && foo.bar.name == 'dog' && foo.bar.woof"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null"; the single-quoted `'dog'` is the likely cause (testAnd with
  // spaces fails with "Unsolved symbol in g"), but this was not tested with double quotes.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testAnd2() {
    assertEquals(true, test("c!=null&&foo.bar.name=='dog'&&foo.bar.woof"));
  }

  // Category not isolated: observed error is "Unsolved symbol in ppyBar" (single-quote-string). With double quotes the error becomes
  // "bad operand types ... String * Integer" because `pi` is a String (string-coercion); both were observed.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testComplexAnd() {
    assertEquals(true, test("(pi * hour) > 0 && foo.happy() == 'happyBar'"));
  }

  // Category not isolated: observed error is a JavaParser null-node failure. The expression also has a `String` operand of `&&` (non-boolean
  // operand, verified in testShortPathExpression2), a trailing `foo.register` without `return` (block-syntax), and MVEL2 passes a root object
  // (`new Base()`). Which one is the trigger was not verified.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testShortPathExpression() {
    assertEquals(null, eval("3 > 4 && foo.toUC('test'); foo.register", new Base(), createTestMap()));
  }

  // Category not isolated: observed error is a parse error from the single-quoted literal. With double quotes the error becomes
  // "bad operand types for binary operator '||' (boolean, String)": `foo.toUC(..)` returns a String and MVEL3 requires a
  // boolean operand (verified). That second difference has no category.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testShortPathExpression2() {
    assertEquals(true, test("4 > 3 || foo.toUC('test')"));
  }

  // Category not isolated: same as testShortPathExpression2: after converting to double quotes the error is "bad operand types for
  // binary operator '||' (boolean, String)" (verified).
  @Test
  @KnownCompatibility("single-quote-string")
  public void testShortPathExpression4() {
    assertEquals(true, test("4>3||foo.toUC('test')"));
  }

  @Test
  public void testOr() {
    assertEquals(true, test("fun || true"));
  }

  @Test
  public void testOrOperator() {
    assertEquals(true, test("true||true"));
  }

  @Test
  public void testOrOperator2() {
    assertEquals(true, test("2 > 3 || 3 > 2"));
  }

  @Test
  @KnownCompatibility("string-coercion")
  public void testOrOperator3() {
    assertEquals(true, test("pi > 5 || pi > 6 || pi > 3"));
  }

  // Category not isolated: `defnull` is a field of the MVEL2 root object (`Base`), which is not in the Map. The adapter evaluates against the
  // Map only; the observed error is "Unsolved symbol in defnull".
  @Test
  @KnownCompatibility("this-reference")
  public void testShortPathExpression3() {
    assertEquals(false, test("defnull != null  && defnull.length() > 0"));
  }

  // Category not isolated: the observed error is a parse error at the missing trailing `return` (block-syntax). The expression also calls
  // `populate()` and reads `barfoo` on the MVEL2 root object (see this-reference), and uses a single-quoted literal. Not tested separately.
  @Test
  @KnownCompatibility("block-syntax")
  public void testMultiStatement() {
    assertEquals(true, test("populate(); barfoo == 'sarah'"));
  }

  // Category not isolated: `'foobie'` is read as a `char` literal; the result is silently `true` (not an exception).
  @Test
  @KnownCompatibility("single-quote-string")
  public void testTernary() {
    assertEquals("foobie", test("zero==0?'foobie':zero"));
  }

  // Category not isolated: `'foobie'`/`'blimpie'` are read as `char` literals; the result is silently `false`. With double quotes it passes (verified).
  @Test
  @KnownCompatibility("single-quote-string")
  public void testTernary2() {
    assertEquals("blimpie", test("zero==1?'foobie':'blimpie'"));
  }

  // Category not isolated: the single-quoted operands are read as `char` literals; the result is silently `false`. With double quotes it passes (verified).
  @Test
  @KnownCompatibility("single-quote-string")
  public void testTernary3() {
    assertEquals("foobiebarbie", test("zero==1?'foobie':'foobie'+'barbie'"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null". The expression also has single-quoted strings; with double quotes
  // the error becomes a parse error at `isdef` (verified), so isdef-operator is the remaining cause.
  @Test
  @KnownCompatibility("isdef-operator")
  public void testTernary5() {
    assertEquals("skat!", test("isdef someWierdVar ? 'squid' : 'skat!';"));
  }

  @Test
  public void testEmptyIf() {
    assertEquals(5, test("a = 5; if (a == 5) { }; return a;"));
  }

  @Test
  public void testEmptyIf2() {
    assertEquals(5, test("a=5;if(a==5){};return a;"));
  }

  @Test
  public void testIf() {
    String ex = "if (5 > 4) { return 10; } else { return 5; }";

    assertEquals(10, eval(ex));

    Serializable s = compileExpression(ex);

    assertEquals(10, executeExpression(s));
  }

  @Test
  public void testIf2() {
    assertEquals(10, test("if (5 < 4) { return 5; } else { return 10; }"));
  }

  @Test
  public void testIf3() {
    String ex = "if(5<4){return 5;}else{return 10;}";

    assertEquals(10, eval(ex));

    assertEquals(10, test("if(5<4){return 5;}else{return 10;}"));
  }

  @Test
  public void testIfAndElse() {
    assertEquals(true, test("if (false) { return false; } else { return true; }"));
  }

  // Category not isolated: "missing return statement": the last `else if` branch has no final `return` (verified: ending with a braced `else if` plus a final `return` makes it pass).
  @Test
  @KnownCompatibility("block-syntax")
  public void testIfAndElseif() {
    assertEquals(true, test("if (false) { return false; } else if(100 < 50) { return false; } else if (10 > 5) return true;"));
  }

  // Category not isolated: same as testIfAndElseif: "missing return statement" (verified).
  @Test
  @KnownCompatibility("block-syntax")
  public void testIfAndElseif2() {
    assertEquals(true, eval("if (false) { return false; } else if(100 < 50) { return false; } else if (10 > 5) return true;"));
  }

  // Category not isolated: same as testIfAndElseif: "missing return statement" (verified).
  @Test
  @KnownCompatibility("block-syntax")
  public void testIfAndElseif3() {
    assertEquals(true, executeExpression(compileExpression("if (false) { return false; } else if(100 < 50) { return false; } else if (10 > 5) return true;")));
  }


  // Category not isolated: the observed parse error ("missing ';' at 'else'") comes from the single-quoted literal. With double quotes it passes (verified).
  @Test
  @KnownCompatibility("single-quote-string")
  public void testIfAndElseIfCondensedGrammar() {
    assertEquals("Foo244", test("if (false) return 'Bar'; else return 'Foo244';"));
  }

  // Category not isolated: `'<test>'` is read as a `char` literal; the result is silently `true`. With double quotes it passes (verified).
  @Test
  @KnownCompatibility("single-quote-string")
  public void testTernary4() {
    assertEquals("<test>", test("true ? '<test>' : '<poo>'"));
  }

  @Test
  public void testPrecedenceOrder1() {
    String ex = "50 > 60 && 20 < 10 || 100 > 90";
    System.out.println("Expression: " + ex);

    assertTrue((Boolean) eval(ex));
  }

  // Category not isolated: undeclared `i`, missing `;` before `}` and no trailing `return`. Each one produces an error; with `int i = 0;`, `i++;` and `return i;` it passes (verified).
  @Test
  @KnownCompatibility("block-syntax")
  public void testDoLoop() {
    assertEquals(10, test("i = 0; do { i++ } while (i != 10); i"));
  }

  // Category not isolated: `do { .. } until (cond)` fails in the parser (NPE on `parExpression`). The expression also uses undeclared `i` and no `return` (block-syntax); with `while (!(cond))` and declarations it passes (verified).
  @Test
  @KnownCompatibility("until-statement")
  public void testDoLoop2() {
    assertEquals(50, test("i=100;do{i--}until(i==50); i"));
  }

  // Category not isolated: the first parse error is at the `;` after `''` (empty char literal). The expression also has an undeclared loop variable `i`, a missing `;` before `}`
  // and no `return` (block-syntax); the first two were observed separately with double quotes.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testForLoop() {
    String ex = "String str = ''; for(i=0;i<6;i++) { str += i }; str";

    assertEquals("012345", eval(ex, new HashMap()));

    assertEquals("012345", test(ex));
  }

  // Category not isolated: the first parse error is at the `;` after `''` (empty char literal). The expression also has an undeclared `i`, a missing `;` before `}`
  // and no `return` (block-syntax); not tested separately.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testForLoop2() {
    assertEquals("012345", eval("String str='';for(i=0;i<6;i++){str+=i};str", new HashMap()));
  }

  // Category not isolated: the first parse error is at the `;` after `''` (single-quote-string); the `until` loop itself fails with "missing ';' at '{'" (verified with
  // double quotes). The expression also has no trailing `return` (block-syntax).
  @Test
  @KnownCompatibility("until-statement")
  public void testUntilLoop() {
    assertEquals("012345", test("String str = ''; int i = 0; until (i == 6) { str += i++; }; str"));
  }

  // Category not isolated: the first parse error is at the `;` after `''`. The expression has no trailing `return` (block-syntax) and `ArrayList<Foo>`
  // input generics cannot be declared through TestCompilerContext, so `for (Foo f : l)` would see `Object`; neither was tested separately.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testQualifiedForLoop() {
    // Original strong typing flag is not an MVEL3 mode. The generic type argument
    // (ArrayList<Foo>) of the input is not expressible through TestCompilerContext.
    TestCompilerContext pCtx = new TestCompilerContext();
    pCtx.addImport(Foo.class);
    pCtx.addInput("l", ArrayList.class);

    List l = new ArrayList();
    l.add(new Foo());
    l.add(new Foo());
    l.add(new Foo());

    Map vars = new HashMap();
    vars.put("l", l);

    Serializable s = compileExpression("String s = ''; for (Foo f : l) { s += f.name }; s", pCtx);

    String r = (String) executeExpression(s, vars);

    assertEquals("dogdogdog", r);
  }

  // Category not isolated: parse error at `{j++ }`: missing `;` before `}` (verified). `for (i = 0; ..)` with an undeclared `i` is an additional cause.
  @Test
  @KnownCompatibility("block-syntax")
  public void testForLoopWithVar() {
    String str = "int height = 100; int j = 0; for (i = 0; i < height; i++) {j++ }; return j;";

    TestCompilerContext pconf = new TestCompilerContext();
    TestCompilerContext pctx = new TestCompilerContext(pconf);
    // Original strong typing flag is not an MVEL3 mode.

    Serializable stmt = compileExpression(str, pctx);

    Map vars = new HashMap();
    assertEquals(new Integer(100), executeExpression(stmt, vars));
  }

  // Category not isolated: "Unsolved symbol in i" (undeclared loop variable). Even when declared, `100000000000` is an int-literal overflow in Java and the loop would
  // run 10^11 iterations in MVEL3; that was not tested.
  @Test
  @KnownCompatibility("block-syntax")
  public void testEmptyLoopSemantics() {
    Serializable s = compileExpression("for (i = 0; i < 100000000000; i++) { }");
    executeExpression(s, new HashMap());
  }

  // Category not isolated: "Unsolved symbol in x" (undeclared `x`). The test also expects the script to write `x` back into the variable map,
  // which MVEL3 locals do not do; not tested separately.
  @Test
  @KnownCompatibility("block-syntax")
  public void testLoopWithEscape() {
    Serializable s = compileExpression("x = 0; for (; x < 10000; x++) {}");
    Map<String, Object> vars = new HashMap<String, Object>();
    executeExpression(s, vars);

    assertEquals(10000, vars.get("x"));

    vars.remove("x");

    eval("x = 0; for (; x < 10000; x++) {}", vars);

    assertEquals(10000, vars.get("x"));
  }


  public static class TargetClass {
    private short _targetValue = 5;

    public short getTargetValue() {
      return _targetValue;
    }
  }

  // Category not isolated: fails with "A reference was unexpectedly null". The expression also uses the `{5} contains ..` inline array and
  // `contains` operator (inline-array-literal, contains-operator); which one is the trigger was not verified.
  @Test
  @KnownCompatibility("foreach-syntax")
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

  // Category not isolated: fails with "A reference was unexpectedly null" (the `foreach` syntax). The expression also has no trailing `return` (block-syntax).
  @Test
  @KnownCompatibility("foreach-syntax")
  public void testStaticallyTypedItemInForEach() {
    assertEquals("1234",
        test("java.lang.StringBuffer sbuf = new java.lang.StringBuffer(); foreach (int i : new int[] { 1,2,3,4 })" +
            " { sbuf.append(i); }; sbuf.toString()"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null". The expression also uses the `{1,32769,32767} contains` inline array and
  // `contains` operator (inline-array-literal, contains-operator); not tested separately.
  @Test
  @KnownCompatibility("foreach-syntax")
  public void testJIRA115() {
    String exp = "results = new java.util.ArrayList(); foreach (element : elements) { " +
        "if( {1,32769,32767} contains element ) { results.add(element);  } }; results";
    Map map = new HashMap();
    map.put("elements",
        new int[]{1, 32769, 32767});
    ArrayList result = (ArrayList) eval(exp,
        map);

    assertEquals(3,
        result.size());
  }

  // Category not isolated: "missing return statement": a lone `System.out.print(..);` has no `return` (verified: adding `return null;` makes it pass).
  @Test
  @KnownCompatibility("block-syntax")
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
      val2 = eval(expression,
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

  // Category not isolated: "expected:<A> but was:<A>": `'A'` is a `Character`, not a `String`.
  @Test
  @KnownCompatibility("single-quote-string")
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
  // Category not isolated: `EV_VI_ANT1.GEBDAT` on a Map value is typed `Object`, so `calculateAge(Date)` cannot be resolved. With the `#java.util.Date#` hint and
  // double quotes the expression returns "N" (verified). The single-character `'Y'`/`'N'` are also `char` literals; not tested separately.
  @Test
  @KnownCompatibility("dynamic-property-type")
  public void testCalculateAge() {
    // calculate age of a person who was born in 1999 jan 10, on 2023 dec 25
    // 2023 dec 25 - 1999 jan 10 = 24 years
    Calendar c1 = Calendar.getInstance();
    c1.set(1999,
        0,
        10); // 1999 jan 10
    Map objectMap = new HashMap(1);
    Map propertyMap = new HashMap(1);
    propertyMap.put("GEBDAT",
        c1.getTime());
    objectMap.put("EV_VI_ANT1",
        propertyMap);
    assertEquals("N",
        testCompiledSimple(
            "new org.mvel3.compat.mvel2.res.PDFFieldUtil().calculateAge(EV_VI_ANT1.GEBDAT) >= 25 ? 'Y' : 'N'",
            null,
            objectMap));
  }

  // Category not isolated: with double quotes the error is "Unsolved symbol in empty" (verified). With the original `''` the call silently returns "12345"
  // for both inputs (single-quote-string); the observed assertion failure is on the second input.
  @Test
  @KnownCompatibility("empty-operator")
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


  // Category not isolated: a bare string as a statement is "not a statement" in Java (verified with double quotes). The original single-quoted literals
  // (single-quote-string) fail earlier with "A reference was unexpectedly null".
  @Test
  @KnownCompatibility("block-syntax")
  public void testCompactIfElse() {
    assertEquals("foo",
        test("if (false) 'bar'; else 'foo';"));
  }

  // Category not isolated: the observed parse error is at the single-quoted statements. With double quotes the error becomes "not a statement" (block-syntax) (verified).
  @Test
  @KnownCompatibility("single-quote-string")
  public void testForInitializerScope() {
    String ex = "for (int i = 0; i < 10; i++) { 'fop'; }\n" +
        "for (int i = 0; i < 10; i++) { 'foo'; }";

    Serializable s = compileExpression(ex);

    executeExpression(s, new HashMap());
  }


  // Category not isolated: "missing return statement": the trailing `System.out.println("END");` has no `return` (verified: adding `return false;` makes it pass).
  @Test
  @KnownCompatibility("block-syntax")
  public void testForEachTerminateFlow() {
    String ex = "for(int i=0;i<5;i++) {\n" +
        "System.out.println(\"LOOP\" + i);\n" +
        "return true;\n" +
        "}\n" +
        "System.out.println(\"END\");";

    Serializable s = compileExpression(ex);

    assertEquals(true, executeExpression(s, new HashMap()));
  }

  // Category not isolated: the original single-quoted `'...'` fails earlier with "Unsupported member reference"; with double quotes the parse error is at `def test(` (verified).
  @Test
  @KnownCompatibility("function-declaration")
  public final void testFunctionCall() {

    eval(
        "def test() { for(i = 0; i < 3; i++) { System.out.println('...') } } \n" +
            "test()",
        new HashMap<String, Object>());
  }

  // Category not isolated: the test catches any exception, so it fails with "Return statement not being honored!" for any error. The body also uses `~=`,
  // `[1: .., 2: ..]` maps and `(null).print()`; not tested separately.
  @Test
  @KnownCompatibility("function-declaration")
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
      executeExpression(compileExpression(expression), new HashMap());
    }
    catch (Exception e) {
      thrown = e;
    }

    assertNull("Return statement not being honored!", thrown);
  }

  // Category not isolated: fails with "A reference was unexpectedly null". The body also uses `empty`, `is`, list literals and `[i, x] + xs` list concatenation; not tested separately.
  @Test
  @KnownCompatibility("function-declaration")
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

    Object o = eval(expression, new HashMap<String, Object>());

    System.out.println(o);
  }

  // Category not isolated: parse error at the missing trailing `return`/`;`. With `var`/`return` the next error is "bad operand types for '+' (List, List)"
  // (list-concat-operator) (verified).
  @Test
  @KnownCompatibility("block-syntax")
  public void testDhanji2() {
    assertEquals(Arrays.asList(1, 2, 3, 4), eval("x = 1; y = 2; [x,y] + [3,4]", new HashMap<String, Object>()));
  }


  private static int fibonacci(int n) {
    if (n < 2)
      return 1;
    else
      return fibonacci(n - 2) + fibonacci(n - 1);
  }
}
