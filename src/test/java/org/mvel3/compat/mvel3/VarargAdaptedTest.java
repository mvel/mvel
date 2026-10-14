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

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;
import org.mvel3.compat.mvel2.VarargTests.MyVarargTestee;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Porting of VarargTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>single-quote-string</b>: {@code 'a'} becomes {@code "a"}.</li>
 *   <li><b>void-method-expression</b>: a {@code void} call cannot be an
 *       {@code Object}-typed expression, so it is written as a block statement
 *       ({@code testee.add("a"); return null;}).</li>
 *   <li><b>inner-class-binary-name</b>: the nested class is imported through the
 *       API imports set and referenced by its simple name instead of the
 *       {@code $}-separated binary name returned by {@code Class.getName()}.</li>
 * </ul>
 */
public class VarargAdaptedTest {

    private static final Set<String> IMPORTS = Set.of(MyVarargTestee.class.getCanonicalName());

    @Test
    public void testSingleArgMethodPreference() {
        MyVarargTestee testee = new MyVarargTestee();
        new MVEL().executeExpression("testee.add(\"a\"); return null;", IMPORTS, Collections.singletonMap("testee", testee));
        assertEquals("singleArgMethoda", testee.toString());
    }

    @Test
    public void testVarargMethod() {
        MyVarargTestee testee = new MyVarargTestee();
        new MVEL().executeExpression("testee.add(\"a\", \"b\"); return null;", IMPORTS, Collections.singletonMap("testee", testee));
        assertEquals("multipleArgsMethod[a, b]", testee.toString());
    }

    @Test
    public void testNoArgCtorPreference() {
        MyVarargTestee result = (MyVarargTestee) new MVEL().executeExpression(
                "new MyVarargTestee()", IMPORTS, Collections.emptyMap());
        assertEquals("", result.toString());
    }

    @Test
    public void testSingleArgCtorPreference() {
        MyVarargTestee result = (MyVarargTestee) new MVEL().executeExpression(
                "new MyVarargTestee(\"a\")", IMPORTS, Collections.emptyMap());
        assertEquals("singleArgCtora", result.toString());
    }

    @Test
    public void testVarargCtor() {
        MyVarargTestee result = (MyVarargTestee) new MVEL().executeExpression(
                "new MyVarargTestee(\"a\", \"b\")", IMPORTS, Collections.emptyMap());
        assertEquals("multipleArgsCtor[a, b]", result.toString());
    }
}
