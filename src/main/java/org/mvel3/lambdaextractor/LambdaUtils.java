package org.mvel3.lambdaextractor;

import java.nio.charset.StandardCharsets;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.mvel3.methodutils.Murmur3F;

public class LambdaUtils {

    private LambdaUtils() {
        // Prevent instantiation
    }

    /**
     * Helper method to create a LambdaKey from a full compilation unit string
     * Note: LambdaKey contains only the normalized method declaration
     */
    public static LambdaKey createLambdaKeyFromCompilationUnit(String compilationUnitStr) {
        CompilationUnit compilationUnit = StaticJavaParser.parse(compilationUnitStr);
        MethodDeclaration methodDecl = compilationUnit.findFirst(MethodDeclaration.class).orElseThrow();
        MethodDeclaration normalized = VariableNameNormalizerVisitor.normalize(methodDecl);
        String normalizedStr = normalized.toString();
        String signature = extractSignature(normalized);
        return new LambdaKey(normalizedStr, signature);
    }

    /**
     * Helper method to create a LambdaKey from a method declaration string
     */
    public static LambdaKey createLambdaKeyFromMethodDeclaration(String methodDeclaration) {
        MethodDeclaration methodDecl = StaticJavaParser.parseMethodDeclaration(methodDeclaration);
        MethodDeclaration normalized = VariableNameNormalizerVisitor.normalize(methodDecl);
        String normalizedStr = normalized.toString();
        String signature = extractSignature(normalized);
        return new LambdaKey(normalizedStr, signature);
    }

    /**
     * Extract method signature from normalized method declaration
     */
    private static String extractSignature(MethodDeclaration method) {
        // Extract return type and parameters (without method body)
        StringBuilder sig = new StringBuilder();
        sig.append(method.getType().asString()).append(" ");
        sig.append(method.getNameAsString()).append("(");
        for (int i = 0; i < method.getParameters().size(); i++) {
            if (i > 0) sig.append(", ");
            sig.append(method.getParameter(i).toString());
        }
        sig.append(")");
        return sig.toString();
    }

    /**
     * Calculate hash from normalized method string
     */
    public static int calculateHash(String normalizedMethod) {
        Murmur3F hasher = new Murmur3F();
        hasher.update(normalizedMethod.getBytes(StandardCharsets.UTF_8));
        return (int) hasher.getValue();
    }
}
