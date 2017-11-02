package org.mvel2.tests.core;

import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.PropertyHandlerFactory;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.tests.core.res.Base;
import org.mvel2.tests.core.res.Cake;
import org.mvel2.tests.core.res.Foo;

import java.io.Serializable;
import java.util.*;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeExpression;


public class PropertyAccessTests extends AbstractTest {
  protected void setUp() throws Exception {
    super.setUp();
    // Ensure MVEL settings are not leaked across tests
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = false;
    OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);
  }

  public void testSingleProperty() {
    assertEquals(false, test("fun"));
  }

  public void testMethodOnValue() {
    assertEquals("DOG", test("foo.bar.name.toUpperCase()"));
  }

  public void testMethodOnValue2() {
    assertEquals("DOG", test("foo. bar. name.toUpperCase()"));
  }

  public void testSimpleProperty() {
    assertEquals("dog", test("foo.bar.name"));
  }

  public void testSimpleProperty2() {
    assertEquals("cat", test("DATA"));
  }

  public void testPropertyViaDerivedClass() {
    assertEquals("cat", test("derived.data"));
  }

  public void testThroughInterface() {
    assertEquals("FOOBAR!", test("testImpl.name"));
  }

  public void testThroughInterface2() {
    assertEquals(true, test("testImpl.foo"));
  }

  public void testMapAccessWithMethodCall() {
    assertEquals("happyBar", test("funMap['foo'].happy()"));
  }

  public void testUninitializedInt() {
    assertEquals(0, test("sarahl"));
  }

  public void testMethodAccess() {
    assertEquals("happyBar", test("foo.happy()"));
  }

  public void testMethodAccess2() {
    assertEquals("FUBAR", test("foo.toUC( 'fubar' )"));
  }

  public void testMethodAccess3() {
    assertEquals(true, test("equalityCheck(c, 'cat')"));
  }

  public void testMethodAccess4() {
    assertEquals(null, test("readBack(null)"));
  }

  public void testMethodAccess5() {
    assertEquals("nulltest", test("appendTwoStrings(null, 'test')"));
  }

  public void testMethodAccess6() {
    assertEquals(true, test("   equalityCheck(   c  \n  ,   \n   'cat'      )   "));
  }

  public void testLiteralPassThrough() {
    assertEquals(true, test("true"));
  }

  public void testLiteralPassThrough2() {
    assertEquals(false, test("false"));
  }

  public void testLiteralPassThrough3() {
    assertEquals(null, test("null"));
  }

  public void testLiteralReduction1() {
    assertEquals("foo", test("null or 'foo'"));
  }

  public void testStrAppend() {
    assertEquals("foobarcar", test("'foo' + 'bar' + 'car'"));
  }

  public void testStrAppend2() {
    assertEquals("foobarcar1", test("'foobar' + 'car' + 1"));
  }

  public void testMapAccess() {
    assertEquals("dog", test("funMap['foo'].bar.name"));
  }

  public void testMapAccess2() {
    assertEquals("dog", test("funMap.foo.bar.name"));
  }

  public void testStaticMethodFromLiteral() {
    assertEquals(String.class.getName(), test("String.valueOf(Class.forName('java.lang.String').getName())"));
  }

  public void testObjectInstantiation() {
    test("new java.lang.String('foobie')");
  }

  public void testObjectInstantiationWithMethodCall() {
    assertEquals("FOOBIE", test("new String('foobie')  . toUpperCase()"));
  }

  public void testObjectInstantiation2() {
    test("new String() is String");
  }

  public void testObjectInstantiation3() {
    test("new java.text.SimpleDateFormat('yyyy').format(new java.util.Date(System.currentTimeMillis()))");
  }

  public void testThisReference() {
    assertEquals(true, test("this") instanceof Base);
  }

  public void testThisReference2() {
    assertEquals(true, test("this.funMap") instanceof Map);
  }

  public void testThisReferenceInMethodCall() {
    assertEquals(101, test("Integer.parseInt(this.number)"));
  }

  public void testThisReferenceInConstructor() {
    assertEquals("101", test("new String(this.number)"));
  }

  public void testStringEscaping() {
    assertEquals("\"Mike Brock\"", test("\"\\\"Mike Brock\\\"\""));
  }

  public void testStringEscaping2() {
    assertEquals("MVEL's Parser is Fast", test("'MVEL\\'s Parser is Fast'"));
  }

  public void testCompiledMethodCall() {
    assertEquals(String.class, executeExpression(compileExpression("c.getClass()"), new Base(), createTestMap()));
  }

  public void testStaticNamespaceCall() {
    assertEquals(java.util.ArrayList.class, test("java.util.ArrayList"));
  }

  public void testStaticNamespaceClassWithMethod() {
    assertEquals("FooBar", test("java.lang.String.valueOf('FooBar')"));
  }


  public void testStaticNamespaceClassWithField() {
    assertEquals(Integer.MAX_VALUE, test("java.lang.Integer.MAX_VALUE"));
  }

  public void testStaticNamespaceClassWithField2() {
    assertEquals(Integer.MAX_VALUE, test("Integer.MAX_VALUE"));
  }

  public void testStaticFieldAsMethodParm() {
    assertEquals(String.valueOf(Integer.MAX_VALUE), test("String.valueOf(Integer.MAX_VALUE)"));
  }

  public void testMagicArraySize() {
    assertEquals(5, test("stringArray.size()"));
  }

  public void testMagicArraySize2() {
    assertEquals(5, test("intArray.size()"));
  }

  public void testObjectCreation() {
    assertEquals(6, test("new Integer( 6 )"));
  }

  public void testCompileTimeLiteralReduction() {
    assertEquals(1000, test("10 * 100"));
  }

  public void testStringAsCollection() {
    assertEquals('o', test("abc = 'foo'; abc[1]"));
  }

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

  public void testReflectionCache() {
    assertEquals("happyBar", test("foo.happy(); foo.bar.happy()"));
  }

  public void testDynamicDeop() {
    Serializable s = compileExpression("name");

    assertEquals("dog", executeExpression(s, new Foo()));
    assertEquals("dog", executeExpression(s, new Foo().getBar()));
  }

  public void testVirtProperty() {
    Map<String, Object> testMap = new HashMap<String, Object>();
    testMap.put("test", "foo");

    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("mp", testMap);

    assertEquals("bar", executeExpression(compileExpression("mp.test = 'bar'; mp.test"), vars));
  }


  public void testBindingCoercion() {
    List list = new LinkedList();
    list.add("Apple");
    list.add("Peach");
    list.add("Icing");

    Cake cake = new Cake();

    MVEL.setProperty(cake, "ingredients", list);

    assertTrue(cake.getIngredients().contains("Apple"));
    assertTrue(cake.getIngredients().contains("Peach"));
    assertTrue(cake.getIngredients().contains("Icing"));
  }

  public void testMVELCompilerBoth() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
    PropertyHandlerFactory.registerPropertyHandler(DynaBean.class, new DynaBeanPropertyHandler());

    TestBean bean = new TestBean("value1");

    Map<String, Object> vars = new LinkedHashMap<String, Object>();
    vars.put("attr", bean);

    ParserContext parserContext = new ParserContext();
    Object compiled = MVEL.compileExpression("attr.value", parserContext);

    assertEquals("value1", MVEL.executeExpression(compiled, null, vars));

    DynaBean dyna = new LazyDynaBean();
    dyna.set("value", "value2");

    vars.put("attr", dyna);

    assertEquals("value2", MVEL.executeExpression(compiled, null, vars));
  }

  public void testMVELCompilerBoth2() {
    PropertyHandlerFactory.registerPropertyHandler(DynaBean.class, new DynaBeanPropertyHandler());

    Map<String, Object> vars = new LinkedHashMap<String, Object>();

    ParserContext parserContext = new ParserContext();
    Object compiled = MVEL.compileExpression("attr.value", parserContext);

    DynaBean dyna = new LazyDynaBean();
    dyna.set("value", "value2");
    vars.put("attr", dyna);
    assertEquals("value2", MVEL.executeExpression(compiled, null, vars));

    TestBean bean = new TestBean("value1");
    vars.put("attr", bean);
    assertEquals("value1", MVEL.executeExpression(compiled, null, vars));

  }

  public static class TestBean {
    private String _value;

    public TestBean(String value) {
      _value = value;
    }

    public String getValue() {
      return _value;
    }

    public void setValue(String value) {
      _value = value;
    }
  }

  public static interface DynaBean {
    public void set(String key, Object value);

    public Object get(String key);
  }

  public static class LazyDynaBean implements DynaBean {
    private Map<String, Object> values = new HashMap<String, Object>();

    public void set(String key, Object value) {
      values.put(key, value);
    }

    public Object get(String key) {
      return values.get(key);
    }
  }

  private static class DynaBeanPropertyHandler implements PropertyHandler {
    public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory) {
      return ((DynaBean) contextObj).get(name);
    }

    public Object setProperty(String name, Object contextObj, VariableResolverFactory variableFactory, Object value) {
      ((DynaBean) contextObj).set(name, value);
      return value;
    }
  }

  public void testNullSafe() {
    Map<String, Map<String, Float>> ctx = new HashMap<String, Map<String, Float>>();
    Map<String, Float> tmp = new HashMap<String, Float>();
    //   tmp.put("latitude", 0.5f);
    ctx.put("SessionSetupRequest", tmp);

    System.out.println("Result = " + MVEL.getProperty("SessionSetupRequest.?latitude", ctx));
  }

  public static class A226 {
    Map<String, Object> map = null;

    public Map<String, Object> getMap() {
      return map;
    }
  }

  public void testMVEL226() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
    A226 a = new A226();
    Map m = Collections.singletonMap("a", a);
    Map<String, Object> nestMap = Collections.<String, Object>singletonMap("foo", "bar");
    String ex = "a.?map['foo']";
    Serializable s;

    assertNull(MVEL.getProperty(ex, m));

    OptimizerFactory.setDefaultOptimizer("ASM");
    s = MVEL.compileExpression(ex);
    assertNull(MVEL.executeExpression(s, m));
    a.map = nestMap;
    assertEquals("bar", MVEL.executeExpression(s, m));
    a.map = null;

    OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);

    s = MVEL.compileExpression(ex);
    assertNull(MVEL.executeExpression(s, m));
    a.map = nestMap;
    assertEquals("bar", MVEL.executeExpression(s, m));
  }
  
  private void infiniteLoop() {
    try {
      Serializable compiled = MVEL.compileExpression("a['b']['c']");
      Map<String, Object> vars = Collections.singletonMap("a", (Object)Collections.emptyMap());
      MVEL.executeExpression(compiled, vars);
      fail("expected exception");
    } catch(CompileException t) {
    }
  }

  public void testInfiniteLoopWithASMOptimizer() {
    OptimizerFactory.setDefaultOptimizer("ASM");
    infiniteLoop();
  }

  public void testInfiniteLoopWithASMOptimizerAO() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
    OptimizerFactory.setDefaultOptimizer("ASM");
    infiniteLoop();
  }

  public void testInfiniteLoop() {
    A226 a = new A226();
    Map m = Collections.singletonMap("a", a);
    String ex = "a.map['foo']";

    try {
      MVEL.getProperty(ex, m);
      fail("access to a null field must fail");
    } catch (Exception e) {
      // ignore
    }
  }
  
  public void testInfiniteLoopAO() {
    MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
    testInfiniteLoop();
  }

  public void testNonHashMapImplMapPutMVEL302() {
    test("map=new java.util.Hashtable();map.foo='bar'");
  }

  public void testNullSafeWithDynamicOptimizerMVEL305() {
    Foo foo = new Foo();
    foo.setBar(null);
    OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);
    Serializable s = MVEL.compileExpression("this.?bar.name");
    // Iterate 100 times to ensure JIT ASM kicks in
    for (int i = 1; i < 100; i++) {
        assertNull(MVEL.executeExpression(s, foo));
    }
  }

  public void testStaleReflectiveCollectionAccessor() {
    try
    {
      OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
      Serializable getFooExpression = MVEL.compileExpression("foo[0]");
      Map vars = new HashMap();

       // Array -> List
      vars.put("foo", new String[]{"1", "2", "3"});
      assertEquals("1", MVEL.executeExpression(getFooExpression, vars));
      vars.put("foo", Collections.singletonList("1"));
      assertEquals("1", MVEL.executeExpression(getFooExpression, vars));
            
      // List -> Array
      vars.put("foo", new String[]{"1", "2", "3"});
      assertEquals("1", MVEL.executeExpression(getFooExpression, vars));
      OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);
    }
    finally
    {
      OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);
    }
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

  public void testStaleReflectiveCollectionIsEmptyAccessor() {
    try
    {
      OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
      Serializable getFooExpression = MVEL.compileExpression("list.empty");
      Map vars = new HashMap();

      // ArrayList -> Colletions.EmptyList
      assertEquals(true, MVEL.executeExpression(getFooExpression, new Data(new ArrayList<Object>())));
      assertEquals(true, MVEL.executeExpression(getFooExpression, new Data(Collections.emptyList())));

      // Colletions.EmptyList -> ArrayList
      assertEquals(true, MVEL.executeExpression(getFooExpression, new Data(Collections.emptyList())));
      assertEquals(true, MVEL.executeExpression(getFooExpression, new Data(new ArrayList<Object>())));
      OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);
    }
    finally
    {
      OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);
    }
  }

  public void testMVEL308() {
    String expression = "foreach(field: updates.entrySet()) { ctx._target[field.key] = field.value; }";
    Serializable compiled = MVEL.compileExpression(expression);

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
        MVEL.executeExpression(compiled, vars);
    }

    assertNull(target.get("value"));
  }

  public void testNullListMapArrayValueMVEL312(){
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

  public void testPublicStaticFieldMVEL314(){
    assertEquals(Foo.STATIC_BAR, runSingleTest("org.mvel2.tests.core.res.Foo.STATIC_BAR"));
  }
}
