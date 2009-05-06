package org.mvel2.tests.core;

import org.mvel2.tests.core.res.Base;
import org.mvel2.integration.PropertyHandlerFactory;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.PropertyAccessor;
import org.mvel2.MVEL;
import junit.framework.TestCase;

import java.util.List;
import java.util.Map;
import java.lang.reflect.Array;

public class PropertyHandlerTests extends TestCase {
    Base base = new Base();

    public void testListPropertyHandler() {
        MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;

        PropertyHandlerFactory.registerPropertyHandler(List.class, new
                PropertyHandler() {
                    public Object getProperty(String name, Object contextObj,
                                              VariableResolverFactory variableFactory) {
                        assertNotNull(contextObj);
                        assertEquals("0", name);
                        assertTrue(contextObj instanceof List);
                        return "gotcalled";
                    }

                    public Object setProperty(String name, Object contextObj,
                                              VariableResolverFactory variableFactory, Object value) {
                        assertNotNull(contextObj);
                        assertEquals("0", name);
                        assertTrue(contextObj instanceof List);
                        ((List) contextObj).set(0, "set");
                        return null;
                    }
                });

        assertEquals("gotcalled", PropertyAccessor.get("list[0]", base));

        PropertyAccessor.set(base, "list[0]", "hey you");
        assertEquals("set", base.list.get(0));

        MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = false;

    }

    public void testMapPropertyHandler() {
        MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;


        PropertyHandlerFactory.registerPropertyHandler(Map.class, new
                PropertyHandler() {
                    public Object getProperty(String name, Object contextObj,
                                              VariableResolverFactory variableFactory) {
                        assertNotNull(contextObj);
                        assertEquals("'key'", name);
                        assertTrue(contextObj instanceof Map);
                        return "gotcalled";
                    }

                    public Object setProperty(String name, Object contextObj,
                                              VariableResolverFactory variableFactory, Object value) {
                        assertNotNull(contextObj);
                        assertEquals("'key'", name);
                        assertTrue(contextObj instanceof Map);
                        ((Map) contextObj).put("key", "set");
                        return null;
                    }
                });

        assertEquals("gotcalled", PropertyAccessor.get("funMap['key']", base));

        PropertyAccessor.set(base, "funMap['key']", "hey you");
        assertEquals("set", base.funMap.get("key"));

        MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = false;

    }

    public void testArrayPropertyHandler() {

        MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;

        PropertyHandlerFactory.registerPropertyHandler(Array.class, new
                PropertyHandler() {
                    public Object getProperty(String name, Object contextObj,
                                              VariableResolverFactory variableFactory) {
                        assertNotNull(contextObj);
                        assertEquals("0", name);
                        assertTrue(contextObj.getClass().isArray());
                        return "gotcalled";
                    }

                    public Object setProperty(String name, Object contextObj,
                                              VariableResolverFactory variableFactory, Object value) {
                        assertNotNull(contextObj);
                        assertEquals("0", name);
                        assertTrue(contextObj.getClass().isArray());
                        Array.set(contextObj, 0, "set");
                        return null;
                    }
                });

        assertEquals("gotcalled", PropertyAccessor.get("stringArray[0]", base));

        PropertyAccessor.set(base, "stringArray[0]", "hey you");
        assertEquals("set", base.stringArray[0]);

        MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = false;        
    }
}