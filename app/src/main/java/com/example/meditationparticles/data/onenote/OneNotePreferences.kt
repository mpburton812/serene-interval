package com.example.meditationparticles.data.onenote

import android.content.Context
import com.example.meditationparticles.domain.onenote.OneNoteEntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class OneNotePrefsSnapshot(
    val syncEnabled: Boolean = false,
    val accountEmail: String? = null,
    val notebookId: String? = null,
    val notebookName: String? = null,
    val sectionId: String? = null,
    val sectionName: String? = null,
    val enabledEntryTypes: Set<OneNoteEntryType> = OneNoteEntryType.entries.toSet(),
    val lastSyncAtMillis: Long? = null,
    val lastError: String? = null,
    val moodScaleBackfillDone: Boolean = false,
)

class OneNotePreferences(context: Context) {
    private val store = OneNotePrefsDataStore(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _snapshot = MutableStateFlow(OneNotePrefsSnapshot())

    val snapshot: StateFlow<OneNotePrefsSnapshot> = _snapshot.asStateFlow()

    init {
        store.ensureMigratedBlocking()
        scope.launch {
            store.dataStore.data.collect { value ->
                _snapshot.value = value
            }
        }
        _snapshot.value = runBlocking(Dispatchers.IO) { store.read() }
    }

    fun load(): OneNotePrefsSnapshot = _snapshot.value

    fun setSyncEnabled(enabled: Boolean) {
        persist { it.copy(syncEnabled = enabled) }
    }

    fun setAccountEmail(email: String?) {
        persist { it.copy(accountEmail = email?.trim()?.takeIf { value -> value.isNotBlank() }) }
    }

    fun setNotebook(notebookId: String?, notebookName: String?) {
        persist {
            it.copy(
                notebookId = notebookId?.trim()?.takeIf { value -> value.isNotBlank() },
                notebookName = notebookName?.trim()?.takeIf { value -> value.isNotBlank() },
            )
        }
    }

    fun setSection(sectionId: String?, sectionName: String?) {
        persist {
            it.copy(
                sectionId = sectionId?.trim()?.takeIf { value -> value.isNotBlank() },
                sectionName = sectionName?.trim()?.takeIf { value -> value.isNotBlank() },
            )
        }
    }

    fun setSectionId(sectionId: String?) {
        setSection(sectionId, load().sectionName)
    }

    fun isEntryTypeEnabled(entryType: OneNoteEntryType): Boolean =
        entryType in load().enabledEntryTypes

    fun setEntryTypeEnabled(entryType: OneNoteEntryType, enabled: Boolean) {
        persist { current ->
            val updated = current.enabledEntryTypes.toMutableSet()
            if (enabled) {
                updated.add(entryType)
            } else {
                updated.remove(entryType)
            }
            current.copy(enabledEntryTypes = updated)
        }
    }

    fun setLastSyncAt(millis: Long?) {
        persist { it.copy(lastSyncAtMillis = millis?.takeIf { value -> value > 0L }) }
    }

    fun setLastError(message: String?) {
        persist {
            it.copy(lastError = message?.trim()?.takeIf { value -> value.isNotBlank() })
        }
    }

    fun clearConnection() {
        persist {
            OneNotePrefsSnapshot(
                enabledEntryTypes = it.enabledEntryTypes,
            )
        }
    }

    fun setMoodScaleBackfillDone(done: Boolean) {
        persist { it.copy(moodScaleBackfillDone = done) }
    }

    private fun persist(transform: (OneNotePrefsSnapshot) -> OneNotePrefsSnapshot) {
        scope.launch {
            store.update(transform)
        }
    }
}
