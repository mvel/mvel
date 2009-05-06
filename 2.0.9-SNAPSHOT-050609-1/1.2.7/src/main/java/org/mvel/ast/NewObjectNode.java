package org.mvel.ast;

import org.mvel.ASTNode;
import static org.mvel.AbstractParser.getCurrentThreadParserContext;
import org.mvel.Accessor;
import org.mvel.CompileException;
import org.mvel.ParserContext;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import static org.mvel.optimizers.OptimizerFactory.getThreadAccessorOptimizer;
import org.mvel.util.ArrayTools;
import static org.mvel.util.ArrayTools.findFirst;

import static java.lang.System.arraycopy;
import static java.lang.Thread.currentThread;

/**
 * @author Christopher Brock
 */
public class NewObjectNode extends ASTNode {
    private transient Accessor newObjectOptimizer;
    private String className;

    public NewObjectNode(char[] expr, int fields) {
        super(expr, fields);

        updateClassName();

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            ParserContext pCtx = getCurrentThreadParserContext();
            if (pCtx != null && pCtx.hasImport(className)) {
                egressType = pCtx.getImport(className);
            }
            else {
                try {
                    egressType = currentThread().getContextClassLoader().loadClass(className);
                }
                catch (ClassNotFoundException e) {
                    // do nothing.
                }
            }

            if (egressType != null) {
                rewriteClassReferenceToFQCN();
            }
        }

    }

    private void rewriteClassReferenceToFQCN() {
        String FQCN = egressType.getName();

        if (className.indexOf('.') == -1) {
            int idx = ArrayTools.findFirst('(', name);
            if (idx == -1) {
                arraycopy(FQCN.toCharArray(), 0, this.name = new char[idx = FQCN.length()], 0, idx);
            }
            else {
                char[] newName = new char[FQCN.length() + (name.length - idx)];
                arraycopy(FQCN.toCharArray(), 0, newName, 0, FQCN.length());
                arraycopy(name, idx, newName, FQCN.length(), (name.length - idx));
                this.name = newName;
            }
            updateClassName();
        }
    }

    private void updateClassName() {
        int endRange = findFirst('(', name);
        if (endRange == -1) {
            className = new String(name);
        }
        else {
            className = new String(name, 0, findFirst('(', name));
        }

    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (newObjectOptimizer == null) {
            if (egressType == null) {
                /**
                 * This means we couldn't resolve the type at the time this AST node was created, which means
                 * we have to attempt runtime resolution.
                 */

                if (factory != null && factory.isResolveable(className)) {
                    try {
                        egressType = (Class) factory.getVariableResolver(className).getValue();
                        rewriteClassReferenceToFQCN();
                    }
                    catch (ClassCastException e) {
                        throw new CompileException("cannot construct object: " + className + " is not a class reference", e);
                    }
                }
            }


            AccessorOptimizer optimizer = getThreadAccessorOptimizer();
            newObjectOptimizer = optimizer.optimizeObjectCreation(name, ctx, thisValue, factory);

            /**
             * Check to see if the optimizer actually produced the object during optimization.  If so,
             * we return that value now.
             */
            if (optimizer.getResultOptPass() != null) {
                egressType = optimizer.getEgressType();
                return optimizer.getResultOptPass();
            }
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
