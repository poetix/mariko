package com.codepoetics.mariko.reflection;

import com.codepoetics.mariko.api.InterpretationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

class ConstructorInstantiator<T> implements Function<Object[], T> {

    private final Constructor<T> constructor;

    public ConstructorInstantiator(Constructor<T> constructor) {
        this.constructor = constructor;
    }

    @Override
    public T apply(Object[] parameterValues) {
        try {
            return constructor.newInstance(parameterValues);
        } catch (InvocationTargetException e) {
            throw new InterpretationException(
                    "Failure invoking constructor %s: %s".formatted(constructor, e.getCause().getMessage())
            );
        } catch (InstantiationException e) {
            throw new InterpretationException(
                    "Failure instantiating %s with constructor %s: %s"
                            .formatted(constructor.getAnnotatedReturnType(), constructor, e.getMessage())
            );
        } catch (IllegalAccessException e) {
            throw new InterpretationException(
                    "Unable to access constructor %s: %s".formatted(constructor, e.getMessage()));
        }
    }
}
