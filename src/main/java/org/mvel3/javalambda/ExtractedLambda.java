package org.mvel3.javalambda;

import com.github.javaparser.ast.expr.LambdaExpr;
import java.nio.file.Path;
import java.util.List;

public record ExtractedLambda(
    Path sourceFile,
    int line,
    int column,
    LambdaExpr lambdaExpr,
    List<Class<?>> parameterTypes,
    boolean capturing
) {}
