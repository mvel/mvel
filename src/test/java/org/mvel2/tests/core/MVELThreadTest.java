package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.optimizers.OptimizerFactory;

import java.io.Serializable;

public class MVELThreadTest {

  public static void main(String[] args) {
    MVELThreadTest threadTest = new MVELThreadTest();
    threadTest.start();
  }

  public void start() {
    //Create two of the same expressions
    final String expression = "firstname";
    final String expression2 = "lastname";

    //Create a bean to run expressions against
    final Bean bean = new Bean();

    //Use reflection mode
    OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);

    //Compile the expressions
    Serializable mvelExp1 = MVEL.compileExpression(expression);
    Serializable mvelExp2 = MVEL.compileExpression(expression2);

    //run the threads
    runThreads(bean, mvelExp1, mvelExp2);
  }

  public void runThreads(final Bean bean, final Serializable mvelExpr1, final Serializable mvelExpr2) {
    //Start 5 threads, each executing the specified MVEL expressions
    for (int i = 0; i < 5; i++) {
      Thread t = new Thread() {
        public void run() {
          testMvel(bean, mvelExpr1, mvelExpr2);
        }
      };
      t.start();
    }
  }

  //by synchronizing the testMvel method, the exception does not occur
  public void testMvel(Bean bean, Serializable mvelExpr1, Serializable mvelExpr2) {
    int iterations = 100;
    for (int i = 0; i < iterations; i++) {
      MVEL.executeExpression(mvelExpr1, bean);
      MVEL.executeExpression(mvelExpr2, bean);
    }
  }

  /**
   * Bean
   */
  public static class Bean {
    private String firstname;

    private String lastname;

    public String getFirstname() {
      return firstname;
    }

    public void setFirstname(String firstname) {
      this.firstname = firstname;
    }

    public String getLastname() {
      return lastname;
    }

    public void setLastname(String lastname) {
      this.lastname = lastname;
    }
  }
}
