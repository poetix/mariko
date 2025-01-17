package com.codepoetics.mariko;

import com.codepoetics.mariko.api.Interpreter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class InstanceBuilder<T> implements Interpreter<T> {

    private final @NotNull Pattern pattern;
    private final @NotNull ParameterValueExtractor extractor;
    private final @NotNull Function<Object[], T> initialiser;

    public InstanceBuilder(@NotNull Pattern pattern, @NotNull ParameterValueExtractor extractor, @NotNull Function<Object[], T> initialiser) {
        this.pattern = pattern;
        this.extractor = extractor;
        this.initialiser = initialiser;
    }

    @NotNull
    @Override
    public Optional<T> tryInterpret(@NotNull String input) {
        Matcher matcher = pattern.matcher(input);
        if (!matcher.matches()) return Optional.empty();

        return Optional.of(
            initialiser.apply(
                    extractor.extractParameterValues(
                            matcher.toMatchResult()))
        );
    }
}
