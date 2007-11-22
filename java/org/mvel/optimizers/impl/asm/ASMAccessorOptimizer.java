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
package org.mvel.optimizers.impl.asm;

import org.mvel.*;
import static org.mvel.DataConversion.canConvert;
import static org.mvel.DataConversion.convert;
import static org.mvel.MVEL.isAdvancedDebugging;
import org.mvel.asm.*;
import static org.mvel.asm.Opcodes.*;
import static org.mvel.asm.Type.*;
import org.mvel.ast.LiteralNode;
import org.mvel.ast.PropertyASTNode;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.AbstractOptimizer;
import org.mvel.optimizers.AccessorOptimizer;
import org.mvel.optimizers.OptimizationNotSupported;
import org.mvel.optimizers.impl.refl.Union;
import static org.mvel.util.ArrayTools.findFirst;
import org.mvel.util.*;
import static org.mvel.util.ParseTools.*;

import java.io.FileWriter;
import java.io.IOException;
import static java.lang.System.getProperty;
import static java.lang.reflect.Array.getLength;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the MVEL Just-in-Time (JIT) compiler for Property Accessors using the ASM bytecode
 * engineering library.
 * <p/>
 * TODO: This class needs serious re-factoring.
 */
public class ASMAccessorOptimizer extends AbstractOptimizer implements AccessorOptimizer {
    private static final String MAP_IMPL = "java/util/HashMap";
    private static final String LIST_IMPL = "org/mvel/util/FastList";

    private static final int OPCODES_VERSION;

    static {
        final String javaVersion = getProperty("java.version");
        if (javaVersion.startsWith("1.4"))
            OPCODES_VERSION = Opcodes.V1_4;
        else if (javaVersion.startsWith("1.5"))
            OPCODES_VERSION = Opcodes.V1_5;
        else if (javaVersion.startsWith("1.6") || javaVersion.startsWith("1.7"))
            OPCODES_VERSION = Opcodes.V1_6;
        else
            OPCODES_VERSION = Opcodes.V1_2;
    }

    private Object ctx;
    private Object thisRef;

    private VariableResolverFactory variableFactory;

    private static final Object[] EMPTYARG = new Object[0];
    private static final Class[] EMPTYCLS = new Class[0];

    private boolean first = true;
    private boolean deferFinish = false;
    private boolean literal = false;

    private String className;
    private ClassWriter cw;
    private MethodVisitor mv;

    private Object val;
    private int stacksize = 1;
    private int maxlocals = 1;
    private long time;

    private ArrayList<ExecutableStatement> compiledInputs;

    private Class returnType;

    @SuppressWarnings({"StringBufferField"})
    private StringAppender buildLog;

    public ASMAccessorOptimizer() {
        //do this to confirm we're running the correct version
        //otherwise should create a verification error in VM
        new ClassWriter(ClassWriter.COMPUTE_MAXS);
    }

    /**
     * Does all the boilerplate for initiating the JIT.
     */
    private void _initJIT() {
        if (isAdvancedDebugging()) {
            buildLog = new StringAppender();
        }

        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);


        synchronized (Runtime.getRuntime()) {
            int r = (int) Math.random() * 100;
            cw.visit(OPCODES_VERSION, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, className = "ASMAccessorImpl_"
                    + String.valueOf(cw.hashCode()).replaceAll("\\-", "_") + (System.currentTimeMillis() / 10) + r,
                    null, "java/lang/Object", new String[]{"org/mvel/Accessor"});
        }

        MethodVisitor m = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitMethodInsn(INVOKESPECIAL, "java/lang/Object",
                "<init>", "()V");
        m.visitInsn(RETURN);

        m.visitMaxs(1, 1);
        m.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "getValue",
                "(Ljava/lang/Object;Ljava/lang/Object;Lorg/mvel/integration/VariableResolverFactory;)Ljava/lang/Object;", null, null);
        mv.visitCode();
    }


    public Accessor optimizeAccessor(char[] property, Object staticContext, Object thisRef, VariableResolverFactory factory, boolean root) {
        time = System.currentTimeMillis();

        //inputs = 0;
        compiledInputs = new ArrayList<ExecutableStatement>();

        start = cursor = 0;

        this.first = true;
        this.val = null;

        this.length = property.length;
        this.expr = property;
        this.ctx = staticContext;
        this.thisRef = thisRef;
        this.variableFactory = factory;

        _initJIT();

        return compileAccessor();
    }


    public SetAccessor optimizeSetAccessor(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory, boolean rootThisRef, Object value) {
        throw new RuntimeException("not implemented");
    }

    private void _finishJIT() {
        if (!deferFinish) {

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
        }

        debug("\n{METHOD STATS (maxstack=" + stacksize + ")}\n");
        mv.visitMaxs(stacksize, maxlocals);

        mv.visitEnd();

        buildInputs();

        cw.visitEnd();

        dumpAdvancedDebugging(); // dump advanced debugging if necessary
    }

    private Accessor _initializeAccessor() throws Exception {

        /**
         * Hot load the class we just generated.
         */
        Class cls = loadClass(className, cw.toByteArray());

        debug("[MVEL JIT Completed Optimization <<" + new String(expr) + ">>]::" + cls + " (time: " + (System.currentTimeMillis() - time) + "ms)");

        Object o;

        try {
            if (compiledInputs.size() == 0) {
                o = cls.newInstance();
            }
            else {
                Class[] parms = new Class[compiledInputs.size()];
                for (int i = 0; i < compiledInputs.size(); i++) {
                    parms[i] = ExecutableStatement.class;
                }
                o = cls.getConstructor(parms).newInstance(compiledInputs.toArray(new ExecutableStatement[compiledInputs.size()]));
            }
        }
        catch (VerifyError e) {
            System.out.println("**** COMPILER BUG! REPORT THIS IMMEDIATELY AT http://jira.codehaus.org/browse/mvel");
            System.out.println("Expression: " + new String(expr));
            throw e;
        }

        if (!(o instanceof Accessor)) {
            dumpAdvancedDebugging();
            throw new RuntimeException("Classloader problem detected. JIT Class is not subclass of org.mvel.Accessor.");
        }

        return (Accessor) o;
    }

    private Accessor compileAccessor() {
        debug("<<INITIATE COMPILE>>");

        Object curr = ctx;

        try {
            while (cursor < length) {
                switch (nextSubToken()) {
                    case BEAN:
                        curr = getBeanProperty(curr, capture());
                        break;
                    case METH:
                        curr = getMethod(curr, capture());
                        break;
                    case COL:
                        curr = getCollectionProperty(curr, capture());
                        break;
                }

                first = false;
            }

            val = curr;

            _finishJIT();

            return _initializeAccessor();
        }
        catch (InvocationTargetException e) {
            throw new PropertyAccessException(new String(expr), e);
        }
        catch (IllegalAccessException e) {
            throw new PropertyAccessException(new String(expr), e);
        }
        catch (IndexOutOfBoundsException e) {
            throw new PropertyAccessException(new String(expr), e);
        }
        catch (PropertyAccessException e) {
            //    throw new PropertyAccessException(e.getMessage(), e);
            throw new CompileException(e.getMessage(), e);
        }
        catch (CompileException e) {
            throw e;
        }
        catch (NullPointerException e) {
            throw new PropertyAccessException(new String(expr), e);
        }
        catch (OptimizationNotSupported e) {
            throw e;
        }
        catch (Exception e) {
            //  throw new PropertyAccessException(new String(expr), e);
            throw new CompileException(e.getMessage(), e);
        }
    }

    private Object getBeanProperty(Object ctx, String property)
            throws IllegalAccessException, InvocationTargetException {

        debug("\n  **  ENTER -> {bean: " + property + "; ctx=" + ctx + "}");

        if (returnType != null && returnType.isPrimitive()) {
            //noinspection unchecked
            wrapPrimitive(returnType);
        }

        Class cls = (ctx instanceof Class ? ((Class) ctx) : ctx != null ? ctx.getClass() : null);
        Member member = cls != null ? PropertyTools.getFieldOrAccessor(cls, property) : null;

        if (first) {
            if (variableFactory != null && variableFactory.isResolveable(property)) {
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

                    returnType = Object.class;
                }
                catch (Exception e) {
                    throw new OptimizationFailure("critical error in JIT", e);
                }

                return variableFactory.getVariableResolver(property).getValue();
            }
            else {
                mv.visitVarInsn(ALOAD, 1);
            }
        }

        if (member instanceof Field) {
            Object o = ((Field) member).get(ctx);

            if (first) {
                debug("ALOAD 1 (A)");
                mv.visitVarInsn(ALOAD, 1);
            }

            if (((member.getModifiers() & Modifier.STATIC) != 0)) {
                debug("GETSTATIC " + getDescriptor(member.getDeclaringClass()) + "."
                        + member.getName() + "::" + getDescriptor(((Field) member).getType()));

                mv.visitFieldInsn(GETSTATIC, getInternalName(member.getDeclaringClass()),
                        member.getName(), getDescriptor(returnType = ((Field) member).getType()));
            }
            else {
                debug("CHECKCAST " + getInternalName(cls));
                mv.visitTypeInsn(CHECKCAST, getInternalName(cls));

                debug("GETFIELD " + property + ":" + getDescriptor(((Field) member).getType()));
                mv.visitFieldInsn(GETFIELD, getInternalName(cls), property, getDescriptor(returnType = ((Field) member).getType()));
            }

            returnType = ((Field) member).getType();

            return o;
        }
        else if (member != null) {
            Object o;

            if (first) {
                debug("ALOAD 1 (B)");
                mv.visitVarInsn(ALOAD, 1);
            }

            try {
                o = ((Method) member).invoke(ctx, EMPTYARG);

                if (returnType != member.getDeclaringClass()) {
                    debug("CHECKCAST " + getInternalName(member.getDeclaringClass()));
                    mv.visitTypeInsn(CHECKCAST, getInternalName(member.getDeclaringClass()));
                }

                returnType = ((Method) member).getReturnType();


                debug("INVOKEVIRTUAL " + member.getName() + ":" + returnType);
                mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(member.getDeclaringClass()), member.getName(),
                        getMethodDescriptor((Method) member));
            }
            catch (IllegalAccessException e) {
                Method iFaceMeth = determineActualTargetMethod((Method) member);

                debug("CHECKCAST " + getInternalName(iFaceMeth.getDeclaringClass()));
                mv.visitTypeInsn(CHECKCAST, getInternalName(iFaceMeth.getDeclaringClass()));

                returnType = iFaceMeth.getReturnType();

                debug("INVOKEINTERFACE " + member.getName() + ":" + returnType);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(iFaceMeth.getDeclaringClass()), member.getName(),
                        getMethodDescriptor((Method) member));

                o = iFaceMeth.invoke(ctx, EMPTYARG);
            }
            return o;

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
        else if (first && "this".equals(property)) {
            debug("ALOAD 2");
            mv.visitVarInsn(ALOAD, 2); // load the thisRef value.

            return this.thisRef;
        }
        else if ("length".equals(property) && ctx.getClass().isArray()) {
            anyArrayCheck(ctx.getClass());

            debug("ARRAYLENGTH");
            mv.visitInsn(ARRAYLENGTH);

            wrapPrimitive(int.class);
            return getLength(ctx);
        }
        else if (LITERALS.containsKey(property)) {
            Object lit = LITERALS.get(property);

            if (lit instanceof Class) {
                ldcClassConstant((Class) lit);
            }

            return LITERALS.get(property);
        }
        else {
            Object ts = tryStaticAccess();

            if (ts != null) {
                if (ts instanceof Class) {
                    ldcClassConstant((Class) ts);
                    return ts;
                }
                else {
                    debug("GETSTATIC " + getDescriptor(((Field) ts).getDeclaringClass()) + "."
                            + ((Field) ts).getName() + "::" + getDescriptor(((Field) ts).getType()));

                    mv.visitFieldInsn(GETSTATIC, getDescriptor(((Field) ts).getDeclaringClass()),
                            ((Field) ts).getName(), getDescriptor(returnType = ((Field) ts).getType()));


                    return ((Field) ts).get(null);
                }
            }
            else if (ctx instanceof Class) {
                /**
                 * This is our ugly support for function pointers.  This works but needs to be re-thought out at some
                 * point.
                 */
                Class c = (Class) ctx;
                for (Method m : c.getMethods()) {
                    if (property.equals(m.getName())) {

                        ldcClassConstant(c);

                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethods", "()[Ljava/lang/reflect/Method;");
                        mv.visitVarInsn(ASTORE, 7);
                        mv.visitInsn(ICONST_0);
                        mv.visitVarInsn(ISTORE, 5);
                        mv.visitVarInsn(ALOAD, 7);
                        mv.visitInsn(ARRAYLENGTH);
                        mv.visitVarInsn(ISTORE, 6);
                        Label l1 = new Label();
                        mv.visitJumpInsn(GOTO, l1);
                        Label l2 = new Label();
                        mv.visitLabel(l2);
                        mv.visitVarInsn(ALOAD, 7);
                        mv.visitVarInsn(ILOAD, 5);
                        mv.visitInsn(AALOAD);
                        mv.visitVarInsn(ASTORE, 4);
                        Label l3 = new Label();
                        mv.visitLabel(l3);
                        mv.visitLdcInsn(m.getName());
                        mv.visitVarInsn(ALOAD, 4);
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getName", "()Ljava/lang/String;");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
                        Label l4 = new Label();
                        mv.visitJumpInsn(IFEQ, l4);
                        Label l5 = new Label();
                        mv.visitLabel(l5);
                        mv.visitVarInsn(ALOAD, 4);
                        mv.visitInsn(ARETURN);
                        mv.visitLabel(l4);
                        mv.visitIincInsn(5, 1);
                        mv.visitLabel(l1);
                        mv.visitVarInsn(ILOAD, 5);
                        mv.visitVarInsn(ILOAD, 6);
                        mv.visitJumpInsn(IF_ICMPLT, l2);
                        Label l6 = new Label();
                        mv.visitLabel(l6);
                        mv.visitInsn(ACONST_NULL);
                        mv.visitInsn(ARETURN);

                        deferFinish = true;

                        return m;
                    }
                }
            }

            throw new PropertyAccessException(property);
        }
    }


    private Object getCollectionProperty(Object ctx, String prop)
            throws IllegalAccessException, InvocationTargetException {
        if (prop.length() > 0) ctx = getBeanProperty(ctx, prop);

        debug("\n  **  ENTER -> {collections: " + prop + "; ctx=" + ctx + "}");

        int start = ++cursor;

        whiteSpaceSkip();

        if (cursor == length)
            throw new CompileException("unterminated '['");

        if (!scanTo(']'))
            throw new CompileException("unterminated '['");

        String tk = new String(expr, start, cursor - start);

        debug("{collection token:<<" + tk + ">>}");

        ExecutableStatement compiled = (ExecutableStatement) subCompileExpression(tk);
        Object item = compiled.getValue(ctx, variableFactory);

        ++cursor;

        if (ctx instanceof Map) {
            debug("CHECKCAST java/util/Map");
            mv.visitTypeInsn(CHECKCAST, "java/util/Map");

            if (item instanceof Integer) {
                intPush((Integer) item);
                wrapPrimitive(int.class);

                debug("INVOKEINTERFACE: get");
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;");

                return ((Map) ctx).get(item);
            }
            else {
                writeLiteralOrSubexpression(compiled);

                debug("INVOKEINTERFACE: get");
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;");

                return ((Map) ctx).get(item);
            }
        }
        else if (ctx instanceof List) {
            debug("CHECKCAST java/util/List");
            mv.visitTypeInsn(CHECKCAST, "java/util/List");

            if (item instanceof Integer) {
                intPush((Integer) item);

                debug("INVOKEINTERFACE: java/util/List.get");
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;");

                return ((List) ctx).get((Integer) item);
            }
            else {
                writeLiteralOrSubexpression(compiled);

                debug("INVOKEINTERFACE: java/util/List.get");
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;");

                dataConversion(Integer.class);
                unwrapPrimitive(int.class);

                return ((List) ctx).get(convert(item, Integer.class));
            }
        }
        else if (ctx instanceof Object[]) {
            debug("CHECKCAST [Ljava/lang/Object;");
            mv.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");
            if (item instanceof Integer) {
                intPush((Integer) item);

                debug("AALOAD");
                mv.visitInsn(AALOAD);

                return ((Object[]) ctx)[(Integer) item];
            }
            else {
                writeLiteralOrSubexpression(compiled, Integer.class);
                unwrapPrimitive(int.class);

                debug("AALOAD");
                mv.visitInsn(AALOAD);

                return ((Object[]) ctx)[convert(item, Integer.class)];
            }
        }
        else if (ctx instanceof CharSequence) {

            debug("CHECKCAST java/lang/CharSequence");
            mv.visitTypeInsn(CHECKCAST, "java/lang/CharSequence");

            if (item instanceof Integer) {
                intPush((Integer) item);

                debug("INVOKEINTERFACE java/lang/CharSequence.charAt");
                mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/CharSequence", "charAt", "(I)C");

                wrapPrimitive(char.class);

                return ((CharSequence) ctx).charAt((Integer) item);
            }
            else {
                writeLiteralOrSubexpression(compiled, Integer.class);
                unwrapPrimitive(int.class);

                debug("INVOKEINTERFACE java/lang/CharSequence.charAt");
                mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/CharSequence", "charAt", "(I)C");

                wrapPrimitive(char.class);

                return ((CharSequence) ctx).charAt(convert(item, Integer.class));

            }
        }
        else {
            throw new CompileException("illegal use of []: unknown type: " + (ctx == null ? null : ctx.getClass().getName()));
        }
    }


    @SuppressWarnings({"unchecked"})
    private Object getMethod(Object ctx, String name)
            throws IllegalAccessException, InvocationTargetException {
        debug("\n  **  {method: " + name + "}");

        if (first && variableFactory != null && variableFactory.isResolveable(name)) {
            Object ptr = variableFactory.getVariableResolver(name).getValue();
            if (ptr instanceof Method) {
                ctx = ((Method) ptr).getDeclaringClass();
                name = ((Method) ptr).getName();
            }
            else if (ptr instanceof MethodStub) {
                ctx = ((MethodStub) ptr).getClassReference();
                name = ((MethodStub) ptr).getMethodName();
            }
            else {
                throw new OptimizationFailure("attempt to optimize a method call for a reference that does not point to a method: "
                        + name + " (reference is type: " + (ctx != null ? ctx.getClass().getName() : null) + ")");
            }

            first = false;
        }
        else if (returnType != null && returnType.isPrimitive()) {
            //noinspection unchecked
            wrapPrimitive(returnType);
        }

        int st = cursor;

        cursor = ParseTools.balancedCapture(expr, cursor, '(');

        String tk = (cursor - st) > 1 ? new String(expr, st + 1, cursor - st - 1) : "";

        cursor++;

        Object[] preConvArgs;
        Object[] args;
        Accessor[] es;

        if (tk.length() == 0) {
            //noinspection ZeroLengthArrayAllocation
            args = new Object[0];

            //noinspection ZeroLengthArrayAllocation
            preConvArgs = new Object[0];
            es = null;
        }
        else {
            String[] subtokens = parseParameterList(tk.toCharArray(), 0, -1);

            es = new ExecutableStatement[subtokens.length];
            args = new Object[subtokens.length];
            preConvArgs = new Object[es.length];

            for (int i = 0; i < subtokens.length; i++) {
                preConvArgs[i] = args[i] = (es[i] = (ExecutableStatement) subCompileExpression(subtokens[i])).getValue(this.ctx, this.thisRef, variableFactory);
            }
        }


        int inputsOffset = compiledInputs.size();

        if (es != null) {
            for (int i = 0; i < es.length; i++) {
                Accessor e = es[i];
                if (e instanceof ExecutableLiteral) {
                    continue;
                }
                else if (e instanceof ExecutableAccessor
                        && ((ExecutableAccessor) e).getNode() instanceof PropertyASTNode
                        && ((PropertyASTNode) ((ExecutableAccessor) e).getNode()).getWrappedNode() instanceof LiteralNode) {
                    es[i] = new ExecutableLiteral(((PropertyASTNode) ((ExecutableAccessor) e).getNode())
                            .getWrappedNode().getLiteralValue());
                    continue;
                }

                compiledInputs.add((ExecutableStatement) e);
            }
        }

        if (first) {
            debug("ALOAD 1 (D) ");
            mv.visitVarInsn(ALOAD, 1);
        }

        /**
         * If the target object is an instance of java.lang.Class itself then do not
         * adjust the Class scope target.
         */
        Class cls = ctx instanceof Class ? (Class) ctx : ctx.getClass();

        Method m;
        Class[] parameterTypes = null;

        /**
         * Try to find an instance method from the class target.
         */
        if ((m = getBestCandidate(args, name, cls.getMethods())) != null) {
            parameterTypes = m.getParameterTypes();
        }

        if (m == null) {
            /**
             * If we didn't find anything, maybe we're looking for the actual java.lang.Class methods.
             */
            if ((m = getBestCandidate(args, name, cls.getClass().getDeclaredMethods())) != null) {
                parameterTypes = m.getParameterTypes();
            }
        }

        if (m == null) {
            StringAppender errorBuild = new StringAppender();

            if (parameterTypes != null) {
                for (int i = 0; i < args.length; i++) {
                    errorBuild.append(parameterTypes[i] != null ? parameterTypes[i].getClass().getName() : null);
                    if (i < args.length - 1) errorBuild.append(", ");
                }
            }

            if ("size".equals(name) && args.length == 0 && cls.isArray()) {
                anyArrayCheck(cls);

                debug("ARRAYLENGTH");
                mv.visitInsn(ARRAYLENGTH);

                wrapPrimitive(int.class);
                return getLength(ctx);
            }

            throw new CompileException("unable to resolve method: " + cls.getName() + "." + name + "(" + errorBuild.toString() + ") [arglength=" + args.length + "]");
        }
        else {
            m = ParseTools.getWidenedTarget(m);

            if (es != null) {
                ExecutableStatement cExpr;
                for (int i = 0; i < es.length; i++) {
                    if ((cExpr = (ExecutableStatement) es[i]).getKnownIngressType() == null) {
                        cExpr.setKnownIngressType(parameterTypes[i]);
                        cExpr.computeTypeConversionRule();
                    }
                    if (!cExpr.isConvertableIngressEgress()) {
                        args[i] = convert(args[i], parameterTypes[i]);
                    }
                }
            }
            else {
                /**
                 * Coerce any types if required.
                 */
                for (int i = 0; i < args.length; i++) {
                    args[i] = convert(args[i], parameterTypes[i]);
                }
            }

            if (m.getParameterTypes().length == 0) {
                if ((m.getModifiers() & Modifier.STATIC) != 0) {
                    debug("INVOKESTATIC " + m.getName());
                    mv.visitMethodInsn(INVOKESTATIC, getInternalName(m.getDeclaringClass()), m.getName(), getMethodDescriptor(m));
                }
                else {
                    debug("CHECKCAST " + getInternalName(m.getDeclaringClass()));
                    mv.visitTypeInsn(CHECKCAST, getInternalName(m.getDeclaringClass()));

                    if (m.getDeclaringClass().isInterface()) {
                        debug("INVOKEINTERFACE " + m.getName());
                        mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(m.getDeclaringClass()), m.getName(),
                                getMethodDescriptor(m));

                    }
                    else {
                        debug("INVOKEVIRTUAL " + m.getName());
                        mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(m.getDeclaringClass()), m.getName(),
                                getMethodDescriptor(m));
                    }
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
                    if (es[i] instanceof ExecutableLiteral) {
                        ExecutableLiteral literal = (ExecutableLiteral) es[i];

                        if (literal.getLiteral() == null) {
                            debug("ICONST_NULL");
                            mv.visitInsn(ACONST_NULL);
                            continue;
                        }
                        else if (parameterTypes[i] == int.class && literal.intOptimized()) {
                            intPush(literal.getInteger32());
                            continue;
                        }
                        else if (parameterTypes[i] == int.class && preConvArgs[i] instanceof Integer) {
                            intPush((Integer) preConvArgs[i]);
                            continue;
                        }
                        else if (parameterTypes[i] == boolean.class) {
                            boolean bool = DataConversion.convert(literal.getLiteral(), Boolean.class);
                            debug(bool ? "ICONST_1" : "ICONST_0");
                            mv.visitInsn(bool ? ICONST_1 : ICONST_0);
                            continue;
                        }
                        else {
                            Object lit = literal.getLiteral();

                            if (parameterTypes[i] == Object.class) {
                                if (isPrimitiveWrapper(lit.getClass())) {
                                    if (lit.getClass() == Integer.class) {
                                        intPush((Integer) lit);
                                    }
                                    else {
                                        debug("LDC " + lit);
                                        mv.visitLdcInsn(lit);
                                    }

                                    wrapPrimitive(lit.getClass());
                                }
                                else if (lit instanceof String) {
                                    mv.visitLdcInsn(lit);
                                    checkcast(Object.class);
                                }
                                continue;
                            }
                            else if (canConvert(parameterTypes[i], lit.getClass())) {
                                debug("LDC " + lit + " (" + lit.getClass().getName() + ")");
                                mv.visitLdcInsn(convert(lit, parameterTypes[i]));
                                continue;
                            }
                        }
                    }

                    debug("ALOAD 0");
                    mv.visitVarInsn(ALOAD, 0);

                    debug("GETFIELD p" + inputsOffset);
                    mv.visitFieldInsn(GETFIELD, className, "p" + inputsOffset, "Lorg/mvel/ExecutableStatement;");

                    inputsOffset++;

                    debug("ALOAD 2");
                    mv.visitVarInsn(ALOAD, 2);

                    debug("ALOAD 3");
                    mv.visitVarInsn(ALOAD, 3);

                    debug("INVOKEINTERFACE ExecutableStatement.getValue");
                    mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(ExecutableStatement.class), "getValue",
                            "(Ljava/lang/Object;Lorg/mvel/integration/VariableResolverFactory;)Ljava/lang/Object;");

                    if (parameterTypes[i].isPrimitive()) {
                        if (preConvArgs[i] == null ||
                                (parameterTypes[i] != String.class &&
                                        !parameterTypes[i].isAssignableFrom(preConvArgs[i].getClass()))) {

                            ldcClassConstant(getWrapperClass(parameterTypes[i]));

                            debug("INVOKESTATIC DataConversion.convert");
                            mv.visitMethodInsn(INVOKESTATIC, "org/mvel/DataConversion", "convert",
                                    "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");

                            unwrapPrimitive(parameterTypes[i]);
                        }

                        else {
                            unwrapPrimitive(parameterTypes[i]);
                        }

                    }
                    else if (preConvArgs[i] == null ||
                            (parameterTypes[i] != String.class &&
                                    !parameterTypes[i].isAssignableFrom(preConvArgs[i].getClass()))) {

                        ldcClassConstant(parameterTypes[i]);

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

            return m.invoke(ctx, args);
        }
    }

//    private void valueFromSubExpression() {
//        debug("ALOAD 0");
//        mv.visitVarInsn(ALOAD, 0);
//        debug("GETFIELD p" + (compiledInputs.size() - 1));
//        mv.visitFieldInsn(GETFIELD, className, "p" + (compiledInputs.size() - 1), "Lorg/mvel/ExecutableStatement;");
//        debug("ALOAD 1");
//        mv.visitVarInsn(ALOAD, 1);
//        debug("ALOAD 3");
//        mv.visitVarInsn(ALOAD, 3);
//        debug("INVOKEINTERFACE org/mvel/ExecutableStatement.getValue");
//        mv.visitMethodInsn(INVOKEINTERFACE, "org/mvel/ExecutableStatement", "getValue", "(Ljava/lang/Object;Lorg/mvel/integration/VariableResolverFactory;)Ljava/lang/Object;");
//    }

    private void dataConversion(Class target) {
        ldcClassConstant(target);
        debug("INVOKESTATIC org/mvel/DataConversion.convert");
        mv.visitMethodInsn(INVOKESTATIC, "org/mvel/DataConversion", "convert", "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");
    }


    private static final ClassLoader classLoader;
    private static final Method defineClass;

    static {
        try {
            classLoader = Thread.currentThread().getContextClassLoader();
            //noinspection RedundantArrayCreation
            defineClass = ClassLoader.class.getDeclaredMethod("defineClass",
                    new Class[]{String.class, byte[].class, int.class, int.class});
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private java.lang.Class loadClass(String className, byte[] b) throws Exception {
        /**
         * This must be synchronized.  Two classes cannot be simultaneously deployed in the JVM.
         */
        synchronized (defineClass) {
            defineClass.setAccessible(true);
            try {
                //noinspection RedundantArrayCreation
                return (Class) defineClass.invoke(classLoader, new Object[]{className, b, 0, (b.length)});
            }
            catch (Exception t) {
                dumpAdvancedDebugging();
                throw t;
            }
            finally {
                defineClass.setAccessible(false);
            }
        }
    }


    private void debug(String instruction) {
        // assert ParseTools.debug(instruction);
        if (buildLog != null) {
            buildLog.append(instruction).append("\n");
        }
    }

    @SuppressWarnings({"SameReturnValue"})
    public String getName() {
        return "ASM";
    }

    public Object getResultOptPass() {
        return val;
    }

    private Class getWrapperClass(Class cls) {
        if (cls == boolean.class) {
            return Boolean.class;
        }
        else if (cls == int.class) {
            return Integer.class;
        }
        else if (cls == float.class) {
            return Float.class;
        }
        else if (cls == double.class) {
            return Double.class;
        }
        else if (cls == short.class) {
            return Short.class;
        }
        else if (cls == long.class) {
            return Long.class;
        }
        else if (cls == byte.class) {
            return Byte.class;
        }
        else if (cls == char.class) {
            return Character.class;
        }

        return null;
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
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
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
        if (OPCODES_VERSION == Opcodes.V1_4) {
            /**
             * JAVA 1.4 SUCKS!  DIE 1.4 DIE!!!
             */

            if (cls == boolean.class || cls == Boolean.class) {
                debug("NEW java/lang/Boolean");
                mv.visitTypeInsn(NEW, "java/lang/Boolean");

                debug("DUP X1");
                mv.visitInsn(DUP_X1);

                debug("SWAP");
                mv.visitInsn(SWAP);

                debug("INVOKESPECIAL java/lang/Boolan.<init>::(Z)V");
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Boolean", "<init>", "(Z)V");

                returnType = Boolean.class;
            }
            else if (cls == int.class || cls == Integer.class) {
                debug("NEW java/lang/Integer");
                mv.visitTypeInsn(NEW, "java/lang/Integer");

                debug("DUP X1");
                mv.visitInsn(DUP_X1);

                debug("SWAP");
                mv.visitInsn(SWAP);

                debug("INVOKESPECIAL java/lang/Integer.<init>::(I)V");
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");

                returnType = Integer.class;
            }
            else if (cls == float.class || cls == Float.class) {
                debug("NEW java/lang/Float");
                mv.visitTypeInsn(NEW, "java/lang/Float");

                debug("DUP X1");
                mv.visitInsn(DUP_X1);

                debug("SWAP");
                mv.visitInsn(SWAP);

                debug("INVOKESPECIAL java/lang/Float.<init>::(F)V");
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Float", "<init>", "(F)V");

                returnType = Float.class;
            }
            else if (cls == double.class || cls == Double.class) {
                debug("NEW java/lang/Double");
                mv.visitTypeInsn(NEW, "java/lang/Double");

                debug("DUP X1");
                mv.visitInsn(DUP_X1);

                debug("SWAP");
                mv.visitInsn(SWAP);

                debug("INVOKESPECIAL java/lang/Double.<init>::(D)V");
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Doble", "<init>", "(D)V");

                returnType = Double.class;
            }
            else if (cls == short.class || cls == Short.class) {
                debug("NEW java/lang/Short");
                mv.visitTypeInsn(NEW, "java/lang/Short");

                debug("DUP X1");
                mv.visitInsn(DUP_X1);

                debug("SWAP");
                mv.visitInsn(SWAP);

                debug("INVOKESPECIAL java/lang/Short.<init>::(S)V");
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Short", "<init>", "(S)V");

                returnType = Short.class;
            }
            else if (cls == long.class || cls == Long.class) {
                debug("NEW java/lang/Long");
                mv.visitTypeInsn(NEW, "java/lang/Long");

                debug("DUP X1");
                mv.visitInsn(DUP_X1);

                debug("SWAP");
                mv.visitInsn(SWAP);

                debug("INVOKESPECIAL java/lang/Long.<init>::(L)V");
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Float", "<init>", "(L)V");

                returnType = Long.class;
            }
            else if (cls == byte.class || cls == Byte.class) {
                debug("NEW java/lang/Byte");
                mv.visitTypeInsn(NEW, "java/lang/Byte");

                debug("DUP X1");
                mv.visitInsn(DUP_X1);

                debug("SWAP");
                mv.visitInsn(SWAP);

                debug("INVOKESPECIAL java/lang/Byte.<init>::(B)V");
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Byte", "<init>", "(B)V");

                returnType = Byte.class;
            }
            else if (cls == char.class || cls == Character.class) {
                debug("NEW java/lang/Character");
                mv.visitTypeInsn(NEW, "java/lang/Character");

                debug("DUP X1");
                mv.visitInsn(DUP_X1);

                debug("SWAP");
                mv.visitInsn(SWAP);

                debug("INVOKESPECIAL java/lang/Character.<init>::(C)V");
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Character", "<init>", "(C)V");

                returnType = Character.class;
            }
        }
        else {
            if (cls == boolean.class || cls == Boolean.class) {
                debug("INVOKESTATIC java/lang/Boolean.valueOf");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
                returnType = Boolean.class;
            }
            else if (cls == int.class || cls == Integer.class) {
                debug("INVOKESTATIC java/lang/Integer.valueOf");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
                returnType = Integer.class;
            }
            else if (cls == float.class || cls == Float.class) {
                debug("INVOKESTATIC java/lang/Float.valueOf");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
                returnType = Float.class;
            }
            else if (cls == double.class || cls == Double.class) {
                debug("INVOKESTATIC java/lang/Double.valueOf");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
                returnType = Double.class;
            }
            else if (cls == short.class || cls == Short.class) {
                debug("INVOKESTATIC java/lang/Short.valueOf");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
                returnType = Short.class;
            }
            else if (cls == long.class || cls == Long.class) {
                debug("INVOKESTATIC java/lang/Long.valueOf");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
                returnType = Long.class;
            }
            else if (cls == byte.class || cls == Byte.class) {
                debug("INVOKESTATIC java/lang/Byte.valueOf");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
                returnType = Byte.class;
            }
            else if (cls == char.class || cls == Character.class) {
                debug("INVOKESTATIC java/lang/Character.valueOf");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
                returnType = Character.class;
            }
        }
    }

    private void anyArrayCheck(Class<? extends Object> cls) {
        if (cls == boolean[].class) {
            debug("CHECKCAST [Z");
            mv.visitTypeInsn(CHECKCAST, "[Z");
        }
        else if (cls == int[].class) {
            debug("CHECKCAST [I");
            mv.visitTypeInsn(CHECKCAST, "[I");
        }
        else if (cls == float[].class) {
            debug("CHECKCAST [F");
            mv.visitTypeInsn(CHECKCAST, "[F");
        }
        else if (cls == double[].class) {
            debug("CHECKCAST [D");
            mv.visitTypeInsn(CHECKCAST, "[D");
        }
        else if (cls == short[].class) {
            debug("CHECKCAST [S");
            mv.visitTypeInsn(CHECKCAST, "[S");
        }
        else if (cls == long[].class) {
            debug("CHECKCAST [J");
            mv.visitTypeInsn(CHECKCAST, "[J");
        }
        else if (cls == byte[].class) {
            debug("CHECKCAST [B");
            mv.visitTypeInsn(CHECKCAST, "[B");
        }
        else if (cls == char[].class) {
            debug("CHECKCAST [C");
            mv.visitTypeInsn(CHECKCAST, "[C");
        }
        else {
            debug("CHECKCAST [Ljava/lang/Object;");
            mv.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");
        }
    }

    private void writeOutLiteralWrapped(Object lit) {
        if (lit instanceof Integer) {
            intPush((Integer) lit);
            wrapPrimitive(int.class);
            return;
        }

        debug("LDC " + lit);
        if (lit instanceof String) {
            mv.visitLdcInsn(lit);
        }
        else if (lit instanceof Long) {
            mv.visitLdcInsn(lit);
            wrapPrimitive(long.class);
        }
        else if (lit instanceof Float) {
            mv.visitLdcInsn(lit);
            wrapPrimitive(float.class);
        }
        else if (lit instanceof Double) {
            mv.visitLdcInsn(lit);
            wrapPrimitive(double.class);
        }
        else if (lit instanceof Short) {
            mv.visitLdcInsn(lit);
            wrapPrimitive(short.class);
        }
        else if (lit instanceof Character) {
            mv.visitLdcInsn(lit);
            wrapPrimitive(char.class);
        }
        else if (lit instanceof Boolean) {
            mv.visitLdcInsn(lit);
            wrapPrimitive(boolean.class);
        }
        else if (lit instanceof Byte) {
            mv.visitLdcInsn(lit);
            wrapPrimitive(byte.class);
        }
    }

//    private void writeOutLiteral(Object lit) {
//        if (lit instanceof Integer) {
//            intPush((Integer) lit);
//            return;
//        }
//
//
//        debug("LDC " + lit);
//        if (lit instanceof String) {
//            mv.visitLdcInsn(lit);
//        }
//        else if (lit instanceof Long) {
//            mv.visitLdcInsn(lit);
//        }
//        else if (lit instanceof Float) {
//            mv.visitLdcInsn(lit);
//        }
//        else if (lit instanceof Double) {
//            mv.visitLdcInsn(lit);
//        }
//        else if (lit instanceof Short) {
//            mv.visitLdcInsn(lit);
//        }
//        else if (lit instanceof Character) {
//            mv.visitLdcInsn(lit);
//        }
//        else if (lit instanceof Boolean) {
//            mv.visitLdcInsn(lit);
//        }
//        else if (lit instanceof Byte) {
//            mv.visitLdcInsn(lit);
//        }
//    }


    private void ldcClassConstant(Class cls) {
        if (OPCODES_VERSION == Opcodes.V1_4) {
            debug("LDC \"" + cls.getName() + "\"");
            mv.visitLdcInsn(cls.getName());
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
            Label l4 = new Label();
            mv.visitJumpInsn(GOTO, l4);
            //    mv.visitLabel(l2);
            mv.visitTypeInsn(NEW, "java/lang/NoClassDefFoundError");
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "getMessage", "()Ljava/lang/String;");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NoClassDefFoundError", "<init>", "(Ljava/lang/String;)V");
            mv.visitInsn(ATHROW);
            mv.visitLabel(l4);

        }
        else {
            debug("LDC " + getType(cls));
            mv.visitLdcInsn(getType(cls));
        }
    }


    private void buildInputs() {
        if (compiledInputs.size() == 0) return;

        debug("\n{SETTING UP MEMBERS...}\n");

        StringAppender constSig = new StringAppender("(");
        int size = compiledInputs.size();

        for (int i = 0; i < size; i++) {
            debug("ACC_PRIVATE p" + i);
            FieldVisitor fv = cw.visitField(ACC_PRIVATE, "p" + i, "Lorg/mvel/ExecutableStatement;", null, null);
            fv.visitEnd();

            constSig.append("Lorg/mvel/ExecutableStatement;");
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
            cv.visitFieldInsn(PUTFIELD, className, "p" + i, "Lorg/mvel/ExecutableStatement;");
        }
        debug("RETURN");
        cv.visitInsn(RETURN);
        cv.visitMaxs(0, 0);
        cv.visitEnd();

        debug("}");
    }

    private static final int ARRAY = 0;
    private static final int LIST = 1;
    private static final int MAP = 2;
    private static final int VAL = 3;


    private int _getAccessor(Object o) {
        if (o instanceof List) {
            debug("NEW " + LIST_IMPL);
            mv.visitTypeInsn(NEW, LIST_IMPL);

            debug("DUP");
            mv.visitInsn(DUP);

            debug("DUP");
            mv.visitInsn(DUP);

            //      stacksize += 2;

            intPush(((List) o).size());
            debug("INVOKESPECIAL " + LIST_IMPL + ".<init>");
            mv.visitMethodInsn(INVOKESPECIAL, LIST_IMPL, "<init>", "(I)V");


            for (Object item : (List) o) {
                if (_getAccessor(item) != VAL) {
                    debug("POP");
                    mv.visitInsn(POP);
                }

                debug("INVOKEINTERFACE java/util/List.add");
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z");

                debug("POP");
                mv.visitInsn(POP);

                debug("DUP");
                mv.visitInsn(DUP);
            }

            return LIST;
        }
        else if (o instanceof Map) {
            debug("NEW " + MAP_IMPL);
            mv.visitTypeInsn(NEW, MAP_IMPL);

            debug("DUP");
            mv.visitInsn(DUP);

            debug("DUP");
            mv.visitInsn(DUP);

            //     stacksize += 2;

            intPush(((Map) o).size());

            debug("INVOKESPECIAL " + MAP_IMPL + ".<init>");
            mv.visitMethodInsn(INVOKESPECIAL, MAP_IMPL, "<init>", "(I)V");

            for (Object item : ((Map) o).keySet()) {
                mv.visitTypeInsn(CHECKCAST, "java/util/Map");

                if (_getAccessor(item) != VAL) {
                    debug("POP");
                    mv.visitInsn(POP);
                }
                if (_getAccessor(((Map) o).get(item)) != VAL) {
                    debug("POP");
                    mv.visitInsn(POP);
                }

                debug("INVOKEINTERFACE java/util/Map.put");
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

                debug("POP");
                mv.visitInsn(POP);

                debug("DUP");
                mv.visitInsn(DUP);

            }

            return MAP;
        }
        else if (o instanceof Object[]) {
            intPush(((Object[]) o).length);

            debug("ANEWARRAY (" + o.hashCode() + ")");
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

            debug("DUP");
            mv.visitInsn(DUP);

            //      stacksize++;

            int i = 0;
            for (Object item : (Object[]) o) {
                intPush(i);

                if (_getAccessor(item) != VAL) {
                    debug("POP");
                    mv.visitInsn(POP);
                }

                debug("AASTORE (" + o.hashCode() + ")");
                mv.visitInsn(AASTORE);

                debug("DUP");
                mv.visitInsn(DUP);

                i++;
            }

            return ARRAY;
        }
        else {
            writeLiteralOrSubexpression(subCompileExpression((String) o));
            return VAL;
        }
    }

    private Class writeLiteralOrSubexpression(Object stmt) {
        return writeLiteralOrSubexpression(stmt, null);
    }


    private Class writeLiteralOrSubexpression(Object stmt, Class desiredTarget) {
        if (stmt instanceof ExecutableLiteral) {
            Class type = ((ExecutableLiteral) stmt).getLiteral().getClass();

            if (desiredTarget != null && type != desiredTarget) {
                dataConversion(desiredTarget);
                writeOutLiteralWrapped(convert(((ExecutableLiteral) stmt).getLiteral(), desiredTarget));
            }
            else {
                writeOutLiteralWrapped(((ExecutableLiteral) stmt).getLiteral());
            }

            return type;
        }
        else {
            literal = false;

            compiledInputs.add((ExecutableStatement) stmt);

            debug("ALOAD 0");
            mv.visitVarInsn(ALOAD, 0);

            debug("GETFIELD p" + (compiledInputs.size() - 1));
            mv.visitFieldInsn(GETFIELD, className, "p" + (compiledInputs.size() - 1), "Lorg/mvel/ExecutableStatement;");

            debug("ALOAD 2");
            mv.visitVarInsn(ALOAD, 2);

            debug("ALOAD 3");
            mv.visitVarInsn(ALOAD, 3);

            debug("INVOKEINTERFACE ExecutableStatement.getValue");
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(ExecutableStatement.class), "getValue",
                    "(Ljava/lang/Object;Lorg/mvel/integration/VariableResolverFactory;)Ljava/lang/Object;");

            Class type = ((ExecutableStatement) stmt).getKnownEgressType();

            if (desiredTarget != null && type != desiredTarget) {
                dataConversion(desiredTarget);
            }

            return type;
        }


    }

    private void addPrintOut(String text) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn(text);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
    }


    public Accessor optimizeCollection(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        this.cursor = 0;
        this.length = (this.expr = property).length;
        this.compiledInputs = new ArrayList<ExecutableStatement>();

        this.ctx = ctx;
        this.thisRef = thisRef;
        this.variableFactory = factory;

        CollectionParser parser = new CollectionParser();
        Object o = ((List) parser.parseCollection(property)).get(0);

        _initJIT();

        literal = true;

        switch (_getAccessor(o)) {
            case LIST:
                this.returnType = List.class;
                break;
            case MAP:
                this.returnType = Map.class;
                break;
            case ARRAY:
                this.returnType = Object[].class;
                break;
        }

        _finishJIT();

        int end = parser.getCursor() + 2;
        try {
            Accessor compiledAccessor = _initializeAccessor();

            if (end < property.length) {
                return new Union(compiledAccessor, subset(property, end));
            }
            else {
                return compiledAccessor;
            }

        }
        catch (Exception e) {
            throw new OptimizationFailure("could not optimize collection", e);
        }
    }

    private void checkcast(Class cls) {
        mv.visitTypeInsn(CHECKCAST, getInternalName(cls));
    }

    private void intPush(int index) {
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
        else if (index < Byte.MAX_VALUE) {
            debug("BIPUSH " + index);
            mv.visitIntInsn(BIPUSH, index);
        }
        else {
            debug("SIPUSH " + index);
            mv.visitIntInsn(SIPUSH, index);
        }
    }

    public Accessor optimizeObjectCreation(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        _initJIT();

        compiledInputs = new ArrayList<ExecutableStatement>();
        this.length = (this.expr = property).length;
        this.ctx = ctx;
        this.thisRef = thisRef;
        this.variableFactory = factory;

        String[] cnsRes = captureContructorAndResidual(property);
        String[] constructorParms = parseMethodOrConstructor(cnsRes[0].toCharArray());

        try {
            if (constructorParms != null) {
                for (String constructorParm : constructorParms) {
                    compiledInputs.add((ExecutableStatement) subCompileExpression(constructorParm));
                }

                String s = new String(subset(property, 0, findFirst('(', property)));

                Class cls = findClass(factory, s);

                debug("NEW " + getInternalName(cls));
                mv.visitTypeInsn(NEW, getInternalName(cls));
                debug("DUP");
                mv.visitInsn(DUP);

                //inputs = constructorParms.length;

                Object[] parms = new Object[constructorParms.length];

                int i = 0;
                for (ExecutableStatement es : compiledInputs) {
                    parms[i++] = es.getValue(ctx, factory);
                }

                Constructor cns = getBestConstructorCanadidate(parms, cls);

                if (cns == null)
                    throw new CompileException("unable to find constructor for: " + cls.getName());

                Class tg;
                for (i = 0; i < constructorParms.length; i++) {
                    debug("ALOAD 0");
                    mv.visitVarInsn(ALOAD, 0);
                    debug("GETFIELD p" + i);
                    mv.visitFieldInsn(GETFIELD, className, "p" + i, "Lorg/mvel/ExecutableStatement;");
                    debug("ALOAD 2");
                    mv.visitVarInsn(ALOAD, 2);
                    debug("ALOAD 3");
                    mv.visitVarInsn(ALOAD, 3);
                    debug("INVOKEINTERFACE org/mvel/ExecutableStatement.getValue");
                    mv.visitMethodInsn(INVOKEINTERFACE, "org/mvel/ExecutableStatement", "getValue", "(Ljava/lang/Object;Lorg/mvel/integration/VariableResolverFactory;)Ljava/lang/Object;");

                    tg = cns.getParameterTypes()[i].isPrimitive()
                            ? getWrapperClass(cns.getParameterTypes()[i]) : cns.getParameterTypes()[i];

                    if (!parms[i].getClass().isAssignableFrom(cns.getParameterTypes()[i])) {
                        ldcClassConstant(tg);

                        debug("INVOKESTATIC org/mvel/DataConversion.convert");
                        mv.visitMethodInsn(INVOKESTATIC, "org/mvel/DataConversion", "convert", "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");

                        if (cns.getParameterTypes()[i].isPrimitive()) {
                            unwrapPrimitive(cns.getParameterTypes()[i]);
                        }
                        else {
                            debug("CHECKCAST " + getInternalName(tg));
                            mv.visitTypeInsn(CHECKCAST, getInternalName(tg));
                        }

                    }
                    else {
                        debug("CHECKCAST " + getInternalName(cns.getParameterTypes()[i]));
                        mv.visitTypeInsn(CHECKCAST, getInternalName(cns.getParameterTypes()[i]));
                    }

                }

                debug("INVOKESPECIAL " + getInternalName(cls) + ".<init> : " + getConstructorDescriptor(cns));
                mv.visitMethodInsn(INVOKESPECIAL, getInternalName(cls), "<init>", getConstructorDescriptor(cns));

                _finishJIT();
                Accessor acc = _initializeAccessor();

                if (cnsRes.length > 1 && cnsRes[1] != null && !cnsRes[1].trim().equals("")) {
                    return new Union(acc, cnsRes[1].toCharArray());
                }

                return acc;
            }
            else {
                Class cls = findClass(factory, new String(property));

                debug("NEW " + getInternalName(cls));
                mv.visitTypeInsn(NEW, getInternalName(cls));
                debug("DUP");
                mv.visitInsn(DUP);

                Constructor cns = cls.getConstructor(EMPTYCLS);

                debug("INVOKESPECIAL <init>");

                mv.visitMethodInsn(INVOKESPECIAL, getInternalName(cls), "<init>", getConstructorDescriptor(cns));

                _finishJIT();
                Accessor acc = _initializeAccessor();

                if (cnsRes.length > 1 && cnsRes[1] != null && !cnsRes[1].trim().equals("")) {
                    return new Union(acc, cnsRes[1].toCharArray());
                }

                return acc;
            }
        }
        catch (ClassNotFoundException e) {
            throw new CompileException("class or class reference not found: " + new String(property));
        }
        catch (Exception e) {
            throw new OptimizationFailure("could not optimize construtor: " + new String(property), e);
        }
    }


    public Accessor optimizeFold(char[] property, Object ctx, Object thisRef, VariableResolverFactory factory) {
        throw new OptimizationNotSupported("JIT does not yet support fold operations.");
    }

    public Class getEgressType() {
        return returnType;
    }


    private void dumpAdvancedDebugging() {
        if (buildLog == null) return;

        System.out.println("JIT Compiler Dump for: <<" + new String(expr) + ">>\n-------------------------------\n");
        System.out.println(buildLog.toString());
        System.out.println("\n<END OF DUMP>\n");
        if (MVEL.isFileDebugging()) {
            try {
                FileWriter writer = ParseTools.getDebugFileWriter();
                writer.write(buildLog.toString());
                writer.flush();
                writer.close();
            }
            catch (IOException e) {
                // --
            }
        }
    }

    public boolean isLiteralOnly() {
        return literal;
    }
}
