package org.mvel2.integration;

import org.mvel2.integration.impl.DefaultClassAccessHandler;

public class ClassAccessHandlerFactory {

	private static ClassAccessHandler classAccessHandler = new DefaultClassAccessHandler();

	public static void registerClassHandler(ClassAccessHandler classHandler) {
		classAccessHandler = classHandler;
	}
	
	public static void registerDefault() {
		classAccessHandler = new DefaultClassAccessHandler();
	}
	
	public static ClassAccessHandler getClassAccessHandler() {
		return classAccessHandler;
	}
	
}
