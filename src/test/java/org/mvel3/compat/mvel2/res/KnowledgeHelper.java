package org.mvel3.compat.mvel2.res;

public interface KnowledgeHelper {
  void insert(Object object);

  void retract(Object object);

  void retract(FactHandle handle);

  WorkingMemory getWorkingMemory();
}
