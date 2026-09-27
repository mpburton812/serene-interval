package com.safehaven.affirmations.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.DialogProperties

/**
 * Tracks entry popups that must survive a horizontal tab swipe.
 * While any source is active, the main tab pager disables scrolling.
 */
class EntryPopupLock {
    val sources = mutableStateMapOf<String, Unit>()

    fun setActive(id: String, active: Boolean) {
        if (active) {
            sources[id] = Unit
        } else {
            sources.remove(id)
        }
    }
}

val LocalEntryPopupLock = staticCompositionLocalOf { EntryPopupLock() }

val PersistentEntryDialogProperties = DialogProperties(
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
)

@Composable
fun ReportEntryPopup(id: String, active: Boolean) {
    val lock = LocalEntryPopupLock.current
    DisposableEffect(id, active, lock) {
        lock.setActive(id, active)
        onDispose { lock.setActive(id, false) }
    }
}
