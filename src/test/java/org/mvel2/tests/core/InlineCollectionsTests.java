package org.mvel2.tests.core;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeExpression;
import org.mvel2.MVEL;

import java.util.List;
import java.util.Map;


public class InlineCollectionsTests extends AbstractTest {
    public void testListCreation2() {
        assertTrue(test("[\"test\"]") instanceof List);
    }

    public void testListCreation3() {
        assertTrue(test("[66]") instanceof List);
    }

    public void testListCreation4() {
        List ar = (List) test("[   66   , \"test\"   ]");
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
}
