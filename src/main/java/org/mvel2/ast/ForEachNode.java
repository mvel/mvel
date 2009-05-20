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
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.DefaultLocalVariableResolverFactory;
import org.mvel2.integration.impl.ItemResolverFactory;
import org.mvel2.util.ParseTools;
import static org.mvel2.util.ParseTools.*;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * @author Christopher Brock
 */
public class ForEachNode extends BlockNode {
    protected String item;
    protected Class itemType;
    private char[] cond;

    protected ExecutableStatement condition;
    protected ExecutableStatement compiledBlock;

    private static final int ITERABLE = 0;
    private static final int ARRAY = 1;
    private static final int CHARSEQUENCE = 2;
    private static final int INTEGER = 3;

    private int type = -1;

    public ForEachNode(char[] condition, char[] block, int fields, ParserContext pCtx) {
        handleCond(this.name = condition, this.fields = fields, pCtx);
        this.block = block;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            if (pCtx.isStrictTypeEnforcement() && itemType != null) {
                pCtx = pCtx.createSubcontext();
                pCtx.addInput(item, itemType);
            }

            this.compiledBlock = (ExecutableStatement) subCompileExpression(block, pCtx);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        ItemResolverFactory.ItemResolver itemR = new ItemResolverFactory.ItemResolver(item);
        ItemResolverFactory itemFactory = new ItemResolverFactory(itemR, new DefaultLocalVariableResolverFactory(factory));

        Object iterCond = condition.getValue(ctx, thisValue, factory);

        if (type == -1) {
            determineIterType(iterCond.getClass());
        }

        switch (type) {
            case ARRAY:
                int len = Array.getLength(iterCond);
                for (int i = 0; i < len; i++) {
                    itemR.setValue(Array.get(iterCond, i));
                    compiledBlock.getValue(ctx, thisValue, itemFactory);
                }
                break;
            case CHARSEQUENCE:
                for (Object o : iterCond.toString().toCharArray()) {
                    itemR.setValue(o);
                    compiledBlock.getValue(ctx, thisValue, itemFactory);
                }
                break;
            case INTEGER:
                int max = (Integer) iterCond + 1;
                for (int i = 1; i != max; i++) {
                    itemR.setValue(i);
                    compiledBlock.getValue(ctx, thisValue, itemFactory);
                }
                break;

            case ITERABLE:
                for (Object o : (Iterable) iterCond) {
                    itemR.setValue(o);
                    compiledBlock.getValue(ctx, thisValue, itemFactory);
                }

                break;
        }

        return null;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        ItemResolverFactory.ItemResolver itemR = new ItemResolverFactory.ItemResolver(item);
        ItemResolverFactory itemFactory = new ItemResolverFactory(itemR, new DefaultLocalVariableResolverFactory(factory));

        Object iterCond = MVEL.eval(cond, thisValue, factory);

        if (itemType != null && itemType.isArray())
            enforceTypeSafety(itemType, getBaseComponentType(iterCond.getClass()));

        this.compiledBlock = (ExecutableStatement) subCompileExpression(block);

        if (iterCond instanceof Iterable) {
            for (Object o : (Iterable) iterCond) {
                itemR.setValue(o);
                compiledBlock.getValue(ctx, thisValue, itemFactory);
            }
        }
        else if (iterCond != null && iterCond.getClass().isArray()) {
            int len = Array.getLength(iterCond);
            for (int i = 0; i < len; i++) {
                itemR.setValue(Array.get(iterCond, i));
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
            throw new CompileException("non-iterable type: " + (iterCond != null ? iterCond.getClass().getName() : "null"));
        }

        return null;
    }

    private void handleCond(char[] condition, int fields, ParserContext pCtx) {
        int cursor = 0;
        while (cursor < condition.length && condition[cursor] != ':') cursor++;

        if (cursor == condition.length || condition[cursor] != ':')
            throw new CompileException("expected : in foreach");

        int x;
        if ((x = (item = createStringTrimmed(condition, 0, cursor)).indexOf(' ')) != -1) {
            String tk = new String(condition, 0, x).trim();
            try {
                itemType = ParseTools.findClass(null, tk, pCtx);
                item = new String(condition, x, cursor - x).trim();

            }
            catch (ClassNotFoundException e) {
                throw new CompileException("cannot resolve identifier: " + tk);
            }
        }

        this.cond = subset(condition, ++cursor);

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            Class egress = (this.condition = (ExecutableStatement) subCompileExpression(this.cond, pCtx)).getKnownEgressType();

            if (itemType != null && egress.isArray()) {
                enforceTypeSafety(itemType, getBaseComponentType(this.condition.getKnownEgressType()));
            }
            else if (pCtx.isStrongTyping()) {
                determineIterType(egress);
            }
        }
    }

    private void determineIterType(Class t) {
        if (Iterable.class.isAssignableFrom(t)) {
            type = ITERABLE;
        }
        else if (t.isArray()) {
            type = ARRAY;
        }
        else if (CharSequence.class.isAssignableFrom(t)) {
            type = CHARSEQUENCE;
        }
        else if (Integer.class.isAssignableFrom(t)) {
            type = INTEGER;
        }
        else {
            throw new CompileException("non-iterable type: " + t.getName());
        }
    }

    private static void enforceTypeSafety(Class required, Class actual) {
        if (!required.isAssignableFrom(actual)) {
            throw new CompileException("type mismatch in foreach: expected: "
                    + required.getName() + "; but found: " + getBaseComponentType(actual));
        }
    }
}
