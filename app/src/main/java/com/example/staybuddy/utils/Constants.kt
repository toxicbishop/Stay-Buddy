package com.example.staybuddy.utils

object Constants {
    // Mapbox
    val MAPBOX_ACCESS_TOKEN: String
        get() = com.example.staybuddy.BuildConfig.MAPBOX_ACCESS_TOKEN


    // Google Sign-In
    // TODO: Replace with your actual Web Client ID from Firebase Console / Google Cloud Console
    const val WEB_CLIENT_ID = "611800697150-lcqo34l8r50v1brm6dsnf7bkrjb5pslr.apps.googleusercontent.com"

    // Firestore Collections
    const val USERS_COLLECTION = "users"
    const val PG_LISTINGS_COLLECTION = "pg_listings"
    const val ROOMMATE_POSTS_COLLECTION = "roommate_posts"
    const val CHATS_COLLECTION = "chats"
    const val MESSAGES_COLLECTION = "messages"
    const val FAVORITES_COLLECTION = "favorites"
    const val INQUIRIES_COLLECTION = "inquiries"
    const val REPORTS_COLLECTION = "reports"

    // Location Data (Firestore reference documents — publicly readable)
    const val LOCATION_DATA_COLLECTION = "location_data"
    const val CITIES_DOCUMENT = "cities"
    const val UNIVERSITIES_DOCUMENT = "universities"
    const val GEOFENCES_DOCUMENT = "geofences"

    // Saved Searches (subcollection under each user)
    const val SAVED_SEARCHES_SUBCOLLECTION = "saved_searches"

    // City-Scale: Promotions & Insights
    const val PROMOTIONS_COLLECTION = "promotions"
    const val INSIGHTS_COLLECTION = "insights"

    // Firebase Storage Paths
    const val PG_IMAGES_PATH = "pg_images"
    const val PROFILE_IMAGES_PATH = "profile_images"

    // User Roles
    const val ROLE_STUDENT = "student"
    const val ROLE_OWNER = "owner"

    // Room Types
    const val ROOM_SINGLE = "Single"
    const val ROOM_DOUBLE = "Double"
    const val ROOM_TRIPLE = "Triple"
    const val ROOM_DORM = "Dorm"

    // Gender
    const val GENDER_MALE = "Male"
    const val GENDER_FEMALE = "Female"
    const val GENDER_ANY = "Any"

    // DataStore Keys
    const val DATASTORE_NAME = "staybuddy_preferences"
    const val KEY_THEME_PREFERENCE = "theme_preference"
    const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    const val KEY_USER_ROLE = "user_role"
    const val KEY_SELECTED_CITY = "selected_city"
    const val KEY_SELECTED_UNIVERSITY = "selected_university"
    const val KEY_RECENT_LOCATIONS = "recent_locations"
    const val KEY_LATITUDE = "latitude"
    const val KEY_LONGITUDE = "longitude"
    const val KEY_SELECTED_LOCATION_NAME = "selected_location_name"
    const val KEY_ANCHOR_TYPE = "anchor_type"
    const val KEY_SEARCH_HISTORY = "search_history"
    const val KEY_FILTER_PRICE_MIN = "filter_price_min"
    const val KEY_FILTER_PRICE_MAX = "filter_price_max"
    const val KEY_FILTER_ROOM_TYPES = "filter_room_types"
    const val KEY_FILTER_GENDER = "filter_gender"
    const val KEY_FILTER_AMENITIES = "filter_amenities"
    const val KEY_SORT_OPTION = "sort_option"

    // Amenities
    val AMENITIES_LIST = listOf(
        "WiFi", "AC", "Laundry", "Food", "Parking",
        "TV", "Geyser", "Power Backup", "Security", "CCTV"
    )
}
