package org.mvel.ast;

import org.mvel.CompileException;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.DefaultLocalVariableResolverFactory;
import org.mvel.integration.impl.ItemResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;
import static org.mvel.util.ParseTools.subset;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Christopher Brock
 */
public class ForEachNode extends BlockNode {
    protected String item;
    protected ExecutableStatement condition;
    protected ExecutableStatement compiledBlock;

    public ForEachNode(char[] condition, char[] block, int fields) {
        super(condition, fields);
        handleCond(condition);
        this.compiledBlock = (ExecutableStatement) subCompileExpression(this.block = block);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        Map<String, Object> locals = new HashMap<String, Object>();
        VariableResolverFactory local = new DefaultLocalVariableResolverFactory(locals);

        Object ret = null;
        Object iterCond = condition.getValue(ctx, thisValue, local.setNextFactory(factory));

        ItemResolverFactory.ItemResolver itemR = new ItemResolverFactory.ItemResolver(item);
        ItemResolverFactory itemFactory = new ItemResolverFactory(itemR, local);

        if (iterCond instanceof Iterable) {
            for (Object o : (Iterable) iterCond) {
                // locals.put(item, o);
                itemR.setValue(o);
                ret = compiledBlock.getValue(ctx, thisValue, itemFactory);
            }
        }
        else if (iterCond instanceof Object[]) {
            for (Object o : (Object[]) iterCond) {
                // locals.put(item, o);
                itemR.setValue(o);
                ret = compiledBlock.getValue(ctx, thisValue, itemFactory);
            }
        }

        return ret == null ? Void.class : ret;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }

    private void handleCond(char[] condition) {
        int cursor = 0;
        while (cursor < condition.length && condition[cursor] != ':') cursor++;

        if (cursor == condition.length || condition[cursor] != ':')
            throw new CompileException("expected : in foreach");

        item = new String(condition, 0, cursor).trim();

        cursor++;

        this.condition = (ExecutableStatement) subCompileExpression(subset(condition, cursor));
    }
}
