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

import org.mvel.CompileException;
import org.mvel.MVEL;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.DefaultLocalVariableResolverFactory;
import org.mvel.integration.impl.ItemResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;
import static org.mvel.util.ParseTools.subset;

/**
 * @author Christopher Brock
 */
public class ForEachNode extends BlockNode {
    protected String item;

    private char[] cond;
    protected ExecutableStatement condition;
    protected ExecutableStatement compiledBlock;

    public ForEachNode(char[] condition, char[] block, int fields) {
        super(condition, fields);
        handleCond(condition);
        this.compiledBlock = (ExecutableStatement) subCompileExpression(this.block = block);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        //   Object ret = null;

        ItemResolverFactory.ItemResolver itemR = new ItemResolverFactory.ItemResolver(item);
        ItemResolverFactory itemFactory = new ItemResolverFactory(itemR, new DefaultLocalVariableResolverFactory(factory));

        Object iterCond = condition.getValue(ctx, thisValue, factory);

        if (iterCond instanceof Iterable) {
            for (Object o : (Iterable) iterCond) {
                itemR.setValue(o);
                compiledBlock.getValue(ctx, thisValue, itemFactory);
            }
        }
        else if (iterCond instanceof Object[]) {
            for (Object o : (Object[]) iterCond) {
                itemR.setValue(o);
                compiledBlock.getValue(ctx, thisValue, itemFactory);
            }
        }
        else if (iterCond instanceof CharSequence) {
            for (Object o : iterCond.toString().toCharArray()) {
                itemR.setValue(o);
                compiledBlock.getValue(ctx, thisValue, itemFactory);
            }
        }
        else if (iterCond instanceof Integer) {
            int max = (Integer) iterCond + 1;
            for (int i = 1; i != max; i++) {
                itemR.setValue(i);
                compiledBlock.getValue(ctx, thisValue, itemFactory);
            }
        }
        else {
            throw new CompileException("non-iterable type: " + iterCond.getClass().getName());
        }

        return null;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {

        ItemResolverFactory.ItemResolver itemR = new ItemResolverFactory.ItemResolver(item);
        ItemResolverFactory itemFactory = new ItemResolverFactory(itemR, new DefaultLocalVariableResolverFactory(factory));

        Object iterCond = MVEL.eval(cond, thisValue, factory);

        if (iterCond instanceof Iterable) {
            for (Object o : (Iterable) iterCond) {
                itemR.setValue(o);
                compiledBlock.getValue(ctx, thisValue, itemFactory);
            }
        }
        else if (iterCond instanceof Object[]) {
            for (Object o : (Object[]) iterCond) {
                itemR.setValue(o);
                compiledBlock.getValue(ctx, thisValue, itemFactory);
            }
        }
        else if (iterCond instanceof CharSequence) {
            for (Object o : iterCond.toString().toCharArray()) {
                itemR.setValue(o);
                compiledBlock.getValue(ctx, thisValue, itemFactory);
            }
        }
        else if (iterCond instanceof Integer) {
            int max = (Integer) iterCond + 1;
            for (int i = 1; i != max; i++) {
                itemR.setValue(i);
                compiledBlock.getValue(ctx, thisValue, itemFactory);
            }
        }
        else {
            throw new CompileException("non-iterable type: " + iterCond.getClass().getName());
        }

        return null;
    }

    private void handleCond(char[] condition) {
        int cursor = 0;
        while (cursor < condition.length && condition[cursor] != ':') cursor++;

        if (cursor == condition.length || condition[cursor] != ':')
            throw new CompileException("expected : in foreach");

        item = new String(condition, 0, cursor).trim();

        this.condition = (ExecutableStatement) subCompileExpression(this.cond = subset(condition, ++cursor));
    }
}
