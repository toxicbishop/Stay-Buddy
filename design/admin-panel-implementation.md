# StayBuddy Admin Panel — Full Implementation Plan

> The definitive, exhaustive blueprint for building the StayBuddy admin panel. Every screen, every component, every API route, every security rule, every edge case — documented here.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack & Dependencies](#2-tech-stack--dependencies)
3. [Project Structure](#3-project-structure)
4. [Environment & Configuration](#4-environment--configuration)
5. [Authentication & Authorization](#5-authentication--authorization)
6. [Firestore Schema — Full Specification](#6-firestore-schema--full-specification)
7. [Security Rules — Complete Rewrite](#7-security-rules--complete-rewrite)
8. [Layout & Navigation](#8-layout--navigation)
9. [Page Specifications — Every Screen](#9-page-specifications--every-screen)
10. [Component Library](#10-component-library)
11. [API Routes](#11-api-routes)
12. [Hooks & State Management](#12-hooks--state-management)
13. [Utilities & Helpers](#13-utilities--helpers)
14. [Firebase Cloud Functions — Admin Extensions](#14-firebase-cloud-functions--admin-extensions)
15. [Android App Changes](#15-android-app-changes)
16. [Testing Strategy](#16-testing-strategy)
17. [Deployment & CI/CD](#17-deployment--cicd)
18. [Implementation Phases — Day-by-Day](#18-implementation-phases--day-by-day)
19. [Edge Cases & Error Handling](#19-edge-cases--error-handling)
20. [Performance Considerations](#20-performance-considerations)
21. [Accessibility](#21-accessibility)

---

## 1. Project Overview

### What

A web-based admin panel for the StayBuddy PG accommodation platform. Provides moderation, management, analytics, configuration, and operational control over the entire StayBuddy ecosystem.

### Why

Currently, admin operations are fragmented:
- Reports auto-deactivate listings at 5 reports with no human review
- Support tickets sync to Notion — a workaround, not a solution
- No visibility into platform health, user growth, or engagement
- Location data must be edited manually in Firebase Console
- No way to send targeted push notifications
- No audit trail for admin actions
- No user ban capability (only client-side blocking)

### Where It Lives

```
StayBuddy/              ← git repo root
├── admin-panel/        ← NEW — Next.js 14+ project (this document)
├── app/                ← Android app (existing, minimal changes)
├── functions/          ← Firebase Cloud Functions (existing, minor additions)
├── staybuddy-worker/   ← Cloudflare Worker (existing, unchanged)
└── design/             ← Design specs (existing)
```

### Who Uses It

| Role | Access Level | Typical Tasks |
|---|---|---|
| **Super Admin** | Full access | Everything + manage admins |
| **Moderator** | Content moderation | Reports, listings, reviews, users (ban) |
| **Support Agent** | Ticket handling | Support tickets, user lookup |
| **Analyst** | Read-only analytics | Dashboard, analytics, exports |

---

## 2. Tech Stack & Dependencies

### Core Framework

| Package | Version | Purpose |
|---|---|---|
| `next` | 14.x+ | React framework with App Router |
| `react` | 18.x | UI library |
| `react-dom` | 18.x | DOM rendering |
| `typescript` | 5.x | Type safety |

### UI

| Package | Version | Purpose |
|---|---|---|
| `tailwindcss` | 3.x | Utility-first CSS |
| `@tailwindcss/typography` | 0.5.x | Prose styling for rich text |
| `shadcn/ui` | latest | Pre-built accessible components (built on Radix UI) |
| `@radix-ui/react-dialog` | latest | Modal dialogs |
| `@radix-ui/react-dropdown-menu` | latest | Dropdown menus |
| `@radix-ui/react-select` | latest | Select inputs |
| `@radix-ui/react-tabs` | latest | Tab panels |
| `@radix-ui/react-tooltip` | latest | Tooltips |
| `@radix-ui/react-toast` | latest | Toast notifications |
| `@radix-ui/react-popover` | latest | Popovers |
| `@radix-ui/react-switch` | latest | Toggle switches |
| `@radix-ui/react-separator` | latest | Visual dividers |
| `lucide-react` | latest | Icon library (shadcn default) |
| `class-variance-authority` | latest | Component variant utility |
| `clsx` | latest | Conditional classnames |
| `tailwind-merge` | latest | Merge tailwind classes without conflicts |

### Data & State

| Package | Version | Purpose |
|---|---|---|
| `firebase` | 10.x | Firebase client SDK (Auth + Firestore) |
| `firebase-admin` | 12.x | Admin SDK (API routes only, server-side) |
| `@tanstack/react-table` | 8.x | Headless table with sort/filter/pagination |
| `zustand` | 4.x | Lightweight global state (filters, selections) |
| `swr` or `@tanstack/react-query` | latest | Data fetching, caching, revalidation |

### Charts & Visualization

| Package | Version | Purpose |
|---|---|---|
| `recharts` | 2.x | Charts (line, bar, area, pie, radar) |
| `@uiw/react-heat-map` | latest | Heatmaps for analytics |
| `date-fns` | 3.x | Date formatting and manipulation |
| `react-countup` | latest | Animated number counters on KPIs |

### Tables & Export

| Package | Version | Purpose |
|---|---|---|
| `@tanstack/react-table` | 8.x | Data tables |
| `papaparse` | 5.x | CSV parsing/generation |
| `file-saver` | 2.x | Trigger file downloads |

### Forms & Validation

| Package | Version | Purpose |
|---|---|---|
| `react-hook-form` | 7.x | Form state management |
| `zod` | 3.x | Schema validation |
| `@hookform/resolvers` | latest | Zod ↔ react-hook-form bridge |

### Maps (for location management)

| Package | Version | Purpose |
|---|---|---|
| `leaflet` | 1.9.x | Map rendering |
| `react-leaflet` | 4.x | React bindings for Leaflet |
| `@types/leaflet` | latest | TypeScript types |

### Utilities

| Package | Version | Purpose |
|---|---|---|
| `next-themes` | 0.3.x | Dark/light mode toggle |
| `sonner` | latest | Toast notifications (shadcn recommended) |
| `cmdk` | latest | Command palette (Ctrl+K search) |
| `vaul` | latest | Drawer component (shadcn) |
| `input-otp` | latest | OTP input for 2FA (future) |

### Dev Dependencies

| Package | Purpose |
|---|---|
| `eslint` + `eslint-config-next` | Linting |
| `prettier` | Code formatting |
| `@testing-library/react` | Component testing |
| `jest` + `jest-environment-jsdom` | Test runner |
| `msw` | API mocking for tests |
| `cypress` or `playwright` | E2E testing (Phase 5) |

### Install Command

```bash
npx create-next-app@latest admin-panel \
  --typescript --tailwind --eslint --app --src-dir \
  --import-alias "@/*"

cd admin-panel

# shadcn/ui setup
npx shadcn@latest init
npx shadcn@latest add \
  button card badge dialog sheet table tabs \
  select input label textarea separator \
  dropdown-menu command popover tooltip \
  toast avatar switch checkbox skeleton \
  sonner calendar form

# Core dependencies
npm install firebase zustand @tanstack/react-table \
  @tanstack/react-query recharts date-fns \
  react-hook-form zod @hookform/resolvers \
  papaparse file-saver react-countup \
  next-themes lucide-react cmdk vaul

# Map dependencies (for location management)
npm install leaflet react-leaflet @types/leaflet

# Dev dependencies
npm install -D @types/papaparse @types/file-saver \
  jest @testing-library/react jest-environment-jsdom \
  @testing-library/jest-dom
```

---

## 3. Project Structure

```
admin-panel/
├── public/
│   ├── favicon.ico
│   ├── logo.svg                       # StayBuddy admin logo
│   └── placeholder.png                # Fallback images
│
├── src/
│   ├── app/                           # Next.js App Router
│   │   ├── (auth)/                    # Auth route group (no sidebar)
│   │   │   ├── login/
│   │   │   │   └── page.tsx           # Login page
│   │   │   ├── layout.tsx             # Auth layout (centered card)
│   │   │   └── denied/
│   │   │       └── page.tsx           # Access denied page
│   │   │
│   │   ├── (dashboard)/               # Dashboard route group (with sidebar)
│   │   │   ├── layout.tsx             # App shell: sidebar + header + main
│   │   │   │
│   │   │   ├── page.tsx               # /dashboard — Overview
│   │   │   │
│   │   │   ├── users/
│   │   │   │   ├── page.tsx           # /users — User list
│   │   │   │   └── [userId]/
│   │   │   │       └── page.tsx       # /users/:id — User detail
│   │   │   │
│   │   │   ├── listings/
│   │   │   │   ├── page.tsx           # /listings — Listing list
│   │   │   │   ├── pending/
│   │   │   │   │   └── page.tsx       # /listings/pending — Approval queue
│   │   │   │   └── [listingId]/
│   │   │   │       └── page.tsx       # /listings/:id — Listing detail
│   │   │   │
│   │   │   ├── roommates/
│   │   │   │   ├── page.tsx           # /roommates — Roommate post list
│   │   │   │   └── [postId]/
│   │   │   │       └── page.tsx       # /roommates/:id — Post detail
│   │   │   │
│   │   │   ├── reports/
│   │   │   │   ├── page.tsx           # /reports — Moderation queue
│   │   │   │   └── [reportId]/
│   │   │   │       └── page.tsx       # /reports/:id — Report detail
│   │   │   │
│   │   │   ├── tickets/
│   │   │   │   ├── page.tsx           # /tickets — Support ticket board
│   │   │   │   └── [ticketId]/
│   │   │   │       └── page.tsx       # /tickets/:id — Ticket detail
│   │   │   │
│   │   │   ├── reviews/
│   │   │   │   └── page.tsx           # /reviews — All reviews
│   │   │   │
│   │   │   ├── chats/
│   │   │   │   ├── page.tsx           # /chats — Chat room list
│   │   │   │   └── [chatId]/
│   │   │   │       └── page.tsx       # /chats/:id — Chat detail
│   │   │   │
│   │   │   ├── analytics/
│   │   │   │   ├── page.tsx           # /analytics — Main analytics
│   │   │   │   ├── users/
│   │   │   │   │   └── page.tsx       # /analytics/users — User analytics
│   │   │   │   ├── listings/
│   │   │   │   │   └── page.tsx       # /analytics/listings — Listing analytics
│   │   │   │   └── revenue/
│   │   │   │       └── page.tsx       # /analytics/revenue — Revenue analytics
│   │   │   │
│   │   │   ├── notifications/
│   │   │   │   ├── page.tsx           # /notifications — Send notification
│   │   │   │   └── history/
│   │   │   │       └── page.tsx       # /notifications/history — Past notifications
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── page.tsx           # /config — Config overview
│   │   │   │   ├── remote-config/
│   │   │   │   │   └── page.tsx       # /config/remote-config
│   │   │   │   ├── locations/
│   │   │   │   │   └── page.tsx       # /config/locations
│   │   │   │   ├── amenities/
│   │   │   │   │   └── page.tsx       # /config/amenities
│   │   │   │   └── content/
│   │   │   │       └── page.tsx       # /config/content (room types, issue types)
│   │   │   │
│   │   │   ├── premium/
│   │   │   │   └── page.tsx           # /premium — Monetization management
│   │   │   │
│   │   │   ├── health/
│   │   │   │   └── page.tsx           # /health — System health
│   │   │   │
│   │   │   ├── audit/
│   │   │   │   └── page.tsx           # /audit — Audit logs
│   │   │   │
│   │   │   └── admins/
│   │   │       └── page.tsx           # /admins — Admin user management
│   │   │
│   │   ├── api/                       # Next.js API Routes (server-side)
│   │   │   ├── admin/
│   │   │   │   ├── create/route.ts    # POST — Create admin user
│   │   │   │   ├── update/route.ts    # POST — Update admin user
│   │   │   │   └── list/route.ts      # GET — List all admins
│   │   │   ├── users/
│   │   │   │   ├── ban/route.ts       # POST — Ban user
│   │   │   │   ├── unban/route.ts     # POST — Unban user
│   │   │   │   ├── delete/route.ts    # POST — Soft-delete user
│   │   │   │   └── export/route.ts    # GET — Export users CSV
│   │   │   ├── listings/
│   │   │   │   ├── approve/route.ts   # POST — Approve listing
│   │   │   │   ├── reject/route.ts    # POST — Reject listing
│   │   │   │   ├── feature/route.ts   # POST — Feature listing
│   │   │   │   ├── boost/route.ts     # POST — Boost listing
│   │   │   │   └── export/route.ts    # GET — Export listings CSV
│   │   │   ├── reports/
│   │   │   │   ├── resolve/route.ts   # POST — Resolve report
│   │   │   │   └── bulk-resolve/route.ts  # POST — Bulk resolve
│   │   │   ├── tickets/
│   │   │   │   ├── reply/route.ts     # POST — Reply to ticket
│   │   │   │   └── assign/route.ts    # POST — Assign ticket
│   │   │   ├── notifications/
│   │   │   │   └── send/route.ts      # POST — Send push notification
│   │   │   ├── config/
│   │   │   │   └── remote-config/route.ts  # GET/POST — Remote Config
│   │   │   ├── analytics/
│   │   │   │   ├── overview/route.ts  # GET — Dashboard analytics
│   │   │   │   ├── users/route.ts     # GET — User analytics
│   │   │   │   └── export/route.ts    # GET — Export analytics CSV
│   │   │   └── audit/
│   │   │       └── log/route.ts       # POST — Write audit log
│   │   │
│   │   ├── layout.tsx                 # Root layout (providers, fonts, theme)
│   │   ├── page.tsx                   # Root — redirect to /dashboard
│   │   └── globals.css                # Global styles + Tailwind directives
│   │
│   ├── components/
│   │   ├── ui/                        # shadcn/ui primitives (auto-generated)
│   │   │   ├── button.tsx
│   │   │   ├── card.tsx
│   │   │   ├── badge.tsx
│   │   │   ├── dialog.tsx
│   │   │   ├── sheet.tsx
│   │   │   ├── table.tsx
│   │   │   ├── tabs.tsx
│   │   │   ├── select.tsx
│   │   │   ├── input.tsx
│   │   │   ├── label.tsx
│   │   │   ├── textarea.tsx
│   │   │   ├── separator.tsx
│   │   │   ├── dropdown-menu.tsx
│   │   │   ├── command.tsx
│   │   │   ├── popover.tsx
│   │   │   ├── tooltip.tsx
│   │   │   ├── toast.tsx
│   │   │   ├── avatar.tsx
│   │   │   ├── switch.tsx
│   │   │   ├── checkbox.tsx
│   │   │   ├── skeleton.tsx
│   │   │   └── sonner.tsx
│   │   │
│   │   ├── layout/
│   │   │   ├── Sidebar.tsx            # Main navigation sidebar
│   │   │   ├── SidebarItem.tsx        # Individual nav item
│   │   │   ├── Header.tsx             # Top header bar
│   │   │   ├── AppShell.tsx           # Sidebar + Header + Main composition
│   │   │   ├── Breadcrumb.tsx         # Dynamic breadcrumbs
│   │   │   ├── SearchCommand.tsx       # Cmd+K command palette
│   │   │   └── UserMenu.tsx           # Admin user dropdown (profile, logout)
│   │   │
│   │   ├── dashboard/
│   │   │   ├── KpiCard.tsx            # Single KPI metric card
│   │   │   ├── KpiGrid.tsx            # Grid of KPI cards
│   │   │   ├── ActivityFeed.tsx        # Recent activity stream
│   │   │   ├── ActivityItem.tsx       # Single activity entry
│   │   │   ├── SignupChart.tsx        # User signup line chart
│   │   │   ├── CityBarChart.tsx       # Listings by city bar chart
│   │   │   ├── PriceHistogram.tsx     # Price distribution histogram
│   │   │   ├── RoleSplitChart.tsx     # Student/Owner donut chart
│   │   │   ├── RoomTypeChart.tsx      # Room type donut chart
│   │   │   ├── InquiryTrendChart.tsx  # Inquiries over time
│   │   │   ├── ReportTrendChart.tsx   # Reports over time
│   │   │   └── TicketTrendChart.tsx   # Tickets created vs resolved
│   │   │
│   │   ├── tables/
│   │   │   ├── DataTable.tsx          # Reusable TanStack Table wrapper
│   │   │   ├── DataTablePagination.tsx # Pagination controls
│   │   │   ├── DataTableToolbar.tsx   # Search + filter bar
│   │   │   ├── DataTableColumnHeader.tsx # Sortable column header
│   │   │   ├── DataTableRowActions.tsx  # Row action dropdown
│   │   │   ├── UserTable.tsx          # User-specific table config
│   │   │   ├── ListingTable.tsx       # Listing-specific table config
│   │   │   ├── RoommateTable.tsx      # Roommate post table config
│   │   │   ├── ReviewTable.tsx        # Review table config
│   │   │   ├── ReportTable.tsx        # Report table config
│   │   │   ├── TicketTable.tsx        # Ticket table config
│   │   │   ├── ChatTable.tsx          # Chat room table config
│   │   │   ├── AuditLogTable.tsx      # Audit log table config
│   │   │   └── AdminTable.tsx         # Admin user table config
│   │   │
│   │   ├── moderation/
│   │   │   ├── ReportCard.tsx         # Report card for kanban view
│   │   │   ├── ReportKanban.tsx       # Kanban board for reports
│   │   │   ├── ReportFilters.tsx      # Filter controls for reports
│   │   │   ├── ResolutionDialog.tsx   # Dialog for resolving reports
│   │   │   ├── BulkResolveDialog.tsx  # Bulk resolution dialog
│   │   │   └── SeverityBadge.tsx      # Report severity indicator
│   │   │
│   │   ├── tickets/
│   │   │   ├── TicketKanban.tsx       # Kanban board for tickets
│   │   │   ├── TicketCard.tsx         # Ticket card
│   │   │   ├── TicketThread.tsx       # Conversation thread
│   │   │   ├── TicketReplyForm.tsx    # Reply form
│   │   │   ├── CannedResponses.tsx    # Template selector
│   │   │   └── TicketStats.tsx        # Ticket statistics
│   │   │
│   │   ├── users/
│   │   │   ├── UserDetailHeader.tsx   # User profile header
│   │   │   ├── UserActivity.tsx       # User activity summary
│   │   │   ├── UserModeration.tsx     # Ban/suspend/delete controls
│   │   │   ├── UserSegmentPicker.tsx  # Segment selection
│   │   │   └── UserStats.tsx          # User statistics
│   │   │
│   │   ├── listings/
│   │   │   ├── ListingDetailHeader.tsx # Listing header with images
│   │   │   ├── ListingModeration.tsx   # Moderation controls
│   │   │   ├── ListingReports.tsx     # Reports against this listing
│   │   │   ├── ListingReviews.tsx     # Reviews for this listing
│   │   │   ├── ListingStats.tsx       # Listing statistics
│   │   │   ├── ListingEditForm.tsx    # Admin edit form
│   │   │   ├── ImageGallery.tsx       # Image gallery viewer
│   │   │   └── ListingMap.tsx         # Map preview
│   │   │
│   │   ├── analytics/
│   │   │   ├── DateRangePicker.tsx    # Date range selector
│   │   │   ├── CitySelector.tsx       # City filter
│   │   │   ├── MetricCard.tsx         # Single metric display
│   │   │   ├── FunnelChart.tsx        # Conversion funnel
│   │   │   ├── CohortTable.tsx        # Retention cohort table
│   │   │   ├── TrendLine.tsx          # Reusable trend line
│   │   │   └── AnalyticsExport.tsx    # Export controls
│   │   │
│   │   ├── config/
│   │   │   ├── RemoteConfigEditor.tsx  # Remote Config key-value editor
│   │   │   ├── CityManager.tsx        # City CRUD
│   │   │   ├── UniversityManager.tsx  # University CRUD
│   │   │   ├── GeofenceEditor.tsx     # Map polygon editor
│   │   │   ├── AmenityManager.tsx     # Amenity CRUD
│   │   │   └── ContentTypeManager.tsx # Room types, issue types
│   │   │
│   │   ├── notifications/
│   │   │   ├── NotificationComposer.tsx # Compose notification form
│   │   │   ├── RecipientPicker.tsx    # Segment/recipient selector
│   │   │   ├── NotificationPreview.tsx # Preview before sending
│   │   │   ├── NotificationHistory.tsx # History list
│   │   │   └── NotificationStats.tsx  # Delivery statistics
│   │   │
│   │   ├── premium/
│   │   │   ├── PremiumUserList.tsx    # Premium user management
│   │   │   ├── FeatureControls.tsx    # Featured listing controls
│   │   │   ├── BoostControls.tsx      # Boost listing controls
│   │   │   └── PricingInsights.tsx    # Price analytics
│   │   │
│   │   ├── shared/
│   │   │   ├── ConfirmDialog.tsx      # Reusable confirmation dialog
│   │   │   ├── DeleteDialog.tsx       # Delete confirmation
│   │   │   ├── StatusBadge.tsx        # Status indicator badge
│   │   │   ├── EmptyState.tsx         # Empty state placeholder
│   │   │   ├── ErrorBoundary.tsx      # Error boundary wrapper
│   │   │   ├── LoadingSpinner.tsx     # Loading indicator
│   │   │   ├── PageHeader.tsx         # Page title + description + actions
│   │   │   ├── SearchInput.tsx        # Debounced search input
│   │   │   ├── FilterPopover.tsx      # Reusable filter popover
│   │   │   ├── ExportButton.tsx       # CSV export trigger
│   │   │   ├── RelativeTime.tsx       # "2 hours ago" display
│   │   │   ├── CopyButton.tsx         # Copy-to-clipboard button
│   │   │   └── CityBadge.tsx          # City name badge with color
│   │   │
│   │   └── chat/
│   │       ├── ChatRoomList.tsx       # Chat room table
│   │       ├── ChatMessageView.tsx    # Message thread viewer
│   │       └── ChatMetadata.tsx       # Room metadata display
│   │
│   ├── lib/
│   │   ├── firebase.ts                # Firebase client SDK init
│   │   ├── firebase-admin.ts          # Firebase Admin SDK (API routes only)
│   │   ├── auth.ts                    # Auth helpers: getCurrentUser, isAdmin, getPermissions
│   │   ├── permissions.ts             # Permission constants + role-permission mapping
│   │   ├── firestore.ts               # Firestore query helpers
│   │   ├── analytics.ts               # Analytics aggregation helpers
│   │   ├── csv-export.ts              # CSV generation utilities
│   │   ├── audit.ts                   # Audit log writer
│   │   ├── notifications.ts           # FCM send helpers
│   │   ├── validation.ts              # Zod schemas for forms
│   │   └── utils.ts                   # General utilities (cn, formatPrice, etc.)
│   │
│   ├── hooks/
│   │   ├── useAuth.ts                 # Current admin auth state
│   │   ├── usePermissions.ts          # Permission checking hook
│   │   ├── useUsers.ts                # User list + CRUD
│   │   ├── useUser.ts                 # Single user detail
│   │   ├── useListings.ts             # Listing list + CRUD
│   │   ├── useListing.ts              # Single listing detail
│   │   ├── useReports.ts              # Reports list + resolve
│   │   ├── useReport.ts               # Single report detail
│   │   ├── useTickets.ts              # Ticket list + CRUD
│   │   ├── useTicket.ts               # Single ticket detail
│   │   ├── useReviews.ts              # Reviews list
│   │   ├── useRoommatePosts.ts        # Roommate post list
│   │   ├── useChatRooms.ts            # Chat room list
│   │   ├── useChatMessages.ts         # Messages for a room
│   │   ├── useAuditLogs.ts            # Audit log list
│   │   ├── useAdmins.ts               # Admin user management
│   │   ├── useDashboard.ts            # Dashboard data aggregation
│   │   ├── useAnalytics.ts            # Analytics data
│   │   ├── useRemoteConfig.ts         # Remote Config CRUD
│   │   ├── useLocationData.ts         # Location data CRUD
│   │   ├── useNotifications.ts        # Notification send + history
│   │   ├── useDebounce.ts             # Debounce hook for search
│   │   ├── usePagination.ts           # Pagination state
│   │   └── useConfirm.ts              # Confirmation dialog hook
│   │
│   ├── stores/
│   │   ├── auth-store.ts              # Admin auth state (Zustand)
│   │   ├── filter-store.ts            # Global filter state
│   │   └── ui-store.ts               # UI state (sidebar collapsed, theme)
│   │
│   ├── types/
│   │   ├── user.ts                    # User type definitions
│   │   ├── listing.ts                 # PgListing type definitions
│   │   ├── report.ts                  # Report type definitions
│   │   ├── ticket.ts                  # SupportTicket type definitions
│   │   ├── review.ts                  # Review type definitions
│   │   ├── chat.ts                    # ChatRoom + Message types
│   │   ├── roommate.ts               # RoommatePost type definitions
│   │   ├── admin.ts                   # Admin user types
│   │   ├── audit.ts                   # Audit log types
│   │   ├── notification.ts            # Notification types
│   │   ├── analytics.ts              # Analytics data types
│   │   ├── config.ts                  # Config types
│   │   └── api.ts                     # API response types
│   │
│   └── styles/
│       └── globals.css                # Tailwind directives + custom CSS
│
├── .env.local                         # Environment variables (gitignored)
├── .env.example                       # Template for env vars
├── .eslintrc.json
├── .prettierrc
├── tailwind.config.ts
├── next.config.ts
├── tsconfig.json
├── package.json
└── README.md
```

---

## 4. Environment & Configuration

### `.env.example`

```env
# Firebase Client SDK (used in browser)
NEXT_PUBLIC_FIREBASE_API_KEY=
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=
NEXT_PUBLIC_FIREBASE_PROJECT_ID=
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=
NEXT_PUBLIC_FIREBASE_APP_ID=

# Firebase Admin SDK (used in API routes only — NEVER exposed to browser)
FIREBASE_ADMIN_PROJECT_ID=
FIREBASE_ADMIN_CLIENT_EMAIL=
FIREBASE_ADMIN_PRIVATE_KEY=

# App Config
NEXT_PUBLIC_APP_URL=https://admin.staybuddy.app
NEXT_PUBLIC_APP_NAME=StayBuddy Admin

# Encryption (for sensitive audit log fields)
AUDIT_LOG_ENCRYPTION_KEY=
```

### `next.config.ts`

```ts
import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  // Only allow requests from the admin domain
  async headers() {
    return [
      {
        source: '/api/:path*',
        headers: [
          { key: 'X-Content-Type-Options', value: 'nosniff' },
          { key: 'X-Frame-Options', value: 'DENY' },
          { key: 'X-XSS-Protection', value: '1; mode=block' },
          { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
        ],
      },
    ]
  },
  // Redirect non-admin domains
  async redirects() {
    return []
  },
}

export default nextConfig
```

---

## 5. Authentication & Authorization

### 5.1 Auth Flow — Full Sequence

```
Admin visits admin.staybuddy.app
  │
  ├─ Not authenticated → /login page
  │    ├─ Enter email/password → Firebase Auth signInWithEmailAndPassword
  │    ├─ Or click "Sign in with Google" → GoogleAuthProvider
  │    ├─ On success → check admin status
  │    │    ├─ Firestore: get doc admins/{uid}
  │    │    ├─ Doc exists AND isActive == true → Store permissions → Redirect to /dashboard
  │    │    ├─ Doc exists AND isActive == false → /denied (suspended admin)
  │    │    └─ Doc does NOT exist → /denied (not an admin)
  │    └─ On failure → Show error toast
  │
  └─ Already authenticated → check admin status (same as above)
       └─ On every page load → verify token + permissions (middleware)
```

### 5.2 `lib/firebase.ts` — Client SDK Init

```ts
import { initializeApp, getApps } from 'firebase/app'
import { getAuth } from 'firebase/auth'
import { getFirestore } from 'firebase/firestore'

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
}

const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApps()[0]

export const auth = getAuth(app)
export const db = getFirestore(app)
```

### 5.3 `lib/firebase-admin.ts` — Admin SDK (API Routes Only)

```ts
import { cert, getApps, initializeApp } from 'firebase-admin/app'
import { getFirestore } from 'firebase-admin/firestore'
import { getAuth } from 'firebase-admin/auth'

if (getApps().length === 0) {
  initializeApp({
    credential: cert({
      projectId: process.env.FIREBASE_ADMIN_PROJECT_ID,
      clientEmail: process.env.FIREBASE_ADMIN_CLIENT_EMAIL,
      privateKey: process.env.FIREBASE_ADMIN_PRIVATE_KEY?.replace(/\\n/g, '\n'),
    }),
  })
}

export const adminDb = getFirestore()
export const adminAuth = getAuth()
```

### 5.4 `lib/permissions.ts` — Permission System

```ts
export const PERMISSIONS = {
  USERS_READ: 'users.read',
  USERS_MODIFY: 'users.modify',
  USERS_BAN: 'users.ban',
  LISTINGS_READ: 'listings.read',
  LISTINGS_MODIFY: 'listings.modify',
  LISTINGS_DELETE: 'listings.delete',
  REPORTS_READ: 'reports.read',
  REPORTS_RESOLVE: 'reports.resolve',
  REVIEWS_READ: 'reviews.read',
  REVIEWS_MODERATE: 'reviews.moderate',
  TICKETS_READ: 'tickets.read',
  TICKETS_RESPOND: 'tickets.respond',
  CHAT_READ: 'chat.read',
  CHAT_MONITOR: 'chat.monitor',
  ANALYTICS_READ: 'analytics.read',
  CONFIG_MANAGE: 'config.manage',
  NOTIFICATIONS_SEND: 'notifications.send',
  AUDIT_READ: 'audit.read',
  ADMINS_MANAGE: 'admins.manage',
  ROOMMATES_READ: 'roommates.read',
  ROOMMATES_MODIFY: 'roommates.modify',
  PREMIUM_MANAGE: 'premium.manage',
  HEALTH_READ: 'health.read',
} as const

export type Permission = (typeof PERMISSIONS)[keyof typeof PERMISSIONS]

export const ROLE_PERMISSIONS: Record<string, Permission[]> = {
  super_admin: Object.values(PERMISSIONS),  // All permissions
  moderator: [
    PERMISSIONS.USERS_READ, PERMISSIONS.USERS_BAN,
    PERMISSIONS.LISTINGS_READ, PERMISSIONS.LISTINGS_MODIFY, PERMISSIONS.LISTINGS_DELETE,
    PERMISSIONS.REPORTS_READ, PERMISSIONS.REPORTS_RESOLVE,
    PERMISSIONS.REVIEWS_READ, PERMISSIONS.REVIEWS_MODERATE,
    PERMISSIONS.ROOMMATES_READ, PERMISSIONS.ROOMMATES_MODIFY,
    PERMISSIONS.CHAT_READ,
    PERMISSIONS.AUDIT_READ,
  ],
  support_agent: [
    PERMISSIONS.USERS_READ,
    PERMISSIONS.LISTINGS_READ,
    PERMISSIONS.TICKETS_READ, PERMISSIONS.TICKETS_RESPOND,
    PERMISSIONS.REPORTS_READ,
  ],
  analyst: [
    PERMISSIONS.USERS_READ,
    PERMISSIONS.LISTINGS_READ,
    PERMISSIONS.REPORTS_READ,
    PERMISSIONS.REVIEWS_READ,
    PERMISSIONS.TICKETS_READ,
    PERMISSIONS.ANALYTICS_READ,
    PERMISSIONS.AUDIT_READ,
    PERMISSIONS.HEALTH_READ,
  ],
}
```

### 5.5 `lib/auth.ts` — Auth Helpers

```ts
import { onAuthStateChanged, User } from 'firebase/auth'
import { doc, getDoc } from 'firebase/firestore'
import { auth, db } from './firebase'
import { Permission } from './permissions'

export interface AdminUser {
  uid: string
  email: string | null
  role: string
  permissions: Permission[]
  isActive: boolean
}

// Client-side: get current admin from Firestore
export async function getCurrentAdmin(user: User): Promise<AdminUser | null> {
  const adminDoc = await getDoc(doc(db, 'admins', user.uid))
  if (!adminDoc.exists()) return null
  const data = adminDoc.data()
  if (!data.isActive) return null
  return {
    uid: user.uid,
    email: user.email,
    role: data.role,
    permissions: data.permissions,
    isActive: data.isActive,
  }
}

// Server-side (API routes): verify ID token + get admin doc
export async function verifyAdmin(idToken: string): Promise<AdminUser> {
  const { adminAuth, adminDb } = await import('./firebase-admin')
  const decoded = await adminAuth.verifyIdToken(idToken)
  const adminDoc = await adminDb.collection('admins').doc(decoded.uid).get()
  if (!adminDoc.exists) throw new Error('Not an admin')
  const data = adminDoc.data()!
  if (!data.isActive) throw new Error('Admin account suspended')
  return {
    uid: decoded.uid,
    email: decoded.email,
    role: data.role,
    permissions: data.permissions,
    isActive: data.isActive,
  }
}

// Check if admin has a specific permission
export function hasPermission(admin: AdminUser, permission: Permission): boolean {
  return admin.permissions.includes(permission)
}

// Middleware helper: extract token from request
export function extractToken(request: Request): string | null {
  const authHeader = request.headers.get('Authorization')
  if (!authHeader?.startsWith('Bearer ')) return null
  return authHeader.slice(7)
}
```

### 5.6 `middleware.ts` — Route Protection

```ts
import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

// Routes that require authentication
const PROTECTED_ROUTES = ['/dashboard', '/users', '/listings', '/reports',
  '/tickets', '/reviews', '/chats', '/analytics', '/notifications',
  '/config', '/premium', '/health', '/audit', '/admins']

// Routes that require specific permissions
const PERMISSION_ROUTES: Record<string, string> = {
  '/admins': 'admins.manage',
  '/config': 'config.manage',
  '/analytics': 'analytics.read',
  '/notifications': 'notifications.send',
  '/premium': 'premium.manage',
  '/health': 'health.read',
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl

  // Allow auth pages, API routes, static files
  if (pathname.startsWith('/login') || pathname.startsWith('/api') ||
      pathname.startsWith('/_next') || pathname === '/') {
    return NextResponse.next()
  }

  // Check if route is protected (client-side auth check happens in layout)
  const isProtected = PROTECTED_ROUTES.some(route => pathname.startsWith(route))
  if (isProtected) {
    // Middleware can't check Firestore — redirect to login if no session cookie
    // Actual permission check happens in the (dashboard)/layout.tsx
    return NextResponse.next()
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
}
```

### 5.7 Admin Bootstrap — Creating the First Admin

**Method 1: Firebase Console (for initial setup)**

Manually create a document in the `admins` collection:

```json
{
  "email": "aasav@staybuddy.app",
  "role": "super_admin",
  "permissions": ["users.read", "users.modify", "users.ban", "listings.read",
    "listings.modify", "listings.delete", "reports.read", "reports.resolve",
    "reviews.read", "reviews.moderate", "tickets.read", "tickets.respond",
    "chat.read", "chat.monitor", "analytics.read", "config.manage",
    "notifications.send", "audit.read", "admins.manage", "roommates.read",
    "roommates.modify", "premium.manage", "health.read"],
  "isActive": true,
  "createdAt": "2026-07-16T00:00:00Z",
  "lastActiveAt": "2026-07-16T00:00:00Z"
}
```

The document ID must be the Firebase Auth UID of the admin user.

**Method 2: API Route (for adding subsequent admins)**

`POST /api/admin/create` — only callable by super_admin.

---

## 6. Firestore Schema — Full Specification

### New Collections

#### `admins/{userId}`

```
{
  email: string,                    // Admin's email
  displayName: string,              // Admin's display name
  role: string,                     // "super_admin" | "moderator" | "support_agent" | "analyst"
  permissions: string[],            // Array of permission keys
  isActive: boolean,                // Soft-disable without deleting
  createdAt: Timestamp,             // When admin access was granted
  createdBy: string,                // UID of admin who created this
  lastActiveAt: Timestamp,          // Last login timestamp
  lastActiveIp: string,             // Last login IP
  notes: string,                    // Internal notes about this admin
}
```

#### `audit_logs/{logId}`

```
{
  adminId: string,                  // Who performed the action
  adminEmail: string,               // For display (denormalized)
  adminRole: string,                // Role at time of action
  action: string,                   // e.g. "user.ban", "listing.deactivate"
  category: string,                 // "user" | "listing" | "report" | "ticket" |
                                   // "review" | "config" | "admin" | "notification" |
                                   // "chat" | "system"
  targetType: string,               // Type of entity affected
  targetId: string,                 // ID of entity affected
  targetSummary: string,            // Human-readable: "PG listing in Vadodara — ₹8000"
  details: {                        // Change details
    before: any,                    // Previous state (for updates)
    after: any,                     // New state (for updates)
    reason: string,                 // Why this action was taken
    metadata: map,                  // Extra context
  },
  ipAddress: string,                // Admin's IP
  userAgent: string,                // Browser user agent
  timestamp: Timestamp,             // When the action occurred
  sessionId: string,                // For grouping related actions
}
```

**Index:** Composite index on `(adminId, timestamp DESC)`, `(action, timestamp DESC)`, `(category, timestamp DESC)`, `(targetId)`

#### `notifications_sent/{notifId}`

```
{
  title: string,
  body: string,
  channel: string,                  // "messages" | "listings" | "roommates" | "general"
  imageUrl: string | null,
  deepLink: string | null,          // e.g. "staybuddy://chat/abc123"
  recipientSegment: string,         // "all" | "students" | "owners" | "city:vadodara" | etc.
  recipientCount: number,
  recipientUserIds: string[],       // Actual UIDs (for dedup/retry)
  sentBy: string,                   // Admin UID
  sentByEmail: string,
  sentAt: Timestamp,
  scheduledFor: Timestamp | null,   // For scheduled sends
  status: string,                   // "sent" | "scheduled" | "failed" | "cancelled"
  errorCount: number,               // Failed deliveries
  errorDetails: string | null,      // Error message if failed
}
```

#### `canned_responses/{responseId}`

```
{
  title: string,                    // Template name
  body: string,                     // Response template text
  category: string,                 // "listing" | "user" | "ticket" | "general"
  createdBy: string,                // Admin UID
  usageCount: number,               // How many times used
  createdAt: Timestamp,
  isActive: boolean,
}
```

#### `moderation_notes/{noteId}`

```
{
  targetType: string,               // "user" | "listing" | "report" | "ticket"
  targetId: string,
  adminId: string,
  adminEmail: string,
  note: string,                     // Internal note (not visible to users)
  isInternal: boolean,              // Always true — admin-only notes
  createdAt: Timestamp,
}
```

### Modified Collections

#### `users/{userId}` — Add Fields

```
+ isActive: boolean = true          // false = banned
+ bannedAt: Timestamp | null
+ banReason: string | null
+ bannedBy: string | null           // Admin UID
+ lastLoginAt: Timestamp | null
+ loginCount: number = 0
+ totalInquiries: number = 0
+ totalReviews: number = 0
+ profileCompleted: boolean = false  // Has finished registration
```

#### `reports/{reportId}` — Add Fields

```
+ status: string = "pending"        // "pending" | "under_review" | "resolved" | "dismissed"
+ severity: string = "low"          // "low" | "medium" | "high" | "critical"
+ resolvedBy: string | null         // Admin UID
+ resolvedAt: Timestamp | null
+ resolutionAction: string | null   // "dismissed" | "deactivated" | "deleted" | "warned" | "banned"
+ resolutionNote: string | null
+ adminNotes: string | null
+ reportType: string = "listing"    // "listing" | "user" | "review" | "message"
+ reportedUserId: string | null     // For user reports (existing field used differently)
+ reportedReviewId: string | null   // For review reports
+ reportedMessageId: string | null  // For message reports
```

#### `support_tickets/{ticketId}` — Add Fields

```
+ assignedTo: string | null         // Admin UID
+ assignedAt: Timestamp | null
+ priority: string = "medium"       // "low" | "medium" | "high" | "urgent"
+ lastReplyAt: Timestamp | null
+ lastReplyBy: string | null        // Admin UID
+ replyCount: number = 0
+ tags: string[] = []               // Custom tags for categorization
```

#### `pg_listings/{listingId}` — Add Fields

```
+ reviewStatus: string = "approved" // "pending_review" | "approved" | "rejected"
+ rejectionReason: string | null
+ reviewedBy: string | null         // Admin UID
+ reviewedAt: Timestamp | null
+ qualityScore: number = 0          // Auto-calculated 0-100
+ lastViewedAt: Timestamp | null    // Last time admin viewed
+ adminNotes: string | null
+ featuredBy: string | null         // Admin who featured it
+ boostedBy: string | null          // Admin who boosted it
```

#### `chats/{chatId}` — Add Fields

```
+ flagged: boolean = false          // Flagged for review
+ flagReason: string | null
+ flaggedBy: string | null          // Admin UID
+ flaggedAt: Timestamp | null
+ messageCount: number = 0          // Denormalized for quick display
```

---

## 7. Security Rules — Complete Rewrite

```rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // ========== HELPER FUNCTIONS ==========

    // Default: deny everything
    match /{document=**} {
      allow read, write: if false;
    }

    // Is the requesting user an active admin?
    function isAdmin() {
      return request.auth != null &&
        exists(/databases/$(database)/documents/admins/$(request.auth.uid)) &&
        get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.isActive == true;
    }

    // Does the admin have a specific permission?
    function hasPermission(permission) {
      return isAdmin() &&
        permission in get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.permissions;
    }

    // Is this the user's own document?
    function isOwner(userId) {
      return request.auth != null && request.auth.uid == userId;
    }

    // ========== ADMINS COLLECTION ==========

    match /admins/{adminId} {
      // Any admin can read the admins list (for UI display)
      allow read: if isAdmin();
      // Only super_admins can create/update/delete admin records
      allow create: if hasPermission('admins.manage');
      allow update: if hasPermission('admins.manage');
      allow delete: if hasPermission('admins.manage');
    }

    // ========== USERS COLLECTION ==========

    match /users/{userId} {
      // Users can read their own profile
      allow read: if isOwner(userId);
      // Admins can read all users
      allow read: if hasPermission('users.read');
      // Users can update their own profile
      allow update: if isOwner(userId);
      // Admins can update any user (for moderation)
      allow update: if hasPermission('users.modify') || hasPermission('users.ban');
      // Users can create their own profile during registration
      allow create: if isOwner(userId);

      // Saved searches subcollection
      match /saved_searches/{searchId} {
        allow read, write: if isOwner(userId);
      }
    }

    // ========== PG LISTINGS ==========

    match /pg_listings/{listingId} {
      // Anyone authenticated can read listings (public marketplace)
      allow read: if request.auth != null;
      // Admins can read all listings
      allow read: if hasPermission('listings.read');
      // Owners can create/update their own listings
      allow create: if request.auth != null;
      allow update: if request.auth != null;
      // Admins can delete any listing
      allow delete: if hasPermission('listings.delete');

      // Reviews subcollection
      match /reviews/{reviewId} {
        allow read: if request.auth != null;
        allow create: if request.auth != null;
        allow update: if request.auth != null || hasPermission('reviews.moderate');
        allow delete: if hasPermission('reviews.moderate');
      }
    }

    // ========== ROOMMATE POSTS ==========

    match /roommate_posts/{postId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null || hasPermission('roommates.modify');
      allow delete: if hasPermission('roommates.modify');
    }

    // ========== CHATS ==========

    match /chats/{chatId} {
      allow read, write: if request.auth != null;
      match /messages/{messageId} {
        allow read, write: if request.auth != null;
        // Admins with chat.monitor can also read for investigations
        allow read: if hasPermission('chat.monitor');
      }
    }

    // ========== FAVORITES ==========

    match /favorites/{favoriteId} {
      allow read, write: if request.auth != null;
    }

    // ========== INQUIRIES ==========

    match /inquiries/{inquiryId} {
      allow read, write: if request.auth != null;
    }

    // ========== REPORTS ==========

    match /reports/{reportId} {
      // Users can create reports
      allow create: if request.auth != null;
      // Users can read their own reports
      allow read: if request.auth != null &&
        resource.data.reporterId == request.auth.uid;
      // Admins can read all reports
      allow read: if hasPermission('reports.read');
      // Admins can resolve reports
      allow update: if hasPermission('reports.resolve');
    }

    // ========== SUPPORT TICKETS ==========

    match /support_tickets/{ticketId} {
      // Users can create tickets
      allow create: if request.auth != null;
      // Users can read their own tickets
      allow read: if request.auth != null &&
        resource.data.userId == request.auth.uid;
      // Admins can read all tickets
      allow read: if hasPermission('tickets.read');
      // Admins can update (reply, assign, change status)
      allow update: if hasPermission('tickets.respond');
    }

    // ========== METADATA ==========

    match /metadata/ticket_counter {
      allow read, write: if request.auth != null;
    }

    // ========== LOCATION DATA ==========

    match /location_data/{docId} {
      allow read: if true;  // Publicly readable (needed before login)
      allow write: if hasPermission('config.manage');
    }

    // ========== NEW: AUDIT LOGS ==========

    match /audit_logs/{logId} {
      allow read: if hasPermission('audit.read');
      allow create: if isAdmin();
      // Audit logs are immutable — no update/delete allowed
    }

    // ========== NEW: CANNED RESPONSES ==========

    match /canned_responses/{responseId} {
      allow read: if isAdmin();
      allow create, update: if isAdmin();
      allow delete: if hasPermission('admins.manage');
    }

    // ========== NEW: MODERATION NOTES ==========

    match /moderation_notes/{noteId} {
      allow read: if isAdmin();
      allow create: if isAdmin();
      allow update: if isAdmin();
    }

    // ========== NEW: NOTIFICATIONS SENT ==========

    match /notifications_sent/{notifId} {
      allow read: if hasPermission('notifications.send');
      allow create: if hasPermission('notifications.send');
    }
  }
}
```

---

## 8. Layout & Navigation

### 8.1 Sidebar Structure

```
┌─────────────────────────────────┐
│  🏠 StayBuddy Admin            │  ← Logo + brand
├─────────────────────────────────┤
│                                 │
│  📊 Dashboard                   │  ← /dashboard
│                                 │
│  ── MANAGEMENT ─────────────── │
│  👥 Users                       │  ← /users
│  🏢 Listings                    │  ← /listings
│     📋 Pending Approval         │  ← /listings/pending
│  🏠 Roommate Posts              │  ← /roommates
│  ⭐ Reviews                     │  ← /reviews
│  💬 Chat Rooms                  │  ← /chats
│                                 │
│  ── MODERATION ────────────── │
│  🚨 Reports                     │  ← /reports  (with badge count)
│  🎫 Support Tickets             │  ← /tickets  (with badge count)
│                                 │
│  ── INSIGHTS ──────────────── │
│  📈 Analytics                   │  ← /analytics
│  📋 Audit Logs                  │  ← /audit
│                                 │
│  ── OPERATIONS ────────────── │
│  🔔 Notifications               │  ← /notifications
│  ⚙️  Configuration              │  ← /config
│  💎 Premium & Monetization      │  ← /premium
│  🏥 System Health               │  ← /health
│                                 │
│  ── ADMINISTRATION ────────── │
│  🛡️  Admin Users                │  ← /admins
│                                 │
├─────────────────────────────────┤
│  👤 Aasav Chauhan               │  ← Current admin
│     super_admin                 │
│     ⚙️ Settings  🚪 Logout      │
└─────────────────────────────────┘
```

### 8.2 Responsive Behavior

- **Desktop (≥1024px):** Full sidebar visible, collapsible to icon-only
- **Tablet (768-1023px):** Sidebar collapsed to icons, expandable on hover
- **Mobile (<768px):** Sidebar hidden, hamburger menu opens as sheet/drawer

### 8.3 Header Bar

```
┌──────────────────────────────────────────────────────────┐
│  ☰  / Dashboard / Users                    🔍  🔔  👤  │
│  ↑ breadcrumbs          ↑ page title    search  notif  admin
└──────────────────────────────────────────────────────────┘
```

- **Breadcrumb:** Dynamic, clickable path segments
- **Page title:** Current page name
- **Search (🔍):** Opens Cmd+K command palette for global search
- **Notifications (🔔):** Bell icon with unread count badge
- **Admin avatar:** Dropdown with profile, settings, logout

### 8.4 Command Palette (Cmd+K)

Global search accessible from any page:

```
┌────────────────────────────────────────────┐
│  🔍 Type a command or search...            │
├────────────────────────────────────────────┤
│  Navigation                                 │
│    → Dashboard                              │
│    → Users                                  │
│    → Listings                               │
│    ...                                      │
│  Quick Actions                              │
│    → Send Notification                      │
│    → Toggle Maintenance Mode                │
│    → Export Users CSV                       │
│  Search                                     │
│    → Search users by name/email             │
│    → Search listings by title/city          │
│    → Search tickets by ID                   │
└────────────────────────────────────────────┘
```

---

## 9. Page Specifications — Every Screen

### 9.1 Login Page (`/login`)

**Layout:** Centered card on a subtle background with the StayBuddy logo.

**Components:**
- StayBuddy logo (SVG)
- "Admin Panel" subtitle
- Email input field
- Password input field
- "Sign In" button (primary)
- "Sign in with Google" button (outline, with Google icon)
- "Forgot password?" link
- Error toast on failure
- Loading state during auth

**Behavior:**
1. On submit → `signInWithEmailAndPassword(auth, email, password)`
2. On Google → `signInWithPopup(auth, googleProvider)`
3. On success → call `getCurrentAdmin(user)`
4. If admin doc exists and `isActive` → store in `auth-store` → redirect to `/dashboard`
5. If not → redirect to `/denied`

### 9.2 Access Denied Page (`/denied`)

**Layout:** Centered message with illustration.

**Content:**
- "Access Denied" heading
- "You don't have permission to access the admin panel." or "Your admin account has been suspended."
- "Sign in with a different account" button
- "Contact super admin" link

### 9.3 Dashboard (`/dashboard`)

**Layout:** Full-width with responsive grid.

**Section 1: KPI Cards (top row, 4 columns)**
```
┌──────────┬──────────┬──────────┬──────────┐
│ 👥 Total │ 🏢 Total │ 🚨 Open  │ 🎫 Open  │
│  Users   │ Listings │ Reports  │ Tickets  │
│  1,247   │   342    │    12    │    8     │
│ +23 today│ +5 today │ 3 urgent │ 2 urgent │
└──────────┴──────────┴──────────┴──────────┘
```

Each KPI card shows:
- Icon + label
- Large number (animated counter on load)
- Trend indicator (up/down arrow + percentage vs last period)
- Sub-label (today's count, or period comparison)

**KPI Metrics:**
| Card | Value | Sub-text | Trend |
|---|---|---|---|
| Total Users | count | "+X today" | vs yesterday |
| Active Listings | count | "X% occupancy" | vs last week |
| Open Reports | count | "X critical" | vs last week |
| Open Tickets | count | "X urgent" | vs last week |
| Total Inquiries | count | "X this week" | vs last week |
| Roommate Posts | count | "X active" | vs last week |
| New Users (7d) | count | "vs X last week" | % change |
| Revenue (MRR) | ₹ amount | "X premium users" | vs last month |

**Section 2: Charts Grid (2 columns)**
```
┌────────────────────────┬────────────────────────┐
│  User Signups (30d)    │  Listings by City       │
│  [Line Chart]          │  [Horizontal Bar]       │
├────────────────────────┼────────────────────────┤
│  Inquiries Trend       │  Room Type Distribution  │
│  [Area Chart]          │  [Donut Chart]           │
├────────────────────────┼────────────────────────┤
│  Reports Trend         │  User Role Split          │
│  [Bar Chart]           │  [Donut Chart]            │
└────────────────────────┴────────────────────────┘
```

**Section 3: Activity Feed (full width)**
```
┌──────────────────────────────────────────────────────────┐
│  Recent Activity                                    ⚙️   │
├──────────────────────────────────────────────────────────┤
│  🟢 New user registered    Aarav Patel — student — Vadodara  2m ago │
│  🏢 New listing added      "2BHK near MSAJ College" — ₹9,500  15m ago │
│  🚨 Report filed           Against "Sunrise PG" — Spam        1h ago  │
│  🎫 Ticket created         TKT-1042 — Account Issue           2h ago  │
│  ⚠️ Listing auto-deactivated "Quick Stay PG" — 5 reports       3h ago │
│  ✅ Ticket resolved        TKT-1039 — Payment Issue            5h ago │
│  👤 User banned            Spam account — 3 reports against    1d ago │
└──────────────────────────────────────────────────────────┘
```

**Data Sources:**
- KPIs: Firestore count queries, cached for 60 seconds
- Charts: Aggregation queries on Firestore, cached for 5 minutes
- Activity: Real-time listener on `audit_logs` collection, limited to 20

### 9.4 Users Page (`/users`)

**Layout:** Full-width table with toolbar.

**Toolbar:**
```
┌────────────────────────────────────────────────────────────────┐
│  🔍 Search users...          [Role ▾] [City ▾] [Status ▾]    │
│  [Gender ▾] [Premium ▾]  [Date Range]    [📥 Export] [Bulk ▾]│
└────────────────────────────────────────────────────────────────┘
```

**Table Columns:**
| Column | Width | Sortable | Notes |
|---|---|---|---|
| Checkbox | 40px | No | For bulk selection |
| Avatar | 48px | No | Profile image or initials |
| Name | auto | Yes | Clickable → user detail |
| Email | auto | Yes | |
| Phone | 140px | No | |
| Role | 100px | Yes | Badge: student (blue) / owner (green) |
| City | 120px | Yes | |
| Joined | 120px | Yes | Relative time |
| Status | 100px | Yes | Active (green) / Banned (red) |
| Premium | 80px | Yes | Free / Pro / Elite badge |
| Actions | 80px | No | ⋮ dropdown menu |

**Row Actions Dropdown:**
- View Profile
- Edit User
- Ban User / Unban User
- Verify as Owner
- Grant Premium
- Delete Account
- View Reports
- View Audit Logs

**Bulk Actions (when rows selected):**
- Export Selected (CSV)
- Ban Selected
- Unban Selected
- Delete Selected

**Empty State:** "No users found matching your filters."

**Pagination:** 25/50/100 per page, with page jump.

### 9.5 User Detail Page (`/users/[userId]`)

**Layout:** Two-column (main content + sidebar).

```
┌─────────────────────────────────┬──────────────────┐
│  ← Back to Users                │                  │
│                                 │  Quick Actions   │
│  ┌──────────────────────────┐  │  ┌──────────────┐│
│  │  [Profile Image]         │  │  │ ✏️ Edit      ││
│  │  Aarav Patel             │  │  │ 🚫 Ban       ││
│  │  student · Vadodara      │  │  │ 🔑 Verify    ││
│  │  aarav@email.com         │  │  │ 💎 Premium   ││
│  │  +91 98765 43210         │  │  │ 🗑️ Delete    ││
│  │  Joined: 2 weeks ago     │  │  └──────────────┘│
│  │  Status: ✅ Active        │  │                  │
│  └──────────────────────────┘  │  User Stats      │
│                                 │  ┌──────────────┐│
│  [About] [Activity] [Moderation]│  │ Listings: 0  ││
│                                 │  │ Inquiries: 3 ││
│  About Tab:                     │  │ Reviews: 1   ││
│  Bio: "...", Preferences: ...   │  │ Favorites: 5 ││
│                                 │  │ Chats: 2     ││
│  Activity Tab:                  │  │ Reports: 0   ││
│  - Recent inquiries             │  └──────────────┘│
│  - Recent reviews               │                  │
│  - Chat rooms                   │  Admin Notes     │
│  - Favorites                    │  ┌──────────────┐│
│                                 │  │ Add note...  ││
│  Moderation Tab:                │  └──────────────┘│
│  - Reports by this user         │                  │
│  - Reports against this user    │  Audit Trail     │
│  - Ban history                  │  ┌──────────────┐│
│  - Admin actions taken          │  │ Banned by... ││
│                                 │  │ 2 days ago   ││
└─────────────────────────────────┴──────────────────┘
```

**Ban Dialog:**
```
┌─────────────────────────────────────┐
│  🚫 Ban User                        │
│                                     │
│  Ban Aarav Patel from StayBuddy?    │
│  This will:                         │
│  • Deactivate their account         │
│  • Hide their listings              │
│  • Prevent login                    │
│  • Remove from chat rooms           │
│                                     │
│  Reason: [dropdown]                 │
│  Details: [textarea]                │
│                                     │
│  Duration:                          │
│  ○ Permanent                        │
│  ○ Temporary (until [date picker])  │
│                                     │
│       [Cancel]  [Ban User]          │
└─────────────────────────────────────┘
```

### 9.6 Listings Page (`/listings`)

**Layout:** Full-width table (same pattern as Users).

**Toolbar:**
```
┌────────────────────────────────────────────────────────────────┐
│  🔍 Search listings...    [City ▾] [Status ▾] [Room ▾]       │
│  [Gender ▾] [Verified ▾] [Price Range]  [📥 Export] [Bulk ▾] │
└────────────────────────────────────────────────────────────────┘
```

**Table Columns:**
| Column | Width | Sortable |
|---|---|---|
| Checkbox | 40px | No |
| Image | 64px | No | Thumbnail, click to expand |
| Title | auto | Yes | Clickable → listing detail |
| Owner | 140px | Yes | Name, clickable |
| City | 100px | Yes | |
| Price | 100px | Yes | ₹ formatted, tertiary color |
| Room Type | 100px | Yes | Badge |
| Status | 100px | Yes | Active/Inactive/Pending |
| Verified | 80px | Yes | ✓/✗ |
| Reports | 80px | Yes | Count, red if > 0 |
| Rating | 80px | Yes | ⭐ X.X |
| Views | 80px | Yes | |
| Created | 100px | Yes | Relative time |
| Actions | 80px | No | ⋮ dropdown |

**Row Actions:**
- View Details
- Edit Listing
- Verify Listing
- Feature Listing (set `featuredUntil`)
- Boost Listing (set `boostExpiresAt`)
- Activate / Deactivate
- Delete Listing
- View Owner
- View Reports

### 9.7 Listing Detail Page (`/listings/[listingId]`)

**Layout:** Two-column.

**Main Content:**
- Image gallery (horizontal scroll, click to fullscreen)
- Title, description, price (tertiary color), deposit
- Location with map preview (osmdroid/Leaflet)
- Room type, gender allowed, available beds
- Amenities as chips/badges
- Lifestyle preferences
- Owner info card (avatar, name, verified badge, link to user)
- Reviews section (list with ratings)

**Sidebar:**
- Status badge (Active/Inactive/Pending)
- Quick actions (same as row actions)
- Listing stats (views, inquiries, favorites, chat initiations)
- Reports against this listing
- Admin notes
- Audit trail for this listing

### 9.8 Reports Page (`/reports`)

**Layout:** Two views toggleable — Kanban and Table.

**Kanban View (default):**
```
┌──────────────┬──────────────┬──────────────┬──────────────┐
│  ⏳ Pending  │  🔍 Review   │  ✅ Resolved  │  🗑️ Dismissed│
│   (12)       │   (3)        │   (45)        │   (8)        │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ ┌──────────┐ │ ┌──────────┐ │ ┌──────────┐ │              │
│ │Report #1 │ │ │Report #8 │ │ │Report #3 │ │              │
│ │Spam      │ │ │Fake listing││ │Resolved  │ │              │
│ │Sunrise PG│ │ │2BHK Lite │ │ │Quick Stay│ │              │
│ │2h ago    │ │ │30m ago   │ │ │1d ago    │ │              │
│ │[Review]  │ │ │[Review]  │ │ │          │ │              │
│ └──────────┘ │ └──────────┘ │ └──────────┘ │              │
│ ┌──────────┐ │              │ ┌──────────┐ │              │
│ │Report #2 │ │              │ │Report #4 │ │              │
│ │Inaccurate│ │              │ │Resolved  │ │              │
│ │Price PG  │ │              │ │City Home │ │              │
│ │5h ago    │ │              │ │2d ago    │ │              │
│ └──────────┘ │              │ └──────────┘ │              │
└──────────────┴──────────────┴──────────────┴──────────────┘
```

**Table View:**
Standard DataTable with columns: ID, Type, Reported Content, Reason, Reporter, Status, Severity, Filed, Actions.

**Report Card:**
```
┌─────────────────────────────┐
│  🚨 Spam listing            │  ← Reason
│  Reported: Sunrise PG       │  ← Content (clickable)
│  By: User #a3f2 (anon)     │  ← Reporter
│  Filed: 2 hours ago         │
│  Severity: 🔴 High          │  ← Auto-assigned
│  Reports on listing: 3      │
│                             │
│  [👁️ Review] [✅ Dismiss]    │  ← Quick actions
└─────────────────────────────┘
```

**Report Detail Slide-over:**
- Full report info
- Reported content preview (listing/post/user card)
- Reporter info
- All other reports on same content
- Resolution actions with confirmation
- Admin notes
- Audit trail

**Resolution Actions:**
```
┌─────────────────────────────────────────┐
│  Resolve Report #rep_abc123             │
│                                         │
│  Action:                                │
│  ○ Dismiss (no action on content)       │
│  ○ Deactivate Content                   │
│  ○ Delete Content                       │
│  ○ Warn Owner/Poster                    │
│  ○ Ban User                             │
│  ○ Escalate to Super Admin              │
│                                         │
│  Note (visible in audit log):           │
│  [textarea]                             │
│                                         │
│  Notify reporter of resolution?  [✓]    │
│                                         │
│       [Cancel]  [Resolve Report]        │
└─────────────────────────────────────────┘
```

### 9.9 Support Tickets Page (`/tickets`)

**Layout:** Kanban (default) + Table toggle.

**Kanban:**
```
┌──────────────┬──────────────┬──────────────┐
│  📥 Pending  │  🔄 In Prog  │  ✅ Resolved  │
│   (5)        │   (3)        │   (89)        │
├──────────────┼──────────────┼──────────────┤
│ ┌──────────┐ │ ┌──────────┐ │              │
│ │TKT-1042  │ │ │TKT-1040  │ │              │
│ │Account   │ │ │Payment   │ │              │
│ │Urgent 🔴 │ │ │Medium 🟡  │ │              │
│ │2h ago    │ │ │1d ago    │ │              │
│ └──────────┘ │ └──────────┘ │              │
└──────────────┴──────────────┴──────────────┘
```

**Ticket Detail:**
```
┌──────────────────────────────────────────────────────┐
│  ← Back to Tickets     TKT-1042     [🔄 In Progress]│
│                                                      │
│  ┌────────────────────────────────────────────────┐  │
│  │  Subject: Account login issue                   │  │
│  │  From: aarav@email.com                         │  │
│  │  Priority: 🔴 Urgent  |  Type: Account Issues  │  │
│  │  Assigned to: Aasav Chauhan                    │  │
│  │  Created: 2 hours ago                          │  │
│  └────────────────────────────────────────────────┘  │
│                                                      │
│  ── Conversation ────────────────────────────────── │
│                                                      │
│  [User] Aarav Patel — 2h ago                        │
│  "I can't log in to my account. I tried resetting   │
│   my password but didn't receive the email."         │
│                                                      │
│  [System] Ticket assigned to Aasav — 1h ago          │
│                                                      │
│  [Admin] Aasav Chauhan — 45m ago                     │
│  "Hi Aarav, I've checked your account. The email    │
│   was sent to aarav@email.com. Can you check your   │
│   spam folder?"                                      │
│                                                      │
│  ── Reply ───────────────────────────────────────── │
│  ┌────────────────────────────────────────────────┐  │
│  │ [Templates ▾]                                  │  │
│  │ [textarea with rich text]                      │  │
│  │                                    [Send Reply] │  │
│  └────────────────────────────────────────────────┘  │
│                                                      │
│  ── Actions ─────────────────────────────────────── │
│  [Change Status ▾] [Assign ▾] [Set Priority ▾]      │
│  [Add Tag] [Close Ticket] [View User Profile]       │
└──────────────────────────────────────────────────────┘
```

### 9.10 Reviews Page (`/reviews`)

**Table Columns:** Listing (title), Reviewer (name), Rating (stars), Comment (truncated), Date, Actions.

**Actions:** View Listing, View Reviewer, Delete Review, Flag Review.

**Rating Analytics sidebar:**
- Average by city
- Distribution histogram
- Suspicious patterns alert

### 9.11 Chat Rooms Page (`/chats`)

**Table Columns:** Room ID, Participants, Listing/Post, Last Message (truncated), Messages, Last Active, Status, Actions.

**Chat Detail:** Read-only message thread (requires `chat.monitor` permission). Admin can:
- View full message history
- Flag the chat room
- Delete specific messages (soft-delete)
- View participant profiles

### 9.12 Analytics Pages

**Main Analytics (`/analytics`):**

Tabbed view:
1. **Overview** — Key metrics with date range picker
2. **Users** — Registration trends, retention, cohorts
3. **Listings** — Supply metrics, price trends, occupancy
4. **Revenue** — MRR, ARPU, conversion (future)
5. **Engagement** — Funnel, session metrics, feature adoption

**Date Range Picker:** Last 7 days / 30 days / 90 days / Custom range.

**City Filter:** All cities or specific city.

**Export:** Download any chart/dataset as CSV.

### 9.13 Notifications Page (`/notifications`)

**Composer:**
```
┌─────────────────────────────────────────────────────┐
│  📤 Send Push Notification                          │
│                                                     │
│  Title:    [________________________]               │
│  Body:     [________________________]               │
│            [________________________]               │
│  Channel:  [General ▾]                              │
│  Image URL:[________________________] (optional)    │
│  Deep Link:[________________________] (optional)    │
│                                                     │
│  Recipients:                                        │
│  ○ All Users (1,247)                                │
│  ○ All Students (892)                               │
│  ○ All Owners (355)                                 │
│  ○ Users in City: [Vadodara ▾] (234)               │
│  ○ Inactive Users (30+ days) (156)                  │
│  ○ Premium Users (42)                               │
│  ○ Custom Segment                                   │
│    └ Filters: [Role ▾] [City ▾] [Joined ▾]        │
│                                                     │
│  Schedule:                                          │
│  ○ Send Now                                         │
│  ○ Schedule: [date picker] [time picker]            │
│                                                     │
│  ┌────────────────────────────────────────────────┐ │
│  │  Preview                                        │ │
│  │  ┌──────────────────────────┐                  │ │
│  │  │ 🔔 StayBuddy            │                  │ │
│  │  │ Title here               │                  │ │
│  │  │ Body text here...        │                  │ │
│  │  └──────────────────────────┘                  │ │
│  └────────────────────────────────────────────────┘ │
│                                                     │
│       [Save as Template]  [Send Notification]       │
└─────────────────────────────────────────────────────┘
```

### 9.14 Configuration Pages

**Remote Config (`/config/remote-config`):**

Table of all parameters:
| Parameter | Value | Type | Description | Actions |
|---|---|---|---|---|
| `maintenance_mode` | false | boolean | Maintenance toggle | Edit, Toggle |
| `report_auto_deactivate_threshold` | 5 | number | Reports before auto-deactivate | Edit |
| `feature_roommate_match` | true | boolean | Enable roommate matching | Toggle |
| ... | ... | ... | ... | ... |

Edit dialog for each parameter with type-appropriate input.

**Locations (`/config/locations`):**

Tabbed: Cities | Universities | Geofences

Cities table with add/edit/delete. Map preview for coordinates.

Geofences: Interactive Leaflet map with polygon drawing tool.

**Amenities (`/config/amenities`):**

Drag-to-reorder list of amenities with icon picker, add/delete.

### 9.15 Premium & Monetization Page (`/premium`)

Tabbed:
1. **Premium Users** — List, grant/revoke, set tier/expiry
2. **Featured Listings** — Currently featured, schedule features
3. **Boosted Listings** — Currently boosted, view performance
4. **Pricing Insights** — Average prices, trends, outliers

### 9.16 System Health Page (`/health`)

```
┌─────────────────────────────────────────────────────┐
│  System Health                     [Last checked: 2m ago] [Refresh]
│                                                     │
│  ┌────────────────────────────────────────────────┐ │
│  │  Firebase Firestore          ✅ Operational    │ │
│  │  Reads: 12.4k/day  Writes: 3.2k/day           │ │
│  │  Latency: avg 45ms  P99: 120ms                │ │
│  └────────────────────────────────────────────────┘ │
│                                                     │
│  ┌────────────────────────────────────────────────┐ │
│  │  Firebase Auth               ✅ Operational    │ │
│  │  Sign-ins today: 234                            │ │
│  │  Failed attempts: 12                            │ │
│  └────────────────────────────────────────────────┘ │
│                                                     │
│  ┌────────────────────────────────────────────────┐ │
│  │  Cloud Functions             ✅ Operational    │ │
│  │  Invocations: 1.2k/day  Errors: 3              │ │
│  │  Avg latency: 230ms                            │ │
│  └────────────────────────────────────────────────┘ │
│                                                     │
│  ┌────────────────────────────────────────────────┐ │
│  │  Cloudflare Worker           ✅ Operational    │ │
│  │  Requests: 890/day  Errors: 0                  │ │
│  │  Avg latency: 89ms                             │ │
│  └────────────────────────────────────────────────┘ │
│                                                     │
│  ┌────────────────────────────────────────────────┐ │
│  │  Maintenance Mode            🔴 OFF            │ │
│  │  [Toggle Maintenance Mode]                     │ │
│  │  Message: "We'll be back soon!"                │ │
│  └────────────────────────────────────────────────┘ │
│                                                     │
│  ┌────────────────────────────────────────────────┐ │
│  │  App Version Distribution                      │ │
│  │  v1.0.32: 67%  ████████████░░░░  834 users    │ │
│  │  v1.0.31: 23%  ████░░░░░░░░░░░░  287 users    │ │
│  │  v1.0.30: 10%  ██░░░░░░░░░░░░░░  125 users    │ │
│  │                                                 │ │
│  │  Force Update Below: v1.0.30                    │ │
│  │  [Edit Force Update Version]                    │ │
│  └────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

### 9.17 Audit Logs Page (`/audit`)

**Table Columns:** Timestamp, Admin, Action, Category, Target, Details, IP.

**Filters:** Admin, Action type, Category, Date range, Target type.

**Export:** CSV download of filtered results.

**Detail expand:** Click row to see full `before`/`after` diff.

### 9.18 Admin Users Page (`/admins`)

**Table:** List of all admin users with role, permissions, status, last active.

**Actions:** Edit role, toggle permissions, deactivate, remove.

**Add Admin dialog:** Email input → search Firebase Auth → assign role → set permissions.

---

## 10. Component Library

### 10.1 `DataTable.tsx` — Reusable Data Table

The core component used across all list pages. Wraps TanStack Table.

```tsx
// Props interface
interface DataTableProps<TData, TValue> {
  columns: ColumnDef<TData, TValue>[]
  data: TData[]
  isLoading?: boolean
  searchKey?: string                    // Column key for search
  searchPlaceholder?: string
  filters?: FilterConfig[]              // Additional filter definitions
  bulkActions?: BulkAction[]            // Actions for selected rows
  onExport?: (data: TData[]) => void    // CSV export handler
  pageSize?: number                     // Default page size
  enableSelection?: boolean             // Show checkboxes
}
```

### 10.2 `KpiCard.tsx`

```tsx
interface KpiCardProps {
  title: string
  value: number | string
  icon: LucideIcon
  trend?: { value: number; isPositive: boolean }  // Percentage change
  subtitle?: string
  loading?: boolean
}
```

### 10.3 `ReportCard.tsx`

```tsx
interface ReportCardProps {
  report: Report
  onReview: (id: string) => void
  onDismiss: (id: string) => void
  onDeactivate: (id: string) => void
}
```

### 10.4 `ConfirmDialog.tsx`

```tsx
interface ConfirmDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description: string
  confirmText?: string
  cancelText?: string
  variant?: 'default' | 'destructive'
  onConfirm: () => void | Promise<void>
  isLoading?: boolean
}
```

### 10.5 `PageHeader.tsx`

```tsx
interface PageHeaderProps {
  title: string
  description?: string
  actions?: React.ReactNode            // Buttons in header
  breadcrumbs?: { label: string; href?: string }[]
}
```

### 10.6 `StatusBadge.tsx`

```tsx
interface StatusBadgeProps {
  status: string
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info'
}
// Renders colored badge: "Active" → green, "Banned" → red, etc.
```

### 10.7 `EmptyState.tsx`

```tsx
interface EmptyStateProps {
  icon: LucideIcon
  title: string
  description: string
  action?: { label: string; onClick: () => void }
}
```

### 10.8 `RelativeTime.tsx`

```tsx
// Renders "2 hours ago", "Yesterday", "3 days ago" etc.
// Auto-updates every minute
interface RelativeTimeProps {
  date: Timestamp | Date | number
}
```

---

## 11. API Routes

All API routes live under `src/app/api/` and follow this pattern:

```ts
// src/app/api/[resource]/[action]/route.ts
import { NextRequest, NextResponse } from 'next/server'
import { verifyAdmin, hasPermission } from '@/lib/auth'
import { extractToken } from '@/lib/auth'
import { PERMISSIONS } from '@/lib/permissions'

export async function POST(request: NextRequest) {
  // 1. Extract and verify auth token
  const token = extractToken(request)
  if (!token) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  let admin
  try {
    admin = await verifyAdmin(token)
  } catch {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
  }

  // 2. Check permission
  if (!hasPermission(admin, PERMISSIONS.USERS_BAN)) {
    return NextResponse.json({ error: 'Insufficient permissions' }, { status: 403 })
  }

  // 3. Parse request body
  const body = await request.json()

  // 4. Validate with Zod
  // 5. Execute operation
  // 6. Write audit log
  // 7. Return response
}
```

### API Route List

| Method | Route | Permission | Description |
|---|---|---|---|
| POST | `/api/admin/create` | `admins.manage` | Create new admin |
| POST | `/api/admin/update` | `admins.manage` | Update admin role/permissions |
| GET | `/api/admin/list` | `admins.manage` | List all admins |
| POST | `/api/users/ban` | `users.ban` | Ban a user |
| POST | `/api/users/unban` | `users.ban` | Unban a user |
| POST | `/api/users/delete` | `users.modify` | Soft-delete a user |
| GET | `/api/users/export` | `users.read` | Export users as CSV |
| POST | `/api/listings/approve` | `listings.modify` | Approve a listing |
| POST | `/api/listings/reject` | `listings.modify` | Reject a listing |
| POST | `/api/listings/feature` | `listings.modify` | Feature a listing |
| POST | `/api/listings/boost` | `listings.modify` | Boost a listing |
| GET | `/api/listings/export` | `listings.read` | Export listings as CSV |
| POST | `/api/reports/resolve` | `reports.resolve` | Resolve a report |
| POST | `/api/reports/bulk-resolve` | `reports.resolve` | Bulk resolve reports |
| POST | `/api/tickets/reply` | `tickets.respond` | Reply to a ticket |
| POST | `/api/tickets/assign` | `tickets.respond` | Assign ticket to admin |
| POST | `/api/notifications/send` | `notifications.send` | Send push notification |
| GET | `/api/config/remote-config` | `config.manage` | Get Remote Config params |
| POST | `/api/config/remote-config` | `config.manage` | Update Remote Config |
| GET | `/api/analytics/overview` | `analytics.read` | Dashboard analytics |
| GET | `/api/analytics/users` | `analytics.read` | User analytics |
| GET | `/api/analytics/export` | `analytics.read` | Export analytics CSV |
| POST | `/api/audit/log` | (any admin) | Write audit log entry |

---

## 12. Hooks & State Management

### 12.1 `useAuth.ts`

```ts
export function useAuth() {
  // Returns: { admin: AdminUser | null, loading: boolean, error: string | null }
  // Listens to onAuthStateChanged, fetches admin doc on auth
  // Provides logout() function
}
```

### 12.2 `usePermissions.ts`

```ts
export function usePermissions() {
  // Returns: { hasPermission: (perm: Permission) => boolean, role: string }
  // Reads from auth-store
}
```

### 12.3 `useUsers.ts`

```ts
export function useUsers(filters: UserFilters) {
  // Returns: { users: User[], total: number, loading: boolean, error: string | null }
  // Paginated, filtered Firestore query
  // Supports: role, city, gender, status, premium, search, dateRange
}
```

### 12.4 `useDashboard.ts`

```ts
export function useDashboard(dateRange: DateRange) {
  // Returns: {
  //   kpis: KpiData,
  //   signupChart: ChartData,
  //   cityChart: ChartData,
  //   activityFeed: ActivityItem[],
  //   loading: boolean
  // }
  // Aggregates data from multiple Firestore queries
  // Caches for 60 seconds (KPIs) / 5 minutes (charts)
}
```

### 12.5 `useConfirm.ts`

```ts
export function useConfirm() {
  // Returns: {
  //   confirm: (props: ConfirmProps) => Promise<boolean>,
  //   ConfirmDialog: React.Component  // Render this in your layout
  // }
  // Usage: const ok = await confirm({ title: "Ban user?", variant: "destructive" })
}
```

### 12.6 Zustand Stores

**`auth-store.ts`:**
```ts
interface AuthState {
  admin: AdminUser | null
  loading: boolean
  setAdmin: (admin: AdminUser | null) => void
  setLoading: (loading: boolean) => void
  logout: () => void
}
```

**`filter-store.ts`:**
```ts
interface FilterState {
  globalSearch: string
  userFilters: UserFilters
  listingFilters: ListingFilters
  reportFilters: ReportFilters
  ticketFilters: TicketFilters
  setGlobalSearch: (q: string) => void
  setUserFilters: (f: UserFilters) => void
  // ... etc
}
```

---

## 13. Utilities & Helpers

### 13.1 `lib/utils.ts`

```ts
import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatPrice(amount: number): string {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(amount)
}

export function formatDate(timestamp: number | { seconds: number }): string {
  const date = typeof timestamp === 'number'
    ? new Date(timestamp)
    : new Date(timestamp.seconds * 1000)
  return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

export function truncate(str: string, length: number): string {
  return str.length > length ? str.slice(0, length) + '…' : str
}

export function getInitials(name: string): string {
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
}
```

### 13.2 `lib/audit.ts`

```ts
import { collection, addDoc, serverTimestamp } from 'firebase/firestore'
import { db } from './firebase'

interface AuditLogEntry {
  adminId: string
  adminEmail: string
  adminRole: string
  action: string
  category: string
  targetType: string
  targetId: string
  targetSummary: string
  details: {
    before?: any
    after?: any
    reason?: string
    metadata?: Record<string, any>
  }
}

export async function writeAuditLog(entry: AuditLogEntry, request?: Request) {
  const logEntry = {
    ...entry,
    ipAddress: request?.headers.get('x-forwarded-for') || 'unknown',
    userAgent: request?.headers.get('user-agent') || 'unknown',
    timestamp: serverTimestamp(),
    sessionId: crypto.randomUUID(),
  }
  await addDoc(collection(db, 'audit_logs'), logEntry)
}
```

### 13.3 `lib/csv-export.ts`

```ts
import Papa from 'papaparse'
import { saveAs } from 'file-saver'

export function exportToCsv<T extends Record<string, any>>(
  data: T[],
  filename: string,
  headers?: Record<keyof T, string>
) {
  const csv = Papa.unparse(data, {
    columns: headers ? Object.keys(headers) : undefined,
  })
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  saveAs(blob, `${filename}_${new Date().toISOString().slice(0, 10)}.csv`)
}
```

---

## 14. Firebase Cloud Functions — Admin Extensions

### New Function: `onUserBanned`

Trigger: Firestore update on `users/{uid}` where `isActive` changes to `false`.

```js
// functions/index.js (addition)
exports.onUserBanned = functions.firestore
  .document('users/{userId}')
  .onUpdate(async (change, context) => {
    const before = change.before.data()
    const after = change.after.data()

    if (before.isActive === true && after.isActive === false) {
      // Deactivate all user's listings
      const listings = await admin.firestore()
        .collection('pg_listings')
        .where('ownerId', '==', context.params.userId)
        .get()

      const batch = admin.firestore().batch()
      listings.forEach(doc => {
        batch.update(doc.ref, { isActive: false })
      })

      // Deactivate all user's roommate posts
      const posts = await admin.firestore()
        .collection('roommate_posts')
        .where('userId', '==', context.params.userId)
        .get()

      posts.forEach(doc => {
        batch.update(doc.ref, { isActive: false })
      })

      await batch.commit()

      // Send notification to banned user
      // ... FCM send
    }
  })
```

### New Function: `onReportAutoAction`

Trigger: Firestore update on `reports/{reportId}`.

```js
exports.onReportAutoAction = functions.firestore
  .document('reports/{reportId}')
  .onUpdate(async (change, context) => {
    const before = change.before.data()
    const after = change.after.data()

    // When report is resolved with "banned" action, send notification
    if (before.status !== 'resolved' && after.status === 'resolved') {
      if (after.resolutionAction === 'warned') {
        // Send warning notification to content owner
      }
      if (after.resolutionAction === 'banned') {
        // Ban user (triggered from admin panel, this is the notification)
      }
    }
  })
```

---

## 15. Android App Changes

Minimal changes needed in the Android app:

### 15.1 `User.kt` — Add Fields

```kotlin
data class User(
    // ... existing fields ...

    // Admin moderation fields
    val isActive: Boolean = true,
    val bannedAt: Long? = null,
    val banReason: String? = null,
    val lastLoginAt: Long? = null,
    val loginCount: Int = 0,
    val profileCompleted: Boolean = false,
)
```

### 15.2 `SplashViewModel.kt` — Ban Check

```kotlin
// In the auth check flow, after fetching user:
if (user.isActive == false) {
    // Show "Account Suspended" screen
    // Don't navigate to home/dashboard
    _uiState.value = SplashUiState.AccountSuspended(
        reason = user.banReason ?: "Your account has been suspended."
    )
}
```

### 15.3 `AuthRepository.kt` — Update Login

```kotlin
// After successful sign-in:
// 1. Update lastLoginAt
// 2. Increment loginCount
// 3. Check isActive — if false, sign out and show suspension message
```

### 15.4 `ReportRepository.kt` — Add Report Types

```kotlin
// Extend report model for user/message reports
suspend fun reportUser(userId: String, reason: String, details: String = ""): Result<Unit>
suspend fun reportMessage(messageId: String, chatId: String, reason: String, details: String = ""): Result<Unit>
```

### 15.5 New: `AccountSuspendedScreen.kt`

A screen shown to banned users with:
- "Account Suspended" heading
- Ban reason (if provided)
- "Contact Support" button
- "Sign Out" button

---

## 16. Testing Strategy

### Unit Tests

| What | Framework | Coverage |
|---|---|---|
| Permission logic | Jest | All permission combinations |
| Utility functions | Jest | All helpers |
| Zod schemas | Jest | All form validations |
| Audit log writer | Jest + MSW | Correct data structure |

### Component Tests

| What | Framework | Notes |
|---|---|---|
| DataTable | Testing Library | Sort, filter, paginate, select |
| KpiCard | Testing Library | Render with data, loading state |
| ReportCard | Testing Library | Actions fire correctly |
| Login form | Testing Library | Submit, error states |
| ConfirmDialog | Testing Library | Confirm/cancel flows |

### Integration Tests

| What | Framework | Notes |
|---|---|---|
| Auth flow | MSW + Testing Library | Login → admin check → dashboard |
| User ban flow | MSW | API call → audit log → UI update |
| Report resolution | MSW | Action → status change → audit |

### E2E Tests (Phase 5)

| Flow | Tool |
|---|---|
| Login → Dashboard → Users → User Detail → Ban | Playwright |
| Reports Queue → Resolve → Audit Log | Playwright |
| Tickets → Reply → Close | Playwright |
| Config Edit → Verify in App | Playwright |

---

## 17. Deployment & CI/CD

### Vercel Configuration

```json
// vercel.json (in admin-panel/)
{
  "framework": "nextjs",
  "buildCommand": "next build",
  "outputDirectory": ".next",
  "installCommand": "npm install",
  "regions": ["sin1"],
  "env": {
    "FIREBASE_ADMIN_PROJECT_ID": "@firebase-admin-project-id",
    "FIREBASE_ADMIN_CLIENT_EMAIL": "@firebase-admin-client-email",
    "FIREBASE_ADMIN_PRIVATE_KEY": "@firebase-admin-private-key"
  }
}
```

### GitHub Actions (`.github/workflows/admin-panel.yml`)

```yaml
name: Admin Panel CI/CD

on:
  push:
    paths: ['admin-panel/**']
  pull_request:
    paths: ['admin-panel/**']

jobs:
  lint-and-type-check:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: admin-panel
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: 'npm'
          cache-dependency-path: admin-panel/package-lock.json
      - run: npm ci
      - run: npm run lint
      - run: npm run type-check

  test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: admin-panel
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: 'npm'
          cache-dependency-path: admin-panel/package-lock.json
      - run: npm ci
      - run: npm test

  deploy:
    needs: [lint-and-type-check, test]
    if: github.ref == 'refs/heads/master'
    runs-on: ubuntu-latest
    steps:
      - uses: amondnet/vercel-action@v25
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
          vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID }}
          working-directory: admin-panel
```

---

## 18. Implementation Phases — Day-by-Day

### Phase 1: Foundation (Days 1–15)

| Day | Task | Deliverable |
|---|---|---|
| 1 | Create Next.js project, install deps, configure Tailwind + shadcn/ui | Project skeleton |
| 2 | Set up `lib/firebase.ts`, `lib/firebase-admin.ts`, auth flow | Firebase connected |
| 3 | Build `lib/permissions.ts`, `lib/auth.ts`, auth-store (Zustand) | Permission system |
| 4 | Create `middleware.ts`, login page, denied page | Auth flow working |
| 5 | Build layout: Sidebar, Header, AppShell, breadcrumbs | Navigation shell |
| 6 | Build `PageHeader`, `StatusBadge`, `EmptyState`, `ConfirmDialog`, `RelativeTime` | Shared components |
| 7 | Build `DataTable` component with TanStack Table | Reusable table |
| 8 | Build `DataTablePagination`, `DataTableToolbar`, `DataTableColumnHeader` | Table features |
| 9 | Wire up Firestore `users` collection → `UserTable` | User list |
| 10 | Build user detail page (profile + activity tabs) | User detail |
| 11 | Build user moderation controls (ban/unban dialog) | User moderation |
| 12 | Wire up Firestore `pg_listings` → `ListingTable` | Listing list |
| 13 | Build listing detail page | Listing detail |
| 14 | Build KPI cards + `useDashboard` hook | Dashboard KPIs |
| 15 | Build dashboard charts (signup, city, role) | Dashboard charts |

### Phase 2: Moderation (Days 16–30)

| Day | Task | Deliverable |
|---|---|---|
| 16 | Build report schema extensions + migration script | Schema ready |
| 17 | Build `ReportCard`, `ReportKanban` components | Report kanban |
| 18 | Build report detail slide-over | Report detail |
| 19 | Build `ResolutionDialog` with action options | Report resolution |
| 20 | Wire up report API route (`/api/reports/resolve`) | Server-side resolution |
| 21 | Build bulk resolve functionality | Bulk operations |
| 22 | Build `TicketKanban`, `TicketCard` components | Ticket kanban |
| 23 | Build ticket detail page with conversation thread | Ticket detail |
| 24 | Build `TicketReplyForm` + `CannedResponses` | Ticket replies |
| 25 | Wire up ticket API routes (reply, assign) | Server-side tickets |
| 26 | Build ticket stats component | Ticket analytics |
| 27 | Build review moderation page | Review management |
| 28 | Build roommate post management | Roommate management |
| 29 | Wire up audit logging for all moderation actions | Audit trail |
| 30 | Build `WriteAuditLog` utility + test all flows | Phase 2 complete |

### Phase 3: Analytics & Config (Days 31–45)

| Day | Task | Deliverable |
|---|---|---|
| 31 | Build analytics data aggregation helpers | Data layer |
| 32 | Build main analytics page with overview tab | Analytics overview |
| 33 | Build user analytics tab (signups, retention, cohorts) | User analytics |
| 34 | Build listing analytics tab (supply, price, occupancy) | Listing analytics |
| 35 | Build date range picker + city selector components | Filter components |
| 36 | Build CSV export functionality | Export feature |
| 37 | Build Remote Config editor page | Config management |
| 38 | Build city management page (CRUD) | City config |
| 39 | Build university management page | University config |
| 40 | Build geofence editor (Leaflet polygon drawing) | Geofence config |
| 41 | Build amenity manager (drag-reorder list) | Amenity config |
| 42 | Build content type manager (room types, issue types) | Content config |
| 43 | Wire up config API routes | Server-side config |
| 44 | Build config audit trail | Config tracking |
| 45 | Test all config flows end-to-end | Phase 3 complete |

### Phase 4: Operations (Days 46–60)

| Day | Task | Deliverable |
|---|---|---|
| 46 | Build notification composer form | Notification UI |
| 47 | Build recipient picker (segments, city, role) | Recipient selection |
| 48 | Build notification preview component | Preview |
| 49 | Wire up notification send API route + FCM | Send capability |
| 50 | Build notification history page | History view |
| 51 | Build premium user management page | Premium management |
| 52 | Build featured/boost listing controls | Monetization controls |
| 53 | Build pricing insights page | Price analytics |
| 54 | Build chat room list + detail pages | Chat oversight |
| 55 | Build system health page | Health dashboard |
| 56 | Build admin user management page | Admin CRUD |
| 57 | Build audit log page with filters | Audit viewer |
| 58 | Build Cmd+K command palette | Quick navigation |
| 59 | Dark mode polish + responsive testing | UI polish |
| 60 | Full integration testing + bug fixes | Phase 4 complete |

### Phase 5: Advanced (Future — Days 61+)

| Feature | Effort | Priority |
|---|---|---|
| Auto-moderation rules engine | 1 week | High |
| 2FA for admin accounts | 3 days | High |
| Content quality scoring | 3 days | Medium |
| E2E tests (Playwright) | 1 week | Medium |
| Scheduled notifications | 3 days | Medium |
| Cohort analysis deep-dive | 3 days | Low |
| A/B testing framework | 1 week | Low |
| Multi-admin collaboration (notes) | 3 days | Low |
| Rate limiting dashboard | 2 days | Low |
| Email notifications (Resend) | 3 days | Low |

---

## 19. Edge Cases & Error Handling

### 19.1 Auth Edge Cases

| Scenario | Handling |
|---|---|
| User signs in but has no admin doc | Redirect to `/denied` with "Not an admin" message |
| Admin doc exists but `isActive == false` | Redirect to `/denied` with "Account suspended" |
| Admin doc permissions are empty array | Show dashboard but all nav items hidden, "No permissions" banner |
| Token expires mid-session | Auto-refresh via Firebase SDK; if refresh fails, redirect to login |
| Admin opens panel in incognito/private window | Works normally (Firebase Auth uses IndexedDB) |

### 19.2 Data Edge Cases

| Scenario | Handling |
|---|---|
| Firestore query returns 0 results | Show `EmptyState` component with contextual message |
| Firestore query fails (network) | Show error toast + retry button; cache last successful response |
| User has no profile image | Show initials avatar (first 2 letters of name) |
| Listing has 0 images | Show placeholder image |
| Listing price is 0 | Show "Free" badge instead of "₹0" |
| Report has empty `details` | Show "No additional details" placeholder |
| Ticket has no `adminReply` | Show "Awaiting response" status |
| Chat room has 0 messages | Show "No messages yet" |
| Timestamp is null/undefined | Show "Unknown" instead of crashing |

### 19.3 Concurrency Edge Cases

| Scenario | Handling |
|---|---|
| Two admins resolve same report simultaneously | Last-write-wins (Firestore handles this); audit log captures both |
| Admin bans user while user is online | User gets pushed to suspended screen via FCM or next Firestore read |
| Admin deletes listing while owner edits it | Owner's edit fails silently (listing no longer exists) |
| Bulk action on 100+ items | Process in batches of 50, show progress indicator |

### 19.4 Security Edge Cases

| Scenario | Handling |
|---|---|
| Non-admin accesses `/dashboard` directly | Middleware + layout auth check → redirect to `/login` |
| Admin's permissions are revoked while active | Next API call returns 403 → redirect to `/denied` |
| Admin tries to ban another super_admin | UI allows it but confirms "You are banning a super admin" |
| Admin tries to delete their own admin record | Prevented: "You cannot remove your own admin access" |
| Firestore rules bypassed (direct REST API) | Rules enforce `isAdmin()` check on every sensitive collection |
| Rate limiting on API routes | Implement basic rate limiting (100 req/min per IP) |

---

## 20. Performance Considerations

### 20.1 Firestore Query Optimization

| Concern | Solution |
|---|---|
| Counting large collections | Use Firestore `count()` aggregation (not fetching all docs) |
| Pagination | Use `startAfter` cursor-based pagination, not offset |
| Filtering + sorting | Create composite indexes for common filter combinations |
| Real-time listeners | Use sparingly — only on dashboard KPIs and active ticket queue |
| Denormalization | Store `ownerName` in listings, `userName` in reports (avoid lookups) |

**Required Firestore Indexes:**

```
// Composite indexes needed:
reports: (status ASC, createdAt DESC)
reports: (reportType ASC, severity ASC, createdAt DESC)
users: (role ASC, city ASC, createdAt DESC)
users: (isActive ASC, createdAt DESC)
pg_listings: (isActive ASC, city ASC, createdAt DESC)
pg_listings: (ownerId ASC, isActive ASC)
support_tickets: (status ASC, priority ASC, createdAt DESC)
support_tickets: (assignedTo ASC, status ASC)
audit_logs: (adminId ASC, timestamp DESC)
audit_logs: (action ASC, timestamp DESC)
audit_logs: (category ASC, timestamp DESC)
chats: (flagged ASC, createdAt DESC)
```

### 20.2 Frontend Performance

| Concern | Solution |
|---|---|
| Large tables (1000+ rows) | Virtual scrolling with `@tanstack/react-virtual` |
| Chart re-rendering | Memoize chart data with `useMemo`, debounce date range changes |
| Image loading | Lazy load listing thumbnails, use blur placeholders |
| Bundle size | Dynamic imports for heavy components (charts, map editor) |
| Search debounce | 300ms debounce on search inputs |
| SWR/React Query | Cache + stale-while-revalidate for all data fetching |

### 20.3 Caching Strategy

| Data | Cache Duration | Invalidation |
|---|---|---|
| KPI counts | 60 seconds | On page focus |
| Chart data | 5 minutes | Manual refresh button |
| User list | 30 seconds | On filter change |
| Config values | 5 minutes | On save (optimistic update) |
| Audit logs | No cache (always fresh) | — |
| Admin permissions | Session (until logout) | On role change |

---

## 21. Accessibility

### 21.1 WCAG 2.1 AA Compliance

| Requirement | Implementation |
|---|---|
| Color contrast | All text meets 4.5:1 ratio; shadcn/ui default theme verified |
| Keyboard navigation | All interactive elements focusable; tab order logical |
| Screen reader labels | `aria-label` on all icon buttons; `aria-describedby` for forms |
| Focus management | Auto-focus on dialog open; trap focus in modals |
| Error announcements | `aria-live="polite"` on error messages |
| Reduced motion | Respect `prefers-reduced-motion` for animations |

### 21.2 Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl+K` / `Cmd+K` | Open command palette |
| `Escape` | Close dialog/sheet/popover |
| `Enter` | Submit form / activate button |
| `Tab` | Move to next focusable element |
| `Shift+Tab` | Move to previous focusable element |
| `Arrow keys` | Navigate within table rows / kanban cards |

---

## Summary

| Aspect | Details |
|---|---|
| **Total Pages** | 20+ unique pages |
| **Total Components** | 80+ reusable components |
| **Total API Routes** | 20+ server-side endpoints |
| **Total Hooks** | 20+ custom React hooks |
| **New Firestore Collections** | 4 (`admins`, `audit_logs`, `notifications_sent`, `canned_responses`, `moderation_notes`) |
| **Modified Firestore Collections** | 5 (`users`, `reports`, `support_tickets`, `pg_listings`, `chats`) |
| **Firestore Security Rules** | Complete rewrite with `isAdmin()` + `hasPermission()` pattern |
| **Android App Changes** | 5 files modified, 1 new screen |
| **Firebase Cloud Functions** | 2 new triggers |
| **Implementation Time** | ~60 working days (12 weeks) across 4 phases |
| **Tech Stack** | Next.js 14 + TypeScript + Tailwind + shadcn/ui + Firebase + TanStack Table + Recharts |

This document is the single source of truth for the admin panel. Every implementation decision should reference this document first.
