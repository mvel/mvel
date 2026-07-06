package org.mvel3.javalambda;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.TypeSolver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class LambdaFinder {

    private final List<LambdaSignature> signatures;
    private final Set<String> targetFqns;

    public LambdaFinder(List<LambdaSignature> signatures) {
        this.signatures = List.copyOf(signatures);
        this.targetFqns = signatures.stream()
                .map(LambdaSignature::samInterfaceFqn)
                .collect(Collectors.toSet());
    }

    public List<ExtractedLambda> find(CompilationUnit cu, Path sourceFile, TypeSolver typeSolver) {
        List<ExtractedLambda> results = new ArrayList<>();

        cu.findAll(LambdaExpr.class).forEach(lambda -> {
            try {
                ResolvedType resolvedType = lambda.calculateResolvedType();
                String typeFqn = resolvedType.describe();
                int genericStart = typeFqn.indexOf('<');
                String rawFqn = genericStart > 0 ? typeFqn.substring(0, genericStart) : typeFqn;

                if (targetFqns.contains(rawFqn)) {
                    int paramCount = lambda.getParameters().size();
                    boolean signatureMatches = signatures.stream()
                            .anyMatch(s -> s.samInterfaceFqn().equals(rawFqn) && s.paramCount() == paramCount);
                    if (signatureMatches) {
                        List<Class<?>> paramTypes = resolveParameterTypes(lambda, paramCount);
                        boolean capturing = CaptureDetector.isCapturing(lambda);
                        int line = lambda.getBegin().map(p -> p.line).orElse(-1);
                        int column = lambda.getBegin().map(p -> p.column).orElse(-1);
                        results.add(new ExtractedLambda(sourceFile, line, column,
                                lambda, paramTypes, capturing));
                    }
                }
            } catch (Exception e) {
                // Type resolution failed — skip this lambda
            }
        });

        return results;
    }

    private List<Class<?>> resolveParameterTypes(LambdaExpr lambda, int paramCount) {
        List<Class<?>> types = new ArrayList<>();
        for (int i = 0; i < paramCount; i++) {
            try {
                ResolvedType paramType = lambda.getParameters().get(i).getType().resolve();
                types.add(Class.forName(paramType.describe()));
            } catch (Exception e) {
                types.add(Object.class);
            }
        }
        return types;
    }
}
