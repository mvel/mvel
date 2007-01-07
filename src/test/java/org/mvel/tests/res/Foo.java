package org.mvel.tests.res;

public class Foo {
    private Bar bar =  new Bar();

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
        return s.toUpperCase();
    }

}
