package org.mvel.tests.perftests;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;
import java.util.Map;
import java.util.HashMap;

public class JSPMapVariableResolver implements VariableResolver {

    private Map<String, Object> vars = new HashMap<String, Object>();

    public Object resolveVariable(String string) throws ELException {
        return vars.get(string);
    }

    public JSPMapVariableResolver(Map<String, Object> vars) {
        this.vars = vars;
    }
}
