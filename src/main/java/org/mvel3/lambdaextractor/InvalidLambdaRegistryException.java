package org.mvel3.lambdaextractor;

import java.io.IOException;

/**
 * Thrown when the lambda registry file is malformed, contains an unsupported
 * {@code format.version}, references a missing catalog entry from an artifact
 * entry, or contains duplicate physical IDs.
 */
public class InvalidLambdaRegistryException extends IOException {
    public InvalidLambdaRegistryException(String message) { super(message); }
    public InvalidLambdaRegistryException(String message, Throwable cause) { super(message, cause); }
}
