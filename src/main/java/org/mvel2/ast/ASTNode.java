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
import static org.mvel2.Operator.NOOP;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import static org.mvel2.PropertyAccessor.get;
import org.mvel2.compiler.Accessor;
import org.mvel2.debug.DebugTools;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AccessorOptimizer;
import org.mvel2.optimizers.OptimizationNotSupported;
import static org.mvel2.optimizers.OptimizerFactory.*;
import static org.mvel2.util.CompilerTools.getInjectedImports;
import static org.mvel2.util.ParseTools.handleNumericConversion;
import static org.mvel2.util.ParseTools.isNumber;

import java.io.Serializable;
import static java.lang.Thread.currentThread;

@SuppressWarnings({"ManualArrayCopy", "CaughtExceptionImmediatelyRethrown"})
public class ASTNode implements Cloneable, Serializable {
    public static final int LITERAL = 1;
    public static final int DEEP_PROPERTY = 1 << 1;
    public static final int OPERATOR = 1 << 2;
    public static final int IDENTIFIER = 1 << 3;
    public static final int COMPILE_IMMEDIATE = 1 << 4;
    public static final int NUMERIC = 1 << 5;

    public static final int INVERT = 1 << 6;
    public static final int ASSIGN = 1 << 7;

    public static final int COLLECTION = 1 << 8;
    public static final int THISREF = 1 << 9;
    public static final int INLINE_COLLECTION = 1 << 10;

    public static final int BLOCK_IF = 1 << 11;
    public static final int BLOCK_FOREACH = 1 << 12;
    public static final int BLOCK_WITH = 1 << 13;
    public static final int BLOCK_UNTIL = 1 << 14;
    public static final int BLOCK_WHILE = 1 << 15;
    public static final int BLOCK_DO = 1 << 16;
    public static final int BLOCK_DO_UNTIL = 1 << 17;
    public static final int BLOCK_FOR = 1 << 18;

    public static final int DEFERRED_TYPE_RES = 1 << 23;
    public static final int STRONG_TYPING = 1 << 24;
    public static final int PCTX_STORED = 1 << 25;
    public static final int ARRAY_TYPE_LITERAL = 1 << 26;

    public static final int NOJIT = 1 << 27;
    public static final int DEOP = 1 << 28;

    public static final int DISCARD = 1 << 29;

    // *** //

    protected int firstUnion;
    protected int endOfName;

    public int fields = 0;

    protected Class egressType;
    protected char[] name;
    protected String nameCache;

    protected Object literal;

    protected transient volatile Accessor accessor;
    protected volatile Accessor safeAccessor;

    protected int cursorPosition;
    public ASTNode nextASTNode;

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (accessor != null) {
            try {
                return accessor.getValue(ctx, thisValue, factory);
            }
            catch (ClassCastException ce) {
                if ((fields & DEOP) == 0) {
                    accessor = null;
                    fields |= DEOP | NOJIT;

                    synchronized (this) {
                        return getReducedValueAccelerated(ctx, thisValue, factory);
                    }
                }
                else {
                    throw ce;
                }
            }
        }
        else {
            if ((fields & DEOP) != 0) {
                fields ^= DEOP;
            }

            AccessorOptimizer optimizer;
            Object retVal = null;

            if ((fields & NOJIT) != 0) {
                optimizer = getAccessorCompiler(SAFE_REFLECTIVE);
            }
            else {
                optimizer = getDefaultAccessorCompiler();
            }

            ParserContext pCtx;

            if ((fields & PCTX_STORED) != 0) {
                pCtx = (ParserContext) literal;
            }
            else {
                pCtx = new ParserContext(new ParserConfiguration(getInjectedImports(factory), null));
            }
            
            try {
                setAccessor(optimizer.optimizeAccessor(pCtx, name, ctx, thisValue, factory, true, egressType));
            }
            catch (OptimizationNotSupported ne) {
                setAccessor((optimizer = getAccessorCompiler(SAFE_REFLECTIVE))
                        .optimizeAccessor(pCtx, name, ctx, thisValue, factory, true, null));
            }


            if (accessor == null) {
                return get(name, ctx, factory, thisValue);
            }

            if (retVal == null) {
                retVal = optimizer.getResultOptPass();
            }

            if (egressType == null) {
                egressType = optimizer.getEgressType();
            }

            return retVal;
        }
    }


    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((fields & (LITERAL)) != 0) {
            return literal;
        }
        else {
            return get(name, ctx, factory, thisValue);
        }
    }

    protected String getAbsoluteRootElement() {
        if ((fields & (DEEP_PROPERTY | COLLECTION)) != 0) {
            return new String(name, 0, getAbsoluteFirstPart());
        }
        return nameCache;
    }

    public Class getEgressType() {
        return egressType;
    }

    public void setEgressType(Class egressType) {
        this.egressType = egressType;
    }

    protected String getAbsoluteRemainder() {
        return (fields & COLLECTION) != 0 ? new String(name, endOfName, name.length - endOfName)
                : ((fields & DEEP_PROPERTY) != 0 ? new String(name, firstUnion + 1, name.length - firstUnion - 1) : null);
    }

    public char[] getNameAsArray() {
        return name;
    }

    private int getAbsoluteFirstPart() {
        if ((fields & COLLECTION) != 0) {
            if (firstUnion < 0 || endOfName < firstUnion) return endOfName;
            else return firstUnion;
        }
        else if ((fields & DEEP_PROPERTY) != 0) {
            return firstUnion;
        }
        else {
            return -1;
        }
    }

    public String getAbsoluteName() {
        if (firstUnion > 0) {
            return new String(name, 0, getAbsoluteFirstPart());
        }
        else {
            return getName();
        }
    }

    public String getName() {
        if (nameCache != null) {
            return nameCache;
        }
        else if (name != null) {
            return nameCache = new String(name);
        }
        return "";
    }

    public Object getLiteralValue() {
        return literal;
    }

    public void storeInLiteralRegister(Object o) {
        this.literal = o;

    }

    public void setLiteralValue(Object literal) {
        this.literal = literal;
        this.fields |= LITERAL;
    }

    protected Object tryStaticAccess(Object thisRef, VariableResolverFactory factory) {
        try {
            /**
             * Try to resolve this *smartly* as a static class reference.
             *
             * This starts at the end of the token and starts to step backwards to figure out whether
             * or not this may be a static class reference.  We search for method calls simply by
             * inspecting for ()'s.  The first union area we come to where no brackets are present is our
             * test-point for a class reference.  If we find a class, we pass the reference to the
             * property accessor along  with trailing methods (if any).
             *
             */
            boolean meth = false;
            int depth = 0;
            int last = name.length;
            for (int i = last - 1; i > 0; i--) {
                switch (name[i]) {
                    case '.':
                        if (depth == 0 && !meth) {
                            try {
                                Class.forName(new String(name, 0, i), true, currentThread().getContextClassLoader());

                                return get(new String(name, last, name.length - last),
                                        Class.forName(new String(name, 0, last), true, currentThread().getContextClassLoader()), factory, thisRef);
                            }
                            catch (ClassNotFoundException e) {
                                return get(new String(name, i + 1, name.length - i - 1),
                                        Class.forName(new String(name, 0, i), true, currentThread().getContextClassLoader()), factory, thisRef);
                            }
                        }
                        meth = false;
                        last = i;
                        break;
                    case ')':
                        depth++;
                        break;
                    case '(':
                        if (--depth == 0) meth = true;
                        break;
                }
            }
        }
        catch (Exception cnfe) {
            // do nothing.
        }

        return null;
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    protected void setName(char[] name) {
        if (isNumber(name)) {
            egressType = (literal = handleNumericConversion(name)).getClass();
            if (((fields |= NUMERIC | LITERAL | IDENTIFIER) & INVERT) != 0) {
                try {
                    literal = ~((Integer) literal);
                }
                catch (ClassCastException e) {
                    throw new CompileException("bitwise (~) operator can only be applied to integers");
                }
            }
            return;
        }

        this.literal = new String(name);


        Scan:
        for (int i = 0; i < name.length; i++) {
            switch (name[i]) {
                case '.':
                    if (firstUnion == 0) {
                        firstUnion = i;
                    }
                    break;
                case '[':
                    if (firstUnion == 0) {
                        firstUnion = i;
                    }
                    if (endOfName == 0) {
                        endOfName = i;
                        if (i < name.length && name[i+1] == ']') fields |= ARRAY_TYPE_LITERAL;
                        break Scan;
                    }
            }
        }

        if ((fields & INLINE_COLLECTION) != 0) {
            return;
        }

        if (firstUnion > 0) {
            fields |= DEEP_PROPERTY | IDENTIFIER;
        }
        else {
            fields |= IDENTIFIER;
        }
    }

    public Accessor setAccessor(Accessor accessor) {
        return this.accessor = accessor;
    }

    public boolean isIdentifier() {
        return (fields & IDENTIFIER) != 0;
    }

    public boolean isLiteral() {
        return (fields & LITERAL) != 0;
    }

    public boolean isThisVal() {
        return (fields & THISREF) != 0;
    }

    public boolean isOperator() {
        return (fields & OPERATOR) != 0;
    }

    public boolean isOperator(Integer operator) {
        return (fields & OPERATOR) != 0 && operator.equals(literal);
    }

    public Integer getOperator() {
        return NOOP;
    }

    protected boolean isCollection() {
        return (fields & COLLECTION) != 0;
    }

    public boolean isAssignment() {
        return ((fields & ASSIGN) != 0);
    }

    public boolean isDeepProperty() {
        return ((fields & DEEP_PROPERTY) != 0);
    }

    public void setAsLiteral() {
        fields |= LITERAL;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }

    public boolean isDiscard() {
        return fields != -1 && (fields & DISCARD) != 0;
    }

    public void discard() {
        this.fields |= DISCARD;
    }

    public void strongTyping() {
        this.fields |= STRONG_TYPING;
    }

    public void storePctx() {
        this.fields |= PCTX_STORED;
    }

    public boolean isDebuggingSymbol() {
        return this.fields == -1;
    }

    public int getFields() {
        return fields;
    }

    public Accessor getAccessor() {
        return accessor;
    }

    public boolean canSerializeAccessor() {
        return safeAccessor != null;
    }

    public ASTNode() {
    }

    public ASTNode(char[] expr, int start, int end, int fields) {
        this.fields = fields;

        name = new char[end - (this.cursorPosition = start)];
        for (int i = 0; i < name.length; i++) {
            name[i] = expr[i + start];
        }

        setName(name);
    }

    public String toString() {
        return isOperator() ? "<<" + DebugTools.getOperatorName(getOperator()) + ">>" : String.valueOf(literal);
    }
}


