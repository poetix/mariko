package com.codepoetics.mariko.api;

import com.codepoetics.mariko.InterpretationException;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * An interpreter which attempts to interpret a String into an instance of a given type T.
 * @param <T> The type into which the input String is interpreted
 */
@FunctionalInterface
public interface Interpreter<T> {

    /**
     * Interpret the input string, or throw an {@link com.codepoetics.mariko.InterpretationException}.
     * @param input The string to interpret
     * @return An object representing the contents of the input string
     * @throws InterpretationException if this interpreter cannot interpret the given input string
     */
    @NotNull default T interpret(@NotNull String input) {
        return tryInterpret(input).orElseThrow(() ->
            new InterpretationException(
                "Unable to interpret input <%s>".formatted(input)
            ));
    }

    /**
     * Try to interpret the input string and return an object representing its contents, or null if unsuccessful
     * @param input The string to interpret
     * @return An object representing the contents of the input string, or Optional.empty if this interpreter could not interpret the input
     */
    @NotNull
    Optional<T> tryInterpret(@NotNull String input);

}
