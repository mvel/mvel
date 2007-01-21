import org.mvel.Accessor;
import org.mvel.DataConversion;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.optimizers.ExecutableStatement;

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

    private ExecutableStatement p1;
    private ExecutableStatement p2;
    
    public TestClass(ExecutableStatement p1, ExecutableStatement p2) {
    	this.p1 = p1;
    	this.p2 = p2;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
    	Short s = Short.valueOf((short) 1);
    	Float f = Float.valueOf(10f);
    	Double d = Double.valueOf(10d);
    	Byte b =Byte.valueOf((byte) 1);
    	Character c = Character.valueOf('a');
    	

        variableFactory.createVariable(String.valueOf(s), DataConversion.convert(p1.getValue(ctx, variableFactory), String.class));

        ;
        
        return s;

        // return System.currentTimeMillis();
    	//((CharSequence)ctx).charAt(10);
    	// return 10;
    	// return ((Map) ((Token)variableFactory.getVariableResolver("foo").getValue()).getValue()).get("test");
    }

 
}
