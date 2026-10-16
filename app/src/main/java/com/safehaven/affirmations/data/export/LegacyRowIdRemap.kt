package com.safehaven.affirmations.data.export

/**
 * Journal rows are inserted with new ids. Mood entries keep the exported legacy id, so restore
 * must translate that id to the row that was just inserted (or the existing duplicate).
 */
internal fun remapLegacyLink(
    legacyTable: String?,
    legacyRowId: Long?,
    idMap: Map<Pair<String, Long>, Long>,
): Pair<String?, Long?> {
    if (legacyTable.isNullOrBlank() || legacyRowId == null) return null to null
    val mapped = idMap[legacyTable to legacyRowId] ?: return null to null
    return legacyTable to mapped
}

internal fun MutableMap<Pair<String, Long>, Long>.rememberExportedId(
    table: String,
    exportedId: Long?,
    storedId: Long,
) {
    if (exportedId != null && exportedId > 0L) {
        this[table to exportedId] = storedId
    }
}
