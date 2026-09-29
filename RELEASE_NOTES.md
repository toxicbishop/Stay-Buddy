# StayBuddy v0.1.0 — Initial Public Beta

Welcome to the initial public beta release of **StayBuddy**! 🏠✨

StayBuddy is an all-in-one Android app designed to simplify PG (paying-guest) accommodation discovery and roommate matching for students and young working professionals in India.

---

### 🚀 Highlights & Features

#### 🔍 PG Discovery & Exploration
- **Browse & Filter Listings:** Search by city, locality, rent range (₹ INR), room sharing (single/double/triple), gender preference, and amenities (Wi-Fi, AC, Attached Bath, Meals).
- **Rich Listing Details:** Detailed amenity breakdown, high-resolution photo carousels, owner contact info, and pricing breakdown.
- **OpenStreetMap Integration:** Seamless map exploration powered by `osmdroid` and `OSMBonusPack` — discover nearby bus stops, metros, and universities without relying on proprietary Google Maps keys.

#### 🤝 Roommate Compatibility Matching
- **Lifestyle & Habit Quiz:** Answer questions on study schedules, sleep habits, cleanliness, and dietary preferences.
- **Compatibility Scoring:** Automatic match calculation to connect you with like-minded roommates.
- **Roommate Posts:** Browse roommate vacancy posts from other students and young professionals.

#### 💬 Real-Time Chat & Inquiries
- **Direct Messaging:** Instant in-app messaging between tenants, prospective roommates, and property owners powered by Cloud Firestore.
- **Inquiry Pipeline:** Send structured booking inquiries directly from listing pages.

#### 🏢 Property Owner Studio
- **Owner Dashboard:** Overview of listed properties, occupancy, and incoming inquiries.
- **Listing Creator:** Easily list new properties with location geocoding via Nominatim and cloud photo uploads via Cloudinary.
- **Tenant Management:** Respond to inquiries and communicate with prospective tenants.

#### 🎨 Design System — "Hearth"
- Built from the ground up with **Jetpack Compose** and **Material 3**.
- Homely palette featuring deep evergreen brand tones (`#16705B`), clay prices (`#9C4A20`), and warm paper surfaces (`#FAF7F1`).
- Full support for both **Light and Dark themes**.

---

### 🛡️ Security & Performance Enhancements
- **Clean Dependency Tree:** 100% resolved all 33 open security advisories across all subprojects (`functions`, `stay-buddy-backend`, `stay-buddy-worker`, and `firestore-seed`).
- **Standardized Build Config:** Configured `gradle.properties` with AndroidX and optimized Gradle heap flags.
- **Offline Caching:** Favorites and recent search history persisted locally via Android Room.

---

### 📱 Downloads & Installation

| Asset | Format | Size | Description |
| :--- | :--- | :--- | :--- |
| **`StayBuddy-debug.apk`** | `.apk` | ~87 MB | Android debug package for testing & side-loading |

#### How to Install:
1. Download **`StayBuddy-debug.apk`** below.
2. Open the downloaded file on your Android device (Android 8.0 / API 26 or higher).
3. If prompted, allow **"Install unknown apps"** in your browser or device settings.
4. Open StayBuddy and start discovering PGs!

---

### 🛠️ Developer & Technical Specifications
- **Kotlin:** 2.1.0
- **Android Gradle Plugin:** 8.9.1
- **Gradle:** 8.11.1
- **Target SDK:** 36 (Android 16) / **Min SDK:** 26 (Android 8.0)
- **Architecture:** MVVM + StateFlow + Repository Pattern + Dagger Hilt
- **License:** GNU General Public License v3.0
