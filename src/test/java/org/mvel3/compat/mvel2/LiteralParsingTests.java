/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3.compat.mvel2;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.LiteralParsingTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class LiteralParsingTests extends AbstractTest {

  @Test
  @KnownCompatibility("class-literal")
  public void testClassLiteral() {
    assertEquals(String.class,
        test("java.lang.String"));
  }

  @Test
  public void testAndOpLiteral() {
    assertEquals(true,
        test("true && true"));
  }

  @Test
  @KnownCompatibility("regex-operator")
  public void testLiteralUnionWithComparison() {
    // Single-quoted strings also differ; adapting the quotes still leaves the unsupported ~= operator.
    assertEquals(Boolean.TRUE,
        executeExpression(compileExpression("1 == 1 && ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')")));
  }

  @Test
  public void testHexCharacter() {
    assertEquals(0x0A,
        eval("0x0A"));
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testOctalEscapes() {
    assertEquals("\344",
        eval("'\\344'"));
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testOctalEscapes2() {
    assertEquals("\7",
        eval("'\\7'"));
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testOctalEscapes3() {
    assertEquals("\777",
        eval("'\\777'"));
  }

  @Test
  @KnownCompatibility("single-quote-string")
  public void testUniHex1() {
    assertEquals("\uFFFF::",
        eval("'\\uFFFF::'"));
  }

  @Test
  public void testNumLiterals() {
    assertEquals(1e1f,
        eval("1e1f"));
  }

  @Test
  public void testNumLiterals2() {
    assertEquals(2.f,
        eval("2.f"));
  }

  @Test
  public void testNumLiterals3() {
    assertEquals(.3f,
        eval(".3f"));
  }

  @Test
  public void testNumLiterals4() {
    assertEquals(3.14f,
        eval("3.14f"));
  }

  @Test
  public void testNumLiterals5() {
    assertEquals(1e1,
        eval("1e1"));
  }

  @Test
  public void testNumLiterals6() {
    assertEquals(2.,
        eval("2."));
  }

  @Test
  public void testNumLiterals7() {
    assertEquals(.3,
        eval(".3"));
  }

  @Test
  public void testNumLiterals8() {
    assertEquals(1e-9d,
        eval("1e-9d"));
  }

  @Test
  public void testNumLiterals9() {
    assertEquals(0x400921FB54442D18L,
        eval("0x400921FB54442D18L"));
  }
}
