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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.Evaluator;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Bar;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.Cheese;
import org.mvel3.compat.mvel2.res.Foo;
import org.mvel3.compat.mvel2.res.MyInterface2;
import org.mvel3.compat.mvel2.res.MyInterface3;
import org.mvel3.compat.mvel2.res.SampleBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Porting of TypesAndInferenceTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>block-syntax</b>: undeclared variables are declared with a type or {@code var}, and the
 *       result is returned explicitly with {@code return}. A block that ends in a statement gets a
 *       trailing {@code return}.</li>
 *   <li><b>void-method-expression</b>: {@code a.foo(value)} is written {@code a.foo(value); return null;}.
 *       The evaluator is compiled for the declared type of {@code a} ({@code A} or {@code B}),
 *       because MVEL3 binds the generated code to the compile-time input type.</li>
 *   <li><b>single-quote-string</b>: string literals use double quotes.</li>
 *   <li><b>this-reference</b>: the MVEL2 root object is passed as a named input variable.</li>
 *   <li><b>string-coercion</b>: {@code foo.bar.woof == "true"} is written with the boolean
 *       literal {@code true}.</li>
 *   <li><b>reserved-method-name</b>: {@code "test".matches(..)} is written
 *       with an anchored {@code Pattern.compile(..).matcher(..).find()}.</li>
 *   <li><b>missing-auto-import</b>: {@code java.math.BigInteger} is added to the import set.</li>
 *   <li><b>dynamic-property-type</b>: the value of a raw {@code HashMap} is cast before use.</li>
 *   <li><b>set-expression-api</b>: property assignment is written as an assignment expression
 *       ({@code foo.name = setValue}) on a map input.</li>
 *   <li>Fixtures are public static nested classes, because the generated evaluator must be able
 *       to access them.</li>
 * </ul>
 *
 * <p>Tests that only compile an expression (or expect a compile-time failure) compile eagerly with
 * {@code compileMapBlock}/{@code compileMapExpression}. The same tests that MVEL2 left out of the
 * {@code compat.mvel2} port (analysis, optimizer and {@code ParserContext} APIs) are not ported here.
 * {@code testDetermineEgressParametricType2} is identical to {@code testDetermineEgressParametricType}
 * and is covered by it.
 */
public class TypesAndInferenceAdaptedTest {

    private static final Set<String> IMPORTS = Set.of(
            Bar.class.getCanonicalName(),
            Base.class.getCanonicalName(),
            Cheese.class.getCanonicalName(),
            Foo.class.getCanonicalName(),
            MyInterface2.class.getCanonicalName(),
            MyInterface3.class.getCanonicalName(),
            Address.class.getCanonicalName(),
            java.math.BigInteger.class.getCanonicalName());

    private static Object run(final String expression, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, IMPORTS, vars);
    }

    private static Map<String, Type<?>> types(final Object... namesAndClasses) {
        final Map<String, Type<?>> types = new LinkedHashMap<>();
        for (int i = 0; i < namesAndClasses.length; i += 2) {
            types.put((String) namesAndClasses[i], Type.type((Class<?>) namesAndClasses[i + 1]));
        }
        return types;
    }

    /** Compiles at once; a block is chosen when the expression contains a statement separator. */
    private static Evaluator<Map<String, Object>, Void, Object> compile(final String expression,
                                                                        final Map<String, Type<?>> types) {
        if (expression.indexOf(';') > 0) {
            return new MVEL().compileMapBlock(expression, Object.class, IMPORTS, types);
        }
        return new MVEL().compileMapExpression(expression, Object.class, IMPORTS, types);
    }

    // ---- fixtures ----

    public static class Person {
        private List<Foo> footributes;

        public List<Foo> getFootributes() {
            return footributes;
        }

        public void setFootributes(List<Foo> footributes) {
            this.footributes = footributes;
        }
    }

    public static final List<String> STRINGS = Arrays.asList("hi", "there");

    public static class A {
        public void foo(String s) {
        }

        public void bar(String s) {
        }

        public List<String> getStrings() {
            return STRINGS;
        }
    }

    public static class B extends A {
        @Override
        public void foo(String s) {
            super.foo(s);
        }

        public void bar(int s) {
        }
    }

    public static class C extends A {
    }

    public static class ScriptHelper228 {
        public void methodA() {
        }

        public void methodB(int param1) {
        }
    }

    public static class Person228 {
        public String getName() {
            return "foo";
        }
    }

    public static class MapWrapper {
        private Map map = new HashMap();

        public Map getMap() {
            return map;
        }

        public void setMap(Map map) {
            this.map = map;
        }
    }

    public static class EchoContext {
        public String echo(String str) {
            return str;
        }
    }

    public static class AGenericTestClass {
        private Map<String, String> map;

        public Map<String, String> getMap() {
            return map;
        }

        public void setMap(Map<String, String> map) {
            this.map = map;
        }
    }

    public static class Address {
        private String street;

        public Address(String street) {
            this.street = street;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        @Override
        public int hashCode() {
            return street == null ? 0 : street.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Address && java.util.Objects.equals(street, ((Address) obj).street);
        }
    }

    public static class PersonAddresses {
        private List<Address> addresses = new ArrayList<Address>();

        public List<Address> getAddresses() {
            return addresses;
        }

        public void setAddresses(List<Address> addresses) {
            this.addresses = addresses;
        }
    }

    // ---- tests ----

    @Test
    public void testGenericInference() {
        final Person p = new Person();
        p.setFootributes(new ArrayList<Foo>());
        p.getFootributes().add(new Foo());

        final Map<String, Object> vars = new HashMap<>();
        vars.put("person", p);

        assertThat(run("var $result = person.footributes[0].name; return $result;", vars)).isEqualTo("dog");
    }

    @Test
    public void testMultiTypeVarDeclr3() {
        // Locals are not written back to the variable map in MVEL3, so the values are returned.
        assertThat(run("int a = 52 * 3, b = 8, c = 16; return a + \",\" + b + \",\" + c;", new HashMap<>()))
                .isEqualTo("156,8,16");
    }

    @Test
    public void testEgressTypeCorrect2() {
        final SampleBean s = new SampleBean();
        s.getMap2().put("yyy", 1);

        final Map<String, Object> vars = new HashMap<>();
        vars.put("s", s);

        assertThat(run("( s.map2[ \"yyy\" ] )", vars)).isEqualTo(1);
    }

    @Test
    public void testDetermineEgressParametricType() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("a", new A());

        assertThat(run("a.getStrings()", vars)).isEqualTo(STRINGS);
    }

    @Test
    public void testJIRA151c() {
        // Compiled for the declared type A; the instances differ at runtime (B overrides foo, C does not).
        final Evaluator<Map<String, Object>, Void, Object> compiled =
                compile("a.foo(value); return null;", types("a", A.class, "value", String.class));

        for (int i = 0; i < 100; i++) {
            for (final A a : new A[]{new B(), new C()}) {
                final Map<String, Object> vars = new HashMap<>();
                vars.put("a", a);
                vars.put("value", "123");
                compiled.eval(vars);
            }
        }
    }

    @Test
    public void testJIRA165() {
        // bar(int) is only declared by B, so B is the compile-time type of the first evaluator.
        final Evaluator<Map<String, Object>, Void, Object> forB =
                compile("a.bar(value); return null;", types("a", B.class, "value", Integer.class));
        final Evaluator<Map<String, Object>, Void, Object> forA =
                compile("a.bar(value); return null;", types("a", A.class, "value", String.class));

        for (int i = 0; i < 100; i++) {
            final Map<String, Object> varsB = new HashMap<>();
            varsB.put("a", new B());
            varsB.put("value", 123);
            forB.eval(varsB);

            final Map<String, Object> varsA = new HashMap<>();
            varsA.put("a", new A());
            varsA.put("value", "123");
            forA.eval(varsA);
        }
    }

    @Test
    public void testStrictTypingCompilationWithVarInsideConstructor() {
        final List<Object> results = new ArrayList<>();
        final Map<String, Object> vars = new HashMap<>();
        vars.put("$likes", "stilton");
        vars.put("results", results);

        run("Cheese c = new Cheese( $likes, 15 );\nresults.add( c ); return null;", vars);

        assertThat(results.get(0)).isEqualTo(new Cheese("stilton", 15));
    }

    @Test
    public void testStrongTyping3() {
        assertThatThrownBy(() -> compile("foo.toUC(100.5", types())).isInstanceOf(Exception.class);
    }

    @Test
    public void testStrictStaticMethodCall() {
        assertThat(run("Bar.staticMethod()", new HashMap<>())).isEqualTo(1);
    }

    @Test
    public void testStrictTypingCompilation3() {
        assertThat(run("var message = \"Hello\"; var b = 7;\nSystem.out.println(message + \";\" + b);\n"
                + "System.out.println(message + \";\" + b); return b;", new HashMap<>())).isEqualTo(7);
    }

    @Test
    public void testStrictStrongTypingCompilationErrors1() {
        assertThatThrownBy(() -> compile("System.out.println( $ba );", types("$bar", Bar.class)))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testStrictStrongTypingCompilationErrors2() {
        assertThatThrownBy(() -> compile("x_a = new Foo244( $ba ); x_a.equals($ba);", types("$bar", Bar.class)))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testProvidedExternalTypes() {
        compile("foo.bar", types("foo", Foo.class));
    }

    @Test
    public void testMVEL228() {
        assertThatThrownBy(() -> compile("helper.methodB(2);\nperson.getName2();",
                types("helper", ScriptHelper228.class, "person", Person228.class)))
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testMVEL232() {
        final String script = "for(int i=0;i<2;i++) { System.out.println(i+\"\"); } return true;";

        assertThat(run(script, new HashMap<>())).isEqualTo(true);
    }

    @Test
    public void testMVEL234() {
        // Any member named "matches" is rejected by MVEL3 (reserved-method-name), including Pattern.matches;
        // an anchored Pattern.find() is equivalent here.
        final String script = "if (java.util.regex.Pattern.compile(\"^[0-9]$\").matcher(\"test\").find()) {"
                + "  return false;"
                + "} else {"
                + "  java.text.SimpleDateFormat sqf = new java.text.SimpleDateFormat(\"yyyyMMdd\");"
                + "  return true;"
                + "}";

        assertThat(run(script, new HashMap<>())).isEqualTo(true);
    }

    @Test
    public void testMVEL235() {
        final String script = "if(var1.equals(var2)) { return true; } return false;";

        compile(script, types("var1", MyInterface2.class, "var2", MyInterface2.class));
    }

    @Test
    public void testMVEL236() {
        compile("MyInterface2 var2 = (MyInterface2)var1; return null;", types("var1", MyInterface3.class));
    }

    @Test
    public void testMapPropertyAccess() {
        final MapWrapper wrapper = new MapWrapper();
        wrapper.getMap().put("key", "value");
        final Map<String, Object> vars = new HashMap<>();
        vars.put("wrapper", wrapper);

        assertThat(run("wrapper.map[\"key\"]", vars)).isEqualTo("value");
    }

    @Test
    public void testTypeCast3() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("foo", new Foo());

        assertThat(run("((org.mvel3.compat.mvel2.res.Bar) foo.getBar()).name != null", vars)).isEqualTo(true);
    }

    @Test
    public void testMapWithStrictTyping() {
        final Map<String, Object> map = new HashMap<>();
        map.put("KEY1", "MSGONE");
        final Map<String, Object> vars = new HashMap<>();
        vars.put("$msg", "MSGONE");
        vars.put("map", map);

        assertThat(run("map[\"KEY1\"] == $msg", vars)).isEqualTo(true);
    }

    @Test
    public void testMapAsContextWithStrictTyping() {
        // The MVEL2 root map ("this") is passed as the named input "ctxMap".
        final Map<String, Object> ctxMap = new HashMap<>();
        ctxMap.put("KEY1", "MSGONE");
        final Map<String, Object> vars = new HashMap<>();
        vars.put("$msg", "MSGONE");
        vars.put("ctxMap", ctxMap);

        assertThat(run("ctxMap[\"KEY1\"] == $msg", vars)).isEqualTo(true);
    }

    @Test
    public void testStrongTyping() {
        assertThatThrownBy(() -> compile("blah", types())).isInstanceOf(Exception.class);
    }

    @Test
    public void testStrongTyping2() {
        assertThatThrownBy(() -> compile("1-blah", types("blah", String.class))).isInstanceOf(Exception.class);
    }

    @Test
    public void testParameterizedTypeInStrictMode() {
        // The type arguments of the HashMap input cannot be declared, so the value is cast.
        compile("((String) foo.get(\"bar\")).toUpperCase()", types("foo", HashMap.class));
    }

    @Test
    public void testSetAccessorOverloadedEqualsStrictMode2() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("foo", new Foo());

        assertThat(run("foo.aValue = \"bar\"", vars)).isEqualTo("bar");
    }

    @Test
    public void testCompileTimeCoercion() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("foo", new Foo());

        assertThat(run("foo.bar.woof == true", vars)).isEqualTo(true);
    }

    @Test
    public void testSetCoercion() {
        final Foo foo = new Foo();
        final Map<String, Object> vars = new HashMap<>();
        vars.put("foo", foo);
        vars.put("setValue", "12");

        run("foo.name = setValue; return null;", vars);

        assertThat(foo.getName()).isEqualTo("12");
    }

    @Test
    public void testSetCoercion2() {
        final Foo foo = new Foo();
        final Map<String, Object> vars = new HashMap<>();
        vars.put("foo", foo);
        vars.put("setValue", "12");

        run("foo.sampleBean.map2[\"bleh\"] = Integer.valueOf(setValue); return null;", vars);

        assertThat(foo.getSampleBean().getMap2().get("bleh").intValue()).isEqualTo(12);
    }

    @Test
    public void testListCoercion() {
        final Foo foo = new Foo();
        foo.getBar().getTestList().add(Integer.valueOf(-1));
        final Map<String, Object> vars = new HashMap<>();
        vars.put("foo", foo);
        vars.put("setValue", "13");

        run("foo.bar.testList.set(0, Integer.valueOf(setValue)); return null;", vars);

        assertThat(foo.getBar().getTestList().get(0).intValue()).isEqualTo(13);
    }

    @Test
    public void testFieldCoercion1() {
        final Foo foo = new Foo();
        final Map<String, Object> vars = new HashMap<>();
        vars.put("foo", foo);
        vars.put("setValue", 12);

        run("foo.bar.assignTest = String.valueOf(setValue); return null;", vars);

        assertThat(foo.getBar().getAssignTest()).isEqualTo("12");
    }

    @Test
    public void testStaticTyping2() {
        final int[] res = (int[]) run("int x = 5; int y = 2; return new int[] { x, y };", new HashMap<>());

        assertThat(res).containsExactly(5, 2);
    }

    @Test
    public void testMVEL190a() {
        compile("a.toString()", types("a", String.class));
    }

    @Test
    public void testPrimitiveTypes() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("base", new Base());

        assertThat(((Number) run("int x = 5; x = x + base.intValue; return x;", vars)).intValue()).isEqualTo(15);
    }

    @Test
    public void testAutoBoxing() {
        final List<?> list = (List<?>) run("var list = new java.util.ArrayList(); list.add( 5 ); return list;",
                new HashMap<>());

        assertThat(list).hasSize(1);
    }

    @Test
    public void testAutoBoxing2() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("base", new Base());

        final List<?> list = (List<?>) run("java.util.List list = new java.util.ArrayList(); "
                + "list.add( base.intValue ); return list;", vars);

        assertThat(list).hasSize(1);
    }

    @Test
    public void testTypeCoercion() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("base", new Base());

        final Number x = (Number) run("java.math.BigInteger x = new java.math.BigInteger( \"5\" );"
                + " return x + base.intValue;", vars);

        assertThat(x.intValue()).isEqualTo(15);
    }

    @Test
    public void testTypeCoercion2() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("base", new Base());

        final Number x = (Number) run("java.math.BigInteger x = new java.math.BigInteger( \"5\" );"
                + " return x + base.intValue;", vars);

        assertThat(x.intValue()).isEqualTo(15);
    }

    @Test
    public void testContextMethodCallsInStrongMode() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("ctx", new EchoContext());

        assertThat(run("ctx.echo( \"Mac\")", vars)).isEqualTo("Mac");
    }

    @Test
    public void testForLoopTypeCoercion() {
        final List<Object> list = new ArrayList<>();
        final Map<String, Object> vars = new HashMap<>();
        vars.put("$type", "pc!!");
        vars.put("l", list);

        run("for (byte bt : $type.getBytes()) { l.add( bt ); } return null;", vars);

        final byte[] exp = "pc!!".getBytes();
        for (int i = 0; i < exp.length; i++) {
            assertThat(list.get(i)).isEqualTo(exp[i]);
        }
    }

    @Test
    public void testGenericMethods() {
        final AGenericTestClass a = new AGenericTestClass();
        a.setMap(new HashMap<String, String>());
        a.getMap().put("x", "10");
        final Map<String, Object> vars = new HashMap<>();
        vars.put("a", a);

        assertThat(((Number) run("Integer.parseInt( a.getMap().get(\"x\") )", vars)).intValue()).isEqualTo(10);
    }

    @Test
    public void testGenerics1() {
        final PersonAddresses pa = new PersonAddresses();
        pa.getAddresses().add(new Address("s1"));
        final Map<String, Object> vars = new HashMap<>();
        vars.put("pa", pa);

        assertThat(run("pa.addresses[0] == new Address(\"s1\") && pa.addresses[0].street == new Address(\"s1\").street",
                vars)).isEqualTo(true);
    }
}
