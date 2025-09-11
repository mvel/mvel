package org.mvel3;

import org.mvel3.transpiler.context.Declaration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CompilerParamtersBuilder<T, K, R> {

    private static final Declaration[] EMPTY_VARS  = new Declaration[0];

    public static final String       CONTEXT_NAME = "__context";
    private              ClassLoader classLoader;

    private        ClassManager  classManager;

    private        Set<String>   imports       = Collections.emptySet();

    private        Set<String>   staticImports = Collections.emptySet();

    private ContextInfoBuilder<T> variableInfo;

    public static Declaration VOID_DECLARATION = Declaration.of("__void", Void.class);

    private Declaration rootDeclaration = VOID_DECLARATION;

    private int rootVarIndex = 0;

    private Type<R> outType;

    private String expression;

    private String[] outVars = new String[0];

    private String generatedClassName = "GeneratorEvaluaor__";

    private String generatedMethodName = "eval";

    private String generatedSuperName;

    public static <T, K, R> CompilerParamtersBuilder<T, K, R> create() {
        CompilerParamtersBuilder builder = new CompilerParamtersBuilder<>();
        builder.outType = Type.type(Void.class); // default no return
        return builder;
    }

    public static <T, K, R> CompilerParamtersBuilder create(CompilerParamtersBuilder<T, K, R> template) {
        CompilerParamtersBuilder<T, K, R> builder = create();
        builder.classLoader   = template.classLoader;
        builder.classManager  = template.classManager;
        builder.imports       = new HashSet<>(template.imports);
        builder.staticImports = new HashSet<>(template.staticImports);
        builder.variableInfo    = (template.variableInfo != null) ? template.variableInfo.clone() : null;
        builder.rootDeclaration =  template.rootDeclaration;
        builder.rootVarIndex    =  template.rootVarIndex;
        builder.outType         = template.outType;
        builder.generatedClassName = template.generatedClassName;
        builder.generatedMethodName = template.generatedMethodName;

        return builder;
    }

    public CompilerParamtersBuilder() {
        setClassLoader(CompilerParamtersBuilder.class.getClassLoader());
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public CompilerParamtersBuilder<T, K, R> setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public ClassManager getClassManager() {
        return classManager;
    }

    public CompilerParamtersBuilder<T, K, R> setClassManager(ClassManager classManager) {
        this.classManager = classManager;
        return this;
    }

    public Set<String> getImports() {
        return imports;
    }

    public CompilerParamtersBuilder<T, K, R> setImports(Set<String> imports) {
        this.imports = imports;
        return this;
    }

    public void addImport(String importName) {
        if (imports == Collections.<String>emptySet()) {
            imports = new HashSet<>();
        }
        imports.add(importName);
    }

    public void addStaticImport(String importName) {
        if (staticImports == Collections.<String>emptySet()) {
            staticImports = new HashSet<>();
        }
        staticImports.add(importName);
    }

    public Set<String> getStaticImports() {
        return staticImports;
    }

    public CompilerParamtersBuilder<T, K, R> setStaticImports(Set<String> staticImports) {
        this.staticImports = staticImports;
        return this;
    }

    public ContextInfoBuilder<T> getVariableInfo() {
        return variableInfo;
    }

    public CompilerParamtersBuilder<T, K, R> setVariableInfo(ContextInfoBuilder<T> variableInfo) {
        this.variableInfo = variableInfo;
        return this;
    }

    public Type<R> getOutType() {
        return outType;
    }

    public CompilerParamtersBuilder<T, K, R> setOutType(Type<R> outType) {
        if (outType.getClazz().isPrimitive()) {
            throw new RuntimeException("Return type must match generics and cannot be a primiive");
        }
        this.outType = outType;
        return this;
    }

    public String getExpression() {
        return expression;
    }

    public CompilerParamtersBuilder setExpression(String expression) {
        this.expression = expression;
        return this;
    }

    /**
     * Sets a root Declaration, and will inject the declaration name into the
     * variable info, if the name does not match the context name and the context
     * type is a Map.
     * @return
     */
    public Declaration<K> getRootDeclaration() {
        return rootDeclaration;
    }

    public CompilerParamtersBuilder<T, K, R> setRootDeclaration(Declaration<K> rootDeclaration) {
        this.rootDeclaration = rootDeclaration;
        return this;
    }

    public CompilerParamtersBuilder<T, K, R> setRootDeclaration(Type type) {
        setRootDeclaration(Declaration.of(CONTEXT_NAME, type));
        return this;
    }

    public String[] getOutVars() {
        return outVars;
    }

    public CompilerParamtersBuilder<T, K, R> setOutVars(String... outVars) {
        this.outVars = outVars;
        return this;
    }

    public void addDeclaration(String name, Class clazz) {
        variableInfo.addDeclaration(Declaration.of(name, clazz));
    }

    public void addDeclaration(String name, Class clazz, String generics) {
        variableInfo.addDeclaration(Declaration.of(name, clazz, generics));
    }

    public String getGeneratedClassName() {
        return generatedClassName;
    }

    public void setGeneratedClassName(String generatedClassName) {
        this.generatedClassName = generatedClassName;
    }

    public String getGeneratedMethodName() {
        return generatedMethodName;
    }

    public void setGeneratedMethodName(String generatedMethodName) {
        this.generatedMethodName = generatedMethodName;
    }

    public String getGeneratedSuperName() {
        return generatedSuperName;
    }

    public void setGeneratedSuperName(String generatedSuperName) {
        this.generatedSuperName = generatedSuperName;
    }

    public CompilerParamters<T, K, R> build() {
        // Either the root and context vars are the same, and no context variables.
        // Or the root variable must be a

        if (rootDeclaration == VOID_DECLARATION && variableInfo == null) {
            throw new IllegalStateException("Both root and context declarations cannot be empty");
        }

        if ( rootDeclaration != VOID_DECLARATION &&
             (variableInfo != null && !rootDeclaration.equals(variableInfo.getDeclaration())) &&
             !variableInfo.getVars().contains(rootDeclaration)) {
            throw new IllegalStateException("Using a root declaration requires matching context declaration with no additional variables. " +
                                            "Or the context must have  matching variable name' root "
                                            + rootDeclaration + " context " + variableInfo);
        }

        if (variableInfo == null) {
            // root cannot be null, so set to same
            variableInfo =  ContextInfoBuilder.create(rootDeclaration);
        }

        CompilerParamters<T, K, R> info = new CompilerParamters<>(classLoader, classManager, imports, staticImports, outType,
                                                                  variableInfo.build(), rootDeclaration, rootVarIndex, expression,
                                                                  generatedClassName, generatedMethodName, generatedSuperName);

        return info;
    }

}
