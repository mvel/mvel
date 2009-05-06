package org.mvel.util;

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
