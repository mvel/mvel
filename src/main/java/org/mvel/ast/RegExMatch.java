package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.ExecutableStatement;
import static org.mvel.MVEL.eval;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;

import static java.lang.String.valueOf;
import static java.util.regex.Pattern.compile;

public class RegExMatch extends ASTNode {
    private ExecutableStatement stmt;
    private ExecutableStatement patternStmt;
    private char[] pattern;

    public RegExMatch(char[] expr, int fields, char[] pattern) {
        super(expr, fields);
        this.pattern = pattern;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            this.stmt = (ExecutableStatement) subCompileExpression(expr);
            this.patternStmt = (ExecutableStatement) subCompileExpression(pattern);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return compile(valueOf(patternStmt.getValue(ctx, thisValue, factory))).matcher(valueOf(stmt.getValue(ctx, thisValue, factory))).matches();
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return compile(valueOf(eval(pattern, ctx, factory))).matcher(valueOf(eval(name, ctx, factory))).matches();
    }
}
