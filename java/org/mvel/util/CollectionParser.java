package org.mvel.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Christopher Brock
 */
public class CollectionParser {
    private char[] property;

    private int cursor;
    private int length;
    private int start;
    private int end;

    private int type;

    public static final int LIST = 0;
    public static final int ARRAY = 1;
    public static final int MAP = 2;


    public CollectionParser() {
    }

    public CollectionParser(int type) {
        this.type = type;
    }

    public Object parseCollection(char[] property) {
        this.property = property;
        this.cursor = 0;
        this.length = property.length;

        while (Character.isWhitespace(property[length - 1]))
            length--;

        return parseCollection();
    }


    private Object parseCollection() {
        Map<Object, Object> map = null;
        List<Object> list = null;

        if (type != -1) {
            switch (type) {
                case ARRAY:
                case LIST:
                    list = new ArrayList<Object>();
                    break;
                case MAP:
                    map = new HashMap<Object, Object>();
                    break;
            }
        }

        Object curr = null;
        Object val;

        int newType = -1;

        int end;
        for (; cursor < length; cursor++) {
            switch (property[cursor]) {
                case'{':
                    if (newType == -1) {
                        newType = ARRAY;
                    }
                    if (type == -1) {
                        type = ARRAY;
                        list = new ArrayList<Object>();
                    }
                case'[':
                    if (type == -1) {
                        type = LIST;
                        list = new ArrayList<Object>();
                    }
                    if (newType == -1) {
                        newType = LIST;
                    }

                    start = cursor;
                    end = balancedCapture(property[cursor]);

                    Object o = new CollectionParser(newType).parseCollection(subset(property, start + 1, end));

                    if (type == MAP) {
                        if (curr == null) {
                            curr = o;
                        }
                        else {
                            val = o;
                            map.put(curr, val);
                        }
                    }
                    else {
                        list.add(curr = o);
                    }

                    start = ++cursor;


                    if (start < (length - 1) && property[start] == ',') {
                        start = ++cursor;
                    }

                    continue;

                case'\"':
                case'\'':
                    end = balancedCapture(property[start = cursor]);
                    if (end == -1)
                        throw new RuntimeException("unterminated string literal");

                    break;

                case',':
                    if (type != MAP) {
                        curr = new String(subset(property, start, cursor));
                        list.add(curr);
                    }
                    else {
                        val = new String(subset(property, start, cursor)).trim();
                        map.put(curr, val);
                    }

                    start = cursor + 1;
                    break;

                case':':
                    if (type != MAP) {
                        map = new HashMap<Object, Object>();
                        type = MAP;
                    }
                    curr = new String(subset(property, start, cursor)).trim();

                    start = cursor + 1;
                    break;
            }
        }


        if (start < length) {
            if (cursor < (length - 1)) cursor++;

            if (type == MAP) {
                val = new String(subset(property, start, cursor)).trim();
                map.put(curr, val);
            }
            else {
                if (cursor < length) cursor++;
                curr = new String(subset(property, start, cursor)).trim();
                list.add(curr);
            }
        }

        switch (type) {
            case MAP:
                return map;
            case ARRAY:
                return list.toArray();
            default:
                return list;
        }
    }


    private static char[] subset(char[] property, int start, int end) {
        while (start < (end - 1) && Character.isWhitespace(property[start]))
            start++;

        char[] newA = new char[end - start];
        System.arraycopy(property, start, newA, 0, end - start);
        return newA;
    }

    private int balancedCapture(char type) {
        int depth = 1;
        char term = type;
        switch (type) {
            case'[':
                term = ']';
                break;
            case'{':
                term = '}';
                break;
        }

        if (type == term) {
            for (cursor++; cursor < length; cursor++) {
                if (property[cursor] == type) {
                    return end = cursor;
                }
            }
        }
        else {
            for (cursor++; cursor < length; cursor++) {
                if (property[cursor] == type) {
                    depth++;
                }
                else if (property[cursor] == term && --depth == 0) {
                    return end = cursor;
                }
            }
        }

        return -1;
    }


    public int getEnd() {
        return end;
    }

    public static void main(String[] args) {
        Object o = new CollectionParser().parseCollection("[a,b,c]".toCharArray());
        System.out.println(o);

        o = new CollectionParser().parseCollection("[{a,b,c}, [foo:bar], abc]".toCharArray());

        System.out.println(o);
    }
}
