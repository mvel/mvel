package org.mvel.debug;

import org.mvel.ast.LineLabel;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.CompiledExpression;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

public class DebuggerContext {
    private Map<String, Set<Integer>> breakpoints;
    private Debugger debugger;
    private int debuggerState = 0;

    public DebuggerContext() {
        breakpoints = new HashMap<String, Set<Integer>>();
    }

    public Map<String, Set<Integer>> getBreakpoints() {
        return breakpoints;
    }

    public void setBreakpoints(Map<String, Set<Integer>> breakpoints) {
        this.breakpoints = breakpoints;
    }

    public Debugger getDebugger() {
        return debugger;
    }

    public void setDebugger(Debugger debugger) {
        this.debugger = debugger;
    }

    public int getDebuggerState() {
        return debuggerState;
    }

    public void setDebuggerState(int debuggerState) {
        this.debuggerState = debuggerState;
    }

    // utility methods

    public void registerBreakpoint(String sourceFile, int lineNumber) {
        if (!breakpoints.containsKey(sourceFile)) breakpoints.put(sourceFile, new HashSet<Integer>());
        breakpoints.get(sourceFile).add(lineNumber);
    }

    public void removeBreakpoint(String sourceFile, int lineNumber) {
        if (!breakpoints.containsKey(sourceFile)) return;
        breakpoints.get(sourceFile).remove(lineNumber);
    }

    public void clearAllBreakpoints() {
        breakpoints.clear();
    }

    public boolean hasBreakpoints() {
        return breakpoints.size() != 0;
    }

    public boolean hasBreakpoint(LineLabel label) {
        return breakpoints.containsKey(label.getSourceFile()) && breakpoints.get(label.getSourceFile()).
                contains(label.getLineNumber());
    }

    public boolean hasBreakpoint(String sourceFile, int lineNumber) {
        return breakpoints.containsKey(sourceFile) && breakpoints.get(sourceFile).contains(lineNumber);
    }

    public boolean hasDebugger() {
        return debugger != null;
    }

    public int checkBreak(LineLabel label, VariableResolverFactory factory, CompiledExpression expression) {
        if (debuggerState == Debugger.STEP || hasBreakpoint(label)) {
            if (debugger == null) throw new RuntimeException("no debugger registered to handle breakpoint");
            return debuggerState = debugger.onBreak(new Frame(label, factory, expression.getParserContext()));

        }
        return 0;
    }

}
