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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.VarargTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class VarargTests extends AbstractTest {
  public static class MyVarargTestee {
    private StringBuilder sb = new StringBuilder();
    public MyVarargTestee() {
    }
    public MyVarargTestee(String arg) {
      sb.append("singleArgCtor" + arg);
    }
    public MyVarargTestee(String ... arg) {
      sb.append("multipleArgsCtor" + Arrays.asList(arg));
    }
    public void add(String arg) {
      sb.append("singleArgMethod" + arg);
    }
    public void add(String ... arg) {
      sb.append("multipleArgsMethod" + Arrays.asList(arg));
    }
    public String toString() {
      return sb.toString();
    }
  }

  @Test
  @KnownCompatibility("void-method-expression")
  public void testSingleArgMethodPreference() {
    Serializable s = compileExpression("testee.add('a')");
    MyVarargTestee testee = new MyVarargTestee();
    executeExpression(s, Collections.singletonMap("testee", testee));
    Assert.assertEquals("singleArgMethoda", testee.toString());
  }

  @Test
  @KnownCompatibility("void-method-expression")
  public void testVarargMethod() {
      Serializable s = compileExpression("testee.add('a', 'b')");
      MyVarargTestee testee = new MyVarargTestee();
      executeExpression(s, Collections.singletonMap("testee", testee));
      Assert.assertEquals("multipleArgsMethod[a, b]", testee.toString());
  }

  @Test
  @KnownCompatibility("inner-class-binary-name")
  public void testNoArgCtorPreference() {
      Serializable s = compileExpression("new " + MyVarargTestee.class.getName() + "()");
      MyVarargTestee result = (MyVarargTestee) executeExpression(s);
      Assert.assertEquals("", result.toString());
  }

  @Test
  @KnownCompatibility("inner-class-binary-name")
  public void testSingleArgCtorPreference() {
      Serializable s = compileExpression("new " + MyVarargTestee.class.getName() + "('a')");
      MyVarargTestee result = (MyVarargTestee) executeExpression(s);
      Assert.assertEquals("singleArgCtora", result.toString());
  }

  @Test
  @KnownCompatibility("inner-class-binary-name")
  public void testVarargCtor() {
      Serializable s = compileExpression("new " + MyVarargTestee.class.getName() + "('a', 'b')");
      MyVarargTestee result = (MyVarargTestee) executeExpression(s);
      Assert.assertEquals("multipleArgsCtor[a, b]", result.toString());
  }
}
