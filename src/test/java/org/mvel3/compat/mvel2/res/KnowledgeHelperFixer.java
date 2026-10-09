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

package org.mvel3.compat.mvel2.res;

import java.util.Set;

/**
 * Imported from org.mvel2.tests.core.res.KnowledgeHelperFixer (mvel2 branch).
 *
 * <p>The original used {@code MVEL.parseMacros()} to substitute Drools macro
 * keywords (insert, update, retract, …) with prefixed equivalents.  MVEL3 has
 * no macro processor, so this copy scans the six keywords while preserving
 * quoted literals, comments, and MVEL2's macro-arming rules.
 */
public class KnowledgeHelperFixer {

    private static final Set<String> MACROS = Set.of(
            "insert", "insertLogical", "modifyRetract", "modifyInsert", "retract", "update");

    public String fix(final String raw) {
        if (raw == null) {
            return raw;
        }
        final StringBuilder result = new StringBuilder(raw.length());
        int cursor = 0;
        boolean macroArmed = true;
        while (cursor < raw.length()) {
            final int start = cursor;
            final char current = raw.charAt(cursor);
            if (Character.isJavaIdentifierPart(current)) {
                while (cursor < raw.length() && Character.isJavaIdentifierPart(raw.charAt(cursor))) {
                    cursor++;
                }
                final String token = raw.substring(start, cursor);
                if (macroArmed && MACROS.contains(token)) {
                    result.append("drools.");
                }
            } else if (current == '"' || current == '\'') {
                cursor++;
                while (cursor < raw.length()) {
                    final char quoted = raw.charAt(cursor++);
                    if (quoted == '\\' && cursor < raw.length()) {
                        cursor++;
                    } else if (quoted == current) {
                        break;
                    }
                }
            } else if (raw.startsWith("//", cursor)) {
                final int end = raw.indexOf('\n', cursor + 2);
                cursor = end < 0 ? raw.length() : end;
            } else if (raw.startsWith("/*", cursor)) {
                final int end = raw.indexOf("*/", cursor + 2);
                cursor = end < 0 ? raw.length() : end + 2;
            } else {
                // MVEL2 disarms macros after a dot until one of these delimiters.
                if (current == '.') {
                    macroArmed = false;
                } else if (current == ';' || current == '{' || current == '(') {
                    macroArmed = true;
                }
                cursor++;
            }
            result.append(raw, start, cursor);
        }
        return result.toString();
    }
}
