# Changelog

## Unreleased

## 2.0.4 - 2026-08-17

### Meditation

- Mood is recorded even when you skip writing a reflection
- Removed Display Mode; sessions always show the countdown

### Toolkit

- Capture Thought (and Anxiety Log) expands to fill the space above the keyboard

## 2.0.3 - 2026-08-05

### Mood Tracker widget

- Widget mood taps reliably record and show a main-thread confirmation toast
- Selected face bounces three times like in-app quick-log (clear enlarge/shrink pulses)
- Stale flash state no longer permanently blocks further taps

### Home

- Mood check-ins from the home screen and widget appear in Recent activity

## 2.0.2 - 2026-08-02

### Mood graphs

- Smoother curves that no longer overshoot markers or fold backward in time
- Day/week/month axes always span the full period (day through midnight; month days evenly spaced)
- Month total mode plots one average point per day; data only appears for days that have passed

### Calendars

- Mood day sheets load historical meditations via range queries (not truncated recent timeline)
- Meditation calendar marks timer, breathing, and visualization practice days
- Meditation and affirmation calendars clamp navigation so you cannot page into the future

## 2.0.1 - 2026-08-02

### Setup & Toolkit

- Experience space toggles apply reliably during setup/Remodel (no longer silently blocked by Quick Start validity)
- Toolkit can be turned back on after being disabled; empty selection seeds default tools and refills Quick Start
- Toolkit step shows **Turn Toolkit back on** when the tab would be hidden
- Walkthrough order is now Spaces → Toolkit → Quick Start → Review

### Home

- Greeting name stacks under “Good afternoon,” so it no longer fights the settings cog

## 2.0.0 - 2026-08-02

### Brand

- Renamed to **SafeHaven:Affirmations & Focus** (`com.safehaven.affirmations`) with a cabin-and-pine launcher icon
- Fresh install required — does not upgrade in place from `com.example.meditationparticles`

### Mood graphs

- Day, week, and month charts use smooth curves into each point, full equally-spaced timelines, and 1–4 color faces on the Y-axis

### UX polish

- Status bar no longer overlaps Toolkit / Capture Thought chrome
- Long journal text scrolls inside the capture field and entry detail dialog
- Home greeting is left/right justified with the settings cog raised
- Flower tab uses a florist icon; Living Flower bubbles fade in/out over 1s when tags change

### OneNote

- Release/CI builds can inject `onenote.clientId` via GitHub secret `ONENOTE_CLIENT_ID` so sync is available in production APKs

## 1.0.39 - 2026-07-29

### Mood Tracker widget

- Selected mood face briefly enlarges on each of the three background color pulses (home-screen bounce feedback)

## 1.0.38 - 2026-07-29

### Mood Tracker widget

- After you log a mood, the widget background fast-fades that color in and out three times
- Red (very dissatisfied) face now shows a frown instead of a smile

## 1.0.37 - 2026-07-19

### Affirmations

- Archive affirmations you no longer want in your active list without deleting them
- View and restore archived affirmations from My Collection

## 1.0.36 - 2026-07-13

### Living Flower

- Name labels use black text on a white plate for clearer readability
- Selecting multiple tags shows only people who match **all** of those tags

## 1.0.35 - 2026-07-13

### Living Flower

- Larger person bubbles on concentric elliptical rings that use more of the screen before shrinking to fit
- Connector/spoke lines removed for a cleaner flower view
- Edit a person (name, tags, and notes) directly from the person detail sheet

## 1.0.34 - 2026-07-12

### Sanctuary tools

- Experience spaces in setup and Remodel now show short descriptions under each toggle
- New optional space: **Katie's Love List** — affirmations-style browse, review, reminders, and Quick Start, seeded with Katie's list (off by default; enable in Remodel → Spaces)

## 1.0.25 - 2026-06-26

### Onboarding & settings

- Paged sanctuary walkthrough for first-time setup (Welcome through Review)
- Settings: **Remodel your Sway** replaces destructive rebuild; preserves journals and history
- Quick Start: Visualizations removed from Other tools; HEARTS proactive/reactive toolkit shortcuts grouped separately

## 1.0.24 - 2026-06-26

### Home timeline

- HEARTS toolkit journal entries (Delight Deposit, Attunement Map, Repair & Reconnect, etc.) now appear in Recent activity on the home screen

### Data protection

- Local automatic JSON backup to a user-chosen folder (daily or weekly) via WorkManager; retains the latest 14 files
- HEARTS journal entries included in export/import JSON backups

### Tooling

- Timeout-guarded Android test runners (`scripts/run-tests.sh`, `scripts/verify-android-env.sh`)

## Production rollback — 2026-06-26

Reverted `main` / production manifest to **v1.0.23** (versionCode 24). Releases v1.0.24–v1.0.33 (Sanctuary walkthrough, landscapes, backup, and subsequent crash hotfixes) are withdrawn from production due to persistent upgrade and launch regressions.

Users on v1.0.24 or newer will not receive an in-app downgrade; reinstall the v1.0.23 APK from GitHub releases if needed.

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
