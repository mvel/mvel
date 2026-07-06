package org.mvel3.javalambda;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class LambdaSourceRewriter {

    private final String registryFqn;
    private final String registrySimpleName;

    public LambdaSourceRewriter(String registryFqn) {
        this.registryFqn = registryFqn;
        int dot = registryFqn.lastIndexOf('.');
        this.registrySimpleName = dot >= 0 ? registryFqn.substring(dot + 1) : registryFqn;
    }

    public String rewrite(ExtractionResult result, Path sourceFile) throws IOException {
        String source = Files.readString(sourceFile);

        List<NormalizedLambda> lambdasInFile = result.allLambdas().stream()
                .filter(nl -> nl.sourceFile().equals(sourceFile))
                .sorted(Comparator.comparingInt(NormalizedLambda::line).reversed()
                        .thenComparing(Comparator.comparingInt(NormalizedLambda::column).reversed()))
                .toList();

        if (lambdasInFile.isEmpty()) {
            return source;
        }

        String[] lines = source.split("\n", -1);

        for (NormalizedLambda nl : lambdasInFile) {
            String fieldRef = registrySimpleName + ".LAMBDA_" + nl.physicalId();
            String lambdaText = nl.originalLambdaExpr().toString();
            int lineIdx = nl.line() - 1;
            if (lineIdx >= 0 && lineIdx < lines.length) {
                lines[lineIdx] = lines[lineIdx].replace(lambdaText, fieldRef);
            }
        }

        String rewritten = String.join("\n", lines);

        String importStatement = "import " + registryFqn + ";";
        if (!rewritten.contains(importStatement)) {
            int insertPos = findImportInsertPosition(rewritten);
            rewritten = rewritten.substring(0, insertPos) + importStatement + "\n" + rewritten.substring(insertPos);
        }

        return rewritten;
    }

    private int findImportInsertPosition(String source) {
        int lastImport = source.lastIndexOf("\nimport ");
        if (lastImport >= 0) {
            int endOfLine = source.indexOf('\n', lastImport + 1);
            return endOfLine + 1;
        }
        int packageEnd = source.indexOf(';');
        if (packageEnd >= 0) {
            int endOfLine = source.indexOf('\n', packageEnd);
            return endOfLine + 1;
        }
        return 0;
    }
}
