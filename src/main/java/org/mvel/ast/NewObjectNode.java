package org.mvel.ast;

import org.mvel.*;
import org.mvel.util.ArrayTools;
import org.mvel.util.ParseTools;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.optimizers.OptimizerFactory.getThreadAccessorOptimizer;

/**
 * @author Christopher Brock
 */
public class NewObjectNode extends ASTNode {
    private Accessor newObjectOptimizer;

    public NewObjectNode(char[] expr, int fields) {
        super(expr, fields);

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            int endRange = ArrayTools.findFirst('(', expr);
            String name;
            if (endRange == -1) {
                name = new String(expr);
            }
            else {
                name = new String(expr, 0, ArrayTools.findFirst('(', expr));
            }

            ParserContext pCtx = AbstractParser.getCurrentThreadParserContext();
            if (pCtx != null && pCtx.hasImport(name)) {
                egressType = pCtx.getImport(name);
            }
            else if (AbstractParser.LITERALS.containsKey(name)) {
                egressType = (Class) AbstractParser.LITERALS.get(name);
            }
            else {
                try {
                    egressType = Class.forName(name);
                }
                catch (ClassNotFoundException e) {
                    throw new CompileException("class not found: " + name, e);
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
