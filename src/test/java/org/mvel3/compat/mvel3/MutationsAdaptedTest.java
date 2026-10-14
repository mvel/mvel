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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.Base;
import org.mvel3.Foo;
import org.mvel3.MVEL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porting of MutationsTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>block-syntax</b>: undeclared variables ({@code x = 1}) are declared with
 *       {@code var}, and the result is returned explicitly with {@code return}.</li>
 *   <li><b>single-quote-string</b>: string literals use double quotes.</li>
 *   <li><b>property-assign-void-setter</b>: {@code foo.bar.age = 21} becomes a {@code void}
 *       setter call, so it is written as a statement followed by {@code return}.</li>
 *   <li><b>this-reference</b>: {@code this} is not the root object; the root object is
 *       passed as a named variable ({@code base}, {@code mock}).</li>
 *   <li><b>inline-import</b>: {@code import java.math.BigDecimal;} is replaced by the
 *       API imports set.</li>
 *   <li><b>local-variable-shadows-input</b>: local variables are declared in a map that
 *       does not already define {@code a}, {@code b}, {@code c}.</li>
 *   <li><b>function-pointer</b>: {@code squareRoot = java.lang.Math.sqrt} has no MVEL3
 *       equivalent; the static method is called directly.</li>
 *   <li><b>string-coercion</b>: {@code String s = 1} is written {@code String s = "1"}.</li>
 *   <li><b>inline-list</b>: MVEL2's {@code {a,b,c}} list literal is written as
 *       {@code [a, b, c]}, the MVEL3 list creation literal.</li>
 * </ul>
 */
public class MutationsAdaptedTest {

    private static final Set<String> IMPORTS = Set.of(
            "java.util.List",
            "java.util.HashMap",
            BigDecimal.class.getCanonicalName(),
            Foo.class.getCanonicalName(),
            Base.class.getCanonicalName());

    private static Object run(final String expression, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, IMPORTS, vars);
    }

    private static Object run(final String expression) {
        return run(expression, new HashMap<>());
    }

    private static Map<String, Object> baseVars() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("base", new Base());
        vars.put("array", new String[]{"", "blip"});
        vars.put("zero", 0);
        return vars;
    }

    @Test
    public void testDeepAssignment() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("foo", new Foo());
        assertThat(run("foo.bar.assignTest = \"crap\"; return foo.bar.assignTest;", vars)).isEqualTo("crap");
        assertThat(((Foo) vars.get("foo")).getBar().getAssignTest()).isEqualTo("crap");
    }

    @Test
    public void testDeepAssignment2() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("foo", new Foo());
        run("foo.bar.age = 21; return null;", vars);
        assertThat(((Foo) vars.get("foo")).getBar().getAge()).isEqualTo(21);
    }

    @Test
    public void testComplexExpression() {
        assertThat(run("var a = \"foo\"; var b = \"bar\"; var c = \"jim\"; var list = [a, b, c]; return list.get(1);"))
                .isEqualTo("bar");
    }

    @Test
    public void testAssignment() {
        assertThat(run("base.populate(); var blahfoo = \"sarah\"; return blahfoo == \"sarah\";", baseVars()))
                .isEqualTo(true);
    }

    @Test
    public void testAssignment2() {
        assertThat(run("base.populate(); var blahfoo = base.barfoo; return blahfoo;", baseVars()))
                .isEqualTo("sarah");
    }

    @Test
    public void testAssignment3() {
        assertThat(run("var blah = 5; return blah;").getClass()).isEqualTo(Integer.class);
    }

    @Test
    public void testAssignment4() {
        assertThat(run("var a = 100 + 1 + 1; return a;")).isEqualTo(102);
    }

    @Test
    public void testAssignment6() {
        assertThat(run("array[zero] = array[zero+1]; return array[zero];", baseVars())).isEqualTo("blip");
    }

    @Test
    public void testConstructor() {
        assertThat(run("var a = \"foobar\"; return new String(a.toCharArray(), 0, 3);")).isEqualTo("foo");
    }

    @Test
    public void testStaticVarAssignment() {
        assertThat(run("String mikeBrock = \"1\"; return mikeBrock;")).isEqualTo("1");
    }

    @Test
    public void testFunctionPointer() {
        assertThat(run("return Math.sqrt(4);")).isEqualTo(2.0);
    }

    @Test
    public void testIncrementOperator() {
        assertThat(run("var x = 1; x++; return x;")).isEqualTo(2);
    }

    @Test
    public void testPreIncrementOperator() {
        assertThat(run("var x = 1; return ++x;")).isEqualTo(2);
    }

    @Test
    public void testDecrementOperator() {
        assertThat(run("var x = 2; x--; return x;")).isEqualTo(1);
    }

    @Test
    public void testPreDecrementOperator() {
        assertThat(run("var x = 2; return --x;")).isEqualTo(1);
    }

    @Test
    public void testQualifiedStaticTyping() {
        Object val = run("java.math.BigDecimal a = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal b = new java.math.BigDecimal( 10.0 ); java.math.BigDecimal c = a + b; return c; ");
        assertThat(val).isEqualTo(new BigDecimal(20));
    }

    @Test
    public void testUnQualifiedStaticTyping() {
        Object val = run("BigDecimal a = new BigDecimal( 10.0 ); BigDecimal b = new BigDecimal( 10.0 ); BigDecimal c = a + b; return c; ");
        assertThat(val).isEqualTo(new BigDecimal(20));
    }

    @Test
    public void testSubExpressionIndexer() {
        assertThat(run("var xx = new HashMap<String, String>(); xx.put(\"foo\", \"bar\"); var prop = \"foo\"; return xx[prop];"))
                .isEqualTo("bar");
    }

    @Test
    public void testAssignListToBean() {
        MockClass mock = new MockClass();
        Map<String, Object> vars = new HashMap<>();
        vars.put("mock", mock);
        run("mock.values = [0, 1, 2, 3, 4]; return null;", vars);
        assertThat(mock.getValues()).hasSize(5);
    }

    public static class MockClass {
        List values;

        public List getValues() {
            return values;
        }

        public void setValues(List values) {
            this.values = values;
        }
    }
}
