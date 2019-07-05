package org.mvel2.integration;

public interface ClassAccessHandler {

	/**
	 * Creates object of the class provided  
	 * 
	 * returns null if class is not allowed 
	 * 
	 * @param name
	 * @param classloader
	 * @return the instance object of the provided class
	 * @throws ClassNotFoundException
	 */
	Class<?> getClassInstance(String name, ClassLoader classloader) throws ClassNotFoundException;
	
}
