package org.mvel.tests.perftests;

// import ognl.Ognl;

import org.apache.commons.el.ExpressionEvaluatorImpl;
import org.mvel.MVEL;

import javax.servlet.jsp.el.Expression;

/**
 * @author Christopher Brock
 */
public class PerfTest {
    private String name;
    private String expression;
    private int runFlags;

    private Object ognlCompiled;
    private Object mvelCompiled;
    private Object groovyCompiled;

    private Expression elCompiled;
    private NativeTest javaNative;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public int getRunFlags() {
        return runFlags;
    }

    public void setRunFlags(int runFlags) {
        this.runFlags = runFlags;
    }


    public Object getOgnlCompiled() {
        return ognlCompiled;
    }

    public void setOgnlCompiled(Object ognlCompiled) {
        this.ognlCompiled = ognlCompiled;
    }

    public Object getMvelCompiled() {
        return mvelCompiled;
    }

    public void setMvelCompiled(Object mvelCompiled) {
        this.mvelCompiled = mvelCompiled;
    }

    public NativeTest getJavaNative() {
        return javaNative;
    }

    public void setJavaNative(NativeTest javaNative) {
        this.javaNative = javaNative;
    }

    public Expression getElCompiled() {
        return elCompiled;
    }

    public void setElCompiled(Expression elCompiled) {
        this.elCompiled = elCompiled;
    }


    public Object getGroovyCompiled() {
        return groovyCompiled;
    }

    public void setGroovyCompiled(Object groovyCompiled) {
        this.groovyCompiled = groovyCompiled;
    }

    public PerfTest(String name, String expression, int runFlags, NativeTest javaNative) {
        this.name = name;
        this.expression = expression;
        this.runFlags = runFlags;
        this.javaNative = javaNative;

        if ((runFlags & ELComparisons.RUN_MVEL) != 0)
            this.mvelCompiled = MVEL.compileExpression(expression);

        try {
            //         if ((runFlags & ELComparisons.RUN_OGNL) != 0)
            //            this.ognlCompiled = Ognl.parseExpression(expression);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
//            if ((runFlags & ELComparisons.RUN_GROOVY) != 0) {
//                Binding binding = new Binding();
//                GroovyShell sh = new GroovyShell(binding);
//                this.groovyCompiled = sh.parse(expression);
//            }

//            this.ognlCompiled = Ognl.parseExpression(expression);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        ExpressionEvaluatorImpl factory = new ExpressionEvaluatorImpl();

        try {
            if ((runFlags & ELComparisons.RUN_COMMONS_EL) != 0)
                this.elCompiled = factory.parseExpression("${" + expression + "}", Object.class, null);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


}
