package org.mvel2.tests.core;

import org.mvel2.MVEL;

import static org.mvel2.MVEL.executeExpression;

import org.mvel2.ast.Function;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.tests.core.res.Member;
import org.mvel2.tests.core.res.SharedFuncLib;

import static org.mvel2.util.CompilerTools.extractAllDeclaredFunctions;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author Dhanji R. Prasanna (dhanji@gmail com)
 */
public class FunctionsTest extends AbstractTest {

  public final void testThatFunctionsCloseOverArguments() {
    final Object o = MVEL.eval(
        "def fun(x) { ($ in [1, 2, 3] if $ > x) }" +
            "" +
            "fun(0)",
        new HashMap<String, Object>()
    );

    assertTrue(o instanceof List);
    assertEquals(Arrays.asList(1, 2, 3), o);
  }

  public void testFunctionDefAndCall() {
    assertEquals("FoobarFoobar",
        test("function heyFoo() { return 'Foobar'; };\n" +
            "return heyFoo() + heyFoo();"));
  }

  public void testFunctionDefAndCall1() {
    assertEquals("FoobarFoobar", MVEL.eval("function heyFoo() { return 'Foobar'; };\n"
        + "return heyFoo() + heyFoo();", new HashMap()));
  }

  public void testFunctionDefAndCall2() {
    ExpressionCompiler compiler = new ExpressionCompiler("function heyFoo() { return 'Foobar'; };\n" +
        "return heyFoo() + heyFoo();");

    Serializable s = compiler.compile();

    Map<String, Function> m = extractAllDeclaredFunctions((CompiledExpression) s);

    assertTrue(m.containsKey("heyFoo"));

    OptimizerFactory.setDefaultOptimizer("reflective");

    assertEquals("FoobarFoobar", executeExpression(s, new HashMap()));
    assertEquals("FoobarFoobar", executeExpression(s, new HashMap()));

    OptimizerFactory.setDefaultOptimizer("dynamic");

  }

  public void testFunctionDefAndCall3() {
    assertEquals("FOOBAR", test("function testFunction() { a = 'foo'; b = 'bar'; a + b; }; testFunction().toUpperCase();  "));
  }

  public void testFunctionDefAndCall4() {
    assertEquals("barfoo", test("function testFunction(input) { return input; }; testFunction('barfoo');"));
  }

  public void testFunctionDefAndCall5() {
    assertEquals(10, test("function testFunction(x, y) { return x + y; }; testFunction(7, 3);"));
  }

  public void testFunctionDefAndCall6() {
    assertEquals("foo", MVEL.eval("def fooFunction(x) x; fooFunction('foo')", new HashMap()));
  }

  public void testAnonymousFunction() {
    assertEquals("foobar", test("a = function { 'foobar' }; a();"));
  }

  public void testJIRA207() {
    String ex = "x = 0; y = 0;" +
        "def foo() { x = 1; System.out.println('Word up'); }\n" +
        "def bar() { y = 1;  System.out.println('Peace out'); }\n" +
        "def doMany(fps) {\n" +
        "foreach(f : fps) { System.out.println(f); f(); }\n" +
        "}\n" +
        "doMany([foo,bar]);" +
        "x == 1 && y == 1;";

    Boolean bool;

    OptimizerFactory.setDefaultOptimizer("ASM");
    Serializable s = MVEL.compileExpression(ex);

    bool = (Boolean) MVEL.executeExpression(s, new HashMap());
    assertTrue(bool);

    OptimizerFactory.setDefaultOptimizer("dynamic");
  }

  public void testBranchesWithReturn() {
    String ex = "function max($a, $b) {\n" +
        " if ($a>$b){\n" +
        "    System.out.println($a);\n" +
        "    return $a;\n" +
        "} else {\n" +
        "   System.out.println($b);\n" +
        "   return $b;\n" +
        "};\n" +
        "}; val = max(20, 30);";

    Serializable s = MVEL.compileExpression(ex);
    Map<String, Object> map = new HashMap<String, Object>();
    MVEL.executeExpression(s, map);

    assertEquals(30, map.get("val"));
  }


  public static class TestClassAZZ {
    public String hey() {
      return "Heythere!";
    }
  }

  public void testCallGlobalStaticFunctionFromMVELFunction() {
    TestClassAZZ azz = new TestClassAZZ();

    String expr = "def foobie12345() { hey(); } foobie12345();";

    assertEquals("Heythere!", MVEL.eval(expr, azz, new HashMap<String, Object>()));
  }

  public void testDeepNestedLoopsInFunction() {
    assertEquals(10,
        test("def increment(i) { i + 1 }; def ff(i) { x = 0; while (i < 1) { " + "x++; " +
            "while (i < 10) { i = increment(i); } }; if (x == 1) return i; else -1; }; i = 0; ff(i);"));
  }

  public void testFunctions5() {
    String exp = "def foo(a,b) { a + b }; foo(1.5,5.25)";
    System.out.println(MVEL.eval(exp,
        new HashMap()));
  }

  public void testJIRA174() {
    OptimizerFactory.setDefaultOptimizer("ASM");

    Serializable s = MVEL.compileExpression("def test(a1) { java.util.Collection a = a1; a.clear(); a.add(1); a.add(2); a.add(3); a.remove((Object) 2); a; }\n" +
        "a = test(new java.util.ArrayList());\n" +
        "b = test(new java.util.HashSet());");

    Map vars = new HashMap();
    executeExpression(s, vars);

    assertEquals(false, ((Collection) vars.get("a")).contains(2));
    assertEquals(2, ((Collection) vars.get("a")).size());

    assertEquals(false, ((Collection) vars.get("b")).contains(2));
    assertEquals(2, ((Collection) vars.get("b")).size());
  }

  public void testMVEL225() {
    Serializable compileExpression = MVEL.compileExpression(
        "def f() { int a=1;a++;return a; }; f();");
    MapVariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
    assertEquals(2, MVEL.executeExpression(compileExpression, factory));
  }


  public void testAnonymousFunctionDecl() {
    assertEquals(3,
        test("anonFunc = function (a,b) { return a + b; }; anonFunc(1,2)"));
  }

  public void testFunctionSemantics() {
    assertEquals(true,
        test("function fooFunction(a) { return a; }; x__0 = ''; 'boob' == fooFunction(x__0 = 'boob') " +
            "&& x__0 == 'boob';"));
  }


  public void testFunctionReuse() {
    VariableResolverFactory functionFactory = new MapVariableResolverFactory();
    MVEL.eval("def foo() { \"foo\"; }; def bar() { \"bar\" };", functionFactory);

    VariableResolverFactory myVarFactory = new MapVariableResolverFactory();
    myVarFactory.setNextFactory(functionFactory);

    Serializable s = MVEL.compileExpression("foo() + bar();");

    assertEquals("foobar", MVEL.executeExpression(s, myVarFactory));
  }



  public void testFunctionReuseMultiThread(){
    ExecutorService executor = Executors.newFixedThreadPool(4);
    List<Callable<BigDecimal>> tasks = new ArrayList<Callable<BigDecimal>>();
    for (int i=0;i<30;i++){
      tasks.add(new Callable<BigDecimal>() {
        @Override
        public BigDecimal call() throws Exception {
          List<Member> lst = new ArrayList<Member>();
          lst.add(new Member("a", 18));
          lst.add(new Member("b", 12));
          lst.add(new Member("c", 40));
          lst.add(new Member("d", 66));
          lst.add(new Member("e", 72));
          HashMap<String, Object> map = new HashMap<String,Object>();
          map.put("members",lst);
          return new SharedFuncLib().eval("round( sum(members,0B, def(p){ return 2B*p.age; }) ,2)", map, BigDecimal.class);
        }
      });
    }

    try {
      List<Future<BigDecimal>> futures = executor.invokeAll(tasks);
      for (Future<BigDecimal> future : futures){
        System.out.println("res=" + future.get().toString());
      }
    } catch (InterruptedException ie){
      throw new RuntimeException(ie);
    } catch (ExecutionException ee) {
      throw new RuntimeException(ee);
    }
  }
}
