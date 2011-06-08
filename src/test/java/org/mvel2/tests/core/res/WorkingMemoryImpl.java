package org.mvel2.tests.core.res;

public class WorkingMemoryImpl implements WorkingMemory {
  private RuleBase ruleBasae;

  public WorkingMemoryImpl(RuleBase ruleBasae) {
    super();
    this.ruleBasae = ruleBasae;
  }

  public RuleBase getRuleBase() {
    return ruleBasae;
  }

}
