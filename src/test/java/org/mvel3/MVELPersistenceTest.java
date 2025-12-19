package org.mvel3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mvel3.lambdaextractor.LambdaRegistry;
import org.mvel3.transpiler.context.Declaration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mvel3.lambdaextractor.LambdaRegistry.DEFAULT_PERSISTENCE_PATH;

public class MVELPersistenceTest {

    @Before
    public void cleanRegistryAndDir() {
        LambdaRegistry.INSTANCE.resetAndRemoveAllPersistedFiles();
    }

    @Test
    public void sameMapExpressionReusesPhysicalClassFile() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("foo", Type.type(Foo.class));
        types.put("bar", Type.type(Bar.class));

        Map<String, Object> vars = new HashMap<>();
        Foo foo = new Foo();
        foo.setName("a");
        vars.put("foo", foo);
        Bar bar = new Bar();
        bar.setName("b");
        vars.put("bar", bar);

        MVEL mvel = new MVEL();
        mvel.compileMapExpression("foo.getName() + bar.getName()", String.class, getImports(), types);

        List<Path> firstFiles = listClassFiles();

        mvel.compileMapExpression("foo.getName() + bar.getName()", String.class, getImports(), types);

        List<Path> secondFiles = listClassFiles();

        assertThat(firstFiles).hasSize(1);
        assertThat(secondFiles).containsExactlyElementsOf(firstFiles);
        assertThat(Files.exists(firstFiles.get(0))).isTrue();
    }

    private List<Path> listClassFiles() {
        try (Stream<Path> walk = Files.walk(DEFAULT_PERSISTENCE_PATH)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static java.util.Set<String> getImports() {
        return java.util.Collections.emptySet();
    }

    public static class Foo {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Bar {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void samePojoExpressionReusesPhysicalClassFile() {
        CompilerParameters<MyPerson, Void, Boolean> evalInfo1 = MVEL.<MyPerson>pojo(MyPerson.class,
                                                                                   Declaration.of("age", int.class)
                )
                .<Boolean>out(Boolean.class)
                .expression("age > 20")
                .imports(getImports()).classManager(new ClassManager())
                .build();

        MVEL mvel1 = new MVEL();
        mvel1.compilePojoEvaluator(evalInfo1);

        List<Path> firstFiles = listClassFiles();

        CompilerParameters<MyPerson, Void, Boolean> evalInfo2 = MVEL.<MyPerson>pojo(MyPerson.class,
                                                                                   Declaration.of("age", int.class)
                )
                .<Boolean>out(Boolean.class)
                .expression("age > 20")
                .imports(getImports()).classManager(new ClassManager())
                .build();

        MVEL mvel2 = new MVEL();
        mvel2.compilePojoEvaluator(evalInfo2);

        List<Path> secondFiles = listClassFiles();

        assertThat(firstFiles).hasSize(1);
        assertThat(secondFiles).containsExactlyElementsOf(firstFiles);
        assertThat(Files.exists(firstFiles.get(0))).isTrue();
    }

    @Test
    public void samePojoExpressionWithReusesPhysicalClassFile_SubClass() {
        CompilerParameters<MyPerson, Void, Boolean> evalInfo1 = MVEL.<MyPerson>pojo(MyPerson.class,
                                                                                    Declaration.of("age", int.class)
                )
                .<Boolean>out(Boolean.class)
                .expression("age > 20")
                .imports(getImports()).classManager(new ClassManager())
                .build();

        MVEL mvel1 = new MVEL();
        mvel1.compilePojoEvaluator(evalInfo1);

        List<Path> firstFiles = listClassFiles();

        CompilerParameters<MyPerson, Void, Boolean> evalInfo2 = MVEL.<MyPerson>pojo(MyPersonSub.class,
                                                                                    Declaration.of("age", int.class)
                )
                .<Boolean>out(Boolean.class)
                .expression("age > 20")
                .imports(getImports()).classManager(new ClassManager())
                .build();

        MVEL mvel2 = new MVEL();
        mvel2.compilePojoEvaluator(evalInfo2);

        List<Path> secondFiles = listClassFiles();

        assertThat(firstFiles).hasSize(1);
        assertThat(secondFiles).containsExactlyElementsOf(firstFiles);
        assertThat(Files.exists(firstFiles.get(0))).isTrue();
    }

    public static class MyPerson {

        private String name;
        private int age;

        public MyPerson(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }

    public static class MyPersonSub extends MyPerson {

        private String petName;

        public MyPersonSub(String name, int age, String petName) {
            super(name, age);
            this.petName = petName;
        }

        public String getPetName() {
            return petName;
        }
    }
}
