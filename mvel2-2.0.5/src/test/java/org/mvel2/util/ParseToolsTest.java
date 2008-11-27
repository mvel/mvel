package org.mvel2.util;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author yone098
 */
public class ParseToolsTest extends TestCase {

	/**
	 * muitlbyte character test
	 * 
	 * @throws IOException
	 */
	public void testLoadFromFileWithEncoding() {
		final String expected = "a = \"?????\";";
		char[] actual = null;
		try {
			actual = ParseTools.loadFromFile(new File(
					"samples/scripts/multibyte.mvel"), "UTF-8");
		} catch (IOException e) {
			fail();
		}

		assertEquals(expected, new String(actual));
	}
}
