package org.mvel.conversion;

import org.mvel.ConversionHandler;

import java.util.HashMap;
import java.util.Map;

public class StringArrayCH implements ConversionHandler {
    private static final Map<Class, Converter> CNV =
            new HashMap<Class, Converter>();


    public Object convertFrom(Object in) {

        if (in.getClass().isArray()) {

            Object[] old = (Object[]) in;
            String[] n = new String[old.length];
            for (int i = 0; i < old.length; i++) {
                n[i] = String.valueOf(old[i]);
            }

            return n;
        }
        else {
            return new String[]{String.valueOf(in)};
        }

        //    return CNV.get(in.getClass()).convert(in);
    }


    public boolean canConvertFrom(Class cls) {
        return CNV.containsKey(cls);
    }

    static {
        CNV.put(Object[].class,
                new Converter() {
                    public Object convert(Object o) {
                        Object[] old = (Object[]) o;
                        String[] n = new String[old.length];
                        for (int i = 0; i < old.length; i++) {
                            n[i] = String.valueOf(old[i]);
                        }

                        return n;
                    }
                }
        );

    }
}
