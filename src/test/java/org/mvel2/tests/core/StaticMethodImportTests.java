package org.mvel2.tests.core;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import static org.junit.Assert.*;
import static org.mvel2.tests.core.StaticMethodImportTests.MatcherAssert.assertThat;
import static org.mvel2.tests.core.StaticMethodImportTests.IsEqual.equalTo;
import static org.mvel2.tests.core.StaticMethodImportTests.IsInstanceOf.instanceOf;
import static org.mvel2.tests.core.StaticMethodImportTests.CoreMatcher.is;

/**
 * Classes ripped from hamcrest for purposes for testing
 * http://code.google.com/p/hamcrest/
 * hamcrest is licensed under "New BDS License"
 *
 */
public class StaticMethodImportTests {

    private static final String IMPORTS =
            "import_static org.mvel2.tests.core.StaticMethodImportTests$MatcherAssert.assertThat;\n" +
            "import_static org.mvel2.tests.core.StaticMethodImportTests$IsEqual.equalTo;\n" +
            "import_static org.mvel2.tests.core.StaticMethodImportTests$IsInstanceOf.instanceOf;\n" +
            "import_static org.mvel2.tests.core.StaticMethodImportTests$CoreMatcher.is;\n";

	@Test
	public void testJava() {
		assertThat(150, is(150));
		testMVEL("assertThat('xxx', 150, is(150));");
		assertThat(150, is(equalTo(150)));
		testMVEL("assertThat('xxx', 150, is(equalTo(150)));");
		
		try {
			assertThat(149, is(150));
			fail( "should have thrown exception" );
		} catch ( AssertionError e) {
			
		}		
		try {
			testMVEL("assertThat(149, is(150));");
			fail( "should have thrown exception" );
		} catch ( Exception e) {
			assertEquals(AssertionError.class, e.getCause().getCause().getClass() );
		}		
		
		try {
			assertThat(149, is(equalTo(150)));
			fail( "should have thrown exception" );
		} catch ( AssertionError e) {
			
		}		
		
		try {
			testMVEL("assertThat(149, is(equalTo(150)));");
			fail( "should have thrown exception" );
		} catch ( Exception e) {
			assertEquals(AssertionError.class, e.getCause().getCause().getClass() );
		}			
		
		assertThat("yoda", is("yoda"));		
		testMVEL("assertThat('yoda', is('yoda'));");
		
		assertThat("yoda", is(equalTo("yoda")));
		testMVEL("assertThat('yoda', is(equalTo('yoda')));");
		
		try {
			assertThat("darth", is("yoda"));
			fail( "should have thrown exception" );
		} catch ( AssertionError e) {
			
		}	
		
		try {
			testMVEL("assertThat('darth', is('yoda'));");
			fail( "should have thrown exception" );
		} catch ( Exception e) {
			assertEquals(AssertionError.class, e.getCause().getCause().getClass() );
		}			
		
		assertThat("yoda", is(instanceOf(String.class)));
		testMVEL("assertThat('yoda', is(instanceOf(String)));");
		
		assertThat("yoda", is(String.class));
		testMVEL("assertThat('yoda', is(String));");
	}
	
	
	private void testMVEL(String text) {
        testMVELUntyped(text);
        testMVELTyped(text);
	}

    private void testMVELUntyped(String text) {
        String str = IMPORTS + text;

        ParserContext pctx = new ParserContext();
        Map<String, Object> vars = new HashMap<String, Object>();

        Object o = MVEL.compileExpression(str, pctx);
        MVEL.executeExpression(o, vars);
    }

    private void testMVELTyped(String text) {
        String str = IMPORTS + text;

        ParserContext pctx = new ParserContext();
        pctx.setStrongTyping(true);
        Map<String, Object> vars = new HashMap<String, Object>();

        Object o = MVEL.compileExpression(str, pctx);
        MVEL.executeExpression(o, vars);
    }

	public static class MatcherAssert {
	    public static <T> void assertThat(T actual, Matcher<? super T> matcher) {
	        if (!matcher.matches(actual)) {	            
	            throw new AssertionError();
	        }	    	
	    }
	    
	    public static <T> void assertThat(String reason, T actual, Matcher<? super T> matcher) {
	        if (!matcher.matches(actual)) {	            
	            throw new AssertionError(reason);
	        }	    	
	    }		
	}
	
	public static class CoreMatcher {

		  public static <T> Matcher<T> is(Matcher<T> matcher) {
		    return Is.<T>is(matcher);
		  }


		  public static <T> Matcher<T> is(T value) {
		    return Is.<T>is(value);
		  }


		  public static <T> Matcher<T> is(java.lang.Class<T> type) {
		    return Is.<T>is(type);
		  }
 
	}	
	
	public static class Is<T> implements Matcher<T> {
	    private final Matcher<T> matcher;

	    public Is(Matcher<T> matcher) {
	        this.matcher = matcher;
	    }
	    
	    public boolean matches(Object arg) {
	        return matcher.matches(arg);
	    }	    
	    
	    public static <T> Matcher<T> is(Matcher<T> matcher) {
	        return new Is<T>(matcher);
	    }

	    public static <T> Matcher<T> is(T value) {
	        return is(equalTo(value));
	    }


	    public static <T> Matcher<T> is(Class<T> type) {
	        final Matcher<T> typeMatcher = instanceOf(type);
	        return is(typeMatcher);
	    }	    
	    
	}
    
	public static class IsEqual<T> implements Matcher<T>{
	    private final Object object;

	    public IsEqual(T equalArg) {
	        object = equalArg;
	    }
	    
	    public boolean matches(Object arg) {
	        return areEqual(arg, object);
	    }	    
	    
	    public static <T> Matcher<T> equalTo(T operand) {
	        return new IsEqual<T>(operand);
	    }		
	    
	    private static boolean areEqual(Object o1, Object o2) {
	        if (o1 == null) {
	            return o2 == null;
	        } else if (o2 != null && isArray(o1)) {
	            return isArray(o2) && areArraysEqual(o1, o2);
	        } else {
	            return o1.equals(o2);
	        }
	    }

	    private static boolean areArraysEqual(Object o1, Object o2) {
	        return areArrayLengthsEqual(o1, o2)
	            && areArrayElementsEqual(o1, o2);
	    }

	    private static boolean areArrayLengthsEqual(Object o1, Object o2) {
	        return Array.getLength(o1) == Array.getLength(o2);
	    }

	    private static boolean areArrayElementsEqual(Object o1, Object o2) {
	        for (int i = 0; i < Array.getLength(o1); i++) {
	            if (!areEqual(Array.get(o1, i), Array.get(o2, i))) return false;
	        }
	        return true;
	    }

	    private static boolean isArray(Object o) {
	        return o.getClass().isArray();
	    }	    
	}
	
    
	public static class IsInstanceOf implements Matcher<Object> {
	    private final Class<?> expectedClass;
	    private final Class<?> matchableClass;


	    public IsInstanceOf(Class<?> expectedClass) {
	        this.expectedClass = expectedClass;
	        this.matchableClass = matchableClass(expectedClass);
	    }
	    
	    public boolean matches(Object item) {
	      if (null == item) {
	        return false;
	      }
	      
	      if (!matchableClass.isInstance(item)) {
	        return false;
	      }
	      
	      return true;
	    }	    
	    
	    private static Class<?> matchableClass(Class<?> expectedClass) {
	        if (boolean.class.equals(expectedClass)) return Boolean.class; 
	        if (byte.class.equals(expectedClass)) return Byte.class; 
	        if (char.class.equals(expectedClass)) return Character.class; 
	        if (double.class.equals(expectedClass)) return Double.class; 
	        if (float.class.equals(expectedClass)) return Float.class; 
	        if (int.class.equals(expectedClass)) return Integer.class; 
	        if (long.class.equals(expectedClass)) return Long.class; 
	        if (short.class.equals(expectedClass)) return Short.class; 
	        return expectedClass;
	      }

	    
	    public static <T> Matcher<T> instanceOf(Class<?> type) {
	        return (Matcher<T>) new IsInstanceOf(type);
	    }	
	    
	    public static <T> Matcher<T> any(Class<T> type) {
	        return (Matcher<T>) new IsInstanceOf(type);
	    }	    
	}	
	
    public static interface Matcher<T> {
        boolean matches(Object item);
    }
}
