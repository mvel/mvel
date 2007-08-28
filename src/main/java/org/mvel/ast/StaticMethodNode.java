package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.optimizers.OptimizerFactory.getThreadAccessorOptimizer;
import static org.mvel.util.ArrayTools.findFirst;
import static org.mvel.util.ParseTools.subset;

import java.lang.reflect.Method;

/**
 * @author Christopher Brock
 */
public class StaticMethodNode extends ASTNode {
    private transient Accessor accessor;
    private String method;
    private Class declaringClass;

    public StaticMethodNode(char[] expr, int fields) {
        super(expr, fields);
        method = new String(subset(expr, findFirst('(', expr)));
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return valRet(accessor.getValue(declaringClass, thisValue, factory));
        }
        catch (NullPointerException e) {
            if (accessor == null) {
                Method m = (Method) factory.getVariableResolver(method).getValue();
                declaringClass = m.getDeclaringClass();
                accessor = getThreadAccessorOptimizer().optimizeAccessor(name, declaringClass, thisValue, factory, false);
                return valRet(accessor.getValue(declaringClass, thisValue, factory));
            }
            else {
                throw e;
            }
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return super.getReducedValue(ctx, thisValue, factory);
    }
}

