package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.tests.core.res.Foo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeExpression;

public class InlineCollectionsTests extends AbstractTest {
  public void testListCreation2() {
    assertTrue(test("[\"test\"]") instanceof List);
  }

  public void testListCreation3() {
    assertTrue(test("[66]") instanceof List);
  }

  public void testListCreation4() {
    List ar = (List) MVEL.eval("[   66   , \"test\"   ]");
    assertEquals(2, ar.size());
    assertEquals(66, ar.get(0));
    assertEquals("test", ar.get(1));
  }

  public void testListCreationWithCall() {
    assertEquals(1, test("[\"apple\"].size()"));
  }

  public void testArrayCreationWithLength() {
    assertEquals(2, test("Array.getLength({'foo', 'bar'})"));
  }

  public void testEmptyList() {
    assertTrue(test("[]") instanceof List);
  }

  public void testEmptyArray() {
    assertTrue(((Object[]) test("{}")).length == 0);
  }

  public void testEmptyArray2() {
    Object o = MVEL.eval("{     }");

    assertTrue(((Object[]) test("{    }")).length == 0);
  }

  public void testArrayCreation() {
    assertEquals(0, test("arrayTest = {{1, 2, 3}, {2, 1, 0}}; arrayTest[1][2]"));
  }

  public void testMapCreation() {
    assertEquals("sarah", test("map = ['mike':'sarah','tom':'jacquelin']; map['mike']"));
  }

  public void testMapCreation2() {
    assertEquals("sarah", test("map = ['mike' :'sarah'  ,'tom'  :'jacquelin'  ]; map['mike']"));
  }

  public void testMapCreation3() {
    assertEquals("foo", test("map = [1 : 'foo']; map[1]"));
  }

  public void testSizeOnInlineArray() {
    assertEquals(3, test("{1,2,3}.size()"));
  }

  public void testSimpleListCreation() {
    test("['foo', 'bar', 'foobar', 'FOOBAR']");
  }

  public void testArrayCoercion() {
    assertEquals("gonk", test("funMethod( {'gonk', 'foo'} )"));
  }

  public void testArrayCoercion2() {
    assertEquals(10, test("sum({2,2,2,2,2})"));
  }

  public void testCompiledMapStructures() {
    executeExpression(compileExpression("['foo':'bar'] contains 'foo'"), null, (Map) null, Boolean.class);
  }

  public void testSubListInMap() {
    assertEquals("pear", test("map = ['test' : 'poo', 'foo' : [c, 'pear']]; map['foo'][1]"));
  }

  public void testForEach2() {
    assertEquals(6, test("total = 0; a = {1,2,3}; foreach(item : a) { total += item }; total"));
  }

  public void testForEach3() {
    assertEquals(true, test("a = {1,2,3}; foreach (i : a) { if (i == 1) { return true; } }"));
  }

  public void testForEach3a() {
    assertEquals(true, MVEL.eval("a = {1,2,3}; foreach (i : a) { if (i == 1) { return true; } }", new HashMap()));
  }

  public void testForEach4() {
    assertEquals("OneTwoThreeFour", test("a = {1,2,3,4}; builder = ''; foreach (i : a) {" +
        " if (i == 1) { builder += 'One' } else if (i == 2) { builder += 'Two' } " +
        "else if (i == 3) { builder += 'Three' } else { builder += 'Four' }" +
        "}; builder;"));
  }

  public void testInlineCollectionNestedObjectCreation() {
    Map m = (Map) test("['Person.age' : [1, 2, 3, 4], 'Person.rating' : ['High', 'Low']," +
        " 'Person.something' : (new String('foo').toUpperCase())]");

    assertEquals("FOO", m.get("Person.something"));
  }

  public void testInlineCollectionNestedObjectCreation1() {
    Map m = (Map) test("[new String('foo') : new String('bar')]");

    assertEquals("bar", m.get("foo"));
  }

  public void testMVEL179() {
    assertTrue((Boolean) MVEL.eval("(($ in [2,4,8,16,32] if $ < 10) != empty)"));
  }

  public void testInlineArrayForEach() {
    assertEquals(11,
        test("counterX = 0; foreach (item:{1,2,3,4,5,6,7,8,9,10}) { counterX++; }; return counterX + 1;"));
  }

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
            vars)).size());
    assertEquals(1,
        ((List) testCompiledSimple(expressionString2,
            vars)).size());
  }

  /**
   * Submitted by: Michael Neale
   */
  public void testInlineCollectionParser1() {
    assertEquals("q",
        ((Map) test("['Person.age' : [1, 2, 3, 4],'Person.rating' : 'q']")).get("Person.rating"));
    assertEquals("q",
        ((Map) test("['Person.age' : [1, 2, 3, 4], 'Person.rating' : 'q']")).get("Person.rating"));
  }

  public void testParsingStability3() {
    assertEquals(false,
        test("!( [\"X\", \"Y\"] contains \"Y\" )"));
  }

  @SuppressWarnings({"UnnecessaryBoxing"})
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
  public void testToListStrictMode() {
    String text = "misc.toList(foo.bar.name, 'hello', 42, ['key1' : 'value1'," +
        " c : [ foo.bar.age, 'car', 42 ]], [42, [c : 'value1']] )";

    ParserContext ctx = new ParserContext();
    ctx.addInput("misc",
        MiscTestClass.class);
    ctx.addInput("foo",
        Foo.class);
    ctx.addInput("c",
        String.class);

    ctx.setStrictTypeEnforcement(true);
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

  public void testArrayAccessorAssign() {
    assertEquals("foo",
        test("a = {'f00', 'bar'}; a[0] = 'foo'; a[0]"));
  }

  public void testInlineListSensitivenessToSpaces() {
    String ex = "([\"a\",\"b\", \"c\"])";

    ParserContext ctx = new ParserContext();
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

  public void testMVEL241() {

    //Regression in mvel2-2.1-20110218.004106-9.jar
    String mvelSource = "[ 'Person.age' : [42, 43],\n'Person.sex' : ['M', 'F'] ]";
    Object eval = MVEL.eval(mvelSource, new HashMap<String, Object>());
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

  public void testMVEL242() {

    //Regression in mvel2-2.1-20110218.004106-9.jar
    String mvelSource = "[ 'Fact.field1' : ['val1', 'val2'], 'Fact.field2' : ['val3', 'val4'], 'Fact.field2[field1=val1]' : ['f1val1a', 'f1val1b'], 'Fact.field2[field1=val2]' : ['f1val2a', 'f1val2b'] ]";
    Object eval = MVEL.eval(mvelSource,
        new HashMap<String, Object>());
    Map<String, Object> map = (Map<String, Object>) eval;

    assertNotNull(map);
    assertEquals(4, map.size());
  }

  public void testEmptyElement() {
    Object result = MVEL.eval("[2, 3, 4, ]", new HashMap());
    assertTrue(result instanceof List);
    List l = (List) result;
    assertEquals(3, l.size());
  }

  public void testAddTwoLists() {
    Object result = test("[1,2,3] + [4,5,6]");
    assertTrue(result instanceof List);
    List l = (List) result;
    assertEquals(6, l.size());
  }

  public void testElementToList() {
    Object result = test("[1,2,3] + 4");
    assertTrue(result instanceof List);
    List l = (List) result;
    assertEquals(4, l.size());
  }


}
