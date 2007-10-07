package org.mvel.tests.main.res;

/**
 * Created by IntelliJ IDEA.
 * User: christopherbrock
 * Date: Sep 28, 2007
 * Time: 11:53:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class PojoStatic {
    private String value;

    public PojoStatic(String value) {
        this.value = value;
    }

    public PojoStatic() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String string) {
        this.value = string;
    }
}
