package org.mvel3;

import org.mvel3.transpiler.context.Declaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MVELBuilder<C, W, O> {

    public static class TypesBuilder {
        List<Declaration> types;

        public static TypesBuilderCollector add(String identifier, Class cls) {
            return add(identifier, cls, null);
        }

        public static TypesBuilderCollector add(String identifier, Class cls, String generics) {
            return new TypesBuilderCollector(identifier, cls, generics);
        }
    }

    public static class TypesBuilderCollector {
        List<Declaration> types;

        public TypesBuilderCollector(String identifier, Class cls, String generics) {
            types = new ArrayList<>();
            types.add(new Declaration(identifier, cls, generics));
        }

        public TypesBuilderCollector add(String identifier, Class cls) {
            add(identifier, cls, null);
            return this;
        }

        public TypesBuilderCollector add(String identifier, Class cls, String generics) {
            types.add(new Declaration(identifier, cls, generics));
            return this;
        }

        public <V> Declaration<V>[] toArray() {
            return types.toArray(new Declaration[types.size()]);
        }
    }

    public static class WithBuilder<C> {
        private ContextType contextType;
        private Declaration<C> contextDeclaration;
        private Declaration<?>[] vars;

        public WithBuilder(ContextType contextType, Declaration<C> contextDeclaration, Declaration<?>[] vars) {
            this.contextType = contextType;
            this.contextDeclaration = contextDeclaration;
            this.vars = vars;
        }

        public <W> OutBuilder<C, W> with(Class cls) {
            return new OutBuilder(contextType, contextDeclaration, vars, new Declaration(WITH_NAME, Type.type(cls)));
        }

        public <O> ContentBuilder<C, Void, O> out(Class cls) {
            return out(Type.type(cls));
        }

        public <O> ContentBuilder<C, Void, O> out(Type outType) {
            return new ContentBuilder(contextType, contextDeclaration, vars, VOID_DECLARATION, outType);
        }

        public MVELBuilder<C, Void, Void> expression(String expression) {
            ContentBuilder builder = new ContentBuilder<>(contextType, contextDeclaration, vars, VOID_DECLARATION, Type.VOID);
            return builder.expression(expression);
        }

        public MVELBuilder<C, Void, Void> block(String block) {
            ContentBuilder builder = new ContentBuilder<>(contextType, contextDeclaration, vars, VOID_DECLARATION, Type.VOID);
            return builder.block(block);
        }
    }

    public static class OutBuilder<C, W> {
        private ContextType contextType;
        private Declaration<C> contextDeclaration;
        private Declaration<?>[] vars;
        private Declaration<W>   withDeclaration;

        public OutBuilder(ContextType contextType, Declaration<W> withDeclaration) {
            this.contextType     = contextType;
            this.withDeclaration = withDeclaration;
        }

        public OutBuilder(ContextType contextType, Declaration<C> contextDeclaration, Declaration<?>[] vars, Declaration<W> withDeclaration) {
            this.contextType        = contextType;
            this.contextDeclaration = contextDeclaration;
            this.vars               = vars;
            this.withDeclaration    = withDeclaration;
        }

        public <O> ContentBuilder<C, W, O> out(Class cls) {
            return new ContentBuilder(contextType, contextDeclaration, vars, withDeclaration, Type.type(cls));
        }
    }

    public static class ContentBuilder<C, W, O> {
        private ContextType contextType;
        private Declaration<C> contextDeclaration;
        private Declaration<?>[] vars;
        private Declaration<W>   withDeclaration;
        private Type             outType;


        public ContentBuilder(ContextType contextType, Declaration<C> contextDeclaration, Declaration<?>[] vars,
                              Declaration<W> withDeclaration, Type outType) {
            this.contextType        = contextType;
            this.contextDeclaration = contextDeclaration;
            this.vars               = vars;
            this.withDeclaration    = withDeclaration;
            this.outType            = outType;
        }

        public MVELBuilder<C, W, O> expression(String expression) {
            MVELBuilder<C, W, O> builder = new MVELBuilder<>();
            builder.contextType = contextType;
            builder.contextDeclaration = contextDeclaration;

            builder.contentType = ContentType.EXPRESSION;
            builder.content = expression;
            if (vars != null) {
                Arrays.stream(vars).forEach(builder::addDeclaration);
            }

            builder.withDeclaration(withDeclaration);
            builder.outType(outType);

            return builder;
        }

        public MVELBuilder<C, W, O> block(String block) {
            MVELBuilder<C, W, O> builder = new MVELBuilder<>();
            builder.contextType = contextType;
            builder.contextDeclaration = contextDeclaration;

            builder.contentType = ContentType.BLOCK;
            builder.content = block;
            if (vars != null) {
                Arrays.stream(vars).forEach(builder::addDeclaration);
            }

            builder.withDeclaration(withDeclaration);
            builder.outType(outType);

            return builder;
        }
    }

    private static final Declaration[] EMPTY_VARS  = new Declaration[0];

    public static final String       CONTEXT_NAME = "__context";

    public static final String       WITH_NAME = "__with";

    private ContextType contextType;

    private Declaration<C> contextDeclaration;

    private ContentType contentType;

    private List<Declaration> variableDeclarations = Collections.emptyList();

    private              ClassLoader classLoader;

    private        ClassManager  classManager;

    private        Set<String>   imports       = Collections.emptySet();

    private        Set<String>   staticImports = Collections.emptySet();

    public static Declaration VOID_DECLARATION = Declaration.of("__void", Void.class);

    private Declaration withDeclaration = VOID_DECLARATION;

    private Type<O> outType;

    private String content;

    private String[] outVars = new String[0];

    private String generatedClassName = "GeneratorEvaluator__";

    private String generatedMethodName = "eval";

    private String generatedSuperName;

    public static <C, W, O> MVELBuilder<C, W, O> create() {
        MVELBuilder builder = new MVELBuilder<>();
        builder.outType = Type.type(Void.class); // default no return
        return builder;
    }

    public static <C, W, O> MVELBuilder create(MVELBuilder<C, W, O> template) {
        MVELBuilder<C, W, O> builder = create();
        builder.classLoader   = template.classLoader;
        builder.classManager  = template.classManager;
        builder.imports       = new HashSet<>(template.imports);
        builder.staticImports = new HashSet<>(template.staticImports);
        builder.withDeclaration = template.withDeclaration;
        builder.outType         = template.outType;
        builder.generatedClassName = template.generatedClassName;
        builder.generatedMethodName = template.generatedMethodName;

        return builder;
    }

    public MVELBuilder() {
        classLoader(MVELBuilder.class.getClassLoader());
    }

    public MVELBuilder<C, W, O> contextType(ContextType contextType) {
        this.contextType = contextType;
        return this;
    }

    public MVELBuilder<C, W, O> contentType(ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    public MVELBuilder<C, W, O> classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public MVELBuilder<C, W, O> classManager(ClassManager classManager) {
        this.classManager = classManager;
        return this;
    }

    public MVELBuilder<C, W, O> imports(Set<String> imports) {
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

    public MVELBuilder<C, W, O> staticImports(Set<String> staticImports) {
        this.staticImports = staticImports;
        return this;
    }

    private MVELBuilder<C, W, O> outType(Type<O> outType) {
        if (outType.getClazz().isPrimitive()) {
            throw new RuntimeException("Return type must match generics and cannot be a primitive");
        }
        this.outType = outType;
        return this;
    }

    private MVELBuilder expression(String expression) {
        this.content = expression;
        return this;
    }

    public MVELBuilder<C, W, O> withDeclaration(Declaration<W> withDeclaration) {
        this.withDeclaration = withDeclaration;
        addDeclaration(withDeclaration);
        return this;
    }

    private MVELBuilder<C, W, O> withDeclaration(Type type) {
        withDeclaration(Declaration.of(CONTEXT_NAME, type));
        return this;
    }

    public MVELBuilder<C, W, O> outVars(String... outVars) {
        this.outVars = outVars;
        return this;
    }

    public ContextType getContextType() {
        return contextType;
    }

    public MVELBuilder<C, W, O>  addDeclaration(String name, Class clazz) {
        addDeclaration(Declaration.of(name, clazz));
        return this;
    }

    public MVELBuilder<C, W, O>  addDeclaration(String name, Class clazz, String generics) {
        addDeclaration(Declaration.of(name, clazz, generics));
        return this;
    }

    private void addDeclaration(Declaration<?> declaration) {
        if (variableDeclarations == Collections.<Declaration>emptyList()) {
            variableDeclarations = new ArrayList<>();
        }
        variableDeclarations.add(declaration);
    }

    public MVELBuilder<C, W, O>  generatedClassName(String generatedClassName) {
        this.generatedClassName = generatedClassName;
        return this;
    }

    public MVELBuilder<C, W, O>  generatedMethodName(String generatedMethodName) {
        this.generatedMethodName = generatedMethodName;
        return this;
    }

    public MVELBuilder<C, W, O>  generatedSuperName(String generatedSuperName) {
        this.generatedSuperName = generatedSuperName;
        return this;
    }

    public Evaluator<C, W, O>  compile() {
        return compile(build());
    }

    public Evaluator<C, W, O>  compile(CompilerParameters<C, W, O> parameters) {
        MVELCompiler compiler = new MVELCompiler();
        return compiler.compile(parameters);
    }

    public CompilerParameters<C, W, O> build() {
        // Either the root and context vars are the same, and no context variables.
        // Or the root variable must be a
        if ( variableDeclarations.isEmpty() && contextDeclaration == null) {
            throw new RuntimeException("These should be declared Void if not used");
        }

        if (withDeclaration != VOID_DECLARATION &&
            (!withDeclaration.equals(contextDeclaration)) &&
            !variableDeclarations.contains(withDeclaration)) {
            throw new IllegalStateException("Using a root declaration requires matching context declaration with no additional variables. " +
                                            "Or the context must have  matching variable name' root "
                                            + withDeclaration + " context " + variableDeclarations);
        }

        if (classManager == null) {
            classManager = new ClassManager();
        }

        CompilerParameters<C, W, O> info = new CompilerParameters<>(contextType, classLoader, classManager, imports, staticImports, outType,
                                                                    contextDeclaration, variableDeclarations, withDeclaration, contentType, content,
                                                                    generatedClassName, generatedMethodName, generatedSuperName);

        return info;
    }

}
