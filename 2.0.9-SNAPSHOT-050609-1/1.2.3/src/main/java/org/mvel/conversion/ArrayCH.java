package org.mvel.conversion;

import org.mvel.ConversionException;
import org.mvel.ConversionHandler;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import java.util.HashMap;
import java.util.Map;

public class ArrayCH implements ConversionHandler {
    private static final Map<Class, Converter> CNV =
            new HashMap<Class, Converter>();


    public Object convertFrom(Object in) {
        if (!CNV.containsKey(in.getClass())) throw new ConversionException("cannot convert type: "
                + in.getClass().getName() + " to: " + Boolean.class.getName());
        return CNV.get(in.getClass()).convert(in);
    }

    public boolean canConvertFrom(Class cls) {
        return CNV.containsKey(cls);
    }

    static {
        CNV.put(String[].class,
                new Converter() {
                    public Object convert(Object o) {
                        Object[] old = (Object[]) o;
                        String[] n = new String[old.length];
                        for (int i = 0; i < old.length; i++) {
                            n[i] = valueOf(old[i]);
                        }

                        return n;
                    }
                }
        );

        CNV.put(Integer[].class,
                new Converter() {
                    public Object convert(Object o) {
                        Object[] old = (Object[]) o;
                        Integer[] n = new Integer[old.length];
                        for (int i = 0; i < old.length; i++) {
                            n[i] = parseInt(valueOf(old[i]));
                        }

                        return n;
                    }

                });

        CNV.put(int[].class,
                new Converter() {
                    public Object convert(Object o) {
                        Object[] old = (Object[]) o;
                        int[] n = new int[old.length];
                        for (int i = 0; i < old.length; i++) {
                            n[i] = parseInt(valueOf(old[i]));
                        }

                        return n;
                    }

                });

    }
}
