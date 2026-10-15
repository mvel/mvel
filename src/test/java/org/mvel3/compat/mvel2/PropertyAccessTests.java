package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.Foo;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.PropertyAccessTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 *
 * <p>Not imported (MVEL2-only APIs): {@code testBindingCoercion} ({@code MVEL.setProperty}),
 * {@code testMVELCompilerBoth} and {@code testMVELCompilerBoth2} ({@code PropertyHandlerFactory} /
 * {@code PropertyHandler}). The {@code setUp} that resets MVEL2 optimizer/property-handler settings is
 * removed, as are the {@code OptimizerFactory} calls inside the tests.
 */
@Tag("mvel2-compatibility")
public class PropertyAccessTests extends AbstractTest {

  @Test
  public void testSingleProperty() {
    assertEquals(false, test("fun"));
  }

  @Test
  public void testMethodOnValue() {
    assertEquals("DOG", test("foo.bar.name.toUpperCase()"));
  }

  @Test
  public void testMethodOnValue2() {
    assertEquals("DOG", test("foo. bar. name.toUpperCase()"));
  }

  @Test
  public void testSimpleProperty() {
    assertEquals("dog", test("foo.bar.name"));
  }

  // Category not isolated: `DATA` is a getter of the MVEL2 root object (`Base`), which is not in the Map; observed "cannot find symbol:
  // variable DATA".
  @Test
  @KnownCompatibility("this-reference")
  public void testSimpleProperty2() {
    assertEquals("cat", test("DATA"));
  }

  @Test
  public void testPropertyViaDerivedClass() {
    assertEquals("cat", test("derived.data"));
  }

  @Test
  public void testThroughInterface() {
    assertEquals("FOOBAR!", test("testImpl.name"));
  }

  @Test
  public void testThroughInterface2() {
    assertEquals(true, test("testImpl.foo"));
  }

  // Category not isolated: `funMap['foo']` silently returns the whole map (the `'foo'` index is read as a `char`). With double quotes the
  // error becomes "cannot find symbol: method happy() ... Object" (verified), i.e. `dynamic-property-type` is a second cause.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMapAccessWithMethodCall() {
    assertEquals("happyBar", test("funMap['foo'].happy()"));
  }

  // Category not isolated: `sarahl` is a field of the MVEL2 root object (`Base`), not in the Map; observed "cannot find symbol: variable
  // sarahl".
  @Test
  @KnownCompatibility("this-reference")
  public void testUninitializedInt() {
    assertEquals(0, test("sarahl"));
  }

  @Test
  public void testMethodAccess() {
    assertEquals("happyBar", test("foo.happy()"));
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testMethodAccess2() {
    assertEquals("FUBAR", test("foo.toUC( 'fubar' )"));
  }

  // Category not isolated: parse error "missing ')'" from `'cat'`. `equalityCheck(..)` is a method of the MVEL2 root object (see
  // this-reference; verified in testMethodAccess4/6), so it would fail next. Not tested separately.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMethodAccess3() {
    assertEquals(true, test("equalityCheck(c, 'cat')"));
  }

  // Category not isolated: `readBack(null)` is a method of the MVEL2 root object (`Base`), not callable from the Map context; observed "No
  // method candidates were found for method call 'readBack'".
  @Test
  @KnownCompatibility("this-reference")
  public void testMethodAccess4() {
    assertEquals(null, test("readBack(null)"));
  }

  // Category not isolated: parse error "missing ')'" from `'test'`. `appendTwoStrings(..)` is also a root-object method (see this-reference).
  // Not tested separately.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMethodAccess5() {
    assertEquals("nulltest", test("appendTwoStrings(null, 'test')"));
  }

  // Category not isolated: `equalityCheck(..)` is a method of the MVEL2 root object (`Base`); observed "No method candidates were found for
  // method call 'equalityCheck'". The single-quoted `'cat'` (single-quote-string) is a second cause, not tested separately.
  @Test
  @KnownCompatibility("this-reference")
  public void testMethodAccess6() {
    assertEquals(true, test("   equalityCheck(   c  \n  ,   \n   'cat'      )   "));
  }

  @Test
  public void testLiteralPassThrough() {
    assertEquals(true, test("true"));
  }

  @Test
  public void testLiteralPassThrough2() {
    assertEquals(false, test("false"));
  }

  @Test
  public void testLiteralPassThrough3() {
    assertEquals(null, test("null"));
  }

  @Test
  @KnownCompatibility("chor-operator")
  public void testLiteralReduction1() {
    assertEquals("foo", test("null or 'foo'"));
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testStrAppend() {
    assertEquals("foobarcar", test("'foo' + 'bar' + 'car'"));
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testStrAppend2() {
    assertEquals("foobarcar1", test("'foobar' + 'car' + 1"));
  }

  // Category not isolated: `funMap['foo']` silently returns the whole map (the `'foo'` index is read as a `char`). With double quotes the
  // error becomes "cannot find symbol: variable bar ... Object" (verified in testMapAccess2), i.e. `dynamic-property-type` is a second cause.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMapAccess() {
    assertEquals("dog", test("funMap['foo'].bar.name"));
  }

  // Category not isolated: `funMap.foo` is resolved but typed `Object`, so `.bar` fails with "cannot find symbol: variable bar ... class
  // java.lang.Object".
  @Test
  @KnownCompatibility("dynamic-property-type")
  public void testMapAccess2() {
    assertEquals("dog", test("funMap.foo.bar.name"));
  }

  // Category not isolated: observed error is a JavaParser null-node failure from the single-quoted literals. With double quotes the error
  // becomes "unreported exception java.lang.ClassNotFoundException; must be caught or declared to be thrown" (verified): MVEL3 compiles to
  // Java, so the checked exception of `Class.forName` must be handled. That second difference has no category.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testStaticMethodFromLiteral() {
    assertEquals(String.class.getName(), test("String.valueOf(Class.forName('java.lang.String').getName())"));
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testObjectInstantiation() {
    test("new java.lang.String('foobie')");
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testObjectInstantiationWithMethodCall() {
    assertEquals("FOOBIE", test("new String('foobie')  . toUpperCase()"));
  }

  @Test
  public void testObjectInstantiation2() {
    test("new String() is String");
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testObjectInstantiation3() {
    test("new java.text.SimpleDateFormat('yyyy').format(new java.util.Date(System.currentTimeMillis()))");
  }

  @Test
  @KnownCompatibility("this-reference")
  public void testThisReference() {
    assertEquals(true, test("this") instanceof Base);
  }

  @Test
  @KnownCompatibility("this-reference")
  public void testThisReference2() {
    assertEquals(true, test("this.funMap") instanceof Map);
  }

  @Test
  @KnownCompatibility("this-reference")
  public void testThisReferenceInMethodCall() {
    assertEquals(101, test("Integer.parseInt(this.number)"));
  }

  @Test
  @KnownCompatibility("this-reference")
  public void testThisReferenceInConstructor() {
    assertEquals("101", test("new String(this.number)"));
  }

  @Test
  public void testStringEscaping() {
    assertEquals("\"Mike Brock\"", test("\"\\\"Mike Brock\\\"\""));
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testStringEscaping2() {
    assertEquals("MVEL's Parser is Fast", test("'MVEL\\'s Parser is Fast'"));
  }

  // Category not isolated: the root object `Base` is passed to `executeExpression`; the POJO-root path fails with an NPE inside JavaParser
  // ("method" is null). The same NPE occurs for root `Base` without `this` in the Map, so it is not caused by `this` itself. Whether the
  // adapter's POJO-root emulation or MVEL3 is responsible was not isolated.
  @Test
  @KnownCompatibility("this-reference")
  public void testCompiledMethodCall() {
    assertEquals(String.class, executeExpression(compileExpression("c.getClass()"), new Base(), createTestMap()));
  }

  @Test
  @KnownCompatibility("class-literal")
  public void testStaticNamespaceCall() {
    assertEquals(java.util.ArrayList.class, test("java.util.ArrayList"));
  }

  // Category not isolated: observed parse error "missing ')'" from `'FooBar'`. With double quotes it passes (verified), so the
  // fully-qualified `java.lang.String.valueOf` is not a cause.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testStaticNamespaceClassWithMethod() {
    assertEquals("FooBar", test("java.lang.String.valueOf('FooBar')"));
  }

  @Test
  public void testStaticNamespaceClassWithField() {
    assertEquals(Integer.MAX_VALUE, test("java.lang.Integer.MAX_VALUE"));
  }

  @Test
  public void testStaticNamespaceClassWithField2() {
    assertEquals(Integer.MAX_VALUE, test("Integer.MAX_VALUE"));
  }

  @Test
  public void testStaticFieldAsMethodParm() {
    assertEquals(String.valueOf(Integer.MAX_VALUE), test("String.valueOf(Integer.MAX_VALUE)"));
  }

  // Category not isolated: observed error is "Unsolved symbol in stringArray" because `stringArray` is a field of the MVEL2 root object (see
  // this-reference), not in the Map. With the array put in the Map, `.size()` still fails ("cannot find symbol: method size()", verified).
  @Test
  @KnownCompatibility("array-size-method")
  public void testMagicArraySize() {
    assertEquals(5, test("stringArray.size()"));
  }

  // Category not isolated: observed error is "Unsolved symbol in intArray" because `intArray` is a field of the MVEL2 root object (see
  // this-reference), not in the Map. With the array put in the Map, `.size()` still fails ("cannot find symbol: method size()", verified).
  @Test
  @KnownCompatibility("array-size-method")
  public void testMagicArraySize2() {
    assertEquals(5, test("intArray.size()"));
  }

  @Test
  public void testObjectCreation() {
    assertEquals(6, test("new Integer( 6 )"));
  }

  @Test
  public void testCompileTimeLiteralReduction() {
    assertEquals(1000, test("10 * 100"));
  }

  // Category not isolated: observed error is "A reference was unexpectedly null" from the undeclared assignment `abc = 'foo'` (block-syntax)
  // and the single-quoted literal. With `var abc = "foo"; return abc[1];` the error is "cannot find symbol: method get(int) ... String"
  // (verified), so `abc[1]` is unsupported even after fixing those.
  @Test
  @KnownCompatibility("string-index-access")
  public void testStringAsCollection() {
    assertEquals('o', test("abc = 'foo'; abc[1]"));
  }

  // Category not isolated: the root object `Foo` is passed to `executeExpression`; the POJO-root path fails with an NPE inside JavaParser
  // ("method" is null). Without a root object, `foo.collectionTest.size()` evaluates fine against the Map (verified). Whether the adapter's
  // POJO-root emulation or MVEL3 is responsible was not isolated.
  @Test
  @KnownCompatibility("this-reference")
  public void testInterfaceResolution() {
    Serializable ex = compileExpression("foo.collectionTest.size()");

    Map map = createTestMap();
    Foo foo = (Foo) map.get("foo");
    foo.setCollectionTest(new HashSet());
    Object result1 = executeExpression(ex, foo, map);

    foo.setCollectionTest(new ArrayList());
    Object result2 = executeExpression(ex, foo, map);

    assertEquals(result1, result2);
  }

  // Category not isolated: fails with "missing ';' at '}'"; with an explicit `return` before the last statement it passes (verified).
  @Test
  @KnownCompatibility("block-syntax")
  public void testReflectionCache() {
    assertEquals("happyBar", test("foo.happy(); foo.bar.happy()"));
  }

  // Category not isolated: `name` is a property of the root object (`Foo`); observed "cannot find symbol: variable name".
  @Test
  @KnownCompatibility("this-reference")
  public void testDynamicDeop() {
    Serializable s = compileExpression("name");

    assertEquals("dog", executeExpression(s, new Foo()));
    assertEquals("dog", executeExpression(s, new Foo().getBar()));
  }

  // Category not isolated: observed error is "A reference was unexpectedly null"; the expression also uses a single-quoted literal and a
  // trailing `mp.test` without `return` (block-syntax). With double quotes and `return`, the error is "The node has neither a public field or
  // a getter method for : mp.test" (verified), so assigning to a `Map` key through dot notation is unsupported.
  @Test
  @KnownCompatibility("map-property-assignment")
  public void testVirtProperty() {
    Map<String, Object> testMap = new HashMap<String, Object>();
    testMap.put("test", "foo");

    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("mp", testMap);

    assertEquals("bar", executeExpression(compileExpression("mp.test = 'bar'; mp.test"), vars));
  }

  // Has no assertion; passes when evaluation does not throw.
  @Test
  public void testNullSafe() {
    Map<String, Map<String, Float>> ctx = new HashMap<String, Map<String, Float>>();
    Map<String, Float> tmp = new HashMap<String, Float>();
    //   tmp.put("latitude", 0.5f);
    ctx.put("SessionSetupRequest", tmp);

    // MVEL.getProperty(expr, ctx) -> eval(expr, ctx)
    System.out.println("Result = " + eval("SessionSetupRequest.?latitude", ctx));
  }

  public static class A226 {
    Map<String, Object> map = null;

    public Map<String, Object> getMap() {
      return map;
    }
  }

  // Category not isolated: `a.?map['foo']` evaluates to the `A226` instance itself instead of `null`. The same happens with double quotes
  // (verified), so the single-quoted literal is not the cause.
  @Test
  @KnownCompatibility("null-safe-operator")
  public void testMVEL226() {
    A226 a = new A226();
    Map m = Collections.singletonMap("a", a);
    Map<String, Object> nestMap = Collections.<String, Object>singletonMap("foo", "bar");
    String ex = "a.?map['foo']";
    Serializable s;

    // MVEL.getProperty(expr, m) -> eval(expr, m)
    assertNull(eval(ex, m));

    // OptimizerFactory.setDefaultOptimizer("ASM") removed (MVEL2 optimizer)
    s = compileExpression(ex);
    assertNull(executeExpression(s, m));
    a.map = nestMap;
    assertEquals("bar", executeExpression(s, m));
    a.map = null;

    // OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC) removed (MVEL2 optimizer)

    s = compileExpression(ex);
    assertNull(executeExpression(s, m));
    a.map = nestMap;
    assertEquals("bar", executeExpression(s, m));
  }

  // MVEL2 throws CompileException; MVEL3 has no such type, so any RuntimeException is accepted.
  private void infiniteLoop() {
    try {
      Serializable compiled = compileExpression("a['b']['c']");
      Map<String, Object> vars = Collections.singletonMap("a", (Object) Collections.emptyMap());
      executeExpression(compiled, vars);
      fail("expected exception");
    } catch (RuntimeException t) {
    }
  }

  // Passes for a different reason than in MVEL2: `a['b']['c']` on `Collections.emptyMap()` fails javac ("EmptyMap has private access"), not at runtime.
  @Test
  public void testInfiniteLoopWithASMOptimizer() {
    // OptimizerFactory.setDefaultOptimizer("ASM") removed (MVEL2 optimizer)
    infiniteLoop();
  }

  // Passes for a different reason than in MVEL2: see testInfiniteLoopWithASMOptimizer.
  @Test
  public void testInfiniteLoopWithASMOptimizerAO() {
    // MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING / OptimizerFactory removed (MVEL2 only)
    infiniteLoop();
  }

  // Category not isolated: `a.map['foo']` throws no exception (silent result). With double quotes it throws an NPE as the test expects
  // (verified).
  @Test
  @KnownCompatibility("single-quote-string")
  public void testInfiniteLoop() {
    A226 a = new A226();
    Map m = Collections.singletonMap("a", a);
    String ex = "a.map['foo']";

    try {
      // MVEL.getProperty(expr, m) -> eval(expr, m)
      eval(ex, m);
      fail("access to a null field must fail");
    } catch (Exception e) {
      // ignore
    }
  }

  // Category not isolated: same as testInfiniteLoop.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testInfiniteLoopAO() {
    // MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING removed (MVEL2 only)
    testInfiniteLoop();
  }

  // Category not isolated: observed parse error "mismatched input '<EOF>'". The expression also assigns `map.foo = 'bar'` on a `Hashtable`
  // (see map-property-assignment) with a single-quoted literal. Not tested separately.
  @Test
  @KnownCompatibility("block-syntax")
  public void testNonHashMapImplMapPutMVEL302() {
    test("map=new java.util.Hashtable();map.foo='bar'");
  }

  // Category not isolated: `this.?bar.name` with root `Foo`; the result is the generated evaluator instance, not `null`. Whether `.?` (see
  // null-safe-operator) also contributes was not tested separately.
  @Test
  @KnownCompatibility("this-reference")
  public void testNullSafeWithDynamicOptimizerMVEL305() {
    Foo foo = new Foo();
    foo.setBar(null);
    // OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC) removed (MVEL2 optimizer)
    Serializable s = compileExpression("this.?bar.name");
    // Iterate 100 times to ensure JIT ASM kicks in
    for (int i = 1; i < 100; i++) {
      assertNull(executeExpression(s, foo));
    }
  }

  // Category not isolated: observed `ClassCastException: Collections$SingletonList cannot be cast to String[]`: the adapter compiles on first
  // evaluation with the runtime type of the first input (`String[]`), and MVEL3 generated code is bound to it. Whether MVEL3 itself rejects a
  // different type for a declared `Object` input was not tested.
  @Test
  @KnownCompatibility("static-input-type")
  public void testStaleReflectiveCollectionAccessor() {
    // OptimizerFactory.setDefaultOptimizer(SAFE_REFLECTIVE / DYNAMIC) removed (MVEL2 optimizer)
    Serializable getFooExpression = compileExpression("foo[0]");
    Map vars = new HashMap();

    // Array -> List
    vars.put("foo", new String[]{"1", "2", "3"});
    assertEquals("1", executeExpression(getFooExpression, vars));
    vars.put("foo", Collections.singletonList("1"));
    assertEquals("1", executeExpression(getFooExpression, vars));

    // List -> Array
    vars.put("foo", new String[]{"1", "2", "3"});
    assertEquals("1", executeExpression(getFooExpression, vars));
  }

  public final class Data {

    private final Collection<Object> list;

    Data(List<Object> list) {
      this.list = list;
    }

    public Collection<Object> getList() {
      return list;
    }
  }

  // Category not isolated: `list.empty` against root `Data`; observed "cannot find symbol: variable list" from the POJO-root path. `Data` is
  // also a non-static inner class. Not tested separately.
  @Test
  @KnownCompatibility("this-reference")
  public void testStaleReflectiveCollectionIsEmptyAccessor() {
    // OptimizerFactory.setDefaultOptimizer(SAFE_REFLECTIVE / DYNAMIC) removed (MVEL2 optimizer)
    Serializable getFooExpression = compileExpression("list.empty");
    Map vars = new HashMap();

    // ArrayList -> Colletions.EmptyList
    assertEquals(true, executeExpression(getFooExpression, new Data(new ArrayList<Object>())));
    assertEquals(true, executeExpression(getFooExpression, new Data(Collections.emptyList())));

    // Colletions.EmptyList -> ArrayList
    assertEquals(true, executeExpression(getFooExpression, new Data(Collections.emptyList())));
    assertEquals(true, executeExpression(getFooExpression, new Data(new ArrayList<Object>())));
  }

  @Test
  @KnownCompatibility("foreach-syntax")
  public void testMVEL308() {
    String expression = "foreach(field: updates.entrySet()) { ctx._target[field.getKey()] = field.value; }";
    Serializable compiled = compileExpression(expression);

    Map<String, Object> target = new HashMap<String, Object>();
    target.put("value", "notnull");

    Map<String, Object> ctx = new HashMap<String, Object>();
    ctx.put("_target", target);

    Map<String, Object> updates = new HashMap<String, Object>();
    updates.put("value", null);

    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("updates", updates);
    vars.put("ctx", ctx);

    for (int i = 0; i < 100; i++) {
      executeExpression(compiled, vars);
    }

    assertNull(target.get("value"));
  }

  // Category not isolated: the first assertion fails with a parse error from `'test1'`. With double quotes, `["test1" : null].test1`,
  // `[null][0]` and `{null}[0]` fail with an NPE / "Cannot be cast or coerced: null" (null elements in list/map literals), and `a=[null]`
  // fails with `input-variable-redeclaration` (`a` is a Map input). Later assertions were not reached.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testNullListMapArrayValueMVEL312() {
    // Map
    assertNull(runSingleTest("['test1' : null].test1"));
    assertNull(runSingleTest("['test1' : null].get('test1')"));
    assertNull(runSingleTest("a=['test1' : null];a.test1"));
    assertNull(runSingleTest("a=['test1' : null];a.get('test1')"));

    // List
    assertNull(runSingleTest("[null][0]"));
    assertNull(runSingleTest("[null].get(0)"));
    assertNull(runSingleTest("a=[null];a[0]"));
    assertNull(runSingleTest("a=[null];a.get(0)"));

    // Array
    assertNull(runSingleTest("{null}[0]"));
    assertNull(runSingleTest("a={null};a[0]"));
  }

  @Test
  public void testPublicStaticFieldMVEL314() {
    assertEquals(Foo.STATIC_BAR, runSingleTest("org.mvel3.compat.mvel2.res.Foo.STATIC_BAR"));
  }

  /*
   * Test fix for https://github.com/mvel/mvel/issues/200
   */
  @Test
  public void testCollectionEmptyASM() {
    Serializable compiled = compileExpression("validationIssues.empty");
    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("validationIssues", new ArrayList<Object>());
    int trueCount = 0;
    final int COUNT = 1000;
    for (int i = 0; i < COUNT; i++) {
      // MVEL.executeExpression(compiled, properties, Boolean.class) -> cast
      if ((Boolean) executeExpression(compiled, properties)) {
        trueCount++;
      }
    }
    assertEquals(COUNT, trueCount);
  }

  public class Service {
    private void hello() {
      System.out.println("hello world");
    }
  }

  // MVEL2 throws PropertyAccessException; MVEL3 has no such type, so any RuntimeException is accepted
  // and the original message check ("Error: unable to resolve method") is retained.
  // Category not isolated: MVEL3 reports the private access as a javac error ("hello() has private access") wrapped in
  // `KieMemoryCompilerException`; the original assertion on the MVEL2 message "Error: unable to resolve method" fails with `AssertionError`.
  @Test
  @KnownCompatibility("exception-type-mismatch")
  public void testPrivateMethod() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("service", new Service());
    try {
      eval("service.hello()", vars);
      fail("Should have thrown a PropertyAccessException");
    } catch (RuntimeException pae) {
      Assert.assertTrue(pae.getMessage().contains("Error: unable to resolve method"));
    }
  }

  // Category not isolated: same as testPrivateMethod.
  @Test
  @KnownCompatibility("exception-type-mismatch")
  public void testPrivateMethodCompiled() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("service", new Service());
    Serializable expr = compileExpression("service.hello()");
    try {
      executeExpression(expr, vars);
      fail("Should have thrown a PropertyAccessException");
    } catch (RuntimeException pae) {
      Assert.assertTrue(pae.getMessage().contains("Error: unable to resolve method"));
    }
  }

  protected static Object runSingleTest(final String ex) {
    return Mvel3TestSupport.eval(ex, createTestMap());
  }
}
