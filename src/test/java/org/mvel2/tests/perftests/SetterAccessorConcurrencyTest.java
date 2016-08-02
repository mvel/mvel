package org.mvel2.tests.perftests;

import junit.framework.TestCase;
import org.junit.Test;
import org.mvel2.MVEL;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.tests.core.res.PojoStatic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SetterAccessorConcurrencyTest extends TestCase {

	private static final Serializable EXPRESSION = MVEL.compileExpression("pojo.value = 2");

	@Test(timeout = 10000)
	public void testDynamic() throws Exception {
		OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);
		// warm up
		MVEL.executeExpression(EXPRESSION, prepareContext());

		internalConcurrentEvaluation();
	}

	private void internalConcurrentEvaluation() throws Exception {
		final int N = 20;
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(N);
		final AtomicInteger errors = new AtomicInteger(0);
		for (int i = 0; i < N; i++) {
			new Thread(new Runnable() {
				public void run() {
					try {
					    HashMap<Object, Object> vars = prepareContext();
						start.await();
                        MVEL.executeExpression(EXPRESSION, vars);
					} catch (Exception e) {
						errors.incrementAndGet();
					} finally {
						end.countDown();
					}
				}
			}, "thread-eval-" + i).start();
		}
		start.countDown();
		assertEquals("Test did not complete withing 10s", true,
				end.await(10, TimeUnit.SECONDS));
		if (errors.get() > 0) {
			fail();
		}
	}

    private static HashMap<Object, Object> prepareContext() {
        HashMap<Object, Object> vars = new HashMap<Object, Object>();
        vars.put("pojo", new PojoStatic("1"));
        return vars;
    }

}
