# StayBuddy Admin Panel — Blueprint

> A comprehensive plan for building an admin panel for moderation, management, analytics, and operational control of the StayBuddy PG accommodation platform.
>
> **📘 Full implementation spec:** See [`admin-panel-implementation.md`](./admin-panel-implementation.md) for the exhaustive day-by-day implementation plan with every file, component, API route, schema, security rule, and edge case documented.

---

## Table of Contents

1. [Architecture Decision](#1-architecture-decision)
2. [Authentication & Role System](#2-authentication--role-system)
3. [Dashboard — Overview](#3-dashboard--overview)
4. [User Management](#4-user-management)
5. [Listing Management](#5-listing-management)
6. [Roommate Post Management](#6-roommate-post-management)
7. [Moderation & Reports](#7-moderation--reports)
8. [Support Ticket Management](#8-support-ticket-management)
9. [Chat & Message Oversight](#9-chat--message-oversight)
10. [Reviews & Ratings Management](#10-reviews--ratings-management)
11. [Analytics & Insights](#11-analytics--insights)
12. [Content & Configuration Management](#12-content--configuration-management)
13. [Push Notification Management](#13-push-notification-management)
14. [Monetization & Premium Controls](#14-monetization--premium-controls)
15. [System Health & Operations](#15-system-health--operations)
16. [Audit Logs & Security](#16-audit-logs--security)
17. [Implementation Phases](#17-implementation-phases)
18. [Tech Stack for the Admin Panel](#18-tech-stack-for-the-admin-panel)
19. [Firestore Schema Changes](#19-firestore-schema-changes)
20. [Security Rules Updates](#20-security-rules-updates)

---

## 1. Architecture Decision

### Option A: Web Admin Panel (Recommended)

A standalone web application (React/Next.js) that talks directly to Firebase/Firestore.

**Pros:**
- Rich dashboard experience with charts, tables, bulk actions
- Accessible from any browser (desktop/mobile)
- Can be deployed on Vercel alongside existing backend
- Easier to iterate on — no app release needed for admin changes
- Better for data-heavy operations (analytics, bulk edits, exports)
- Can use Firebase Admin SDK server-side for elevated permissions

**Cons:**
- Separate codebase to maintain
- Needs its own auth flow

### Option B: In-App Admin Screen

Admin screens inside the Android app, gated by role.

**Pros:**
- Single codebase
- Uses existing UI components and theme

**Cons:**
- Limited screen real estate for dashboards/tables
- Requires app release for admin feature updates
- Admin functionality bloats the APK
- Harder to do data-dense operations (bulk actions, exports, charts)

### Verdict: **Option A — Web Admin Panel**

Build a separate Next.js web app. The existing Cloudflare Worker backend already provides a pattern for this. The Android app's role field gets a new `"admin"` value for identification, but the admin UI lives on the web.

---

## 2. Authentication & Role System

### 2.1 Firestore Schema Changes

Add an `admin` document under a new `admins` collection:

```
admins/{userId}:
  email: string          // admin email
  role: string           // "super_admin" | "moderator" | "support_agent" | "analyst"
  permissions: string[]  // granular permission keys
  createdAt: timestamp
  lastActiveAt: timestamp
  isActive: boolean
```

**Permission keys** (granular, assigned per role):

| Key | What it grants |
|---|---|
| `users.read` | View user list and profiles |
| `users.modify` | Edit user profiles, change roles |
| `users.ban` | Ban/suspend users |
| `listings.read` | View all listings |
| `listings.modify` | Edit, approve, reject, feature listings |
| `listings.delete` | Delete listings |
| `reports.read` | View reports |
| `reports.resolve` | Mark reports as resolved, take action |
| `reviews.read` | View all reviews |
| `reviews.moderate` | Delete inappropriate reviews |
| `tickets.read` | View support tickets |
| `tickets.respond` | Reply to support tickets |
| `chat.read` | View chat metadata (not message content without reason) |
| `chat.monitor` | Read message content for active investigations |
| `analytics.read` | View analytics dashboard |
| `config.manage` | Edit Remote Config, location data, app settings |
| `notifications.send` | Send push notifications to users/segments |
| `audit.read` | View audit logs |
| `admins.manage` | Manage other admin accounts (super_admin only) |

### 2.2 Admin Authentication Flow

1. Admin navigates to `admin.staybuddy.app`
2. Firebase Auth sign-in (email/password + Google — same SDK)
3. After auth, check `admins/{uid}` document exists and `isActive == true`
4. If not found → access denied page
5. If found → load permissions, store in session context
6. All Firestore queries use Firebase Security Rules to enforce `admins/{uid}` existence

### 2.3 Default Admin Bootstrap

- First super_admin created via a Firebase Functions callable (`createAdmin`)
- Only callable by existing super_admin (or via Firebase Console for initial setup)
- Seed document: `admins/{uid}` with `role: "super_admin"` and full permissions

---

## 3. Dashboard — Overview

The landing page after login. A high-level snapshot of platform health.

### 3.1 KPI Cards (Top Row)

| Metric | Source | Update Frequency |
|---|---|---|
| Total Users | `users` collection count | Real-time (Firestore count query) |
| Total Listings | `pg_listings` collection count | Real-time |
| Active Listings | `pg_listings` where `isActive == true` | Real-time |
| Total Roommate Posts | `roommate_posts` collection count | Real-time |
| Open Reports | `reports` where `resolved == false` | Real-time |
| Open Support Tickets | `support_tickets` where `status != "Resolved"` | Real-time |
| New Users (Today/7d/30d) | `createdAt` filter | Cached (5 min) |
| New Listings (Today/7d/30d) | `createdAt` filter | Cached (5 min) |

### 3.2 Charts Panel

| Chart | Type | Data |
|---|---|---|
| User Signups Over Time | Line chart | Daily/weekly new registrations |
| Listings by City | Bar chart | Distribution across 7 cities |
| Listing Price Distribution | Histogram | Price ranges across listings |
| Roommate Posts Trend | Area chart | OFFER vs SEEK posts over time |
| Inquiries per Week | Bar chart | Volume of student inquiries |
| Reports per Week | Bar chart | Reports filed over time |
| Support Ticket Volume | Line chart | Tickets created vs resolved |
| Room Type Distribution | Donut chart | Single vs Shared vs Double |
| User Role Split | Donut chart | Students vs Owners |

### 3.3 Recent Activity Feed

A chronological feed of platform events:
- New user registered (name, role, city)
- New listing added (title, city, price)
- Report filed (listing, reason)
- Support ticket created (type, status)
- Listing auto-deactivated (reason: reports threshold)

---

## 4. User Management

### 4.1 User List View

- Paginated, searchable, sortable table
- Columns: Avatar, Name, Email, Phone, Role, City, Joined Date, Status, Actions
- Filters: Role (student/owner/all), City, Gender, Status (active/banned), Premium Status
- Search by name, email, phone
- Bulk actions: Select multiple → Export CSV, Bulk ban/unban

### 4.2 User Detail View

Click a row → slide-out panel or dedicated page:

**Profile Section:**
- All User model fields displayed
- Profile image (large)
- Lifestyle preferences as badges
- Quiz results visualization

**Activity Section:**
- Listings owned (for owners) — count + recent list
- Roommate posts created
- Reviews written
- Inquiries sent/received
- Chat rooms active (count, not content)
- Reports filed by this user
- Reports against this user
- Support tickets

**Moderation Section:**
- **Ban/Suspend User**: Set `isActive = false` in `admins` doc or add `bannedAt`/`banReason` fields to user
- **Force Password Reset**: Invalidate sessions
- **Delete Account**: Trigger the same soft-delete flow as self-deletion
- **Change Role**: Toggle student/owner
- **Verify Owner**: Set `isOwnerVerified = true`
- **Premium Status**: Manually set subscription tier

**History Section:**
- Audit log entries for this user (admin actions taken)

### 4.3 User Segmentation

Pre-built segments for targeted operations:
- Users who registered in last 7/30 days
- Owners with no listings
- Students who haven't made inquiries
- Premium vs free users
- Users by city
- Inactive users (no activity in 30/60/90 days)
- Users with pending quiz completion

---

## 5. Listing Management

### 5.1 Listing List View

- Paginated table with image thumbnail
- Columns: Thumbnail, Title, Owner, City, Area, Price, Room Type, Status, Verified, Reports, Rating, Created, Actions
- Filters: City, Status (active/inactive/all), Verified, Premium, Room Type, Gender Allowed, Price Range, Has Reports
- Sort: Price, Rating, Reports, Created, View Count
- Bulk actions: Approve, Deactivate, Feature, Delete

### 5.2 Listing Detail View

**Info Panel:**
- All PgListing fields
- Image gallery (full-size, expandable)
- Map pin location
- Owner info with link to user detail
- Lifestyle preferences
- Amenities list

**Moderation Panel:**
- **Approve/Reject Listing**: For a future approval workflow
- **Activate/Deactivate**: Toggle `isActive`
- **Mark as Verified**: Set `isVerified = true` (manual verification after physical inspection)
- **Feature Listing**: Set `featuredUntil` timestamp (premium placement)
- **Boost Listing**: Set `boostExpiresAt` timestamp
- **Edit Listing**: Admin can modify any field (title, description, price, amenities, etc.)
- **Delete Listing**: Remove with confirmation

**Reports Section:**
- List of all reports against this listing
- Reporter info (anonymized or visible based on policy)
- Report reason + details
- Action: Dismiss report, Deactivate listing, Contact owner

**Reviews Section:**
- All reviews for this listing
- Flag/delete inappropriate reviews
- Override rating if needed

**Stats:**
- View count over time
- Inquiry count
- Favorite count
- Chat initiations

### 5.3 Listing Approval Workflow (Future)

When enabled via Remote Config:
1. Owner submits new listing → status: `PENDING_REVIEW`
2. Admin sees it in "Pending Approval" queue
3. Admin reviews → Approve (sets `isActive = true`, status: `APPROVED`) or Reject (with reason, status: `REJECTED`)
4. Owner gets notified of decision
5. Rejected listings can be edited and resubmitted

---

## 6. Roommate Post Management

### 6.1 Post List View

- Similar to listings table
- Columns: User, City, Type (OFFER/SEEK), Price Share, Beds, Status, Created, Actions
- Filters: City, Type, Active/Inactive

### 6.2 Post Detail View

- All RoommatePost fields
- Compatibility fields displayed as badges
- User profile link
- Location on map

**Moderation:**
- Activate/Deactivate
- Delete (with reason)
- Contact poster

---

## 7. Moderation & Reports

### 7.1 Reports Queue

A dedicated moderation queue — the most operationally important screen.

**Queue View:**
- Cards (not table) showing each unresolved report
- Each card shows:
  - Report reason + details
  - Reported listing/post/user info with thumbnail
  - Reporter info (anonymized)
  - Report count on the listing
  - Timestamp
  - Quick actions: Dismiss, Deactivate Content, Ban User, Escalate

**Filters:**
- Type: Listing Reports, User Reports
- Reason category
- Date range
- Status: Pending, Reviewed, Resolved, Dismissed
- Severity (auto-assigned based on reason)

**Batch Operations:**
- Select multiple reports on the same listing → one-click resolve all

### 7.2 Report Resolution Actions

| Action | Effect |
|---|---|
| Dismiss | Mark report as `resolved`, no action on content |
| Deactivate Content | Set `isActive = false` on listing/post |
| Delete Content | Remove listing/post permanently |
| Warn Owner/Poster | Send in-app notification with warning message |
| Ban User | Set user as banned, deactivate all their content |
| Escalate | Mark for super_admin review |
| Contact Reporter | Send thank-you notification |

### 7.3 Auto-Moderation Rules (Configurable)

Via Remote Config or admin settings:

| Rule | Default | Configurable |
|---|---|---|
| Auto-deactivate listing at N reports | 5 | Yes (N) |
| Auto-ban user at N reports against them | 10 | Yes (N) |
| Profanity filter on listing descriptions | Off | Yes |
| Image content screening | Off | Yes (future: ML-based) |
| Duplicate listing detection | Off | Yes (future) |
| Price anomaly detection (too cheap/suspicious) | Off | Yes (future) |

### 7.4 Content Quality Score

Auto-assign a quality score to listings based on:
- Has images (count)
- Description length
- Amenities filled out
- Price合理性 (within city average ± 2σ)
- Owner verified status
- Review count and rating

Listings below a threshold get flagged for admin review.

---

## 8. Support Ticket Management

### 8.1 Ticket Queue

Replaces the current Notion-based workflow with a proper admin UI.

**Kanban Board View:**
- Columns: Pending → In Progress → Resolved
- Each card: Ticket ID, User, Issue Type, Created, Priority
- Drag-and-drop to change status

**Table View (alternative):**
- Sortable, filterable table of all tickets

### 8.2 Ticket Detail View

- Full ticket info (all SupportTicket fields)
- User profile link
- Conversation thread:
  - User's original message
  - Admin replies (chronological)
  - System notes (auto-generated: "Ticket created", "Status changed to In Progress")
- Reply textarea with template support
- Status change dropdown
- Assign to specific admin agent
- Priority levels: Low, Medium, High, Urgent

### 8.3 Ticket Statistics

- Average resolution time
- Tickets by issue type
- Tickets by status
- Admin response time
- User satisfaction (future: post-resolution survey)

### 8.4 Canned Responses

Pre-built response templates for common issues:
- "Listing removed due to policy violation"
- "Your listing has been verified"
- "Account issue resolved"
- "Escalated to technical team"
- Custom templates per admin

---

## 9. Chat & Message Oversight

### 9.1 Chat Room List

- Paginated list of all chat rooms
- Columns: Room ID, Participants, Last Message, Created, Messages Count, Status
- Filters: Active/Cleared, Has listing, Has roommate post, Date range
- Search by participant name

### 9.2 Chat Room Detail

**Metadata View:**
- Participants (with profile links)
- Associated listing or roommate post
- Message count, last activity
- Match confirmation status

**Message View (requires `chat.monitor` permission):**
- Full message thread
- Only accessible when there's an active investigation/report
- Flagged message highlighting
- Admin can delete individual messages (soft-delete)

### 9.3 Abuse Detection (Future)

- Keyword flagging in messages
- Spam detection (repeated messages, rapid sending)
- Harassment pattern detection
- Auto-flag for review

---

## 10. Reviews & Ratings Management

### 10.1 Reviews List

- All reviews across all listings
- Columns: Listing, Reviewer, Rating, Comment, Date, Actions
- Filters: Rating (1-5), Date range, Listing, Has flagged content
- Sort: Date, Rating

### 10.2 Review Detail

- Full review text
- Reviewer profile link
- Listing link
- **Actions:** Delete, Flag, Edit (admin correction), Respond (admin note on listing)

### 10.3 Rating Analytics

- Average rating by city
- Average rating by room type
- Rating distribution (histogram)
- Listings with suspicious rating patterns (all 5s from same IP/timeframe)

---

## 11. Analytics & Insights

### 11.1 Platform Health Metrics

| Category | Metrics |
|---|---|
| **Growth** | DAU, WAU, MAU, Registration rate, Churn rate |
| **Engagement** | Sessions per user, Avg session duration, Screen flow (funnel), Search→Inquiry conversion |
| **Listings** | New/active/expired listings, Avg time to first inquiry, Occupancy rate |
| **Revenue** | Premium subscriptions, Featured listing revenue, Boost revenue (future) |
| **Support** | Ticket volume, Resolution time, SLA compliance |
| **Moderation** | Reports filed, Actions taken, Auto-deactivation rate, Content quality score trend |

### 11.2 City-Level Analytics

Drill down per city:
- Active users (students + owners)
- Active listings
- Avg price by room type
- Demand signal (searches, inquiries)
- Supply signal (new listings)
- Competition metrics

### 11.3 User Funnel

Track the user journey:
```
Install → Register → Complete Profile → Browse Listings → Save Favorite → Send Inquiry → Confirm Match → Move In
```

Each step with conversion rates and drop-off analysis.

### 11.4 Cohort Analysis

- Registration cohort (weekly/monthly)
- Retention curves
- Feature adoption rates

### 11.5 Revenue Analytics (Future)

- MRR/ARR
- Premium conversion rate
- ARPU
- LTV by cohort

---

## 12. Content & Configuration Management

### 12.1 Remote Config Management

View and edit all Remote Config parameters from the admin panel:

| Parameter | Current Value | Type | Description |
|---|---|---|---|
| `force_update_below_version` | 10 | number | Force update if app version below this |
| `maintenance_mode` | false | boolean | Show maintenance screen |
| `feature_roommate_match` | true | boolean | Enable/disable roommate matching |
| `latest_version_name` | "1.0.32" | string | Shown in update dialog |
| `update_message` | "" | string | Custom update prompt |

**New parameters to add:**

| Parameter | Default | Description |
|---|---|---|
| `admin_panel_enabled` | true | Enable admin panel access |
| `report_auto_deactivate_threshold` | 5 | Reports needed for auto-deactivation |
| `listing_approval_required` | false | Enable listing approval workflow |
| `profanity_filter_enabled` | false | Auto-filter profanity in listings |
| `max_images_per_listing` | 10 | Max images an owner can upload |
| `max_listings_per_owner` | 5 | Max active listings per owner |
| `chat_monitoring_enabled` | false | Enable message keyword flagging |
| `saved_search_notification_interval` | 60 | Minutes between saved search checks |
| `maintenance_message` | "We'll be back soon!" | Message shown during maintenance |

### 12.2 Location Data Management

Edit the `location_data` Firestore collection from the admin panel:

**Cities Manager:**
- List of all supported cities
- Add/Edit/Delete cities
- Set coordinates, search radius
- Enable/disable city

**Universities Manager:**
- List of all universities
- Add/Edit/Delete universities
- Associate with cities
- Set coordinates

**Geofences Manager:**
- Visual polygon editor on a map
- Draw/edit geofence boundaries
- Associate with cities

### 12.3 Amenity Management

Manage the global list of available amenities:
- Add/Edit/Delete amenities
- Set display name and icon
- Order for display
- Enable/disable

### 12.4 Room Type Management

Manage available room types:
- Single, Shared, Double, Triple, etc.
- Add/Remove options

### 12.5 Issue Type Management

Manage support ticket issue types:
- Account Issues, Listing Problems, Payment, Bug Report, Feature Request, Other
- Add/Remove/Edit

---

## 13. Push Notification Management

### 13.1 Send Notification

Compose and send push notifications:

- **Recipients:**
  - All users
  - All students
  - All owners
  - Users in specific city
  - Users inactive for N days
  - Premium users only
  - Custom segment (by criteria)

- **Content:**
  - Title
  - Body
  - Channel (messages/listings/roommates/saved_searches)
  - Deep link route (optional)
  - Image (optional)

- **Scheduling:**
  - Send now
  - Schedule for later (date/time)

### 13.2 Notification History

- Log of all admin-sent notifications
- Sent time, recipient count, content
- Delivery status (future: with FCM delivery receipts)

### 13.3 Notification Templates

Pre-built templates for common scenarios:
- Welcome message
- Listing approved/rejected
- New message alert
- Weekly digest
- Maintenance announcement

---

## 14. Monetization & Premium Controls

### 14.1 Premium User Management

- List of premium users
- Manually grant/revoke premium status
- Set subscription tier
- Set expiry date
- View payment history (future)

### 14.2 Featured Listings

- List of featured listings
- Manually feature a listing (set `featuredUntil`)
- View which listings have been featured
- Revenue tracking per featured listing

### 14.3 Boost Management

- List of boosted listings
- Manually boost a listing (set `boostExpiresAt`)
- View boost performance (views before/after)

### 14.4 Pricing Insights

- Average prices by city, room type, area
- Price trends over time
- Price distribution outliers
- Competitive pricing recommendations (future)

---

## 15. System Health & Operations

### 15.1 System Status

- Firebase Firestore read/write counts (from Firebase Console API)
- Cloud Functions invocation counts and error rates
- Cloudflare Worker request counts and latency
- Room DB sync status indicators

### 15.2 Error Monitoring

- Aggregated client-side errors (from Firebase Crashlytics — future)
- Cloud Functions error logs
- Worker error logs
- Firestore permission denied events

### 15.3 Maintenance Mode

- One-click toggle for `maintenance_mode` Remote Config
- Custom maintenance message editor
- Scheduled maintenance window (future)

### 15.4 Version Management

- Current version stats (users on each version)
- Force update management
- Gradual rollout control (future)

---

## 16. Audit Logs & Security

### 16.1 Audit Log Schema

New Firestore collection `audit_logs`:

```
audit_logs/{logId}:
  adminId: string        // who performed the action
  adminEmail: string     // for display
  action: string         // "user.ban", "listing.deactivate", "config.update", etc.
  targetType: string     // "user", "listing", "report", "ticket", "config", "admin"
  targetId: string       // ID of the affected entity
  details: map           // freeform: what changed, before/after values
  timestamp: timestamp
  ipAddress: string      // admin's IP
```

### 16.2 Audit Log Viewer

- Filterable by: Admin, Action type, Target type, Date range
- Searchable by target ID
- Exportable to CSV

### 16.3 Action Examples

| Action | Target | Details |
|---|---|---|
| `user.ban` | User | `{ reason: "Report threshold", reportCount: 12 }` |
| `listing.deactivate` | Listing | `{ reason: "admin_action", triggeredBy: "report_review" }` |
| `listing.feature` | Listing | `{ until: "2026-08-01", adminNote: "Quality listing" }` |
| `ticket.resolve` | Ticket | `{ response: "Issue fixed", resolutionTime: "2h" }` |
| `config.update` | Remote Config | `{ key: "report_threshold", oldValue: 5, newValue: 3 }` |
| `admin.create` | Admin | `{ email: "new@sb.com", role: "moderator" }` |
| `notification.send` | Broadcast | `{ recipients: "all_owners", title: "New feature" }` |

---

## 17. Implementation Phases

### Phase 1 — Foundation (Weeks 1–3)

**Goal:** Working admin panel with auth, dashboard, and basic user/listing management.

| Task | Effort |
|---|---|
| Set up Next.js project with Tailwind CSS | 1 day |
| Firebase Auth integration for admin login | 1 day |
| Admin role/permissions system (Firestore + middleware) | 2 days |
| Admin layout (sidebar nav, header, responsive) | 2 days |
| Dashboard with KPI cards (using Firestore queries) | 3 days |
| User list view with search/filter/pagination | 2 days |
| User detail view | 1 day |
| Listing list view with search/filter/pagination | 2 days |
| Listing detail view | 1 day |
| Basic audit logging | 1 day |

**Deliverable:** Admin can log in, see overview, browse users and listings.

### Phase 2 — Moderation (Weeks 4–6)

**Goal:** Complete moderation workflow.

| Task | Effort |
|---|---|
| Reports queue (kanban/list view) | 3 days |
| Report resolution actions (deactivate, ban, dismiss) | 2 days |
| Support ticket management (replaces Notion) | 3 days |
| Ticket reply system with canned responses | 2 days |
| Review moderation | 1 day |
| Roommate post management | 1 day |
| Audit logging for all moderation actions | 1 day |

**Deliverable:** Full moderation pipeline, support tickets handled in-app.

### Phase 3 — Analytics & Config (Weeks 7–9)

**Goal:** Data insights and configuration management.

| Task | Effort |
|---|---|
| Analytics dashboard (charts with Recharts/Chart.js) | 4 days |
| City-level analytics drill-down | 2 days |
| User funnel visualization | 2 days |
| Remote Config management UI | 2 days |
| Location data management (cities, universities, geofences) | 3 days |
| Amenity/room type management | 1 day |

**Deliverable:** Admin can monitor platform health and manage configuration.

### Phase 4 — Operations (Weeks 10–12)

**Goal:** Operational tools and advanced features.

| Task | Effort |
|---|---|
| Push notification composer and send | 2 days |
| Notification history | 1 day |
| Premium/monetization management | 2 days |
| Chat oversight (metadata + message monitoring) | 2 days |
| System health dashboard | 2 days |
| Maintenance mode toggle | 0.5 day |
| CSV export for all data tables | 1 day |
| Admin user management (create/edit admins) | 2 days |

**Deliverable:** Full operational control.

### Phase 5 — Advanced (Future)

| Feature | Description |
|---|---|
| Auto-moderation rules engine | Configurable rules with thresholds |
| Content quality scoring | ML-based listing quality assessment |
| A/B testing framework | Remote Config-driven experiments |
| Cohort analysis | Retention curves, LTV calculations |
| Scheduled notifications | Calendar-based notification scheduling |
| Multi-admin collaboration | Admin-to-admin notes on reports/tickets |
| Two-factor auth for admins | TOTP-based 2FA |
| Rate limiting dashboard | API usage monitoring |

---

## 18. Tech Stack for the Admin Panel

| Layer | Technology | Why |
|---|---|---|
| Framework | **Next.js 14+ (App Router)** | SSR for auth, API routes for admin-only operations, great DX |
| UI | **Tailwind CSS** + **shadcn/ui** | Fast to build, consistent, accessible, M3-inspired components |
| Charts | **Recharts** or **Tremor** | React-native chart libraries, good for dashboards |
| Tables | **TanStack Table** | Powerful sorting, filtering, pagination for data-heavy views |
| Auth | **Firebase Auth** (client SDK) | Same auth system as the Android app |
| Database | **Firestore** (client SDK + Admin SDK in API routes) | Direct reads for fast UI, Admin SDK for privileged operations |
| State | **Zustand** or React Context | Lightweight state for filters, selections |
| Deployment | **Vercel** | Same platform as existing backend, zero-config |
| Email | **Resend** or **Nodemailer** | Admin notifications, user communication (future) |

---

## 19. Firestore Schema Changes

### New Collections

```
admins/
  {userId}:
    email: string
    role: string              // "super_admin" | "moderator" | "support_agent" | "analyst"
    permissions: string[]
    createdAt: timestamp
    lastActiveAt: timestamp
    isActive: boolean

audit_logs/
  {logId}:
    adminId: string
    adminEmail: string
    action: string
    targetType: string
    targetId: string
    details: map
    timestamp: timestamp
    ipAddress: string

notifications_sent/          (admin broadcast history)
  {notifId}:
    title: string
    body: string
    recipientSegment: string
    recipientCount: number
    sentBy: string
    sentAt: timestamp
    channel: string
```

### Modified Collections

**`users`** — add fields:
```
isActive: boolean = true     // false = banned
bannedAt: timestamp?
banReason: string?
```

**`reports`** — add fields:
```
status: string = "pending"   // "pending" | "reviewed" | "resolved" | "dismissed"
resolvedBy: string?          // admin userId
resolvedAt: timestamp?
resolutionNote: string?
```

**`support_tickets`** — add fields:
```
assignedTo: string?          // admin userId
priority: string = "medium"  // "low" | "medium" | "high" | "urgent"
```

---

## 20. Security Rules Updates

```
// Admin access pattern (in Firestore rules)
function isAdmin() {
  return get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.isActive == true;
}

function hasPermission(permission) {
  return isAdmin() &&
    permission in get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.permissions;
}

// Users collection
match /users/{userId} {
  // Admins can read all users
  allow read: if isAdmin();
  // Admins can update any user (for moderation)
  allow update: if hasPermission('users.modify') || hasPermission('users.ban');
}

// Reports collection
match /reports/{reportId} {
  // Only admins can read reports
  allow read: if hasPermission('reports.read');
  allow update: if hasPermission('reports.resolve');
}

// Admins collection
match /admins/{adminId} {
  allow read: if isAdmin();
  allow write: if hasPermission('admins.manage');
}

// Audit logs
match /audit_logs/{logId} {
  allow read: if hasPermission('audit.read');
  allow create: if isAdmin();
}
```

---

## Summary: What You Can Build

| Category | Features |
|---|---|
| **Overview** | Real-time KPI dashboard, charts, activity feed |
| **User Management** | List, search, filter, detail view, ban/unban, role change, verification |
| **Listing Management** | List, search, filter, detail, approve/reject, activate/deactivate, feature, boost, edit, delete |
| **Moderation** | Reports queue, resolution actions, auto-moderation rules, content quality scoring |
| **Support** | Ticket kanban, reply system, canned responses, assignment, priority |
| **Chat Oversight** | Room metadata, message monitoring (investigation-only), abuse detection |
| **Reviews** | List, moderate, delete, analytics |
| **Analytics** | Growth metrics, engagement funnels, city insights, revenue tracking |
| **Configuration** | Remote Config, location data, amenities, room types, feature flags |
| **Notifications** | Compose, segment, schedule, send, history |
| **Monetization** | Premium management, featured/boost controls, pricing insights |
| **System** | Health monitoring, maintenance mode, version management |
| **Security** | Audit logs, admin management, 2FA (future) |

This blueprint gives you a 12+ week roadmap of work. Start with Phase 1 (foundation + auth + dashboard + basic CRUD) and iterate forward. Each phase is independently valuable — you don't need to build everything before it becomes useful.
