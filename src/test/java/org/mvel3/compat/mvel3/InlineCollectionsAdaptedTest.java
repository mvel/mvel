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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;
import org.mvel3.compat.mvel2.AbstractTest.MiscTestClass;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.Foo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porting of InlineCollectionsTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>single-quote-string</b>: string literals use double quotes.</li>
 *   <li><b>block-syntax</b>: undeclared variables are declared with {@code var} / a type and the
 *       result is returned explicitly with {@code return}.</li>
 *   <li><b>inline-array-literal</b>: MVEL2's {@code {1,2,3}} array literal is written as
 *       {@code new int[] {1,2,3}} / {@code new Object[] {...}}; MVEL3 uses Java array syntax.</li>
 *   <li><b>foreach-syntax</b>: {@code foreach (x : xs)} is written as a Java {@code for}.</li>
 *   <li><b>nested-inline-collection</b>: a list or map literal cannot be nested inside another
 *       list or map literal ("illegal start of expression" from javac). The inner collection
 *       is declared first and referenced by name, or built with {@code List.of(..)}.</li>
 *   <li><b>contains-operator</b>: the infix {@code contains} is replaced by {@code indexOf(..) >= 0}
 *       or {@code containsKey(..)}; {@code list.contains(..)} on a local variable fails with
 *       "Unsupported member reference".</li>
 *   <li><b>list-concat-operator</b>: {@code list + list} / {@code list + element} do not compile;
 *       {@code addAll} / {@code add} are used.</li>
 *   <li><b>this-reference</b>: methods of the MVEL2 root object ({@code funMethod}, {@code sum})
 *       are called on a {@code base} variable.</li>
 *   <li><b>empty-operator</b> / <b>projection-operator</b>: {@code ($ in list if $ < 10) != empty}
 *       is written as a {@code for} loop and {@code isEmpty()}.</li>
 *   <li>The MVEL2 trailing comma {@code [2, 3, 4, ]} is not supported and is left out.</li>
 * </ul>
 */
public class InlineCollectionsAdaptedTest {

    private static final Set<String> IMPORTS = Set.of(
            "java.util.ArrayList",
            "java.util.List",
            "java.util.Map",
            "java.lang.reflect.Array",
            "java.util.Arrays",
            "java.util.HashMap",
            "org.mvel3.compat.mvel3.InlineCollectionsAdaptedTest.Target");

    private static Object run(final String expression, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, IMPORTS, vars);
    }

    private static Object run(final String expression) {
        return run(expression, new HashMap<>());
    }

    private static Map<String, Object> baseVars() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("base", new Base());
        return vars;
    }

    @Test
    public void testListCreation2() {
        assertThat(run("[\"test\"]")).isInstanceOf(List.class);
    }

    @Test
    public void testListCreation3() {
        assertThat(run("[66]")).isInstanceOf(List.class);
    }

    @Test
    public void testListCreation4() {
        List<?> ar = (List<?>) run("[   66   , \"test\"   ]");
        assertThat(ar).hasSize(2);
        assertThat(ar.get(0)).isEqualTo(66);
        assertThat(ar.get(1)).isEqualTo("test");
    }

    @Test
    public void testListCreationWithCall() {
        assertThat(run("[\"apple\"].size()")).isEqualTo(1);
    }

    @Test
    public void testArrayCreationWithLength() {
        assertThat(run("Array.getLength(new String[] {\"foo\", \"bar\"})")).isEqualTo(2);
    }

    @Test
    public void testEmptyList() {
        assertThat(run("[]")).isInstanceOf(List.class);
    }

    @Test
    public void testEmptyArray() {
        assertThat((Object[]) run("new Object[0]")).isEmpty();
    }

    @Test
    public void testEmptyArray2() {
        assertThat((Object[]) run("new Object[] {    }")).isEmpty();
    }

    @Test
    public void testArrayCreation() {
        assertThat(run("var arrayTest = new int[][] {{1, 2, 3}, {2, 1, 0}}; return arrayTest[1][2];")).isEqualTo(0);
    }

    @Test
    public void testMapCreation() {
        assertThat(run("var map = [\"mike\":\"sarah\",\"tom\":\"jacquelin\"]; return map.get(\"mike\");"))
                .isEqualTo("sarah");
    }

    @Test
    public void testMapCreation2() {
        assertThat(run("var map = [\"mike\" :\"sarah\"  ,\"tom\"  :\"jacquelin\"  ]; return map.get(\"mike\");"))
                .isEqualTo("sarah");
    }

    @Test
    public void testMapCreation3() {
        assertThat(run("var map = [1 : \"foo\"]; return map.get(1);")).isEqualTo("foo");
    }

    @Test
    public void testSizeOnInlineArray() {
        assertThat(run("new int[] {1,2,3}.length")).isEqualTo(3);
    }

    @Test
    public void testSimpleListCreation() {
        assertThat((List<?>) run("[\"foo\", \"bar\", \"foobar\", \"FOOBAR\"]")).hasSize(4);
    }

    @Test
    public void testArrayCoercion() {
        assertThat(run("return base.funMethod(new String[] {\"gonk\", \"foo\"});", baseVars())).isEqualTo("gonk");
    }

    @Test
    public void testArrayCoercion2() {
        assertThat(run("return base.sum(new int[] {2,2,2,2,2});", baseVars())).isEqualTo(10);
    }

    @Test
    public void testCompiledMapStructures() {
        assertThat(run("var m = [\"foo\":\"bar\"]; return m.containsKey(\"foo\");")).isEqualTo(true);
    }

    @Test
    public void testSubListInMap() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("c", "cat");
        assertThat(run("List<String> sub = [c, \"pear\"]; Map<String, Object> map = new HashMap<>(); "
                + "map.put(\"test\", \"poo\"); map.put(\"foo\", sub); "
                + "return ((List) map.get(\"foo\")).get(1);", vars)).isEqualTo("pear");
    }

    @Test
    public void testForEach2() {
        assertThat(run("var total = 0; var a = new int[] {1,2,3}; for (var item : a) { total += item; } return total;"))
                .isEqualTo(6);
    }

    @Test
    public void testForEach3() {
        assertThat(run("var a = new int[] {1,2,3}; for (var i : a) { if (i == 1) { return true; } } return false;"))
                .isEqualTo(true);
    }

    @Test
    public void testForEach3a() {
        assertThat(run("var a = new int[] {1,2,3}; for (var i : a) { if (i == 1) { return true; } } return false;",
                new HashMap<>())).isEqualTo(true);
    }

    @Test
    public void testForEach4() {
        assertThat(run("var a = new int[] {1,2,3,4}; var builder = \"\"; for (var i : a) {"
                + " if (i == 1) { builder += \"One\"; } else if (i == 2) { builder += \"Two\"; } "
                + "else if (i == 3) { builder += \"Three\"; } else { builder += \"Four\"; }"
                + "} return builder;")).isEqualTo("OneTwoThreeFour");
    }

    @Test
    public void testInlineCollectionNestedObjectCreation() {
        Map<?, ?> m = (Map<?, ?>) run("List<Integer> age = [1, 2, 3, 4]; List<String> rating = [\"High\", \"Low\"]; "
                + "return [\"Person.age\" : age, \"Person.rating\" : rating, "
                + "\"Person.something\" : (new String(\"foo\").toUpperCase())];");
        assertThat(m.get("Person.something")).isEqualTo("FOO");
    }

    @Test
    public void testInlineCollectionNestedObjectCreation1() {
        Map<?, ?> m = (Map<?, ?>) run("[new String(\"foo\") : new String(\"bar\")]");
        assertThat(m.get("foo")).isEqualTo("bar");
    }

    @Test
    public void testMVEL179() {
        assertThat(run("List<Integer> src = [2,4,8,16,32]; var res = new ArrayList<Integer>(); "
                + "for (int i : src) { if (i < 10) { res.add(i); } } return !res.isEmpty();")).isEqualTo(true);
    }

    @Test
    public void testInlineArrayForEach() {
        assertThat(run("var counterX = 0; for (var item : new int[] {1,2,3,4,5,6,7,8,9,10}) { counterX++; } "
                + "return counterX + 1;")).isEqualTo(11);
    }

    @Test
    public void testInlineArrayForEach2() {
        assertThat(run("var counterX = 10; for (var item : new int[] {1,1,1,1,1,1,1,1,1,1}) { counterX -= item; } "
                + "return counterX;")).isEqualTo(0);
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

    @Test
    public void testNegativeArraySizeBug() {
        // The two MVEL2 expressions differ only in the size of the inline array literal.
        String array1 = "new int[] {30, 214, 158, 31, 95, 223, 213, 86, 159, 34, 32, 96, 224, 160, 85, 201, 29, 157, 100, 146,"
                + " 82, 203, 194, 145, 140, 81, 27, 166, 212, 38, 28, 94, 168, 23, 87, 150, 35, 149, 193, 33, 132,"
                + " 206, 93, 196, 24, 88, 195, 36, 26, 154, 167, 108, 204, 74, 46, 25, 153, 202, 79, 207, 143, 43, "
                + "16, 80, 198, 208, 144, 41, 97, 142, 83, 18, 162, 103, 155, 98, 44, 17, 205, 77, 156, 141, 165,"
                + " 102, 84, 37, 101, 222, 40, 104, 99, 177, 182, 22, 180, 21, 137, 221, 179, 78, 42, 178, 19, 183,"
                + " 139, 218, 219, 39, 220, 20, 184, 217, 138, 62, 190, 171, 123, 113, 59, 118, 225, 124, 169, 60, "
                + "117, 1}";
        String array2 = array1.substring(0, array1.length() - 1) + ", 61, 189, 122, 68, 58, 119, 63, 226, 3, 172}";

        List<Target> targets = new ArrayList<>();
        targets.add(new Target(1));
        targets.add(new Target(999));
        Map<String, Object> vars = new HashMap<>();
        vars.put("elements", targets);

        for (String array : new String[]{array1, array2}) {
            Object result = run("var results = new ArrayList<Target>(); var values = " + array + "; "
                    + "for (Target element : (List<Target>) elements) { "
                    + "boolean found = false; "
                    + "for (int v : values) { if (v == element.getAttribute()) { found = true; } } "
                    + "if (found) { results.add(element); } } return results;", vars);
            assertThat((List<?>) result).hasSize(1);
        }
    }

    @Test
    public void testInlineCollectionParser1() {
        Map<String, Object> m1 = castMap(run("List<Integer> age = [1, 2, 3, 4]; "
                + "return [\"Person.age\" : age, \"Person.rating\" : \"q\"];"));
        assertThat(m1.get("Person.rating")).isEqualTo("q");
        Map<String, Object> m2 = castMap(run("List<Integer> age = [1, 2, 3, 4]; "
                + "return [\"Person.age\" : age, \"Person.rating\" : \"q\"];"));
        assertThat(m2.get("Person.rating")).isEqualTo("q");
    }

    @Test
    public void testParsingStability3() {
        assertThat(run("List<String> l = [\"X\", \"Y\"]; return !(l.indexOf(\"Y\") >= 0);")).isEqualTo(false);
    }

    @Test
    public void testToList() {
        assertToList(run(TO_LIST, toListVars()));
    }

    @Test
    public void testToListStrictMode() {
        // MVEL2's strict type enforcement has no MVEL3 equivalent; MVEL3 always types inputs.
        assertToList(run(TO_LIST, toListVars()));
    }

    private static final String TO_LIST =
            "List<Object> inner = new ArrayList<>(); inner.add(foo.bar.age); inner.add(\"car\"); inner.add(42); "
                    + "Map<String, Object> m1 = new HashMap<>(); m1.put(\"key1\", \"value1\"); m1.put(c, inner); "
                    + "Map<String, Object> m2 = new HashMap<>(); m2.put(c, \"value1\"); "
                    + "List<Object> l2 = new ArrayList<>(); l2.add(42); l2.add(m2); "
                    + "return misc.toList(foo.bar.name, \"hello\", 42, m1, l2);";

    private static Map<String, Object> toListVars() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("misc", new MiscTestClass());
        vars.put("foo", new Foo());
        vars.put("c", "cat");
        return vars;
    }

    @SuppressWarnings("unchecked")
    private static void assertToList(final Object result) {
        List<Object> list = (List<Object>) result;
        assertThat(list.get(0)).isEqualTo("dog");
        assertThat(list.get(1)).isEqualTo("hello");
        assertThat(list.get(2)).isEqualTo(42);
        Map<String, Object> map = (Map<String, Object>) list.get(3);
        assertThat(map.get("key1")).isEqualTo("value1");

        List<Object> nestedList = (List<Object>) map.get("cat");
        assertThat(nestedList.get(0)).isEqualTo(14);
        assertThat(nestedList.get(1)).isEqualTo("car");
        assertThat(nestedList.get(2)).isEqualTo(42);

        nestedList = (List<Object>) list.get(4);
        assertThat(nestedList.get(0)).isEqualTo(42);
        map = (Map<String, Object>) nestedList.get(1);
        assertThat(map.get("cat")).isEqualTo("value1");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(final Object o) {
        return (Map<String, Object>) o;
    }

    @Test
    public void testArrayAccessorAssign() {
        assertThat(run("var a = new String[] {\"f00\", \"bar\"}; a[0] = \"foo\"; return a[0];")).isEqualTo("foo");
    }

    @Test
    public void testInlineListSensitivenessToSpaces() {
        List<?> result = (List<?>) run("([\"a\",\"b\", \"c\"])");
        assertThat(result).isNotNull();
        assertThat(result.get(0)).isEqualTo("a");
        assertThat(result.get(1)).isEqualTo("b");
        assertThat(result.get(2)).isEqualTo("c");
    }

    @Test
    public void testMVEL241() {
        Map<String, Object> map = castMap(run("List<Integer> age = [42, 43]; List<String> sex = [\"M\", \"F\"]; "
                + "return [ \"Person.age\" : age,\n\"Person.sex\" : sex ];"));

        assertThat(map).isNotNull();
        assertThat(map).hasSize(2);
        assertThat(map).containsKey("Person.age");
        assertThat(map).containsKey("Person.sex");

        List<?> listSex = (List<?>) map.get("Person.sex");
        assertThat(listSex).hasSize(2);

        List<?> listAge = (List<?>) map.get("Person.age");
        assertThat(listAge).hasSize(2);
    }

    @Test
    public void testMVEL242() {
        Map<String, Object> map = castMap(run("List<String> f1 = [\"val1\", \"val2\"]; List<String> f2 = [\"val3\", \"val4\"]; "
                + "List<String> a = [\"f1val1a\", \"f1val1b\"]; List<String> b = [\"f1val2a\", \"f1val2b\"]; "
                + "return [ \"Fact.field1\" : f1, \"Fact.field2\" : f2, \"Fact.field2[field1=val1]\" : a, "
                + "\"Fact.field2[field1=val2]\" : b ];"));

        assertThat(map).isNotNull();
        assertThat(map).hasSize(4);
    }

    @Test
    public void testEmptyElement() {
        // MVEL2 ignores a trailing comma in "[2, 3, 4, ]"; the trailing comma is removed.
        Object result = run("[2, 3, 4]");
        assertThat(result).isInstanceOf(List.class);
        assertThat((List<?>) result).hasSize(3);
    }

    @Test
    public void testAddTwoLists() {
        Object result = run("List<Integer> l = new ArrayList<>([1,2,3]); l.addAll([4,5,6]); return l;");
        assertThat(result).isInstanceOf(List.class);
        assertThat((List<?>) result).hasSize(6);
    }

    @Test
    public void testElementToList() {
        Object result = run("List<Integer> l = new ArrayList<>([1,2,3]); l.add(4); return l;");
        assertThat(result).isInstanceOf(List.class);
        assertThat((List<?>) result).hasSize(4);
    }
}
