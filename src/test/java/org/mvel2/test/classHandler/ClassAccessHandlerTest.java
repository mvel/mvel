package org.mvel2.test.classHandler;

import org.mvel2.integration.ClassAccessHandlerFactory;
import org.mvel2.tests.core.AbstractTest;

public class ClassAccessHandlerTest extends AbstractTest{
	
	public void testWithRestrictedClass() {
		ClassAccessHandlerFactory.registerClassHandler(new RestrictedClassAccessTestHandler());
	    try {
	    	test("java.lang.System");
	    } catch(Exception e) {
	    	if(!e.getCause().getMessage().contains("could not access: java;")) {
	    		fail();
	    	}
	    }
	    ClassAccessHandlerFactory.registerDefault();
	}
	
	public void testWithUnrestrictedClass() {
		ClassAccessHandlerFactory.registerClassHandler(new RestrictedClassAccessTestHandler());
	    assertEquals(Integer.class, test("java.lang.Integer"));
	    ClassAccessHandlerFactory.registerDefault();
	}
}
