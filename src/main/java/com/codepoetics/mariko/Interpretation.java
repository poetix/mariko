package com.codepoetics.mariko;

import com.codepoetics.mariko.api.Interpreter;

public final class Interpretation {

    private Interpretation() {
    }

    public static <T> Interpreter<T> interpreter(Class<T> targetClass) {
        return InterpretationContext.DEFAULT.makeInterpreter(targetClass);
    }

    public static <T> Interpreter<T> interpreter(Class<T> targetClass, String pattern) {
        return InterpretationContext.DEFAULT.makeInterpreter(targetClass, pattern);
    }

    public static <T> T interpret(Class<T> targetClass, String input) {
        return interpreter(targetClass).interpret(input);
    }
}
