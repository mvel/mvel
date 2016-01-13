package org.mvel2.tests.core.res;

public interface OverloadedInterface {

	public void putXX(int a, int b);
	
	public void putXX(int a, String b[]);
	
	public void putXX(int a, String b);
	
}
