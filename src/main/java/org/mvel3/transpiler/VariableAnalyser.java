package org.mvel3.transpiler;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import org.mvel3.parser.ast.expr.DrlNameExpr;
import org.mvel3.parser.ast.visitor.DrlVoidVisitorAdapter;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class VariableAnalyser extends DrlVoidVisitorAdapter<Void> {

    private Set<String> available;
    private Set<String> used = new HashSet<>();

    private Set<String> found = new HashSet<>();

    // All identifier reads in the expression (NameExpr names + decoded no-arg getter calls).
    // Iteration order preserved so generated arrays are deterministic across builds.
    // Consumers filter against the actual settable-property set of the context type.
    private Set<String> readProperties = new LinkedHashSet<>();

    public VariableAnalyser(Set<String> available) {
        this.available = available;
    }

    public void visit(NameExpr n, Void arg) {
        String name = n.getNameAsString();
        if (available.contains(name)) {
            used.add(name);
        }
        readProperties.add(name);
    }

    public void visit(DrlNameExpr n, Void arg) {
        String name = n.getNameAsString();
        if (available.contains(name)) {
            used.add(name);
        } else {
            found.add(name);
        }
        readProperties.add(name);
    }

    public void visit(MethodCallExpr n, Void arg) {
        if (n.getScope().isEmpty() && n.getArguments().isEmpty()) {
            String prop = getter2property(n.getNameAsString());
            if (prop != null) {
                readProperties.add(prop);
            }
        }
        super.visit(n, arg);
    }

    public Set<String> getUsed() {
        return used;
    }

    public Set<String> getFound() {
        return found;
    }

    public Set<String> getReadProperties() {
        return readProperties;
    }

    /**
     * JavaBeans getter name → property name. Returns null if the name does not
     * follow the {@code getXxx} / {@code isXxx} convention.
     */
    static String getter2property(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }
        return null;
    }
}
