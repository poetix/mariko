package com.codepoetics.mariko;

import com.codepoetics.mariko.api.FromList;
import com.codepoetics.mariko.api.FromPattern;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListParameterTest {

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
}
