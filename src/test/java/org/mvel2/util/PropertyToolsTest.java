package org.mvel2.util;

import java.lang.reflect.Method;

import junit.framework.TestCase;
import org.junit.Test;

public class PropertyToolsTest extends TestCase {

    @Test
    public void testGetGetter() {
        Method getter = PropertyTools.getGetter(ClassA.class, "resource");
        assertEquals("isResource", getter.getName());
    }

    public static class ClassA {

        private boolean resource;

        public boolean isResource() {
            return resource;
        }

        public boolean isresource() {
            return resource;
        }

        public boolean getResource() {
            return resource;
        }

        public boolean getresource() {
            return resource;
        }

        public boolean resource() {
            return resource;
        }

        public void setResource(boolean resource) {
            this.resource = resource;
        }
    }

}
