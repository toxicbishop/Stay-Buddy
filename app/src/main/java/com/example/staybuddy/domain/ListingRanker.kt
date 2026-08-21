package com.example.staybuddy.domain

import com.example.staybuddy.data.model.PgListing

/**
 * Pure, deterministic ranking for listing discovery.
 *
 * Promotions are city-scoped and strict: a featured/boosted listing whose
 * scope targets another city is invisible there ("Everywhere" = null scope
 * shows everywhere). Within a city, listings rank by promotion tier
 * (featured ≫ boosted ≫ premium) and then by quality (rating, reviews,
 * freshness).
 */
object ListingRanker {

    private fun PgListing.isFeatureActive(now: Long): Boolean =
        featuredUntil != null && featuredUntil.toDate().time > now

    private fun PgListing.isBoostActive(now: Long): Boolean =
        boostExpiresAt != null && boostExpiresAt.toDate().time > now

    /** Bidirectional substring match so "Delhi" ↔ "New Delhi" both resolve. */
    private fun sameCity(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return false
        return a.contains(b, ignoreCase = true) || b.contains(a, ignoreCase = true)
    }

    /**
     * Strict city visibility. An *active* promotion scoped to a specific city
     * hides the listing from every other city. Null scope = "Everywhere".
     * Area scope never hides a listing.
     */
    fun isVisibleInCity(
        listing: PgListing,
        city: String,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        if (listing.isFeatureActive(now) && listing.featuredCity != null &&
            !sameCity(listing.featuredCity, city)
        ) return false
        if (listing.isBoostActive(now) && listing.boostedCity != null &&
            !sameCity(listing.boostedCity, city)
        ) return false
        return true
    }

    /**
     * Deterministic rank score for [city]/[area]. Higher wins. Tiers:
     *   featured:  1,000,000 + priority·100,000 (+500,000 if area matches)
     *   boosted:     100,000 + priority·10,000 (+50,000 if area matches)
     *   premium:      50,000
     *   quality:  rating·100 + reviewCount·10 + freshness (≤48h, in hours)
     */
    fun rankScore(
        listing: PgListing,
        city: String,
        area: String? = null,
        now: Long = System.currentTimeMillis()
    ): Long {
        var score = 0L

        val areaMatch = area != null && listing.area.equals(area, ignoreCase = true)

        if (listing.isFeatureActive(now)) {
            score += 1_000_000L + (listing.featuredPriority ?: 2) * 100_000L
            if (areaMatch) score += 500_000L
        }
        if (listing.isBoostActive(now)) {
            score += 100_000L + (listing.boostPriority ?: 2) * 10_000L
            if (areaMatch) score += 50_000L
        }
        if (listing.isPremium) score += 50_000L

        // Quality signals
        score += (listing.rating * 100).toLong()
        score += listing.reviewCount * 10L

        // Freshness: newest listings get up to 48 points, decaying hourly
        val ageHours = ((now - listing.createdAt).coerceAtLeast(0L)) / 3_600_000L
        if (ageHours <= 48L) score += 48L - ageHours

        return score
    }
}
