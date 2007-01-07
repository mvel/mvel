package org.mvel.tests;

import junit.framework.TestCase;
import org.mvel.ExpressionParser;
import static org.mvel.ExpressionParser.evalToBoolean;
import org.mvel.Interpreter;
import org.mvel.tests.res.Bar;
import org.mvel.tests.res.Base;
import org.mvel.tests.res.Foo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ParserUnitTest extends TestCase {
    Foo foo = new Foo();
    Map<String, Object> map = new HashMap<String, Object>();
    Base base = new Base();

    public ParserUnitTest() {
        foo.setBar(new Bar());
        map.put("foo", foo);
        map.put("a", null);
        map.put("b", null);
        map.put("c", "cat");
        map.put("BWAH", "");

   //     map.put("misc", new MiscTestClass());

        map.put("pi", "3.14");
        map.put("hour", "60");
        map.put("zero", 0);

        map.put("doubleTen", new Double(10));
    }

    public void testPassThru() {
        assertEquals("poopy", parse("poopy"));
    }

    public void testMethodOnValue() {
        assertEquals("DOG", parse("@{foo.bar.name.toUpperCase()}"));
    }

    public void testSimpleProperty() {
        assertEquals("dog", parse("@{foo.bar.name}"));
    }

    public void testBooleanOperator() {
        assertEquals(true, parse("@{foo.bar.woof == true}"));
    }

    public void testBooleanOperator2() {
        assertEquals(false, parse("@{foo.bar.woof == false}"));
    }

    public void testTextComparison() {
        assertEquals(true, parse("@{foo.bar.name == 'dog'}"));
    }

    public void testNETextComparison() {
        assertEquals(true, parse("@{foo.bar.name != 'foo'}"));
    }

    public void testChor() {
        assertEquals("cat", parse("@{a or b or c}"));
    }

    public void testChorWithLiteral() {
        assertEquals("fubar", parse("@{a or 'fubar'}"));
    }

    public void testNullCompare() {
        assertEquals(true, parse("@{c != null}"));
    }

    public void testAnd() {
        assertEquals(true, parse("@{c != null && foo.bar.name == 'dog' && foo.bar.woof}"));
    }

    public void testMath() {
        assertEquals(188.4, parse("@{pi * hour}"));
    }

    public void testTemplating() {
        assertEquals("dogDOGGIE133.5", parse("@{foo.bar.name}DOGGIE@{hour*2.225+1-1}"));
    }

    public void testComplexAnd() {
        assertEquals(true, parse("@{(pi * hour) > 0 && foo.happy() == 'happyBar'}"));
    }

    public void testModulus() {
        assertEquals(38392 % 2,
                parse("@{38392 % 2}"));
    }


    public void testLessThan() {
        assertEquals(true, parse("@{pi < 3.15}"));
        assertEquals(true, parse("@{pi <= 3.14}"));
        assertEquals(false, parse("@{pi > 3.14}"));
        assertEquals(true, parse("@{pi >= 3.14}"));
    }

    public void testMethodAccess() {
        assertEquals("happyBar", parse("@{foo.happy()}"));
    }

    public void testMethodAccess2() {
        assertEquals("FUBAR", parse("@{foo.toUC('fubar')}"));
    }

    public void testMethodAccess3() {
        assertEquals(true, parse("@{equalityCheck(c, 'cat')}"));
    }

    public void testMethodAccess4() {
        assertEquals(null, parse("@{readBack(null)}"));
    }

    public void testMethodAccess5() {
        assertEquals("nulltest", parse("@{appendTwoStrings(null, 'test')}"));
    }

    public void testMethodAccess6() {
        assertEquals(false, parse("@{!foo.bar.isWoof()}"));
    }

    public void testNegation() {
        assertEquals(true, parse("@{!fun && !fun}"));
    }

    public void testNegation2() {
        assertEquals(false, parse("@{fun && !fun}"));
    }

    public void testNegation3() {
        assertEquals(true, parse("@{!(fun && fun)}"));
    }

    public void testNegation4() {
        assertEquals(false, parse("@{(fun && fun)}"));
    }

    public void testMultiStatement() {
        assertEquals(true, parse("@{populate(); barfoo == 'sarah'}"));
    }

    public void testAssignment() {
        assertEquals(true, parseDirect("populate(); blahfoo = barfoo; blahfoo == 'sarah'"));
    }

    public void testAssignment2() {
        assertEquals("sarah", parse("@{populate(); blahfoo = barfoo}"));
    }

    public void testOr() {
        assertEquals(true, parse("@{fun || true}"));
    }

    public void testLiteralPassThrough() {
        assertEquals(true, parse("@{true}"));
    }

    public void testLiteralPassThrough2() {
        assertEquals(false, parse("@{false}"));
    }

    public void testLiteralPassThrough3() {
        assertEquals(null, parse("@{null}"));
    }

    public void testControlLoop() {
        assertEquals("HappyHappy!JoyJoy!",
                parse(
                        "@foreach{list as fun}" +
                                "@{fun}" +
                                "@end{}"
                ));
    }

    public void testControlLoopMultiple() {
        for (int i = 0; i < 100; i++) {
            testControlLoop();
        }
    }

    public void testControlLoop2() {
        assertEquals("HappyHappy!JoyJoy!",
                parse(
                        "@foreach{list}" +
                                "@{item}" +
                                "@end{}"
                ));
    }

    public void testIfStatement() {
        assertEquals("sarah", parse("@if{'fun' == 'fun'}sarah@end{}"));
    }

    public void testIfStatement2() {
        assertEquals("poo", parse("@if{'fun' == 'bar'}sarah@else{}poo@end{}"));
    }

    public void testRegEx() {
        assertEquals(true, parse("@{foo.bar.name ~= '[a-z].+'}"));
    }

    public void testRegExNegate() {
        assertEquals(false, parse("@{!(foo.bar.name ~= '[a-z].+')}"));
    }

    public void testRegEx2() {
        assertEquals(true, parse("@{foo.bar.name ~= '[a-z].+' && foo.bar.name != null}"));
    }

    public void testBlank() {
        assertEquals(true, parse("@{'' == empty}"));
    }

    public void testBlank2() {
        assertEquals(true, parse("@{BWAH == empty}"));
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

    public void testBooleanModeOnly5() {
        assertEquals(false, (Object) evalToBoolean("!foo.bar.isFoo(this.foo)", base, map));
    }

    public void testBooleanModeOnly6() {
        for (int i = 0; i < 25; i++) {
            testBooleanModeOnly5();
        }
    }

    public void testTernary() {
        assertEquals("foobie", parse("@{zero==0?'foobie':zero}"));
    }

    public void testTernary2() {
        assertEquals("blimpie", parse("@{zero==1?'foobie':'blimpie'}"));
    }

    public void testTernary3() {
        assertEquals("foobiebarbie", parse("@{zero==1?'foobie':'foobie'+'barbie'}"));
    }

    public void testTernary4() {
        assertEquals("no", parse("@{ackbar ? 'yes' : 'no'}"));
    }

    public void testStrAppend() {
        assertEquals("foobarcar", parse("@{'foo' + 'bar' + 'car'}"));
    }

    public void testStrAppend2() {
        assertEquals("foobarcar1", parse("@{'foobar' + 'car' + 1}"));
    }

    public void testInstanceCheck1() {
        assertEquals(true, parse("@{c is 'java.lang.String'}"));
    }

    public void testInstanceCheck2() {
        assertEquals(false, parse("@{pi is 'java.lang.Integer'}"));
    }

    public void testBitwiseOr1() {
        assertEquals(6, parse("@{2 | 4}"));
    }

    public void testBitwiseOr2() {
        assertEquals(true, parse("@{(2 | 1) > 0}"));
    }

    public void testBitwiseOr3() {
        assertEquals(true, parse("@{(2 | 1) == 3}"));
    }

    public void testBitwiseAnd1() {
        assertEquals(2, parse("@{2 & 3}"));
    }

    public void testShiftLeft() {
        assertEquals(4, parse("@{2 << 1}"));
    }

    public void testUnsignedShiftLeft() {
        assertEquals(2, parse("@{-2 <<< 0}"));
    }

    public void testShiftRight() {
        assertEquals(128, parse("@{256 >> 1}"));
    }

    public void testXOR() {
        assertEquals(3, parse("@{1 ^ 2}"));
    }

    public void testContains1() {
        assertEquals(true, parse("@{list contains 'Happy!'}"));
    }

    public void testContains2() {
        assertEquals(false, parse("@{list contains 'Foobie'}"));
    }

    public void testContains3() {
        assertEquals(true, parse("@{sentence contains 'fox'}"));
    }

    public void testContains4() {
        assertEquals(false, parse("@{sentence contains 'mike'}"));
    }

    public void testContains5() {
        assertEquals(true, parse("@{!(sentence contains 'mike')}"));
    }

    public void testInvert() {
        assertEquals(~10, parseDirect("~10"));
        assertEquals(~(10 + 1), parseDirect("~(10 + 1)"));
        assertEquals(~10 + (1 + ~50), parseDirect("~10 + (1 + ~50)"));
    }


    public void testNumericInteroperability() {
        assertEquals(true, parseDirect("doubleTen > 5"));
    }


    public void testExpression10() {
        assertEquals(true, parse("@{10 + 1 > 5 && 10 + 1 < 20}"));
    }

    public void testArrayAccess() {
        assertEquals("dog", parseDirect("testArray[0].bar.name"));
    }

    public void testArrayCreation() {
        assertEquals("foobie", parseDirect("a = {{'foo', 'bar'}, {'foobie', 'barbie'}}; a[1][0]"));
    }

    public void testTokenMethodAccess() {
        assertEquals(String.class, parse("@{a = 'foo'; a.getClass()}"));
    }

    public void testComplexExpression() {
        assertEquals("bar", parse("@{a = 'foo'; b = 'bar'; c = 'jim'; list = {a,b,c}; list[1]}"));
    }

    public void testComplexExpression2() {
        assertEquals("foobar", parse("@{x = 'bar'; y = 'foo'; array = {y,x}; array[0] + array[1]}"));
    }

    public void testListCreation() {
        assertEquals("foobar", parse("@{test = ['apple', 'pear', 'foobar']; test[2]}"));
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
        assertEquals(2, parse("@{Array.getLength({'foo', 'bar'})}"));
    }

    public void testMapCreation() {
        assertEquals("sarah", parse("@{map = ['mike':'sarah','tom':'jacquelin']; map['mike']}"));
    }

    public void testProjectionSupport() {
        assertEquals(true, parse("@{(name in things) contains 'Bob'}"));
    }

    public void testProjectionSupport2() {
        assertEquals(3, parse("@{(('name') in things).size()}"));
    }


    public void testStaticMethodFromLiteral() {
        assertEquals(String.class.getName(), parse("@{String.valueOf(Class.forName('java.lang.String').getName())}"));
    }

    public void testStaticCalls() {
        assertTrue(Math.abs(1.1d) == ((Double) parse("@{Math.abs(1.1)}")));
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
        parse("@{new java.lang.String('foobie')}");
    }

    public void testObjectInstantiationWithMethodCall() {
        parse("@{new String('foobie').toString()}");
    }

    public void testObjectInstantiation2() {
        parse("@{new String() is String}");
    }

    public void testObjectInstantiation3() {
        parseDirect("new java.text.SimpleDateFormat('yyyy').format(new java.util.Date(System.currentTimeMillis()))");
    }

    public void testArrayCoercion() {
        assertEquals("gonk", parse("@{funMethod( {'gonk', 'foo'} )}"));
    }

    public void testArrayCoercion2() {
        assertEquals(10, parseDirect("sum({2,2,2,2,2})"));
    }

    public void testMapAccess() {
        assertEquals("dog", parse("@{funMap['foo'].bar.name}"));
    }

    public void testMapAccess2() {
        assertEquals("dog", parse("@{funMap.foo.bar.name}"));
    }

    public void testSoundex() {
        assertTrue((Boolean) parse("@{'foobar' soundslike 'fubar'}"));
    }

    public void testSoundex2() {
        assertFalse((Boolean) parse("@{'flexbar' soundslike 'fubar'}"));
    }

    public void testThisReference() {
        assertEquals(true, parse("@{this}") instanceof Base);
    }

    public void testThisReference2() {
        assertEquals(true, parseDirect("this.funMap") instanceof Map);
    }

    public void testThisReference3() {
        assertEquals(true, parseDirect("this is 'org.mvel.tests.res.Base'"));
    }


    public void testStringEscaping() {
        assertEquals("\"Mike Brock\"", parse("@{\"\\\"Mike Brock\\\"\"}"));
    }

    public void testStringEscaping2() {
        assertEquals("MVEL's Parser is Fast", parse("@{'MVEL\\'s Parser is Fast'}"));
    }

    public void testThisReferenceInMethodCall() {
        assertEquals(101, parseDirect("Integer.parseInt(this.number)"));
    }

    public void testInlineVarAssignment() {
        assertTrue((Boolean) parseDirect("x = ((a = 100) + (b = 200) + (c = 300)); (a == 100 && b == 200 && c == 300 && x == 600)"));
    }
    

    public void testEvalToBoolean() {
        assertEquals(true, (boolean) ExpressionParser.evalToBoolean("true ", "true"));
        assertEquals(true, (boolean) ExpressionParser.evalToBoolean("true ", "true"));
    }

//    public void testCompiledListStructures() {
//        Serializable compiled = ExpressionParser.compileExpression("[\"test\", \"yeolpass\"] contains \"yeolpass\"");
//        assertEquals(true, ExpressionParser.executeExpression(compiled));
//    }

//    public void testCompiledMapStructures() {
//        Serializable compiled = ExpressionParser.compileExpression("['foo':'bar'] contains 'foo'");
//        ExpressionParser.executeExpression(compiled, null, null, Boolean.class);
//    }

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

    public Object parse(String ex) {
        return Interpreter.parse(ex, base, map);
    }

    public Object parseDirect(String ex) {
      //  return compiledExecute(ex);
        return ExpressionParser.eval(ex, base, map);
    }

    public Object compiledExecute(String ex) {
        Serializable compiled = ExpressionParser.compileExpression(ex);
        return ExpressionParser.executeExpression(compiled, base, map);
    }

//    public void testToList() {
//        String text = "misc.toList(foo.bar.name, 'hello', 42, ['key1' : 'value1', c : [ foo.bar.age, 'car', 42 ]], [42, [c : 'value1']] )";
//        List list = null; //(List) parseDirect(text);
//
//        Object expr = ExpressionParser.compileExpression( text );
//        list =  ( List  ) ExpressionParser.executeExpression(expr, null, map);
//
//        int count = 10000000;
//        long start = System.currentTimeMillis();
//        for ( int i = 0; i < count; i++ ) {
//            list =  ( List  ) ExpressionParser.executeExpression(expr, null, map);
//        }
//        long end = System.currentTimeMillis();
//        System.out.println( end  - start);
//
//        assertSame( "dog", list.compileGetChain( 0 ) );
//        assertEquals( "hello", list.compileGetChain( 1 ) );
//        assertEquals( new Integer( 42 ), list.compileGetChain( 2 ) );
//        Map map = ( Map ) list.compileGetChain( 3 );
//        assertEquals( "value1", map.compileGetChain( "key1" ) );
//
//        List nestedList = ( List ) map.compileGetChain(  "cat" );
//        assertEquals(  new Integer(14), nestedList.compileGetChain( 0 )  );
//        assertEquals( "car", nestedList.compileGetChain( 1 )  );
//        assertEquals( new BigDecimal(42), nestedList.compileGetChain( 2 )  );
//
//        nestedList  = (List) list.compileGetChain( 4 );
//        assertEquals( new BigDecimal(42), nestedList.compileGetChain( 0 )  );
//        map = ( Map ) nestedList.compileGetChain( 1 );
//        assertEquals( "value1", map.compileGetChain( "cat" )  );
//    }
//
//    public class MiscTestClass {
//        public List toList(Object object1, String string, int integer, Map map, List list) {
//            List l = new ArrayList();
//            l.add(object1);
//            l.add(string);
//            l.add(new Integer(integer));
//            l.add(map);
//            l.add(list);
//            return l;
//        }
//    }
}
