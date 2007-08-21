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

public class PropertyAccessException extends RuntimeException {

    public PropertyAccessException() {
        super();
    }

    public PropertyAccessException(String message) {
        super("unable to resolve property: " + message);
    }

    public PropertyAccessException(String message, Throwable cause) {
        super("unable to resolve property: " + message, cause);
    }

    public PropertyAccessException(Throwable cause) {
        super(cause);
    }

}
