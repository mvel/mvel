package org.mvel2.compiler;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mvel2.CompileException;
import org.mvel2.MVEL;

import static org.junit.internal.matchers.StringContains.containsString;

/**
 * @author Anton Rybochkin (anton.rybochkin@axibase.com)
 */
public class CollectionElementByIndexTest {
	@Rule
	public ExpectedException expectException = ExpectedException.none();

	@Test
	public void testGetElementByIndexInSet() {
		expectException.expect(CompileException.class);
		expectException.expectMessage(containsString("unknown type: java.util.Collections$SingletonSet"));
		final Map<String, Set<String>> map = Collections.singletonMap("set", Collections.singleton("test"));
		final Serializable ser = MVEL.compileExpression("set[0]");
		MVEL.executeExpression(ser, map);
	}
}
