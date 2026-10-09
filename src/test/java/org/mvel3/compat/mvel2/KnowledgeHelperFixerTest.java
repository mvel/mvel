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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mvel3.compat.mvel2.res.KnowledgeHelperFixer;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeHelperFixerTest {

    private final KnowledgeHelperFixer fixer = new KnowledgeHelperFixer();

    @ParameterizedTest
    @ValueSource(strings = {"insert", "insertLogical", "modifyRetract", "modifyInsert", "retract", "update"})
    void replacesMacroIdentifiers(final String macro) {
        assertThat(fixer.fix(macro + "(x)")).isEqualTo("drools." + macro + "(x)");
    }

    @ParameterizedTest
    @ValueSource(strings = {"inserted", "preinsert", "$insert", "_update", "éupdate", "update1", "1insert"})
    void preservesOtherIdentifiers(final String identifier) {
        assertThat(fixer.fix(identifier)).isEqualTo(identifier);
    }

    @Test
    void preservesQuotedLiteralsAndEscapedQuotes() {
        final String raw = """
            "insert" 'update' "escaped \\"retract\\"" 'escaped \\'modifyInsert\\''; insert(x);
            """;
        final String expected = """
            "insert" 'update' "escaped \\"retract\\"" 'escaped \\'modifyInsert\\''; drools.insert(x);
            """;
        assertThat(fixer.fix(raw)).isEqualTo(expected);
    }

    @Test
    void preservesLineCommentsAndLineEndings() {
        final String raw = """
            // insert update " ; { (
            update(x); // retract insertLogical
            """.replace("\n", "\r\n");
        final String expected = """
            // insert update " ; { (
            drools.update(x); // retract insertLogical
            """.replace("\n", "\r\n");
        assertThat(fixer.fix(raw)).isEqualTo(expected);
        assertThat(fixer.fix("// insert")).isEqualTo("// insert");
    }

    @Test
    void preservesBlockComments() {
        final String raw = """
            /* insert ' " update ; { (
               modifyRetract modifyInsert */ retract(x); /* insertLogical */
            """;
        final String expected = """
            /* insert ' " update ; { (
               modifyRetract modifyInsert */ drools.retract(x); /* insertLogical */
            """;
        assertThat(fixer.fix(raw)).isEqualTo(expected);
    }

    @Test
    void preservesQualifiedCalls() {
        assertThat(fixer.fix("service.update(x); drools.insert(x); service. /* update */ retract(x)"))
                .isEqualTo("service.update(x); drools.insert(x); service. /* update */ retract(x)");
    }

    @Test
    void rearmsMacrosAtParenthesesSemicolonsAndBlocks() {
        assertThat(fixer.fix("service.update(insert(x)); update(x); service.update { retract(x); }"))
                .isEqualTo("service.update(drools.insert(x)); drools.update(x); service.update { drools.retract(x); }");
    }

    @Test
    void keepsMacrosDisarmedUntilARearmingDelimiter() {
        assertThat(fixer.fix("service.update /* ; { ( */ + insert; insert(x)"))
                .isEqualTo("service.update /* ; { ( */ + insert; drools.insert(x)");
    }

    @Test
    void preservesNullAndEmptyInput() {
        assertThat(fixer.fix(null)).isNull();
        assertThat(fixer.fix("")).isEmpty();
    }
}
