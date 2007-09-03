package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.CompileException;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.findClassImportResolverFactory;

/**
 * @author Christopher Brock
 */
public class ImportNode extends ASTNode {
    private Class importClass;

    public ImportNode(char[] expr, int fields) {
        super(expr, fields);

        try {
            this.importClass = Thread.currentThread().getContextClassLoader().loadClass(new String(expr));
        }
        catch (ClassNotFoundException e) {
            throw new CompileException("class not found: " + new String(expr));
        }
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return findClassImportResolverFactory(factory).addClass(importClass);
        //    return importClass;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }


    public Class getImportClass() {
        return importClass;
    }
}

