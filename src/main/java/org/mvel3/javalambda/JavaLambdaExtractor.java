package org.mvel3.javalambda;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import org.mvel3.lambdaextractor.LambdaCatalog;
import org.mvel3.lambdaextractor.LambdaKey;
import org.mvel3.lambdaextractor.LambdaUtils;
import org.mvel3.lambdaextractor.RegistrationResult;
import org.mvel3.lambdaextractor.VariableNameNormalizerVisitor;
import org.mvel3.transpiler.VariableAnalyser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JavaLambdaExtractor {

    public ExtractionResult extract(List<Path> sourceFiles, TypeSolver typeSolver) {
        StaticJavaParser.getParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        // Phase 1: find all lambdas, normalize body, group by hash
        Map<String, List<ExtractedLambda>> bodyGroups = new HashMap<>();
        int skippedCaptures = 0;

        for (Path file : sourceFiles) {
            CompilationUnit cu;
            try {
                cu = StaticJavaParser.parse(Files.readString(file));
            } catch (IOException e) {
                continue;
            }

            for (LambdaExpr lambda : cu.findAll(LambdaExpr.class)) {
                if (CaptureDetector.isCapturing(lambda)) {
                    skippedCaptures++;
                    continue;
                }

                int line = lambda.getBegin().map(p -> p.line).orElse(-1);
                int column = lambda.getBegin().map(p -> p.column).orElse(-1);
                ExtractedLambda el = new ExtractedLambda(file, line, column, lambda, false);

                String normalizedBody = normalizeBody(lambda);
                bodyGroups.computeIfAbsent(normalizedBody, k -> new ArrayList<>()).add(el);
            }
        }

        // Phase 2: only process groups with 2+ members (duplicates)
        LambdaCatalog catalog = new LambdaCatalog();
        List<NormalizedLambda> allLambdas = new ArrayList<>();
        Map<Integer, NormalizedLambda> uniqueMap = new LinkedHashMap<>();

        for (Map.Entry<String, List<ExtractedLambda>> group : bodyGroups.entrySet()) {
            if (group.getValue().size() < 2) {
                continue;
            }

            for (ExtractedLambda el : group.getValue()) {
                MethodDeclaration syntheticMethod = LambdaConverter.toMethodDeclaration(
                        el.lambdaExpr(), "eval");

                LambdaKey key = LambdaUtils.createLambdaKeyFromMethodDeclaration(syntheticMethod);
                Set<String> readProperties = extractReadProperties(syntheticMethod);
                RegistrationResult regResult = catalog.register(key);
                String resolvedType = resolveTargetType(el.lambdaExpr());

                NormalizedLambda normalized = new NormalizedLambda(
                        el.sourceFile(), el.line(), el.column(),
                        key, regResult.physicalId(), regResult.reused(),
                        readProperties, el.lambdaExpr(), resolvedType);

                allLambdas.add(normalized);
                if (!regResult.reused()) {
                    uniqueMap.put(regResult.physicalId(), normalized);
                }
            }
        }

        int reusedCount = (int) allLambdas.stream().filter(NormalizedLambda::reused).count();

        return new ExtractionResult(
                Collections.unmodifiableList(allLambdas),
                Collections.unmodifiableMap(uniqueMap),
                allLambdas.size(),
                uniqueMap.size(),
                reusedCount,
                skippedCaptures);
    }

    private String resolveTargetType(LambdaExpr lambda) {
        try {
            return lambda.calculateResolvedType().describe();
        } catch (Exception e) {
            return "Object";
        }
    }

    private String normalizeBody(LambdaExpr lambda) {
        LambdaExpr clone = lambda.clone();
        LambdaExpr normalized = VariableNameNormalizerVisitor.normalize(clone);
        return normalized.getBody().toString();
    }

    private Set<String> extractReadProperties(MethodDeclaration md) {
        Set<String> paramNames = new HashSet<>();
        md.getParameters().forEach(p -> paramNames.add(p.getNameAsString()));
        VariableAnalyser analyser = new VariableAnalyser(paramNames);
        md.getBody().ifPresent(body -> body.accept(analyser, null));
        return analyser.getReadProperties();
    }
}
