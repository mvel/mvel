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

import org.mvel.MVEL;
import static org.mvel.MVEL.executeSetExpression;
import org.mvel.Operator;
import org.mvel.ParserContext;
import org.mvel.CompileException;
import static org.mvel.compiler.AbstractParser.getCurrentThreadParserContext;
import org.mvel.compiler.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import static org.mvel.util.ParseTools.*;
import org.mvel.util.PropertyTools;
import org.mvel.util.StringAppender;
import static org.mvel.util.PropertyTools.createStringTrimmed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Christopher Brock
 */
public class ThisWithNode extends WithNode {

    public ThisWithNode(char[] expr, char[] block, int fields) {
        super(expr, block, fields);
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (thisValue == null) throw new CompileException("with-block against null pointer (this)");

        for (ParmValuePair pvp : withExpressions) {
            if (pvp.getSetExpression() != null) {
                executeSetExpression(pvp.getSetExpression(), thisValue, factory, pvp.getStatement().getValue(ctx, thisValue, factory));
            }
            else {
                pvp.getStatement().getValue(thisValue, thisValue, factory);
            }
        }
        return thisValue;
    }


    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return getReducedValueAccelerated(ctx, thisValue, factory);
    }
}