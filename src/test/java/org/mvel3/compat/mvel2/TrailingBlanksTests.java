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
import static org.mvel3.compat.mvel2.Mvel3TestSupport.executeExpression;

/**
 * Imported from org.mvel2.tests.core.TrailingBlanksTests (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class TrailingBlanksTests extends AbstractTest {

    @Test
    @KnownCompatibility("single-quote-string")
    public void testWithTrailingNewLine() {
        String expression = "if (true) {'ok';} else if (false) {'ko';} else {'ko';}\n";
        Object result = executeExpression(compileExpression(expression));
        assertEquals("ok", result);
    }

    @Test
    @KnownCompatibility("single-quote-string")
    public void testWithTrailingBlank() {
        String expression = "if (true) {'ok';} else if (false) {'ko';} else {'ko';} ";
        Object result = executeExpression(compileExpression(expression));
        assertEquals("ok", result);
    }

    @Test
    @KnownCompatibility("single-quote-string")
    public void testWithoutTrailingBlank() {
        String expression = "if (true) {'ok';} else if (false) {'ko';} else {'ko';}";
        Object result = executeExpression(compileExpression(expression));
        assertEquals("ok", result);
    }
}
