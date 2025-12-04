package org.mvel3.lambdaextractor;

import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mvel3.lambdaextractor.LambdaUtils.calculateHash;
import static org.mvel3.lambdaextractor.LambdaUtils.createLambdaKeyFromMethodDeclaration;

public class LambdaRegistryTest {

    @Test
    public void testRegisterLambda_SameLambdaDifferentVariableNames_ShouldSharePhysicalId() {
        // Case: person.getAge() > 20 and employee.getAge() > 20
        // After normalization, both become v1.getAge() > 20
        // They should get the SAME physical ID

        LambdaRegistry registry = LambdaRegistry.INSTANCE;

        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Person employee) { return employee.getAge() > 20; }";

        LambdaKey key1 = createLambdaKeyFromMethodDeclaration(method1);
        LambdaKey key2 = createLambdaKeyFromMethodDeclaration(method2);

        System.out.println("Key1 body: " + key1.getNormalisedBody());
        System.out.println("Key2 body: " + key2.getNormalisedBody());
        System.out.println("Key1 signature: " + key1.getSignature());
        System.out.println("Key2 signature: " + key2.getSignature());

        // Keys should be equal
        assertThat(key1).isEqualTo(key2);

        int hash1 = calculateHash(key1.getNormalisedBody());
        int hash2 = calculateHash(key2.getNormalisedBody());

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

        LambdaRegistry registry = LambdaRegistry.INSTANCE;

        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Employee employee) { return employee.getAge() > 20; }";

        LambdaKey key1 = createLambdaKeyFromMethodDeclaration(method1);
        LambdaKey key2 = createLambdaKeyFromMethodDeclaration(method2);

        System.out.println("Key1 signature: " + key1.getSignature());
        System.out.println("Key2 signature: " + key2.getSignature());

        // Keys should NOT be equal (different signatures)
        assertThat(key1).isNotEqualTo(key2);

        int hash1 = calculateHash(key1.getNormalisedBody());
        int hash2 = calculateHash(key2.getNormalisedBody());

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

        LambdaRegistry registry = LambdaRegistry.INSTANCE;

        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Person person) { return person.getAge() < 20; }";

        LambdaKey key1 = createLambdaKeyFromMethodDeclaration(method1);
        LambdaKey key2 = createLambdaKeyFromMethodDeclaration(method2);

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

        LambdaRegistry registry = LambdaRegistry.INSTANCE;

        // Now register the SAME lambda 3 times with logical IDs 5, 9, 10
        String method = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String methodVariant1 = "public boolean eval(org.example.Person employee) { return employee.getAge() > 20; }";
        String methodVariant2 = "public boolean eval(org.example.Person p) { return p.getAge() > 20; }";

        LambdaKey key = createLambdaKeyFromMethodDeclaration(method);
        LambdaKey key1 = createLambdaKeyFromMethodDeclaration(methodVariant1);
        LambdaKey key2 = createLambdaKeyFromMethodDeclaration(methodVariant2);

        // All keys should be equal (same normalized body and signature)
        assertThat(key).isEqualTo(key1);
        assertThat(key).isEqualTo(key2);

        int hash = calculateHash(key.getNormalisedBody());

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
