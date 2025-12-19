package org.mvel3.lambdaextractor.extended;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;
import org.mvel3.lambdaextractor.VariableNameNormalizerVisitor;
import org.mvel3.methodutils.Murmur3F;

public class LambdaUtilsEx {

    private static final Map<String, Class<?>> PRIMITIVES = Map.of(
            "boolean", boolean.class,
            "byte", byte.class,
            "short", short.class,
            "int", int.class,
            "long", long.class,
            "float", float.class,
            "double", double.class,
            "char", char.class,
            "void", void.class
    );

    private LambdaUtilsEx() {
        // Prevent instantiation
    }

    /**
     * Helper method to create a LambdaKey from a full compilation unit string
     * Note: LambdaKey contains only the normalized method declaration. So the class name does not affect the equality.
     */
    public static LambdaKeyEx createLambdaKeyFromCompilationUnit(String compilationUnitStr) {
        CompilationUnit compilationUnit = StaticJavaParser.parse(compilationUnitStr);
        MethodDeclaration methodDecl = compilationUnit.findFirst(MethodDeclaration.class).orElseThrow();
        return createLambdaKeyFromMethodDeclaration(methodDecl);
    }

    /**
     * Helper method to create a LambdaKey from a method declaration AST node
     */
    public static LambdaKeyEx createLambdaKeyFromMethodDeclaration(MethodDeclaration methodDeclaration) {
        MethodDeclaration normalizedMethodDeclaration = VariableNameNormalizerVisitor.normalize(methodDeclaration);
        String methodSignature = normalizedMethodDeclaration.getDeclarationAsString(true, true, true);
        BlockStmt normalizedBody = normalizedMethodDeclaration.getBody().orElseThrow(() -> new IllegalStateException("MethodDeclaration has no body"));
        String normalizedStr = normalizedBody.toString();
        Type returnType = methodDeclaration.getType();
        Class<?> returnClass = resolveType(returnType);
        List<Class<?>> parameterTypes = methodDeclaration.getParameters().stream().<Class<?>>map(p -> resolveType(p.getType())).toList();
        LambdaKeyEx.MethodSignatureInfo methodSignatureInfo =
                new LambdaKeyEx.MethodSignatureInfo(
                        returnClass,
                        methodDeclaration.getNameAsString(),
                        parameterTypes
                );
        return new LambdaKeyEx(methodSignature, normalizedStr, methodSignatureInfo);
    }

    private static Class<?> resolveType(Type type) {
        // this resolution relies on the mvel implementation that FQCN is retained in source code
        // to be generic resolution, SymbolResolver will be required, but it would be slow
        String fqcn = type.asString();
        if (PRIMITIVES.containsKey(fqcn)) {
            return PRIMITIVES.get(fqcn);
        }
        try {
            return Class.forName(fqcn);
        } catch (ClassNotFoundException e) {
            try {
                // replace the last `.` with `$` to handle inner class
                int lastDot = fqcn.lastIndexOf('.');
                String possibleInnerClassName = fqcn.substring(0, lastDot) + '$' + fqcn.substring(lastDot + 1);
                return Class.forName(possibleInnerClassName);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex); // we may change this to a warning
            }
        }
    }

    /**
     * Helper method to create a LambdaKey from a method declaration string
     */
    public static LambdaKeyEx createLambdaKeyFromMethodDeclarationString(String methodDeclarationString) {
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
