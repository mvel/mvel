package org.mvel.optimizers.impl.asm;

import org.mvel.Token;
import org.mvel.TokenIterator;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.ExecutableStatement;
import org.mvel.optimizers.Optimizer;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ASMOptimizer implements Optimizer {

    public ExecutableStatement optimize(TokenIterator tokenIterator, Object staticContext, VariableResolverFactory factory) {

        Token token;


        while (tokenIterator.hasMoreTokens()) {
            token = tokenIterator.nextToken();

            if (token.isIdentifier()) {
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                writer.visit(Opcodes.V1_4, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, token.getName().replace(".", "_"),
                        null, "java/lang/Object", new String[]{"org.mvel.optimizers.ExecutableStatement"});

                MethodVisitor m = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                m.visitCode();
                m.visitVarInsn(Opcodes.ALOAD, 0);
                m.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object",
                        "<init>", "()V");
                m.visitInsn(Opcodes.RETURN);
                m.visitMaxs(1, 1);
                m.visitEnd();

                
            }
        }

        return null;
    }

    public String getName() {
        return "ASM";
    }
}
