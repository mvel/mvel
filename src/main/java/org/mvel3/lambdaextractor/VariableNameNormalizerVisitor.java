package org.mvel3.lambdaextractor;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Visitor that normalizes variable names in JavaParser AST.
 * Variables are renamed to v1, v2, v3, ... in order of declaration.
 * This allows structurally identical code with different variable names to be recognized as equivalent.
 *
 * IMPORTANT: This visitor should be applied to full method declarations, not just expressions or blocks.
 * The method signature (including parameter types) is preserved to ensure type safety when comparing lambdas.
 *
 * SCOPE HANDLING: Variables are registered ONLY at declaration points (Parameter, VariableDeclarator, etc.).
 * At reference points (NameExpr), we only normalize if the variable was previously declared.
 * This ensures method names, class names, and undeclared references are left unchanged.
 */
public class VariableNameNormalizerVisitor extends ModifierVisitor<Void> {

    private final Deque<Map<String, String>> scopeStack = new ArrayDeque<>();
    private int variableCounter = 1;

    public VariableNameNormalizerVisitor() {
        enterScope();
    }

    /**
     * Get the current variable mapping (original name -> normalized name)
     */
    public Map<String, String> getVariableMapping() {
        Map<String, String> flattened = new LinkedHashMap<>();
        scopeStack.descendingIterator().forEachRemaining(flattened::putAll);
        return flattened;
    }

    /**
     * Reset the normalizer state
     */
    public void reset() {
        scopeStack.clear();
        enterScope();
        variableCounter = 1;
    }

    /**
     * Register a new variable and return its normalized name
     */
    private String registerVariable(String originalName) {
        Map<String, String> currentScope = scopeStack.peek();
        if (currentScope == null) {
            throw new IllegalStateException("Scope stack is empty while registering variable " + originalName);
        }
        if (!currentScope.containsKey(originalName)) {
            String normalizedName = "v" + variableCounter++;
            currentScope.put(originalName, normalizedName);
            return normalizedName;
        }
        return currentScope.get(originalName);
    }

    private void normalizeParameter(Parameter parameter) {
        String originalName = parameter.getNameAsString();
        String normalizedName = registerVariable(originalName);
        parameter.setName(normalizedName);
    }

    /**
     * Get normalized name for a variable (returns original if not found)
     */
    private Optional<String> getNormalizedName(String originalName) {
        for (Map<String, String> scope : scopeStack) {
            String normalized = scope.get(originalName);
            if (normalized != null) {
                return Optional.of(normalized);
            }
        }
        return Optional.empty();
    }

    private void enterScope() {
        scopeStack.push(new LinkedHashMap<>());
    }

    private void exitScope() {
        if (scopeStack.isEmpty()) {
            throw new IllegalStateException("Attempted to exit a scope when none was active");
        }
        scopeStack.pop();
    }

    // Handle variable declarations

    @Override
    public Visitable visit(VariableDeclarator n, Void arg) {
        String originalName = n.getNameAsString();
        String normalizedName = registerVariable(originalName);
        n.setName(normalizedName);
        return super.visit(n, arg);
    }

    @Override
    public Visitable visit(ForEachStmt n, Void arg) {
        enterScope();
        try {
            n.getVariable().getVariables().forEach(var -> {
                String normalizedName = registerVariable(var.getNameAsString());
                var.setName(normalizedName);
            });
            return super.visit(n, arg);
        } finally {
            exitScope();
        }
    }

    // Handle variable references

    @Override
    public Visitable visit(NameExpr n, Void arg) {
        getNormalizedName(n.getNameAsString()).ifPresent(n::setName);
        return n;
    }

    @Override
    public Visitable visit(BlockStmt n, Void arg) {
        enterScope();
        try {
            return super.visit(n, arg);
        } finally {
            exitScope();
        }
    }

    @Override
    public Visitable visit(TryStmt n, Void arg) {
        enterScope();
        try {
            n.getResources().forEach(resource -> resource.accept(this, arg));
            n.getTryBlock().accept(this, arg);
            n.getCatchClauses().forEach(catchClause -> catchClause.accept(this, arg));
            n.getFinallyBlock().ifPresent(block -> block.accept(this, arg));
            return n;
        } finally {
            exitScope();
        }
    }

    @Override
    public Visitable visit(CatchClause n, Void arg) {
        enterScope();
        try {
            normalizeParameter(n.getParameter());
            return super.visit(n, arg);
        } finally {
            exitScope();
        }
    }

    @Override
    public Visitable visit(ForStmt n, Void arg) {
        enterScope();
        try {
            n.getInitialization().forEach(expr -> expr.accept(this, arg));
            n.getCompare().ifPresent(expr -> expr.accept(this, arg));
            n.getUpdate().forEach(expr -> expr.accept(this, arg));
            n.getBody().accept(this, arg);
            return n;
        } finally {
            exitScope();
        }
    }

    @Override
    public Visitable visit(SwitchStmt n, Void arg) {
        enterScope();
        try {
            return super.visit(n, arg);
        } finally {
            exitScope();
        }
    }

    @Override
    public Visitable visit(LambdaExpr n, Void arg) {
        enterScope();
        try {
            n.getParameters().forEach(this::normalizeParameter);
            return super.visit(n, arg);
        } finally {
            exitScope();
        }
    }

    @Override
    public Visitable visit(MethodDeclaration n, Void arg) {
        enterScope();
        try {
            n.getParameters().forEach(this::normalizeParameter);
            return super.visit(n, arg);
        } finally {
            exitScope();
        }
    }

    @Override
    public Visitable visit(ConstructorDeclaration n, Void arg) {
        enterScope();
        try {
            n.getParameters().forEach(this::normalizeParameter);
            return super.visit(n, arg);
        } finally {
            exitScope();
        }
    }

    @Override
    public Visitable visit(InitializerDeclaration n, Void arg) {
        enterScope();
        try {
            return super.visit(n, arg);
        } finally {
            exitScope();
        }
    }

    /**
     * Apply normalization to a node (returns a clone with normalized names)
     */
    public static <T extends Node> T normalize(T node) {
        VariableNameNormalizerVisitor visitor = new VariableNameNormalizerVisitor();
        @SuppressWarnings("unchecked")
        T cloned = (T) node.clone();
        cloned.accept(visitor, null);
        return cloned;
    }

    /**
     * Apply normalization to a node using an existing visitor (modifies in place)
     */
    public <T extends Node> T normalizeInPlace(T node) {
        node.accept(this, null);
        return node;
    }
}
