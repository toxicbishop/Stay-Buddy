package com.example.staybuddy.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.staybuddy.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val themePreference: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[stringPreferencesKey(Constants.KEY_THEME_PREFERENCE)] ?: "SYSTEM"
        }

    suspend fun setThemePreference(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Constants.KEY_THEME_PREFERENCE)] = theme
        }
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[booleanPreferencesKey(Constants.KEY_ONBOARDING_COMPLETED)] ?: false
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(Constants.KEY_ONBOARDING_COMPLETED)] = completed
        }
    }

    val userRole: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[stringPreferencesKey(Constants.KEY_USER_ROLE)] ?: Constants.ROLE_STUDENT
        }

    suspend fun setUserRole(role: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Constants.KEY_USER_ROLE)] = role
        }
    }

    val selectedCity: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[stringPreferencesKey(Constants.KEY_SELECTED_CITY)] }

    suspend fun setSelectedCity(city: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Constants.KEY_SELECTED_CITY)] = city
        }
    }

    val selectedUniversity: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[stringPreferencesKey(Constants.KEY_SELECTED_UNIVERSITY)] }

    suspend fun setSelectedUniversity(university: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Constants.KEY_SELECTED_UNIVERSITY)] = university
        }
    }

    val targetAnchor: Flow<com.example.staybuddy.domain.model.TargetAnchor?> = context.dataStore.data
        .map { preferences ->
            val lat = preferences[doublePreferencesKey(Constants.KEY_LATITUDE)]
            val lon = preferences[doublePreferencesKey(Constants.KEY_LONGITUDE)]
            val name = preferences[stringPreferencesKey(Constants.KEY_SELECTED_LOCATION_NAME)] ?: "Vadodara"
            val typeStr = preferences[stringPreferencesKey(Constants.KEY_ANCHOR_TYPE)] ?: com.example.staybuddy.domain.model.AnchorType.CITY.name
            
            if (lat != null && lon != null) {
                com.example.staybuddy.domain.model.TargetAnchor(
                    type = com.example.staybuddy.domain.model.AnchorType.valueOf(typeStr),
                    name = name,
                    lat = lat,
                    lon = lon
                )
            } else {
                null
            }
        }

    suspend fun setTargetAnchor(anchor: com.example.staybuddy.domain.model.TargetAnchor) {
        context.dataStore.edit { preferences ->
            preferences[doublePreferencesKey(Constants.KEY_LATITUDE)] = anchor.lat
            preferences[doublePreferencesKey(Constants.KEY_LONGITUDE)] = anchor.lon
            preferences[stringPreferencesKey(Constants.KEY_SELECTED_LOCATION_NAME)] = anchor.name
            preferences[stringPreferencesKey(Constants.KEY_ANCHOR_TYPE)] = anchor.type.name
        }
        addRecentLocation(anchor)
    }

    val recentLocations: Flow<List<com.example.staybuddy.domain.model.TargetAnchor>> = context.dataStore.data
        .map { preferences ->
            val serialized = preferences[stringPreferencesKey(Constants.KEY_RECENT_LOCATIONS)] ?: ""
            if (serialized.isBlank()) return@map emptyList()
            
            serialized.split(";;").mapNotNull { part ->
                try {
                    val segments = part.split("|")
                    if (segments.size == 4) {
                        com.example.staybuddy.domain.model.TargetAnchor(
                            type = com.example.staybuddy.domain.model.AnchorType.valueOf(segments[0]),
                            name = segments[1],
                            lat = segments[2].toDouble(),
                            lon = segments[3].toDouble()
                        )
                    } else null
                } catch (e: Exception) { null }
            }
        }

    private suspend fun addRecentLocation(anchor: com.example.staybuddy.domain.model.TargetAnchor) {
        context.dataStore.edit { preferences ->
            val serialized = preferences[stringPreferencesKey(Constants.KEY_RECENT_LOCATIONS)] ?: ""
            val currentList = if (serialized.isBlank()) emptyList() else serialized.split(";;")
            
            val newString = "${anchor.type.name}|${anchor.name}|${anchor.lat}|${anchor.lon}"
            
            // Remove duplicates
            var updatedList = currentList.filter { it != newString }.toMutableList()
            updatedList.add(0, newString) // Add to top
            
            // Keep max 3
            if (updatedList.size > 3) {
                updatedList = updatedList.take(3).toMutableList()
            }
            
            preferences[stringPreferencesKey(Constants.KEY_RECENT_LOCATIONS)] = updatedList.joinToString(";;")
        }
    }

    // Search Filters
    val filterPriceRange: Flow<Pair<Float, Float>> = context.dataStore.data
        .map { preferences ->
            val min = preferences[floatPreferencesKey(Constants.KEY_FILTER_PRICE_MIN)] ?: 500f
            val max = preferences[floatPreferencesKey(Constants.KEY_FILTER_PRICE_MAX)] ?: 30000f
            Pair(min, max)
        }

    suspend fun setFilterPriceRange(min: Float, max: Float) {
        context.dataStore.edit { preferences ->
            preferences[floatPreferencesKey(Constants.KEY_FILTER_PRICE_MIN)] = min
            preferences[floatPreferencesKey(Constants.KEY_FILTER_PRICE_MAX)] = max
        }
    }

    val selectedRoomTypes: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[stringSetPreferencesKey(Constants.KEY_FILTER_ROOM_TYPES)] ?: emptySet()
        }

    suspend fun setSelectedRoomTypes(types: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[stringSetPreferencesKey(Constants.KEY_FILTER_ROOM_TYPES)] = types
        }
    }

    val selectedGender: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[stringPreferencesKey(Constants.KEY_FILTER_GENDER)] ?: Constants.GENDER_ANY
        }

    suspend fun setSelectedGender(gender: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Constants.KEY_FILTER_GENDER)] = gender
        }
    }

    val selectedAmenities: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[stringSetPreferencesKey(Constants.KEY_FILTER_AMENITIES)] ?: emptySet()
        }

    suspend fun setSelectedAmenities(amenities: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[stringSetPreferencesKey(Constants.KEY_FILTER_AMENITIES)] = amenities
        }
    }

    val sortOption: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[stringPreferencesKey(Constants.KEY_SORT_OPTION)] ?: "NEWEST"
        }

    suspend fun setSortOption(option: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Constants.KEY_SORT_OPTION)] = option
        }
    }

    fun isVerifiedBannerDismissed(userId: String): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[booleanPreferencesKey("verified_banner_dismissed_$userId")] ?: false
        }

    suspend fun setVerifiedBannerDismissed(userId: String, dismissed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("verified_banner_dismissed_$userId")] = dismissed
        }
    }

    suspend fun clearPreferences() {
        context.dataStore.edit { it.clear() }
    }
}
