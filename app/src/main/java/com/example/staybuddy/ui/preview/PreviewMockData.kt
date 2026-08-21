package com.example.staybuddy.ui.preview

import com.example.staybuddy.data.model.*

/**
 * Centralized mock data for Compose @Preview functions.
 * Provides realistic sample data for all screen previews.
 */
object PreviewMockData {

    val sampleUser = User(
        userId = "user_001",
        name = "Aasav Chauhan",
        email = "aasav@example.com",
        phone = "+91 98765 43210",
        role = "student",
        gender = "Male",
        city = "Vadodara",
        college = "PDEU",
        profileImage = "",
        sleepSchedule = "night_owl",
        cleanlinessLevel = "average",
        foodPreference = "non_veg",
        smokingDrinking = "no",
        guestsVisitors = "day_only"
    )

    val sampleOwner = User(
        userId = "owner_001",
        name = "Rahul Sharma",
        email = "rahul.sharma@example.com",
        phone = "+91 99887 76654",
        role = "owner",
        gender = "Male",
        city = "Vadodara",
        isOwnerVerified = true
    )

    val sampleListings = listOf(
        PgListing(
            listingId = "listing_001",
            ownerId = "owner_001",
            title = "Sunrise PG for Boys",
            description = "Well-furnished PG with AC rooms, high-speed WiFi, and home-cooked meals. Located near PDEU campus.",
            city = "Vadodara",
            area = "Raysan",
            latitude = 23.1815,
            longitude = 72.6277,
            price = 8500,
            deposit = 10000,
            roomType = "Double Sharing",
            genderAllowed = "Boys",
            amenities = listOf("WiFi", "AC", "Meals", "Laundry", "Power Backup"),
            images = emptyList(),
            availableBeds = 3,
            isActive = true,
            rating = 4.5f,
            ownerName = "Rahul Sharma",
            isVerified = true,
            viewCount = 142
        ),
        PgListing(
            listingId = "listing_002",
            ownerId = "owner_002",
            title = "Green Valley Girls Hostel",
            description = "Premium hostel with garden view, CCTV security, and attached bathroom.",
            city = "Vadodara",
            area = "Gandhinagar Highway",
            latitude = 23.1900,
            longitude = 72.6300,
            price = 12000,
            deposit = 15000,
            roomType = "Single Room",
            genderAllowed = "Girls",
            amenities = listOf("WiFi", "AC", "Security", "Gym", "Parking"),
            images = emptyList(),
            availableBeds = 1,
            isActive = true,
            rating = 4.8f,
            ownerName = "Priya Patel",
            isVerified = true,
            isPremium = true,
            viewCount = 310
        ),
        PgListing(
            listingId = "listing_003",
            ownerId = "owner_003",
            title = "Budget Boys PG – Alkapuri",
            description = "Affordable PG near MS University. Basic amenities included.",
            city = "Vadodara",
            area = "Alkapuri",
            latitude = 22.3100,
            longitude = 73.1800,
            price = 5000,
            deposit = 5000,
            roomType = "Triple Sharing",
            genderAllowed = "Boys",
            amenities = listOf("WiFi", "Meals"),
            images = emptyList(),
            availableBeds = 5,
            isActive = true,
            rating = 3.8f,
            ownerName = "Amit Desai",
            viewCount = 78
        )
    )

    val sampleRoommatePosts = listOf(
        RoommatePost(
            postId = "rp_001",
            userId = "user_002",
            city = "Vadodara",
            location = "Raysan",
            priceShare = 6000,
            availableBeds = 1,
            totalBeds = 3,
            roomType = "Shared",
            postType = RoommatePostType.OFFER,
            description = "Looking for a chill roommate who respects quiet hours after 11 PM. Room is fully furnished with AC.",
            userName = "Karan Mehta",
            address = "Near PDEU Gate, Raysan",
            preferences = mapOf("Diet" to "Vegetarian Only", "Smoking" to "Non-Smoker"),
            latitude = 23.18,
            longitude = 72.63
        ),
        RoommatePost(
            postId = "rp_002",
            userId = "user_003",
            city = "Vadodara",
            location = "Alkapuri",
            priceShare = 8000,
            roomType = "Single",
            postType = RoommatePostType.SEEK,
            description = "Working professional looking for a clean, well-maintained flat near Alkapuri.",
            userName = "Sneha Joshi"
        )
    )

    val sampleChatRooms = listOf(
        ChatRoom(
            roomId = "chat_001",
            participants = listOf("user_001", "owner_001"),
            listingId = "listing_001",
            lastMessage = "Is the room still available?",
            lastMessageTime = System.currentTimeMillis() - 3600000,
            unreadCount = mapOf("user_001" to 2)
        ),
        ChatRoom(
            roomId = "chat_002",
            participants = listOf("user_001", "user_002"),
            roommatePostId = "rp_001",
            lastMessage = "Sure, let's meet tomorrow!",
            lastMessageTime = System.currentTimeMillis() - 86400000,
            unreadCount = emptyMap()
        )
    )

    val sampleMessages = listOf(
        Message(
            messageId = "msg_001",
            roomId = "chat_001",
            senderId = "user_001",
            text = "Hi! I'm interested in the Sunrise PG listing.",
            timestamp = System.currentTimeMillis() - 7200000
        ),
        Message(
            messageId = "msg_002",
            roomId = "chat_001",
            senderId = "owner_001",
            text = "Hello! Yes, we have 3 beds available. When would you like to visit?",
            timestamp = System.currentTimeMillis() - 3600000
        ),
        Message(
            messageId = "msg_003",
            roomId = "chat_001",
            senderId = "user_001",
            text = "Is the room still available?",
            timestamp = System.currentTimeMillis() - 1800000
        )
    )

    val sampleInquiries = listOf(
        Inquiry(
            inquiryId = "inq_001",
            listingId = "listing_001",
            userId = "user_001",
            hostId = "owner_001",
            moveInDate = System.currentTimeMillis() + 604800000,
            roomType = "Double Sharing",
            status = "PENDING",
            message = "I'd like to move in next week. Is early morning access possible?"
        ),
        Inquiry(
            inquiryId = "inq_002",
            listingId = "listing_001",
            userId = "user_003",
            hostId = "owner_001",
            moveInDate = System.currentTimeMillis() + 1209600000,
            roomType = "Double Sharing",
            status = "ACCEPTED",
            message = ""
        )
    )
}
