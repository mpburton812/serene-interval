# Changelog

## Unreleased

### Toolchain and dependencies

- Bumped compile/target SDK to 36, JVM target to 17, and enabled KSP2
- Upgraded AGP 9.2, Kotlin 2.4, KSP 2.3.9, Room 2.8, Compose BOM 2026.05, OkHttp 5.3, MSAL 8.3, and related AndroidX libraries
- Added DataStore and Tink dependencies for encrypted OneNote preference storage (wired in prior commits)

### Remove toolkit audio and dictate

- Removed `ToolkitAudioHelper`, `RECORD_AUDIO` permission, and audio path fields from Room entities, repositories, and UI
- Database migration v13→v14 rebuilds toolkit tables without audio columns (see migration extraction commit)
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
- Database migration v14→v15 remaps stored mood values
- JSON export bumped to version 2; includes meditation reflections and `moodScaleMax` metadata
- Legacy import backups map mood level 5 → 4
- OneNote pages show mood as X/4 with color label; synced entries with mood are re-queued once after upgrade
