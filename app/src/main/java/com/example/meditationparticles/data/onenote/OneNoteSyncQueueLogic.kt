package com.example.meditationparticles.data.onenote

import com.example.meditationparticles.data.local.OneNoteSyncQueueEntity

object OneNoteSyncQueueLogic {
    const val MAX_RETRY_COUNT = 10

    fun sortPendingQueue(items: List<OneNoteSyncQueueEntity>): List<OneNoteSyncQueueEntity> =
        items.sortedWith(compareBy({ it.enqueuedAt }, { it.id }))

    fun eligibleForProcessing(items: List<OneNoteSyncQueueEntity>): List<OneNoteSyncQueueEntity> =
        sortPendingQueue(items).filter { it.retryCount < MAX_RETRY_COUNT }

    fun shouldDropItem(item: OneNoteSyncQueueEntity): Boolean =
        item.retryCount >= MAX_RETRY_COUNT
}
