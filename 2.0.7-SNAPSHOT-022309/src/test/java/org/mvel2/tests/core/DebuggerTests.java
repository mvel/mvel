package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.MVEL;
import org.mvel2.MVELRuntime;
import org.mvel2.ParserContext;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExpressionCompiler;
import static org.mvel2.debug.DebugTools.decompile;
import org.mvel2.debug.Debugger;
import org.mvel2.debug.Frame;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.DefaultLocalVariableResolverFactory;

public class DebuggerTests extends TestCase {

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
        ExpressionCompiler c = new ExpressionCompiler(src);
      //  c.setDebugSymbols(true);
        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("mysource");
        ctx.setDebugSymbols(true);
        CompiledExpression compexpr = c.compile(ctx);

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

        ExpressionCompiler c = new ExpressionCompiler(src);
      //  c.setDebugSymbols(true);
        ParserContext ctx = new ParserContext();
        ctx.setSourceFile("mysource");
        ctx.setDebugSymbols(true);
        CompiledExpression compexpr = c.compile(ctx);

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

}