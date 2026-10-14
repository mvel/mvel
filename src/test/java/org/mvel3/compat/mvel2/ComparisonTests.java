package org.mvel3.compat.mvel2;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.res.Foo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from mvel2 at d7c96a36c692df6a9a027a8416e275253fdfda01.
 * Expressions, inputs, and assertions are retained; evaluation uses MVEL3.
 * See README.md in this package for execution and adapter limitations.
 */
@Tag("mvel2-compatibility")
public class ComparisonTests extends AbstractTest {

  @Test
  public void testBooleanOperator() {
    assertEquals(true, test("foo.bar.woof == true"));
  }

  @Test
  public void testBooleanOperator2() {
    assertEquals(false, test("foo.bar.woof == false"));
  }

  @Test
  public void testBooleanOperator3() {
    assertEquals(true, test("foo.bar.woof== true"));
  }

  @Test
  public void testBooleanOperator4() {
    assertEquals(false, test("foo.bar.woof ==false"));
  }

  @Test
  public void testBooleanOperator5() {
    assertEquals(true, test("foo.bar.woof == true"));
  }

  @Test
  public void testBooleanOperator6() {
    assertEquals(false, test("foo.bar.woof==false"));
  }

  // Category not isolated: observed error is "Unsolved symbol in g". In an experiment that converted single-quoted literals to double quotes this test passed, which supports single-quote-string as the only cause.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testTextComparison() {
    assertEquals(true, test("foo.bar.name == 'dog'"));
  }

  // Category not isolated: observed error is "Unsolved symbol in o". In an experiment that converted single-quoted literals to double quotes this test passed, which supports single-quote-string as the only cause.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testNETextComparison() {
    assertEquals(true, test("foo.bar.name != 'foo'"));
  }

  @Test
  @KnownCompatibility("chor-operator")
  public void testChor() {
    assertEquals("cat", test("a or b or c"));
  }

  @Test
  @KnownCompatibility("chor-operator")
  public void testChorWithLiteral() {
    assertEquals("fubar", test("a or 'fubar'"));
  }

  @Test
  public void testNullCompare() {
    assertEquals(true, test("c != null"));
  }

  @Test
  @KnownCompatibility("string-coercion")
  public void testLessThan() {
    assertEquals(true, test("pi < 3.15"));
    assertEquals(true, test("pi <= 3.14"));
    assertEquals(false, test("pi > 3.14"));
    assertEquals(true, test("pi >= 3.14"));
  }

  @Test
  public void testNegation() {
    String ex = "!fun && !fun";
    assertEquals(true, test(ex));
  }

  @Test
  public void testNegation2() {
    assertEquals(false, test("fun && !fun"));
  }

  @Test
  public void testNegation3() {
    assertEquals(true, test("!(fun && fun)"));
  }

  @Test
  public void testNegation4() {
    assertEquals(false, test("(fun && fun)"));
  }

  @Test
  public void testNegation5() {
    assertEquals(true, test("!false"));
  }

  @Test
  public void testNegation6() {
    assertEquals(false, test("!true"));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testNegation7() {
    assertEquals(true, test("s = false; t = !s; t"));
  }

  @Test
  @KnownCompatibility("block-syntax")
  public void testNegation8() {
    assertEquals(true, test("s = false; t =! s; t"));
  }

  // Category not isolated: the observed result is the left operand, not a boolean, so `~=` is not evaluated as an
  // operator (regex-operator). The single-quoted pattern literal was not tested separately.
  @Test
  @KnownCompatibility("regex-operator")
  public void testRegEx() {
    assertEquals(true, test("foo.bar.name ~= '[a-z].+'"));
  }

  // Category not isolated: observed error is a null-node failure. The `~=` operator (regex-operator) is the suspected
  // cause; the negation `!( ... )` and the single-quoted pattern were not tested separately.
  @Test
  @KnownCompatibility("regex-operator")
  public void testRegExNegate() {
    assertEquals(false, test("!(foo.bar.name ~= '[a-z].+')"));
  }

  @Test
  @KnownCompatibility("regex-operator")
  public void testRegEx2() {
    assertEquals(true, test("foo.bar.name ~= '[a-z].+' && foo.bar.name != null"));
  }

  // Category not isolated: the observed result is the left operand, not a boolean, so `~=` is not evaluated as an
  // operator (regex-operator). The single-quoted pattern literal was not tested separately.
  @Test
  @KnownCompatibility("regex-operator")
  public void testRegEx3() {
    assertEquals(true, test("foo.bar.name~='[a-z].+'&&foo.bar.name!=null"));
  }

  // Category not isolated: observed error is a parse error at `==` caused by the `empty` keyword. The single-quoted
  // `''` (empty char literal) may also be rejected by MVEL3; not tested separately.
  @Test
  @KnownCompatibility("empty-operator")
  public void testBlank() {
    assertEquals(true, test("'' == empty"));
  }

  @Test
  @KnownCompatibility("empty-operator")
  public void testBlank2() {
    assertEquals(true, test("BWAH == empty"));
  }

  @Test
  @KnownCompatibility("empty-operator")
  public void testBlank3() {
    assertEquals(true, _test("[] == empty"));
  }

//  public void testBlank4() {
//    assertEquals(true, _test("empty  == []"));
//  }

  @Test
  @KnownCompatibility("empty-operator")
  public void testBlank5() {
    assertEquals(true, _test("['a'] != empty"));
  }

  @Test
  @KnownCompatibility("empty-operator")
  public void testBlank6() {
    assertEquals(true, _test("empty != ['a']"));
  }

  @Test
  @KnownCompatibility("empty-operator")
  public void testBlank7() {
    assertEquals(false, _test("[] != empty"));
  }

//  public void testBlank8() {
//    assertEquals(false, _test("empty  != []"));
//  }

  @Test
  @KnownCompatibility("empty-operator")
  public void testBlank9() {
    assertEquals(false, _test("['a'] == empty"));
  }

  @Test
  @KnownCompatibility("empty-operator")
  public void testBlank10() {
    assertEquals(false, _test("empty == ['a']"));
  }

  @Test
  @KnownCompatibility("is-operator")
  public void testInstanceCheck1() {
    assertEquals(true, test("c is java.lang.String"));
  }

  @Test
  @KnownCompatibility("is-operator")
  public void testInstanceCheck2() {
    assertEquals(false, test("pi is java.lang.Integer"));
  }

  @Test
  @KnownCompatibility("is-operator")
  public void testInstanceCheck3() {
    assertEquals(true, test("foo is org.mvel3.compat.mvel2.res.Foo"));
  }

  // Category not isolated: the observed result is the left operand, not a boolean, so infix `contains` is not
  // evaluated as an operator (contains-operator). The single-quoted literal was not tested separately.
  @Test
  @KnownCompatibility("contains-operator")
  public void testContains1() {
    assertEquals(true, test("list contains 'Happy!'"));
  }

  @Test
  @KnownCompatibility("contains-operator")
  public void testContains2() {
    assertEquals(false, test("list contains 'Foobie'"));
  }

  // Category not isolated: the observed result is the left operand, not a boolean, so infix `contains` is not
  // evaluated as an operator (contains-operator). The single-quoted literal was not tested separately.
  @Test
  @KnownCompatibility("contains-operator")
  public void testContains3() {
    assertEquals(true, test("sentence contains 'fox'"));
  }

  @Test
  @KnownCompatibility("contains-operator")
  public void testContains4() {
    assertEquals(false, test("sentence contains 'mike'"));
  }

  // Category not isolated: observed error is a null-node failure. The infix `contains` operator is the suspected
  // cause; the single-quoted literal and the negation were not tested separately.
  @Test
  @KnownCompatibility("contains-operator")
  public void testContains5() {
    assertEquals(true, test("!(sentence contains 'mike')"));
  }

  @Test
  @KnownCompatibility("contains-operator")
  public void testContains6() {
    assertEquals(true, test("bwahbwah = 'mikebrock'; testVar10 = 'mike'; bwahbwah contains testVar10"));
  }

  // Category not isolated: the observed result is the left operand, not a boolean, so infix `contains` is not
  // evaluated as an operator (contains-operator). The single-quoted literal was not tested separately.
  @Test
  @KnownCompatibility("contains-operator")
  public void testContains7() {
     assertEquals(true, test("sentence contains ('fox')"));
   }

  // Category not isolated: observed error is a parse error at `soundslike`. With single-quoted literals converted to
  // double quotes (experiment) the failure became a ClassCastException (String to Boolean), so single-quote-string
  // is also a cause of the original parse error.
  @Test
  @KnownCompatibility("soundslike-operator")
  public void testSoundex() {
    assertTrue((Boolean) test("'foobar' soundslike 'fubar'"));
  }

  // Category not isolated: observed error is a parse error at `soundslike`. With single-quoted literals converted to
  // double quotes (experiment) the failure became a ClassCastException (String to Boolean), so single-quote-string
  // is also a cause of the original parse error.
  @Test
  @KnownCompatibility("soundslike-operator")
  public void testSoundex2() {
    assertFalse((Boolean) test("'flexbar' soundslike 'fubar'"));
  }

  @Test
  @KnownCompatibility("soundslike-operator")
  public void testSoundex3() {
    assertEquals(true, test("(c soundslike 'kat')"));
  }

  // Category not isolated: the observed result is `cat` (the left operand), not a similarity value, so `c strsim 'kat'`
  // is not evaluated as an operator. The single-quoted `'kat'` literal was not tested separately.
  @Test
  @KnownCompatibility("strsim-operator")
  public void testSimilarity1() {
    assertEquals(0.6666667f, test("c strsim 'kat'"));
  }

  // Category not isolated: observed error is a null-node failure. The `soundslike` operator is the suspected cause;
  // the undeclared assignments (block-syntax) and single-quoted literals were not tested separately.
  @Test
  @KnownCompatibility("soundslike-operator")
  public void testSoundex4() {
    assertEquals(true, test("_xx1 = 'cat'; _xx2 = 'katt'; (_xx1 soundslike _xx2)"));
  }

  @Test
  @KnownCompatibility("soundslike-operator")
  public void testSoundex5() {
    assertEquals(true, test("_type = 'fubar';_type soundslike \"foobar\""));
  }

  // Category not isolated: the observed result is the evaluator instance (`this` is not the root object), which fits
  // this-reference. The `is` operator (see is-operator) is also used in this expression and was not tested separately.
  @Test
  @KnownCompatibility("this-reference")
  public void testThisReference3() {
    assertEquals(true, test("this is org.mvel3.compat.mvel2.res.Base"));
  }

  @Test
  @KnownCompatibility("this-reference")
  public void testThisReference4() {
    assertEquals(true, test("this.funMap instanceof java.util.Map"));
  }

  // Category not isolated: observed error is "Unsolved symbol : data" (this-reference). The single-quoted
  // `'cat'` literal (single-quote-string) was not tested separately.
  @Test
  @KnownCompatibility("this-reference")
  public void testThisReference5() {
    assertEquals(true, test("this.data == 'cat'"));
  }

  @Test
  @KnownCompatibility("date-comparison")
  public void testDateComparison() {
    assertTrue((Boolean) test("dt1 < dt2"));
  }

  @Test
  @KnownCompatibility("convertable-to-operator")
  public void testConvertableTo() {
    assertEquals(true, test("pi convertable_to Integer"));
  }

  // Category not isolated: observed result is `false` instead of `true`. In an experiment that converted single-quoted literals to double quotes this test passed, which supports single-quote-string as the only cause.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testStringEquals() {
    assertEquals(true, test("ipaddr == '10.1.1.2'"));
  }

  @Test
  public void testCharComparison() {
    assertEquals(true, test("'z' > 'a'"));
  }

  @Test
  public void testCharComparison2() {
    assertEquals(false, test("'z' < 'a'"));
  }

  // Category not isolated: observed error is "not a statement". The expression has a trailing `;` and no `return`,
  // which fits block-syntax, but integer-division ((8 / 10) * 100 <= 80) was not tested separately.
  @Test
  @KnownCompatibility("block-syntax")
  public void testJIRA100b() {
    String expression = "(8 / 10) * 100 <= 80;";
    assertEquals((8 / 10) * 100 <= 80, testCompiledSimple(expression, new HashMap()));
  }

  // Category not isolated: observed error is "cannot find symbol: variable ringValue" (single-quoted literal split
  // as char + identifier). With the literal converted to double quotes (experiment) the error became
  // "bad operand types for binary operator '>'" (String > null), so a relational comparison of a String with
  // null (see null-relational-comparison) is a second cause.
  @Test
  @KnownCompatibility("single-quote-string")
  public void testJIRA92() {
    assertEquals(false, test("'stringValue' > null"));
  }

  // Category not isolated: observed error is a null-node failure. In an experiment that converted single-quoted
  // literals to double quotes it became a parse error at `isdef`, which supports isdef-operator; the undeclared
  // assignment (block-syntax) was not tested separately.
  @Test
  @KnownCompatibility("isdef-operator")
  public void testIsDefOperator() {
    assertEquals(true, test("_v1 = 'bar'; isdef _v1"));
  }

  @Test
  @KnownCompatibility("isdef-operator")
  public void testIsDefOperator2() {
    assertEquals(false, test("isdef _v1"));
  }

  // Category not isolated: observed error is an internal NPE (`LambdaBodyContext.block()` is null) rather than a
  // plain parse error at `isdef`. The `!( ... )` negation was not tested separately.
  @Test
  @KnownCompatibility("isdef-operator")
  public void testIsDefOperator3() {
    assertEquals(true, test("!(isdef _v1)"));
  }

  @Test
  @KnownCompatibility("isdef-operator")
  public void testIsDefOperator4() {
    assertEquals(true, test("! (isdef _v1)"));
  }

  @Test
  @KnownCompatibility("isdef-operator")
  public void testIsDefOperator5() {
    assertEquals(true, test("!isdef _v1"));
  }

  @Test
  @KnownCompatibility("isdef-operator")
  public void testIsDefOperator6() {
    Foo foo = new Foo();
    assertEquals(true, eval("isdef name", foo));
    assertEquals(true, executeExpression(compileExpression("isdef name"), foo));
  }

  @Test
  public void testJIRA152() {
    assertEquals(true, eval("1== -(-1)"));
    assertEquals(true, executeExpression(compileExpression("1==-(-1)")));
  }

  @Test
  public void testJIRA157() {
    assertEquals(true, eval("1 == ((byte) 1)"));
  }

  @Test
  public void testJIRA181() {
    assertEquals(false, eval("0<-1"));
  }

  @Test
  @KnownCompatibility("string-coercion")
  public void testStringCoercionForComparison() {
    assertEquals(false, eval("36 > 242"));
    assertEquals(false, eval("\"36\" > 242"));
    assertEquals(false, eval("36 > \"242\""));
    assertEquals(true, eval("\"36\" > \"242\""));

    TestCompilerContext parserContext = new TestCompilerContext();
    // Strong typing / strict type enforcement flags removed in MVEL3 adapter

    assertEquals(false, executeExpression(compileExpression("36 > 242", parserContext)));
    assertEquals(false, executeExpression(compileExpression("\"36\" > 242", parserContext)));
    assertEquals(false, executeExpression(compileExpression("36 > \"242\"", parserContext)));
    assertEquals(true, executeExpression(compileExpression("\"36\" > \"242\"", parserContext)));

    parserContext = new TestCompilerContext();
    parserContext.addInput("a", String.class);
    parserContext.addInput("b", String.class);

    Serializable expression = compileExpression("a > b", parserContext);
    assertEquals(true, executeExpression(expression, new HashMap() {{
      put("a", "36");
      put("b", "242");
    }}));
  }
}
