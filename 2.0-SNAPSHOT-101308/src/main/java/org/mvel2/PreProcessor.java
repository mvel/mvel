package org.mvel2;

public interface PreProcessor {
    public char[] parse(char[] input);

    public String parse(String input);
}
