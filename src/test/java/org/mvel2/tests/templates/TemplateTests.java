package org.mvel2.tests.templates;

import junit.framework.TestCase;
import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.SimpleTemplateRegistry;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRegistry;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.tests.core.CoreConfidenceTests;
import org.mvel2.tests.core.res.Bar;
import org.mvel2.tests.core.res.Base;
import org.mvel2.tests.core.res.Foo;
import org.mvel2.tests.core.res.TestMVEL197;
import org.mvel2.tests.templates.tests.res.TestPluginNode;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static org.mvel2.templates.TemplateCompiler.compileTemplate;


@SuppressWarnings({"AssertEqualsBetweenInconvertibleTypes"})
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
        return TemplateRuntime.execute(compiled, base, vrf);
    }

    public void testPassThru() {
        String s = "foobar!";
        assertEquals("foobar!", test(s));
    }

    public void testBasicParsing() {
        String s = "foo: @{_foo_}--@{_bar_}!";
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

    public void testFileBasedEval() {
        assertEquals("Foo::Bar", TemplateRuntime.eval(new File("src/test/java/org/mvel2/tests/templates/templateTest.mv"),
                base, new MapVariableResolverFactory(map), null));
    }

    public void testInclusionOfTemplateFile() {
        String s = "<<@include{'src/test/java/org/mvel2/tests/templates/templateTest.mv'}>>";
        assertEquals("<<Foo::Bar>>", test(s));
    }

    public void testInclusionOfTemplateFile2() {
        String s = "<<@include{'src/test/java/org/mvel2/tests/templates/templateError.mv'}>>";
        try {
            test(s);
        }
        catch (CompileException e) {
            System.out.println(e.toString()
            );
            return;
        }
        assertTrue(false);
    }

    public void testForEachException1() {
        String s = "<<@foreach{arrayList}@{item}@end{}>>";
        try {
            test(s);
        }
        catch (Exception e) {
            System.out.println(e.toString());
            return;
        }
        assertTrue(false);
    }

    public void testForEachException2() {
        String s = "<<@foreach{item:arrayList}@{item}>>";
        try {
            test(s);
        }
        catch (Exception e) {
            System.out.println(e.toString());
            return;
        }
        assertTrue(false);
    }

    public void testTemplateFile() {
        String s = (String) TemplateRuntime.eval(new File("src/test/java/org/mvel2/tests/templates/templateIfTest.mv"),
                base, new MapVariableResolverFactory(map), null);

        System.out.println(s);

    }

    public void testInclusionOfNamedTemplate() {
        SimpleTemplateRegistry registry = new SimpleTemplateRegistry();
        registry.addNamedTemplate("footemplate", compileTemplate("@{_foo_}@{_bar_}"));
        registry.addNamedTemplate("bartemplate", compileTemplate("@{_bar_}@{_foo_}"));

        String s = "@includeNamed{'footemplate'}  ::  @includeNamed{'bartemplate'}";
        assertEquals("FooBar  ::  BarFoo", TemplateRuntime.eval(s, map, registry));
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

      public void testInlineDeclarations2() {
        String s = "@declare{'fudge'}Hello @{name}!@end{}@code{toInclude='fudge'}@includeNamed{toInclude; name='John'} -- @includeNamed{toInclude; name='Mary'}";
        assertEquals("Hello John! -- Hello Mary!", test(s));
    }

    public void testPluginNode() {
        Map<String, Class<? extends org.mvel2.templates.res.Node>> plugins = new HashMap<String, Class<? extends org.mvel2.templates.res.Node>>();
        plugins.put("testNode", TestPluginNode.class);

        TemplateCompiler compiler = new TemplateCompiler("Foo:@testNode{}!!", plugins);
        CompiledTemplate compiled = compiler.compile();

        assertEquals("Foo:THIS_IS_A_TEST!!", TemplateRuntime.execute(compiled));
    }


    public void testComments() {
        assertEquals("Foo", test("@comment{ This section is commented }@{_foo_}"));
    }

    /**
     * Integration of old tests
     */

    public void testPassThru2() {
        assertEquals("foo@bar.com", TemplateRuntime.eval("foo@bar.com", map));
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
        assertEquals(188.4, test("@{pi * hour}"));
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
                test(
                        "@foreach{item : list}" +
                                "@{item}" +
                                "@end{}"
                ));
    }

    public void testControlLoopArray() {
        assertEquals("Happy0Happy!1Joy2Joy!3",
                test(
                        "@code{i=0}@foreach{item : array}" +
                                "@{item}@{i++}" +
                                "@end{}"
                ));
    }

    public void testMultiCollectionControlLoop() {
        assertEquals("0=Happy:Happy,1=Happy!:Happy!,2=Joy:Joy,3=Joy!:Joy!",
                test(
                        "@code{i=0}@foreach{item : list, listItem : array}" +
                                "@{i++}=@{item}:@{listItem}" +
                                "@end{','}"
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

    public static interface TestInterface {
        public String getName();

        public boolean isFoo();
    }

    public void testControlLoop2() {
        assertEquals("HappyHappy!JoyJoy!",
                test(
                        "@foreach{item : list}" +
                                "@{item}" +
                                "@end{}"
                ));
    }

    public void testControlLoop3() {
        assertEquals("HappyHappy!JoyJoy!",
                test(
                        "@foreach{item : list }" +
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
        assertEquals(true, test("@{c is java.lang.String}"));
    }

    public void testInstanceCheck2() {
        assertEquals(false, test("@{pi is java.lang.Integer}"));
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

    public void testEvalNodes() {
        assertEquals("foo", test("@eval{\"@{'foo'}\"}"));
    }

    public void testIteration1() {
        List<String> list = new ArrayList<String>();
        list.add("a1");
        list.add("a2");
        list.add("a3");

        String template = "@foreach{item : list}a@end{}";
        Map map = new HashMap();
        map.put("list", list);
        String r = (String) TemplateRuntime.eval(template, map);
        System.out.println("r: " + r);
        assertEquals("aaa", r);
    }

    public void testIteration2() {
        Folder f1 = new Folder("f1", null);

        String template = "@{name} @foreach{item : children}a@end{}";
        String r = (String) TemplateRuntime.eval(template, f1);
        System.out.println("r: " + r);
    }

    public void testIteration3() {
        Folder f = new Folder("a1", null);
        List<Page> list = f.getChildren();

        String template = "@foreach{item : list}a@end{}";
        Map map = new HashMap();
        map.put("list", list);
        String r = (String) TemplateRuntime.eval(template, map);
        System.out.println("r: " + r);
        assertEquals("aaa", r);
    }

    public void testIteration4() {
        Folder f = new Folder("a1", null);

        String template = "@foreach{item : f.children}a@end{}";
        Map map = new HashMap();
        map.put("f", f);
        String r = (String) TemplateRuntime.eval(template, map);
        System.out.println("r: " + r);
        assertEquals("aaa", r);
    }

    public void testMVEL197() {
        Map<String, Object> context = new HashMap<String, Object>();
        Object[] args = new Object[1];
        TestMVEL197 test = new TestMVEL197();
        test.setName1("name1");
        test.setName2("name2");
        args[0] = test;
        context.put("args", args);
        String template = "${(args[0].name1=='name1'&&args[0].name2=='name2')?'a':'b'}";
        Object value = TemplateRuntime.eval(template, context);

        assertEquals("a", value);
    }

    public void testEscaping() {
        String template = "@@{'foo'}ABC";
        assertEquals("@{'foo'}ABC", TemplateRuntime.eval(template, new Object()));
    }

    public class Page {
        String name;
        Folder parent;

        public Page(String name, Folder parent) {
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

    public class Folder extends Page {
        public Folder(String name, Folder parent) {
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

    public void testMVEL229() {
        final Object context = new Object();
        final String template = "@code{sumText = 0}@{sumText}";
        System.out.println(TemplateRuntime.eval(template, new HashMap()));
    }

    public void testOutputStream1() {
        final StringBuilder sb = new StringBuilder();
        OutputStream outstream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                sb.append((char) b);
            }
        };

        String template = "@foreach{item:['foo','far']}@{item}@end{}";

        CompiledTemplate compiled = TemplateCompiler.compileTemplate(template);

        TemplateRuntime.execute(compiled, new HashMap(), outstream);

        assertEquals("foofar", sb.toString());
    }

    private Map<String, Object> setupVarsMVEL219() {
        Map<String, Object> vars = new LinkedHashMap<String, Object>();
        vars.put("bal", new BigDecimal("999.99"));
        vars.put("word", "ball");
        vars.put("object", new CoreConfidenceTests.Dog());
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("foo", "bar");
        map.put("fu", new CoreConfidenceTests.Dog());
        map.put("trueValue", true);
        map.put("falseValue", false);
        map.put("one", 1);
        map.put("zero", 0);
        vars.put("map", map);

        return vars;
    }

    private Map<String, Object> setupVarsMVEL220() {
        Map<String, Object> vars = new LinkedHashMap<String, Object>();
        vars.put("word", "ball");
        vars.put("object", new CoreConfidenceTests.Dog());
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("foo", "bar");
        map.put("fu", new CoreConfidenceTests.Dog());
        map.put("trueValue", true);
        map.put("falseValue", false);
        map.put("one", 1);
        map.put("zero", 0);
        map.put("list", "john,paul,ringo,george");
        vars.put("map", map);

        return vars;
    }

    String[] testCasesMVEL220 = {
            //        "map[\"foundIt\"] = !(map['list']).contains(\"john\")",
            "map[\"foundIt\"] = !(map['list'].contains(\"john\"))",
    };
    String[] templateTestCasesMVEL220 = {
            "@{map[\"foundIt\"] = !(map['list']).contains(\"john\")}",
            "@{map[\"foundIt\"] = !(map['list'].contains(\"john\"))}"
    };

    public void testEvalMVEL220() {
        Map<String, Object> vars = setupVarsMVEL220();

        System.out.println("Evaluation=====================");

        for (String expr : testCasesMVEL220) {
            System.out.println("Evaluating '" + expr + "': ......");
            Object ret = MVEL.eval(expr, vars);
            System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }

        System.out.println("Evaluation=====================");
    }

    public void testCompiledMVEL220() {
        Map<String, Object> vars = setupVarsMVEL220();

        System.out.println("Compilation=====================");

        for (String expr : testCasesMVEL220) {
            System.out.println("Compiling '" + expr + "': ......");
            Serializable compiled = MVEL.compileExpression(expr);
            Boolean ret = (Boolean) MVEL.executeExpression(compiled, vars);
            System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }
        System.out.println("Compilation=====================");
    }

    public void testTemplateMVEL220() {
        Map<String, Object> vars = setupVarsMVEL220();

        System.out.println("Templates=====================");

        for (String expr : templateTestCasesMVEL220) {
            System.out.println("Templating '" + expr + "': ......");
            Object ret = TemplateRuntime.eval(expr, vars);
            System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }

        System.out.println("Templates=====================");
    }

    String[] testCasesMVEL219 = {
            "map['foo']==map['foo']", // ok
            "(map['one'] > 0)", // ok
            "(map['one'] > 0) && (map['foo'] == map['foo'])", // ok
            "(map['one'] > 0) && (map['foo']==map['foo'])", // broken
    };
    String[] templateTestCasesMVEL219 = {
            "@{map['foo']==map['foo']}", // ok
            "@(map['one'] > 0)}", // ok
            "@{(map['one'] > 0) && (map['foo'] == map['foo'])}", // ok
            "@{(map['one'] > 0) && (map['foo']==map['foo'])}" // broken
    };

    public void testEvalMVEL219() {
        Map<String, Object> vars = setupVarsMVEL219();

        for (String expr : testCasesMVEL219) {
            System.out.println("Evaluating '" + expr + "': ......");
            Object ret = MVEL.eval(expr, vars);
            System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }
    }

    public void testCompiledMVEL219() {
        Map<String, Object> vars = setupVarsMVEL219();

        for (String expr : testCasesMVEL219) {
            System.out.println("Compiling '" + expr + "': ......");
            Serializable compiled = MVEL.compileExpression(expr);
            Boolean ret = (Boolean) MVEL.executeExpression(compiled, vars);
            System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }
    }

    public void testTemplateMVEL219() {
        Map<String, Object> vars = setupVarsMVEL219();

        for (String expr : templateTestCasesMVEL219) {
            System.out.println("Templating '" + expr + "': ......");
            Object ret = TemplateRuntime.eval(expr, vars);
            System.out.println("'" + expr + " ' = " + ret.toString());
            assertNotNull(ret);
        }
    }

    public void testStringCoercion() {
        String expr = " buffer = new StringBuilder(); i = 10; buffer.append( i + \"blah\" ); buffer.toString()";
        Serializable s = MVEL.compileExpression(expr);
        Object ret = MVEL.executeExpression(s, setupVarsMVEL219());
        System.out.println(":" + ret);
    }

    public void testTemplateStringCoercion() {
        String expr = "@code{ buffer = new StringBuilder(); i = 10; buffer.append( i + \"blah\" );}@{buffer.toString()}";
        Map<String, Object> vars = setupVarsMVEL219();
        System.out.println("Templating '" + expr + "': ......");
        Object ret = TemplateRuntime.eval(expr, vars);
        System.out.println("'" + expr + " ' = " + ret.toString());
        assertNotNull(ret);
    }

    public void testMVEL244() {
        Foo244 foo = new Foo244("plop");

        String template = "@foreach{val : foo.liste[0].liste} plop @end{}";

        CompiledTemplate compiledTemplate = TemplateCompiler.compileTemplate(template);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("foo", foo);

        System.out.println(TemplateRuntime.execute(compiledTemplate, new ParserContext(), new MapVariableResolverFactory(model)));
    }

    public void testImportsInTemplate() {
        String template = "@code{import java.util.HashMap; i = 10;}_____________@code{new HashMap().toString() + i}";

        Map map = new HashMap();
        Object result = TemplateRuntime.eval(template, map);
        assertNotNull("result cannot be null", result);
        assertEquals("result did not return string", String.class, result.getClass());
    }



    public static class Foo244 {
        private List<Foo244> liste = new ArrayList<Foo244>();

        private String val = "";

        public Foo244() {
        }

        public Foo244(String plop) {
            liste.add(new Foo244());
            liste.add(new Foo244());
            liste.add(new Foo244());
            liste.add(new Foo244());

            liste.get(0).getListe().add(new Foo244());
            liste.get(0).getListe().add(new Foo244());
            liste.get(0).getListe().add(new Foo244());
            liste.get(0).getListe().add(new Foo244());
        }

        public List<Foo244> getListe() {
            return liste;
        }

        public void setListe(List<Foo244> liste) {
            this.liste = liste;
        }

        public String getVal() {
            return val;
        }

        public void setVal(String val) {
            this.val = val;
        }
    }

    public static class Node {
        public Node(int base, List<Node> list) {
            this.base = base;
            this.list = list;
        }

        public int base;
        public List<Node> list;
    }

    public void testDRLTemplate() {

        String template = "@declare{\"drl\"}@includeNamed{\"ced\"; node=root }@end{}" +
                          "" +
                          "@declare{\"ced\"}" +
                          "@if{ node.base==1 } @includeNamed{ \"cedX\"; connect=\"AND\"; args=node.list }" +
                          "@elseif{ node.base ==2 }@includeNamed{ \"cedX\"; connect=\"OR\"; args=node.list }" +
                          "@end{}" +
                          "@end{}" +
                          "" +
                          "@declare{\"cedX\"}@{connect}@foreach{child : args}" +
                          "@includeNamed{\"ced\"; node=child; }@end{} @{connect}@end{}";

        TemplateRegistry reportRegistry = new SimpleTemplateRegistry();

        reportRegistry.addNamedTemplate("drl", TemplateCompiler.compileTemplate(template));
        TemplateRuntime.execute(reportRegistry.getNamedTemplate("drl"), null, reportRegistry);

        Map<String, Object> context = new HashMap<String, Object>();
        context.put( "root", new Node( 2,
                                       Arrays.asList( new Node( 1,
                                                                Collections.EMPTY_LIST ) ) ) );


        String result = (String) TemplateRuntime.execute( reportRegistry.getNamedTemplate( "drl" ),
                                                          null,
                                                          new MapVariableResolverFactory( context ),
                                                          reportRegistry );

        assertEquals("OR AND AND OR", result);
    }
}
