package org.mvel2;

import org.mvel2.ast.Function;
import org.mvel2.ast.LineLabel;
import org.mvel2.integration.Interceptor;
import org.mvel2.util.MethodStub;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * The ParserContext is the main enviroment object used for sharing state throughout the entire
 * parser/compile process.
 */
public class ParserContext implements Serializable {
    private String sourceFile;

    private int lineCount = 1;
    private int lineOffset;

    private ParserConfiguration parserConfiguration = new ParserConfiguration();

    private ArrayList<String> indexedVariables;
    private Map<String, Class> variables;
    private Map<String, Class> inputs;

    private transient Map<String, Map<String, Class>> typeParameters;
    private transient Type[] lastTypeParameters;
    private Map<String, Function> globalFunctions;

    private transient List<ErrorDetail> errorList;

    private Map<String, Set<Integer>> sourceMap;
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

    public boolean hasVarOrInput(String name) {
        return (variables != null && variables.containsKey(name))
                || (inputs != null && inputs.containsKey(name));
    }

    public Class getVarOrInputType(String name) {
        if (variables != null && variables.containsKey(name)) {
            return variables.get(name);
        }
        else if (inputs != null && inputs.containsKey(name)) {
            return inputs.get(name);
        }
        return Object.class;
    }

    public int getLineCount() {
        return lineCount;
    }

    public int setLineCount(int lineCount) {
        return this.lineCount = (short) lineCount;
    }

    public int incrementLineCount(int increment) {
        return this.lineCount += increment;
    }

    public int getLineOffset() {
        return lineOffset;
    }

    public void setLineOffset(short lineOffset) {
        this.lineOffset = lineOffset;
    }

    public void setLineAndOffset(int lineCount, int lineOffset) {
        addKnownLine(this.lineCount = lineCount);
        this.lineOffset = lineOffset;
    }

    public Class getImport(String name) {
        return parserConfiguration.getImport(name);
    }

    public MethodStub getStaticImport(String name) {
        return parserConfiguration.getStaticImport(name);
    }

    public Object getStaticOrClassImport(String name) {
        return parserConfiguration.getStaticOrClassImport(name);
    }

    public void addPackageImport(String packageName) {
        parserConfiguration.addPackageImport(packageName);
    }

    public boolean hasImport(String name) {
        return parserConfiguration.hasImport(name);
    }

    public void addImport(Class cls) {
        addImport(cls.getSimpleName(), cls);
    }

    public void addImport(String name, Class cls) {
        parserConfiguration.addImport(name, cls);
    }

    public void addImport(String name, Method method) {
        addImport(name, new MethodStub(method));
    }

    public void addImport(String name, MethodStub method) {
        parserConfiguration.addImport(name, method);
    }

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

    public void setStrictTypeEnforcement(boolean strictTypeEnforcement) {
        this.strictTypeEnforcement = strictTypeEnforcement;
    }

    public boolean isStrongTyping() {
        return strongTyping;
    }

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

    public Map<String, Class> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Class> variables) {
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
        if (sourceMap == null) sourceMap = new LinkedHashMap<String, Set<Integer>>();
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

    public Map<String, Class> getTypeParameters(String name) {
        if (typeParameters == null) return null;
        return typeParameters.get(name);
    }

    public Type[] getTypeParametersAsArray(String name) {
        Class c = inputs.get(name);
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
}
