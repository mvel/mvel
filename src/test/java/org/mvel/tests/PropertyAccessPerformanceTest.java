package org.mvel.tests;

import junit.framework.TestCase;
import ognl.Ognl;
import ognl.OgnlException;
import org.mvel.PropertyAccessor;
import org.mvel.tests.res.Base;


public class PropertyAccessPerformanceTest extends TestCase {
    Base base = new Base();

    public void testPropertyAccess() {
        for (int i = 0; i < 100000; i++) {
            PropertyAccessor.get("foo.bar.name", base);
        }
    }

    public void testPropertyAccessOgnl() throws OgnlException {
        for (int i = 0; i < 100000; i++) {
            Ognl.getValue("foo.bar.name", base);
        }
    }

//    public void testBeanUtil() throws Exception {
//        for (int i = 0; i < 100000; i++) {
//            PropertyUtils.getNestedProperty(base, "foo.bar.name");
//        }
//    }


    public void testPropertyAccess2() {
        testPropertyAccess();
    }

//    public void testPropertyAccessOgnl2() throws OgnlException {
//        testPropertyAccessOgnl();
//    }

//    public void testBeanUtil2() throws Exception {
//        testBeanUtil();
//    }

    public void testSetProperty() {
        for (int i = 0; i < 10000; i++) {
            PropertyAccessor.set(base, "ackbar", "false");
        }
    }

//    public void testSetPropertyOgnl() throws OgnlException {
//        for (int i = 0; i < 100000; i++) {
//            Ognl.setValue("foo.bar.name", base, "false");
//        }
//    }

    public void testSetProperty2() {
        testSetProperty();
    }


    public void testZZReport() {
        PropertyAccessor.reportCacheSizes();
    }
//    public void testSetPropertyOgnl2() throws OgnlException {
//        testSetPropertyOgnl();
//    }

}


