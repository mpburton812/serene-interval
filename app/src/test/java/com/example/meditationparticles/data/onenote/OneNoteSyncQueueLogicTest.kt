package com.example.meditationparticles.data.onenote

import com.example.meditationparticles.data.local.OneNoteSyncQueueEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OneNoteSyncQueueLogicTest {
    @Test
    fun sortPendingQueue_ordersByEnqueuedAtThenId() {
        val sorted = OneNoteSyncQueueLogic.sortPendingQueue(
            listOf(
                item(id = 3L, enqueuedAt = 200L),
                item(id = 1L, enqueuedAt = 100L),
                item(id = 2L, enqueuedAt = 100L),
            ),
        )

        assertEquals(listOf(1L, 2L, 3L), sorted.map { it.id })
    }

    @Test
    fun eligibleForProcessing_excludesMaxRetryItems() {
        val eligible = OneNoteSyncQueueLogic.eligibleForProcessing(
            listOf(
                item(id = 1L, retryCount = 9),
                item(id = 2L, retryCount = 10),
                item(id = 3L, retryCount = 0),
            ),
        )

        assertEquals(listOf(1L, 3L), eligible.map { it.id })
    }

    @Test
    fun shouldDropItem_whenRetryCountReached() {
        assertTrue(OneNoteSyncQueueLogic.shouldDropItem(item(retryCount = 10)))
        assertFalse(OneNoteSyncQueueLogic.shouldDropItem(item(retryCount = 9)))
    }

    private fun item(
        id: Long = 1L,
        enqueuedAt: Long = 100L,
        retryCount: Int = 0,
    ) = OneNoteSyncQueueEntity(
        id = id,
        localEntryId = 99L,
        entryType = "NVC",
        enqueuedAt = enqueuedAt,
        retryCount = retryCount,
    )
}
