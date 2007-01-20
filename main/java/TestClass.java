import org.mvel.Token;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.ExecutableStatement;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

public class TestClass {


    public static void main(String[] args) throws Exception {
        Class cls = Class.forName(args[0]);

        for (Method m : cls.getMethods()) {
            System.out.println(Type.getMethodDescriptor(m));
        }
    }

    public Object getValue(ExecutableStatement stmt, Object ctx, VariableResolverFactory resolver) {
        return ((Token)ctx).getName();
    }

}
