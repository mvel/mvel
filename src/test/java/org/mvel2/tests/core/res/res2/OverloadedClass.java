package org.mvel2.tests.core.res.res2;

import org.mvel2.tests.core.res.OverloadedInterface;

public class OverloadedClass implements OverloadedInterface{

	public void putXX(int a, int b) {
	}

	public void putXX(int a, String b) {
	}
	
	public void putXX(int a, String[] b) {
	}
	
}
