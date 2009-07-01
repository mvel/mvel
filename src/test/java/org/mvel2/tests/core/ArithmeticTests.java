package org.mvel2.tests.core;

import org.mvel2.MVEL;
import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeExpression;
import org.mvel2.ParserContext;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExpressionCompiler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ArithmeticTests extends AbstractTest {
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
        int val = (int) ((100d % 3d) * 2d - 1d / 1d + 8d + (5d * 2d));
        assertEquals(val, test("(100 % 3) * 2 - 1 / 1 + 8 + (5 * 2)"));
    }

    public void testMath4a() {
        String expression = "(100 % 90) * 20 - 15 / 16 + 80 + (50 * 21)";
        System.out.println("Expression: " + expression);
        assertEquals(((100d % 90d) * 20d - 15d / 16d + 80d + (50d * 21d)), MVEL.eval(expression));
    }

    public void testMath5() {
        assertEquals(300.5 / 5.3 / 2.1 / 1.5, test("300.5 / 5.3 / 2.1 / 1.5"));
    }

    public void testMath5a() {
        String expression = "300.5 / 5.3 / 2.1 / 1.5";
        System.out.println("Expression: " + expression);
        assertEquals(300.5 / 5.3 / 2.1 / 1.5, MVEL.eval(expression));
    }

    public void testMath6() {
        int val = (300 * 5 + 1) + 100 / 2 * 2;
        assertEquals(val, test("(300 * five + 1) + (100 / 2 * 2)"));
    }

    public void testMath7() {
        int val = (int) ((100d % 3d) * 2d - 1d / 1d + 8d + (5d * 2d));
        assertEquals(val, test("(100 % 3) * 2 - 1 / 1 + 8 + (5 * 2)"));
    }

    public void testMath8() {
        double val = 5d * (100.56d * 30.1d);
        assertEquals(val, test("5 * (100.56 * 30.1)"));
    }

    public void testPowerOf() {
        assertEquals(25, test("5 ** 2"));
    }

    public void testSignOperator() {
        assertEquals(-15, test("int x = 15; -x"));
    }


    public void testMath14() {
        assertEquals(10 - 5 * 2 + 5 * 8 - 4, test("10-5*2 + 5*8-4"));
    }

    public void testMath15() {
        String ex = "100-500*200 + 500*800-400";
        assertEquals(100 - 500 * 200 + 500 * 800 - 400, test(ex));
    }

    public void testMath16() {
        String ex = "100-500*200*150 + 500*800-400";
        assertEquals(100 - 500 * 200 * 150 + 500 * 800 - 400, test(ex));
    }

    public void testMath17() {
        String ex = "(100d * 50d) * 20d / 30d * 2d";
        Object o = test(ex);
        assertEquals((100d * 50d) * 20d / 30d * 2d, o);
    }

    public void testMath18() {
        String ex = "a = 100d; b = 50d; c = 20d; d = 30d; e = 2d; (a * b) * c / d * e";
        System.out.println("Expression: " + ex);
        assertEquals((100d * 50d) * 20d / 30d * 2d, testCompiledSimple(ex, new HashMap()));
    }

    public void testMath19() {
        String ex = "a = 100; b = 500; c = 200; d = 150; e = 500; f = 800; g = 400; a-b*c*d + e*f-g";
        System.out.println("Expression: " + ex);
        assertEquals(100 - 500 * 200 * 150 + 500 * 800 - 400, testCompiledSimple(ex, new HashMap()));
    }

    public void testMath32() {
        String ex = "x = 20; y = 10; z = 5; x-y-z";
        System.out.println("Expression: " + ex);
        assertEquals(20 - 10 - 5, testCompiledSimple(ex, new HashMap()));
    }

    public void testMath33() {
        String ex = "x = 20; y = 2; z = 2; x/y/z";
        System.out.println("Expression: " + ex);
        assertEquals(20 / 2 / 2, testCompiledSimple(ex, new HashMap()));
    }

    public void testMath20() {
        String ex = "10-5*7-3*8-6";
        System.out.println("Expression: " + ex);
        assertEquals(10 - 5 * 7 - 3 * 8 - 6, test(ex));
    }

    public void testMath21() {
        String expression = "100-50*70-30*80-60";
        System.out.println("Expression: " + expression);
        assertEquals(100 - 50 * 70 - 30 * 80 - 60, test(expression));
    }

    public void testMath22() {
        String expression = "(100-50)*70-30*(20-9)**3";
        System.out.println("Expression: " + expression);
        assertEquals((int) ((100 - 50) * 70 - 30 * Math.pow(20 - 9, 3)), test(expression));
    }

    public void testMath22b() {
        String expression = "a = 100; b = 50; c = 70; d = 30; e = 20; f = 9; g = 3; (a-b)*c-d*(e-f)**g";
        System.out.println("Expression: " + expression);
        assertEquals((int) ((100 - 50) * 70 - 30 * Math.pow(20 - 9, 3)), testCompiledSimple(expression, new HashMap()));
    }

    public void testMath23() {
        String expression = "10 ** (3)*10**3";
        System.out.println("Expression: " + expression);
        assertEquals((int) (Math.pow(10, 3) * Math.pow(10, 3)), test(expression));
    }

    public void testMath24() {
        String expression = "51 * 52 * 33 / 24 / 15 + 45 * 66 * 47 * 28 + 19";
        double val = 51d * 52d * 33d / 24d / 15d + 45d * 66d * 47d * 28d + 19d;
        System.out.println("Expression: " + expression);
        System.out.println("Expected Result: " + val);

        assertEquals(val, test(expression));
    }

    public void testMath25() {
        String expression = "51 * (40 - 1000 * 50) + 100 + 50 * 20 / 10 + 11 + 12 - 80";
        int val = 51 * (40 - 1000 * 50) + 100 + 50 * 20 / 10 + 11 + 12 - 80;
        System.out.println("Expression: " + expression);
        System.out.println("Expected Result: " + val);
        assertEquals(val, test(expression));
    }

    public void testMath26() {
        String expression = "5 + 3 * 8 * 2 ** 2";
        int val = (int) (5d + 3d * 8d * Math.pow(2, 2));
        System.out.println("Expression: " + expression);
        System.out.println("Expected Result: " + val);
        Object result = test(expression);
        assertEquals(val, result);
    }

    public void testMath27() {
        String expression = "50 + 30 * 80 * 20 ** 3 * 51";
        double val = 50 + 30 * 80 * Math.pow(20, 3) * 51;
        System.out.println("Expression: " + expression);
        System.out.println("Expected Result: " + val);
        Object result = test(expression);
        assertEquals((int) val, result);
    }

    public void testMath28() {
        String expression = "50 + 30 + 80 + 11 ** 2 ** 2 * 51";
        double val = 50 + 30 + 80 + Math.pow(Math.pow(11, 2), 2) * 51;
        Object result = test(expression);

        assertEquals((int) val, result);
    }

    public void testMath29() {
        String expression = "10 + 20 / 4 / 4";
        System.out.println("Expression: " + expression);
        double val = 10d + 20d / 4d / 4d;

        assertEquals(val, MVEL.eval(expression));
    }

    public void testMath30() {
        String expression = "40 / 20 + 10 + 60 / 21";
        System.out.println("Expression: " + expression);
        double val = 40d / 20d + 10d + 60d / 21d;
        assertEquals(val, MVEL.eval(expression));
    }

    public void testMath31() {
        String expression = "40 / 20 + 5 - 4 + 8 / 2 * 2 * 6 ** 2 + 6 - 8";
        double val = 40f / 20f + 5f - 4f + 8f / 2f * 2f * Math.pow(6, 2) + 6f - 8f;
        assertEquals((int) val, MVEL.eval(expression));
    }

    public void testMath34() {
        String expression = "a+b-c*d*x/y-z+10";

        Map map = new HashMap();
        map.put("a", 200);
        map.put("b", 100);
        map.put("c", 150);
        map.put("d", 2);
        map.put("x", 400);
        map.put("y", 300);
        map.put("z", 75);

        Serializable s = compileExpression(expression);

        assertEquals(200 + 100 - 150 * 2 * 400 / 300 - 75 + 10, executeExpression(s, map));
    }

    public void testMath34_Interpreted() {
        String expression = "a+b-c*x/y-z";

        Map map = new HashMap();
        map.put("a", 200);
        map.put("b", 100);
        map.put("c", 150);
        map.put("x", 400);
        map.put("y", 300);
        map.put("z", 75);

        assertEquals(200 + 100 - 150 * 400 / 300 - 75, MVEL.eval(expression, map));
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

        assertNumEquals(20d / 40d / 20d / 20d * 50d + 10d, executeExpression(compileExpression(expression), map));
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

        Serializable s = compileExpression(expression);

        assertNumEquals(20d / 40d * 60d / 10d + 40d - 20d + 40d - 20d / 60d + 50d, executeExpression(s, map));
    }

    public void testMath37() {
        String expression = "x+a*a*c/x*b*z+x/y-b";

        Map map = new HashMap();
        map.put("a", 10);
        map.put("b", 20);
        map.put("c", 30);
        map.put("x", 2);
        map.put("y", 2);
        map.put("z", 60);

        Serializable s = compileExpression(expression);

        assertNumEquals(2d + 10d * 10d * 30d / 2d * 20d * 60d + 2d / 2d - 20d, executeExpression(s, map));
    }

    public void testMath38() {
        String expression = "100 + 200 - 300 + 400 - 500 + 105 / 205 - 405 + 305 * 206";
        double res = 100d + 200d - 300d + 400d - 500d + 105d / 205d - 405d + 305d * 206d;

        System.out.println("Expression: " + expression);
        System.out.println("CorrectResult:" + res);
        assertEquals(res,
                MVEL.eval(expression));
    }

    public void testMath39() {
        String expression = "147 + 60 / 167 % 448 + 36 * 23 / 166";
        double res = 147d + 60d / 167d % 448d + 36d * 23d / 166d;

        System.out.println("Expression: " + expression);
        System.out.println("CorrectRes: " + res);

        assertEquals(res, MVEL.eval(expression));
    }

    public void testMath40() {
        String expression = "228 - 338 % 375 - 103 + 260 + 412 * 177 + 121";
        double res = 228d - 338d % 375d - 103d + 260d + 412d * 177d + 121d;

        System.out.println("Expression: " + expression);
        System.out.println("CorrectRes: " + res);

        assertEquals(res, MVEL.eval(expression, Double.class));
    }

    public void testMath41() {
        String expression = "304d - 246d / 242d % 235d / 425d - 326d + 355d * 264d % 308d";
        double res = 304d - 246d / 242d % 235d / 425d - 326d + 355d * 264d % 308d;

        System.out.println("Expression: " + expression);
        System.out.println("CorrectRes: " + res);

        assertEquals(res, MVEL.eval(expression));
    }

    public void testMath42() {
        String expression = "11d - 7d / 3d * 18d % 14d * 8d * 11d - 2d - 11d / 13d + 14d";
        double res = 11d - 7d / 3d * 18d % 14d * 8d * 11d - 2d - 11d / 13d + 14d;

        System.out.println("Expression: " + expression);
        System.out.println("CorrectRes: " + res);

        assertEquals(res, MVEL.eval(expression));
    }

    public void testMath43() {
        String expression = "4d/3d*6d%8d*5d*8d+7d+9d*1d";
        double res = 4d / 3d * 6d % 8d * 5d * 8d + 7d + 9d * 1d;

        System.out.println("Expression: " + expression);
        System.out.println("CorrectRes: " + res);

        assertEquals(res, MVEL.eval(expression));
    }

    public void testMath44() {
        String expression = "6d+8d/9d*1d*9d*10d%4d*4d-4d*6d*3d";
        double res = 6d + 8d / 9d * 1d * 9d * 10d % 4d * 4d - 4d * 6d * 3d;

        System.out.println("Expression: " + expression);
        System.out.println("CorrectRes: " + res);

        assertEquals(res, MVEL.eval(expression));
    }

    public void testMath44b() {
        String expression = "a+b/c*d*e*f%g*h-i*j*k";
        double res = 6d + 8d / 9d * 1d * 9d * 10d % 4d * 4d - 4d * 6d * 3d;

        Serializable s = compileExpression(expression);

        Map vars = new HashMap();
        vars.put("a", 6d);
        vars.put("b", 8d);
        vars.put("c", 9d);
        vars.put("d", 1d);
        vars.put("e", 9d);
        vars.put("f", 10d);
        vars.put("g", 4d);
        vars.put("h", 4d);
        vars.put("i", 4d);
        vars.put("j", 6d);
        vars.put("k", 3d);

        assertEquals(res, executeExpression(s, vars));
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
        assertEquals(true, executeExpression(compiler.compile(), vars));
    }

    public void testModulus() {
        assertEquals(38392 % 2, test("38392 % 2"));
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

    public void testBitwiseOr4() {
        assertEquals(2 | 5, test("2|five"));
    }

    public void testBitwiseAnd1() {
        assertEquals(2, test("2 & 3"));
    }

    public void testBitwiseAnd2() {
        assertEquals(5 & 3, test("five & 3"));
    }

    public void testShiftLeft() {
        assertEquals(4, test("2 << 1"));
    }

    public void testShiftLeft2() {
        assertEquals(5 << 1, test("five << 1"));
    }

    public void testUnsignedShiftLeft() {
        assertEquals(2, test("-2 <<< 0"));
    }

    public void testShiftRight() {
        assertEquals(128, test("256 >> 1"));
    }

    public void testShiftRight2() {
        assertEquals(5 >> 1, test("five >> 1"));
    }

    public void testUnsignedShiftRight() {
        assertEquals(-5 >>> 1, test("-5 >>> 1"));
    }

    public void testUnsignedShiftRight2() {
        assertEquals(-5 >>> 1, test("(five - 10) >>> 1"));
    }

    public void testShiftRightAssign() {
        assertEquals(5 >> 2, test("_zZz = 5; _zZz >>= 2"));
    }

    public void testShiftLeftAssign() {
        assertEquals(10 << 2, test("_yYy = 10; _yYy <<= 2"));
    }

    public void testUnsignedShiftRightAssign() {
        assertEquals(-5 >>> 2, test("_xXx = -5; _xXx >>>= 2"));
    }

    public void testXOR() {
        assertEquals(3, test("1 ^ 2"));
    }

    public void testXOR2() {
        assertEquals(5 ^ 2, test("five ^ 2"));
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

    public void testDeepPropertyAdd() {
        assertEquals(10, test("foo.countTest+ 10"));
    }

    public void testDeepAssignmentIncrement() {
        assertEquals(true, test("foo.countTest += 5; if (foo.countTest == 5) { foo.countTest = 0; return true; } else { foo.countTest = 0; return false; }"));
    }

    public void testDeepAssignmentWithBlock() {
        assertEquals(true, test("with (foo) { countTest += 5 }; if (foo.countTest == 5) { foo.countTest = 0; return true; } else { foo.countTest = 0; return false; }"));
    }

    public void testOperativeAssignMod() {
        int val = 5;
        assertEquals(val %= 2, test("int val = 5; val %= 2; val"));
    }

    public void testOperativeAssignDiv() {
        int val = 10;
        assertEquals(val /= 2, test("int val = 10; val /= 2; val"));
    }

    public void testOperativeAssignShift1() {
        int val = 5;
        assertEquals(val <<= 2, test("int val = 5; val <<= 2; val"));
    }

    public void testOperativeAssignShift2() {
        int val = 5;
        assertEquals(val >>= 2, test("int val = 5; val >>= 2; val"));
    }

    public void testOperativeAssignShift3() {
        int val = -5;
        assertEquals(val >>>= 2, test("int val = -5; val >>>= 2; val"));
    }

    public void testAssignPlus() {
        assertEquals(10, test("xx0 = 5; xx0 += 4; xx0 + 1"));
    }

    public void testAssignPlus2() {
        assertEquals(10, test("xx0 = 5; xx0 =+ 4; xx0 + 1"));
    }

    public void testAssignDiv() {
        assertEquals(2.0, test("xx0 = 20; xx0 /= 10; xx0"));
    }

    public void testAssignMult() {
        assertEquals(36, test("xx0 = 6; xx0 *= 6; xx0"));
    }

    public void testAssignSub() {
        assertEquals(11, test("xx0 = 15; xx0 -= 4; xx0"));
    }

    public void testAssignSub2() {
        assertEquals(-95, test("xx0 = 5; xx0 =- 100"));
    }

    public void testBooleanStrAppend() {
        assertEquals("footrue", test("\"foo\" + true"));
    }

    public void testStringAppend() {
        assertEquals("catbar", test("c + 'bar'"));
    }

    public void testNegation() {
        assertEquals(1, test("-(-1)"));
    }

    public void testStrongTypingModeComparison() {
        ParserContext parserContext = new ParserContext();
        parserContext.setStrongTyping(true);
        parserContext.addInput("a", Long.class);

        CompiledExpression compiledExpression = new ExpressionCompiler("a==0").compile(parserContext);
        HashMap<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", 0l);
        MVEL.executeExpression(compiledExpression, variables);
    }

    public void testJIRA158() {
        Serializable s = MVEL.compileExpression("4/2 + Math.sin(1)");

        assertEquals(4 / 2 + Math.sin(1), MVEL.executeExpression(s));

        s = MVEL.compileExpression("(float) (4/2 + Math.sin(1))", ParserContext.create().stronglyTyped());

        assertEquals((float) (4 / 2 + Math.sin(1)), MVEL.executeExpression(s));
    }


    public void testJIRA162() {
        Serializable s = MVEL.compileExpression("1d - 2d + (3d * var1) * var1", ParserContext.create().withInput("var1", double.class));
        Map vars = new HashMap();
        vars.put("var1", 1d);

        assertEquals((1 - 2 + (3 * 1d) * 1), MVEL.executeExpression(s, vars));
    }

    public void testJIRA161() {
        Serializable s = MVEL.compileExpression("1==(-1)", ParserContext.create().stronglyTyped());
        assertEquals(false, MVEL.executeExpression(s));
    }

    public void testJIRA163() {
        Serializable s = MVEL.compileExpression("1d - 2d + (3d * 4d) * var1", ParserContext.create().withInput("var1", double.class));
        Map vars = new HashMap();
        vars.put("var1", 1d);

        assertEquals((1 - 2 + (3 * 4) * 1d), MVEL.executeExpression(s, vars));
    }
}
