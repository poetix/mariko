package com.codepoetics.mariko.kotlin

import com.codepoetics.mariko.api.FromPattern
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestKotlinSealedInterface {

    sealed interface Creature {
        @FromPattern(value = "(\\w+)\\s+(\\w+)")
        data class Person(val firstName: String, val lastName: String) : Creature

        @FromPattern(value = "(\\w+), owned by (.*)")
        data class Dog(val name: String, val owner: Person): Creature {
            companion object {
                @FromPattern(value = "(\\w+\\s+\\w+)'s dog, (\\w+)")
                fun alternative(owner: Person, name: String): Dog = Dog(name, owner)
            }
        }
    }

    @Test
    fun `can interpret Kotlin sealed interface`() {
        assertEquals(
            Creature.Dog("Fido", Creature.Person("Arthur", "Putey")),
            "Fido, owned by Arthur Putey".interpret<Creature>())

        assertEquals(
            Creature.Dog("Fido", Creature.Person("Arthur", "Putey")),
            "Arthur Putey's dog, Fido".interpret<Creature>())
    }
}