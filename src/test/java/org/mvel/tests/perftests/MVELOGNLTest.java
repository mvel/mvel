package org.mvel.tests.perftests;

import ognl.OgnlContext;
import ognl.Node;
import ognl.Ognl;
import org.mvel.ExecutableStatement;
import org.mvel.MVEL;

public class MVELOGNLTest {
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
    }

    public static class Bar {
        private String name = "John Doe";


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


    public static void main(String[] args) throws Exception {
        Base base = new Base();

        String ex = "foo.bar.name";

        OgnlContext ctx = new OgnlContext();
        Node node = Ognl.compileExpression(ctx, base, ex);

        long time = System.currentTimeMillis();

        for (int x = 0; x < 3; x++) {
            for (int i = 0; i < 1000000; i++) {
                node.getAccessor().get(new OgnlContext(), base);
            }
            System.out.println("ognl: " + (System.currentTimeMillis() - time) + "ms");

            ExecutableStatement compile = (ExecutableStatement) MVEL.compileExpression(ex);

            time = System.currentTimeMillis();
            for (int i = 0; i < 1000000; i++) {
                compile.getValue(base, null);
            }
            System.out.println("mvel: " + (System.currentTimeMillis() - time) + "ms");
        }


        System.out.println();
    }

}
