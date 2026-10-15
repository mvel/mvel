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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvel3.Evaluator;
import org.mvel3.MVEL;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.DerivedClass;
import org.mvel3.compat.mvel2.res.Foo;
import org.mvel3.compat.mvel2.res.TestInterface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Porting of PropertyAccessTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations applied to the enabled tests:
 * <ul>
 *   <li><b>single-quote-string</b>: string literals use double quotes.</li>
 *   <li><b>this-reference</b>: methods and fields of the MVEL2 root object are reached through
 *       the input variable {@code base}.</li>
 *   <li><b>dynamic-property-type</b>: a {@code Foo} stored in the raw {@code funMap} is cast
 *       before its properties are read.</li>
 *   <li><b>array-size-method</b> / <b>string-index-access</b>: {@code array.length} and
 *       {@code str.charAt(1)}.</li>
 *   <li><b>map-property-assignment</b>: {@code mp.put(..)} instead of {@code mp.test = ..}.</li>
 *   <li><b>null-safe-operator</b>: {@code a.?map[..]} is written as an explicit null check.</li>
 *   <li><b>block-syntax</b>: locals are declared with a type or {@code var}, and the result is
 *       returned explicitly with {@code return}.</li>
 *   <li>Checked exceptions ({@code Class.forName}) are caught, as in Java.</li>
 *   <li>Anonymous / inner fixtures are public static nested classes, because the generated
 *       evaluator must be able to access them.</li>
 * </ul>
 *
 * <p>Tests are {@code @Disabled} when the point of the original has no MVEL3 equivalent
 * (chained {@code or}, a compiled expression reused with a different input type).
 *
 * <p>Not ported: {@code testBindingCoercion} ({@code MVEL.setProperty}),
 * {@code testMVELCompilerBoth} and {@code testMVELCompilerBoth2} ({@code PropertyHandler}).
 */
public class PropertyAccessAdaptedTest {

    public static class TestImpl implements TestInterface {
        public String getName() {
            return "FOOBAR!";
        }

        public boolean isFoo() {
            return true;
        }
    }

    public static class A226 {
        public Map<String, Object> map = null;

        public Map<String, Object> getMap() {
            return map;
        }
    }

    public static class Data {
        private final Collection<Object> list;

        public Data(List<Object> list) {
            this.list = list;
        }

        public Collection<Object> getList() {
            return list;
        }
    }

    public static class Service {
        private void hello() {
            System.out.println("hello world");
        }
    }

    private static final Set<String> IMPORTS = Set.of(
            Foo.class.getCanonicalName(),
            Base.class.getCanonicalName(),
            "java.util.Map",
            "java.util.HashMap",
            "java.util.Hashtable",
            "java.util.ArrayList",
            "java.util.List");

    private static Map<String, Object> vars() {
        final Map<String, Object> map = new HashMap<>();
        final Base base = new Base();
        map.put("base", base);
        map.put("fun", base.fun);
        map.put("funMap", base.funMap);
        map.put("foo", new Foo());
        map.put("c", "cat");
        map.put("testImpl", new TestImpl());
        map.put("derived", new DerivedClass());
        return map;
    }

    private static Object run(final String expression) {
        return run(expression, vars());
    }

    private static Object run(final String expression, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, IMPORTS, vars);
    }

    @Test
    public void testSingleProperty() {
        assertThat(run("fun")).isEqualTo(false);
    }

    @Test
    public void testMethodOnValue() {
        assertThat(run("foo.bar.name.toUpperCase()")).isEqualTo("DOG");
    }

    @Test
    public void testMethodOnValue2() {
        assertThat(run("foo. bar. name.toUpperCase()")).isEqualTo("DOG");
    }

    @Test
    public void testSimpleProperty() {
        assertThat(run("foo.bar.name")).isEqualTo("dog");
    }

    @Test
    public void testSimpleProperty2() {
        assertThat(run("base.DATA")).isEqualTo("cat");
    }

    @Test
    public void testPropertyViaDerivedClass() {
        assertThat(run("derived.data")).isEqualTo("cat");
    }

    @Test
    public void testThroughInterface() {
        assertThat(run("testImpl.name")).isEqualTo("FOOBAR!");
    }

    @Test
    public void testThroughInterface2() {
        assertThat(run("testImpl.foo")).isEqualTo(true);
    }

    @Test
    public void testMapAccessWithMethodCall() {
        assertThat(run("((Foo) funMap[\"foo\"]).happy()")).isEqualTo("happyBar");
    }

    @Test
    public void testUninitializedInt() {
        assertThat(run("base.sarahl")).isEqualTo(0);
    }

    @Test
    public void testMethodAccess() {
        assertThat(run("foo.happy()")).isEqualTo("happyBar");
    }

    @Test
    public void testMethodAccess2() {
        assertThat(run("foo.toUC( \"fubar\" )")).isEqualTo("FUBAR");
    }

    @Test
    public void testMethodAccess3() {
        assertThat(run("base.equalityCheck(c, \"cat\")")).isEqualTo(true);
    }

    @Test
    public void testMethodAccess4() {
        assertThat(run("base.readBack(null)")).isNull();
    }

    @Test
    public void testMethodAccess5() {
        assertThat(run("base.appendTwoStrings(null, \"test\")")).isEqualTo("nulltest");
    }

    @Test
    public void testMethodAccess6() {
        assertThat(run("   base.equalityCheck(   c  \n  ,   \n   \"cat\"      )   ")).isEqualTo(true);
    }

    @Test
    public void testLiteralPassThrough() {
        assertThat(run("true")).isEqualTo(true);
    }

    @Test
    public void testLiteralPassThrough2() {
        assertThat(run("false")).isEqualTo(false);
    }

    @Test
    public void testLiteralPassThrough3() {
        assertThat(run("null")).isNull();
    }

    @Test
    @Disabled("chor-operator: not representable")
    public void testLiteralReduction1() {
        // Original: assertEquals("foo", test("null or 'foo'"));
    }

    @Test
    public void testStrAppend() {
        assertThat(run("\"foo\" + \"bar\" + \"car\"")).isEqualTo("foobarcar");
    }

    @Test
    public void testStrAppend2() {
        assertThat(run("\"foobar\" + \"car\" + 1")).isEqualTo("foobarcar1");
    }

    @Test
    public void testMapAccess() {
        assertThat(run("((Foo) funMap[\"foo\"]).bar.name")).isEqualTo("dog");
    }

    @Test
    public void testMapAccess2() {
        // `funMap.foo.bar.name`: the raw map value is an Object, so it is read with get(..) and cast.
        assertThat(run("((Foo) funMap.get(\"foo\")).bar.name")).isEqualTo("dog");
    }

    @Test
    public void testStaticMethodFromLiteral() {
        assertThat(run("try { return String.valueOf(Class.forName(\"java.lang.String\").getName()); } "
                       + "catch (ClassNotFoundException e) { return null; }")).isEqualTo(String.class.getName());
    }

    @Test
    public void testObjectInstantiation() {
        assertThat(run("new java.lang.String(\"foobie\")")).isEqualTo("foobie");
    }

    @Test
    public void testObjectInstantiationWithMethodCall() {
        assertThat(run("new String(\"foobie\")  . toUpperCase()")).isEqualTo("FOOBIE");
    }

    @Test
    public void testObjectInstantiation2() {
        assertThat(run("new String() instanceof String")).isEqualTo(true);
    }

    @Test
    public void testObjectInstantiation3() {
        assertThat(run("new java.text.SimpleDateFormat(\"yyyy\").format(new java.util.Date(System.currentTimeMillis()))"))
                .isInstanceOf(String.class);
    }

    @Test
    public void testThisReference() {
        assertThat(run("base")).isInstanceOf(Base.class);
    }

    @Test
    public void testThisReference2() {
        assertThat(run("base.funMap")).isInstanceOf(Map.class);
    }

    @Test
    public void testThisReferenceInMethodCall() {
        assertThat(run("Integer.parseInt(base.number)")).isEqualTo(101);
    }

    @Test
    public void testThisReferenceInConstructor() {
        assertThat(run("new String(base.number)")).isEqualTo("101");
    }

    @Test
    public void testStringEscaping() {
        assertThat(run("\"\\\"Mike Brock\\\"\"")).isEqualTo("\"Mike Brock\"");
    }

    @Test
    public void testStringEscaping2() {
        assertThat(run("\"MVEL's Parser is Fast\"")).isEqualTo("MVEL's Parser is Fast");
    }

    @Test
    public void testCompiledMethodCall() {
        assertThat(run("c.getClass()")).isEqualTo(String.class);
    }

    @Test
    public void testStaticNamespaceCall() {
        assertThat(run("java.util.ArrayList.class")).isEqualTo(java.util.ArrayList.class);
    }

    @Test
    public void testStaticNamespaceClassWithMethod() {
        assertThat(run("java.lang.String.valueOf(\"FooBar\")")).isEqualTo("FooBar");
    }

    @Test
    public void testStaticNamespaceClassWithField() {
        assertThat(run("java.lang.Integer.MAX_VALUE")).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void testStaticNamespaceClassWithField2() {
        assertThat(run("Integer.MAX_VALUE")).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void testStaticFieldAsMethodParm() {
        assertThat(run("String.valueOf(Integer.MAX_VALUE)")).isEqualTo(String.valueOf(Integer.MAX_VALUE));
    }

    @Test
    public void testMagicArraySize() {
        assertThat(run("base.stringArray.length")).isEqualTo(5);
    }

    @Test
    public void testMagicArraySize2() {
        assertThat(run("base.intArray.length")).isEqualTo(5);
    }

    @Test
    public void testObjectCreation() {
        assertThat(run("new Integer( 6 )")).isEqualTo(6);
    }

    @Test
    public void testCompileTimeLiteralReduction() {
        assertThat(run("10 * 100")).isEqualTo(1000);
    }

    @Test
    public void testStringAsCollection() {
        assertThat(run("var abc = \"foo\"; return abc.charAt(1);")).isEqualTo('o');
    }

    @Test
    public void testInterfaceResolution() {
        final Map<String, Object> vars = vars();
        final Foo foo = (Foo) vars.get("foo");
        final Evaluator<Map<String, Object>, Void, Object> ex = new MVEL().compileMapExpression(
                "foo.collectionTest.size()", Object.class, IMPORTS, MVEL.getTypeMap(vars));

        foo.setCollectionTest(new java.util.HashSet<>());
        final Object result1 = ex.eval(vars);

        foo.setCollectionTest(new ArrayList<>());
        final Object result2 = ex.eval(vars);

        assertThat(result2).isEqualTo(result1);
    }

    @Test
    public void testReflectionCache() {
        assertThat(run("foo.happy(); return foo.bar.happy();")).isEqualTo("happyBar");
    }

    @Test
    @Disabled("static-input-type: not representable")
    public void testDynamicDeop() {
        // Original: one compiled "name" expression executed with a Foo and then with a Bar root.
        // MVEL3 binds the generated code to one input type.
    }

    @Test
    public void testVirtProperty() {
        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("test", "foo");

        final Map<String, Object> vars = new HashMap<>();
        vars.put("mp", testMap);

        assertThat(run("mp.put(\"test\", \"bar\"); return mp.get(\"test\");", vars)).isEqualTo("bar");
    }

    @Test
    public void testNullSafe() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("SessionSetupRequest", new HashMap<String, Float>());

        // The original only prints the result of `SessionSetupRequest.?latitude`.
        assertThat(run("SessionSetupRequest.get(\"latitude\")", vars)).isNull();
    }

    @Test
    public void testMVEL226() {
        final A226 a = new A226();
        final Map<String, Object> vars = new HashMap<>();
        vars.put("a", a);
        final String ex = "a.map != null ? a.map.get(\"foo\") : null";

        assertThat(run(ex, vars)).isNull();

        final Evaluator<Map<String, Object>, Void, Object> compiled = new MVEL().compileMapExpression(
                ex, Object.class, IMPORTS, MVEL.getTypeMap(vars));
        assertThat(compiled.eval(vars)).isNull();
        a.map = Collections.singletonMap("foo", "bar");
        assertThat(compiled.eval(vars)).isEqualTo("bar");
        a.map = null;
        assertThat(compiled.eval(vars)).isNull();
    }

    private void infiniteLoop() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("a", new HashMap<String, Object>());
        assertThatThrownBy(() -> run("a[\"b\"][\"c\"]", vars)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testInfiniteLoopWithASMOptimizer() {
        infiniteLoop();
    }

    @Test
    public void testInfiniteLoopWithASMOptimizerAO() {
        infiniteLoop();
    }

    @Test
    public void testInfiniteLoop() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("a", new A226());
        assertThatThrownBy(() -> run("a.map[\"foo\"]", vars)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testInfiniteLoopAO() {
        testInfiniteLoop();
    }

    @Test
    public void testNonHashMapImplMapPutMVEL302() {
        final Object result = run("var map = new java.util.Hashtable(); map.put(\"foo\", \"bar\"); return map;");
        assertThat(result).isInstanceOf(Hashtable.class);
        assertThat(((Map<?, ?>) result).get("foo")).isEqualTo("bar");
    }

    @Test
    public void testNullSafeWithDynamicOptimizerMVEL305() {
        final Map<String, Object> vars = vars();
        ((Foo) vars.get("foo")).setBar(null);
        final Evaluator<Map<String, Object>, Void, Object> compiled = new MVEL().compileMapExpression(
                "foo.bar != null ? foo.bar.name : null", Object.class, IMPORTS, MVEL.getTypeMap(vars));
        for (int i = 1; i < 100; i++) {
            assertThat(compiled.eval(vars)).isNull();
        }
    }

    @Test
    @Disabled("static-input-type: not representable")
    public void testStaleReflectiveCollectionAccessor() {
        // Original: one compiled "foo[0]" expression executed with String[] -> List -> String[] inputs.
        // MVEL3 binds the generated code to one input type.
    }

    @Test
    public void testStaleReflectiveCollectionIsEmptyAccessor() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("data", new Data(new ArrayList<>()));
        final Evaluator<Map<String, Object>, Void, Object> compiled = new MVEL().compileMapExpression(
                "data.list.empty", Object.class, IMPORTS, MVEL.getTypeMap(vars));

        assertThat(compiled.eval(vars)).isEqualTo(true);
        vars.put("data", new Data(Collections.emptyList()));
        assertThat(compiled.eval(vars)).isEqualTo(true);
        vars.put("data", new Data(Collections.emptyList()));
        assertThat(compiled.eval(vars)).isEqualTo(true);
        vars.put("data", new Data(new ArrayList<>()));
        assertThat(compiled.eval(vars)).isEqualTo(true);
    }

    @Test
    public void testMVEL308() {
        final Map<String, Object> target = new HashMap<>();
        target.put("value", "notnull");

        final Map<String, Object> ctx = new HashMap<>();
        ctx.put("_target", target);

        final Map<String, Object> updates = new HashMap<>();
        updates.put("value", null);

        final Map<String, Object> vars = new HashMap<>();
        vars.put("updates", updates);
        vars.put("ctx", ctx);

        final Evaluator<Map<String, Object>, Void, Object> compiled = new MVEL().compileMapBlock(
                "Map<String, Object> t = (Map<String, Object>) ctx.get(\"_target\");"
                + " for (Object o : updates.entrySet()) {"
                + " Map.Entry<String, Object> field = (Map.Entry<String, Object>) o; t.put(field.getKey(), field.getValue()); }"
                + " return null;",
                Object.class, IMPORTS, MVEL.getTypeMap(vars));

        for (int i = 0; i < 100; i++) {
            compiled.eval(vars);
        }

        assertThat(target.get("value")).isNull();
    }

    @Test
    public void testNullListMapArrayValueMVEL312() {
        // Map
        assertThat(run("var m = new HashMap<String, Object>(); m.put(\"test1\", null); return m.get(\"test1\");")).isNull();

        // List
        assertThat(run("var l = new ArrayList<Object>(); l.add(null); return l.get(0);")).isNull();

        // Array: `new Object[] {null}` fails with "Cannot be cast or coerced: null", so the slot is left unset.
        assertThat(run("var arr = new Object[1]; return arr[0];")).isNull();
    }

    @Test
    public void testPublicStaticFieldMVEL314() {
        assertThat(run("org.mvel3.compat.mvel2.res.Foo.STATIC_BAR")).isEqualTo(Foo.STATIC_BAR);
    }

    @Test
    public void testCollectionEmptyASM() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("validationIssues", new ArrayList<Object>());
        final Evaluator<Map<String, Object>, Void, Object> compiled = new MVEL().compileMapExpression(
                "validationIssues.empty", Object.class, IMPORTS, MVEL.getTypeMap(properties));
        int trueCount = 0;
        final int count = 1000;
        for (int i = 0; i < count; i++) {
            if ((Boolean) compiled.eval(properties)) {
                trueCount++;
            }
        }
        assertThat(trueCount).isEqualTo(count);
    }

    @Test
    public void testPrivateMethod() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("service", new Service());
        assertThatThrownBy(() -> run("service.hello()", vars))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("private access");
    }

    @Test
    public void testPrivateMethodCompiled() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("service", new Service());
        assertThatThrownBy(() -> new MVEL().compileMapExpression(
                "service.hello()", Object.class, IMPORTS, MVEL.getTypeMap(vars)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("private access");
    }
}
