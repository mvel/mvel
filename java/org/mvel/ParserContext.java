package org.mvel;

import org.mvel.integration.Interceptor;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The ParserContext is the main enviroment object used for sharing state throughout the entire
 * parser/compile process.
 */
public class ParserContext {
    private String sourceFile;
    private int lineCount;

    protected Map<String, Class> imports;
    protected Map<String, Interceptor> interceptors;

    private Map<String, Class> variables;
    private Map<String, Class> inputs;

    private List<ErrorDetail> errorList;

    private Object rootParser;

    private boolean compiled = false;
    private boolean strictTypeEnforcement = false;
    private boolean fatalError = false;
    private boolean retainParserState = false;

    public ParserContext() {
    }

    public ParserContext(Object rootParser) {
        this.rootParser = rootParser;
    }

    public ParserContext(Map<String, Class> imports, Map<String, Interceptor> interceptors, String sourceFile) {
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

    public Class getImport(String name) {
        return imports != null ? imports.get(name) : null;
    }

    public boolean hasImport(String name) {
        return imports != null && imports.containsKey(name);
    }

    public void addImport(String name, Class cls) {
        if (this.imports == null) this.imports = new HashMap<String, Class>();
        this.imports.put(name, cls);
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


    public Map<String, Class> getImports() {
        return imports;
    }

    public void setImports(Map<String, Class> imports) {
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
}
