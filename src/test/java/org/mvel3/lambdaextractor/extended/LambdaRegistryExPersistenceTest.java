package org.mvel3.lambdaextractor.extended;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;
import org.mvel3.ClassManager;
import org.mvel3.javacompiler.KieMemoryCompiler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class LambdaRegistryExPersistenceTest {

    @Test
    void sameLambdaContentReusesPhysicalClassFile() throws Exception {
        LambdaRegistryEx registry = LambdaRegistryEx.INSTANCE;
        registry.resetAndRemoveAllPersistedFiles();

        Path tempDir = Files.createTempDirectory("lambda-registry-ex");
        String javaFqn = "org.mvel3.GeneratorEvaluatorEx__";
        String classSource = ""
                + "package org.mvel3;\n"
                + "public class GeneratorEvaluatorEx__ {\n"
                + "  public boolean eval(java.util.Map m) { return m != null; }\n"
                + "}\n";

        MethodDeclaration methodDecl = StaticJavaParser.parseMethodDeclaration(
                "public boolean eval(java.util.Map m) { return m != null; }");
        LambdaKeyEx key = LambdaUtilsEx.createLambdaKeyFromMethodDeclaration(methodDecl);

        // First registration + persist
        int logicalId1 = registry.getNextLogicalId();
        int physicalId1 = registry.registerLambda(logicalId1, key);
        ClassManager classManager = new ClassManager();
        Path persistedPath1 = KieMemoryCompiler
                .compileAndPersist(classManager,
                        Collections.singletonMap(javaFqn, classSource),
                        Thread.currentThread().getContextClassLoader(),
                        null,
                        tempDir)
                .get(0);
        registry.registerPhysicalPath(physicalId1, persistedPath1);

        // Second registration of the same lambda
        int logicalId2 = registry.getNextLogicalId();
        int physicalId2 = registry.registerLambda(logicalId2, key);

        assertThat(physicalId2).isEqualTo(physicalId1);
        assertThat(registry.isPersisted(physicalId2)).isTrue();
        assertThat(registry.getPhysicalPath(physicalId2)).isEqualTo(persistedPath1);
        assertThat(Files.exists(persistedPath1)).isTrue();
    }
}
