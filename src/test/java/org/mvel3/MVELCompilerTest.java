package org.mvel3;

import org.junit.Test;
import org.mvel3.EvaluatorBuilder.ContextInfoBuilder;
import org.mvel3.EvaluatorBuilder.EvaluatorInfo;
import org.mvel3.transpiler.context.Declaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class MVELCompilerTest {

    public static class ContextCamelCase {
        private Foo foo;
        private Bar bar;

        private List<Foo> foos;

        public ContextCamelCase(Foo foo, Bar bar, Foo... foos) {
            this.foo = foo;
            this.bar = bar;
            this.foos = new ArrayList<>();
            this.foos.addAll(Arrays.asList(foos));
        }

        public Foo getFoo() {
            return foo;
        }

        public Bar getBar() {
            return bar;
        }

        public List<Foo> getFoos() {
            return foos;
        }
    }

    public static class ContextRecord {
        private Foo foo;
        private Bar bar;

        private List<Foo> foos;

        public ContextRecord(Foo foo, Bar bar, Foo... foos) {
            this.foo = foo;
            this.bar = bar;
            this.foos = new ArrayList<>();
            this.foos.addAll(Arrays.asList(foos));
        }

        public Foo foo() {
            return foo;
        }

        public Bar bar() {
            return bar;
        }

        public List<Foo> foos() {
            return foos;
        }
    }

    public static class ContextMixed {
        private Foo foo;
        private Bar bar;

        private List<Foo> foos;

        public ContextMixed(Foo foo, Bar bar, Foo... foos) {
            this.foo = foo;
            this.bar = bar;
            this.foos = new ArrayList<>();
            this.foos.addAll(Arrays.asList(foos));
        }

        public Foo foo() {
            return foo;
        }

        public Bar getBar() {
            return bar;
        }

        public List<Foo> foos() {
            return foos;
        }
    }

    public static class ContextWithInts {
        private int a;
        private int b;
        private int c;
        private int d;

        public ContextWithInts() {

        }

        public ContextWithInts(int a, int b, int c, int d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        public int getB() {
            return b;
        }

        public void setB(int b) {
            this.b = b;
        }

        public int getC() {
            return c;
        }

        public void setC(int c) {
            this.c = c;
        }

        public int getD() {
            return d;
        }

        public void setD(int d) {
            this.d = d;
        }
    }

    @Test
    public void testMapEvaluator() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("foo", Type.type(Foo.class));
        types.put("bar", Type.type(Bar.class));

        Map<String, Object> vars = new HashMap<>();
        Foo foo = new Foo();
        foo.setName("xxx");
        vars.put("foo", foo);

        Bar bar = new Bar();
        bar.setName("yyy");
        vars.put("bar", bar);

        MVEL mvel = new MVEL();
        Evaluator<Map<String, Object>, Void, String>  evaluator = mvel.compileMapEvaluator("foo.getName() + bar.getName()", String.class, getImports(), types);
        assertThat(evaluator.eval(vars)).isEqualTo("xxxyyy");
    }

    @Test
    public void testMapEvaluatorWithGenerics() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("foos", Type.type(List.class, "<Foo>"));

        Foo foo1 = new Foo();
        foo1.setName("foo1");

        Foo foo2 = new Foo();
        foo2.setName("foo2");

        List<Foo> foos = new ArrayList<>();
        foos.add(foo1);
        foos.add(foo2);

        Map<String, Object> vars = new HashMap<>();
        vars.put("foos", foos);

        MVEL mvel = new MVEL();
        Evaluator<Map<String, Object>, Void, String> evaluator = mvel.compileMapEvaluator("foos[0].name + foos[1].name", String.class, getImports(), types);
        assertThat(evaluator.eval(vars)).isEqualTo("foo1foo2");
    }

    @Test
    public void testMapEvaluatorReturns() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("a", Type.type(int.class));
        types.put("b", Type.type(int.class));
        types.put("c", Type.type(int.class));
        types.put("d", Type.type(int.class));

        Map<String, Integer> vars = new HashMap<>();
        vars.put("a", 1);
        vars.put("b", 2);
        vars.put("c", 3);
        vars.put("d", -1);

        MVEL mvel = new MVEL();
        Evaluator<Map<String, Integer>, Void, Integer> evaluator = mvel.compileMapEvaluator("a = 4; b = 5; c = 6; return d = a + b + c;", Integer.class, getImports(), types);
        assertThat((int) evaluator.eval(vars)).isEqualTo(15);

        assertThat(vars.get("a")).isEqualTo(4);
        assertThat(vars.get("b")).isEqualTo(5);
        assertThat(vars.get("c")).isEqualTo(6);
        assertThat(vars.get("d")).isEqualTo(15);
    }

    // @TODO this should assign back to the context (mdp)
    @Test
    public void testMapEvalutorInputs() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("a", Type.type(int.class));
        types.put("b", Type.type(int.class));
        //types.put("d", int.class);

        Map<String, Integer> vars = new HashMap<>();
        vars.put("a", 1);
        vars.put("b", 2);

        MVEL mvel = new MVEL();
        Evaluator<Map<String, Integer>, Void, Integer> evaluator = mvel.compileMapEvaluator("a = 4; b = 5; int c = 6; int d = a + b + c; return d;", Integer.class, getImports(), types);
        assertThat(evaluator.eval(vars)).isEqualTo(15);

        assertThat(vars.get("a")).isEqualTo(4);
        assertThat(vars.get("b")).isEqualTo(5);
    }

    @Test
    public void testListEvaluator() {
        Declaration[] declrs = new Declaration[] {new Declaration("foo", Foo.class),
                                                 new Declaration("bar", Bar.class)};

        Foo foo = new Foo();
        foo.setName("xxx");

        Bar bar = new Bar();
        bar.setName("yyy");

        MVEL mvel = new MVEL();
        Evaluator<List<Object>, Void, String> evaluator = mvel.compileListEvaluator("return foo.getName() + bar.getName();", String.class, getImports(), declrs);

        assertThat(evaluator.eval(List.of(foo, bar))).isEqualTo("xxxyyy");
    }

    @Test
    public void testListEvaluatorWithGenerics() {
        Declaration[] types = new Declaration[] {new Declaration("foos", List.class, "<Foo>")};

        Foo foo1 = new Foo();
        foo1.setName("foo1");

        Foo foo2 = new Foo();
        foo2.setName("foo2");

        List<Foo> foos = new ArrayList<>();
        foos.add(foo1);
        foos.add(foo2);

        MVEL mvel = new MVEL();
        Evaluator<List<Object>, Void, String> evaluator = mvel.compileListEvaluator("foos[0].name", String.class, getImports(), types);
        assertThat(evaluator.eval(List.of(foos))).isEqualTo("foo1");
    }

    @Test
    public void testListEvaluatorReturns() {
        Declaration[] types = new Declaration[] {
            new Declaration("a", int.class),
            new Declaration("b", int.class),
            new Declaration("c", int.class),
            new Declaration("d", int.class)
        };

        List<Object> vars = Arrays.asList(1, 2, 3, -1);

        MVEL mvel = new MVEL();
        Evaluator<List<Object>, Void, Integer> evaluator = mvel.compileListEvaluator("a = 4; b = 5; c = 6; return d = a + b + c;", Integer.class, getImports(), types);
        assertThat(evaluator.eval(vars)).isEqualTo(15);

        assertThat(vars.get(0)).isEqualTo(4);
        assertThat(vars.get(1)).isEqualTo(5);
        assertThat(vars.get(2)).isEqualTo(6);
        assertThat(vars.get(3)).isEqualTo(15);
    }

//    @Test
//    public void testRootObjectWithMapEvaluator() {
//        Foo foo1 = new Foo();
//        foo1.setName("xxx");
//
//        Foo foo2 = new Foo();
//        foo2.setName("xxx");
//
//        Map<String, Foo> foos = new HashMap<>();
//        foos.put("foo1", foo1);
//        foos.put("foo2", foo2);
//
//        RootObjectValues evalValues = RootObjectValuesBuilder.create()
//                                                             .imports(getImports())
//                                                             .expression("foo1.getName() + foo2.getName()")
//                                                             .rootClass(Map.class)
//                                                             .rootGenerics("<String, Foo>")
//                                                             .vars("foo1", "foo2")
//                                                             .outClass(String.class)
//                                                             .build();
//
//        MVEL mvel = new MVEL();
//        RootObjectEvaluator<Map<String, Foo>, String> evaluator = mvel.compileRootObjectEvaluator(evalValues);
//
//        assertThat(evaluator.eval(foos)).isEqualTo("xxxyyy");
//    }

    @Test
    public void testPojoEvalutorInputs() {
        ContextWithInts context = new ContextWithInts();
        context.setA(1);
        context.setB(1);

        EvaluatorInfo<ContextWithInts, Void, Integer> evalInfo = EvaluatorBuilder
                                                                         .create()
                                                                         .setImports(getImports())
                                                                         .setExpression("a = 4; b = 5; int c = 6; int d = a + b + c; return d;")
                                                                         .setVariableInfo(ContextInfoBuilder.create(Type.type(ContextWithInts.class))
                                                                                                            .setVars(Declaration.of("a", int.class),
                                                                                                                     Declaration.of("b", int.class)))
                                                                         .setOutType(Type.type(Integer.class))
                                                                         .build();

        MVEL mvel = new MVEL();
        Evaluator<ContextWithInts, Void, Integer> evaluator = mvel.compilePojoEvaluator(evalInfo);
        assertThat(evaluator.eval(context)).isEqualTo(15);

        assertThat(context.getA()).isEqualTo(4);
        assertThat(context.getB()).isEqualTo(5);
    }

    @Test
    public void testPojoContextCamelCaseEvaluator() {
        Foo foo = new Foo();
        foo.setName("xxx");

        Bar bar = new Bar();
        bar.setName("yyy");

        EvaluatorInfo<ContextCamelCase, Void, String> evalInfo = EvaluatorBuilder
                .create()
                .setImports(getImports())
                .setExpression("{ return foo.getName() + bar.getName(); }")
                .setVariableInfo(ContextInfoBuilder.create(Type.type(ContextCamelCase.class))
                                                   .setVars(Declaration.of("foo", Foo.class),
                                                            Declaration.of("bar", Bar.class)))
                .setOutType(Type.type(String.class))
                .build();

//        MVEL mvel = new MVEL();
//        PojoEvaluator<ContextCamelCase, String> evaluator = mvel.compilePojoEvaluator(evalValues);

        MVEL mvel = new MVEL();
        Evaluator<ContextCamelCase, Void, String> evaluator = mvel.compile(evalInfo);

        ContextCamelCase context = new ContextCamelCase(foo, bar);
        assertThat(evaluator.eval(context)).isEqualTo("xxxyyy");
    }

    @Test
    public void testPojoContextRecordEvaluator() {
        Foo foo = new Foo();
        foo.setName("xxx");

        Bar bar = new Bar();
        bar.setName("yyy");

        EvaluatorInfo<ContextRecord, Void, String> evalValues = EvaluatorBuilder.create().setImports(getImports())
                                                   .setExpression("{ return foo.getName() + bar.getName(); }")
                                                   .setVariableInfo(ContextInfoBuilder.create(Type.type(ContextRecord.class))
                                                                                      .setVars(Declaration.of("foo", Foo.class),
                                                                                               Declaration.of("bar", Bar.class)))
                                                   .setOutType(Type.type(String.class))
                                                   .build();

        MVEL mvel = new MVEL();
        Evaluator<ContextRecord, Void, String> evaluator = mvel.compilePojoEvaluator(evalValues);

        ContextRecord context = new ContextRecord(foo, bar);
        assertThat(evaluator.eval(context)).isEqualTo("xxxyyy");
    }

    @Test
    public void testPojoContextRecordEvaluatorWithGenerics() {
        Foo foo1 = new Foo();
        foo1.setName("foo1");

        Foo foo2 = new Foo();
        foo2.setName("foo2");

        EvaluatorInfo<ContextRecord, Void, String> evalValues = EvaluatorBuilder.create().setImports(getImports())
                                                                                .setExpression("{ return foos[0].name + foos[1].name; }")
                                                                                .setVariableInfo(ContextInfoBuilder.create(Type.type(ContextRecord.class))
                                                                                                                   .setVars(Declaration.of("foos", Type.type(List.class, "<" + Foo.class.getCanonicalName() + ">"))))
                                                                                .setOutType(Type.type(String.class))
                                                                                .build();

        MVEL mvel = new MVEL();
        Evaluator<ContextRecord, Void, String> evaluator = mvel.compilePojoEvaluator(evalValues);

        ContextRecord context = new ContextRecord(null, null, foo1, foo2);
        assertThat(evaluator.eval(context)).isEqualTo("foo1foo2");
    }

    @Test
    public void testPojoContextMixed() {
        Foo foo = new Foo();
        foo.setName("xxx");

        Bar bar = new Bar();
        bar.setName("yyy");

//        ContextInfo evalValues = ContextInfoBuilder.create()
//                                                   .imports(getImports())
//                                                   .expression("foo.getName() + bar.getName()")
//                                                   .contextClass(ContextMixed.class)
//                                                   .vars("foo", "bar")
//                                                   .outClass(String.class)
//                                                   .build();
//
//        EvaluatorInfo<ContextMixed, Void, String> evalValues = EvaluatorBuilder.create().setImports(getImports())
//                                                                               .setExpression("foos[0].name + foos[1].name")
//                                                                               .setVariableInfo(ContextInfoBuilder.create(Type.type(ContextMixed.class))
//                                                                                                                   .setVars(Declaration.of("foos", Type.type(List.class, "<" + Foo.class.getCanonicalName() + ">"))))
//                                                                               .setOutType(Type.type(String.class))
//                                                                               .build();

        EvaluatorInfo<ContextMixed, Void, String> evalValues = EvaluatorBuilder.create().setImports(getImports())
                                                                               .setExpression("{ return foo.getName() + bar.getName(); }")
                                                                               .setVariableInfo(ContextInfoBuilder.create(Type.type(ContextMixed.class))
                                                                                                                  .setVars(Declaration.of("foo", Foo.class),
                                                                                                                           Declaration.of("bar", Bar.class)))
                                                                               .setOutType(Type.type(String.class))
                                                                               .build();

        MVEL mvel = new MVEL();
        Evaluator<ContextMixed, Void, String> evaluator = mvel.compilePojoEvaluator(evalValues);

        ContextMixed context = new ContextMixed(foo, bar);
        assertThat(evaluator.eval(context)).isEqualTo("xxxyyy");
    }

    @Test
    public void testPojoEvaluatorReturns() {
//        ContextWithInts context = new ContextWithInts(1, 2, 3, -4);
//
//
//        MVEL mvel = new MVEL();
//        PojoEvaluator<ContextWithInts, Integer> evaluator = mvel.compilePojoEvaluator("a = 4; b = 5; c = 6; d = a + b + c;",
//                                                                                      ContextWithInts.class, Integer.class,
//                                                                                      "a", "d");
//        assertThat((int) evaluator.eval(context)).isEqualTo(15);
//
//        assertThat(context.getA()).isEqualTo(4); // updated
//        assertThat(context.getB()).isEqualTo(2); // not updated
//        assertThat(context.getC()).isEqualTo(3); // not updated
//        assertThat(context.getD()).isEqualTo(15); // updated
    }


    public static Set<String> getImports() {

        Set<String> imports = new HashSet<>();
        imports.add("java.util.List");
        imports.add("java.util.ArrayList");
        imports.add("java.util.HashMap");
        imports.add("java.util.Map");
        imports.add("java.math.BigDecimal");
        imports.add(org.mvel3.Address.class.getCanonicalName());
        imports.add(Foo.class.getCanonicalName());

        return imports;
    }
}
