/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
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
 *
 */
package org.mvel.conversion;

import org.mvel.ConversionException;
import org.mvel.ConversionHandler;
import static org.mvel.DataConversion.canConvert;
import static org.mvel.DataConversion.convert;
import static org.mvel.util.PropertyTools.getBaseComponentType;

import static java.lang.reflect.Array.newInstance;
import static java.lang.reflect.Array.set;
import java.util.HashMap;
import java.util.Map;

public class PrimArrayHandler implements ConversionHandler {
    private final Map<Class, Converter> CNV = new HashMap<Class, Converter>();

    private final Class primitiveType;

    public PrimArrayHandler(Class type) {
        this.primitiveType = type;

        CNV.put(Object[].class, new Converter() {
            public Object convert(Object o) {
                return handleLooseTypeConversion(o.getClass(), (Object[]) o, primitiveType);
            }
        });

        CNV.put(String[].class, new Converter() {
            public Object convert(Object o) {
                return handleLooseTypeConversion(o.getClass(), (String[]) o, primitiveType);
            }
        });
    }

    public Object convertFrom(Object in) {
        if (!CNV.containsKey(in.getClass())) throw new ConversionException("cannot convert type: "
                + in.getClass().getName() + " to: " + primitiveType.getName());

        return CNV.get(in.getClass()).convert(in);
    }

    public boolean canConvertFrom(Class cls) {
        return CNV.containsKey(cls);
    }


    /**
     * Messy method to handle primitive boxing for conversion. If someone can re-write this more
     * elegantly, be my guest.
     *
     * @param sourceType
     * @param input
     * @param targetType
     * @return
     */
    private static Object handleLooseTypeConversion(Class sourceType, Object[] input, Class targetType) {
        Class targType = getBaseComponentType(targetType);

        Object target = newInstance(targType, input.length);

        if (input.length > 0
                && canConvert(targetType.getComponentType(), getBaseComponentType(sourceType))) {
            for (int i = 0; i < input.length; i++) {
                set(target, i, convert(input[i], targType));
            }
        }
        else {
            throw new ConversionException("cannot convert to type: "
                    + targetType.getComponentType().getName() + "[] from " + getBaseComponentType(sourceType).getName());
        }

        return target;
    }
}
