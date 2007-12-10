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

import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.subCompileExpression;

/**
 * @author Christopher Brock
 */
public class WhileNode extends BlockNode {
    protected String item;

    protected char[] cond;
    protected ExecutableStatement condition;
    protected ExecutableStatement compiledBlock;

    public WhileNode(char[] condition, char[] block, int fields) {
        super(condition, fields);

        this.condition = (ExecutableStatement) subCompileExpression(this.cond = condition);
        this.compiledBlock = (ExecutableStatement) subCompileExpression(this.block = block);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        //   Object ret = null;


        while ((Boolean) condition.getValue(ctx, thisValue, factory)) {
            compiledBlock.getValue(ctx, thisValue, factory);
        }

        return null;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {

        while ((Boolean) condition.getValue(ctx, thisValue, factory)) {
            compiledBlock.getValue(ctx, thisValue, factory);
        }
        return null;
    }

}