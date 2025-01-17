package com.codepoetics.mariko.reflection;

import com.codepoetics.mariko.api.InterpretationException;
import com.codepoetics.mariko.api.InterpreterBuildingException;
import com.codepoetics.mariko.api.FromPattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record InstanceBuilderInfo<T>(Pattern pattern, List<ParameterInfo> parameters, Function<Object[], T> instantiator) {

    public static <T> @NotNull List<InstanceBuilderInfo<T>> forClass(@NotNull Class<T> targetClass, @Nullable Pattern overridePattern) {
        if (targetClass.isSealed()) {
            return forSealedSubclasses(targetClass, overridePattern);
        }

        if (targetClass.isEnum()) {
            return forEnum(targetClass, overridePattern);
        }

        List<InstanceBuilderInfo<T>> result =  new ArrayList<>();

        addDefaultConstructor(targetClass, overridePattern, result);
        addSecondaryConstructors(targetClass, result);
        addStaticBuilders(targetClass, result);

        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> @NotNull List<InstanceBuilderInfo<T>> forSealedSubclasses(@NotNull Class<T> targetClass, @Nullable Pattern overridePattern) {
        if (overridePattern != null) {
            throw new InterpreterBuildingException(
                    "Override pattern %s supplied for sealed class %s"
                            .formatted(overridePattern, targetClass));
        }

        return Arrays.stream(targetClass.getPermittedSubclasses())
                .flatMap(subclass -> forClass(subclass, null).stream().map(info -> (InstanceBuilderInfo<T>) info))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> @NotNull List<InstanceBuilderInfo<T>> forEnum(@NotNull Class<T> targetClass, @Nullable Pattern overridePattern) {
        Pattern classLevelPattern = overridePattern == null
                ? targetClass.isAnnotationPresent(FromPattern.class)
                    ? Pattern.compile(targetClass.getAnnotation(FromPattern.class).value())
                    : Pattern.compile(Arrays.stream(targetClass.getEnumConstants())
                        .map(e -> ((Enum<?>) e).name().toLowerCase() )
                        .collect(Collectors.joining("|", "^(?i)", "$")))
                : overridePattern;

        Function<Object[], T> instantiator = args -> {
            String input = (String) args[0];
            return (T) Arrays.stream(targetClass.getEnumConstants())
                    .map(e -> (Enum<?>) e)
                    .filter(enumConstant -> enumConstant.name().equalsIgnoreCase(input))
                    .findFirst()
                    .orElseThrow(() -> new InterpretationException(
                            "No enum constant %s found for input \"%s\""
                                    .formatted(targetClass, input)));
        };

        List<ParameterInfo> parameters = List.of(new ParameterInfo("value", String.class, String.class, null));

        return List.of(new InstanceBuilderInfo<>(classLevelPattern, parameters, instantiator));
    }

    @SuppressWarnings("unchecked")
    private static <T> void addDefaultConstructor(@NotNull Class<T> targetClass, @Nullable Pattern overridePattern, List<InstanceBuilderInfo<T>> result) {
        var classLevelPattern = overridePattern == null
            ? targetClass.isAnnotationPresent(FromPattern.class)
                ? Pattern.compile(targetClass.getAnnotation(FromPattern.class).value())
                : null
            : overridePattern;

        var unannotatedConstructors = Arrays.stream(targetClass.getDeclaredConstructors())
                .filter(ctor -> !ctor.isAnnotationPresent(FromPattern.class))
                .map(ctor -> (Constructor<T>) ctor)
                .toList();

        if (unannotatedConstructors.size() == 1 && classLevelPattern != null) {
            result.add(new InstanceBuilderInfo<>(
                    classLevelPattern,
                    getParameters(unannotatedConstructors.get(0)),
                    new ConstructorInstantiator<>(unannotatedConstructors.get(0))));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void addSecondaryConstructors(@NotNull Class<T> targetClass, List<InstanceBuilderInfo<T>> result) {
        Arrays.stream(targetClass.getDeclaredConstructors())
                .filter(ctor -> ctor.isAnnotationPresent(FromPattern.class))
                .map(ctor -> (InstanceBuilderInfo<T>) new InstanceBuilderInfo<>(
                        getPattern(ctor),
                        getParameters(ctor),
                        new ConstructorInstantiator<>(ctor)
                ))
                .forEach(result::add);
    }

    private static @NotNull List<ParameterInfo> getParameters(@NotNull Executable executable) {
        return Arrays.stream(executable.getParameters())
                .map(parameter -> new ParameterInfo(
                        parameter.getName(),
                        parameter.getType(),
                        parameter.getParameterizedType(),
                        parameter.isAnnotationPresent(FromPattern.class)
                            ? parameter.getAnnotation(FromPattern.class).value()
                            : null
                        )
                )
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> void addStaticBuilders(@NotNull Class<T> targetClass, List<InstanceBuilderInfo<T>> result) {
        Arrays.stream(targetClass.getDeclaredMethods())
                .filter(method -> isStaticBuilder(targetClass, method))
                .map(InstanceBuilderInfo::<T>makeMethodInstanceBuilder)
                .forEach(result::add);

        Arrays.stream(targetClass.getDeclaredClasses())
                .filter(cls -> cls.getSimpleName().equals("Companion") && Modifier.isStatic(cls.getModifiers()))
                .findFirst()
                .ifPresent(companionClass -> makeCompanionMethodBuilders(targetClass, result, companionClass));
    }

    private static <T> void makeCompanionMethodBuilders(@NotNull Class<T> targetClass, List<InstanceBuilderInfo<T>> result, Class<?> companionClass) {
        Object companion = getCompanionObject(targetClass);
        Arrays.stream(companionClass.getDeclaredMethods())
                .filter(method -> isCompanionBuilder(targetClass, method))
                .map(method -> makeCompanionMethodInstanceBuilder(targetClass, companion, method))
                .forEach(result::add);
    }

    private static boolean isCompanionBuilder(Class<?> targetClass, Method method) {
        return method.isAnnotationPresent(FromPattern.class) &&
                method.getReturnType().equals(targetClass);
    }

    private static <T> InstanceBuilderInfo<T> makeCompanionMethodInstanceBuilder(Class<T> targetClass, Object companionObject, Method method) {
        return new InstanceBuilderInfo<>(
                getPattern(method),
                getParameters(method),
                new MethodInstantiator<>(companionObject, method)
        );
    }

    private static <T> @NotNull Object getCompanionObject(@NotNull Class<T> targetClass) {
        try {
            Field companion = targetClass.getDeclaredField("Companion");
            companion.setAccessible(true);
            return companion.get(null);
        } catch (Exception e) {
            throw new InterpreterBuildingException(
                    "Unable to obtain companion object for %s: %s"
                            .formatted(targetClass, e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> @NotNull InstanceBuilderInfo<T> makeMethodInstanceBuilder(Method method) {
        return new InstanceBuilderInfo<>(
                getPattern(method),
                getParameters(method),
                new MethodInstantiator<>(method)
        );
    }

    private static @NotNull Pattern getPattern(AnnotatedElement element) {
        return Pattern.compile(element.getAnnotation(FromPattern.class).value());
    }

    private static <T> boolean isStaticBuilder(@NotNull Class<T> targetClass, Method method) {
        return Modifier.isStatic(method.getModifiers()) &&
                method.getReturnType().equals(targetClass) &&
                method.isAnnotationPresent(FromPattern.class);
    }
}
