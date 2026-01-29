package org.mvel3.lambdaextractor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mvel3.lambdaextractor.LambdaUtils.createLambdaKeyFromMethodDeclarationString;

class LambdaRegistryTest {

    @BeforeEach
    void setup() {
        // Reset before each test, because the tests assert IDs from 0 upwards
        LambdaRegistry.INSTANCE.resetAndRemoveAllPersistedFiles();
    }

    @Test
    void testRegisterLambda_SameLambdaDifferentVariableNames_ShouldSharePhysicalId() {
        LambdaRegistry registry = LambdaRegistry.INSTANCE;

        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Person employee) { return employee.getAge() > 20; }";

        LambdaKey key1 = createLambdaKeyFromMethodDeclarationString(method1);
        LambdaKey key2 = createLambdaKeyFromMethodDeclarationString(method2);

        // Keys should be equal after normalization (signature + body)
        assertThat(key1).isEqualTo(key2);

        int physicalId1 = registry.registerLambda(1, key1);
        int physicalId2 = registry.registerLambda(2, key2);

        // Should get the SAME physical ID (deduplication)
        assertThat(physicalId1).isEqualTo(physicalId2);
        assertThat(registry.getPhysicalId(1)).isEqualTo(physicalId1);
        assertThat(registry.getPhysicalId(2)).isEqualTo(physicalId1);
    }

    @Test
    void testRegisterLambda_DifferentTypes_ShouldHaveDifferentPhysicalIds() {
        LambdaRegistry registry = LambdaRegistry.INSTANCE;

        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Employee employee) { return employee.getAge() > 20; }";

        LambdaKey key1 = createLambdaKeyFromMethodDeclarationString(method1);
        LambdaKey key2 = createLambdaKeyFromMethodDeclarationString(method2);

        // Keys should NOT be equal (different parameter types)
        assertThat(key1).isNotEqualTo(key2);

        int physicalId1 = registry.registerLambda(1, key1);
        int physicalId2 = registry.registerLambda(2, key2);

        assertThat(physicalId1).isNotEqualTo(physicalId2);
    }

    @Test
    void testRegisterLambda_HashCollision_ShouldStillCreateDistinctPhysicalIds() {
        LambdaRegistry registry = LambdaRegistry.INSTANCE;

        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Person person) { return person.getAge() < 20; }";

        LambdaKey key1 = createLambdaKeyFromMethodDeclarationString(method1);
        LambdaKey key2 = createLambdaKeyFromMethodDeclarationString(method2);

        // Keys are different (different operators in the body)
        assertThat(key1).isNotEqualTo(key2);

        // Force the same hash (simulate collision)
        int forcedHash = 12345;
        key1.forceHash(forcedHash);
        key2.forceHash(forcedHash);

        int physicalId1 = registry.registerLambda(1, key1);
        int physicalId2 = registry.registerLambda(2, key2);

        // Should get DIFFERENT physical IDs (keys are different even if hash collides)
        assertThat(physicalId1).isNotEqualTo(physicalId2);
        assertThat(registry.getPhysicalId(1)).isEqualTo(physicalId1);
        assertThat(registry.getPhysicalId(2)).isEqualTo(physicalId2);
    }

    @Test
    void testRegisterLambda_MultipleLogicalToOnePhysical() {
        LambdaRegistry registry = LambdaRegistry.INSTANCE;

        String method = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String methodVariant1 = "public boolean eval(org.example.Person employee) { return employee.getAge() > 20; }";
        String methodVariant2 = "public boolean eval(org.example.Person p) { return p.getAge() > 20; }";

        LambdaKey key = createLambdaKeyFromMethodDeclarationString(method);
        LambdaKey key1 = createLambdaKeyFromMethodDeclarationString(methodVariant1);
        LambdaKey key2 = createLambdaKeyFromMethodDeclarationString(methodVariant2);

        assertThat(key).isEqualTo(key1);
        assertThat(key).isEqualTo(key2);

        int physicalId5 = registry.registerLambda(5, key);
        int physicalId9 = registry.registerLambda(9, key1);
        int physicalId10 = registry.registerLambda(10, key2);

        assertThat(physicalId5).isEqualTo(0);
        assertThat(physicalId9).isEqualTo(0);
        assertThat(physicalId10).isEqualTo(0);

        assertThat(registry.getPhysicalId(5)).isEqualTo(0);
        assertThat(registry.getPhysicalId(9)).isEqualTo(0);
        assertThat(registry.getPhysicalId(10)).isEqualTo(0);
    }

    @Test
    void testRegisterLambda_SubtypeParameter_ShouldReusePhysicalId() {
        // This tests the subtype overload detection feature.
        // When a lambda has the same body and a parameter type that is a subtype of
        // an already registered lambda's parameter type, it should reuse the physical ID.

        LambdaRegistry registry = LambdaRegistry.INSTANCE;

        // First lambda with Object parameter (must use FQCN for type resolution)
        String methodWithObject = "public boolean eval(java.lang.Object obj) { return obj != null; }";
        // Second lambda with String parameter (String is a subtype of Object)
        String methodWithString = "public boolean eval(java.lang.String str) { return str != null; }";

        LambdaKey keyObject = createLambdaKeyFromMethodDeclarationString(methodWithObject);
        LambdaKey keyString = createLambdaKeyFromMethodDeclarationString(methodWithString);

        // Keys are NOT equal because signatures differ (Object vs String)
        assertThat(keyObject).isNotEqualTo(keyString);

        // But normalized bodies should be the same
        assertThat(keyObject.getNormalisedBody()).isEqualTo(keyString.getNormalisedBody());

        // Register the Object version first
        int physicalIdObject = registry.registerLambda(1, keyObject);

        // Register the String version - should reuse the Object version's physical ID
        // because String is a subtype of Object and the body is identical
        int physicalIdString = registry.registerLambda(2, keyString);

        // Both should share the SAME physical ID due to subtype overload detection
        assertThat(physicalIdString).isEqualTo(physicalIdObject);
        assertThat(registry.getPhysicalId(1)).isEqualTo(physicalIdObject);
        assertThat(registry.getPhysicalId(2)).isEqualTo(physicalIdObject);
    }
}
