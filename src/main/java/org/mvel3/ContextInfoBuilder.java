package org.mvel3;

import org.mvel3.transpiler.context.Declaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ContextInfoBuilder<T> {
    private Declaration<T> declaration;

    private List<Declaration> vars = Collections.emptyList();

    public static <K> ContextInfoBuilder<K> create(Type<K> type) {
        return create(new Declaration<>(CompilerParamtersBuilder.CONTEXT_NAME, type));
    }

    public static <K> ContextInfoBuilder<K> create(Declaration<K> declaration) {
        ContextInfoBuilder<K> contextInfoBuilder = new ContextInfoBuilder<>();
        contextInfoBuilder.declaration = declaration;
        return contextInfoBuilder;
    }

    public Declaration<T> getDeclaration() {
        return declaration;
    }

    public List<Declaration> getVars() {
        return vars;
    }

    public ContextInfoBuilder<T> setVars(Declaration... vars) {
        this.vars = Arrays.asList(vars);
        return this;
    }

    public ContextInfoBuilder<T> addDeclaration(Declaration declr) {
        if (vars == Collections.<Declaration>emptyList()) {
            vars = new ArrayList<>();
        }
        this.vars.add(declr);
        return this;
    }

    @Override
    public ContextInfoBuilder<T> clone() {
        return null;
    }

    public ContextInfo<T> build() {
        ContextInfo<T> values = new ContextInfo<>(declaration,
                                                  vars.toArray(new Declaration[0]));
        return values;
    }
}
