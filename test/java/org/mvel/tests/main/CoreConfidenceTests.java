package org.mvel.tests.main;

import junit.framework.TestCase;
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
import org.mvel.integration.impl.LocalVariableResolverFactory;
import org.mvel.integration.impl.MapVariableResolverFactory;
import org.mvel.integration.impl.StaticMethodImportResolverFactory;
import org.mvel.optimizers.OptimizerFactory;
import org.mvel.tests.main.res.*;

import java.io.Serializable;
import static java.lang.System.currentTimeMillis;
import java.text.SimpleDateFormat;
import java.util.*;

public class CoreConfidenceTests extends TestCase {
    protected Foo foo = new Foo();
    protected Map<String, Object> map = new HashMap<String, Object>();
    protected Base base = new Base();
    protected DerivedClass derived = new DerivedClass();

    public CoreConfidenceTests() {
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
                new TestInterface() {

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

    public void testBooleanOperator3() {
        assertEquals(true, parseDirect("foo.bar.woof== true"));
    }

    public void testBooleanOperator4() {
        assertEquals(false, parseDirect("foo.bar.woof ==false"));
    }

    public void testBooleanOperator5() {
        assertEquals(true, parseDirect("foo.bar.woof == true"));
    }

    public void testBooleanOperator6() {
        assertEquals(false, parseDirect("foo.bar.woof==false"));
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

    public void testAnd2() {
        assertEquals(true, parseDirect("c!=null&&foo.bar.name=='dog'&&foo.bar.woof"));
    }

    public void testMath() {
        assertEquals(188.4f, parseDirect("pi * hour"));
    }

    public void testMath2() {
        assertEquals(3, parseDirect("foo.number-1"));
    }

    public void testPowerOf() {
        assertEquals(25, parseDirect("5 ** 2"));
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

    public void testShortPathExpression4() {
        assertEquals(true, parseDirect("4>3||foo.toUC('test')"));
    }

    public void testOrOperator() {
        assertEquals(true, parseDirect("true||true"));
    }

    public void testOrOperator2() {
        assertEquals(true, parseDirect("2 > 3 || 3 > 2"));
    }

    public void testOrOperator3() {
        assertEquals(true, parseDirect("pi > 5 || pi > 6 || pi > 3"));
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

    public void testMethodAccess6() {
        assertEquals(true, parseDirect("   equalityCheck(   c  \n  ,   \n   'cat'      )   "));
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

    public void testRegEx3() {
        assertEquals(true, parseDirect("foo.bar.name~='[a-z].+'&&foo.bar.name!=null"));
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
        assertEquals(true, parseDirect("c is java.lang.String"));
    }

    public void testInstanceCheck2() {
        assertEquals(false, parseDirect("pi is java.lang.Integer"));
    }

    public void testInstanceCheck3() {
        assertEquals(true, parseDirect("foo is org.mvel.tests.main.res.Foo"));
    }

    public void testBitwiseOr1() {
        assertEquals(6, parseDirect("2|4"));
    }

    public void testBitwiseOr2() {
        assertEquals(true, parseDirect("(2 | 1) > 0"));
    }

    public void testBitwiseOr3() {
        assertEquals(true, parseDirect("(2|1) == 3"));
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

    public void testEmptyArray2() {
        assertTrue(((Object[]) parseDirect("{    }")).length == 0);
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

    public void testMapCreation3() {
        assertEquals("foo", parseDirect("map = [1 : 'foo']; map[1]"));
    }

    public void testProjectionSupport() {
        assertEquals(true, parseDirect("(name in things)contains'Bob'"));
    }

    public void testProjectionSupport1() {
        assertEquals(true, parseDirect("(name in things) contains 'Bob'"));
    }

    public void testProjectionSupport2() {
        assertEquals(3, parseDirect("(name in things).size()"));
    }

    public void testSizeOnInlineArray() {
        assertEquals(3, parseDirect("{1,2,3}.size()"));
    }


    public void testStaticMethodFromLiteral() {
        assertEquals(String.class.getName(), parseDirect("String.valueOf(Class.forName('java.lang.String').getName())"));
    }

//    public void testMethodCallsEtc() {
//        parseDirect("title = 1; " +
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
        parseDirect("new java.lang.String('foobie')");
    }

    public void testObjectInstantiationWithMethodCall() {
        parseDirect("new String('foobie')  . toString()");
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
        assertEquals(true, parseDirect("this is org.mvel.tests.main.res.Base"));
    }

    public void testThisReference4() {
        assertEquals(true, parseDirect("this.funMap instanceof java.util.Map"));
    }

    public void testThisReference5() {
        assertEquals(true, parseDirect("this.data == 'cat'"));
    }

    public void testThisReferenceInMethodCall() {
        assertEquals(101, parseDirect("Integer.parseInt(this.number)"));
    }

    public void testThisReferenceInConstructor() {
        assertEquals("101", parseDirect("new String(this.number)"));
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

        OptimizerFactory.setDefaultOptimizer("ASM");

        // Run test
        assertEquals(true, MVEL.executeExpression(compiled, map, factory));
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
        Serializable compiled = compileExpression("['foo':'bar'] contains 'foo'");
        executeExpression(compiled, null, null, Boolean.class);
    }

    public void testSubListInMap() {
        assertEquals("pear", parseDirect("map = ['test' : 'poo', 'foo' : [c, 'pear']]; map['foo'][1]"));
    }

    public void testCompiledMethodCall() {
        Serializable compiled = compileExpression("c.getClass()");
        assertEquals(String.class, executeExpression(compiled, base, map));
    }

    public void testStaticNamespaceCall() {
        assertEquals(java.util.ArrayList.class, parseDirect("java.util.ArrayList"));
    }

    public void testStaticNamespaceClassWithMethod() {
        assertEquals("FooBar", parseDirect("java.lang.String.valueOf('FooBar')"));
    }

    public void testConstructor() {
        assertEquals("foo", parseDirect("a = 'foobar'; new String(a.toCharArray(), 0, 3)"));
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

    public void testEmptyIf() {
        assertEquals(5, parseDirect("a = 5; if (a == 5) { }; return a;"));
    }

    public void testEmptyIf2() {
        assertEquals(5, parseDirect("a=5;if(a==5){};return a;"));
    }

    public void testIf() {
        assertEquals(10, parseDirect("if (5 > 4) { return 10; } else { return 5; }"));
    }

    public void testIf2() {
        assertEquals(10, parseDirect("if (5 < 4) { return 5; } else { return 10; }"));
    }

    public void testIf3() {
        assertEquals(10, parseDirect("if(5<4){return 5;}else{return 10;}"));
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
        assertEquals(6, parseDirect("total = 0; a = {1,2,3}; foreach(item : a) { total += item }; total"));
    }

    public void testForEach3() {
        assertEquals(true, parseDirect("a = {1,2,3}; foreach (i : a) { if (i == 1) { return true; } }"));
    }

    public void testForEach4() {
        assertEquals("OneTwoThreeFour", parseDirect("a = {1,2,3,4}; builder = ''; foreach (i : a) {" +
                " if (i == 1) { builder += 'One' } else if (i == 2) { builder += 'Two' } " +
                "else if (i == 3) { builder += 'Three' } else { builder += 'Four' }" +
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

    public void testIncrementOperator() {
        assertEquals(2, parseDirect("x = 1; x++; x"));
    }

    public void testPreIncrementOperator() {
        assertEquals(2, parseDirect("x = 1; ++x"));
    }

    public void testDecrementOperator() {
        assertEquals(1, parseDirect("x = 2; x--; x"));
    }

    public void testPreDecrementOperator() {
        assertEquals(1, parseDirect("x = 2; --x"));
    }

    public void testQualifiedStaticTyping() {
        assertEquals(20, parseDirect("java.math.BigDecimal a = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal b = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal c = a + b; return c; "));
    }

    public void testUnQualifiedStaticTyping() {
        assertEquals(20, parseDirect("import java.math.BigDecimal; BigDecimal a = new BigDecimal( 10.0 ); BigDecimal b = new BigDecimal( 10.0 ); BigDecimal c = a + b; return c; "));
    }

    public void testObjectCreation() {
        assertEquals(6, parseDirect("new Integer( 6 )"));
    }

    public void testTernary4() {
        assertEquals("<test>", parseDirect("true ? '<test>' : '<poo>'"));
    }

    public void testStringAsCollection() {
        assertEquals('o', parseDirect("abc = 'foo'; abc[1]"));
    }

    public void testSubExpressionIndexer() {
        assertEquals("bar", parseDirect("xx = new java.util.HashMap(); xx.put('foo', 'bar'); prop = 'foo'; xx[prop];"));
    }

    public void testCompileTimeLiteralReduction() {
        assertEquals(1000, parseDirect("10 * 100"));
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
                parseDirect("set = new java.util.HashSet(); " +
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
        assertEquals(5, parseDirect("l = new java.util.LinkedList();" +
                "l.add('fun');" +
                "l.add('happy');" +
                "l.add('fun');" +
                "l.add('slide');" +
                "l.add('crap');" +
                "poo = new java.util.ArrayList(l);" +
                "poo.size();"));
    }

    public void testMapOperations() {
        assertEquals("poo5", parseDirect(
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
        assertEquals(10, parseDirect(
                "stk = new java.util.Stack();" +
                        "stk.push(5);" +
                        "stk.push(5);" +
                        "stk.pop() + stk.pop();"
        ));
    }

    public void testSystemOutPrint() {
         parseDirect("a = 0;\r\nSystem.out.println('This is a test');");
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
        ExpressionCompiler compiler = new ExpressionCompiler("/** This is a comment\n" +
                "Second comment line\n" +
                "Third Comment Line\n" +
                "*/\n" +
                "System.out.println('4');\n" +
                "System.out.println('5');\n" +
                "a = 0;\n" +
                " b = 1;\n" +
                " a + b");
        compiler.setDebugSymbols(true);

        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("test2.mv");

        CompiledExpression compiled = compiler.compile(ctx);

        System.out.println(DebugTools.decompile(compiled));

        MVELRuntime.registerBreakpoint("test2.mv", 5);

        Debugger testDebugger = new Debugger() {

            public int onBreak(Frame frame) {
                System.out.println("Breakpoint [source:" + frame.getSourceName() + "; line:" + frame.getLineNumber() + "]");
                return 0;
            }

        };

        MVELRuntime.setThreadDebugger(testDebugger);

        assertEquals(1, MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(map)));
    }


    public void testBreakpointsAcrossComments2() {
        ExpressionCompiler compiler = new ExpressionCompiler(
                "// This is a comment\n" +
                        "//Second comment line\n" +
                        "//Third Comment Line\n" +
                        "\n" +
                        "System.out.println('4');\n" +
                        "System.out.println('5');\n" +
                        "a = 0;\n" +
                        "b = 1;\n" +
                        " a + b");
        compiler.setDebugSymbols(true);

        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("test2.mv");

        CompiledExpression compiled = compiler.compile(ctx);

        System.out.println(DebugTools.decompile(compiled));

        MVELRuntime.registerBreakpoint("test2.mv", 6);

        Debugger testDebugger = new Debugger() {
            public int onBreak(Frame frame) {
                System.out.println("Breakpoint [source:" + frame.getSourceName() + "; line:" + frame.getLineNumber() + "]");
                return 0;
            }
        };

        MVELRuntime.setThreadDebugger(testDebugger);

        assertEquals(1, MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(map)));
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
        assertEquals("happyBar", parseDirect("foo.happy(); foo.bar.happy()"));
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
                Object object = ((WithNode) node).getNestedStatement().getValue(null,
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
        assertEquals(10, parseDirect("// This is a comment\n5 + 5"));
    }

    public void testComments2() {
        assertEquals(20, parseDirect("10 + 10; // This is a comment"));
    }

    public void testComments3() {
        assertEquals(30, parseDirect("/* This is a test of\r\n" +
                "MVEL's support for\r\n" +
                "multi-line comments\r\n" +
                "*/\r\n 15 + 15"));
    }

    public void testComments4() {
        assertEquals(50, parseDirect("/** This is a fun test script **/\r\n" +
                "a = 10;\r\n" +
                "/**\r\n" +
                "* Here is a useful variable\r\n" +
                "*/\r\n" +
                "b = 20; // set b to '20'\r\n" +
                "return ((a + b) * 2) - 10;\r\n" +
                "// last comment\n"));
    }

    public void testSubtractNoSpace1() {
        assertEquals(59, parseDirect("hour-1"));
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
        ctx.addImport( "Bar", Bar.class );
        ctx.setStrictTypeEnforcement(true);

        compiler.compile(ctx);
        
        assertEquals(1, executeExpression(compiler.compile(ctx) ) );
    }    

    public void testStrictTypingCompilation2() throws NoSuchMethodException {
        ParserContext ctx = new ParserContext();
        //noinspection RedundantArrayCreation
        ctx.addImport("getRuntime", Runtime.class.getMethod("getRuntime", new Class[]{}));

        ctx.setStrictTypeEnforcement(true);

        ExpressionCompiler compiler = new ExpressionCompiler("getRuntime()");
        StaticMethodImportResolverFactory si = new StaticMethodImportResolverFactory(ctx);

        assertTrue(executeExpression(compiler.compile(ctx), si) instanceof Runtime);
    }

    public void testStrictTypingCompilation3() throws NoSuchMethodException {
        ParserContext ctx = new ParserContext();
        //   ctx.addImport("getRuntime", Runtime.class.getMethod("getRuntime", new Class[]{}));

        ctx.setStrictTypeEnforcement(true);

        ExpressionCompiler compiler =
                new ExpressionCompiler("message='Hello';b=7;\nSystem.out.println(message + ';' + b);\n" +
                        "System.out.println(message + ';' + b); b");


        assertEquals(7, executeExpression(compiler.compile(ctx), new LocalVariableResolverFactory()));
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

        assertTrue((Boolean) parseDirect("dt1 < dt2"));
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
        assertEquals("foo", parseDirect("map = new java.util.HashMap(); map['test'] = 'foo'; map['test'];"));
    }

    public void testClassLiteral() {
        assertEquals(String.class, parseDirect("java.lang.String"));
    }

    public void testDeepMethod() {
        assertEquals(false, parseDirect("foo.bar.testList.add(new String()); foo.bar.testList == empty"));
    }

    public void testArrayAccessorAssign() {
        assertEquals("foo", parseDirect("a = {'f00', 'bar'}; a[0] = 'foo'; a[0]"));
    }

    public void testListAccessorAssign() {
        assertEquals("bar", parseDirect("a = new java.util.ArrayList(); a.add('foo'); a.add('BAR'); a[1] = 'bar'; a[1]"));
    }

    public void testBracketInString() {
        parseDirect("System.out.println('1)your guess was:');");
    }

    public void testNesting() {
        assertEquals("foo", parseDirect("new String(new String(new String(\"foo\")));"));
    }

    public void testDeepPropertyAdd() {
        assertEquals(10, parseDirect("foo.countTest+ 10"));
    }

    public void testDeepAssignmentIncrement() {
        assertEquals(true, parseDirect("foo.countTest += 5; if (foo.countTest == 5) { foo.countTest = 0; return true; } else { foo.countTest = 0; return false; }"));
    }

    public void testDeepAssignmentWithBlock() {
        assertEquals(true, parseDirect("with (foo) { countTest += 5 }; if (foo.countTest == 5) { foo.countTest = 0; return true; } else { foo.countTest = 0; return false; }"));
    }

    public void testTypeCast() {
        assertEquals("10", parseDirect("(String) 10"));
    }


    public Object parseDirect(String ex) {
        return compiledExecute(ex);
    }

    public Object compiledExecute(String ex) {
        OptimizerFactory.setDefaultOptimizer("ASM");

        ExpressionCompiler compiler = new ExpressionCompiler(ex);

        Serializable compiled = compiler.compile();

        Object first = executeExpression(compiled, base, map);
        Object second = executeExpression(compiled, base, map);

        Object third = MVEL.eval(ex, base, map);

        if (first != null && !first.getClass().isArray()) {
            if (!first.equals(second)) {
                throw new AssertionError("Different result from test 1 and 2 (Compiled Re-Run / JIT) [first: "
                        + String.valueOf(first) + "; second: " + String.valueOf(second) + "]");
            }


            if (!first.equals(third)) {
                throw new AssertionError("Different result from test 1 and 3 (Compiled to Interpreted) [first: " +
                        String.valueOf(first) + " (" + (first != null ? first.getClass().getName() : "null") + "); third: " + String.valueOf(third) + " (" + (second != null ? first.getClass().getName() : "null") + ")]");
            }
        }

        OptimizerFactory.setDefaultOptimizer("reflective");

        compiled = compileExpression(ex);

        Object fourth = executeExpression(compiled, base, map);
        Object fifth = executeExpression(compiled, base, map);

        if (fourth != null && !fourth.getClass().isArray()) {
            if (!fourth.equals(fifth)) {
                throw new AssertionError("Different result from test 4 and 5 (Compiled Re-Run / Reflective) [first: "
                        + String.valueOf(first) + "; second: " + String.valueOf(second) + "]");
            }
        }

        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("unittest");
        ExpressionCompiler debuggingCompiler = new ExpressionCompiler(ex);
        debuggingCompiler.setDebugSymbols(true);

        Serializable compiledD = debuggingCompiler.compile(ctx);

        Object sixth = executeExpression(compiledD, base, map);
        if (sixth != null && !sixth.getClass().isArray()) {
            if (!fifth.equals(sixth)) {
                System.out.println("Payload 1 -- No Symbols: ");
                System.out.println(DebugTools.decompile(compiled));
                System.out.println();

                System.out.println("Payload 2 -- With Symbols: ");
                System.out.println(DebugTools.decompile(compiledD));
                System.out.println();

                throw new AssertionError("Different result from test 5 and 6 (Compiled to Compiled+DebuggingSymbols) [first: "
                        + String.valueOf(fifth) + "; second: " + String.valueOf(sixth) + "]");
            }
        }

        Object seventh = executeExpression(compiledD, base, map);

        if (seventh != null && !seventh.getClass().isArray()) {
             if (!seventh.equals(sixth)) {
                 throw new AssertionError("Different result from test 4 and 5 (Compiled Re-Run / Reflective) [first: "
                         + String.valueOf(first) + "; second: " + String.valueOf(second) + "]");
             }
         }


        return second;
    }

    public Object compiledExecute(String ex, Object base, Map map) {
        Serializable compiled = compileExpression(ex);

        Object first = executeExpression(compiled, base, map);
        Object second = executeExpression(compiled, base, map);

        if (first != null && !first.getClass().isArray())
            assertSame(first, second);

        return second;
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

    public void testToList2() {
        for (int i = 0; i < 10; i++) {
            testToList();
        }
    }

    public static class MiscTestClass {
        int exec = 0;

        @SuppressWarnings({"unchecked", "UnnecessaryBoxing"})
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


    /**
     * Community provided test cases
     */

    @SuppressWarnings({"unchecked"})
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

    public static class Bean {
        private Date myDate = new Date();

        public Date getToday() {
            return new Date();
        }

        public Date getNullDate() {
            return null;
        }

        public String getNullString() {
            return null;
        }

        public Date getMyDate() {
            return myDate;
        }

        public void setMyDate(Date myDate) {
            this.myDate = myDate;
        }
    }

    public static class Context {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
        private Bean bean;

        public Bean getBean() {
            return bean;
        }

        public void setBean(Bean bean) {
            this.bean = bean;
        }

        public String formatDate(Date date) {
            return date == null ? null : dateFormat.format(date);
        }

        public String formatString(String str) {
            return str == null ? "<NULL>" : str;
        }
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
        assertEquals(1, MVEL.eval("org.mvel.tests.main.CoreConfidenceTests$Message.GOODBYE", new HashMap()));
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

    public static class Person {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public static class Address {
        private String street;

        public Address(String street) {
            super();
            this.street = street;
        }

    }

    public static class Drools {
        public void insert(Object obj) {
        }
    }


    public static class Model {
        private List latestHeadlines;


        public List getLatestHeadlines() {
            return latestHeadlines;
        }

        public void setLatestHeadlines(List latestHeadlines) {
            this.latestHeadlines = latestHeadlines;
        }
    }


    public static class Message {
        public static final int HELLO = 0;
        public static final int GOODBYE = 1;

        private String message;

        private int status;

        public String getMessage() {
            return this.message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getStatus() {
            return this.status;
        }

        public void setStatus(int status) {
            this.status = status;
        }
    }

}
