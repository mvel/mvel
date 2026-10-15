/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.compat.mvel3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.Evaluator;
import org.mvel3.MVEL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porting of MvelCompileExpNullSafeTest from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>null-safe-operator</b>: {@code a.?b} returns the object on its left in MVEL3, so it is
 *       written as an explicit null check ({@code a != null ? a.b : null}).</li>
 *   <li><b>projection-operator</b>: {@code ($.?child.firstName in list if $.?child.firstName != null)}
 *       is written as a typed {@code for} loop that counts the matching elements.</li>
 *   <li><b>dynamic-property-type</b>: values of a raw {@code Map} are cast before use.</li>
 *   <li>Fixtures are public static nested classes, because the generated evaluator must be able
 *       to access them.</li>
 * </ul>
 *
 * <p>Not ported: {@code testCustomResolverFactory} (MVEL2-only {@code VariableResolverFactory} API).
 */
public class MvelCompileExpNullSafeAdaptedTest {

    public static class StringConverter {
        public String convert(String value) {
            return value.toUpperCase();
        }
    }

    public static class Child {
        private String firstName;
        private String lastName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

    public static class Parent {
        private Child child;

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }
    }

    public static class ParentGroup {
        private List<Parent> parentList;

        public List<Parent> getParentList() {
            return parentList;
        }

        public void setParentList(List<Parent> parentList) {
            this.parentList = parentList;
        }
    }

    private static final Set<String> IMPORTS = Set.of(
            Child.class.getCanonicalName(),
            Parent.class.getCanonicalName(),
            ParentGroup.class.getCanonicalName(),
            StringConverter.class.getCanonicalName(),
            "java.util.Map");

    private static final String NULL_FIRST_NAME_EXPRESSION =
            "int count = 0;"
            + " if (parentGroup != null) {"
            + "   for (Parent p : parentGroup.getParentList()) {"
            + "     if (p.getChild() != null && p.getChild().getFirstName() != null) { count++; }"
            + "   }"
            + " }"
            + " return parentGroup != null && count > 0;";

    private static Evaluator<Map<String, Object>, Void, Object> compileParentGroup(final ParentGroup sample) {
        final Map<String, Object> vars = new HashMap<>();
        vars.put("parentGroup", sample);
        return new MVEL().compileMapBlock(NULL_FIRST_NAME_EXPRESSION, Object.class, IMPORTS, MVEL.getTypeMap(vars));
    }

    private static Parent parentWith(final Child child) {
        final Parent parent = new Parent();
        parent.setChild(child);
        return parent;
    }

    private static Child childWith(final String firstName) {
        final Child child = new Child();
        child.setFirstName(firstName);
        return child;
    }

    private static ParentGroup groupOf(final Parent... parents) {
        final ParentGroup group = new ParentGroup();
        final List<Parent> list = new ArrayList<>();
        Collections.addAll(list, parents);
        group.setParentList(list);
        return group;
    }

    @Test
    public void testMvelCompileNullSafeNullFirst() {
        final Evaluator<Map<String, Object>, Void, Object> compiled = compileParentGroup(groupOf());

        ParentGroup pGroup = groupOf(parentWith(null));
        assertThat(compiled.eval(Collections.singletonMap("parentGroup", pGroup))).isEqualTo(false);

        pGroup = groupOf(parentWith(childWith("vlaa")), parentWith(childWith(null)));
        assertThat(compiled.eval(Collections.singletonMap("parentGroup", pGroup))).isEqualTo(true);
    }

    @Test
    public void testMvelCompileNullSafeNullSecond() {
        final Evaluator<Map<String, Object>, Void, Object> compiled = compileParentGroup(groupOf());

        ParentGroup pGroup = groupOf(parentWith(childWith("vlaa")), parentWith(childWith(null)));
        assertThat(compiled.eval(Collections.singletonMap("parentGroup", pGroup))).isEqualTo(true);

        pGroup = groupOf(parentWith(null));
        assertThat(compiled.eval(Collections.singletonMap("parentGroup", pGroup))).isEqualTo(false);
    }

    /** Tests the null-safe access with a segment which shadows a variable. */
    @Test
    public void testNullSafeVariableOverlap() {
        final String expected = "You got me!";

        final Map<String, Object> foo = new HashMap<>();
        final Map<String, Object> bar = Collections.singletonMap("baz", expected);
        foo.put("bar", null);

        // The input variable `baz` overlaps the segment resolved after the null check.
        final Map<String, Object> vars = new HashMap<>();
        vars.put("foo", foo);
        vars.put("baz", "no, over here!");

        final Evaluator<Map<String, Object>, Void, Object> compiled = new MVEL().compileMapExpression(
                "foo.get(\"bar\") != null ? ((Map) foo.get(\"bar\")).get(\"baz\") : null",
                Object.class, IMPORTS, MVEL.getTypeMap(vars));
        assertThat(compiled.eval(vars)).isNull();

        foo.put("bar", bar);
        assertThat(compiled.eval(vars)).isEqualTo(expected);
    }

    @Test
    public void testNullSafeWithVariableAsMapKey() {
        final String expected = "You got me!";

        final Map<String, Object> foo = new HashMap<>();
        final Map<String, Object> bar = Collections.singletonMap("baz", expected);
        foo.put("bar", null);

        final Map<String, Object> vars = new HashMap<>();
        vars.put("foo", foo);
        vars.put("myvar", "baz");

        final Evaluator<Map<String, Object>, Void, Object> compiled = new MVEL().compileMapExpression(
                "foo.get(\"bar\") != null ? ((Map) foo.get(\"bar\")).get(myvar) : null",
                Object.class, IMPORTS, MVEL.getTypeMap(vars));
        assertThat(compiled.eval(vars)).isNull();

        foo.put("bar", bar);
        assertThat(compiled.eval(vars)).isEqualTo(expected);
    }

    @Test
    public void testNullSafeWithVariableAsMethodArg() {
        final Map<String, Object> foo = new HashMap<>();
        foo.put("bar", null);

        final Map<String, Object> vars = new HashMap<>();
        vars.put("foo", foo);
        vars.put("myvar", "hello");

        final Evaluator<Map<String, Object>, Void, Object> compiled = new MVEL().compileMapExpression(
                "foo.get(\"bar\") != null ? ((StringConverter) foo.get(\"bar\")).convert(myvar) : null",
                Object.class, IMPORTS, MVEL.getTypeMap(vars));
        assertThat(compiled.eval(vars)).isNull();

        foo.put("bar", new StringConverter());
        assertThat(compiled.eval(vars)).isEqualTo("HELLO");
    }
}
