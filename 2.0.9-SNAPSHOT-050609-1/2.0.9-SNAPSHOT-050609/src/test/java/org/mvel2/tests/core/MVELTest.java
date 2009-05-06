package org.mvel2.tests.core;

import java.io.File;
import java.io.IOException;

import org.mvel2.MVEL;

import junit.framework.TestCase;

/**
 * @author yone098
 */
public class MVELTest extends TestCase {
	
	private File file;
	
	public void setUp() {
		file = new File("samples/scripts/multibyte.mvel");
	}

	/**
	 * evalFile with encoding(workspace encoding utf-8)
	 * @throws IOException
	 */
	public void testEvalFile1() throws IOException {
		Object obj = MVEL.evalFile(file, "UTF-8");
		assertEquals("?????", obj);
		
		// use default encoding
		obj = MVEL.evalFile(file);
		assertEquals("?????", obj);
	}
}
