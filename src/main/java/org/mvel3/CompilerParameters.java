package org.mvel3;

import org.mvel3.transpiler.context.Declaration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record CompilerParameters<T, K, R>(ContextType contextType,
                                          ClassLoader classLoader,
                                          ClassManager classManager,
                                          Set<String> imports,
                                          Set<String> staticImports,
                                          Type<R> outType,
                                          Declaration<T> contextDeclaration,
                                          List<Declaration>variableDeclarations,
                                          Declaration<K> withDeclaration,
                                          ContentType contentType,
                                          String expression,
                                          String generatedClassName,
                                          String generatedMethodName,
                                          String generatedSuperName) {

    public Map<String, Declaration> allVars() {
        if (variableDeclarations.isEmpty()) {
            return Collections.emptyMap();
        }

        return variableDeclarations.stream().collect(Collectors.toMap(Declaration::name, Function.identity()));
    }

    public int indexOf(String name) {
        for (int i = 0; i < variableDeclarations.size(); i++) {
            if (variableDeclarations.get(i).name().equals(name)) {
                return i;
            }
        }

        return -1;
    }
}
