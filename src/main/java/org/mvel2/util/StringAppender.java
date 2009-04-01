/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.util;

import static java.lang.System.arraycopy;

import java.io.UnsupportedEncodingException;
import java.io.PrintWriter;
import java.io.Writer;

public class StringAppender implements CharSequence {
    private static final int DEFAULT_SIZE = 15;

    private char[] str;
    private int capacity;
    private int size = 0;
    private byte[] btr;
    private String encoding;

    public StringAppender() {
        str = new char[capacity = DEFAULT_SIZE];
    }

    public StringAppender(int capacity) {
        str = new char[this.capacity = capacity];
    }

    public StringAppender(int capacity, String encoding) {
        str = new char[this.capacity = capacity];
        this.encoding = encoding;
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

    public StringAppender append(char[] chars) {
        if (chars.length > (capacity - size)) grow(chars.length);
        for (int i = 0; i < chars.length; size++) {
            str[size] = chars[i++];
        }
        return this;
    }

    public StringAppender append(byte[] chars) {
        if (chars.length > (capacity - size)) grow(chars.length);
        for (int i = 0; i < chars.length; size++) {
            str[size] = (char) chars[i++];
        }
        return this;
    }

    public StringAppender append(char[] chars, int start, int length) {
        if (length > (capacity - size)) grow(length);
        int x = start + length;
        for (int i = start; i < x; i++) {
            str[size++] = chars[i];
        }
        return this;
    }

    public StringAppender append(byte[] chars, int start, int length) {
        if (length > (capacity - size)) grow(length);
        int x = start + length;
        for (int i = start; i < x; i++) {
            str[size++] = (char) chars[i];
        }
        return this;
    }

    public StringAppender append(Object o) {
        return append(String.valueOf(o));
    }

    public StringAppender append(CharSequence s) {
        if (s.length() > (capacity - size)) grow(s.length());
        for (int i = 0; i < s.length(); size++) {
            str[size] = s.charAt(i++);
        }
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

    public StringAppender append(byte b) {
        if (btr == null)
            btr = new byte[capacity = DEFAULT_SIZE];
        if (size >= capacity)
            growByte(1);
        btr[size++] = b;
        return this;
    }


    public int length() {
        return size;
    }

    private void grow(int s) {
        if (capacity == 0) capacity = DEFAULT_SIZE;
        final char[] newArray = new char[capacity += s * 2];
        arraycopy(str, 0, newArray, 0, size);
        str = newArray;
    }

    private void growByte(int s) {
        final byte[] newByteArray = new byte[capacity += s];
        arraycopy(btr, 0, newByteArray, 0, size);
        btr = newByteArray;
    }

    public char[] getChars(int start, int count) {
        char[] chars = new char[count];
        arraycopy(str, start, chars, 0, count);
        return chars;
    }

    public char[] toChars() {
        if (btr != null) {
            if (encoding == null)
                encoding = System.getProperty("file.encoding");
            String s;
            try {
                s = new String(btr, encoding);
            }
            catch (UnsupportedEncodingException e) {
                s = new String(btr);
            }
            return s.toCharArray();
        }
        char[] chars = new char[size];
        arraycopy(str, 0, chars, 0, size);
        return chars;
    }

    public String toString() {
        if (btr != null) {
            if (encoding == null)
                encoding = System.getProperty("file.encoding");
            String s;
            try {
                s = new String(btr, encoding);
            }
            catch (UnsupportedEncodingException e) {
                s = new String(btr);
            }
            return s;
        }
        if (size == capacity) return new String(str);
        else return new String(str, 0, size);
    }

    public void getChars(int start, int count, char[] target, int offset) {
        int delta = offset;
        for (int i = start; i < count; i++) {
            target[delta++] = str[i];
        }
    }

    public void reset() {
        size = 0;
    }

    public char charAt(int index) {
        return str[index];
    }

    public CharSequence subSequence(int start, int end) {
        return new String(str, start, (end - start));
    }


}

