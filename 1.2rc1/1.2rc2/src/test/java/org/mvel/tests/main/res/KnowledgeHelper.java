package org.mvel.tests.main.res;

public interface KnowledgeHelper {
    void insert(Object object);
    void retract(Object object);
    void retract(FactHandle handle);
}
