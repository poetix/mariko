package com.codepoetics.mariko;

import com.codepoetics.mariko.api.Interpreter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

final class FirstMatchingInstanceBuilderInterpreter<T> implements Interpreter<T> {

    private final List<InstanceBuilder<T>> builders;

    public FirstMatchingInstanceBuilderInterpreter(List<InstanceBuilder<T>> builders) {
        this.builders = builders;
    }

    @NotNull
    @Override
    public Optional<T> tryInterpret(@NotNull String input) {
        return builders.stream().map(builder -> builder.tryInterpret(input))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}
