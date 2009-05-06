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

package org.mvel2.compiler;

import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import static org.mvel2.optimizers.OptimizerFactory.getThreadAccessorOptimizer;

import java.io.Serializable;

public class CompiledAccExpression implements ExecutableStatement, Serializable {
    private char[] expression;
    private transient Accessor accessor;
    private ParserContext context;
    private Class ingressType;

    public CompiledAccExpression(char[] expression, Class ingressType, ParserContext context) {
        this.expression = expression;
        this.context = context;
        this.ingressType = ingressType != null ? ingressType : Object.class;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory vrf, Object value) {
        if (accessor == null) {
            if (ingressType == Object.class && value != null) ingressType = value.getClass();
            accessor = getThreadAccessorOptimizer()
                    .optimizeSetAccessor(context, expression, ctx, ctx, vrf, false, value, ingressType);

        }
        else {
             accessor.setValue(ctx, elCtx, vrf, value);
        }
        return value;
    }

    public Object getValue(Object staticContext, VariableResolverFactory factory) {
        if (accessor == null) {
            accessor = getThreadAccessorOptimizer()
                    .optimizeAccessor(context, expression, staticContext, staticContext, factory, false, ingressType);
        }
        return accessor.getValue(staticContext, staticContext, factory);
    }

    public void setKnownIngressType(Class type) {
        this.ingressType = type;
    }

    public void setKnownEgressType(Class type) {

    }

    public Class getKnownIngressType() {
        return ingressType;
    }

    public Class getKnownEgressType() {
        return null;
    }

    public boolean isConvertableIngressEgress() {
        return false;
    }

    public void computeTypeConversionRule() {
    }

    public boolean intOptimized() {
        return false;
    }

    public boolean isLiteralOnly() {
        return false;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (accessor == null) {
            accessor = getThreadAccessorOptimizer().optimizeAccessor(context, expression, ctx, elCtx,
                    variableFactory, false, ingressType);
        }
        return accessor.getValue(ctx, elCtx, variableFactory);
    }

    public Accessor getAccessor() {
        return accessor;
    }
}
