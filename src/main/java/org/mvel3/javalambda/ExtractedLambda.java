package org.mvel3.javalambda;

import com.github.javaparser.ast.expr.LambdaExpr;
import java.nio.file.Path;

public record ExtractedLambda(
    Path sourceFile,
    int line,
    int column,
    LambdaExpr lambdaExpr,
    boolean capturing
) {}
