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
 *
 *   <dt>{@code "string-coercion"}</dt>
 *   <dd>MVEL2 implicitly coerces {@code String} values to numbers at runtime;
 *       MVEL3 resolves types at compile time and rejects {@code String * Integer}.</dd>
 *
 *   <dt>{@code "unsigned-left-shift"}</dt>
 *   <dd>MVEL2 has a {@code <<<} unsigned-left-shift operator (equivalent to a
 *       bitwise rotate); Java and MVEL3 do not have this operator.</dd>
 *
 *   <dt>{@code "single-quote-string"}</dt>
 *   <dd>MVEL2 accepts single-quoted multi-character literals as {@code String};
 *       MVEL3 treats them as {@code char} literals (Java semantics), so
 *       {@code 'bar'} is a parse error.</dd>
 *
 *   <dt>{@code "missing-auto-import"}</dt>
 *   <dd>MVEL2 auto-imports {@code java.math.*} (including {@code BigDecimal});
 *       MVEL3 does not, so expressions that use {@code BigDecimal} by simple name
 *       fail to compile unless the type is fully qualified.</dd>
 *
 *   <dt>{@code "dynamic-property-type"}</dt>
 *   <dd>MVEL2 resolves property access on {@code Map} values dynamically at
 *       runtime; MVEL3 resolves types statically and property access on an
 *       {@code Object}-typed map value yields {@code Object}, which cannot be
 *       used as an arithmetic operand.</dd>
 *
 *   <dt>{@code "class-literal"}</dt>
 *   <dd>MVEL2 evaluates a bare qualified class name (e.g. {@code java.lang.String})
 *       as a {@code Class} object; MVEL3 treats it as a field-access chain and
 *       fails to compile.  Use {@code String.class} instead.</dd>
 * </dl>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Tag("mvel2-known-compatibility-difference")
public @interface KnownCompatibility {
    /**
     * Short identifier matching a section in {@code Compatibility.md}.
     * One of: {@code "power-operator-type"}, {@code "integer-division"},
     * {@code "block-syntax"}, {@code "string-coercion"},
     * {@code "unsigned-left-shift"}, {@code "single-quote-string"},
     * {@code "missing-auto-import"}, {@code "dynamic-property-type"},
     * {@code "class-literal"}.
     */
    String value();
}
