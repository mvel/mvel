/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.mvel;

import static java.lang.Character.isWhitespace;
import static java.lang.String.copyValueOf;
import java.util.ArrayList;
import java.util.List;

public class CompileException extends RuntimeException {
    private char[] expr;
    private int cursor;
    private List<ErrorDetail> errors;

    public CompileException() {
        super();
    }

    public CompileException(String message) {
        super(message);
    }

    public CompileException(String message, List<ErrorDetail> errors) {
        super(message);

        this.errors = errors;
        for (ErrorDetail error : errors) {
            System.err.println("* " + error.toString());
        }
        System.err.flush();
    }


    public CompileException(String message, char[] expr, int cursor, Exception e) {
        super("Failed to compile:\n[Error: " + message + "]\n[Near: { ... " + showCodeNearError(expr, cursor) + " ... } ]", e);
        this.expr = expr;
        this.cursor = cursor;
    }

    public CompileException(String message, char[] expr, int cursor) {
        super("Failed to compile:\n[Error: " + message + "]\n[Near: { ... " + showCodeNearError(expr, cursor) + " ... } ]");
        this.expr = expr;
        this.cursor = cursor;
    }

    public CompileException(String message, char[] expr, int cursor, boolean concatError) {
        super(concatError ? "Failed to compile:\n[Error: " + message + "]\n[Near: { ... " + showCodeNearError(expr, cursor) + " ... } ]" : message);
        this.expr = expr;
        this.cursor = cursor;
    }


    public CompileException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompileException(Throwable cause) {
        super(cause);
    }

    private static CharSequence showCodeNearError(char[] expr, int cursor) {
        if (expr == null) return "Unknown";

        int start = cursor - 10;
        int end = (cursor + 20);

        if (end > expr.length) {
            end = expr.length - 1;
            start -= 20;
        }

        if (start < 0) {
            start = 0;
        }

        while (start < end && isWhitespace(expr[start])) start++;
        //   while (end > start && isWhitespace(expr[end])) end--;

        return copyValueOf(expr, start, end - start);
    }


    public char[] getExpr() {
        return expr;
    }

    public int getCursor() {
        return cursor;
    }

    public List<ErrorDetail> getErrors() {
        return errors != null ? errors : new ArrayList(0);
    }

    public void setErrors(List<ErrorDetail> errors) {
        this.errors = errors;
    }
}
