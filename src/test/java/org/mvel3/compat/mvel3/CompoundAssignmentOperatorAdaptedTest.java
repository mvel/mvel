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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;
import org.mvel3.compat.mvel2.res.NumberHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porting of CompoundAssignmentOperatorTest from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>compound-assign-void-setter</b>: a compound assignment on a property
 *       ({@code holder.intPrimitive += 10}) is transpiled to a setter call that returns
 *       {@code void}, so it cannot be the value of an expression. The assignment is
 *       written as a statement and the new value is read back with {@code return}
 *       ({@code holder.intPrimitive += 10; return holder.intPrimitive;}).</li>
 *   <li><b>missing-auto-import</b>: {@code java.math.BigDecimal} is added to the imports,
 *       because the transpiled {@code BigDecimal.valueOf(..)} refers to it by simple name.</li>
 *   <li><b>integer-division</b>: {@code holder.intPrimitive /= 10} yields {@code int} 12
 *       (Java semantics) where MVEL2 returned {@code double} 12.0.</li>
 *   <li>MVEL2's separate eval/compile passes and strict-typing {@code ParserContext}
 *       (indexed inputs, {@code DefaultKnowledgeHelper}) have no MVEL3 equivalent; MVEL3
 *       always compiles with declared input types, so each case runs once.</li>
 * </ul>
 */
public class CompoundAssignmentOperatorAdaptedTest {

    private static final String HOLDER = "holder";

    private static final Set<String> IMPORTS = Set.of(
            BigDecimal.class.getCanonicalName(),
            NumberHolder.class.getCanonicalName());

    private Map<String, Object> createVarMap() {
        Map<String, Object> varMap = new HashMap<>();
        NumberHolder holder = new NumberHolder();
        holder.setIntPrimitive(120);
        holder.setBigDecimal(new BigDecimal("120"));
        varMap.put(HOLDER, holder);
        varMap.put("myIntValue", 10);
        varMap.put("myBigDecimalValue", 10);
        return varMap;
    }

    private static Object run(final String expression, final Map<String, Object> vars) {
        return new MVEL().executeExpression(expression, IMPORTS, vars);
    }

    private void assertInt(final String op, final String operand, final int expected) {
        Map<String, Object> varMap = createVarMap();
        Object result = run("holder.intPrimitive " + op + " " + operand + "; return holder.intPrimitive;", varMap);
        assertThat(result).isEqualTo(expected);
        assertThat(((NumberHolder) varMap.get(HOLDER)).getIntPrimitive()).isEqualTo(expected);
    }

    private void assertBigDecimal(final String op, final String operand, final String expected) {
        Map<String, Object> varMap = createVarMap();
        Object result = run("holder.bigDecimal " + op + " " + operand + "; return holder.bigDecimal;", varMap);
        assertThat((BigDecimal) result).isEqualByComparingTo(new BigDecimal(expected));
        assertThat(((NumberHolder) varMap.get(HOLDER)).getBigDecimal()).isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    public void testSimpleIntAddNum() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("simpleInteger", Integer.valueOf(120));
        Object result = run("simpleInteger += 10", varMap);
        assertThat(result).isEqualTo(130);
        assertThat(varMap.get("simpleInteger")).isEqualTo(130);
    }

    @Test
    public void testIntAddNum() {
        assertInt("+=", "10", 130);
    }

    @Test
    public void testIntSubNum() {
        assertInt("-=", "10", 110);
    }

    @Test
    public void testIntMultNum() {
        assertInt("*=", "10", 1200);
    }

    @Test
    public void testIntDivNum() {
        assertInt("/=", "10", 12);
    }

    @Test
    public void testIntAddVar() {
        assertInt("+=", "myIntValue", 130);
    }

    @Test
    public void testIntSubVar() {
        assertInt("-=", "myIntValue", 110);
    }

    @Test
    public void testIntMultVar() {
        assertInt("*=", "myIntValue", 1200);
    }

    @Test
    public void testIntDivVar() {
        assertInt("/=", "myIntValue", 12);
    }

    @Test
    public void testBigDecimalAddNum() {
        assertBigDecimal("+=", "10", "130");
    }

    @Test
    public void testBigDecimalSubNum() {
        assertBigDecimal("-=", "10", "110");
    }

    @Test
    public void testBigDecimalMultNum() {
        assertBigDecimal("*=", "10", "1200");
    }

    @Test
    public void testBigDecimalDivNum() {
        assertBigDecimal("/=", "10", "12");
    }

    @Test
    public void testBigDecimalAddVar() {
        assertBigDecimal("+=", "myBigDecimalValue", "130");
    }

    @Test
    public void testBigDecimalSubVar() {
        assertBigDecimal("-=", "myBigDecimalValue", "110");
    }

    @Test
    public void testBigDecimalMultVar() {
        assertBigDecimal("*=", "myBigDecimalValue", "1200");
    }

    @Test
    public void testBigDecimalDivVar() {
        assertBigDecimal("/=", "myBigDecimalValue", "12");
    }

    @Test
    public void testBigDecimalAddNumStrictType() {
        assertBigDecimal("+=", "10", "130");
    }

    @Test
    public void testBigDecimalAddVarStrictType() {
        assertBigDecimal("+=", "myBigDecimalValue", "130");
    }
}
