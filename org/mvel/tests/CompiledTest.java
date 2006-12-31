package org.mvel.tests;

import org.mvel.ExpressionParser;
import static org.mvel.ExpressionParser.evalToBoolean;
import junit.framework.TestCase;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompiledTest extends TestCase {
    Foo foo = new Foo();
    Map<String, Object> map = new HashMap<String, Object>();
    Base base = new Base();

    public CompiledTest() {
        foo.setBar(new Bar());
        map.put("foo", foo);
        map.put("a", null);
        map.put("b", null);
        map.put("c", "cat");
        map.put("BWAH", "");

        map.put("misc", new MiscTestClass());

        map.put("pi", "3.14");
        map.put("hour", "60");
        map.put("zero", 0);
    }


    public void testMethodOnValue() {
        assertEquals("DOG", parseDirect("foo.bar.name.toUpperCase()"));
    }

    public void testSimpleProperty() {
        assertEquals("dog", parseDirect("foo.bar.name"));
    }


    public void testBooleanOperator() {
        assertEquals(true, parseDirect("foo.bar.woof == true"));
    }

    public void testBooleanOperator2() {
        assertEquals(false, parseDirect("foo.bar.woof == false"));
    }

    public void testTextComparison() {
        assertEquals(true, parseDirect("foo.bar.name == 'dog'"));
    }

    public void testNETextComparison() {
        assertEquals(true, parseDirect("foo.bar.name != 'foo'"));
    }

    public void testChor() {
        assertEquals("cat", parseDirect("a or b or c"));
    }

    public void testChorWithLiteral() {
        assertEquals("fubar", parseDirect("a or 'fubar'"));
    }

    public void testNullCompare() {
        assertEquals(true, parseDirect("c != null"));
    }

    public void testAnd() {
        assertEquals(true, parseDirect("c != null && foo.bar.name == 'dog' && foo.bar.woof"));
    }

    public void testMath() {
        assertEquals(188.4, parseDirect("pi * hour"));
    }


    public void testComplexAnd() {
        assertEquals(true, parseDirect("(pi * hour) > 0 && foo.happy() == 'happyBar'"));
    }

    public void testModulus() {
        assertEquals(38392 % 2,
                parseDirect("38392 % 2"));
    }


    public void testLessThan() {
        assertEquals(true, parseDirect("pi < 3.15"));
        assertEquals(true, parseDirect("pi <= 3.14"));
        assertEquals(false, parseDirect("pi > 3.14"));
        assertEquals(true, parseDirect("pi >= 3.14"));
    }

    public void testMethodAccess() {
        assertEquals("happyBar", parseDirect("foo.happy()"));
    }

    public void testMethodAccess2() {
        assertEquals("FUBAR", parseDirect("foo.toUC('fubar')"));
    }

    public void testMethodAccess3() {
        assertEquals(true, parseDirect("equalityCheck(c, 'cat')"));
    }

    public void testMethodAccess4() {
        assertEquals(null, parseDirect("readBack(null)"));
    }

    public void testMethodAccess5() {
        assertEquals("nulltest", parseDirect("appendTwoStrings(null, 'test')"));
    }

    public void testNegation() {
        assertEquals(true, parseDirect("!fun && !fun"));
    }

    public void testNegation2() {
        assertEquals(false, parseDirect("fun && !fun"));
    }

    public void testNegation3() {
        assertEquals(true, parseDirect("!(fun && fun)"));
    }

    public void testNegation4() {
        assertEquals(false, parseDirect("(fun && fun)"));
    }

    public void testMultiStatement() {
        assertEquals(true, parseDirect("populate(); barfoo == 'sarah'"));
    }

    public void testAssignment() {
        assertEquals(true, parseDirect("populate(); blahfoo = 'sarah'; blahfoo == 'sarah'"));
    }

    public void testAssignment2() {
        assertEquals("sarah", parseDirect("populate(); blahfoo = barfoo"));
    }

    public void testOr() {
        assertEquals(true, parseDirect("fun || true"));
    }

    public void testLiteralPassThrough() {
        assertEquals(true, parseDirect("true"));
    }

    public void testLiteralPassThrough2() {
        assertEquals(false, parseDirect("false"));
    }

    public void testLiteralPassThrough3() {
        assertEquals(null, parseDirect("null"));
    }



    public void testRegEx() {
        assertEquals(true, parseDirect("foo.bar.name ~= '[a-z].+'"));
    }

    public void testRegExNegate() {
        assertEquals(false, parseDirect("!(foo.bar.name ~= '[a-z].+')"));
    }

    public void testRegEx2() {
        assertEquals(true, parseDirect("foo.bar.name ~= '[a-z].+' && foo.bar.name != null"));
    }

    public void testBlank() {
        assertEquals(true, parseDirect("'' == empty"));
    }

    public void testBlank2() {
        assertEquals(true, parseDirect("BWAH == empty"));
    }

    public void testBooleanModeOnly() {
        assertEquals(true, (Object) evalToBoolean("!BWAH", base, map));
    }

    public void testBooleanModeOnly2() {
        assertEquals(false, (Object) evalToBoolean("BWAH", base, map));
    }

    public void testBooleanModeOnly3() {
        assertEquals(true, (Object) evalToBoolean("!zero", base, map));
    }

    public void testBooleanModeOnly4() {
        assertEquals(true, (Object) evalToBoolean("hour == (hour + 0)", base, map));
    }

    public void testTernary() {
        assertEquals("foobie", parseDirect("zero==0?'foobie':zero"));
    }

    public void testTernary2() {
        assertEquals("blimpie", parseDirect("zero==1?'foobie':'blimpie'"));
    }

    public void testTernary3() {
        assertEquals("foobiebarbie", parseDirect("zero==1?'foobie':'foobie'+'barbie'"));
    }

    public void testStrAppend() {
        assertEquals("foobarcar", parseDirect("'foo' + 'bar' + 'car'"));
    }

    public void testStrAppend2() {
        assertEquals("foobarcar1", parseDirect("'foobar' + 'car' + 1"));
    }

    public void testInstanceCheck1() {
        assertEquals(true, parseDirect("c is 'java.lang.String'"));
    }

    public void testInstanceCheck2() {
        assertEquals(false, parseDirect("pi is 'java.lang.Integer'"));
    }

    public void testBitwiseOr1() {
        assertEquals(6, parseDirect("2 | 4"));
    }

    public void testBitwiseOr2() {
        assertEquals(true, parseDirect("(2 | 1) > 0"));
    }

    public void testBitwiseOr3() {
        assertEquals(true, parseDirect("(2 | 1) == 3"));
    }

    public void testBitwiseAnd1() {
        assertEquals(2, parseDirect("2 & 3"));
    }

    public void testShiftLeft() {
        assertEquals(4, parseDirect("2 << 1"));
    }

    public void testUnsignedShiftLeft() {
        assertEquals(2, parseDirect("-2 <<< 0"));
    }

    public void testShiftRight() {
        assertEquals(128, parseDirect("256 >> 1"));
    }

    public void testXOR() {
        assertEquals(3, parseDirect("1 ^ 2"));
    }

    public void testContains1() {
        assertEquals(true, parseDirect("list contains 'Happy!'"));
    }

    public void testContains2() {
        assertEquals(false, parseDirect("list contains 'Foobie'"));
    }

    public void testContains3() {
        assertEquals(true, parseDirect("sentence contains 'fox'"));
    }

    public void testContains4() {
        assertEquals(false, parseDirect("sentence contains 'mike'"));
    }

    public void testContains5() {
        assertEquals(true, parseDirect("!(sentence contains 'mike')"));
    }

    public void testInvert() {
        assertEquals(~10, parseDirect("~10"));
        assertEquals(~(10 + 1), parseDirect("~(10 + 1)"));
        assertEquals(~10 + (1 + ~50), parseDirect("~10 + (1 + ~50)"));
    }


    public void testListCreation2() {
        assertEquals(ArrayList.class, parseDirect("[\"test\"]").getClass());
    }

    public void testListCreation3() {
        assertEquals(ArrayList.class, parseDirect("[66]").getClass());
    }

    public void testListCreationWithCall() {
        assertEquals(1, parseDirect("[\"apple\"].size()"));
    }

    public void testArrayCreationWithLength() {
        assertEquals(2, parseDirect("Array.getLength({'foo', 'bar'})"));
    }

    public void testArrayCreation() {
        assertEquals(0, parseDirect("arrayTest = {{1, 2, 3}, {2, 1, 0}}; arrayTest[1][2]"));
    }

    public void testMapCreation() {
        assertEquals("sarah", parseDirect("map = ['mike':'sarah','tom':'jacquelin']; map['mike']"));
    }

    public void testProjectionSupport() {
        assertEquals(true, parseDirect("(name in things) contains 'Bob'"));
    }

    public void testProjectionSupport2() {
        assertEquals(3, parseDirect("(('name') in things).size()"));
    }


    public void testStaticMethodFromLiteral() {
        assertEquals(String.class.getName(), parseDirect("String.valueOf(Class.forName('java.lang.String').getName())"));
    }

    public void testStaticCalls() {
        assertTrue(Math.abs(1.1d) == ((Double) parseDirect("Math.abs(1.1)")));
    }

    public void testMethodCallsEtc() {
        parseDirect("title = 1; " +
                "frame = new javax.swing.JFrame; " +
                "label = new javax.swing.JLabel; " +
                "title = title + 1;" +
                "frame.setTitle(title);" +
                "label.setText('this is a test of mvel');" +
                "frame.setVisible(true);");
    }

    public void testObjectInstantiation() {
        parseDirect("new java.lang.String('foobie')");
    }

    public void testObjectInstantiationWithMethodCall() {
        parseDirect("new String('foobie').toString()");
    }

    public void testObjectInstantiation2() {
        parseDirect("new String() is String");
    }

    public void testObjectInstantiation3() {
        parseDirect("new java.text.SimpleDateFormat('yyyy').format(new java.util.Date(System.currentTimeMillis()))");
    }

    public void testArrayCoercion() {
        assertEquals("gonk", parseDirect("funMethod( {'gonk', 'foo'} )"));
    }

    public void testArrayCoercion2() {
        assertEquals(10, parseDirect("sum({2,2,2,2,2})"));
    }

    public void testMapAccess() {
        assertEquals("dog", parseDirect("funMap['foo'].bar.name"));
    }

    public void testMapAccess2() {
        assertEquals("dog", parseDirect("funMap.foo.bar.name"));
    }

    public void testSoundex() {
        assertTrue((Boolean) parseDirect("'foobar' soundslike 'fubar'"));
    }

    public void testSoundex2() {
        assertFalse((Boolean) parseDirect("'flexbar' soundslike 'fubar'"));
    }

    public void testThisReference() {
        assertEquals(true, parseDirect("this") instanceof Base);
    }

    public void testThisReference2() {
        assertEquals(true, parseDirect("this.funMap") instanceof Map);
    }

    public void testThisReference3() {
        assertEquals(true, parseDirect("this is 'org.mvel.tests.Base'"));
    }


    public void testStringEscaping() {
        assertEquals("\"Mike Brock\"", parseDirect("\"\\\"Mike Brock\\\"\""));
    }

    public void testStringEscaping2() {
        assertEquals("MVEL's Parser is Fast", parseDirect("'MVEL\\'s Parser is Fast'"));
    }

    public void testEvalToBoolean() {
        assertEquals(true, (boolean) ExpressionParser.evalToBoolean("true ", "true"));
        assertEquals(true, (boolean) ExpressionParser.evalToBoolean("true ", "true"));
    }

//    public void testCompiledListStructures() {
//        Serializable compiled = ExpressionParser.compileExpression("[\"test\", \"yeolpass\"] contains \"yeolpass\"");
//        assertEquals(true, ExpressionParser.executeExpression(compiled));
//    }

    public void testCompiledMapStructures() {
        Serializable compiled = ExpressionParser.compileExpression("['foo':'bar'] contains 'foo'");
        ExpressionParser.executeExpression(compiled, null, null, Boolean.class);
    }

    public void testSubListInMap() {
       assertEquals("pear", parseDirect("map = ['test' : 'poo', 'foo' : [c, 'pear']]; map['foo'][1]"));
    }

    public void testCompiledMethodCall() {
        Serializable compiled = ExpressionParser.compileExpression("c.getClass()");
        assertEquals(String.class, ExpressionParser.executeExpression(compiled, base, map));
    }

    public void testStaticNamespaceCall() {
        assertEquals(java.util.ArrayList.class, parseDirect("java.util.ArrayList"));
    }

    public void testStaticNamespaceClassWithMethod() {
        assertEquals("FooBar", parseDirect("java.lang.String.valueOf('FooBar')"));
    }


    public Object parseDirect(String ex) {
        return compiledExecute(ex);
    }

    public Object compiledExecute(String ex) {
        Serializable compiled = ExpressionParser.compileExpression(ex);
        return ExpressionParser.executeExpression(compiled, base, map);
    }

    public void testToList() {
        String expr = "misc.toList(foo, 'hello', 42, ['key1' : 'value1', c : [ foo, 'car', 42 ]], [42, [c : 'value1']] )";
        List list = (List) parseDirect(expr);
        assertSame(foo, list.get(0));
        assertEquals("hello", list.get(1));
        assertEquals(new Integer(42), list.get(2));
        Map map = (Map) list.get(3);
        assertEquals("value1", map.get("key1"));

        List nestedList = (List) map.get("cat");
        assertSame(foo, nestedList.get(0));
        assertEquals("car", nestedList.get(1));
        assertEquals(new BigDecimal(42), nestedList.get(2));

        nestedList = (List) list.get(4);
        assertEquals(new BigDecimal(42), nestedList.get(0));
        map = (Map) nestedList.get(1);
        assertEquals("value1", map.get("cat"));
    }

    public class MiscTestClass {
        public List toList(Object object1, String string, int integer, Map map, List list) {
            List l = new ArrayList();
            l.add(object1);
            l.add(string);
            l.add(new Integer(integer));
            l.add(map);
            l.add(list);
            return l;
        }
    }
}
