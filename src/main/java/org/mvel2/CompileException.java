/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
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
 */

package org.mvel2;

import static org.mvel2.util.ParseTools.isWhitespace;
import static org.mvel2.util.ParseTools.repeatChar;

import org.mvel2.util.StringAppender;

import static java.lang.String.copyValueOf;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard exception thrown for all general compile and some runtime failures.
 */
public class CompileException extends RuntimeException {
    private char[] expr;

    private int cursor = 0;
    private int msgOffset = 0;

    private int lineNumber = 1;
    private int column = 0;

    private int lastLineStart = 0;

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
    }

    public CompileException(String message, int cursor) {
        super(message);
        this.cursor = cursor;
    }

    public String toString() {
       return generateErrorMessage();
    }

    public CompileException(String message, char[] expr, int cursor, Throwable e) {
        super(message, e);
        this.expr = expr;
        this.cursor = cursor;
    }

    public CompileException(String message, char[] expr, int cursor) {
        super(message);
        this.expr = expr;
        this.cursor = cursor;
    }

    public CompileException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompileException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return generateErrorMessage();
    }

    private CharSequence showCodeNearError(char[] expr, int cursor) {
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

        CharSequence cs = null;

        try {
            cs = copyValueOf(expr, start, end - start);
        }
        catch (StringIndexOutOfBoundsException e) {
            System.out.println("");
            throw e;
        }

        msgOffset = start;

        return cs;
    }

    private String generateErrorMessage() {
        StringAppender appender = new StringAppender().append("[Error: " + super.getMessage() + "]\n");

        int offset = appender.length();

        appender.append("[Near : {... ");

        offset = appender.length() - offset;

        appender.append(showCodeNearError(expr, cursor))
                .append(" ....}]\n")
                .append(repeatChar(' ', offset));

        if ((offset = cursor - msgOffset - 1) < 0) offset = 0;

        appender.append(repeatChar(' ', offset)).append("^");

        if (lineNumber != -1) {
            appender.append('\n')
                    .append("[Line: " + lineNumber + ", Column: " + column + "]");
        }
        return appender.toString();
    }

    public char[] getExpr() {
        return expr;
    }

    public int getCursor() {
        return cursor;
    }

    public List<ErrorDetail> getErrors() {
        return errors != null ? errors : new ArrayList<ErrorDetail>(0);
    }

    public void setErrors(List<ErrorDetail> errors) {
        this.errors = errors;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setExpr(char[] expr) {
        this.expr = expr;
    }

    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

    public int getLastLineStart() {
        return lastLineStart;
    }

    public void setLastLineStart(int lastLineStart) {
        this.lastLineStart = lastLineStart;
    }
}
