package org.mvel2.tests.core.res;

import java.util.ArrayList;
import java.util.List;

public class DefaultKnowledgeHelper implements KnowledgeHelper {

  private WorkingMemory workingMemory;

  public DefaultKnowledgeHelper() {

  }

  public DefaultKnowledgeHelper(WorkingMemory workingMemory) {
    this.workingMemory = workingMemory;
  }

  public WorkingMemory getWorkingMemory() {
    return this.workingMemory;
  }

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
