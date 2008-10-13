/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
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
 *
 */
package org.mvel2.compiler;

import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;

import java.io.Serializable;

public class CompiledSetExpression implements ExecutableStatement, Serializable {
    private char[] expression;
    private transient Accessor accessor;

    public CompiledSetExpression(char[] expression) {
        this.expression = expression;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory vrf, Object value) {
        if (accessor == null) {
            accessor = OptimizerFactory.getThreadAccessorOptimizer().optimizeSetAccessor(expression, ctx, ctx, vrf, false, value);
        }
        else {
            accessor.setValue(ctx, elCtx, vrf, value);
        }
        return value;
    }

    public Object getValue(Object staticContext, VariableResolverFactory factory) {
        throw new RuntimeException("not supported");
    }

    public void setKnownIngressType(Class type) {
    }

    public void setKnownEgressType(Class type) {
    }

    public Class getKnownIngressType() {
        return null;
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
        throw new RuntimeException("not supported");
    }

    public Accessor getAccessor() {
        return accessor;
    }
}
