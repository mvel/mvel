package org.mvel.conversion;

import org.mvel.ConversionHandler;
import org.mvel.Unit;


public class UnitConversion implements ConversionHandler {
    public Object convertFrom(Object in) {
        try {
            return Unit.class.newInstance().convertFrom(in);
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean canConvertFrom(Class cls) {
        if (Unit.class.isAssignableFrom(cls) || Number.class.isAssignableFrom(cls)) {
            try {
                return Unit.class.newInstance().canConvertFrom(cls);
            }
            catch (InstantiationException e) {
                e.printStackTrace();
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
