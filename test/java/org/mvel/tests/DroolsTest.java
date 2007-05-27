package org.mvel.tests;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.mvel.MVEL;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class DroolsTest extends TestCase {

    private static ClassLoader classLoader;
    private static Method      defineClass;      
    
    public final static byte[] getRawBytes(File f) {
        try {
            if ( !f.exists() ) return null;
            FileInputStream fin = new FileInputStream( f );
            byte[] buffer = new byte[(int) f.length()];
            fin.read( buffer );
            fin.close();
            return buffer;
        } catch ( Exception err ) {
            err.printStackTrace();
            return null;
        }
    }

    private java.lang.Class loadClass(String className,
                                      byte[] b) throws Exception {
        synchronized ( defineClass ) {
            defineClass.setAccessible( true );
            try {
                //noinspection RedundantArrayCreation
                return (Class) defineClass.invoke( classLoader,
                                                   new Object[]{className, b, 0, (b.length)} );
            } catch ( Exception t ) {
                //dumpAdvancedDebugging();
                throw t;
            } finally {
                defineClass.setAccessible( false );
            }
        }
    }

        public void test1() throws Exception {    
            MVEL.executeExpression( MVEL.compileExpression( "new Integer(5)" ),
                                    null, new HashMap() );  
        }
        
        public void test2() throws Exception {
            byte[] bytes = ASMIntegerDump1.dump();
            
            classLoader = Thread.currentThread().getContextClassLoader();
            defineClass =  ClassLoader.class.getDeclaredMethod("defineClass",
                                                               new Class[]{String.class, byte[].class, int.class, int.class});
            Class clazz = loadClass( "org.mvel.tests.ASMInteger1",
                                     bytes );

        }  
        
        public void test3() throws Exception {
            byte[] bytes = ASMIntegerDump2.dump();
            
            classLoader = Thread.currentThread().getContextClassLoader();
            defineClass =  ClassLoader.class.getDeclaredMethod("defineClass",
                                                               new Class[]{String.class, byte[].class, int.class, int.class});
            Class clazz = loadClass( "org.mvel.tests.ASMInteger2",
                                     bytes );

        }
        
        public static class ASMIntegerDump1
        implements
        Opcodes {


        public static byte[] dump () throws Exception {

        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "org/mvel/tests/ASMInteger1", null, "java/lang/Object", new String[] { "org/mvel/Accessor" });

        {
        fv = cw.visitField(ACC_PRIVATE, "p0", "Lorg/mvel/ExecutableStatement;", null, null);
        fv.visitEnd();
        }
        {
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        }
        {
        mv = cw.visitMethod(ACC_PUBLIC, "getValue", "(Ljava/lang/Object;Ljava/lang/Object;Lorg/mvel/integration/VariableResolverFactory;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, "Ljava/lang/Integer;");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "org/mvel/tests/ASMInteger1", "p0", "Lorg/mvel/ExecutableStatement;");
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEINTERFACE, "org/mvel/ExecutableStatement", "getValue", "(Ljava/lang/Object;Lorg/mvel/integration/VariableResolverFactory;)Ljava/lang/Object;");
        mv.visitLdcInsn(Type.getType("Ljava/lang/Integer;"));
        mv.visitMethodInsn(INVOKESTATIC, "org/mvel/DataConversion", "convert", "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");
        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
        mv.visitMethodInsn(INVOKESPECIAL, "Ljava/lang/Integer;", "<init>", "(I)V");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(5, 4);
        mv.visitEnd();
        }
        {
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Lorg/mvel/ExecutableStatement;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, "org/mvel/tests/ASMInteger1", "p0", "Lorg/mvel/ExecutableStatement;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
        }
    }        
        
        public static class ASMIntegerDump2
        implements
        Opcodes {


        public static byte[] dump () throws Exception {

        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "org/mvel/tests/ASMInteger2", null, "java/lang/Object", new String[] { "org/mvel/Accessor" });

        {
        fv = cw.visitField(ACC_PRIVATE, "p0", "Lorg/mvel/ExecutableStatement;", null, null);
        fv.visitEnd();
        }
        {
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        }
        {
        mv = cw.visitMethod(ACC_PUBLIC, "getValue", "(Ljava/lang/Object;Ljava/lang/Object;Lorg/mvel/integration/VariableResolverFactory;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, "java/lang/Integer");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "org/mvel/tests/ASMInteger2", "p0", "Lorg/mvel/ExecutableStatement;");
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEINTERFACE, "org/mvel/ExecutableStatement", "getValue", "(Ljava/lang/Object;Lorg/mvel/integration/VariableResolverFactory;)Ljava/lang/Object;");
        mv.visitLdcInsn(Type.getType("Ljava/lang/Integer;"));
        mv.visitMethodInsn(INVOKESTATIC, "org/mvel/DataConversion", "convert", "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;");
        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(5, 4);
        mv.visitEnd();
        }
        {
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Lorg/mvel/ExecutableStatement;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, "org/mvel/tests/ASMInteger2", "p0", "Lorg/mvel/ExecutableStatement;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
        }        
    }
}
