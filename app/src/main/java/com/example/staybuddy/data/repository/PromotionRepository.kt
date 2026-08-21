package com.example.staybuddy.data.repository

import android.util.Log
import com.example.staybuddy.data.model.CityInsight
import com.example.staybuddy.data.model.Promotion
import com.example.staybuddy.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromotionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "PromotionRepo"
    }

    /**
     * Fetch active promotions for a city (or "all" cities).
     * Returns the highest-priority single promotion for display.
     * Caches in memory for the session to avoid repeated reads.
     */
    @Volatile
    private var cachedPromotions: List<Promotion> = emptyList()

    suspend fun getActivePromotion(city: String): Promotion? {
        try {
            val now = com.google.firebase.Timestamp.now()

            val snapshot = firestore.collection("promotions")
                .whereEqualTo("isActive", true)
                .orderBy("priority", Query.Direction.DESCENDING)
                .get()
                .await()

            val promotions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Promotion::class.java)
            }.filter { promo ->
                // City match: "all" covers every city, specific city name must match
                val cityMatch = promo.city == "all" || promo.city.equals(city, ignoreCase = true)
                // Date range check
                val afterStart = promo.startDate == null || promo.startDate!! <= now
                val beforeEnd = promo.endDate == null || promo.endDate!! >= now
                cityMatch && afterStart && beforeEnd
            }

            cachedPromotions = promotions
            return promotions.firstOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch promotions", e)
            return cachedPromotions.firstOrNull()
        }
    }

    /**
     * Fetch city market insights — pre-computed stats.
     */
    @Volatile
    private var cachedInsights: Map<String, CityInsight> = emptyMap()

    suspend fun getCityInsight(city: String): CityInsight? {
        // Return from cache if available
        cachedInsights[city.lowercase()]?.let { return it }

        return try {
            val doc = firestore.collection("insights")
                .document(city.lowercase())
                .get()
                .await()

            if (doc.exists()) {
                val insight = doc.toObject(CityInsight::class.java)
                if (insight != null) {
                    cachedInsights = cachedInsights + (city.lowercase() to insight)
                }
                insight
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch insight for $city", e)
            cachedInsights[city.lowercase()]
        }
    }
}
