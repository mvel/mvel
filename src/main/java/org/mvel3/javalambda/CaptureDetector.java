package org.mvel3.javalambda;

import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.body.Parameter;

import java.util.Set;
import java.util.stream.Collectors;

public final class CaptureDetector {

    private CaptureDetector() {}

    /**
     * Returns true if the lambda body references any NameExpr that is not
     * one of the lambda's own parameters. This is a conservative check:
     * it treats any non-parameter name reference (including static fields,
     * class names used as bare identifiers) as a capture. Method call
     * scopes (e.g., s in s.length()) are parameters and pass.
     */
    public static boolean isCapturing(LambdaExpr lambdaExpr) {
        Set<String> paramNames = lambdaExpr.getParameters().stream()
                .map(Parameter::getNameAsString)
                .collect(Collectors.toSet());

        return lambdaExpr.getBody().findAll(NameExpr.class).stream()
                .map(NameExpr::getNameAsString)
                .anyMatch(name -> !paramNames.contains(name));
    }
}
