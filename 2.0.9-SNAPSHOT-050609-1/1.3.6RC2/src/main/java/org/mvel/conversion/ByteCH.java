package org.mvel.conversion;

import org.mvel.ConversionException;
import org.mvel.ConversionHandler;

import static java.lang.String.valueOf;
import java.util.HashMap;
import java.util.Map;

public class ByteCH implements ConversionHandler {
    private static final Map<Class, Converter> CNV =
            new HashMap<Class, Converter>();


    public Object convertFrom(Object in) {
        if (!CNV.containsKey(in.getClass())) throw new ConversionException("cannot convert type: "
                + in.getClass().getName() + " to: " + Integer.class.getName());
        return CNV.get(in.getClass()).convert(in);
    }


    public boolean canConvertFrom(Class cls) {
        return CNV.containsKey(cls);
    }

    static {
        CNV.put(String.class,
                new Converter() {
                    public Object convert(Object o) {
                        return Byte.parseByte(((String) o));
                    }
                }
        );

        CNV.put(Object.class,
                new Converter() {
                    public Object convert(Object o) {
                        return CNV.get(String.class).convert(valueOf(o));
                    }
                }
        );

        CNV.put(Byte.class,
                new Converter() {
                    public Object convert(Object o) {
                        //noinspection UnnecessaryBoxing
                        return new Byte(((Byte) o));
                    }
                }
        );


    }
}
