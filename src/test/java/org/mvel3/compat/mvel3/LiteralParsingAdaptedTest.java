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

package org.mvel3.compat.mvel3;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mvel3.MVEL;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Porting of LiteralParsingTests from MVEL2 adapted for MVEL3 syntax and features.
 *
 * <p>Each method corresponds to a test in
 * {@code org.mvel3.compat.mvel2.LiteralParsingTests}.  Tests whose MVEL2
 * feature is unsupported in MVEL3 are marked {@link Disabled} with a brief
 * explanation; all other tests must pass without modification to the
 * MVEL3 engine.
 */
public class LiteralParsingAdaptedTest {

  private static Object eval(final String expression) {
    return new MVEL().executeExpression(expression);
  }

  // -------------------------------------------------------------------------
  // class-literal: MVEL3 does not support bare qualified-name class literals
  // such as "java.lang.String". Use String.class instead.
  // -------------------------------------------------------------------------

  @Test
  @Disabled("class-literal: 'java.lang.String' is not supported; use String.class")
  public void testClassLiteral() {
    // Original: test("java.lang.String")  expects String.class
    // No MVEL3-compatible expression available without engine changes.
  }

  // -------------------------------------------------------------------------
  // Straightforward literals — these pass unchanged.
  // -------------------------------------------------------------------------

  @Test
  public void testAndOpLiteral() {
    assertEquals(true, eval("true && true"));
  }

  // -------------------------------------------------------------------------
  // testLiteralUnionWithComparison: requires both single-quote strings
  // and the ~= regex operator, neither of which is supported in MVEL3.
  // -------------------------------------------------------------------------

  @Test
  @Disabled("single-quote-string + regex-operator: single-quoted strings and ~= are not supported")
  public void testLiteralUnionWithComparison() {
    // Original: 1 == 1 && ('Hello'.toUpperCase() ~= '[A-Z]{0,5}')
    // Adapted form (double-quoted, Java matches): blocked by regex-operator.
  }

  @Test
  public void testHexCharacter() {
    assertEquals(0x0A, eval("0x0A"));
  }

  // -------------------------------------------------------------------------
  // single-quote-string: MVEL3 treats single-quoted literals as char (Java
  // semantics). Octal / unicode escape sequences inside single-quoted literals
  // are not interpreted as in MVEL2.  Use double-quoted String literals.
  // -------------------------------------------------------------------------

  @Test
  public void testOctalEscapes() {
    // MVEL2: eval("'\\344'") returned "\344" (String with octal char ä).
    // MVEL3: use a double-quoted string literal with the same escape.
    assertEquals("\344", eval("\"\\344\""));
  }

  @Test
  public void testOctalEscapes2() {
    // MVEL2: eval("'\\7'") returned "\7" (String with BEL char).
    // MVEL3: use a double-quoted string literal.
    assertEquals("\7", eval("\"\\7\""));
  }

  @Test
  public void testOctalEscapes3() {
    // Java parses \777 as the octal escape \77 followed by '7', producing "?7".
    // MVEL3: use a double-quoted string literal, preserving the MVEL2 expected value.
    assertEquals("\777", eval("\"\\777\""));
  }

  @Test
  public void testUniHex1() {
    // MVEL2: eval("'\\uFFFF::'") returned "\uFFFF::".
    // MVEL3: use a double-quoted string literal.
    assertEquals("\uFFFF::", eval("\"\\uFFFF::\""));
  }

  // -------------------------------------------------------------------------
  // Numeric literals — all pass unchanged in MVEL3.
  // -------------------------------------------------------------------------

  @Test
  public void testNumLiterals() {
    assertEquals(1e1f, eval("1e1f"));
  }

  @Test
  public void testNumLiterals2() {
    assertEquals(2.f, eval("2.f"));
  }

  @Test
  public void testNumLiterals3() {
    assertEquals(.3f, eval(".3f"));
  }

  @Test
  public void testNumLiterals4() {
    assertEquals(3.14f, eval("3.14f"));
  }

  @Test
  public void testNumLiterals5() {
    assertEquals(1e1, eval("1e1"));
  }

  @Test
  public void testNumLiterals6() {
    assertEquals(2., eval("2."));
  }

  @Test
  public void testNumLiterals7() {
    assertEquals(.3, eval(".3"));
  }

  @Test
  public void testNumLiterals8() {
    assertEquals(1e-9d, eval("1e-9d"));
  }

  @Test
  public void testNumLiterals9() {
    assertEquals(0x400921FB54442D18L, eval("0x400921FB54442D18L"));
  }
}
