package com.example.staybuddy.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtils {
    /**
     * Calculates the straight-line distance between two geographic coordinates using the Haversine formula.
     * @return Distance in kilometers.
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of the earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Determines if a point (lat, lon) is inside a given polygon using the Ray-Casting algorithm.
     * @param lat The latitude of the point to check.
     * @param lon The longitude of the point to check.
     * @param polygon A list of (latitude, longitude) pairs forming the closed shape.
     * @return true if the point is strictly inside the polygon, false otherwise.
     */
    fun isPointInPolygon(lat: Double, lon: Double, polygon: List<Pair<Double, Double>>): Boolean {
        var isInside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val (latI, lonI) = polygon[i]
            val (latJ, lonJ) = polygon[j]

            val intersect = ((lonI > lon) != (lonJ > lon)) &&
                    (lat < (latJ - latI) * (lon - lonI) / (lonJ - lonI) + latI)
            
            if (intersect) {
                isInside = !isInside
            }
            j = i
        }
        return isInside
    }
}
