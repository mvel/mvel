package org.mvel2.tests.perftests;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.junit.Test;
import org.mvel2.MVEL;
import org.mvel2.optimizers.OptimizerFactory;

public class ConcurrentEvaluationTests extends TestCase {

	Serializable expression = MVEL.compileExpression("Thread.sleep(2000)");

	@Test(timeout = 10000)
	public void testReflective() throws Exception {
		OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
		internalConcurrentEvaluation();
	}

	@Test(timeout = 10000)
	public void testDynamic() throws Exception {
		OptimizerFactory.setDefaultOptimizer(OptimizerFactory.DYNAMIC);
		internalConcurrentEvaluation();
	}

	@Test(timeout = 10000)
	public void testASM() throws Exception {
		OptimizerFactory.setDefaultOptimizer("ASM");
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
						start.await();
						MVEL.executeExpression(expression);
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

}
