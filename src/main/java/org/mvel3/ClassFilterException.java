/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel3;

import java.util.Collections;
import java.util.List;

/**
 * Thrown at compile time when the configured {@link ClassFilter} rejects one
 * or more class references in an MVEL expression. All violations found during
 * a single compile are batched into a single exception so the author can see
 * every problem at once rather than fixing them one at a time.
 */
public class ClassFilterException extends RuntimeException {

    /**
     * One blocked class reference, with enough context for an error message.
     * {@code line} and {@code column} are 1-based; negative values mean the
     * position was unavailable. {@code reason} is a human-readable phrase
     * describing why the class was rejected (e.g. "not on the allowlist",
     * "blocked by ClassFilter").
     */
    public record Violation(String className, int line, int column, String reason) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (line > 0) {
                sb.append("line ").append(line);
                if (column > 0) {
                    sb.append(", col ").append(column);
                }
                sb.append(": ");
            }
            sb.append("class '").append(className).append("' ").append(reason);
            return sb.toString();
        }
    }

    private final List<Violation> violations;

    public ClassFilterException(List<Violation> violations) {
        super(buildMessage(violations));
        this.violations = List.copyOf(violations);
    }

    public List<Violation> getViolations() {
        return Collections.unmodifiableList(violations);
    }

    private static String buildMessage(List<Violation> violations) {
        if (violations.isEmpty()) {
            return "ClassFilter rejected the expression (no details)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(violations.size())
          .append(" blocked class reference(s) in expression:");
        for (Violation v : violations) {
            sb.append("\n  - ").append(v);
        }
        return sb.toString();
    }
}
