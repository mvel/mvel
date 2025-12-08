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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mvel3.lambdaextractor.LambdaRegistry.DEFAULT_PERSISTENCE_PATH;

public class MVELPersistenceTest {

    @Before
    public void cleanRegistryAndDir() {
        LambdaRegistry.INSTANCE.resetAndRemoveAllPersistedFiles();
    }

    @Test
    public void sameMapExpressionReusesPhysicalClassFile() throws Exception {
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

    private List<Path> listClassFiles() throws Exception {
        try (Stream<Path> walk = Files.walk(DEFAULT_PERSISTENCE_PATH)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .sorted()
                    .collect(Collectors.toList());
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
}
