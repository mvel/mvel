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
package org.mvel.optimizers.impl.refl.collection;

import org.mvel.compiler.Accessor;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.ParseTools;

/**
 * @author Christopher Brock
 */
public class ExprValueAccessor implements Accessor {
    public ExecutableStatement stmt;

    public ExprValueAccessor(String ex) {
        stmt = (ExecutableStatement) ParseTools.subCompileExpression(ex);
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        return stmt.getValue(elCtx, variableFactory);
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        // not implemented
        return null;
    }
}
