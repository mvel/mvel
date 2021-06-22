package org.mvel2.tests.perftests;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import org.junit.Test;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.IndexedVariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.tests.core.res.DefaultKnowledgeHelper;
import org.mvel2.tests.core.res.NumberHolder;

public class ConcurrentEvaluationWithContextTests extends TestCase {

    private static final int LOOP = 100;

    private String[] names = {"result", "$bus", "$title"};

    private Serializable expression;

    private void compileExpression() {

        boolean allowNakedMethCall = MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL;
        boolean allowOverrideAllProphandling = MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING;
        boolean allowResolveInnerclassesWithDotnotation = MVEL.COMPILER_OPT_ALLOW_RESOLVE_INNERCLASSES_WITH_DOTNOTATION;
        boolean supportJavaStyleClassLiterals = MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS;

        try {
            // mimic drools usage
            MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true;
            MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
            MVEL.COMPILER_OPT_ALLOW_RESOLVE_INNERCLASSES_WITH_DOTNOTATION = true;
            MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS = true;

            ParserContext parserContext = ParserContext.create();
            parserContext.setStrictTypeEnforcement(true);
            parserContext.setStrongTyping(true);
            parserContext.setIndexAllocation(true);

            parserContext.addIndexedInput(names);
            parserContext.addInput("result", List.class);
            parserContext.addInput("$bus", Bus.class);
            parserContext.addInput("$title", String.class);

            expression = MVEL.compileExpression("result.add($bus.karaoke.dvd[$title].artist);", parserContext);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = allowNakedMethCall;
            MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = allowOverrideAllProphandling;
            MVEL.COMPILER_OPT_ALLOW_RESOLVE_INNERCLASSES_WITH_DOTNOTATION = allowResolveInnerclassesWithDotnotation;
            MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS = supportJavaStyleClassLiterals;
        }

    }

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

        for (int loopCount = 0; loopCount < LOOP; loopCount++) {

            compileExpression();

            final int N = 20;
            final CountDownLatch start = new CountDownLatch(1);
            final CountDownLatch end = new CountDownLatch(N);
            final AtomicInteger errors = new AtomicInteger(0);
            for (int i = 0; i < N; i++) {
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            DefaultKnowledgeHelper helper = new DefaultKnowledgeHelper();

                            // result
                            List<String> result = new ArrayList<>();

                            // $bus
                            Bus bus = new Bus("red", 30);
                            bus.getKaraoke().getDvd().put("POWER PLANT", new Album("POWER PLANT", "GAMMA RAY"));
                            bus.getKaraoke().getDvd().put("Somewhere Out In Space", new Album("Somewhere Out In Space", "GAMMA RAY"));

                            // $title
                            String title = "POWER PLANT";

                            NumberHolder holder = new NumberHolder();
                            holder.setBigDecimal(new BigDecimal("120"));
                            Object[] values = {result, bus, title};

                            VariableResolverFactory variableResolverFactory = new IndexedVariableResolverFactory(names, values);

                            start.await();
                            MVEL.executeExpression(expression, helper, variableResolverFactory);
                        } catch (Exception e) {
                            e.printStackTrace();
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
                System.out.println("#### loopCount = " + loopCount);
                fail();
            }
        }
    }

    public static class Bus {

        private String name;
        private int capacity;
        private BigDecimal weight;
        private Karaoke karaoke = new Karaoke();

        public Bus(String name, int capacity) {
            this.name = name;
            this.capacity = capacity;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPerson() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public void setWeight(BigDecimal weight) {
            this.weight = weight;
        }

        public Karaoke getKaraoke() {
            return karaoke;
        }
    }

    public static class Karaoke {

        private Map<String, Album> dvd = new HashMap<>();

        public Map<String, Album> getDvd() {
            return dvd;
        }

        public void fix() {
            dvd = Collections.unmodifiableMap(dvd);
        }
    }

    public static class Album {

        private String title;
        private String artist;

        public Album(String title, String artist) {
            this.title = title;
            this.artist = artist;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }
    }
}
