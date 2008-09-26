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
package org.mvel.ast;

import org.mvel.ast.cache.CachedListAccessor;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizerFactory;
import static org.mvel.optimizers.OptimizerFactory.SAFE_REFLECTIVE;
import static org.mvel.optimizers.OptimizerFactory.getAccessorCompiler;
import org.mvel.util.CollectionParser;
import static org.mvel.util.ParseTools.subset;

import java.util.List;

/**
 * @author Christopher Brock
 */
public class InlineCollectionNode extends ASTNode {
    private Object collectionGraph;
    private char[] trailing;

    public InlineCollectionNode(char[] expr, int start, int end, int fields) {
        super(expr, start, end, fields | INLINE_COLLECTION);

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            parseGraph(true,null);
        }
    }

    public InlineCollectionNode(char[] expr, int start, int end, int fields, Class type) {
        super(expr, start, end, fields | INLINE_COLLECTION);

        this.egressType = type;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            parseGraph(true,type);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (accessor != null) {
            return accessor.getValue(ctx, thisValue, factory);
        }
        else {
            AccessorOptimizer ao = OptimizerFactory.getThreadAccessorOptimizer();
            if (collectionGraph == null) parseGraph(true,null);

            accessor = ao.optimizeCollection(collectionGraph, egressType, trailing, ctx, thisValue, factory);
            egressType = ao.getEgressType();

            if (ao.isLiteralOnly()) {
                if (egressType == List.class) {
                    List v = (List) accessor.getValue(null, null, null);
                    accessor = new CachedListAccessor(v);
                    return v;
                }
            }
            return accessor.getValue(ctx, thisValue, factory);
        }

    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {

        parseGraph(false,egressType);

        return getAccessorCompiler(SAFE_REFLECTIVE)
                .optimizeCollection(collectionGraph, egressType, trailing, ctx, thisValue, factory).getValue(ctx, thisValue, factory);
    }

    private void parseGraph(boolean compile, Class type) {
        CollectionParser parser = new CollectionParser();

        if (type == null) {
            collectionGraph = ((List) parser.parseCollection(name, compile)).get(0);
        }
        else {
            collectionGraph = ((List) parser.parseCollection(name, compile, type)).get(0);
        }

        if (parser.getCursor() + 2 < name.length)
            trailing = subset(name, parser.getCursor() + 2);
    }
}
