package org.mvel3.transpiler;
import com.github.javaparser.ast.expr.NameExpr;
import org.mvel3.parser.ast.expr.DrlNameExpr;
import org.mvel3.parser.ast.visitor.DrlVoidVisitorAdapter;

import java.util.HashSet;
import java.util.Set;

public class VariableAnalyser extends DrlVoidVisitorAdapter<Void> {

    private Set<String> available;
    private Set<String> used = new HashSet<>();

    private Set<String> found = new HashSet<>();

    public VariableAnalyser(Set<String> available) {
        this.available = available;
    }

    public void visit(NameExpr n, Void arg) {
        if (available.contains(n.getNameAsString())) {
            used.add(n.getNameAsString());
        }
    }

    public void visit(DrlNameExpr n, Void arg) {
        if (available.contains(n.getNameAsString())) {
            used.add(n.getNameAsString());
        } else {
            found.add(n.getNameAsString());
        }
    }

    public Set<String> getUsed() {
        return used;
    }

    public Set<String> getFound() {
        return found;
    }
}
