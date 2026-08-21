# StayBuddy — Full App Audit Report

> **Date:** 15 July 2026
> **Audit Scope:** Code quality, bugs, UI/UX consistency, build health, accessibility, Firebase infrastructure
> **Method:** Automated + manual analysis across 131 Kotlin files, build config, Firestore rules

---

## Executive Summary

| Category | Critical | High | Medium | Low |
|----------|----------|------|--------|-----|
| Crash / Security Bugs | 4 | 3 | — | — |
| Memory / Listener Leaks | 2 | 2 | — | — |
| UI/UX Consistency | 1 | 3 | 2 | — |
| Build / Lint | — | 2 | 3 | ~23 |
| Accessibility | 1 | — | — | — |
| **Totals** | **8** | **10** | **5** | **23+** |

**Bottom line:** App is feature-complete and functional. No showstopper crashes in the happy path. But several listener leaks will cause memory growth over long sessions, and the hardcoded strings / missing accessibility make the app non-localizable and screen-reader-unfriendly.

---

## Part 1: Crash & Security Bugs

### 1.1 Listing images crash on empty list

- **File:** `ui/screens/listing/ListingDetailScreen.kt`
- **Line:** 1347
- **Code:** `.data(listing.images.first())`
- **Problem:** `first()` throws `NoSuchElementException` if `images` is empty. Corrupted listings, legacy data, or race conditions during edit can produce an empty list.
- **Impact:** Immediate crash when opening listing detail.
- **Fix:** Replace with `.data(listing.images.firstOrNull() ?: return)` or add a null guard.

---

### 1.2 AppUpdater registerReceiver crash (Android 14+)

- **File:** `utils/AppUpdater.kt`
- **Line:** 166
- **Code:** `context.registerReceiver(onDownloadComplete, filter)`
- **Problem:** Android 14+ requires `RECEIVER_EXPORTED` or `RECEIVER_NOT_EXPORTED` flag for non-system broadcasts. Missing flag → `SecurityException`.
- **Impact:** Crash on Android 14+ devices when app update download completes.
- **Fix:** Use `ContextCompat.registerReceiver(context, receiver, filter, RECEIVER_NOT_EXPORTED)`.

---

### 1.3 WorkManager double initialization

- **File:** `AndroidManifest.xml`
- **Line:** 25
- **Problem:** App implements `Configuration.Provider` for on-demand WorkManager init, but the default `WorkManagerInitializer` from `androidx.startup` is still in the manifest.
- **Impact:** Potential conflict or double initialization.
- **Fix:** Add `<provider android:name="androidx.startup.InitializationProvider" tools:node="remove" />` to manifest.

---

### 1.4 POST_NOTIFICATIONS permission not checked

- **File:** `notifications/NotificationHelper.kt`
- **Line:** 66
- **Code:** `NotificationManagerCompat.from(context).notify(...)`
- **Problem:** No runtime check for `POST_NOTIFICATIONS` permission (required Android 13+). If denied → `SecurityException`.
- **Impact:** Crash when trying to show notification after permission denied.
- **Fix:** Wrap in `if (ActivityCompat.checkSelfPermission(...) == PERMISSION_GRANTED)` or try-catch `SecurityException`.

---

### 1.5 CAMERA permission without uses-feature

- **File:** `AndroidManifest.xml`
- **Line:** 8
- **Problem:** `CAMERA` permission declared but no `<uses-feature android:name="android.hardware.camera" android:required="false" />`. Google Play assumes camera is required → filters out Chromebooks.
- **Fix:** Add the `<uses-feature>` tag with `required="false"`.

---

### 1.6 CredentialManager NoCredentialException unhandled

- **File:** `ui/screens/auth/LoginViewModel.kt`
- **Line:** 106
- **Problem:** `credentialManager.getCredential()` can throw `NoCredentialException` when no saved credentials exist. Not caught.
- **Impact:** Crash during Google Sign-In on some devices.
- **Fix:** Wrap in try-catch for `NoCredentialException`.

---

## Part 2: Memory & Listener Leaks

### 2.1 ChatViewModel — Accumulating Firestore listeners

- **File:** `ui/screens/chat/ChatViewModel.kt`
- **Lines:** 114-135
- **Problem:** Inside `loadRoomDetails()`, every time the chat room Flow emits (including typing indicators every ~3s), two new `getUserFlow()` coroutines are launched. These register Firestore snapshot listeners that are never cancelled.
- **Impact:** Dozens of permanent listeners accumulate during a chat session. Memory growth + excessive Firestore reads + billing impact.
- **Fix:** Use `flatMapLatest` or cancel previous collector before launching new one. Or use `snapshotFlow` with proper lifecycle.

---

### 2.2 MainViewModel refreshSession — Listener accumulation

- **File:** `MainViewModel.kt`
- **Lines:** 80-93
- **Problem:** `refreshSession()` launches new `getUnreadChatsCount()` and `getPendingInquiriesCount()` collectors on every call. Called on every navigation to Home/Profile routes. Each call adds permanent Firestore listeners.
- **Impact:** Progressive memory + Firestore read growth with each tab switch.
- **Fix:** Initialize collectors once in `init{}` and use a flag to prevent re-launching.

---

### 2.3 ProfileViewModel — Snapshot listener leak

- **File:** `ui/screens/profile/ProfileViewModel.kt`
- **Line:** 101
- **Problem:** `addSnapshotListener` returns a `ListenerRegistration` that is discarded. Each call to `refreshUserProfile()` registers another permanent listener.
- **Impact:** Each profile refresh leaks one listener. Accumulates over time.
- **Fix:** Store the `ListenerRegistration` and remove it before adding a new one. Or override `onCleared()` to remove all listeners.

---

### 2.4 DirectReplyReceiver + 5 repositories — Uncancelled CoroutineScope

- **Files:** `notifications/DirectReplyReceiver.kt:29`, `AuthRepository.kt:36`, `FavoriteRepository.kt:24`, `InquiryRepository.kt:36`, `ListingRepository.kt:112`, `RoommateRepository.kt:75`
- **Problem:** Raw `CoroutineScope(SupervisorJob() + Dispatchers.IO)` created as field-level val, never cancelled. BroadcastReceivers are short-lived; system may create new instances.
- **Impact:** Coroutine scope growth, prevents GC of referenced objects.
- **Fix:** Use `goAsync().pendingResult` for BroadcastReceiver work, or use `lifecycleScope`. For repositories, use `viewModelScope` or injected scope.

---

## Part 3: Navigation & Data Issues

### 3.1 BackgroundSyncWorker notification clicks navigate nowhere

- **Files:** `worker/BackgroundSyncWorker.kt:69-73`, `MainActivity.kt:32-49`
- **Problem:** Worker sends `putExtra("chatId", chatId)` in the notification intent. But `MainActivity.onCreate()` and `onNewIntent()` never read this extra. Users tapping the notification always land on the default screen.
- **Impact:** Notification deep-linking is broken. Users can't navigate to the relevant chat.
- **Fix:** In `MainActivity.onNewIntent()`, read the intent extras and trigger navigation via the NavController.

---

### 3.2 ListingDetailScreen PriceStrip — Missing INR formatting

- **File:** `ui/screens/listing/ListingDetailScreen.kt`
- **Lines:** 896-902
- **Problem:** Uses `"₹%,d".format(listing.price)` instead of the shared `formatInr()` / `PriceTag` component. Inconsistent with other price displays.
- **Impact:** Minor visual inconsistency. Prices in other screens use Indian comma formatting (₹10,000) while this uses standard formatting.

---

## Part 4: UI/UX Consistency

### 4.1 Hardcoded Colors (8 instances)

| File | Line | Color | Should Be |
|------|------|-------|-----------|
| `LoginScreen.kt` | 394, 401 | `Color(0xFF4285F4)` | Acceptable (Google brand) |
| `ChatScreen.kt` | 744 | `Color(0xFF34B7F1)` | Define in Color.kt as `ReadTickBlue` |
| `ListingDetailScreen.kt` | 699, 700 | `Color(0xFF25D366)` | Acceptable (WhatsApp brand) |
| `SplashScreen.kt` | 103-105 | `Color(0xFF16705B)` etc. | Use `MaterialTheme.colorScheme.primary` |
| `RoommateListScreen.kt` | 699 | `Color(0xFF25D366)` | Acceptable (WhatsApp brand) |
| `CompactMapCard.kt` | 125 | `Color(0xFFFFB300)` | Use `RatingAmber` from Color.kt |

---

### 4.2 Hardcoded Shapes (25+ instances)

The theme defines: extraSmall=8dp, small=12dp, medium=16dp, large=22dp, extraLarge=28dp.

Many screens use custom values: `14.dp`, `20.dp`, `24.dp`, `32.dp`, `40.dp` which don't map to the theme scale.

**Key offenders:**
- `ListingDetailScreen.kt` — 10+ hardcoded shapes
- `SearchScreen.kt` — 6 hardcoded shapes
- `ProfileScreen.kt` — 2 nonstandard shapes

**Impact:** Visual inconsistency across screens. Theme changes won't propagate.

---

### 4.3 Missing Empty States (7 screens)

| Screen | Missing |
|--------|---------|
| MapViewScreen | Loading, Error, Empty |
| NotificationsScreen | Error |
| OwnerDashboardScreen | Error |
| CompatibilityQuizScreen | Error |
| AddListingScreen | Loading shimmer |
| AddRoommatePostScreen | Loading shimmer |
| OwnerInquiriesScreen | Error (snackbar only) |

---

### 4.4 Inconsistent Error Patterns (4 different approaches)

| Pattern | Used In |
|---------|---------|
| Shared `ErrorBanner` | FavoritesScreen, SearchScreen, ListingDetailScreen |
| Custom `Surface` | ProfileScreen, RoommateListScreen |
| `AlertDialog` | AddListingScreen, AddRoommatePostScreen |
| Snackbar only | ChatScreen, OwnerInquiriesScreen |

---

### 4.5 PriceTag Not Used Consistently

| File | Line | Code | Should Use |
|------|------|------|------------|
| `OwnerDashboardScreen.kt` | 349 | `"₹${listing.price}/mo"` | `PriceTag` |
| `ListingDetailScreen.kt` | 896 | `"₹%,d".format(price)` | `PriceTag` |
| `ListingDetailScreen.kt` | 1383 | `"₹${listing.price}"` | `PriceTag` |
| `RoommateListScreen.kt` | 520 | `"₹${post.priceShare}"` | `PriceTag` |

---

## Part 5: Accessibility

### 5.1 Missing contentDescription (~90+ icons)

| Screen | Icons with null contentDescription |
|--------|-------------------------------------|
| ListingDetailScreen | ~25 |
| ProfileScreen | ~18 |
| RoommateListScreen | ~9 |
| ChatListScreen | ~7 |
| HomeScreen | 5 |
| ChatScreen | 5 |
| LoginScreen | 4 |
| NotificationsScreen | 3 |
| SupportTicketScreen | 3 |
| SearchScreen | 3 |
| AddListingScreen | 3 |
| OwnerInquiriesScreen | 3 |
| Others | ~10 |

**Impact:** Screen readers (TalkBack) skip important controls. App fails accessibility audits.

---

### 5.2 Hardcoded Strings (~500+)

- `strings.xml` contains only `app_name`
- Every user-visible string across all screens is hardcoded in Kotlin/Compose
- App cannot be localized without modifying every screen file
- No string resource system in place

---

## Part 6: Build & Dependencies

### 6.1 Obsolete SDK Checks (3 files)

| File | Line | Check |
|------|------|-------|
| `BackgroundSyncWorker.kt` | 92 | `SDK_INT >= O` (minSdk is 26, already O) |
| `NotificationHelper.kt` | 18 | Same |
| `StayBuddyMessagingService.kt` | 238 | Same |

---

### 6.2 Outdated Dependencies (23 catalog entries)

| Dependency | Current | Available |
|------------|---------|-----------|
| Compose BOM | 2024.12.01 | 2026.06.01 |
| Room | 2.6.1 | 2.8.4 |
| Firebase BOM | 33.7.0 | 34.16.0 |
| Hilt | 2.56.2 | 2.59.1 |
| Kotlin | 2.1.0 | 2.3.20 |
| AGP | 8.9.1 | 9.3.0 |
| Navigation Compose | 2.8.5 | 2.9.8 |
| Coroutines | 1.9.0 | 1.10.2 |
| Cloudinary | 2.4.0 | 3.0.2 |
| OkHttp | 4.12.0 | 5.3.2 |
| Credentials | 1.2.2 | 1.6.0 |
| WorkManager | 2.10.0 | 2.11.2 |
| DataStore | 1.1.1 | 1.2.1 |

---

### 6.3 Unused Resources (20 files)

**Colors:** `primary_light`, `primary_dark`, `secondary`, `secondary_light`, `secondary_dark`, `splash_background`, `white`, `black`, `background`

**Drawables:** `ic_launcher_background.xml`, `ic_launcher_foreground_old.xml`, `ic_launcher_foreground_premium.xml`, `ic_map_marker_premium.xml`, `ic_staybuddy_background.xml`, `ic_staybuddy_foreground.png`, `ic_staybuddy_icon.webp`, `ic_staybuddy_logo.xml`, `ic_staybuddy_logo_premium.xml`, `transparent.xml`

---

### 6.4 Deprecation Warnings

| Warning | File | Line |
|---------|------|------|
| `Icons.Filled.ArrowBack` deprecated | ListingDetailScreen.kt | 574 |
| `Icons.Filled.ViewList` deprecated | SearchScreen.kt | 581 |
| `Icons.Filled.Chat` deprecated | OnboardingScreen.kt | 111 |
| `LocalConfiguration.current.screenWidthDp` deprecated | ResponsiveUtils.kt | 15,41,49,57 |
| `Locale(String)` constructor deprecated | SbComponents.kt | 46 |

---

### 6.5 TODO/FIXME

| File | Line | Content |
|------|------|---------|
| `Constants.kt` | 5 | `// TODO: Replace with your actual Web Client ID from Firebase Console` |

---

## Recommended Fix Priority

### P0 — Fix Now (Crash / Security)

| # | Issue | Est. Time |
|---|-------|-----------|
| 1 | ChatViewModel + MainViewModel listener leaks | 1-2 hrs |
| 2 | AppUpdater registerReceiver flag | 15 min |
| 3 | WorkManager double init | 15 min |
| 4 | listing.images.first() null check | 5 min |
| 5 | POST_NOTIFICATIONS permission check | 15 min |

### P1 — Fix This Week

| # | Issue | Est. Time |
|---|-------|-----------|
| 6 | BackgroundSyncWorker notification navigation | 1 hr |
| 7 | ProfileViewModel listener leak | 30 min |
| 8 | LoginViewModel exception handling | 30 min |
| 9 | Camera uses-feature tag | 5 min |

### P2 — Fix This Month

| # | Issue | Est. Time |
|---|-------|-----------|
| 10 | Migrate strings to resources (start with key screens) | 2-3 days |
| 11 | Add missing empty states (7 screens) | 1-2 days |
| 12 | Standardize error display patterns | 1 day |
| 13 | Fix hardcoded shapes to use theme | 1 day |
| 14 | Fix hardcoded colors to use theme/color constants | 1 day |

### P3 — When Time Permits

| # | Issue | Est. Time |
|---|-------|-----------|
| 15 | Update dependencies (major: Compose BOM, Room, Firebase) | 1-2 days |
| 16 | Add content descriptions for accessibility | 1-2 days |
| 17 | Clean unused resources | 1 hr |
| 18 | Remove obsolete SDK checks | 15 min |
| 19 | Fix deprecated icon references | 15 min |

---

## Appendix: File-Level Reference

| File | Issues Found |
|------|-------------|
| `ChatViewModel.kt` | Listener leak (CRITICAL) |
| `MainViewModel.kt` | Listener leak (CRITICAL) |
| `ProfileViewModel.kt` | Listener leak (HIGH) |
| `BackgroundSyncWorker.kt` | Notification navigation broken, obsolete SDK check |
| `StayBuddyMessagingService.kt` | Obsolete SDK check |
| `NotificationHelper.kt` | Missing permission check, obsolete SDK check |
| `AppUpdater.kt` | registerReceiver crash (CRITICAL) |
| `LoginViewModel.kt` | NoCredentialException unhandled |
| `ListingDetailScreen.kt` | images.first() crash, hardcoded shapes, missing PriceTag usage |
| `AndroidManifest.xml` | WorkManager double init, missing uses-feature |
| `Constants.kt` | TODO placeholder WEB_CLIENT_ID |
| `SearchScreen.kt` | Hardcoded shapes, deprecated icon |
| `HomeScreen.kt` | Hardcoded shapes, missing content descriptions |
| `ProfileScreen.kt` | Hardcoded shapes, missing content descriptions |
| `RoommateListScreen.kt` | Missing PriceTag usage, missing empty states |
| `ChatListScreen.kt` | Missing content descriptions |
| `ChatScreen.kt` | Hardcoded color, missing content descriptions |
| `NotificationsScreen.kt` | Missing error state, custom empty state |
| `OwnerDashboardScreen.kt` | Missing PriceTag usage, missing error state |
| `MapViewScreen.kt` | Missing all empty states |
| `CompatibilityQuizScreen.kt` | Missing error state |
| `AddListingScreen.kt` | Missing loading state |
| `AddRoommatePostScreen.kt` | Missing loading state |
| `SplashScreen.kt` | Hardcoded gradient colors |
| `CompactMapCard.kt` | Hardcoded color instead of RatingAmber |
| `ResponsiveUtils.kt` | Deprecated screenWidthDp API |
| `SbComponents.kt` | Deprecated Locale constructor |
| `DirectReplyReceiver.kt` | Uncancelled coroutine scope |
| `AuthRepository.kt` | Uncancelled coroutine scope |
| `FavoriteRepository.kt` | Uncancelled coroutine scope |
| `InquiryRepository.kt` | Uncancelled coroutine scope |
| `ListingRepository.kt` | Uncancelled coroutine scope |
| `RoommateRepository.kt` | Uncancelled coroutine scope |

---

## Part 7: Firebase Infrastructure Audit

### 7.1 Firestore Security Rules

**File:** `firestore.rules`

#### CRITICAL: No Ownership Validation (7+ collections)

All collections use `allow read, write: if request.auth != null` — any authenticated user can read/write ANY document.

| Collection | Risk |
|-----------|------|
| `/users/{userId}` | Any user can edit any profile, read fcmToken, email, phone |
| `/pg_listings/{listingId}` | Any user can edit/delete any listing |
| `/roommate_posts/{postId}` | Any user can edit/delete any post |
| `/chats/{chatId}` | Any user can read any chat room |
| `/favorites/{favoriteId}` | Any user can read/delete any user's favorites |
| `/inquiries/{inquiryId}` | Any user can manipulate any inquiry |
| `/reports/{reportId}` | Any user can read all reports |

**Fix:** Add ownership checks:
```javascript
match /users/{userId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
match /pg_listings/{listingId} {
  allow read: if request.auth != null;
  allow write: if request.auth != null && request.auth.uid == resource.data.ownerId;
}
```

#### CRITICAL: Ticket Counter Unprotected

`/metadata/ticket_counter` — any authenticated user can repeatedly write, enabling abuse.

**Fix:** Only allow Cloud Functions to write:
```javascript
match /metadata/ticket_counter {
  allow read: if request.auth != null;
  allow write: if false; // Admin/Cloud Functions only
}
```

#### MEDIUM: Reviews Subcollection — No Duplicate Prevention

`pg_listings/{listingId}/reviews` — no check that a user hasn't already reviewed, or has actually stayed at the property.

#### ✅ Good: `saved_searches` Rule

```javascript
match /users/{userId}/saved_searches/{searchId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```
Only properly scoped rule in the file.

#### ✅ Good: `location_data` Rule

```javascript
match /location_data/{docId} {
  allow read: if true;   // Public reference data
  allow write: if false; // Admin-only
}
```

#### Missing: Storage Rules

No `storage.rules` file found. Firebase Storage uses default rules if none deployed. Images may be publicly accessible or inaccessible depending on defaults.

---

### 7.2 Firestore Structure

| Collection | Model | Document Fields |
|-----------|-------|-----------------|
| `users` | `User.kt` | userId, name, email, phone, role, gender, city, college, profileImage, bio, fcmToken, createdAt, isPremiumUser, subscriptionTier, isOwnerVerified, + lifestyle fields, blockedUsers |
| `pg_listings` | `PgListing.kt` | listingId, ownerId, title, description, city, area, lat/lon, price, deposit, roomType, genderAllowed, amenities, images, availableBeds, isActive, rating, ownerName/Image/Phone, createdAt, isVerified, reportCount, reviewCount, isPremium, boostExpiresAt, featuredUntil, viewCount, lifestylePreferences |
| `pg_listings/{id}/reviews` | `Review.kt` | reviewId, listingId, userId, userName, userAvatarUrl, rating (Int), comment, createdAt |
| `roommate_posts` | `RoommatePost.kt` | postId, userId, city, location, priceShare, beds, roomType, postType, description, preferences, address, lat/lon, userName/Image/Phone, isActive, createdAt, + lifestyle fields |
| `chats` | `ChatRoom.kt` | roomId, participants, listingId, roommatePostId, lastMessage, lastMessageTime, confirmedBy, isMatchConfirmed, unreadCount, typingStatus, clearedAt, deletedBy |
| `chats/{id}/messages` | `Message.kt` | messageId, roomId, senderId, text, timestamp, isRead, isDeleted |
| `inquiries` | `Inquiry.kt` | inquiryId, listingId, userId, userName, userPhotoUrl, hostId, moveInDate, roomType, status, message, createdAt |
| `favorites` | (inline) | userId, listingId, createdAt |
| `reports` | `Report.kt` | reportId, listingId, reporterId, reason, details, createdAt |
| `support_tickets` | `SupportTicket.kt` | id, displayId, userId, email, issueType, description, status, adminReply, timestamp, unreadByUser, isClosed |
| `location_data/cities` | `CityDataDocument.kt` | cities: Map<String, {lat, lon}>, displayOrder |
| `location_data/universities` | `UniversityDataDocument.kt` | universities: Map<String, {lat, lon}>, cityAffiliations |
| `location_data/geofences` | `GeofenceDataDocument.kt` | areas: Map<String, List<{lat, lon}>> |
| `users/{uid}/saved_searches` | `SavedSearch.kt` | id, userId, name, priceMin/Max, roomTypes, gender, amenities, sortOption, city, university, notifyEnabled, createdAt, lastNotifiedAt |

#### Field Type Inconsistencies

| Issue | Location |
|-------|----------|
| `PgListing.price` is `Int`, but `SavedSearchNotificationWorker` reads it as `Double` | Worker line 82 |
| `Review.rating` is `Int`, `PgListing.rating` is `Float` | Different precision expectations |
| `favorites` has no model class — uses inline `hashMapOf` | `FavoriteRepository.kt` |

---

### 7.3 Firestore Indexes

**File:** `firestore.indexes.json` — **EMPTY**

Queries that WILL FAIL without composite indexes:

| Query | Collection | Needs Index On |
|-------|-----------|----------------|
| `orderBy("createdAt", DESC)` | `saved_searches` | createdAt (single-field) |
| `whereEqualTo("notifyEnabled", true)` | `saved_searches` | notifyEnabled (single-field) |
| `whereEqualTo("city", X) && whereGreaterThan("createdAt", Y)` | `pg_listings` | city + createdAt (composite) |
| `whereEqualTo("userId", X)` | `favorites` | userId (auto-created) |
| `whereEqualTo("ownerId", X)` | `pg_listings` | ownerId (auto-created) |

---

### 7.4 Cloud Functions

**File:** `functions/index.js`

| Trigger | What It Does |
|---------|-------------|
| `onChatMessage` | Chat message create → push notification to receiver |
| `onNewInquiry` | Inquiry create → push notification to listing owner |
| `onTicketCreated` | Support ticket create → sync to Notion database |

#### HIGH: No Input Validation

All three functions lack schema validation, rate limiting, and field sanitization.

#### HIGH: Notion Placeholder Keys

```javascript
const notion = new Client({ auth: process.env.NOTION_API_KEY || "YOUR_NOTION_API_KEY" });
```
If env vars not set → silent failure with placeholder string.

#### MEDIUM: `sendToDevice()` Deprecated (line 66)

Should use `send()` or `sendEachForMulticast()`.

#### MEDIUM: Node 18 EOL

`functions/package.json` uses `"node": "18"` — EOL since April 2025. Upgrade to Node 20+.

#### LOW: No Retry Logic for Failed FCM Sends

`onNewInquiry` doesn't save send status back to the inquiry document.

---

### 7.5 Vercel Backend

**File:** `staybuddy-backend/api/send-notification.js`

#### HIGH: No Authentication on Endpoint

`/api/send-notification` accepts any POST request with no auth. Anyone can:
- Send spam notifications to any user
- Harvest fcmTokens by probing user IDs
- Abuse the endpoint at zero cost

**Fix:** Add API key validation or Firebase Admin SDK token verification.

#### MEDIUM: No Input Sanitization

Fields `title`, `body`, `routeType`, `routeId`, `avatarUrl` taken directly from request body without length limits.

#### LOW: Duplicates Cloud Functions

The Vercel endpoint does the same thing as Cloud Functions `onChatMessage`/`onNewInquiry`. Two parallel systems for the same task.

---

### 7.6 Cloudinary Secret — CRITICAL

**File:** `StayBuddyApp.kt:78`

```kotlin
val config = mapOf(
    "cloud_name" to "dufri0nc6",
    "api_key" to "289581127678316",
    "api_secret" to "_UCbYliWoYp-IBPvwPs9HiJYZUw",
    "secure" to true
)
```

**The `api_secret` is hardcoded in plain text.** Compiled into APK. Any decompilation tool (jadx, apktool) extracts it immediately. Attacker can delete images, upload malicious content, or abuse storage.

**Fix:** Move to a backend proxy endpoint, or at minimum inject via `BuildConfig` from `gradle.properties` (which is git-ignored).

---

### 7.7 Analytics & Remote Config

#### AnalyticsHelper — ✅ Properly Implemented

- `@Singleton` with Hilt injection
- 7 methods: `logScreenView`, `logListingViewed`, `logListingFavorited`, `logChatStarted`, `logSearchPerformed`, `logRoommateMatched`, `logUpdatePrompted`
- **Gap:** Only `HomeViewModel` uses it. Other ViewModels (Login, Chat, Search, etc.) have no tracking.

#### RemoteConfigManager — ⚠️ Partially Working

| Key | In Code | In Template | Status |
|-----|---------|-------------|--------|
| `force_update_below_version` | ✅ | ✅ | Working |
| `latest_version_name` | ✅ | ✅ | Working |
| `update_message` | ✅ | ✅ | Working |
| `maintenance_mode` | ✅ | ❌ | **Non-functional** — always returns default |
| `feature_roommate_match` | ✅ | ❌ | **Non-functional** — always returns default |
| `latest_version_code` | AppUpdater only | ✅ | Bypasses RemoteConfigManager |
| `is_update_mandatory` | AppUpdater only | ✅ | Bypasses RemoteConfigManager |
| `apk_download_url` | AppUpdater only | ✅ | Bypasses RemoteConfigManager |

**Issues:**
1. `maintenance_mode` and `feature_roommate_match` can't be toggled remotely — missing from template
2. `AppUpdater` bypasses `RemoteConfigManager` — two parallel consumers, redundant init

---

### 7.8 Firebase Dependencies

| Service | Status |
|---------|--------|
| Firebase Auth | ✅ Included |
| Firestore | ✅ Included |
| Firebase Storage | ✅ Included |
| Firebase Messaging | ✅ Included |
| Firebase Analytics | ✅ Included |
| Firebase Remote Config | ✅ Included |
| Firebase BOM | 33.7.0 (current: 34.16.0) |
| google-services plugin | 4.4.2 (current: 4.5.0) |

All required services present. Version updates available but not urgent.

---

### 7.9 Firebase Audit — Priority Summary

| Priority | Issue | Est. Time |
|----------|-------|-----------|
| **P0** | Cloudinary secret → BuildConfig | 30 min |
| **P0** | Firestore rules ownership validation | 2-3 hrs |
| **P0** | Ticket counter protection | 10 min |
| **P1** | Firestore composite indexes (Firebase Console) | 30 min |
| **P1** | Vercel endpoint authentication | 1 hr |
| **P1** | Cloud Functions env var validation | 30 min |
| **P2** | Remote Config template fix | 15 min |
| **P2** | Node 20+ upgrade | 30 min |
| **P2** | Unify Remote Config consumers | 1 hr |
| **P2** | Analytics coverage expansion | 2 hrs |
| **P3** | Storage rules deployment | 30 min |
| **P3** | ProGuard string obfuscation | 30 min |
| **P3** | Backend .env.example documentation | 15 min |

---

*Generated by StayBuddy audit on 15 July 2026. Re-audit recommended after P0 fixes.*
