package org.mvel3.lambdaextractor;

import java.nio.file.Path;

/**
 * Lambda runtime configuration. Sourced from system properties at
 * {@link LambdaRuntime#getInstance()} time.
 */
public record RuntimeConfig(boolean persistenceEnabled, Path persistenceRoot,
                            Path registryFile, boolean resetOnTestStartup) {

    public static RuntimeConfig fromSystemProperties() {
        boolean persistenceEnabled = Boolean.parseBoolean(
                System.getProperty("mvel3.compiler.lambda.persistence", "true"));
        Path persistenceRoot = Path.of(
                System.getProperty("mvel3.compiler.lambda.persistence.path", "target/generated-classes/mvel"));
        Path registryFile = Path.of(System.getProperty("mvel3.compiler.lambda.registry.file",
                persistenceRoot.resolve("lambda-registry.dat").toString()));
        boolean resetOnTestStartup = Boolean.getBoolean("mvel3.compiler.lambda.resetOnTestStartup");
        return new RuntimeConfig(persistenceEnabled, persistenceRoot, registryFile, resetOnTestStartup);
    }
}
