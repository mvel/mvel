package org.mvel2.tests.core;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.junit.Test;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.IndexedVariableResolverFactory;
import org.mvel2.tests.core.res.DefaultKnowledgeHelper;
import org.mvel2.tests.core.res.NumberHolder;

public class CompoundAssignmentOperatorTest extends TestCase {

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
        Object result = MVEL.eval(str, varMap);

        Integer simpleInteger = (Integer) varMap.get("simpleInteger");

        assertEquals(130, result);
        assertEquals(130, simpleInteger.intValue());

        // compile test
        varMap = new HashMap<>();
        varMap.put("simpleInteger", new Integer(120));
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        simpleInteger = (Integer) varMap.get("simpleInteger");

        assertEquals(130, result);
        assertEquals(130, simpleInteger.intValue());
    }

    @Test
    public void testIntAddNum() {

        String str = "holder.intPrimitive += 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(130, result);
        assertEquals(130, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(130, result);
        assertEquals(130, holder.getIntPrimitive());
    }

    @Test
    public void testIntSubNum() {

        String str = "holder.intPrimitive -= 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(110, result);
        assertEquals(110, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(110, result);
        assertEquals(110, holder.getIntPrimitive());
    }

    @Test
    public void testIntMultNum() {

        String str = "holder.intPrimitive *= 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(1200, result);
        assertEquals(1200, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(1200, result);
        assertEquals(1200, holder.getIntPrimitive());
    }

    @Test
    public void testIntDivNum() {

        String str = "holder.intPrimitive /= 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(12.0, result);
        assertEquals(12, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(12.0, result);
        assertEquals(12, holder.getIntPrimitive());
    }

    @Test
    public void testIntAddVar() {

        String str = "holder.intPrimitive += myIntValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(130, result);
        assertEquals(130, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(130, result);
        assertEquals(130, holder.getIntPrimitive());
    }

    @Test
    public void testIntSubVar() {

        String str = "holder.intPrimitive -= myIntValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(110, result);
        assertEquals(110, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(110, result);
        assertEquals(110, holder.getIntPrimitive());
    }

    @Test
    public void testIntMultVar() {

        String str = "holder.intPrimitive *= myIntValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(1200, result);
        assertEquals(1200, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(1200, result);
        assertEquals(1200, holder.getIntPrimitive());
    }

    @Test
    public void testIntDivVar() {

        String str = "holder.intPrimitive /= myIntValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(12.0, result);
        assertEquals(12, holder.getIntPrimitive());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(12.0, result);
        assertEquals(12, holder.getIntPrimitive());
    }

    @Test
    public void testBigDecimalAddNum() {

        String str = "holder.bigDecimal += 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("130"), result);
        assertEquals(new BigDecimal("130"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("130"), result);
        assertEquals(new BigDecimal("130"), holder.getBigDecimal());
    }

    @Test
    public void testBigDecimalSubNum() {

        String str = "holder.bigDecimal -= 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("110"), result);
        assertEquals(new BigDecimal("110"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("110"), result);
        assertEquals(new BigDecimal("110"), holder.getBigDecimal());
    }

    @Test
    public void testBigDecimalMultNum() {

        String str = "holder.bigDecimal *= 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("1200"), result);
        assertEquals(new BigDecimal("1200"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("1200"), result);
        assertEquals(new BigDecimal("1200"), holder.getBigDecimal());
    }

    @Test
    public void testBigDecimalDivNum() {

        String str = "holder.bigDecimal /= 10";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("12"), result);
        assertEquals(new BigDecimal("12"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("12"), result);
        assertEquals(new BigDecimal("12"), holder.getBigDecimal());
    }

    @Test
    public void testBigDecimalAddVar() {

        String str = "holder.bigDecimal += myBigDecimalValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("130"), result);
        assertEquals(new BigDecimal("130"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("130"), result);
        assertEquals(new BigDecimal("130"), holder.getBigDecimal());
    }

    @Test
    public void testBigDecimalSubVar() {

        String str = "holder.bigDecimal -= myBigDecimalValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("110"), result);
        assertEquals(new BigDecimal("110"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("110"), result);
        assertEquals(new BigDecimal("110"), holder.getBigDecimal());
    }

    @Test
    public void testBigDecimalMultVar() {

        String str = "holder.bigDecimal *= myBigDecimalValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("1200"), result);
        assertEquals(new BigDecimal("1200"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("1200"), result);
        assertEquals(new BigDecimal("1200"), holder.getBigDecimal());
    }

    @Test
    public void testBigDecimalDivVar() {

        String str = "holder.bigDecimal /= myBigDecimalValue";

        // eval test
        Map<String, Object> varMap = createVarMap();
        Object result = MVEL.eval(str, varMap);

        NumberHolder holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("12"), result);
        assertEquals(new BigDecimal("12"), holder.getBigDecimal());

        // compile test
        varMap = createVarMap();
        Serializable s = MVEL.compileExpression(str);
        result = MVEL.executeExpression(s, varMap);

        holder = (NumberHolder) varMap.get(HOLDER);

        assertEquals(new BigDecimal("12"), result);
        assertEquals(new BigDecimal("12"), holder.getBigDecimal());
    }

    @Test
    public void testBigDecimalAddNumStrictType() {

        boolean allowNakedMethCall = MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL;
        boolean allowOverrideAllProphandling = MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING;
        boolean allowResolveInnerclassesWithDotnotation = MVEL.COMPILER_OPT_ALLOW_RESOLVE_INNERCLASSES_WITH_DOTNOTATION;
        boolean supportJavaStyleClassLiterals = MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS;

        try {
            // mimic drools usage
            MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true;
            MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
            MVEL.COMPILER_OPT_ALLOW_RESOLVE_INNERCLASSES_WITH_DOTNOTATION = true;
            MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS = true;

            ParserContext parserContext = ParserContext.create();
            parserContext.setStrictTypeEnforcement(true);
            parserContext.setStrongTyping(true);
            parserContext.setIndexAllocation(true);

            String[] names = {"holder"};
            parserContext.addIndexedInput(names);
            parserContext.addInput("holder", NumberHolder.class);

            String str = "holder.bigDecimal += 10";

            // compile test
            Serializable s = MVEL.compileExpression(str, parserContext);

            DefaultKnowledgeHelper helper = new DefaultKnowledgeHelper();

            NumberHolder holder = new NumberHolder();
            holder.setBigDecimal(new BigDecimal("120"));
            Object[] values = {holder};

            VariableResolverFactory variableResolverFactory = new IndexedVariableResolverFactory(names, values);

            Object result = MVEL.executeExpression(s, helper, variableResolverFactory);

            assertEquals(new BigDecimal("130"), result);
            assertEquals(new BigDecimal("130"), holder.getBigDecimal());
        } finally {
            MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = allowNakedMethCall;
            MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = allowOverrideAllProphandling;
            MVEL.COMPILER_OPT_ALLOW_RESOLVE_INNERCLASSES_WITH_DOTNOTATION = allowResolveInnerclassesWithDotnotation;
            MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS = supportJavaStyleClassLiterals;
        }
    }

    @Test
    public void testBigDecimalAddVarStrictType() {

        boolean allowNakedMethCall = MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL;
        boolean allowOverrideAllProphandling = MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING;
        boolean allowResolveInnerclassesWithDotnotation = MVEL.COMPILER_OPT_ALLOW_RESOLVE_INNERCLASSES_WITH_DOTNOTATION;
        boolean supportJavaStyleClassLiterals = MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS;

        try {
            // mimic drools usage
            MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = true;
            MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = true;
            MVEL.COMPILER_OPT_ALLOW_RESOLVE_INNERCLASSES_WITH_DOTNOTATION = true;
            MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS = true;

            ParserContext parserContext = ParserContext.create();
            parserContext.setStrictTypeEnforcement(true);
            parserContext.setStrongTyping(true);
            parserContext.setIndexAllocation(true);

            String[] names = {"holder", "myBigDecimalValue"};
            parserContext.addIndexedInput(names);
            parserContext.addInput("holder", NumberHolder.class);
            parserContext.addInput("myBigDecimalValue", BigDecimal.class);

            String str = "holder.bigDecimal += myBigDecimalValue";

            // compile test
            Serializable s = MVEL.compileExpression(str, parserContext);

            DefaultKnowledgeHelper helper = new DefaultKnowledgeHelper();

            NumberHolder holder = new NumberHolder();
            holder.setBigDecimal(new BigDecimal("120"));

            BigDecimal myBigDecimalValue = new BigDecimal("10");
            Object[] values = {holder, myBigDecimalValue};
            VariableResolverFactory variableResolverFactory = new IndexedVariableResolverFactory(names, values);

            Object result = MVEL.executeExpression(s, helper, variableResolverFactory);

            assertEquals(new BigDecimal("130"), result);
            assertEquals(new BigDecimal("130"), holder.getBigDecimal());
        } finally {
            MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL = allowNakedMethCall;
            MVEL.COMPILER_OPT_ALLOW_OVERRIDE_ALL_PROPHANDLING = allowOverrideAllProphandling;
            MVEL.COMPILER_OPT_ALLOW_RESOLVE_INNERCLASSES_WITH_DOTNOTATION = allowResolveInnerclassesWithDotnotation;
            MVEL.COMPILER_OPT_SUPPORT_JAVA_STYLE_CLASS_LITERALS = supportJavaStyleClassLiterals;
        }
    }
}
