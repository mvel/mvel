package org.mvel3.transpiler;
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

    // Names of identifiers read by the expression (every NameExpr / DrlNameExpr).
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

    public Set<String> getUsed() {
        return used;
    }

    public Set<String> getFound() {
        return found;
    }

    public Set<String> getReadProperties() {
        return readProperties;
    }
}
