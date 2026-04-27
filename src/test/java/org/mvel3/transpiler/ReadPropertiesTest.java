package org.mvel3.transpiler;

import org.junit.jupiter.api.Test;
import org.mvel3.ClassManager;
import org.mvel3.CompilerParameters;
import org.mvel3.Evaluator;
import org.mvel3.MVEL;
import org.mvel3.transpiler.context.Declaration;

import static org.assertj.core.api.Assertions.assertThat;

class ReadPropertiesTest {

    /**
     * Mirrors DRLX's compilation pattern: every JavaBeans property of the
     * pattern type is registered as an explicit Declaration so that bare
     * identifiers in the expression resolve against them.
     */
    private Evaluator<Object, Void, Boolean> compile(String expression) {
        CompilerParameters<Object, Void, Boolean> info = MVEL.pojo(ReadPropsFixture.class,
                        Declaration.of("salary", int.class),
                        Declaration.of("basePay", int.class),
                        Declaration.of("active", boolean.class))
                .<Boolean>out(Boolean.class)
                .expression(expression)
                .classManager(new ClassManager())
                .build();
        return new MVEL().compilePojoEvaluator(info);
    }

    @Test
    void bareNameExpr_collectsProperty() {
        Evaluator<Object, Void, Boolean> ev = compile("salary > 0");
        assertThat(ev.getReadProperties()).containsExactlyInAnyOrder("salary");
    }

    @Test
    void multipleProperties_collectsAll() {
        Evaluator<Object, Void, Boolean> ev = compile("salary > 0 && basePay == 1000");
        assertThat(ev.getReadProperties()).containsExactlyInAnyOrder("salary", "basePay");
    }

    @Test
    void noProperties_returnsEmpty() {
        Evaluator<Object, Void, Boolean> ev = compile("1 == 1");
        assertThat(ev.getReadProperties()).isEmpty();
    }
}
