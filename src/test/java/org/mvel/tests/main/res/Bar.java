package org.mvel.tests.main.res;

import java.util.List;
import java.util.ArrayList;

public class Bar {
    private String name = "dog";
    private boolean woof = true;
    private int age = 14;
    private String assignTest = "";
    private List testList = new ArrayList();


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isWoof() {
        return woof;
    }

    public void setWoof(boolean woof) {
        this.woof = woof;
    }


    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isFoo(Object obj) {
        return obj instanceof Foo;
    }


    public String getAssignTest() {
        return assignTest;
    }

    public void setAssignTest(String assignTest) {
        this.assignTest = assignTest;
    }


    public List getTestList() {
        return testList;
    }

    public void setTestList(List testList) {
        this.testList = testList;
    }

    public String happy() {
        return "happyBar";
    }

    public static int staticMethod() {
        return 1;
    }
}
