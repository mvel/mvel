package org.mvel.tests.main;

import org.mvel.MVEL;
import org.mvel.asm.Type;
import org.mvel.tests.main.res.Foo;

import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;


public class Test {
    public static void main(String[] args) {
        System.out.println(Type.getDescriptor(int[].class));
    }
}
