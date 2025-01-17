package com.codepoetics.mariko;

import com.codepoetics.mariko.api.Interpreter;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class InterpreterCache {

    public static InterpreterCache withPrimitives() {
        InterpreterCache interpreterCache = new InterpreterCache();

        interpreterCache.put(int.class, s -> Optional.of(Integer.parseInt(s)));
        interpreterCache.put(long.class, s -> Optional.of(Long.parseLong(s)));
        interpreterCache.put(boolean.class, s -> Optional.of(Boolean.parseBoolean(s)));
        interpreterCache.put(char.class, s -> Optional.of(s.toCharArray()[0]));
        interpreterCache.put(String.class, Optional::ofNullable);

        return interpreterCache;
    }

    private sealed interface InterpreterKey {
        Class<?> targetClass();
        record ClassIdentifier(Class<?> targetClass) implements InterpreterKey { }
        record ClassAndPatternIdentifier(Class<?> targetClass, String pattern) implements InterpreterKey { }
    }

    private static final class DeferredInterpreter<T> implements Interpreter<T> {
        private final InterpreterCache cache;
        private final InterpreterKey key;
        private final AtomicReference<Interpreter<T>> finishedInterpreter = new AtomicReference<>();

        private DeferredInterpreter(InterpreterCache cache, InterpreterKey key) {
            this.cache = cache;
            this.key = key;
        }

        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public Optional<T> tryInterpret(@NotNull String input) {
            return finishedInterpreter.updateAndGet(existing ->
                    existing == null ? (Interpreter<T>) cache.cachedInterpreters.get(key) : existing)
                    .tryInterpret(input);
        }
    }

    private final ConcurrentMap<InterpreterKey, Interpreter<?>> cachedInterpreters = new ConcurrentHashMap<>();
    private final ThreadLocal<Set<InterpreterKey>> underConstruction = ThreadLocal.withInitial(HashSet::new);

    public <T> void put(Class<T> targetClass, Interpreter<T> interpreter) {
        cachedInterpreters.put(new InterpreterKey.ClassIdentifier(targetClass), interpreter);
    }

    public <T> Interpreter<T> getOrPut(Class<T> targetClass, Supplier<Interpreter<T>> builder) {
        return getOrPut(new InterpreterKey.ClassIdentifier(targetClass), builder);
    }

    public <T> Interpreter<T> getOrPut(Class<T> targetClass, String pattern, Supplier<Interpreter<T>> builder) {
        return getOrPut(new InterpreterKey.ClassAndPatternIdentifier(targetClass, pattern), builder);
    }

    @SuppressWarnings("unchecked")
    private <T> Interpreter<T> getOrPut(InterpreterKey key, Supplier<Interpreter<T>> builder) {
        var cached = cachedInterpreters.get(key);
        if (cached != null) return (Interpreter<T>) cached;

        if (!underConstruction.get().add(key)) {
            return new DeferredInterpreter<>(this, key);
        }
        try {
            var built = builder.get();
            cachedInterpreters.put(key, built);
            return built;
        } finally {
            underConstruction.get().remove(key);
        }
    }
}
