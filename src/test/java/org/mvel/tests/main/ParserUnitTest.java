package org.mvel.tests.main;

import junit.framework.TestCase;
import org.mvel.Interpreter;
import org.mvel.MVEL;
import org.mvel.tests.main.res.Bar;
import org.mvel.tests.main.res.Base;
import org.mvel.tests.main.res.Foo;
import org.mvel.tests.main.res.PDFFieldUtil;
import org.mvel.util.FastList;

import java.io.Serializable;
import java.util.Calendar;
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

        map.put("variable_with_underscore", "HELLO");

        map.put("testImpl",
                new TestInterface() {

                    public String getName() {
                        return "FOOBAR!";
                    }


                    public boolean isFoo() {
                        return true;
                    }
                });

    }

    public static interface TestInterface {
        public String getName();

        public boolean isFoo();
    }

    public void testPassThru() {
        assertEquals("poopy", parse("poopy"));
    }

    public void testPassThru2() {
        assertEquals("foo@bar.com", Interpreter.eval("foo@bar.com", map));
    }

    public void testMethodOnValue() {
        assertEquals("DOG", parse("@{foo.bar.name.toUpperCase()}"));
    }

    public void testSimpleProperty() {
        assertEquals("dog", parse("@{foo.bar.name}"));
    }

    public void testThroughInterface() {
        assertEquals("FOOBAR!", parseDirect("testImpl.name"));
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

    public void testTextComparison2() {
        assertEquals(false, parseDirect("null == foo.bar.name"));
    }

    public void testNENumbers() {
        assertEquals(true, parseDirect("10 != 9"));
    }

    public void testNENumbers2() {
        assertEquals(true, parseDirect("10 != null"));
    }

    public void testNENumbers3() {
        assertEquals(true, parseDirect("null != 10"));
    }

    public void testShortPathExpression() {
        assertEquals(null, parseDirect("3 > 4 && foo.toUC('test'); foo.register"));
    }

    public void testShortPathExpression2() {
        assertEquals(true, parseDirect("4 > 3 || foo.toUC('test')"));
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
        assertEquals(188.4f, parse("@{pi * hour}"));
    }

    public void testMath2() {
        assertEquals(10f / 4f, parseDirect("10 / 4"));
    }

    public void testMath3() {
        assertEquals(10 + 1 - 1, parseDirect("10 + 1 - 1"));
    }

    public void testMath4() {
        assertEquals(100, parseDirect("10 ** 2"));
    }

    public void testTemplating() {
        assertEquals("dogDOGGIE133.5", parse("@{foo.bar.name}DOGGIE@{hour*2.225+1-1}"));
    }

    public void testComplexAnd() {
        assertEquals(true, parse("@{(pi * hour) > 0 && foo.happy() == 'happyBar'}"));
    }

    public void testGthan() {
        assertEquals(true, (boolean) parseBooleanMode("hour>0"));
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

    public void testAssignment3() {
        assertEquals(102, parseDirect("a = 100 + 1 + 1"));
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

    public void testControlLoopList() {
        assertEquals("HappyHappy!JoyJoy!",
                parse(
                        "@foreach{list as fun}" +
                                "@{fun}" +
                                "@end{}"
                ));
    }
    
    public void testControlLoopArray() {
        assertEquals("HappyHappy!JoyJoy!",
                parse(
                        "@foreach{array as fun}" +
                                "@{fun}" +
                                "@end{}"
                ));
    }    
    
    public void testMultiCollectionControlLoop() {
        assertEquals("HappyHappyHappy!Happy!JoyJoyJoy!Joy!",
                     parse(
                             "@foreach{list, array as listItem, arrayItem}" +
                                     "@{listItem}@{arrayItem}" +
                                     "@end{}"
                     ));        
    }
    
    public void testMultiCollectionWithSeperatorControlLoop() {
        assertEquals("Happy,Happy,Happy!,Happy!,Joy,Joy,Joy!,Joy!",
                     parse(
                             "@foreach{list, array as listItem, arrayItem}" +
                                     "@{listItem}@{arrayItem}" +
                                     "@end{\",\"}"
                     ));        
    }    

    public void testControlLoopListMultiple() {
        for (int i = 0; i < 100; i++) {
            testControlLoopList();
        }
    }
    
    public void testControlLoopArrayMultiple() {
        for (int i = 0; i < 100; i++) {
            testControlLoopArray();
        }
    }    
    
    public void testMultiCollectionControlLoopMultiple() {
        for (int i = 0; i < 100; i++) {
            testMultiCollectionControlLoop();
        }
    }       

    
    public void testMultiCollectionWithSeperatorControlLoopMultiple() {
        for (int i = 0; i < 100; i++) {
            testMultiCollectionWithSeperatorControlLoop();
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

    public void testControlLoop3() {
        assertEquals("HappyHappy!JoyJoy!",
                parse(
                        "@foreach{ list }" +
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


    public void testBooleanModeOnly2() {
        assertEquals(false, (Object) MVEL.evalToBoolean("BWAH", base, map));
    }

    public void testBooleanModeOnly4() {
        assertEquals(true, (Object) MVEL.evalToBoolean("hour == (hour + 0)", base, map));
    }

    public void testBooleanModeOnly5() {
        assertEquals(false, (Object) MVEL.evalToBoolean("!foo.bar.isFoo(this.foo)", base, map));
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

    public void testTernary5() {
        assertEquals("<<FooBar>>", parseDirect("ackbar ? 'Ack' : '<<FooBar>>'"));
    }

    public void testTernary6() {
        assertEquals("<Ack>", parseDirect("!ackbar ? '<Ack>' : '<<FooBar>>'"));
    }

    public void testStrAppend() {
        assertEquals("foobarcar", parse("@{'foo' + 'bar' + 'car'}"));
    }

    public void testStrAppendForce() {
        assertEquals("11", parseDirect("1 # 1"));
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

    public void testConversionCheck() {
        assertEquals(true, parseDirect("pi convertable_to java.math.BigDecimal"));
    }

    public void testConversionCheck2() {
        assertEquals(true, parseDirect("pi convertable_to java.lang.Integer"));
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

    public void testUnsignedRightShift() {
        assertEquals(-5 >>> 2, parseDirect("-5 >>> 2"));
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
    }

    public void testInvert2() {
        assertEquals(~(10 + 1), parseDirect("~(10 + 1)"));
    }

    public void testInvert3() {
        assertEquals(~10 + (1 + ~50), parseDirect("~10 + (1 + ~50)"));
    }


    public void testNumericInteroperability() {
        assertEquals(true, parseDirect("doubleTen > 5"));
    }

    public void testExpression10() {
        assertEquals(true, parseDirect("10 + 1 > 5 && 10 + 1 < 20"));
    }

    public void testArrayAccess() {
        assertEquals("dog", parseDirect("testArray[0].bar.name"));
    }

    public void testArrayCreation() {
        assertEquals("foobie", parseDirect("a = {{'foo', 'bar'}, {'foobie', 'barbie'}}; a[1][0]"));
    }

    public void testArrayCreation2() {
        assertEquals(5, parseDirect("a = {1,3,5}; a[2]"));
    }

    public void testTokenMethodAccess() {
        assertEquals(String.class, parse("@{a = 'foo'; a.getClass()}"));
    }

    public void testVariableAccess() {
        assertEquals("HELLO", parseDirect("variable_with_underscore"));
    }

    public void testMapAccess3() {
        assertEquals("happyBar", parseDirect("funMap.foo_bar.happy()"));
    }

    public void testMapAccess4() {
        assertEquals("happyBar", parseDirect("funMap['foo'].happy()"));
    }

    public void testMapAsMethodParm() {
        assertEquals("happyBar", parseDirect("readBack(funMap.foo_bar.happy())"));
    }

    public void testComplexExpression() {
        assertEquals("bar", parseDirect("a = 'foo'; b = 'bar'; c = 'jim'; list = {a,b,c}; list[1]"));
    }

    public void testComplexExpression2() {
        assertEquals("foobar", parseDirect("x = 'bar'; y = 'foo'; array = {y,x}; array[0] + array[1]"));
    }

    public void testListCreation() {
        assertEquals("foobar", parseDirect("test = ['apple', 'pear', 'foobar']; test[2]"));
    }

    public void testListCreation2() {
        assertEquals(FastList.class, parseDirect("[\"test\"]").getClass());
    }

    public void testListCreation3() {
        assertEquals(FastList.class, parseDirect("[66]").getClass());
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
        assertEquals(3, parse("@{(name in things).size()}"));
    }


    public void testStaticMethodFromLiteral() {
        assertEquals(String.class.getName(), parse("@{String.valueOf(Class.forName('java.lang.String').getName())}"));
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
        assertEquals("foobie", parse("@{new java.lang.String('foobie')}"));
    }

    public void testObjectInstantiationWithMethodCall() {
        assertEquals("foobie", parse("@{new String('foobie').toString()}"));
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
        assertEquals(true, parseDirect("this is 'org.mvel.tests.main.res.Base'"));
    }


    public void testStringEscaping() {
        assertEquals("\"Mike Brock\"", parse("@{\"\\\"Mike Brock\\\"\"}"));
    }

    public void testStringEscaping2() {
        assertEquals("MVEL's Parser is Fast", parse("@{'MVEL\\'s Parser is Fast'}"));
    }

    public void testThisReferenceInConstructor() {
        assertEquals("101", parseDirect("new String(this.number)"));
    }

    public void testChainedMethodCallOnConstructor() {
        assertEquals(String.class, parseDirect("new String('foo').getClass()"));
    }

    public void testThisReferenceInMethodCall() {
        assertEquals(101, parseDirect("Integer.parseInt(this.number)"));
    }

    public void testInlineVarAssignment() {
        assertTrue((Boolean) parseDirect("x = ((a = 100) + (b = 200) + (c = 300)); (a == 100 && b == 200 && c == 300 && x == 600)"));
    }


    public void testEvalToBoolean() {
        assertEquals(true, (boolean) MVEL.evalToBoolean("true ", "true"));
        assertEquals(true, (boolean) MVEL.evalToBoolean("true ", "true"));
    }

    public void testCompiledMethodCall() {
        Serializable compiled = MVEL.compileExpression("c.getClass()");
        assertEquals(String.class, MVEL.executeExpression(compiled, base, map));
    }

    public void testStaticNamespaceCall() {
        assertEquals(java.util.ArrayList.class, parseDirect("java.util.ArrayList"));
    }

    public void testStaticNamespaceClassWithMethod() {
        assertEquals("FooBar", parseDirect("java.lang.String.valueOf('FooBar')"));
    }

    public void testForeAch2() {
        assertEquals(6, parseDirect("total = 0; a = {1,2,3}; foreach (item : a) { total = total + item }; total"));
    }

    public void testForEach3() {
        assertEquals(true, parseDirect("a = {1,2,3}; foreach (i : a)\n{\nif (i == 1){\t return true; } \n}"));
    }


    public void testStaticNamespaceClassWithField() {
        assertEquals(String.CASE_INSENSITIVE_ORDER, parseDirect("java.lang.String.CASE_INSENSITIVE_ORDER"));
    }

    public Object parse(String ex) {
        return Interpreter.parse(ex, base, map);
    }

    public Object parseDirect(String ex) {
        //  return compiledExecute(ex);
        return MVEL.eval(ex, base, map);
    }

    public Boolean parseBooleanMode(String ex) {
        return MVEL.evalToBoolean(ex, base, map);
    }

    public Object compiledExecute(String ex) {
        Serializable compiled = MVEL.compileExpression(ex);
        return MVEL.executeExpression(compiled, base, map);
    }

    public void calculateAge() {
        System.out.println("Calculating the Age");
        Calendar c1 = Calendar.getInstance();
        c1.set(1999, 0, 10); // 1999 jan 20
        Map objectMap = new HashMap(1);
        Map propertyMap = new HashMap(1);
        propertyMap.put("GEBDAT", c1.getTime());
        objectMap.put("EV_VI_ANT1", propertyMap);
        System.out.println(new PDFFieldUtil().calculateAge(c1.getTime()));
        System.out.println(MVEL.eval("new org.mvel.tests.main.res.PDFFieldUtil().calculateAge(EV_VI_ANT1.GEBDAT) >= 25 ? 'X' : ''"
                , null, objectMap));
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
//        assertSame( "dog", list.compileAccessor( 0 ) );
//        assertEquals( "hello", list.compileAccessor( 1 ) );
//        assertEquals( new Integer( 42 ), list.compileAccessor( 2 ) );
//        Map map = ( Map ) list.compileAccessor( 3 );
//        assertEquals( "value1", map.compileAccessor( "key1" ) );
//
//        List nestedList = ( List ) map.compileAccessor(  "cat" );
//        assertEquals(  new Integer(14), nestedList.compileAccessor( 0 )  );
//        assertEquals( "car", nestedList.compileAccessor( 1 )  );
//        assertEquals( new BigDecimal(42), nestedList.compileAccessor( 2 )  );
//
//        nestedList  = (List) list.compileAccessor( 4 );
//        assertEquals( new BigDecimal(42), nestedList.compileAccessor( 0 )  );
//        map = ( Map ) nestedList.compileAccessor( 1 );
//        assertEquals( "value1", map.compileAccessor( "cat" )  );
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
