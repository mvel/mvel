package org.mvel2.tests.core;

import java.util.HashMap;
import java.util.Map;

import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;

public class ThreadUnsafeTest extends AbstractTest {

    public void testClassImportResolver() {
        MVEL.RUNTIME_OPT_THREAD_UNSAFE = true;

        try {
            ParserContext pCtx = new ParserContext();

            AlgoContext ctx = new AlgoContext();
            ExpressionContext expressionContext = new ExpressionContext();
            ctx.setExpressionContext(expressionContext);
            expressionContext.setContext(ctx);

            Order order = new Order(1, 10, 100.49);
            OrderHelper helper = new OrderHelper();
            helper.setOrder(order);
            expressionContext.setHelper(helper);

            Object compiledExpression = MVEL.compileExpression("import java.util.List; total", pCtx);
            Object compiledExpression1 = MVEL.compileExpression("import java.util.List; farTouchPrice", pCtx);

            double total = (Double) MVEL.executeExpression(compiledExpression, expressionContext, expressionContext.getVariableMap());
            assertEquals(1004.9, total, 0.1);

            double farrouchprice = (Double) MVEL.executeExpression(compiledExpression1, expressionContext, expressionContext.getVariableMap());
            assertEquals(101.4, farrouchprice);

            order = new Order(2, 20, 101.49);
            helper.setOrder(order);
            total = (Double) MVEL.executeExpression(compiledExpression, expressionContext, expressionContext.getVariableMap());
            assertEquals(2029.8, total, 0.1);
        } finally {
            MVEL.RUNTIME_OPT_THREAD_UNSAFE = false;
        }
    }

    public void testStackResolver() {
        MVEL.RUNTIME_OPT_THREAD_UNSAFE = true;

        try {
            ParserContext pCtx = new ParserContext();

            AlgoContext ctx = new AlgoContext();
            ExpressionContext expressionContext = new ExpressionContext();
            ctx.setExpressionContext(expressionContext);
            expressionContext.setContext(ctx);

            Order order = new Order(1, 10,100.49);
            OrderHelper helper = new OrderHelper();
            helper.setOrder(order);
            expressionContext.setHelper(helper);

            Object compiledExpression = MVEL.compileExpression( "total", pCtx );
            double total = (Double) MVEL.executeExpression(compiledExpression, expressionContext, expressionContext.getVariableMap());
            assertEquals(1004.9, total, 0.1);

            Object compiledExpression1 = MVEL.compileExpression( "leavesQty <10 ? 1.0 : (RemainSecond <30? 2.0 : 0.66)", pCtx );
            double remaining = (Double) MVEL.executeExpression(compiledExpression1, expressionContext, expressionContext.getVariableMap());
            assertEquals(0.66, remaining, 0.1);

            order = new Order(2, 20,101.49);
            helper.setOrder(order);
            total = MVEL.executeExpression(compiledExpression, expressionContext, expressionContext.getVariableMap(), Double.class);
            assertEquals(2029.8, total, 0.1);

            Object compiledExpression2 = MVEL.compileExpression( "LeavesUnreservedQty<10 ? 1 : (RemainingSeconds<30?2:0.66)", pCtx );
            int remainingSeconds = (int) MVEL.executeExpression(compiledExpression2, expressionContext, expressionContext.getVariableMap());
            assertEquals(2, remainingSeconds);
        } finally {
            MVEL.RUNTIME_OPT_THREAD_UNSAFE = false;
        }
    }

    public static class AlgoContext {

        ExpressionContext expressionContext;

        public ExpressionContext getExpressionContext() {
            return expressionContext;
        }

        public void setExpressionContext(ExpressionContext expressionContext) {
            this.expressionContext = expressionContext;
        }

        public double getFarTouchPrice()
        {
            return 101.4;
        }
    }

    public class ExpressionContext {
        AlgoContext context;

        OrderHelper helper;

        Map<String, Object> variableMap = new HashMap();

        public AlgoContext getContext() {
            return context;
        }

        public void setContext(AlgoContext context) {
            this.context = context;
        }

        public OrderHelper getHelper() {
            return helper;
        }

        public void setHelper(OrderHelper helper) {
            this.helper = helper;
        }

        public double getTotal() {
            return helper.getQty() * helper.getPrice();
        }

        public Map<String, Object> getVariableMap() {
            return variableMap;
        }

        public double getFarTouchPrice() {
            return context.getFarTouchPrice();
        }

        public void setVariableMap(Map<String, Object> variableMap) {
            this.variableMap = variableMap;
        }

        public boolean HAS(String key) {
            return variableMap.containsKey(key);
        }

        public Object GET(String key) {
            return variableMap.get(key);
        }

        public void SET(String key, Object obj) {
            variableMap.put(key, obj);
        }

        public double getleavesQty() {
            return helper.getQty();
        }

        public int getRemainSecond() {
            return 30;
        }

        public long getLeavesUnreservedQty() {
            return (long) helper.getQty();
        }

        public int getRemainingSeconds() {
            return 5;
        }
    }

    public static class Order {

        long orderId;

        int qty;

        double price;

        public Order(long id, int qty, double price) {
            this.orderId = id;
            this.qty = qty;
            this.price = price;

        }
        public int getQty() {
            return qty;
        }

        public void setQty(int qty) {
            this.qty = qty;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public long getOrderId() {
            return orderId;
        }

        public void setOrderId(long orderId) {
            this.orderId = orderId;
        }
    }

    public static class OrderHelper {
        Order order;

        public double getQty() {
            return order.getQty();
        }

        public double getPrice() {
            return order.getPrice();
        }

        public void setOrder(Order order) {
            this.order = order;
        }
    }
}
