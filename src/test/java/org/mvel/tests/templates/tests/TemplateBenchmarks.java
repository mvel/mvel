package org.mvel.templates.tests;

import org.mvel.TemplateInterpreter;
import org.mvel.templates.TemplateRuntime;

import java.util.Map;
import java.util.HashMap;


public class TemplateBenchmarks {

    public static void main(String[] args) {
        Map map = new HashMap();
        map.put("foo", "Foo");
        map.put("bar", "Bar");

        String template = "test: @{foo}--@{bar}!";

      TemplateInterpreter.setDisableCache(true);

        for (int i = 0; i < 4; i++) {
            test12(template, map);
            test20(template, map);
        }

    }


    public static void test12(String s, Map vars) {
        long time = System.currentTimeMillis();

  //    String result = (String) TemplateInterpreter.eval(s, vars);

        for (int i = 0; i < 10000; i++) {
            TemplateInterpreter.eval(s, vars);
        }
        time = System.currentTimeMillis() - time;

 //      System.out.println("result=" + result);
        System.out.println("1.2 result = " + time + "ms");
    }

    public static void test20(String s, Map vars) {
        long time = System.currentTimeMillis();

        for (int i = 0; i < 10000; i++) {
            TemplateRuntime.eval(s, vars);
        }
        time = System.currentTimeMillis() - time;

        System.out.println("2.0 result = " + time + "ms");
    }
}
