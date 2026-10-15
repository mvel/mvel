package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.Mvel3TestSupport.CompiledExpression;
import org.mvel3.compat.mvel2.Mvel3TestSupport.ExpressionCompiler;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.compiler.MvelCompileExpNullSafeTest (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 *
 * <p>Adaptations: the MVEL2 {@code BaseMvelTest} (optimizer reset) is removed;
 * {@code new ExpressionCompiler(expression, true)} (the {@code true} is MVEL2's
 * "verifying" flag) becomes {@code new ExpressionCompiler(expression)}.
 * Not imported: {@code testCustomResolverFactory} and its {@code MyResolverFactory}
 * fixture (MVEL2-only {@code VariableResolverFactory} API).
 */
@Tag("mvel2-compatibility")
public class MvelCompileExpNullSafeTest {

	// Category not isolated: fails with a parse error "missing ')' at '&&'" (verified). The projection `($ in list if ..)` alone also fails
	// to parse (verified), but without the outer parentheses the expression compiles and silently returns `true` even for `.size() > 5`
	// (verified), so the projection is not simply a parse error. The null-safe `.?` (see null-safe-operator) was not tested separately here.
	@Test
	@KnownCompatibility("projection-operator")
	public void testMvelCompileNullSafeNullFirst() {
        String expression = "((parentGroup != null) && ($.?child.firstName in parentGroup.parentList if  $.?child.firstName != null).size() > 0)";
        ExpressionCompiler compiler = new ExpressionCompiler(expression);
        CompiledExpression compiledExpression = compiler.compile();

    	ParentGroup pGroup = new ParentGroup();
    	List<Parent> list = new ArrayList<Parent>();
    	Parent parent = new Parent();
    	parent.setChild(null);
    	list.add(parent);
    	pGroup.setParentList(list);

    	Boolean result = (Boolean)executeExpression(compiledExpression, Collections.<String, Object>singletonMap("parentGroup", pGroup));
    	assert result == false;

    	Child child = new Child();
    	child.setFirstName("vlaa");

    	pGroup = new ParentGroup();
    	list = new ArrayList<Parent>();
    	parent = new Parent();
    	parent.setChild(child);
    	list.add(parent);
    	child = new Child();
    	child.setFirstName(null);
    	parent = new Parent();
    	parent.setChild(child);
    	list.add(parent);
    	pGroup.setParentList(list);

    	result = (Boolean)executeExpression(compiledExpression, Collections.<String, Object>singletonMap("parentGroup", pGroup));
    	assert result == true;
	}

	// Category not isolated: same as testMvelCompileNullSafeNullFirst.
	@Test
	@KnownCompatibility("projection-operator")
	public void testMvelCompileNullSafeNullSecond() {
        String expression = "((parentGroup != null) && ($.?child.firstName in parentGroup.parentList if  $.?child.firstName != null).size() > 0)";
        ExpressionCompiler compiler = new ExpressionCompiler(expression);
        CompiledExpression compiledExpression = compiler.compile();

    	Child child = new Child();
    	child.setFirstName("vlaa");

    	ParentGroup pGroup = new ParentGroup();
    	List<Parent> list = new ArrayList<Parent>();
    	Parent parent = new Parent();
    	parent.setChild(child);
    	list.add(parent);
    	child = new Child();
    	child.setFirstName(null);
    	parent = new Parent();
    	parent.setChild(child);
    	list.add(parent);
    	pGroup.setParentList(list);

    	Boolean result = (Boolean)executeExpression(compiledExpression, Collections.<String, Object>singletonMap("parentGroup", pGroup));
    	assert result == true;

    	pGroup = new ParentGroup();
    	list = new ArrayList<Parent>();
    	parent = new Parent();
    	parent.setChild(null);
    	list.add(parent);
    	pGroup.setParentList(list);

    	result = (Boolean)executeExpression(compiledExpression, Collections.<String, Object>singletonMap("parentGroup", pGroup));
    	assert result == false;

	}

	/**
	 * Tests NullSafe with a segment which shadows a variable.
	 */
	@Test
	@KnownCompatibility("null-safe-operator")
	public void testNullSafeVariableOverlap() {
		String expression = "foo.?bar.baz"; // "baz" shadows the variable named "baz".

		final String expectedResult = "You got me!";

		Map<String, Object> foo = new HashMap<>();
		Map<String, Object> ctx = Collections.singletonMap("foo", foo);
		Map<String, Object> bar = Collections.singletonMap("baz", expectedResult);
		foo.put("bar", null); // start with null, as this will cause NullSafe to be added to the AST.

		// This variable overlaps the segment to be resolved next after NullSafe.
		Map<String, Object> vars = Collections.singletonMap("baz", "no, over here!");

		Serializable compiledExpr = Mvel3TestSupport.compileExpression(expression);
		Assert.assertNull(executeExpression(compiledExpr, ctx, vars));

		foo.put("bar", bar);
		Assert.assertEquals(expectedResult, executeExpression(compiledExpr, ctx, vars));
	}

	@Test
	@KnownCompatibility("null-safe-operator")
	public void testNullSafeWithVariableAsMapKey() {
		String expression = "foo.?bar[myvar]";

		final String expectedResult = "You got me!";

		Map<String, Object> foo = new HashMap<>();
		Map<String, Object> ctx = Collections.singletonMap("foo", foo);
		Map<String, Object> bar = Collections.singletonMap("baz", expectedResult);
		foo.put("bar", null); // start with null, as this will cause NullSafe to be added to the AST.

		Map<String, Object> vars = new HashMap<>();
		vars.put("myvar", "baz");

		Serializable compiledExpr = Mvel3TestSupport.compileExpression(expression);
		Assert.assertNull(executeExpression(compiledExpr, ctx, vars));

		foo.put("bar", bar);
		Assert.assertEquals(expectedResult, executeExpression(compiledExpr, ctx, vars));
	}

	@Test
	@KnownCompatibility("null-safe-operator")
	public void testNullSafeWithVariableAsMethodArg() {
		String expression = "foo.?bar.convert(myvar)";

		Map<String, Object> foo = new HashMap<>();
		Map<String, Object> ctx = Collections.singletonMap("foo", foo);
		foo.put("bar", null); // start with null, as this will cause NullSafe to be added to the AST.

		Map<String, Object> vars = new HashMap<>();
		vars.put("myvar", "hello");

		Serializable compiledExpr = Mvel3TestSupport.compileExpression(expression);
		Assert.assertNull(executeExpression(compiledExpr, ctx, vars));

		foo.put("bar", new StringConverter());
		Assert.assertEquals("HELLO", executeExpression(compiledExpr, ctx, vars));
	}

	public class StringConverter {
		public String convert(String value) {
			return value.toUpperCase();
		}
	}

	public class Child {
		private String firstName;
		private String lastName;

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
	}

    public class Parent {
    	private Child child;

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}
    }

    public class ParentGroup {
    	private List<Parent> parentList;

		public List<Parent> getParentList() {
			return parentList;
		}

		public void setParentList(List<Parent> parentList) {
			this.parentList = parentList;
		}

    }
}
