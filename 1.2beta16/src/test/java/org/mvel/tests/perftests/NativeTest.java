package org.mvel.tests.perftests;

import org.mvel.tests.main.res.Base;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Mike Brock
 * Date: 19-Feb-2007
 * Time: 8:30:20 PM
 * To change this template use File | Settings | File Templates.
 */
public interface NativeTest {
    public Object run(Object baseClass, Map vars);
}
