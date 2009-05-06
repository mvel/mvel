package org.mvel.templates;

import java.util.Iterator;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: christopherbrock
 * Date: Mar 19, 2008
 * Time: 7:43:52 PM
 * To change this template use File | Settings | File Templates.
 */
public interface TemplateRegistry {
    Iterator iterator();

    Set<String> getNames();

    boolean contains(String name);

    void addNamedTemplate(String name, CompiledTemplate template);

    CompiledTemplate getNamedTemplate(String name);
}
