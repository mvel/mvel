package org.mvel2.tests.core;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.tests.core.res.Foo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ComparisonTests extends AbstractTest {

  public void testBooleanOperator() {
    assertEquals(true, test("foo.bar.woof == true"));
  }

  public void testBooleanOperator2() {
    assertEquals(false, test("foo.bar.woof == false"));
  }

  public void testBooleanOperator3() {
    assertEquals(true, test("foo.bar.woof== true"));
  }

  public void testBooleanOperator4() {
    assertEquals(false, test("foo.bar.woof ==false"));
  }

  public void testBooleanOperator5() {
    assertEquals(true, test("foo.bar.woof == true"));
  }

  public void testBooleanOperator6() {
    assertEquals(false, test("foo.bar.woof==false"));
  }

  public void testTextComparison() {
    assertEquals(true, test("foo.bar.name == 'dog'"));
  }

  public void testNETextComparison() {
    assertEquals(true, test("foo.bar.name != 'foo'"));
  }

  public void testChor() {
    assertEquals("cat", test("a or b or c"));
  }

  public void testChorWithLiteral() {
    assertEquals("fubar", test("a or 'fubar'"));
  }

  public void testNullCompare() {
    assertEquals(true, test("c != null"));
  }

  public void testLessThan() {
    assertEquals(true, test("pi < 3.15"));
    assertEquals(true, test("pi <= 3.14"));
    assertEquals(false, test("pi > 3.14"));
    assertEquals(true, test("pi >= 3.14"));
  }

  public void testNegation() {
    String ex = "!fun && !fun";
    assertEquals(true, test(ex));
  }

  public void testNegation2() {
    assertEquals(false, test("fun && !fun"));
  }

  public void testNegation3() {
    assertEquals(true, test("!(fun && fun)"));
  }

  public void testNegation4() {
    assertEquals(false, test("(fun && fun)"));
  }

  public void testNegation5() {
    assertEquals(true, test("!false"));
  }

  public void testNegation6() {
    assertEquals(false, test("!true"));
  }

  public void testNegation7() {
    assertEquals(true, test("s = false; t = !s; t"));
  }

  public void testNegation8() {
    assertEquals(true, test("s = false; t =! s; t"));
  }

  public void testRegEx() {
    assertEquals(true, test("foo.bar.name ~= '[a-z].+'"));
  }

  public void testRegExNegate() {
    assertEquals(false, test("!(foo.bar.name ~= '[a-z].+')"));
  }

  public void testRegEx2() {
    assertEquals(true, test("foo.bar.name ~= '[a-z].+' && foo.bar.name != null"));
  }

  public void testRegEx3() {
    assertEquals(true, test("foo.bar.name~='[a-z].+'&&foo.bar.name!=null"));
  }

  public void testBlank() {
    assertEquals(true, test("'' == empty"));
  }

  public void testBlank2() {
    assertEquals(true, test("BWAH == empty"));
  }
  
  public void testBlank3() {
    assertEquals(true, _test("[] == empty"));
  }
//
//  public void testBlank4() {
//    assertEquals(true, _test("empty  == []"));
//  }

  public void testBlank5() {
    assertEquals(true, _test("['a'] != empty"));
  }

  public void testBlank6() {
    assertEquals(true, _test("empty != ['a']"));
  }
  
  public void testBlank7() {
    assertEquals(false, _test("[] != empty"));
  }

//  public void testBlank8() {
//    assertEquals(false, _test("empty  != []"));
//  }

  public void testBlank9() {
    assertEquals(false, _test("['a'] == empty"));
  }

  public void testBlank10() {
    assertEquals(false, _test("empty == ['a']"));
  }

  public void testInstanceCheck1() {
    assertEquals(true, test("c is java.lang.String"));
  }

  public void testInstanceCheck2() {
    assertEquals(false, test("pi is java.lang.Integer"));
  }

  public void testInstanceCheck3() {
    assertEquals(true, test("foo is org.mvel2.tests.core.res.Foo"));
  }

  public void testContains1() {
    assertEquals(true, test("list contains 'Happy!'"));
  }

  public void testContains2() {
    assertEquals(false, test("list contains 'Foobie'"));
  }

  public void testContains3() {
    assertEquals(true, test("sentence contains 'fox'"));
  }

  public void testContains4() {
    assertEquals(false, test("sentence contains 'mike'"));
  }

  public void testContains5() {
    assertEquals(true, test("!(sentence contains 'mike')"));
  }

  public void testContains6() {
    assertEquals(true, test("bwahbwah = 'mikebrock'; testVar10 = 'mike'; bwahbwah contains testVar10"));
  }

  public void testContains7() {
     assertEquals(true, test("sentence contains ('fox')"));
   }

  public void testSoundex() {
    assertTrue((Boolean) test("'foobar' soundslike 'fubar'"));
  }

  public void testSoundex2() {
    assertFalse((Boolean) test("'flexbar' soundslike 'fubar'"));
  }

  public void testSoundex3() {
    assertEquals(true, test("(c soundslike 'kat')"));
  }

  public void testSimilarity1() {
    assertEquals(0.6666667f, test("c strsim 'kat'"));
  }

  public void testSoundex4() {
    assertEquals(true, test("_xx1 = 'cat'; _xx2 = 'katt'; (_xx1 soundslike _xx2)"));
  }

  public void testSoundex5() {
    assertEquals(true, test("_type = 'fubar';_type soundslike \"foobar\""));
  }

  public void testThisReference3() {
    assertEquals(true, test("this is org.mvel2.tests.core.res.Base"));
  }

  public void testThisReference4() {
    assertEquals(true, test("this.funMap instanceof java.util.Map"));
  }

  public void testThisReference5() {
    assertEquals(true, test("this.data == 'cat'"));
  }


  public void testDateComparison() {
    assertTrue((Boolean) test("dt1 < dt2"));
  }

  public void testConvertableTo() {
    assertEquals(true, test("pi convertable_to Integer"));
  }

  public void testStringEquals() {
    assertEquals(true, test("ipaddr == '10.1.1.2'"));
  }

  public void testCharComparison() {
    assertEquals(true, test("'z' > 'a'"));
  }

  public void testCharComparison2() {
    assertEquals(false, test("'z' < 'a'"));
  }

  public void testJIRA100b() {
    String expression = "(8 / 10) * 100 <= 80;";
    assertEquals((8 / 10) * 100 <= 80, testCompiledSimple(expression, new HashMap()));
  }

  public void testJIRA92() {
    assertEquals(false, test("'stringValue' > null"));
  }

  public void testIsDefOperator() {
    assertEquals(true, test("_v1 = 'bar'; isdef _v1"));
  }

  public void testIsDefOperator2() {
    assertEquals(false, test("isdef _v1"));
  }

  public void testIsDefOperator3() {
    assertEquals(true, test("!(isdef _v1)"));
  }

  public void testIsDefOperator4() {
    assertEquals(true, test("! (isdef _v1)"));
  }

  public void testIsDefOperator5() {
    assertEquals(true, test("!isdef _v1"));
  }

  public void testIsDefOperator6() {
    Foo foo = new Foo();
    assertEquals(true, MVEL.eval("isdef name", foo));
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("isdef name"), foo));
  }

  public void testJIRA152() {
    assertEquals(true, MVEL.eval("1== -(-1)"));
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("1==-(-1)")));
  }

  public void testJIRA157() {
    assertEquals(true, MVEL.eval("1 == ((byte) 1)"));
  }

  public void testJIRA181() {
    assertEquals(false, MVEL.eval("0<-1"));
  }

  public void testStringCoercionForComparison() {
    assertEquals(false, MVEL.eval("36 > 242"));
    assertEquals(false, MVEL.eval("\"36\" > 242"));
    assertEquals(false, MVEL.eval("36 > \"242\""));
    assertEquals(true, MVEL.eval("\"36\" > \"242\""));

    ParserContext parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);

    assertEquals(false, MVEL.executeExpression(MVEL.compileExpression("36 > 242", parserContext)));
    assertEquals(false, MVEL.executeExpression(MVEL.compileExpression("\"36\" > 242", parserContext)));
    assertEquals(false, MVEL.executeExpression(MVEL.compileExpression("36 > \"242\"", parserContext)));
    assertEquals(true, MVEL.executeExpression(MVEL.compileExpression("\"36\" > \"242\"", parserContext)));

    parserContext = new ParserContext();
    parserContext.setStrictTypeEnforcement(true);
    parserContext.setStrongTyping(true);
    parserContext.addInput("a", String.class);
    parserContext.addInput("b", String.class);

    Serializable expression = MVEL.compileExpression("a > b", parserContext);
    assertEquals(true, MVEL.executeExpression(expression, new HashMap() {{
      put("a", "36");
      put("b", "242");
    }}));
  }
}
