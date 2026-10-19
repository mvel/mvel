package org.mvel3.compat.mvel2;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;
import org.mvel3.Type;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;

/**
 * Imported from org.mvel2.tests.core.FailureTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class FailureTests extends AbstractTest {

  private static void compileEagerly(final String expression) {
    compileEagerly(expression, new TestCompilerContext());
  }

  private static void compileEagerly(final String expression, final TestCompilerContext ctx) {
    final Map<String, Type<?>> types = new LinkedHashMap<>();
    ctx.getInputs().forEach((name, type) -> types.put(name, Type.type(type)));
    final Set<String> imports = new LinkedHashSet<>();
    ctx.getImports().forEach(type -> imports.add(type.getCanonicalName()));
    if (expression.indexOf(';') > 0) {
      new MVEL().compileMapBlock(expression, Object.class, imports, types);
    } else {
      new MVEL().compileMapExpression(expression, Object.class, imports, types);
    }
  }

  public void shouldThrowException() {
    fail("should have thrown exception, but didn't");
  }

  @Test
  public void testBadParserConstruct() {
    try {
      eval("a = 0; a =+++ 5;");
    }
    catch (RuntimeException e) {
      return;
    }

    shouldThrowException();
  }

  @Test
  public void testShouldFail() {
    try {
      eval("i = 0; i < 99 dksadlka", new HashMap());
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  @Test
  public void testShouldFail2() {
    try {
      compileEagerly("i = 0; i < 99 dksadlka");
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  @Test
  public void testShouldFail3() {
    try {
      compileEagerly("def foo() { 'bar' }; foo(123);");
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  @Test
  public void testShouldFail4() {
    try {
      eval("hour zzz", createTestMap());
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  @Test
  public void testShouldFail5() {
    try {
      eval("[");
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  @Test
  public void testShouldFail6() {
    try {
      TestCompilerContext pctx = new TestCompilerContext();
      compileEagerly("new int[] {1.5}", pctx);
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  @Test
  public void testShouldFail7() {
    try {
      TestCompilerContext pctx = new TestCompilerContext();
      compileEagerly("String x = 'foo'; int y = 2; new int[] { x, y }", pctx);
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  @Test
  public void testShouldFail8() {
    try {
      TestCompilerContext pCtx = new TestCompilerContext();
      compileEagerly("for (String s : new java.util.HashMap()) { }", pCtx);
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  @Test
  public void testShouldFail9() {
    try {
      compileEagerly("foo = ");
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  @Test
  public void testShouldFail10() {
    try {
      compileEagerly("foo = [1,1,qq,zz]");
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  @Test
  @KnownCompatibility("javaparser-null-node")
  public void testShouldFailCleanly() {
    try {
      eval("6:f-\t\n" +
          "r:wsGbL%X5&wb<C.8n(\n" +
          "1X-;zUX-L<%<TG)S#z1fh69:tn`#eH9XneL8XFIB94-z*XzQ-RPhX{&bbp{fLDe@`3<-\tZp9_k*Xo\tDy36t15XX>-75EArR]6`**<kw}P<FpD-+XA-<+K!\n" +
          "Rb9n)zA-<L9pIIbHb?!b-bO%n<zLFqzbmm-n#~-zL`5Fq_PYD+9`NPt4Tnt\tvT-L[d\n" +
          "<b<9l9XV9-9 X9h7#9Ln?FnnLXLkg5<V-Z%bb-n&Et<B-X[n\"jbvg&@b{X0?*9eC{%zU\n" +
          "L\t{RPX\tbwhY&L`z<`Oh`<8pH\n" +
          "b:y:#H-;&,PzXw\ttHicFbs");
    }
    catch (Exception e) {
      return;
    }
    shouldThrowException();
  }

  @Test
  public void testShouldFail11() {
    try {
      eval("for (int i = 0; i--; i++) {}");
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  @Test
  public void testShouldFail12() {
    try {
      eval("def x(_");
    }
    catch (Exception e) {
      return;
    }

    shouldThrowException();
  }

  @Test
  public void testErrorUtilShouldNotThrowNPE() {
    try {
      compileEagerly("new org.mvel3.compat.mvel2.FailureTests$Person()");
      fail("Should throw CompileException");
    } catch (Exception ce) {
      assertFalse("Should not throw NullPointerException", ce instanceof NullPointerException);
    }
  }

  public static class Person {

      @SuppressWarnings("unused")
      private String name;

      public Person(String name) {
          super();
          this.name = name;
      }

  }
}
