package org.mvel.optimizers.impl.asm;

import org.mvel.*;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorCompiler;
import org.mvel.optimizers.ExecutableStatement;
import org.mvel.optimizers.OptimizationNotSupported;
import org.mvel.util.ParseTools;
import org.mvel.util.PropertyTools;
import org.mvel.util.StringAppender;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

import java.lang.reflect.*;
import java.util.*;

public class ASMAccessorCompiler implements AccessorCompiler {
    private int start = 0;
    private int cursor = 0;

    private char[] property;
    private int length;

    private Object ctx;
    private Object thisRef;

    private VariableResolverFactory variableFactory;

    private static final int DONE = -1;
    private static final int BEAN = 0;
    private static final int METH = 1;
    private static final int COL = 2;

    private static final Object[] EMPTYARG = new Object[0];

    private boolean first = true;

    private String className;
    private ClassWriter cw;
    private MethodVisitor mv;

    private Object val;
    private int stacksize = 1;
    private long time;

    private int inputs;

    private ArrayList<ExecutableStatement> compiledInputs;

    private Class returnType;

    public ASMAccessorCompiler(char[] property, Object ctx, Object thisRef, VariableResolverFactory variableResolverFactory) {
        this.property = property;
        this.ctx = ctx;
        this.variableFactory = variableResolverFactory;
        this.thisRef = thisRef;
    }


    public ASMAccessorCompiler() {
    }

    public Accessor compile(char[] property, Object staticContext, Object thisRef, VariableResolverFactory factory, boolean root) {
        time = System.currentTimeMillis();

        inputs = 0;
        compiledInputs = new ArrayList<ExecutableStatement>();

        start = cursor = 0;

        this.first = true;
        this.val = null;

        this.length = property.length;
        this.property = property;
        this.ctx = staticContext;
        this.thisRef = thisRef;
        this.variableFactory = factory;

        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, className = "ASMAccessorImpl_" + String.valueOf(cw.hashCode()).replaceAll("\\-", "_"),
                null, "java/lang/Object", new String[]{"org/mvel/Accessor"});

        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object",
                "<init>", "()V");
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(1, 1);
        m.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "getValue",
                "(Ljava/lang/Object;Ljava/lang/Object;Lorg/mvel/integration/VariableResolverFactory;)Ljava/lang/Object;", null, null);
        mv.visitCode();

        return compileAccessor();
    }

    public Accessor compileAccessor() {
        debug("\n{Initiate Compile: " + new String(property) + "}\n");

        Object curr = ctx;

        try {
            while (cursor < length) {
                switch (nextToken()) {
                    case BEAN:
                        curr = getBeanProperty(curr, capture());
                        break;
                    case METH:
                        curr = getMethod(curr, capture());
                        break;
                    case COL:
                        curr = getCollectionProperty(curr, capture());
                        break;
                    case DONE:
                        break;
                }

                first = false;
            }

            val = curr;

            if (returnType != null && returnType.isPrimitive()) {
                //noinspection unchecked
                wrapPrimitive(returnType);
            }

            if (returnType == void.class) {
                debug("ACONST_NULL");
                mv.visitInsn(ACONST_NULL);
            }


            debug("ARETURN");

            mv.visitInsn(ARETURN);


            debug("\n{METHOD STATS (maxstack=" + stacksize + ")}\n");
            mv.visitMaxs(stacksize, 1);

            mv.visitEnd();

            buildInputs();

            cw.visitEnd();


            Class cls = loadClass(cw.toByteArray());

            debug("[MVEL JIT Completed Optimization <<" + new String(property) + ">>]::" + cls + " (time: " + (System.currentTimeMillis() - time) + "ms)");


            Accessor a;

            if (inputs == 0) {
                a = (Accessor) cls.newInstance();
            }
            else {
                Class[] parms = new Class[inputs];
                for (int i = 0; i < inputs; i++) {
                    parms[i] = ExecutableStatement.class;
                }
                a = (Accessor) cls.getConstructor(parms).newInstance(compiledInputs.toArray(new CompiledExpression[compiledInputs.size()]));
            }

            debug("[MVEL JIT Test Output: " + a.getValue(ctx, thisRef, variableFactory) + "]");

            return a;
        }
        catch (InvocationTargetException e) {
            throw new PropertyAccessException("could not access property", e);
        }
        catch (IllegalAccessException e) {
            throw new PropertyAccessException("could not access property", e);
        }
        catch (IndexOutOfBoundsException e) {
            throw new PropertyAccessException("array or collection index out of bounds (property: " + new String(property) + ")", e);
        }
        catch (PropertyAccessException e) {
            throw new PropertyAccessException("failed to access property: <<" + new String(property) + ">> in: " + (ctx != null ? ctx.getClass() : null), e);
        }
        catch (CompileException e) {
            throw e;
        }
        catch (NullPointerException e) {
            throw new PropertyAccessException("null pointer exception in property: " + new String(property), e);
        }
        catch (OptimizationNotSupported e) {
            throw e;
        }
        catch (Exception e) {
            throw new PropertyAccessException("unknown exception in expression: " + new String(property), e);
        }
    }


    private int nextToken() {
        switch (property[start = cursor]) {
            case'[':
                return COL;
            case'.':
                cursor = ++start;
        }

        //noinspection StatementWithEmptyBody
        while (++cursor < length && Character.isJavaIdentifierPart(property[cursor])) ;


        if (cursor < length) {
            switch (property[cursor]) {
                case'[':
                    return COL;
                case'(':
                    return METH;
                default:
                    return 0;
            }
        }
        return 0;
    }

    private String capture() {
        return new String(property, start, cursor - start);
    }


    private Object getBeanProperty(Object ctx, String property)
            throws IllegalAccessException, InvocationTargetException {

        debug("{bean: " + property + "}");

        Class cls = (ctx instanceof Class ? ((Class) ctx) : ctx != null ? ctx.getClass() : null);
        Member member = cls != null ? PropertyTools.getFieldOrAccessor(cls, property) : null;

        if (first && variableFactory != null && variableFactory.isResolveable(property)) {
            try {
                debug("ALOAD 3");
                mv.visitVarInsn(ALOAD, 3);

                debug("LDC :" + property);
                mv.visitLdcInsn(property);

                debug("INVOKEINTERFACE org/mvel/integration/VariableResolverFactory.getVariableResolver");
                mv.visitMethodInsn(INVOKEINTERFACE, "org/mvel/integration/VariableResolverFactory",
                        "getVariableResolver", "(Ljava/lang/String;)Lorg/mvel/integration/VariableResolver;");

                debug("INVOKEINTERFACE org/mvel/integration/VariableResolver.getValue");
                mv.visitMethodInsn(INVOKEINTERFACE, "org/mvel/integration/VariableResolver",
                        "getValue", "()Ljava/lang/Object;");
            }
            catch (Exception e) {
                throw new OptimizationFailure("critical error in JIT", e);
            }


            return variableFactory.getVariableResolver(property).getValue();
        }
        else if (member instanceof Field) {
            Object o = ((Field) member).get(ctx);

            if (first) {
                debug("ALOAD 2");
                mv.visitVarInsn(ALOAD, 2);
            }

            debug("CHECKCAST " + getInternalName(cls));
            mv.visitTypeInsn(CHECKCAST, getInternalName(cls));

            debug("GETFIELD " + property + ":" + getDescriptor(((Field) member).getType()));
            mv.visitFieldInsn(GETFIELD, getInternalName(cls), property, getDescriptor(((Field) member).getType()));
            //  addAccessorComponent(cls, property, FIELD, ((Field) member).getType());
            return o;
        }
        else if (member != null) {
            if (first) {
                debug("ALOAD 2");
                mv.visitVarInsn(ALOAD, 2);
            }

            debug("CHECKCAST " + getInternalName(member.getDeclaringClass()));
            mv.visitTypeInsn(CHECKCAST, getInternalName(member.getDeclaringClass()));

            returnType = ((Method) member).getReturnType();

            debug("INVOKEVIRTUAL " + member.getName() + ":" + returnType);
            mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(member.getDeclaringClass()), member.getName(),
                    getMethodDescriptor((Method) member));

            stacksize++;

            return ((Method) member).invoke(ctx, EMPTYARG);
        }
        else if (ctx instanceof Map && ((Map) ctx).containsKey(property)) {

            debug("CHECKCAST java/util/Map");
            mv.visitTypeInsn(CHECKCAST, "java/util/Map");

            debug("LDC: \"" + property + "\"");
            mv.visitLdcInsn(property);

            debug("INVOKEINTERFACE: get");
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
            return ((Map) ctx).get(property);
        }
        else if ("this".equals(property)) {

            debug("ALOAD 2");
            mv.visitVarInsn(ALOAD, 2); // load the thisRef value.

            return this.thisRef;
        }
        else if (Token.LITERALS.containsKey(property)) {
            return Token.LITERALS.get(property);
        }
        else {
            Class tryStaticMethodRef = tryStaticAccess();

            if (tryStaticMethodRef != null) {
                throw new OptimizationNotSupported("class literal: " + tryStaticMethodRef);
            }
            else
                throw new PropertyAccessException("could not access property (" + property + ")");
        }
    }

    private void whiteSpaceSkip() {
        if (cursor < length)
            //noinspection StatementWithEmptyBody
            while (Character.isWhitespace(property[cursor]) && ++cursor < length) ;
    }

    private boolean scanTo(char c) {
        for (; cursor < length; cursor++) {
            if (property[cursor] == c) {
                return true;
            }
        }
        return false;
    }

    private int containsStringLiteralTermination() {
        int pos = cursor;
        for (pos--; pos > 0; pos--) {
            if (property[pos] == '\'' || property[pos] == '"') return pos;
            else if (!Character.isWhitespace(property[pos])) return pos;
        }
        return -1;
    }


    /**
     * Handle accessing a property embedded in a collection, map, or array
     *
     * @param ctx  -
     * @param prop -
     * @return -
     * @throws Exception -
     */
    private Object getCollectionProperty(Object ctx, String prop) throws Exception {
        if (prop.length() > 0) ctx = getBeanProperty(ctx, prop);

        debug("{collection: " + prop + "} ctx=" + ctx);

        int start = ++cursor;

        whiteSpaceSkip();

        if (cursor == length)
            throw new PropertyAccessException("unterminated '['");

        String item;

        if (property[cursor] == '\'' || property[cursor] == '"') {
            start++;

            int end;

            if (!scanTo(']'))
                throw new PropertyAccessException("unterminated '['");
            if ((end = containsStringLiteralTermination()) == -1)
                throw new PropertyAccessException("unterminated string literal in collection accessor");

            item = new String(property, start, end - start);
        }
        else {
            if (!scanTo(']'))
                throw new PropertyAccessException("unterminated '['");

            item = new String(property, start, cursor - start);
        }

        ++cursor;

        if (ctx instanceof Map) {
            debug("CHECKCAST java/util/Map");
            mv.visitTypeInsn(CHECKCAST, "java/util/Map");

            debug("LDC: \"" + item + "\"");
            mv.visitLdcInsn(item);

            debug("INVOKEINTERFACE: get");
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;");

            return ((Map) ctx).get(item);
        }
        else if (ctx instanceof List) {
            int index = Integer.parseInt(item);

            debug("CHECKCAST java/util/List");
            mv.visitTypeInsn(CHECKCAST, "java/util/List");

            debug("BIGPUSH: " + 6);
            mv.visitIntInsn(BIPUSH, index);

            debug("INVOKEINTERFACE: java/util/List.get");
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;");

            return ((List) ctx).get(index);
        }
        else if (ctx instanceof Collection) {
            int count = Integer.parseInt(item);
            if (count > ((Collection) ctx).size())
                throw new PropertyAccessException("index [" + count + "] out of bounds on collection");

            Iterator iter = ((Collection) ctx).iterator();
            for (int i = 0; i < count; i++) iter.next();
            return iter.next();
        }
        else if (ctx instanceof Object[]) {
            int index = Integer.parseInt(item);

            debug("CHECKCAST [Ljava/lang/Object;");
            mv.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");

            if (index < 6) {
                switch (index) {
                    case 0:
                        debug("ICONST_0");
                        mv.visitInsn(ICONST_0);
                        break;
                    case 1:
                        debug("ICONST_1");
                        mv.visitInsn(ICONST_1);
                        break;
                    case 2:
                        debug("ICONST_2");
                        mv.visitInsn(ICONST_2);
                        break;
                    case 3:
                        debug("ICONST_3");
                        mv.visitInsn(ICONST_3);
                        break;
                    case 4:
                        debug("ICONST_4");
                        mv.visitInsn(ICONST_4);
                        break;
                    case 5:
                        debug("ICONST_5");
                        mv.visitInsn(ICONST_5);
                        break;
                }
            }
            else {
                debug("BIPUSH " + index);
                mv.visitIntInsn(BIPUSH, index);
            }

            mv.visitInsn(AALOAD);

            return ((Object[]) ctx)[index];
        }
        else if (ctx instanceof CharSequence) {
            int index = Integer.parseInt(item);

            mv.visitIntInsn(BIPUSH, index);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/CharSequence", "charAt", "(I)C");

            return ((CharSequence) ctx).charAt(index);
        }
        else {
            throw new PropertyAccessException("illegal use of []: unknown type: " + (ctx == null ? null : ctx.getClass().getName()));
        }
    }

    private static final Map<String, ExecutableStatement[]> SUBEXPRESSION_CACHE
            = new WeakHashMap<String, ExecutableStatement[]>();

    /**
     * Find an appropriate method, execute it, and return it's response.
     *
     * @param ctx  -
     * @param name -
     * @return -
     * @throws Exception -
     */
    @SuppressWarnings({"unchecked"})
    private Object getMethod(Object ctx, String name) throws Exception {
        debug("{method: " + name + "}");


        int st = cursor;

        int depth = 1;

        while (cursor++ < length - 1 && depth != 0) {
            switch (property[cursor]) {
                case'(':
                    depth++;
                    continue;
                case')':
                    depth--;

            }
        }
        cursor--;

        String tk = (cursor - st) > 1 ? new String(property, st + 1, cursor - st - 1) : "";

        cursor++;

        Object[] preConvArgs;
        Object[] args;
        ExecutableStatement[] es;

        if (tk.length() == 0) {
            //noinspection ZeroLengthArrayAllocation
            args = new Object[0];

            //noinspection ZeroLengthArrayAllocation            
            preConvArgs = new Object[0];
            es = null;
        }
        else {
            if (SUBEXPRESSION_CACHE.containsKey(tk)) {
                es = SUBEXPRESSION_CACHE.get(tk);
                args = new Object[es.length];
                preConvArgs = new Object[es.length];
                for (int i = 0; i < es.length; i++) {
                    preConvArgs[i] = args[i] = es[i].getValue(this.ctx, variableFactory);
                }
            }
            else {
                String[] subtokens = ParseTools.parseParameterList(tk.toCharArray(), 0, -1);
                es = new ExecutableStatement[subtokens.length];
                args = new Object[subtokens.length];
                preConvArgs = new Object[es.length];

                for (int i = 0; i < subtokens.length; i++) {
                    preConvArgs[i] = args[i] = (es[i] = (CompiledExpression) ExpressionParser.compileExpression(subtokens[i])).getValue(this.ctx, variableFactory);
                }
                SUBEXPRESSION_CACHE.put(tk, es);
            }

        }

        if (es != null) {
            for (ExecutableStatement e : es)
                compiledInputs.add(e);
        }
        /**
         * If the target object is an instance of java.lang.Class itself then do not
         * adjust the Class scope target.
         */
        Class cls = ctx instanceof Class ? (Class) ctx : ctx.getClass();

        Method m;
        Class[] parameterTypes = null;

        /**
         * If we have not cached the method then we need to go ahead and try to resolve it.
         */
        /**
         * Try to find an instance method from the class target.
         */

        if ((m = ParseTools.getBestCanadidate(args, name, cls.getMethods())) != null) {
            parameterTypes = m.getParameterTypes();
        }

        if (m == null) {
            /**
             * If we didn't find anything, maybe we're looking for the actual java.lang.Class methods.
             */
            if ((m = ParseTools.getBestCanadidate(args, name, cls.getClass().getDeclaredMethods())) != null) {
                parameterTypes = m.getParameterTypes();
            }
        }


        if (m == null) {
            StringAppender errorBuild = new StringAppender();
            for (int i = 0; i < args.length; i++) {
                errorBuild.append(args[i] != null ? args[i].getClass().getName() : null);
                if (i < args.length - 1) errorBuild.append(", ");
            }

            throw new PropertyAccessException("unable to resolve method: " + cls.getName() + "." + name + "(" + errorBuild.toString() + ") [arglength=" + args.length + "]");
        }
        else {
            if (es != null) {
                CompiledExpression cExpr;
                for (int i = 0; i < es.length; i++) {
                    cExpr = ((CompiledExpression) es[i]);
                    if (cExpr.getKnownIngressType() == null) {
                        cExpr.setKnownIngressType(parameterTypes[i]);
                        cExpr.pack();
                    }
                    if (!cExpr.isConvertableIngressEgress()) {
                        args[i] = DataConversion.convert(args[i], parameterTypes[i]);
                    }
                }
            }
            else {
                /**
                 * Coerce any types if required.
                 */
                for (int i = 0; i < args.length; i++)
                    args[i] = DataConversion.convert(args[i], parameterTypes[i]);
            }



            if (first) {
                debug("ALOAD 1");
                mv.visitVarInsn(ALOAD, 1);
            }

            if (m.getParameterTypes().length == 0) {
                if ((m.getModifiers() & Modifier.STATIC) != 0) {

                    debug("INVOKESTATIC " + m.getName());
                    mv.visitMethodInsn(INVOKESTATIC, getInternalName(m.getDeclaringClass()), m.getName(), getMethodDescriptor(m));
                }
                else {

                    debug("CHECKCAST " + getInternalName(m.getDeclaringClass()));
                    mv.visitTypeInsn(CHECKCAST, getInternalName(m.getDeclaringClass()));

                    debug("INVOKEVIRTUAL " + m.getName());
                    mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(m.getDeclaringClass()), m.getName(),
                            getMethodDescriptor(m));
                }

                returnType = m.getReturnType();

                stacksize++;
            }
            else {
                if ((m.getModifiers() & Modifier.STATIC) == 0) {
                    debug("CHECKCAST " + getInternalName(cls));
                    mv.visitTypeInsn(CHECKCAST, getInternalName(cls));
                }

                for (int i = 0; i < es.length; i++) {
                    debug("ALOAD 0");
                    mv.visitVarInsn(ALOAD, 0);

                    debug("GETFIELD p" + inputs++);
                    mv.visitFieldInsn(GETFIELD, className, "p" + (inputs - 1), "Lorg/mvel/optimizers/ExecutableStatement;");

                    debug("ALOAD 2");
                    mv.visitVarInsn(ALOAD, 2);

                    debug("ALOAD 3");
                    mv.visitVarInsn(ALOAD, 3);

                    debug("INVOKEINTERFACE ExecutableStatement.getValue");
                    mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(ExecutableStatement.class), "getValue",
                            "(Ljava/lang/Object;Lorg/mvel/integration/VariableResolverFactory;)Ljava/lang/Object;");

                    if (parameterTypes[i].isPrimitive()) {
                        unwrapPrimitive(parameterTypes[i]);
                    }
                    else if (preConvArgs[i] == null ||
                            (parameterTypes[i] != String.class &&
                                    !parameterTypes[i].isAssignableFrom(preConvArgs[i].getClass()))) {

                        debug("LDC " + getType(parameterTypes[i]));
                        mv.visitLdcInsn(getType(parameterTypes[i]));

                        debug("INVOKESTATIC DataConversion.convert");
                        mv.visitMethodInsn(INVOKESTATIC, "org/mvel/DataConversion", "convert",
                                "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");

                        debug("CHECKCAST " + getInternalName(parameterTypes[i]));
                        mv.visitTypeInsn(CHECKCAST, getInternalName(parameterTypes[i]));
                    }
                    else if (parameterTypes[i] == String.class) {
                        debug("<<<DYNAMIC TYPE OPTIMIZATION STRING>>");
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
                    }
                    else {
                        debug("<<<DYNAMIC TYPING BYPASS>>>");
                        debug("<<<OPT. JUSTIFICATION " + parameterTypes[i] + "=" + preConvArgs[i].getClass() + ">>>");

                        debug("CHECKCAST " + getInternalName(parameterTypes[i]));
                        mv.visitTypeInsn(CHECKCAST, getInternalName(parameterTypes[i]));
                    }


                    stacksize += 3;
                }

                if ((m.getModifiers() & Modifier.STATIC) != 0) {
                    debug("INVOKESTATIC: " + m.getName());
                    mv.visitMethodInsn(INVOKESTATIC, getInternalName(m.getDeclaringClass()), m.getName(), getMethodDescriptor(m));
                }
                else {
                    if (m.getDeclaringClass() != cls && m.getDeclaringClass().isInterface()) {
                        debug("INVOKEINTERFACE: " + getInternalName(m.getDeclaringClass()) + "." + m.getName());
                        mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(m.getDeclaringClass()), m.getName(),
                                getMethodDescriptor(m));
                    }
                    else {
                        debug("INVOKEVIRTUAL: " + getInternalName(cls) + "." + m.getName());
                        mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(cls), m.getName(),
                                getMethodDescriptor(m));
                    }
                }

                returnType = m.getReturnType();

                stacksize++;
            }



            return  m.invoke(ctx, args);
        }
    }


    private Class tryStaticAccess() {
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
            int last = property.length;
            for (int i = property.length - 1; i > 0; i--) {
                switch (property[i]) {
                    case'.':
                        if (!meth) {
                            return Class.forName(new String(property, 0, last));
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


    private java.lang.Class loadClass(byte[] b) throws Exception {
        //override classDefine (as it is protected) and define the class.
        Class clazz = null;
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        Class cls = Class.forName("java.lang.ClassLoader");
        java.lang.reflect.Method method =
                cls.getDeclaredMethod("defineClass", new Class[]{String.class, byte[].class, int.class, int.class});

        // protected method invocaton
        method.setAccessible(true);
        try {
            Object[] args = new Object[]{className, b, 0, (b.length)};
            clazz = (Class) method.invoke(loader, args);
        }
        finally {
            method.setAccessible(false);
        }

        return clazz;
    }


    public static void debug(String instruction) {
        assert ParseTools.debug(instruction);
    }

    public String getName() {
        return "ASM";
    }


    public Object getResultOptPass() {
        return val;
    }

    private void unwrapPrimitive(Class cls) {
        if (cls == boolean.class) {
            debug("CHECKCAST java/lang/Boolean");
            mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
            debug("INVOKEVIRTUAL java/lang/Boolean.booleanValue");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
        }
        else if (cls == int.class) {
            debug("CHECKCAST java/lang/Integer");
            mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
            debug("INVOKEVIRTUAL java/lang/Integer.intValue");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
        }
        else if (cls == float.class) {
            debug("CHECKCAST java/lang/Float");
            mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
            debug("INVOKEVIRTUAL java/lang/Float.floatValue");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F");
        }
        else if (cls == double.class) {
            debug("CHECKCAST java/lang/Double");
            mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
            debug("INVOKEVIRTUAL java/lang/Double.doubleValue");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");
        }
        else if (cls == short.class) {
            debug("CHECKCAST java/lang/Short");
            mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
            debug("INVOKEVIRTUAL java/lang/Short.shortValue");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");
        }
        else if (cls == long.class) {
            debug("CHECKCAST java/lang/Long");
            mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
            debug("INVOKEVIRTUAL java/lang/Long.longValue");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L");
        }
        else if (cls == byte.class) {
            debug("CHECKCAST java/lang/Byte");
            mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
            debug("INVOKEVIRTUAL java/lang/Byte.byteValue");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B");
        }
        else if (cls == char.class) {
            debug("CHECKCAST java/lang/Character");
            mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
            debug("INVOKEVIRTUAL java/lang/Character.charValue");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");
        }

    }


    private void wrapPrimitive(Class<? extends Object> cls) {
        if (cls == boolean.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
        }
        else if (cls == int.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
        }
        else if (cls == float.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
        }
        else if (cls == double.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
        }
        else if (cls == short.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
        }
        else if (cls == long.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
        }
        else if (cls == byte.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
        }
        else if (cls == char.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
        }
    }


    public void buildInputs() {
        if (inputs == 0) return;

        debug("\n{SETTING UP MEMBERS...}\n");

        StringAppender constSig = new StringAppender("(");
        int size = inputs;

        for (int i = 0; i < size; i++) {
            debug("ACC_PRIVATE p" + i);
            FieldVisitor fv = cw.visitField(ACC_PRIVATE, "p" + i, "Lorg/mvel/optimizers/ExecutableStatement;", null, null);
            fv.visitEnd();

            constSig.append("Lorg/mvel/optimizers/ExecutableStatement;");
        }
        constSig.append(")V");


        debug("\n{CREATING INJECTION CONSTRUCTOR}\n");

        MethodVisitor cv = cw.visitMethod(ACC_PUBLIC, "<init>", constSig.toString(), null, null);
        cv.visitCode();
        debug("ALOAD 0");
        cv.visitVarInsn(ALOAD, 0);
        debug("INVOKESPECIAL java/lang/Object.<init>");
        cv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

        for (int i = 0; i < size; i++) {
            debug("ALOAD 0");
            cv.visitVarInsn(ALOAD, 0);
            debug("ALOAD " + (i + 1));
            cv.visitVarInsn(ALOAD, i + 1);
            debug("PUTFIELD p" + i);
            cv.visitFieldInsn(PUTFIELD, className, "p" + i, "Lorg/mvel/optimizers/ExecutableStatement;");
        }
        debug("RETURN");
        cv.visitInsn(RETURN);
        cv.visitMaxs(0, 0);
        cv.visitEnd();

        debug("}");

    }
}
