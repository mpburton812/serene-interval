# Changelog

## Unreleased

## 1.0.28 - 2026-06-25

### Launch crash hotfix

- Fix backup prompt check closing the Room database on startup (subsequent tab loads crashed)
- Guard backup scheduler and prompt logic with runCatching; schedule WorkManager off main thread

## 1.0.27 - 2026-06-25

### Sanctuary Phase 2 — landscape themes

- Seven tab backdrop landscapes (beach, cabin, desert, snowscape, deep woods, moon, space) with day/night variants
- Space day uses a golden-sun gradient; time-responsive appearance shifts landscapes automatically
- Restored Visual Sanctuary scene picker and bottom-nav Visualizations tab toggle
- Landscape theme picker in Settings, walkthrough, and onboarding

### Data protection

- Automatic JSON backups to a user-chosen folder (daily or weekly) via WorkManager; retains the latest 14 files
- Optional Google device backup for the journal database when enabled in Settings
- First-run prompt to set up backups after journal data is created
- Clear Settings copy that uninstalling Sway deletes in-app data unless backups are enabled

## 1.0.26 - 2026-06-25

### Launch crash hotfix (Pixel / Android 15+)

- Remove main-thread blocking from OneNote preferences initialization
- Load only the visible bottom-nav tab on launch instead of all tabs at once
- Move startup database work off the main thread
- Lazy-init OneNote sync in tab ViewModels; guard Room flows with catch
- Align MainActivity theme with edge-to-edge setup

## 1.0.25 - 2026-06-25

### Launch crash hotfix

- Catch home timeline DB errors instead of crashing on launch
- Guard startup Future Self database access
- Reconcile toolkit tab visibility with enabled tools on settings load
- Fix onboarding walkthrough ViewModel scoping and pager sync
- Register missing Visualizations navigation routes

## 1.0.24 - 2026-06-25

### Sway sanctuary walkthrough (Phase 1)

- Paged onboarding: Welcome, Name, Appearance, Spaces, Quick Start, Toolkit, Review
- Settings → Remodel your Sway (non-destructive; journals and history preserved)
- Zero toolkit tools hides the Toolkit tab; re-enabling any tool restores it
- HEARTS entries appear on home activity timeline when tools are disabled
- User-facing branding renamed to Sway

## 1.0.23 - 2026-06-25

### Toolkit — HEARTS lane

- New HEARTS-centered toolkit lane with Proactive and Reactive sections
- Tier 1: Delight Deposit, Attunement Map, Repair & Reconnect, Secure Self Check
- Tier 2: Presence Timer, Appreciation Ritual, Needs Before Negotiation, Attachment Story Snapshot
- Partner HEARTS Touchpoints: Flower dashboard for people tagged Partner
- Journal persistence and mood tracking for HEARTS tools

## 1.0.22 - 2026-06-25

### Affirmations

- Monthly affirmation review calendar between Affirmations Review and My Collection
- Daily Reminder moved below the affirmation calendar

### Living Flower

- Multi-ring sphere placement maximizes bubble size while avoiding overlap
- Spokes (pipes) validated so spheres do not cross pipe paths between center and other nodes

## 1.0.21 - 2026-06-14

### Mood graphs

- Draw graph lines with step paths (horizontal into each point, horizontal out) instead of smooth curves
- Block navigation and data for future days, weeks, and months; clip current-period graphs to now
- Bold dotted trend line for period averages, including 7-day rolling mode
- 7-day rolling month graph shows daily trailing-average plot points with step connections

### Mood calendar

- Tap a day to open a list of that day’s mood check-ins and activity events
- Tap an event for details (mood label or full journal/session text)

### Living Flower

- Rename Living Tree to Living Flower across navigation and settings
- Multi-ring bubble layout with even spacing; remove drag repositioning
- Re-space bubbles evenly when tag filters change

## 1.0.20 - 2026-06-14

### Meditation

- Monthly meditation calendar on the timer page with green days for completed sessions
- Month navigation to review prior meditation history

### Home & summaries

- Show mood icon on Recent Activity entries when a mood was logged
- Show mood icon on Previous Reflections list entries when a mood was logged

## 1.0.19 - 2026-06-14

### Home

- Recent activity timeline under Quick Start with chronological sessions and journal entries
- Tap entries with text to open the full note in a dialog

### Living Tree

- Remove floating pipe-texture artifacts; spokes connect center to nodes only
- Rounded corners on in-sphere name plate backgrounds

### Mood widget

- Evenly distribute mood faces across the widget with equal edge and inter-button spacing

## 1.0.18 - 2026-06-14

### Affirmations review

- Tap left/right below beads to go previous or next
- Rainbow bead colors from red (first) to purple (last)
- Session assessment dialog with mood and notes on finish
- OneNote sync for saved affirmation review sessions (Room schema v21)

### Living Tree

- Fix long-press drag so spheres follow your finger smoothly
- Add name plates behind in-sphere labels for readability

## 1.0.17 - 2026-06-14

### Living Tree

- Redesign nodes as glass spheres with breath-style rendering, pipe spokes, and shimmer
- Render person names inside spheres with contrast-aware label styling
- Tighten bubble layout with 10px gap and improved orbit sizing
- Persist drag positions with `radiusFraction` and `isUserPlaced` (Room schema v20)
- Extract shared glass sphere renderer and filter logic for clearer layout code

## 1.0.16 - 2026-06-05

### Living Tree

- Prevent bubble overlap when many nodes are visible; enlarge default bubble sizing
- Fix spacing when tag filters reduce visible nodes; correct dp/px mix in layout math
- Long-press drag to reposition bubbles with haptic feedback

## 1.0.15 - 2026-06-05

### Living Tree

- New Tree tab with radial star graph, multi-tag filtering, and setup CRUD
- Room schema v18, JSON export version 4, and unit tests
- Dynamic bubble sizing, drag reposition, bulk add in setup, and layout polish
- Tag colors, RGB picker, comma-separated names, default tags, and bubble readability overlay

### Affirmations

- Review mode with reorder; removed list view and favorites
- Fixed review mode so affirmation text and bead counter advance together

### Timer

- Removed Hourglass display mode from the meditation timer
## 1.0.14 â€” 2026-06-05

### Mood tracker widget

- Home screen widget with four mood buttons matching in-app quick-log colors and icons
- Tap records to `mood_entries` with `WIDGET` source via `MoodTrackerRepository`
- Configuration activity: white or black background and transparency slider (0â€“100%)
- Per-widget preferences stored in `MoodWidgetPreferences`; cleared when widget is removed

### Mood tracker (Phase 2)

- Home screen quick mood log card with large mood buttons and pulse animation
- Toolkit header shows day/week/month average mood faces; tap opens period graph
- Mood graph screen with canvas line chart, entry count, and average subtitle
- Date and mood axis labels on toolkit mood graphs
- Dual-write to `mood_entries` when saving mood from toolkit journals and timer reflections
- JSON export version 3 includes dedicated `moodEntries` array with clear-and-restore import

### Toolchain and dependencies

- Bumped compile/target SDK to 36, JVM target to 17, and enabled KSP2
- Upgraded AGP 9.2, Kotlin 2.4, KSP 2.3.9, Room 2.8, Compose BOM 2026.05, OkHttp 5.3, MSAL 8.3, and related AndroidX libraries
- Added DataStore and Tink dependencies for encrypted OneNote preference storage (wired in prior commits)

### Remove toolkit audio and dictate

- Removed `ToolkitAudioHelper`, `RECORD_AUDIO` permission, and audio path fields from Room entities, repositories, and UI
- Database migration v13â†’v14 rebuilds toolkit tables without audio columns (see migration extraction commit)
- Dropped home dictate/record flows and related toolkit audio controls

### OneNote and MSAL

- Updated `OneNoteAuthManager` for MSAL 8 interactive `signIn` and silent token APIs

### UI polish

- Added `HistoryGlassCard` and `sereneHistoryGlassStyle` for history surfaces
- Fixed `LocalActivity` retrieval in onboarding, settings, and particle canvas
- Timer and notification overlay cleanup tied to audio removal

### Mood scale overhaul (4 levels)

- Reduced mood scale from 5 to 4 levels (Red, Yellow, Blue, Green)
- Top mood uses dark green (`#2E7D32`); old level 5 entries migrate to level 4
- Unified `MoodPicker` and `MoodDisplay` across toolkit, timer reflections, and history
- Database migration v14â†’v15 remaps stored mood values
- JSON export bumped to version 2; includes meditation reflections and `moodScaleMax` metadata
- Legacy import backups map mood level 5 â†’ 4
- OneNote pages show mood as X/4 with color label; synced entries with mood are re-queued once after upgrade
