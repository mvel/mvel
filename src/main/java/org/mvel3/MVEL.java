/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Borrowed from MVEL, under the ASL2.0 license.
 *  
 */

package org.mvel3;

import org.mvel3.transpiler.context.Declaration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MVEL {


    public static MVEL EXPRESSION;

    public static MVEL BLOCK;

    private static MVEL instance;

    public static MVEL get() {
        if (instance == null) {
            instance = new MVEL();
        }

        return instance;
    }

    public MVEL() {
    }

    public <T, K, R> Evaluator<T, K, R> compile(CompilerParamters<T, K, R> evalInfo) {
        MVELCompiler compiler = new MVELCompiler();
        Evaluator<T, K, R> eval = compiler.compile(evalInfo);
        return eval;
    }

    public <T extends Map, Void, R> Evaluator<T, Void, R> compileMapEvaluator(final String content, final Class<R> outClass, final Set<String> imports, final Map<String, Type<?>> types) {
        return compileMapEvaluator(content, outClass, imports, types, new ClassManager(), ClassLoader.getSystemClassLoader());
    }

    public <T extends Map, Void, R> Evaluator<T, Void, R> compileMapEvaluator(final String content, final Class<R> outClass, final Set<String> imports, final Map<String, Type<?>> types,
                                                                              ClassManager clsManager, ClassLoader classLoader) {
        MVELCompiler                         compiler = new MVELCompiler();
        CompilerParamtersBuilder<T, Void, R> eval     = new CompilerParamtersBuilder<>();
        eval.setClassManager(clsManager).setClassLoader(classLoader)
            .setExpression(content)
            .setImports(imports)
            .setVariableInfo(ContextInfoBuilder.create(Type.type(Map.class, "<String, Object>"))
                                               .setVars(Declaration.from(types)))
            .setOutType(Type.type(outClass));

        Evaluator<T, Void, R> evaluator = compiler.compile(eval.build());

        return  evaluator;
    }

    public <T extends List, Void, R> Evaluator<T, Void, R> compileListEvaluator(final String content, Class<R> outClass, final Set<String> imports, final Declaration[] types) {
        return compileListEvaluator(content, outClass, imports, types, new ClassManager(), ClassLoader.getSystemClassLoader());
    }

    public <T extends List, Void, R> Evaluator<T, Void, R> compileListEvaluator(final String content, Class<R> outClass, final Set<String> imports, final Declaration[] types,
                                                                                ClassManager clsManager, ClassLoader classLoader) {
        MVELCompiler                         compiler = new MVELCompiler();
        CompilerParamtersBuilder<T, Void, R> eval     = new CompilerParamtersBuilder<>();
        eval.setClassManager(clsManager).setClassLoader(ClassLoader.getSystemClassLoader())
            .setExpression(content)
            .setImports(imports)
            .setVariableInfo(ContextInfoBuilder.create(Type.type(List.class, "<Object>"))
                                               .setVars(types))
            .setOutType(Type.type(outClass));

        Evaluator<T, Void, R> evaluator = compiler.compile(eval.build());

        return  evaluator;
    }

    public <T, K, R> Evaluator<T, K, R> compilePojoEvaluator(CompilerParamters<T, K, R> info) {
        MVELCompiler compiler = new MVELCompiler();
        Evaluator<T, K, R> evaluator = compiler.compile(info);

        return  evaluator;
    }

    public Object executeExpression(final String content) {
        return executeExpression(content, new HashSet<>(), new HashMap<>(), Object.class);
    }

    public Object executeExpression(final String expression, Set<String> imports, final Map<String, Object> vars) {
        return executeExpression(expression, imports, vars, Object.class);
    }

    public <R> R executeExpression(final String expr, Set<String> imports, final Map<String, Object> vars,  Class<R> outClass) {
        return executeExpression(expr, imports, getTypeMap(vars), outClass != null ? Type.type(outClass) : null, vars);
    }

    public <R> R executeExpression(final String expr, Set<String> imports,
                                   final Map<String, Type<?>> types,
                                   Type<R> outType,
                                   final Map<String, Object> vars) {
        return executeExpression(expr, imports, types, outType, vars, new ClassManager(), ClassLoader.getSystemClassLoader());
    }

    public <R> R executeExpression(final String expr, Set<String> imports,
                                   final Map<String, Type<?>> types,
                                   Type<R> outType,
                                   final Map<String, Object> vars,
                                   ClassManager clsManager, ClassLoader classLoader) {
        for (Map.Entry<String, Object> o : vars.entrySet()) {
            if ( types.keySet().contains(o.getKey())) {
                Class type = o.getValue() == null ? Object.class : o.getValue().getClass();
                types.put(o.getKey(), Type.type(type));
            }
        }

        MVELCompiler                                           MVELCompiler = new MVELCompiler();
        CompilerParamtersBuilder<Map<String, Object>, Void, R> eval         = CompilerParamtersBuilder.create();

        eval.setClassManager(clsManager).setClassLoader(classLoader)
            .setExpression(expr)
            .setImports(imports)
            .setVariableInfo(ContextInfoBuilder.create(Type.type(Map.class, "<String, Object>"))
                                               .setVars(Declaration.from(types)));

        if (outType!=null) {
            eval.setOutType(outType);
        }

        Evaluator<Map<String, Object>, Void, R> evaluator = MVELCompiler.compile(eval.build());

        return evaluator.eval(vars);
    }

    public static Map<String, Type<?>> getTypeMap(Map<String, ?> vars) {
        Map<String, Type<?>> types = new HashMap<>();

        for (Map.Entry<String, ?> o : vars.entrySet()) {
            types.put(o.getKey(), Type.type(o.getValue() != null ? o.getValue().getClass() : null));
        }

        return types;
    }

    public static <T> T putMap(Map map, String key, T v) {
        map.put(key, v);
        return v;
    }

    public static <T> T setList(List list, int index, T v) {
        list.set(index, v);
        return v;
    }
}
