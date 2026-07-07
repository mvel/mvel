package org.mvel3.javalambda;

import com.github.javaparser.ast.expr.LambdaExpr;
import org.mvel3.lambdaextractor.LambdaKey;
import java.nio.file.Path;
import java.util.Set;

public record NormalizedLambda(
    Path sourceFile,
    int line,
    int column,
    LambdaKey key,
    int physicalId,
    boolean reused,
    Set<String> readProperties,
    LambdaExpr originalLambdaExpr
) {}
