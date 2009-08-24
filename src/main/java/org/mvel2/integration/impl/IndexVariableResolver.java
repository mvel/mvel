package org.mvel2.integration.impl;

import org.mvel2.integration.VariableResolver;

/**
 * Created by IntelliJ IDEA.
 * User: christopherbrock
 * Date: 24-Aug-2009
 * Time: 12:48:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class IndexVariableResolver implements VariableResolver {
    private int indexPos;
    private Object[] vars;

    public IndexVariableResolver(int indexPos, Object[] vars) {
        this.indexPos = indexPos;
        this.vars = vars;
    }

    public String getName() {
        return null;
    }

    public Class getType() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setStaticType(Class type) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getFlags() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getValue() {
        return vars[indexPos];
    }

    public void setValue(Object value) {
       vars[indexPos] = value;
    }
}
