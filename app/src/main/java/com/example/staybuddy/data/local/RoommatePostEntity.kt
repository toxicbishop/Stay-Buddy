package com.example.staybuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.staybuddy.data.model.RoommatePost
import com.example.staybuddy.data.model.RoommatePostType

@Entity(tableName = "roommate_posts")
data class RoommatePostEntity(
    @PrimaryKey
    val postId: String,
    val userId: String,
    val city: String,
    val location: String,
    val priceShare: Int,
    val availableBeds: Int,
    val totalBeds: Int,
    val roomType: String,
    val postType: String,
    val description: String,
    val preferences: Map<String, String>,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val userName: String,
    val userProfileImage: String,
    val userPhone: String,
    val isActive: Boolean,
    val createdAt: Long,
    val genderPreference: String,
    val sleepSchedule: String,
    val cleanlinessLevel: String,
    val foodPreference: String,
    val smokingDrinking: String,
    val guestsVisitors: String,
    val noiseLevel: String,
    val petFriendly: String,
    val alcoholHabit: String,
    val workSchedule: String,
    val hobbies: String
)

fun RoommatePostEntity.toDomainModel(): RoommatePost {
    return RoommatePost(
        postId = postId,
        userId = userId,
        city = city,
        location = location,
        priceShare = priceShare,
        availableBeds = availableBeds,
        totalBeds = totalBeds,
        roomType = roomType,
        postType = RoommatePostType.valueOf(postType),
        description = description,
        preferences = preferences,
        address = address,
        latitude = latitude,
        longitude = longitude,
        userName = userName,
        userProfileImage = userProfileImage,
        userPhone = userPhone,
        isActive = isActive,
        createdAt = createdAt,
        genderPreference = genderPreference,
        sleepSchedule = sleepSchedule,
        cleanlinessLevel = cleanlinessLevel,
        foodPreference = foodPreference,
        smokingDrinking = smokingDrinking,
        guestsVisitors = guestsVisitors,
        noiseLevel = noiseLevel,
        petFriendly = petFriendly,
        alcoholHabit = alcoholHabit,
        workSchedule = workSchedule,
        hobbies = hobbies
    )
}

fun RoommatePost.toEntity(): RoommatePostEntity {
    return RoommatePostEntity(
        postId = postId,
        userId = userId,
        city = city,
        location = location,
        priceShare = priceShare,
        availableBeds = availableBeds,
        totalBeds = totalBeds,
        roomType = roomType,
        postType = postType.name,
        description = description,
        preferences = preferences,
        address = address,
        latitude = latitude,
        longitude = longitude,
        userName = userName,
        userProfileImage = userProfileImage,
        userPhone = userPhone,
        isActive = isActive,
        createdAt = createdAt,
        genderPreference = genderPreference,
        sleepSchedule = sleepSchedule,
        cleanlinessLevel = cleanlinessLevel,
        foodPreference = foodPreference,
        smokingDrinking = smokingDrinking,
        guestsVisitors = guestsVisitors,
        noiseLevel = noiseLevel,
        petFriendly = petFriendly,
        alcoholHabit = alcoholHabit,
        workSchedule = workSchedule,
        hobbies = hobbies
    )
}
