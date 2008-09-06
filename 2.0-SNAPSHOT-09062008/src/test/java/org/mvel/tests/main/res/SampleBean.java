package org.mvel.tests.main.res;

import java.util.Map;
import java.util.HashMap;

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
