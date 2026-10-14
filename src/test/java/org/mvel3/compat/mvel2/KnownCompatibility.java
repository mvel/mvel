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
 *
 *   <dt>{@code "hash-concat-operator"}</dt>
 *   <dd>MVEL2's {@code #} operator coerces both sides to {@code String} and
 *       concatenates them; MVEL3 uses {@code #} for inline casts such as
 *       {@code value#int#} and does not support this concatenation syntax.
 *       Use {@code "" + a + b} instead.</dd>
 *
 *   <dt>{@code "inline-import"}</dt>
 *   <dd>MVEL2 accepts {@code import Foo;} as an inline expression statement;
 *       MVEL3 rejects it (import is not valid inside a method body).
 *       Pass the type via the API imports set instead.</dd>
 *
 *   <dt>{@code "foreach-syntax"}</dt>
 *   <dd>MVEL3 does not yet support MVEL2's {@code foreach} statement syntax.
 *       The lexer defines the keyword, but the parser has no corresponding
 *       statement rule. The current null-node failure also occurs without
 *       comments in the body.</dd>
 *   <dt>{@code "javaparser-null-node"}</dt>
 *   <dd>Certain patterns crash the MVEL3 transpiler with a JavaParser null-node
 *       NPE ({@code "A reference was unexpectedly null"}).  Known trigger:
 *       {@code foreach} transpilation fails even without a comment in the body.
 *       These are transpiler defects; no workaround exists.</dd>
 *
 *   <dt>{@code "void-method-expression"}</dt>
 *   <dd>MVEL2 accepts a {@code void} method call as an expression whose value is
 *       ignored; MVEL3 compiles a single expression as an {@code Object}-typed
 *       return value, so {@code void} is rejected.  Use a block statement
 *       ({@code call(); return null;}).</dd>
 *
 *   <dt>{@code "compound-assign-void-setter"}</dt>
 *   <dd>A compound assignment on a property ({@code holder.intPrimitive += 10},
 *       {@code holder.bigDecimal += 10}) is transpiled to a {@code void} setter
 *       call, so it cannot be returned as the expression value (MVEL2 returns the
 *       new value).  {@code BigDecimal} targets additionally need an explicit
 *       {@code java.math.BigDecimal} import.  Write the assignment as a statement
 *       and {@code return} the property afterwards.</dd>
 *
 *   <dt>{@code "property-assign-void-setter"}</dt>
 *   <dd>A plain property assignment ({@code foo.bar.age = 21}) is transpiled to a
 *       {@code void} setter call, so it cannot be the value of an expression (MVEL2
 *       returns the assigned value).  Write it as a statement followed by
 *       {@code return}.</dd>
 *
 *   <dt>{@code "input-variable-redeclaration"}</dt>
 *   <dd>MVEL2 lets an expression declare a local variable whose name matches an
 *       entry of the variable map ({@code a}, {@code b}, {@code c}); MVEL3 turns every
 *       map entry into a method-local declaration, so redeclaring it fails with
 *       {@code variable a is already defined}.</dd>
 *
 *   <dt>{@code "inner-class-binary-name"}</dt>
 *   <dd>{@code "new " + Outer.Inner.class.getName()} yields the {@code $}-separated
 *       binary name, which MVEL3 cannot resolve.  Import the class through the
 *       API imports set and use its simple name.</dd>
 *
 *   <dt>{@code "big-number-null-comparison"}</dt>
 *   <dd>In MVEL2, {@code ==} / {@code !=} with {@code BigDecimal}/{@code BigInteger}
 *       and {@code null} evaluates safely without NPE. In MVEL3, comparison is
 *       transpiled to {@code .compareTo(...) == 0} without null checks, causing
 *       {@code NullPointerException} when either operand is null.</dd>
 *
 *   <dt>{@code "null-relational-comparison"}</dt>
 *   <dd>In MVEL2, relational comparisons ({@code <}, {@code >}, {@code <=}, {@code >=})
 *       with {@code null} safely evaluate to false. In MVEL3, unboxing null wrapper
 *       types or calling {@code .compareTo} on null causes {@code NullPointerException}.</dd>
 *
 *   <dt>{@code "set-expression-api"}</dt>
 *   <dd>MVEL2's {@code compileSetExpression} / {@code executeSetExpression} (assign a
 *       value to a property path with type coercion) have no MVEL3 equivalent.  Use a
 *       normal assignment statement in a block with the value passed as an input.</dd>
 *
 *   <dt>{@code "projection-operator"}</dt>
 *   <dd>MVEL2 collection projection {@code (name in things)} and
 *       {@code (toUpperCase() in list if ($ == 'bar'))} is not supported by MVEL3
 *       (parse error at {@code in}).  Use an explicit {@code for} loop.</dd>
 *
 *   <dt>{@code "inline-array-literal"}</dt>
 *   <dd>MVEL2's inline array literal {@code {1,2,3}} / {@code {{1,2},{3}}} is not supported by
 *       MVEL3 (parse error at {@code {}).  Use Java array syntax such as
 *       {@code new int[] {1,2,3}}.</dd>
 *
 *   <dt>{@code "list-concat-operator"}</dt>
 *   <dd>{@code [1,2,3] + [4,5,6]} and {@code [1,2,3] + 4} concatenate lists in MVEL2; MVEL3
 *       generates a Java {@code +} on two {@code List} operands, which does not compile.
 *       Use {@code addAll} / {@code add}.</dd>
 *
 *   <dt>{@code "trailing-comma-list"}</dt>
 *   <dd>MVEL2 ignores a trailing comma in {@code [2, 3, 4, ]}; MVEL3 fails with "A reference was
 *       unexpectedly null".  Remove the trailing comma.</dd>
 *
 *   <dt>{@code "nested-inline-collection"}</dt>
 *   <dd>A list or map literal nested inside another list or map literal
 *       ({@code ["k" : [1,2]]}, {@code [[1,2],[3]]}) generates invalid Java, which javac rejects
 *       with "illegal start of expression" (MVEL2 accepts it).  Declare the inner collection
 *       first and reference it by name.</dd>
 *
 *   <dt>{@code "reserved-method-name"}</dt>
 *   <dd><b>IMPORTANT: to be addressed with priority.</b> {@code contains} and {@code matches}
 *       are lexer keywords ({@code CONTAINS}, {@code MATCHES}) that the {@code identifier} rule
 *       does not accept, so a method call such as {@code list.contains("x")} or
 *       {@code s.matches("..")} fails with "Unsupported member reference" on any receiver.
 *       Use {@code indexOf(..) >= 0}, {@code containsKey}, {@code containsAll}, etc.</dd>
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
     * {@code "class-literal"}, {@code "hash-concat-operator"},
     * {@code "foreach-syntax"}.
     * {@code "inline-import"}, {@code "javaparser-null-node"},
     * {@code "void-method-expression"}, {@code "inner-class-binary-name"},
     * {@code "compound-assign-void-setter"}, {@code "property-assign-void-setter"},
     * {@code "input-variable-redeclaration"}, {@code "set-expression-api"},
     * {@code "projection-operator"}, {@code "inline-array-literal"},
     * {@code "list-concat-operator"}, {@code "trailing-comma-list"},
     * {@code "nested-inline-collection"}, {@code "reserved-method-name"}.
     */
    String value();
}
