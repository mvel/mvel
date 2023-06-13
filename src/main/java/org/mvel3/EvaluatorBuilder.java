package org.mvel3;

import org.mvel3.transpiler.context.Declaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EvaluatorBuilder<T, K, R> {

    private static final Declaration[] EMPTY_VARS  = new Declaration[0];

    public static final String       CONTEXT_NAME = "__context";
    private              ClassLoader classLoader;

    private        ClassManager  classManager;

    private        Set<String>   imports       = Collections.emptySet();

    private        Set<String>   staticImports = Collections.emptySet();

    private ContextInfoBuilder<T> variableInfo;

    public static Declaration VOID_DECLARATION = Declaration.of(CONTEXT_NAME, Void.class);
    private Declaration rootDeclaration = VOID_DECLARATION;

    private int rootVarIndex = 0;

    private Type<R> outType;

    private String expression;

    private String[] outVars = new String[0];

    public static <T, K, R> EvaluatorBuilder<T, K, R> create() {
        EvaluatorBuilder builder = new EvaluatorBuilder<>();
        builder.outType = Type.type(Void.class); // default no return
        return builder;
    }

    public static <T, K, R> EvaluatorBuilder create(EvaluatorBuilder<T, K, R> template) {
        EvaluatorBuilder<T, K, R> builder = create();
        builder.classLoader   = template.classLoader;
        builder.classManager  = template.classManager;
        builder.imports       = new HashSet<>(template.imports);
        builder.staticImports = new HashSet<>(template.staticImports);
        builder.variableInfo    = (template.variableInfo != null) ? template.variableInfo.clone() : null;
        builder.rootDeclaration =  template.rootDeclaration;
        builder.rootVarIndex    =  template.rootVarIndex;
        builder.outType         = template.outType;

        return builder;
    }

    public EvaluatorBuilder() {
        setClassLoader(EvaluatorBuilder.class.getClassLoader());
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public EvaluatorBuilder<T, K, R> setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public ClassManager getClassManager() {
        return classManager;
    }

    public EvaluatorBuilder<T, K, R> setClassManager(ClassManager classManager) {
        this.classManager = classManager;
        return this;
    }

    public Set<String> getImports() {
        return imports;
    }

    public EvaluatorBuilder<T, K, R> setImports(Set<String> imports) {
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

    public EvaluatorBuilder<T, K, R> setStaticImports(Set<String> staticImports) {
        this.staticImports = staticImports;
        return this;
    }

    public ContextInfoBuilder<T> getVariableInfo() {
        return variableInfo;
    }

    public EvaluatorBuilder<T, K, R> setVariableInfo(ContextInfoBuilder<T> variableInfo) {
        this.variableInfo = variableInfo;
        return this;
    }

    public Type<R> getOutType() {
        return outType;
    }

    public EvaluatorBuilder<T, K, R> setOutType(Type<R> outType) {
        if (outType.getClazz().isPrimitive()) {
            throw new RuntimeException("Return type must match generics and cannot be a primiive");
        }
        this.outType = outType;
        return this;
    }

    public String getExpression() {
        return expression;
    }

    public EvaluatorBuilder setExpression(String expression) {
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

    public EvaluatorBuilder<T, K, R> setRootDeclaration(Declaration<K> rootDeclaration) {
        this.rootDeclaration = rootDeclaration;
        return this;
    }

    public EvaluatorBuilder<T, K, R> setRootDeclaration(Type type) {
        setRootDeclaration(Declaration.of(CONTEXT_NAME, type));
        return this;
    }

    public String[] getOutVars() {
        return outVars;
    }

    public EvaluatorBuilder<T, K, R>  setOutVars(String... outVars) {
        this.outVars = outVars;
        return this;
    }

    public void addDeclaration(String name, Class clazz) {
        variableInfo.addDeclaration(Declaration.of(name, clazz));
    }

    public void addDeclaration(String name, Class clazz, String generics) {
        variableInfo.addDeclaration(Declaration.of(name, clazz, generics));
    }

    public EvaluatorInfo<T, K, R> build() {
        // Either the root and context vars are the same, and no context variables.
        // Or the root variable must be a

        if (rootDeclaration == VOID_DECLARATION && variableInfo == null) {
            throw new IllegalStateException("Both root and context declarations cannot be empty");
        }

        if ( rootDeclaration != VOID_DECLARATION &&
             (variableInfo != null && !rootDeclaration.equals(variableInfo.declaration)) &&
             !variableInfo.vars.contains(rootDeclaration)) {
            throw new IllegalStateException("Using a root declaration requires matching context declaration with no additional variables. " +
                                            "Or the context must have  matching variable name' root "
                                            + rootDeclaration + " context " + variableInfo);
        }

        EvaluatorInfo<T, K, R> info = new EvaluatorInfo<>();
        info.classLoader   = classLoader;
        info.classManager  = classManager;
        info.imports       = new HashSet<>(imports);
        info.staticImports = new HashSet<>(staticImports);
        if (variableInfo == null) {
            // root cannot be null, so set to same
            variableInfo =  ContextInfoBuilder.create(rootDeclaration);
        }
        info.variableInfo = variableInfo.build();
        info.rootDeclaration = rootDeclaration;
        info.rootVarIndex  = rootVarIndex;
        info.expression    = expression;
        info.outType      = outType;

        return info;
    }



    public static class EvaluatorInfo<T, K, R> {
        private ClassLoader classLoader;

        private ClassManager classManager;

        private Set<String> imports;

        private Set<String> staticImports;

        private Type<R> outType;
        private ContextInfo<T> variableInfo;
        private Declaration<K> rootDeclaration;

        private int rootVarIndex;

        private String expression;

        private Map<String, Declaration> allVars;

        public ClassLoader classLoader() {
            return classLoader;
        }

        public ClassManager classManager() {
            return classManager;
        }

        public Set<String> imports() {
            return imports;
        }

        public Set<String> staticImports() {
            return staticImports;
        }

        public ContextInfo<T> variableInfo() {
            return variableInfo;
        }

        public Declaration<K> rootDeclaration() {
            return rootDeclaration;
        }

        public int rootVarIndex() {
            return rootVarIndex;
        }

        public Type<R> outType() {
            return outType;
        }

        public String expression() {
            return expression;
        }

        public Map<String, Declaration> allVars() {
            if (variableInfo == null) {
                return Collections.emptyMap();
            }

            if (allVars == null) {
                allVars = Arrays.stream(variableInfo.vars).collect(Collectors.toMap(Declaration::name, Function.identity()));
            }

            return allVars;
        }

        @Override
        public String toString() {
            return "EvaluatorInfo{" +
                   "classLoader=" + classLoader +
                   ", classManager=" + classManager +
                   ", imports=" + imports +
                   ", staticImports=" + staticImports +
                   ", outType=" + outType +
                   ", variableInfo=" + variableInfo +
                   ", rootDeclaration=" + rootDeclaration +
                   ", rootVarIndex=" + rootVarIndex +
                   ", expression='" + expression + '\'' +
                   ", allVars=" + allVars +
                   '}';
        }
    }

    public static class ContextInfo<T> {
        private Declaration   declaration;

        private Declaration[] vars;

        private String[] varNames;

        public Declaration declaration() {
            return declaration;
        }

        public Declaration[] vars() {
            return vars;
        }

        public int indexOf(String name) {
            for (int i = 0; i < vars.length; i++) {
                if ( vars[i].name().equals(name)) {
                    return i;
                }
            }

            return -1;
        }
        public String[] varNames() {
            if (varNames == null) {
                varNames = Arrays.stream(vars).map(d -> d.name()).collect(Collectors.toList()).toArray(new String[0]);
            }
            return varNames;
        }


        @Override
        public String toString() {
            return "ContextInfo{" +
                   "declaration=" + declaration +
                   ", vars=" + Arrays.toString(vars) +
                   ", varNames=" + Arrays.toString(varNames) +
                   '}';
        }
    }

    public static class ContextInfoBuilder<T> {
        private Declaration<T>   declaration;

        private List<Declaration> vars = Collections.emptyList();

        public static <K> ContextInfoBuilder<K> create(Type<K> type) {
            return create(new Declaration<>(CONTEXT_NAME, type));
        }

        public static <K> ContextInfoBuilder<K> create(Declaration<K> declaration) {
            ContextInfoBuilder<K> contextInfoBuilder = new ContextInfoBuilder<>();
            contextInfoBuilder.declaration = declaration;
            return contextInfoBuilder;
        }

        public Declaration<T> getDeclaration() {
            return declaration;
        }

        public List<Declaration> getVars() {
            return vars;
        }

        public ContextInfoBuilder<T> setVars(Declaration... vars) {
            this.vars = Arrays.asList(vars);
            return this;
        }

        public ContextInfoBuilder<T>  addDeclaration(Declaration declr) {
            if (vars == Collections.<Declaration>emptyList()) {
                vars = new ArrayList<>();
            }
            this.vars.add(declr);
            return this;
        }

        @Override
        public ContextInfoBuilder<T> clone() {
            return null;
        }

        public ContextInfo<T> build() {
            ContextInfo<T> values = new ContextInfo<>();

            values.declaration    = declaration;

            values.vars = vars.toArray(new Declaration[0]);

            return values;
        }
    }

}
