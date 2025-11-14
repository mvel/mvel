package org.mvel3.lambdaextractor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.Test;
import org.mvel3.methodutils.Murmur3F;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LambdaRegistryTest {

    /**
     * Helper method to create a LambdaKey from a method declaration string
     */
    private LambdaKey createLambdaKey(String methodDeclaration) {
        MethodDeclaration methodDecl = StaticJavaParser.parseMethodDeclaration(methodDeclaration);
        MethodDeclaration normalized = VariableNameNormalizerVisitor.normalize(methodDecl);
        String normalizedStr = normalized.toString();
        String signature = extractSignature(normalized);
        return new LambdaKey(normalizedStr, signature);
    }

    /**
     * Extract method signature from normalized method declaration
     */
    private String extractSignature(MethodDeclaration method) {
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
    private int calculateHash(String normalizedMethod) {
        Murmur3F hasher = new Murmur3F();
        hasher.update(normalizedMethod.getBytes(StandardCharsets.UTF_8));
        return (int) hasher.getValue();
    }

    @Test
    public void testRegisterLambda_SameLambdaDifferentVariableNames_ShouldSharePhysicalId() {
        // Case: person.getAge() > 20 and employee.getAge() > 20
        // After normalization, both become v1.getAge() > 20
        // They should get the SAME physical ID

        LambdaRegistry registry = new LambdaRegistry();

        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Person employee) { return employee.getAge() > 20; }";

        LambdaKey key1 = createLambdaKey(method1);
        LambdaKey key2 = createLambdaKey(method2);

        System.out.println("Key1 body: " + key1.normalisedBody);
        System.out.println("Key2 body: " + key2.normalisedBody);
        System.out.println("Key1 signature: " + key1.signature);
        System.out.println("Key2 signature: " + key2.signature);

        // Keys should be equal
        assertThat(key1).isEqualTo(key2);

        int hash1 = calculateHash(key1.normalisedBody);
        int hash2 = calculateHash(key2.normalisedBody);

        // Register both lambdas with different logical IDs
        int physicalId1 = registry.registerLambda(1, key1, hash1);
        int physicalId2 = registry.registerLambda(2, key2, hash2);

        // Should get the SAME physical ID (deduplication)
        assertThat(physicalId1).isEqualTo(physicalId2);

        // Both logical IDs should map to the same physical ID
        assertThat(registry.getPhysicalId(1)).isEqualTo(physicalId1);
        assertThat(registry.getPhysicalId(2)).isEqualTo(physicalId1);

        System.out.println("Physical ID 1: " + physicalId1);
        System.out.println("Physical ID 2: " + physicalId2);
        System.out.println("Both lambdas share the same physical ID: " + physicalId1);
    }

    @Test
    public void testRegisterLambda_DifferentTypes_ShouldHaveDifferentPhysicalIds() {
        // Case: Person vs Employee (different types)
        // Even if the body looks the same, the signature is different
        // They should get DIFFERENT physical IDs

        LambdaRegistry registry = new LambdaRegistry();

        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Employee employee) { return employee.getAge() > 20; }";

        LambdaKey key1 = createLambdaKey(method1);
        LambdaKey key2 = createLambdaKey(method2);

        System.out.println("Key1 signature: " + key1.signature);
        System.out.println("Key2 signature: " + key2.signature);

        // Keys should NOT be equal (different signatures)
        assertThat(key1).isNotEqualTo(key2);

        int hash1 = calculateHash(key1.normalisedBody);
        int hash2 = calculateHash(key2.normalisedBody);

        // Register both lambdas
        int physicalId1 = registry.registerLambda(1, key1, hash1);
        int physicalId2 = registry.registerLambda(2, key2, hash2);

        // Should get DIFFERENT physical IDs
        assertThat(physicalId1).isNotEqualTo(physicalId2);

        System.out.println("Physical ID 1 (Person): " + physicalId1);
        System.out.println("Physical ID 2 (Employee): " + physicalId2);
        System.out.println("Different types have different physical IDs");
    }

    @Test
    public void testRegisterLambda_HashCollision_ShouldTrackBothLogicalIds() {
        // Case: Simulate hash collision
        // Two different LambdaKeys that happen to have the same hash
        // They should:
        // 1. Both be tracked in hashToLogicalIds (under the same hash)
        // 2. Get DIFFERENT physical IDs (because keys are different)

        LambdaRegistry registry = new LambdaRegistry();

        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Person person) { return person.getAge() < 20; }";

        LambdaKey key1 = createLambdaKey(method1);
        LambdaKey key2 = createLambdaKey(method2);

        // Keys are different (different operators)
        assertThat(key1).isNotEqualTo(key2);

        // Force the same hash (simulate collision)
        int forcedHash = 12345;

        // Register both with the same hash but different keys
        int physicalId1 = registry.registerLambda(1, key1, forcedHash);
        int physicalId2 = registry.registerLambda(2, key2, forcedHash);

        // Should get DIFFERENT physical IDs (keys are different)
        assertThat(physicalId1).isNotEqualTo(physicalId2);

        // But both logical IDs should be tracked under the same hash
        List<Integer> logicalIdsWithSameHash = registry.getLogicalIdsWithSameHash(forcedHash);
        assertThat(logicalIdsWithSameHash).containsExactly(1, 2);

        System.out.println("Forced hash: " + forcedHash);
        System.out.println("Physical ID 1: " + physicalId1);
        System.out.println("Physical ID 2: " + physicalId2);
        System.out.println("Logical IDs with same hash: " + logicalIdsWithSameHash);
        System.out.println("Hash collision handled correctly: same hash, different physical IDs");
    }

    @Test
    public void testRegisterLambda_MultipleLogicalToOnePhysical() {
        // scenario: logical IDs 5, 9, 10 all point to physical ID 4
        // This happens when we have 3 rules with identical constraints

        LambdaRegistry registry = new LambdaRegistry();

        // Now register the SAME lambda 3 times with logical IDs 5, 9, 10
        String method = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String methodVariant1 = "public boolean eval(org.example.Person employee) { return employee.getAge() > 20; }";
        String methodVariant2 = "public boolean eval(org.example.Person p) { return p.getAge() > 20; }";

        LambdaKey key = createLambdaKey(method);
        LambdaKey key1 = createLambdaKey(methodVariant1);
        LambdaKey key2 = createLambdaKey(methodVariant2);

        // All keys should be equal (same normalized body and signature)
        assertThat(key).isEqualTo(key1);
        assertThat(key).isEqualTo(key2);

        int hash = calculateHash(key.normalisedBody);

        // Register logical IDs 5, 9, 10 with the same lambda
        int physicalId5 = registry.registerLambda(5, key, hash);
        int physicalId9 = registry.registerLambda(9, key1, hash);
        int physicalId10 = registry.registerLambda(10, key2, hash);

        // All should map to physical ID 0
        assertThat(physicalId5).isEqualTo(0);
        assertThat(physicalId9).isEqualTo(0);
        assertThat(physicalId10).isEqualTo(0);

        // Verify the mappings
        assertThat(registry.getPhysicalId(5)).isEqualTo(0);
        assertThat(registry.getPhysicalId(9)).isEqualTo(0);
        assertThat(registry.getPhysicalId(10)).isEqualTo(0);

        System.out.println("  Logical ID 5 -> Physical ID " + registry.getPhysicalId(5));
        System.out.println("  Logical ID 9 -> Physical ID " + registry.getPhysicalId(9));
        System.out.println("  Logical ID 10 -> Physical ID " + registry.getPhysicalId(10));
        System.out.println("All three logical IDs share physical ID 0");
    }
}
