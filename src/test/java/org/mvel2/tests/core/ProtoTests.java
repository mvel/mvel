package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.MVEL;
import org.mvel2.ast.Proto;

import java.util.HashMap;

public class ProtoTests extends TestCase {
    public void testBasicProtoConstruct() {
        assertTrue(
                MVEL.eval("proto Person { int age; String name; }; new Person();", new HashMap<String, Object>())
                instanceof
                        Proto.ProtoInstance);
    }

}
