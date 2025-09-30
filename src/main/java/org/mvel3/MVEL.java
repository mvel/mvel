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

import org.mvel3.MVELBuilder.ContentBuilder;
import org.mvel3.MVELBuilder.TypesBuilderCollector;
import org.mvel3.transpiler.context.Declaration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mvel3.MVELBuilder.WITH_NAME;

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


    public <T, K, R> MVELBuilder<T, K, R> content(String content) {
        MVELBuilder<T, K, R> builder = new MVELBuilder<>();
        return builder;
    }

    public static <V> MVELBuilder.WithBuilder<Map<String, V>> map() {
        return new MVELBuilder.WithBuilder<>(ContextType.MAP, Declaration.of(MVELBuilder.CONTEXT_NAME, Type.MAP), null);
    }

    public static <V> MVELBuilder.WithBuilder<Map<String, V>> map(TypesBuilderCollector types) {
        return map(types.toArray());
    }

    public static <V> MVELBuilder.WithBuilder<Map<String, V>> map(Declaration<?>... types) {
        return new MVELBuilder.WithBuilder<>(ContextType.MAP, Declaration.of(MVELBuilder.CONTEXT_NAME, Type.MAP), types);
    }

    public static <V> MVELBuilder.WithBuilder<List<V>> list(TypesBuilderCollector types) {
        return list(types.toArray());
    }

    public static <V> MVELBuilder.WithBuilder<List<V>> list(Declaration<?>... types) {
        return new MVELBuilder.WithBuilder<>(ContextType.LIST, Declaration.of(MVELBuilder.CONTEXT_NAME, Type.LIST), types);
    }

    public static <C> MVELBuilder.WithBuilder<C> pojo(Class cls) {
        return new MVELBuilder.WithBuilder<>(ContextType.POJO, Declaration.of(MVELBuilder.CONTEXT_NAME, Type.type(cls)), null);
    }

    public static <C> MVELBuilder.WithBuilder<C> pojo(Type type) {
        return new MVELBuilder.WithBuilder<>(ContextType.POJO, Declaration.of(MVELBuilder.CONTEXT_NAME, type), null);
    }

    public static <C> MVELBuilder.WithBuilder<C> pojo(Class cls, TypesBuilderCollector types) {
        return new MVELBuilder.WithBuilder<>(ContextType.POJO, Declaration.of(MVELBuilder.CONTEXT_NAME, Type.type(cls)), types.toArray());
    }

    public static <V> MVELBuilder.WithBuilder<V> pojo(Class cls, Declaration<?> type, Declaration<?>... types) {
        Declaration<?>[] merged = new Declaration[types.length+1];
        merged[0] = type;
        System.arraycopy(types, 0, merged, 1, types.length);
        return new MVELBuilder.WithBuilder<>(ContextType.POJO, Declaration.of(MVELBuilder.CONTEXT_NAME, Type.type(cls)), merged);
    }

    public static <W> MVELBuilder.OutBuilder<Void, W> with(Class cls) {
        return new MVELBuilder.OutBuilder<>(ContextType.NONE, Declaration.of(MVELBuilder.CONTEXT_NAME, Type.VOID));
    }

    public <C, W, O> Evaluator<C, W, O> compile(CompilerParameters<C, W, O> evalInfo) {
        MVELCompiler compiler = new MVELCompiler();
        Evaluator<C, W, O> eval = compiler.compile(evalInfo);
        return eval;
    }

    public  <V, R> Evaluator<Map<String, V>, Void, R> compileMapBlock(final String content, final Class<R> outClass, final Set<String> imports, final Map<String, Type<?>> types) {
        return compileMapBlock(content, outClass, imports, types, new ClassManager(), ClassLoader.getSystemClassLoader());
    }

    public <V, R> Evaluator<Map<String, V>, Void, R> compileMapBlock(final String content, final Class<R> outClass, final Set<String> imports, final Map<String, Type<?>> types,
                                                                          ClassManager clsManager, ClassLoader classLoader) {

        Evaluator<Map<String, V>, Void, R> evaluator = MVEL.<V>map(Declaration.from(types))
                .<R>out(outClass)
                .block(content)
                .imports(imports)
                .classManager(clsManager)
                .classLoader(classLoader)
                .compile();

        return  evaluator;
    }

    public  <V, R> Evaluator<Map<String, V>, Void, R> compileMapExpression(final String content, final Class<R> outClass, final Set<String> imports, final Map<String, Type<?>> types) {
        return compileMapExpression(content, outClass, imports, types, new ClassManager(), ClassLoader.getSystemClassLoader());
    }

    public <V, R> Evaluator<Map<String, V>, Void, R> compileMapExpression(final String content, final Class<R> outClass, final Set<String> imports, final Map<String, Type<?>> types,
                                                                          ClassManager clsManager, ClassLoader classLoader) {

        Evaluator<Map<String, V>, Void, R> evaluator = MVEL.<V>map(Declaration.from(types))
                                                           .<R>out(outClass)
                                                           .expression(content)
                                                           .imports(imports)
                                                           .classManager(clsManager)
                                                           .classLoader(classLoader)
                                                           .compile();

        return  evaluator;
    }

    public <V, R> Evaluator<List<V>, Void, R> compileListBlock(final String content, Class<R> outClass, final Set<String> imports, final Declaration<V>[] types) {
        return compileListBlock(content, outClass, imports, types, new ClassManager(), ClassLoader.getSystemClassLoader());
    }

    public <V, R> Evaluator<List<V>, Void, R> compileListBlock(final String content, Class outClass, final Set<String> imports, final Declaration<V>[] types,
                                                               ClassManager clsManager, ClassLoader classLoader) {

        Evaluator<List<V>, Void, R> evaluator = MVEL.<V>list(types)
                                                     .<R>out(outClass)
                                                     .block(content)
                                                     .imports(imports)
                                                     .classManager(clsManager)
                                                     .classLoader(classLoader)
                                                     .compile();

        return  evaluator;
    }


    public <V, R> Evaluator<List<V>, Void, R>  compileListExpression(final String content, Class<R> outClass, final Set<String> imports, final Declaration<V>[] types) {
        return compileListExpression(content, outClass, imports, types, new ClassManager(), ClassLoader.getSystemClassLoader());
    }

    public <V, R> Evaluator<List<V>, Void, R> compileListExpression(final String content, Class outClass, final Set<String> imports, final Declaration<V>[] types,
                                                               ClassManager clsManager, ClassLoader classLoader) {
        Evaluator<List<V>, Void, R> evaluator = MVEL.<V>list(types)
                                                    .<R>out(outClass)
                                                    .expression(content)
                                                    .imports(imports)
                                                    .classManager(clsManager)
                                                    .classLoader(classLoader)
                                                    .compile();

        return  evaluator;
    }

    public <T, K, R> Evaluator<T, K, R> compilePojoEvaluator(CompilerParameters<T, K, R> info) {
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
                                   Type outType,
                                   final Map<String, Object> vars,
                                   ClassManager clsManager, ClassLoader classLoader) {
        for (Map.Entry<String, Object> o : vars.entrySet()) {
            if ( types.keySet().contains(o.getKey())) {
                Class type = o.getValue() == null ? Object.class : o.getValue().getClass();
                types.put(o.getKey(), Type.type(type));
            }
        }

        ContentBuilder<Map<String, Object>, Void, R> contentBuilder = MVEL.map(Declaration.from(types)).<R>out(outType);

        MVELBuilder<Map<String, Object>, Void, R> mvelBuilder;
        if (expr.indexOf(';') > 0) {
            mvelBuilder = contentBuilder.block(expr);
        } else {
            mvelBuilder = contentBuilder.expression(expr);
        }

        Evaluator<Map<String, Object>, Void, R> evaluator = mvelBuilder.imports(imports)
                   .classManager(clsManager)
                   .classLoader(classLoader)
                   .compile();

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
