package org.mvel3.lambdaextractor.extended;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mvel3.lambdaextractor.extended.LambdaUtilsEx.createLambdaKeyFromMethodDeclarationString;

public class LambdaRegistryExTest {

    @Before
    public void setup() {
        // Reset before each test, because the tests assert IDs from 0 upwards
        LambdaRegistryEx.INSTANCE.resetAndRemoveAllPersistedFiles();
    }

    @Test
    public void testRegisterLambda_SameLambdaDifferentVariableNames_ShouldSharePhysicalId() {
        LambdaRegistryEx registry = LambdaRegistryEx.INSTANCE;

        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Person employee) { return employee.getAge() > 20; }";

        LambdaKeyEx key1 = createLambdaKeyFromMethodDeclarationString(method1);
        LambdaKeyEx key2 = createLambdaKeyFromMethodDeclarationString(method2);

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
    public void testRegisterLambda_DifferentTypes_ShouldHaveDifferentPhysicalIds() {
        LambdaRegistryEx registry = LambdaRegistryEx.INSTANCE;

        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Employee employee) { return employee.getAge() > 20; }";

        LambdaKeyEx key1 = createLambdaKeyFromMethodDeclarationString(method1);
        LambdaKeyEx key2 = createLambdaKeyFromMethodDeclarationString(method2);

        // Keys should NOT be equal (different parameter types)
        assertThat(key1).isNotEqualTo(key2);

        int physicalId1 = registry.registerLambda(1, key1);
        int physicalId2 = registry.registerLambda(2, key2);

        assertThat(physicalId1).isNotEqualTo(physicalId2);
    }

    @Test
    public void testRegisterLambda_HashCollision_ShouldStillCreateDistinctPhysicalIds() {
        LambdaRegistryEx registry = LambdaRegistryEx.INSTANCE;

        String method1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String method2 = "public boolean eval(org.example.Person person) { return person.getAge() < 20; }";

        LambdaKeyEx key1 = createLambdaKeyFromMethodDeclarationString(method1);
        LambdaKeyEx key2 = createLambdaKeyFromMethodDeclarationString(method2);

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
    public void testRegisterLambda_MultipleLogicalToOnePhysical() {
        LambdaRegistryEx registry = LambdaRegistryEx.INSTANCE;

        String method = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String methodVariant1 = "public boolean eval(org.example.Person employee) { return employee.getAge() > 20; }";
        String methodVariant2 = "public boolean eval(org.example.Person p) { return p.getAge() > 20; }";

        LambdaKeyEx key = createLambdaKeyFromMethodDeclarationString(method);
        LambdaKeyEx key1 = createLambdaKeyFromMethodDeclarationString(methodVariant1);
        LambdaKeyEx key2 = createLambdaKeyFromMethodDeclarationString(methodVariant2);

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
}
