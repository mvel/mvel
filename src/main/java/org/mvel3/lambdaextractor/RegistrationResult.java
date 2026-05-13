package org.mvel3.lambdaextractor;

/**
 * Result of {@link LambdaCatalog#register(LambdaKey)}.
 * <p>
 * {@code reused} is true if this registration matched an existing entry via
 * exact-key or subtype-overload reuse.
 */
public record RegistrationResult(int logicalId, int physicalId, boolean reused) {}
