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

package org.mvel2.util;

import java.io.Serializable;
import java.lang.reflect.Method;


public class MethodStub implements Serializable {
    private Class classReference;
    private String methodName;

    private transient Method method;

    public MethodStub() {
    }

    public MethodStub(Method method) {
        this.classReference = method.getDeclaringClass();
        this.methodName = method.getName();
    }

    public MethodStub(Class classReference, String methodName) {
        this.classReference = classReference;
        this.methodName = methodName;
    }

    public Class getClassReference() {
        return classReference;
    }

    public void setClassReference(Class classReference) {
        this.classReference = classReference;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Method getMethod() {
        if (method == null) {
            for (Method method : classReference.getMethods()) {
                if (methodName.equals(method.getName())) return this.method = method;
            }
        }
        return method;
    }
}
