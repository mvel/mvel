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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.Thing;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porting of ProjectionsTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>MVEL3 has no projection syntax ({@code (name in things)},
 * {@code (toUpperCase() in list if ($ == 'bar'))}). Every projection is rewritten as an explicit
 * {@code for} loop that fills an {@code ArrayList}. The whitespace variants of the MVEL2 tests
 * ({@code ( name in things )}, tabs, newline) only differ in projection syntax, so they all run
 * the same loop.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>projection-operator</b>: {@code (expr in collection [if (cond)])} is written as a
 *       {@code for} loop; {@code $} becomes the loop variable.</li>
 *   <li><b>raw-collection-typing</b>: input collections are cast to a typed collection in the
 *       {@code for} header ({@code (List<Thing>) things}); a loop or lambda variable over a raw
 *       {@code List} input cannot be resolved.</li>
 *   <li><b>contains-operator</b>: the infix {@code contains} is replaced by
 *       {@code indexOf(..) >= 0}; {@code res.contains(..)} on a local variable fails with
 *       "Unsupported member reference".</li>
 *   <li><b>single-quote-string</b>: string literals use double quotes.</li>
 *   <li><b>this-reference</b>: the {@code Base} root object is not used; {@code things} is passed
 *       as a variable.</li>
 *   <li><b>inline-list</b>: {@code ['bar', 'foo']} is written as a typed MVEL3 list literal
 *       ({@code List<String> l = ["bar", "foo"]}).</li>
 *   <li><b>function-definition</b>: {@code def concat(lists) {...}} and {@code foreach} are
 *       replaced by nested {@code for} loops.</li>
 * </ul>
 */
public class ProjectionsAdaptedTest {

    private static final Set<String> IMPORTS = Set.of(
            "java.util.ArrayList",
            "java.util.Collection",
            "java.util.List",
            "java.util.Map",
            Thing.class.getCanonicalName(),
            User.class.getCanonicalName());

    private static final String NAMES_OF_THINGS =
            "var res = new ArrayList<String>(); for (Thing t : (List<Thing>) things) { res.add(t.getName()); } ";

    private static Object run(final String expression, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, IMPORTS, vars);
    }

    private static Map<String, Object> thingsVars() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("things", new Base().things);
        return vars;
    }

    private static Map<String, Object> stringVars() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("strs", new ArrayList<>(Arrays.asList("zero", "zen", "bar", "foo")));
        return vars;
    }

    /** {@code (toUpperCase() in strs if (condition))}, where the condition tests the original element {@code s}. */
    private static String upperCasedWhere(final String condition) {
        return "var res = new ArrayList<String>(); for (String s : (List<String>) strs) "
                + "{ if (" + condition + ") { res.add(s.toUpperCase()); } } return res;";
    }

    private void assertProjectionContainsBob() {
        Collection<Object> col = (Collection<Object>) run(NAMES_OF_THINGS + "return res;", thingsVars());
        assertThat(col).hasSize(3).contains("Bob");
    }

    @Test
    public void testProjectionSupport() {
        assertThat(run(NAMES_OF_THINGS + "return res.indexOf(\"Bob\") >= 0;", thingsVars())).isEqualTo(true);
    }

    @Test
    public void testProjectionSupport1() {
        assertThat(run(NAMES_OF_THINGS + "return res.indexOf(\"Bob\") >= 0;", thingsVars())).isEqualTo(true);
    }

    @Test
    public void testProjectionSupport2() {
        assertThat(run(NAMES_OF_THINGS + "return res.size();", thingsVars())).isEqualTo(3);
    }

    @Test
    public void testProjectionSupport3() {
        assertThat(run("List<String> l = [\"bar\", \"foo\"]; var res = new ArrayList<String>(); "
                + "for (String s : l) { res.add(s.toUpperCase()); } return res.get(1);", new HashMap<>()))
                .isEqualTo("FOO");
    }

    @Test
    public void testProjectionSupport4() {
        Collection<Object> col = (Collection<Object>) run(upperCasedWhere("s.equals(\"bar\")"), stringVars());
        assertThat(col).containsExactly("BAR");
    }

    @Test
    public void testProjectionSupport5() {
        Collection<Object> col = (Collection<Object>) run(upperCasedWhere("s.startsWith(\"z\")"), stringVars());
        assertThat(col).containsExactly("ZERO", "ZEN");
    }

    @Test
    public void testProjectionSupport6() {
        assertThat(run(NAMES_OF_THINGS + "return res.indexOf(\"Bob\") >= 0;", thingsVars())).isEqualTo(true);
    }

    @Test
    public void testProjectionStandaloneWithSpaces1() {
        assertProjectionContainsBob();
    }

    @Test
    public void testProjectionStandaloneWithSpaces2() {
        assertProjectionContainsBob();
    }

    @Test
    public void testProjectionStandaloneWithSpaces3() {
        assertProjectionContainsBob();
    }

    @Test
    public void testProjectionStandaloneWithSpaces4() {
        assertProjectionContainsBob();
    }

    @Test
    public void testProjectionStandaloneWithSpaces5() {
        assertProjectionContainsBob();
    }

    @Test
    public void testProjectionStandaloneWithSpaces6() {
        assertProjectionContainsBob();
    }

    @Test
    public void testProjectionMultipleSpaces() {
        assertProjectionContainsBob();
    }

    @Test
    public void testProjectionWithTabs() {
        assertProjectionContainsBob();
    }

    @Test
    public void testProjectionIfWithTrailingSpace() {
        Collection<Object> col = (Collection<Object>) run(upperCasedWhere("s.equals(\"bar\")"), stringVars());
        assertThat(col).hasSize(1);
        assertThat(col.iterator().next()).isEqualTo("BAR");
    }

    @Test
    public void testProjectionCollectionNameStartingWithIn() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("innings", new ArrayList<>(Arrays.asList(
                new HashMap<String, Object>() {{ put("name", "First"); }},
                new HashMap<String, Object>() {{ put("name", "Second"); }})));
        Collection<Object> col = (Collection<Object>) run(
                "var res = new ArrayList<Object>(); for (Map m : (List<Map>) innings) { res.add(m.get(\"name\")); } return res;",
                vars);
        assertThat(col).hasSize(2).contains("First");
    }

    @Test
    public void testProjectionCollectionNameStartingWithIf() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("iffy_list", new ArrayList<>(Arrays.asList("zero", "zen", "bar")));
        Collection<Object> col = (Collection<Object>) run(
                "var res = new ArrayList<String>(); for (String s : (List<String>) iffy_list) "
                        + "{ if (s.startsWith(\"z\")) { res.add(s.toUpperCase()); } } return res;",
                vars);
        assertThat(col).hasSize(2).contains("ZERO", "ZEN");
    }

    @Test
    public void testConcatProjection() {
        Collection<User> users = new ArrayList<>();
        User user1 = new User("John");
        user1.getFamilyMembers().add(new User("George"));
        User user2 = new User("Bob");
        User user3 = new User("Cindy");
        user3.getFamilyMembers().add(new User("Michael"));
        user3.getFamilyMembers().add(new User("Laura"));
        users.add(user1);
        users.add(user2);
        users.add(user3);

        Map<String, Object> vars = new HashMap<>();
        vars.put("users", users);

        Object result = run("var res = new ArrayList<String>(); "
                + "for (User u : (Collection<User>) users) { "
                + "for (Object f : u.getFamilyMembers()) { res.add(((User) f).getName()); } } "
                + "return res;", vars);
        assertThat(result).isEqualTo(Arrays.asList("George", "Michael", "Laura"));
    }

    public static final class User {
        private String name;
        private Collection familyMembers = new ArrayList();

        public User(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Collection getFamilyMembers() {
            return familyMembers;
        }

        public void setFamilyMembers(Collection familyMembers) {
            this.familyMembers = familyMembers;
        }
    }
}
