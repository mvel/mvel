package org.mvel2.tests.core.res;

public interface KnowledgeHelper {
  void insert(Object object);

  void retract(Object object);

  void retract(FactHandle handle);

  WorkingMemory getWorkingMemory();
}
