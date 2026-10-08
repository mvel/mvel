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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mvel3.compat.mvel2.Mvel3TestSupport.eval;

/**
 * Imported from org.mvel2.tests.core.StringConcatWithHashTest (mvel2 branch).
 *
 * <p>Expressions, input values, expected values, and assertions are retained
 * unchanged. Failures are intentionally visible; known behavioural differences
 * are annotated with {@link KnownCompatibility}.
 */
@Tag("mvel2-compatibility")
public class StringConcatWithHashTest {

    @Test
    @KnownCompatibility("hash-concat-operator")
    public void testConcatWithHash() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("number1", Integer.valueOf(0));
        props.put("number2", Integer.valueOf(1));
        props.put("foo", "bar");
        Map<String, Object> vars = new HashMap<>();
        vars.put("props", props);

        eval("props['res'] = props['number1'] # props['number2']", vars);
        assertEquals("01", props.get("res"));

        eval("props['res'] = props['number1'] # props['foo']", vars);
        assertEquals("0bar", props.get("res"));

        eval("props['res'] = 'bar' # props['foo']", vars);
        assertEquals("barbar", props.get("res"));
    }
}
