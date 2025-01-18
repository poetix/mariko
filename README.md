# Mariko

> Now the Anjin has _two_ problems.

Often when completing Advent of Code puzzles in Java or Kotlin I've found myself hand-rolling code to parse a string with a regular expression, pull values out of subgroups of the regex match, convert those values into other types and populate a record or data class of some kind with them.

Because I am very lazy, but also very industrious, I have made Mariko to make this easier to do. Perhaps it will be useful for other purposes as well.

In the simplest case, it looks like this (Java):

```java
@FromPattern("\\((-?\\d+),\\s?(-?\\d+)\\)")
public record Point(long x, long y) { }

@Test
public void populateRecordFromString() {
    assertEquals(new Point(23, -6), interpret(Point.class, "(23, -6)"));
}

@FromPattern("Item #(\\d+) is at position (.*)")
public record ItemPosition(int itemId, Point position) { }

@Test
public void populateNestedDataClassFromSubstring() {
    assertEquals(new ItemPosition(123, new Point(-15, 7)),
            interpret(ItemPosition.class, "Item #123 is at position (-15, 7)"));
}
```

or like this (Kotlin):

```kotlin
@FromPattern("x=(-?\\d+),y=(-?\\d+)")
data class Point(val x: Long, val y: Long)

@Test
fun `populate data class from string`() {
    assertEquals(Point(-5L, 23L), "x=-5,y=23".interpret<Point>())
}

@FromPattern("Item #(\\d+) is at position (.*)")
data class ItemPosition(val id: Int, val position: Point)

@Test
fun `populate nested data class from substring`() {
    assertEquals(ItemPosition(123, Point(16, -23)),
        "Item #123 is at position x=16,y=-23".interpret<ItemPosition>())
}
```

In more complex cases we sometimes want to match inputs of different shapes to different sub-classes of the same base class.

This can be done with sealed interfaces like so (in Kotlin):

```kotlin
sealed interface Operand {
    @FromPattern("[a-z]")
    data class Register(val name: Char) : Operand

    @FromPattern("-?\\d+")
    data class Literal(val value: Int): Operand
}
```

and with non-sealed interfaces using static factory methods like so:

```kotlin
interface Opcode {
    companion object {
        @FromPattern("cpy (.*) ([a-z])")
        fun cpy(lhs: Operand, rhs: Char): Opcode = Cpy(lhs, rhs)

        @FromPattern("jnz ([a-z]) (-?\\d+)")
        fun jnz(register: Char, offset: Int): Opcode = Jnz(register, offset)
    }

    data class Cpy(val lhs: Operand, val rhs: Char) : Opcode
    data class Jnz(val register: Char, val offset: Int) : Opcode
}
```

In Java, this looks like:

```java
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
```

We can also do lists (but not yet maps):

```java
    public enum Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST
}

public record Point(long x, long y) {
    @FromPattern("(\\d+)m ((?i)north|south), (\\d+)m ((?i)east|west)")
    public static Point of(int y, Direction northOrSouth, int x, Direction eastOrWest) {
        return new Point(
                eastOrWest == Direction.EAST ? x : -x,
                northOrSouth == Direction.SOUTH ? y : -y);
    }

    @FromPattern("(\\d+)m ((?i)east|west)")
    public static Point of(int x, Direction eastOrWest) {
        return new Point(
                eastOrWest == Direction.EAST ? x : -x,
                0);
    }
}

@FromPattern("To find the treasure, go (.*)")
public record TreasureLocations(@FromList(", then ") List<Point> coordinates) { }

@Test
public void parsesListWithSeparator() {
    assertEquals(
            new TreasureLocations(
                    List.of(
                            new Point(10, -7),
                            new Point(-3, 16),
                            new Point(7, 0))),
            Interpretation.interpret(TreasureLocations.class,
                    "To find the treasure, go 7m North, 10m East, " +
                            "then 16m South, 3m West, then 7m East"));
}
```