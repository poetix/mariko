package com.codepoetics.mariko.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public record ParameterInfo(
        @NotNull String name,
        @NotNull Class<?> parameterClass,
        @NotNull Type parameterType,
        @Nullable String annotatedPattern) {
}
