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

import java.util.HashMap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.mvel3.compat.mvel2.Mvel3TestSupport.compileExpression;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.ScopeTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class ScopeTests extends AbstractTest {

    @Test
    public void testNoScopeLeakageInterpreted() {
        String ex = "if (true) { var i = 0 }; i";

        try {
            eval(ex, new HashMap<String, Object>());
            fail("should have failed");
        }
        catch (Exception e) {
            // good!
        }
    }

    @Test
    public void testNoScopeLeakageCompiled() {
        String ex = "if (true) { var i = 0 }; i";

        try {
            // MVEL2's ParserContext.create().stronglyTyped() has no MVEL3 equivalent
            // (MVEL3 is always statically typed). Mvel3TestSupport compiles lazily on
            // first evaluation, so the expression is executed to trigger compilation.
            executeExpression(compileExpression(ex));
            fail("should have failed");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
