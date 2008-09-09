package org.mvel.ast;

import static org.mvel.util.PropertyTools.endsWith;
import static org.mvel.util.PropertyTools.findLast;
import static org.mvel.util.ParseTools.findClassImportResolverFactory;
import org.mvel.CompileException;
import org.mvel.integration.VariableResolverFactory;

/**
 * Created by IntelliJ IDEA.
 * User: christopherbrock
 * Date: Sep 8, 2008
 * Time: 9:50:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class IsDef extends ASTNode {
    private String varName;

    public IsDef(char[] expr) {
        varName = new String(this.name = expr);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return factory.isResolveable(varName);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return factory.isResolveable(varName);
    }
}
