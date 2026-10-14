/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.compat.mvel3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvel3.Base;
import org.mvel3.Evaluator;
import org.mvel3.Foo;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.PDFFieldUtil;
import org.mvel3.transpiler.context.Declaration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porting of ControlFlowTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations applied to the enabled tests:
 * <ul>
 *   <li><b>single-quote-string</b>: string literals use double quotes.</li>
 *   <li><b>block-syntax</b>: loop variables and locals are declared with a type, statements end
 *       with {@code ;}, and the result is returned explicitly with {@code return}. An {@code if}
 *       chain gets a final {@code return}.</li>
 *   <li><b>until-statement</b>: {@code until (c)} becomes {@code while (!(c))}.</li>
 *   <li><b>string-coercion</b>: {@code pi#double#} instead of the String {@code pi}.</li>
 *   <li><b>this-reference</b>: methods and fields of the MVEL2 root object are reached through
 *       the input variable {@code base}.</li>
 *   <li>A non-boolean operand of {@code ||} / {@code &&} (for example
 *       {@code foo.toUC("test")}) is made boolean with {@code .isEmpty()}.</li>
 * </ul>
 *
 * <p>MVEL3 has no interpreter, so the "interpreted vs compiled" pairs of the original run the
 * same path twice and are collapsed to a single evaluation.
 *
 * <p>Tests are {@code @Disabled("... not representable")} when the point of the original is a
 * syntax with no MVEL3 equivalent ({@code isdef}, function declarations).
 */
public class ControlFlowAdaptedTest {

    private static final String NOT_REPRESENTABLE_FUNCTION = "function-declaration: not representable";

    public static class TargetClass {
        private final short targetValue = 5;

        public short getTargetValue() {
            return targetValue;
        }
    }

    private static final Set<String> IMPORTS = Set.of(
            Foo.class.getCanonicalName(),
            Base.class.getCanonicalName(),
            TargetClass.class.getCanonicalName(),
            PDFFieldUtil.class.getCanonicalName(),
            "java.util.List",
            "java.util.ArrayList");

    private static Map<String, Object> vars() {
        final Map<String, Object> map = new HashMap<>();
        final Base base = new Base();
        map.put("base", base);
        map.put("foo", new Foo());
        map.put("c", "cat");
        map.put("pi", "3.14");
        map.put("hour", 60);
        map.put("zero", 0);
        map.put("fun", base.fun);
        return map;
    }

    private static Object run(final String expression) {
        return new MVEL().executeExpression(expression, IMPORTS, vars());
    }

    private static Object run(final String expression, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, IMPORTS, vars);
    }

    @Test
    public void testSimpleIfStatement() {
        assertThat(run("if (true) { System.out.println(\"test!\"); } return null;")).isNull();
    }

    @Test
    public void testAnd() {
        assertThat(run("c != null && foo.bar.name == \"dog\" && foo.bar.woof")).isEqualTo(true);
    }

    @Test
    public void testAnd2() {
        assertThat(run("c!=null&&foo.bar.name==\"dog\"&&foo.bar.woof")).isEqualTo(true);
    }

    @Test
    public void testComplexAnd() {
        assertThat(run("(pi#double# * hour) > 0 && foo.happy() == \"happyBar\"")).isEqualTo(true);
    }

    @Test
    public void testShortPathExpression() {
        // `foo.toUC(..)` returns a String; `.isEmpty()` makes the right operand boolean. The right
        // operand must not be evaluated because `3 > 4` is false, so foo.register stays null.
        assertThat(run("var r = 3 > 4 && foo.toUC(\"test\").isEmpty(); return foo.register;")).isNull();
    }

    @Test
    public void testShortPathExpression2() {
        assertThat(run("4 > 3 || foo.toUC(\"test\").isEmpty()")).isEqualTo(true);
    }

    @Test
    public void testShortPathExpression4() {
        assertThat(run("4>3||foo.toUC(\"test\").isEmpty()")).isEqualTo(true);
    }

    @Test
    public void testOr() {
        assertThat(run("fun || true")).isEqualTo(true);
    }

    @Test
    public void testOrOperator() {
        assertThat(run("true||true")).isEqualTo(true);
    }

    @Test
    public void testOrOperator2() {
        assertThat(run("2 > 3 || 3 > 2")).isEqualTo(true);
    }

    @Test
    public void testOrOperator3() {
        assertThat(run("pi#double# > 5 || pi#double# > 6 || pi#double# > 3")).isEqualTo(true);
    }

    @Test
    public void testShortPathExpression3() {
        // The root-object field `defnull` is declared as a local String.
        assertThat(run("String defnull = null; return defnull != null && defnull.length() > 0;")).isEqualTo(false);
    }

    @Test
    public void testMultiStatement() {
        // populate() / barfoo belong to the MVEL2 root object; here they are reached through `base`.
        assertThat(run("base.populate(); return base.barfoo == \"sarah\";")).isEqualTo(true);
    }

    @Test
    public void testTernary() {
        assertThat(run("zero==0?\"foobie\":zero")).isEqualTo("foobie");
    }

    @Test
    public void testTernary2() {
        assertThat(run("zero==1?\"foobie\":\"blimpie\"")).isEqualTo("blimpie");
    }

    @Test
    public void testTernary3() {
        assertThat(run("zero==1?\"foobie\":\"foobie\"+\"barbie\"")).isEqualTo("foobiebarbie");
    }

    // Original: isdef someWierdVar ? 'squid' : 'skat!';
    // The `isdef` operator is not supported by MVEL3.
    @Test
    @Disabled("isdef-operator: not representable")
    public void testTernary5() {
    }

    @Test
    public void testEmptyIf() {
        assertThat(run("var a = 5; if (a == 5) { } return a;")).isEqualTo(5);
    }

    @Test
    public void testEmptyIf2() {
        assertThat(run("var a=5;if(a==5){}return a;")).isEqualTo(5);
    }

    @Test
    public void testIf() {
        assertThat(run("if (5 > 4) { return 10; } else { return 5; }")).isEqualTo(10);
    }

    @Test
    public void testIf2() {
        assertThat(run("if (5 < 4) { return 5; } else { return 10; }")).isEqualTo(10);
    }

    @Test
    public void testIf3() {
        assertThat(run("if(5<4){return 5;}else{return 10;}")).isEqualTo(10);
    }

    @Test
    public void testIfAndElse() {
        assertThat(run("if (false) { return false; } else { return true; }")).isEqualTo(true);
    }

    @Test
    public void testIfAndElseif() {
        assertThat(run("if (false) { return false; } else if(100 < 50) { return false; } "
                       + "else if (10 > 5) { return true; } return false;")).isEqualTo(true);
    }

    @Test
    public void testIfAndElseif2() {
        assertThat(run("if (false) { return false; } else if(100 < 50) { return false; } "
                       + "else if (10 > 5) { return true; } return false;")).isEqualTo(true);
    }

    @Test
    public void testIfAndElseif3() {
        assertThat(run("if (false) { return false; } else if(100 < 50) { return false; } "
                       + "else if (10 > 5) { return true; } return false;")).isEqualTo(true);
    }

    @Test
    public void testIfAndElseIfCondensedGrammar() {
        assertThat(run("if (false) return \"Bar\"; else return \"Foo244\";")).isEqualTo("Foo244");
    }

    @Test
    public void testTernary4() {
        assertThat(run("true ? \"<test>\" : \"<poo>\"")).isEqualTo("<test>");
    }

    @Test
    public void testPrecedenceOrder1() {
        assertThat(run("50 > 60 && 20 < 10 || 100 > 90")).isEqualTo(true);
    }

    @Test
    public void testDoLoop() {
        assertThat(run("int i = 0; do { i++; } while (i != 10); return i;")).isEqualTo(10);
    }

    @Test
    public void testDoLoop2() {
        // do { .. } until (c)  ->  do { .. } while (!(c))
        assertThat(run("int i=100;do{i--;}while(!(i==50)); return i;")).isEqualTo(50);
    }

    @Test
    public void testForLoop() {
        assertThat(run("String str = \"\"; for(int i=0;i<6;i++) { str += i; } return str;")).isEqualTo("012345");
    }

    @Test
    public void testForLoop2() {
        assertThat(run("String str=\"\";for(int i=0;i<6;i++){str+=i;}return str;")).isEqualTo("012345");
    }

    @Test
    public void testUntilLoop() {
        // until (i == 6) { .. }  ->  while (!(i == 6)) { .. }
        assertThat(run("String str = \"\"; int i = 0; while (!(i == 6)) { str += i++; } return str;"))
                .isEqualTo("012345");
    }

    @Test
    public void testQualifiedForLoop() {
        final List<Foo> l = new ArrayList<>();
        l.add(new Foo());
        l.add(new Foo());
        l.add(new Foo());

        final Map<String, Object> vars = new HashMap<>();
        vars.put("l", l);

        // `l` is declared as List<Foo> so that the typed for-each variable resolves.
        final Evaluator<Map<String, Object>, Void, Object> evaluator = MVEL
                .map(new Declaration<>("l", Type.type(List.class, "<org.mvel3.Foo>")))
                .out(Type.OBJECT)
                .block("String s = \"\"; for (Foo f : l) { s += f.name; } return s;")
                .imports(IMPORTS)
                .compile();

        assertThat(evaluator.eval(vars)).isEqualTo("dogdogdog");
    }

    @Test
    public void testForLoopWithVar() {
        assertThat(run("int height = 100; int j = 0; for (int i = 0; i < height; i++) { j++; } return j;"))
                .isEqualTo(100);
    }

    // The original loops 10^11 times (`i < 100000000000`, an int-literal overflow in Java).
    // The bound is reduced; the test still checks that an empty loop body compiles and runs.
    @Test
    public void testEmptyLoopSemantics() {
        assertThat(run("for (int i = 0; i < 100000; i++) { } return null;")).isNull();
    }

    @Test
    public void testLoopWithEscape() {
        // The original reads x back from the variable map; MVEL3 locals are not written back,
        // so the value is returned instead.
        assertThat(run("int x = 0; for (; x < 10000; x++) {} return x;")).isEqualTo(10000);
    }

    // Original: foreach (element : elements) { if ({5} contains element.targetValue.intValue()) ... }
    // `foreach`, the inline array literal and the `contains` operator are replaced by a typed
    // for loop and `==`. The original has no assertion; the result is checked here.
    @Test
    public void testNestedMethodCall() {
        final List<TargetClass> elements = new ArrayList<>();
        elements.add(new TargetClass());

        final Map<String, Object> vars = new HashMap<>();
        vars.put("elements", elements);

        final Evaluator<Map<String, Object>, Void, Object> evaluator = MVEL
                .map(new Declaration<>("elements",
                                       Type.type(List.class, "<" + TargetClass.class.getCanonicalName() + ">")))
                .out(Type.OBJECT)
                .block("List<TargetClass> results = new ArrayList<>(); "
                       + "for (TargetClass element : elements) { "
                       + "if (element.targetValue == 5) { results.add(element); } } return results;")
                .imports(IMPORTS)
                .compile();

        assertThat((List<?>) evaluator.eval(vars)).hasSize(1);
    }

    @Test
    public void testStaticallyTypedItemInForEach() {
        assertThat(run("java.lang.StringBuffer sbuf = new java.lang.StringBuffer(); "
                       + "for (int i : new int[] { 1,2,3,4 }) { sbuf.append(i); } return sbuf.toString();"))
                .isEqualTo("1234");
    }

    // Original: foreach (element : elements) { if ({1,32769,32767} contains element ) ... }
    // The inline array and `contains` are replaced by explicit comparisons.
    @Test
    public void testJIRA115() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("elements", new int[]{1, 32769, 32767});

        final Object result = run("List<Integer> results = new ArrayList<>(); "
                                  + "for (int element : elements) { "
                                  + "if (element == 1 || element == 32769 || element == 32767) { results.add(element); } } "
                                  + "return results;", vars);

        assertThat((List<?>) result).hasSize(3);
    }

    @Test
    public void testStringWithTernaryIf() {
        assertThat(run("System.out.print(\"Hello : \" + (foo != null ? \"FOO!\" : \"NO FOO\") + \". Bye.\"); return null;"))
                .isNull();
    }

    @Test
    public void testJIRA124() {
        // MVEL3 has no interpreter, so the original "interpreted equals compiled" check is a single evaluation.
        assertThat(ternary("testValue == true ? \"A\" :  \"B\" + \"C\"", true)).isEqualTo("A");
        assertThat(ternary("testValue ? \"A\" +  \"B\" : \"C\"", true)).isEqualTo("AB");
        assertThat(ternary("(testValue ? \"A\" :  \"B\" + \"C\")", true)).isEqualTo("A");
        assertThat(ternary("(testValue ? \"A\" +  \"B\" : \"C\")", true)).isEqualTo("AB");
        assertThat(ternary("(testValue ? \"A\" :  (\"B\" + \"C\"))", true)).isEqualTo("A");
        assertThat(ternary("(testValue ? (\"A\" + \"B\") : \"C\")", true)).isEqualTo("AB");

        assertThat(ternary("testValue ? \"A\" :  \"B\" + \"C\"", false)).isEqualTo("BC");
        assertThat(ternary("testValue ? \"A\" +  \"B\" : \"C\"", false)).isEqualTo("C");
        assertThat(ternary("(testValue ? \"A\" :  \"B\" + \"C\")", false)).isEqualTo("BC");
        assertThat(ternary("(testValue ? \"A\" +  \"B\" : \"C\")", false)).isEqualTo("C");
        assertThat(ternary("(testValue ? \"A\" :  (\"B\" + \"C\"))", false)).isEqualTo("BC");
        assertThat(ternary("(testValue ? (\"A\" + \"B\") : \"C\")", false)).isEqualTo("C");
    }

    private static Object ternary(final String expression, final boolean testValue) {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("testValue", testValue);
        return run(expression, vars);
    }

    @Test
    public void testCalculateAge() {
        // calculate age of a person who was born in 1999 jan 10, on 2023 dec 25
        final Calendar c1 = Calendar.getInstance();
        c1.set(1999, 0, 10);
        final Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("GEBDAT", c1.getTime());
        final Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("EV_VI_ANT1", propertyMap);

        // The Map value is typed Object, so a #java.util.Date# hint is needed (dynamic-property-type).
        assertThat(run("new PDFFieldUtil().calculateAge(EV_VI_ANT1.GEBDAT#java.util.Date#) >= 25 ? \"Y\" : \"N\"",
                       objectMap)).isEqualTo("N");
    }

    @Test
    public void testSubEvaluation() {
        // `!= empty` is dropped (empty-operator); a null Boolean would not be a valid input here anyway.
        final Map<String, Object> map = new HashMap<>();
        map.put("EV_BER_BER_NR", "12345");
        map.put("EV_BER_BER_PRIV", Boolean.FALSE);

        final String expression = "EV_BER_BER_NR + ((EV_BER_BER_PRIV == true) ? \"/PRIVAT\" : \"\")";
        assertThat(run(expression, map)).isEqualTo("12345");

        map.put("EV_BER_BER_PRIV", Boolean.TRUE);
        assertThat(run(expression, map)).isEqualTo("12345/PRIVAT");
    }

    @Test
    public void testCompactIfElse() {
        assertThat(run("if (false) { return \"bar\"; } else { return \"foo\"; }")).isEqualTo("foo");
    }

    @Test
    public void testForInitializerScope() {
        assertThat(run("for (int i = 0; i < 10; i++) { String s = \"fop\"; }\n"
                       + "for (int i = 0; i < 10; i++) { String s = \"foo\"; }\n"
                       + "return null;")).isNull();
    }

    @Test
    public void testForEachTerminateFlow() {
        assertThat(run("for(int i=0;i<5;i++) {\n"
                       + "System.out.println(\"LOOP\" + i);\n"
                       + "return true;\n"
                       + "}\n"
                       + "System.out.println(\"END\");\n"
                       + "return false;")).isEqualTo(true);
    }

    // Original: def test() { for(i = 0; i < 3; i++) { System.out.println('...') } } test()
    // Without the function declaration the test would only check a plain for loop; not representable.
    @Test
    @Disabled(NOT_REPRESENTABLE_FUNCTION)
    public void testFunctionCall() {
    }

    // Original: def cond(x, y) { if (x ~= "fet.*") { .. return [1: ((y > 12) ? 1 : 2), 2: (12 + 1)]; } (null).print(); } cond('fetlock', 12)
    // Function declaration, `~=` and `(null).print()`; not representable.
    @Test
    @Disabled(NOT_REPRESENTABLE_FUNCTION)
    public void testMultipleArgumentsInFunction() {
    }

    // Original: recursive `def insert(i, ls)` using `empty`, `is` and list concatenation.
    @Test
    @Disabled(NOT_REPRESENTABLE_FUNCTION)
    public void testDhanji1() {
    }

    @Test
    public void testDhanji2() {
        // [x,y] + [3,4] (list-concat-operator) -> addAll on a mutable list (list literals are immutable)
        assertThat(run("var x = 1; var y = 2; List<Integer> l = new ArrayList<>(); l.add(x); l.add(y); l.addAll([3, 4]); return l;"))
                .isEqualTo(Arrays.asList(1, 2, 3, 4));
    }
}
