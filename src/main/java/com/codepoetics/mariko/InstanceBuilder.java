package com.codepoetics.mariko;

import com.codepoetics.mariko.api.Interpreter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstanceBuilder<T> implements Interpreter<T> {

    private final Pattern pattern;
    private final ParameterValueExtractor extractor;
    private final Function<Object[], T> initialiser;

    public InstanceBuilder(Pattern pattern, ParameterValueExtractor extractor, Function<Object[], T> initialiser) {
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
