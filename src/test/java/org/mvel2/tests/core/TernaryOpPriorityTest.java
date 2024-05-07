package org.mvel2.tests.core;

import java.io.Serializable;
import java.util.HashMap;

import junit.framework.Assert;
import org.mvel2.MVEL;
import org.mvel2.tests.BaseMvelTestCase;

public class TernaryOpPriorityTest extends BaseMvelTestCase {
	public void testTernaryOperatorPriority_Interpreted () {
		@SuppressWarnings("unused")
		int javaResult = false ? true ? 9 : 5 : 1;
		Integer mvelResult = MVEL.eval("false ? true ? 9 : 5 : 1", Integer.class);
	    Assert.assertEquals(javaResult, mvelResult.intValue());
	}
	public void testTernaryOperatorPriority_Compiled () {
		int javaResult = false ? true ? 9 : 5 : 1;
		Serializable f = MVEL.compileExpression("false ? true ? 9 : 5 : 1");
		Integer mvelResult = MVEL.executeExpression(f, new HashMap(), new HashMap(), Integer.class);
	    Assert.assertEquals(javaResult, mvelResult.intValue());
	}
}
