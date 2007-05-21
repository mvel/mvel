package org.mvel.conversion;

import org.mvel.ConversionException;
import org.mvel.ConversionHandler;

import static java.lang.String.valueOf;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class CharCH implements ConversionHandler {
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
                        if ((((String) o).length()) > 1)
                            throw new ConversionException("cannot convert a string with a length greater than 1 to java.lang.Character");

                        return (((String) o)).charAt(0);
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

        CNV.put(Character.class,
                new Converter() {
                    public Object convert(Object o) {
                        //noinspection UnnecessaryBoxing
                        return new Character(((Character) o));
                    }
                }
        );

        CNV.put(BigDecimal.class,
                new Converter() {
                    public Object convert(Object o) {
                        return (char) ((BigDecimal) o).intValue();
                    }
                }
        );

        CNV.put(Integer.class,
                new Converter() {

                    public Object convert(Object o) {
                        return (char) ((Integer) o).intValue();
                    }
                }
        );
    }
}
