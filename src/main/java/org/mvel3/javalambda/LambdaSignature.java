package org.mvel3.javalambda;

public record LambdaSignature(
    String samInterfaceFqn,
    String methodName,
    int paramCount
) {}
