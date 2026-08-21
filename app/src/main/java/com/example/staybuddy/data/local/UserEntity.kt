package com.example.staybuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.staybuddy.data.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val gender: String,
    val city: String,
    val college: String,
    val profileImage: String,
    val bio: String,
    val fcmToken: String,
    val createdAt: Long,
    val isPremiumUser: Boolean,
    val subscriptionTier: String,
    val isOwnerVerified: Boolean,
    val sleepSchedule: String,
    val cleanlinessLevel: String,
    val foodPreference: String,
    val smokingDrinking: String,
    val guestsVisitors: String,
    val noiseLevel: String,
    val petFriendly: String,
    val alcoholHabit: String,
    val workSchedule: String,
    val hobbies: String,
    val blockedUsers: List<String>
)

fun UserEntity.toDomainModel(): User {
    return User(
        userId = userId,
        name = name,
        email = email,
        phone = phone,
        role = role,
        gender = gender,
        city = city,
        college = college,
        profileImage = profileImage,
        bio = bio,
        fcmToken = fcmToken,
        createdAt = createdAt,
        isPremiumUser = isPremiumUser,
        subscriptionTier = subscriptionTier,
        isOwnerVerified = isOwnerVerified,
        sleepSchedule = sleepSchedule,
        cleanlinessLevel = cleanlinessLevel,
        foodPreference = foodPreference,
        smokingDrinking = smokingDrinking,
        guestsVisitors = guestsVisitors,
        noiseLevel = noiseLevel,
        petFriendly = petFriendly,
        alcoholHabit = alcoholHabit,
        workSchedule = workSchedule,
        hobbies = hobbies,
        blockedUsers = blockedUsers
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        userId = userId,
        name = name,
        email = email,
        phone = phone,
        role = role,
        gender = gender,
        city = city,
        college = college,
        profileImage = profileImage,
        bio = bio,
        fcmToken = fcmToken,
        createdAt = createdAt,
        isPremiumUser = isPremiumUser,
        subscriptionTier = subscriptionTier,
        isOwnerVerified = isOwnerVerified,
        sleepSchedule = sleepSchedule,
        cleanlinessLevel = cleanlinessLevel,
        foodPreference = foodPreference,
        smokingDrinking = smokingDrinking,
        guestsVisitors = guestsVisitors,
        noiseLevel = noiseLevel,
        petFriendly = petFriendly,
        alcoholHabit = alcoholHabit,
        workSchedule = workSchedule,
        hobbies = hobbies,
        blockedUsers = blockedUsers
    )
}
