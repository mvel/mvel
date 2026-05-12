package org.mvel3.lambdaextractor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mvel3.lambdaextractor.LambdaUtils.createLambdaKeyFromMethodDeclarationString;

class LambdaCatalogTest {

    private LambdaCatalog catalog;

    @BeforeEach
    void setup() {
        catalog = new LambdaCatalog();
    }

    @Test
    void M1_dedup_exactKey_reusesPhysicalId() {
        String method = "public boolean eval(org.example.Person p) { return p.getAge() > 20; }";
        LambdaKey k1 = createLambdaKeyFromMethodDeclarationString(method);
        LambdaKey k2 = createLambdaKeyFromMethodDeclarationString(method);

        RegistrationResult r1 = catalog.register(k1);
        RegistrationResult r2 = catalog.register(k2);

        assertThat(r1.reused()).isFalse();
        assertThat(r2.reused()).isTrue();
        assertThat(r2.physicalId()).isEqualTo(r1.physicalId());
        assertThat(r2.logicalId()).isNotEqualTo(r1.logicalId());
    }

    @Test
    void M2_dedup_subtypeOverload_supertypeFirst_subtypeReuses() {
        String supertypeMethod = "public boolean eval(java.lang.Object o) { return o != null; }";
        String subtypeMethod = "public boolean eval(java.lang.String o) { return o != null; }";

        LambdaKey supertype = createLambdaKeyFromMethodDeclarationString(supertypeMethod);
        LambdaKey subtype = createLambdaKeyFromMethodDeclarationString(subtypeMethod);

        RegistrationResult r1 = catalog.register(supertype);
        RegistrationResult r2 = catalog.register(subtype);

        assertThat(r1.reused()).isFalse();
        assertThat(r2.reused()).isTrue();
        assertThat(r2.physicalId()).isEqualTo(r1.physicalId());
    }

    @Test
    void M3_dedup_subtypeOverload_subtypeFirst_supertypeDoesNotReuse() {
        String subtypeMethod = "public boolean eval(java.lang.String o) { return o != null; }";
        String supertypeMethod = "public boolean eval(java.lang.Object o) { return o != null; }";

        LambdaKey subtype = createLambdaKeyFromMethodDeclarationString(subtypeMethod);
        LambdaKey supertype = createLambdaKeyFromMethodDeclarationString(supertypeMethod);

        RegistrationResult r1 = catalog.register(subtype);
        RegistrationResult r2 = catalog.register(supertype);

        assertThat(r1.reused()).isFalse();
        assertThat(r2.reused()).isFalse();
        assertThat(r2.physicalId()).isNotEqualTo(r1.physicalId());
    }

    @Test
    void differentParamTypes_unrelated_keepSeparatePhysicalIds() {
        // Ported from LambdaRegistryTest#testRegisterLambda_DifferentTypes_ShouldHaveDifferentPhysicalIds.
        // Person and Employee are unrelated classes (neither is a supertype of the other),
        // so the subtype-overload reuse should not trigger.
        String m1 = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String m2 = "public boolean eval(org.example.Employee employee) { return employee.getAge() > 20; }";

        LambdaKey k1 = createLambdaKeyFromMethodDeclarationString(m1);
        LambdaKey k2 = createLambdaKeyFromMethodDeclarationString(m2);

        assertThat(k1).isNotEqualTo(k2);

        RegistrationResult r1 = catalog.register(k1);
        RegistrationResult r2 = catalog.register(k2);

        assertThat(r1.physicalId()).isNotEqualTo(r2.physicalId());
    }

    @Test
    void multipleRegistrations_sameKey_allShareSamePhysicalId() {
        // Ported from LambdaRegistryTest#testRegisterLambda_MultipleLogicalToOnePhysical.
        // Three normalisation-equivalent keys all map to the same physical ID;
        // the 2nd and 3rd register() calls report reused=true.
        String m = "public boolean eval(org.example.Person person) { return person.getAge() > 20; }";
        String mv1 = "public boolean eval(org.example.Person employee) { return employee.getAge() > 20; }";
        String mv2 = "public boolean eval(org.example.Person p) { return p.getAge() > 20; }";

        LambdaKey k = createLambdaKeyFromMethodDeclarationString(m);
        LambdaKey k1 = createLambdaKeyFromMethodDeclarationString(mv1);
        LambdaKey k2 = createLambdaKeyFromMethodDeclarationString(mv2);

        assertThat(k).isEqualTo(k1);
        assertThat(k).isEqualTo(k2);

        RegistrationResult r1 = catalog.register(k);
        RegistrationResult r2 = catalog.register(k1);
        RegistrationResult r3 = catalog.register(k2);

        assertThat(r1.physicalId()).isEqualTo(r2.physicalId());
        assertThat(r2.physicalId()).isEqualTo(r3.physicalId());
        assertThat(r1.reused()).isFalse();
        assertThat(r2.reused()).isTrue();
        assertThat(r3.reused()).isTrue();
    }

    @Test
    void M4_hashCollision_distinctKeys_keepSeparatePhysicalIds() {
        String method1 = "public boolean eval(java.lang.Object o) { return o != null; }";
        String method2 = "public int compute(java.lang.Object o) { return 1; }";

        LambdaKey k1 = createLambdaKeyFromMethodDeclarationString(method1);
        LambdaKey k2 = createLambdaKeyFromMethodDeclarationString(method2);
        k2.forceHash(k1.hashCode());     // package-private; same package

        RegistrationResult r1 = catalog.register(k1);
        RegistrationResult r2 = catalog.register(k2);

        assertThat(r1.physicalId()).isNotEqualTo(r2.physicalId());
        assertThat(r2.reused()).isFalse();
    }
}
