package com.codepoetics.mariko.kotlin

import com.codepoetics.mariko.api.FromPattern
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DataClassTest {

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
}