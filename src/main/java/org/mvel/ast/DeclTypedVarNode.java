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

import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.checkNameSafety;

/**
 * @author Christopher Brock
 */
public class DeclTypedVarNode extends ASTNode implements Assignment {
    private String name;

    public DeclTypedVarNode(String name, int fields, Class type) {
        //  super(null, fields);
        this.egressType = type;

        checkNameSafety(this.name = name);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        factory.createVariable(name, null, egressType);
        return null;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        factory.createVariable(name, null, egressType);
        return null;
    }

    public String getName() {
        return name;
    }

    public String getAssignmentVar() {
        return name;
    }

    public char[] getExpression() {
        return new char[0];
    }

    public boolean isAssignment() {
        return true;
    }
}