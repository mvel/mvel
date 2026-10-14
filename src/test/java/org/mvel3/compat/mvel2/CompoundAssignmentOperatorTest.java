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

package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.res.NumberHolder;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.CompoundAssignmentOperatorTest (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class CompoundAssignmentOperatorTest extends AbstractTest {

    private static final String HOLDER = "holder";

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

    @Test
    public void testSimpleIntAddNum() {

        String str = "simpleInteger += 10";

        // eval test
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("simpleInteger", new Integer(120));
        Object result = eval(str, varMap);

        Integer simpleInteger = (Integer) varMap.get("simpleInteger");

        assertEquals(130, result);
        assertEquals(130, simpleInteger.intValue());

        // compile test
        varMap = new HashMap<>();
        varMap.put("simpleInteger", new Integer(120));
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        simpleInteger = (Integer) varMap.get("simpleInteger");

        assertEquals(130, result);
        assertEquals(130, simpleInteger.intValue());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testIntAddNum() {

        String str = "holder.intPrimitive += 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(130, result);
        assertEquals(130, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(130, result);
        assertEquals(130, holder.getIntPrimitive());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testIntSubNum() {

        String str = "holder.intPrimitive -= 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(110, result);
        assertEquals(110, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(110, result);
        assertEquals(110, holder.getIntPrimitive());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testIntMultNum() {

        String str = "holder.intPrimitive *= 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(1200, result);
        assertEquals(1200, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(1200, result);
        assertEquals(1200, holder.getIntPrimitive());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testIntDivNum() {

        String str = "holder.intPrimitive /= 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(12.0, result);
        assertEquals(12, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(12.0, result);
        assertEquals(12, holder.getIntPrimitive());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testIntAddVar() {

        String str = "holder.intPrimitive += myIntValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(130, result);
        assertEquals(130, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(130, result);
        assertEquals(130, holder.getIntPrimitive());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testIntSubVar() {

        String str = "holder.intPrimitive -= myIntValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(110, result);
        assertEquals(110, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(110, result);
        assertEquals(110, holder.getIntPrimitive());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testIntMultVar() {

        String str = "holder.intPrimitive *= myIntValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(1200, result);
        assertEquals(1200, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(1200, result);
        assertEquals(1200, holder.getIntPrimitive());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testIntDivVar() {

        String str = "holder.intPrimitive /= myIntValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(12.0, result);
        assertEquals(12, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(12.0, result);
        assertEquals(12, holder.getIntPrimitive());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testBigDecimalAddNum() {

        String str = "holder.bigDecimal += 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("130"), result);
        assertEqualsByComparingTo(new BigDecimal("130"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("130"), result);
        assertEqualsByComparingTo(new BigDecimal("130"), holder.getBigDecimal());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testBigDecimalSubNum() {

        String str = "holder.bigDecimal -= 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("110"), result);
        assertEqualsByComparingTo(new BigDecimal("110"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("110"), result);
        assertEqualsByComparingTo(new BigDecimal("110"), holder.getBigDecimal());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testBigDecimalMultNum() {

        String str = "holder.bigDecimal *= 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("1200"), result);
        assertEqualsByComparingTo(new BigDecimal("1200"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("1200"), result);
        assertEqualsByComparingTo(new BigDecimal("1200"), holder.getBigDecimal());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testBigDecimalDivNum() {

        String str = "holder.bigDecimal /= 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("12"), result);
        assertEqualsByComparingTo(new BigDecimal("12"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("12"), result);
        assertEqualsByComparingTo(new BigDecimal("12"), holder.getBigDecimal());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testBigDecimalAddVar() {

        String str = "holder.bigDecimal += myBigDecimalValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("130"), result);
        assertEqualsByComparingTo(new BigDecimal("130"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("130"), result);
        assertEqualsByComparingTo(new BigDecimal("130"), holder.getBigDecimal());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testBigDecimalSubVar() {

        String str = "holder.bigDecimal -= myBigDecimalValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("110"), result);
        assertEqualsByComparingTo(new BigDecimal("110"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("110"), result);
        assertEqualsByComparingTo(new BigDecimal("110"), holder.getBigDecimal());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testBigDecimalMultVar() {

        String str = "holder.bigDecimal *= myBigDecimalValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("1200"), result);
        assertEqualsByComparingTo(new BigDecimal("1200"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("1200"), result);
        assertEqualsByComparingTo(new BigDecimal("1200"), holder.getBigDecimal());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testBigDecimalDivVar() {

        String str = "holder.bigDecimal /= myBigDecimalValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("12"), result);
        assertEqualsByComparingTo(new BigDecimal("12"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = compileExpression(str);
        result = executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEqualsByComparingTo(new BigDecimal("12"), result);
        assertEqualsByComparingTo(new BigDecimal("12"), holder.getBigDecimal());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testBigDecimalAddNumStrictType() {
        // MVEL2 ParserContext (strong typing, indexed inputs), the COMPILER_OPT_* flags and the
        // IndexedVariableResolverFactory/DefaultKnowledgeHelper root have no MVEL3 equivalent;
        // MVEL3 is always statically typed, so the input type is declared via TestCompilerContext.
        TestCompilerContext parserContext = new TestCompilerContext();
        parserContext.addInput("holder", NumberHolder.class);

        String str = "holder.bigDecimal += 10";

        // compile test
        Serializable s = compileExpression(str, parserContext);

        NumberHolder holder = new NumberHolder();
        holder.setBigDecimal(new BigDecimal("120"));
        Map<String, Object> vars = new HashMap<>();
        vars.put("holder", holder);

        Object result = executeExpression(s, vars);

        assertEqualsByComparingTo(new BigDecimal("130"), result);
        assertEqualsByComparingTo(new BigDecimal("130"), holder.getBigDecimal());
    }

    @Test
    @KnownCompatibility("compound-assign-void-setter")
    public void testBigDecimalAddVarStrictType() {
        // See testBigDecimalAddNumStrictType for the MVEL2-only APIs that were replaced.
        TestCompilerContext parserContext = new TestCompilerContext();
        parserContext.addInput("holder", NumberHolder.class);
        parserContext.addInput("myBigDecimalValue", BigDecimal.class);

        String str = "holder.bigDecimal += myBigDecimalValue";

        // compile test
        Serializable s = compileExpression(str, parserContext);

        NumberHolder holder = new NumberHolder();
        holder.setBigDecimal(new BigDecimal("120"));

        BigDecimal myBigDecimalValue = new BigDecimal("10");
        Map<String, Object> vars = new HashMap<>();
        vars.put("holder", holder);
        vars.put("myBigDecimalValue", myBigDecimalValue);

        Object result = executeExpression(s, vars);

        assertEqualsByComparingTo(new BigDecimal("130"), result);
        assertEqualsByComparingTo(new BigDecimal("130"), holder.getBigDecimal());
    }
}
