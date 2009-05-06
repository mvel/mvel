package org.mvel.tests.main;

import org.mvel.MVEL;
import org.mvel.MVELRuntime;
import org.mvel.debug.DebugTools;
import org.mvel.optimizers.OptimizerFactory;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.integration.impl.MapVariableResolverFactory;

import java.io.Serializable;
import java.util.HashMap;


public class ExampleTest {
    public static class Person {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }


    public static void main(String[] args) {
        Person per = new Person();
        per.setName("John Doe");
        per.setAge(37);

     //   HashMap map = new HashMap();
     //   map.put("foo", per);

        /**
         * Integration starts
         */
        
        Object o = MVEL.eval("name.toUpperCase()", per);
      //  Object o = MVEL.executeExpression(s, per);


        System.out.println(o);
    }
}
