package org.mvel3.compat.mvel2;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Mvel3TestSupportTest {

    @Test
    void evaluatesWithoutDeclaredInputs() {
        assertThat(Mvel3TestSupport.eval("1 + 2")).isEqualTo(3);
    }

    @Test
    void evaluatesWithDeclaredPrimitiveInput() {
        final TestCompilerContext context = TestCompilerContext.create().withInput("x", double.class);
        final Mvel3TestSupport.CompiledExpression expression = Mvel3TestSupport.compileExpression("x / 2", context);

        assertThat(Mvel3TestSupport.executeExpression(expression, Map.of("x", 3.0))).isEqualTo(1.5);
    }

    @Test
    void reusesCompiledExpressionWithUpdatedVariables() {
        final Mvel3TestSupport.CompiledExpression expression = Mvel3TestSupport.compileExpression("x + 1");
        final Map<String, Object> variables = new HashMap<>();
        variables.put("x", 1);
        assertThat(Mvel3TestSupport.executeExpression(expression, variables)).isEqualTo(2);

        variables.put("x", 7);
        assertThat(Mvel3TestSupport.executeExpression(expression, variables)).isEqualTo(8);
    }

    @Test
    void retainsClassImports() {
        final TestCompilerContext context = new TestCompilerContext();
        context.addImport(BigDecimal.class);
        final Mvel3TestSupport.CompiledExpression expression =
                Mvel3TestSupport.compileExpression("new BigDecimal(1)", context);

        assertThat(Mvel3TestSupport.executeExpression(expression)).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void retainsClassImportsFromConfiguration() {
        final TestCompilerContext configuration = new TestCompilerContext();
        configuration.addImport(BigDecimal.class);
        final TestCompilerContext context = new TestCompilerContext(configuration);
        final Mvel3TestSupport.CompiledExpression expression =
                Mvel3TestSupport.compileExpression("new BigDecimal(1)", context);

        assertThat(Mvel3TestSupport.executeExpression(expression)).isEqualTo(BigDecimal.ONE);
    }
}
