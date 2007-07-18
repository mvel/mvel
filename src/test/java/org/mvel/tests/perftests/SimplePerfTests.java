package org.mvel.tests.perftests;

import org.mvel.*;
import org.mvel.tests.main.res.Foo;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.enhance.ExpressionAccessor;

/**
 * Created by IntelliJ IDEA.
 * User: christopherbrock
 * Date: Jul 18, 2007
 * Time: 3:55:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimplePerfTests {

    public static void main(String[] args) {
        String expression = "foo.bar.name";
        int iterations = 10000;


        Base base = new Base();

        ExecutableAccessor c = (ExecutableAccessor) MVEL.compileExpression(expression);
        c.getValue(base, null);

        Accessor a = c.getNode().getAccessor();

        long tm = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            assert "test".equals(a.getValue(base, null, null));
        }
        System.out.println("MVEL  : " + ((System.currentTimeMillis() - tm)) + "ms");


        ExpressionAccessor accessor = null;
        OgnlContext oCtx = null;

        try {
            accessor = Ognl.compileExpression(oCtx = new OgnlContext(), base, expression).getAccessor();
        }
        catch (Exception e) {

        }

        tm = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            oCtx.clear();
            assert "test".equals(accessor.get(oCtx, base));
        }
        System.out.println("OGNL  : " + ((System.currentTimeMillis() - tm)) + "ms");


    }


    public static class Base {
        private Foo foo = new Foo();


        public Foo getFoo() {
            return foo;
        }

        public void setFoo(Foo foo) {
            this.foo = foo;
        }
    }

    public static class Foo {
        private Bar bar = new Bar();


        public Bar getBar() {
            return bar;
        }

        public void setBar(Bar bar) {
            this.bar = bar;
        }
    }

    public static class Bar {
        private String name = "test";


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
