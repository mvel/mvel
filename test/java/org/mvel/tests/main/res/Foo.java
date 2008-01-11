package org.mvel.tests.main.res;

import java.util.Collection;

public class Foo {
    private Bar bar = new Bar();
    public String register;

    public String aValue = "";
    public String bValue = "";

    private String name = "dog";

    private int countTest = 0;

    private Collection collectionTest;

    public void abc() {
    }

    public Bar getBar() {
        return bar;
    }

    public void setBar(Bar bar) {
        this.bar = bar;
    }

    public String happy() {
        return "happyBar";
    }

    public String toUC(String s) {
        register = s;
        return s.toUpperCase();
    }

    public int getNumber() {
        return 4;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection getCollectionTest() {
        return collectionTest;
    }

    public void setCollectionTest(Collection collectionTest) {
        this.collectionTest = collectionTest;
    }


    public int getCountTest() {
        return countTest;
    }

    public void setCountTest(int countTest) {
        this.countTest = countTest;
    }

    public boolean equals(Object o) {
        return o instanceof Foo;
    }
}
