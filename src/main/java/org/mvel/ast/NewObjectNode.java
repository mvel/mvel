package org.mvel.ast;

import org.mvel.ASTNode;
import org.mvel.CompileException;
import static org.mvel.DataConversion.convert;
import static org.mvel.MVEL.eval;
import org.mvel.ParserContext;
import org.mvel.PropertyAccessor;
import static org.mvel.compiler.AbstractParser.getCurrentThreadParserContext;
import org.mvel.compiler.Accessor;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import static org.mvel.optimizers.OptimizerFactory.getThreadAccessorOptimizer;
import org.mvel.util.ArrayTools;
import static org.mvel.util.ArrayTools.findFirst;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.*;
import static org.mvel.util.PropertyTools.getBaseComponentType;

import java.io.Serializable;
import static java.lang.Character.isWhitespace;
import static java.lang.Thread.currentThread;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Christopher Brock
 */
@SuppressWarnings({"ManualArrayCopy"})
public class NewObjectNode extends ASTNode {
    private transient Accessor newObjectOptimizer;
    private String className;

    private ArraySize[] arraySize;
    private ExecutableStatement[] compiledArraySize;

    public NewObjectNode(char[] expr, int fields) {
        super(expr, fields);

        updateClassName(fields);

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
                rewriteClassReferenceToFQCN(fields);
                if (arraySize != null) {
                    try {
                        egressType = findClass(null, repeatChar('[', arraySize.length) + "L" + egressType.getName() + ";");
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        // for now, don't handle this.
                    }
                }
            }

        }

    }

    private void rewriteClassReferenceToFQCN(int fields) {
        String FQCN = egressType.getName();

        if (className.indexOf('.') == -1) {
            int idx = ArrayTools.findFirst('(', name);

            char[] fqcn = FQCN.toCharArray();

            if (idx == -1) {
                this.name = new char[idx = fqcn.length];
                for (int i = 0; i < idx; i++)
                    this.name[i] = fqcn[i];
            }
            else {
                char[] newName = new char[fqcn.length + (name.length - idx)];

                for (int i = 0; i < fqcn.length; i++)
                    newName[i] = fqcn[i];

                int i0 = name.length - idx;
                int i1 = fqcn.length;
                for (int i = 0; i < i0; i++)
                    newName[i + i1] = name[i + idx];

                this.name = newName;
            }
            updateClassName(fields);
        }
    }

    private void updateClassName(int fields) {
        int endRange = findFirst('(', name);
        if (endRange == -1) {
            if ((endRange = findFirst('[', name)) != -1) {
                className = new String(name, 0, endRange);
                int to;

                LinkedList<char[]> sizes = new LinkedList<char[]>();

                while (endRange < name.length) {
                    while (endRange < name.length && isWhitespace(name[endRange])) endRange++;

                    if (endRange == name.length) break;

                    if (name[endRange] != '[')
                        throw new CompileException("unexpected token in contstructor", name, endRange);

                    if ((to = balancedCapture(name, endRange, '[')) == -1)
                        throw new CompileException("unbalanced brace '['", name, endRange);

                    sizes.add(ParseTools.subset(name, ++endRange, to - endRange));
                    endRange = to + 1;
                }

                Iterator<char[]> iter = sizes.iterator();
                arraySize = new ArraySize[sizes.size()];
                for (int i = 0; i < arraySize.length; i++)
                    arraySize[i] = new ArraySize(iter.next());


                if ((fields & COMPILE_IMMEDIATE) != 0) {
                    compiledArraySize = new ExecutableStatement[arraySize.length];
                    for (int i = 0; i < compiledArraySize.length; i++)
                        compiledArraySize[i] = (ExecutableStatement) subCompileExpression(arraySize[i].value);
                }

                return;
            }

            className = new String(name);
        }
        else {
            className = new String(name, 0, endRange);
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
                        rewriteClassReferenceToFQCN(COMPILE_IMMEDIATE);

                        if (arraySize != null) {
                            try {
                                egressType = findClass(factory, repeatChar('[', arraySize.length) + "L" + egressType.getName() + ";");
                            }
                            catch (Exception e) {
                                // for now, don't handle this.
                            }
                        }

                    }
                    catch (ClassCastException e) {
                        throw new CompileException("cannot construct object: " + className + " is not a class reference", e);
                    }
                }
            }

            Class cls = Class[].class;

            if (arraySize != null) {
                return (newObjectOptimizer = new NewObjectArray(getBaseComponentType(egressType.getComponentType()), compiledArraySize))
                        .getValue(ctx, thisValue, factory);
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

    private static final Class[] EMPTYCLS = new Class[0];

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {

        try {
            if (arraySize != null) {
                Class cls = findClass(factory, className);

                int[] s = new int[arraySize.length];
                for (int i = 0; i < s.length; i++) {
                    s[i] = convert(eval(arraySize[i].value, ctx, factory), Integer.class);
                }

                return Array.newInstance(cls, s);
            }
            else {
                String[] cnsRes = captureContructorAndResidual(name);
                String[] constructorParms = parseMethodOrConstructor(cnsRes[0].toCharArray());

                if (constructorParms != null) {
                    Class cls = findClass(factory, new String(subset(name, 0, findFirst('(', name))));

                    Object[] parms = new Object[constructorParms.length];
                    for (int i = 0; i < constructorParms.length; i++) {
                        parms[i] = eval(constructorParms[i], ctx, factory);
                    }

                    Constructor cns = getBestConstructorCanadidate(parms, cls);

                    if (cns == null)
                        throw new CompileException("unable to find constructor for: " + cls.getName());

                    for (int i = 0; i < parms.length; i++) {
                        //noinspection unchecked
                        parms[i] = convert(parms[i], cns.getParameterTypes()[i]);
                    }

                    if (cnsRes.length > 1) {
                        return PropertyAccessor.get(cnsRes[1], cns.newInstance(parms), factory, thisValue);
                    }
                    else {
                        return cns.newInstance(parms);
                    }
                }

                else {
                    Constructor<?> cns = currentThread().getContextClassLoader()
                            .loadClass(new String(name)).getConstructor(EMPTYCLS);

                    if (cnsRes.length > 1) {
                        return PropertyAccessor.get(cnsRes[1], cns.newInstance(), factory, thisValue);
                    }
                    else {
                        return cns.newInstance();
                    }
                }
            }
        }
        catch (ClassNotFoundException e) {
            throw new CompileException("unable to resolve class: " + e.getMessage());
        }
        catch (NoSuchMethodException e) {
            throw new CompileException("cannot resolve constructor: " + e.getMessage());
        }
        catch (Exception e) {
            throw new CompileException("could not instantiate class: " + e.getMessage());
        }
    }


    public Accessor getNewObjectOptimizer() {
        return newObjectOptimizer;
    }

    public static class ArraySize implements Serializable {
        public ArraySize(char[] value) {
            this.value = value;
        }

        public char[] value;
    }

    public static class NewObjectArray implements Accessor, Serializable {
        private ExecutableStatement[] sizes;
        private Class arrayType;

        public NewObjectArray(Class arrayType, ExecutableStatement[] sizes) {
            this.arrayType = arrayType;
            this.sizes = sizes;
        }

        public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
            int[] s = new int[sizes.length];
            for (int i = 0; i < s.length; i++) {
                s[i] = convert(sizes[i].getValue(ctx, elCtx, variableFactory), Integer.class);
            }

            return Array.newInstance(arrayType, s);
        }

        public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
            return null;
        }
    }
}
