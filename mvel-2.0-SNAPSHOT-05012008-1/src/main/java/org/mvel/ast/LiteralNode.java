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

/**
 * @author Christopher Brock
 */
public class LiteralNode extends ASTNode {
    public LiteralNode(Object literal, Class type) {
        this(literal);
        this.egressType = type;
    }

    public LiteralNode(Object literal) {
        this.fields |= LITERAL;
        if (literal instanceof Integer) {
            this.fields |= INTEGER32;
            this.intRegister = ((Integer) (this.literal = literal));
        }
        else {
            this.literal = literal;
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return literal;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return literal;
    }

    public Object getLiteralValue() {
        return literal;
    }

    public void setLiteralValue(Object literal) {
        this.literal = literal;
    }

    public boolean isLiteral() {
        return true;
    }
}
