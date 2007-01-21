import org.mvel.Accessor;
import org.mvel.Token;
import org.mvel.integration.VariableResolverFactory;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

public class TestClass implements Accessor {


    public static void main(String[] args) throws Exception {
        Class cls = Class.forName(args[0]);

        for (Method m : cls.getMethods()) {
            System.out.println(m + "::" + Type.getMethodDescriptor(m));
        }
    }


    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) throws Exception {
        return ((Object[]) ((Token)variableFactory.getVariableResolver("foo").getValue()).getValue())[6];
    }

 
}
