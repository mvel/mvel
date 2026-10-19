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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvel3.Evaluator;
import org.mvel3.MVEL;
import org.mvel3.Type;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Porting of FailureTests from MVEL2 adapted for MVEL3 syntax and negative-testing semantics.
 */
public class FailureAdaptedTest {

    private static Evaluator<Map<String, Object>, Void, Object> compile(final String expression) {
        return compile(expression, Collections.emptyMap());
    }

    private static Evaluator<Map<String, Object>, Void, Object> compile(final String expression,
                                                                        final Map<String, Type<?>> types) {
        if (expression.indexOf(';') > 0) {
            return new MVEL().compileMapBlock(expression, Object.class, Set.of(), types);
        }
        return new MVEL().compileMapExpression(expression, Object.class, Set.of(), types);
    }

    private static Object run(final String expression, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, Set.of(), vars);
    }

    private static Object run(final String expression) {
        return run(expression, Collections.emptyMap());
    }

    @Test
    public void testBadParserConstruct() {
        assertThatThrownBy(() -> compile("a = 0; a =+++ 5;"))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testShouldFail() {
        assertThatThrownBy(() -> run("i = 0; i < 99 dksadlka", new HashMap<>()))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testShouldFail2() {
        assertThatThrownBy(() -> compile("i = 0; i < 99 dksadlka"))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testShouldFail3() {
        assertThatThrownBy(() -> compile("def foo() { \"bar\" }; foo(123);"))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testShouldFail4() {
        assertThatThrownBy(() -> run("hour zzz", Map.of("hour", 60)))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testShouldFail5() {
        assertThatThrownBy(() -> run("["))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testShouldFail6() {
        assertThatThrownBy(() -> compile("new int[] {1.5}"))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testShouldFail7() {
        assertThatThrownBy(() -> run("String x = \"foo\"; int y = 2; return new int[] { x, y };"))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testShouldFail8() {
        assertThatThrownBy(() -> compile("for (String s : new java.util.HashMap()) { }"))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testShouldFail9() {
        assertThatThrownBy(() -> compile("foo = "))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testShouldFail10() {
        assertThatThrownBy(() -> compile("foo = [1,1,qq,zz]"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @Disabled("javaparser-null-node: transpiling malformed fuzz input triggers an AssertionError in JavaParser")
    public void testShouldFailCleanly() {
        assertThatThrownBy(() -> run("6:f-\t\n" +
                "r:wsGbL%X5&wb<C.8n(\n" +
                "1X-;zUX-L<%<TG)S#z1fh69:tn`#eH9XneL8XFIB94-z*XzQ-RPhX{&bbp{fLDe@`3<-\tZp9_k*Xo\tDy36t15XX>-75EArR]6`**<kw}P<FpD-+XA-<+K!\n" +
                "Rb9n)zA-<L9pIIbHb?!b-bO%n<zLFqzbmm-n#~-zL`5Fq_PYD+9`NPt4Tnt\tvT-L[d\n" +
                "<b<9l9XV9-9 X9h7#9Ln?FnnLXLkg5<V-Z%bb-n&Et<B-X[n\"jbvg&@b{X0?*9eC{%zU\n" +
                "L\t{RPX\tbwhY&L`z<`Oh`<8pH\n" +
                "b:y:#H-;&,PzXw\ttHicFbs"))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testShouldFail11() {
        assertThatThrownBy(() -> compile("for (int i = 0; i--; i++) {}"))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testShouldFail12() {
        assertThatThrownBy(() -> compile("def x(_"))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testErrorUtilShouldNotThrowNPE() {
        assertThatThrownBy(() -> compile("new org.mvel3.compat.mvel3.FailureAdaptedTest$Person()"))
                .isInstanceOf(Exception.class)
                .isNotInstanceOf(NullPointerException.class);
    }

    public static class Person {
        @SuppressWarnings("unused")
        private String name;

        public Person(String name) {
            this.name = name;
        }
    }
}
