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

package org.mvel2.optimizers.dynamic;

import org.mvel2.ParserContext;
import org.mvel2.compiler.Accessor;
import org.mvel2.compiler.AbstractParser;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AccessorOptimizer;
import org.mvel2.optimizers.OptimizerFactory;

import static java.lang.System.currentTimeMillis;

public class DynamicGetAccessor implements DynamicAccessor {
    private char[] property;
    private long stamp;
    private int type;

    private int runcount;

    private boolean opt = false;

    private ParserContext context;

    private Accessor _safeAccessor;
    private Accessor _accessor;

    public DynamicGetAccessor(ParserContext context, char[] property, int type, Accessor _accessor) {
        this._safeAccessor = this._accessor = _accessor;
        this.type = type;
        this.property = property;
        this.context = context;
        stamp = currentTimeMillis();
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        if (!opt) {
            if (++runcount > DynamicOptimizer.tenuringThreshold) {
                if ((currentTimeMillis() - stamp) < DynamicOptimizer.timeSpan) {
                    opt = true;
                    return optimize(ctx, elCtx, variableFactory);
                }
                else {
                    runcount = 0;
                    stamp = currentTimeMillis();
                }
            }
        }

        return _accessor.getValue(ctx, elCtx, variableFactory);
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        runcount++;
        return _accessor.setValue(ctx, elCtx, variableFactory, value);
    }

    private Object optimize(Object ctx, Object elCtx, VariableResolverFactory variableResolverFactory) {

        if (DynamicOptimizer.classLoader.isOverloaded()) {
            DynamicOptimizer.enforceTenureLimit();
        }

        AccessorOptimizer ao = OptimizerFactory.getAccessorCompiler("ASM");
        switch (type) {
            case DynamicOptimizer.REGULAR_ACCESSOR:
                _accessor = ao.optimizeAccessor(context, property, ctx, elCtx, variableResolverFactory, false, null);
                return ao.getResultOptPass();
            case DynamicOptimizer.OBJ_CREATION:
                _accessor = ao.optimizeObjectCreation(context, property, ctx, elCtx, variableResolverFactory);
                return _accessor.getValue(ctx, elCtx, variableResolverFactory);
            case DynamicOptimizer.COLLECTION:
                _accessor = ao.optimizeCollection(AbstractParser.getCurrentThreadParserContext(), ctx, null, property, ctx, elCtx, variableResolverFactory);
                return _accessor.getValue(ctx, elCtx, variableResolverFactory);
        }
        return null;
    }

    public void deoptimize() {
        this._accessor = this._safeAccessor;
        opt = false;
        runcount = 0;
        stamp = currentTimeMillis();
    }

    public long getStamp() {
        return stamp;
    }

    public int getRuncount() {
        return runcount;
    }

    public Class getKnownEgressType() {
        return _safeAccessor.getKnownEgressType();
    }
}
