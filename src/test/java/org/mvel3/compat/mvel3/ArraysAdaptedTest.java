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

import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Cheese;
import org.mvel3.compat.mvel2.res.Foo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porting of ArraysTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>block-syntax</b>: undeclared variables ({@code xStr = ...}) are declared with
 *       {@code var}, and the result is returned explicitly with {@code return}.</li>
 *   <li><b>single-quote-string</b>: string literals use double quotes.</li>
 * </ul>
 */
public class ArraysAdaptedTest {

    private static final Set<String> IMPORTS = Set.of(Cheese.class.getCanonicalName());

    private static Object run(final String expression, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, IMPORTS, vars);
    }

    private static Object run(final String expression) {
        return run(expression, new HashMap<>());
    }

    @Test
    public void testArrayConstructionSupport1() {
        assertThat(run("new String[5]")).isInstanceOf(String[].class);
    }

    @Test
    public void testArrayConstructionSupport2() {
        assertThat(run("var xStr = new String[5]; return xStr.length == 5;")).isEqualTo(true);
    }

    @Test
    public void testArrayConstructionSupport3() {
        assertThat(run("var xStr = new String[5][5]; xStr[4][0] = \"foo\"; return xStr[4][0];")).isEqualTo("foo");
    }

    @Test
    public void testArrayConstructionSupport4() {
        assertThat(run("var xStr = new String[5][10]; xStr[4][0] = \"foo\"; return xStr[4].length;")).isEqualTo(10);
    }

    @Test
    public void testArrayDefinitionWithInitializer() {
        String[] results = (String[]) run("new String[] { \"foo\", \"bar\" }");
        assertThat(results).containsExactly("foo", "bar");
    }

    @Test
    public void testArrayDefinitionWithCoercion() {
        double[] d = (double[]) run("new double[] { 1,2,3,4 }");
        assertThat(d[1]).isEqualTo(2d);
    }

    @Test
    public void testArrayDefinitionWithCoercion2() {
        float[] d = (float[]) run("new float[] { 1,2,3,4 }");
        assertThat(d[1]).isEqualTo(2f);
    }

    @Test
    public void testArrayDefinitionWithCoercionBoolean() {
        boolean[] b = (boolean[]) run("new boolean[] { false, true, false }");
        assertThat(b[0]).isFalse();
        assertThat(b[1]).isTrue();
    }

    @Test
    public void testArrayDefinitionWithAutoBoxing() {
        Boolean[] b = (Boolean[]) run("new Boolean[] { !true, true, !!false }");
        assertThat(b[0]).isFalse();
        assertThat(b[1]).isTrue();
        assertThat(b[2]).isFalse();
    }

    @Test
    public void testArrayDefinitionWithCoercionInt() {
        int[] i = (int[]) run("new int[] { 0, 1, 2 }");
        assertThat(i[0]).isEqualTo(0);
        assertThat(i[1]).isEqualTo(1);
    }

    @Test
    public void testArrayDefinitionWithCoercionShort() {
        short[] s = (short[]) run("new short[] { 0, 1, 2 }");
        assertThat(s[0]).isEqualTo((short) 0);
        assertThat(s[1]).isEqualTo((short) 1);
    }

    @Test
    public void testArrayCreation2() {
        String[][] s = (String[][]) run("new String[][] {{\"2008-04-01\", \"2008-05-10\"}, {\"2007-03-01\", \"2007-02-12\"}}");
        assertThat(s[1][0]).isEqualTo("2007-03-01");
    }

    @Test
    public void testArrayCreation3() {
        String[][] s = (String[][]) run("new String[][] {{\"2008-04-01\", \"2008-05-10\"}, {\"2007-03-01\", \"2007-02-12\"}}");
        assertThat(s[1][0]).isEqualTo("2007-03-01");
    }

    @Test
    public void testArrayCreation4() {
        String[][] s = (String[][]) run("new String[][]{{\"2008-04-01\", \"2008-05-10\"}, {\"2007-03-01\", \"2007-02-12\"}}");
        assertThat(s[1][0]).isEqualTo("2007-03-01");
    }

    @Test
    public void testArrayCoercion1() {
        Foo foo = new Foo();
        Map<String, Object> vars = new HashMap<>();
        vars.put("foo", foo);
        vars.put("value", 12);
        run("foo.bar.intarray[0] = value; return null;", vars);
        assertThat(foo.getBar().getIntarray()[0].intValue()).isEqualTo(12);

        vars.put("value", 13);
        run("foo.bar.intarray[0] = value; return null;", vars);
        assertThat(foo.getBar().getIntarray()[0].intValue()).isEqualTo(13);
    }

    @Test
    public void testArrayLength() {
        new MVEL().compileMapExpression("x.length", Object.class, Set.of(),
                Map.of("x", Type.type(String[].class)));
    }

    @Test
    public void testMultiDimensionalArrayType() {
        Map<String, Object> vars = new HashMap<>();
        Column c = new Column("x", 1);
        c.setCheeses(new Cheese[5][5]);
        vars.put("c", c);
        run("c.cheeses[0][0] = new Cheese(\"brie\", 15); return null;", vars);
        assertThat(c.getCheeses()[0][0]).isEqualTo(new Cheese("brie", 15));
    }

    public static class Column {
        private String name;
        private int length;

        private Cheese[][] cheeses;

        public Cheese[][] getCheeses() {
            return cheeses;
        }

        public void setCheeses(Cheese[][] cheeses) {
            this.cheeses = cheeses;
        }

        public Column(String name, int length) {
            this.name = name;
            this.length = length;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }
    }

    @Test
    public void testAssignmentOnTwoDimensionalArrayUsingIndexedInput() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("array", new Double[2][2]);
        assertThat(run("array[1][1] = 42.0;\nreturn array[1][1];", vars)).isEqualTo(42.0);
    }
}
