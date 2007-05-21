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

package org.mvel;

import static org.mvel.PropertyAccessor.get;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizationNotSupported;
import static org.mvel.optimizers.OptimizerFactory.*;
import static org.mvel.util.ArrayTools.findFirst;
import static org.mvel.util.ParseTools.handleEscapeSequence;
import static org.mvel.util.PropertyTools.handleNumericConversion;
import static org.mvel.util.PropertyTools.isNumber;
import org.mvel.util.ThisLiteral;

import java.io.Serializable;
import static java.lang.Class.forName;
import static java.lang.System.arraycopy;
import java.lang.reflect.Method;

public class ASTNode implements Cloneable, Serializable {
    public static final int LITERAL = 1;
    public static final int DEEP_PROPERTY = 1 << 1;
    public static final int OPERATOR = 1 << 2;
    public static final int IDENTIFIER = 1 << 3;
    public static final int SUBEVAL = 1 << 4;
    public static final int NUMERIC = 1 << 5;
    public static final int NEGATION = 1 << 6;
    public static final int INVERT = 1 << 8;
    public static final int FOLD = 1 << 9;
    public static final int STATICMETHOD = 1 << 10;

    public static final int ASSIGN = 1 << 12;
    public static final int LOOKAHEAD = 1 << 13;
    public static final int COLLECTION = 1 << 14;
    public static final int NEW = 1 << 15;
    public static final int CAPTURE_ONLY = 1 << 16;
    public static final int THISREF = 1 << 17;
    public static final int INLINE_COLLECTION = 1 << 18;
    public static final int NOCOMPILE = 1 << 19;
    public static final int STR_LITERAL = 1 << 20;
    public static final int PUSH = 1 << 21;

    public static final int BLOCK = 1 << 22;
    public static final int BLOCK_IF = 1 << 23;
    public static final int BLOCK_FOREACH = 1 << 24;
    public static final int BLOCK_WITH = 1 << 25;

    public static final int TYPED = 1 << 30;
    public static final int RETURN = 1 << 31;

    protected int firstUnion;
    protected int endOfName;

    protected int fields = 0;

    protected Class egressType;
    protected char[] name;
    protected String nameCache;

    protected Object literal;

    protected Accessor accessor;

    protected int cursorPosition;

    public ASTNode nextASTNode;

    protected boolean discard;

    public ASTNode(char[] expr, int start, int end, int fields) {
        this.cursorPosition = start;
        this.fields = fields;

        if ((fields & RETURN) != 0) {
            this.fields |= OPERATOR;
            literal = Operator.RETURN;
        }

        char[] name = new char[end - start];
        arraycopy(expr, start, name, 0, end - start);
        setName(name);
    }

    public ASTNode(char[] expr, int fields) {
        this.fields = fields;
        this.name = expr;
    }

    public ASTNode(int fields, Object literalValue) {
        this.fields = fields;
        this.literal = literalValue;
    }


    protected String getAbsoluteRootElement() {
        if ((fields & (DEEP_PROPERTY | COLLECTION)) != 0) {
            return new String(name, 0, getAbsoluteFirstPart());
        }
        return null;
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
        if ((fields & (COLLECTION | DEEP_PROPERTY)) != 0) {
            return new String(name, 0, getAbsoluteFirstPart());
        }
        else {
            return getName();
        }
    }

    public String getName() {
        if (nameCache != null) return nameCache;
        else if (name != null) return nameCache = new String(name);
        return "";
    }

    public Object getLiteralValue() {
        return literal;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if ((fields & (LITERAL)) != 0) {
            if ((fields & THISREF) != 0)
                return thisValue;
            else
                return literal;
        }
        try {
            return valRet(accessor.getValue(ctx, thisValue, factory));
        }
        catch (NullPointerException e) {
            //todo: FIX JIT, so we don't have to force safe reflective mode.
            AccessorOptimizer optimizer;
            Object retVal = null;

            if ((fields & SUBEVAL) != 0) {
                optimizer = getAccessorCompiler(SAFE_REFLECTIVE);
                accessor = (ExecutableStatement) MVEL.compileExpression(name);
                retVal = accessor.getValue(ctx, thisValue, factory);
            }
            else if ((fields & INLINE_COLLECTION) != 0) {
                optimizer = getDefaultAccessorCompiler();
                accessor = optimizer.optimizeCollection(name, ctx, thisValue, factory);
                retVal = accessor.getValue(ctx, thisValue, factory);
            }
            else if ((fields & FOLD) != 0) {
                optimizer = getAccessorCompiler(SAFE_REFLECTIVE);
                accessor = optimizer.optimizeFold(name, ctx, thisValue, factory);
                retVal = accessor.getValue(ctx, thisValue, factory);
            }
            else {
                try {
                    accessor = (optimizer = getDefaultAccessorCompiler()).optimizeAccessor(name, ctx, thisValue, factory, true);
                }
                catch (OptimizationNotSupported ne) {
                    accessor = (optimizer = getAccessorCompiler(SAFE_REFLECTIVE)).optimizeAccessor(name, ctx, thisValue, factory, true);
                }
            }

            if (accessor == null)
                throw new OptimizationFailure("failed optimization", e);

            if (retVal == null) {
                retVal = optimizer.getResultOptPass();
            }

            if (egressType == null) {
                egressType = optimizer.getEgressType();
            }

            return valRet(retVal);
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        // assert debug("REDUCE <<" + new String(name) + ">> ctx=" + ctx + ";literal=" + (fields & LITERAL) + ";assign=" + (fields & ASSIGN));
        String s;
        if ((fields & (LITERAL)) != 0) {
            if ((fields & THISREF) != 0)
                return thisValue;
            else
                return literal;
        }
        else if ((fields & SUBEVAL) != 0) {
            return valRet(MVEL.eval(name, ctx, factory));
        }

        else if ((fields & INLINE_COLLECTION) != 0) {
            if (accessor == null) {
                accessor = getAccessorCompiler(SAFE_REFLECTIVE)
                        .optimizeCollection(name, ctx, thisValue, factory);
            }

            return accessor.getValue(ctx, thisValue, factory);
        }
        else if ((fields & FOLD) != 0) {
            if (accessor == null) {
                AccessorOptimizer optimizer = getAccessorCompiler(SAFE_REFLECTIVE);
                accessor = optimizer.optimizeFold(name, ctx, thisValue, factory);

                return optimizer.getResultOptPass();
            }
        }

        if ((fields & DEEP_PROPERTY) != 0) {
            /**
             * The token is a DEEP PROPERTY (meaning it contains unions) in which case we need to traverse an object
             * graph.
             */
            if (AbstractParser.LITERALS.containsKey(s = getAbsoluteRootElement())) {
                /**
                 * The root of the DEEP PROPERTY is a literal.
                 */
                Object literal = AbstractParser.LITERALS.get(s);
                if (literal == ThisLiteral.class) literal = thisValue;

                return valRet(get(getAbsoluteRemainder(), literal, factory, thisValue));
            }
            else if (factory != null && factory.isResolveable(s)) {
                /**
                 * The root of the DEEP PROPERTY is a local or global var.
                 */
                return valRet(get(getAbsoluteRemainder(), factory.getVariableResolver(s).getValue(), factory, thisValue));

            }
            else if (ctx != null) {
                /**
                 * We didn't resolve the root, yet, so we assume that if we have a VROOT then the property must be
                 * accessible as a field of the VROOT.
                 */

                try {
                    return valRet(get(name, ctx, factory, thisValue));
                }
                catch (PropertyAccessException e) {
                    /**
                     * No luck. Make a last-ditch effort to resolve this as a static-class reference.
                     */
                    Object sa = tryStaticAccess(ctx, factory);
                    if (sa == null) throw e;
                    return valRet(sa);
                }
            }
        }
        else {
            if (factory != null && factory.isResolveable(s = getAbsoluteName())) {
                /**
                 * The token is a local or global var.
                 */

                if (isCollection()) {
                    return valRet(get(new String(name, endOfName, name.length - endOfName),
                            factory.getVariableResolver(s).getValue(), factory, thisValue));
                }

                return valRet(factory.getVariableResolver(s).getValue());
            }
            else if (ctx != null) {
                /**
                 * Check to see if the var exists in the VROOT.
                 */
                try {
                    return valRet(get(name, ctx, factory, thisValue));
                }
                catch (RuntimeException e) {
                    e.printStackTrace();
                    throw new UnresolveablePropertyException(this);
                }
            }
            else {
                if (isOperator()) {
                    throw new CompileException("incomplete statement");
                }
                else {
                    int mBegin = findFirst('(', name);
                    if (mBegin != -1) {
                        if (factory.isResolveable(s = new String(name, 0, mBegin))) {
                            Method m = (Method) factory.getVariableResolver(s).getValue();

                            return valRet(get(m.getName() + new String(name, mBegin, name.length - mBegin),
                                    m.getDeclaringClass(), factory, thisValue));
                        }
                    }
                }

                throw new UnresolveablePropertyException(this);
            }
        }

        return null;
    }


    protected Object valRet(final Object value) {
        if ((fields & NEGATION) != 0) {
            try {
                return !((Boolean) value);
            }
            catch (Exception e) {
                throw new CompileException("illegal negation of non-boolean value");
            }
        }
        else if ((fields & INVERT) != 0) {
            try {
                return ~((Integer) value);
            }
            catch (Exception e) {
                throw new CompileException("bitwise (~) operator can only be applied to integers");
            }
        }


        return value;
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
                    case'.':
                        if (!meth) {
                            try {
                                return get(new String(name, last, name.length - last), forName(new String(name, 0, last)), factory, thisRef);
                            }
                            catch (ClassNotFoundException e) {
                                return get(new String(name, i + 1, name.length - i - 1), forName(new String(name, 0, i)), factory, thisRef);
                            }
                        }
                        meth = false;
                        last = i;
                        break;
                    case')':
                        if (depth++ == 0)
                            meth = true;
                        break;
                    case'(':
                        depth--;
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
    private void setName(char[] name) {
        if ((fields & RETURN) != 0) {
            this.name = name;
            return;
        }
        if ((fields & STR_LITERAL) != 0) {
            fields |= LITERAL;

            int escapes = 0;
            for (int i = 0; i < name.length; i++) {
                if (name[i] == '\\') {
                    name[i++] = 0;
                    name[i] = handleEscapeSequence(name[i]);
                    escapes++;
                }
            }

            char[] processedEscapeString = new char[name.length - escapes];
            int cursor = 0;
            for (char aName : name) {
                if (aName == 0) {
                    continue;
                }
                processedEscapeString[cursor++] = aName;
            }

            this.literal = new String(this.name = processedEscapeString);

        }
        else {
            this.literal = new String(this.name = name);
        }

        if ((fields & (SUBEVAL | LITERAL)) != 0) {
            //    return;
        }
        else if (AbstractParser.LITERALS.containsKey(literal)) {
            fields |= LITERAL | IDENTIFIER;
            if ((literal = AbstractParser.LITERALS.get(literal)) == ThisLiteral.class) fields |= THISREF;
        }
        else if (AbstractParser.OPERATORS.containsKey(literal)) {
            fields |= OPERATOR;
            literal = AbstractParser.OPERATORS.get(literal);
            return;
        }
        else if (isNumber(name)) {
            fields |= NUMERIC | LITERAL | IDENTIFIER;
            literal = handleNumericConversion(name);

            if ((fields & INVERT) != 0) {
                try {
                    literal = ~((Integer) literal);
                }
                catch (ClassCastException e) {
                    throw new CompileException("bitwise (~) operator can only be applied to integers");
                }
            }
            return;
        }
        else if ((fields & INLINE_COLLECTION) != 0) {
            return;
        }
        else if ((firstUnion = findFirst('.', name)) > 0) {
            fields |= DEEP_PROPERTY | IDENTIFIER;
        }
        else {
            fields |= IDENTIFIER;
        }

        if ((endOfName = findFirst('[', name)) > 0) fields |= COLLECTION;

    }

    public void setAccessor(Accessor accessor) {
        this.accessor = accessor;
    }

    public boolean isIdentifier() {
        return (fields & IDENTIFIER) != 0;
    }

    public boolean isSubeval() {

        return (fields & SUBEVAL) != 0;
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
        return (Integer) literal;
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


    public int getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }


    public boolean isDiscard() {
        return discard;
    }

    public void setDiscard(boolean discard) {
        this.discard = discard;
    }
}


