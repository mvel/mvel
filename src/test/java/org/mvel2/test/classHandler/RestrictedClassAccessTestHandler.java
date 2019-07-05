package org.mvel2.test.classHandler;

import org.mvel2.integration.ClassAccessHandler;

public class RestrictedClassAccessTestHandler implements ClassAccessHandler{

	@Override
	public Class<?> getClassInstance(String name, ClassLoader classloader) throws ClassNotFoundException {
		if(name.startsWith("java.lang.System")) {
			return null;
		}
		return classloader.loadClass(name);
	}

}
