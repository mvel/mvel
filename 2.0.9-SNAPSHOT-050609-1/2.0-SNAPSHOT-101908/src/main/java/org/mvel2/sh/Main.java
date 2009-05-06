package org.mvel2.sh;

import org.mvel2.MVEL;

public class Main {
    public static void main(String[] args) {
        showSplash();
        new ShellSession().run();
    }

    private static void showSplash() {
        System.out.println("\nMVEL-Shell (MVELSH)");
        System.out.println("Copyright (C) 2008, Christopher Brock, The Codehaus");
        System.out.println("Version " + MVEL.VERSION + "." + MVEL.VERSION_SUB + "\n");
    }
}
