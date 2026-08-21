package com.example.staybuddy.data.model

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "", // "student" or "owner"
    val gender: String = "",
    val city: String = "",
    val college: String = "",
    val profileImage: String = "",
    val bio: String = "", // Add Bio
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // Premium & verification hooks
    val isPremiumUser: Boolean = false,
    val subscriptionTier: String = "free",
    val isOwnerVerified: Boolean = false,
    val verificationStatus: String = "UNVERIFIED", // "UNVERIFIED", "PENDING", "VERIFIED", "REJECTED"
    val verificationDocUrl: String = "",

    // Lifestyle Preferences (USP Sprint 4)
    val sleepSchedule: String = "", // "early_bird", "night_owl", "flexible"
    val cleanlinessLevel: String = "", // "obsessive", "average", "relaxed"
    val foodPreference: String = "", // "veg", "non_veg", "flexible"
    val smokingDrinking: String = "", // "no", "yes", "outside"
    val guestsVisitors: String = "", // "no_guests", "day_only", "anytime"
    val noiseLevel: String = "",
    val petFriendly: String = "",
    val alcoholHabit: String = "",
    val workSchedule: String = "",
    val hobbies: String = "",
    val blockedUsers: List<String> = emptyList(),

    // Moderation (set by admin panel)
    val isActive: Boolean = true,
    val bannedAt: Long? = null,
    val banReason: String? = null
) {
    /** Derived map used by roommate compatibility scoring. */
    val quizResults: Map<String, Int>
        get() {
            val results = mutableMapOf<String, Int>()
            if (sleepSchedule.isNotBlank()) results["sleepSchedule"] = answerToScore(sleepSchedule)
            if (cleanlinessLevel.isNotBlank()) results["cleanlinessLevel"] = answerToScore(cleanlinessLevel)
            if (foodPreference.isNotBlank()) results["foodPreference"] = answerToScore(foodPreference)
            if (smokingDrinking.isNotBlank()) results["smokingDrinking"] = answerToScore(smokingDrinking)
            if (guestsVisitors.isNotBlank()) results["guestsVisitors"] = answerToScore(guestsVisitors)
            if (noiseLevel.isNotBlank()) results["noiseLevel"] = answerToScore(noiseLevel)
            if (petFriendly.isNotBlank()) results["petFriendly"] = answerToScore(petFriendly)
            if (alcoholHabit.isNotBlank()) results["alcoholHabit"] = answerToScore(alcoholHabit)
            if (workSchedule.isNotBlank()) results["workSchedule"] = answerToScore(workSchedule)
            if (hobbies.isNotBlank()) results["hobbies"] = answerToScore(hobbies)
            return results
        }

    private fun answerToScore(answer: String): Int = when (answer) {
        // Sleep Schedule
        "Early Bird" -> 1; "Night Owl" -> 4; "Flexible" -> 2; "Shift Worker" -> 3
        // Cleanliness
        "Neat Freak" -> 1; "Moderately Clean" -> 2; "Lived-in" -> 3; "Messy but Organized" -> 4
        // Diet/Food
        "Vegetarian" -> 1; "Non-Vegetarian" -> 3; "Vegan" -> 2; "No Preference" -> 2
        // Smoking
        "Non-Smoker" -> 1; "Occasional Smoker" -> 2; "Outside Only" -> 3; "Regular Smoker" -> 4
        // Guests
        "Never" -> 1; "Occasionally" -> 2; "Frequently" -> 3; "Always a Party" -> 4
        // Noise Level
        "Pin-drop Silence" -> 1; "Library Quiet" -> 2; "Background Music/TV" -> 3; "Lively & Loud" -> 4
        // Pets
        "Love Pets" -> 1; "Comfortable with Small Pets" -> 2; "No Pets Please" -> 3; "Allergic" -> 4
        // Alcohol
        "Non-Drinker" -> 1; "Occasional/Social" -> 2; "Outside Only (Alcohol)" -> 3; "Regular" -> 4
        // Work Schedule
        "9-5 Office" -> 1; "Work from Home" -> 2; "Student Schedule" -> 3; "Irregular Hours" -> 4
        // Hobbies (categorized abstractly for matching)
        "Gaming/Tech" -> 1; "Fitness/Sports" -> 2; "Reading/Art" -> 3; "Outdoors/Travel" -> 4
        
        // Legacy fallback
        "early_bird" -> 1; "night_owl" -> 4; "flexible" -> 2
        "obsessive" -> 1; "average" -> 2; "relaxed" -> 3
        "veg" -> 1; "non_veg" -> 3
        "no" -> 1; "outside" -> 3; "yes" -> 4
        "no_guests" -> 1; "day_only" -> 2; "anytime" -> 4
        else -> 0
    }
}
