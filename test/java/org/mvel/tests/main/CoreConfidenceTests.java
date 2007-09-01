package org.mvel.tests.main;

import org.mvel.*;
import static org.mvel.MVEL.*;
import org.mvel.ast.WithNode;
import org.mvel.debug.DebugTools;
import org.mvel.debug.Debugger;
import org.mvel.debug.Frame;
import org.mvel.integration.Interceptor;
import org.mvel.integration.ResolverTools;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.ClassImportResolverFactory;
import org.mvel.integration.impl.DefaultLocalVariableResolverFactory;
import org.mvel.integration.impl.MapVariableResolverFactory;
import org.mvel.integration.impl.StaticMethodImportResolverFactory;
import org.mvel.optimizers.OptimizerFactory;
import org.mvel.tests.main.res.*;
import org.mvel.util.MethodStub;

import java.awt.*;
import java.io.Serializable;
import static java.lang.System.currentTimeMillis;
import java.util.*;
import java.util.List;

public class CoreConfidenceTests extends AbstractTest {

    public void testSingleProperty() {
        assertEquals(false, test("fun"));
    }

    public void testMethodOnValue() {
        assertEquals("DOG", test("foo.bar.name.toUpperCase()"));
    }

    public void testSimpleProperty() {
        assertEquals("dog", test("foo.bar.name"));
    }

    public void testSimpleProperty2() {
        assertEquals("cat", test("DATA"));
    }

    public void testPropertyViaDerivedClass() {
        assertEquals("cat", test("derived.data"));
    }

    public void testDeepAssignment() {
        assertEquals("crap", test("foo.bar.assignTest = 'crap'"));
        assertEquals("crap", test("foo.bar.assignTest"));
    }

    public void testThroughInterface() {
        assertEquals("FOOBAR!", test("testImpl.name"));
    }

    public void testThroughInterface2() {
        assertEquals(true, test("testImpl.foo"));
    }

    public void testMapAccessWithMethodCall() {
        assertEquals("happyBar", test("funMap['foo'].happy()"));
    }

    public void testSimpleIfStatement() {
        test("if (true) { System.out.println(\"test!\") }  \n");
    }

    public void testBooleanOperator() {
        assertEquals(true, test("foo.bar.woof == true"));
    }

    public void testBooleanOperator2() {
        assertEquals(false, test("foo.bar.woof == false"));
    }

    public void testBooleanOperator3() {
        assertEquals(true, test("foo.bar.woof== true"));
    }

    public void testBooleanOperator4() {
        assertEquals(false, test("foo.bar.woof ==false"));
    }

    public void testBooleanOperator5() {
        assertEquals(true, test("foo.bar.woof == true"));
    }

    public void testBooleanOperator6() {
        assertEquals(false, test("foo.bar.woof==false"));
    }

    public void testTextComparison() {
        assertEquals(true, test("foo.bar.name == 'dog'"));
    }

    public void testNETextComparison() {
        assertEquals(true, test("foo.bar.name != 'foo'"));
    }

    public void testChor() {
        assertEquals("cat", test("a or b or c"));
    }

    public void testChorWithLiteral() {
        assertEquals("fubar", test("a or 'fubar'"));
    }

    public void testNullCompare() {
        assertEquals(true, test("c != null"));
    }

    public void testUninitializedInt() {
        assertEquals(0, test("sarahl"));
    }

    public void testAnd() {
        assertEquals(true, test("c != null && foo.bar.name == 'dog' && foo.bar.woof"));
    }

    public void testAnd2() {
        assertEquals(true, test("c!=null&&foo.bar.name=='dog'&&foo.bar.woof"));
    }

    public void testMath() {
        assertEquals(188.4f, test("pi * hour"));
    }

    public void testMath2() {
        assertEquals(3, test("foo.number-1"));
    }

    public void testPowerOf() {
        assertEquals(25, test("5 ** 2"));
    }

    public void testWhileUsingImports() {
        Map<String, Object> imports = new HashMap<String, Object>();
        imports.put("ArrayList", java.util.ArrayList.class);
        imports.put("List", java.util.List.class);

        ParserContext context = new ParserContext(imports, null, "testfile");
        ExpressionCompiler compiler = new ExpressionCompiler("List list = new ArrayList(); return (list == empty)");
        assertTrue((Boolean) MVEL.executeExpression(compiler.compile(context), new DefaultLocalVariableResolverFactory()));
    }


    public void testComplexExpression() {
        assertEquals("bar", test("a = 'foo'; b = 'bar'; c = 'jim'; list = {a,b,c}; list[1]"));
    }

    public void testComplexAnd() {
        assertEquals(true, test("(pi * hour) > 0 && foo.happy() == 'happyBar'"));
    }

    public void testShortPathExpression() {
        assertEquals(null, test("3 > 4 && foo.toUC('test'); foo.register"));
    }

    public void testShortPathExpression2() {
        assertEquals(true, test("4 > 3 || foo.toUC('test')"));
    }

    public void testShortPathExpression4() {
        assertEquals(true, test("4>3||foo.toUC('test')"));
    }

    public void testOrOperator() {
        assertEquals(true, test("true||true"));
    }

    public void testOrOperator2() {
        assertEquals(true, test("2 > 3 || 3 > 2"));
    }

    public void testOrOperator3() {
        assertEquals(true, test("pi > 5 || pi > 6 || pi > 3"));
    }


    public void testShortPathExpression3() {
        assertEquals(false, test("defnull != null  && defnull.length() > 0"));
    }

    public void testModulus() {
        assertEquals(38392 % 2,
                test("38392 % 2"));
    }


    public void testLessThan() {
        assertEquals(true, test("pi < 3.15"));
        assertEquals(true, test("pi <= 3.14"));
        assertEquals(false, test("pi > 3.14"));
        assertEquals(true, test("pi >= 3.14"));
    }

    public void testMethodAccess() {
        assertEquals("happyBar", test("foo.happy()"));
    }

    public void testMethodAccess2() {
        assertEquals("FUBAR", test("foo.toUC( 'fubar' )"));
    }

    public void testMethodAccess3() {
        assertEquals(true, test("equalityCheck(c, 'cat')"));
    }

    public void testMethodAccess4() {
        assertEquals(null, test("readBack(null)"));
    }

    public void testMethodAccess5() {
        assertEquals("nulltest", test("appendTwoStrings(null, 'test')"));
    }

    public void testMethodAccess6() {
        assertEquals(true, test("   equalityCheck(   c  \n  ,   \n   'cat'      )   "));
    }

    public void testNegation() {
        assertEquals(true, test("!fun && !fun"));
    }

    public void testNegation2() {
        assertEquals(false, test("fun && !fun"));
    }

    public void testNegation3() {
        assertEquals(true, test("!(fun && fun)"));
    }

    public void testNegation4() {
        assertEquals(false, test("(fun && fun)"));
    }

    public void testMultiStatement() {
        assertEquals(true, test("populate(); barfoo == 'sarah'"));
    }

    public void testAssignment() {
        assertEquals(true, test("populate(); blahfoo = 'sarah'; blahfoo == 'sarah'"));
    }

    public void testAssignment2() {
        assertEquals("sarah", test("populate(); blahfoo = barfoo"));
    }

    public void testAssignment3() {
        assertEquals(java.lang.Integer.class, test("blah = 5").getClass());
    }

    public void testAssignment4() {
        assertEquals(102, test("a = 100 + 1 + 1"));
    }

    public void testOr() {
        assertEquals(true, test("fun || true"));
    }

    public void testLiteralPassThrough() {
        assertEquals(true, test("true"));
    }

    public void testLiteralPassThrough2() {
        assertEquals(false, test("false"));
    }

    public void testLiteralPassThrough3() {
        assertEquals(null, test("null"));
    }

    public void testRegEx() {
        assertEquals(true, test("foo.bar.name ~= '[a-z].+'"));
    }

    public void testRegExNegate() {
        assertEquals(false, test("!(foo.bar.name ~= '[a-z].+')"));
    }

    public void testRegEx2() {
        assertEquals(true, test("foo.bar.name ~= '[a-z].+' && foo.bar.name != null"));
    }

    public void testRegEx3() {
        assertEquals(true, test("foo.bar.name~='[a-z].+'&&foo.bar.name!=null"));
    }

    public void testBlank() {
        assertEquals(true, test("'' == empty"));
    }

    public void testBlank2() {
        assertEquals(true, test("BWAH == empty"));
    }

    public void testBooleanModeOnly2() {
        assertEquals(false, (Object) evalToBoolean("BWAH", base, map));
    }

    public void testBooleanModeOnly4() {
        assertEquals(true, (Object) evalToBoolean("hour == (hour + 0)", base, map));
    }

    public void testTernary() {
        assertEquals("foobie", test("zero==0?'foobie':zero"));
    }

    public void testTernary2() {
        assertEquals("blimpie", test("zero==1?'foobie':'blimpie'"));
    }

    public void testTernary3() {
        assertEquals("foobiebarbie", test("zero==1?'foobie':'foobie'+'barbie'"));
    }

    public void testStrAppend() {
        assertEquals("foobarcar", test("'foo' + 'bar' + 'car'"));
    }

    public void testStrAppend2() {
        assertEquals("foobarcar1", test("'foobar' + 'car' + 1"));
    }

    public void testInstanceCheck1() {
        assertEquals(true, test("c is java.lang.String"));
    }

    public void testInstanceCheck2() {
        assertEquals(false, test("pi is java.lang.Integer"));
    }

    public void testInstanceCheck3() {
        assertEquals(true, test("foo is org.mvel.tests.main.res.Foo"));
    }

    public void testBitwiseOr1() {
        assertEquals(6, test("2|4"));
    }

    public void testBitwiseOr2() {
        assertEquals(true, test("(2 | 1) > 0"));
    }

    public void testBitwiseOr3() {
        assertEquals(true, test("(2|1) == 3"));
    }

    public void testBitwiseAnd1() {
        assertEquals(2, test("2 & 3"));
    }

    public void testShiftLeft() {
        assertEquals(4, test("2 << 1"));
    }

    public void testUnsignedShiftLeft() {
        assertEquals(2, test("-2 <<< 0"));
    }

    public void testShiftRight() {
        assertEquals(128, test("256 >> 1"));
    }

    public void testXOR() {
        assertEquals(3, test("1 ^ 2"));
    }

    public void testContains1() {
        assertEquals(true, test("list contains 'Happy!'"));
    }

    public void testContains2() {
        assertEquals(false, test("list contains 'Foobie'"));
    }

    public void testContains3() {
        assertEquals(true, test("sentence contains 'fox'"));
    }

    public void testContains4() {
        assertEquals(false, test("sentence contains 'mike'"));
    }

    public void testContains5() {
        assertEquals(true, test("!(sentence contains 'mike')"));
    }

    public void testContains6() {
        assertEquals(true, test("bwahbwah = 'mikebrock'; testVar10 = 'mike'; bwahbwah contains testVar10"));
    }

    public void testInvert() {
        assertEquals(~10, test("~10"));
    }

    public void testInvert2() {
        assertEquals(~(10 + 1), test("~(10 + 1)"));
    }

    public void testInvert3() {
        assertEquals(~10 + (1 + ~50), test("~10 + (1 + ~50)"));
    }


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

    public void testProjectionSupport() {
        assertEquals(true, test("(name in things)contains'Bob'"));
    }

    public void testProjectionSupport1() {
        assertEquals(true, test("(name in things) contains 'Bob'"));
    }

    public void testProjectionSupport2() {
        assertEquals(3, test("(name in things).size()"));
    }

    public void testSizeOnInlineArray() {
        assertEquals(3, test("{1,2,3}.size()"));
    }


    public void testStaticMethodFromLiteral() {
        assertEquals(String.class.getName(), test("String.valueOf(Class.forName('java.lang.String').getName())"));
    }

//    public void testMethodCallsEtc() {
//        test("title = 1; " +
//                "frame = new javax.swing.JFrame; " +
//                "label = new javax.swing.JLabel; " +
//                "title = title + 1;" +
//                "frame.setTitle(title);" +
//                "label.setText('MVEL UNIT TEST PACKAGE -- IF YOU SEE THIS, THAT IS GOOD');" +
//                "frame.getContentPane().add(label);" +
//                "frame.pack();" +
//                "frame.setVisible(true);");
//    }

    public void testObjectInstantiation() {
        test("new java.lang.String('foobie')");
    }

    public void testObjectInstantiationWithMethodCall() {
        assertEquals("FOOBIE", test("new String('foobie')  . toUpperCase()"));
    }

    public void testObjectInstantiation2() {
        test("new String() is String");
    }

    public void testObjectInstantiation3() {
        test("new java.text.SimpleDateFormat('yyyy').format(new java.util.Date(System.currentTimeMillis()))");
    }

    public void testArrayCoercion() {
        assertEquals("gonk", test("funMethod( {'gonk', 'foo'} )"));
    }

    public void testArrayCoercion2() {
        assertEquals(10, test("sum({2,2,2,2,2})"));
    }

    public void testMapAccess() {
        assertEquals("dog", test("funMap['foo'].bar.name"));
    }

    public void testMapAccess2() {
        assertEquals("dog", test("funMap.foo.bar.name"));
    }

    public void testSoundex() {
        assertTrue((Boolean) test("'foobar' soundslike 'fubar'"));
    }

    public void testSoundex2() {
        assertFalse((Boolean) test("'flexbar' soundslike 'fubar'"));
    }

    public void testThisReference() {
        assertEquals(true, test("this") instanceof Base);
    }

    public void testThisReference2() {
        assertEquals(true, test("this.funMap") instanceof Map);
    }

    public void testThisReference3() {
        assertEquals(true, test("this is org.mvel.tests.main.res.Base"));
    }

    public void testThisReference4() {
        assertEquals(true, test("this.funMap instanceof java.util.Map"));
    }

    public void testThisReference5() {
        assertEquals(true, test("this.data == 'cat'"));
    }

    public void testThisReferenceInMethodCall() {
        assertEquals(101, test("Integer.parseInt(this.number)"));
    }

    public void testThisReferenceInConstructor() {
        assertEquals("101", test("new String(this.number)"));
    }


    // interpreted
    public void testThisReferenceMapVirtualObjects() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("foo", "bar");

        VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
        factory.createVariable("this", map);

        assertEquals(true, MVEL.eval("this.foo == 'bar'", map, factory));
    }

    // compiled - reflective
    public void testThisReferenceMapVirtualObjects1() {
        // Create our root Map object
        Map<String, String> map = new HashMap<String, String>();
        map.put("foo", "bar");

        VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
        factory.createVariable("this", map);

        Serializable compiled = MVEL.compileExpression("this.foo == 'bar'");

        OptimizerFactory.setDefaultOptimizer("reflective");

        // Run test
        assertEquals(true, MVEL.executeExpression(compiled, map, factory));
    }

    // compiled - asm
    public void testThisReferenceMapVirtualObjects2() {
        // Create our root Map object
        Map<String, String> map = new HashMap<String, String>();
        map.put("foo", "bar");

        VariableResolverFactory factory = new MapVariableResolverFactory(new HashMap<String, Object>());
        factory.createVariable("this", map);

        // I think we can all figure this one out.
        Serializable compiled = MVEL.compileExpression("this.foo == 'bar'");

        if (!Boolean.getBoolean("mvel.disable.jit")) OptimizerFactory.setDefaultOptimizer("ASM");

        // Run test
        assertEquals(true, MVEL.executeExpression(compiled, map, factory));
    }

    public void testStringEscaping() {
        assertEquals("\"Mike Brock\"", test("\"\\\"Mike Brock\\\"\""));
    }

    public void testStringEscaping2() {
        assertEquals("MVEL's Parser is Fast", test("'MVEL\\'s Parser is Fast'"));
    }

    public void testEvalToBoolean() {
        assertEquals(true, (boolean) evalToBoolean("true ", "true"));
        assertEquals(true, (boolean) evalToBoolean("true ", "true"));
    }

    public void testCompiledMapStructures() {
        Serializable compiled = compileExpression("['foo':'bar'] contains 'foo'");
        executeExpression(compiled, null, null, Boolean.class);
    }

    public void testSubListInMap() {
        assertEquals("pear", test("map = ['test' : 'poo', 'foo' : [c, 'pear']]; map['foo'][1]"));
    }

    public void testCompiledMethodCall() {
        Serializable compiled = compileExpression("c.getClass()");
        assertEquals(String.class, executeExpression(compiled, base, map));
    }

    public void testStaticNamespaceCall() {
        assertEquals(java.util.ArrayList.class, test("java.util.ArrayList"));
    }

    public void testStaticNamespaceClassWithMethod() {
        assertEquals("FooBar", test("java.lang.String.valueOf('FooBar')"));
    }

    public void testConstructor() {
        assertEquals("foo", test("a = 'foobar'; new String(a.toCharArray(), 0, 3)"));
    }

    public void testStaticNamespaceClassWithField() {
        assertEquals(Integer.MAX_VALUE, test("java.lang.Integer.MAX_VALUE"));
    }

    public void testStaticNamespaceClassWithField2() {
        assertEquals(Integer.MAX_VALUE, test("Integer.MAX_VALUE"));
    }

    public void testStaticFieldAsMethodParm() {
        assertEquals(String.valueOf(Integer.MAX_VALUE), test("String.valueOf(Integer.MAX_VALUE)"));
    }

    public void testEmptyIf() {
        assertEquals(5, test("a = 5; if (a == 5) { }; return a;"));
    }

    public void testEmptyIf2() {
        assertEquals(5, test("a=5;if(a==5){};return a;"));
    }

    public void testIf() {
        assertEquals(10, test("if (5 > 4) { return 10; } else { return 5; }"));
    }

    public void testIf2() {
        assertEquals(10, test("if (5 < 4) { return 5; } else { return 10; }"));
    }

    public void testIf3() {
        assertEquals(10, test("if(5<4){return 5;}else{return 10;}"));
    }

    public void testIfAndElse() {
        assertEquals(true, test("if (false) { return false; } else { return true; }"));
    }

    public void testIfAndElseif() {
        assertEquals(true, test("if (false) { return false; } else if(100 < 50) { return false; } else if (10 > 5) return true;"));
    }

    public void testIfAndElseIfCondensedGrammar() {
        assertEquals("Foo", test("if (false) return 'Bar'; else return 'Foo';"));
    }

    public void testForeEach2() {
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

    public void testWith() {
        assertEquals("OneTwo", test("with (foo) {aValue = 'One',bValue='Two'}; foo.aValue + foo.bValue;"));
    }

    public void testWith2() {
        assertEquals("OneTwo", test(
                "with (foo) { \n" +
                        "aValue = 'One', \n" +
                        "bValue='Two' \n" +
                        "}; \n" +
                        "foo.aValue + foo.bValue;"));
    }

    public void testAssertion() {
        try {
            test("assert false");
            assertTrue(false);
        }
        catch (AssertionError error) {
        }
    }

    public void testAssertion2() {
        try {
            test("assert true;");
        }
        catch (AssertionError error) {
            assertTrue(false);
        }
    }

    public void testMagicArraySize() {
        assertEquals(5, test("stringArray.size()"));
    }

    public void testMagicArraySize2() {
        assertEquals(5, test("intArray.size()"));
    }

    public void testStaticVarAssignment() {
        assertEquals("1", test("String mikeBrock = 1; mikeBrock"));
    }

    public void testIntentionalFailure() {
        try {
            test("int = 0"); // should fail because int is a reserved word.
            assertTrue(false);
        }
        catch (Exception e) {
        }
    }

    public void testImport() {
        assertEquals(HashMap.class, test("import java.util.HashMap; HashMap;"));
    }

    public void testStaticImport() {
        assertEquals(2.0, test("import_static java.lang.Math.sqrt; sqrt(4)"));
    }

    public void testFunctionPointer() {
        assertEquals(2.0, test("squareRoot = java.lang.Math.sqrt; squareRoot(4)"));
    }

    public void testFunctionPointerAsParam() {
        assertEquals("2.0", test("squareRoot = Math.sqrt; new String(String.valueOf(squareRoot(4)));"));
    }

    public void testFunctionPointerInAssignment() {
        assertEquals(5.0, test("squareRoot = Math.sqrt; i = squareRoot(25); return i;"));
    }

    public void testIncrementOperator() {
        assertEquals(2, test("x = 1; x++; x"));
    }

    public void testPreIncrementOperator() {
        assertEquals(2, test("x = 1; ++x"));
    }

    public void testDecrementOperator() {
        assertEquals(1, test("x = 2; x--; x"));
    }

    public void testPreDecrementOperator() {
        assertEquals(1, test("x = 2; --x"));
    }

    public void testQualifiedStaticTyping() {
        assertEquals(20, test("java.math.BigDecimal a = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal b = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal c = a + b; return c; "));
    }

    public void testUnQualifiedStaticTyping() {
        assertEquals(20, test("import java.math.BigDecimal; BigDecimal a = new BigDecimal( 10.0 ); BigDecimal b = new BigDecimal( 10.0 ); BigDecimal c = a + b; return c; "));
    }

    public void testObjectCreation() {
        assertEquals(6, test("new Integer( 6 )"));
    }

    public void testTernary4() {
        assertEquals("<test>", test("true ? '<test>' : '<poo>'"));
    }

    public void testStringAsCollection() {
        assertEquals('o', test("abc = 'foo'; abc[1]"));
    }

    public void testSubExpressionIndexer() {
        assertEquals("bar", test("xx = new java.util.HashMap(); xx.put('foo', 'bar'); prop = 'foo'; xx[prop];"));
    }

    public void testCompileTimeLiteralReduction() {
        assertEquals(1000, test("10 * 100"));
    }

    public void testInterfaceResolution() {
        Serializable ex = MVEL.compileExpression("foo.collectionTest.size()");

        foo.setCollectionTest(new HashSet());
        Object result1 = MVEL.executeExpression(ex, map);

        foo.setCollectionTest(new ArrayList());
        Object result2 = MVEL.executeExpression(ex, map);

        assertEquals(result1, result2);
    }


    /**
     * Start collections framework based compliance tests
     */
    public void testCreationOfSet() {
        assertEquals("foo bar foo bar",
                test("set = new java.util.HashSet(); " +
                        "set.add('foo');" +
                        "set.add('bar');" +
                        "output = '';" +
                        "foreach (item : set) {" +
                        "output = output + item + ' ';" +
                        "} " +
                        "foreach (item : set) {" +
                        "output = output + item + ' ';" +
                        "} " +
                        "output = output.trim();" +
                        "if (set.size() == 2) { return output; }"));

    }


    public void testCreationOfList() {
        assertEquals(5, test("l = new java.util.LinkedList();" +
                "l.add('fun');" +
                "l.add('happy');" +
                "l.add('fun');" +
                "l.add('slide');" +
                "l.add('crap');" +
                "poo = new java.util.ArrayList(l);" +
                "poo.size();"));
    }

    public void testMapOperations() {
        assertEquals("poo5", test(
                "l = new java.util.ArrayList();" +
                        "l.add('plop');" +
                        "l.add('poo');" +
                        "m = new java.util.HashMap();" +
                        "m.put('foo', l);" +
                        "m.put('cah', 'mah');" +
                        "m.put('bar', 'foo');" +
                        "m.put('sarah', 'mike');" +
                        "m.put('edgar', 'poe');" +
                        "" +
                        "if (m.edgar == 'poe') {" +
                        "return m.foo[1] + m.size();" +
                        "}"));
    }

    public void testStackOperations() {
        assertEquals(10, test(
                "stk = new java.util.Stack();" +
                        "stk.push(5);" +
                        "stk.push(5);" +
                        "stk.pop() + stk.pop();"
        ));
    }

    public void testSystemOutPrint() {
        test("a = 0;\r\nSystem.out.println('This is a test');");
    }

    public void testBreakpoints() {
        ExpressionCompiler compiler = new ExpressionCompiler("a = 5;\nb = 5;\n\nif (a == b) {\n\nSystem.out.println('Good');\nreturn a + b;\n}\n");
        System.out.println("-------\n" + compiler.getExpression() + "\n-------\n");

        compiler.setDebugSymbols(true);

        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("test.mv");

        CompiledExpression compiled = compiler.compile(ctx);

        System.out.println(DebugTools.decompile(compiled));

        MVELRuntime.registerBreakpoint("test.mv", 7);

        Debugger testDebugger = new Debugger() {

            public int onBreak(Frame frame) {
                System.out.println("Breakpoint [source:" + frame.getSourceName() + "; line:" + frame.getLineNumber() + "]");

                return 0;
            }

        };

        MVELRuntime.setThreadDebugger(testDebugger);

        assertEquals(10, MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(map)));
    }

    public void testBreakpoints2() {
        ExpressionCompiler compiler = new ExpressionCompiler("System.out.println('test the debugger');\n a = 0;");
        compiler.setDebugSymbols(true);

        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("test.mv");

        CompiledExpression compiled = compiler.compile(ctx);

        System.out.println(DebugTools.decompile(compiled));
    }

    public void testBreakpoints3() {
        String expr = "System.out.println( \"a1\" );\n" +
                "System.out.println( \"a2\" );\n" +
                "System.out.println( \"a3\" );\n" +
                "System.out.println( \"a4\" );\n";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);
        //      compiler.setDebugSymbols(true);

        ParserContext context = new ParserContext();
        context.addImport("System", System.class);
        context.setStrictTypeEnforcement(true);
        context.setDebugSymbols(true);
        context.setSourceFile("mysource");

        Serializable compiledExpression = compiler.compile(context);

        String s = org.mvel.debug.DebugTools.decompile(compiledExpression);

        System.out.println("output: " + s);

        int fromIndex = 0;
        int count = 0;
        while ((fromIndex = s.indexOf("DEBUG_SYMBOL", fromIndex + 1)) > -1) {
            count++;
        }
        assertEquals(4, count);

    }


    public void testBreakpointsAcrossComments() {
        String expression = "/** This is a comment\n" +  // 1
                " *  Second comment line\n" +        // 2
                " *  Third Comment Line\n" +         // 3
                " */\n" +                         // 4
                "System.out.println('4');\n" +   // 5
                "System.out.println('5');\n" +   // 6
                "a = 0;\n" +                     // 7
                "b = 1;\n" +                    // 8
                "a + b";                        // 9

        ExpressionCompiler compiler = new ExpressionCompiler(expression);
        compiler.setDebugSymbols(true);

        System.out.println("Expression:\n------------");
        System.out.println(expression);
        System.out.println("------------");


        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("test2.mv");

        CompiledExpression compiled = compiler.compile(ctx);

        System.out.println(DebugTools.decompile(compiled));

        MVELRuntime.registerBreakpoint("test2.mv", 9);

        Debugger testDebugger = new Debugger() {

            public int onBreak(Frame frame) {
                System.out.println("Breakpoint Encountered [source:" + frame.getSourceName() + "; line:" + frame.getLineNumber() + "]");
                System.out.println("vars:" + frame.getFactory().getKnownVariables());
                System.out.println("Resume Execution");
                return 0;
            }

        };

        MVELRuntime.setThreadDebugger(testDebugger);

        assertEquals(1, MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(map)));
    }


    public void testBreakpointsAcrossComments2() {
        ExpressionCompiler compiler = new ExpressionCompiler(
                "// This is a comment\n" +                  // 1
                        "//Second comment line\n" +         // 2
                        "//Third Comment Line\n" +          // 3
                        "\n" +                              // 4
                        "//Test\n" +                        // 5
                        "System.out.println('4');\n" +      // 6
                        "//System.out.println('5'); \n" +    // 7
                        "a = 0;\n" +                        // 8
                        "b = 1;\n" +                        // 9
                        " a + b");                          // 10

        compiler.setDebugSymbols(true);

        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("test2.mv");

        CompiledExpression compiled = compiler.compile(ctx);

        System.out.println(DebugTools.decompile(compiled));

        MVELRuntime.registerBreakpoint("test2.mv", 6);
        MVELRuntime.registerBreakpoint("test2.mv", 8);
        MVELRuntime.registerBreakpoint("test2.mv", 9);
        MVELRuntime.registerBreakpoint("test2.mv", 10);

        Debugger testDebugger = new Debugger() {
            public int onBreak(Frame frame) {
                System.out.println("Breakpoint [source:" + frame.getSourceName() + "; line:" + frame.getLineNumber() + "]");
                return 0;
            }
        };

        MVELRuntime.setThreadDebugger(testDebugger);

        assertEquals(1, MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(map)));
    }

    public void testBreakpoints4() {
        String expression = "System.out.println('foo');\n" +
                "a = new Foo();\n" +
                "update (a) { name = 'bar' };\n" +
                "System.out.println('name:' + a.name);\n" +
                "return a.name;";


        Map<String, Interceptor> interceptors = new HashMap<String, Interceptor>();
        Map<String, Macro> macros = new HashMap<String, Macro>();

        interceptors.put("Update", new Interceptor() {
            public int doBefore(ASTNode node, VariableResolverFactory factory) {
                ((WithNode) node).getNestedStatement().getValue(null,
                        factory);
                System.out.println("fired update interceptor -- before");
                return 0;
            }

            public int doAfter(Object val, ASTNode node, VariableResolverFactory factory) {
                System.out.println("fired update interceptor -- after");
                return 0;
            }
        });

        macros.put("update", new Macro() {
            public String doMacro() {
                return "@Update with";
            }
        });


        expression = parseMacros(expression, macros);


        ExpressionCompiler compiler = new ExpressionCompiler(expression);
        compiler.setDebugSymbols(true);


        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("test2.mv");
        ctx.addImport("Foo", Foo.class);
        ctx.setInterceptors(interceptors);


        CompiledExpression compiled = compiler.compile(ctx);

        System.out.println("\nExpression:------------");
        System.out.println(expression);
        System.out.println("------------");


        System.out.println(DebugTools.decompile(compiled));

        MVELRuntime.registerBreakpoint("test2.mv", 3);
        MVELRuntime.registerBreakpoint("test2.mv", 4);
        MVELRuntime.registerBreakpoint("test2.mv", 5);
//        MVELRuntime.registerBreakpoint("test2.mv", 10);

        Debugger testDebugger = new Debugger() {
            public int onBreak(Frame frame) {
                System.out.println("Breakpoint [source:" + frame.getSourceName() + "; line:" + frame.getLineNumber() + "]");
                return 0;
            }
        };

        MVELRuntime.setThreadDebugger(testDebugger);

        assertEquals("bar", MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(map)));
    }

    public void testBreakpoints5() {
        String expression = "System.out.println('foo');\r\n" +
                "a = new Foo();\r\n" +
                "a.name = 'bar'\r\n" +
                "foo.happy();\r\n" +
                "System.out.println( 'name:' + a.name );               \r\n" +
                "System.out.println( 'name:' + a.name );         \r\n" +
                "System.out.println( 'name:' + a.name );     \r\n" +
                "return a.name;";


        Map<String, Interceptor> interceptors = new HashMap<String, Interceptor>();
        Map<String, Macro> macros = new HashMap<String, Macro>();

        interceptors.put("Update", new Interceptor() {
            public int doBefore(ASTNode node, VariableResolverFactory factory) {
                ((WithNode) node).getNestedStatement().getValue(null,
                        factory);
                System.out.println("fired update interceptor -- before");
                return 0;
            }

            public int doAfter(Object val, ASTNode node, VariableResolverFactory factory) {
                System.out.println("fired update interceptor -- after");
                return 0;
            }
        });

        macros.put("update", new Macro() {
            public String doMacro() {
                return "@Update with";
            }
        });


        expression = parseMacros(expression, macros);


        ExpressionCompiler compiler = new ExpressionCompiler(expression);
        compiler.setDebugSymbols(true);


        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("test2.mv");
        ctx.addImport("Foo", Foo.class);
        ctx.setInterceptors(interceptors);


        CompiledExpression compiled = compiler.compile(ctx);

        System.out.println("\nExpression:------------");
        System.out.println(expression);
        System.out.println("------------");


        System.out.println(DebugTools.decompile(compiled));
        MVELRuntime.registerBreakpoint("test2.mv", 1);
//        MVELRuntime.registerBreakpoint("test2.mv", 10);

        Debugger testDebugger = new Debugger() {
            public int onBreak(Frame frame) {
                System.out.println("Breakpoint [source:" + frame.getSourceName() + "; line:" + frame.getLineNumber() + "]");
                //           System.out.println("Stepover");
                return Debugger.STEP_OVER;
            }
        };

        MVELRuntime.setThreadDebugger(testDebugger);

        System.out.println("\n==RUN==\n");

        assertEquals("bar", MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(map)));

        //       MVELRuntime.setThreadDebugger(null);
    }


    public void testDebugSymbolsWithWindowsLinedEndings() throws Exception {
        String expr = "   System.out.println( \"a1\" );\r\n" +
                "   System.out.println( \"a2\" );\r\n" +
                "   System.out.println( \"a3\" );\r\n" +
                "   System.out.println( \"a4\" );\r\n";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);
        compiler.setDebugSymbols(true);

        ParserContext ctx = new ParserContext();
        ctx.setStrictTypeEnforcement(true);
        ctx.setDebugSymbols(true);
        ctx.setSourceFile("mysource");

        Serializable compiledExpression = compiler.compile(ctx);

        String s = org.mvel.debug.DebugTools.decompile(compiledExpression);

        System.out.println(s);

        int fromIndex = 0;
        int count = 0;
        while ((fromIndex = s.indexOf("DEBUG_SYMBOL", fromIndex + 1)) > -1) {
            count++;
        }
        assertEquals(4, count);

    }


    public void testDebugSymbolsWithUnixLinedEndings() throws Exception {
        String expr = "   System.out.println( \"a1\" );\n" +
                "   System.out.println( \"a2\" );\n" +
                "   System.out.println( \"a3\" );\n" +
                "   System.out.println( \"a4\" );\n";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);
        compiler.setDebugSymbols(true);

        ParserContext ctx = new ParserContext();
        ctx.setStrictTypeEnforcement(true);
        ctx.setDebugSymbols(true);
        ctx.setSourceFile("mysource");

        Serializable compiledExpression = compiler.compile(ctx);

        String s = org.mvel.debug.DebugTools.decompile(compiledExpression);

        int fromIndex = 0;
        int count = 0;
        while ((fromIndex = s.indexOf("DEBUG_SYMBOL", fromIndex + 1)) > -1) {
            count++;
        }
        assertEquals(4, count);

    }

    public void testDebugSymbolsWithMixedLinedEndings() throws Exception {
        String expr = "   System.out.println( \"a1\" );\n" +
                "   System.out.println( \"a2\" );\r\n" +
                "   System.out.println( \"a3\" );\n" +
                "   System.out.println( \"a4\" );\r\n";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);
        compiler.setDebugSymbols(true);

        ParserContext ctx = new ParserContext();
        ctx.setStrictTypeEnforcement(true);
        ctx.setDebugSymbols(true);
        ctx.setSourceFile("mysource");

        Serializable compiledExpression = compiler.compile(ctx);

        String s = org.mvel.debug.DebugTools.decompile(compiledExpression);

        System.out.println(s);

        int fromIndex = 0;
        int count = 0;
        while ((fromIndex = s.indexOf("DEBUG_SYMBOL", fromIndex + 1)) > -1) {
            count++;
        }
        assertEquals(4, count);

    }


    public void testReflectionCache() {
        assertEquals("happyBar", test("foo.happy(); foo.bar.happy()"));
    }

    public void testVarInputs() {
        ExpressionCompiler compiler = new ExpressionCompiler("test != foo && bo.addSomething(trouble); String bleh = foo; twa = bleh;");

        compiler.compile();

        ParserContext pCtx = compiler.getParserContextState();

        assertEquals(4, pCtx.getInputs().size());

        assertTrue(pCtx.getInputs().containsKey("test"));
        assertTrue(pCtx.getInputs().containsKey("foo"));
        assertTrue(pCtx.getInputs().containsKey("bo"));
        assertTrue(pCtx.getInputs().containsKey("trouble"));

        assertEquals(2, pCtx.getVariables().size());

        assertTrue(pCtx.getVariables().containsKey("bleh"));
        assertTrue(pCtx.getVariables().containsKey("twa"));

        assertEquals(String.class, pCtx.getVarOrInputType("bleh"));
    }

    public void testVarInputs2() {
        ExpressionCompiler compiler = new ExpressionCompiler("test != foo && bo.addSomething(trouble); String bleh = foo; twa = bleh;");

        ParserContext ctx = new ParserContext();
        ctx.setRetainParserState(true);

        compiler.compile(ctx);

        System.out.println(ctx.getVarOrInputType("bleh"));
    }

    public void testVarInputs3() {
        ExpressionCompiler compiler = new ExpressionCompiler("addresses['home'].street");
        compiler.compile();

        assertFalse(compiler.getParserContextState().getInputs().keySet().contains("home"));
    }

    public void testVarInputs4() {
        ExpressionCompiler compiler = new ExpressionCompiler("System.out.println( message );");
        compiler.compile();

        assertTrue(compiler.getParserContextState().getInputs().keySet().contains("message"));
    }


    public void testAnalyzer() {
        ExpressionCompiler compiler = new ExpressionCompiler("order.id == 10");
        compiler.compile();

        for (String input : compiler.getParserContextState().getInputs().keySet()) {
            System.out.println("input>" + input);
        }

        assertEquals(1, compiler.getParserContextState().getInputs().size());
        assertTrue(compiler.getParserContextState().getInputs().containsKey("order"));
    }


    public void testClassImportViaFactory() {
        MapVariableResolverFactory mvf = new MapVariableResolverFactory(map);
        ClassImportResolverFactory classes = new ClassImportResolverFactory();
        classes.addClass(HashMap.class);

        ResolverTools.appendFactory(mvf, classes);

        Serializable compiled = compileExpression("HashMap map = new HashMap()", classes.getImportedClasses());

        assertTrue(executeExpression(compiled, mvf) instanceof HashMap);
    }

    public void testSataticClassImportViaFactory() {
        MapVariableResolverFactory mvf = new MapVariableResolverFactory(map);
        ClassImportResolverFactory classes = new ClassImportResolverFactory();
        classes.addClass(Person.class);

        ResolverTools.appendFactory(mvf, classes);

        Serializable compiled = compileExpression("p = new Person('tom'); return p.name;", classes.getImportedClasses());

        assertEquals("tom", executeExpression(compiled, mvf));
    }

    public void testSataticClassImportViaFactoryAndWithModification() {
        MapVariableResolverFactory mvf = new MapVariableResolverFactory(map);
        ClassImportResolverFactory classes = new ClassImportResolverFactory();
        classes.addClass(Person.class);

        ResolverTools.appendFactory(mvf, classes);

        Serializable compiled = compileExpression("p = new Person('tom'); p.age = 20; with( p ) { age = p.age + 1 }; return p.age;", classes.getImportedClasses());

        assertEquals(21, executeExpression(compiled, mvf));
    }

    public void testCheeseConstructor() {
        MapVariableResolverFactory mvf = new MapVariableResolverFactory(map);
        ClassImportResolverFactory classes = new ClassImportResolverFactory();
        classes.addClass(Cheese.class);

        ResolverTools.appendFactory(mvf, classes);

        Serializable compiled = compileExpression("cheese = new Cheese(\"cheddar\", 15);", classes.getImportedClasses());

        assertTrue(executeExpression(compiled, mvf) instanceof Cheese);
    }

    public void testInterceptors() {
        Interceptor testInterceptor = new Interceptor() {
            public int doBefore(ASTNode node, VariableResolverFactory factory) {
                System.out.println("BEFORE Node: " + node.getName());
                return 0;
            }

            public int doAfter(Object val, ASTNode node, VariableResolverFactory factory) {
                System.out.println("AFTER Node: " + node.getName());
                return 0;
            }
        };

        Map<String, Interceptor> interceptors = new HashMap<String, Interceptor>();
        interceptors.put("test", testInterceptor);

        Serializable compiled = compileExpression("@test System.out.println('MIDDLE');", null, interceptors);

        executeExpression(compiled);
    }


    public void testMacroSupport() {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("foo", new Foo());

        Map<String, Interceptor> interceptors = new HashMap<String, Interceptor>();
        Map<String, Macro> macros = new HashMap<String, Macro>();

        interceptors.put("Modify", new Interceptor() {
            public int doBefore(ASTNode node, VariableResolverFactory factory) {
                ((WithNode) node).getNestedStatement().getValue(null,
                        factory);
                factory.createVariable("mod", "FOOBAR!");
                return 0;
            }

            public int doAfter(Object val, ASTNode node, VariableResolverFactory factory) {
                return 0;
            }
        });

        macros.put("modify", new Macro() {
            public String doMacro() {
                return "@Modify with";
            }
        });

        ExpressionCompiler compiler = new ExpressionCompiler(parseMacros("modify (foo) { aValue = 'poo' }; mod", macros));
        compiler.setDebugSymbols(true);

        ParserContext ctx = new ParserContext(null, interceptors, null);
        ctx.setSourceFile("test.mv");

        CompiledExpression compiled = compiler.compile(ctx);

        assertEquals("FOOBAR!", MVEL.executeExpression(compiled, null, vars));
    }

    public void testComments() {
        assertEquals(10, test("// This is a comment\n5 + 5"));
    }

    public void testComments2() {
        assertEquals(20, test("10 + 10; // This is a comment"));
    }

    public void testComments3() {
        assertEquals(30, test("/* This is a test of\r\n" +
                "MVEL's support for\r\n" +
                "multi-line comments\r\n" +
                "*/\r\n 15 + 15"));
    }

    public void testComments4() {
        assertEquals(50, test("/** This is a fun test script **/\r\n" +
                "a = 10;\r\n" +
                "/**\r\n" +
                "* Here is a useful variable\r\n" +
                "*/\r\n" +
                "b = 20; // set b to '20'\r\n" +
                "return ((a + b) * 2) - 10;\r\n" +
                "// last comment\n"));
    }

    public void testSubtractNoSpace1() {
        assertEquals(59, test("hour-1"));
    }

    public void testStrictTypingCompilation() {
        ExpressionCompiler compiler = new ExpressionCompiler("a.foo;\nb.foo;\n x = 5");
        ParserContext ctx = new ParserContext();
        ctx.setStrictTypeEnforcement(true);

        try {
            compiler.compile(ctx);
        }
        catch (CompileException e) {
            e.printStackTrace();
            assertEquals(2, e.getErrors().size());
            return;
        }
        assertTrue(false);
    }

    public void testStrictStaticMethodCall() {
        ExpressionCompiler compiler = new ExpressionCompiler("Bar.staticMethod()");
        ParserContext ctx = new ParserContext();
        ctx.addImport("Bar", Bar.class);
        ctx.setStrictTypeEnforcement(true);

        Serializable s = compiler.compile(ctx);

        DebugTools.decompile(s);

        assertEquals(1, executeExpression(s));
    }

    public void testStrictTypingCompilation2() throws Exception {
        ParserContext ctx = new ParserContext();
        //noinspection RedundantArrayCreation
        ctx.addImport("getRuntime", new MethodStub(Runtime.class.getMethod("getRuntime", new Class[]{})));

        ctx.setStrictTypeEnforcement(true);

        ExpressionCompiler compiler = new ExpressionCompiler("getRuntime()");
        StaticMethodImportResolverFactory si = new StaticMethodImportResolverFactory(ctx);

        Serializable expression = compiler.compile(ctx);

        serializationTest(expression);

        assertTrue(executeExpression(expression, si) instanceof Runtime);
    }

    public void testStrictTypingCompilation3() throws NoSuchMethodException {
        ParserContext ctx = new ParserContext();

        ctx.setStrictTypeEnforcement(true);

        ExpressionCompiler compiler =
                new ExpressionCompiler("message='Hello';b=7;\nSystem.out.println(message + ';' + b);\n" +
                        "System.out.println(message + ';' + b); b");


        assertEquals(7, executeExpression(compiler.compile(ctx), new DefaultLocalVariableResolverFactory()));
    }

    public void testProvidedExternalTypes() {
        ExpressionCompiler compiler = new ExpressionCompiler("foo.bar");
        ParserContext ctx = new ParserContext();
        ctx.setStrictTypeEnforcement(true);
        ctx.addInput("foo", Foo.class);

        compiler.compile(ctx);
    }

    public void testEqualityRegression() {
        ExpressionCompiler compiler = new ExpressionCompiler("price == (new Integer( 5 ) + 5 ) ");
        compiler.compile();
    }

    public void testEvaluationRegression() {
        ExpressionCompiler compiler = new ExpressionCompiler("(p.age * 2)");
        compiler.compile();
        assertTrue(compiler.getParserContextState().getInputs().containsKey("p"));
    }

    public void testAssignmentRegression() {
        ExpressionCompiler compiler = new ExpressionCompiler("total = total + $cheese.price");
        compiler.compile();
    }

    public void testTypeRegression() {
        ExpressionCompiler compiler = new ExpressionCompiler("total = 0");
        ParserContext ctx = new ParserContext();
        ctx.setStrictTypeEnforcement(true);
        compiler.compile(ctx);
        assertEquals(Integer.class,
                compiler.getParserContextState().getVarOrInputType("total"));
    }

    public void testDateComparison() {
        map.put("dt1", new Date(currentTimeMillis() - 100000));
        map.put("dt2", new Date(currentTimeMillis()));

        assertTrue((Boolean) test("dt1 < dt2"));
    }

    public void testDynamicDeop() {
        Serializable s = MVEL.compileExpression("name");

        assertEquals("dog", MVEL.executeExpression(s, foo));
        assertEquals("dog", MVEL.executeExpression(s, foo.getBar()));
    }

    public void testVirtProperty() {
        Map<String, Object> testMap = new HashMap<String, Object>();
        testMap.put("test", "foo");

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("mp", testMap);

        assertEquals("bar", MVEL.executeExpression(compileExpression("mp.test = 'bar'; mp.test"), vars));
    }

    public void testMapPropertyCreateCondensed() {
        assertEquals("foo", test("map = new java.util.HashMap(); map['test'] = 'foo'; map['test'];"));
    }

    public void testClassLiteral() {
        assertEquals(String.class, test("java.lang.String"));
    }

    public void testDeepMethod() {
        assertEquals(false, test("foo.bar.testList.add(new String()); foo.bar.testList == empty"));
    }

    public void testArrayAccessorAssign() {
        assertEquals("foo", test("a = {'f00', 'bar'}; a[0] = 'foo'; a[0]"));
    }

    public void testListAccessorAssign() {
        assertEquals("bar", test("a = new java.util.ArrayList(); a.add('foo'); a.add('BAR'); a[1] = 'bar'; a[1]"));
    }

    public void testBracketInString() {
        test("System.out.println('1)your guess was:');");
    }

    public void testNesting() {
        assertEquals("foo", test("new String(new String(new String(\"foo\")));"));
    }

    public void testDeepPropertyAdd() {
        assertEquals(10, test("foo.countTest+ 10"));
    }

    public void testDeepAssignmentIncrement() {
        assertEquals(true, test("foo.countTest += 5; if (foo.countTest == 5) { foo.countTest = 0; return true; } else { foo.countTest = 0; return false; }"));
    }

    public void testDeepAssignmentWithBlock() {
        assertEquals(true, test("with (foo) { countTest += 5 }; if (foo.countTest == 5) { foo.countTest = 0; return true; } else { foo.countTest = 0; return false; }"));
    }

    public void testTypeCast() {
        assertEquals("10", test("(String) 10"));
    }

    public void testMapAccessSemantics() {
        Map<String, Object> outermap = new HashMap<String, Object>();
        Map<String, Object> innermap = new HashMap<String, Object>();

        innermap.put("test", "foo");
        outermap.put("innermap", innermap);

        assertEquals("foo", test("innermap['test']", outermap, null));
    }

    public void testMapBindingSemantics() {
        Map<String, Object> outermap = new HashMap<String, Object>();
        Map<String, Object> innermap = new HashMap<String, Object>();

        innermap.put("test", "foo");
        outermap.put("innermap", innermap);

        MVEL.setProperty(outermap, "innermap['test']", "bar");

        assertEquals("bar", test("innermap['test']", outermap, null));
    }

    public void testSetSemantics() {
        Bar bar = new Bar();
        Foo foo = new Foo();

        assertEquals("dog", MVEL.getProperty("name", bar));
        assertEquals("dog", MVEL.getProperty("name", foo));
    }

    public void testMapBindingSemantics2() {
        Map<String, Object> outermap = new HashMap<String, Object>();
        Map<String, Object> innermap = new HashMap<String, Object>();

        innermap.put("test", "foo");
        outermap.put("innermap", innermap);

        Serializable s = MVEL.compileSetExpression("innermap['test']");

        MVEL.executeSetExpression(s, outermap, "bar");

        assertEquals("bar", test("innermap['test']", outermap, null));
    }

    public void testDynamicImports() {
        ParserContext ctx = new ParserContext();
        ctx.addPackageImport("java.util");

        ExpressionCompiler compiler = new ExpressionCompiler("HashMap");
        Serializable s = compiler.compile(ctx);

        assertEquals(HashMap.class, MVEL.executeExpression(s));

        compiler = new ExpressionCompiler("map = new HashMap(); map.size()");
        s = compiler.compile(ctx);

        assertEquals(0, MVEL.executeExpression(s, new DefaultLocalVariableResolverFactory()));
    }

    public void testTypedAssignment() {
        assertEquals("foobar", test("java.util.Map map = new java.util.HashMap(); map.put('conan', 'foobar'); map['conan'];"));
    }

    public void testFQCNwithStaticInList() {
        assertEquals(Integer.MIN_VALUE, test("list = [java.lang.Integer.MIN_VALUE]; list[0]"));
    }

    public void testPrecedenceOrder() {
        assertTrue((Boolean) test("5 > 6 && 2 < 1 || 10 > 9"));
    }

    @SuppressWarnings({"unchecked"})
    public void testDifferentImplSameCompile() {
        Serializable compiled = compileExpression("a.funMap.hello");

        Map testMap = new HashMap();

        for (int i = 0; i < 100; i++) {
            Base b = new Base();
            b.funMap.put("hello", "dog");
            testMap.put("a", b);


            assertEquals("dog", executeExpression(compiled, testMap));

            b = new Base();
            b.funMap.put("hello", "cat");
            testMap.put("a", b);

            assertEquals("cat", executeExpression(compiled, testMap));
        }
    }

    @SuppressWarnings({"unchecked"})
    public void testInterfaceMethodCallWithSpace() {
        Serializable compiled = compileExpression("drools.retract (cheese)");
        Map map = new HashMap();
        DefaultKnowledgeHelper helper = new DefaultKnowledgeHelper();
        map.put("drools", helper);
        Cheese cheese = new Cheese("stilton", 15);
        map.put("cheese", cheese);

        executeExpression(compiled, map);
        assertSame(cheese, helper.retracted.get(0));
    }

    @SuppressWarnings({"unchecked"})
    public void testInterfaceMethodCallWithMacro() {
        Map macros = new HashMap(1);

        macros.put("retract",
                new Macro() {
                    public String doMacro() {
                        return "drools.retract";
                    }
                });

        Serializable compiled = compileExpression(parseMacros("retract(cheese)", macros));
        Map map = new HashMap();
        DefaultKnowledgeHelper helper = new DefaultKnowledgeHelper();
        map.put("drools", helper);
        Cheese cheese = new Cheese("stilton", 15);
        map.put("cheese", cheese);

        executeExpression(compiled, map);
        assertSame(cheese, helper.retracted.get(0));
    }


    @SuppressWarnings({"UnnecessaryBoxing"})
    public void testToList() {
        String text = "misc.toList(foo.bar.name, 'hello', 42, ['key1' : 'value1', c : [ foo.bar.age, 'car', 42 ]], [42, [c : 'value1']] )";

        List list = (List) test(text);

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

    @SuppressWarnings({"UnnecessaryBoxing"})
    public void testToListStrictMode() {
        String text = "misc.toList(foo.bar.name, 'hello', 42, ['key1' : 'value1', c : [ foo.bar.age, 'car', 42 ]], [42, [c : 'value1']] )";

        ParserContext ctx = new ParserContext();
        ctx.addInput("misc", MiscTestClass.class);
        ctx.addInput("foo", Foo.class);
        ctx.addInput("c", String.class);

        ctx.setStrictTypeEnforcement(true);
        ExpressionCompiler compiler = new ExpressionCompiler(text);
        Serializable expr = compiler.compile(ctx);

        List list = (List) MVEL.executeExpression(expr, map);

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

//    public void testToList2() {
//        for (int i = 0; i < 10; i++) {
//            testToList();
//        }
//    }

    public void testParsingStability1() {
        assertEquals(true, test("( order.number == 1 || order.number == ( 1+1) || order.number == $id )"));
    }

    public void testParsingStability2() {

        ExpressionCompiler compiler = new ExpressionCompiler("( dim.height == 1 || dim.height == ( 1+1) || dim.height == x )");

        Map<String, Object> imports = new HashMap<String, Object>();
        imports.put("java.awt.Dimension", Dimension.class);

        final ParserContext parserContext = new ParserContext(imports,
                null,
                "sourceFile");

        parserContext.setStrictTypeEnforcement(false);

        compiler.compile(parserContext);

    }

    public void testParsingStability3() {
        assertEquals(false, test("!( [\"X\", \"Y\"] contains \"Y\" )"));
    }

    public void testParsingStability4() {
        assertEquals(true, test("vv=\"Edson\"; !(vv ~= \"Mark\")"));
    }

    public void testConcatWithLineBreaks() {
        ExpressionCompiler parser = new ExpressionCompiler("\"foo\"+\n\"bar\"");

        ParserContext ctx = new ParserContext();
        ctx.setDebugSymbols(true);
        ctx.setSourceFile("source.mv");

        Serializable c = parser.compile(ctx);

        assertEquals("foobar", MVEL.executeExpression(c));
    }


    /**
     * Community provided test cases
     */
    @SuppressWarnings({"unchecked"})
    public void testCalculateAge() {
        Calendar c1 = Calendar.getInstance();
        c1.set(1999, 0, 10); // 1999 jan 20
        Map objectMap = new HashMap(1);
        Map propertyMap = new HashMap(1);
        propertyMap.put("GEBDAT", c1.getTime());
        objectMap.put("EV_VI_ANT1", propertyMap);
        assertEquals("N", test("new org.mvel.tests.main.res.PDFFieldUtil().calculateAge(EV_VI_ANT1.GEBDAT) >= 25 ? 'Y' : 'N'"
                , null, objectMap));
    }

    /**
     * Provided by: Alex Roytman
     */

    public void testMethodResolutionWithNullParameter() {
        Context ctx = new Context();
        ctx.setBean(new Bean());
        Map<String, Object> vars = new HashMap<String, Object>();
        System.out.println("bean.today: " + MVEL.eval("bean.today", ctx, vars));
        System.out.println("formatDate(bean.today): " + MVEL.eval("formatDate(bean.today)", ctx, vars));
        //calling method with string param with null parameter works
        System.out.println("formatString(bean.nullString): " + MVEL.eval("formatString(bean.nullString)", ctx, vars));
        System.out.println("bean.myDate = bean.nullDate: " + MVEL.eval("bean.myDate = bean.nullDate; return bean.nullDate;", ctx, vars));
        //calling method with Date param with null parameter fails
        System.out.println("formatDate(bean.myDate): " + MVEL.eval("formatDate(bean.myDate)", ctx, vars));
        //same here
        System.out.println(MVEL.eval("formatDate(bean.nullDate)", ctx, vars));
    }

    /**
     * Provided by: Phillipe Ombredanne
     */
    public void testCompileParserContextShouldNotLoopIndefinitelyOnValidJavaExpression() {
        String expr = "		System.out.println( message );\n" + //
                "m.setMessage( \"Goodbye cruel world\" );\n" + //
                "System.out.println(m.getStatus());\n" + //
                "m.setStatus( Message.GOODBYE );\n";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);

        ParserContext context = new ParserContext();
        context.setStrictTypeEnforcement(false);

        context.addImport("Message", Message.class);
        context.addInput("System", void.class);
        context.addInput("message", Object.class);
        context.addInput("m", Object.class);

        compiler.compile(context);
    }

    public void testStaticNested() {
        assertEquals(1, MVEL.eval("org.mvel.tests.main.AbstractTest$Message.GOODBYE", new HashMap()));
    }

    public void testStaticNestedWithImport() {
        String expr = "Message.GOODBYE;\n";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);

        ParserContext context = new ParserContext();
        context.setStrictTypeEnforcement(false);

        context.addImport("Message", Message.class);
        Serializable compiledExpression = compiler.compile(context);

        assertEquals(1, MVEL.executeExpression(compiledExpression));
    }

    public void testStaticNestedWithMethodCall() {
        String expr = "item = new Item( \"Some Item\"); $msg.addItem( item ); return $msg";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);

        ParserContext context = new ParserContext();
        context.setStrictTypeEnforcement(false);

        context.addImport("Message", Message.class);
        context.addImport("Item", Item.class);
        Serializable compiledExpression = compiler.compile(context);

        Map vars = new HashMap();
        vars.put("$msg", new Message());
        Message msg = (Message) MVEL.executeExpression(compiledExpression, vars);
        Item item = (Item) msg.getItems().get(0);
        assertEquals("Some Item", item.getName());
    }

//    public void testParserStringIssueNeverReturns() {
//        String expr = "Sstem.out.println(drools.workingMemory); ";
//
//        ExpressionCompiler compiler = new ExpressionCompiler(expr);
//
//        ParserContext context = new ParserContext();
//        context.setStrictTypeEnforcement(true);
//        context.addInput( "drools", KnowledgeHelper.class);
//
//        RuleBase ruleBase = new RuleBaseImpl();
//        WorkingMemory wm = new WorkingMemoryImpl( ruleBase );
//        KnowledgeHelper drools = new DefaultKnowledgeHelper( wm );
//        Serializable compiledExpression = compiler.compile(context);
//
//        Map vars = new HashMap();
//        vars.put( "drools", drools );
//        MVEL.executeExpression(compiledExpression, vars);
//    }

    public void testsequentialAccessorsThenMethodCall() {
        String expr = "System.out.println(drools.workingMemory); drools.workingMemory.ruleBase.removeRule(\"org.drools.examples\", \"some rule\"); ";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);

        ParserContext context = new ParserContext();
        context.setStrictTypeEnforcement(true);
        context.addInput("drools", KnowledgeHelper.class);

        RuleBase ruleBase = new RuleBaseImpl();
        WorkingMemory wm = new WorkingMemoryImpl(ruleBase);
        KnowledgeHelper drools = new DefaultKnowledgeHelper(wm);
        Serializable compiledExpression = compiler.compile(context);

        Map vars = new HashMap();
        vars.put("drools", drools);
        MVEL.executeExpression(compiledExpression, vars);
    }


    /**
     * Provided by: Aadi Deshpande
     */
    public void testPropertyVerfierShoudldNotLoopIndefinately() {
        String expr = "\t\tmodel.latestHeadlines = $list;\n" +
                "model.latestHeadlines.add( 0, (model.latestHeadlines[2]) );";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);
        compiler.setVerifying(true);

        ParserContext pCtx = new ParserContext();
        pCtx.addInput("$list", List.class);
        pCtx.addInput("model", Model.class);

        compiler.compile(pCtx);
    }

    public void testCompileWithNewInsideMethodCall() {
        String expr = "     p.name = \"goober\";\n" +
                "     System.out.println(p.name);\n" +
                "     drools.insert(new Address(\"Latona\"));\n";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);

        ParserContext context = new ParserContext();
        context.setStrictTypeEnforcement(false);

        context.addImport("Person", Person.class);
        context.addImport("Address", Address.class);

        context.addInput("p", Person.class);
        context.addInput("drools", Drools.class);

        compiler.compile(context);
    }


    /**
     * Submitted by: cleverpig
     */

    public void testBug4() {
        ClassA A = new ClassA();
        ClassB B = new ClassB();
        System.out.println(MVEL.getProperty("date", A));
        System.out.println(MVEL.getProperty("date", B));
    }


    /**
     * Submitted by: Michael Neale
     */

    public void testInlineCollectionParser1() {
        assertEquals("q", ((Map) test("['Person.age' : [1, 2, 3, 4],'Person.rating' : 'q']")).get("Person.rating"));
        assertEquals("q", ((Map) test("['Person.age' : [1, 2, 3, 4], 'Person.rating' : 'q']")).get("Person.rating"));
    }

    public void testIndexer() {
        assertEquals("foobar", test("import java.util.LinkedHashMap; LinkedHashMap map = new LinkedHashMap();" +
                " map.put('a', 'foo'); map.put('b', 'bar'); s = ''; foreach (key : map.keySet()) { System.out.println(map[key]); s += map[key]; }; return s;"));
    }

    public void testLateResolveOfClass() {
        ExpressionCompiler compiler = new ExpressionCompiler("System.out.println(new Foo());");
        ParserContext ctx = new ParserContext();
        ctx.addImport(Foo.class);

        CompiledExpression s = compiler.compile(ctx);
        compiler.removeParserContext();

        System.out.println(MVEL.executeExpression(s));
    }

//    public void testSwing() {
//        test("import javax.swing.JFrame;\n" +
//                "import javax.swing.JLabel;\n" +
//                "\n" +
//                "with (frame = new JFrame()) {\n" +
//                "    title = \"My Swing Frame\",\n" +
//                "    resizable = true\n" +
//                "}\n" +
//                "\n" +
//                "frame.contentPane.add(new JLabel(\"My Label\"));\n" +
//                "frame.pack();\n" +
//                "frame.visible = true;");
//    }

}



