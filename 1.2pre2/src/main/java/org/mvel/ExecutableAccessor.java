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
package org.mvel;

import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.handleParserEgress;

public class ExecutableAccessor implements ExecutableStatement {
    private ASTNode accessor;

    private Class ingress;
    private Class egress;
    private boolean convertable;

    private boolean returnBigDecimal;

    public ExecutableAccessor(ASTNode accessor, boolean returnBigDecimal) {
        this.accessor = accessor;
        this.returnBigDecimal = returnBigDecimal;
    }


    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return getValue(elCtx, variableFactory);
    }

    public Object getValue(Object staticContext, VariableResolverFactory factory) {
        return handleParserEgress(accessor.getReducedValueAccelerated(staticContext, staticContext, factory),
                returnBigDecimal);
    }


    public void setKnownIngressType(Class type) {
        this.ingress = type;
    }

    public void setKnownEgressType(Class type) {
        this.egress = type;
    }

    public Class getKnownIngressType() {
        return ingress;
    }

    public Class getKnownEgressType() {
        return egress;
    }

    public boolean isConvertableIngressEgress() {
        return convertable;
    }

    public void computeTypeConversionRule() {
        if (ingress != null && egress != null) {
            convertable = ingress.isAssignableFrom(egress);
        }
    }

    public boolean intOptimized() {
        return false;
    }

    public ASTNode getAccessor() {
        return accessor;
    }
}


