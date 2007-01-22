package org.mvel;

import static org.mvel.DataConversion.convert;
import static org.mvel.Operator.*;
import static org.mvel.PropertyAccessor.get;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorCompiler;
import org.mvel.optimizers.ExecutableStatement;
import org.mvel.optimizers.OptimizationNotSupported;
import org.mvel.optimizers.OptimizerFactory;
import static org.mvel.optimizers.OptimizerFactory.SAFE_REFLECTIVE;
import org.mvel.optimizers.impl.refl.Deferral;
import static org.mvel.util.ArrayTools.findFirst;
import org.mvel.util.ParseTools;
import static org.mvel.util.ParseTools.handleEscapeSequence;
import static org.mvel.util.ParseTools.valueOnly;
import static org.mvel.util.PropertyTools.isNumber;
import org.mvel.util.ThisLiteral;

import java.io.Serializable;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Class.forName;
import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import java.math.BigDecimal;
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
    public static final int EVAL_RIGHT = 1 << 7;
    public static final int INVERT = 1 << 8;
    public static final int REQUIRE_REDUCTION = 1 << 9;
    public static final int BOOLEAN_MODE = 1 << 10;
    public static final int TERNARY = 1 << 11;
    public static final int ASSIGN = 1 << 12;
    public static final int LOOKAHEAD = 1 << 13;
    public static final int COLLECTION = 1 << 14;
    public static final int LISTCREATE = 1 << 15;
    public static final int DO_NOT_REDUCE = 1 << 16;
    public static final int CAPTURE_ONLY = 1 << 17;
    public static final int MAPCREATE = 1 << 18;
    public static final int THISREF = 1 << 19;
    public static final int ARRAYCREATE = 1 << 20;
    public static final int NOCOMPILE = 1 << 21;
    public static final int STR_LITERAL = 1 << 25;
    public static final int FLOATING_NUMERIC = 1 << 26;

    public static final int PUSH = 1 << 22;

    public static final int NEST = 1 << 23;   // token begins a nesting area
    public static final int ENDNEST = 1 << 24; // token ends a nesting area

    public static final int OPTIMIZED_REF = 1 << 31; // future use

    private int start;
    private int end;
    private int firstUnion;
    private int endOfName;

    private char[] name;
    private String nameCache;

    private transient Object value;
    private transient Object resetValue;

    private BigDecimal numericValue;
    private Class srcType;

    private int fields = 0;

    private ExecutableStatement compiledExpression;
    private Accessor accessor;
    private int knownSize = 0;

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
        System.arraycopy(expr, this.start = start, name, 0, (this.end = end) - start);
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

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public boolean emptyRange() {
        return this.start == this.end;
    }


    public char[] createRootElementArray() {
        if ((fields & DEEP_PROPERTY) != 0) {
            char[] root = new char[(firstUnion)];
            System.arraycopy(name, 0, root, 0, root.length);
            return root;
        }
        return null;
    }

    public String getAbsoluteRootElement() {
        if ((fields & (DEEP_PROPERTY | COLLECTION)) != 0) {
            return new String(name, 0, getAbsoluteFirstPart());
        }
        return null;
    }

    public String getRootElement() {
        return (fields & DEEP_PROPERTY) != 0 ? new String(name, 0, firstUnion) : getName();
        //   return new String(root);
    }

    public char[] createRemainderArray() {
        if ((fields & DEEP_PROPERTY) != 0) {
            char[] remainder = new char[(name.length - firstUnion - 1)];
            System.arraycopy(name, firstUnion + 1, remainder, 0, remainder.length);
            return remainder;
        }
        return null;
    }


    public String getRemainder() {
        //   return new String(remainder);
        return (fields & DEEP_PROPERTY) != 0 ? new String(name, firstUnion + 1, name.length - firstUnion - 1) : null;
    }

    public char[] getNameAsArray() {
        return name;
    }


    public int getEndOfName() {
        return endOfName;
    }

    public int getAbsoluteFirstPart() {
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

    public String getAbsoluteName() {
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

    public Object getValue() {
        return value;
    }

    public Token getOptimizedValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        try {
            if ((fields & NUMERIC) != 0) {
                value = accessor.getValue(ctx, elCtx, variableFactory);
                if (srcType == null && value != null) {
                    srcType = value.getClass();
                }

                if (srcType == String.class) {
                    value = numericValue = new BigDecimal((String) value);
                }
                else if (srcType == BigDecimal.class) {
                    numericValue = (BigDecimal) value;
                }
                else if (srcType == Integer.class) {
                    value = numericValue = new BigDecimal((Integer) value);
                }
                else if (srcType == Long.class) {
                    value = numericValue = new BigDecimal((Long) value);
                }
                else if (srcType == Short.class) {
                    value = numericValue = new BigDecimal((Short) value);
                }
                else if (srcType == Float.class) {
                    value = numericValue = new BigDecimal((Float) value);
                }
                else if (srcType == Double.class) {
                    value = numericValue = new BigDecimal((Double) value);
                }
                else {
                    throw new CompileException("expected numeric value: got: " + value);
                }


                // value = numericValue = convert(accessor.getValue(ctx, elCtx, variableFactory), BigDecimal.class);
            }
            else {
                value = accessor.getValue(ctx, elCtx, variableFactory);
            }

            if ((fields & NEGATION) != 0) value = !((Boolean) value);

            return this;
        }
        catch (NullPointerException e) {
            if (accessor == null) {
                if (nextToken != null && nextToken.isOperator(Operator.ASSIGN)) {
                    createDeferralOptimization();
                    return this;
                }

                optimizeAccessor(isPush() ? valueOnly(ctx) : ctx, elCtx, variableFactory, elCtx != null);
                return this;
            }
            else {
                throw e;
            }
        }

    }

    public Token createDeferralOptimization() {
        accessor = new Deferral();
        return this;
    }

    public Object optimizeAccessor(Object ctx, Object thisRef, VariableResolverFactory variableFactory, boolean thisRefPush) {
        try {
            AccessorCompiler compiler = OptimizerFactory.getDefaultAccessorCompiler();
            accessor = compiler.compile(name, ctx, thisRef, variableFactory, thisRefPush);

            setNumeric(false);
            setValue(compiler.getResultOptPass());
            setFlag(true, Token.OPTIMIZED_REF);

            return compiler.getResultOptPass();

        }
        catch (OptimizationNotSupported e) {
            assert ParseTools.debug("[Falling Back to Reflective Optimizer]");
            // fall back to the safe reflective optimizer
            AccessorCompiler compiler = OptimizerFactory.getAccessorCompiler(SAFE_REFLECTIVE);
            accessor = compiler.compile(name, ctx, thisRef, variableFactory, thisRefPush);

            setNumeric(false);
            setValue(compiler.getResultOptPass());
            setFlag(true, Token.OPTIMIZED_REF);

            return compiler.getResultOptPass();
        }
        catch (Exception e) {
            throw new OptimizationFailure("failed to optimize accessor: " + new String(name), e);
        }
    }

    public void deOptimize() {
        accessor = null;
    }

    public boolean isOptimized() {
        return accessor != null;
    }

    public BigDecimal getNumericValue() {
        return numericValue;
    }

    public String getValueAsString() {
        if (value instanceof String) return (String) value;
        else if (value instanceof char[]) return new String((char[]) value);
        else return valueOf(value);
    }

    public char[] getValueAsCharArray() {
        if (value instanceof char[]) return ((char[]) value);
        else if (value instanceof String) return ((String) value).toCharArray();
        else return valueOf(value).toCharArray();
    }


    public Token getReducedValueAccelerated(Object ctx, Object eCtx, VariableResolverFactory factory) {
        if ((fields & (LITERAL)) != 0) {
            return this;
        }

        return _getReducedValueAccelerated(ctx, eCtx, factory, false);
    }

    public Token _getReducedValueAccelerated(Object ctx, Object eCtx, VariableResolverFactory factory, boolean failBit) {
        try {
            return getOptimizedValue(ctx, eCtx, factory);
        }
        catch (OptimizationFailure e) {
            /**
             * If the token failed to optimize, we perform a lookahead to see if this is forgivable.
             */
            if (lookAhead()) {
                /**
                 * The token failed to reduce for forgivable reasons (ie. an assignment) so we record this
                 * as a deferral, so we don't try to reduce this anymore at runtime.
                 */

                createDeferralOptimization();
                return this;
            }
            else {
                /**
                 * This is a genuine error.  We bail.
                 */
                throw e;
            }
        }
        catch (PropertyAccessException e) {
            throw e;
        }
        catch (Exception e) {
            /**
             * For the purpose of dynamic re-optimization we allow a "retry" of the reduction after we have
             * cleared the old optimized tree.  However, if the failNext bit remains high, we bail as we
             * clearly can't re-optimize.
             */
            if (failBit) {
                throw new CompileException("optimization failure for: " + this, e);
            }


            try {
                synchronized (this) {
                    deOptimize();
                    return _getReducedValueAccelerated(ctx, eCtx, factory, true);
                }
            }
            catch (Exception e2) {
                throw new CompileException("optimization failure for: " + this, e);
            }
        }
    }


    /**
     * This method is called by the parser when it can't resolve a token.  There are two cases where this may happen
     * under non-fatal circumstances: ASSIGNMENT or PROJECTION.  If one of these situations is indeed the case,
     * the execution continues after a quick adjustment to allow the parser to continue as if we're just at a
     * junction.  Otherwise we explode.
     *
     * @return -
     */
    private boolean lookAhead() {
        Token tk = this;

        if ((tk = tk.nextToken) != null) {
            if (tk.isOperator(Operator.ASSIGN) || tk.isOperator(Operator.PROJECTION)) {
                nextToken = tk;

            }
            else if (!tk.isOperator()) {
                throw new CompileException("expected operator but encountered token: " + tk.getName());
            }
            else
                return false;
        }
        else {
            return false;
        }
        return true;
    }

    public Token getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        String s;
        if ((fields & LITERAL) != 0 || ((fields & Token.CAPTURE_ONLY) != 0)) {
            return this;
        }

        /**
         * To save the GC a little bit of work, we don't initialize a property accessor unless we need it, and we
         * reuse the same instance if we need it more than once.
         */
        // if (propertyAccessor == null)

        if ((fields & Token.PUSH) != 0) {
            /**
             * This token is attached to a stack value, and we use the top stack value as the context for the
             * property accessor.
             */
            return setValue(get(name, ctx, factory, thisValue));
        }
        else if ((fields & Token.DEEP_PROPERTY) != 0) {
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

                return setValue(get(getRemainder(), literal, factory, thisValue));
            }
            else if (factory != null && factory.isResolveable(s)) {
                /**
                 * The root of the DEEP PROPERTY is a local or global var.
                 */
                return setValue(get(getRemainder(), factory.getVariableResolver(s).getValue(), factory, thisValue));

            }
            else if (thisValue != null) {
                /**
                 * We didn't resolve the root, yet, so we assume that if we have a VROOT then the property must be
                 * accessible as a field of the VROOT.
                 */

                try {
                    return setValue(get(name, thisValue, factory, thisValue));
                }
                catch (PropertyAccessException e) {
                    /**
                     * No luck. Make a last-ditch effort to resolve this as a static-class reference.
                     */
                    Token tk = tryStaticAccess(thisValue, factory);
                    if (tk == null) throw e;
                    return tk;
                }
            }
            else {
                Token tk = tryStaticAccess(thisValue, factory);
                if (tk == null) throw new CompileException("unable to resolve token: " + s);
                return tk;
            }
        }
        else {
            if (Token.LITERALS.containsKey(s = getAbsoluteName())) {
                /**
                 * The token is actually a literal.
                 */
                return setValue(Token.LITERALS.get(s));
            }
            else if (factory != null && factory.isResolveable(s)) {
                /**
                 * The token is a local or global var.
                 */

                if (isCollection()) {
                    return setValue(get(new String(name, endOfName, name.length - endOfName),
                            factory.getVariableResolver(s).getValue(), factory, thisValue));
                }
                return setValue(factory.getVariableResolver(s).getValue());
            }
            else if (thisValue != null) {
                /**
                 * Check to see if the var exists in the VROOT.
                 */
                try {
                    return setValue(get(name, thisValue, factory, thisValue));
                }
                catch (RuntimeException e) {
                    /**
                     * Nope.  Let's see if this a a LA-issue.
                     */
                    if (!lookAhead()) throw e;
                }
            }
            else {
                if (!lookAhead())
                    throw new CompileException("unable to resolve token: " + s);
            }
        }
        return this;
    }

    private Token tryStaticAccess(Object thisRef, VariableResolverFactory factory) {
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
                            return setValue(
                                    get(new String(name, last, name.length - last), forName(new String(name, 0, last)), factory, thisRef)
                            );
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


    public Token setValue(Object value) {
        //String s;
        try {
            if ((fields & (NEGATION | BOOLEAN_MODE)) == (NEGATION | BOOLEAN_MODE)) {
                this.value = BlankLiteral.INSTANCE.equals(value);
            }
            else if ((fields & NEGATION) != 0) {
                if (value instanceof Boolean) {
                    this.value = !((Boolean) value);
                }
                else {
                    throw new CompileException("illegal negation - not a boolean expression");
                }
            }
            else {
                if (value instanceof BigDecimal) {
                    fields |= NUMERIC;
                    this.numericValue = (BigDecimal) value;
                }
                else if (isNumber(value)) {
                    fields |= NUMERIC;
                    this.numericValue = convert(value, BigDecimal.class);
                }
                this.value = value;
            }
        }
        catch (NumberFormatException e) {
            throw new CompileException("unable to create numeric value from: '" + value + "'");
        }

        return this;
    }

    public Token setFinalValue(int fields, Object value) {
        this.fields |= fields;
        this.value = value;
        return this;
    }

    public Token setFinalValue(Object value) {
        this.value = value;
        return this;
    }


    @SuppressWarnings({"SuspiciousMethodCalls"})
    public void setName(char[] name) {

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

            this.value = new String(this.name = processedEscapeString);

        }
        else {
            this.value = new String(this.name = name);
        }

        if ((fields & (SUBEVAL | LITERAL)) != 0) {
            //    return;
        }
        else if (LITERALS.containsKey(value)) {
            fields |= EVAL_RIGHT | LITERAL;
            if ((value = LITERALS.get(value)) == ThisLiteral.class) fields |= THISREF;
        }
        else if (OPERATORS.containsKey(value)) {
            fields |= OPERATOR;
            resetValue = value = OPERATORS.get(value);
            return;
        }
        else if (((fields & NUMERIC) != 0) || isNumber(name)) {
            if (((fields |= LITERAL | NUMERIC) & INVERT) != 0) {
                value = this.numericValue = new BigDecimal(~parseInt((String) value));
            }
            else {
                value = this.numericValue = new BigDecimal(valueOf(name));
            }
            if (this.numericValue.scale() > 0) fields |= FLOATING_NUMERIC;
        }
        else if ((firstUnion = findFirst('.', name)) > 0) {
            fields |= DEEP_PROPERTY | IDENTIFIER;
        }
        else {
            fields |= IDENTIFIER;
        }

        if ((endOfName = findFirst('[', name)) > 0) fields |= COLLECTION;

        resetValue = value;
    }

    public int getFlags() {
        return fields;
    }

    private boolean getFlag(int field) {
        return (fields & field) != 0;
    }

    public void setFlag(boolean setting, int flag) {
        if (getFlag(flag) ^ setting) {
            fields = fields ^ flag;
        }
        else if (setting) {
            fields = fields | flag;
        }
    }


    public String toString() {
        return valueOf(value);
    }

    public boolean equals(Object obj) {
        if (obj instanceof Token)
            return value == null ? ((Token) obj).value == null : value.equals(((Token) obj).value);
        else
            return value == null ? obj == value : value.equals(obj);
    }

    public int hashCode() {
        return value == null ? super.hashCode() : value.hashCode();
    }

    public boolean isValidNameIdentifier() {
        return !Character.isDigit(name[0]);
    }


    public int getFirstUnion() {
        return firstUnion;
    }

    public ExecutableStatement getCompiledExpression() {
        return compiledExpression;
    }


    public Accessor getAccessor() {
        return accessor;
    }

    public void setAccessor(Accessor accessor) {
        this.accessor = accessor;
    }

    public void setCompiledExpression(ExecutableStatement compiledExpression) {
        this.compiledExpression = compiledExpression;
    }

    public boolean isCollectionCreation() {
        return (fields & (MAPCREATE | ARRAYCREATE | LISTCREATE)) != 0;
    }

    public int getCollectionCreationType() {
        return fields & (MAPCREATE | ARRAYCREATE | LISTCREATE);
    }

    public boolean isNestBegin() {
        return (fields & Token.NEST) != 0;
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

    public void reset() {
        if (resetValue == null) {
            setFlag(false, LITERAL);
            setName(name);
        }
        else {
            value = resetValue;
        }
    }

    public boolean isIdentifier() {
        return (fields & IDENTIFIER) != 0;
    }

    public void setIdentifier(boolean identifier) {
        setFlag(identifier, IDENTIFIER);
    }

    public boolean isSubeval() {
        return (fields & SUBEVAL) != 0;
    }

    public void setExpand(boolean unreduced) {
        setFlag(unreduced, SUBEVAL);
    }

    public boolean isNumeric() {
        return (fields & NUMERIC) != 0;
    }

    public void setNumeric(boolean numeric) {
        setFlag(numeric, NUMERIC);
    }

    public boolean isEvalRight() {
        return (fields & EVAL_RIGHT) != 0;
    }

    public void setEvalRight(boolean evalRight) {
        setFlag(evalRight, EVAL_RIGHT);
    }

    public boolean isInvert() {
        return (fields & INVERT) != 0;
    }

    public void setInvert(boolean invert) {
        setFlag(invert, INVERT);
    }

    public boolean isNoCompile() {
        return (fields & NOCOMPILE) != 0;
    }

    public boolean isThisRef() {
        return (fields & THISREF) != 0;
    }

    public boolean isDoNotReduce() {
        return (fields & DO_NOT_REDUCE) != 0;
    }


    public boolean isLiteral() {
        return (fields & LITERAL) != 0;
    }

    public void setLiteral(boolean literal) {
        setFlag(literal, LITERAL);
    }

    public boolean isDeepProperty() {
        return (fields & DEEP_PROPERTY) != 0;
    }

    public void setDeepProperty(boolean deepProperty) {
        setFlag(deepProperty, DEEP_PROPERTY);
    }

    public boolean isOperator() {
        return (fields & OPERATOR) != 0;
    }

    public boolean isOperator(Integer operator) {
        return (fields & OPERATOR) != 0 && value == operator;
    }

    public Integer getOperator() {
        return (Integer) value;
    }

    public void setOperator(boolean operator) {
        setFlag(operator, OPERATOR);
    }

    public boolean isNegation() {
        return getFlag(NEGATION);
    }

    public void setNegation(boolean negation) {
        setFlag(negation, NEGATION);
    }

    public boolean isCollection() {
        return (fields & COLLECTION) != 0;
    }

    public boolean isEndNest() {
        return (fields & ENDNEST) != 0;
    }

    public boolean isPush() {
        return (fields & PUSH) != 0;
    }

    public boolean isCaptureOnly() {
        return (fields & CAPTURE_ONLY) != 0;
    }

    public boolean isReducable() {
        return ((fields & CAPTURE_ONLY) | (fields & LITERAL)) == 0;
    }

    public int getKnownSize() {
        return knownSize;
    }

    public void setKnownSize(int knownSize) {
        this.knownSize = knownSize;
    }


    public Token getNextToken() {
        return nextToken;
    }

    public void setNextToken(Token nextToken) {
        this.nextToken = nextToken;
    }
}


