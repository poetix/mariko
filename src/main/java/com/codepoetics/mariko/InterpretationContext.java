package com.codepoetics.mariko;

import com.codepoetics.mariko.api.Interpreter;
import com.codepoetics.mariko.reflection.InstanceBuilderInfo;
import com.codepoetics.mariko.reflection.ParameterInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.regex.Pattern;

public final class InterpretationContext {

    public static final InterpretationContext DEFAULT = new InterpretationContext();

    private final InterpreterCache interpreterCache = InterpreterCache.withPrimitives();

    public <T> @NotNull InterpretationContext addInterpreter(@NotNull Class<T> targetClass, @NotNull Interpreter<T> interpreter) {
        interpreterCache.put(targetClass, interpreter);

        return this;
    }

    public <T> @NotNull Interpreter<T> makeInterpreter(@NotNull Class<T> targetClass, @NotNull Type targetType) {
        if (targetType.equals(targetClass)) return makeInterpreter(targetClass);
        throw new UnsupportedOperationException("We don't do generics yet");
    }

    public <T> @NotNull Interpreter<T> makeInterpreter(@NotNull Class<T> targetClass) {
        return interpreterCache.getOrPut(
                targetClass,
                () -> makeInterpreterUncached(targetClass, null));
    }

    public <T> @NotNull Interpreter<T> makeInterpreter(@NotNull Class<T> targetClass, @NotNull String overridePattern) {
        return interpreterCache.getOrPut(
                targetClass,
                overridePattern,
                () -> makeInterpreterUncached(targetClass, Pattern.compile(overridePattern)));
    }

    private <T> @NotNull Interpreter<T> makeInterpreterUncached(@NotNull Class<T> targetClass, @Nullable Pattern overridePattern) {
        var builders = InstanceBuilderInfo.forClass(targetClass, overridePattern).stream()
                .map(info -> new InstanceBuilder<>(
                        info.pattern(),
                        new ParameterValueExtractor(info.parameters().stream()
                                .map(this::makeParameterInterpreter)
                                .toList()),
                        info.instantiator()
                )).toList();

        if (builders.isEmpty()) {
            if (targetClass.isSealed()) {
                throw new InterpreterBuildingException(
                        ("No method found to interpret input into sealed %s -" +
                        "try annotating a permitted subclass (if it has a unique default constructor), " +
                        "one of its constructors or a static builder method with @FromPattern")
                                .formatted(targetClass));
            }
            throw new InterpreterBuildingException(
                    ("No method found to interpret input into %s - " +
                    "try annotating the class (if it has a unique default constructor), " +
                    "a constructor, a static builder method, or a permitted subclass with @FromPattern")
                            .formatted(targetClass));
        }

        return new FirstMatchingInstanceBuilderInterpreter<>(builders);
    }

    public <T> @NotNull Interpreter<T> makeInterpreter(@NotNull Class<T> targetClass, @NotNull Type targetType, @NotNull String pattern) {
        if (targetType.equals(targetClass)) return makeInterpreter(targetClass, pattern);
        throw new UnsupportedOperationException("We don't do generics yet");
    }

    public @NotNull Interpreter<?> makeParameterInterpreter(ParameterInfo parameter) {
        return parameter.annotatedPattern() != null
                ? makeInterpreter(
                    parameter.parameterClass(),
                    parameter.parameterType(),
                    parameter.annotatedPattern()
                )
                : makeInterpreter(parameter.parameterClass(), parameter.parameterType());
    }
}
