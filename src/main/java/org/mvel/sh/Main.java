package org.mvel.sh;

import org.mvel.MVEL;

public class Main {
    public static void main(String[] args) {
        showSplash();
        ShellSession session = new ShellSession();
        session.run();
    }

    private static void showSplash() {
        System.out.println("\nMVEL-Shell (MVELSH)");
        System.out.println("Copyright (C) 2008, Christopher Brock, The Codehaus");
        System.out.println("Version " + MVEL.VERSION + "." + MVEL.VERSION_SUB + "  -- Written by: Christopher Brock\n");
    }
}
