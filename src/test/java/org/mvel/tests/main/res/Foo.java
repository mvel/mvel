package org.mvel.tests.main.res;

import java.util.Collection;

public class Foo {
    private Bar bar = new Bar();
    public String register;

    public String aValue = "";
    public String bValue = "";

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


    public Collection getCollectionTest() {
        return collectionTest;
    }

    public void setCollectionTest(Collection collectionTest) {
        this.collectionTest = collectionTest;
    }
}
