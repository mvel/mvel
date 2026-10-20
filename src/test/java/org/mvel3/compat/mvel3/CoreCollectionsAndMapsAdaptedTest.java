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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.junit.jupiter.api.Test;
import org.mvel3.Evaluator;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Foo;
import org.mvel3.compat.mvel2.res.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Porting of the collection and map tests of CoreConfidenceTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>single-quote-string</b>: string literals use double quotes.</li>
 *   <li><b>block-syntax</b>: undeclared variables are declared with a type or {@code var}, and the
 *       result is returned explicitly with {@code return}.</li>
 *   <li><b>foreach-syntax</b>: {@code foreach (item : set)} is written {@code for (String item : set)}.</li>
 *   <li><b>this-reference</b>: the MVEL2 root object is passed as a named input variable ({@code base},
 *       {@code person}, {@code c}).</li>
 *   <li><b>set-expression-api</b>: {@code compileSetExpression}/{@code executeSetExpression} become an
 *       assignment, with the value passed as an input.</li>
 *   <li><b>list-to-array-coercion</b>: the array is built explicitly.</li>
 *   <li><b>set-index-access</b>: {@code pojo.dates[0]} is written {@code pojo.dates.iterator().next()}.</li>
 *   <li><b>non-public-input-type</b>: the input is declared as {@code Collection}.</li>
 *   <li><b>inline-array-literal</b>: {@code {'foo','test'}[1]} is written {@code new String[] {"foo", "test"}[1]}.</li>
 *   <li>A nested property inside a map index ({@code map[key]}) is written with an explicit getter call
 *       ({@code base.map[base.getKey()]}).</li>
 *   <li>Fixtures are public static nested classes, because the generated evaluator must be able
 *       to access them.</li>
 * </ul>
 *
 * <p>Not ported (as in the MVEL2 import): {@code testMapBindingSemantics}, {@code testSetSemantics},
 * {@code testAddIntToMapWithMapSyntax}, {@code testUpdateIntInMapWithMapSyntax},
 * {@code testMapsWithVariableAsKey}, {@code testNullCollection} and {@code testParseGenericMap}.
 */
public class CoreCollectionsAndMapsAdaptedTest {

    private static final Set<String> NO_IMPORTS = Set.of();

    private static Object run(final String expression) {
        return run(expression, NO_IMPORTS, new HashMap<>());
    }

    private static Object run(final String expression, final Set<String> imports, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, imports, vars);
    }

    private static Evaluator<Map<String, Object>, Void, Object> compile(final String expression,
                                                                          final Set<String> imports,
                                                                          final Map<String, Type<?>> types) {
        if (expression.indexOf(';') > 0) {
            return (Evaluator<Map<String, Object>, Void, Object>) new MVEL().compileMapBlock(expression, Object.class, imports, types);
        }
        return (Evaluator<Map<String, Object>, Void, Object>) new MVEL().compileMapExpression(expression, Object.class, imports, types);
    }

    private static Set<String> imports(final String... names) {
        return new LinkedHashSet<>(List.of(names));
    }

    private static Map<String, Object> vars(final Object... keyValues) {
        final Map<String, Object> vars = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            vars.put((String) keyValues[i], keyValues[i + 1]);
        }
        return vars;
    }

    public static class Person {
        private Map<Object, Foo> objectKeyMaptributes;

        public Map<Object, Foo> getObjectKeyMaptributes() {
            return objectKeyMaptributes;
        }

        public void setObjectKeyMaptributes(final Map<Object, Foo> objectKeyMaptributes) {
            this.objectKeyMaptributes = objectKeyMaptributes;
        }
    }

    public static class POJO {
        private Set<Date> dates = new HashSet<>();
        private Map<String, Object> map = new HashMap<>();

        public POJO() {
            dates.add(new Date());
        }

        public Set<Date> getDates() {
            return dates;
        }

        public String function(final long num) {
            return String.valueOf(num);
        }

        public String aMethod(final long num) {
            return String.valueOf(num);
        }

        public Map<String, Object> getMap() {
            return map;
        }

        public String getKey() {
            return "1";
        }
    }

    public static class Triangle {
        public static enum Foo {
            INCOMPLETE, UNCLASSIFIED,
            EQUILATERAL, ISOSCELES, RECTANGLED, ISOSCELES_RECTANGLED, ACUTE, OBTUSE;
        }

        private final List<Map<String, Object>> deliveries = new ArrayList<>();

        public List<Map<String, Object>> getDeliveries() {
            return deliveries;
        }
    }

    @Test
    public void testCreationOfSet() {
        // single-quote-string + block-syntax + foreach-syntax
        assertThat(run("var set = new LinkedHashSet<String>(); "
                        + "set.add(\"foo\"); set.add(\"bar\"); "
                        + "String output = \"\"; "
                        + "for (String item : set) { output = output + item + \" \"; } "
                        + "for (String item : set) { output = output + item + \" \"; } "
                        + "output = output.trim(); "
                        + "if (set.size() == 2) { return output; } "
                        + "return null;",
                imports("java.util.LinkedHashSet"), new HashMap<>())).isEqualTo("foo bar foo bar");
    }

    @Test
    public void testCreationOfList() {
        // single-quote-string + block-syntax
        assertThat(run("var l = new LinkedList<String>(); l.add(\"fun\"); l.add(\"happy\"); l.add(\"fun\"); l.add(\"slide\");"
                        + "l.add(\"crap\"); var poo = new ArrayList<String>(l); return poo.size();",
                imports("java.util.LinkedList", "java.util.ArrayList"), new HashMap<>())).isEqualTo(5);
    }

    @Test
    public void testMapOperations() {
        // single-quote-string + block-syntax; the list read from the raw map is cast, because its element type is Object
        assertThat(run("var l = new ArrayList<String>(); l.add(\"plop\"); l.add(\"poo\"); var m = new HashMap<String, Object>();"
                        + "m.put(\"foo\", l); m.put(\"cah\", \"mah\"); m.put(\"bar\", \"foo\"); m.put(\"sarah\", \"mike\");"
                        + "m.put(\"edgar\", \"poe\"); if (m.edgar == \"poe\") { return \"\" + ((List) m.get(\"foo\")).get(1) + m.size(); } "
                        + "return null;",
                imports("java.util.ArrayList", "java.util.HashMap", "java.util.List"), new HashMap<>())).isEqualTo("poo5");
    }

    @Test
    public void testStackOperations() {
        // block-syntax: typed Stack, so that the popped values are Integer
        assertThat(run("var stk = new Stack<Integer>(); stk.push(5); stk.push(5); return stk.pop() + stk.pop();",
                imports("java.util.Stack"), new HashMap<>())).isEqualTo(10);
    }

    @Test
    public void testMapPropertyCreateCondensed() {
        assertThat(run("var map = new HashMap<String, String>(); map[\"test\"] = \"foo\"; return map[\"test\"];",
                imports("java.util.HashMap"), new HashMap<>())).isEqualTo("foo");
    }

    @Test
    public void testListAccessorAssign() {
        final String ex = "var a = new ArrayList<String>(); a.add(\"foo\"); a.add(\"BAR\"); a[1] = \"bar\"; return a[1];";

        final Evaluator<Map<String, Object>, Void, Object> evaluator = compile(ex, imports("java.util.ArrayList"), new HashMap<>());
        assertThat(evaluator.eval(new HashMap<>())).isEqualTo("bar");
        assertThat(evaluator.eval(new HashMap<>())).isEqualTo("bar");
    }

    @Test
    public void testMapAccessSemantics() {
        final Map<String, Object> innermap = new HashMap<>();
        innermap.put("test", "foo");

        assertThat(run("innermap[\"test\"]", NO_IMPORTS, vars("innermap", innermap))).isEqualTo("foo");
    }

    @Test
    public void testMapNestedInsideList() {
        // single-quote-string + block-syntax
        final Set<String> imports = imports("org.mvel3.compat.mvel2.res.User");

        List<?> list = (List<?>) run("var users = [ \"darth\" : new User(\"Darth\", \"Vadar\"),\n"
                + "\"bobba\" : new User(\"Bobba\", \"Feta\") ]; return [ users.get(\"darth\"), users.get(\"bobba\") ];",
                imports, new HashMap<>());
        assertThat(((User) list.get(0)).getFirstName()).isEqualTo("Darth");
        assertThat(((User) list.get(1)).getFirstName()).isEqualTo("Bobba");

        list = (List<?>) run("var users = [ \"darth\" : new User(\"Darth\", \"Vadar\"),\n"
                + "\"bobba\" : new User(\"Bobba\", \"Feta\") ]; return [ users[\"darth\"], users[\"bobba\"] ];",
                imports, new HashMap<>());
        assertThat(((User) list.get(0)).getFirstName()).isEqualTo("Darth");
        assertThat(((User) list.get(1)).getFirstName()).isEqualTo("Bobba");
    }

    @Test
    public void testListNestedInsideList() {
        // single-quote-string + block-syntax
        final Set<String> imports = imports("org.mvel3.compat.mvel2.res.User");

        List<?> list = (List<?>) run("var users = [ new User(\"Darth\", \"Vadar\"), new User(\"Bobba\", \"Feta\") ]; "
                + "return [ users.get( 0 ), users.get( 1 ) ];", imports, new HashMap<>());
        assertThat(((User) list.get(0)).getFirstName()).isEqualTo("Darth");
        assertThat(((User) list.get(1)).getFirstName()).isEqualTo("Bobba");

        list = (List<?>) run("var users = [ new User(\"Darth\", \"Vadar\"), new User(\"Bobba\", \"Feta\") ]; "
                + "return [ users[0], users[1] ];", imports, new HashMap<>());
        assertThat(((User) list.get(0)).getFirstName()).isEqualTo("Darth");
        assertThat(((User) list.get(1)).getFirstName()).isEqualTo("Bobba");
    }

    @Test
    public void testMapBindingSemantics2() {
        // set-expression-api + single-quote-string: the set expression becomes an assignment, the value is an input
        final Map<String, Object> innermap = new HashMap<>();
        innermap.put("test", "foo");

        run("innermap[\"test\"] = setValue", NO_IMPORTS, vars("innermap", innermap, "setValue", "bar"));

        assertThat(run("innermap[\"test\"]", NO_IMPORTS, vars("innermap", innermap))).isEqualTo("bar");
    }

    @Test
    public void testMapsAndLists() {
        // block-syntax: typed declarations and an explicit return
        final List<Object> list = new ArrayList<>();

        final Object result = run("var m = new HashMap<String, Object>(); var l = new ArrayList<String>(); l.add(\"first\"); "
                        + "m.put(\"content\", l); return list.add(((ArrayList<String>) m[\"content\"])[0]);",
                imports("java.util.HashMap", "java.util.ArrayList"), vars("list", list));

        assertThat(result).isEqualTo(true);
        assertThat(list).containsExactly("first");
    }

    @Test
    public void testMapsAndLists2() {
        final List<Object> list = new ArrayList<>();
        final String expression = "HashMap<String, Object> m = new HashMap<String, Object>(); ArrayList<String> l = new ArrayList<String>(); "
                + "l.add(\"first\"); m.put(\"content\", l); return list.add(((ArrayList<String>) m[\"content\"])[0]);";

        final Object result = run(expression, imports("java.util.HashMap", "java.util.ArrayList"), vars("list", list));

        assertThat(result).isEqualTo(true);
        assertThat(list).containsExactly("first");
    }

    @Test
    public void testMapsWithVariableAsKey2() {
        // this-reference: the root Person is the named input `person`
        final Foo foo = new Foo();
        final Person person = new Person();
        person.setObjectKeyMaptributes(new HashMap<>());
        person.getObjectKeyMaptributes().put(person, foo);

        assertThat(run("person.objectKeyMaptributes[$aPerson] == foo", NO_IMPORTS,
                vars("person", person, "foo", foo, "$aPerson", person))).isEqualTo(true);
    }

    @Test
    public void testMapAssignmentNestedExpression() {
        // single-quote-string + block-syntax
        final Map<String, Object> map = new HashMap<>();
        map.put("map", new HashMap<>());

        assertThat(run("map[java.lang.Integer.MAX_VALUE] = \"bar\"; return map[java.lang.Integer.MAX_VALUE];",
                NO_IMPORTS, vars("map", new HashMap<>()))).isEqualTo("bar");
    }

    @Test
    public void testMapAssignmentNestedExpression2() {
        // single-quote-string + block-syntax
        assertThat(run("map[x] = \"foo\"; return map[\"bar\"];", NO_IMPORTS,
                vars("x", "bar", "map", new HashMap<>()))).isEqualTo("foo");
    }

    @Test
    public void testNestedNumInMapKey() {
        // this-reference: the root Person is the named input `person`
        final Foo foo = new Foo();
        final Person person = new Person();
        final Map<Object, Foo> map = new HashMap<>();
        map.put(Triangle.Foo.OBTUSE, foo);
        person.setObjectKeyMaptributes(map);

        assertThat(run("person.objectKeyMaptributes[Triangle.Foo.OBTUSE]",
                imports(Triangle.class.getCanonicalName()), vars("person", person))).isSameAs(foo);
    }

    @Test
    public void testBindListToArray() {
        // set-expression-api + list-to-array-coercion: the char[] is built from the list, then assigned
        final Foo foo = new Foo();
        final List<String> list = new ArrayList<>(List.of("a", "b", "c"));

        run("char[] arr = new char[list.size()]; for (int i = 0; i < arr.length; i++) { arr[i] = ((String) list.get(i)).charAt(0); } "
                + "foo.charArray = arr; return null;", NO_IMPORTS, vars("foo", foo, "list", list));

        assertThat(foo.getCharArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testBindListToMultiArray() {
        // set-expression-api + list-to-array-coercion: the char[][] is built from the lists, then assigned.
        // `new char[n][]` (open second dimension) is miscompiled by MVEL3 (char[] cannot be converted to char[][]), so both
        // dimensions are given.
        final Foo foo = new Foo();
        final List<List<String>> list = new ArrayList<>();
        list.add(new ArrayList<>(List.of("a", "b", "c")));
        list.add(new ArrayList<>(List.of("d", "e", "f")));
        list.add(new ArrayList<>(List.of("g", "h", "i")));

        run("char[][] arr = new char[list.size()][((List) list.get(0)).size()]; "
                + "for (int i = 0; i < arr.length; i++) { "
                + "List row = (List) list.get(i); "
                + "for (int j = 0; j < arr[i].length; j++) { arr[i][j] = ((String) row.get(j)).charAt(0); } } "
                + "foo.charArrayMulti = arr; return null;", imports("java.util.List"), vars("foo", foo, "list", list));

        assertThat(foo.getCharArrayMulti()).hasNumberOfRows(3);
        assertThat(foo.getCharArrayMulti()[2][2]).isEqualTo('i');
    }

    @Test
    public void testGenericsMap() {
        final Map<String, Type<?>> types = new LinkedHashMap<>();
        types.put("triangle", Type.type(Triangle.class));

        compile("triangle.deliveries[0].containsKey( \"x\" )", NO_IMPORTS, types);
    }

    @Test
    public void testStrictModeAddAll() {
        final Map<String, Type<?>> types = new LinkedHashMap<>();
        types.put("o", Type.type(Object.class));
        types.put("list", Type.type(ArrayList.class));

        // o is not a Collection, so addAll(o) does not compile
        assertThatThrownBy(() -> compile("list.addAll( o );", NO_IMPORTS, types)).isInstanceOf(Exception.class);
    }

    @Test
    public void testArrayList() {
        // this-reference: the collection is the named input `c`
        final Collection<String> collection = new ArrayList<>();
        collection.add("I CAN HAS CHEEZBURGER");

        assertThat(run("c.size()", NO_IMPORTS, vars("c", collection))).isEqualTo(collection.size());
    }

    @Test
    public void testUnmodifiableCollection() {
        // this-reference + non-public-input-type: the input is declared as Collection
        Collection<String> collection = new ArrayList<>();
        collection.add("I CAN HAS CHEEZBURGER");
        collection = Collections.unmodifiableCollection(collection);

        final Map<String, Type<?>> types = new LinkedHashMap<>();
        types.put("c", Type.type(Collection.class));

        assertThat(compile("c.size()", NO_IMPORTS, types).eval(vars("c", collection))).isEqualTo(collection.size());
    }

    @Test
    public void testSingleton() {
        // this-reference + non-public-input-type: the input is declared as Collection
        final Collection<String> collection = Collections.singleton("I CAN HAS CHEEZBURGER");

        final Map<String, Type<?>> types = new LinkedHashMap<>();
        types.put("c", Type.type(Collection.class));

        assertThat(compile("c.size()", NO_IMPORTS, types).eval(vars("c", collection))).isEqualTo(collection.size());
    }

    private static POJO pojoWithMapEntry() {
        final POJO pojo = new POJO();
        pojo.getMap().put("1", "one");
        return pojo;
    }

    @Test
    public void testMapAccessWithNestedMethodCall() {
        // this-reference: the root POJO is the named input `base`
        assertThat(run("base.map[base.aMethod(1)] == \"one\"", NO_IMPORTS, vars("base", pojoWithMapEntry()))).isEqualTo(true);
    }

    @Test
    public void testMapAccessProperty() {
        // this-reference: the root POJO is the named input `base`. MVEL3 reads `map.key` of a Map as the entry "key",
        // which is absent, instead of failing like MVEL2's PropertyAccessException.
        assertThat(run("base.map.key", NO_IMPORTS, vars("base", new POJO()))).isNull();
    }

    @Test
    public void testMapAccessWithNestedPropertyAO() {
        // this-reference + nested property in the map index: `base.getKey()`
        final Evaluator<Map<String, Object>, Void, Object> evaluator = compile("base.map[base.getKey()] == \"one\"",
                NO_IMPORTS, Map.of("base", Type.type(POJO.class)));
        final Map<String, Object> vars = vars("base", pojoWithMapEntry());

        assertThat(evaluator.eval(vars)).isEqualTo(true);
        assertThat(evaluator.eval(vars)).isEqualTo(true);
    }

    @Test
    public void testMapAccessWithNestedPropertyAO_ASM() {
        final Evaluator<Map<String, Object>, Void, Object> evaluator = compile("base.map[base.getKey()] == \"one\"",
                NO_IMPORTS, Map.of("base", Type.type(POJO.class)));
        final Map<String, Object> vars = vars("base", pojoWithMapEntry());

        assertThat(evaluator.eval(vars)).isEqualTo(true);
        assertThat(evaluator.eval(vars)).isEqualTo(true);
    }

    @Test
    public void testMapAccessWithNestedProperty() {
        assertThat(run("base.map[base.getKey()] == \"one\"", NO_IMPORTS, vars("base", pojoWithMapEntry()))).isEqualTo(true);
    }

    @Test
    public void testMapAccessWithNestedPropertyRepeated() {
        final Evaluator<Map<String, Object>, Void, Object> evaluator = compile("base.map[base.getKey()] == \"one\"",
                NO_IMPORTS, Map.of("base", Type.type(POJO.class)));
        final Map<String, Object> vars = vars("base", pojoWithMapEntry());

        for (int i = 0; i < 500; ++i) {
            assertThat(evaluator.eval(vars)).as("iteration %d", i).isEqualTo(true);
        }
    }

    @Test
    public void testMethodInvocationWithCollectionElement() {
        // set-index-access: `pojo.dates[0]` on a Set is written with iterator().next()
        final POJO pojo = new POJO();

        final Object result = run("pojo.function(pojo.dates.iterator().next().getTime())", NO_IMPORTS,
                vars("pojo", pojo, "number", "1192800637980"));

        assertThat(result).isEqualTo(String.valueOf(pojo.getDates().iterator().next().getTime()));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testNestedMethod1() {
        // single-quote-string + block-syntax
        final Vector vectorA = new Vector();
        final Vector vectorB = new Vector();

        vectorA.add("Foo244");

        run("vecB.add(vecA.remove(0)); vecA.add(\"Foo244\"); return null;", NO_IMPORTS, vars("vecA", vectorA, "vecB", vectorB));

        assertThat(vectorB.get(0)).isEqualTo("Foo244");
    }

    @Test
    public void testInlineUnion() {
        // inline-array-literal + single-quote-string
        assertThat(run("new String[] {\"foo\", \"test\"}[1]")).isEqualTo("test");
    }
}
