package org.mvel2.tests.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.mvel2.MVEL;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExpressionCompiler;

public class EmptyValueTests extends AbstractTest {

	public void testOrConditionIfPropertyNotExist() {
		String expression = "name == 'joy' || age > 10";
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("age", 15);
		CompiledExpression compiledExpression = new ExpressionCompiler(expression).compile();
		assertEquals(true, MVEL.executeExpression(compiledExpression, paramMap));
		assertEquals(true, MVEL.eval(expression, paramMap));
	}

	public void testOrConditionIfPropertyNotExistNested1() {
		String expression = "name.first == 'joy' || age.num > 10";
		Map<String, Object> numMap = new HashMap<>();
		numMap.put("num", 15);
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("age", numMap);
		CompiledExpression compiledExpression = new ExpressionCompiler(expression).compile();
		assertEquals(true, MVEL.executeExpression(compiledExpression, paramMap));
		assertEquals(true, MVEL.eval(expression, paramMap));
	}

	public void testOrConditionIfPropertyNotExistNested2() {
		String expression = "name.first.ab.ad.ajh == 'joy' || age.num > 10";
		Map<String, Object> numMap = new HashMap<>();
		numMap.put("num1", 15);
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("age", numMap);
		CompiledExpression compiledExpression = new ExpressionCompiler(expression).compile();
		assertEquals(false, MVEL.executeExpression(compiledExpression, paramMap));
		assertEquals(false, MVEL.eval(expression, paramMap));
	}

	public void testOrConditionIfPropertyNotExistNested3() {
		String expression = "name == 'joy' && age.num > 10";
		Map<String, Object> numMap = new HashMap<>();
		numMap.put("num1", 15);
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("age", numMap);
		CompiledExpression compiledExpression = new ExpressionCompiler(expression).compile();
		assertEquals(false, MVEL.executeExpression(compiledExpression, paramMap));
		assertEquals(false, MVEL.eval(expression, paramMap));
	}

	public void testOrConditionIfPropertyNotExistNested4() {
		String expression = "name == 'joy' && age.num > 10";
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("name", "joy");
		CompiledExpression compiledExpression = new ExpressionCompiler(expression).compile();
		assertEquals(false, MVEL.executeExpression(compiledExpression, paramMap));
		assertEquals(false, MVEL.eval(expression, paramMap));
	}

	public void testOrConditionIfPropertyNotExistNested5() {
		String expression = "name == 'joy' && age.num > 10";
		Map<String, Object> paramMap = null;
		CompiledExpression compiledExpression = new ExpressionCompiler(expression).compile();
		assertEquals(false, MVEL.executeExpression(compiledExpression, paramMap));
		assertEquals(false, MVEL.eval(expression, paramMap));
	}

	public void testOrConditionIfPropertyNotExistNested6() {
		String expression = "name == 'joy' && age.num > 10";
		Map<String, Object> numMap = new HashMap<>();
		numMap.put("num1", 15);
		Map<String, Object> paramMap = new HashMap<>();
		Serializable compiledExpression = MVEL.compileExpression(expression);
		assertEquals(false, MVEL.executeExpression(compiledExpression, paramMap));
		assertEquals(false, MVEL.evalToBoolean(expression, paramMap).booleanValue());
	}

	public void testReturnNullIfPropertyNotExist() {
		String expression = "age.name.first.ab.ad.ajh";
		Map<String, Object> numMap = new HashMap<>();
		numMap.put("name", "joy");
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("age", numMap);
		CompiledExpression compiledExpression = new ExpressionCompiler(expression).compile();
		assertEquals(null, MVEL.executeExpression(compiledExpression, paramMap));
		assertEquals(null, MVEL.eval(expression, paramMap));
	}

}
