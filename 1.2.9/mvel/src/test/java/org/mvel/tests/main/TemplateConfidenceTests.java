package org.mvel.tests.main;

import junit.framework.TestCase;
import junit.framework.TestCase;
import org.mvel.MVEL;
import org.mvel.MVELTemplateRegistry;
import org.mvel.TemplateInterpreter;
import org.mvel.TemplateRegistry;
import org.mvel.tests.main.res.Bar;
import org.mvel.tests.main.res.Base;
import org.mvel.tests.main.res.Foo;
import org.mvel.tests.main.res.PDFFieldUtil;
import org.mvel.util.FastList;

import java.io.Serializable;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class TemplateConfidenceTests extends TestCase {
    Foo foo = new Foo();
    Map<String, Object> map = new HashMap<String, Object>();
    Base base = new Base();

    public TemplateConfidenceTests() {
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

        //noinspection UnnecessaryBoxing
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
    
    public void testPassThru2() {
        assertEquals("foo@bar.com", TemplateInterpreter.eval("foo@bar.com", map));
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
        assertEquals(188.4f, parse("@{pi * hour}"));
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

    public void testControlLoopList() {
        assertEquals("HappyHappy!JoyJoy!",
                parse(
                        "@foreach{list as fun}" +
                                "@{fun}" +
                                "@end{}"
                ));
    }

    public void testControlLoopArray() {
        assertEquals("Happy0Happy!1Joy2Joy!3",
                parse(
                        "@foreach{array as fun}" +
                                "@{fun}@{i0}" +
                                "@end{}"
                ));
    }

    public void testMultiCollectionControlLoop() {
        assertEquals("HappyHappy0Happy!Happy!1JoyJoy2Joy!Joy!3",
                parse(
                        "@foreach{list, array as listItem, arrayItem}" +
                                "@{listItem}@{arrayItem}@{i0}" +
                                "@end{}"
                ));
    }

    public void testMultiCollectionWithSingleCharSeperatorControlLoop() {
        assertEquals("Happy0Happy,Happy!1Happy!,Joy2Joy,Joy!3Joy!",
                parse(
                        "@foreach{list, array as listItem, arrayItem}" +
                                "@{listItem}@{i0}@{arrayItem}" +
                                "@end{\",\"  }"
                ));
    }

    public void testMultiCollectionWithMultipleCharSeperatorControlLoop() {
        assertEquals("HappyHappy,|Happy!Happy!,|JoyJoy,|Joy!Joy!",
                parse(
                        "@foreach{list, array as listItem, arrayItem}" +
                                "@{listItem}@{arrayItem}" +
                                "@end{\",|\"  }"
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


    public void testMultiCollectionWithSingleCharSeperatorControlLoopMultiple() {
        for (int i = 0; i < 100; i++) {
            testMultiCollectionWithSingleCharSeperatorControlLoop();
        }
    }

    public void testMultiCollectionWithMultipleCharSeperatorControlLoopMultiple() {
        for (int i = 0; i < 100; i++) {
            testMultiCollectionWithMultipleCharSeperatorControlLoop();
        }
    }    

    public static interface TestInterface {
        public String getName();

        public boolean isFoo();
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
    
    public void testTokenMethodAccess() {
        assertEquals(String.class, parse("@{a = 'foo'; a.getClass()}"));
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
    
    public void testObjectInstantiation() {
        assertEquals("foobie", parse("@{new java.lang.String('foobie')}"));
    }

    public void testObjectInstantiationWithMethodCall() {
        assertEquals("foobie", parse("@{new String('foobie').toString()}"));
    }

    public void testObjectInstantiation2() {
        parse("@{new String() is String}");
    }    
    
    public void testArrayCoercion() {
        assertEquals("gonk", parse("@{funMethod( {'gonk', 'foo'} )}"));
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

    public void testIncludeByRef() {
        TemplateRegistry registry = new MVELTemplateRegistry();
        registry.registerTemplate("templateName", "@{var1}@{var2}");

        assertEquals("xvalue1catx", parse("x@includeByRef{templateName(var1 = \"value1\", var2 = c)}x", registry));
    }

    public void testIncludeByRefNoParams() {
        TemplateRegistry registry = new MVELTemplateRegistry();
        registry.registerTemplate("templateName", "hello");

        assertEquals("xhellox", parse("x@includeByRef{templateName()}x", registry));
    }

    public void testIncludeByRefNoSpaces() {
        TemplateRegistry registry = new MVELTemplateRegistry();
        registry.registerTemplate("templateName", "@{var1}@{var2}");

        assertEquals("xvalue1catx", parse("x@includeByRef{templateName(var1=\"value1\", var2=c)}x", registry));
    }


    public void testRegisterTemplateGroup() {
        StringReader reader = new StringReader("myTemplate1() ::=<<@{var1}>>=::  myTemplate2() ::=<<@{var2}>>=::");
        TemplateRegistry registry = new MVELTemplateRegistry();
        registry.registerTemplate(reader);

        assertEquals("xvalue1catx", parse("x@includeByRef{myTemplate1(var1 = \"value1\")}@includeByRef{myTemplate2(var2 = c)}x", registry));
    }

    public void testRecursiveRegisterTemplateGroup() {
        StringReader reader = new StringReader("myTemplate1() ::=<<@{var1}@includeByRef{myTemplate2(var2 = var2)}>>=::  myTemplate2() ::=<<@{var2}>>=::");
        TemplateRegistry registry = new MVELTemplateRegistry();
        registry.registerTemplate(reader);

        assertEquals("xvalue1catx", parse("x@includeByRef{myTemplate1(var1 = \"value1\", var2 = c)}x", registry));
    }

    public void testIfLoopInTemplate() {
        assertEquals("ONETWOTHREE", parse("@foreach{things}@if{item.name=='Bob'}ONE@elseif{item.name=='Smith'}TWO@elseif{item.name=='Cow'}THREE@end{}@end{}"));
    }

    public void testStringEscaping() {
        assertEquals("\"Mike Brock\"", parse("@{\"\\\"Mike Brock\\\"\"}"));
    }

    public void testStringEscaping2() {
        assertEquals("MVEL's Parser is Fast", parse("@{'MVEL\\'s Parser is Fast'}"));
    }


    public Object parse(String ex, TemplateRegistry registry) {
        return TemplateInterpreter.parse(ex, base, map, registry);
    }

    public Object parse(String ex) {
        return TemplateInterpreter.parse(ex, base, map);
    }

}
