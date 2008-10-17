package org.mvel2.conversion;

import org.mvel2.ConversionHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ListCH implements ConversionHandler {
    public Object convertFrom(Object in) {
        Class type = in.getClass();
        List newList = new ArrayList();
        if (type.isArray()) {
            newList.addAll(Arrays.asList(((Object[]) in)));
        }
        else if (type.isAssignableFrom(Collection.class)) {
            newList.addAll((Collection) in);
        }
        else if (type.isAssignableFrom(Iterable.class)) {
            for (Object o : (Iterable) in) {
                newList.add(o);
            }
        }

        return newList;
    }

    public boolean canConvertFrom(Class cls) {
        return cls.isArray() || Collection.class.isAssignableFrom(cls) || Iterable.class.isAssignableFrom(cls);
    }
}
