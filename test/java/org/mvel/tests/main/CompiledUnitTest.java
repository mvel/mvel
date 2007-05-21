package org.mvel.tests.main;

import junit.framework.TestCase;
import org.mvel.ExpressionCompiler;
import org.mvel.MVEL;
import org.mvel.debug.DebugTools;
import org.mvel.tests.main.res.Bar;
import org.mvel.tests.main.res.Base;
import org.mvel.tests.main.res.DerivedClass;
import org.mvel.tests.main.res.Foo;

import java.io.Serializable;
import java.util.*;

public class CompiledUnitTest extends TestCase {
    protected Foo foo = new Foo();
    protected Map<String, Object> map = new HashMap<String, Object>();
    protected Base base = new Base();
    protected DerivedClass derived = new DerivedClass();

    public CompiledUnitTest() {
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

        map.put("testImpl",
                new ParserUnitTest.TestInterface() {

                    public String getName() {
                        return "FOOBAR!";
                    }


                    public boolean isFoo() {
                        return true;
                    }
                });

        map.put("derived", derived);
    }

    public void testSingleProperty() {
        assertEquals(false, parseDirect("fun"));
    }


    public void testMethodOnValue() {
        assertEquals("DOG", parseDirect("foo.bar.name.toUpperCase()"));
    }

    public void testSimpleProperty() {
        assertEquals("dog", parseDirect("foo.bar.name"));
    }

    public void testSimpleProperty2() {
        assertEquals("cat", parseDirect("DATA"));
    }

    public void testPropertyViaDerivedClass() {
        assertEquals("cat", parseDirect("derived.data"));
    }

    public void testDeepAssignment() {
        assertEquals("crap", parseDirect("foo.bar.assignTest = 'crap'"));
        assertEquals("crap", parseDirect("foo.bar.assignTest"));
    }

    public void testThroughInterface() {
        assertEquals("FOOBAR!", parseDirect("testImpl.name"));
    }

    public void testThroughInterface2() {
        assertEquals(true, parseDirect("testImpl.foo"));
    }

    public void testMapAccessWithMethodCall() {
        assertEquals("happyBar", parseDirect("funMap['foo'].happy()"));
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

    public void testUninitializedInt() {
        assertEquals(0, parseDirect("sarahl"));
    }

    public void testAnd() {
        assertEquals(true, parseDirect("c != null && foo.bar.name == 'dog' && foo.bar.woof"));
    }

    public void testMath() {
        assertEquals(188.4f, parseDirect("pi * hour"));
    }

    public void testMath2() {
        assertEquals(3, parseDirect("foo.number-1"));
    }

    public void testComplexExpression() {
        assertEquals("bar", parseDirect("a = 'foo'; b = 'bar'; c = 'jim'; list = {a,b,c}; list[1]"));
    }

    public void testComplexAnd() {
        assertEquals(true, parseDirect("(pi * hour) > 0 && foo.happy() == 'happyBar'"));
    }

    public void testShortPathExpression() {
        assertEquals(null, parseDirect("3 > 4 && foo.toUC('test'); foo.register"));
    }

    public void testShortPathExpression2() {
        assertEquals(true, parseDirect("4 > 3 || foo.toUC('test')"));
    }

    public void testShortPathExpression3() {
        assertEquals(false, parseDirect("defnull != null  && defnull.length() > 0"));
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

    public void testAssignment3() {
        assertEquals(java.lang.Integer.class, parseDirect("blah = 5").getClass());
    }

    public void testAssignment4() {
        assertEquals(102, parseDirect("a = 100 + 1 + 1"));
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

    public void testBooleanModeOnly2() {
        assertEquals(false, (Object) MVEL.evalToBoolean("BWAH", base, map));
    }

    public void testBooleanModeOnly4() {
        assertEquals(true, (Object) MVEL.evalToBoolean("hour == (hour + 0)", base, map));
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
    }

    public void testInvert2() {
        assertEquals(~(10 + 1), parseDirect("~(10 + 1)"));
    }

    public void testInvert3() {
        assertEquals(~10 + (1 + ~50), parseDirect("~10 + (1 + ~50)"));
    }


    public void testListCreation2() {
        assertTrue(parseDirect("[\"test\"]") instanceof List);
    }

    public void testListCreation3() {
        assertTrue(parseDirect("[66]") instanceof List);
    }

    public void testListCreation4() {
        List ar = (List) parseDirect("[   66   , \"test\"   ]");
        assertEquals(2, ar.size());
        assertEquals(66, ar.get(0));
        assertEquals("test", ar.get(1));
    }


    public void testListCreationWithCall() {
        assertEquals(1, parseDirect("[\"apple\"].size()"));
    }

    public void testArrayCreationWithLength() {
        assertEquals(2, parseDirect("Array.getLength({'foo', 'bar'})"));
    }

    public void testEmptyList() {
        assertTrue(parseDirect("[]") instanceof List);
    }

    public void testEmptyArray() {
        assertTrue(((Object[]) parseDirect("{}")).length == 0);
    }

    public void testArrayCreation() {
        assertEquals(0, parseDirect("arrayTest = {{1, 2, 3}, {2, 1, 0}}; arrayTest[1][2]"));
    }

    public void testMapCreation() {
        assertEquals("sarah", parseDirect("map = ['mike':'sarah','tom':'jacquelin']; map['mike']"));
    }

    public void testMapCreation2() {
        assertEquals("sarah", parseDirect("map = ['mike' :'sarah'  ,'tom'  :'jacquelin'  ]; map['mike']"));
    }

    public void testProjectionSupport() {
        assertEquals(true, parseDirect("(name in things) contains 'Bob'"));
    }

    public void testProjectionSupport2() {
        assertEquals(3, parseDirect("(name in things).size()"));
    }


    public void testStaticMethodFromLiteral() {
        assertEquals(String.class.getName(), parseDirect("String.valueOf(Class.forName('java.lang.String').getName())"));
    }

    public void testMethodCallsEtc() {
        parseDirect("title = 1; " +
                "frame = new javax.swing.JFrame; " +
                "label = new javax.swing.JLabel; " +
                "title = title + 1;" +
                "frame.setTitle(title);" +
                "label.setText('MVEL UNIT TEST PACKAGE -- IF YOU SEE THIS, THAT IS GOOD');" +
                "frame.getContentPane().add(label);" +
                "frame.pack();" +
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
        assertEquals(true, parseDirect("this is 'org.mvel.tests.main.res.Base'"));
    }


    public void testStringEscaping() {
        assertEquals("\"Mike Brock\"", parseDirect("\"\\\"Mike Brock\\\"\""));
    }

    public void testStringEscaping2() {
        assertEquals("MVEL's Parser is Fast", parseDirect("'MVEL\\'s Parser is Fast'"));
    }

    public void testEvalToBoolean() {
        assertEquals(true, (boolean) MVEL.evalToBoolean("true ", "true"));
        assertEquals(true, (boolean) MVEL.evalToBoolean("true ", "true"));
    }

    public void testCompiledMapStructures() {
        Serializable compiled = MVEL.compileExpression("['foo':'bar'] contains 'foo'");
        MVEL.executeExpression(compiled, null, null, Boolean.class);
    }

    public void testSubListInMap() {
        assertEquals("pear", parseDirect("map = ['test' : 'poo', 'foo' : [c, 'pear']]; map['foo'][1]"));
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

    public void testThisReferenceInMethodCall() {
        assertEquals(101, parseDirect("Integer.parseInt(this.number)"));
    }

    public void testThisReferenceInConstructor() {
        assertEquals("101", parseDirect("new String(this.number)"));
    }

    public void testStaticNamespaceClassWithField() {
        assertEquals(Integer.MAX_VALUE, parseDirect("java.lang.Integer.MAX_VALUE"));
    }

    public void testStaticNamespaceClassWithField2() {
        assertEquals(Integer.MAX_VALUE, parseDirect("Integer.MAX_VALUE"));
    }

    public void testStaticFieldAsMethodParm() {
        assertEquals(String.valueOf(Integer.MAX_VALUE), parseDirect("String.valueOf(Integer.MAX_VALUE)"));
    }

    public void testIf() {
        assertEquals(10, parseDirect("if (5 > 4) { return 10; } else { return 5; }"));
    }

    public void testIf2() {
        assertEquals(10, parseDirect("if (5 < 4) { return 5; } else { return 10; }"));
    }

    public void testIfAndElse() {
        assertEquals(true, parseDirect("if (false) { return false; } else { return true; }"));
    }

    public void testIfAndElseif() {
        assertEquals(true, parseDirect("if (false) { return false; } else if(100 < 50) { return false; } else if (10 > 5) return true;"));
    }

    public void testIfAndElseIfCondensedGrammar() {
        assertEquals("Foo", parseDirect("if (false) return 'Bar'; else return 'Foo';"));
    }

    public void testForeEach2() {
        assertEquals(6, parseDirect("total = 0; a = {1,2,3}; foreach(item : a) { total = total + item }; total"));
    }

    public void testForEach3() {
        assertEquals(true, parseDirect("a = {1,2,3}; foreach (i : a) { if (i == 1) { return true; } }"));
    }

    public void testForEach4() {
        assertEquals("OneTwoThreeFour", parseDirect("a = {1,2,3,4}; builder = ''; foreach (i : a) {" +
                " if (i == 1) { builder = builder + 'One' } else if (i == 2) { builder = builder + 'Two' } " +
                "else if (i == 3) { builder = builder + 'Three' } else { builder = builder + 'Four' }" +
                "}; builder;"));
    }

    public void testWith() {
        assertEquals("OneTwo", parseDirect("with (foo) {aValue = 'One',bValue='Two'}; foo.aValue + foo.bValue;"));
    }

    public void testWith2() {
        assertEquals("OneTwo", parseDirect(
                "with (foo) { \n" +
                        "aValue = 'One', \n" +
                        "bValue='Two' \n" +
                        "}; \n" +
                        "foo.aValue + foo.bValue;"));
    }

    public void testAssertion() {
        try {
            parseDirect("assert false");
            assertTrue(false);
        }
        catch (AssertionError error) {
        }
    }

    public void testAssertion2() {
        try {
            parseDirect("assert true;");
        }
        catch (AssertionError error) {
            assertTrue(false);
        }
    }

    public void testMagicArraySize() {
        assertEquals(5, parseDirect("stringArray.size()"));
    }

    public void testMagicArraySize2() {
        assertEquals(5, parseDirect("intArray.size()"));
    }

    public void testStaticVarAssignment() {
        assertEquals("1", parseDirect("String mikeBrock = 1; mikeBrock"));
    }

    public void testIntentionalFailure() {
        try {
            parseDirect("int = 0"); // should fail because int is a reserved word.
            assertTrue(false);
        }
        catch (Exception e) {
        }
    }

    public void testImport() {
        assertEquals(HashMap.class, parseDirect("import java.util.HashMap; HashMap;"));
    }

    public void testStaticImport() {
        assertEquals(2.0, parseDirect("import_static java.lang.Math.sqrt; sqrt(4)"));
    }

    public void testFunctionPointer() {
        assertEquals(2.0, parseDirect("squareRoot = java.lang.Math.sqrt; squareRoot(4)"));
    }

    public void testFunctionPointerAsParam() {
        assertEquals("2.0", parseDirect("squareRoot = Math.sqrt; new String(String.valueOf(squareRoot(4)));"));
    }

    public void testVarInputs() {
        ExpressionCompiler compiler = new ExpressionCompiler("test != foo && bo.addSomething(trouble); bleh = foo; twa = bleh");
        compiler.compile();

        assertTrue(compiler.getInputs().contains("test"));
        assertTrue(compiler.getInputs().contains("foo"));
        assertTrue(compiler.getInputs().contains("bo"));
        assertTrue(compiler.getInputs().contains("trouble"));

        assertTrue(compiler.getLocals().contains("bleh"));
        assertTrue(compiler.getLocals().contains("twa"));
    }

    public Object parseDirect(String ex) {
        return compiledExecute(ex);
    }

    public Object compiledExecute(String ex) {
        Serializable compiled = MVEL.compileExpression(ex);

        System.out.println(DebugTools.decompile(compiled));

        Object first = MVEL.executeExpression(compiled, base, map);
        Object second = MVEL.executeExpression(compiled, base, map);


        if (first != null && !first.getClass().isArray())
            assertEquals(first, second);

        return second;
    }

    public Object compiledExecute(String ex, Object base, Map map) {
        Serializable compiled = MVEL.compileExpression(ex);
        Object first = MVEL.executeExpression(compiled, base, map);
        Object second = MVEL.executeExpression(compiled, base, map);

        if (first != null && !first.getClass().isArray())
            assertSame(first, second);
        return second;
    }


    public void testDifferentImplSameCompile() {
        Serializable compiled = MVEL.compileExpression("a.funMap.hello");

        Map testMap = new HashMap();

        for (int i = 0; i < 100; i++) {
            Base b = new Base();
            b.funMap.put("hello", "dog");
            testMap.put("a", b);


            assertEquals("dog", MVEL.executeExpression(compiled, testMap));

            b = new Base();
            b.funMap.put("hello", "cat");
            testMap.put("a", b);

            assertEquals("cat", MVEL.executeExpression(compiled, testMap));
        }
    }


    public void testToList() {
        String text = "misc.toList(foo.bar.name, 'hello', 42, ['key1' : 'value1', c : [ foo.bar.age, 'car', 42 ]], [42, [c : 'value1']] )";
        List list = (List) parseDirect(text);
        assertSame("dog", list.get(0));
        assertEquals("hello", list.get(1));
        assertEquals(new Integer(42), list.get(2));
        Map map = (Map) list.get(3);
        assertEquals("value1", map.get("key1"));

        List nestedList = (List) map.get("cat");
        assertEquals(14, nestedList.get(0));
        assertEquals("car", nestedList.get(1));
        assertEquals(42, nestedList.get(2));

        nestedList = (List) list.get(4);
        assertEquals(42, nestedList.get(0));
        map = (Map) nestedList.get(1);
        assertEquals("value1", map.get("cat"));
    }

    public void testToList2() {
        for (int i = 0; i < 10; i++) {
            testToList();
        }
    }

    public static class MiscTestClass {
        int exec = 0;

        public List toList(Object object1, String string, int integer, Map map, List list) {
            exec++;
            List l = new ArrayList();
            l.add(object1);
            l.add(string);
            l.add(new Integer(integer));
            l.add(map);
            l.add(list);
            return l;
        }


        public int getExec() {
            return exec;
        }
    }

    public void testCalculateAge() {
        //    System.out.println("Calculating the Age");
        Calendar c1 = Calendar.getInstance();
        c1.set(1999, 0, 10); // 1999 jan 20
        Map objectMap = new HashMap(1);
        Map propertyMap = new HashMap(1);
        propertyMap.put("GEBDAT", c1.getTime());
        objectMap.put("EV_VI_ANT1", propertyMap);
        assertEquals("N", compiledExecute("new org.mvel.tests.main.res.PDFFieldUtil().calculateAge(EV_VI_ANT1.GEBDAT) >= 25 ? 'Y' : 'N'"
                , null, objectMap));
    }

}
