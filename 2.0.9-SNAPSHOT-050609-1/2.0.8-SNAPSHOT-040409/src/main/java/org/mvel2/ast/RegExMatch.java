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
import org.mvel2.ParserContext;
import static org.mvel2.MVEL.eval;
import org.mvel2.compiler.ExecutableLiteral;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import static org.mvel2.util.ParseTools.subCompileExpression;

import static java.lang.String.valueOf;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.compile;
import java.util.regex.PatternSyntaxException;

public class RegExMatch extends ASTNode {
    private ExecutableStatement stmt;
    private ExecutableStatement patternStmt;
    private char[] pattern;
    private Pattern p;

    public RegExMatch(char[] expr, int fields, char[] pattern, ParserContext pCtx) {
        this.name = expr;
        this.pattern = pattern;

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            this.stmt = (ExecutableStatement) subCompileExpression(expr);
            if ((this.patternStmt = (ExecutableStatement)
                    subCompileExpression(pattern, pCtx)) instanceof ExecutableLiteral) {

                try {
                    p = compile(valueOf(patternStmt.getValue(null, null)));
                }
                catch (PatternSyntaxException e) {
                    throw new CompileException("bad regular expression", e);
                }
            }
        }
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (p == null) {
            return compile(valueOf(patternStmt.getValue(ctx, thisValue, factory))).matcher(valueOf(stmt.getValue(ctx, thisValue, factory))).matches();
        }
        else {
            return p.matcher(valueOf(stmt.getValue(ctx, thisValue, factory))).matches();
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        try {
            return compile(valueOf(eval(pattern, ctx, factory))).matcher(valueOf(eval(name, ctx, factory))).matches();
        }
        catch (PatternSyntaxException e) {
            throw new CompileException("bad regular expression", e);
        }
    }

    public Class getEgressType() {
        return Boolean.class;
    }
}
