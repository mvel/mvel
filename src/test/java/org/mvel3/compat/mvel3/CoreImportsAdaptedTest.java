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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvel3.ClassManager;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Cheese;
import org.mvel3.compat.mvel2.res.Cheesery;
import org.mvel3.compat.mvel2.res.Foo;
import org.mvel3.compat.mvel2.res.MyClass;
import org.mvel3.compat.mvel2.res.MyInterface;
import org.mvel3.compat.mvel2.res.Status;
import org.mvel3.compat.mvel2.res.User;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porting of the import tests of CoreConfidenceTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>inline-import</b>: {@code import java.util.*;} inside the expression is replaced by the
 *       API imports set ({@code java.util.*} or the class name); {@code import_static} likewise
 *       ({@code Math.sqrt(4)} is called through its class).</li>
 *   <li><b>block-syntax</b>: undeclared variables are declared with a type or {@code var}, and the
 *       result is returned explicitly with {@code return}; a block that ends in a statement gets a
 *       {@code return null;}.</li>
 *   <li><b>single-quote-string</b>: string literals use double quotes.</li>
 *   <li><b>class-literal</b>: a bare class name used as the value is written {@code HashMap.class}.</li>
 *   <li><b>this-reference</b>: the MVEL2 root object is passed as a named input variable.</li>
 *   <li><b>inner-class-binary-name</b>: {@code Outer$Inner} is written with dots or imported.</li>
 *   <li><b>string-coercion</b>: {@code (java.lang.Integer) '1'} is written {@code Integer.valueOf("1")}.</li>
 *   <li><b>static-member-unqualified</b>: {@code new Status( START )} is written {@code Status.START}.</li>
 *   <li><b>undeclared-input-variable</b>: {@code bar} is declared as an input.</li>
 *   <li><b>empty-operator</b>: {@code list == empty} is written {@code list.isEmpty()}.</li>
 *   <li>Fixtures are public static nested classes, because the generated evaluator must be able
 *       to access them.</li>
 * </ul>
 *
 * <p>Tests that only compile an expression compile eagerly with
 * {@code compileMapBlock}/{@code compileMapExpression}. {@code testUnwantedImport} (MVEL2 analysis API)
 * is not ported.
 *
 * <p>{@code @Disabled}: class aliasing ({@code class-alias}), the nested enum of a class that has a
 * property of the same name ({@code external-jar-classloader}) and the inner class reached through
 * an instance-inner class ({@code CoreImportsTests.Fruit.Apple}; MVEL3 reads {@code Fruit.Apple} as a
 * member of an instance) have no MVEL3 equivalent.
 */
public class CoreImportsAdaptedTest {

    private static final Set<String> NO_IMPORTS = Set.of();

    private static Object run(final String expression) {
        return run(expression, NO_IMPORTS, new HashMap<>());
    }

    private static Object run(final String expression, final Set<String> imports, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, imports, vars);
    }

    private static void compile(final String expression, final Set<String> imports, final Map<String, Type<?>> types) {
        if (expression.indexOf(';') > 0) {
            new MVEL().compileMapBlock(expression, Object.class, imports, types);
        } else {
            new MVEL().compileMapExpression(expression, Object.class, imports, types);
        }
    }

    private static Set<String> imports(final String... names) {
        return new LinkedHashSet<>(List.of(names));
    }

    @Test
    public void testWhileUsingImports() {
        // empty-operator: `list == empty` -> list.isEmpty()
        assertThat((Boolean) run("List list = new ArrayList(); return list.isEmpty();",
                imports("java.util.ArrayList", "java.util.List"), new HashMap<>())).isTrue();
    }

    @Test
    public void testImport() {
        // inline-import + class-literal: the import goes through the API, the value is HashMap.class
        assertThat(run("return HashMap.class;", imports("java.util.HashMap"), new HashMap<>())).isEqualTo(HashMap.class);
    }

    @Test
    public void testImport2() {
        // inline-import + block-syntax
        final HashMap[] maps = (HashMap[]) run("HashMap[] maps = new HashMap[10]; return maps;",
                imports("java.util.*"), new HashMap<>());

        assertThat(maps).hasSize(10);
    }

    public static class MyPerson {
        private final String name;

        public MyPerson(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    public void testUnusedPackageImport() {
        // inline-import + this-reference: the package is imported through the API (and unused), the root is an input
        final Map<String, Object> vars = new HashMap<>();
        vars.put("person", new MyPerson("Mario"));
        assertThat((Boolean) run("person.getName().equals(\"Mario\")", imports("java.util.*"), vars)).isTrue();
    }

    @Test
    public void testStaticImport() {
        // inline-import: import_static java.lang.Math.sqrt -> Math.sqrt
        assertThat(run("Math.sqrt(4)")).isEqualTo(2.0);
    }

    @Test
    public void testCheeseConstructor() {
        // block-syntax: typed declaration
        assertThat(run("Cheese cheese = new Cheese(\"cheddar\", 15); return cheese;",
                imports(Cheese.class.getCanonicalName()), new HashMap<>())).isInstanceOf(Cheese.class);
    }

    @Test
    public void testDynamicImports() {
        assertThat(run("return HashMap.class;", imports("java.util.*"), new HashMap<>())).isEqualTo(HashMap.class);

        assertThat(run("var map = new HashMap(); return map.size();", imports("java.util.*"), new HashMap<>()))
                .isEqualTo(0);
    }

    @Test
    public void testDynamicImports3() {
        final String expression = "HashMap map = new HashMap(); return map.size();";

        assertThat(run(expression, imports("java.util.*"), new HashMap<>())).isEqualTo(0);

        assertThat(run(expression, imports("java.util.*"), new HashMap<>())).isEqualTo(0);
    }

    @Test
    public void testDynamicImportsInList() {
        final List<?> list = (List<?>) run("[ new User(\"Bobba\", \"Feta\") ]",
                imports("org.mvel3.compat.mvel2.res.*"), new HashMap<>());
        final User user = (User) list.get(0);
        assertThat(user.getFirstName()).isEqualTo("Bobba");
    }

    @Test
    public void testDynamicImportsInMap() {
        final Map<?, ?> map = (Map<?, ?>) run("[ \"bobba\" : new User(\"Bobba\", \"Feta\") ]",
                imports("org.mvel3.compat.mvel2.res.*"), new HashMap<>());
        final User user = (User) map.get("bobba");
        assertThat(user.getFirstName()).isEqualTo("Bobba");
    }

    @Test
    public void testDynamicImportsOnNestedExpressions() {
        final Cheesery p1 = new Cheesery("bobbo", new Cheese("cheddar", 15));
        final Cheesery p2 = (Cheesery) run("new Cheesery(\"bobbo\", new Cheese(\"cheddar\", 15))",
                imports("org.mvel3.compat.mvel2.res.*"), new HashMap<>());

        assertThat(p2).isEqualTo(p1);
    }

    @Test
    public void testDynamicImportsWithNullConstructorParam() {
        final Cheesery p1 = new Cheesery("bobbo", null);
        final Cheesery p2 = (Cheesery) run("new Cheesery(\"bobbo\", null)",
                imports("org.mvel3.compat.mvel2.res.*"), new HashMap<>());

        assertThat(p2).isEqualTo(p1);
    }

    @Test
    public void testDynamicImportsWithNullConstructorParamWithStrongType() {
        // MVEL3 is always strongly typed, so this is the same as the previous test.
        final Cheesery p1 = new Cheesery("bobbo", null);
        final Cheesery p2 = (Cheesery) run("new Cheesery(\"bobbo\", null)",
                imports("org.mvel3.compat.mvel2.res.*"), new HashMap<>());

        assertThat(p2).isEqualTo(p1);
    }

    @Test
    public void testDynamicImportsWithIdentifierSameAsClassWithDiffCase() {
        // undeclared-input-variable: bar is declared as an input
        final Map<String, Type<?>> types = new LinkedHashMap<>();
        types.put("bar", Type.type(ArrayList.class));
        compile("bar.add(\"hello\")", imports("org.mvel3.compat.mvel2.res.*"), types);
    }

    @Test
    public void testTypedAssignment() {
        // single-quote-string + block-syntax: double quotes, return
        assertThat(run("java.util.Map map = new java.util.HashMap(); map.put(\"conan\", \"foobar\"); return map[\"conan\"];"))
                .isEqualTo("foobar");
    }

    @Test
    public void testFQCNwithStaticInList() {
        // block-syntax: var + return
        assertThat(run("var list = [java.lang.Integer.MIN_VALUE]; return list[0];"))
                .isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    public void testStaticNested() {
        // inner-class-binary-name: dotted name instead of AbstractTest$Message
        assertThat(run("org.mvel3.compat.mvel2.AbstractTest.Message.GOODBYE")).isEqualTo(1);
    }

    @Test
    public void testStaticNestedWithImport() {
        // block-syntax: the value is returned, without the trailing `;`
        assertThat(run("Message.GOODBYE",
                imports("org.mvel3.compat.mvel2.AbstractTest.Message"), new HashMap<>())).isEqualTo(1);
    }

    @Test
    public void testStaticNestedWithMethodCall() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("$msg", new org.mvel3.compat.mvel2.AbstractTest.Message());

        // block-syntax: typed declaration
        final org.mvel3.compat.mvel2.AbstractTest.Message msg = (org.mvel3.compat.mvel2.AbstractTest.Message) run(
                "Item item = new Item( \"Some Item\"); $msg.addItem( item ); return $msg;",
                imports("org.mvel3.compat.mvel2.AbstractTest.Message", "org.mvel3.compat.mvel2.AbstractTest.Item"), vars);
        final org.mvel3.compat.mvel2.AbstractTest.Item item = (org.mvel3.compat.mvel2.AbstractTest.Item) msg.getItems().get(0);
        assertThat(item.getName()).isEqualTo("Some Item");
    }

    @Test
    public void testLateResolveOfClass() {
        // block-syntax: return null after the void call
        System.out.println(run("System.out.println(new Foo()); return null;",
                imports(Foo.class.getCanonicalName()), new HashMap<>()));
    }

    @Test
    @Disabled("class-alias: `Foo244 = String; new Foo244(..)` has no MVEL3 equivalent")
    public void testClassAliasing() {
        // Original: Foo244 = String; new Foo244('foobar')
    }

    @Test
    public void testDynamicImports2() {
        // inline-import + class-literal
        assertThat(run("return BufferedReader.class;", imports("java.io.*"), new HashMap<>()))
                .isEqualTo(java.io.BufferedReader.class);
    }

    @Test
    public void testFullyQualifiedTypeAndCast() {
        // string-coercion + single-quote-string: MVEL2 converts the String in the cast; MVEL3 needs valueOf
        assertThat(run("java.lang.Integer number = java.lang.Integer.valueOf(\"1\"); return number;")).isEqualTo(1);
    }

    @Test
    @Disabled("MVEL3 reads CoreImportsTests.Fruit.Apple (instance inner class) as a member of an instance; only .class works")
    public void testInnerClassReference() {
        // Original: import ...CoreConfidenceTests; CoreConfidenceTests.Fruit.Apple
    }

    public static class Fruit {
        public static class Apple {
        }
    }

    @Test
    public void testInnerClassReferenceStatic() {
        // inline-import + class-literal: static nested classes, imported through the API, with .class
        assertThat(run("return CoreImportsAdaptedTest.Fruit.Apple.class;",
                imports(CoreImportsAdaptedTest.class.getCanonicalName()), new HashMap<>()))
                .isEqualTo(Fruit.Apple.class);
    }

    @Test
    public void testEnumSupport() {
        // this-reference + inner-class-binary-name: the root is an input and the enum is written with dots
        final MyInterface myInterface = new MyClass();
        myInterface.setType(MyInterface.MY_ENUM.TWO, true);
        final Map<String, Object> vars = new HashMap<>();
        vars.put("myInterface", myInterface);
        final boolean isType = (Boolean) run("myInterface.isType(org.mvel3.compat.mvel2.res.MyInterface.MY_ENUM.ONE)",
                NO_IMPORTS, vars);
        assertThat(isType).isFalse();
    }

    @Test
    public void testJavaLangImport() {
        // block-syntax: return the variable
        compile("Exception e = null; return e;", NO_IMPORTS, new LinkedHashMap<>());
    }

    public static class Triangle {
        public enum Foo {
            INCOMPLETE, UNCLASSIFIED,
            EQUILATERAL, ISOSCELES, RECTANGLED, ISOSCELES_RECTANGLED, ACUTE, OBTUSE
        }
    }

    @Test
    public void testNestedEnum() {
        // inline-import: the class is imported through the API
        assertThat(run("Triangle.Foo.OBTUSE", imports(Triangle.class.getCanonicalName()), new HashMap<>()))
                .isEqualTo(Triangle.Foo.OBTUSE);
    }

    @Test
    @Disabled("external-jar-classloader: EventRequest.Status.ACTIVE is read as the property `status`; the jar is not found without directory entries")
    public void testNestedEnumFromJar() throws Exception {
        // Original: EventRequest.Status.ACTIVE with the class loaded from eventing-example.jar through a URLClassLoader
        final URL resource = getClass().getResource("/eventing-example.jar");
        assertThat(resource).isNotNull();
        final URLClassLoader loader = new URLClassLoader(new URL[]{resource}, getClass().getClassLoader());
        final Class<?> er = loader.loadClass("org.drools.examples.eventing.EventRequest");
        assertThat(er).isNotNull();
        new MVEL().compileMapExpression("EventRequest.Status.ACTIVE", Object.class, imports(er.getCanonicalName()),
                new LinkedHashMap<>(), new ClassManager(), loader);
    }

    @Test
    public void testFullyQualifiedEnums() {
        // inner-class-binary-name: dotted name instead of MyInterface$STATIC_ENUM; block-syntax: return null
        final String str = "System.out.println( STATIC_ENUM.FOO ); \n" +
                "System.out.println( org.mvel3.compat.mvel2.res.MyInterface.STATIC_ENUM.BAR );\n" +
                "System.out.println( org.mvel3.compat.mvel2.res.MyInterface.MyInnerInterface.INNER_STATIC_ENUM.BAR );\n" +
                "System.out.println( RoundingMode.UP );\n" +
                "System.out.println( java.math.RoundingMode.DOWN );\n" +
                "return null;";

        final Map<String, Type<?>> types = new LinkedHashMap<>();
        types.put("thisObj", Type.type(MyInterface.class));
        compile(str, imports(MyInterface.STATIC_ENUM.class.getCanonicalName(), java.math.RoundingMode.class.getCanonicalName()),
                types);
    }

    @Test
    public void testPackageImportEnum() {
        // static-member-unqualified: Status.START instead of START
        final Object result = run("new Status( Status.START )", imports("org.mvel3.compat.mvel2.res.*"), new HashMap<>());
        assertThat(result).isEqualTo(new Status(Status.START));
        assertThat(new Status(Status.STOP)).isNotEqualTo(result);
    }

    public static class ARef {
        public static int getSize(final String s) {
            return 0;
        }
    }

    @Test
    public void testInstanceofOnInnerClass() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("value", new ARef());
        assertThat(run("value instanceof ARef", imports(ARef.class.getCanonicalName()), vars)).isEqualTo(true);
        assertThat(run("value instanceof " + ARef.class.getCanonicalName(), imports(ARef.class.getCanonicalName()), vars))
                .isEqualTo(true);
    }

    @Test
    public void testInstanceofWithPackageImport() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("value", new CoreImportsAdaptedTest());
        assertThat(run("value instanceof CoreImportsAdaptedTest", imports("org.mvel3.compat.mvel3.*"), vars)).isEqualTo(true);
        assertThat(run("value instanceof " + CoreImportsAdaptedTest.class.getCanonicalName(),
                imports("org.mvel3.compat.mvel3.*"), vars)).isEqualTo(true);
    }

    @Test
    public void testInstanceofWithPackageImportAndInnerClass() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("value", new ARef());
        assertThat(run("value instanceof ARef", imports(ARef.class.getCanonicalName()), vars)).isEqualTo(true);
        assertThat(run("value instanceof " + ARef.class.getCanonicalName(), imports(ARef.class.getCanonicalName()), vars))
                .isEqualTo(true);
    }
}
