package com.mywehr.exceptions;

/**
 * Single unchecked exception type for every unrecoverable framework-level
 * failure (bad config, unreadable test data, unsupported browser...).
 * Keeping one type makes listener-level handling and reporting uniform.
 */
public class FrameworkException extends RuntimeException {

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
