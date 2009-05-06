package org.mvel.tests.main.res;

import org.mvel.integration.PropertyHandler;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.impl.asm.ProducesBytecode;
import org.mvel.asm.MethodVisitor;
import static org.mvel.asm.Opcodes.*;

public class SampleBeanAccessor implements PropertyHandler, ProducesBytecode {
    public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory) {
        return ((SampleBean) contextObj).getProperty(name);
    }

    public Object setProperty(String name, Object contextObj, VariableResolverFactory variableFactory, Object value) {
        return ((SampleBean) contextObj).setProperty(name, value);
    }

    // implement the bytecode generation stubs to work with the JIT.
    public void produceBytecodeGet(MethodVisitor mv, String propertyName, VariableResolverFactory variableResolverFactory) {
    	mv.visitTypeInsn(CHECKCAST, "org/mvel/tests/main/res/SampleBean");
    	mv.visitLdcInsn(propertyName);
    	mv.visitMethodInsn(INVOKEVIRTUAL, "org/mvel/tests/main/res/SampleBean", "getProperty", "(Ljava/lang/String;)Ljava/lang/Object;");
    }

    public void produceBytecodePut(MethodVisitor mv, String propertyName, VariableResolverFactory variableResolverFactory) {
        throw new RuntimeException("not implemented");
    }
}
