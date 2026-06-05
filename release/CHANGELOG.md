# Changelog

## Unreleased

### Mood scale overhaul (4 levels)

- Reduced mood scale from 5 to 4 levels (Red, Yellow, Blue, Green)
- Top mood uses dark green (`#2E7D32`); old level 5 entries migrate to level 4
- Unified `MoodPicker` and `MoodDisplay` across toolkit, timer reflections, and history
- Database migration v14→v15 remaps stored mood values
- JSON export bumped to version 2; includes meditation reflections and `moodScaleMax` metadata
- Legacy import backups map mood level 5 → 4
- OneNote pages show mood as X/4 with color label; synced entries with mood are re-queued once after upgrade
