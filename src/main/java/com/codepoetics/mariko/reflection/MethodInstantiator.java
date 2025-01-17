package com.codepoetics.mariko.reflection;

import com.codepoetics.mariko.InterpretationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

public class MethodInstantiator<T> implements Function<Object[], T> {

    @Nullable
    private final Object target;

    @NotNull
    private final Method method;

    public MethodInstantiator(@NotNull Object target, @NotNull Method method) {
        this.target = target;
        this.method = method;
    }

    public MethodInstantiator(@NotNull Method method) {
        this.target = null;
        this.method = method;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    public T apply(@NotNull Object[] parameterValues) {
        try {
            return (T) method.invoke(target, parameterValues);
        } catch (InvocationTargetException e) {
            throw new InterpretationException(
                    "Failure invoking method %s: %s".formatted(method, e.getCause().getMessage()));
        } catch (IllegalAccessException e) {
            throw new InterpretationException(
                    "Unable to access method %s: %s".formatted(method, e.getMessage()));
        }
    }
}
