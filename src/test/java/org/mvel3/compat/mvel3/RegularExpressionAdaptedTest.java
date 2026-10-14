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

/**
 * Porting of RegularExpressionTests from MVEL2 adapted for MVEL3.
 *
 * <p>MVEL2 provides a {@code ~=} binary operator for regular expression matching.
 * MVEL3 does not support the {@code ~=} regex operator (see {@code regex-operator}
 * in {@code Compatibility.md}).
 *
 * <p>These tests are disabled in MVEL3 as the operator is unsupported.
 */
public class RegularExpressionAdaptedTest {

    @Test
    @Disabled("regex-operator: ~= is not supported in MVEL3")
    public void testRegExpOK() {
    }

    @Test
    @Disabled("regex-operator: ~= is not supported in MVEL3")
    public void testRegExpPreCompiledBug() {
    }

    @Test
    @Disabled("regex-operator: ~= is not supported in MVEL3")
    public void testRegExpOrBug() {
    }

    @Test
    @Disabled("regex-operator: ~= is not supported in MVEL3")
    public void testRegExpAndBug() {
    }

    @Test
    @Disabled("regex-operator: ~= is not supported in MVEL3")
    public void testRegExSurroundedByBrackets() {
    }

    @Test
    @Disabled("regex-operator: ~= is not supported in MVEL3")
    public void testMVEL231() {
    }

    @Test
    @Disabled("regex-operator: ~= is not supported in MVEL3")
    public void testParsingStability4() {
    }

    @Test
    @Disabled("regex-operator: ~= is not supported in MVEL3")
    public void testRegExOR() {
    }

    @Test
    @Disabled("regex-operator: ~= is not supported in MVEL3")
    public void testRegExOR2() {
    }

    @Test
    @Disabled("regex-operator: ~= is not supported in MVEL3")
    public void testRegExMatch() {
    }
}
