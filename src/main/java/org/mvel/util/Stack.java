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
package org.mvel.util;

import java.io.Serializable;

public interface Stack extends Serializable {
    public boolean isEmpty();

    public Object peek();

    public Object peek2();

    public void add(Object obj);

    public void push(Object obj);

    public Object pushAndPeek(Object obj);

    public void push(Object obj1, Object obj2);

    public void push(Object obj1, Object obj2, Object obj3);

    public Object pop();

    public void discard();

    public void clear();

    public int size();

    public void showStack();
}
