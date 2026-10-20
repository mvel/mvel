package org.mvel3.compat.mvel2;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Input types and class imports used by the arithmetic test adapter. */
public final class TestCompilerContext {
    private final Map<String, Class<?>> inputs = new LinkedHashMap<>();
    private final Set<Class<?>> imports = new LinkedHashSet<>();
    private final Set<String> packageImports = new LinkedHashSet<>();

    public TestCompilerContext() {
    }

    public TestCompilerContext(final TestCompilerContext configuration) {
        imports.addAll(configuration.imports);
        packageImports.addAll(configuration.packageImports);
    }

    public static TestCompilerContext create() {
        return new TestCompilerContext();
    }

    public TestCompilerContext withInput(final String name, final Class<?> type) {
        addInput(name, type);
        return this;
    }

    public void addInput(final String name, final Class<?> type) {
        inputs.put(name, type);
    }

    public void addImport(final Class<?> type) {
        imports.add(type);
    }

    /** Adds an on-demand import ({@code pkg.*}), the MVEL3 counterpart of MVEL2's {@code addPackageImport}. */
    public void addPackageImport(final String packageName) {
        packageImports.add(packageName);
    }

    Map<String, Class<?>> getInputs() {
        return inputs;
    }

    Set<Class<?>> getImports() {
        return imports;
    }

    Set<String> getPackageImports() {
        return packageImports;
    }
}
