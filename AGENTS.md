# AGENTS.md — StayBuddy

Universal instructions for AI coding agents (Claude Code, GitHub Copilot, Cursor, Antigravity, Gemini, Codex, and any IDE assistant). Read this file fully before making any change to this repository.

---

## 1. About the app

**StayBuddy** is an Android app for PG (paying-guest) accommodation discovery in one place:

- **Discover** — browse PG listings with price, room type, location, amenities, owner details, images, and availability.
- **Match** — roommate posts + compatibility quiz to find people whose lifestyle fits.
- **Connect** — in-app chat with owners/roommates, inquiries, favorites, and maps.
- **Own** — property owners get a dashboard to manage listings and inquiries.

Target users are students and working professionals in India. Prices are in INR (₹).

## 2. Tech stack

| Layer | Technology |
| --- | --- |
| Language | Kotlin 2.1 |
| UI | Jetpack Compose with **Material 3** (`androidx.compose.material3`) — no XML layouts, no Material 2 |
| Architecture | MVVM — ViewModel + StateFlow, repository pattern, Hilt DI |
| Navigation | Navigation Compose |
| Backend | Firebase (Auth, Firestore, Storage, Messaging, Remote Config, Analytics) |
| Local cache | Room + DataStore Preferences |
| Images | Coil (loading), Cloudinary (upload) |
| Maps | osmdroid + osmbonuspack (OpenStreetMap — **not** Google Maps) |
| Background work | WorkManager + Hilt Work |
| SDK | minSdk 26, target/compileSdk 36 |

## 3. Project structure

```
app/src/main/java/com/example/staybuddy/
├── MainActivity.kt / MainViewModel.kt / StayBuddyApp.kt
├── data/          # api, local (Room), manager, model, repository
├── di/            # Hilt modules
├── domain/        # use cases / domain models
├── notifications/ # FCM handling
├── service/       # foreground/background services
├── ui/
│   ├── components/  # shared design-system pieces (SbComponents.kt, PgListingCard.kt, …)
│   ├── screens/     # auth, chat, favorites, home, listing, map, onboarding,
│   │                # owner, profile, quiz, roommate, search, splash
│   └── theme/       # Color.kt, Theme.kt, Type.kt, Shape.kt  ← design source of truth
├── util/ + utils/
└── worker/        # WorkManager workers
staybuddy-backend/ # backend code
functions/         # Firebase Cloud Functions
design/            # design references/specs
```

---

## 4. STRICT rules — non-negotiable

1. **Material Design 3 only.** Every UI element must be a Material 3 component from `androidx.compose.material3` (`Scaffold`, `Card`, `Button`, `FilledTonalButton`, `TextField`, `NavigationBar`, `TopAppBar`, `ModalBottomSheet`, `AssistChip`, …). Never import `androidx.compose.material` (M2), never hand-roll a widget that M3 already provides, never add another UI framework.
2. **Never hardcode colors, text styles, or corner shapes in screens.** Always go through the theme:
   - Colors → `MaterialTheme.colorScheme.*` (e.g. `primary`, `surfaceContainer`, `onSurfaceVariant`)
   - Text → `MaterialTheme.typography.*` (e.g. `titleMedium`, `bodyLarge`)
   - Corners → `MaterialTheme.shapes.*`
   - The only exception: semantic badge colors already defined in `ui/theme/Color.kt` (`SuccessGreen`, `WarningAmber`, `RatingAmber`, `VerifiedTeal`).
3. **Don't change existing working things** — screens, components, logic, navigation, gradle config, CI — unless the user explicitly asked for it or it is part of an agreed plan. A task to fix screen A is not permission to "improve" screen B. No drive-by refactors, renames, dependency bumps, or reformatting of untouched code.
4. **Reuse before you create.** Check `ui/components/` (especially `SbComponents.kt`, `PgListingCard.kt`, `VerifiedBadge.kt`, `FreshnessTag.kt`, `ShimmerEffect.kt`) before writing a new composable — see the **Shared component API** table in §6. If a similar piece exists, use or extend it. Only add a new shared component when it will be used by 2+ screens.
5. **Do not modify the theme files** (`Color.kt`, `Theme.kt`, `Type.kt`, `Shape.kt`) unless the task is explicitly about the theme. They are the design source of truth for the whole app.
6. **No new dependencies without asking.** Propose the library, why it's needed, and what it costs (size, transitive deps) first.
7. **Never commit secrets.** `local.properties`, keystores, `google-services.json`, and API keys stay out of source and out of generated code.
8. **Follow MVVM strictly.** No business logic or Firebase/Room calls inside composables — screens observe ViewModel state (`StateFlow` + `collectAsStateWithLifecycle`) and emit events upward. Data access lives in repositories, injected via Hilt.

## 5. Understand the request first

Before writing any code:

1. **Restate the goal to yourself.** What exactly is being asked? Which screens/files does it touch? If the request is ambiguous, ask a clarifying question instead of guessing.
2. **Read the surrounding code.** Look at how neighboring screens/components solve the same kind of problem and match that pattern — this codebase values consistency over novelty.
3. **Ideate before you implement.** For anything non-trivial (new screen, new feature, structural change), briefly brainstorm 2–3 approaches, weigh them against the existing architecture, pick one, and state the plan. For big changes, present the plan and wait for approval before touching code.
4. **Keep diffs minimal.** The smallest change that correctly solves the request wins. Don't reformat, reorder imports of untouched files, or rewrite what already works.

## 6. Design language — "Hearth"

The visual identity is warm and homely: warm paper surfaces, deep evergreen primary (trust/home), clay accent for prices and highlights, sage secondary. Defined in `ui/theme/`.

### Color roles (light theme)

| Role | Color | Hex | Used for |
| --- | --- | --- | --- |
| `primary` | Evergreen | `#16705B` | CTAs, active states, brand |
| `secondary` | Sage | `#4C6358` | Supporting UI, chips |
| `tertiary` | Clay | `#9C4A20` | **Prices**, warm highlights |
| `background` / `surface` | Paper | `#FAF7F1` | Screen backgrounds |
| `error` | | `#BA1A1A` | Errors, destructive actions |

Dark theme uses bright variants (`EvergreenBright #87D6BC`, etc.) on green-tinted charcoal (`#131511`). Semantic extras (outside M3 roles): `SuccessGreen #2E7D32`, `WarningAmber #B26A00`, `RatingAmber #E8A000`, `VerifiedTeal #0E7B6C`.

Rules:
- Prices always render in the **tertiary (Clay)** role.
- Dynamic (Material You) color is deliberately **off** — brand-first. Don't enable it.
- Support both light and dark themes in everything you build; never assume light.

### Typography

Single family: **Plus Jakarta Sans** (bundled variable font). Use the `MaterialTheme.typography` scale — display (ExtraBold) for hero numbers/splash, headline (Bold) for screen titles, title/body/label for everything else. Never introduce another font or inline `TextStyle` with a hardcoded size.

### Shape

Soft, homely corners via `MaterialTheme.shapes`: extraSmall 8dp (badges/tags) · small 12dp (chips/fields) · medium 16dp (cards) · large 22dp (sheets/hero cards) · extraLarge 28dp (dialogs/bottom sheets).

### Consistency checklist for every UI change

- [ ] Only `material3` components; theme roles for all colors/type/shapes
- [ ] Reuses existing shared components where one fits
- [ ] Looks correct in **both** light and dark theme
- [ ] Loading states use `ShimmerEffect`, not spinners invented per-screen
- [ ] Spacing follows the 4dp grid (4/8/12/16/24dp)
- [ ] Content descriptions on meaningful icons/images (accessibility)
- [ ] Strings that users see are not hardcoded in odd formats — currency uses ₹ INR formatting like existing components

### Shared component API — reuse these before hand-rolling anything

All in `com.example.staybuddy.ui.components`. Import and use; do **not** reinvent a price pill, spinner, empty state, or badge.

| Need | Use | Key params |
| --- | --- | --- |
| Show a rent price | `PriceTag` | `PriceTag(price: Int, suffix = "/mo", large = false)` — renders in Clay/tertiary |
| Format currency inline | `formatInr` | `formatInr(amount: Int): String` → `"₹8,500"` (Indian digit grouping) |
| Section title + "See all" | `SectionHeader` | `SectionHeader(title, actionLabel?, onAction?)` |
| Empty screen state | `EmptyState` | `EmptyState(icon, title, message, actionLabel?, onAction?)` |
| Inline error + retry | `ErrorBanner` | `ErrorBanner(message, onRetry?)` |
| PG listing card | `PgListingCard` | `PgListingCard(listing, isFavorite, onCardClick, onFavoriteClick, onEditClick?, distanceKm?)` |
| "Verified" mark | `VerifiedBadge` | `VerifiedBadge(size = 16.dp, showLabel = false)` |
| Freshness tag ("New / 2d ago") | `FreshnessTag` | `FreshnessTag(createdAt: Long)` |
| Loading placeholder | `ShimmerEffect` family | `ListingCardSkeleton`, `ChatListShimmer`, `ProfileScreenShimmer`, `GenericScreenShimmer`, `shimmerBrush()` |
| Map / location pick | `OsmMapView`, `LocationPicker` | osmdroid-backed (not Google Maps) |

### Screen skeleton to copy

Every data-backed screen handles **loading / empty / error / success** — copy the pattern from a neighboring screen; don't invent per-screen spinners.

```kotlin
@Composable
fun XxxScreen(
    navController: NavController,
    viewModel: XxxViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Title") }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val s = state) {
            is UiState.Loading -> GenericScreenShimmer(Modifier.padding(padding))
            is UiState.Error   -> ErrorBanner(s.message, onRetry = viewModel::retry)
            is UiState.Empty   -> EmptyState(Icons.Rounded.Inbox, "Nothing here yet", "…")
            is UiState.Success -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { /* content */ }
        }
    }
}
```

### Anti-patterns — reject on sight

```kotlin
// ❌ M2 import            → ✅ import androidx.compose.material3.Button
import androidx.compose.material.Button
// ❌ hardcoded price color → ✅ PriceTag(price = 8500)
Text("₹8500", color = Color(0xFF9C4A20))
// ❌ inline text size      → ✅ style = MaterialTheme.typography.headlineMedium
Text("Title", fontSize = 22.sp, fontWeight = FontWeight.Bold)
// ❌ raw shape literal      → ✅ shape = MaterialTheme.shapes.medium
Card(shape = RoundedCornerShape(16.dp)) { … }
// ❌ Firebase in composable → ✅ collectAsStateWithLifecycle() from the ViewModel
val docs = Firebase.firestore.collection("pgs").get()
```

## 7. Code conventions

- Kotlin official style; composables in PascalCase; one screen package per feature under `ui/screens/`.
- Screen pattern: `XxxScreen(navController, viewModel: XxxViewModel = hiltViewModel())` — stateless content composables where practical, state hoisted to the ViewModel.
- Use `KDoc`-style comments only where intent isn't obvious from code; match the comment density of the file you're editing.
- Prefer `remember`/`derivedStateOf` correctly; avoid recomposition traps (unstable lambdas, reading state too high).
- Handle empty/loading/error states for every data-backed screen — the codebase already has patterns for this; copy them.

## 8. Build & verify

```bash
./gradlew :app:assembleDebug      # build
./gradlew :app:lintDebug          # lint
./gradlew test                    # unit tests
```

- The change must compile before you declare it done. If you cannot build in your environment, say so explicitly — never claim untested code works.
- Don't edit files under `build/`, `*.txt` build logs at the repo root, or generated code.
- CI runs the Android workflow on PRs (`.github/workflows/`); don't break it.

## 9. Git etiquette

- Never commit or push unless the user asks.
- Conventional-commit style messages (`feat:`, `fix:`, `docs:`, `ci:`), matching existing history.
- Work on the current branch; never force-push or rewrite history.

## 10. When in doubt

Ask. A one-line clarifying question is always cheaper than an unwanted rewrite. If you spot a real bug outside your task's scope, report it — don't silently fix it.

## 11. Reticle and multi-agent tooling

Reticle MCP is configured for multiple agents/IDEs in this repository. Treat it as local agent tooling, not an Android app dependency.

- Use Reticle only for browser-rendered surfaces that have the Reticle dev SDK connected, such as prototypes in `design/` or a future web dashboard.
- Do not add `@reticlehq/core` or any Reticle SDK dependency to the native Android Gradle app unless the user explicitly asks for a supported web surface.
- Native StayBuddy verification still requires Android-native checks: `./gradlew :app:assembleDebug`, `./gradlew :app:lintDebug`, `./gradlew test`, and emulator/manual QA where needed.
- Never claim a Compose screen, Room state, Firebase call, or emulator flow is Reticle-verified unless there is an actual connected browser session proving that specific web surface.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
