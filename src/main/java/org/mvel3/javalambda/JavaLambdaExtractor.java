package org.mvel3.javalambda;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import org.mvel3.lambdaextractor.LambdaCatalog;
import org.mvel3.lambdaextractor.LambdaKey;
import org.mvel3.lambdaextractor.LambdaUtils;
import org.mvel3.lambdaextractor.RegistrationResult;
import org.mvel3.transpiler.VariableAnalyser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JavaLambdaExtractor {

    private final List<LambdaSignature> signatures = new ArrayList<>();

    public void addTargetSignature(String samInterfaceFqn, String methodName, int paramCount) {
        signatures.add(new LambdaSignature(samInterfaceFqn, methodName, paramCount));
    }

    public ExtractionResult extract(List<Path> sourceFiles, TypeSolver typeSolver) {
        StaticJavaParser.getParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(typeSolver));

        LambdaCatalog catalog = new LambdaCatalog();
        LambdaFinder finder = new LambdaFinder(signatures);

        List<NormalizedLambda> allLambdas = new ArrayList<>();
        Map<Integer, NormalizedLambda> uniqueMap = new LinkedHashMap<>();
        int skippedCaptures = 0;

        for (Path file : sourceFiles) {
            CompilationUnit cu;
            try {
                cu = StaticJavaParser.parse(Files.readString(file));
            } catch (IOException e) {
                continue;
            }

            List<ExtractedLambda> extracted = finder.find(cu, file, typeSolver);

            for (ExtractedLambda el : extracted) {
                if (el.capturing()) {
                    skippedCaptures++;
                    continue;
                }

                String methodName = signatures.stream()
                        .filter(s -> s.paramCount() == el.parameterTypes().size())
                        .map(LambdaSignature::methodName)
                        .findFirst().orElse("eval");

                MethodDeclaration syntheticMethod = LambdaConverter.toMethodDeclaration(
                        el.lambdaExpr(), methodName, el.parameterTypes());

                LambdaKey key = LambdaUtils.createLambdaKeyFromMethodDeclaration(syntheticMethod);

                Set<String> readProperties = extractReadProperties(syntheticMethod);

                RegistrationResult regResult = catalog.register(key);

                String samFqn = signatures.stream()
                        .filter(s -> s.paramCount() == el.parameterTypes().size())
                        .map(LambdaSignature::samInterfaceFqn)
                        .findFirst().orElse("");

                NormalizedLambda normalized = new NormalizedLambda(
                        el.sourceFile(), el.line(), el.column(),
                        key, regResult.physicalId(), regResult.reused(),
                        readProperties, el.lambdaExpr(), samFqn);

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

    private Set<String> extractReadProperties(MethodDeclaration md) {
        Set<String> paramNames = new HashSet<>();
        md.getParameters().forEach(p -> paramNames.add(p.getNameAsString()));
        VariableAnalyser analyser = new VariableAnalyser(paramNames);
        md.getBody().ifPresent(body -> body.accept(analyser, null));
        return analyser.getReadProperties();
    }
}
