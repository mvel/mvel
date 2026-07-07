package org.mvel3.javalambda;

import java.util.Map;
import java.util.Set;

public final class LambdaRegistryGenerator {

    private final String packageName;
    private final String className;

    public LambdaRegistryGenerator(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
    }

    public String generate(ExtractionResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("public final class ").append(className).append(" {\n\n");
        sb.append("    private ").append(className).append("() {}\n\n");

        for (Map.Entry<Integer, NormalizedLambda> entry : result.uniqueMap().entrySet()) {
            int physicalId = entry.getKey();
            NormalizedLambda lambda = entry.getValue();
            String fieldName = "LAMBDA_" + physicalId;

            String lambdaSource = lambda.originalLambdaExpr().toString();
            String fieldType = lambda.resolvedTypeFqn();

            sb.append("    public static final ").append(fieldType)
                    .append(" ").append(fieldName).append(" = ")
                    .append(lambdaSource).append(";\n\n");

            Set<String> props = lambda.readProperties();
            sb.append("    public static final String[] ").append(fieldName).append("_READ_PROPS = {");
            sb.append(String.join(", ", props.stream().map(p -> "\"" + p + "\"").toList()));
            sb.append("};\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }
}
