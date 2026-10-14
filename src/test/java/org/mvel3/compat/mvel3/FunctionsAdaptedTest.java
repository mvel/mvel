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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porting of FunctionsTest from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>MVEL2's {@code def f(x) {..}} / {@code function f(x) {..}} declarations and anonymous
 * functions are not part of the MVEL3 grammar ({@code function-declaration}). Where the test
 * still checks something about <em>calling a function</em>, the function is written as a
 * public static method of {@link Funcs} (imported through the API imports set).
 *
 * <p>Where removing the function declaration leaves a test that no longer checks what the
 * original checked (the test would pass without verifying anything about functions), the
 * test is {@code @Disabled("function-declaration: not representable")} and its body is
 * replaced by the original expression in a comment.
 *
 * <p>Other adaptations in the enabled tests:
 * <ul>
 *   <li><b>block-syntax</b>: variables are declared with a type, and the result is returned
 *       explicitly with {@code return}.</li>
 *   <li><b>single-quote-string</b>: string literals use double quotes.</li>
 * </ul>
 *
 * <p>Not ported: {@code testFunctionDefAndCall2}, {@code testFunctionReuse} and
 * {@code testFunctionReuseMultiThread} exercise MVEL2-only APIs
 * ({@code CompilerTools.extractAllDeclaredFunctions}, chained {@code VariableResolverFactory},
 * {@code SharedFuncLib}).
 */
public class FunctionsAdaptedTest {

    private static final String NOT_REPRESENTABLE = "function-declaration: not representable";

    public static class Funcs {
        public static String heyFoo() {
            return "Foobar";
        }

        public static Object identity(final Object x) {
            return x;
        }

        public static int add(final int x, final int y) {
            return x + y;
        }

        public static int increment(final int i) {
            return i + 1;
        }

        public static double addDouble(final double a, final double b) {
            return a + b;
        }

        public static int max(final int a, final int b) {
            if (a > b) {
                return a;
            } else {
                return b;
            }
        }
    }

    private static final Set<String> IMPORTS = Set.of(Funcs.class.getCanonicalName());

    private static Object run(final String expression, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, IMPORTS, vars);
    }

    private static Object run(final String expression) {
        return run(expression, new HashMap<>());
    }

    // Original: def fun(x) { ($ in [1, 2, 3] if $ > x) } fun(0)
    // Needs a function that closes over its argument and a projection (projection-operator).
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testThatFunctionsCloseOverArguments() {
    }

    @Test
    public void testFunctionDefAndCall() {
        assertThat(run("return Funcs.heyFoo() + Funcs.heyFoo();")).isEqualTo("FoobarFoobar");
    }

    @Test
    public void testFunctionDefAndCall1() {
        assertThat(run("return Funcs.heyFoo() + Funcs.heyFoo();", new HashMap<>())).isEqualTo("FoobarFoobar");
    }

    // Original: function testFunction() { a = 'foo'; b = 'bar'; a + b; }; testFunction().toUpperCase();
    // Needs a function whose body assigns undeclared locals and returns its last expression.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testFunctionDefAndCall3() {
    }

    @Test
    public void testFunctionDefAndCall4() {
        assertThat(run("return Funcs.identity(\"barfoo\");")).isEqualTo("barfoo");
    }

    @Test
    public void testFunctionDefAndCall5() {
        assertThat(run("return Funcs.add(7, 3);")).isEqualTo(10);
    }

    @Test
    public void testFunctionDefAndCall6() {
        assertThat(run("return Funcs.identity(\"foo\");")).isEqualTo("foo");
    }

    // Original: a = function { 'foobar' }; a();
    // Needs an anonymous function stored in a variable.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testAnonymousFunction() {
    }

    // Original: def foo() {..} def bar() {..} def doMany(fps) { foreach(f : fps) { f(); } } doMany([foo,bar]);
    // Needs function declarations, functions as values in a list, and calling them dynamically.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testJIRA207() {
    }

    @Test
    public void testBranchesWithReturn() {
        // The result is returned instead of being assigned to the undeclared variable `val`.
        assertThat(run("var val = Funcs.max(20, 30); return val;")).isEqualTo(30);
    }

    // Original: def foobie12345() { hey(); } foobie12345();   (evaluated against a root object with hey())
    // Needs a function that calls a method of the root object.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testCallGlobalStaticFunctionFromMVELFunction() {
    }

    @Test
    public void testDeepNestedLoopsInFunction() {
        // def ff(i) is inlined; the nested loops, the call to increment(i) and the return stay.
        assertThat(run("int i = 0; int x = 0; while (i < 1) { x++; while (i < 10) { i = Funcs.increment(i); } } "
                       + "if (x == 1) { return i; } else { return -1; }")).isEqualTo(10);
    }

    @Test
    public void testFunctions5() {
        assertThat(run("return Funcs.addDouble(1.5, 5.25);")).isEqualTo(6.75);
    }

    // Original: def test(a1) { java.util.Collection a = a1; a.clear(); ..; a; }
    //           a = test(new java.util.ArrayList()); b = test(new java.util.HashSet());
    // Needs one function declaration reused with two argument types, and results assigned to undeclared variables.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testJIRA174() {
    }

    // Original: def f() { int a=1;a++;return a; }; f();
    // Needs a function declaration; the body alone does not check anything about functions.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testMVEL225() {
    }

    // Original: anonFunc = function (a,b) { return a + b; }; anonFunc(1,2)
    // Needs an anonymous function stored in a variable and called with arguments.
    @Test
    @Disabled(NOT_REPRESENTABLE)
    public void testAnonymousFunctionDecl() {
    }

    @Test
    public void testFunctionSemantics() {
        // Checks that an assignment used as an argument is evaluated before the call.
        assertThat(run("String x__0 = \"\"; return \"boob\".equals(Funcs.identity(x__0 = \"boob\")) "
                       + "&& x__0.equals(\"boob\");")).isEqualTo(true);
    }
}
