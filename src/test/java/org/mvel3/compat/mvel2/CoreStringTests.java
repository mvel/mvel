package org.mvel3.compat.mvel2;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.Cheese;
import org.mvel3.compat.mvel2.res.Foo;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from the string tests of org.mvel2.tests.core.CoreConfidenceTests (mvel2 branch): string literals and
 * escapes, string concatenation, string indexing and calls of {@code String} methods.
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged where MVEL3 has an equivalent. Failures are intentionally visible;
 * known behavioural differences are annotated with {@link KnownCompatibility}.
 *
 * <p>Not imported, because they use MVEL2-only APIs inside the expression string: {@code testRandomExpression5}
 * and {@code testRandomExpression6} (call {@code org.mvel2.MVEL.*} dynamically);
 * {@code testStrDoubleEqualsEquals} (commented out in MVEL2).
 *
 * <p>Adaptations: {@code ParserContext}/{@code ParserConfiguration} become {@link TestCompilerContext};
 * strong typing / strict type enforcement flags and optimizer selection are removed. The call
 * {@code MVEL.setProperty(foo, "name", 5)} of {@code testRandomSomething} is removed. The adapter's
 * {@code compileExpression} is lazy and does not compile, so the tests that only compile an expression use
 * {@link #compileEagerly}, which compiles at once. The method import of {@code testStringConcatenation3}
 * ({@code ParserConfiguration.addImport(name, Method)}) becomes a static import.
 */
@Tag("mvel2-compatibility")
public class CoreStringTests extends AbstractTest {

  /** Compiles at once (the adapter's compileExpression only creates a lazy carrier). */
  private static void compileEagerly(final String expression, final TestCompilerContext ctx) {
    compileEagerly(expression, ctx, new LinkedHashSet<>());
  }

  private static void compileEagerly(final String expression, final TestCompilerContext ctx,
                                     final Set<String> extraImports) {
    final Map<String, Type<?>> types = new LinkedHashMap<>();
    ctx.getInputs().forEach((name, type) -> types.put(name, Type.type(type)));
    final Set<String> imports = new LinkedHashSet<>(extraImports);
    ctx.getImports().forEach(type -> imports.add(type.getCanonicalName()));
    ctx.getPackageImports().forEach(packageName -> imports.add(packageName + ".*"));
    if (expression.indexOf(';') > 0) {
      new MVEL().compileMapBlock(expression, Object.class, imports, types);
    } else {
      new MVEL().compileMapExpression(expression, Object.class, imports, types);
    }
  }

  public static interface Services {
    public final static String A_CONST = "Hello World";

    public void log(String text);
  }

  // Category not isolated: `'1)your guess was:'` is read as a char literal (JavaParser null-node NPE); the void
  // call `System.out.println(..);` as the last statement also fails with "missing return statement"
  // (void-method-expression). Not tested with a double-quoted string and a trailing `return`.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testBracketInString() {
    test("System.out.println('1)your guess was:');");
  }

  // The trailing `;` makes the expression a block, which needs `return` ("missing return statement"). Without
  // the `;` the expression is not a block.
  @Test
  @KnownCompatibility("block-syntax")
  public void testNesting() {
    assertEquals("foo",
        test("new String(new String(new String(\"foo\")));"));
  }

  // Category not isolated: `new String('foo')` fails with a parse error at the first `;`; `list = ..`
  // (undeclared local, block-syntax), `list` clashing with the `list` map input (input-variable-redeclaration)
  // and `list[0]` without `return` may also contribute. Not tested one at a time.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testEdson() {
    assertEquals("foo",
        test("list = new java.util.ArrayList(); list.add(new String('foo')); list[0]"));
  }

  // Category not isolated: `'foobar'` fails with a null-reference error; `a[4]` on a String becomes `.get(4)`
  // (string-index-access), `a` clashes with the `a` map input (input-variable-redeclaration) and the block
  // needs `return` (block-syntax). Not tested one at a time.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testStringIndex() {
    assertEquals(true,
        test("a = 'foobar'; a[4] == 'a'"));
  }

  @Test
  public void testStringEscape() {
    String expression = "[\"a\\\"b\"]";
    Serializable compiledExpression = compileExpression(expression);
    List result = (List) executeExpression(compiledExpression);
    assertEquals(1, result.size());
    assertEquals("a\"b", result.get(0));
  }

  @Test
  public void testConcatWithLineBreaks() {
    TestCompilerContext ctx = new TestCompilerContext();

    Mvel3TestSupport.ExpressionCompiler parser =
        new Mvel3TestSupport.ExpressionCompiler("\"foo\"+\n\"bar\"", ctx);
    assertEquals("foobar",
        executeExpression(parser.compile()));
  }

  // Confirmed: `a.explanation = ..` becomes a void setter call; with `; return null;` appended it works.
  @Test
  @KnownCompatibility("property-assign-void-setter")
  public void testComaProblemStrikesBack() {
    String ex = "a.explanation = \"There is a coma, in here\"";

    TestCompilerContext ctx = new TestCompilerContext();
    Mvel3TestSupport.ExpressionCompiler compiler = new Mvel3TestSupport.ExpressionCompiler(ex, ctx);
    Serializable s = compiler.compile();

    Base a = new Base();
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("a",
        a);

    executeExpression(s,
        variables);
    assertEquals("There is a coma, in here",
        a.data);
  }

  // Confirmed: with `; return null;` appended, the same expression compiles.
  @Test
  @KnownCompatibility("void-method-expression")
  public void testStringConcatenation() {
    // debugging MVEL code, it seems that MVEL 'thinks' that the result of the expression:
    // "Drop +5%: "+$sb+" avg: $"+$av+" price: $"+$pr
    // is a double, and as so, he looks for a method:
    // Services.log( double );
    // but finds only:
    // Services.log( String );
    // raising the error.
    String ex = "services.log((String) \"Drop +5%: \"+$sb+\" avg: $\"+$av+\" price: $\"+$pr );";
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("$sb",
        String.class);
    ctx.addInput("$av",
        double.class);
    ctx.addInput("$pr",
        double.class);
    ctx.addInput("services",
        Services.class);
    try {
      compileEagerly(ex, ctx);
    }
    catch (Throwable e) {
      e.printStackTrace();
      fail("Should not raise exception: " + e.getMessage());
    }
  }

  // Confirmed: with `; return null;` appended, the same expression compiles.
  @Test
  @KnownCompatibility("void-method-expression")
  public void testStringConcatenation2() {
    String ex = "services.log( $cheese + \" some string \" );";
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("$cheese",
        Cheese.class);
    ctx.addInput("services",
        Services.class);
    try {
      compileEagerly(ex, ctx);
    }
    catch (Throwable e) {
      e.printStackTrace();
      fail("Should not raise exception: " + e.getMessage());
    }
  }

  // Category not isolated: `percent` is a method alias for `String.valueOf(double)`
  // (`ParserConfiguration.addImport(name, Method)`), which MVEL3 does not have; a static import of `valueOf`
  // fails the same way ("No method candidates") when called as `valueOf($av)`. The void call `services.log(..)`
  // as the last statement also needs `return null` (void-method-expression).
  @Test
  @KnownCompatibility("static-import-method-call")
  public void testStringConcatenation3() {
    // BUG: return type of the string concatenation is inferred as double instead of String
    String ex = "services.log($av + \"Drop +5%: \"+$sb+\" avg: $\"+percent($av)+\" price: $\"+$pr );";
    TestCompilerContext ctx = new TestCompilerContext();
    ctx.addInput("$sb",
        String.class);
    ctx.addInput("$av",
        double.class);
    ctx.addInput("$pr",
        double.class);
    ctx.addInput("services",
        Services.class);
    Set<String> imports = new LinkedHashSet<>();
    imports.add("static java.lang.String.valueOf");
    try {
      compileEagerly(ex, ctx, imports);

      Services services = new Services() {
        public void log(String text) {
        }
      };
      Map<String, Object> vars = new HashMap<String, Object>();
      vars.put("services", services);
      vars.put("$sb", "RHT");
      vars.put("$av", 15.0);
      vars.put("$pr", 10.0);

      executeExpression(compileExpression(ex, ctx), vars);
    }
    catch (Throwable e) {
      e.printStackTrace();
      fail("Should not raise exception: " + e.getMessage());
    }
  }

  // Category not isolated: `'foobar'.toUpperCase();` fails with a parse error; the trailing `;` also makes it a
  // block that needs `return` (block-syntax). `return "foobar".toUpperCase();` works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testRandomExpression7() {
    assertEquals("FOOBAR",
        test("'foobar'.toUpperCase();"));
  }

  // Category not isolated: `'someString'` fails with a null-reference error; the last statement `a == b;` is
  // "not a statement" without `return` (block-syntax). `return` with double quotes works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testRandomExpression8() {
    assertEquals(true,
        test("'someString'.intern(); 'someString'.hashCode() == 'someString'.hashCode();"));
  }

  @Test
  public void testRandomSomething() {

    Foo foo = new Foo();
    foo.setName("foo1");

    Foo foo2 = new Foo();
    foo2.setName("foo2");

    Serializable s = compileExpression("name.toUpperCase()", TestCompilerContext.create().withInput("name", String.class));

    Object _return = executeExpression(s, foo);

    System.out.println("returned value: " + String.valueOf(_return));

    _return = executeExpression(s, foo2);

    System.out.println("returned value: " + String.valueOf(_return));

  }

  @Test
  public void testJIRA96() {
    TestCompilerContext ctx = new TestCompilerContext();

    ctx.addInput("fooString",
        String[].class);

    compileEagerly("fooString[0].toUpperCase()", ctx);
  }

  // Confirmed: the parse error comes from the last expression having no `return`; with `return` the same
  // expression (single-quoted char literals included) returns true.
  @Test
  @KnownCompatibility("block-syntax")
  public void testJIRA122() {
    Serializable s = compileExpression("System.out.println('>'+java.lang.Character.toLowerCase(name.charAt(0))); java.lang.Character.toLowerCase(name.charAt(0)) == 'a'");

    Map map = new HashMap();
    map.put("name",
        "Adam");

    assertEquals(true,
        executeExpression(s,
            map));
    assertEquals(true,
        executeExpression(s,
            map));
  }

  // Confirmed: same cause as testJIRA122 (the optimizer selection does not exist in MVEL3).
  @Test
  @KnownCompatibility("block-syntax")
  public void testJIRA122b() {
    Serializable s = compileExpression("System.out.println('>'+java.lang.Character.toLowerCase(name.charAt(0))); java.lang.Character.toLowerCase(name.charAt(0)) == 'a'");

    Map map = new HashMap();
    map.put("name",
        "Adam");

    assertEquals(true,
        executeExpression(s,
            map));
    assertEquals(true,
        executeExpression(s,
            map));
  }

  // Category not isolated: `'attribute'` fails with a null-reference error; `{ end = 'attribute'.length()}`
  // without `;` and the final `substring(..)` without `return` (block-syntax) may also fail. With double
  // quotes, `;` and `return` it works.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testJIRA183() {
    String exp1 = "int end = 'attribute'.indexOf('@');  if(end == -1)" +
        " { end = 'attribute'.length()} 'attribute'.substring(0, end);";
    Object val1 = Mvel3TestSupport.eval(exp1, new HashMap<String, Object>());

    String exp2 = "int end = 'attribute'.indexOf('@');  if(end == -1)" +
        " { end = 'attribute'.length() } 'attribute'.substring(0, end);";
    Object val2 = Mvel3TestSupport.eval(exp2, new HashMap<String, Object>());
  }
}
