package org.mvel.sh;

public class Main {
    public static final int VERSION = 1;
    public static final int VERSION_SUB = 0;



    public static void main(String[] args) {
        showSplash();
        ShellSession session = new ShellSession();
        session.run();
    }


    private static void showSplash() {
        System.out.println("\nMVEL-Shell (MVELSH)");
        System.out.println("Copyright (C) 2007, Christopher Brock, The Codehaus");
        System.out.println("Version " + VERSION + "." + VERSION_SUB + "  -- Written by: Christopher Brock\n");
    }
}
