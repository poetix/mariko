package com.codepoetics.mariko

import com.codepoetics.mariko.api.Interpreter
import java.util.*

inline fun <reified T : Any> interpreter(): Interpreter<T> =
    InterpretationContext.DEFAULT.makeInterpreter(T::class.java)

inline fun <reified T : Any> interpreter(pattern: String): Interpreter<T> =
    InterpretationContext.DEFAULT.makeInterpreter(T::class.java, pattern)

inline fun <reified T : Any> String.interpret(): T =
    interpreter<T>().interpret(this)

inline fun <reified T : Any> Iterable<String>.interpret(): List<T> =
    map(interpreter<T>()::interpret)

inline fun <reified T : Any> Iterable<String>.interpret(pattern: String): List<T> =
    map(interpreter<T>(pattern)::interpret)

inline fun <reified T : Any> Sequence<String>.interpret(): Sequence<T> =
    map(interpreter<T>()::interpret)

inline fun <reified T : Any> Sequence<String>.interpret(pattern: String): Sequence<T> =
    map(interpreter<T>(pattern)::interpret)

class InterpretationContextScope(val context: InterpretationContext) {
    inline fun <reified T : Any> add(crossinline interpreter: (String) -> T?) {
        context.addInterpreter(T::class.java) { input -> Optional.ofNullable(interpreter(input)) }
    }

    inline fun <reified T : Any> interpreter() =
        context.makeInterpreter(T::class.java)

    inline fun <reified T : Any> interpreter(pattern: String) =
        context.makeInterpreter(T::class.java, pattern)

    inline fun <reified T : Any> String.interpret(): T = interpreter<T>().interpret(this)

    inline fun <reified T : Any> Iterable<String>.interpret(): List<T> =
        map(interpreter<T>()::interpret)

    inline fun <reified T : Any> Iterable<String>.interpret(pattern: String): List<T> =
        map(interpreter<T>(pattern)::interpret)

    inline fun <reified T : Any> Sequence<String>.interpret(): Sequence<T> =
        map(interpreter<T>()::interpret)

    inline fun <reified T : Any> Sequence<String>.interpret(pattern: String): Sequence<T> =
        map(interpreter<T>(pattern)::interpret)
}

inline fun <R> inContext(context: InterpretationContext, block: InterpretationContextScope.() -> R): R
    = InterpretationContextScope(context).block()

inline fun <R> inNewContext(block: InterpretationContextScope.() -> R): R
    = InterpretationContextScope(InterpretationContext()).block()
