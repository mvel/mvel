package org.mvel.tests;

import org.mvel.Interpreter;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class ParserTest {
    public static class TestClass {
        private String foo = "foobie";
        private String bar = "barbie";
        private TestClass sub;
        private ArrayList col = new ArrayList();

        public TestClass() {
            col.add("fun");
            col.add("fun");
            col.add("sun");
        }

        public ArrayList getCol() {
            return col;
        }

        public void setCol(ArrayList col) {
            this.col = col;
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public String getBar() {
            return bar;
        }

        public void setBar(String bar) {
            this.bar = bar;
        }

        public TestClass getSub() {
            if (sub == null) sub = new TestClass();
            return sub;
        }

        public void setSub(TestClass sub) {
            this.sub = sub;
        }

        public String getSomething(String test) {
            return test.toUpperCase();
        }


    }

    public static class Test2 {
        private int figaro = 5;


        public int getFigaro() {
            return figaro;
        }

        public void setFigaro(int figaro) {
            this.figaro = figaro;
        }

    }


    public static void main(String[] args) {
        TestClass test = new TestClass();
        Map<String, Object> tokens = new HashMap<String, Object>();
        tokens.put("apple", test);
        tokens.put("orange", new Test2());
        tokens.put("blah", true);

        System.out.println(new Interpreter("${false == false}").execute(test, tokens));

   /**

        String ex = "\n" +
                "<wha>" +
                "$foreach{apple.col as poo}\n" +
                "   <test>${poo}</test>\n" +
                "   $if{poo.equals(\"fun\")}\n" +
                "       -- IT WAS FUN -- \n" +
                "   $elseif{poo.equals(\"sun\")}\n" +
                "       -- NOT FUN -- \n" +
                "       $foreach{apple.col} MEOW! $end{}\n" +
                "   $else{}\n" +
                "       BWAHH\n" +
                "   $end{}\n" +
                "   kwa!\n" +
                "$end{}" +
                "</wha>";


        long start;
        for (int i = 0; i < 10; i++) {
            start = System.currentTimeMillis();
            System.out.println(new Interpreter(ex).parse(test, tokens));
            System.out.println("exec = " + (System.currentTimeMillis() - start));
        }
        System.out.println(new Interpreter("${apple.getSomething(\"hello world\")}").parse(null, tokens));

        try {

            for (int i = 0; i < 5; i++) {
                start = System.currentTimeMillis();
                System.out.println(Interpreter.parse("${apple.getSomething('${orange.figaro}')}", null, tokens));
                System.out.println("exec = " + (System.currentTimeMillis() - start));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
      **/
    }


}
