package com.codepoetics.mariko;

import com.codepoetics.mariko.api.FromPattern;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InterpretationContextTest {

    @FromPattern("(.*) (.*)")
    public record UUIDPair(UUID first, UUID second) { }

    @Test
    public void addInterpreterForUUID() {
        var context = new InterpretationContext()
                .addInterpreter(UUID.class, s -> Optional.of(UUID.fromString(s)));

        var uuid1 = UUID.randomUUID();
        var uuid2 = UUID.randomUUID();

        assertEquals(new UUIDPair(uuid1, uuid2),
                context.makeInterpreter(UUIDPair.class).interpret("%s %s".formatted(uuid1, uuid2)));
    }
}
