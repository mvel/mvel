package org.mvel2.integration.impl;

import org.mvel2.integration.ClassAccessHandler;

public class DefaultClassAccessHandler implements ClassAccessHandler{

	@Override
	public Class<?> getClassInstance(String name, ClassLoader classloader) throws ClassNotFoundException {
		return classloader.loadClass(name);
	}

}
