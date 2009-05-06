package org.mvel.util;


public class StringAppender implements CharSequence {
    private static final int DEFAULT_SIZE = 15;

    private char[] str;
    private int capacity;
    private int size = 0;


    public StringAppender() {
        str = new char[capacity = DEFAULT_SIZE];
    }

    public StringAppender(int capacity) {
        str = new char[this.capacity = capacity];
    }

    public StringAppender(char c) {
        (str = new char[this.capacity = DEFAULT_SIZE])[0] = c;
    }

    public StringAppender(char[] s) {
        capacity = size = (str = s).length;
    }

    public StringAppender(CharSequence s) {
        str = new char[this.capacity = size = s.length()];
        for (int i = 0; i < str.length; i++)
            str[i] = s.charAt(i);
    }

    public StringAppender(String s) {
        capacity = size = (str = s.toCharArray()).length;
    }

    public StringAppender append(Object o) {
        return append(String.valueOf(o));
    }

    public StringAppender append(CharSequence s) {
        if (s.length() > (capacity - size)) grow(s.length());
        for (int i = 0; size < capacity; size++) {
            str[size] = s.charAt(i++);
        }
        size += s.length();
        return this;
    }

    public StringAppender append(String s) {
        if (s == null) return this;

        int len = s.length();
        if (len > (capacity - size)) {
            grow(len);
        }

        s.getChars(0, len, str, size);
        size += len;

        return this;
    }

    public StringAppender append(char c) {
        if (size >= capacity) grow(1);
        str[size++] = c;
        return this;
    }

    public int length() {
        return size;
    }

    private void grow(int s) {
        if (capacity == 0) capacity = DEFAULT_SIZE;
        final char[] newArray = new char[capacity += s * 2];
        System.arraycopy(str, 0, newArray, 0, size);
        str = newArray;
    }

    public String toString() {
        if (size == capacity) return new String(str);
        else return new String(str, 0, size);
    }


    public char charAt(int index) {
        return str[index];
    }

    public CharSequence subSequence(int start, int end) {
        return new String(str, start, (end - start));
    }
}

