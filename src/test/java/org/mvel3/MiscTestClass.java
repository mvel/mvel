package org.mvel3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MiscTestClass {
    int exec = 0;

    @SuppressWarnings({"unchecked", "UnnecessaryBoxing"})
    public List toList(Object object1, String string, int integer, Map map, List list) {
        exec++;
        List l = new ArrayList();
        l.add(object1);
        l.add(string);
        l.add(Integer.valueOf(integer));
        l.add(map);
        l.add(list);
        return l;
    }


    public int getExec() {
        return exec;
    }
}
