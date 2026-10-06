package org.mvel3.compat.mvel2;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an imported MVEL2 test that is expected to fail due to a known,
 * documented behavioural difference between MVEL2 and MVEL3.
 *
 * <p>The test is still executed and its failure remains visible; this
 * annotation only records the reason. See {@code Compatibility.md} in this
 * package for a description of each difference.
 *
 * <h3>Values</h3>
 * <dl>
 *   <dt>{@code "power-operator-type"}</dt>
 *   <dd>{@code **} returns {@code int} in MVEL2 but {@code double} in MVEL3.</dd>
 *
 *   <dt>{@code "integer-division"}</dt>
 *   <dd>Integer division returns {@code double} in MVEL2 but {@code int} in MVEL3.</dd>
 *
 *   <dt>{@code "block-syntax"}</dt>
 *   <dd>MVEL2 allows bare assignments and omits {@code var} / trailing semicolons;
 *       MVEL3 requires Java block syntax.</dd>
 * </dl>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Tag("mvel2-known-compatibility-difference")
public @interface KnownCompatibility {
    /**
     * Short identifier matching a section in {@code Compatibility.md}.
     * One of: {@code "power-operator-type"}, {@code "integer-division"},
     * {@code "block-syntax"}.
     */
    String value();
}
