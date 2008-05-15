package org.mvel.templates;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class SimpleTemplateRegistry implements TemplateRegistry {
    private Map<String, CompiledTemplate> NAMED_TEMPLATES = new HashMap<String, CompiledTemplate>();

    public void addNamedTemplate(String name, CompiledTemplate template) {
        NAMED_TEMPLATES.put(name, template);
    }

    public CompiledTemplate getNamedTemplate(String name) {
        CompiledTemplate t = NAMED_TEMPLATES.get(name);
        if (t == null) throw new TemplateError("no named template exists '" + name + "'");
        return t;
    }

    public Iterator iterator() {
        return NAMED_TEMPLATES.keySet().iterator();
    }

    public Set<String> getNames() {
        return NAMED_TEMPLATES.keySet();
    }

    public boolean contains(String name) {
        return NAMED_TEMPLATES.containsKey(name);
    }
}
