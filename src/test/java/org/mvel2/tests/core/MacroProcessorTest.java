package org.mvel2.tests.core;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.mvel2.MVEL;
import org.mvel2.MVELRuntime;
import org.mvel2.Macro;
import org.mvel2.MacroProcessor;
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
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.tests.core.res.Foo;

import java.util.HashMap;
import java.util.Map;

import static org.mvel2.MVEL.executeExpression;
import static org.mvel2.MVEL.parseMacros;

public class MacroProcessorTest extends TestCase {

  private MacroProcessor macroProcessor;

  protected void setUp() throws Exception {
    super.setUp();
    Map<String, Macro> macros = new HashMap<String, Macro>();
    macros.put("insert",
        new Macro() {
          public String doMacro() {
            return "drools.insert";
          }
        });
    macroProcessor = new MacroProcessor();
    macroProcessor.setMacros(macros);
  }

  public void testParseString() {
    String raw = "    l.add( \"rule 2 executed \" + str);";
    try {
      String result = macroProcessor.parse(raw);
      assertEquals(raw, result);
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("there shouldn't be any exception: " + ex.getMessage());
    }
  }

  public void testParseConsequenceWithComments() {
    String raw = "    // str is null, we are just testing we don't get a null pointer \n " +
        "    list.add( p );";
    try {
      String result = macroProcessor.parse(raw);
      assertEquals(raw, result);
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("there shouldn't be any exception: " + ex.getMessage());
    }
  }

  public void testInfiniteLoop() {
    String str = "";
    str += "int insuranceAmt = caseRate + (charges * pctDiscount / 100);\n";
    str += "update (estimate); \n";

    Map<String, Macro> macros = new HashMap<String, Macro>();
    macros.put("update",
        new Macro() {
          public String doMacro() {
            return "drools.update";
          }
        });

    String result = parseMacros(str, macros);

    str = "";
    str += "int insuranceAmt = caseRate + (charges * pctDiscount / 100);\n";
    str += "drools.update (estimate);";

    assertEquals(str, result);
  }

  public void testMacroSupport() {
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("foo", new Foo());

    Map<String, Interceptor> interceptors = new HashMap<String, Interceptor>();
    Map<String, Macro> macros = new HashMap<String, Macro>();

    interceptors.put("Modify", new Interceptor() {
      public int doBefore(ASTNode node, VariableResolverFactory factory) {
        ((WithNode) node).getNestedStatement().getValue(null,
            factory);
        factory.createVariable("mod", "FOOBAR!");
        return 0;
      }

      public int doAfter(Object val, ASTNode node, VariableResolverFactory factory) {
        return 0;
      }
    });

    macros.put("modify", new Macro() {
      public String doMacro() {
        return "@Modify with";
      }
    });

    ParserContext ctx = new ParserContext(null, interceptors, null);
    ctx.setSourceFile("test.mv");
    ctx.setDebugSymbols(true);

    ExpressionCompiler compiler = new ExpressionCompiler(parseMacros("modify (foo) { aValue = 'poo = poo', bValue = 'poo, poo' }; mod", macros), ctx);
    assertEquals("FOOBAR!", executeExpression(compiler.compile(), null, vars));
  }


  public void testMacroSupportWithStrings() {
    Map<String, Object> vars = new HashMap<String, Object>();
    Foo foo = new Foo();
    vars.put("foo", foo);

    Map<String, Macro> macros = new HashMap<String, Macro>();

    macros.put("modify", new Macro() {
      public String doMacro() {
        return "drools.modify";
      }
    });

    assertEquals("", foo.aValue);

    ParserContext ctx = new ParserContext(null, null, null);
    ctx.setSourceFile("test.mv");
    ctx.setDebugSymbols(true);

    ExpressionCompiler compiler = new ExpressionCompiler(parseMacros("\"This is an modify()\"", macros), ctx);
    assertEquals("This is an modify()", executeExpression(compiler.compile(), null, vars));
  }


  public void testMacroSupportWithDebugging() {
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("foo", new Foo());

    Map<String, Interceptor> interceptors = new HashMap<String, Interceptor>();
    Map<String, Macro> macros = new HashMap<String, Macro>();

    interceptors.put("Modify", new Interceptor() {
      public int doBefore(ASTNode node, VariableResolverFactory factory) {
        ((WithNode) node).getNestedStatement().getValue(null,
            factory);

        factory.createVariable("mod", "FOOBAR!");


        return 0;
      }

      public int doAfter(Object val, ASTNode node, VariableResolverFactory factory) {
        return 0;
      }
    });

    macros.put("modify", new Macro() {
      public String doMacro() {
        return "@Modify with";
      }
    });

    ParserContext ctx = new ParserContext(null, interceptors, null);
    ctx.setSourceFile("test.mv");
    ctx.setDebugSymbols(true);

    ExpressionCompiler compiler = new ExpressionCompiler(
        parseMacros(
            "System.out.println('hello');\n" +
                "System.out.println('bye');\n" +
                "modify (foo) { aValue = 'poo', \n" +
                " aValue = 'poo' };\n mod", macros)
    , ctx);
    // compiler.setDebugSymbols(true);

    CompiledExpression compiled = compiler.compile();

    MVELRuntime.setThreadDebugger(new Debugger() {

      public int onBreak(Frame frame) {
        System.out.println(frame.getSourceName() + ":" + frame.getLineNumber());

        return Debugger.STEP;
      }
    });

    MVELRuntime.registerBreakpoint("test.mv", 3);

    System.out.println(DebugTools.decompile(compiled
    ));

    Assert.assertEquals("FOOBAR!", MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(vars)));
  }

  public void testParseStringUnmatchedChars() {
    String raw = "result.add( \"\\\"\\\' there are } [ unmatched characters in this string (\"  );";
    try {
      String result = macroProcessor.parse(raw);
      assertEquals(raw, result);
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("there shouldn't be any exception: " + ex.getMessage());
    }
  }

  public void testParseConsequenceWithFlowControlBlocks() {
    String raw = "    // str is null, we are just testing we don't get a null pointer \n " +
        "     if (l.x < 1)  {\n" +
        "        insert( new RuleLink(\"FIRST.INLET\" , comp, comp) );\n" +
        "     } else {\n" +
        "        insert( new RuleLink(\"FIRST.INLET.NOT\" , comp, comp) );\n" +
        "     }\n" +
        "    if( 1 < 2 ) { \n" +
        "        insert( p ); \n" +
        "    } else { \n" +
        "        while( true ) {insert(x);}\n" +
        "    }";
    String expected = "    // str is null, we are just testing we don't get a null pointer \n " +
        "     if (l.x < 1)  {\n" +
        "        drools.insert( new RuleLink(\"FIRST.INLET\" , comp, comp) );\n" +
        "     } else {\n" +
        "        drools.insert( new RuleLink(\"FIRST.INLET.NOT\" , comp, comp) );\n" +
        "     }\n" +
        "    if( 1 < 2 ) { \n" +
        "        drools.insert( p ); \n" +
        "    } else { \n" +
        "        while( true ) {drools.insert(x);}\n" +
        "    }";

    try {
      String result = macroProcessor.parse(raw);
      assertEquals(expected, result);
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("there shouldn't be any exception: " + ex.getMessage());
    }
  }

  public void testCommentParsingWithMacro() {
    String raw = "/** This is a block comment **/ insert /** This is a second \n\nblock comment insert **/";
    String expected = "/** This is a block comment **/ drools.insert /** This is a second \n\nblock comment insert **/";

    try {
      String result = macroProcessor.parse(raw);
      assertEquals(expected, result);
    }
    catch (Exception ex) {
      ex.printStackTrace();
      fail("there shouldn't be any exception: " + ex.getMessage());
    }
  }
}
