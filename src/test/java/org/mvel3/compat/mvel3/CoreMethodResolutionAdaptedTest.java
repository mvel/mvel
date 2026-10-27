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

import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.Evaluator;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.AbstractTest;
import org.mvel3.compat.mvel2.res.AStatic;
import org.mvel3.compat.mvel2.res.Bar;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.Cheese;
import org.mvel3.compat.mvel2.res.DefaultKnowledgeHelper;
import org.mvel3.compat.mvel2.res.Foo;
import org.mvel3.compat.mvel2.res.KnowledgeHelper;
import org.mvel3.compat.mvel2.res.PojoStatic;
import org.mvel3.compat.mvel2.res.RuleBaseImpl;
import org.mvel3.compat.mvel2.res.WorkingMemoryImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Porting of the method-resolution tests of CoreConfidenceTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>this-reference</b>: the MVEL2 root object is passed as a named input variable ({@code ctx},
 *       {@code base}, {@code o}, {@code bar}, {@code t}, ...); an unqualified call such as {@code method(5, 9, "x")}
 *       is written {@code o.method(5, 9, "x")}.</li>
 *   <li><b>block-syntax</b>: undeclared variables are declared with a type or {@code var}, and the
 *       result is returned explicitly with {@code return}; a block that ends in a statement gets a
 *       {@code return null;}.</li>
 *   <li><b>single-quote-string</b>: string literals use double quotes.</li>
 *   <li><b>inline-import</b>: {@code import pkg.Cls;} inside the expression is replaced by the API imports set.</li>
 *   <li><b>void-method-expression</b>: a void call as a statement followed by {@code return null;}.</li>
 *   <li><b>inner-class-binary-name</b>: {@code Outer$Inner} is written with dots.</li>
 *   <li><b>runtime-type-method-resolution</b>: the input is declared with the precise type
 *       ({@code Object m} becomes {@code Message m}); the call on a {@code PrintStream} subclass installed in
 *       {@code System.out} is replaced by {@code println}.</li>
 *   <li><b>static-import-method-call</b>: {@code is(getList(..))} is called through the class
 *       ({@code StaticMethods.is(StaticMethods.getList(..))}).</li>
 *   <li><b>naked-static-method-call</b>: {@code System.currentTimeMillis} is written with parentheses.</li>
 *   <li><b>raw-collection-element-type</b>: the list is declared with generics ({@code List<String>}).</li>
 *   <li><b>inline-array-literal</b>: {@code {'1','2'}} is written {@code new String[] {"1", "2"}}.</li>
 *   <li><b>with-block-syntax</b>: {@code new Cheese().{ type = $c.type }} is written with a {@code with} block.</li>
 *   <li>Fixtures are public static nested classes, because the generated evaluator must be able
 *       to access them.</li>
 * </ul>
 *
 * <p>Not ported (as in the MVEL2 import): the tests that only assert the result of {@code MVEL.analyze}
 * ({@code testNoArgMethodInheritance}, {@code testMethodReturningPrimitiveTypeAnalysis}, {@code testUntypedClone}),
 * the {@code ParseTools} tests ({@code testMethodOverloadMatch}, {@code testGetBestCandidateForBigDecimalArg},
 * {@code testGetBestConstructorCandidateOfBigDecimal}, {@code testHandleNumericConversionBug}) and {@code testBug4}
 * ({@code MVEL.getProperty}). The {@code ASM} variants run the same expression as their base test.
 */
public class CoreMethodResolutionAdaptedTest {

    private static final String CM = "org.mvel3.compat.mvel3.CoreMethodResolutionAdaptedTest";
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

    // ---------------------------------------------------------------- fixtures

    public static class Bean {
        private Date myDate = new Date();

        public Date getToday() {
            return new Date();
        }

        public Date getNullDate() {
            return null;
        }

        public String getNullString() {
            return null;
        }

        public Date getMyDate() {
            return myDate;
        }

        public void setMyDate(final Date myDate) {
            this.myDate = myDate;
        }
    }

    public static class Context {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
        private Bean bean;

        public Bean getBean() {
            return bean;
        }

        public void setBean(final Bean bean) {
            this.bean = bean;
        }

        public String formatDate(final Date date) {
            return date == null ? null : dateFormat.format(date);
        }

        public String formatString(final String str) {
            return str == null ? "<NULL>" : str;
        }
    }

    public static class Person {
        private String name;
        private int age;
        private String likes;

        public Person() {
        }

        public Person(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public Person(final String name, final String likes, final int age) {
            this.name = name;
            this.likes = likes;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class Address {
        private String street;

        public Address(final String street) {
            this.street = street;
        }

        public String getStreet() {
            return street;
        }
    }

    public static class Drools {
        public void insert(final Object obj) {
        }
    }

    public static class Model {
        private List latestHeadlines;

        public List getLatestHeadlines() {
            return latestHeadlines;
        }

        public void setLatestHeadlines(final List latestHeadlines) {
            this.latestHeadlines = latestHeadlines;
        }
    }

    public static class MvelContext {
        public boolean singleCalled;
        public boolean arrayCalled;

        public void methodForTest(final String string) {
            singleCalled = true;
        }

        public void methodForTest(final String[] strings) {
            arrayCalled = true;
        }
    }

    public static class StaticMethods {
        public static <T> boolean is(final List<T> arg) {
            return true;
        }

        public static boolean is(final java.util.Collection arg) {
            throw new RuntimeException("Wrong method called");
        }

        public static List<Object> getList(final Class<?> arg) {
            final ArrayList<Object> result = new ArrayList<>();
            result.add(arg);
            return result;
        }
    }

    public static class ProcessManager {
        public void startProcess(final String name, final Map<String, Object> variables) {
        }
    }

    public static class KnowledgeRuntimeHelper {
        public ProcessManager getProcessManager() {
            return new ProcessManager();
        }
    }

    public static class PetStore {
        public List getPets() {
            final List pets = new ArrayList();
            pets.add(new Dog());
            pets.add(new Cat());
            return pets;
        }
    }

    public static class Pet {
        public void run() {
        }
    }

    public static class Dog extends Pet {
    }

    public static class Cat extends Pet {
    }

    public static class A {
        public int invoke(final String s1, final String s2, final B... bs) {
            return bs.length;
        }

        public static int invokeSum(final int start, final B... bs) {
            int sum = start;
            for (final B b : bs) {
                sum += b.getValue();
            }
            return sum;
        }
    }

    public static class B {
        private int value;

        public B() {
        }

        public B(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static class MySet {
        private final Set<String> set = new HashSet<>();

        public MySet(final String... strings) {
            add(strings);
        }

        public void add(final String... strings) {
            for (final String s : strings) {
                set.add(s);
            }
        }

        public boolean contains(final String s) {
            return set.contains(s);
        }
    }

    public static class MyObj {
        public static String doSomething(final MyObj s1, final String s2) {
            return s1 + s2;
        }
    }

    public interface InterfaceA {
        InterfaceB getB();
    }

    public interface InterfaceB {
    }

    public static class ImplementationA implements InterfaceA {
        public ImplementationB getB() {
            return new ImplementationB();
        }
    }

    public static class ImplementationB implements InterfaceB {
        public int getValue() {
            return 42;
        }
    }

    public static class Overloaded {
        public int method(final int i, final int j, final String s) {
            return i + j + s.length();
        }

        public int method(final int i, final String s, final int j) {
            return i + s.length() - j;
        }

        public int method(final String s, final int i, final int j) {
            return s.length() - i - j;
        }
    }

    public static abstract class AbstractBase<T> {
        protected T foo;

        public T getFoo() {
            return foo;
        }
    }

    public static class StringConcrete extends AbstractBase<String> {
        public StringConcrete() {
            this.foo = new String();
        }
    }

    public static class ARef {
        public static int getSize(final String s) {
            return 0;
        }
    }

    public static class BRef extends ARef {
        public static int getSize(final String s) {
            return s.length();
        }
    }

    public static class O1 {
        public O2 getObj() {
            return new O2();
        }
    }

    public static class O2 extends O3 {
    }

    public abstract static class O3 {
        public String getValue() {
            return "value";
        }
    }

    public static class Thingy {
        private final String name;

        public Thingy(final String name, final String version, final Object... items) {
            this.name = name;
        }

        public Thingy(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class TestHelper {
        public static void method(final int id, final Object[] arr) {
        }

        public static void method(final Object obj1, final Object obj2) {
        }
    }

    public static class StaticClassWithStaticMethod {
        public static String getString() {
            return "hello";
        }
    }

    private static class PrivatePrintStream extends PrintStream {
        PrivatePrintStream(final OutputStream os) {
            super(os);
        }

        @Override
        public void println(final String s) {
            super.println(s);
        }
    }

    public static class Triangle {
        private final List<Map<String, Object>> deliveries = new ArrayList<>();

        public List<Map<String, Object>> getDeliveries() {
            return deliveries;
        }
    }

    // ------------------------------------------------------------------- tests

    @Test
    public void testMethodResolutionWithNullParameter() {
        // this-reference: the root Context is the named input `ctx`
        final Context ctx = new Context();
        ctx.setBean(new Bean());
        final Map<String, Object> vars = vars("ctx", ctx);

        assertThat(run("ctx.bean.today", NO_IMPORTS, vars)).isInstanceOf(Date.class);
        assertThat(run("ctx.formatDate(ctx.bean.today)", NO_IMPORTS, vars)).isNotNull();
        // calling a method with a String parameter with a null argument
        assertThat(run("ctx.formatString(ctx.bean.nullString)", NO_IMPORTS, vars)).isEqualTo("<NULL>");
        assertThat(run("ctx.bean.myDate = ctx.bean.nullDate; return ctx.bean.nullDate;", NO_IMPORTS, vars)).isNull();
        // calling a method with a Date parameter with a null argument
        assertThat(run("ctx.formatDate(ctx.bean.myDate)", NO_IMPORTS, vars)).isNull();
        assertThat(run("ctx.formatDate(ctx.bean.nullDate)", NO_IMPORTS, vars)).isNull();
    }

    @Test
    public void testDeepMethod() {
        // block-syntax + empty-operator: `== empty` becomes isEmpty(); an Integer list gets an Integer
        final Map<String, Object> vars = vars("foo", new Foo());
        assertThat(run("foo.bar.testList.add(1); return foo.bar.testList.isEmpty();", NO_IMPORTS, vars)).isEqualTo(false);
    }

    @Test
    public void testsequentialAccessorsThenMethodCall() {
        // block-syntax: return null
        final KnowledgeHelper drools = new DefaultKnowledgeHelper(new WorkingMemoryImpl(new RuleBaseImpl()));

        assertThat(run("System.out.println(drools.workingMemory); "
                + "drools.workingMemory.ruleBase.removeRule(\"org.drools.examples\", \"some rule\"); return null;",
                NO_IMPORTS, vars("drools", drools))).isNull();
    }

    @Test
    public void testCompileWithNewInsideMethodCall() {
        // block-syntax: return null
        final Map<String, Type<?>> types = new LinkedHashMap<>();
        types.put("p", Type.type(Person.class));
        types.put("drools", Type.type(Drools.class));

        compile("     p.name = \"goober\";\n     System.out.println(p.name);\n     drools.insert(new Address(\"Latona\")); return null;",
                imports(Person.class.getCanonicalName(), Address.class.getCanonicalName()), types);
    }

    @Test
    public void testStaticWithExplicitParam() {
        // single-quote-string
        assertThat(run("AStatic.Process(\"10\")", imports(AStatic.class.getCanonicalName()), new HashMap<>())).isEqualTo("10");
    }

    @Test
    public void testStaticWithExpressionParam() {
        // this-reference: the root PojoStatic is the named input `pojo`
        assertThat(run("AStatic.Process(pojo.value.getClass().getName().toString())",
                imports(AStatic.class.getCanonicalName()), vars("pojo", new PojoStatic("10")))).isEqualTo("java.lang.String");
    }

    @Test
    public void testMethodResolutionOrder() {
        // inline-array-literal + single-quote-string: Java array syntax
        final MvelContext context = new MvelContext();
        run("c.methodForTest(new String[] {\"1\", \"2\"}); c.methodForTest(\"1\"); return null;", NO_IMPORTS, vars("c", context));

        assertThat(context.arrayCalled && context.singleCalled).isTrue();
    }

    @Test
    public void testMethodScoring() {
        // static-import-method-call: called through the class; class-literal: Formatter.class
        assertThat(run("StaticMethods.is(StaticMethods.getList(java.util.Formatter.class))",
                imports(StaticMethods.class.getCanonicalName()), new HashMap<>())).isEqualTo(true);
    }

    @Test
    public void testMethodWithNegativeIntParamMVEL313() {
        // this-reference: the root Base is the named input `base`
        assertThat(run("base.ord(true,-1)", NO_IMPORTS, vars("base", new Base()))).isEqualTo(true);
    }

    @Test
    public void testDeepMethodNameResolution() {
        // block-syntax: typed declaration, because the map literal would be inferred as Map<String,String>
        assertThat(run("Map<String, Object> variables = [ \"symbol\" : \"RHT\" ]; \n"
                + "drools.getProcessManager().startProcess(\"id\", variables ); return null;",
                imports("java.util.Map"), vars("drools", new KnowledgeRuntimeHelper()))).isNull();
    }

    @Test
    public void testMethodCaching() {
        // foreach-syntax + this-reference: a typed loop over Object with a cast, over the named input `store`
        assertThat(run("for (Object pet : store.getPets()) { ((Pet) pet).run(); } return null;",
                imports(Pet.class.getCanonicalName()), vars("store", new PetStore()))).isNull();
    }

    @Test
    public void testAmbiguousGetName() {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("foo", new Foo());
        vars.put("Foo244", Foo.class);

        assertThat(run("foo.getClass().getName()", NO_IMPORTS, vars)).isEqualTo(Foo.class.getName());
        assertThat(run("Foo244.getName()", NO_IMPORTS, vars)).isEqualTo(Foo.class.getName());
    }

    @Test
    public void testVarArgs() {
        // block-syntax: an expression without the trailing `;`
        final Evaluator<Map<String, Object>, Void, Object> evaluator = compile("String.format(\"\")", NO_IMPORTS, new HashMap<>());
        assertThat(evaluator.eval(new HashMap<>())).isEqualTo("");
    }

    @Test
    public void testNakedMethodCall() {
        // naked-static-method-call + block-syntax: System.currentTimeMillis() declared with a type
        assertThat((Long) run("long tm = System.currentTimeMillis(); return tm;")).isGreaterThan(0L);

        // the naked instance call `foo.happy` works
        assertThat(run("foo.happy", NO_IMPORTS, vars("foo", new Foo()))).isEqualTo("happyBar");
    }

    @Test
    public void testInterfaceMethodCallWithSpace() {
        // void-method-expression: statement + return null
        final DefaultKnowledgeHelper helper = new DefaultKnowledgeHelper();
        final Cheese cheese = new Cheese("stilton", 15);

        run("drools.retract (cheese); return null;", NO_IMPORTS, vars("drools", helper, "cheese", cheese));

        assertThat(helper.retracted.get(0)).isSameAs(cheese);
    }

    @Test
    public void testChainedMethodCallsWithParams() {
        assertThat(run("foo.toUC(\"abcd\").equals(\"ABCD\")", NO_IMPORTS, vars("foo", new Foo()))).isEqualTo(true);
    }

    @Test
    public void testMethodCallWithSpaces() {
        // block-syntax: the trailing call is returned
        final String[] expressions = {
            "Foo f = new Foo(); f.setBoolTest( true )   ; return f.isBoolTest();",
            "Foo f = new Foo(); f . setBoolTest( true ) ; return f.isBoolTest();",
            "Foo f = new Foo(); f. setBoolTest( true )  ; return f.isBoolTest();",
            "Foo f = new Foo(); f .setBoolTest( true )  ; return f.isBoolTest();",
            "Foo f = new Foo(); f.boolTest = true   ; return f.isBoolTest();",
            "Foo f = new Foo(); f . boolTest = true ; return f.isBoolTest();",
            "Foo f = new Foo(); f. boolTest = true  ; return f.isBoolTest();",
            "Foo f = new Foo(); f .boolTest = true  ; return f.isBoolTest();"
        };

        for (final String expression : expressions) {
            assertThat(run(expression, imports(Foo.class.getCanonicalName()), new HashMap<>()))
                    .as(expression).isEqualTo(true);
        }
    }

    @Test
    public void testMethodCallWithSpacesASM() {
        testMethodCallWithSpaces();
    }

    @Test
    public void testVarArgsParams() {
        // inline-import + block-syntax; the inline array `{a, b}` is written as a variable arguments call
        final Set<String> imports = imports(AStatic.class.getCanonicalName());

        assertThat(run("AStatic.process((String) null)", imports, new HashMap<>())).isEqualTo(AStatic.process((String) null));
        assertThat(run("AStatic.process(\"hello\",\"world\")", imports, new HashMap<>())).isEqualTo("hello,world,");
        assertThat(run("AStatic.process()", imports, new HashMap<>())).isEqualTo("");
        assertThat(run("AStatic.process(null)", imports, new HashMap<>())).isEqualTo("null");

        assertThat(run("String.format(\"xxx\")")).isEqualTo("xxx");
        assertThat(run("String.format(\"%010d\", 123)")).isEqualTo(String.format("%010d", 123));
        assertThat(run("Object[] a = new Object[1]; a[0] = 123; return String.format(\"%010d\", a);"))
                .isEqualTo(String.format("%010d", 123));
        assertThat(run("int a = 123; return String.format(\"%010d\", a);")).isEqualTo(String.format("%010d", 123));
        assertThat(run("int a = 123; int b = 456; return String.format(\"%010d -- %010d\", a, b);"))
                .isEqualTo(String.format("%010d -- %010d", 123, 456));
        assertThat(run("Object[] a = new Object[2]; a[0] = 123; a[1] = 456; return String.format(\"%010d -- %010d\", a);"))
                .isEqualTo(String.format("%010d -- %010d", 123, 456));
        assertThat(run("String.format(\"%010d -- %010d\", 123, 456)")).isEqualTo(String.format("%010d -- %010d", 123, 456));
    }

    @Test
    public void testTypedVarArgsParams() {
        // inline-import: the classes are imported through the API
        final Set<String> imports = imports(A.class.getCanonicalName(), B.class.getCanonicalName());

        assertThat(run("new A().invoke(\"s1\", \"s2\")", imports, new HashMap<>())).isEqualTo(0);
        assertThat(run("A.invokeSum(3)", imports, new HashMap<>())).isEqualTo(3);
        assertThat(run("new A().invoke(\"s1\", \"s2\", new B(), new B(), new B())", imports, new HashMap<>())).isEqualTo(3);
        assertThat(run("A.invokeSum(3, new B(4), new B(5))", imports, new HashMap<>())).isEqualTo(12);
    }

    @Test
    public void testTypedVarArgsConstructor() {
        // inline-import: the class is imported through the API
        final MySet result = (MySet) run("new MySet(\"s1\", \"s2\")", imports(MySet.class.getCanonicalName()), new HashMap<>());
        assertThat(result.contains("s1")).isTrue();
        assertThat(result.contains("s2")).isTrue();
    }

    @Test
    public void testTypedVarArgsConstructorASM() {
        testTypedVarArgsConstructor();
    }

    @Test
    public void testStaticMethodsInvocationWithNullArg() {
        // inner-class-binary-name: dotted name instead of CoreConfidenceTests$MyObj
        assertThat(run(CM + ".MyObj.doSomething(null, \"abc\")")).isEqualTo(null + "abc");
    }

    @Test
    public void testStaticMethodsInvocationWithNullArgASM() {
        testStaticMethodsInvocationWithNullArg();
    }

    @Test
    public void testCovariance() {
        // this-reference: the root ImplementationA is the named input `a`
        assertThat(run("a.b.value", NO_IMPORTS, vars("a", new ImplementationA()))).isEqualTo(42);
    }

    @Test
    public void testOverloading() {
        // this-reference: the root Overloaded is the named input `o`
        final Map<String, Object> vars = vars("o", new Overloaded());

        assertThat(run("o.method(5, 9, \"x\")", NO_IMPORTS, vars)).isEqualTo(15);
        assertThat(run("o.method(5, \"x\", 9)", NO_IMPORTS, vars)).isEqualTo(-3);
        assertThat(run("o.method(\"x\", 5, 9)", NO_IMPORTS, vars)).isEqualTo(-13);
    }

    @Test
    public void testPippo() {
        assertThat(run("new String(\"b)ar\")")).isEqualTo("b)ar");
    }

    @Test
    public void testReturnTypeExtendingGeneric() {
        // this-reference: the root StringConcrete is the named input `s`
        final Map<String, Object> vars = vars("s", new StringConcrete());

        assertThat(run("s.foo.concat(\"bar\")", NO_IMPORTS, vars)).isEqualTo("bar");
        assertThat(run("s.getFoo().concat(\"bar\")", NO_IMPORTS, vars)).isEqualTo("bar");
    }

    @Test
    public void testStaticMethodInvocation() {
        final Set<String> imports = imports(ARef.class.getCanonicalName(), BRef.class.getCanonicalName());
        final Map<String, Object> vars = vars("value", "1234");

        assertThat(run("ARef.getSize(value)", imports, vars)).isEqualTo(0);
        assertThat(run("BRef.getSize(value)", imports, vars)).isEqualTo(4);
    }

    @Test
    public void testInvokeMethodInAbstractClass() {
        assertThat(run("a.getObj().getValue()", NO_IMPORTS, vars("a", new O1()))).isEqualTo("value");
    }

    @Test
    public void testInvokeVarargConstructor() {
        final Thingy result = (Thingy) run("new Thingy(name)", imports(Thingy.class.getCanonicalName()), vars("name", "test"));
        assertThat(result.getName()).isEqualTo("test");
    }

    @Test
    public void testEmptyVarargConstructor() {
        // inner-class-binary-name: dotted name
        assertThat(run("new " + CM + ".MySet()")).isNotNull();
    }

    @Test
    public void testEmptyVarargMethod() {
        // void-method-expression: statement + return null
        assertThat(run("m.add(); return null;", NO_IMPORTS, vars("m", new MySet()))).isNull();
    }

    @Test
    public void testConstructor() {
        // single-quote-string: double quotes. The five-argument Person constructor does not exist, so javac rejects it.
        assertThatThrownBy(() -> compile("TestHelper.method(new Person(\"bob\", 30), new Person(\"mark\", 40, 999, 55, 10));",
                imports(TestHelper.class.getCanonicalName(), Person.class.getCanonicalName()), new HashMap<>()))
                .hasMessageContaining("no suitable constructor");
    }

    @Test
    public void testEmptyConstructorWithSpace() {
        compile("getString( )", imports("static " + StaticClassWithStaticMethod.class.getCanonicalName() + ".getString"),
                new HashMap<>());
    }

    @Test
    public void testInlineConstructor() {
        // with-block-syntax + block-syntax: a `with` block with semicolons, and a typed declaration
        final Cheese source = new Cheese();
        source.setType("stilton");

        final Cheese cheese = (Cheese) run("Cheese cheese = new Cheese(); with (cheese) { type = $c.type; } return cheese;",
                imports(Cheese.class.getCanonicalName()), vars("$c", source));

        assertThat(cheese.getType()).isEqualTo("stilton");
    }

    @Test
    public void testContextObjMethodCall() {
        // this-reference: the root Bar is the named input `bar`
        final Bar bar = new Bar();
        bar.setName("bob");

        assertThat(run("bar.getName() == \"bob\"", NO_IMPORTS, vars("bar", bar))).isEqualTo(true);
    }

    @Test
    public void testSystemOutOnPrivateClass() {
        // block-syntax: return null
        final PrintStream originalSystemOut = System.out;
        System.setOut(new PrivatePrintStream(System.out));
        try {
            assertThat(run("System.out.println(\"Hello World\"); return null;")).isNull();
        } finally {
            System.setOut(originalSystemOut);
        }
    }

    @Test
    public void testSystemOutWithActualInstanceMethod() {
        // runtime-type-method-resolution: `System.out` is a PrintStream for MVEL3, so the call goes to println
        final PrintStream originalSystemOut = System.out;
        System.setOut(new PrivatePrintStream(System.out));
        try {
            assertThat(run("System.out.println(\"Hello World\"); return null;")).isNull();
        } finally {
            System.setOut(originalSystemOut);
        }
    }

    @Test
    public void testSystemOutPrint() {
        // single-quote-string + block-syntax
        assertThat(run("int a = 0;\r\nSystem.out.println(\"This is a test\"); return null;")).isNull();
    }

    @Test
    public void testPrimitiveSubtyping() {
        final BigDecimal result = (BigDecimal) run("java.math.BigDecimal.valueOf(100)");
        assertThat(result).hasToString("100");
    }

    @Test
    public void testSetAccessorOverloadedEqualsStrictMode() {
        // `foo.bar = 0` assigns an int to a Bar property, so javac rejects it
        assertThatThrownBy(() -> run("foo.bar = 0", NO_IMPORTS, vars("foo", new Foo()))).isInstanceOf(Exception.class);
    }

    @Test
    public void testIssue286() {
        assertThat(run("java.lang.Character.toLowerCase(name.charAt(0)) == 'a'", NO_IMPORTS, vars("name", "Adam")))
                .isEqualTo(true);
    }

    @Test
    public void testContextFieldNotFound() {
        // single-quote-string + this-reference: the root Cheese is the named input `cheese`; the property `type` is qualified
        assertThat(run("\"stilton\".equals( cheese.type )", NO_IMPORTS, vars("cheese", new Cheese()))).isEqualTo(false);
    }

    @Test
    public void testLiteralToStringWithSpace() {
        // single-quote-string
        assertThat(run("\"foo\". hashCode()")).isEqualTo("foo".hashCode());
    }

    @Test
    public void testLiteralToStringWithSpaceASM() {
        testLiteralToStringWithSpace();
    }

    @Test
    public void testPropertyVerfierShoudldNotLoopIndefinately() {
        // block-syntax: return null
        final Map<String, Type<?>> types = new LinkedHashMap<>();
        types.put("$list", Type.type(List.class));
        types.put("model", Type.type(Model.class));

        compile("\t\tmodel.latestHeadlines = $list;\n model.latestHeadlines.add( 0, (model.latestHeadlines[2]) ); return null;",
                NO_IMPORTS, types);
    }

    @Test
    public void testCompileParserContextShouldNotLoopIndefinitelyOnValidJavaExpression() {
        // runtime-type-method-resolution + block-syntax: `m` is declared with its precise type; return null
        final Map<String, Type<?>> types = new LinkedHashMap<>();
        types.put("message", Type.type(Object.class));
        types.put("m", Type.type(AbstractTest.Message.class));

        compile("System.out.println( message );\nm.setMessage( \"Goodbye cruel world\" );\n"
                        + "System.out.println(m.getStatus());\nm.setStatus( Message.GOODBYE ); return null;",
                imports(AbstractTest.Message.class.getCanonicalName()), types);
    }

    @Test
    public void testForLoopWithSpaces() {
        // raw-collection-element-type: the list is declared with generics
        final Map<String, Type<?>> types = new LinkedHashMap<>();
        types.put("strings", Type.type(List.class, "<String>"));

        final String expression = "for (   String   s : strings ) {\n  return s;\n} return null;";

        assertThat(compile(expression, NO_IMPORTS, types).eval(vars("strings", Arrays.asList("test")))).isEqualTo("test");
    }

    @Test
    public void testNestedClassWithNestedGenericsOnNakedMethod() {
        // this-reference: the root Triangle is the named input `t`
        final Map<String, Object> vars = vars("t", new Triangle());

        assertThat(run("t.deliveries.size", NO_IMPORTS, vars)).isEqualTo(0);
        assertThat(run("t.deliveries.size == 0", NO_IMPORTS, vars)).isEqualTo(true);
    }
}
