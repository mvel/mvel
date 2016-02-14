package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.MVELRuntime;
import org.mvel2.Macro;
import org.mvel2.ParserContext;
import org.mvel2.ast.ASTNode;
import org.mvel2.ast.WithNode;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.debug.DebugTools;
import org.mvel2.debug.Debugger;
import org.mvel2.debug.Frame;
import org.mvel2.integration.Interceptor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.DefaultLocalVariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.tests.core.res.Cheese;
import org.mvel2.tests.core.res.Foo;
import org.mvel2.util.Make;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mvel2.MVEL.parseMacros;
import static org.mvel2.debug.DebugTools.decompile;

public class DebuggerTests extends AbstractTest {

  private static int count;
  private static int a1 = 0;
  private static int a4 = 0;

  public void testDebuggerInvoke() {
    count = 0;

    MVELRuntime.resetDebugger();
    MVELRuntime.setThreadDebugger(new Debugger() {
      public int onBreak(Frame frame) {
        if (frame.getFactory().isResolveable("a1")) {
          a1++;
        }
        if (frame.getFactory().isResolveable("a4")) {
          a4++;
          System.out.println("HEI " + frame.getLineNumber());
        }
        count++;
        return 0;
      }
    });

    String src = "a1=7;\na2=8;\na3=9;\na4=10;\na5=11;\na6=12;\na7=13;\na8=14;";
    ParserContext ctx = new ParserContext();
    ctx.setSourceFile("mysource");
    ctx.setDebugSymbols(true);
    ExpressionCompiler c = new ExpressionCompiler(src, ctx);
    CompiledExpression compexpr = c.compile();

    System.out.println(decompile(compexpr));

    MVELRuntime.registerBreakpoint(ctx.getSourceFile(), 1);
    MVELRuntime.registerBreakpoint(ctx.getSourceFile(), 3);
    MVELRuntime.registerBreakpoint(ctx.getSourceFile(), 7);

    VariableResolverFactory factory = new DefaultLocalVariableResolverFactory();
    MVEL.executeDebugger(compexpr, null, factory);

    System.out.println(a1);
    System.out.println(a4);
    System.out.println(count);
    assertEquals(2, a1);
    assertEquals(1, a4);   // test passes but the breakpoint should be received by line 7, not by line 3
    assertEquals(3, count); // three breakpoints FAILS
  }

  public void testDebuggerInvoke2() {
    count = 0;

    MVELRuntime.resetDebugger();
    MVELRuntime.setThreadDebugger(new Debugger() {
      public int onBreak(Frame frame) {
        count++;
        return 0;
      }
    });

    String src = "a1=7;\na2=8;\nSystem.out.println(\"h\");\nac=23;\nde=23;\nge=23;\ngef=34;";

    ParserContext ctx = new ParserContext();
    ctx.setSourceFile("mysource");
    ctx.setDebugSymbols(true);
    ExpressionCompiler c = new ExpressionCompiler(src, ctx);
    CompiledExpression compexpr = c.compile();

    System.out.println(decompile(compexpr));

    MVELRuntime.registerBreakpoint(ctx.getSourceFile(), 1);
    MVELRuntime.registerBreakpoint(ctx.getSourceFile(), 2);
    MVELRuntime.registerBreakpoint(ctx.getSourceFile(), 3);
    MVELRuntime.registerBreakpoint(ctx.getSourceFile(), 4);
    MVELRuntime.registerBreakpoint(ctx.getSourceFile(), 5);

    VariableResolverFactory factory = new DefaultLocalVariableResolverFactory();
    MVEL.executeDebugger(compexpr, null, factory);

    System.out.println(count);
    assertEquals(5, count);
  }

  public void testBreakpoints() {

    ParserContext ctx = new ParserContext();
    ctx.setSourceFile( "test.mv" );
    ctx.setDebugSymbols( true );

    ExpressionCompiler compiler = new ExpressionCompiler("a = 5;\nb = 5;\n\nif (a == b) {\n\nSystem.out.println('Good');\nreturn a + b;\n}\n", ctx);
    System.out.println("-------\n" + compiler.getExpression() + "\n-------\n");
    CompiledExpression compiled = compiler.compile();

    MVELRuntime.registerBreakpoint("test.mv", 7);

    final Set<Integer> breaked = new HashSet<Integer>();

    Debugger testDebugger = new Debugger() {
      public int onBreak(Frame frame) {
        System.out.println("Breakpoint [source:" + frame.getSourceName() + "; line:" + frame.getLineNumber() + "]");
        breaked.add(frame.getLineNumber());

        return 0;
      }
    };

    MVELRuntime.setThreadDebugger(testDebugger);

    assertEquals(10, MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(createTestMap())));
    assertTrue("did not break at line 7", breaked.contains(7));
  }

  public void testBreakpoints2() {

    ParserContext ctx = new ParserContext();
    ctx.setSourceFile("test.mv");
    ctx.setDebugSymbols(true);

    ExpressionCompiler compiler = new ExpressionCompiler("System.out.println('test the debugger');\n a = 0;", ctx);
    CompiledExpression compiled = compiler.compile();
  }

  public void testBreakpoints3() {
    String expr = "System.out.println( \"a1\" );\n" +
            "System.out.println( \"a2\" );\n" +
            "System.out.println( \"a3\" );\n" +
            "System.out.println( \"a4\" );\n";

    ParserContext context = new ParserContext();
    context.addImport("System", System.class);
    context.setStrictTypeEnforcement(true);
    context.setDebugSymbols(true);
    context.setSourceFile("mysource");

    ExpressionCompiler compiler = new ExpressionCompiler(expr, context);
    String s = org.mvel2.debug.DebugTools.decompile(compiler.compile());

    System.out.println("output: " + s);

    int fromIndex = 0;
    int count = 0;
    while ((fromIndex = s.indexOf("DEBUG_SYMBOL", fromIndex + 1)) > -1) {
      count++;
    }
    assertEquals(4, count);

  }

  public void testBreakpointsAcrossWith() {
    String line1 = "System.out.println( \"a1\" );\n";
    String line2 = "c = new Cheese();\n";
    String line3 = "with ( c ) { type = 'cheddar',\n" +
            "             price = 10 };\n";
    String line4 = "System.out.println( \"a1\" );\n";
    String expr = line1 + line2 + line3 + line4;

    System.out.println(expr);

    ParserContext context = new ParserContext();
    context.addImport("System", System.class);
    context.addImport("Cheese", Cheese.class);
    context.setStrictTypeEnforcement(true);
    context.setDebugSymbols(true);
    context.setSourceFile("mysource");

    ExpressionCompiler compiler = new ExpressionCompiler(expr, context);
    String s = org.mvel2.debug.DebugTools.decompile(compiler.compile());

    System.out.println("output: " + s);

    int fromIndex = 0;
    int count = 0;
    while ((fromIndex = s.indexOf("DEBUG_SYMBOL", fromIndex + 1)) > -1) {
      count++;
    }
    assertEquals(5, count);

  }

  public void testBreakpointsAcrossComments() {
    String expression = "/** This is a comment\n" +  // 1
            " *  Second comment line\n" +        // 2
            " *  Third Comment Line\n" +         // 3
            " */\n" +                         // 4
            "System.out.println('4');\n" +   // 5
            "System.out.println('5');\n" +   // 6
            "a = 0;\n" +                     // 7
            "b = 1;\n" +                    // 8
            "a + b";                        // 9

    ParserContext ctx = new ParserContext();
    ctx.setSourceFile("test2.mv");
    ctx.setDebugSymbols( true );

    ExpressionCompiler compiler = new ExpressionCompiler(expression, ctx);

    System.out.println( "Expression:\n------------");
    System.out.println( expression);
    System.out.println( "------------");

    CompiledExpression compiled = compiler.compile();

    MVELRuntime.registerBreakpoint("test2.mv", 9);

    final Set<Integer> linesEncountered = new HashSet<Integer>();

    Debugger testDebugger = new Debugger() {

      public int onBreak(Frame frame) {
        linesEncountered.add(frame.getLineNumber());

        System.out.println("Breakpoint Encountered [source:" + frame.getSourceName() + "; line:" + frame.getLineNumber() + "]");
        System.out.println("vars:" + frame.getFactory().getKnownVariables());
        System.out.println("Resume Execution");
        return 0;
      }
    };

    MVELRuntime.setThreadDebugger(testDebugger);

    assertEquals(1, MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(createTestMap())));
    assertTrue("Debugger did not break at line 9", linesEncountered.contains(9));
  }

  public void testBreakpointsAcrossComments2() {
    ParserContext ctx = new ParserContext();
    ctx.setSourceFile("test2.mv");
    ctx.setDebugSymbols(true);

    ExpressionCompiler compiler = new ExpressionCompiler(
            "// This is a comment\n" +                  // 1
                    "//Second comment line\n" +         // 2
                    "//Third Comment Line\n" +          // 3
                    "\n" +                              // 4
                    "//Test\n" +                        // 5
                    "System.out.println('4');\n" +      // 6
                    "//System.out.println('5'); \n" +    // 7
                    "a = 0;\n" +                        // 8
                    "b = 1;\n" +                        // 9
                    " a + b", ctx);                          // 10


    CompiledExpression compiled = compiler.compile();

    MVELRuntime.registerBreakpoint("test2.mv", 6);
    MVELRuntime.registerBreakpoint("test2.mv", 8);
    MVELRuntime.registerBreakpoint("test2.mv", 9);
    MVELRuntime.registerBreakpoint("test2.mv", 10);

    final Set<Integer> breaked = new HashSet<Integer>();

    Debugger testDebugger = new Debugger() {
      public int onBreak(Frame frame) {
        System.out.println("Breakpoint [source:" + frame.getSourceName() + "; line:" + frame.getLineNumber() + "]");
        breaked.add(frame.getLineNumber());
        return 0;
      }
    };

    MVELRuntime.setThreadDebugger(testDebugger);

    assertEquals(1, MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(createTestMap())));
    assertEquals("did not break at expected lines", Make.Set.<Integer>$()._(6)._(8)._(9)._(10)._(), breaked);
  }

  public void testBreakpoints4() {
    String expression = "System.out.println('foo');\n" +
            "a = new Foo244();\n" +
            "update (a) { name = 'bar' };\n" +
            "System.out.println('name:' + a.name);\n" +
            "return a.name;";


    Map<String, Interceptor> interceptors = new HashMap<String, Interceptor>();
    Map<String, Macro> macros = new HashMap<String, Macro>();

    class TestResult {
      boolean firedBefore;
      boolean firedAfter;
    }

    final TestResult result = new TestResult();

    interceptors.put("Update", new Interceptor() {
      public int doBefore(ASTNode node, VariableResolverFactory factory) {
        ((WithNode) node).getNestedStatement().getValue(null,
                factory);
        System.out.println("fired update interceptor -- before");
        result.firedBefore = true;
        return 0;
      }

      public int doAfter(Object val, ASTNode node, VariableResolverFactory factory) {
        System.out.println("fired update interceptor -- after");
        result.firedAfter = true;
        return 0;
      }
    });

    macros.put("update", new Macro() {
      public String doMacro() {
        return "@Update with";
      }
    });

    expression = parseMacros(expression, macros);

    ParserContext ctx = new ParserContext();
    ctx.setDebugSymbols(true);
    ctx.setSourceFile("test2.mv");
    ctx.addImport("Foo244", Foo.class);
    ctx.setInterceptors(interceptors);

    ExpressionCompiler compiler = new ExpressionCompiler(expression, ctx);
    CompiledExpression compiled = compiler.compile();

    System.out.println("\nExpression:------------");
    System.out.println(expression);
    System.out.println("------------");

    MVELRuntime.registerBreakpoint("test2.mv", 3);
    MVELRuntime.registerBreakpoint("test2.mv", 4);
    MVELRuntime.registerBreakpoint("test2.mv", 5);

    final Set<Integer> breaked = new HashSet<Integer>();

    Debugger testDebugger = new Debugger() {
      public int onBreak(Frame frame) {
        System.out.println("Breakpoint [source:" + frame.getSourceName() + "; line:" + frame.getLineNumber() + "]");
        breaked.add(frame.getLineNumber());
        return 0;
      }
    };


    MVELRuntime.setThreadDebugger(testDebugger);

    assertEquals("bar", MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(createTestMap())));
    assertTrue("did not fire before", result.firedBefore);
    assertTrue("did not fire after", result.firedAfter);
    assertEquals("did not break at expected points", Make.Set.<Integer>$()._(3)._(4)._(5)._(), breaked);
  }

  public void testBreakpoints5() {
    OptimizerFactory.setDefaultOptimizer("ASM");
    String expression = "System.out.println('foo');\r\n" +
            "a = new Foo244();\r\n" +
            "a.name = 'bar';\r\n" +
            "foo.happy();\r\n" +
            "System.out.println( 'name:' + a.name );               \r\n" +
            "System.out.println( 'name:' + a.name );         \r\n" +
            "System.out.println( 'name:' + a.name );     \r\n" +
            "return a.name;";

    Map<String, Interceptor> interceptors = new HashMap<String, Interceptor>();
    Map<String, Macro> macros = new HashMap<String, Macro>();

    expression = parseMacros(expression, macros);

    ParserContext ctx = new ParserContext();
    ctx.setSourceFile("test2.mv");
    ctx.setDebugSymbols(true);
    ctx.addImport("Foo244", Foo.class);
    ctx.setInterceptors(interceptors);

    ExpressionCompiler compiler = new ExpressionCompiler(expression, ctx);
    CompiledExpression compiled = compiler.compile();

    System.out.println("\nExpression:------------");
    System.out.println(expression);
    System.out.println("------------");

    System.out.println(DebugTools.decompile(compiled));
    MVELRuntime.registerBreakpoint("test2.mv", 1);

    final Set<Integer> breaked = new HashSet<Integer>();

    Debugger testDebugger = new Debugger() {
      public int onBreak(Frame frame) {
        System.out.println("Breakpoint [source:" + frame.getSourceName() + "; line:" + frame.getLineNumber() + "]");
        breaked.add(frame.getLineNumber());
        return Debugger.STEP_OVER;
      }
    };

    MVELRuntime.setThreadDebugger(testDebugger);

    System.out.println("\n==RUN==\n");

    assertEquals("bar", MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(createTestMap())));
    assertTrue("did not break at line 1", breaked.contains(1));

  }

  public void testDebugSymbolsWithWindowsLinedEndings() throws Exception {
    String expr = "   System.out.println( \"a1\" );\r\n" +
            "   System.out.println( \"a2\" );\r\n" +
            "   System.out.println( \"a3\" );\r\n" +
            "   System.out.println( \"a4\" );\r\n";

    ParserContext ctx = new ParserContext();
    ctx.setStrictTypeEnforcement(true);
    ctx.setDebugSymbols(true);
    ctx.setSourceFile("mysource");

    ExpressionCompiler compiler = new ExpressionCompiler(expr, ctx);
    String s = org.mvel2.debug.DebugTools.decompile(compiler.compile());

    System.out.println(s);

    int fromIndex = 0;
    int count = 0;
    while ((fromIndex = s.indexOf("DEBUG_SYMBOL", fromIndex + 1)) > -1) {
      count++;
    }
    assertEquals(4, count);
  }

  public void testDebugSymbolsWithUnixLinedEndings() throws Exception {
    String expr = "   System.out.println( \"a1\" );\n" +
            "   System.out.println( \"a2\" );\n" +
            "   System.out.println( \"a3\" );\n" +
            "   System.out.println( \"a4\" );\n";

    ParserContext ctx = new ParserContext();
    ctx.setStrictTypeEnforcement(true);
    ctx.setDebugSymbols(true);
    ctx.setSourceFile("mysource");

    ExpressionCompiler compiler = new ExpressionCompiler(expr, ctx);
    String s = org.mvel2.debug.DebugTools.decompile(compiler.compile());

    int fromIndex = 0;
    int count = 0;
    while ((fromIndex = s.indexOf("DEBUG_SYMBOL", fromIndex + 1)) > -1) {
      count++;
    }
    assertEquals(4, count);

  }

  public void testDebugSymbolsWithMixedLinedEndings() throws Exception {
    String expr = "   System.out.println( \"a1\" );\n" +
            "   System.out.println( \"a2\" );\r\n" +
            "   System.out.println( \"a3\" );\n" +
            "   System.out.println( \"a4\" );\r\n";

    ParserContext ctx = new ParserContext();
    ctx.setStrictTypeEnforcement(true);
    ctx.setDebugSymbols(true);
    ctx.setSourceFile("mysource");

    ExpressionCompiler compiler = new ExpressionCompiler(expr, ctx);
    String s = org.mvel2.debug.DebugTools.decompile(compiler.compile());

    System.out.println(s);

    int fromIndex = 0;
    int count = 0;
    while ((fromIndex = s.indexOf("DEBUG_SYMBOL", fromIndex + 1)) > -1) {
      count++;
    }
    assertEquals(4, count);

  }

  public void testDebugSymbolsSingleStatement() {
    String ex = "System.out.println( Cheese.STILTON );";
    ParserContext ctx = new ParserContext();
    ctx.setStrongTyping(true);
    ctx.addImport(Cheese.class);
    try {
      ExpressionCompiler compiler = new ExpressionCompiler(ex, ctx);
      CompiledExpression expr = compiler.compile();

      // executing the following line with a MVEL.executeExpression() works fine
      // but executeDebugger() fails
      MVEL.executeDebugger(expr, null, (VariableResolverFactory) null);
    }
    catch (Throwable e) {
      e.printStackTrace();
      fail("Should not raise exception: " + e.getMessage());
    }
  }


}