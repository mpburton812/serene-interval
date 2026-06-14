# Changelog

## Unreleased

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
