package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.Mvel3TestSupport.ExpressionCompiler;
import org.mvel3.compat.mvel2.res.Foo;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.InlineCollectionsTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class InlineCollectionsTests extends AbstractTest {
  @Test
  public void testListCreation2() {
    assertTrue(test("[\"test\"]") instanceof List);
  }

  @Test
  public void testListCreation3() {
    assertTrue(test("[66]") instanceof List);
  }

  @Test
  public void testListCreation4() {
    List ar = (List) eval("[   66   , \"test\"   ]");
    assertEquals(2, ar.size());
    assertEquals(66, ar.get(0));
    assertEquals("test", ar.get(1));
  }

  @Test
  public void testListCreationWithCall() {
    assertEquals(1, test("[\"apple\"].size()"));
  }

  // Category not isolated: fails with a JavaParser NPE ("current" is null) on `{'foo', 'bar'}`. The `{..}` array
  // literal (inline-array-literal) is the suspected trigger; single-quoted strings (single-quote-string) were not tested separately.
  @Test
  @KnownCompatibility("inline-array-literal")
  public void testArrayCreationWithLength() {
    assertEquals(2, test("Array.getLength({'foo', 'bar'})"));
  }

  @Test
  public void testEmptyList() {
    assertTrue(test("[]") instanceof List);
  }

  @Test
  @KnownCompatibility("inline-array-literal")
  public void testEmptyArray() {
    assertTrue(((Object[]) test("{}")).length == 0);
  }

  @Test
  @KnownCompatibility("inline-array-literal")
  public void testEmptyArray2() {
    Object o = eval("{     }");

    assertTrue(((Object[]) test("{    }")).length == 0);
  }

  // Category not isolated: fails with "A reference was unexpectedly null". The expression combines the `{{..}}` array
  // literal (inline-array-literal) with an undeclared assignment without `return` (block-syntax); which one triggers it was not verified.
  @Test
  @KnownCompatibility("inline-array-literal")
  public void testArrayCreation() {
    assertEquals(0, test("arrayTest = {{1, 2, 3}, {2, 1, 0}}; arrayTest[1][2]"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null". The expression combines single-quoted strings with an
  // undeclared assignment without `return` (block-syntax); which one triggers it was not verified.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMapCreation() {
    assertEquals("sarah", test("map = ['mike':'sarah','tom':'jacquelin']; map['mike']"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null". The expression combines single-quoted strings with an
  // undeclared assignment without `return` (block-syntax); which one triggers it was not verified.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMapCreation2() {
    assertEquals("sarah", test("map = ['mike' :'sarah'  ,'tom'  :'jacquelin'  ]; map['mike']"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null". The expression combines single-quoted strings with an
  // undeclared assignment without `return` (block-syntax); which one triggers it was not verified.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMapCreation3() {
    assertEquals("foo", test("map = [1 : 'foo']; map[1]"));
  }

  @Test
  @KnownCompatibility("inline-array-literal")
  public void testSizeOnInlineArray() {
    assertEquals(3, test("{1,2,3}.size()"));
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testSimpleListCreation() {
    test("['foo', 'bar', 'foobar', 'FOOBAR']");
  }

  // Category not isolated: fails with a JavaParser NPE ("current" is null) on `{'gonk', 'foo'}`. The `{..}` array literal
  // (inline-array-literal) is the suspected trigger; single-quote-string and `funMethod` being a method of the MVEL2 root object
  // (this-reference) were not tested separately.
  @Test
  @KnownCompatibility("inline-array-literal")
  public void testArrayCoercion() {
    assertEquals("gonk", test("funMethod( {'gonk', 'foo'} )"));
  }

  // Category not isolated: the `{..}` array literal (inline-array-literal) is the first parse error; calling `sum(..)`
  // on the MVEL2 root object (this-reference) would be the next problem and was not tested separately.
  @Test
  @KnownCompatibility("inline-array-literal")
  public void testArrayCoercion2() {
    assertEquals(10, test("sum({2,2,2,2,2})"));
  }

  // Category not isolated: the first parse error is at the single-quoted strings. The infix `contains` operator
  // (contains-operator) was not tested separately.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testCompiledMapStructures() {
    executeExpression(compileExpression("['foo':'bar'] contains 'foo'"), null, (Map) null, Boolean.class);
  }

  // Category not isolated: fails with "A reference was unexpectedly null". The expression combines single-quoted strings
  // (single-quote-string), an undeclared assignment without `return` (block-syntax) and a list literal nested in a map literal
  // (nested-inline-collection); which one triggers the observed error was not verified. With only the quotes changed to double
  // quotes it fails with a parse error "missing ';'" (block-syntax); with `var` and `return` added as well, javac rejects the
  // nested literal ("illegal start of expression").
  @Test
  @KnownCompatibility("single-quote-string")
  public void testSubListInMap() {
    assertEquals("pear", test("map = ['test' : 'poo', 'foo' : [c, 'pear']]; map['foo'][1]"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null" (see javaparser-null-node). The expression combines
  // `foreach`, the `{1,2,3}` array literal (inline-array-literal) and undeclared assignments (block-syntax); which one triggers it was not verified.
  @Test
  @KnownCompatibility("foreach-syntax")
  public void testForEach2() {
    assertEquals(6, test("total = 0; a = {1,2,3}; foreach(item : a) { total += item }; total"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null" (see javaparser-null-node). The expression combines
  // `foreach`, the `{1,2,3}` array literal (inline-array-literal) and undeclared assignments (block-syntax); which one triggers it was not verified.
  @Test
  @KnownCompatibility("foreach-syntax")
  public void testForEach3() {
    assertEquals(true, test("a = {1,2,3}; foreach (i : a) { if (i == 1) { return true; } }"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null" (see javaparser-null-node). The expression combines
  // `foreach`, the `{1,2,3}` array literal (inline-array-literal) and undeclared assignments (block-syntax); which one triggers it was not verified.
  @Test
  @KnownCompatibility("foreach-syntax")
  public void testForEach3a() {
    assertEquals(true, eval("a = {1,2,3}; foreach (i : a) { if (i == 1) { return true; } }", new HashMap()));
  }

  // Category not isolated: fails with "A reference was unexpectedly null" (see javaparser-null-node). The expression combines
  // `foreach`, the `{1,2,3,4}` array literal (inline-array-literal), single-quoted strings (single-quote-string) and undeclared
  // assignments (block-syntax); which one triggers it was not verified.
  @Test
  @KnownCompatibility("foreach-syntax")
  public void testForEach4() {
    assertEquals("OneTwoThreeFour", test("a = {1,2,3,4}; builder = ''; foreach (i : a) {" +
        " if (i == 1) { builder += 'One' } else if (i == 2) { builder += 'Two' } " +
        "else if (i == 3) { builder += 'Three' } else { builder += 'Four' }" +
        "}; builder;"));
  }

  // Category not isolated: the first parse error comes from the single-quoted strings (single-quote-string). The expression also
  // nests a list literal inside a map literal (nested-inline-collection): with only the single quotes replaced by double quotes,
  // javac rejects it ("illegal start of expression").
  @Test
  @KnownCompatibility("single-quote-string")
  public void testInlineCollectionNestedObjectCreation() {
    Map m = (Map) test("['Person.age' : [1, 2, 3, 4], 'Person.rating' : ['High', 'Low']," +
        " 'Person.something' : (new String('foo').toUpperCase())]");

    assertEquals("FOO", m.get("Person.something"));
  }

  // Category not isolated: the first parse error comes from the single-quoted strings; with double quotes the
  // expression passes (map literal with `new String(..)` key and value).
  @Test
  @KnownCompatibility("single-quote-string")
  public void testInlineCollectionNestedObjectCreation1() {
    Map m = (Map) test("[new String('foo') : new String('bar')]");

    assertEquals("bar", m.get("foo"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null". The expression uses a projection with `if`
  // (projection-operator), the `empty` keyword (empty-operator) and `$`; which one triggers it was not verified.
  @Test
  @KnownCompatibility("projection-operator")
  public void testMVEL179() {
    assertTrue((Boolean) eval("(($ in [2,4,8,16,32] if $ < 10) != empty)"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null" (see javaparser-null-node). The expression combines
  // `foreach`, the `{1,2,...}` array literal (inline-array-literal) and an undeclared assignment (block-syntax); which one triggers it was not verified.
  @Test
  @KnownCompatibility("foreach-syntax")
  public void testInlineArrayForEach() {
    assertEquals(11,
        test("counterX = 0; foreach (item:{1,2,3,4,5,6,7,8,9,10}) { counterX++; }; return counterX + 1;"));
  }

  // Category not isolated: fails with "A reference was unexpectedly null" (see javaparser-null-node). The expression combines
  // `foreach`, the `{1,1,...}` array literal (inline-array-literal) and an undeclared assignment (block-syntax); which one triggers it was not verified.
  @Test
  @KnownCompatibility("foreach-syntax")
  public void testInlineArrayForEach2() {
    assertEquals(0,
        test("counterX = 10; foreach (item:{1,1,1,1,1,1,1,1,1,1}) { counterX -= item; } return counterX;"));
  }


  public static final class Target {
    private int _attribute;

    public Target(int attribute_) {
      _attribute = attribute_;
    }

    public int getAttribute() {
      return _attribute;
    }
  }

  // Category not isolated: fails with "A reference was unexpectedly null" (see javaparser-null-node). The expression combines
  // `foreach`, a large `{..}` array literal (inline-array-literal), the infix `contains` (contains-operator) and undeclared
  // assignments (block-syntax); which one triggers it was not verified.
  @Test
  @KnownCompatibility("foreach-syntax")
  public void testNegativeArraySizeBug() throws Exception {
    String expressionString1 = "results = new java.util.ArrayList(); foreach (element : elements) { " +
        "if( ( {30, 214, 158, 31, 95, 223, 213, 86, 159, 34, 32, 96, 224, 160, 85, 201, 29, 157, 100, 146," +
        " 82, 203, 194, 145, 140, 81, 27, 166, 212, 38, 28, 94, 168, 23, 87, 150, 35, 149, 193, 33, 132," +
        " 206, 93, 196, 24, 88, 195, 36, 26, 154, 167, 108, 204, 74, 46, 25, 153, 202, 79, 207, 143, 43, " +
        "16, 80, 198, 208, 144, 41, 97, 142, 83, 18, 162, 103, 155, 98, 44, 17, 205, 77, 156, 141, 165," +
        " 102, 84, 37, 101, 222, 40, 104, 99, 177, 182, 22, 180, 21, 137, 221, 179, 78, 42, 178, 19, 183," +
        " 139, 218, 219, 39, 220, 20, 184, 217, 138, 62, 190, 171, 123, 113, 59, 118, 225, 124, 169, 60, " +
        "117, 1} contains element.attribute ) ) { results.add(element); } }; results";

    String expressionString2 = "results = new java.util.ArrayList(); foreach (element : elements) { " +
        "if( ( {30, 214, 158, 31, 95, 223, 213, 86, 159, 34, 32, 96, 224, 160, 85, 201, 29, 157, 100, 146," +
        " 82, 203, 194, 145, 140, 81, 27, 166, 212, 38, 28, 94, 168, 23, 87, 150, 35, 149, 193, 33, 132, " +
        "206, 93, 196, 24, 88, 195, 36, 26, 154, 167, 108, 204, 74, 46, 25, 153, 202, 79, 207, 143, 43," +
        " 16, 80, 198, 208, 144, 41, 97, 142, 83, 18, 162, 103, 155, 98, 44, 17, 205, 77, 156, 141, 165," +
        " 102, 84, 37, 101, 222, 40, 104, 99, 177, 182, 22, 180, 21, 137, 221, 179, 78, 42, 178, 19, 183," +
        " 139, 218, 219, 39, 220, 20, 184, 217, 138, 62, 190, 171, 123, 113, 59, 118, 225, 124, 169, 60," +
        " 117, 1, 61, 189, 122, 68, 58, 119, 63, 226, 3, 172}" +
        " contains element.attribute ) ) { results.add(element); } }; results";

    List<Target> targets = new ArrayList<Target>();
    targets.add(new Target(1));
    targets.add(new Target(999));

    Map vars = new HashMap();
    vars.put("elements",
        targets);

    assertEquals(1,
        ((List) testCompiledSimple(expressionString1,
            null,
            vars)).size());
    assertEquals(1,
        ((List) testCompiledSimple(expressionString2,
            null,
            vars)).size());
  }

  /**
   * Submitted by: Michael Neale
   */
  // Category not isolated: the first parse error comes from the single-quoted strings (single-quote-string). The expression also
  // nests a list literal inside a map literal (nested-inline-collection): with only the single quotes replaced by double quotes,
  // javac rejects it ("illegal start of expression").
  @Test
  @KnownCompatibility("single-quote-string")
  public void testInlineCollectionParser1() {
    assertEquals("q",
        ((Map) test("['Person.age' : [1, 2, 3, 4],'Person.rating' : 'q']")).get("Person.rating"));
    assertEquals("q",
        ((Map) test("['Person.age' : [1, 2, 3, 4], 'Person.rating' : 'q']")).get("Person.rating"));
  }

  @Test
  @KnownCompatibility("contains-operator")
  public void testParsingStability3() {
    assertEquals(false,
        test("!( [\"X\", \"Y\"] contains \"Y\" )"));
  }

  @SuppressWarnings({"UnnecessaryBoxing"})
  // Category not isolated: the first parse error ("mismatched input '42'") comes from the single-quoted strings
  // (single-quote-string). The expression also nests list/map literals (nested-inline-collection): with only the single quotes
  // replaced by double quotes, javac rejects it ("illegal start of expression").
  @Test
  @KnownCompatibility("single-quote-string")
  public void testToList() {
    String text = "misc.toList(foo.bar.name, 'hello', 42, ['key1' : 'value1'," +
        " c : [ foo.bar.age, 'car', 42 ]], [42, [c : 'value1']] )";

    List list = (List) test(text);

    assertSame("dog",
        list.get(0));
    assertEquals("hello",
        list.get(1));
    assertEquals(new Integer(42),
        list.get(2));
    Map map = (Map) list.get(3);
    assertEquals("value1",
        map.get("key1"));

    List nestedList = (List) map.get("cat");
    assertEquals(14,
        nestedList.get(0));
    assertEquals("car",
        nestedList.get(1));
    assertEquals(42,
        nestedList.get(2));

    nestedList = (List) list.get(4);
    assertEquals(42,
        nestedList.get(0));
    map = (Map) nestedList.get(1);
    assertEquals("value1",
        map.get("cat"));
  }

  @SuppressWarnings({"UnnecessaryBoxing"})
  // Category not isolated: same expression as testToList. The first parse error comes from the single-quoted strings
  // (single-quote-string); the nested list/map literals (nested-inline-collection) fail in javac once the quotes are double
  // ("illegal start of expression"). MVEL2 strict type enforcement has no MVEL3 equivalent and is removed from the test.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testToListStrictMode() {
    String text = "misc.toList(foo.bar.name, 'hello', 42, ['key1' : 'value1'," +
        " c : [ foo.bar.age, 'car', 42 ]], [42, [c : 'value1']] )";

    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("misc",
        MiscTestClass.class);
    ctx.addInput("foo",
        Foo.class);
    ctx.addInput("c",
        String.class);

    // ctx.setStrictTypeEnforcement(true) removed: MVEL3 has no such mode.
    ExpressionCompiler compiler = new ExpressionCompiler(text, ctx);

    List list = (List) executeExpression(compiler.compile(),
        createTestMap());

    assertSame("dog",
        list.get(0));
    assertEquals("hello",
        list.get(1));
    assertEquals(new Integer(42),
        list.get(2));
    Map map = (Map) list.get(3);
    assertEquals("value1",
        map.get("key1"));

    List nestedList = (List) map.get("cat");
    assertEquals(14,
        nestedList.get(0));
    assertEquals("car",
        nestedList.get(1));
    assertEquals(42,
        nestedList.get(2));

    nestedList = (List) list.get(4);
    assertEquals(42,
        nestedList.get(0));
    map = (Map) nestedList.get(1);
    assertEquals("value1",
        map.get("cat"));
  }

  // Category not isolated: the `{..}` array literal (inline-array-literal) is the first parse error; single-quoted
  // strings (single-quote-string) and the undeclared assignment without `return` (block-syntax) were not tested separately.
  @Test
  @KnownCompatibility("inline-array-literal")
  public void testArrayAccessorAssign() {
    assertEquals("foo",
        test("a = {'f00', 'bar'}; a[0] = 'foo'; a[0]"));
  }

  @Test
  public void testInlineListSensitivenessToSpaces() {
    String ex = "([\"a\",\"b\", \"c\"])";

    TestCompilerContext ctx = new TestCompilerContext();
    Serializable s = compileExpression(ex,
        ctx);

    List result = (List) executeExpression(s,
        new HashMap());
    assertNotNull(result);
    assertEquals("a",
        result.get(0));
    assertEquals("b",
        result.get(1));
    assertEquals("c",
        result.get(2));
  }

  // Category not isolated: the first parse error comes from the single-quoted strings (single-quote-string). The expression also
  // nests a list literal inside a map literal (nested-inline-collection): with only the single quotes replaced by double quotes,
  // javac rejects it ("illegal start of expression").
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMVEL241() {

    //Regression in mvel2-2.1-20110218.004106-9.jar
    String mvelSource = "[ 'Person.age' : [42, 43],\n'Person.sex' : ['M', 'F'] ]";
    Object eval = eval(mvelSource, new HashMap<String, Object>());
    Map<String, Object> map = (Map<String, Object>) eval;

    assertNotNull(map);
    assertTrue(map.size() == 2);
    assertTrue(map.containsKey("Person.age"));
    assertTrue(map.containsKey("Person.sex"));

    List listSex = (List) map.get("Person.sex");
    assertNotNull(listSex);
    assertTrue(listSex.size() == 2);

    List listAge = (List) map.get("Person.age");
    assertNotNull(listAge);
    assertTrue(listAge.size() == 2);
  }

  // Category not isolated: the first parse error comes from the single-quoted strings (single-quote-string). The expression also
  // nests a list literal inside a map literal (nested-inline-collection): with only the single quotes replaced by double quotes,
  // javac rejects it ("illegal start of expression").
  @Test
  @KnownCompatibility("single-quote-string")
  public void testMVEL242() {

    //Regression in mvel2-2.1-20110218.004106-9.jar
    String mvelSource = "[ 'Fact.field1' : ['val1', 'val2'], 'Fact.field2' : ['val3', 'val4'], 'Fact.field2[field1=val1]' : ['f1val1a', 'f1val1b'], 'Fact.field2[field1=val2]' : ['f1val2a', 'f1val2b'] ]";
    Object eval = eval(mvelSource,
        new HashMap<String, Object>());
    Map<String, Object> map = (Map<String, Object>) eval;

    assertNotNull(map);
    assertEquals(4, map.size());
  }

  @Test
  @KnownCompatibility("trailing-comma-list")
  public void testEmptyElement() {
    Object result = eval("[2, 3, 4, ]", new HashMap());
    assertTrue(result instanceof List);
    List l = (List) result;
    assertEquals(3, l.size());
  }

  @Test
  @KnownCompatibility("list-concat-operator")
  public void testAddTwoLists() {
    Object result = test("[1,2,3] + [4,5,6]");
    assertTrue(result instanceof List);
    List l = (List) result;
    assertEquals(6, l.size());
  }

  @Test
  @KnownCompatibility("list-concat-operator")
  public void testElementToList() {
    Object result = test("[1,2,3] + 4");
    assertTrue(result instanceof List);
    List l = (List) result;
    assertEquals(4, l.size());
  }


}
