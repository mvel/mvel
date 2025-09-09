package org.mvel3;

import org.mvel3.transpiler.context.Declaration;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record CompilerParamters<T, K, R>(ClassLoader classLoader,
                                         ClassManager classManager,
                                         Set<String> imports,
                                         Set<String> staticImports,
                                         Type<R> outType,
                                         ContextInfo<T> variableInfo,
                                         Declaration<K> rootDeclaration,
                                         int rootVarIndex,
                                         String expression,
                                         String generatedClassName,
                                         String generatedMethodName,
                                         String generatedSuperName) {

    public Map<String, Declaration> allVars() {
        if (variableInfo == null) {
            return Collections.emptyMap();
        }

        return Arrays.stream(variableInfo.vars()).collect(Collectors.toMap(Declaration::name, Function.identity()));
    }
}
