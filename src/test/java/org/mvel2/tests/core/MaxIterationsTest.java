package org.mvel2.tests.core;

import static org.mvel2.MVEL.executeExpression;

import java.util.HashMap;
import java.util.Map;

import org.mvel2.ParserContext;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

public class MaxIterationsTest extends AbstractTest {

	
	public void testInfiniteWhile(){
		
		ExpressionCompiler compiler = new ExpressionCompiler("while(true){}");
		ParserContext ctx = new ParserContext();
		ctx.setMaxLoopIterationsBeforeExit(50);
		CompiledExpression ce = compiler.compile();
		
		try{
			executeExpression(ce,ctx);
		}catch(RuntimeException ex){
			assertEquals(ex.getMessage(), "Loop Iterations Count Exceeded");
		}
	}
	
	public void testInfiniteUntil(){
		
		ExpressionCompiler compiler = new ExpressionCompiler("until(false){}");
		ParserContext ctx = new ParserContext();
		ctx.setMaxLoopIterationsBeforeExit(50);
		CompiledExpression ce = compiler.compile(ctx);
		
		try{
			executeExpression(ce,ctx);
		}catch(RuntimeException ex){
			assertEquals(ex.getMessage(), "Loop Iterations Count Exceeded");
		}
	}	
	
	public void testInfiniteDoUntil(){
		
		ExpressionCompiler compiler = new ExpressionCompiler("do{} until(false);");
		ParserContext ctx = new ParserContext();
		ctx.setMaxLoopIterationsBeforeExit(50);
		CompiledExpression ce = compiler.compile(ctx);
		
		try{
			executeExpression(ce,ctx);
		}catch(RuntimeException ex){
			assertEquals(ex.getMessage(), "Loop Iterations Count Exceeded");
		}
	}	
	
	public void testInfiniteDoWhile(){
		
		ExpressionCompiler compiler = new ExpressionCompiler("do{} while(true);");
		ParserContext ctx = new ParserContext();
		ctx.setMaxLoopIterationsBeforeExit(50);
		CompiledExpression ce = compiler.compile(ctx);
		
		try{
			executeExpression(ce,ctx);
		}catch(RuntimeException ex){
			assertEquals(ex.getMessage(), "Loop Iterations Count Exceeded");
		}
	}	
	
	public void testMaximumRange(){
		
	
		Map<String, Object> vars = new HashMap<String, Object>();
		VariableResolverFactory factory = new MapVariableResolverFactory(vars);
		
		
		ExpressionCompiler compiler = new ExpressionCompiler("idx = 0; while(idx < 50) { idx ++}; return idx;");
		ParserContext ctx = new ParserContext();
		ctx.setMaxLoopIterationsBeforeExit(50);
		CompiledExpression ce = compiler.compile();
		
		Number result = (Number) executeExpression(ce,ctx,factory);
		assertEquals(result.intValue(), 50);
	}		
	
	public void testLessThanMaximumRange(){
		
		
		Map<String, Object> vars = new HashMap<String, Object>();
		VariableResolverFactory factory = new MapVariableResolverFactory(vars);
		
		ExpressionCompiler compiler = new ExpressionCompiler("idx = 0; while(idx < 30) { idx ++}; return idx;");
		ParserContext ctx = new ParserContext();
		ctx.setMaxLoopIterationsBeforeExit(50);
		CompiledExpression ce = compiler.compile();
		
		Number result = (Number) executeExpression(ce,ctx,factory);
		assertEquals(result.intValue(), 30);
	}	
	
	public void testMoreThanMaximumRange(){
			
		Map<String, Object> vars = new HashMap<String, Object>();
		VariableResolverFactory factory = new MapVariableResolverFactory(vars);
		
		ExpressionCompiler compiler = new ExpressionCompiler("idx = 0; while(idx < 60) { idx ++}; return idx;");
		ParserContext ctx = new ParserContext();
		ctx.setMaxLoopIterationsBeforeExit(50);
		CompiledExpression ce = compiler.compile();
		
		try{
			executeExpression(ce,ctx,factory);
		}catch(RuntimeException ex){
			assertEquals(ex.getMessage(), "Loop Iterations Count Exceeded");
		}
	}	
}
