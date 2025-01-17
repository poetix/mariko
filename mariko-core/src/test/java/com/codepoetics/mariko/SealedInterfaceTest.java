package com.codepoetics.mariko;

import com.codepoetics.mariko.api.FromPattern;
import org.junit.jupiter.api.Test;

import static com.codepoetics.mariko.Interpretation.interpret;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SealedInterfaceTest {

    public sealed interface PasswordOperation permits
            PasswordOperation.Rotate,
            PasswordOperation.Substitute {

        @FromPattern("Rotate the password left by (\\d) places")
        record Rotate(int places) implements PasswordOperation { }

        @FromPattern("Substitute '([a-z])' for the character in position (\\d)")
        record Substitute(char newCharacter, int position) implements PasswordOperation { }
    }

    @Test
    public void selectsSealedInterfaceMemberByRegexMax() {
        assertEquals(new PasswordOperation.Rotate(7),
                interpret(PasswordOperation.class, "Rotate the password left by 7 places"));

        assertEquals(new PasswordOperation.Substitute('c', 4),
                interpret(PasswordOperation.class, "Substitute 'c' for the character in position 4"));
    }
}
