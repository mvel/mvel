package org.mvel.tests.main.res;

import java.util.ArrayList;
import java.util.List;

public class DefaultKnowledgeHelper implements KnowledgeHelper {

    public List retracted = new ArrayList();

    public void insert(Object object) {
    }

    public void retract(Object object) {
        retracted.add(object);
    }

    public void retract(FactHandle handle) {
        retracted.add(handle);
    }

}
