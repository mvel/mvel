package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.util.SharedVariableSpaceModel;
import org.mvel2.util.SimpleVariableSpaceModel;
import org.mvel2.util.VariableSpaceCompiler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
        + "for (i = 0; i < 5000; i++) { foo++; }; foo;";

    ParserContext ctx = ParserContext.create();
    ctx.addIndexedInput(varNames);
    ctx.setIndexAllocation(true);

    SharedVariableSpaceModel model = VariableSpaceCompiler.compileShared(expr, ctx, values);

    Serializable indexCompile = MVEL.compileExpression(expr, ctx);
    Serializable dynamicCompile = MVEL.compileExpression(expr, ParserContext.create());

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("x", 10);
    map.put("y", 20);
    map.put("z", 30);

    assertEquals(MVEL.executeExpression(dynamicCompile, map),
        MVEL.executeExpression(indexCompile, model.createFactory()));
//
//        for (int x = 0; x < 10; x++) {
//            long tm = System.currentTimeMillis();
//            for (int i = 0; i < 10000; i++) {
//                MVEL.executeExpression(indexCompile, model.createFactory());
//            }
//            tm = System.currentTimeMillis() - tm;
//            System.out.println("(StaticInjection (ms): " + tm + ")");
//
//            tm = System.currentTimeMillis();
//            Map<String, Object> map = new HashMap<String, Object>();
//            map.put("x", 10);
//            map.put("y", 20);
//            map.put("z", 30);
//
//            MapVariableResolverFactory factory = new MapVariableResolverFactory(map);
//            for (int i = 0; i < 10000; i++) {
//                MVEL.executeExpression(dynamicCompile, factory);
//            }
//            tm = System.currentTimeMillis() - tm;
//            System.out.println("(MapInjection    (ms): " + tm + ")");
//        }

  }

  public void testVariableInjection2() {
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

    SimpleVariableSpaceModel model = VariableSpaceCompiler.compile(expr, ctx);

    Serializable indexCompile = MVEL.compileExpression(expr, ctx);
    Serializable dynamicCompile = MVEL.compileExpression(expr, ParserContext.create());

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("x", 10);
    map.put("y", 20);
    map.put("z", 30);

    assertEquals(MVEL.executeExpression(dynamicCompile, map),
        MVEL.executeExpression(indexCompile, model.createFactory(values)));

  }

  public void testVariableInjection3() {
    String[] varNames = {"x", "y", "z"};
    Object[] values = {10, 20, 30};


    String expr = "def add(a,b) { a + b }; foo = -1; res = x + y + z;\n" +
        "if (x > 9) {\n" +
        "   res = z - y - x;\n" +
        "   int k = 5;\n" +
        "   foo = add(5,10);" +
        "}; \n"
        + "for (i = 0; i < 100000; i++) { foo++; }; foo;";

    ParserContext ctx = ParserContext.create();
    ctx.addIndexedInput(varNames);
    ctx.setIndexAllocation(true);

    SimpleVariableSpaceModel model = VariableSpaceCompiler.compile(expr, ctx);

    Serializable indexCompile = MVEL.compileExpression(expr, ctx);
    Serializable dynamicCompile = MVEL.compileExpression(expr, ParserContext.create());

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("x", 10);
    map.put("y", 20);
    map.put("z", 30);

    assertEquals(MVEL.executeExpression(dynamicCompile, map),
        MVEL.executeExpression(indexCompile, model.createFactory(values)));

  }
}
