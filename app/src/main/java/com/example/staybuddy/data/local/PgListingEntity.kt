package com.example.staybuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.ColumnInfo
import com.example.staybuddy.data.model.PgListing
import com.example.staybuddy.data.model.LifestylePreferences

@Entity(tableName = "pg_listings")
data class PgListingEntity(
    @PrimaryKey val listingId: String,
    val ownerId: String,
    val title: String,
    val description: String,
    val city: String,
    val area: String,
    val latitude: Double,
    val longitude: Double,
    val price: Int,
    val deposit: Int,
    val roomType: String,
    val genderAllowed: String,
    val amenities: List<String>,
    val images: List<String>,
    val availableBeds: Int,
    val isActive: Boolean,
    val rating: Float,
    val ownerName: String,
    val ownerProfileImage: String,
    val ownerPhone: String,
    val createdAt: Long,
    val isVerified: Boolean,
    val reportCount: Int,
    val reviewCount: Int,
    val isPremium: Boolean,
    val boostExpiresAt: Long?,
    val featuredUntil: Long?,
    val featuredCity: String?,
    val featuredArea: String?,
    val boostedCity: String?,
    val boostedArea: String?,
    @ColumnInfo(defaultValue = "2") val featuredPriority: Int,
    @ColumnInfo(defaultValue = "2") val boostPriority: Int,
    val viewCount: Int,
    @Embedded(prefix = "lifestyle_") val lifestylePreferences: LifestylePreferences = LifestylePreferences()
)

fun PgListingEntity.toDomainModel(): PgListing {
    return PgListing(
        listingId = listingId,
        ownerId = ownerId,
        title = title,
        description = description,
        city = city,
        area = area,
        latitude = latitude,
        longitude = longitude,
        price = price,
        deposit = deposit,
        roomType = roomType,
        genderAllowed = genderAllowed,
        amenities = amenities,
        images = images,
        availableBeds = availableBeds,
        isActive = isActive,
        rating = rating,
        ownerName = ownerName,
        ownerProfileImage = ownerProfileImage,
        ownerPhone = ownerPhone,
        createdAt = createdAt,
        isVerified = isVerified,
        reportCount = reportCount,
        reviewCount = reviewCount,
        isPremium = isPremium,
        boostExpiresAt = boostExpiresAt?.let { com.google.firebase.Timestamp(it / 1000, ((it % 1000) * 1_000_000).toInt()) },
        featuredUntil = featuredUntil?.let { com.google.firebase.Timestamp(it / 1000, ((it % 1000) * 1_000_000).toInt()) },
        featuredCity = featuredCity,
        featuredArea = featuredArea,
        boostedCity = boostedCity,
        boostedArea = boostedArea,
        featuredPriority = featuredPriority,
        boostPriority = boostPriority,
        viewCount = viewCount,
        lifestylePreferences = lifestylePreferences
    )
}

fun PgListing.toEntity(): PgListingEntity {
    return PgListingEntity(
        listingId = listingId,
        ownerId = ownerId,
        title = title,
        description = description,
        city = city,
        area = area,
        latitude = latitude,
        longitude = longitude,
        price = price,
        deposit = deposit,
        roomType = roomType,
        genderAllowed = genderAllowed,
        amenities = amenities,
        images = images,
        availableBeds = availableBeds,
        isActive = isActive,
        rating = rating,
        ownerName = ownerName,
        ownerProfileImage = ownerProfileImage,
        ownerPhone = ownerPhone,
        createdAt = createdAt,
        isVerified = isVerified,
        reportCount = reportCount,
        reviewCount = reviewCount,
        isPremium = isPremium,
        boostExpiresAt = boostExpiresAt?.toDate()?.time,
        featuredUntil = featuredUntil?.toDate()?.time,
        featuredCity = featuredCity,
        featuredArea = featuredArea,
        boostedCity = boostedCity,
        boostedArea = boostedArea,
        featuredPriority = featuredPriority ?: 2,
        boostPriority = boostPriority ?: 2,
        viewCount = viewCount,
        lifestylePreferences = lifestylePreferences
    )
}
