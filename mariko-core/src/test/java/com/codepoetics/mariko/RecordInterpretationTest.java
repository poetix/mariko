package com.codepoetics.mariko;

import com.codepoetics.mariko.api.FromPattern;
import com.codepoetics.mariko.api.InterpretationException;
import org.junit.jupiter.api.Test;

import static com.codepoetics.mariko.Interpretation.interpret;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RecordInterpretationTest {

    @FromPattern(value = "The (\\w+) sat on the (\\w+)")
    public record Sentence(String animal, String furniture) { }

    enum Animal {
        CAT,
        DOG,
        GOLDFISH;
    }

    @FromPattern(value = "[t|T]he (\\w+) sat on the (\\w+)")
    public record SentenceWithEnum(Animal animal, String furniture) { }

    @Test
    public void recordClassWithPrimitiveFields() {
        assertEquals(new Sentence("cat", "mat"),
                interpret(Sentence.class, "The cat sat on the mat"));
    }

    @Test
    public void recordClassWithEnumField() {
        assertEquals(new SentenceWithEnum(Animal.CAT, "mat"),
                interpret(SentenceWithEnum.class, "The cat sat on the mat"));

        assertEquals(new SentenceWithEnum(Animal.GOLDFISH, "mat"),
                interpret(SentenceWithEnum.class, "The GOLDFISH sat on the mat"));

        assertEquals(new SentenceWithEnum(Animal.DOG, "mat"),
                interpret(SentenceWithEnum.class, "The Dog sat on the mat"));

        assertThrows(InterpretationException.class, () ->
                interpret(SentenceWithEnum.class, "The axolotl sat on the mat"));
    }

    @FromPattern(value = "(\\w+) (\\w+)")
    public record Person(String firstname, String lastname) { }

    @FromPattern(value = "(.*) said, \"(.*)\"")
    public record IndirectSentence(Person speaker, SentenceWithEnum sentence) { }

    @Test
    public void recordClassWithRecordFields() {
        assertEquals(new IndirectSentence(
                new Person("Arthur", "Putey"),
                new SentenceWithEnum(Animal.DOG, "couch")),
                interpret(IndirectSentence.class, "Arthur Putey said, \"the dog sat on the couch\""));
    }

    @FromPattern(value = "\\((-?\\d+),\\s?(-?\\d+)\\)")
    public record Point(long x, long y) { }

    @Test
    public void populateRecordFromString() {
        assertEquals(new Point(23, -6), interpret(Point.class, "(23, -6)"));
    }

    @FromPattern(value = "Item #(\\d+) is at position (.*)")
    public record ItemPosition(int itemId, Point position) { }

    @Test
    public void populateNestedDataClassFromSubstring() {
        assertEquals(new ItemPosition(123, new Point(-15, 7)),
                interpret(ItemPosition.class, "Item #123 is at position (-15, 7)"));
    }

    @FromPattern(value = "From (.*) to (.*)")
    public record Line(
            @FromPattern(value = "x=(-?\\d+), y=(-?\\d+)") Point from,
            Point to) { }

    @Test
    public void parameterLevelPatternOverride() {
        assertEquals(new Point(12L, -6L), interpret(Point.class, "(12,-6)"));
        assertEquals(new Line(new Point(-7L, 0L), new Point(120L, -5L)),
                interpret(Line.class, "From x=-7, y=0 to (120,-5)"));
    }
}
