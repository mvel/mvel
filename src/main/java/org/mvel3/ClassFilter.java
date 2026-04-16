/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates.
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
 */

package org.mvel3;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Optional allow/deny policy for class references in an MVEL expression.
 * Configured via {@link MVELBuilder#classFilter(ClassFilter)}; when not
 * configured, every class is accepted (default behavior).
 *
 * <p><b>Defense-in-depth, not a security boundary.</b> Expression authors are
 * trusted (see {@code SECURITY.md}). This filter operates on the generated
 * Java source and cannot stop reflection-based lookups
 * (e.g. {@code Class.forName}). It reduces the blast radius of mistakes, not
 * adversarial input.
 *
 * <p>Filters compose via {@link #and}, {@link #or}, {@link #negate}. Typical
 * use: combine an {@link #allowlist} with a {@link #blocklistPackage}, or
 * start from {@link #SAFE_PRESET} and add further restrictions.
 */
@FunctionalInterface
public interface ClassFilter {

    /** Returns {@code true} if {@code clazz} is permitted, {@code false} to reject. */
    boolean accept(Class<?> clazz);

    default ClassFilter and(ClassFilter other) {
        return clazz -> this.accept(clazz) && other.accept(clazz);
    }

    default ClassFilter or(ClassFilter other) {
        return clazz -> this.accept(clazz) || other.accept(clazz);
    }

    default ClassFilter negate() {
        return clazz -> !this.accept(clazz);
    }

    /**
     * Accepts only the listed classes (exact match). Subclasses are NOT
     * implicitly allowed — use {@link #allowlistWithSubtypes} if you need that.
     */
    static ClassFilter allowlist(Class<?>... allowed) {
        Set<Class<?>> set = new HashSet<>(Arrays.asList(allowed));
        return set::contains;
    }

    /**
     * Accepts any class that equals or is a subtype of one of the listed base classes.
     */
    static ClassFilter allowlistWithSubtypes(Class<?>... bases) {
        Set<Class<?>> set = new HashSet<>(Arrays.asList(bases));
        return clazz -> {
            for (Class<?> b : set) {
                if (b.isAssignableFrom(clazz)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Rejects the listed classes (exact match). Subclasses are NOT
     * implicitly rejected — validation runs against a method's
     * {@code declaringType()}, so a blocklist that included a broad supertype
     * like {@code Object} or {@code Throwable} would reject harmless inherited
     * calls. Use {@link #blocklistWithSubtypes} only when you genuinely want
     * to reject every subtype (e.g. for {@code ClassLoader}).
     */
    static ClassFilter blocklist(Class<?>... blocked) {
        Set<Class<?>> set = new HashSet<>(Arrays.asList(blocked));
        return clazz -> !set.contains(clazz);
    }

    /**
     * Rejects the listed classes and any subtype of them. Use for classes
     * whose entire hierarchy is dangerous (e.g. {@link ClassLoader}).
     */
    static ClassFilter blocklistWithSubtypes(Class<?>... bases) {
        Set<Class<?>> set = new HashSet<>(Arrays.asList(bases));
        return clazz -> {
            for (Class<?> b : set) {
                if (b.isAssignableFrom(clazz)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Rejects any class whose fully-qualified name starts with one of the given
     * prefixes. Useful for package-shaped threat surfaces like
     * {@code java.lang.reflect.}, {@code sun.}, {@code jdk.internal.}
     * (include the trailing dot to avoid prefix collisions).
     */
    static ClassFilter blocklistPackage(String... prefixes) {
        String[] copy = prefixes.clone();
        return clazz -> {
            String name = clazz.getName();
            for (String p : copy) {
                if (name.startsWith(p)) {
                    return false;
                }
            }
            return true;
        };
    }

    /** Accepts every class. Useful as an explicit default. */
    ClassFilter ACCEPT_ALL = clazz -> true;

    /**
     * A built-in starting point that blocks commonly dangerous classes:
     * process/thread/classloader hierarchies, {@link System}, {@link Class}
     * itself, filesystem APIs, and the {@code java.lang.reflect},
     * {@code java.lang.invoke}, {@code javax.script}, {@code sun.},
     * {@code jdk.internal.} packages.
     *
     * <p>This is a reasonable baseline, not an exhaustive sandbox. Extend it
     * based on your application's threat model.
     */
    ClassFilter SAFE_PRESET =
            blocklistWithSubtypes(
                    Runtime.class,
                    Process.class,
                    ProcessBuilder.class,
                    ProcessHandle.class,
                    ClassLoader.class,
                    Thread.class)
            .and(blocklist(
                    System.class,
                    Class.class,
                    java.io.File.class,
                    java.nio.file.Files.class,
                    java.nio.file.Paths.class))
            .and(blocklistPackage(
                    "java.lang.reflect.",
                    "java.lang.invoke.",
                    "javax.script.",
                    "sun.",
                    "jdk.internal."));
}
