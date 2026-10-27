package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Foo;
import org.mvel3.compat.mvel2.res.User;

import static java.util.Collections.unmodifiableCollection;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.ExpressionCompiler;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileSetExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeSetExpression;

/**
 * Imported from the collection and map tests of org.mvel2.tests.core.CoreConfidenceTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged where MVEL3 has an equivalent. Failures are intentionally visible;
 * known behavioural differences are annotated with {@link KnownCompatibility}.
 *
 * <p>Not imported, because they use MVEL2-only APIs: {@code testMapBindingSemantics}
 * ({@code MVEL.setProperty}), {@code testSetSemantics} ({@code MVEL.getProperty} on a bean, covered by
 * {@code PropertyAccessTests}), {@code testAddIntToMapWithMapSyntax} and
 * {@code testUpdateIntInMapWithMapSyntax} ({@code PropertyAccessor.set}), {@code testMapsWithVariableAsKey}
 * (asserts on {@code ExpressionCompiler.getParserContextState().getInputs()}), {@code testNullCollection}
 * ({@code IndexedVariableResolverFactory}) and {@code testParseGenericMap} ({@code MVEL.analyze}).
 * {@code testMapAccessWithNestedPropertyAO} and {@code testMapAccessWithNestedPropertyAO_ASM} only differ from
 * {@code testMapAccessWithNestedProperty} by the MVEL2 optimizer flags
 * ({@code COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING}, ASM) and are imported without them.
 *
 * <p>Adaptations: {@code ParserContext} becomes {@link TestCompilerContext}; the root object is passed as the
 * input {@code this}; strong typing flags and optimizer selection are removed; {@code Person} and {@code POJO}
 * are public static nested classes. {@code MVEL.compileSetExpression}/{@code executeSetExpression} use the
 * adapter's set-expression emulation. The adapter's {@code compileExpression} is lazy and does not compile, so
 * the tests that only compile an expression use {@link #compileEagerly}, which compiles at once.
 */
@Tag("mvel2-compatibility")
public class CoreCollectionsAndMapsTests extends AbstractTest {

  /** Compiles at once (the adapter's compileExpression only creates a lazy carrier). */
  private static void compileEagerly(final String expression, final TestCompilerContext ctx) {
    final Map<String, Type<?>> types = new LinkedHashMap<>();
    ctx.getInputs().forEach((name, type) -> types.put(name, Type.type(type)));
    final Set<String> imports = new LinkedHashSet<>();
    ctx.getImports().forEach(type -> imports.add(type.getCanonicalName()));
    if (expression.indexOf(';') > 0) {
      new MVEL().compileMapBlock(expression, Object.class, imports, types);
    } else {
      new MVEL().compileMapExpression(expression, Object.class, imports, types);
    }
  }

  public static class Person {
    private String name;
    private int age;
    private String likes;
    private List<Foo> footributes;
    private Map<String, Foo> maptributes;
    private Map<Object, Foo> objectKeyMaptributes;

    public Person() {
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }

    public List<Foo> getFootributes() {
      return footributes;
    }

    public void setFootributes(List<Foo> footributes) {
      this.footributes = footributes;
    }

    public Map<String, Foo> getMaptributes() {
      return maptributes;
    }

    public void setMaptributes(Map<String, Foo> maptributes) {
      this.maptributes = maptributes;
    }

    public Map<Object, Foo> getObjectKeyMaptributes() {
      return objectKeyMaptributes;
    }

    public void setObjectKeyMaptributes(Map<Object, Foo> objectKeyMaptributes) {
      this.objectKeyMaptributes = objectKeyMaptributes;
    }
  }

  public static class POJO {
    private Set<Date> dates = new HashSet<Date>();
    private Map<String, Object> map = new HashMap<String, Object>();

    public POJO() {
      dates.add(new Date());
    }

    public Set<Date> getDates() {
      return dates;
    }

    public void setDates(Set<Date> dates) {
      this.dates = dates;
    }

    public String function(long num) {
      return String.valueOf(num);
    }

    public String aMethod(long num) {
      return String.valueOf(num);
    }

    public Map<String, Object> getMap() {
      return map;
    }

    public void setMap(Map<String, Object> map) {
      this.map = map;
    }

    public String getKey() {
      return "1";
    }
  }

  public static class Triangle {
    public static enum Foo {
      INCOMPLETE, UNCLASSIFIED,
      EQUILATERAL, ISOSCELES, RECTANGLED, ISOSCELES_RECTANGLED, ACUTE, OBTUSE;
    }

    private List<Map<String, Object>> deliveries = new ArrayList<Map<String, Object>>();

    public List<Map<String, Object>> getDeliveries() {
      return deliveries;
    }
  }

  // Category not isolated: fails with a JavaParser NPE ("HasParentNode.setParentNode") on the single-quoted strings. The same
  // expression has other causes that were not tested one by one: `foreach` (foreach-syntax; `foreach (item : set) {..}` alone fails
  // with "A reference was unexpectedly null") and the undeclared variables `set` / `output` (block-syntax).
  @Test
  @KnownCompatibility("single-quote-string")
  public void testCreationOfSet() {
    assertEquals("foo bar foo bar",
        test("set = new java.util.LinkedHashSet(); "
            + "set.add('foo');" + "set.add('bar');"
            + "output = '';" + "foreach (item : set) {"
            + "output = output + item + ' ';"
            + "} "
            + "foreach (item : set) {"
            + "output = output + item + ' ';"
            + "} " + "output = output.trim();"
            + "if (set.size() == 2) { return output; }"));

  }

  // Category not isolated: fails with a JavaParser NPE ("HasParentNode.setParentNode") on the single-quoted strings. The undeclared
  // variables `l` / `poo` and the trailing `poo.size();` without `return` (block-syntax) were not tested separately.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testCreationOfList() {
    assertEquals(5,
        test("l = new java.util.LinkedList(); l.add('fun'); l.add('happy'); l.add('fun'); l.add('slide');"
            + "l.add('crap'); poo = new java.util.ArrayList(l); poo.size();"));
  }

  // Category not isolated: fails with a JavaParser NPE ("HasParentNode.setParentNode") on the single-quoted strings. Other likely
  // causes, not tested separately: undeclared variables `l` / `m` (block-syntax) and `m.foo[1]` on a raw HashMap, whose value is
  // `Object` (dynamic-property-type; `get(int)` is not found).
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMapOperations() {
    assertEquals("poo5",
        test("l = new java.util.ArrayList(); l.add('plop'); l.add('poo'); m = new java.util.HashMap();"
            + "m.put('foo', l); m.put('cah', 'mah'); m.put('bar', 'foo'); m.put('sarah', 'mike');"
            + "m.put('edgar', 'poe'); if (m.edgar == 'poe') { return m.foo[1] + m.size(); }"));
  }

  // Category not isolated: fails with "Unsolved symbol in stk" (undeclared variable, block-syntax). With `var stk` declared, the
  // trailing `stk.pop() + stk.pop();` fails with "bad operand types Object + Object" (the popped values are `Object`, as in
  // dynamic-property-type); a cast to `(Integer)` works. In a scratch test `new java.util.Stack<Integer>()` inside a block also
  // failed with "cannot find symbol: class java"; this was not investigated further.
  @Test
  @KnownCompatibility("block-syntax")
  public void testStackOperations() {
    assertEquals(10,
        test("stk = new java.util.Stack();" + "stk.push(5);" + "stk.push(5);" + "stk.pop() + stk.pop();"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null" on the single-quoted strings. The undeclared variable `map`
  // (block-syntax) was not tested separately; `map["test"] = "foo"` with double quotes parses.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMapPropertyCreateCondensed() {
    assertEquals("foo",
        test("map = new java.util.HashMap(); map['test'] = 'foo'; map['test'];"));
  }

  // Category not isolated: fails with a JavaParser NPE ("HasParentNode.setParentNode") on the single-quoted strings. The undeclared
  // variable `a` and the trailing `a[1]` without `return` (block-syntax) were not tested separately.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testListAccessorAssign() {
    String ex = "a = new java.util.ArrayList(); a.add('foo'); a.add('BAR'); a[1] = 'bar'; a[1]";

    // OptimizerFactory.setDefaultOptimizer("ASM") is removed (MVEL2 optimizer selection).

    Serializable s = compileExpression(ex);
    assertEquals("bar", executeExpression(s, new HashMap()));

    // OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC) is removed (MVEL2 optimizer selection).

    assertEquals("bar",
        test("a = new java.util.ArrayList(); a.add('foo'); a.add('BAR'); a[1] = 'bar'; a[1]"));
  }

  // `innermap['test']` is read as a `char` literal index and the expression returns the whole map ({test=foo}); with double quotes
  // it works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMapAccessSemantics() {
    Map<String, Object> outermap = new HashMap<String, Object>();
    Map<String, Object> innermap = new HashMap<String, Object>();

    innermap.put("test",
        "foo");
    outermap.put("innermap",
        innermap);

    assertEquals("foo",
        testCompiledSimple("innermap['test']",
            outermap,
            null));
  }

  // Category not isolated: fails with a JavaParser NPE ("HasParentNode.setParentNode") on the single-quoted strings. The undeclared
  // variable `users` (block-syntax) is a second cause; `var users = ["darth" : new User(..)]; [users["darth"], ..]` works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMapNestedInsideList() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addImport(User.class);

    ExpressionCompiler compiler =
        new ExpressionCompiler("users = [ 'darth'  : new User('Darth', 'Vadar')," +
            "\n'bobba' : new User('Bobba', 'Feta') ]; [ users.get('darth'), users.get('bobba') ]", ctx);
    List list = (List) executeExpression(compiler.compile(),
        new HashMap());
    User user = (User) list.get(0);
    assertEquals("Darth",
        user.getFirstName());
    user = (User) list.get(1);
    assertEquals("Bobba",
        user.getFirstName());

    compiler =
        new ExpressionCompiler("users = [ 'darth'  : new User('Darth', 'Vadar')," +
            "\n'bobba' : new User('Bobba', 'Feta') ]; [ users['darth'], users['bobba'] ]", ctx);
    list = (List) executeExpression(compiler.compile(),
        new HashMap());
    user = (User) list.get(0);
    assertEquals("Darth",
        user.getFirstName());
    user = (User) list.get(1);
    assertEquals("Bobba",
        user.getFirstName());
  }

  // Category not isolated: fails with "A reference was unexpectedly null" on the single-quoted strings. The undeclared variable
  // `users` (block-syntax) is a second cause; `var users = [new User(..), ..]; [users[0], users[1]]` with double quotes works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testListNestedInsideList() {
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addImport(User.class);

    ExpressionCompiler compiler =
        new ExpressionCompiler("users = [ new User('Darth', 'Vadar'), " +
            "new User('Bobba', 'Feta') ]; [ users.get( 0 ), users.get( 1 ) ]", ctx);
    List list = (List) executeExpression(compiler.compile(),
        new HashMap());
    User user = (User) list.get(0);
    assertEquals("Darth",
        user.getFirstName());
    user = (User) list.get(1);
    assertEquals("Bobba",
        user.getFirstName());

    compiler = new ExpressionCompiler("users = [ new User('Darth', 'Vadar'), " +
        "new User('Bobba', 'Feta') ]; [ users[0], users[1] ]", ctx);
    list = (List) executeExpression(compiler.compile(),
        new HashMap());
    user = (User) list.get(0);
    assertEquals("Darth",
        user.getFirstName());
    user = (User) list.get(1);
    assertEquals("Bobba",
        user.getFirstName());
  }

  // Category not isolated: fails with expected:<bar> but was:<{test=foo}>. The adapter emulates a set expression by compiling
  // `innermap['test'] = setValue`; `['test']` is read as a `char` literal, so the assignment is a no-op (single-quote-string). The
  // set-expression-api difference also applies: `innermap["test"] = setValue` with double quotes works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMapBindingSemantics2() {
    // OptimizerFactory.setDefaultOptimizer("ASM") is removed (MVEL2 optimizer selection).
    Map<String, Object> outermap = new HashMap<String, Object>();
    Map<String, Object> innermap = new HashMap<String, Object>();

    innermap.put("test",
        "foo");
    outermap.put("innermap",
        innermap);

    executeSetExpression(compileSetExpression("innermap['test']", new TestCompilerContext()),
        outermap,
        "bar");

    assertEquals("bar",
        testCompiledSimple("innermap['test']",
            outermap,
            null));
  }

  // Category not isolated: fails with "Unsolved symbol in m" (undeclared variables `m` / `l`, block-syntax). With typed declarations or
  // `var` it was not run to the end, so the cast `((ArrayList)m["content"])[0]` and the `Boolean` result of the trailing `list.add(..)`
  // without `return` were not checked.
  @Test
  @KnownCompatibility("block-syntax")
  public void testMapsAndLists() {
    // OptimizerFactory.setDefaultOptimizer("ASM") is removed (MVEL2 optimizer selection).

    TestCompilerContext ctx = new TestCompilerContext();
    // setStrongTyping(true) is removed (MVEL3 has no such mode).
    ctx.addImport(HashMap.class);
    ctx.addImport(ArrayList.class);
    ctx.addInput("list",
        List.class);

    String expression = "m = new HashMap();\n" + "l = new ArrayList();\n" + "l.add(\"first\");\n" +
        "m.put(\"content\", l);\n" + "list.add(((ArrayList)m[\"content\"])[0]);";

    Serializable s = compileExpression(expression,
        ctx);

    Map vars = new HashMap();
    List list = new ArrayList();
    vars.put("list",
        list);

    Boolean result = (Boolean) executeExpression(s,
        vars);

    assertNotNull(result);
    assertTrue(result);
    assertEquals(1,
        list.size());
    assertEquals("first",
        list.get(0));
  }

  // Category not isolated: same as testMapsAndLists ("Unsolved symbol in m"); the cast and the trailing `list.add(..)` result were
  // not checked.
  @Test
  @KnownCompatibility("block-syntax")
  public void testMapsAndLists2() {
    // OptimizerFactory.setDefaultOptimizer("reflective") is removed (MVEL2 optimizer selection).

    TestCompilerContext ctx = new TestCompilerContext();
    // setStrongTyping(true) is removed (MVEL3 has no such mode).
    ctx.addImport(HashMap.class);
    ctx.addImport(ArrayList.class);
    ctx.addInput("list",
        List.class);

    String expression = "m = new HashMap();\n" + "l = new ArrayList();\n" + "l.add(\"first\");\n" +
        "m.put(\"content\", l);\n" + "list.add(((ArrayList)m[\"content\"])[0]);";

    Serializable s = compileExpression(expression,
        ctx);

    Map vars = new HashMap();
    List list = new ArrayList();
    vars.put("list",
        list);

    Boolean result = (Boolean) executeExpression(s,
        vars);

    assertNotNull(result);
    assertTrue(result);
    assertEquals(1,
        list.size());
    assertEquals("first",
        list.get(0));
  }

  // Category not isolated: fails with an internal error ("MethodUsage.getName() ... method is null") with `this` declared as an input
  // and Person as the root. The unqualified `objectKeyMaptributes` is then not resolved against the root (this-reference; the same
  // expression with `this` as a named input fails with "Unsolved symbol in objectKeyMaptributes"). With a named input
  // `person.objectKeyMaptributes[$aPerson] == foo` works.
  @Test
  @KnownCompatibility("this-reference")
  public void testMapsWithVariableAsKey2() {
    String ex = "objectKeyMaptributes[$aPerson] == foo";
    Foo foo = new Foo();
    Person person = new Person();
    person.setObjectKeyMaptributes(new HashMap<Object, Foo>());
    person.getObjectKeyMaptributes().put(person, foo);
    Map<String, Class> inputs = new HashMap<String, Class>();
    inputs.put("this", Person.class);
    inputs.put("foo", Foo.class);
    inputs.put("$aPerson", Person.class);
    TestCompilerContext ctx = new TestCompilerContext();
    // setStrongTyping(true) is removed (MVEL3 has no such mode).
    // ParserContext.setInputs(Map) is replaced by addInput for each entry.
    inputs.forEach(ctx::addInput);

    Serializable expression = compileExpression(ex, ctx);
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("foo", foo);
    variables.put("$aPerson", person);

    Boolean result = (Boolean) executeExpression(expression, person, variables);
    assertTrue(result);
  }

  // Category not isolated: fails with "A reference was unexpectedly null" on `'bar'`. The trailing `map[..];` without `return`
  // (block-syntax) was not tested separately.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMapAssignmentNestedExpression() {
    Map map = new HashMap();
    map.put("map",
        new HashMap());

    String ex = "map[java.lang.Integer.MAX_VALUE] = 'bar'; map[java.lang.Integer.MAX_VALUE];";

    assertEquals("bar",
        executeExpression(compileExpression(ex),
            map));
    assertEquals("bar",
        eval(ex,
            map));
  }

  // Category not isolated: fails with "A reference was unexpectedly null" on the single-quoted strings. The trailing `map['bar'];`
  // without `return` (block-syntax) was not tested separately.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMapAssignmentNestedExpression2() {
    Map map = new HashMap();
    map.put("x",
        "bar");
    map.put("map",
        new HashMap());

    String ex = "map[x] = 'foo'; map['bar'];";
    assertEquals("foo",
        executeExpression(compileExpression(ex),
            map));
    assertEquals("foo",
        eval(ex,
            map));
  }

  // Fails with "Unsolved symbol in objectKeyMaptributes": the root `Person` is not used for unqualified names. With a named input
  // `person.objectKeyMaptributes[Triangle.Foo.OBTUSE]` works.
  @Test
  @KnownCompatibility("this-reference")
  public void testNestedNumInMapKey() {
    String str = "objectKeyMaptributes[Triangle.Foo.OBTUSE]";

    TestCompilerContext pctx = new TestCompilerContext();
    pctx.addImport(Triangle.class);
    pctx.addInput("this", Person.class);
    // setStrongTyping(true) is removed (MVEL3 has no such mode).

    Foo foo = new Foo();
    Person p = new Person();
    Map<Object, Foo> map = new HashMap<Object, Foo>();
    map.put(Triangle.Foo.OBTUSE, foo);
    p.setObjectKeyMaptributes(map);

    Serializable stmt = compileExpression(str, pctx);
    Object o = executeExpression(stmt, p, new HashMap());
  }

  // Fails with "ArrayList cannot be converted to char[]". The adapter emulates the set expression with `foo.charArray = setValue`;
  // the same assignment written directly in a block fails in the same way.
  @Test
  @KnownCompatibility("list-to-array-coercion")
  public void testBindListToArray() {
    Map<String, Object> vars = createTestMap();

    ArrayList<String> list = new ArrayList<String>();
    list.add("a");
    list.add("b");
    list.add("c");

    // OptimizerFactory.setDefaultOptimizer("reflective") is removed (MVEL2 optimizer selection).
    Serializable s = compileSetExpression("foo.charArray", new TestCompilerContext());

    executeSetExpression(s, vars, list);

    assertEquals(((Foo) vars.get("foo")).getCharArray().length, 3);
  }

  // Fails with "ArrayList cannot be converted to char[][]" (same cause as testBindListToArray).
  @Test
  @KnownCompatibility("list-to-array-coercion")
  public void testBindListToMultiArray() {
    Map<String, Object> vars = createTestMap();

    ArrayList<List<String>> list = new ArrayList<List<String>>();

    List<String> l1 = new ArrayList<String>();
    l1.add("a");
    l1.add("b");
    l1.add("c");

    List<String> l2 = new ArrayList<String>();
    l2.add("d");
    l2.add("e");
    l2.add("f");

    List<String> l3 = new ArrayList<String>();
    l3.add("g");
    l3.add("h");
    l3.add("i");

    list.add(l1);
    list.add(l2);
    list.add(l3);

    // OptimizerFactory.setDefaultOptimizer("reflective") is removed (MVEL2 optimizer selection).
    Serializable s = compileSetExpression("foo.charArrayMulti", new TestCompilerContext());

    executeSetExpression(s, vars, list);

    Foo foo = (Foo) vars.get("foo");

    assertEquals(foo.getCharArrayMulti().length, 3);
    assertEquals(foo.getCharArrayMulti()[2][2], 'i');
  }

  @Test
  public void testGenericsMap() throws Exception {
    try {
      String str = "triangle.deliveries[0].containsKey( \"x\" )";

      TestCompilerContext pctx = new TestCompilerContext();
      // setStrongTyping(true) is removed (MVEL3 has no such mode).
      pctx.addInput("triangle", Triangle.class);
      // The original only compiles the expression; compile at once so that the result is checked.
      compileEagerly(str, pctx);
    }
    catch (Exception e) {
      // it should not raise CCE
      e.printStackTrace();
      throw e;
    }
  }

  @Test
  public void testStrictModeAddAll() {
    String str = "list.addAll( o );";

    TestCompilerContext pctx = new TestCompilerContext();
    // setStrongTyping(true) is removed (MVEL3 has no such mode).
    pctx.addInput("o", Object.class);
    pctx.addInput("list", ArrayList.class);
    try {
      // The original only compiles the expression; compile at once so that the result is checked.
      compileEagerly(str, pctx);
      fail("This should not compileShared, as o is not of a type Collection");
    }
    catch (Exception e) {

    }
  }

  // Fails with "cannot find symbol: method size()": an unqualified method call is not resolved against the root collection
  // (this-reference). `c.size()` with a named input `c` works.
  @Test
  @KnownCompatibility("this-reference")
  public void testArrayList() throws SecurityException,
      NoSuchMethodException {
    Collection<String> collection = new ArrayList<String>();
    collection.add("I CAN HAS CHEEZBURGER");
    assertEquals(collection.size(),
        eval("size()",
            collection));
  }

  // Category not isolated: fails with "Collections.UnmodifiableCollection is not public". The input type is taken from the runtime class
  // of the root, a non-public JDK class. The unqualified `size()` (this-reference) is a second cause; declaring the input as
  // `Collection` and calling `c.size()` works.
  @Test
  @KnownCompatibility("non-public-input-type")
  public void testUnmodifiableCollection() throws SecurityException,
      NoSuchMethodException {
    Collection<String> collection = new ArrayList<String>();
    collection.add("I CAN HAS CHEEZBURGER");
    collection = unmodifiableCollection(collection);
    assertEquals(collection.size(),
        eval("size()",
            collection));
  }

  // Category not isolated: fails with "Collections.SingletonSet has private access". The input type is taken from the runtime class of
  // the root, a non-public JDK class. The unqualified `size()` (this-reference) is a second cause; declaring the input as
  // `Collection` and calling `c.size()` works.
  @Test
  @KnownCompatibility("non-public-input-type")
  public void testSingleton() throws SecurityException,
      NoSuchMethodException {
    Collection<String> collection = Collections.singleton("I CAN HAS CHEEZBURGER");
    assertEquals(collection.size(),
        eval("size()",
            collection));
  }

  // Fails with "Unsolved symbol in map": the properties of the root POJO are not usable by simple name. With a named input
  // `base.map[base.aMethod(1)] == "one"` works.
  @Test
  @KnownCompatibility("this-reference")
  public void testMapAccessWithNestedMethodCall() {
    String str = "map[aMethod(1)] == \"one\"";

    TestCompilerContext pctx = new TestCompilerContext();
    // setStrongTyping(true) is removed (MVEL3 has no such mode).
    pctx.addInput("this", POJO.class);
    Serializable stmt = compileExpression(str, pctx);

    POJO ctx = new POJO();
    ctx.getMap().put("1", "one");
    Boolean result = (Boolean) executeExpression(stmt, ctx);
    assertTrue(result);
  }

  @Test
  public void testMapAccessProperty() {
    String str = "map.key";

    TestCompilerContext pctx = new TestCompilerContext();
    // setStrongTyping(true) is removed (MVEL3 has no such mode).
    pctx.addInput("this", POJO.class);
    Serializable stmt = compileExpression(str, pctx);

    POJO ctx = new POJO();
    try {
      executeExpression(stmt, ctx);
      fail("Expected PropertyAccessException");
    }
    catch (RuntimeException ex) {
      // MVEL2's PropertyAccessException ("could not access: key") has no MVEL3 equivalent; any exception is accepted
      // here, and the message assertion is dropped.
    }
  }

  // Category not isolated: fails with "Unsolved symbol in map" (this-reference). With a named input `base.map[base.key]` fails too
  // ("cannot find symbol: variable key": the property inside the index is not qualified by `base`), so a nested property as map key
  // is a second cause; `base.map[base.getKey()]` and `base.map[k]` work.
  @Test
  @KnownCompatibility("this-reference")
  public void testMapAccessWithNestedPropertyAO() {
    // COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING is removed (MVEL2 optimizer flag).
    String str = "map[key] == \"one\"";
    TestCompilerContext pctx = new TestCompilerContext();
    // setStrongTyping(true) is removed (MVEL3 has no such mode).
    pctx.addInput("this", POJO.class);
    Serializable stmt = compileExpression(str, pctx);

    POJO ctx = new POJO();
    ctx.getMap().put("1", "one");
    Boolean result = (Boolean) executeExpression(stmt, ctx);
    assertTrue(result);
    result = (Boolean) executeExpression(stmt, ctx);
    assertTrue(result);
  }

  // Category not isolated: same as testMapAccessWithNestedPropertyAO (this-reference plus a nested property inside the map index).
  @Test
  @KnownCompatibility("this-reference")
  public void testMapAccessWithNestedPropertyAO_ASM() {
    // OptimizerFactory.setDefaultOptimizer("ASM") and COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING are removed
    // (MVEL2 optimizer selection).
    String str = "map[key] == \"one\"";
    TestCompilerContext pctx = new TestCompilerContext();
    // setStrongTyping(true) is removed (MVEL3 has no such mode).
    pctx.addInput("this", POJO.class);
    Serializable stmt = compileExpression(str, pctx);

    POJO ctx = new POJO();
    ctx.getMap().put("1", "one");
    Boolean result = (Boolean) executeExpression(stmt, ctx);
    assertTrue(result);
    result = (Boolean) executeExpression(stmt, ctx);
    assertTrue(result);
  }

  // Category not isolated: same as testMapAccessWithNestedPropertyAO (this-reference plus a nested property inside the map index).
  @Test
  @KnownCompatibility("this-reference")
  public void testMapAccessWithNestedProperty() {
    String str = "map[key] == \"one\"";

    TestCompilerContext pctx = new TestCompilerContext();
    // setStrongTyping(true) is removed (MVEL3 has no such mode).
    pctx.addInput("this", POJO.class);
    Serializable stmt = compileExpression(str, pctx);

    POJO ctx = new POJO();
    ctx.getMap().put("1", "one");
    Boolean result = (Boolean) executeExpression(stmt, ctx);
    assertTrue(result);
  }

  // Category not isolated: same as testMapAccessWithNestedPropertyAO (this-reference plus a nested property inside the map index);
  // the 500 repetitions were never reached.
  @Test
  @KnownCompatibility("this-reference")
  public void testMapAccessWithNestedPropertyRepeated() {
    /*
     * 181 - Nested property access successful in ReflectiveAccessorOptimizer 
     *   but fails in ASMAccessorOptimizer
     */
    String str = "map[key] == \"one\"";

    TestCompilerContext pctx = new TestCompilerContext();
    // setStrongTyping(true) is removed (MVEL3 has no such mode).
    pctx.addInput("this", POJO.class);
    Serializable stmt = compileExpression(str, pctx);

    POJO ctx = new POJO();
    ctx.getMap().put("1", "one");
    for (int i = 0; i < 500; ++i) {
      try {
        Boolean result = (Boolean) executeExpression(stmt, ctx);
        assertTrue(result);
      }
      catch (RuntimeException ex) {
        if (i == 0) {
          throw ex;
        }
        throw new IllegalStateException("Expression failed at iteration " + i, ex);
      }
    }
  }

  // Fails with "Method 'get' cannot be resolved ... pojo.getDates().get(0)": MVEL3 turns `[0]` into `.get(0)`, which a `Set` does not
  // have. `pojo.dates.iterator().next().getTime()` works.
  @Test
  @KnownCompatibility("set-index-access")
  public void testMethodInvocationWithCollectionElement() {
    Map context = new HashMap();
    context.put("pojo",
        new POJO());
    context.put("number",
        "1192800637980");

    Object result = eval("pojo.function(pojo.dates[0].time)",
        context);
    assertEquals(String.valueOf(((POJO) context.get("pojo")).getDates().iterator().next().getTime()),
        result);
  }

  // Fails with a parse error at `'Foo244'`. With double quotes and `return null;` it works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testNestedMethod1() {
    Vector vectorA = new Vector();
    Vector vectorB = new Vector();

    vectorA.add("Foo244");

    Map map = new HashMap();
    map.put("vecA",
        vectorA);
    map.put("vecB",
        vectorB);

    testCompiledSimple("vecB.add(vecA.remove(0)); vecA.add('Foo244');",
        null,
        map);

    assertEquals("Foo244",
        vectorB.get(0));
  }

  // Category not isolated: fails with a parse error at `{` (inline-array-literal). The single-quoted strings are a second cause;
  // `new String[] {"foo","test"}[1]` and `["foo","test"][1]` work.
  @Test
  @KnownCompatibility("inline-array-literal")
  public void testInlineUnion() {
    assertEquals("test",
        test("{'foo', 'test'}[1]"));
  }
}
