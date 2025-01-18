package com.codepoetics.mariko.kotlin

import com.codepoetics.mariko.api.FromPattern
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompanionObjectBuilderTest {

    sealed interface Operand {
        @FromPattern(value = "[a-z]")
        data class Register(val name: Char) : Operand

        @FromPattern(value = "-?\\d+")
        data class Literal(val value: Int): Operand
    }

    interface Opcode {
        companion object {
            @FromPattern(value = "cpy (.*) ([a-z])")
            fun cpy(lhs: Operand, rhs: Char): Opcode = Cpy(lhs, rhs)

            @FromPattern(value = "jnz ([a-z]) (-?\\d+)")
            fun jnz(register: Char, offset: Int): Opcode = Jnz(register, offset)
        }

        data class Cpy(val lhs: Operand, val rhs: Char) : Opcode
        data class Jnz(val register: Char, val offset: Int) : Opcode
    }

    @Test
    fun `can interpret non-sealed interface with builder methods in companion`() {
        assertEquals(Opcode.Cpy(Operand.Register('a'), 'b'), "cpy a b".interpret<Opcode>())
        assertEquals(Opcode.Cpy(Operand.Literal(12), 'b'), "cpy 12 b".interpret<Opcode>())
        assertEquals(Opcode.Jnz('c', -2), "jnz c -2".interpret<Opcode>())
    }
}