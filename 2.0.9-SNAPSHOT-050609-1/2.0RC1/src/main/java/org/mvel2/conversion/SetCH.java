package org.mvel2.conversion;

import org.mvel2.ConversionHandler;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class SetCH implements ConversionHandler {
    public Object convertFrom(Object in) {
        Class type = in.getClass();
        Set newSet = new LinkedHashSet();
        if (type.isArray()) {
            newSet.addAll(Arrays.asList(((Object[]) in)));
        }
        else if (type.isAssignableFrom(Collection.class)) {
            newSet.addAll((Collection) in);
        }
        else if (type.isAssignableFrom(Iterable.class)) {
            for (Object o : (Iterable) in) {
                newSet.add(o);
            }
        }

        return newSet;
    }

    public boolean canConvertFrom(Class cls) {
        return cls.isArray() || Collection.class.isAssignableFrom(cls) || Iterable.class.isAssignableFrom(cls);
    }
}
