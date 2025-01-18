package com.codepoetics.mariko;

import com.codepoetics.mariko.api.InterpretationException;
import com.codepoetics.mariko.api.Interpreter;
import com.codepoetics.mariko.api.InterpreterBuildingException;
import com.codepoetics.mariko.reflection.InstanceBuilderInfo;
import com.codepoetics.mariko.reflection.ParameterInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class InterpretationContext {

    public static final InterpretationContext DEFAULT = new InterpretationContext();

    private final InterpreterCache interpreterCache = InterpreterCache.withPrimitives();

    public <T> @NotNull InterpretationContext addInterpreter(@NotNull Class<T> targetClass, @NotNull Interpreter<T> interpreter) {
        interpreterCache.put(targetClass, interpreter);

        return this;
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

    public @NotNull Interpreter<?> makeParameterInterpreter(@NotNull ParameterInfo parameter) {
        if (parameter instanceof ParameterInfo.ScalarParameter)
            return makeScalarParameterInterpreter((ParameterInfo.ScalarParameter) parameter);

        if (parameter instanceof ParameterInfo.CollectionParameter)
            return makeCollectionParameterInterpreter((ParameterInfo.CollectionParameter) parameter);

        throw new UnsupportedOperationException(
                "Cannot make parameter interpreter for parameter %s"
                        .formatted(parameter));
    }

    private @NotNull Interpreter<?> makeScalarParameterInterpreter(@NotNull ParameterInfo.ScalarParameter parameter) {
            return parameter.annotatedPattern() != null
                    ? makeInterpreter(parameter.parameterClass(), parameter.annotatedPattern())
                    : makeInterpreter(parameter.parameterClass());
    }

    private @NotNull Interpreter<?> makeCollectionParameterInterpreter(@NotNull ParameterInfo.CollectionParameter parameter) {
        return parameter.annotatedPattern() != null
            ? interpreterCache.getOrPut(
                parameter.collectionClass(),
                parameter.itemClass(),
                parameter.separator(),
                parameter.annotatedPattern(),
                () -> makeCollectionParameterInterpreterUncached(parameter))
            : interpreterCache.getOrPut(
                parameter.collectionClass(),
                parameter.itemClass(),
                parameter.separator(),
                () -> makeCollectionParameterInterpreterUncached(parameter));
    }

    @SuppressWarnings("unchecked")
    private @NotNull <T> Interpreter<Collection<T>> makeCollectionParameterInterpreterUncached(ParameterInfo.@NotNull CollectionParameter parameter) {
        Interpreter<T> scalarParameterInterpreter = (Interpreter<T>) (parameter.annotatedPattern() != null
                ? makeInterpreter(parameter.itemClass(), parameter.annotatedPattern())
                : makeInterpreter(parameter.itemClass()));

        var separatorRegex = Pattern.compile(parameter.separator());
        Supplier<Collection<T>> targetSupplier = supplierOf((Class<Collection<T>>) parameter.collectionClass());
        return (input) ->
                Optional.of(separatorRegex.splitAsStream(input)
                        .map(scalarParameterInterpreter::interpret)
                        .collect(Collectors.toCollection(targetSupplier)));
    }

    private static <T> Supplier<Collection<T>> supplierOf(Class<Collection<T>> collectionClass) {
        return () -> {
            try {
                return (Collection<T>) collectionClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new InterpretationException(
                        "Unable to instantiate collection of type %s".formatted(collectionClass)
                );
            }
        };
    }
}
