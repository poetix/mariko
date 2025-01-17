package com.codepoetics.mariko.api;

/**
 * Exception thrown if interpretation of an input string is not possible.
 */
public class InterpretationException extends RuntimeException {
    public InterpretationException(String message) {
        super(message);
    }
}
