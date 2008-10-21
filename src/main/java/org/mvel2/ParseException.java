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

public class ParseException extends RuntimeException {
    private int cursorPosition;
    private String message;

    public ParseException() {
        super();
    }

    public ParseException(String message) {
        this.message = message;
    }

    public ParseException(String message, char[] expr, int cursorPosition) {
        super();

        this.cursorPosition = cursorPosition;
        int start = cursorPosition - 15;
        int end = cursorPosition + 15;

        if (start < 0) {
            end += (0 - start);
            start = 0;
        }

        if (end > expr.length) {
            end = expr.length;
        }

        String nearCode = new String(expr, start, end - start);


        this.message = message + " (near code: << ... " + nearCode + " ...>>) (position: " + cursorPosition + ")";
    }


    public ParseException(String message, Throwable cause) {
        super(cause);
        this.message = message;
    }

    public ParseException(Throwable cause) {
        super(cause);
    }


    public String getMessage() {
        return message;
    }


    public int getCursorPosition() {
        return cursorPosition;
    }
}
