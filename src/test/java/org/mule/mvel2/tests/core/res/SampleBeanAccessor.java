package org.mule.mvel2.tests.core.res;

import org.mule.mvel2.asm.MethodVisitor;

import static org.mule.mvel2.asm.Opcodes.CHECKCAST;
import static org.mule.mvel2.asm.Opcodes.INVOKEVIRTUAL;

import org.mule.mvel2.integration.PropertyHandler;
import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.mvel2.optimizers.impl.asm.ProducesBytecode;

public class SampleBeanAccessor implements PropertyHandler, ProducesBytecode {
  public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory) {
    return ((SampleBean) contextObj).getProperty(name);
  }

  public Object setProperty(String name, Object contextObj, VariableResolverFactory variableFactory, Object value) {
    return ((SampleBean) contextObj).setProperty(name, value);
  }

  // implement the bytecode generation stubs to work with the JIT.
  public void produceBytecodeGet(MethodVisitor mv, String propertyName, VariableResolverFactory variableResolverFactory) {
    mv.visitTypeInsn(CHECKCAST, "org/mule/mvel2/tests/core/res/SampleBean");
    mv.visitLdcInsn(propertyName);
    mv.visitMethodInsn(INVOKEVIRTUAL, "org/mule/mvel2/tests/core/res/SampleBean", "getProperty", "(Ljava/lang/String;)Ljava/lang/Object;");
  }

  public void produceBytecodePut(MethodVisitor mv, String propertyName, VariableResolverFactory variableResolverFactory) {
    throw new RuntimeException("not implemented");
  }
}
