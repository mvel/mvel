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

public interface NodeType {
    public static final int IF = 0;
    public static final int FOREACH = 1;
    public static final int ELSEIF = 2;
    public static final int ELSE = 3;
    public static final int END = 4;
    public static final int PROPERTY_EX = 5;
    public static final int LITERAL = 6;
    public static final int TERMINUS = 7;
    public static final int GOTO = 8;
    public static final int OPERATOR = 9;
    public static final int INCLUDE_BY_REF = 10;
}
