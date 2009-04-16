package org.mvel2.compiler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;

/**
 * Asserts that the element at the end of the parse chain has its type parameter correctly inferred
 * IF the egress type is a parametric type (i.e. generic).
 *
 * @author Dhanji R. Prasanna (dhanji@gmail com)
 */
public class GenericsTypeInferenceTest extends TestCase {
    private static final List<String> STRINGS = Arrays.asList("hi", "there", "dude");

    public final void testInferLastTypeParametersFromProperty() {
        ParserContext context = new ParserContext();
        context.setStrongTyping(true);

        context.addInput("a", A.class);

        final CompiledExpression compiledExpression = new ExpressionCompiler("a.strings")
                .compile(context);

        final Object val = MVEL.executeExpression(compiledExpression, new AWrapper());

        assertTrue("Expression did not evaluate correctly: " + val, STRINGS.equals(val));
        assertTrue("No type parameters detected", null != context.getLastTypeParameters());
        assertTrue("Wrong parametric type inferred", String.class.equals(context.getLastTypeParameters()[0]));
    }

    public final void testInferLastTypeParametersFromMethod() {
        ParserContext context = new ParserContext();
        context.setStrongTyping(true);

        context.addInput("a", A.class);

        final CompiledExpression compiledExpression = new ExpressionCompiler("a.values()")
                .compile(context);

        final Object val = MVEL.executeExpression(compiledExpression, new AWrapper());

        assertTrue("Expression did not evaluate correctly: " + val, STRINGS.equals(val));
        assertTrue("No type parameters detected", null != context.getLastTypeParameters());
        assertTrue("Wrong parametric type inferred", String.class.equals(context.getLastTypeParameters()[0]));
    }

    public final void testInferLastTypeParametersFromPropertyMethod() {
        ParserContext context = new ParserContext();
        context.setStrongTyping(true);

        context.addInput("a", A.class);

        final CompiledExpression compiledExpression = new ExpressionCompiler("a.getFooMap()[\"key\"].someMethod()")
                .compile(context);

        final Object val = MVEL.executeExpression(compiledExpression, new AWrapper());

        assertEquals("Expression did not evaluate correctly: ", "bar", val);
        assertNotNull("No type parameters detected", context.getLastTypeParameters());
        assertEquals("Wrong parametric type inferred", String.class, context.getLastTypeParameters()[0]);
    }

    public final void testTypeByMethod() {
        ParserContext context = new ParserContext();
        context.setStrongTyping(true);

        context.addInput("a", A.class);

         CompiledExpression compiledExpression = new ExpressionCompiler("!a.show").compile(context);

        assertEquals(Boolean.class, compiledExpression.getKnownEgressType());
    }


//    public final void testInferLastTypeParametersFromPropertyMethod2() {
//        ParserContext context = new ParserContext();
//        context.setStrictTypeEnforcement( true );
//
//        context.addInput("a", A.class);
//
//        ExpressionCompiler compiler = new ExpressionCompiler("a.getBarMap()[\"key\"].someMethod();");
//        final CompiledExpression compiledExpression = compiler.compile(context);
//
//        Map<String,Object> vars = new HashMap<String,Object>();
//        vars.put( "a", new A() );
//        final Object val = MVEL.executeExpression(compiledExpression, vars);
//
//        assertEquals("Expression did not evaluate correctly: ", "bar", val);
//        assertNotNull("No type parameters detected", context.getLastTypeParameters());
//        assertEquals("Wrong parametric type inferred", String.class, context.getLastTypeParameters()[0]);
//    }

    public static class AWrapper {
        public A getA() {
            return new A();
        }
    }

    public static class A {
        private boolean show;

        public boolean isShow() {
            return show;
        }

        public void setShow(boolean show) {
            this.show = show;
        }

        public List<String> getStrings() {
            return STRINGS;
        }

        public List<String> values() {
            return STRINGS;
        }
        
        public Map<String, Foo> getFooMap() {
            Map<String,Foo> map = new HashMap<String,Foo>();
            map.put( "key", new Foo(){
                public String someMethod() {
                    return "bar";
                } } );
            
            return map;
        }
        
        public Map<String, Foo> getBarMap() {
            Map<String,Foo> map = new HashMap<String,Foo>();
            map.put( "key", new FooImpl() );
            return map;
        }
        
        
    }
    
    public static interface Foo {
        
        public String someMethod();
    }
    
    public static class FooImpl implements Foo {

        public String someMethod() {
            return "bar";
        }
        
    }
}
