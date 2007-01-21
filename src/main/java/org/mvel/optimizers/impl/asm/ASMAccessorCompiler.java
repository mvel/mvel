package org.mvel.optimizers.impl.asm;

import org.mvel.*;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AccessorCompiler;
import org.mvel.optimizers.ExecutableStatement;
import org.mvel.optimizers.OptimizationNotSupported;
import org.mvel.optimizers.impl.refl.IndexedCharSeqAccessor;
import org.mvel.optimizers.impl.refl.MethodAccessor;
import org.mvel.util.ParseTools;
import org.mvel.util.PropertyTools;
import org.mvel.util.StringAppender;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
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
    private static final int FIELD = 3;
    private static final int VAR = 4;
    private static final int THIS = 5;

    private static final Object[] EMPTYARG = new Object[0];

    private boolean first = true;

    private String className;
    private ClassWriter classWriter;
    private MethodVisitor mv;

    private Object val;
    private int stacksize = 1;
    private long time;

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

        start = cursor = 0;

        this.first = true;
        this.val = null;

        this.length = property.length;
        this.property = property;
        this.ctx = staticContext;
        this.thisRef = thisRef;
        this.variableFactory = factory;

        classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classWriter.visit(Opcodes.V1_2, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, className = "ASMAccessorImpl_" + String.valueOf(classWriter.hashCode()).replaceAll("\\-", "_"),
                null, "java/lang/Object", new String[]{"org/mvel/Accessor"});

        MethodVisitor m = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object",
                "<init>", "()V");
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(1, 1);
        m.visitEnd();

        mv = classWriter.visitMethod(ACC_PUBLIC, "getValue",
                "(Ljava/lang/Object;Ljava/lang/Object;Lorg/mvel/integration/VariableResolverFactory;)Ljava/lang/Object;", null, null);
        mv.visitCode();

        return compileAccessor();
    }

    public Accessor compileAccessor() {
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

            debug("{exit: " + returnType + "}");

            if (returnType != null && returnType.isPrimitive()) {
                wrapPrimitive(returnType);
            }

            debug("ARETURN");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(stacksize, 1);

            mv.visitEnd();
            classWriter.visitEnd();


            Class cls = loadClass(classWriter.toByteArray());

            System.out.println("[MVEL JIT Completed Optimization <<" + new String(property) + ">>]::" + cls + " (time: " + (System.currentTimeMillis() - time) + "ms)");

            Accessor a = (Accessor) cls.newInstance();

            System.out.println("[MVEL JIT Test Output: " + a.getValue(ctx, thisRef, variableFactory) + "]");

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

        debug("{BEAN(" + property + ")}");

        Class cls = (ctx instanceof Class ? ((Class) ctx) : ctx != null ? ctx.getClass() : null);
        Member member = cls != null ? PropertyTools.getFieldOrAccessor(cls, property) : null;

        if (first && variableFactory != null && variableFactory.isResolveable(property)) {
            try {
                debug("ALOAD 3");
                mv.visitVarInsn(ALOAD, 3);

                debug("LDC :" + property);
                mv.visitLdcInsn(property);

                debug("INVOKEINTERFACE");
                mv.visitMethodInsn(INVOKEINTERFACE, "org/mvel/integration/VariableResolverFactory",
                        "getVariableResolver", "(Ljava/lang/String;)Lorg/mvel/integration/VariableResolver;");

                debug("INVOKEINTERFACE");
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

            debug("CHECKCAST: " + getInternalName(cls));
            mv.visitTypeInsn(CHECKCAST, getInternalName(cls));

            debug("GETFIELD: " + property + ":" + getDescriptor(((Field) member).getType()));
            mv.visitFieldInsn(GETFIELD, getInternalName(cls), property, getDescriptor(((Field) member).getType()));
            //  addAccessorComponent(cls, property, FIELD, ((Field) member).getType());
            return o;
        }
        else if (member != null) {
            if (first) {
                debug("ALOAD 2");
                mv.visitVarInsn(ALOAD, 2);
            }

            debug("CHECKCAST: " + getInternalName(member.getDeclaringClass()));
            mv.visitTypeInsn(CHECKCAST, getInternalName(member.getDeclaringClass()));

            returnType = ((Method) member).getReturnType();

            debug("INVOKEVIRTUAL: " + member.getName() + ":" + returnType);
            mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(member.getDeclaringClass()), member.getName(),
                    getMethodDescriptor((Method) member));

            stacksize++;

            return ((Method) member).invoke(ctx, EMPTYARG);
        }
        else if (ctx instanceof Map && ((Map) ctx).containsKey(property)) {

            //   addAccessorComponent(cls, property, COL, null);

            return ((Map) ctx).get(property);
        }
        else if ("this".equals(property)) {

            debug("ALOAD 2");
            mv.visitVarInsn(ALOAD, 2); // load the thisRef value.

            return this.thisRef;
        }
        else if (Token.LITERALS.containsKey(property)) {
            throw new OptimizationNotSupported("literal: " + property);
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
            debug("CHECKCAST: java/util/Map");
            mv.visitTypeInsn(CHECKCAST, "java/util/Map");

            debug("LDC: \"" + item + "\"");
            mv.visitLdcInsn(item);

            debug("INVOKEINTERFACE: get");
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
            
            return ((Map) ctx).get(item);
        }
        else if (ctx instanceof List) {
            int index = Integer.parseInt(item);

            debug("CHECKCAST: java/util/List");
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

            debug("CHECKCAST: [Ljava/lang/Object;");
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
            IndexedCharSeqAccessor accessor = new IndexedCharSeqAccessor();
            accessor.setIndex(Integer.parseInt(item));

            //    addAccessorNode(accessor);

            return ((CharSequence) ctx).charAt(accessor.getIndex());
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

        Object[] args;
        ExecutableStatement[] es;

        if (tk.length() == 0) {
            args = new Object[0];
            es = null;
        }
        else {
            if (SUBEXPRESSION_CACHE.containsKey(tk)) {
                es = SUBEXPRESSION_CACHE.get(tk);
                args = new Object[es.length];
                for (int i = 0; i < es.length; i++) {
                    args[i] = es[i].getValue(this.ctx, variableFactory);
                }
            }
            else {
                String[] subtokens = ParseTools.parseParameterList(tk.toCharArray(), 0, -1);
                es = new ExecutableStatement[subtokens.length];
                args = new Object[subtokens.length];
                for (int i = 0; i < subtokens.length; i++) {
                    args[i] = (es[i] = (CompiledExpression) ExpressionParser.compileExpression(subtokens[i])).getValue(this.ctx, variableFactory);
                }
                SUBEXPRESSION_CACHE.put(tk, es);
            }

        }

        /**
         * If the target object is an instance of java.lang.Class itself then do not
         * adjust the Class scope target.
         */
        Class cls = ctx instanceof Class ? (Class) ctx : ctx.getClass();

        //    Integer signature = ;

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


            MethodAccessor access = new MethodAccessor();
            access.setMethod(m);
            access.setParms(es);

            //  addAccessorNode(access);

            if (m.getTypeParameters().length == 0) {

                debug("CHECKCAST: " + getInternalName(m.getDeclaringClass()));
                mv.visitTypeInsn(CHECKCAST, getInternalName(m.getDeclaringClass()));

                debug("INVOKEVIRTUAL");
                mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(m.getDeclaringClass()), m.getName(),
                        getMethodDescriptor(m));
                stacksize++;
            }
            else {
                throw new OptimizationNotSupported("JIT does not currently support method parameters");
            }

            //addAccessorComponent(cls, m.getName(), METH, null);

            /**
             * Invoke the target method and return the response.
             */
            return m.invoke(ctx, args);
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


    public void addAccessorComponent(Class context, String name, int type, Class signature) {
        switch (type) {
            case BEAN:
                addMethodComponent(mv, context, name);
                return;

            case METH:
                throw new OptimizationNotSupported("cannot optimize method: " + name);

            case FIELD:
                addFieldComponent(mv, context, name, signature);
                return;
        }
    }


    public void addMethodComponent(MethodVisitor methodVisitor, Class context, String name) {
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, getDescriptor(context), name, "()I");
    }

    public void addFieldComponent(MethodVisitor methodVisitor, Class context, String name, Class signature) {
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitFieldInsn(GETFIELD, getInternalName(context), name, getDescriptor(signature));
    }

    private java.lang.Class loadClass(byte[] b) {
        //override classDefine (as it is protected) and define the class.
        Class clazz = null;
        try {
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
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return clazz;
    }


    public void debug(String instruction) {
    //    System.out.println(instruction);
    }

    public String getName() {
        return "ASM";
    }


    public Object getResultOptPass() {
        return val;
    }

    private void wrapPrimitive(Class<? extends Object> cls) {
        if (cls == boolean.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
        }
        else if (cls == int.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(Z)Ljava/lang/Integer;");
        }
        else if (cls == float.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(Z)Ljava/lang/Float;");
        }
        else if (cls == double.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(Z)Ljava/lang/Double;");
        }
        else if (cls == short.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(Z)Ljava/lang/Short;");
        }
        else if (cls == byte.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(Z)Ljava/lang/Byte;");
        }
        else if (cls == char.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(Z)Ljava/lang/Character;");
        }
    }

}
