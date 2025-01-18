package com.codepoetics.mariko.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface ParameterInfo permits ParameterInfo.ScalarParameter, ParameterInfo.CollectionParameter {
    record ScalarParameter(
            @NotNull String name,
            @NotNull Class<?> parameterClass,
            @Nullable String annotatedPattern) implements ParameterInfo { }

    record CollectionParameter(
            @NotNull String name,
            @NotNull Class<?> itemClass,
            @NotNull String separator,
            @NotNull Class<?> collectionClass,
            @Nullable String annotatedPattern) implements ParameterInfo { }
}
