package org.mvel3.lambdaextractor;

import java.nio.charset.StandardCharsets;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import org.junit.Test;
import org.mvel3.methodutils.Murmur3F;

import static org.assertj.core.api.Assertions.assertThat;

public class VariableNameNormalizerVisitorTest {

    /**
     * Helper method to calculate hash of a string
     */
    private String calculateHash(String code) {
        Murmur3F hasher = new Murmur3F();
        hasher.update(code.getBytes(StandardCharsets.UTF_8));
        return hasher.getValueHexString();
    }

    private String normalizeMethod(String methodSource) {
        MethodDeclaration methodDeclaration = StaticJavaParser.parseMethodDeclaration(methodSource);
        return VariableNameNormalizerVisitor.normalize(methodDeclaration).toString();
    }

    private void assertNormalizedEquality(String method1, String method2) {
        String normalized1 = normalizeMethod(method1);
        String normalized2 = normalizeMethod(method2);
        assertThat(normalized1).isEqualTo(normalized2);
        assertThat(calculateHash(normalized1)).isEqualTo(calculateHash(normalized2));
    }

    @Test
    public void testNormalizeSimpleExpressionWithSameType() {
        // Two methods with different parameter names but same type and structure
        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Person employee) { return employee.getAge() > 20; }";

        // Parse method declarations
        MethodDeclaration methodDecl1 = StaticJavaParser.parseMethodDeclaration(method1);
        MethodDeclaration methodDecl2 = StaticJavaParser.parseMethodDeclaration(method2);

        // Normalize variable names
        MethodDeclaration normalized1 = VariableNameNormalizerVisitor.normalize(methodDecl1);
        MethodDeclaration normalized2 = VariableNameNormalizerVisitor.normalize(methodDecl2);

        // Convert to string
        String normalizedStr1 = normalized1.toString();
        String normalizedStr2 = normalized2.toString();

        System.out.println("Original 1: " + method1);
        System.out.println("Normalized 1: " + normalizedStr1);
        System.out.println("Original 2: " + method2);
        System.out.println("Normalized 2: " + normalizedStr2);

        // Calculate hashes
        String hash1 = calculateHash(normalizedStr1);
        String hash2 = calculateHash(normalizedStr2);

        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);

        // Assert that normalized strings are the same
        assertThat(normalizedStr1).isEqualTo(normalizedStr2);

        // Assert that hashes are the same
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    public void testInnerBlockRedeclarationProducesDeterministicNames() {
        String method1 = "public void eval(org.example.Person person) { int age = person.getAge(); if (age > 20) { int tmp = age; person.setAge(tmp); } }";
        String method2 = "public void eval(org.example.Person someone) { int years = someone.getAge(); if (years > 20) { int copy = years; someone.setAge(copy); } }";

        assertNormalizedEquality(method1, method2);
    }

    @Test
    public void testSequentialIdsAcrossNestedScopes() {
        String method1 = "public void eval(org.example.Person person) { int age = person.getAge(); { int age = 10; person.setAge(age); } int olderAge = age + 1; }";
        String method2 = "public void eval(org.example.Person somebody) { int years = somebody.getAge(); { int years = 10; somebody.setAge(years); } int newerAge = years + 1; }";

        String normalized = normalizeMethod(method1);
        assertThat(normalized).contains("int v3 = 10;");
        assertThat(normalized).contains("int v4 = v2 + 1;");
        assertNormalizedEquality(method1, method2);
    }

    @Test
    public void testForLoopVariableScope() {
        String method1 = "public int sum(java.util.List<Integer> values) { int total = 0; for (int i = 0; i < values.size(); i++) { total += values.get(i); } return total; }";
        String method2 = "public int sum(java.util.List<Integer> numbers) { int accumulator = 0; for (int index = 0; index < numbers.size(); index++) { accumulator += numbers.get(index); } return accumulator; }";

        assertNormalizedEquality(method1, method2);
    }

    @Test
    public void testForEachLoopVariableScope() {
        String method1 = "public int sum(java.util.List<Integer> values) { int total = 0; for (Integer value : values) { total += value; } return total; }";
        String method2 = "public int sum(java.util.List<Integer> numbers) { int accumulator = 0; for (Integer element : numbers) { accumulator += element; } return accumulator; }";

        assertNormalizedEquality(method1, method2);
    }

    @Test
    public void testTryCatchResourcesAreScoped() {
        String method1 = "public void run() { try (java.io.Reader reader = open()) { reader.read(); } catch (java.io.IOException ex) { log(ex.getMessage()); } }";
        String method2 = "public void run() { try (java.io.Reader input = open()) { input.read(); } catch (java.io.IOException error) { log(error.getMessage()); } }";

        String normalized = normalizeMethod(method1);
        assertThat(normalized).contains("try (java.io.Reader v1 = open())");
        assertThat(normalized).contains("catch (java.io.IOException v2)");
        assertNormalizedEquality(method1, method2);
    }

    @Test
    public void testLambdaParametersAreNormalized() {
        String method1 = "public java.util.function.Function<org.example.Person, Integer> make() { return person -> person.getAge(); }";
        String method2 = "public java.util.function.Function<org.example.Person, Integer> make() { return candidate -> candidate.getAge(); }";

        String normalized = normalizeMethod(method1);
        assertThat(normalized).contains("return v1 -> v1.getAge();");
        assertNormalizedEquality(method1, method2);
    }

    @Test
    public void testNormalizeExpressionWithDifferentTypes() {
        // Two methods with different parameter types - should produce DIFFERENT hashes
        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Employee employee) { return employee.getAge() > 20; }";

        // Parse method declarations
        MethodDeclaration methodDecl1 = StaticJavaParser.parseMethodDeclaration(method1);
        MethodDeclaration methodDecl2 = StaticJavaParser.parseMethodDeclaration(method2);

        // Normalize variable names
        MethodDeclaration normalized1 = VariableNameNormalizerVisitor.normalize(methodDecl1);
        MethodDeclaration normalized2 = VariableNameNormalizerVisitor.normalize(methodDecl2);

        // Convert to string
        String normalizedStr1 = normalized1.toString();
        String normalizedStr2 = normalized2.toString();

        System.out.println("Original 1: " + method1);
        System.out.println("Normalized 1: " + normalizedStr1);
        System.out.println("Original 2: " + method2);
        System.out.println("Normalized 2: " + normalizedStr2);

        // Calculate hashes
        String hash1 = calculateHash(normalizedStr1);
        String hash2 = calculateHash(normalizedStr2);

        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);

        // Assert that normalized strings are DIFFERENT (different types)
        assertThat(normalizedStr1).isNotEqualTo(normalizedStr2);

        // Assert that hashes are DIFFERENT
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    public void testNormalizeVoidMethodWithSameType() {
        // Two void methods with different parameter names but same type and structure
        String method1 = "public void eval(org.example.Person person) { person.setAge(21); update(person); }";
        String method2 = "public void eval(org.example.Person employee) { employee.setAge(21); update(employee); }";

        // Parse method declarations
        MethodDeclaration methodDecl1 = StaticJavaParser.parseMethodDeclaration(method1);
        MethodDeclaration methodDecl2 = StaticJavaParser.parseMethodDeclaration(method2);

        // Normalize variable names
        MethodDeclaration normalized1 = VariableNameNormalizerVisitor.normalize(methodDecl1);
        MethodDeclaration normalized2 = VariableNameNormalizerVisitor.normalize(methodDecl2);

        // Convert to string
        String normalizedStr1 = normalized1.toString();
        String normalizedStr2 = normalized2.toString();

        System.out.println("Original 1: " + method1);
        System.out.println("Normalized 1: " + normalizedStr1);
        System.out.println("Original 2: " + method2);
        System.out.println("Normalized 2: " + normalizedStr2);

        // Calculate hashes
        String hash1 = calculateHash(normalizedStr1);
        String hash2 = calculateHash(normalizedStr2);

        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);

        // Assert that normalized strings are the same
        assertThat(normalizedStr1).isEqualTo(normalizedStr2);

        // Assert that hashes are the same
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    public void testNormalizeMethodWithLocalVariables() {
        // Two methods with local variable declarations using different names but same structure
        String method1 = "public void eval(org.example.Person person) { int age = person.getAge(); age = age + 1; person.setAge(age); }";
        String method2 = "public void eval(org.example.Person employee) { int years = employee.getAge(); years = years + 1; employee.setAge(years); }";

        // Parse method declarations
        MethodDeclaration methodDecl1 = StaticJavaParser.parseMethodDeclaration(method1);
        MethodDeclaration methodDecl2 = StaticJavaParser.parseMethodDeclaration(method2);

        // Normalize variable names
        MethodDeclaration normalized1 = VariableNameNormalizerVisitor.normalize(methodDecl1);
        MethodDeclaration normalized2 = VariableNameNormalizerVisitor.normalize(methodDecl2);

        // Convert to string
        String normalizedStr1 = normalized1.toString();
        String normalizedStr2 = normalized2.toString();

        System.out.println("Original 1: " + method1);
        System.out.println("Normalized 1: " + normalizedStr1);
        System.out.println("Original 2: " + method2);
        System.out.println("Normalized 2: " + normalizedStr2);

        // Calculate hashes
        String hash1 = calculateHash(normalizedStr1);
        String hash2 = calculateHash(normalizedStr2);

        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);

        // Assert that normalized strings are the same
        assertThat(normalizedStr1).isEqualTo(normalizedStr2);

        // Assert that hashes are the same
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    public void testDifferentStructureProducesDifferentHash() {
        // Two methods with different operations (> vs <)
        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Person person) { return person.getAge() < 20; }";

        // Parse method declarations
        MethodDeclaration methodDecl1 = StaticJavaParser.parseMethodDeclaration(method1);
        MethodDeclaration methodDecl2 = StaticJavaParser.parseMethodDeclaration(method2);

        // Normalize variable names
        MethodDeclaration normalized1 = VariableNameNormalizerVisitor.normalize(methodDecl1);
        MethodDeclaration normalized2 = VariableNameNormalizerVisitor.normalize(methodDecl2);

        // Convert to string
        String normalizedStr1 = normalized1.toString();
        String normalizedStr2 = normalized2.toString();

        // Calculate hashes
        String hash1 = calculateHash(normalizedStr1);
        String hash2 = calculateHash(normalizedStr2);

        System.out.println("Method 1: " + normalizedStr1 + " -> Hash: " + hash1);
        System.out.println("Method 2: " + normalizedStr2 + " -> Hash: " + hash2);

        // Assert that normalized strings are different
        assertThat(normalizedStr1).isNotEqualTo(normalizedStr2);

        // Assert that hashes are different
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    public void testNormalizeMethodWithMultipleParameters() {
        // Method with multiple parameters - should normalize in order
        String method1 = "public int eval(org.example.Person person, org.example.Account account) { return person.getAge() + account.getBalance(); }";
        String method2 = "public int eval(org.example.Person employee, org.example.Account savings) { return employee.getAge() + savings.getBalance(); }";

        // Parse method declarations
        MethodDeclaration methodDecl1 = StaticJavaParser.parseMethodDeclaration(method1);
        MethodDeclaration methodDecl2 = StaticJavaParser.parseMethodDeclaration(method2);

        // Normalize variable names
        MethodDeclaration normalized1 = VariableNameNormalizerVisitor.normalize(methodDecl1);
        MethodDeclaration normalized2 = VariableNameNormalizerVisitor.normalize(methodDecl2);

        // Convert to string
        String normalizedStr1 = normalized1.toString();
        String normalizedStr2 = normalized2.toString();

        System.out.println("Original 1: " + method1);
        System.out.println("Normalized 1: " + normalizedStr1);
        System.out.println("Original 2: " + method2);
        System.out.println("Normalized 2: " + normalizedStr2);

        // Calculate hashes
        String hash1 = calculateHash(normalizedStr1);
        String hash2 = calculateHash(normalizedStr2);

        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);

        // Parameters should be normalized to v1 and v2 in order
        assertThat(normalizedStr1).contains("org.example.Person v1");
        assertThat(normalizedStr1).contains("org.example.Account v2");

        // Both should normalize to same structure
        assertThat(normalizedStr1).isEqualTo(normalizedStr2);
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    public void testNormalizeSimpleExpressionWithoutType_hashShouldBeDifferent() {
        // Two expressions with different variable names but same structure
        // Type is not specified in variable names
        String expression1 = "person.getAge() > 20";
        String expression2 = "employee.getAge() > 20";

        // Parse method declarations
        Expression expr1 = StaticJavaParser.parseExpression(expression1);
        Expression expr2 = StaticJavaParser.parseExpression(expression2);

        // Normalize variable names
        Expression normalized1 = VariableNameNormalizerVisitor.normalize(expr1);
        Expression normalized2 = VariableNameNormalizerVisitor.normalize(expr2);

        // Convert to string
        String normalizedStr1 = normalized1.toString();
        String normalizedStr2 = normalized2.toString();

        System.out.println("Original 1: " + expression1);
        System.out.println("Normalized 1: " + normalizedStr1);
        System.out.println("Original 2: " + expression2);
        System.out.println("Normalized 2: " + normalizedStr2);

        // Calculate hashes
        String hash1 = calculateHash(normalizedStr1);
        String hash2 = calculateHash(normalizedStr2);

        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);

        // Assert that normalized strings are not the same
        assertThat(normalizedStr1).isNotEqualTo(normalizedStr2);

        // Assert that hashes are not the same
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
