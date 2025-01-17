package com.codepoetics.mariko

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.*

class TestInterpretationContextScope {

    @Test
    fun `scope interpretation to a context`() {
        val uuid = UUID.randomUUID()

        inNewContext {
            add<UUID> { UUID.fromString(it) }

            assertEquals(uuid, uuid.toString().interpret<UUID>())
        }

        assertThrows(InterpreterBuildingException::class.java) {
            uuid.toString().interpret<UUID>()
        }
    }
}