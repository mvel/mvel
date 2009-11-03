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

import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.DataConversion.convert;
import static org.mvel2.MVEL.eval;

import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;

import static org.mvel2.util.ParseTools.subCompileExpression;

import org.mvel2.util.CompilerTools;
import org.mvel2.ParserContext;
import org.mvel2.DataConversion;
import org.mvel2.CompileException;

public class TypeCast extends ASTNode {
    private ExecutableStatement statement;
    private boolean widen;

    public TypeCast(char[] expr, Class cast, int fields, ParserContext pCtx) {
        this.egressType = cast;
        this.name = expr;
        if ((fields & COMPILE_IMMEDIATE) != 0) {

            if ((statement = (ExecutableStatement) subCompileExpression(name, pCtx)).getKnownEgressType() != Object.class
                    && !canConvert(cast, statement.getKnownEgressType())) {

                if (statement.getKnownEgressType().isAssignableFrom(cast)) {
                    widen = true;
                } else {
                    throw new CompileException("unable to cast type: " + statement.getKnownEgressType() + "; to: " + cast);
                }
            }

        }
    }


    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        //noinspection unchecked
        return widen ? typeCheck(statement.getValue(ctx, thisValue, factory), egressType) : convert(statement.getValue(ctx, thisValue, factory), egressType);
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        //noinspection unchecked
        return widen ? typeCheck(eval(name, ctx, factory), egressType) : convert(eval(name, ctx, factory), egressType);
    }

    private static Object typeCheck(Object inst, Class type) {
        if (inst == null) return null;
        if (type.isInstance(inst)) {
            return inst;
        }
        else {
            throw new ClassCastException(inst.getClass().getName() + " cannot be cast to: " + type.getClass().getName());
        }
    }
}
