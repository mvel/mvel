package org.mvel;

import org.mvel.integration.Interceptor;
import org.mvel.ast.LineLabel;

import java.util.*;
import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * The ParserContext is the main enviroment object used for sharing state throughout the entire
 * parser/compile process.
 */
public class ParserContext implements Serializable {
    private String sourceFile;

    private int lineCount = 1;
    private int lineOffset;

    protected Map<String, Object> imports;
    protected Set<String> packageImports;

    protected Map<String, Interceptor> interceptors;

    private Map<String, Class> variables;
    private Map<String, Class> inputs;

    private List<ErrorDetail> errorList;

    private Map<String, Set<Integer>> sourceMap;
    private LineLabel firstLineLabel;

    private Object rootParser;

    private boolean compiled = false;
    private boolean strictTypeEnforcement = false;
    private boolean fatalError = false;
    private boolean retainParserState = false;
    private boolean debugSymbols = false;

    public ParserContext() {
    }

    public ParserContext(boolean debugSymbols) {
        this.debugSymbols = debugSymbols;
    }

    public ParserContext(Object rootParser) {
        this.rootParser = rootParser;
    }

    public ParserContext(Map<String, Object> imports, Map<String, Interceptor> interceptors, String sourceFile) {
        this.imports = imports;
        this.interceptors = interceptors;
        this.sourceFile = sourceFile;
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

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }

    public int getLineOffset() {
        return lineOffset;
    }

    public void setLineOffset(int lineOffset) {
        this.lineOffset = lineOffset;
    }

    public void setLineAndOffset(int lineCount, int lineOffset) {
        this.lineCount = lineCount;
        this.lineOffset = lineOffset;
    }

    public Class getImport(String name) {
        return (imports != null && imports.containsKey(name) ? (Class) imports.get(name) : (Class) AbstractParser.LITERALS.get(name));
    }

    public Method getStaticImport(String name) {
        return imports != null ? (Method) imports.get(name) : null;
    }

    public void addPackageImport(String packageName) {
        if (packageImports == null) packageImports = new HashSet<String>();
        packageImports.add(packageName);
    }

    private boolean checkForDynamicImport(String className) {
        if (packageImports == null) return false;

        int found = 0;
        Class cls = null;
        for (String pkg : packageImports) {
            try {
                cls = Class.forName(pkg + "." + className);
                found++;
            }
            catch (ClassNotFoundException e) {
                // do nothing.
            }
        }

        if (found > 1) {
            throw new CompileException("ambiguous class name: " + className);
        }
        else if (found == 1) {
            addImport(className, cls);
            return true;
        }
        else {
            return false;
        }
    }

    public boolean hasImport(String name) {
        return (imports != null && imports.containsKey(name)) ||
                (!"this".equals(name) && !"self".equals(name) && !"empty".equals(name) && !"null".equals(name) &&
                        !"nil".equals(name) && !"true".equals(name) && !"false".equals(name)
            && AbstractParser.LITERALS.containsKey(name))
                || checkForDynamicImport(name);
    }


    public void addImport(String name, Class cls) {
        if (this.imports == null) this.imports = new HashMap<String, Object>();
        this.imports.put(name, cls);
    }

    public void addImport(String name, Method method) {
        if (this.imports == null) this.imports = new HashMap<String, Object>();
        this.imports.put(name, method);
    }

    public void initializeTables() {
        if (variables == null) variables = new HashMap<String, Class>();
        if (inputs == null) inputs = new HashMap<String, Class>();
    }

    public void addVariable(String name, Class type) {
        if (variables.containsKey(name)) return;
        if (type == null) type = Object.class;
        variables.put(name, type);
    }

    public void addInput(String name, Class type) {
        if (inputs == null) inputs = new HashMap<String, Class>();
        if (inputs.containsKey(name)) return;
        if (type == null) type = Object.class;
        inputs.put(name, type);
    }

    public void processTables() {
        for (String name : variables.keySet()) {
            inputs.remove(name);
        }
    }

    // accessors -- start here


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
        return interceptors;
    }

    public void setInterceptors(Map<String, Interceptor> interceptors) {
        this.interceptors = interceptors;
    }


    public Map<String, Object> getImports() {
        return imports;
    }

    public void setImports(Map<String, Object> imports) {
        if (imports == null) return;

        if (this.imports != null) {
            this.imports.putAll(imports);
        }
        else {
            this.imports = imports;
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
        if (sourceMap == null) sourceMap = new HashMap<String, Set<Integer>>();
        if (!sourceMap.containsKey(sourceName)) sourceMap.put(sourceName, new HashSet<Integer>());
        sourceMap.get(sourceName).add(lineNumber);
    }


    public LineLabel getFirstLineLabel() {
        return firstLineLabel;
    }

    public void setFirstLineLabel(LineLabel firstLineLabel) {
        this.firstLineLabel = firstLineLabel;
    }

    public boolean hasImports() {
        return (imports != null && imports.size() != 0) || (packageImports != null && packageImports.size() != 0);
    }
}
