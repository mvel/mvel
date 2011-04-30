package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.util.VariableSpaceCompiler;
import org.mvel2.util.VariableSpaceModel;

import java.io.Serializable;

/**
 * @author Mike Brock .
 */
public class IndexedVariablesTests extends TestCase {
    public void testVariableInjection1() {
        String[] varNames = {"x", "y", "z"};
        Object[] values = {10, 20, 30};


        String expr = "foo = -1; res = x + y + z;\n" +
                "if (x > 9) {\n" +
                "   res = z - y - x;\n" +
                "   int k = 5;\n" +
                "   foo = k;" +
                "}; \n"
                + "for (i = 0; i < 100000; i++) { foo++; }; foo;";

        ParserContext ctx = ParserContext.create();
        ctx.addIndexedInput(varNames);
        ctx.setIndexAllocation(true);

        VariableSpaceModel model = VariableSpaceCompiler.compile(expr, ctx, values);

        Serializable s = MVEL.compileExpression(expr, ctx);

    //    IndexedVariableResolverFactory factory = new IndexedVariableResolverFactory(varNames, values);

        System.out.println(MVEL.executeExpression(s, model.createFactory()));
    }
}
