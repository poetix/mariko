package com.codepoetics.mariko;

/**
 * Exception thrown if an interpreter could not be built
 */
public class InterpreterBuildingException extends RuntimeException {
    public InterpreterBuildingException(String message) {
        super(message);
    }
}
