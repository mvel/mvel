package org.mvel.tests.main;

import org.mvel.*;
import static org.mvel.DataConversion.convert;
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

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;

@SuppressWarnings({"PointlessArithmeticExpression", "AssertEqualsBetweenInconvertibleTypes"})
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
        Serializable s = MVEL.compileExpression("foo.bar.woof == true");

        assertEquals(true, MVEL.executeExpression(s, createTestMap()));
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
        assertEquals(188.4d, test("pi * hour"));
    }

    public void testMath2() {
        assertEquals(3, test("foo.number-1"));
    }

    public void testMath3() {
        assertEquals((10d * 5d) * 2d / 3d, test("(10 * 5) * 2 / 3"));
    }

    public void testMath4() {
        double val = ((100d % 3d) * 2d - 1d / 1d + 8d + (5d * 2d));
        System.out.println("val=" + val);

        assertNumEquals(val, test("(100 % 3) * 2 - 1 / 1 + 8 + (5 * 2)"));
    }

    public void testMath5() {
        assertNumEquals(300.5 / 5.3 / 2.1 / 1.5, test("300.5 / 5.3 / 2.1 / 1.5"));
    }

    public void testMath6() {
        assertNumEquals((300 * 5 + 1) + 100 / 2 * 2, test("(300 * five + 1) + (100 / 2 * 2)"));
    }

    public void testMath7() {
        assertNumEquals((int) ((100d % 3d) * 2d - 1d / 1d + 8d + (5d * 2d)), test("(100 % 3) * 2 - 1 / 1 + 8 + (5 * 2)"));
    }

    public void testMath8() {
        assertNumEquals(5f * (100.56f * 30.1f), test("5 * (100.56 * 30.1)"));
    }

    public void testMath9() {
        assertNumEquals(25, test("(five * 5) + 1 - 1"));
    }

    public void testMath10() {
        assertNumEquals(5f * (0.95f / 2f), convert(test("five * (0.95 / 2)"), Float.class));
    }

    public void testMath11() {
        String expression = "10-5*7-3*8-6";
        CompiledExpression expr = compileSimpleExpression(expression);
        assertNumEquals(10 - 5 * 7 - 3 * 8 - 6, executeExpression(expr));
    }

    public void testMath12() {
        String expression = "(10-5)*7-3*(8-6)**3";
        CompiledExpression expr = compileSimpleExpression(expression);
        assertNumEquals(11, executeExpression(expr));
    }

    public void testMath13() {
        String expression = "3** (2)*5**2";
        CompiledExpression expr = compileSimpleExpression(expression);
        assertNumEquals(225, executeExpression(expr));
    }

    public void testMath14() {
        assertNumEquals(10 - 5 * 2 + 5 * 8 - 4, test("10-5*2 + 5*8-4"));
    }

    public void testMath15() {
        String ex = "100-500*200 + 500*800-400";
        //   System.out.println("Expression: " + ex);

        assertNumEquals(100 - 500 * 200 + 500 * 800 - 400, test(ex));
    }

    public void testMath16() {
        String ex = "100-500*200*150 + 500*800-400";
        assertNumEquals(100 - 500 * 200 * 150 + 500 * 800 - 400, test(ex));
    }

    public void testMath17() {
        String ex = "(100 * 50) * 20 / 30 * 2";
        //    System.out.println("Expression: " + ex);
        assertNumEquals((100d * 50d) * 20d / 30d * 2d, test(ex));
    }

    public void testMath18() {
        String ex = "a = 100; b = 50; c = 20; d = 30; e = 2; (a * b) * c / d * e";
        //    System.out.println("Expression: " + ex);
        assertNumEquals((100d * 50d) * 20d / 30d * 2d, test(ex));
    }

    public void testMath19() {
        String ex = "a = 100; b = 500; c = 200; d = 150; e = 500; f = 800; g = 400; a-b*c*d + e*f-g";
        //     System.out.println("Expression: " + ex);
        assertNumEquals(100 - 500 * 200 * 150 + 500 * 800 - 400, test(ex));
    }

    public void testMath20() {
        String ex = "10-5*7-3*8-6";
        //   System.out.println("Expression: " + ex);
        assertNumEquals(10 - 5 * 7 - 3 * 8 - 6, test(ex));
    }

    public void testMath21() {
        String expression = "100-50*70-30*80-60";
        //   System.out.println("Expression: " + expression);
        assertNumEquals(100 - 50 * 70 - 30 * 80 - 60, test(expression));
    }

    public void testMath22() {
        String expression = "(100-50)*70-30*(20-9)**3";
        //  System.out.println("Expression: " + expression);
        assertNumEquals((int) ((100 - 50) * 70 - 30 * Math.pow(20 - 9, 3)), test(expression));
    }

    public void testMath22b() {
        String expression = "a = 100; b = 50; c = 70; d = 30; e = 20; f = 9; g = 3; (a-b)*c-d*(e-f)**g";
        //      System.out.println("Expression: " + expression);
        assertNumEquals((int) ((100 - 50) * 70 - 30 * Math.pow(20 - 9, 3)), testCompiledSimple(expression, new HashMap()));
    }


    public void testMath23() {
        String expression = "10 ** (3)*10**3";
        //       System.out.println("Expression: " + expression);
        assertNumEquals((int) (Math.pow(10, 3) * Math.pow(10, 3)), test(expression));
    }

    public void testMath24() {
        String expression = "51 * 52 * 33 / 24 / 15 + 45 * 66 * 47 * 28 + 19";
        float val = 51 * 52 * 33 / 24 / 15 + 45 * 66 * 47 * 28 + 19;
//        System.out.println("Expression: " + expression);
//        System.out.println("Expected Result: " + val);
        assertNumEquals(val, test(expression));
    }

    public void testMath25() {
        String expression = "51 * (4 - 100 * 5) + 10 + 5 * 2 / 1 + 0 + 0 - 80";
        int val = 51 * (4 - 100 * 5) + 10 + 5 * 2 / 1 + 0 + 0 - 80;
//        System.out.println("Expression: " + expression);
//        System.out.println("Expected Result: " + val);
        assertNumEquals(val, test(expression));
    }

    public void testMath26() {
        String expression = "5 + 3 * 8 * 2 ** 2";
        double val = 5 + 3 * 8 * Math.pow(2, 2);
//        System.out.println("Expression: " + expression);
//        System.out.println("Expected Result: " + val);
        Object result = test(expression);
        assertNumEquals((int) val, result);
    }

    public void testMath27() {
        String expression = "50 + 30 * 80 * 20 ** 3 * 51";
        double val = 50 + 30 * 80 * Math.pow(20, 3) * 51;
//        System.out.println("Expression: " + expression);
//        System.out.println("Expected Result: " + val);
        Object result = test(expression);
        assertNumEquals((int) val, result);
    }

    public void testMath28() {
        String expression = "50 + 30 + 80 + 11 ** 2 ** 2 * 51";
        double val = 50 + 30 + 80 + Math.pow(Math.pow(11, 2), 2) * 51;
        Object result = test(expression);

        assertNumEquals((int) val, result);
    }

    public void testMath29() {
        assertNumEquals(1 + 20 / 2 / 2, test("1 + 20 / 2 / 2"));
    }

    public void testMath30() {
        String expression = "40 / 20 + 10 + 6 / 2";
        float val = 40f / 20f + 10f + 6f / 2f;
        assertNumEquals((int) val, test(expression));
    }

    public void testMath31() {
        String expression = "40 / 20 + 5 - 4 + 8 / 2 * 2 * 6 ** 2 + 6 - 8";
        double val = 40f / 20f + 5f - 4f + 8f / 2f * 2f * Math.pow(6, 2) + 6f - 8f;
        assertNumEquals((int) val, test(expression));
    }

    public void testMath34() {
        String expression = "a+b-c*d*x/y-z+10";

        Map map = new HashMap();
        map.put("a", "200");
        map.put("b", "100");
        map.put("c", "150");
        map.put("d", "2");
        map.put("x", "400");
        map.put("y", "300");
        map.put("z", "75");

        Serializable s = MVEL.compileExpression(expression);

        assertNumEquals(200 + 100 - 150 * 2 * 400 / 300 - 75 + 10, MVEL.executeExpression(s, map));

    }

    public void testMath34_Interpreted() {
        String expression = "a+b-c*x/y-z";

        Map map = new HashMap();
        map.put("a", "200");
        map.put("b", "100");
        map.put("c", "150");
        map.put("x", "400");
        map.put("y", "300");
        map.put("z", "75");

        assertNumEquals(200 + 100 - 150 * 400 / 300 - 75, MVEL.eval(expression, map));
    }


    public void testMath35() {
        String expression = "b/x/b/b*y+a";

        Map map = new HashMap();
        map.put("a", 10);
        map.put("b", 20);
        map.put("c", 30);
        map.put("x", 40);
        map.put("y", 50);
        map.put("z", 60);

        Serializable s = MVEL.compileExpression(expression);

        assertNumEquals(20d / 40d / 20d / 20d * 50d + 10d, MVEL.executeExpression(s, map));
    }

    public void testMath35_Interpreted() {
        String expression = "b/x/b/b*y+a";

        Map map = new HashMap();
        map.put("a", 10);
        map.put("b", 20);
        map.put("c", 30);
        map.put("x", 40);
        map.put("y", 50);
        map.put("z", 60);

        //   Serializable s = MVEL.compileExpression(expression);


        assertNumEquals(20d / 40d / 20d / 20d * 50d + 10d, MVEL.eval(expression, map));
    }

    public void testMath36() {
        String expression = "b/x*z/a+x-b+x-b/z+y";

        Map map = new HashMap();
        map.put("a", 10);
        map.put("b", 20);
        map.put("c", 30);
        map.put("x", 40);
        map.put("y", 50);
        map.put("z", 60);

        Serializable s = MVEL.compileExpression(expression);

        assertNumEquals(20d / 40d * 60d / 10d + 40d - 20d + 40d - 20d / 60d + 50d, MVEL.executeExpression(s, map, Double.class));
    }


    public void testPowerOf() {
        assertNumEquals(25, test("5 ** 2"));
    }

    public void testWhileUsingImports() {
        Map<String, Object> imports = new HashMap<String, Object>();
        imports.put("ArrayList", java.util.ArrayList.class);
        imports.put("List", java.util.List.class);

        ParserContext context = new ParserContext(imports, null, "testfile");
        ExpressionCompiler compiler = new ExpressionCompiler("List list = new ArrayList(); return (list == empty)");
        assertTrue((Boolean) executeExpression(compiler.compile(context), new DefaultLocalVariableResolverFactory()));
    }

    public void testComplexExpression() {
        assertEquals("bar", test("a = 'foo'; b = 'bar'; c = 'jim'; list = {a,b,c}; list[1]"));
    }

    public void testComplexAnd() {
        assertEquals(true, test("(pi * hour) > 0 && foo.happy() == 'happyBar'"));
    }

    public void testOperatorPrecedence() {
        String ex = "_x_001 = 500.2; _x_002 = 200.8; _r_001 = 701; _r_001 == _x_001 + _x_002 || _x_001 == 500 + 0.1";
        assertEquals(true, test(ex));
    }

    public void testOperatorPrecedence2() {
        String ex = "_x_001 = 500.2; _x_002 = 200.8; _r_001 = 701; _r_001 == _x_001 + _x_002 && _x_001 == 500 + 0.2";
        assertEquals(true, test(ex));
    }

    public void testOperatorPrecedence3() {
        String ex = "_x_001 = 500.2; _x_002 = 200.9; _r_001 = 701; _r_001 == _x_001 + _x_002 && _x_001 == 500 + 0.2";
        assertEquals(false, test(ex));
    }

    public void testOperatorPrecedence4() {
        String ex = "_x_001 = 500.2; _x_002 = 200.9; _r_001 = 701; _r_001 == _x_001 + _x_002 || _x_001 == 500 + 0.2";
        assertEquals(true, test(ex));
    }

    public void testOperatorPrecedence5() {
        String ex = "_x_001 == _x_001 / 2 - _x_001 + _x_001 + _x_001 / 2 && _x_002 / 2 == _x_002 / 2";

        Map vars = new HashMap();
        vars.put("_x_001", 500.2);
        vars.put("_x_002", 200.9);
        vars.put("_r_001", 701);

        ExpressionCompiler compiler = new ExpressionCompiler(ex);
        Serializable s = compiler.compile();

        assertEquals(true, MVEL.executeExpression(s, vars));
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

    public void testNegation5() {
        assertEquals(true, test("!false"));
    }

    public void testNegation6() {
        assertEquals(false, test("!true"));
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
        assertEquals(false, (Object) eval("BWAH", new Base(), createTestMap(), Boolean.class));
    }

    public void testBooleanModeOnly4() {
        assertEquals(true, (Object) eval("hour == (hour + 0)", new Base(), createTestMap(), Boolean.class));
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

    public void testProjectionSupport3() {
        assertEquals(3, ((Collection) test("(name in things)")).size());
    }

    public void testSizeOnInlineArray() {
        assertEquals(3, test("{1,2,3}.size()"));
    }

    public void testSimpleListCreation() {
        test("['foo', 'bar', 'foobar', 'FOOBAR']");
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

        assertEquals(true, eval("this.foo == 'bar'", map, factory));
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
        assertEquals(true, executeExpression(compiled, map, factory));
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
        assertEquals(true, executeExpression(compiled, map, factory));
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
        assertEquals(String.class, executeExpression(compiled, new Base(), createTestMap()));
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

    public void testWith() {
        assertEquals("OneTwo", test("with (foo) {aValue = 'One',bValue='Two'}; foo.aValue + foo.bValue;"));
    }

    public void testWith2() {
        assertEquals("OneTwo", test(
                "with (foo) { \n" +
                        "aValue = 'One', // this is a comment \n" +
                        "bValue='Two'  // this is also a comment \n" +
                        "}; \n" +
                        "foo.aValue + foo.bValue;"));
    }

//    public void testAssertion() {
//        try {
//            test("assert false");
//            assertTrue(false);
//        }
//        catch (AssertionError error) {
//        }
//    }

//    public void testAssertion2() {
//        try {
//            test("assert true;");
//        }
//        catch (AssertionError error) {
//            assertTrue(false);
//        }
//    }

    public void testMagicArraySize() {
        assertEquals(5, test("stringArray.size()"));
    }

    public void testMagicArraySize2() {
        assertEquals(5, test("intArray.size()"));
    }

    public void testStaticVarAssignment() {
        assertEquals("1", test("String mikeBrock = 1; mikeBrock"));
    }

//    public void testIntentionalFailure() {
//        try {
//            test("int = 0"); // should fail because int is a reserved word.
//            assertTrue(false);
//        }
//        catch (Exception e) {
//        }
//    }

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
        assertEquals(20, test("java.math.BigDecimal axx = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal bxx = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal cxx = axx + bxx; return cxx; "));
    }

    public void testUnQualifiedStaticTyping() {
        assertEquals(20.0d, testCompiledSimple("import java.math.BigDecimal;  BigDecimal axx = new BigDecimal( 10.0 ); BigDecimal bxx = new BigDecimal( 10.0 ); BigDecimal cxx = axx + bxx; return cxx; ", new HashMap()));
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

//    public void testInterfaceResolution() {
//        Serializable ex = MVEL.compileExpression("foo.collectionTest.size()");
//
//        foo.setCollectionTest(new HashSet());
//        Object result1 = MVEL.executeExpression(ex, map);
//
//        foo.setCollectionTest(new ArrayList());
//        Object result2 = MVEL.executeExpression(ex, map);
//
//        assertEquals(result1, result2);
//    }


    /**
     * Start collections framework based compliance tests
     */
    public void testCreationOfSet() {
        assertEquals("foo bar foo bar",
                test("set = new java.util.LinkedHashSet(); " +
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

        assertEquals(10, MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(createTestMap())));
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

    public void testBreakpointsAcrossWith() {
        String line1 = "System.out.println( \"a1\" );\n";
        String line2 = "c = new Cheese();\n";
        String line3 = "with ( c ) { type = 'cheddar',\n" +
                "             price = 10 };\n";
        String line4 = "System.out.println( \"a1\" );\n";
        String expr = line1 + line2 + line3 + line4;

        System.out.println(expr);

        ExpressionCompiler compiler = new ExpressionCompiler(expr);
        //      compiler.setDebugSymbols(true);

        ParserContext context = new ParserContext();
        context.addImport("System", System.class);
        context.addImport("Cheese", Cheese.class);
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
        assertEquals(5, count);

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

        assertEquals(1, MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(createTestMap())));
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

        assertEquals(1, MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(createTestMap())));
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

        assertEquals("bar", MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(createTestMap())));
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

        assertEquals("bar", MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(createTestMap())));

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
        MapVariableResolverFactory mvf = new MapVariableResolverFactory(createTestMap());
        ClassImportResolverFactory classes = new ClassImportResolverFactory();
        classes.addClass(HashMap.class);

        ResolverTools.appendFactory(mvf, classes);

        Serializable compiled = compileExpression("HashMap map = new HashMap()", classes.getImportedClasses());

        assertTrue(executeExpression(compiled, mvf) instanceof HashMap);
    }

    public void testSataticClassImportViaFactory() {
        MapVariableResolverFactory mvf = new MapVariableResolverFactory(createTestMap());
        ClassImportResolverFactory classes = new ClassImportResolverFactory();
        classes.addClass(Person.class);

        ResolverTools.appendFactory(mvf, classes);

        Serializable compiled = compileExpression("p = new Person('tom'); return p.name;", classes.getImportedClasses());

        assertEquals("tom", executeExpression(compiled, mvf));
    }

    public void testSataticClassImportViaFactoryAndWithModification() {
        MapVariableResolverFactory mvf = new MapVariableResolverFactory(createTestMap());
        ClassImportResolverFactory classes = new ClassImportResolverFactory();
        classes.addClass(Person.class);

        ResolverTools.appendFactory(mvf, classes);

        Serializable compiled = compileExpression("p = new Person('tom'); p.age = 20; with( p ) { age = p.age + 1 }; return p.age;", classes.getImportedClasses());

        assertEquals(21, executeExpression(compiled, mvf));
    }

    public void testCheeseConstructor() {
        MapVariableResolverFactory mvf = new MapVariableResolverFactory(createTestMap());
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

        assertEquals("FOOBAR!", executeExpression(compiled, null, vars));
    }


    public void testMacroSupportWithDebugging() {
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

        ExpressionCompiler compiler = new ExpressionCompiler(
                parseMacros(
                        "System.out.println('hello');\n" +
                                "System.out.println('bye');\n" +
                                "modify (foo) { aValue = 'poo', \n" +
                                " aValue = 'poo' };\n mod", macros)
        );
        compiler.setDebugSymbols(true);

        ParserContext ctx = new ParserContext(null, interceptors, null);
        ctx.setSourceFile("test.mv");

        CompiledExpression compiled = compiler.compile(ctx);

        MVELRuntime.setThreadDebugger(new Debugger() {

            public int onBreak(Frame frame) {
                System.out.println(frame.getSourceName() + ":" + frame.getLineNumber());

                return Debugger.STEP;
            }
        });

        MVELRuntime.registerBreakpoint("test.mv", 3);

        System.out.println(DebugTools.decompile(compiled
        ));

        assertEquals("FOOBAR!", MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(vars)));
    }

    public void testExecuteCoercionTwice() {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("foo", new Foo());
        vars.put("$value", new Long(5));

        ExpressionCompiler compiler = new ExpressionCompiler("with (foo) { countTest = $value };");
        compiler.setDebugSymbols(true);

        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("test.mv");

        CompiledExpression compiled = compiler.compile(ctx);

        executeExpression(compiled, null, vars);

        executeExpression(compiled, null, vars);
    }

    public void testComments() {
        assertEquals(10, test("// This is a comment\n5 + 5"));
    }

    public void testComments2() {
        assertEquals(20, test("10 + 10; // This is a comment"));
    }

    public void testComments2b() {
        assertEquals(20, MVEL.eval("10 + 10; // This is a comment"));
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

    public void testStrictTypingCompilation4() throws NoSuchMethodException {
        ParserContext ctx = new ParserContext();

        ctx.setStrictTypeEnforcement(true);
        ctx.addImport(Foo.class);

        ExpressionCompiler compiler =
                new ExpressionCompiler("x_a = new Foo()");

        compiler.compile(ctx);


        assertEquals(Foo.class, ctx.getVariables().get("x_a"));
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

        assertTrue((Boolean) test("dt1 < dt2"));
    }

    public void testDynamicDeop() {
        Serializable s = MVEL.compileExpression("name");

        assertEquals("dog", executeExpression(s, new Foo()));
        assertEquals("dog", executeExpression(s, new Foo().getBar()));
    }

    public void testVirtProperty() {
        Map<String, Object> testMap = new HashMap<String, Object>();
        testMap.put("test", "foo");

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("mp", testMap);

        assertEquals("bar", executeExpression(compileExpression("mp.test = 'bar'; mp.test"), vars));
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


        assertEquals("foo", testCompiledSimple("innermap['test']", outermap));
    }

    public void testMapBindingSemantics() {
        Map<String, Object> outermap = new HashMap<String, Object>();
        Map<String, Object> innermap = new HashMap<String, Object>();

        innermap.put("test", "foo");
        outermap.put("innermap", innermap);

        MVEL.setProperty(outermap, "innermap['test']", "bar");

        assertEquals("bar", testCompiledSimple("innermap['test']", outermap));
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

        assertEquals("bar", testCompiledSimple("innermap['test']", outermap));
    }

    public void testDynamicImports() {
        ParserContext ctx = new ParserContext();
        ctx.addPackageImport("java.util");

        ExpressionCompiler compiler = new ExpressionCompiler("HashMap");
        Serializable s = compiler.compile(ctx);

        assertEquals(HashMap.class, executeExpression(s));

        compiler = new ExpressionCompiler("map = new HashMap(); map.size()");
        s = compiler.compile(ctx);

        assertEquals(0, executeExpression(s, new DefaultLocalVariableResolverFactory()));
    }

    public void testDynamicImports2() {
        assertTrue(test("import java.util.*; map = new HashMap(); map") instanceof HashMap);
    }

    public void testDynamicImportsWithIdentifierSameAsClassWithDiffCase() {
        ParserContext ctx = new ParserContext();
        ctx.addPackageImport("org.mvel.tests.main.res");
        ctx.setStrictTypeEnforcement(false);

        ExpressionCompiler compiler = new ExpressionCompiler("bar.add(\"hello\")");
        Serializable s = compiler.compile(ctx);
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

        List list = (List) executeExpression(expr, createTestMap());

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

        assertEquals("foobar", executeExpression(c));
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

        assertEquals("N", eval("new org.mvel.tests.main.res.PDFFieldUtil().calculateAge(EV_VI_ANT1.GEBDAT) >= 25 ? 'Y' : 'N'"
                , objectMap));
        assertEquals("N", testCompiledSimple("new org.mvel.tests.main.res.PDFFieldUtil().calculateAge(EV_VI_ANT1.GEBDAT) >= 25 ? 'Y' : 'N'"
                , objectMap));
    }

    /**
     * Provided by: Alex Roytman
     */

    public void testMethodResolutionWithNullParameter() {
        Context ctx = new Context();
        ctx.setBean(new Bean());
        Map<String, Object> vars = new HashMap<String, Object>();
        System.out.println("bean.today: " + eval("bean.today", ctx, vars));
        System.out.println("formatDate(bean.today): " + eval("formatDate(bean.today)", ctx, vars));
        //calling method with string param with null parameter works
        System.out.println("formatString(bean.nullString): " + eval("formatString(bean.nullString)", ctx, vars));
        System.out.println("bean.myDate = bean.nullDate: " + eval("bean.myDate = bean.nullDate; return bean.nullDate;", ctx, vars));
        //calling method with Date param with null parameter fails
        System.out.println("formatDate(bean.myDate): " + eval("formatDate(bean.myDate)", ctx, vars));
        //same here
        System.out.println(eval("formatDate(bean.nullDate)", ctx, vars));
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
        assertEquals(1, eval("org.mvel.tests.main.AbstractTest$Message.GOODBYE", new HashMap()));
    }

    public void testStaticNestedWithImport() {
        String expr = "Message.GOODBYE;\n";

        ExpressionCompiler compiler = new ExpressionCompiler(expr);

        ParserContext context = new ParserContext();
        context.setStrictTypeEnforcement(false);

        context.addImport("Message", Message.class);
        Serializable compiledExpression = compiler.compile(context);

        assertEquals(1, executeExpression(compiledExpression));
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
        Message msg = (Message) executeExpression(compiledExpression, vars);
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
        executeExpression(compiledExpression, vars);
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
        assertEquals("foobar", eval("import java.util.LinkedHashMap; LinkedHashMap map = new LinkedHashMap();" +
                " map.put('a', 'foo'); map.put('b', 'bar'); s = ''; foreach (key : map.keySet()) { System.out.println(map[key]); s += map[key]; }; return s;", new HashMap()));

        assertEquals("foobar", testCompiledSimple("import java.util.LinkedHashMap; LinkedHashMap map = new LinkedHashMap();" +
                " map.put('a', 'foo'); map.put('b', 'bar'); s = ''; foreach (key : map.keySet()) { System.out.println(map[key]); s += map[key]; }; return s;", new HashMap()));
    }

    public void testLateResolveOfClass() {
        ExpressionCompiler compiler = new ExpressionCompiler("System.out.println(new Foo());");
        ParserContext ctx = new ParserContext();
        ctx.addImport(Foo.class);

        CompiledExpression s = compiler.compile(ctx);
        compiler.removeParserContext();

        System.out.println(executeExpression(s));
    }

    public void testClassAliasing() {
        assertEquals("foobar", test("Foo = String; new Foo('foobar')"));
    }

    public void testRandomExpression1() {
        assertEquals("HelloWorld", test("if ((x15 = foo.bar) == foo.bar && x15 == foo.bar) { return 'HelloWorld'; } else { return 'GoodbyeWorld' } "));
    }

    public void testRandomExpression2() {
        assertEquals(11, test("counterX = 0; foreach (item:{1,2,3,4,5,6,7,8,9,10}) { counterX++; }; return counterX + 1;"));
    }

    public void testRandomExpression3() {
        assertEquals(0, test("counterX = 10; foreach (item:{1,1,1,1,1,1,1,1,1,1}) { counterX -= item; } return counterX;"));
    }

    public void testRandomExpression4() {
        assertEquals(true, test("result = org.mvel.MVEL.eval('10 * 3'); result == (10 * 3);"));
    }

    public void testRandomExpression5() {
        assertEquals(true, test("FooClassRef = foo.getClass(); fooInst = new FooClassRef(); name = org.mvel.MVEL.eval('name', fooInst); return name == 'dog'"));
    }

    public void testRandomExpression6() {
        assertEquals(500, test("exprString = '250' + ' ' + '*' + ' ' + '2'; compiledExpr = org.mvel.MVEL.compileExpression(exprString);" +
                " return org.mvel.MVEL.executeExpression(compiledExpr);"));
    }

    public void testRandomExpression7() {
        assertEquals("FOOBAR", test("'foobar'.toUpperCase();"));
    }

    public void testRandomExpression8() {
        assertEquals(true, test("'someString'.intern(); 'someString'.hashCode() == 'someString'.hashCode();"));
    }

    public void testRandomExpression9() {
        assertEquals(false, test("_abc = 'someString'.hashCode(); _xyz = _abc + 1; _abc == _xyz"));
    }

    public void testRandomExpression10() {
        assertEquals(false, test("(_abc = (_xyz = 'someString'.hashCode()) + 1); _abc == _xyz"));
    }


    /**
     * Submitted by: Guerry Semones
     */
    private Map<Object, Object> outerMap;
    private Map<Object, Object> innerMap;


    public void testAddIntToMapWithMapSyntax() throws Throwable {
        outerMap = new HashMap<Object, Object>();
        innerMap = new HashMap<Object, Object>();
        outerMap.put("innerMap", innerMap);

        // fails because mvel checks for 'foo' in the outerMap,
        // rather than inside innerMap in outerMap
        PropertyAccessor.set(outerMap, "innerMap['foo']", 42);

        // mvel set it here
//        assertEquals(42, outerMap.get("foo"));

        // instead of here
        assertEquals(42, innerMap.get("foo"));
    }

    public void testUpdateIntInMapWithMapSyntax() throws Throwable {

        outerMap = new HashMap<Object, Object>();
        innerMap = new HashMap<Object, Object>();
        outerMap.put("innerMap", innerMap);

        // fails because mvel checks for 'foo' in the outerMap,
        // rather than inside innerMap in outerMap
        innerMap.put("foo", 21);
        PropertyAccessor.set(outerMap, "innerMap['foo']", 42);

        // instead of updating it here
        assertEquals(42, innerMap.get("foo"));
    }


    private HashMap<String, Object> context = new HashMap<String, Object>();


    public void before() {
        HashMap<String, Object> map = new HashMap<String, Object>();

        MyBean bean = new MyBean();
        bean.setVar(4);

        map.put("bean", bean);
        context.put("map", map);
    }


    public void testDeepProperty() {

        before();
        Serializable compiled = MVEL.compileExpression("map.bean.var");

        Object obj = executeExpression(compiled, context);
        assertEquals(4, obj);
    }


    public void testDeepProperty2() {
        before();

        Serializable compiled = MVEL.compileExpression("map.bean.getVar()");

        Object obj = executeExpression(compiled, context);
        assertEquals(4, obj);
    }


    public class MyBean {
        int var;

        public int getVar() {
            return var;
        }

        public void setVar(int var) {
            this.var = var;
        }
    }


    public static class TargetClass {
        private short _targetValue = 5;

        public short getTargetValue() {
            return _targetValue;
        }
    }

    public void testNestedMethodCall() {
        List elements = new ArrayList();
        elements.add(new TargetClass());
        Map variableMap = new HashMap();
        variableMap.put("elements", elements);
        eval(
                "results = new java.util.ArrayList(); foreach (element : elements) { if( {5} contains element.targetValue.intValue()) { results.add(element); } }; results",
                variableMap);
    }


    public void testBooleanEvaluation() {
        assertEquals(true, test("true||false||false"));
    }

    public void testBooleanEvaluation2() {
        assertEquals(true, test("equalityCheck(1,1)||fun||ackbar"));
    }

    /**
     * Submitted by: Dimitar Dimitrov
     */
    public void testFailing() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("os", "windows");
        assertTrue((Boolean) eval("os ~= 'windows|unix'", map));
    }

    public void testSuccess() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("os", "windows");
        assertTrue((Boolean) eval("'windows' ~= 'windows|unix'", map));
        assertFalse((Boolean) eval("time ~= 'windows|unix'", new java.util.Date()));
    }


    public void testBooleanStrAppend() {
        assertEquals("footrue", test("\"foo\" + true"));
    }

    public void testAssignPlus() {
        assertNumEquals(10, test("xx0 = 5; xx0 += 4; xx0 + 1"));
    }

    public void testAssignDiv() {
        assertNumEquals(2, test("xx0 = 20; xx0 /= 10; xx0"));
    }

    public void testAssignMult() {
        assertEquals(36, test("xx0 = 6; xx0 *= 6; xx0"));
    }

    public void testAssignSub() {
        assertEquals(11, test("xx0 = 15; xx0 -= 4; xx0"));
    }


    public void testFail() {

        Map map = new HashMap();
        map.put("a", new JButton());
        map.put("b", new JButton());
        new JButton().setToolTipText("");
        System.out.println(testCompiledSimple(
                "if (a.text!=null) {\n" +
                        "    b.text = a.text;\n" +
                        "} else if (a.toolTipText!=null) { \n" +
                        "    b.text = a.toolTipText;\n" +
                        "} " +
                        "return b;"
                , map
        ));
    }

    public void testPass() {
        Map map = new HashMap();
        map.put("a", new JButton());
        map.put("b", new JButton());
        new JButton().setToolTipText("");


        System.out.println(testCompiledSimple(
                "if (a.text!=null) {\n" +
                        "    b.text = a.text;\n" +
                        "} " +
                        "if (a.text!=null && a.toolTipText!=null) { \n" +
                        "    b.text = a.toolTipText;\n" +
                        "}" +
                        "return b;"
                , map
        ));


        System.out.println(testCompiledSimple(
                "if (a.text!=null) {\n" +
                        "    b.text = a.text;\n" +
                        "} else if (a.text!=null && a.toolTipText!=null) { \n" +
                        "    b.text = a.toolTipText;\n" +
                        "}"
                , map
        ));
    }

//    public void testCommentsInWith() {
//        HashMap map = new HashMap();
//        map.put("f", new JFrame());
//        System.out.println(eval(
//                "with (f) {\n" +
//                        "title = 'blah', // setting title\n" +
//                        "alwaysOnTop = false \n" +
//                        "}", map
//        ));
//    }


    public void testStaticWithExplicitParam() {
        PojoStatic pojo = new PojoStatic("10");
        eval("org.mvel.tests.main.res.AStatic.Process('10')", pojo, new HashMap());
    }

    public void testSimpleExpression() {
        PojoStatic pojo = new PojoStatic("10");
        eval("value!= null", pojo, new HashMap());
    }

    public void testStaticWithExpressionParam() {
        PojoStatic pojo = new PojoStatic("10");
        assertEquals("java.lang.String", eval("org.mvel.tests.main.res.AStatic.Process(value.getClass().getName().toString())", pojo));
    }


    public void testStringIndex() {
        assertEquals(true, test("a = 'foobar'; a[4] == 'a'"));
    }

    public void testArrayConstructionSupport1() {
        assertTrue(test("new String[5]") instanceof String[]);
    }

    public void testArrayConstructionSupport2() {
        assertTrue((Boolean) test("xStr = new String[5]; xStr.size() == 5"));
    }

    public void testArrayConstructionSupport3() {
        assertEquals("foo", test("xStr = new String[5][5]; xStr[4][0] = 'foo'; xStr[4][0]"));
    }

    public void testArrayConstructionSupport4() {
        assertEquals(10, test("xStr = new String[5][10]; xStr[4][0] = 'foo'; xStr[4].length"));
    }


    /**
     * MVEL-57 (Submitted by: Rognvald Eaversen) -- Slightly modified by cbrock to include a positive testcase.
     */
    public void testMethodInvocationWithCollectionElement() {
        context = new HashMap();
        context.put("pojo", new POJO());
        context.put("number", "1192800637980");

        Object result = MVEL.eval("pojo.function(pojo.dates[0].time)", context);
        assertEquals(String.valueOf(((POJO) context.get("pojo")).getDates().iterator().next().getTime()), result);
    }

    public void testNestedWithInList() {
        Recipient recipient1 = new Recipient();
        recipient1.setName("userName1");
        recipient1.setEmail("user1@domain.com");

        Recipient recipient2 = new Recipient();
        recipient2.setName("userName2");
        recipient2.setEmail("user2@domain.com");

        List list = new ArrayList();
        list.add(recipient1);
        list.add(recipient2);

        String text =
                "array = [" +
                        "(with ( new Recipient() ) {name = 'userName1', email = 'user1@domain.com' })," +
                        "(with ( new Recipient() ) {name = 'userName2', email = 'user2@domain.com' })];\n";

        ParserContext context = new ParserContext();
        context.addImport(Recipient.class);

        ExpressionCompiler compiler = new ExpressionCompiler(text);
        Serializable execution = compiler.compile(context);
        List result = (List) executeExpression(execution);
        assertEquals(list, result);
    }

    public void testNestedWithInMethod() {
        Recipient recipient1 = new Recipient();
        recipient1.setName("userName1");
        recipient1.setEmail("user1@domain.com");

        Recipients recipients = new Recipients();
        recipients.addRecipient(recipient1);

        String text =
                "recipients = new Recipients();\n" +
                        "recipients.addRecipient( (with ( new Recipient() ) {name = 'userName1', email = 'user1@domain.com' }) );\n" +
                        "return recipients;\n";

        ParserContext context = new ParserContext();
        context = new ParserContext();
        context.addImport(Recipient.class);
        context.addImport(Recipients.class);

        ExpressionCompiler compiler = new ExpressionCompiler(text);
        Serializable execution = compiler.compile(context);
        Recipients result = (Recipients) executeExpression(execution);
        assertEquals(recipients, result);
    }

    public void testNestedWithInComplexGraph() {
        Recipients recipients = new Recipients();

        Recipient recipient1 = new Recipient();
        recipient1.setName("user1");
        recipient1.setEmail("user1@domain.com");
        recipients.addRecipient(recipient1);

        Recipient recipient2 = new Recipient();
        recipient2.setName("user2");
        recipient2.setEmail("user2@domain.com");
        recipients.addRecipient(recipient2);

        EmailMessage msg = new EmailMessage();
        msg.setRecipients(recipients);
        msg.setFrom("from@domain.com");

        String text = "(with ( new EmailMessage() ) { recipients = (with (new Recipients()) { recipients = [(with ( new Recipient() ) {name = 'user1', email = 'user1@domain.com'}), (with ( new Recipient() ) {name = 'user2', email = 'user2@domain.com'}) ] }), " +
                " from = 'from@domain.com' } )";
        ParserContext context = new ParserContext();
        context = new ParserContext();
        context.addImport(Recipient.class);
        context.addImport(Recipients.class);
        context.addImport(EmailMessage.class);

        ExpressionCompiler compiler = new ExpressionCompiler(text);
        Serializable execution = compiler.compile(context);
        EmailMessage result = (EmailMessage) executeExpression(execution);
        assertEquals(msg, result);

    }

    public static class Recipient {
        private String name;
        private String email;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((email == null) ? 0 : email.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Recipient other = (Recipient) obj;
            if (email == null) {
                if (other.email != null) return false;
            }
            else if (!email.equals(other.email)) return false;
            if (name == null) {
                if (other.name != null) return false;
            }
            else if (!name.equals(other.name)) return false;
            return true;
        }


    }

    public static class Recipients {
        private List<Recipient> list = Collections.EMPTY_LIST;

        public void setRecipients(List<Recipient> recipients) {
            this.list = recipients;
        }

        public boolean addRecipient(Recipient recipient) {
            if (list == Collections.EMPTY_LIST) {
                this.list = new ArrayList<Recipient>();
            }

            if (!this.list.contains(recipient)) {
                this.list.add(recipient);
                return true;
            }
            return false;
        }

        public boolean removeRecipient(Recipient recipient) {
            return this.list.remove(recipient);
        }

        public List<Recipient> getRecipients() {
            return this.list;
        }

        public Recipient[] toArray() {
            return (Recipient[]) list.toArray(new Recipient[list.size()]);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((list == null) ? 0 : list.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Recipients other = (Recipients) obj;
            if (list == null) {
                if (other.list != null) return false;
            }
            else if (!list.equals(other.list)) return false;
            return true;
        }


    }

    public static class EmailMessage {
        private Recipients recipients;
        private String from;

        public EmailMessage() {

        }

        public Recipients getRecipients() {
            return recipients;
        }

        public void setRecipients(Recipients recipients) {
            this.recipients = recipients;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((from == null) ? 0 : from.hashCode());
            result = prime * result + ((recipients == null) ? 0 : recipients.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {

            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final EmailMessage other = (EmailMessage) obj;
            if (from == null) {
                if (other.from != null) return false;
            }
            else if (!from.equals(other.from)) return false;
            if (recipients == null) {
                if (other.recipients != null) return false;
            }
            else if (!recipients.equals(other.recipients)) return false;
            return true;
        }


    }


    public class POJO {
        private Set<Date> dates = new HashSet<Date>();

        public POJO() {
            dates.add(new Date());
        }

        public Set<Date> getDates() {
            return dates;
        }

        public void setDates(Set<Date> dates) {
            this.dates = dates;
        }

        public String function(long num) {
            return String.valueOf(num);
        }
    }

    public void testSubEvaluation() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("EV_BER_BER_NR", "12345");
        map.put("EV_BER_BER_PRIV", Boolean.FALSE);

        assertEquals("12345", eval("EV_BER_BER_NR + ((EV_BER_BER_PRIV != empty && EV_BER_BER_PRIV == true) ? \"/PRIVAT\" : '')", map));

        map.put("EV_BER_BER_PRIV", Boolean.TRUE);
        assertEquals("12345/PRIVAT", testCompiledSimple("EV_BER_BER_NR + ((EV_BER_BER_PRIV != empty && EV_BER_BER_PRIV == true) ? \"/PRIVAT\" : '')", map));
    }

    public void testNestedMethod1() {
        Vector vectorA = new Vector();
        Vector vectorB = new Vector();

        vectorA.add("Foo");

        Map map = new HashMap();
        map.put("vecA", vectorA);
        map.put("vecB", vectorB);

        testCompiledSimple("vecB.add(vecA.remove(0)); vecA.add('Foo');", map);

        assertEquals("Foo", vectorB.get(0));
    }

    public void testNegativeArraySizeBug() throws Exception {
        String expressionString1 = "results = new java.util.ArrayList(); foreach (element : elements) { if( ( {30, 214, 158, 31, 95, 223, 213, 86, 159, 34, 32, 96, 224, 160, 85, 201, 29, 157, 100, 146, 82, 203, 194, 145, 140, 81, 27, 166, 212, 38, 28, 94, 168, 23, 87, 150, 35, 149, 193, 33, 132, 206, 93, 196, 24, 88, 195, 36, 26, 154, 167, 108, 204, 74, 46, 25, 153, 202, 79, 207, 143, 43, 16, 80, 198, 208, 144, 41, 97, 142, 83, 18, 162, 103, 155, 98, 44, 17, 205, 77, 156, 141, 165, 102, 84, 37, 101, 222, 40, 104, 99, 177, 182, 22, 180, 21, 137, 221, 179, 78, 42, 178, 19, 183, 139, 218, 219, 39, 220, 20, 184, 217, 138, 62, 190, 171, 123, 113, 59, 118, 225, 124, 169, 60, 117, 1} contains element.attribute ) ) { results.add(element); } }; results";
        String expressionString2 = "results = new java.util.ArrayList(); foreach (element : elements) { if( ( {30, 214, 158, 31, 95, 223, 213, 86, 159, 34, 32, 96, 224, 160, 85, 201, 29, 157, 100, 146, 82, 203, 194, 145, 140, 81, 27, 166, 212, 38, 28, 94, 168, 23, 87, 150, 35, 149, 193, 33, 132, 206, 93, 196, 24, 88, 195, 36, 26, 154, 167, 108, 204, 74, 46, 25, 153, 202, 79, 207, 143, 43, 16, 80, 198, 208, 144, 41, 97, 142, 83, 18, 162, 103, 155, 98, 44, 17, 205, 77, 156, 141, 165, 102, 84, 37, 101, 222, 40, 104, 99, 177, 182, 22, 180, 21, 137, 221, 179, 78, 42, 178, 19, 183, 139, 218, 219, 39, 220, 20, 184, 217, 138, 62, 190, 171, 123, 113, 59, 118, 225, 124, 169, 60, 117, 1, 61, 189, 122, 68, 58, 119, 63, 226, 3, 172} contains element.attribute ) ) { results.add(element); } }; results";

        List<Target> targets = new ArrayList<Target>();
        targets.add(new Target(1));
        targets.add(new Target(999));

        Map vars = new HashMap();
        vars.put("elements", targets);

        assertEquals(1, ((List) eval(expressionString1, vars)).size());
        assertEquals(1, ((List) eval(expressionString2, vars)).size());

        assertEquals(1, ((List) testCompiledSimple(expressionString1, vars)).size());
        assertEquals(1, ((List) testCompiledSimple(expressionString2, vars)).size());

    }

    public static final class Target {
        private int _attribute;

        public Target(int attribute_) {
            _attribute = attribute_;
        }

        public int getAttribute() {
            return _attribute;
        }
    }

    public void testSetExpressions1() {
        Map<String, Object> myMap = new HashMap<String, Object>();

        final Serializable fooExpr = MVEL.compileSetExpression("foo");
        MVEL.executeSetExpression(fooExpr, myMap, "blah");
        assertEquals("blah", myMap.get("foo"));

        MVEL.executeSetExpression(fooExpr, myMap, "baz");
        assertEquals("baz", myMap.get("foo"));
    }

    public void testInlineCollectionNestedObjectCreation() {
        Map m = (Map) test("['Person.age' : [1, 2, 3, 4], 'Person.rating' : ['High', 'Low']," +
                " 'Person.something' : (new String('foo').toUpperCase())]");

        assertEquals("FOO", m.get("Person.something"));
    }

    public void testEgressType() {
        ExpressionCompiler compiler = new ExpressionCompiler("( $cheese )");
        ParserContext context = new ParserContext();
        context.addInput("$cheese", Cheese.class);

        ExecutableStatement expr = (ExecutableStatement) compiler.compile(context);

        assertEquals(Cheese.class, expr.getKnownEgressType());

    }

    public void testDuplicateVariableDeclaration() {
        ExpressionCompiler compiler = new ExpressionCompiler("String x = \"abc\"; Integer x = new Integer( 10 );");
        ParserContext context = new ParserContext();

        try {
            ExecutableStatement expr = (ExecutableStatement) compiler.compile(context);
            fail("Compilation must fail with duplicate variable declaration exception.");
        }
        catch (CompileException ce) {
            // success
        }
    }

    public void testThreadSafetyInterpreter1() {

        //First evaluation
        System.out.println("First evaluation: " + MVEL.eval("true"));

        new Thread(new Runnable() {
            public void run() {
                // Second evaluation - this succeeds only if the first evaluation is not commented out
                System.out.println("Second evaluation: " + MVEL.eval("true"));
            }
        }).start();
    }

    public void testGraphNavigation() {
        test("System.out.println(submap['foo'].name)");
    }

    public void testDynamicImportsOnNestedExpressions() {
        ParserContext ctx = new ParserContext();
        ctx.addPackageImport("org.mvel.tests.main.res");


        ExpressionCompiler compiler = new ExpressionCompiler("new Person(\"bobbo\", new Cheese(\"cheddar\", 15))");
        Serializable s = compiler.compile(ctx);

        org.mvel.tests.main.res.Person p1 = new org.mvel.tests.main.res.Person("bobbo", new Cheese("cheddar", 15));

        org.mvel.tests.main.res.Person p2 = (org.mvel.tests.main.res.Person) executeExpression(s, new DefaultLocalVariableResolverFactory());

        assertEquals(p1, p2);
    }

    public void testDynamicImportsWithNullConstructorParam() {
        ParserContext ctx = new ParserContext();
        ctx.addPackageImport("org.mvel.tests.main.res");


        ExpressionCompiler compiler = new ExpressionCompiler("new Person(\"bobbo\", null)");
        Serializable s = compiler.compile(ctx);

        org.mvel.tests.main.res.Person p1 = new org.mvel.tests.main.res.Person("bobbo", null);

        org.mvel.tests.main.res.Person p2 = (org.mvel.tests.main.res.Person) executeExpression(s, new DefaultLocalVariableResolverFactory());

        assertEquals(p1, p2);
    }

    public void testCompileMatches() {
        ExpressionCompiler compiler = new ExpressionCompiler("String source = \"abc\"; String pat = \"abc\"; source ~= pat ");
        Serializable s = compiler.compile();
        Boolean result = (Boolean) executeExpression(s, new DefaultLocalVariableResolverFactory());

        assertEquals(Boolean.TRUE, result);
    }

    public void testCompileMatches2() {
        ExpressionCompiler compiler = new ExpressionCompiler("source = \"abc\"; pat = \"abc\"; source ~= pat ");
        Serializable s = compiler.compile();
        Boolean result = (Boolean) executeExpression(s, new DefaultLocalVariableResolverFactory());

        assertEquals(Boolean.TRUE, result);
    }


    /**
     * @param expression
     * @return
     */
    private CompiledExpression compileSimpleExpression(String expression) {
        ExpressionCompiler compiler = new ExpressionCompiler(expression);
        CompiledExpression expr = compiler.compile();
        return expr;
    }

    public void testStringEquals() {
        assertEquals(true, test("ipaddr == '10.1.1.2'"));
    }

    public void testArrayList() throws SecurityException, NoSuchMethodException {
        Collection<String> collection = new ArrayList<String>();
        collection.add("I CAN HAS CHEEZBURGER");
        assertEquals(collection.size(), MVEL.eval("size()", collection));
    }

    public void testUnmodifiableCollection() throws SecurityException, NoSuchMethodException {
        Collection<String> collection = new ArrayList<String>();
        collection.add("I CAN HAS CHEEZBURGER");
        collection = Collections.unmodifiableCollection(collection);
        assertEquals(collection.size(), MVEL.eval("size()", collection));
    }

    public void testSingleton() throws SecurityException, NoSuchMethodException {
        Collection<String> collection = Collections.singleton("I CAN HAS CHEEZBURGER");
        assertEquals(collection.size(), MVEL.eval("size()", collection));
    }

    public void testCharComparison() {
        assertEquals(true, test("'z' > 'a'"));
    }

    public void testRegExMatch() {
        assertEquals(true, MVEL.eval("$test = 'foo'; $ex = 'f.*'; $test ~= $ex", new HashMap()));
    }

    public static class TestClass2 {
        public void addEqualAuthorizationConstraint(Foo leg, Bar ctrlClass, Integer authorization) {
        }
    }

    public void testJIRA93() {
        Map testMap = createTestMap();
        testMap.put("testClass2", new TestClass2());

        Serializable s = MVEL.compileExpression("testClass2.addEqualAuthorizationConstraint(foo, foo.bar, 5)");

        for (int i = 0; i < 3; i++) {
            MVEL.executeExpression(s, testMap);
        }
    }

    public void testJIRA96() {
        ParserContext ctx = new ParserContext();
        ctx.setStrictTypeEnforcement(true);

        ctx.addInput("fooString", String[].class);

        ExpressionCompiler compiler = new ExpressionCompiler("fooString[0].toUpperCase()");
        compiler.compile(ctx);
    }

    public void testParserErrorHandling() {
        try {
            final ParserContext ctx = new ParserContext();
            ExpressionCompiler compiler = new ExpressionCompiler("a[");
            compiler.compile(ctx);
        }
        catch (Exception e) {
            return;
        }
        assertTrue(false);
    }

    public void testJIRA99() {
        Map map = new HashMap();
        map.put("x", 20);
        map.put("y", 10);
        map.put("z", 5);

        assertEquals(20 - 10 - 5, testCompiledSimple("x - y - z", map));
    }

    public void testJIRA100() {
        assertEquals(20, test("java.math.BigDecimal axx = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal bxx = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal cxx = axx + bxx; return cxx; "));
    }

    public void testJIRA100a() {
        assertEquals(233.23, test("java.math.BigDecimal axx = new java.math.BigDecimal( 109.45 ); java.math.BigDecimal bxx = new java.math.BigDecimal( 123.78 ); java.math.BigDecimal cxx = axx + bxx; return cxx; "));
    }

    public void testJIRA100b() {
        String expression = "(8 / 10) * 100 <= 80;";

        assertEquals((8 / 10) * 100 <= 80, testCompiledSimple(expression, new HashMap()));
    }

    public void testJIRA100c() {
        String expression = "(9 / 10) * 100 >= 90;";

        assertEquals((8 / 10) * 100 <= 80, testCompiledSimple(expression, new HashMap()));
    }

    public void testJIRA92() {
        test("'stringValue' > null");
    }

    public void testAssignToBean() {
        Person person = new Person();
        MVEL.eval("this.name = 'foo'", person);

        assertEquals("foo", person.getName());

        Serializable s = MVEL.compileExpression("this.name = 'bar'");

        MVEL.executeExpression(s, person);

        assertEquals("bar", person.getName());
    }

    public void testMapAssignmentNestedExpression() {
        Map map = new HashMap();
        map.put("map", new HashMap());

        String ex = "map[java.lang.Integer.MAX_VALUE] = 'bar'; map[java.lang.Integer.MAX_VALUE];";

        Serializable s = MVEL.compileExpression(ex);

        assertEquals("bar", MVEL.executeExpression(s, map));
        assertEquals("bar", MVEL.eval(ex, map));
    }

    public void testMapAssignmentNestedExpression2() {
        Map map = new HashMap();
        map.put("x", "bar");
        map.put("map", new HashMap());

        String ex = "map[x] = 'foo'; map['bar'];";
        Serializable s = MVEL.compileExpression(ex);
        assertEquals("foo", MVEL.executeExpression(s, map));
        assertEquals("foo", MVEL.eval(ex, map));
    }

    /**
     * MVEL-103
     */
    public static class MvelContext {
        public boolean singleCalled;
        public boolean arrayCalled;

        public void methodForTest(String string) {
            System.out.println("sigle param method called!");
            singleCalled = true;
        }

        public void methodForTest(String[] strings) {
            System.out.println("array param method called!");
            arrayCalled = true;
        }
    }

    public void testMethodResolutionOrder() {
        MvelContext mvelContext = new MvelContext();
        MVEL.eval("methodForTest({'1','2'})", mvelContext);
        MVEL.eval("methodForTest('1')", mvelContext);

        assertTrue(mvelContext.arrayCalled && mvelContext.singleCalled);
    }

    public void testOKQuoteComment() throws Exception {
        // ' in comments outside of blocks seem OK
        MVEL.compileExpression("// ' this is OK!");
        MVEL.compileExpression("// ' this is OK!\n");
        MVEL.compileExpression("// ' this is OK!\nif(1==1) {};");
    }

    public void testOKDblQuoteComment() throws Exception {
        // " in comments outside of blocks seem OK
        MVEL.compileExpression("// \" this is OK!");
        MVEL.compileExpression("// \" this is OK!\n");
        MVEL.compileExpression("// \" this is OK!\nif(1==1) {};");
    }

    public void testIfComment() throws Exception {
        // No quote?  OK!
        MVEL.compileExpression("if(1 == 1) {\n" +
                "  // Quote & Double-quote seem to break this expression\n" +
                "}");
    }

    public void testIfQuoteCommentBug() throws Exception {
        // Comments in an if seem to fail if they contain a '
        MVEL.compileExpression("if(1 == 1) {\n" +
                "  // ' seems to break this expression\n" +
                "}");
    }

    public void testIfDblQuoteCommentBug() throws Exception {
        // Comments in a foreach seem to fail if they contain a '
        MVEL.compileExpression("if(1 == 1) {\n" +
                "  // ' seems to break this expression\n" +
                "}");
    }

    public void testForEachQuoteCommentBug() throws Exception {
        // Comments in a foreach seem to fail if they contain a '
        MVEL.compileExpression("foreach ( item : 10 ) {\n" +
                "  // The ' character causes issues\n" +
                "}");
    }

    public void testForEachDblQuoteCommentBug() throws Exception {
        // Comments in a foreach seem to fail if they contain a '
        MVEL.compileExpression("foreach ( item : 10 ) {\n" +
                "  // The \" character causes issues\n" +
                "}");
    }

    public void testForEachCommentOK() throws Exception {
        // No quote?  OK!
        MVEL.compileExpression("foreach ( item : 10 ) {\n" +
                "  // The quote & double quote characters cause issues\n" +
                "}");
    }

    public void testElseIfCommentBugPreCompiled() throws Exception {
        // Comments can't appear before else if() - compilation works, but evaluation fails
        MVEL.executeExpression((Object) MVEL.compileExpression("// This is never true\n" +
                "if (1==0) {\n" +
                "  // Never reached\n" +
                "}\n" +
                "// This is always true...\n" +
                "else if (1==1) {" +
                "  System.out.println('Got here!');" +
                "}\n"));
    }

    public void testElseIfCommentBugEvaluated() throws Exception {
        // Comments can't appear before else if()
        MVEL.eval("// This is never true\n" +
                "if (1==0) {\n" +
                "  // Never reached\n" +
                "}\n" +
                "// This is always true...\n" +
                "else if (1==1) {" +
                "  System.out.println('Got here!');" +
                "}\n");
    }


    public void testRegExpOK() throws Exception {
        // This works OK intepreted
        assertEquals(Boolean.TRUE, MVEL.eval("'Hello'.toUpperCase() ~= '[A-Z]{0,5}'"));
        assertEquals(Boolean.TRUE, MVEL.eval("1 == 0 || ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')"));
        // This works OK if toUpperCase() is avoided in pre-compiled
        Object ser = MVEL.compileExpression("'Hello' ~= '[a-zA-Z]{0,5}'");
        assertEquals(Boolean.TRUE, MVEL.executeExpression(ser));
    }

    public void testRegExpPreCompiledBug() throws Exception {
        // If toUpperCase() is used in the expression then this fails; returns null not
        // a boolean.
        Object ser = MVEL.compileExpression("'Hello'.toUpperCase() ~= '[a-zA-Z]{0,5}'");
        assertEquals(Boolean.TRUE, MVEL.executeExpression(ser));
    }

    public void testRegExpOrBug() throws Exception {
        // This fails during execution due to returning null, I think...
        Object ser = MVEL.compileExpression("1 == 0 || ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')");
        assertEquals(Boolean.TRUE, MVEL.executeExpression(ser));
    }

    public void testRegExpAndBug() throws Exception {
        // This also fails due to returning null, I think...
        Object ser = MVEL.compileExpression("1 == 1 && ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')");
        assertEquals(Boolean.TRUE, MVEL.executeExpression(ser));
    }

    public void testSetAccessorOverloadedEqualsStrictMode() {
        ParserContext ctx = new ParserContext();
        ctx.setStrictTypeEnforcement(true);
        ctx.addInput("foo", Foo.class);

        try {
            CompiledExpression expr = new ExpressionCompiler("foo.bar = 0").compile(ctx);
        }
        catch (CompileException e) {
            // should fail.

            e.printStackTrace();
            return;
        }

        assertTrue(false);
    }

    public void testAnalysisCompile() {
        CompiledExpression ce = new ExpressionCompiler("foo.aValue = 'bar'").compile();
        assertTrue(ce.getParserContext().getInputs().keySet().contains("foo"));
    }

    public interface FooHolder {
        public Foo getFoo();
    }

    public class FooHolderImpl {
        private Foo foo = new Foo();

        public Foo getFoo() {
            return foo;
        }
    }

    public class DerivedFooHolderImpl {
        private Foo foo = new Foo();

        public Foo getFoo() {
            return foo;
        }
    }

    public void testJIRA111() {
        OptimizerFactory.setDefaultOptimizer("reflective");
        Serializable compiled = MVEL.compileExpression("foo.bar.name");

        Object obj = executeExpression(compiled, new FooHolderImpl());
        assertEquals("dog", obj);

        obj = executeExpression(compiled, new DerivedFooHolderImpl());
        assertEquals("dog", obj);
    }

    public static double minim(double[] tab) {
        double min = Float.MAX_VALUE;
        for (int i = 0; i < tab.length; i++) {
            if (min > tab[i]) {
                min = tab[i];
            }
        }
        return min;
    }

    public void testJIRA113() {
        assertEquals(true, test("org.mvel.tests.main.CoreConfidenceTests.minim( {456.2, 2.3} ) == 2.3"));
    }

    public void testJIRA115() {
   //     OptimizerFactory.setDefaultOptimizer("ASM");
        String exp = "results = new java.util.ArrayList(); foreach (element : elements) { if( {1,32769,32767} contains element ) { results.add(element) }  }; results";
        Map map = new HashMap();
        map.put("elements", new int[]{1, 32769, 32767});
        ArrayList result = (ArrayList) MVEL.eval(exp, map);

        assertEquals(3, result.size());
    }

}





