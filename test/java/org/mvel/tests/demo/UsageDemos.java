package org.mvel.tests.demo;

import junit.framework.TestCase;
import org.mvel.MVEL;

public class UsageDemos extends TestCase {

    public void testDemo1() {
        String test = "Hello";
        Object result = MVEL.eval("toUpperCase()", test);
        System.out.println("result: " + result);
    }

    public void testDemo2() {
        User user = new User();
        user.setName("Bob");
        user.setPassword("Despot");
        user.setAge(30);

        String name = (String) MVEL.eval("name", user);
    }

    public class User {
        private String name;
        private String password;
        private int age;


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

}
