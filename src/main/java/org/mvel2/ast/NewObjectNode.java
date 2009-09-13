/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mvel2.ast;

import org.mvel2.CompileException;
import static org.mvel2.DataConversion.convert;
import static org.mvel2.MVEL.eval;
import org.mvel2.ParserContext;
import org.mvel2.PropertyAccessor;
import org.mvel2.ErrorDetail;
import static org.mvel2.compiler.AbstractParser.getCurrentThreadParserContext;
import org.mvel2.compiler.Accessor;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AccessorOptimizer;
import static org.mvel2.optimizers.OptimizerFactory.getThreadAccessorOptimizer;
import org.mvel2.util.ArrayTools;
import org.mvel2.util.CompilerTools;
import static org.mvel2.util.CompilerTools.getInjectedImports;
import static org.mvel2.util.ArrayTools.findFirst;
import static org.mvel2.util.ParseTools.*;

import java.io.Serializable;
import static java.lang.Thread.currentThread;
import static java.lang.reflect.Array.newInstance;
import java.lang.reflect.Constructor;

/**
 * @author Christopher Brock
 */
@SuppressWarnings({"ManualArrayCopy"})
public class NewObjectNode extends ASTNode {
    private transient Accessor newObjectOptimizer;
    private TypeDescriptor typeDescr;

    public NewObjectNode(TypeDescriptor typeDescr, int fields, ParserContext pCtx) {
        this.typeDescr = typeDescr;
        this.fields = fields;
        this.name = typeDescr.getClassNameArray();

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            if (pCtx != null && pCtx.hasImport(typeDescr.getClassName())) {
                pCtx.setAllowBootstrapBypass(false);
                egressType = pCtx.getImport(typeDescr.getClassName());
            }
            else {
                try {
                    egressType = Class.forName(typeDescr.getClassName(), true, currentThread().getContextClassLoader());
                }
                catch (ClassNotFoundException e) {
                    if (pCtx != null && pCtx.isStrongTyping()) 
                        pCtx.addError(new ErrorDetail("could not resolve class: " + typeDescr.getClassName(), true));

                    // do nothing.
                }
            }

            if (egressType != null) {
                rewriteClassReferenceToFQCN(fields);
                if (typeDescr.isArray()) {
                    try {
                        egressType = findClass(null,
                                repeatChar('[', typeDescr.getArrayLength()) + "L" + egressType.getName() + ";", pCtx);
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

        if (typeDescr.getClassName().indexOf('.') == -1) {
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

            this.typeDescr.updateClassName(name, fields);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (newObjectOptimizer == null) {
            if (egressType == null) {
                /**
                 * This means we couldn't resolve the type at the time this AST node was created, which means
                 * we have to attempt runtime resolution.
                 */

                if (factory != null && factory.isResolveable(typeDescr.getClassName())) {
                    try {
                        egressType = (Class) factory.getVariableResolver(typeDescr.getClassName()).getValue();
                        rewriteClassReferenceToFQCN(COMPILE_IMMEDIATE);

                        if (typeDescr.isArray()) {
                            try {
                                egressType = findClass(factory,
                                        repeatChar('[', typeDescr.getArrayLength()) + "L" + egressType.getName() + ";", null);
                            }
                            catch (Exception e) {
                                // for now, don't handle this.
                            }
                        }

                    }
                    catch (ClassCastException e) {
                        throw new CompileException("cannot construct object: " + typeDescr.getClassName() + " is not a class reference", e);
                    }
                }
            }

            if (typeDescr.isArray()) {
                return (newObjectOptimizer = new NewObjectArray(getBaseComponentType(egressType.getComponentType()), typeDescr.getCompiledArraySize()))
                        .getValue(ctx, thisValue, factory);
            }

            AccessorOptimizer optimizer = getThreadAccessorOptimizer();

            ParserContext pCtx = new ParserContext();
            pCtx.getParserConfiguration().setAllImports(getInjectedImports(factory));

            newObjectOptimizer = optimizer.optimizeObjectCreation(pCtx, name, ctx, thisValue, factory);

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
            if (typeDescr.isArray()) {
                Class cls = findClass(factory, typeDescr.getClassName(), null);

                int[] s = new int[typeDescr.getArrayLength()];
                ArraySize[] arraySize = typeDescr.getArraySize();

                for (int i = 0; i < s.length; i++) {
                    s[i] = convert(eval(arraySize[i].value, ctx, factory), Integer.class);
                }

                return newInstance(cls, s);
            }
            else {
                String[] cnsRes = captureContructorAndResidual(name);
                String[] constructorParms = parseMethodOrConstructor(cnsRes[0].toCharArray());

                if (constructorParms != null) {
                    Class cls = findClass(factory, new String(subset(name, 0, findFirst('(', name))), null);

                    Object[] parms = new Object[constructorParms.length];
                    for (int i = 0; i < constructorParms.length; i++) {
                        parms[i] = eval(constructorParms[i], ctx, factory);
                    }

                    Constructor cns = getBestConstructorCanadidate(parms, cls, false);

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
                    Constructor<?> cns = Class.forName(typeDescr.getClassName(), true, currentThread().getContextClassLoader())
                            .getConstructor(EMPTYCLS);

                    if (cnsRes.length > 1) {
                        return PropertyAccessor.get(cnsRes[1], cns.newInstance(), factory, thisValue);
                    }
                    else {
                        return cns.newInstance();
                    }
                }
            }
        }
        catch (CompileException e) {
            throw e;
        }
        catch (ClassNotFoundException e) {
            throw new CompileException("unable to resolve class: " + e.getMessage(), e);
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

            return newInstance(arrayType, s);
        }

        public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
            return null;
        }

        public Class getKnownEgressType() {
            try {
                return Class.forName("[L" + arrayType.getName() + ";");
            }
            catch (ClassNotFoundException cne) {
                return null;
            }
        }
    }

    public TypeDescriptor getTypeDescr() {
        return typeDescr;
    }
}
