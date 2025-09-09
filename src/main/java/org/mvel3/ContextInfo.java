package org.mvel3;

import org.mvel3.transpiler.context.Declaration;

import java.util.Arrays;
import java.util.stream.Collectors;

public record ContextInfo<T>(Declaration declaration, Declaration[] vars) {
    public int indexOf(String name) {
        for (int i = 0; i < vars.length; i++) {
            if (vars[i].name().equals(name)) {
                return i;
            }
        }

        return -1;
    }

    public String[] varNames() {
        return Arrays.stream(vars).map(d -> d.name()).collect(Collectors.toList()).toArray(new String[0]);
    }
}
