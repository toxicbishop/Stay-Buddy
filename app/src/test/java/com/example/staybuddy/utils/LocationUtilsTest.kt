package com.example.staybuddy.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun testIsPointInPolygon() {
        // A simple square polygon representing Vadodara
        val vadodaraPolygon = listOf(
            Pair(22.42, 73.05), // Top Left
            Pair(22.42, 73.28), // Top Right
            Pair(22.18, 73.28), // Bottom Right
            Pair(22.18, 73.05)  // Bottom Left
        )

        // Point inside the box (e.g., MS University Vadodara coordinates)
        val msUniversity = Pair(22.3106, 73.1812)
        assertTrue(
            "MS University should be inside Vadodara polygon",
            LocationUtils.isPointInPolygon(msUniversity.first, msUniversity.second, vadodaraPolygon)
        )

        // Point strictly outside the box (e.g., somewhere in Mumbai)
        val mumbai = Pair(19.0760, 72.8777)
        assertFalse(
            "Mumbai should be outside Vadodara polygon",
            LocationUtils.isPointInPolygon(mumbai.first, mumbai.second, vadodaraPolygon)
        )
    }
}
