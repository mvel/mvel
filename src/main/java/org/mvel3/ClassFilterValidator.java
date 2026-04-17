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

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Post-transpile walker that rejects class references forbidden by a
 * {@link ClassFilter}. Runs on the fully-transpiled {@link CompilationUnit}
 * (after {@link org.mvel3.transpiler.MVELTranspiler} has produced the Java
 * source that javac will compile), so it sees every class reference in one
 * place regardless of which rewriter resolved it.
 *
 * <p>Walks: imports, type nodes ({@link ClassOrInterfaceType}),
 * {@link ObjectCreationExpr}, {@link ClassExpr} (`.class` literals),
 * {@link MethodReferenceExpr}, {@link MethodCallExpr} (declaring type),
 * {@link FieldAccessExpr} (declaring type),
 * {@link InstanceOfExpr}, {@link TypeExpr}, {@link AnnotationExpr}.
 *
 * <p>Violations are batched: every blocked reference found in a single
 * compile is reported together via one {@link ClassFilterException}.
 *
 * <p>Resolution: uses JavaParser's symbol solver first; if that fails, falls
 * back to {@link Class#forName} with the configured classloader. If both
 * fail, the node is not a loadable class reference (package path fragment,
 * typo, etc.) and is skipped — javac will reject genuinely-invalid user code
 * seconds later, so the filter does not need to double-check.
 */
public final class ClassFilterValidator {

    private ClassFilterValidator() {}

    /**
     * @param unit the transpiled compilation unit about to be handed to javac
     * @param filter the policy to apply; if {@code null} this method is a no-op
     * @param classLoader classloader used for last-chance {@link Class#forName}
     *                    resolution when JavaParser can't resolve a node
     * @throws ClassFilterException if any class reference is rejected
     */
    public static void validate(CompilationUnit unit, ClassFilter filter, ClassLoader classLoader) {
        if (filter == null) {
            return;
        }
        List<ClassFilterException.Violation> violations = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. User-supplied imports (the generator only copies the user's
        //    imports onto the compilation unit, so these are all
        //    user-authored).
        for (ImportDeclaration imp : unit.getImports()) {
            checkImport(imp, filter, classLoader, violations, seen);
        }

        // 2. User code inside method bodies. We deliberately skip the
        //    evaluator class's own header (its `implements Evaluator<Map<...>>`
        //    or `extends` clauses) — that's framework scaffolding, not user
        //    code, and would cause false positives under a strict allowlist.
        List<BlockStmt> userCodeBlocks = new ArrayList<>();
        for (MethodDeclaration method : unit.findAll(MethodDeclaration.class)) {
            method.getBody().ifPresent(userCodeBlocks::add);
        }
        for (BlockStmt body : userCodeBlocks) {
            walkUserCode(body, filter, classLoader, violations, seen);
        }

        if (!violations.isEmpty()) {
            throw new ClassFilterException(violations);
        }
    }

    private static void walkUserCode(Node root, ClassFilter filter, ClassLoader classLoader,
                                      List<ClassFilterException.Violation> violations, Set<String> seen) {
        root.findAll(ClassOrInterfaceType.class).forEach(t -> {
            // A ClassOrInterfaceType whose parent is also a ClassOrInterfaceType
            // is the *scope prefix* of an outer type (e.g. `java.lang` inside
            // `java.lang.Runtime`). It's a package path fragment, not a real
            // type reference — the outer type will be checked on its own.
            if (t.getParentNode().filter(p -> p instanceof ClassOrInterfaceType).isPresent()) {
                return;
            }
            checkClassName(resolveTypeName(t), t, filter, classLoader, violations, seen);
        });

        root.findAll(ObjectCreationExpr.class).forEach(oc ->
                checkClassName(resolveTypeName(oc.getType()), oc, filter, classLoader, violations, seen));

        root.findAll(ClassExpr.class).forEach(ce ->
                checkClassName(resolveTypeNameFromType(ce.getType()), ce, filter, classLoader, violations, seen));

        root.findAll(InstanceOfExpr.class).forEach(ie ->
                checkClassName(resolveTypeNameFromType(ie.getType()), ie, filter, classLoader, violations, seen));

        root.findAll(TypeExpr.class).forEach(te ->
                checkClassName(resolveTypeNameFromType(te.getType()), te, filter, classLoader, violations, seen));

        root.findAll(AnnotationExpr.class).forEach(ae ->
                checkClassName(resolveAnnotationName(ae), ae, filter, classLoader, violations, seen));

        root.findAll(MethodReferenceExpr.class).forEach(mr ->
                checkClassName(resolveMethodReferenceScope(mr), mr, filter, classLoader, violations, seen));

        root.findAll(MethodCallExpr.class).forEach(mc ->
                checkClassName(resolveMethodDeclaringType(mc), mc, filter, classLoader, violations, seen));

        root.findAll(FieldAccessExpr.class).forEach(fa ->
                checkClassName(resolveFieldDeclaringType(fa), fa, filter, classLoader, violations, seen));
    }

    // ----- resolution helpers ------------------------------------------------

    private static String resolveTypeName(ClassOrInterfaceType t) {
        try {
            ResolvedType r = t.resolve();
            if (r.isReferenceType()) {
                return r.asReferenceType().getQualifiedName();
            }
        } catch (Throwable ignore) {
            // fall through to fallback
        }
        return t.getNameWithScope();
    }

    private static String resolveTypeNameFromType(com.github.javaparser.ast.type.Type t) {
        try {
            ResolvedType r = t.resolve();
            return qualifiedNameOf(r);
        } catch (Throwable ignore) {
            return t.asString();
        }
    }

    private static String qualifiedNameOf(ResolvedType r) {
        if (r == null) return null;
        if (r.isPrimitive() || r.isVoid() || r.isNull() || r.isTypeVariable() || r.isWildcard()) {
            return null;
        }
        if (r.isArray()) {
            return qualifiedNameOf(r.asArrayType().getComponentType());
        }
        if (r.isReferenceType()) {
            return r.asReferenceType().getQualifiedName();
        }
        return null;
    }

    private static String resolveAnnotationName(AnnotationExpr ae) {
        try {
            ResolvedTypeDeclaration d = (ResolvedTypeDeclaration) ae.resolve();
            if (d instanceof ResolvedReferenceTypeDeclaration r) {
                return r.getQualifiedName();
            }
        } catch (Throwable ignore) {
        }
        return ae.getNameAsString();
    }

    private static String resolveMethodReferenceScope(MethodReferenceExpr mr) {
        try {
            ResolvedType r = mr.getScope().calculateResolvedType();
            return qualifiedNameOf(r);
        } catch (Throwable ignore) {
            return mr.getScope().toString();
        }
    }

    private static String resolveMethodDeclaringType(MethodCallExpr mc) {
        try {
            ResolvedMethodDeclaration m = mc.resolve();
            return m.declaringType().getQualifiedName();
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static String resolveFieldDeclaringType(FieldAccessExpr fa) {
        try {
            var resolved = fa.resolve();
            if (resolved.isField()) {
                return resolved.asField().declaringType().getQualifiedName();
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    // ----- filter check ------------------------------------------------------

    private static void checkImport(ImportDeclaration imp, ClassFilter filter, ClassLoader cl,
                                     List<ClassFilterException.Violation> violations, Set<String> seen) {
        if (imp.isAsterisk()) {
            // Star imports have no single class to check here. Their uses are
            // caught via type / method-call / field-access walks below.
            return;
        }
        String name = imp.getNameAsString();
        // Static imports reference a member; the declaring class is the part
        // before the last dot.
        if (imp.isStatic()) {
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                name = name.substring(0, dot);
            }
        }
        checkClassName(name, imp, filter, cl, violations, seen);
    }

    private static void checkClassName(String name, Node node, ClassFilter filter, ClassLoader cl,
                                        List<ClassFilterException.Violation> violations, Set<String> seen) {
        if (name == null || name.isEmpty()) {
            return;
        }
        // Strip array brackets, if any leaked through.
        while (name.endsWith("[]")) {
            name = name.substring(0, name.length() - 2);
        }
        // Skip primitive type names (they're never classes under a filter).
        switch (name) {
            case "boolean": case "byte": case "short": case "int":
            case "long": case "float": case "double": case "char": case "void":
                return;
        }
        String dedupKey = name + "@" + positionKey(node);
        if (!seen.add(dedupKey)) {
            return;
        }
        Class<?> clazz = loadClass(name, cl);
        if (clazz == null) {
            // If neither JavaParser nor the user classLoader can load it, this
            // isn't a real class reference (package path fragment, typo, etc.).
            // javac will reject genuinely-invalid user references a moment later,
            // so we don't need to false-positive here.
            return;
        }
        if (!filter.accept(clazz)) {
            violations.add(violation(clazz.getName(), node, "is not permitted by the configured ClassFilter"));
        }
    }

    private static Class<?> loadClass(String name, ClassLoader cl) {
        ClassLoader loader = cl != null ? cl : Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = ClassFilterValidator.class.getClassLoader();
        }
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // Try treating the last segment as an inner class.
            int lastDot = name.lastIndexOf('.');
            while (lastDot > 0) {
                name = name.substring(0, lastDot) + "$" + name.substring(lastDot + 1);
                try {
                    return Class.forName(name, false, loader);
                } catch (ClassNotFoundException | NoClassDefFoundError ignore) {
                }
                lastDot = name.lastIndexOf('.', lastDot - 1);
            }
            return null;
        }
    }

    private static ClassFilterException.Violation violation(String name, Node node, String reason) {
        Position p = node.getBegin().orElse(null);
        int line = p == null ? -1 : p.line;
        int col = p == null ? -1 : p.column;
        return new ClassFilterException.Violation(name, line, col, reason);
    }

    private static String positionKey(Node node) {
        return node.getBegin().map(p -> p.line + ":" + p.column).orElse("?");
    }
}
