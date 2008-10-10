package org.mvel2.tests.core.res;

import java.util.HashMap;
import java.util.Map;

public class SampleBean {
    private Map<String, Object> map = new HashMap<String, Object>();

    public SampleBean() {
        map.put("bar", new Bar());
    }

    public Object getProperty(String name) {
        return map.get(name);
    }

    public Object setProperty(String name, Object value) {
        map.put(name, value);
        return value;
    }
}
