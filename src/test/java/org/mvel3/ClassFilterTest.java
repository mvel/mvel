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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mvel3.parser.MvelParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ClassFilterTest {

    @BeforeAll
    static void enableAntlrParser() {
        MvelParser.Factory.USE_ANTLR = true;
    }

    private Evaluator<Map<String, Object>, Void, Object> compile(String expression, ClassFilter filter) {
        return compile(expression, filter, Set.of());
    }

    private Evaluator<Map<String, Object>, Void, Object> compile(String expression, ClassFilter filter, Set<String> imports) {
        MVELBuilder<Map<String, Object>, Void, Object> b = MVEL.<Object>map()
                .<Object>out(Object.class)
                .expression(expression)
                .classManager(new ClassManager())
                .classLoader(ClassLoader.getSystemClassLoader())
                .imports(imports);
        if (filter != null) {
            b.classFilter(filter);
        }
        return b.compile();
    }

    private Evaluator<Map<String, Object>, Void, Object> compileBlock(String block, ClassFilter filter, Set<String> imports) {
        MVELBuilder<Map<String, Object>, Void, Object> b = MVEL.<Object>map()
                .<Object>out(Object.class)
                .block(block)
                .classManager(new ClassManager())
                .classLoader(ClassLoader.getSystemClassLoader())
                .imports(imports);
        if (filter != null) {
            b.classFilter(filter);
        }
        return b.compile();
    }

    // 1. no filter configured → no restriction (regression check)
    @Test
    void noFilter_allowsEverything() {
        assertThatNoException().isThrownBy(() ->
                compile("new java.util.ArrayList()", null));
    }

    // 2. blocklist rejects Runtime
    @Test
    void blocklist_rejectsRuntime() {
        ClassFilter filter = ClassFilter.blocklist(Runtime.class);
        assertThatExceptionOfType(ClassFilterException.class)
                .isThrownBy(() -> compile("java.lang.Runtime.getRuntime()", filter))
                .satisfies(ex -> {
                    assertThat(ex.getMessage()).contains("java.lang.Runtime");
                    assertThat(ex.getViolations()).isNotEmpty();
                });
    }

    // 3. blocklistWithSubtypes rejects subtypes
    @Test
    void blocklistWithSubtypes_rejectsSubtype() {
        // URLClassLoader extends ClassLoader -> blocked
        ClassFilter filter = ClassFilter.blocklistWithSubtypes(ClassLoader.class);
        Set<String> imports = new HashSet<>();
        imports.add("java.net.URLClassLoader");
        imports.add("java.net.URL");
        assertThatExceptionOfType(ClassFilterException.class)
                .isThrownBy(() -> compile("new URLClassLoader(new URL[0])", filter, imports));
    }

    // 4. allowlist — allowed class works, unlisted class rejected
    @Test
    void allowlist_allowsListedAndRejectsOthers() {
        ClassFilter filter = ClassFilter.allowlist(ArrayList.class, String.class, Object.class);

        assertThatNoException().isThrownBy(() ->
                compile("new ArrayList()", filter, Set.of("java.util.ArrayList")));

        assertThatExceptionOfType(ClassFilterException.class)
                .isThrownBy(() -> compile("new HashMap()", filter, Set.of("java.util.HashMap")));
    }

    // 5. SAFE_PRESET — normal code works, dangerous code blocked
    @Test
    void safePreset_blocksRuntimeAllowsCollections() {
        Set<String> imports = Set.of("java.util.ArrayList");
        assertThatNoException().isThrownBy(() ->
                compile("new ArrayList()", ClassFilter.SAFE_PRESET, imports));

        assertThatExceptionOfType(ClassFilterException.class)
                .isThrownBy(() -> compile("java.lang.Runtime.getRuntime()", ClassFilter.SAFE_PRESET));
    }

    // 6. Import-level rejection. Note: JavaParser silently drops java.lang
    // imports because java.lang is auto-imported, so we test with java.io.File.
    @Test
    void importOfBlockedClass_rejected() {
        ClassFilter filter = ClassFilter.blocklist(java.io.File.class);
        Set<String> imports = Set.of("java.io.File");
        assertThatExceptionOfType(ClassFilterException.class)
                .isThrownBy(() -> compile("1", filter, imports))
                .satisfies(ex -> assertThat(ex.getMessage()).contains("java.io.File"));
    }

    // 7. .class literal rejection
    @Test
    void classLiteral_rejected() {
        ClassFilter filter = ClassFilter.blocklist(Runtime.class);
        assertThatExceptionOfType(ClassFilterException.class)
                .isThrownBy(() -> compile("java.lang.Runtime.class", filter));
    }

    // 8. array-of-blocked-type rejection
    @Test
    void arrayOfBlockedClass_rejected() {
        ClassFilter filter = ClassFilter.blocklist(Runtime.class);
        assertThatExceptionOfType(ClassFilterException.class)
                .isThrownBy(() -> compile("new java.lang.Runtime[0]", filter));
    }

    // 9. instanceof rejection
    @Test
    void instanceofBlockedClass_rejected() {
        ClassFilter filter = ClassFilter.blocklistWithSubtypes(ClassLoader.class);
        Map<String, Object> vars = new HashMap<>();
        vars.put("x", "hello");
        assertThatExceptionOfType(ClassFilterException.class)
                .isThrownBy(() -> compile("x instanceof java.lang.ClassLoader", filter));
    }

    // 10. Batched error output — multiple violations in one exception
    @Test
    void batchedErrors_reportedTogether() {
        ClassFilter filter = ClassFilter.blocklist(Runtime.class, java.io.File.class);
        assertThatExceptionOfType(ClassFilterException.class)
                .isThrownBy(() -> compileBlock(
                        "java.lang.Runtime r = null; java.io.File f = null; return r;",
                        filter, Set.of()))
                .satisfies(ex -> {
                    assertThat(ex.getViolations()).hasSizeGreaterThanOrEqualTo(2);
                    String msg = ex.getMessage();
                    assertThat(msg).contains("Runtime");
                    assertThat(msg).contains("File");
                });
    }

    // 11. Blocklist exact-match does NOT reject subtypes unintentionally
    @Test
    void blocklistExactMatch_doesNotRejectUnrelated() {
        // blocklist Runtime but don't touch Object/ArrayList/etc. Every
        // method call's declaringType() chain passes through Object, so a
        // broken implementation would flag this.
        ClassFilter filter = ClassFilter.blocklist(Runtime.class);
        assertThatNoException().isThrownBy(() ->
                compile("new java.util.ArrayList().toString()", filter));
    }

    // 12. Exact blocklist does NOT reject Runtime's supertype Object
    @Test
    void blocklistExactMatch_doesNotRejectSupertype() {
        ClassFilter filter = ClassFilter.blocklist(ArrayList.class);
        // HashMap uses Object in its method chain; must still compile
        assertThatNoException().isThrownBy(() ->
                compile("new java.util.HashMap().toString()", filter));
    }

    // 13. blocklistPackage rejects members of the package
    @Test
    void blocklistPackage_rejectsPackageMember() {
        ClassFilter filter = ClassFilter.blocklistPackage("java.lang.reflect.");
        Set<String> imports = Set.of("java.lang.reflect.Method");
        assertThatExceptionOfType(ClassFilterException.class)
                .isThrownBy(() -> compile("1", filter, imports));
    }

    // 14. Composed filter: allowlist AND blocklist must BOTH pass
    @Test
    void composedFilter_allowlistAndBlocklist() {
        ClassFilter filter = ClassFilter.allowlistWithSubtypes(Object.class)
                .and(ClassFilter.blocklist(Runtime.class));
        // ArrayList -> accepted by allowlist, not in blocklist -> OK
        Set<String> imports = Set.of("java.util.ArrayList");
        assertThatNoException().isThrownBy(() ->
                compile("new ArrayList()", filter, imports));
        // Runtime -> accepted by allowlist but in blocklist -> rejected
        assertThatExceptionOfType(ClassFilterException.class)
                .isThrownBy(() -> compile("java.lang.Runtime.getRuntime()", filter));
    }

    // 15. Violations carry source position
    @Test
    void violation_hasSourcePosition() {
        ClassFilter filter = ClassFilter.blocklist(Runtime.class);
        assertThatExceptionOfType(ClassFilterException.class)
                .isThrownBy(() -> compile("java.lang.Runtime.getRuntime()", filter))
                .satisfies(ex -> {
                    List<ClassFilterException.Violation> vs = ex.getViolations();
                    assertThat(vs).isNotEmpty();
                    assertThat(vs.get(0).line()).isPositive();
                });
    }

    // 16. Normal evaluator execution works end-to-end with filter configured
    @Test
    void endToEnd_evalRunsWithFilter() {
        Evaluator<Map<String, Object>, Void, Object> eval = compile(
                "1 + 2", ClassFilter.SAFE_PRESET);
        assertThat(eval.eval(new HashMap<>())).isEqualTo(3);
    }
}
