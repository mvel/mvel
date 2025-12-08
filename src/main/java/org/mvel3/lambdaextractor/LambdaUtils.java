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
     * Note: LambdaKey contains only the normalized method declaration. So the class name does not affect the equality.
     */
    public static LambdaKey createLambdaKeyFromCompilationUnit(String compilationUnitStr) {
        CompilationUnit compilationUnit = StaticJavaParser.parse(compilationUnitStr);
        MethodDeclaration methodDecl = compilationUnit.findFirst(MethodDeclaration.class).orElseThrow();
        return createLambdaKeyFromMethodDeclaration(methodDecl);
    }

    /**
     * Helper method to create a LambdaKey from a method declaration AST node
     */
    public static LambdaKey createLambdaKeyFromMethodDeclaration(MethodDeclaration methodDeclaration) {
        MethodDeclaration normalized = VariableNameNormalizerVisitor.normalize(methodDeclaration);
        String normalizedStr = normalized.toString();
        return new LambdaKey(normalizedStr);
    }

    /**
     * Helper method to create a LambdaKey from a method declaration string
     */
    public static LambdaKey createLambdaKeyFromMethodDeclarationString(String methodDeclarationString) {
        MethodDeclaration methodDeclaration = StaticJavaParser.parseMethodDeclaration(methodDeclarationString);
        return createLambdaKeyFromMethodDeclaration(methodDeclaration);
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
