/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2;

import org.mvel2.ast.Function;
import org.mvel2.ast.LineLabel;
import org.mvel2.ast.Proto;
import org.mvel2.integration.Interceptor;
import org.mvel2.util.MethodStub;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * The <tt>ParserContext</tt> is the main enviroment object used for sharing state throughout the entire
 * parser/compile process.<br/><br/>
 * The <tt>ParserContext</tt> is used to configure the parser/compiler.  For example:
 * <pre><code>
 * ParserContext parserContext = new ParserContext();
 * parserContext.setStrongTyping(true); // turn on strong typing.
 * <p/>
 * Serializable comp = MVEL.compileExpression("foo.bar", parserContext);
 * </code</pre>
 */
public class ParserContext implements Serializable {
    private String sourceFile;

    private int lineCount = 1;
    private int lineOffset;

    private ParserConfiguration parserConfiguration = new ParserConfiguration();

    private ArrayList<String> indexedVariables;
    private HashMap<String, Class> variables;
    private Map<String, Class> inputs;

    private transient HashMap<String, Map<String, Class>> typeParameters;
    private transient Type[] lastTypeParameters;
    private HashMap<String, Function> globalFunctions;

    private transient List<ErrorDetail> errorList;

    private HashMap<String, Set<Integer>> sourceMap;
    private LineLabel lastLineLabel;

    private transient Object rootParser;

    private boolean compiled = false;
    private boolean strictTypeEnforcement = false;
    private boolean strongTyping = false;

    private boolean fatalError = false;
    private boolean retainParserState = false;
    private boolean debugSymbols = false;
    private boolean blockSymbols = false;
    private boolean executableCodeReached = false;
    private boolean indexAllocation = false;
    private boolean allowBootstrapBypass = true;

    public ParserContext() {
    }

    public ParserContext(boolean debugSymbols) {
        this.debugSymbols = debugSymbols;
    }

    public ParserContext(Object rootParser) {
        this.rootParser = rootParser;
    }

    public ParserContext(ParserConfiguration parserConfiguration) {
        this.parserConfiguration = parserConfiguration;
    }

    public ParserContext(Map<String, Object> imports, Map<String, Interceptor> interceptors, String sourceFile) {
        this.sourceFile = sourceFile;
        this.parserConfiguration = new ParserConfiguration(imports, interceptors);
    }

    public ParserContext createSubcontext() {
        ParserContext ctx = new ParserContext(parserConfiguration);
        ctx.sourceFile = sourceFile;

        ctx.addInputs(inputs);
        ctx.addVariables(variables);
        ctx.addIndexedVariables(indexedVariables);
        ctx.addTypeParameters(typeParameters);

        ctx.sourceMap = sourceMap;
        ctx.lastLineLabel = lastLineLabel;

        ctx.globalFunctions = globalFunctions;
        ctx.lastTypeParameters = lastTypeParameters;
        ctx.errorList = errorList;
        ctx.rootParser = rootParser;
        ctx.lineCount = lineCount;
        ctx.lineOffset = lineOffset;

        ctx.compiled = compiled;
        ctx.strictTypeEnforcement = strictTypeEnforcement;
        ctx.strongTyping = strongTyping;

        ctx.fatalError = fatalError;
        ctx.retainParserState = retainParserState;
        ctx.debugSymbols = debugSymbols;
        ctx.blockSymbols = blockSymbols;
        ctx.executableCodeReached = executableCodeReached;
        ctx.indexAllocation = indexAllocation;

        return ctx;
    }


    /**
     * Tests whether or not a variable or input exists in the current parser context.
     *
     * @param name The name of the identifier.
     * @return boolean
     */
    public boolean hasVarOrInput(String name) {
        return (variables != null && variables.containsKey(name))
                || (inputs != null && inputs.containsKey(name));
    }

    /**
     * Return the variable or input type froom the current parser context.  Returns <tt>Object.class</tt> if the
     * type cannot be determined.
     *
     * @param name The name of the identifier
     * @return boolean
     */
    public Class getVarOrInputType(String name) {
        if (variables != null && variables.containsKey(name)) {
            return variables.get(name);
        }
        else if (inputs != null && inputs.containsKey(name)) {
            return inputs.get(name);
        }
        return Object.class;
    }

    public Class getVarOrInputTypeOrNull(String name) {
        if (variables != null && variables.containsKey(name)) {
            return variables.get(name);
        }
        else if (inputs != null && inputs.containsKey(name)) {
            return inputs.get(name);
        }
        return null;
    }

    /**
     * Get total number of lines declared in the current context.
     *
     * @return int of lines
     */
    public int getLineCount() {
        return lineCount;
    }

    /**
     * Set the current number of lines in the current context. (Generally only used by the compiler)
     *
     * @param lineCount The number of lines
     * @return int of lines
     */
    public int setLineCount(int lineCount) {
        return this.lineCount = lineCount;
    }

    /**
     * Increments the current line count by the specified amount
     *
     * @param increment The number of lines to increment
     * @return int of lines
     */
    public int incrementLineCount(int increment) {
        return this.lineCount += increment;
    }

    /**
     * Get the current line offset.  This measures the number of cursor positions back to the beginning of the line.
     *
     * @return int offset
     */
    public int getLineOffset() {
        return lineOffset;
    }

    /**
     * Sets the current line offset. (Generally only used by the compiler)
     *
     * @param lineOffset The offset amount
     */
    public void setLineOffset(short lineOffset) {
        this.lineOffset = lineOffset;
    }

    /**
     * Sets both the current line count and line offset
     *
     * @param lineCount  The line count
     * @param lineOffset The line offset
     */
    public void setLineAndOffset(int lineCount, int lineOffset) {
        addKnownLine(this.lineCount = lineCount);
        this.lineOffset = lineOffset;
    }

    /**
     * Get an import that has been declared, either in the parsed script or programatically
     *
     * @param name The name identifier for the imported class (ie. "HashMap")
     * @return An instance of <tt>Class</tt> denoting the imported class.
     */
    public Class getImport(String name) {
        return parserConfiguration.getImport(name);
    }

    /**
     * Get a {@link MethodStub} which wraps a static method import.
     *
     * @param name The name identifier
     * @return An instance of {@link MethodStub}
     */
    public MethodStub getStaticImport(String name) {
        return parserConfiguration.getStaticImport(name);
    }

    /**
     * Returns either an instance of <tt>Class</tt> or {@link MethodStub} (whichever matches).
     *
     * @param name The name identifier.
     * @return An instance of <tt>Class</tt> or {@link MethodStub}
     */
    public Object getStaticOrClassImport(String name) {
        return parserConfiguration.getStaticOrClassImport(name);
    }

    /**
     * Adds a package import to a parse session.
     *
     * @param packageName A fully qualified package (eg. <tt>java.util.concurrent</tt>).
     */
    public void addPackageImport(String packageName) {
        parserConfiguration.addPackageImport(packageName);
    }

    /**
     * Tests to see if the specified import exists.
     *
     * @param name A name identifier
     * @return boolean
     */
    public boolean hasImport(String name) {
        return parserConfiguration.hasImport(name);
    }

    public boolean hasProtoImport(String name) {
        if (parserConfiguration.getImports() == null) return false;
        Object o = parserConfiguration.getImports().get(name);
        return o != null && o instanceof Proto;
    }

    public Proto getProtoImport(String name) {
        return (Proto) parserConfiguration.getImports().get(name);
    }

    /**
     * Adds an import for the specified <tt>Class</tt>.
     *
     * @param cls The instance of the <tt>Class</tt> which represents the imported class.
     */
    public void addImport(Class cls) {
        addImport(cls.getSimpleName(), cls);
    }

    public void addImport(Proto proto) {
        parserConfiguration.addImport(proto.getName(), proto);

    }

    /**
     * Adds an import for a specified <tt>Class</tt> using an alias.  For example:
     * <pre><code>
     * parserContext.addImport("sys", System.class);
     * </code></pre>
     * ... doing this would allow an MVEL script to be written as such:
     * <pre><code>
     * sys.currentTimeMillis();
     * </code></pre>
     *
     * @param name The alias to use
     * @param cls  The instance of the <tt>Class</tt> which represents the imported class.
     */
    public void addImport(String name, Class cls) {
        parserConfiguration.addImport(name, cls);
    }

    /**
     * Adds an import for a specified <tt>Method</tt> representing a static method import using an alias. For example:
     * <pre><code>
     * parserContext.addImport("time", MVEL.getStaticMethod(System.class, "currentTimeMillis", new Class[0]));
     * </code></pre>
     * ... doing this allows the <tt>System.currentTimeMillis()</tt> method to be executed in a script simply by writing
     * <tt>time()</tt>.
     *
     * @param name   The alias to use
     * @param method The instance of <tt>Method</tt> which represents the static import.
     */
    public void addImport(String name, Method method) {
        addImport(name, new MethodStub(method));
    }

    /**
     * Adds a static import for the specified {@link MethodStub} with an alias.
     *
     * @param name   The alias to use
     * @param method The instance of <tt>Method</tt> which represents the static import.
     * @see #addImport(String, org.mvel2.util.MethodStub)
     */
    public void addImport(String name, MethodStub method) {
        parserConfiguration.addImport(name, method);
    }

    /**
     * Initializes internal Maps.  Called by the compiler.
     */
    public void initializeTables() {
        if (variables == null) variables = new LinkedHashMap<String, Class>();
        if (inputs == null) inputs = new LinkedHashMap<String, Class>();
    }


    public void addVariable(String name, Class type, boolean failIfNewAssignment) {
        initializeTables();
        if (variables.containsKey(name) && failIfNewAssignment)
            throw new CompileException("statically-typed variable already defined in scope: " + name);

        if (type == null) type = Object.class;

        variables.put(name, type);
    }

    public void addVariable(String name, Class type) {
        initializeTables();
        if (variables.containsKey(name)) return;
        if (type == null) type = Object.class;
        variables.put(name, type);
    }

    public void addVariables(Map<String, Class> variables) {
        if (variables == null) return;
        initializeTables();
        for (Map.Entry<String, Class> entry : variables.entrySet()) {
            addVariable(entry.getKey(), entry.getValue());
        }
    }

    public void addInput(String name, Class type) {
        if (inputs == null) inputs = new LinkedHashMap<String, Class>();
        if (inputs.containsKey(name)) return;
        if (type == null) type = Object.class;

        inputs.put(name, type);
    }

    public void addInput(String name, Class type, Class[] typeParameters) {
        if (type == null) type = Object.class;
        addInput(name, type);

        if (this.typeParameters == null) {
            this.typeParameters = new LinkedHashMap<String, Map<String, Class>>();
        }
        if (this.typeParameters.get(name) == null) {
            this.typeParameters.put(name, new LinkedHashMap<String, Class>());
        }

        Map<String, Class> t = this.typeParameters.get(name);

        if (typeParameters.length != type.getTypeParameters().length) {
            throw new RuntimeException("wrong number of type parameters for: " + type.getName());
        }

        TypeVariable[] tvs = type.getTypeParameters();

        for (int i = 0; i < typeParameters.length; i++) {
            t.put(tvs[i].getName(), typeParameters[i]);
        }
    }

    public void addInputs(Map<String, Class> inputs) {
        if (inputs == null) return;
        for (Map.Entry<String, Class> entry : inputs.entrySet()) {
            addInput(entry.getKey(), entry.getValue());
        }
    }

    public void processTables() {
        for (String name : variables.keySet()) {
            inputs.remove(name);
        }
    }

    public Map<String, Class> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Class> inputs) {
        this.inputs = inputs;
    }

    public List<ErrorDetail> getErrorList() {
        return errorList;
    }

    public void setErrorList(List<ErrorDetail> errorList) {
        this.errorList = errorList;
    }

    public void addError(ErrorDetail errorDetail) {
        if (errorList == null) errorList = new ArrayList<ErrorDetail>();
        if (errorDetail.isCritical()) fatalError = true;
        errorList.add(errorDetail);
    }

    public boolean isFatalError() {
        return fatalError;
    }

    public void setFatalError(boolean fatalError) {
        this.fatalError = fatalError;
    }

    public boolean isStrictTypeEnforcement() {
        return strictTypeEnforcement;
    }

    /**
     * Enables strict type enforcement -
     *
     * @param strictTypeEnforcement -
     */
    public void setStrictTypeEnforcement(boolean strictTypeEnforcement) {
        this.strictTypeEnforcement = strictTypeEnforcement;
    }

    public boolean isStrongTyping() {
        return strongTyping;
    }

    /**
     * Enables strong type enforcement.
     *
     * @param strongTyping
     */
    public void setStrongTyping(boolean strongTyping) {
        if (this.strongTyping = strongTyping) {
            // implies strict-type enforcement too
            this.strictTypeEnforcement = true;
        }
    }

    public boolean isRetainParserState() {
        return retainParserState;
    }

    public void setRetainParserState(boolean retainParserState) {
        this.retainParserState = retainParserState;
    }

    public Object getRootParser() {
        return rootParser;
    }

    public void setRootParser(Object rootParser) {
        this.rootParser = rootParser;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        if (sourceFile != null)
            this.sourceFile = sourceFile;
    }

    public Map<String, Interceptor> getInterceptors() {
        return this.parserConfiguration.getInterceptors();
    }

    public void setInterceptors(Map<String, Interceptor> interceptors) {
        this.parserConfiguration.setInterceptors(interceptors);
    }

    public Map<String, Object> getImports() {
        return this.parserConfiguration.getImports();
    }

    public void setImports(Map<String, Object> imports) {
        if (imports == null) return;

        Object val;
        for (Map.Entry<String, Object> entry : imports.entrySet()) {
            if ((val = entry.getValue()) instanceof Class) {
                addImport(entry.getKey(), (Class) val);
            }
            else if (val instanceof Method) {
                addImport(entry.getKey(), (Method) val);
            }
            else if (val instanceof MethodStub) {
                addImport(entry.getKey(), (MethodStub) val);
            }
            else {
                throw new RuntimeException("invalid element in imports map: " + entry.getKey() + " (" + val + ")");
            }
        }
    }

    public HashMap<String, Class> getVariables() {
        return variables;
    }

    public void setVariables(HashMap<String, Class> variables) {
        this.variables = variables;
    }

    public boolean isCompiled() {
        return compiled;
    }

    public void setCompiled(boolean compiled) {
        this.compiled = compiled;
    }

    public boolean isDebugSymbols() {
        return debugSymbols;
    }

    public void setDebugSymbols(boolean debugSymbols) {
        this.debugSymbols = debugSymbols;
    }

    public boolean isKnownLine(String sourceName, int lineNumber) {
        return sourceMap != null && sourceMap.containsKey(sourceName) && sourceMap.get(sourceName).contains(lineNumber);
    }

    public void addKnownLine(String sourceName, int lineNumber) {
        if (sourceMap == null) sourceMap = new HashMap<String, Set<Integer>>();
        if (!sourceMap.containsKey(sourceName)) sourceMap.put(sourceName, new HashSet<Integer>());
        sourceMap.get(sourceName).add(lineNumber);
    }

    public void addKnownLine(int lineNumber) {
        addKnownLine(sourceFile, lineNumber);
    }

    public LineLabel getLastLineLabel() {
        return lastLineLabel;
    }

    public LineLabel setLastLineLabel(LineLabel lastLineLabel) {
        return this.lastLineLabel = lastLineLabel;
    }

    public boolean hasImports() {
        return parserConfiguration.hasImports();
    }

    public void declareFunction(Function function) {
        if (globalFunctions == null) globalFunctions = new LinkedHashMap<String, Function>();
        globalFunctions.put(function.getName(), function);
    }

    public Function getFunction(String name) {
        if (globalFunctions == null) return null;
        return globalFunctions.get(name);
    }

    public Map getFunctions() {
        return globalFunctions;
    }

    public boolean hasFunction(String name) {
        return globalFunctions != null && globalFunctions.containsKey(name);
    }

    public boolean hasFunction() {
        return globalFunctions != null && globalFunctions.size() != 0;
    }

    public void addTypeParameters(String name, Class type) {
        if (typeParameters == null) typeParameters = new HashMap<String, Map<String, Class>>();

        Map<String, Class> newPkg = new HashMap<String, Class>();

        for (Type t : type.getTypeParameters()) {
            newPkg.put(t.toString(), Object.class);
        }

        typeParameters.put(name, newPkg);
    }

    public void addTypeParameters(Map<String, Map<String, Class>> typeParameters) {
        if (typeParameters == null) return;
        if (this.typeParameters == null) typeParameters = new HashMap<String, Map<String, Class>>();

        Map iMap;
        for (Map.Entry<String, Map<String, Class>> e : typeParameters.entrySet()) {
            iMap = new HashMap<String, Class>();
            for (Map.Entry<String, Class> ie : e.getValue().entrySet()) {
                iMap.put(ie.getKey(), ie.getValue());
            }
            typeParameters.put(e.getKey(), iMap);
        }

    }

    public Map<String, Class> getTypeParameters(String name) {
        if (typeParameters == null) return null;
        return typeParameters.get(name);
    }

    public Type[] getTypeParametersAsArray(String name) {
        Class c = (variables != null && variables.containsKey(name)) ? variables.get(name) : inputs.get(name);
        if (c == null) return null;

        Type[] tp = c.getTypeParameters();
        Type[] types = new Type[tp.length];

        Map<String, Class> typeVars = getTypeParameters(name);
        if (typeVars == null) {
            return null;
        }

        for (int i = 0; i < tp.length; i++) {
            types[i] = typeVars.get(tp[i].toString());
        }

        return types;
    }

    public boolean isBlockSymbols() {
        return blockSymbols;
    }

    public void setBlockSymbols(boolean blockSymbols) {
        this.blockSymbols = blockSymbols;
    }

    public boolean isExecutableCodeReached() {
        return executableCodeReached;
    }

    public void setExecutableCodeReached(boolean executableCodeReached) {
        this.executableCodeReached = executableCodeReached;
    }

    private void initIndexedVariables() {
        if (indexedVariables == null) indexedVariables = new ArrayList<String>();
    }

    public ArrayList<String> getIndexedVariables() {
        initIndexedVariables();
        return indexedVariables;
    }

    public void addIndexedVariables(String[] variables) {
        initIndexedVariables();
        for (String s : variables) {
            if (!indexedVariables.contains(s))
                indexedVariables.add(s);
        }
    }

    public void addIndexedVariable(String variable) {
        initIndexedVariables();
        if (!indexedVariables.contains(variable)) indexedVariables.add(variable);
    }

    public void addIndexedVariables(Collection<String> variables) {
        if (variables == null) return;
        initIndexedVariables();
        for (String s : variables) {
            if (!indexedVariables.contains(s))
                indexedVariables.add(s);
        }
    }

    public int variableIndexOf(String name) {
        return indexedVariables != null ? indexedVariables.indexOf(name) : -1;
    }

    public boolean hasIndexedVariables() {
        return indexedVariables != null && indexedVariables.size() != 0;
    }

    public boolean isIndexAllocation() {
        return indexAllocation;
    }

    public void setIndexAllocation(boolean indexAllocation) {
        this.indexAllocation = indexAllocation;
    }

    public ParserConfiguration getParserConfiguration() {
        return parserConfiguration;
    }

    public Type[] getLastTypeParameters() {
        return lastTypeParameters;
    }

    public void setLastTypeParameters(Type[] lastTypeParameters) {
        this.lastTypeParameters = lastTypeParameters;
    }

    public boolean isAllowBootstrapBypass() {
        return allowBootstrapBypass;
    }

    public void setAllowBootstrapBypass(boolean allowBootstrapBypass) {
        this.allowBootstrapBypass = allowBootstrapBypass;
    }

    public String[] getIndexedVarNames() {
        String[] s = new String[indexedVariables.size()];
        indexedVariables.toArray(s);
        return s;
    }

    // Introduce some new Fluent API stuff here.

    public static ParserContext create() {
        return new ParserContext();
    }

    public ParserContext stronglyTyped() {
        setStrongTyping(true);
        return this;
    }

    public ParserContext withInput(String name, Class type) {
        addInput(name, type);
        return this;
    }

    public ParserContext withInputs(Map<String, Class> inputs) {
        setInputs(inputs);
        return this;
    }

    public ParserContext withTypeParameter(String name, Class type) {
        addTypeParameters(name, type);
        return this;
    }

    public ParserContext withTypeParameters(Map<String, Map<String, Class>> typeParameters) {
        addTypeParameters(typeParameters);
        return this;
    }

    public ParserContext withImport(Class clazz) {
        addImport(clazz);
        return this;
    }

    public ParserContext withIndexedVars(String[] varNames) {
        indexedVariables = new ArrayList<String>();
        for (String s : varNames) {
            indexedVariables.add(s);
        }

        return this;
    }

}
