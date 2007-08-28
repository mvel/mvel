package org.mvel.ast;

import org.mvel.ASTNode;
import static org.mvel.AbstractParser.getCurrentThreadParserContext;
import org.mvel.Accessor;
import org.mvel.CompileException;
import org.mvel.ParserContext;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.optimizers.OptimizerFactory.getThreadAccessorOptimizer;
import static org.mvel.util.ArrayTools.findFirst;

import static java.lang.Class.forName;

/**
 * @author Christopher Brock
 */
public class NewObjectNode extends ASTNode {
    private transient Accessor newObjectOptimizer;

    public NewObjectNode(char[] expr, int fields) {
        super(expr, fields);

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            int endRange = findFirst('(', expr);
            String name;
            if (endRange == -1) {
                name = new String(expr);
            }
            else {
                name = new String(expr, 0, findFirst('(', expr));
            }

            ParserContext pCtx = getCurrentThreadParserContext();
            if (pCtx != null && pCtx.hasImport(name)) {
                egressType = pCtx.getImport(name);
            }
            else {
                try {
                    egressType = forName(name);
                }
                catch (ClassNotFoundException e) {
                    throw new CompileException("class not found: " + name, e);
                }
            }

            String FQCN = egressType.getName();

            if (!name.equals(FQCN)) {
                int idx = FQCN.lastIndexOf('$');
                if (idx != -1 && name.lastIndexOf('$') == -1) {
                    this.name = (FQCN.substring(0, idx + 1) + new String(this.name)).toCharArray();
                }
                else {
                    this.name = (FQCN.substring(0, FQCN.lastIndexOf('.') + 1) + new String(this.name)).toCharArray();
                }
            }
        }

    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (newObjectOptimizer == null) {
            newObjectOptimizer = getThreadAccessorOptimizer().optimizeObjectCreation(name, ctx, thisValue, factory);
        }

        return newObjectOptimizer.getValue(ctx, thisValue, factory);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }


    public Accessor getNewObjectOptimizer() {
        return newObjectOptimizer;
    }
}
