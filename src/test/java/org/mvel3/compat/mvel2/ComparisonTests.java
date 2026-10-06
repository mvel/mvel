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

  @Test
  @KnownCompatibility("single-quote-string")
  public void testTextComparison() {
    assertEquals(true, test("foo.bar.name == 'dog'"));
  }

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

  @Test
  @KnownCompatibility("regex-operator")
  public void testRegEx() {
    assertEquals(true, test("foo.bar.name ~= '[a-z].+'"));
  }

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

  @Test
  @KnownCompatibility("regex-operator")
  public void testRegEx3() {
    assertEquals(true, test("foo.bar.name~='[a-z].+'&&foo.bar.name!=null"));
  }

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

  @Test
  @KnownCompatibility("contains-operator")
  public void testContains7() {
     assertEquals(true, test("sentence contains ('fox')"));
   }

  @Test
  @KnownCompatibility("soundslike-operator")
  public void testSoundex() {
    assertTrue((Boolean) test("'foobar' soundslike 'fubar'"));
  }

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

  @Test
  @KnownCompatibility("strsim-operator")
  public void testSimilarity1() {
    assertEquals(0.6666667f, test("c strsim 'kat'"));
  }

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

  @Test
  @KnownCompatibility("block-syntax")
  public void testJIRA100b() {
    String expression = "(8 / 10) * 100 <= 80;";
    assertEquals((8 / 10) * 100 <= 80, testCompiledSimple(expression, new HashMap()));
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testJIRA92() {
    assertEquals(false, test("'stringValue' > null"));
  }

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
