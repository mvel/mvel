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

import static org.mvel.Operator.*;
import static org.mvel.PropertyAccessor.get;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizationNotSupported;
import org.mvel.optimizers.OptimizerFactory;
import static org.mvel.optimizers.OptimizerFactory.SAFE_REFLECTIVE;
import static org.mvel.util.ArrayTools.findFirst;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.getBigDecimalFromType;
import static org.mvel.util.ParseTools.handleEscapeSequence;
import static org.mvel.util.PropertyTools.handleNumericConversion;
import static org.mvel.util.PropertyTools.isNumber;
import org.mvel.util.ThisLiteral;

import java.io.Serializable;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Class.forName;
import java.util.HashMap;
import java.util.Map;

public class Token implements Cloneable, Serializable {
    public static final int LITERAL = 1;
    public static final int DEEP_PROPERTY = 1 << 1;
    public static final int OPERATOR = 1 << 2;
    public static final int IDENTIFIER = 1 << 3;
    public static final int SUBEVAL = 1 << 4;
    public static final int NUMERIC = 1 << 5;
    public static final int NEGATION = 1 << 6;
    public static final int INVERT = 1 << 8;
    public static final int FOLD = 1 << 9;

    public static final int ASSIGN = 1 << 12;
    public static final int LOOKAHEAD = 1 << 13;
    public static final int COLLECTION = 1 << 14;
    public static final int NEW = 1 << 15;
    public static final int CAPTURE_ONLY = 1 << 17;
    public static final int THISREF = 1 << 19;
    public static final int INLINE_COLLECTION = 1 << 20;
    public static final int NOCOMPILE = 1 << 21;
    public static final int STR_LITERAL = 1 << 25;
    public static final int PUSH = 1 << 22;

    private int firstUnion;
    private int endOfName;

    private char[] name;
    private String nameCache;

    private Object literal;
    private int knownType = 0;

    private int fields = 0;

    private Accessor accessor;

    public Token nextToken;

    public static final Map<String, Object> LITERALS =
            new HashMap<String, Object>(35, 0.6f);

    static {

        /**
         * Setup the basic literals
         */
        LITERALS.put("true", TRUE);
        LITERALS.put("false", FALSE);

        LITERALS.put("null", null);
        LITERALS.put("nil", null);

        LITERALS.put("empty", BlankLiteral.INSTANCE);

        LITERALS.put("this", ThisLiteral.class);

        /**
         * Add System and all the class wrappers from the JCL.
         */
        LITERALS.put("System", System.class);

        LITERALS.put("String", String.class);
        LITERALS.put("Integer", Integer.class);
        LITERALS.put("Long", Long.class);
        LITERALS.put("Boolean", Boolean.class);
        LITERALS.put("Short", Short.class);
        LITERALS.put("Character", Character.class);
        LITERALS.put("Double", Double.class);
        LITERALS.put("Float", Float.class);
        LITERALS.put("Math", Math.class);
        LITERALS.put("Void", Void.class);
        LITERALS.put("Object", Object.class);

        LITERALS.put("Class", Class.class);
        LITERALS.put("ClassLoader", ClassLoader.class);
        LITERALS.put("Runtime", Runtime.class);
        LITERALS.put("Thread", Thread.class);
        LITERALS.put("Compiler", Compiler.class);
        LITERALS.put("StringBuffer", StringBuffer.class);
        LITERALS.put("ThreadLocal", ThreadLocal.class);
        LITERALS.put("SecurityManager", SecurityManager.class);
        LITERALS.put("StrictMath", StrictMath.class);

        LITERALS.put("Array", java.lang.reflect.Array.class);

        float version = Float.parseFloat(System.getProperty("java.version").substring(0, 2));
        if (version >= 1.5) {
            try {
                LITERALS.put("StringBuilder", Class.forName("java.lang.StringBuilder"));
            }
            catch (Exception e) {
                throw new RuntimeException("cannot resolve a built-in literal", e);
            }
        }
    }

    private static final Map<String, Integer> OPERATORS =
            new HashMap<String, Integer>(25 * 2, 0.6f);

    static {
        OPERATORS.put("+", ADD);
        OPERATORS.put("-", SUB);
        OPERATORS.put("*", MULT);
        OPERATORS.put("/", DIV);
        OPERATORS.put("%", MOD);
        OPERATORS.put("==", EQUAL);
        OPERATORS.put("!=", NEQUAL);
        OPERATORS.put(">", GTHAN);
        OPERATORS.put(">=", GETHAN);
        OPERATORS.put("<", LTHAN);
        OPERATORS.put("<=", LETHAN);
        OPERATORS.put("&&", AND);
        OPERATORS.put("and", AND);
        OPERATORS.put("||", OR);
        OPERATORS.put("or", CHOR);
        OPERATORS.put("~=", REGEX);
        OPERATORS.put("instanceof", INSTANCEOF);
        OPERATORS.put("is", INSTANCEOF);
        OPERATORS.put("contains", CONTAINS);
        OPERATORS.put("soundslike", SOUNDEX);
        OPERATORS.put("strsim", SIMILARITY);
        OPERATORS.put("convertable_to", CONVERTABLE_TO);

        OPERATORS.put("#", STR_APPEND);

        OPERATORS.put("&", BW_AND);
        OPERATORS.put("|", BW_OR);
        OPERATORS.put("^", BW_XOR);
        OPERATORS.put("<<", BW_SHIFT_LEFT);
        OPERATORS.put("<<<", BW_USHIFT_LEFT);
        OPERATORS.put(">>", BW_SHIFT_RIGHT);
        OPERATORS.put(">>>", BW_USHIFT_RIGHT);

        OPERATORS.put("?", Operator.TERNARY);
        OPERATORS.put(":", TERNARY_ELSE);

        OPERATORS.put("=", Operator.ASSIGN);

        OPERATORS.put(";", END_OF_STMT);

        OPERATORS.put("new", NEW);

        OPERATORS.put("in", PROJECTION);
    }


    public Token(char[] expr, int start, int end, int fields) {
        this.fields = fields;

        char[] name = new char[end - start];
        System.arraycopy(expr, start, name, 0, end - start);
        setName(name);
    }

    public Token(char expr, int fields) {
        this.fields = fields;
        setName(new char[]{expr});
    }

    public Token(char[] expr, int fields) {
        this.fields = fields;
        setName(expr);
    }

    public Token(int fields, Object literalValue) {
        this.fields = fields;
        this.literal = literalValue;
    }


    private String getAbsoluteRootElement() {
        if ((fields & (DEEP_PROPERTY | COLLECTION)) != 0) {
            return new String(name, 0, getAbsoluteFirstPart());
        }
        return null;
    }


    private String getRemainder() {
        return (fields & DEEP_PROPERTY) != 0 ? new String(name, firstUnion + 1, name.length - firstUnion - 1) : null;
    }

    public char[] getNameAsArray() {
        return name;
    }

    private int getAbsoluteFirstPart() {
        if ((fields & Token.COLLECTION) != 0) {
            if (firstUnion < 0 || endOfName < firstUnion) return endOfName;
            else return firstUnion;
        }
        else if ((fields & Token.DEEP_PROPERTY) != 0) {
            return firstUnion;
        }
        else {
            return -1;
        }

    }

    private String getAbsoluteName() {
        if ((fields & COLLECTION) != 0) {
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

            if ((fields & ASSIGN) != 0) {
                optimizer = OptimizerFactory.getDefaultAccessorCompiler();
                accessor = optimizer.optimizeAssignment(name, ctx, thisValue, factory);
                retVal = accessor.getValue(ctx, thisValue, factory);
            }
            else if ((fields & SUBEVAL) != 0) {
                optimizer = OptimizerFactory.getAccessorCompiler(SAFE_REFLECTIVE);
                accessor = (ExecutableStatement) MVEL.compileExpression(name);
                retVal = accessor.getValue(ctx, thisValue, factory);
            }
            else if ((fields & INLINE_COLLECTION) != 0) {
                optimizer = OptimizerFactory.getDefaultAccessorCompiler();
                accessor = optimizer.optimizeCollection(name, ctx, thisValue, factory);
                retVal = accessor.getValue(ctx, thisValue, factory);
            }
            else if ((fields & FOLD) != 0) {
                optimizer = OptimizerFactory.getAccessorCompiler(SAFE_REFLECTIVE);
                accessor = optimizer.optimizeFold(name, ctx, thisValue, factory);
                retVal = accessor.getValue(ctx, thisValue, factory);
            }
            else if ((fields & NEW) != 0) {
                optimizer = OptimizerFactory.getDefaultAccessorCompiler();
                accessor = optimizer.optimizeObjectCreation(name, ctx, thisValue, factory);
                retVal = accessor.getValue(ctx, thisValue, factory);
            }
            else {
                try {
                    accessor = (optimizer = OptimizerFactory.getDefaultAccessorCompiler()).optimize(name, ctx, thisValue, factory, true);
                }
                catch (OptimizationNotSupported ne) {
                    accessor = (optimizer = OptimizerFactory.getAccessorCompiler(SAFE_REFLECTIVE)).optimize(name, ctx, thisValue, factory, true);
                }
            }

            if (accessor == null)
                throw new OptimizationFailure("failed optimization", e);

            if (retVal == null) {
                retVal = optimizer.getResultOptPass();
            }

            knownType = ParseTools.resolveType(retVal == null ? null : retVal.getClass());

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
        else if ((fields & ASSIGN) != 0) {
            // assert debug("TK_ASSIGN");

            if (accessor == null) {
                accessor = OptimizerFactory.getAccessorCompiler(SAFE_REFLECTIVE)
                        .optimizeAssignment(name, ctx, thisValue, factory);
            }

            return accessor.getValue(ctx, thisValue, factory);
        }
        else if ((fields & SUBEVAL) != 0) {
            // assert debug("TK_SUBEVAL");

            return valRet(MVEL.eval(name, ctx, factory));
        }

        else if ((fields & INLINE_COLLECTION) != 0) {
            // assert debug("TK_INLINE_COLLECTION");

            if (accessor == null) {
                accessor = OptimizerFactory.getAccessorCompiler(SAFE_REFLECTIVE)
                        .optimizeCollection(name, ctx, thisValue, factory);
            }

            return accessor.getValue(ctx, thisValue, factory);
        }
        else if ((fields & FOLD) != 0) {
            // assert debug("FOLD");

            if (accessor == null) {
                AccessorOptimizer optimizer = OptimizerFactory.getAccessorCompiler(SAFE_REFLECTIVE);
                accessor = optimizer.optimizeFold(name, ctx, thisValue, factory);

                return optimizer.getResultOptPass();
            }
        }
        else if ((fields & NEW) != 0) {
            // assert debug("NEW");

            if (accessor == null) {
                AccessorOptimizer optimizer = OptimizerFactory.getAccessorCompiler(SAFE_REFLECTIVE);
                accessor = optimizer.optimizeObjectCreation(name, ctx, thisValue, factory);

                return optimizer.getResultOptPass();
            }
        }

        if ((fields & Token.DEEP_PROPERTY) != 0) {
            /**
             * The token is a DEEP PROPERTY (meaning it contains unions) in which case we need to traverse an object
             * graph.
             */
            if (Token.LITERALS.containsKey(s = getAbsoluteRootElement())) {
                /**
                 * The root of the DEEP PROPERTY is a literal.
                 */
                Object literal = Token.LITERALS.get(s);
                if (literal == ThisLiteral.class) literal = thisValue;

                return valRet(get(getRemainder(), literal, factory, thisValue));
            }
            else if (factory != null && factory.isResolveable(s)) {
                /**
                 * The root of the DEEP PROPERTY is a local or global var.
                 */
                return valRet(get(getRemainder(), factory.getVariableResolver(s).getValue(), factory, thisValue));

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
            else {
                Object sa = tryStaticAccess(ctx, factory);
                if (sa == null) throw new CompileException("unable to resolve token: " + s);
                return valRet(sa);
            }
        }
        else {
            if (Token.LITERALS.containsKey(s = getAbsoluteName())) {
                /**
                 * The token is actually a literal.
                 */
                return valRet(Token.LITERALS.get(s));
            }
            else if (factory != null && factory.isResolveable(s)) {
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
                throw new UnresolveablePropertyException(this);
            }
        }
    }


    private Object valRet(Object value) {
        if ((fields & (NEGATION | INVERT)) == 0) return value;
        else if ((fields & NEGATION) != 0) {
            if (value instanceof Boolean) {
                return !((Boolean) value);
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

    private Object tryStaticAccess(Object thisRef, VariableResolverFactory factory) {
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
        else if (LITERALS.containsKey(literal)) {
            fields |= LITERAL;
            if ((literal = LITERALS.get(literal)) == ThisLiteral.class) fields |= THISREF;
        }
        else if (OPERATORS.containsKey(literal)) {
            fields |= OPERATOR;
            literal = OPERATORS.get(literal);
            return;
        }
        else if (isNumber(name)) {
            fields |= NUMERIC | LITERAL;
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
        else if ((firstUnion = findFirst('.', name)) > 0) {
            fields |= DEEP_PROPERTY | IDENTIFIER;
        }
        else {
            fields |= IDENTIFIER;
        }

        if ((endOfName = findFirst('[', name)) > 0) fields |= COLLECTION;

    }

    public int getFlags() {
        return fields;
    }


    public String toString() {
        return isOperator() ? "OPCODE_" + getOperator() : new String(name);
    }

    public boolean equals(Object obj) {
        if (obj instanceof Token)
            return literal == null ? ((Token) obj).literal == null : literal.equals(((Token) obj).literal);
        else
            return literal == null ? obj == literal : literal.equals(obj);
    }

    public int hashCode() {
        return literal == null ? super.hashCode() : literal.hashCode();
    }


    public void setAccessor(Accessor accessor) {
        this.accessor = accessor;
    }

    public Token clone() throws CloneNotSupportedException {
        try {
            return (Token) super.clone();
        }
        catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
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

    private boolean isCollection() {
        return (fields & COLLECTION) != 0;
    }

    public boolean isNewObject() {
        return ((fields & NEW) != 0);
    }

}


