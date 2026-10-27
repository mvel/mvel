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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mvel3.Evaluator;
import org.mvel3.MVEL;
import org.mvel3.Type;
import org.mvel3.compat.mvel2.res.Base;
import org.mvel3.compat.mvel2.res.Cheese;
import org.mvel3.compat.mvel2.res.Foo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porting of the string tests of CoreConfidenceTests from MVEL2 adapted for MVEL3 syntax.
 *
 * <p>Adaptations:
 * <ul>
 *   <li><b>single-quote-string</b>: single-quoted string literals are changed to double quotes {@code "..."}.</li>
 *   <li><b>block-syntax</b>: blocks end with an explicit {@code return}; variables are declared with types.</li>
 *   <li><b>void-method-expression</b>: a {@code void} call such as {@code System.out.println(..)} or
 *       {@code services.log(..)} is followed by {@code return null;}.</li>
 *   <li><b>property-assign-void-setter</b>: {@code a.explanation = ".."} is followed by {@code return null;}.</li>
 *   <li><b>string-index-access</b>: {@code a[4]} on a {@code String} is rewritten using {@code charAt(4)}.</li>
 *   <li><b>input-variable-redeclaration</b>: the local variables {@code list} and {@code a} are renamed
 *       ({@code strings}, {@code s}), because the test map of MVEL2 already had inputs of these names.</li>
 *   <li><b>static-import-method-call</b>: the method alias {@code percent}
 *       ({@code ParserConfiguration.addImport(name, Method)}) is replaced by {@code String.valueOf(..)}, and the
 *       logged text is asserted.</li>
 *   <li>The root object of {@code testRandomSomething} is passed as the {@code foo} input.</li>
 * </ul>
 */
public class CoreStringAdaptedTest {

  public interface Services {
    String A_CONST = "Hello World";

    void log(String text);
  }

  private static Object run(final String expression) {
    return new MVEL().executeExpression(expression, Set.of(), new HashMap<>());
  }

  private static Evaluator<Map<String, Object>, Void, Object> compile(final String expression,
                                                                       final Map<String, Type<?>> types) {
    if (expression.indexOf(';') > 0) {
      return new MVEL().compileMapBlock(expression, Object.class, Set.of(), types);
    }
    return new MVEL().compileMapExpression(expression, Object.class, Set.of(), types);
  }

  @Test
  public void testBracketInString() {
    assertThat(run("System.out.println(\"1)your guess was:\"); return null;")).isNull();
  }

  @Test
  public void testNesting() {
    assertThat(run("return new String(new String(new String(\"foo\")));")).isEqualTo("foo");
  }

  @Test
  public void testEdson() {
    assertThat(new MVEL().executeExpression("ArrayList<String> strings = new ArrayList<String>(); "
        + "strings.add(new String(\"foo\")); return strings.get(0);",
        Set.of("java.util.ArrayList"), new HashMap<>())).isEqualTo("foo");
  }

  @Test
  public void testStringIndex() {
    assertThat(run("String s = \"foobar\"; return s.charAt(4) == 'a';")).isEqualTo(true);
  }

  @Test
  public void testStringEscape() {
    List<?> result = (List<?>) run("[\"a\\\"b\"]");
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("a\"b");
  }

  @Test
  public void testConcatWithLineBreaks() {
    assertThat(run("\"foo\"+\n\"bar\"")).isEqualTo("foobar");
  }

  @Test
  public void testComaProblemStrikesBack() {
    Base a = new Base();
    Map<String, Object> variables = new HashMap<>();
    variables.put("a", a);

    new MVEL().executeExpression("a.explanation = \"There is a coma, in here\"; return null;",
        Set.of(), variables);

    assertThat(a.data).isEqualTo("There is a coma, in here");
  }

  @Test
  public void testStringConcatenation() {
    Map<String, Type<?>> types = new HashMap<>();
    types.put("$sb", Type.type(String.class));
    types.put("$av", Type.type(double.class));
    types.put("$pr", Type.type(double.class));
    types.put("services", Type.type(Services.class));

    compile("services.log((String) \"Drop +5%: \"+$sb+\" avg: $\"+$av+\" price: $\"+$pr ); return null;", types);
  }

  @Test
  public void testStringConcatenation2() {
    Map<String, Type<?>> types = new HashMap<>();
    types.put("$cheese", Type.type(Cheese.class));
    types.put("services", Type.type(Services.class));

    compile("services.log( $cheese + \" some string \" ); return null;", types);
  }

  @Test
  public void testStringConcatenation3() {
    List<String> logged = new ArrayList<>();
    Services services = logged::add;

    Map<String, Type<?>> types = new HashMap<>();
    types.put("$sb", Type.type(String.class));
    types.put("$av", Type.type(double.class));
    types.put("$pr", Type.type(double.class));
    types.put("services", Type.type(Services.class));

    Map<String, Object> vars = new HashMap<>();
    vars.put("services", services);
    vars.put("$sb", "RHT");
    vars.put("$av", 15.0);
    vars.put("$pr", 10.0);

    compile("services.log($av + \"Drop +5%: \"+$sb+\" avg: $\"+String.valueOf($av)+\" price: $\"+$pr ); return null;",
        types).eval(vars);

    assertThat(logged).containsExactly("15.0Drop +5%: RHT avg: $15.0 price: $10.0");
  }

  @Test
  public void testRandomExpression7() {
    assertThat(run("return \"foobar\".toUpperCase();")).isEqualTo("FOOBAR");
  }

  @Test
  public void testRandomExpression8() {
    assertThat(run("\"someString\".intern(); return \"someString\".hashCode() == \"someString\".hashCode();"))
        .isEqualTo(true);
  }

  @Test
  public void testRandomSomething() {
    Foo foo = new Foo();
    foo.setName("foo1");

    Foo foo2 = new Foo();
    foo2.setName("foo2");

    Evaluator<Map<String, Object>, Void, Object> evaluator =
        compile("foo.getName().toUpperCase()", Map.of("foo", Type.type(Foo.class)));

    assertThat(evaluator.eval(Map.of("foo", foo))).isEqualTo("FOO1");
    assertThat(evaluator.eval(Map.of("foo", foo2))).isEqualTo("FOO2");
  }

  @Test
  public void testJIRA96() {
    Evaluator<Map<String, Object>, Void, Object> evaluator =
        compile("fooString[0].toUpperCase()", Map.of("fooString", Type.type(String[].class)));

    assertThat(evaluator.eval(Map.of("fooString", new String[]{"abc"}))).isEqualTo("ABC");
  }

  @Test
  public void testJIRA122() {
    Evaluator<Map<String, Object>, Void, Object> evaluator = compile(
        "System.out.println(\">\"+java.lang.Character.toLowerCase(name.charAt(0))); "
            + "return java.lang.Character.toLowerCase(name.charAt(0)) == 'a';",
        Map.of("name", Type.type(String.class)));

    assertThat(evaluator.eval(Map.of("name", "Adam"))).isEqualTo(true);
    assertThat(evaluator.eval(Map.of("name", "Adam"))).isEqualTo(true);
  }

  @Test
  public void testJIRA122b() {
    Evaluator<Map<String, Object>, Void, Object> evaluator = compile(
        "System.out.println(\">\"+java.lang.Character.toLowerCase(name.charAt(0))); "
            + "return java.lang.Character.toLowerCase(name.charAt(0)) == 'a';",
        Map.of("name", Type.type(String.class)));

    assertThat(evaluator.eval(Map.of("name", "Adam"))).isEqualTo(true);
    assertThat(evaluator.eval(Map.of("name", "Adam"))).isEqualTo(true);
  }

  @Test
  public void testJIRA183() {
    String exp1 = "int end = \"attribute\".indexOf(\"@\");  if(end == -1)"
        + " { end = \"attribute\".length(); } return \"attribute\".substring(0, end);";
    assertThat(run(exp1)).isEqualTo("attribute");

    String exp2 = "int end = \"attribute\".indexOf(\"@\");  if(end == -1)"
        + " { end = \"attribute\".length(); } return \"attribute\".substring(0, end);";
    assertThat(run(exp2)).isEqualTo("attribute");
  }
}
