package org.mvel.ast;

import org.mvel.CompileException;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ArrayTools.findLast;
import static org.mvel.util.ParseTools.findStaticMethodImportResolverFactory;
import static org.mvel.util.ParseTools.subset;

import static java.lang.Thread.currentThread;
import java.lang.reflect.Method;
import static java.lang.reflect.Modifier.isStatic;

/**
 * @author Christopher Brock
 */
public class StaticImportNode extends ASTNode {
    private Class declaringClass;
    private String methodName;
    private transient Method method;

    public StaticImportNode(char[] expr, int fields) {
        super(expr, fields);

        try {
            declaringClass = currentThread().getContextClassLoader().loadClass(new String(subset(expr, 0, findLast('.', expr))));
            methodName = new String(subset(expr, findLast('.', expr) + 1));

            if (resolveMethod() == null) {
                throw new CompileException("can not find method for static import: "
                        + declaringClass.getName() + "." + methodName);
            }
        }
        catch (Exception e) {
            throw new CompileException("unable to import class", e);
        }
    }

    private Method resolveMethod() {
        for (Method meth : declaringClass.getMethods()) {
            if (isStatic(meth.getModifiers()) && methodName.equals(meth.getName())) {
                return method = meth;
                //      return;
            }
        }
        return null;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (method == null) {
            method = resolveMethod();
        }

        findStaticMethodImportResolverFactory(factory).createVariable(methodName, method);
        return null;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }
}
