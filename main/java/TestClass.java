import org.mvel.Accessor;
import org.mvel.integration.VariableResolverFactory;

public class TestClass implements Accessor {


    public static void main(String[] args) throws Exception {
        int test = 0;
        int a = 1;
        Integer b = 1;

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            test = a;
        }
        System.out.println("(unwrapped) time = " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            test = b;
        }
        System.out.println("(wrapped) time = " + (System.currentTimeMillis() - start));

    }


    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) throws Exception {
    	Short.valueOf((short) 1);
    	Float.valueOf(10f);
    	Double.valueOf(10d);
    	Byte.valueOf((byte) 1);
    	Character.valueOf('a');
    	
    	
    	return System.currentTimeMillis();
    	//((CharSequence)ctx).charAt(10);
    	// return 10;
    	// return ((Map) ((Token)variableFactory.getVariableResolver("foo").getValue()).getValue()).get("test");
    }

 
}
