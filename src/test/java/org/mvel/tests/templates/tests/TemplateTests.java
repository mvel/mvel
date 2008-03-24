package org.mvel.templates.tests;

import junit.framework.TestCase;
import org.mvel.templates.*;
import static org.mvel.templates.TemplateCompiler.compileTemplate;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.MapVariableResolverFactory;
import org.mvel.tests.main.res.Foo;
import org.mvel.tests.main.res.Base;
import org.mvel.tests.main.res.Bar;

import org.mvel.*;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;


public class TemplateTests extends TestCase {
    private Map<String, Object> map = new HashMap<String, Object>();
    private VariableResolverFactory vrf = new MapVariableResolverFactory(map);
    private Foo foo = new Foo();
    private Base base = new Base();


    public TemplateTests() {
        map.put("_foo_", "Foo");
        map.put("_bar_", "Bar");

        ArrayList list = new ArrayList(3);
        list.add("Jane");
        list.add("John");
        list.add("Foo");

        map.put("arrayList", list);

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

    public Object test(String template) {
        CompiledTemplate compiled = compileTemplate(template);
        //  TemplateDebug.decompile(compiled, template.toCharArray());
        return TemplateRuntime.execute(compiled, base, vrf);
    }

    public void testPassThru() {
        String s = "foobar!";
        assertEquals("foobar!", test(s));
    }

    public void testBasicParsing() {
        String s = "foo: @bar{_foo_}--@{_bar_}!";
        assertEquals("foo: Foo--Bar!", test(s));
    }

    public void testIfStatement() {
        String s = "@if{_foo_=='Foo'}Hello@end{}";
        assertEquals("Hello", test(s));
    }

    public void testIfStatement2() {
        String s = "@if{_foo_=='Bar'}Hello@else{_foo_=='Foo'}Goodbye@end{}";
        assertEquals("Goodbye", test(s));
    }         

    public void testIfStatement3() {
        String s = "@if{_foo_=='Bar'}Hello@else{_foo_=='foo'}Goodbye@else{}Nope@end{}";
        assertEquals("Nope", test(s));
    }

    public void testIfStatement4() {
        String s = "@if{_foo_=='Foo'}Hello@else{_foo_=='foo'}Goodbye@else{}Nope@end{}End";
        assertEquals("HelloEnd", test(s));
    }

    public void testIfStatement5() {
        String s = "@if{_foo_=='foo'}Hello@end{}Goodbye";
        assertEquals("Goodbye", test(s));
    }

    public void testIfNesting() {
        String s = "@if{_foo_=='Foo'}Hello@if{_bar_=='Bar'}Bar@end{}@else{_foo_=='foo'}Goodbye@else{}Nope@end{}";
        assertEquals("HelloBar", test(s));
    }

    public void testForEach() {
        String s = "List:@foreach{item : arrayList}@{item}@end{}";
        assertEquals("List:JaneJohnFoo", test(s));
    }

    public void testForEachMulti() {
        String s = "Multi:@foreach{item : arrayList, item2 : arrayList}@{item}-@{item2}@end{','}:Multi";
        assertEquals("Multi:Jane-Jane,John-John,Foo-Foo:Multi", test(s));
    }

    public void testComplexTemplate() {
        String s = "@foreach{item : arrayList}@if{item[0] == 'J'}@{item}@end{}@end{}";
        assertEquals("JaneJohn", test(s));
    }

    public void testInclusionOfTemplateFile() {
        String s = "<<@include{'src/tests/org/mvel/templates/tests/templateTest.mv'}>>";
        assertEquals("<<Foo::Bar>>", test(s));
    }

    public void testInclusionOfNamedTemplate() {
        SimpleTemplateRegistry registry = new SimpleTemplateRegistry();
        registry.addNamedTemplate("footemplate", compileTemplate("@{_foo_}@{_bar_}"));
        registry.addNamedTemplate("bartemplate", compileTemplate("@{_bar_}@{_foo_}"));

        String s = "@includeNamed{'footemplate'}::@includeNamed{'bartemplate'}";
        assertEquals("FooBar::BarFoo", TemplateRuntime.eval(s, map, registry));
    }

    @SuppressWarnings({"AssertEqualsBetweenInconvertibleTypes"})
    public void testExpressions() {
        String s = "@{_foo_.length()}";
        Object r = test(s);
        assertEquals(3, r);
    }

    public void testCode() {
        String s = "@code{a = 'foo'; b = 'bar'}@{a}@{b}";
        assertEquals("foobar", test(s));
    }

    public void testInlineDeclarations() {
        String s = "@declare{'fudge'}Hello @{name}!@end{}@includeNamed{'fudge'; name='John'} -- @includeNamed{'fudge'; name='Mary'}";
        assertEquals("Hello John! -- Hello Mary!", test(s));
    }

    /**
     * Integration of old tests
     */

    public void testPassThru2() {
        assertEquals("foo@bar.com", TemplateInterpreter.eval("foo@bar.com", map));
    }

    public void testMethodOnValue() {
        assertEquals("DOG", test("@{foo.bar.name.toUpperCase()}"));
    }

    public void testSimpleProperty() {
        assertEquals("dog", test("@{foo.bar.name}"));
    }

    public void testBooleanOperator() {
        assertEquals(true, test("@{foo.bar.woof == true}"));
    }

    public void testBooleanOperator2() {
        assertEquals(false, test("@{foo.bar.woof == false}"));
    }

    public void testTextComparison() {
        assertEquals(true, test("@{foo.bar.name == 'dog'}"));
    }

    public void testNETextComparison() {
        assertEquals(true, test("@{foo.bar.name != 'foo'}"));
    }

    public void testChor() {
        assertEquals("cat", test("@{a or b or c}"));
    }

    public void testChorWithLiteral() {
        assertEquals("fubar", test("@{a or 'fubar'}"));
    }

    public void testNullCompare() {
        assertEquals(true, test("@{c != null}"));
    }

    public void testAnd() {
        assertEquals(true, test("@{c != null && foo.bar.name == 'dog' && foo.bar.woof}"));
    }

    public void testMath() {
        assertEquals(188.4f, test("@{pi * hour}"));
    }

    public void testTemplating() {
        assertEquals("dogDOGGIE133.5", test("@{foo.bar.name}DOGGIE@{hour*2.225+1-1}"));
    }


    public void testComplexAnd() {
        assertEquals(true, test("@{(pi * hour) > 0 && foo.happy() == 'happyBar'}"));
    }

    public void testModulus() {
        assertEquals(38392 % 2,
                test("@{38392 % 2}"));
    }


    public void testLessThan() {
        assertEquals(true, test("@{pi < 3.15}"));
        assertEquals(true, test("@{pi <= 3.14}"));
        assertEquals(false, test("@{pi > 3.14}"));
        assertEquals(true, test("@{pi >= 3.14}"));
    }

    public void testMethodAccess() {
        assertEquals("happyBar", test("@{foo.happy()}"));
    }

    public void testMethodAccess2() {
        assertEquals("FUBAR", test("@{foo.toUC('fubar')}"));
    }

    public void testMethodAccess3() {
        assertEquals(true, test("@{equalityCheck(c, 'cat')}"));
    }

    public void testMethodAccess4() {
        assertEquals(null, test("@{readBack(null)}"));
    }

    public void testMethodAccess5() {
        assertEquals("nulltest", test("@{appendTwoStrings(null, 'test')}"));
    }

    public void testMethodAccess6() {
        assertEquals(false, test("@{!foo.bar.isWoof()}"));
    }

    public void testNegation() {
        assertEquals(true, test("@{!fun && !fun}"));
    }

    public void testNegation2() {
        assertEquals(false, test("@{fun && !fun}"));
    }

    public void testNegation3() {
        assertEquals(true, test("@{!(fun && fun)}"));
    }

    public void testNegation4() {
        assertEquals(false, test("@{(fun && fun)}"));
    }

    public void testMultiStatement() {
        assertEquals(true, test("@{populate(); barfoo == 'sarah'}"));
    }

    public void testAssignment2() {
        assertEquals("sarah", test("@{populate(); blahfoo = barfoo}"));
    }

    public void testOr() {
        assertEquals(true, test("@{fun || true}"));
    }

    public void testLiteralPassThrough() {
        assertEquals(true, test("@{true}"));
    }

    public void testLiteralPassThrough2() {
        assertEquals(false, test("@{false}"));
    }

    public void testLiteralPassThrough3() {
        assertEquals(null, test("@{null}"));
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
        assertEquals("0=Happy:Happy,1=Happy!:Happy!,2=Joy:Joy,3=Joy!:Joy!",
                parse(
                        "@foreach{list, array as listItem}" +
                                "@{i0}=@{item}:@{listItem}" +
                                "@end{','}"
                ));
    }
//
//    public void testMultiCollectionWithSingleCharSeperatorControlLoop() {
//        assertEquals("Happy0Happy,Happy!1Happy!,Joy2Joy,Joy!3Joy!",
//                parse(
//                        "@foreach{list, array as listItem, arrayItem}" +
//                                "@{listItem}@{i0}@{arrayItem}" +
//                                "@end{\",\"  }"
//                ));
//    }


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

//    public void testMultiCollectionControlLoopMultiple() {
//        for (int i = 0; i < 100; i++) {
//            testMultiCollectionControlLoop();
//        }
//    }
//
//
//    public void testMultiCollectionWithSingleCharSeperatorControlLoopMultiple() {
//        for (int i = 0; i < 100; i++) {
//            testMultiCollectionWithSingleCharSeperatorControlLoop();
//        }
//    }

//    public void testMultiCollectionWithMultipleCharSeperatorControlLoopMultiple() {
//        for (int i = 0; i < 100; i++) {
//            testMultiCollectionWithMultipleCharSeperatorControlLoop();
//        }
//    }

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

    public void testIfStatement6() {
        assertEquals("sarah", test("@if{'fun' == 'fun'}sarah@end{}"));
    }

    public void testIfStatement7() {
        assertEquals("poo", test("@if{'fun' == 'bar'}sarah@else{}poo@end{}"));
    }

    public void testRegEx() {
        assertEquals(true, test("@{foo.bar.name ~= '[a-z].+'}"));
    }

    public void testRegExNegate() {
        assertEquals(false, test("@{!(foo.bar.name ~= '[a-z].+')}"));
    }

    public void testRegEx2() {
        assertEquals(true, test("@{foo.bar.name ~= '[a-z].+' && foo.bar.name != null}"));
    }

    public void testBlank() {
        assertEquals(true, test("@{'' == empty}"));
    }

    public void testBlank2() {
        assertEquals(true, test("@{BWAH == empty}"));
    }

    public void testTernary() {
        assertEquals("foobie", test("@{zero==0?'foobie':zero}"));
    }

    public void testTernary2() {
        assertEquals("blimpie", test("@{zero==1?'foobie':'blimpie'}"));
    }

    public void testTernary3() {
        assertEquals("foobiebarbie", test("@{zero==1?'foobie':'foobie'+'barbie'}"));
    }

    public void testTernary4() {
        assertEquals("no", test("@{ackbar ? 'yes' : 'no'}"));
    }

    public void testStrAppend() {
        assertEquals("foobarcar", test("@{'foo' + 'bar' + 'car'}"));
    }

    public void testStrAppend2() {
        assertEquals("foobarcar1", test("@{'foobar' + 'car' + 1}"));
    }

    public void testInstanceCheck1() {
        assertEquals(true, test("@{c is 'java.lang.String'}"));
    }

    public void testInstanceCheck2() {
        assertEquals(false, test("@{pi is 'java.lang.Integer'}"));
    }

    public void testBitwiseOr1() {
        assertEquals(6, test("@{2 | 4}"));
    }

    public void testBitwiseOr2() {
        assertEquals(true, test("@{(2 | 1) > 0}"));
    }

    public void testBitwiseOr3() {
        assertEquals(true, test("@{(2 | 1) == 3}"));
    }

    public void testBitwiseAnd1() {
        assertEquals(2, test("@{2 & 3}"));
    }

    public void testShiftLeft() {
        assertEquals(4, test("@{2 << 1}"));
    }

    public void testUnsignedShiftLeft() {
        assertEquals(2, test("@{-2 <<< 0}"));
    }

    public void testShiftRight() {
        assertEquals(128, test("@{256 >> 1}"));
    }

    public void testXOR() {
        assertEquals(3, test("@{1 ^ 2}"));
    }

    public void testContains1() {
        assertEquals(true, test("@{list contains 'Happy!'}"));
    }

    public void testContains2() {
        assertEquals(false, test("@{list contains 'Foobie'}"));
    }

    public void testContains3() {
        assertEquals(true, test("@{sentence contains 'fox'}"));
    }

    public void testContains4() {
        assertEquals(false, test("@{sentence contains 'mike'}"));
    }

    public void testContains5() {
        assertEquals(true, test("@{!(sentence contains 'mike')}"));
    }

    public void testTokenMethodAccess() {
        assertEquals(String.class, test("@{a = 'foo'; a.getClass()}"));
    }

    public void testArrayCreationWithLength() {
        assertEquals(2, test("@{Array.getLength({'foo', 'bar'})}"));
    }

    public void testMapCreation() {
        assertEquals("sarah", test("@{map = ['mike':'sarah','tom':'jacquelin']; map['mike']}"));
    }

    public void testProjectionSupport() {
        assertEquals(true, test("@{(name in things) contains 'Bob'}"));
    }

    public void testProjectionSupport2() {
        assertEquals(3, test("@{(name in things).size()}"));
    }

    public void testObjectInstantiation() {
        assertEquals("foobie", test("@{new java.lang.String('foobie')}"));
    }

    public void testObjectInstantiationWithMethodCall() {
        assertEquals("foobie", test("@{new String('foobie').toString()}"));
    }

    public void testObjectInstantiation2() {
        test("@{new String() is String}");
    }

    public void testArrayCoercion() {
        assertEquals("gonk", test("@{funMethod( {'gonk', 'foo'} )}"));
    }

    public void testMapAccess() {
        assertEquals("dog", test("@{funMap['foo'].bar.name}"));
    }

    public void testMapAccess2() {
        assertEquals("dog", test("@{funMap.foo.bar.name}"));
    }

    public void testSoundex() {
        assertTrue((Boolean) test("@{'foobar' soundslike 'fubar'}"));
    }

    public void testSoundex2() {
        assertFalse((Boolean) test("@{'flexbar' soundslike 'fubar'}"));
    }

    public void testThisReference() {
        assertEquals(true, test("@{this}") instanceof Base);
    }


//
//    public void testRegisterTemplateGroup() {
//        StringReader reader = new StringReader("myTemplate1() ::=<<@{var1}>>=::  myTemplate2() ::=<<@{var2}>>=::");
//        org.mvel.TemplateRegistry registry = new MVELTemplateRegistry();
//        registry.registerTemplate(reader);
//
//        assertEquals("xvalue1catx", test("x@includeByRef{myTemplate1(var1 = \"value1\")}@includeByRef{myTemplate2(var2 = c)}x", registry));
//    }
//
//    public void testRecursiveRegisterTemplateGroup() {
//        StringReader reader = new StringReader("myTemplate1() ::=<<@{var1}@includeByRef{myTemplate2(var2 = var2)}>>=::  myTemplate2() ::=<<@{var2}>>=::");
//        org.mvel.TemplateRegistry registry = new MVELTemplateRegistry();
//        registry.registerTemplate(reader);
//
//        assertEquals("xvalue1catx", test("x@includeByRef{myTemplate1(var1 = \"value1\", var2 = c)}x", registry));
//    }

    public void testIfLoopInTemplate() {
        assertEquals("ONETWOTHREE", test("@foreach{item :things}@if{item.name=='Bob'}ONE@elseif{item.name=='Smith'}TWO@elseif{item.name=='Cow'}THREE@end{}@end{}"));
    }

    public void testStringEscaping() {
        assertEquals("\"Mike Brock\"", test("@{\"\\\"Mike Brock\\\"\"}"));
    }

    public void testStringEscaping2() {
        assertEquals("MVEL's Parser is Fast", test("@{'MVEL\\'s Parser is Fast'}"));
    }

    public void testNestedAtSymbol() {
        assertEquals("email:foo@foo.com", test("email:@{'foo@foo.com'}"));
    }

    public void testEscape() {
        assertEquals("foo@foo.com", test("foo@@@{'foo.com'}"));
    }


    public void testIteration1() {
        List<String> list = new ArrayList<String>();
        list.add("a1");
        list.add("a2");
        list.add("a3");

        String template = "@foreach{list}a@end{}";
        Map map = new HashMap();
        map.put("list", list);
        String r = TemplateInterpreter.evalToString(template, map);
        System.out.println("r: " + r);
        assertEquals("aaa", r);
    }


    public void testIteration2() {
        Folder f1 = new Folder("f1", null);

        String template = "@{name} @foreach{children}a@end{}";
        String r = TemplateInterpreter.evalToString(template, f1);
        System.out.println("r: " + r);
    }

    public void testIteration3() {
        Folder f = new Folder("a1", null);
        List<Page> list = f.getChildren();

        String template = "@foreach{list}a@end{}";
        Map map = new HashMap();
        map.put("list", list);
        String r = TemplateInterpreter.evalToString(template, map);
        System.out.println("r: " + r);
        assertEquals("aaa", r);
    }

    public void testIteration4() {
        Folder f = new Folder("a1", null);

        String template = "@foreach{f.children}a@end{}";
        Map map = new HashMap();
        map.put("f", f);
        String r = TemplateInterpreter.evalToString(template, map);
        System.out.println("r: " + r);
        assertEquals("aaa", r);
    }

    class Page {
        String name;
        Folder parent;

        Page(String name, Folder parent) {
            this.name = name;
            this.parent = parent;
        }

        public String getName() {
            return name;
        }

        public Folder getParent() {
            return parent;
        }
    }

    class Folder extends Page {
        Folder(String name, Folder parent) {
            super(name, parent);
        }

        public List<Page> getChildren() {
            List<Page> list = new ArrayList<Page>();
            list.add(new Page("a1", this));
            list.add(new Page("a2", this));
            list.add(new Page("a3", this));
            return list;
        }
    }


    public Object parse(String ex, org.mvel.TemplateRegistry registry) {
        return TemplateInterpreter.parse(ex, base, map, registry);
    }

    public Object parse(String ex) {
        return TemplateInterpreter.parse(ex, base, map);
    }
}
